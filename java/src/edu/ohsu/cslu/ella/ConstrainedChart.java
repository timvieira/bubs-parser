package edu.ohsu.cslu.ella;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;
import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.util.Math;

/**
 * Represents a parse chart constrained by a parent chart (e.g., for inside-outside parameter estimation constrained by
 * gold trees or (possibly) coarse-to-fine parsing).
 * 
 * @author Aaron Dunlop
 * @since Jun 2, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ConstrainedChart extends ParallelArrayChart {

    /**
     * Parallel array storing non-terminals (parallel to {@link ParallelArrayChart#insideProbabilities},
     * {@link ParallelArrayChart#packedChildren}, and {@link ParallelArrayChart#midpoints}. Entries for each cell begin
     * at indices from {@link #cellOffsets}.
     */
    public final short[] nonTerminalIndices;

    private final SplitVocabulary splitVocabulary;

    private final SymbolSet<String> lexicon;

    short[][] openCells;

    int maxUnaryChainLength;

    /**
     * Constructs a {@link ConstrainedChart}.
     * 
     * At most, a cell can contain:
     * 
     * 1 entry for each substate of the constraining non-terminal
     * 
     * k unary children of each of those states, where k is the maximum length of a unary chain in the constraining
     * chart.
     * 
     * So the maximum number of entries in a cell is maxSubstates * (1 + maxUnaryChainLength)
     * 
     * @param constrainingChart
     * @param sparseMatrixGrammar
     */
    protected ConstrainedChart(final ConstrainedChart constrainingChart, final SparseMatrixGrammar sparseMatrixGrammar,
            final int beamWidth) {
        super(constrainingChart.size, chartArraySize(constrainingChart.size, constrainingChart.maxUnaryChainLength,
                ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits), sparseMatrixGrammar);
        this.splitVocabulary = (SplitVocabulary) sparseMatrixGrammar.nonTermSet;
        this.beamWidth = lexicalRowBeamWidth = beamWidth;
        this.nonTerminalIndices = new short[chartArraySize];
        Arrays.fill(nonTerminalIndices, Short.MIN_VALUE);
        Arrays.fill(insideProbabilities, Float.NEGATIVE_INFINITY);
        this.lexicon = sparseMatrixGrammar.lexSet;
        this.openCells = constrainingChart.openCells;
        this.maxUnaryChainLength = constrainingChart.maxUnaryChainLength;

        System.arraycopy(constrainingChart.midpoints, 0, midpoints, 0, midpoints.length);

        // Calculate all cell offsets, etc
        computeOffsets();
    }

    protected ConstrainedChart(final ConstrainedChart constrainingChart, final SparseMatrixGrammar sparseMatrixGrammar) {
        this(constrainingChart, sparseMatrixGrammar, constrainingChart.beamWidth * 2);
    }

    public ConstrainedChart(final BinaryTree<String> goldTree, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(goldTree.leaves(), chartArraySize(goldTree.leaves(), goldTree.maxUnaryChainLength(),
                ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits), sparseMatrixGrammar);
        this.splitVocabulary = (SplitVocabulary) sparseMatrixGrammar.nonTermSet;
        this.lexicon = sparseMatrixGrammar.lexSet;

        this.nonTerminalIndices = new short[chartArraySize];
        Arrays.fill(nonTerminalIndices, Short.MIN_VALUE);

        this.maxUnaryChainLength = goldTree.maxUnaryChainLength() + 1;
        this.beamWidth = this.lexicalRowBeamWidth = ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits
                * maxUnaryChainLength;

        final IntArrayList tokenList = new IntArrayList();

        short start = 0;
        for (final Iterator<BinaryTree<String>> iter = goldTree.preOrderIterator(); iter.hasNext();) {
            final BinaryTree<String> node = iter.next();

            if (node.isLeaf()) {
                // Increment the start index every time we process a leaf. The lexical entry was already populated (see
                // below)
                start++;
                continue;
            }

            final int end = start + node.leaves();
            final short parent = (short) splitVocabulary.getInt(node.label());
            final int cellIndex = cellIndex(start, end);

            // Find the index of this non-terminal in the main chart array.
            // Unary children are positioned _after_ parents
            final int cellOffset = cellOffset(start, end);
            final int i = cellOffset + splitVocabulary.subcategoryIndices[parent] + splitVocabulary.maxSplits
                    * node.unaryChainDepth();

            if (node.rightChild() == null) {
                if (node.leftChild().isLeaf()) {
                    // Lexical production
                    midpoints[cellIndex] = 0;
                    final int child = lexicon.getIndex(node.leftChild().label());
                    packedChildren[i] = sparseMatrixGrammar.cartesianProductFunction.packLexical(child);

                    tokenList.add(child);
                } else {
                    // Unary production
                    final short child = (short) splitVocabulary.getIndex(node.leftChild().label());
                    // shiftCellEntriesDownward(cellOffset);
                    packedChildren[i] = sparseMatrixGrammar.cartesianProductFunction.packUnary(child);
                }
            } else {
                // Binary production
                midpoints[cellIndex] = (short) (start + node.leftChild().leaves());
                final short leftChild = (short) splitVocabulary.getIndex(node.leftChild().label());
                final short rightChild = (short) splitVocabulary.getIndex(node.rightChild().label());

                packedChildren[i] = sparseMatrixGrammar.cartesianProductFunction.pack(leftChild, rightChild);
            }
            nonTerminalIndices[i] = parent;
            insideProbabilities[i] = 0;
        }

        // Populate openCells with the start/end pairs of each populated cell. Used by {@link ConstrainedCellSelector}
        this.openCells = new short[size * 2 - 1][2];
        int i = 0;
        for (short span = 1; span <= size; span++) {
            for (short s = 0; s < size - span + 1; s++) {
                if (nonTerminalIndices[cellOffset(s, s + span)] != Short.MIN_VALUE) {
                    openCells[i][0] = s;
                    openCells[i][1] = (short) (s + span);
                    i++;
                }
            }
        }

        this.tokens = tokenList.toIntArray();

        // Calculate all cell offsets, etc
        computeOffsets();
    }

    static int chartArraySize(final int size, final int maxUnaryChainLength, final int maxSplits) {
        return (size * (size + 1) / 2) * (maxUnaryChainLength + 1) * maxSplits;
    }

    @Override
    public void clear(final int sentenceLength) {
        throw new UnsupportedOperationException();
    }

    public void clear(final ConstrainedChart constrainingChart) {
        this.size = constrainingChart.size;
        this.beamWidth = lexicalRowBeamWidth = constrainingChart.beamWidth * 2;
        Arrays.fill(
                nonTerminalIndices,
                0,
                chartArraySize(constrainingChart.size, constrainingChart.maxUnaryChainLength,
                        ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits), (short) -1);
        computeOffsets();

        this.openCells = constrainingChart.openCells;
        this.maxUnaryChainLength = constrainingChart.maxUnaryChainLength;
    }

    void shiftCellEntriesDownward(final int topEntryOffset) {
        for (int unaryDepth = maxUnaryChainLength - 2; unaryDepth >= 0; unaryDepth--) {
            final int origin = topEntryOffset + unaryDepth * splitVocabulary.maxSplits;
            final int destination = origin + splitVocabulary.maxSplits;

            nonTerminalIndices[destination] = nonTerminalIndices[origin];
            insideProbabilities[destination] = insideProbabilities[origin];
            packedChildren[destination] = packedChildren[origin];
        }
        nonTerminalIndices[topEntryOffset] = Short.MIN_VALUE;
        insideProbabilities[topEntryOffset] = Float.NEGATIVE_INFINITY;
        packedChildren[topEntryOffset] = Integer.MIN_VALUE;
    }

    private void computeOffsets() {
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end <= size; end++) {
                final int cellIndex = cellIndex(start, end);
                cellOffsets[cellIndex] = cellOffset(start, end);
            }
        }
    }

    /**
     * @param cellOffset
     * @return The unary chain depth of a cell, including the binary or lexical production. e.g. unaryChainDepth(a ->
     *         foo) = 2 and unaryChainDepth(s -> a -> a -> a b) = 3
     */
    int unaryChainDepth(final int cellOffset) {

        for (int unaryDepth = 0; unaryDepth < maxUnaryChainLength; unaryDepth++) {
            if (nonTerminalIndices[cellOffset + unaryDepth * splitVocabulary.maxSplits] < 0) {
                return unaryDepth;
            }
        }
        return maxUnaryChainLength;
    }

    @Override
    public ParseTree extractBestParse(final int start, final int end, final int parent) {
        return extractBestParse(start, end, parent, 0);
    }

    private ParseTree extractBestParse(final int start, final int end, final int parent, final int unaryDepth) {

        if (unaryDepth > maxUnaryChainLength) {
            throw new IllegalArgumentException("Max unary chain length exceeded");
        }

        final int i = cellOffset(start, end) + unaryDepth * splitVocabulary.maxSplits
                + splitVocabulary.subcategoryIndices[parent];

        final int nt = nonTerminalIndices[i];
        if (nt != parent) {
            System.out.format("Discrepancy; parent = %s (%d); NT = %s (%d)\n", splitVocabulary.getSymbol(parent),
                    parent, splitVocabulary.getSymbol(nt), nt);
        }

        final ParseTree subtree = new ParseTree(splitVocabulary.getSymbol(parent));
        final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(packedChildren[i]);
        final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(packedChildren[i]);

        if (rightChild == Production.UNARY_PRODUCTION) {
            subtree.children.add(extractBestParse(start, end, leftChild, unaryDepth + 1));

        } else if (rightChild == Production.LEXICAL_PRODUCTION) {
            subtree.addChild(new ParseTree(lexicon.getSymbol(leftChild)));

        } else {
            // binary production
            final short edgeMidpoint = midpoints[cellIndex(start, end)];
            subtree.children.add(extractBestParse(start, edgeMidpoint, leftChild, 0));
            subtree.children.add(extractBestParse(edgeMidpoint, end, rightChild, 0));
        }
        return subtree;
    }

    public void countRuleObservations(final MappedCountGrammar grammar) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int offset = cellOffset(start, end);

        // We could compute the possible indices directly, but it's a very short range to linear search, and this method
        // should only be called in testing
        for (int i = offset; i < offset + beamWidth; i++) {
            if (nonTerminalIndices[i] == nonTerminal) {
                return insideProbabilities[i];
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Populates a new {@link ConstrainedChart} with a re-merged grammar, including all merged non-terminals populated
     * in the pre-merge chart.
     * 
     * @param mergedGrammar
     * @return Chart containing merged versions of the current chart's pre-merge non-terminals.
     */
    public ConstrainedChart merge(final SparseMatrixGrammar mergedGrammar) {

        final SplitVocabulary mergedVocabulary = (SplitVocabulary) mergedGrammar.nonTermSet;
        final ConstrainedChart mergedChart = new ConstrainedChart(this, mergedGrammar, mergedVocabulary.maxSplits
                * maxUnaryChainLength);

        // Iterate over each open cell
        for (final short[] startAndEnd : openCells) {

            // Iterate over each entry in the existing chart
            for (int unaryDepth = 0; unaryDepth < maxUnaryChainLength; unaryDepth++) {

                final int cellIndex = cellIndex(startAndEnd[0], startAndEnd[1]);
                final int offset = offset(cellIndex) + splitVocabulary.maxSplits * unaryDepth;
                final int mergedOffset = mergedChart.offset(cellIndex) + mergedVocabulary.maxSplits * unaryDepth;

                // Stop when we reach an un-populated unary depth
                if (nonTerminalIndices[offset] < 0) {
                    break;
                }

                for (int i = offset; i < offset + splitVocabulary.maxSplits; i++) {

                    final short parent = nonTerminalIndices[i];
                    if (parent < 0) {
                        continue;
                    }
                    final int leftChild = sparseMatrixGrammar.cartesianProductFunction
                            .unpackLeftChild(packedChildren[i]);
                    final short rightChild = sparseMatrixGrammar.cartesianProductFunction
                            .unpackRightChild(packedChildren[i]);
                    final float probability = insideProbabilities[i];

                    // Record the parent non-terminal and sum the probabilities
                    final short mergedParent = mergedVocabulary.mergedIndices.get(parent);
                    final int mergedEntryIndex = mergedOffset + mergedVocabulary.subcategoryIndices[mergedParent];
                    mergedChart.nonTerminalIndices[mergedEntryIndex] = mergedParent;

                    final float currentMergedProbability = mergedChart.insideProbabilities[mergedEntryIndex];
                    mergedChart.insideProbabilities[mergedEntryIndex] = currentMergedProbability == Float.NEGATIVE_INFINITY ? probability
                            : Math.logSum(currentMergedProbability, probability);

                    // Record packed children
                    if (rightChild == Production.LEXICAL_PRODUCTION) {
                        // A lexical entry is the same regardless of grammar
                        mergedChart.packedChildren[mergedEntryIndex] = mergedGrammar.cartesianProductFunction
                                .packLexical(leftChild);

                    } else if (rightChild == Production.UNARY_PRODUCTION) {
                        final short mergedLeftChild = mergedVocabulary.mergedIndices.get((short) leftChild);
                        final int mergedChildren = mergedGrammar.cartesianProductFunction.packUnary(mergedLeftChild);
                        mergedChart.packedChildren[mergedEntryIndex] = mergedChildren;

                    } else {
                        // Binary Production
                        final short mergedLeftChild = mergedVocabulary.mergedIndices.get((short) leftChild);
                        final short mergedRightChild = mergedVocabulary.mergedIndices.get(rightChild);
                        final int mergedChildren = mergedGrammar.cartesianProductFunction.pack(mergedLeftChild,
                                mergedRightChild);
                        mergedChart.packedChildren[mergedEntryIndex] = mergedChildren;
                    }
                }
            }
        }
        return mergedChart;
    }

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        throw new UnsupportedOperationException("Not supported by ConstrainedChart");
    }

    // TODO Is this ever called? Can we drop the entire ConstrainedChartCell class?
    @Override
    public ConstrainedChartCell getCell(final int start, final int end) {
        return new ConstrainedChartCell(start, end);
    }

    @Override
    public String toString() {
        return extractBestParse(0).toString();
    }

    public class ConstrainedChartCell extends ParallelArrayChartCell {
        public final int cellIndex;

        public ConstrainedChartCell(final int start, final int end) {
            super(start, end);
            this.cellIndex = cellIndex(start, end);
        }

        @Override
        public float getInside(final int nonTerminal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProbability) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateInside(final ChartEdge edge) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChartEdge getBestEdge(final int nonTerminal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getNumNTs() {
            return 0;
        }

        /**
         * Warning: Not truly thread-safe, since it doesn't validate that the two cells belong to the same chart.
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof ConstrainedChartCell)) {
                return false;
            }

            final ConstrainedChartCell constrainedChartCell = (ConstrainedChartCell) o;
            return (constrainedChartCell.start == start && constrainedChartCell.end == end);
        }

        @Override
        public String toString() {
            return extractBestParse(0).toString();
        }
    }
}
