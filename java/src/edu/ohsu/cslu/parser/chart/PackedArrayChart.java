/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.lela.ConstrainingChart;
import edu.ohsu.cslu.parser.ParseTask;

/**
 * Stores a chart in a 4-way parallel array of:
 * <ol>
 * <li>Populated non-terminals (short)</li>
 * <li>Probabilities of those non-terminals (float)</li>
 * <li>Child pairs producing each non-terminal --- equivalent to the grammar rule (int)</li>
 * <li>Midpoints (short)</li>
 * </ol>
 * 
 * Those 4 pieces of information allow us to back-trace through the chart and construct the parse tree.
 * 
 * Each parallel array entry consumes 2 + 4 + 4 + 2 = 12 bytes
 * 
 * Individual cells in the parallel array are indexed by cell offsets of fixed length (the number of non-terminals in
 * the grammar).
 * 
 * The ancillary data structures are relatively small, so the total size consumed is approximately = n * (n-1) / 2 * V *
 * 12 bytes.
 * 
 * Similar to {@link DenseVectorChart}, but observed non-terminals are packed together in
 * {@link PackedArrayChartCell#finalizeCell()}; this packing scan and the resulting denser access to observed
 * non-terminals may prove beneficial on certain architectures.
 * 
 * @see DenseVectorChart
 * 
 * @author Aaron Dunlop
 * @since March 25, 2010
 */
public class PackedArrayChart extends ParallelArrayChart {

    /**
     * Parallel array storing non-terminals (parallel to {@link ParallelArrayChart#insideProbabilities},
     * {@link ParallelArrayChart#packedChildren}, and {@link ParallelArrayChart#midpoints}. Entries for each cell begin
     * at indices from {@link #cellOffsets}.
     */
    public final short[] nonTerminalIndices;

    /**
     * The number of non-terminals populated in each cell. Indexed by cell index ({@link #cellIndex(int, int)} ).
     */
    public final int[] numNonTerminals;

    /**
     * The index in the main chart array of the first non-terminal in each cell which is valid as a left child. Indexed
     * by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] minLeftChildIndex;

    /**
     * The index in the main chart array of the last non-terminal in each cell which is valid as a left child. Indexed
     * by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] maxLeftChildIndex;

    /**
     * The index in the main chart array of the last non-terminal in each cell which is valid as a right child. Indexed
     * by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] minRightChildIndex;

    /**
     * The index in the main chart array of the last non-terminal in each cell which is valid as a right child. Indexed
     * by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] maxRightChildIndex;

    /** The number of 'segments' dividing left children for multi-threading cartesian-product operation. */
    private final int leftChildSegments;

    /**
     * Indices in the main chart array of each 'segment', dividing left children for multi-threading cartesian-product
     * operation. Indexed by cellIndex * (segment count + 1) + segment num. e.g. if each left cell is divided into 4
     * segments ({@link #leftChildSegments} == 4), segment 3 of cell 12 will begin at
     * {@link #leftChildSegmentStartIndices}[12 * 5 + 3] and end at {@link #leftChildSegmentStartIndices} [12 * 5 + 4].
     */
    public final int[] leftChildSegmentStartIndices;

    protected final ThreadLocal<PackedArrayChart.TemporaryChartCell> threadLocalTemporaryCells;

