package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.ParseTree;

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
 * 
 * @version $Revision$ $Date$ $Author$
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
    private final int[] numNonTerminals;

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

    // TODO Remove this large array and share a single temporary cell. Should avoid some object creation and GC
    public final PackedArrayChartCell[][] temporaryCells;

    private final EdgeSelector edgeSelector;

    /**
     * Constructs a chart
     * 
     * @param tokens Sentence tokens, mapped to integer indices
     * @param sparseMatrixGrammar Grammar
     * @param beamWidth
     */
    public PackedArrayChart(final int[] tokens, final SparseMatrixGrammar sparseMatrixGrammar, final int beamWidth,
            final EdgeSelector edgeSelector) {
        super(tokens, sparseMatrixGrammar, Math.min(beamWidth, sparseMatrixGrammar.numNonTerms()));

        numNonTerminals = new int[cells];
        minLeftChildIndex = new int[cells];
        maxLeftChildIndex = new int[cells];
        minRightChildIndex = new int[cells];
        maxRightChildIndex = new int[cells];

        nonTerminalIndices = new short[chartArraySize];

        temporaryCells = new PackedArrayChartCell[size][size + 1];
        this.edgeSelector = edgeSelector;
    }

    /**
     * Constructs a chart
     * 
     * @param tokens Sentence tokens, mapped to integer indices
     * @param sparseMatrixGrammar Grammar
     */
    public PackedArrayChart(final int[] tokens, final SparseMatrixGrammar sparseMatrixGrammar) {
        this(tokens, sparseMatrixGrammar, sparseMatrixGrammar.numNonTerms(), null);
    }

    @Override
    public void clear(final int sentenceLength) {
        this.size = sentenceLength;
        Arrays.fill(numNonTerminals, 0, cellIndex(0, sentenceLength) + 1, 0);
    }

    @Override
    public ParseTree extractBestParse(final int start, final int end, final int parent) {
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

        final ParseTree subtree = new ParseTree(sparseMatrixGrammar.nonTermSet.getSymbol(parent));
        final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(edgeChildren);
        final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(edgeChildren);

        if (rightChild == Production.UNARY_PRODUCTION) {
            subtree.children.add(extractBestParse(start, end, leftChild));

        } else if (rightChild == Production.LEXICAL_PRODUCTION) {
            subtree.addChild(new ParseTree(sparseMatrixGrammar.lexSet.getSymbol(leftChild)));

        } else {
            // binary production
            subtree.children.add(extractBestParse(start, edgeMidpoint, leftChild));
            subtree.children.add(extractBestParse(edgeMidpoint, end, rightChild));
        }
        return subtree;
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int cellIndex = cellIndex(start, end);
        final int offset = cellIndex * beamWidth;
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
        if (temporaryCells[start][end] != null) {
            return temporaryCells[start][end];
        }

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

    public class PackedArrayChartCell extends ParallelArrayChartCell {

        /**
         * Temporary storage for manipulating cell entries. Indexed by parent non-terminal. Only allocated when the cell
         * is being modified
         */
        public int[] tmpPackedChildren;
        public float[] tmpInsideProbabilities;
        public float[] tmpFom;
        public short[] tmpMidpoints;

        public PackedArrayChartCell(final int start, final int end) {
            super(start, end);
        }

        public void allocateTemporaryStorage() {
            temporaryCells[start][end] = this;

            // Allocate storage
            if (tmpPackedChildren == null) {
                final int arraySize = sparseMatrixGrammar.numNonTerms();

                this.tmpPackedChildren = new int[arraySize];
                this.tmpInsideProbabilities = new float[arraySize];
                Arrays.fill(tmpInsideProbabilities, Float.NEGATIVE_INFINITY);
                this.tmpMidpoints = new short[arraySize];

                // Copy from main chart array to temporary parallel array
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                    final int nonTerminal = nonTerminalIndices[i];
                    tmpPackedChildren[nonTerminal] = packedChildren[i];
                    tmpInsideProbabilities[nonTerminal] = insideProbabilities[i];
                    tmpMidpoints[nonTerminal] = midpoints[i];
                }
            }
        }

        public void clearTemporaryStorage() {
            tmpPackedChildren = null;
            tmpInsideProbabilities = null;
            tmpMidpoints = null;
        }

        /**
         * Copy and pack entries from temporary array into the main chart array
         */
        @Override
        public void finalizeCell() {

            if (tmpPackedChildren == null) {
                return;
            }

            if (beamWidth == tmpInsideProbabilities.length) {
                // Copy all populated entries from temporary storage
                boolean foundMinLeftChild = false, foundMinRightChild = false;
                int nonTerminalOffset = offset;

                minLeftChildIndex[cellIndex] = offset;
                maxLeftChildIndex[cellIndex] = offset - 1;
                minRightChildIndex[cellIndex] = offset;
                maxRightChildIndex[cellIndex] = offset - 1;

                for (short nonTerminal = 0; nonTerminal < tmpInsideProbabilities.length; nonTerminal++) {

                    if (tmpInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {

                        nonTerminalIndices[nonTerminalOffset] = nonTerminal;
                        insideProbabilities[nonTerminalOffset] = tmpInsideProbabilities[nonTerminal];
                        packedChildren[nonTerminalOffset] = tmpPackedChildren[nonTerminal];
                        midpoints[nonTerminalOffset] = tmpMidpoints[nonTerminal];

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

            } else {
                final boolean lexical = (end == start + 1);

                // If maxEntriesPerCell < |V|, we're performing pruned search; pack only the most probable entries,
                // still in numerical order so we can iterate over them normally in subsequent cells.
                final BoundedPriorityQueue q = new BoundedPriorityQueue(beamWidth);
                for (short nt = 0; nt < tmpInsideProbabilities.length; nt++) {

                    final float insideProbability = tmpInsideProbabilities[nt];
                    if (insideProbability != Float.NEGATIVE_INFINITY) {
                        if (edgeSelector != null) {
                            if (lexical) {
                                q.insert(nt, edgeSelector.calcLexicalFOM(start, end, nt, insideProbability));
                            } else {
                                q.insert(nt, edgeSelector.calcFOM(start, end, nt, insideProbability));
                            }
                        } else {
                            q.insert(nt, insideProbability);
                        }
                    }
                }

                // Force including the start symbol in the top cell, even if it would not meet the FOM threshold
                if (start == 0 && end == size) {
                    if (edgeSelector != null) {
                        q.forceInsert((short) sparseMatrixGrammar.startSymbol, edgeSelector.calcFOM(start, end,
                                (short) sparseMatrixGrammar.startSymbol,
                                tmpInsideProbabilities[sparseMatrixGrammar.startSymbol]));
                    } else {
                        q.forceInsert((short) sparseMatrixGrammar.startSymbol,
                                tmpInsideProbabilities[sparseMatrixGrammar.startSymbol]);
                    }
                }

                q.sortByNonterminalIndex();

                // Copy all populated entries from temporary storage. Copied nearly verbatim from non-pruned version
                // above
                boolean foundMinLeftChild = false, foundMinRightChild = false;
                int nonTerminalOffset = offset;

                minLeftChildIndex[cellIndex] = offset;
                maxLeftChildIndex[cellIndex] = offset - 1;
                minRightChildIndex[cellIndex] = offset;
                maxRightChildIndex[cellIndex] = offset - 1;

                for (int i = 0; i < q.size; i++) {

                    final short nonTerminal = q.queueParentIndices[i];
                    if (nonTerminal == Short.MAX_VALUE) {
                        break;
                    }

                    if (tmpInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {

                        nonTerminalIndices[nonTerminalOffset] = nonTerminal;
                        // TODO If we want to accumulate FOM scores instead of inside probabilities in the chart, switch
                        // to using scores from the queue here
                        insideProbabilities[nonTerminalOffset] = tmpInsideProbabilities[nonTerminal];
                        packedChildren[nonTerminalOffset] = tmpPackedChildren[nonTerminal];
                        midpoints[nonTerminalOffset] = tmpMidpoints[nonTerminal];

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
            }

            clearTemporaryStorage();
            temporaryCells[start][end] = null;
        }

        @Override
        public float getInside(final int nonTerminal) {
            if (tmpPackedChildren != null) {
                return tmpInsideProbabilities[nonTerminal];
            }

            // TODO Use getBestEdge() ?
            final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                    (short) nonTerminal);
            if (index < 0) {
                return Float.NEGATIVE_INFINITY;
            }
            return insideProbabilities[index];
        }

        @Override
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProbability) {
            // Allow update of cells created without temporary storage, even though this will be inefficient;
            // it should be rare, and is at least useful for unit testing.
            allocateTemporaryStorage();

            final int parent = p.parent;
            numEdgesConsidered++;

            if (insideProbability > tmpInsideProbabilities[p.parent]) {
                if (p.isBinaryProd()) {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().pack(p.leftChild,
                            p.rightChild);
                } else if (p.isLexProd()) {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().packLexical(p.leftChild);
                } else {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().packUnary(p.leftChild);
                }
                tmpInsideProbabilities[parent] = insideProbability;

                // Midpoint == end for unary productions
                tmpMidpoints[parent] = (short) leftCell.end();

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

            if (edge.inside() > tmpInsideProbabilities[parent]) {

                if (edge.prod.isBinaryProd()) {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().pack(
                            edge.prod.leftChild, edge.prod.rightChild);
                } else if (edge.prod.isLexProd()) {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().packLexical(
                            edge.prod.leftChild);
                } else {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().packUnary(
                            edge.prod.leftChild);
                }
                tmpInsideProbabilities[parent] = edge.inside();

                // Midpoint == end for unary productions
                tmpMidpoints[parent] = (short) edge.leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public ChartEdge getBestEdge(final int nonTerminal) {
            int edgeChildren;
            short edgeMidpoint;

            if (tmpPackedChildren != null) {
                if (tmpInsideProbabilities[nonTerminal] == Float.NEGATIVE_INFINITY) {
                    return null;
                }

                edgeChildren = tmpPackedChildren[nonTerminal];
                edgeMidpoint = tmpMidpoints[nonTerminal];

            } else {
                final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                        (short) nonTerminal);
                if (index < 0) {
                    return null;
                }
                edgeChildren = packedChildren[index];
                edgeMidpoint = midpoints[index];
            }

            final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(edgeChildren);
            final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(edgeChildren);
            final PackedArrayChartCell leftChildCell = getCell(start(), edgeMidpoint);
            final PackedArrayChartCell rightChildCell = edgeMidpoint < end ? (PackedArrayChartCell) getCell(
                    edgeMidpoint, end) : null;

            Production p;
            if (rightChild == Production.LEXICAL_PRODUCTION) {
                final float probability = sparseMatrixGrammar.lexicalLogProbability(nonTerminal, leftChild);
                p = sparseMatrixGrammar.new Production(nonTerminal, leftChild, probability, true);

            } else if (rightChild == Production.UNARY_PRODUCTION) {
                final float probability = sparseMatrixGrammar.unaryLogProbability(nonTerminal, leftChild);
                p = sparseMatrixGrammar.new Production(nonTerminal, leftChild, probability, false);

            } else {
                final float probability = sparseMatrixGrammar.binaryLogProbability(nonTerminal, edgeChildren);
                p = sparseMatrixGrammar.new Production(nonTerminal, leftChild, rightChild, probability);
            }
            return new ChartEdge(p, leftChildCell, rightChildCell);
        }

        @Override
        public int getNumNTs() {
            if (tmpPackedChildren != null) {
                int numNTs = 0;
                for (int i = 0; i < tmpInsideProbabilities.length; i++) {
                    if (tmpInsideProbabilities[i] != Float.NEGATIVE_INFINITY) {
                        numNTs++;
                    }
                }
                return numNTs;
            }

            return numNonTerminals[cellIndex(start, end)];
        }

        public int leftChildren() {
            if (tmpPackedChildren != null) {
                int count = 0;
                for (int i = 0; i < tmpInsideProbabilities.length; i++) {
                    if (tmpInsideProbabilities[i] != Float.NEGATIVE_INFINITY && sparseMatrixGrammar.isValidLeftChild(i)) {
                        count++;
                    }
                }
                return count;
            }

            return maxLeftChildIndex[cellIndex(start, end)] - minLeftChildIndex[cellIndex(start, end)] + 1;
        }

        public int rightChildren() {
            if (tmpPackedChildren != null) {
                int count = 0;
                for (int i = 0; i < tmpInsideProbabilities.length; i++) {
                    if (tmpInsideProbabilities[i] != Float.NEGATIVE_INFINITY
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
            final StringBuilder sb = new StringBuilder(256);

            sb.append("PackedArrayChartCell[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + sparseMatrixGrammar.numNonTerms() + ") edges\n");

            if (tmpPackedChildren == null) {
                // Format entries from the main chart array
                for (int index = offset; index < offset + numNonTerminals[cellIndex]; index++) {
                    final int childProductions = packedChildren[index];
                    final float insideProbability = insideProbabilities[index];
                    final int midpoint = midpoints[index];

                    final int nonTerminal = nonTerminalIndices[index];

                    sb.append(formatCellEntry(nonTerminal, childProductions, insideProbability, midpoint));
                }
            } else {

                // Format entries from temporary cell storage
                for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {

                    if (tmpInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                        final int childProductions = tmpPackedChildren[nonTerminal];
                        final float insideProbability = tmpInsideProbabilities[nonTerminal];
                        final int midpoint = tmpMidpoints[nonTerminal];

                        sb.append(formatCellEntry(nonTerminal, childProductions, insideProbability, midpoint));
                    }
                }

            }
            return sb.toString();
        }
    }

    /**
     * Stores a bounded heap of cell entries, ordered by a figure-of-merit. This could be stored directly in the main
     * chart parallel array, but it's easier to test in separate storage during development.
     * 
     * @author Aaron Dunlop
     * @since Sep 10, 2010
     * 
     * @version $Revision$ $Date$ $Author$
     */
    public static class BoundedPriorityQueue {

        /**
         * Parallel array storing a bounded cell population (parents, probabilities, packed children, and midpoints).
         * Analagous to {@link ParallelArrayChart#insideProbabilities}, {@link ParallelArrayChart#packedChildren},
         * {@link ParallelArrayChart#midpoints}, and {@link #nonTerminalIndices}. The most probable entry should be
         * stored in index 0.
         */
        final short[] queueParentIndices;
        final float[] queueFom;

        private int size = 0;

        public BoundedPriorityQueue(final int maxSize) {
            queueFom = new float[maxSize];
            Arrays.fill(queueFom, Float.NEGATIVE_INFINITY);
            queueParentIndices = new short[maxSize];
        }

        /**
         * Inserts an entry in the priority queue, if its figure-of-merit meets the current threshold (ejecting the
         * lowest item in the queue if the size bound is exceeded).
         * 
         * @param parentIndex
         * @param fom
         */
        public void insert(final short parentIndex, final float fom) {

            if (size == queueFom.length) {
                // Ignore entries which are less probable than the minimum-priority entry
                if (fom <= queueFom[size - 1]) {
                    return;
                }
            } else {
                size++;
            }

            int i = size - 1;

            queueFom[i] = fom;
            queueParentIndices[i] = parentIndex;

            while (i > 0 && queueFom[i] > queueFom[i - 1]) {
                swap(i, i - 1);
                i--;
            }
        }

        /**
         * Replaces the least-probable entry in the queue, regardless of the relative probability of the new entry. Used
         * to ensure that the top chart cell contains the start symbol.
         * 
         * @param parentIndex
         * @param fom
         */
        public void forceInsert(final short parentIndex, final float fom) {

            // First, linear-search for an existing entry
            for (int i = 0; i < size; i++) {
                if (queueParentIndices[i] == parentIndex) {
                    queueFom[i] = fom;
                    queueParentIndices[i] = parentIndex;

                    return;
                }
            }

            if (size != queueFom.length) {
                size++;
            }

            int i = size - 1;

            queueFom[i] = fom;
            queueParentIndices[i] = parentIndex;

            while (i > 0 && queueFom[i] > queueFom[i - 1]) {
                swap(i, i - 1);
                i--;
            }
        }

        private void swap(final int i1, final int i2) {
            final float t1 = queueFom[i1];
            queueFom[i1] = queueFom[i2];
            queueFom[i2] = t1;

            final short t2 = queueParentIndices[i1];
            queueParentIndices[i1] = queueParentIndices[i2];
            queueParentIndices[i2] = t2;
        }

        /**
         * Sorts parallel array storage by non-terminal index. Note that this invalidates the heap assumption.
         */
        public void sortByNonterminalIndex() {
            // Ensure that any un-populated entries sort to the end of the array
            for (int i = 0; i < queueFom.length; i++) {
                if (queueFom[i] == Float.NEGATIVE_INFINITY) {
                    queueParentIndices[i] = Short.MAX_VALUE;
                }
            }
            edu.ohsu.cslu.util.Arrays.sort(queueParentIndices, queueFom);
        }

        public int size() {
            return size;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(1024);
            for (int i = 0; i < queueFom.length; i++) {
                sb.append(String.format("%d %.3f\n", queueParentIndices[i], queueFom[i]));
            }
            return sb.toString();
        }
    }
}