    /**
     * Constructs a chart
     * 
     * @param tokens Sentence tokens, mapped to integer indices
     * @param sparseMatrixGrammar Grammar
     * @param beamWidth
     * @param lexicalRowBeamWidth
     * @param leftChildSegments The number of 'segments' to split left children into; used to multi-thread
     *            cartesian-product operation.
     */
    public PackedArrayChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar,
            final int beamWidth, final int lexicalRowBeamWidth, final int leftChildSegments) {
        super(parseTask, sparseMatrixGrammar, Math.min(beamWidth, sparseMatrixGrammar.numNonTerms()), Math.min(
                lexicalRowBeamWidth, sparseMatrixGrammar.numNonTerms()));

        numNonTerminals = new int[cells];
        minLeftChildIndex = new int[cells];
        maxLeftChildIndex = new int[cells];
        minRightChildIndex = new int[cells];
        maxRightChildIndex = new int[cells];

        nonTerminalIndices = new short[chartArraySize];

        this.leftChildSegments = leftChildSegments;
        if (leftChildSegments > 0) {
            leftChildSegmentStartIndices = new int[(cells * (leftChildSegments + 1)) + 1];
        } else {
            leftChildSegmentStartIndices = null;
        }
        reset(parseTask);

        // Temporary cell storage for each cell-level thread
        this.threadLocalTemporaryCells = new ThreadLocal<PackedArrayChart.TemporaryChartCell>() {
            @Override
            protected PackedArrayChart.TemporaryChartCell initialValue() {
                return new PackedArrayChart.TemporaryChartCell(grammar);
            }
        };
    }

    /**
     * Constructs a chart
     * 
     * @param tokens Sentence tokens, mapped to integer indices
     * @param sparseMatrixGrammar Grammar
     * @param beamWidth
     * @param lexicalRowBeamWidth
     */
    public PackedArrayChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar,
            final int beamWidth, final int lexicalRowBeamWidth) {
        this(parseTask, sparseMatrixGrammar, beamWidth, lexicalRowBeamWidth, 0);
    }

    /**
     * Constructs a chart for exhaustive inference.
     * 
     * @param tokens Sentence tokens, mapped to integer indices
     * @param sparseMatrixGrammar Grammar
     */
    public PackedArrayChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar) {
        this(parseTask, sparseMatrixGrammar, sparseMatrixGrammar.numNonTerms(), sparseMatrixGrammar.numNonTerms(), 0);
    }

    /**
     * Constructs a chart for constrained parsing (see {@link ConstrainingChart}.
     * 
     * @param size
     * @param chartArraySize
     * @param sparseMatrixGrammar
     */
    protected PackedArrayChart(final int size, final int chartArraySize, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(size, chartArraySize, sparseMatrixGrammar);

        this.nonTerminalIndices = new short[chartArraySize];
        Arrays.fill(nonTerminalIndices, Short.MIN_VALUE);

        this.threadLocalTemporaryCells = null;
        this.numNonTerminals = new int[cells];
        this.minLeftChildIndex = null;
        this.minRightChildIndex = null;
        this.maxLeftChildIndex = null;
        this.maxRightChildIndex = null;
        this.leftChildSegments = 0;
        this.leftChildSegmentStartIndices = null;
    }

    @Override
    public void reset(final ParseTask task) {
        this.parseTask = task;
        this.size = task.sentenceLength();
        Arrays.fill(numNonTerminals, 0);
        if (leftChildSegmentStartIndices != null) {
            Arrays.fill(leftChildSegmentStartIndices, 0);
        }

        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end <= size; end++) {
                final int cellIndex = cellIndex(start, end);
                final int offset = cellOffset(start, end);

                cellOffsets[cellIndex] = offset;
                minLeftChildIndex[cellIndex] = offset;
                maxLeftChildIndex[cellIndex] = offset - 1;
                minRightChildIndex[cellIndex] = offset;
                maxRightChildIndex[cellIndex] = offset - 1;
            }
        }
    }

    @Override
    public BinaryTree<String> extractBestParse(final int start, final int end, final int parent) {
        final PackedArrayChartCell packedCell = getCell(start, end);

        if (packedCell == null) {
            return null;
        }

        // Find the index of the non-terminal in the chart storage
        final int i = Arrays.binarySearch(nonTerminalIndices, packedCell.offset, packedCell.offset
                + numNonTerminals[packedCell.cellIndex], (short) parent);
        if (i < 0) {
            return null;
        }
        final int edgeChildren = packedChildren[i];
        final short edgeMidpoint = midpoints[i];

        final BinaryTree<String> subtree = new BinaryTree<String>(sparseMatrixGrammar.nonTermSet.getSymbol(parent));
        final int leftChild = sparseMatrixGrammar.packingFunction().unpackLeftChild(edgeChildren);
        final int rightChild = sparseMatrixGrammar.packingFunction().unpackRightChild(edgeChildren);

        if (rightChild == Production.UNARY_PRODUCTION) {
            subtree.addChild(extractBestParse(start, end, leftChild));

        } else if (rightChild == Production.LEXICAL_PRODUCTION) {
            subtree.addChild(new BinaryTree<String>(sparseMatrixGrammar.lexSet.getSymbol(leftChild)));

        } else {
            // binary production
            subtree.addChild(extractBestParse(start, edgeMidpoint, leftChild));
            subtree.addChild(extractBestParse(edgeMidpoint, end, rightChild));
        }
        return subtree;
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int cellIndex = cellIndex(start, end);
        final int offset = offset(cellIndex);
        final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                (short) nonTerminal);
        if (index < 0) {
            return Float.NEGATIVE_INFINITY;
        }
        return insideProbabilities[index];
    }

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        throw new UnsupportedOperationException("Not supported by PackedArrayChart");
    }

    @Override
    public PackedArrayChartCell getCell(final int start, final int end) {
        return new PackedArrayChartCell(start, end);
    }

    public final int minLeftChildIndex(final int cellIndex) {
        return minLeftChildIndex[cellIndex];
    }

    public final int maxLeftChildIndex(final int cellIndex) {
        return maxLeftChildIndex[cellIndex];
    }

    public final int minRightChildIndex(final int cellIndex) {
        return minRightChildIndex[cellIndex];
    }

    public final int maxRightChildIndex(final int cellIndex) {
        return maxRightChildIndex[cellIndex];
    }

    public int[] numNonTerminals() {
        return numNonTerminals;
    }

    // Added by Nate
    public int cellNonTermStartIndex(final short start, final short end) {
        return offset(cellIndex(start, end));
    }

    public int cellNonTermEndIndex(final short start, final short end) {
        return cellNonTermStartIndex(start, end) + numNonTerminals()[cellIndex(start, end)] - 1;
    }

    public class PackedArrayChartCell extends ParallelArrayChartCell {

        public TemporaryChartCell tmpCell;

        public PackedArrayChartCell(final int start, final int end) {
            super(start, end);
        }

        public void allocateTemporaryStorage() {
            // Allocate storage
            if (tmpCell == null) {
                // this.tmpCell = threadLocalTemporaryCells.get();
                // this.tmpCell.clear();
                this.tmpCell = new TemporaryChartCell(grammar);

                // Copy from main chart array to temporary parallel array
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                    final int nonTerminal = nonTerminalIndices[i];
                    tmpCell.packedChildren[nonTerminal] = packedChildren[i];
                    tmpCell.insideProbabilities[nonTerminal] = insideProbabilities[i];
                    tmpCell.midpoints[nonTerminal] = midpoints[i];
                }
            }
        }

        /**
         * Copy and pack entries from temporary array into the main chart array
         */
        @Override
        public void finalizeCell() {

            if (tmpCell == null) {
                return;
            }

            // Copy all populated entries from temporary storage
            boolean foundMinLeftChild = false, foundMinRightChild = false;
            int nonTerminalOffset = offset;

            minLeftChildIndex[cellIndex] = offset;
            maxLeftChildIndex[cellIndex] = offset - 1;
            minRightChildIndex[cellIndex] = offset;
            maxRightChildIndex[cellIndex] = offset - 1;

            for (short nonTerminal = 0; nonTerminal < tmpCell.insideProbabilities.length; nonTerminal++) {

                if (tmpCell.insideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {

                    nonTerminalIndices[nonTerminalOffset] = nonTerminal;
                    insideProbabilities[nonTerminalOffset] = tmpCell.insideProbabilities[nonTerminal];
                    packedChildren[nonTerminalOffset] = tmpCell.packedChildren[nonTerminal];
                    midpoints[nonTerminalOffset] = tmpCell.midpoints[nonTerminal];

                    if (sparseMatrixGrammar.isValidLeftChild(nonTerminal)) {
                        if (!foundMinLeftChild) {
                            minLeftChildIndex[cellIndex] = nonTerminalOffset;
                            foundMinLeftChild = true;
                        }
                        maxLeftChildIndex[cellIndex] = nonTerminalOffset;
                    }

                    if (sparseMatrixGrammar.isValidRightChild(nonTerminal)) {
                        if (!foundMinRightChild) {
                            minRightChildIndex[cellIndex] = nonTerminalOffset;
                            foundMinRightChild = true;
                        }
                        maxRightChildIndex[cellIndex] = nonTerminalOffset;
                    }

                    nonTerminalOffset++;
                }
            }

            numNonTerminals[cellIndex] = nonTerminalOffset - offset;
            this.tmpCell = null;
            finalizeSegmentStartIndices();
        }

        /**
         * Special-case to populate a cell with a single entry without a linear search of the temporary storage
         * 
         * @param entryNonTerminal
         * @param entryInsideProbability
         * @param entryPackedChildren
         * @param entryMidpoint
         */
        public void finalizeCell(final short entryNonTerminal, final float entryInsideProbability,
                final int entryPackedChildren, final short entryMidpoint) {

            minLeftChildIndex[cellIndex] = offset;
            maxLeftChildIndex[cellIndex] = offset - 1;
            minRightChildIndex[cellIndex] = offset;
            maxRightChildIndex[cellIndex] = offset - 1;

            nonTerminalIndices[offset] = entryNonTerminal;
            insideProbabilities[offset] = entryInsideProbability;
            packedChildren[offset] = entryPackedChildren;
            midpoints[offset] = entryMidpoint;

            if (sparseMatrixGrammar.isValidLeftChild(entryNonTerminal)) {
                maxLeftChildIndex[cellIndex] = offset;
            }

            if (sparseMatrixGrammar.isValidRightChild(entryNonTerminal)) {
                maxRightChildIndex[cellIndex] = offset;
            }

            numNonTerminals[cellIndex] = 1;
            this.tmpCell = null;
            // TODO We could probably special-case this as well
            finalizeSegmentStartIndices();
        }

        /**
         * Special-case to finalize an empty cell
         */
        public void finalizeEmptyCell() {

            minLeftChildIndex[cellIndex] = offset;
            maxLeftChildIndex[cellIndex] = offset - 1;
            minRightChildIndex[cellIndex] = offset;
            maxRightChildIndex[cellIndex] = offset - 1;

            numNonTerminals[cellIndex] = 0;
            this.tmpCell = null;
            // TODO We could probably special-case this as well
            finalizeSegmentStartIndices();
        }

        private void finalizeSegmentStartIndices() {
            if (leftChildSegmentStartIndices != null) {
                // Split up the left-child non-terminals into 'segments' for multi-threading of cartesian
                // product operation.

                // The cell population is likely to be biased toward a specific range of non-terminals, but we
                // still have to use fixed segment boundaries (instead of splitting the actual population range
                // equally) so that individual x-product threads can operate on different regions of the x-product
                // vector without interfering with one another.

                // We use equal segment ranges, with the exception of POS. POS will occur only in one midpoint
                // per cell. Since many non-terms are POS (particularly in latent-variable grammars) and the threads
                // allocated to POS would be mostly idle, we include all POS in the segment containing the first segment
                // containing POS.
                final int cellSegmentStartIndex = cellIndex * (leftChildSegments + 1);
                leftChildSegmentStartIndices[cellSegmentStartIndex] = minLeftChildIndex[cellIndex];

                final int segmentSize = (sparseMatrixGrammar.numNonTerms() - sparseMatrixGrammar.numPosSymbols)
                        / leftChildSegments + 1;
                int i = 0;

                int segmentEndNT = segmentSize;
                if (segmentEndNT >= sparseMatrixGrammar.posStart && segmentEndNT <= sparseMatrixGrammar.posEnd) {
                    segmentEndNT += sparseMatrixGrammar.posEnd - sparseMatrixGrammar.posStart + 1;
                }
                for (int j = minLeftChildIndex[cellIndex]; i < (leftChildSegments - 1)
                        && j <= maxLeftChildIndex[cellIndex]; j++) {
                    while (nonTerminalIndices[j] >= segmentEndNT) {
                        i++;
                        leftChildSegmentStartIndices[cellSegmentStartIndex + i] = j;
                        segmentEndNT += segmentSize;
                        if (segmentEndNT >= sparseMatrixGrammar.posStart && segmentEndNT <= sparseMatrixGrammar.posEnd) {
                            segmentEndNT += sparseMatrixGrammar.posEnd - sparseMatrixGrammar.posStart + 1;
                        }
                    }
                }
                Arrays.fill(leftChildSegmentStartIndices, cellSegmentStartIndex + i + 1, cellSegmentStartIndex
                        + leftChildSegments + 1, maxLeftChildIndex[cellIndex] + 1);
            }
        }

        @Override
        public float getInside(final int nonTerminal) {
            if (tmpCell != null) {
                return tmpCell.insideProbabilities[nonTerminal];
            }

            final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                    (short) nonTerminal);
            if (index < 0) {
                return Float.NEGATIVE_INFINITY;
            }
            return insideProbabilities[index];
        }

        public short getMidpoint(final short nonTerminal) {
            if (tmpCell != null) {
                return tmpCell.midpoints[nonTerminal];
            }

            final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                    nonTerminal);
            if (index < 0) {
                return -1;
            }
            return midpoints[index];
        }

        @Override
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProbability) {
            // Allow update of cells created without temporary storage, even though this will be inefficient;
            // it should be rare, and is at least useful for unit testing.
            allocateTemporaryStorage();

            final int parent = p.parent;
            numEdgesConsidered++;

            if (insideProbability > tmpCell.insideProbabilities[p.parent]) {
                if (p.isBinaryProd()) {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().pack((short) p.leftChild,
                            (short) p.rightChild);
                } else if (p.isLexProd()) {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().packLexical(p.leftChild);
                } else {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().packUnary(
                            (short) p.leftChild);
                }
                tmpCell.insideProbabilities[parent] = insideProbability;

                // Midpoint == end for unary productions
                tmpCell.midpoints[parent] = leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public void updateInside(final ChartEdge edge) {

            // Allow update of cells created without temporary storage, even though this will be inefficient;
            // it should be rare, and is at least useful for unit testing.
            allocateTemporaryStorage();

            final int parent = edge.prod.parent;
            numEdgesConsidered++;

            if (edge.inside() > tmpCell.insideProbabilities[parent]) {

                if (edge.prod.isBinaryProd()) {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().pack(
                            (short) edge.prod.leftChild, (short) edge.prod.rightChild);
                } else if (edge.prod.isLexProd()) {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().packLexical(
                            edge.prod.leftChild);
                } else {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().packUnary(
                            (short) edge.prod.leftChild);
                }
                tmpCell.insideProbabilities[parent] = edge.inside();

                // Midpoint == end for unary productions
                tmpCell.midpoints[parent] = edge.leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public ChartEdge getBestEdge(final int nonTerminal) {
            int edgeChildren;
            short edgeMidpoint;

            if (tmpCell != null) {
                if (tmpCell.insideProbabilities[nonTerminal] == Float.NEGATIVE_INFINITY) {
                    return null;
                }

                edgeChildren = tmpCell.packedChildren[nonTerminal];
                edgeMidpoint = tmpCell.midpoints[nonTerminal];

            } else {
                final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                        (short) nonTerminal);
                if (index < 0) {
                    return null;
                }
                edgeChildren = packedChildren[index];
                edgeMidpoint = midpoints[index];
            }

            final int leftChild = sparseMatrixGrammar.packingFunction().unpackLeftChild(edgeChildren);
            final int rightChild = sparseMatrixGrammar.packingFunction().unpackRightChild(edgeChildren);
            final PackedArrayChartCell leftChildCell = getCell(start(), edgeMidpoint);
            final PackedArrayChartCell rightChildCell = edgeMidpoint < end ? (PackedArrayChartCell) getCell(
                    edgeMidpoint, end) : null;

            Production p;
            if (rightChild == Production.LEXICAL_PRODUCTION) {
                final float probability = sparseMatrixGrammar.lexicalLogProbability((short) nonTerminal, leftChild);
                p = new Production(nonTerminal, leftChild, probability, true, sparseMatrixGrammar);

            } else if (rightChild == Production.UNARY_PRODUCTION) {
                final float probability = sparseMatrixGrammar.unaryLogProbability((short) nonTerminal,
                        (short) leftChild);
                p = new Production(nonTerminal, leftChild, probability, false, sparseMatrixGrammar);

            } else {
                final float probability = sparseMatrixGrammar.binaryLogProbability((short) nonTerminal, edgeChildren);
                p = new Production(nonTerminal, leftChild, rightChild, probability, sparseMatrixGrammar);
            }
            return new ChartEdge(p, leftChildCell, rightChildCell);
        }

        @Override
        public int getNumNTs() {
            if (tmpCell != null) {
                int count = 0;
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
                    if (tmpCell.insideProbabilities[nt] != Float.NEGATIVE_INFINITY) {
                        count++;
                    }
                }
                return count;
            }

            return numNonTerminals[cellIndex(start, end)];
        }

        @Override
        public int getNumUnfactoredNTs() {
            if (tmpCell != null) {
                int count = 0;
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
                    if (tmpCell.insideProbabilities[nt] != Float.NEGATIVE_INFINITY
                            && !grammar.nonTermSet.isFactored(nt)) {
                        count++;
                    }
                }
                return count;
            }

            int count = 0;
            for (int i = offset; i < offset + numNonTerminals[cellIndex(start, end)]; i++) {
                if (!grammar.nonTermSet.isFactored(nonTerminalIndices[i])) {
                    count++;
                }
            }
            return count;
        }

        public int leftChildren() {
            if (tmpCell != null) {
                int count = 0;
                for (int i = 0; i < tmpCell.insideProbabilities.length; i++) {
                    if (tmpCell.insideProbabilities[i] != Float.NEGATIVE_INFINITY
                            && sparseMatrixGrammar.isValidLeftChild(i)) {
                        count++;
                    }
                }
                return count;
            }

            return maxLeftChildIndex[cellIndex(start, end)] - minLeftChildIndex[cellIndex(start, end)] + 1;
        }

        public int rightChildren() {
            if (tmpCell != null) {
                int count = 0;
                for (int i = 0; i < tmpCell.insideProbabilities.length; i++) {
                    if (tmpCell.insideProbabilities[i] != Float.NEGATIVE_INFINITY
                            && sparseMatrixGrammar.isValidRightChild(i)) {
                        count++;
                    }
                }
                return count;
            }

            return maxRightChildIndex[cellIndex(start, end)] - minRightChildIndex[cellIndex(start, end)] + 1;
        }

        /**
         * Returns the index of the first non-terminal in this cell which is valid as a left child. The grammar must be
         * sorted right, both, left, unary-only, as in {@link Grammar}.
         * 
         * @return the index of the first non-terminal in this cell which is valid as a left child.
         */
        public final int minLeftChildIndex() {
            return minLeftChildIndex[cellIndex];
        }

        /**
         * Returns the index of the last non-terminal in this cell which is valid as a left child. The grammar must be
         * sorted right, both, left, unary-only, as in {@link Grammar}.
         * 
         * @return the index of the last non-terminal in this cell which is valid as a left child.
         */
        public final int maxLeftChildIndex() {
            return maxLeftChildIndex[cellIndex];
        }

        /**
         * Returns the index of the last non-terminal in this cell which is valid as a right child. The grammar must be
         * sorted right, both, left, unary-only, as in {@link Grammar}.
         * 
         * @return the index of the last non-terminal in this cell which is valid as a right child.
         */
        public final int maxRightChildIndex() {
            return maxRightChildIndex[cellIndex];
        }

        /**
         * Warning: Not truly thread-safe, since it doesn't validate that the two cells belong to the same chart.
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof PackedArrayChartCell)) {
                return false;
            }

            final PackedArrayChartCell packedArrayChartCell = (PackedArrayChartCell) o;
            return (packedArrayChartCell.start == start && packedArrayChartCell.end == end);
        }

        @Override
        public String toString() {
            return toString(false);
        }

        @Override
        public String toString(final boolean formatFractions) {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("PackedArrayChartCell[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + sparseMatrixGrammar.numNonTerms() + ") edges\n");

            if (tmpCell == null) {
                // Format entries from the main chart array
                for (int index = offset; index < offset + numNonTerminals[cellIndex]; index++) {
                    final int childProductions = packedChildren[index];
                    final float insideProbability = insideProbabilities[index];
                    final int midpoint = midpoints[index];

                    final int nonTerminal = nonTerminalIndices[index];

                    sb.append(formatCellEntry(sparseMatrixGrammar, nonTerminal, childProductions, insideProbability,
                            midpoint, formatFractions));
                }

            } else {
                // Format entries from temporary cell storage
                for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {

                    if (tmpCell.insideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                        sb.append(formatCellEntry(sparseMatrixGrammar, nonTerminal,
                                tmpCell.packedChildren[nonTerminal], tmpCell.insideProbabilities[nonTerminal],
                                tmpCell.midpoints[nonTerminal], formatFractions));
                    }
                }
            }
            return sb.toString();
        }
    }

    public final static class TemporaryChartCell {

        public final int[] packedChildren;
        public final float[] insideProbabilities;
        public final short[] midpoints;
        private final SparseMatrixGrammar sparseMatrixGrammar;

        public TemporaryChartCell(final Grammar grammar) {
            this.packedChildren = new int[grammar.numNonTerms()];
            this.insideProbabilities = new float[grammar.numNonTerms()];
            this.midpoints = new short[grammar.numNonTerms()];
            this.sparseMatrixGrammar = grammar instanceof SparseMatrixGrammar ? (SparseMatrixGrammar) grammar : null;
            clear();
        }

        public void clear() {
            Arrays.fill(insideProbabilities, Float.NEGATIVE_INFINITY);
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(final boolean formatFractions) {
            final StringBuilder sb = new StringBuilder(128);

            for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {

                if (insideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                    sb.append(formatCellEntry(sparseMatrixGrammar, nonTerminal, packedChildren[nonTerminal],
                            insideProbabilities[nonTerminal], midpoints[nonTerminal], formatFractions));
                }
            }

            return sb.toString();
        }
    }
}
