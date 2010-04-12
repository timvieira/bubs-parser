package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.SortedGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;

public class PackedArrayChart extends Chart {

    public final SparseMatrixGrammar sparseMatrixGrammar;

    /**
     * Start indices for each cell. Computed from cell start and end indices and stored in the chart for
     * convenience
     */
    private final int[] cellOffsets;

    /**
     * The number of non-terminals populated in each cell. Indexed by cell index ({@link #cellIndex(int, int)}
     * ).
     */
    private final int[] numNonTerminals;

    /**
     * The index in the main chart array of the last non-terminal in each cell which is valid as a right
     * child. Indexed by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] maxRightChildIndex;

    /**
     * The index in the main chart array of the last non-terminal in each cell which is valid as a left child.
     * Indexed by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] maxLeftChildIndex;

    /**
     * The index in the main chart array of the first non-terminal in each cell which is valid as a left
     * child. Indexed by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] minLeftChildIndex;

    /** The number of cells in this chart */
    public final int cells;

    /**
     * Stores packed non-terminals and their inside probabilities. Entries for each cell begin at indices from
     * {@link #cellOffsets}.
     */
    public final int[] nonTerminalIndices;
    public final float[] insideProbabilities;
    public final int[] children;
    public final short[] midpoints;

    public final PackedArrayChartCell[][] temporaryCells;

    public PackedArrayChart(final int size, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(size, true, null);
        this.sparseMatrixGrammar = sparseMatrixGrammar;

        cells = cellIndex(0, size) + 1;
        numNonTerminals = new int[cells];
        minLeftChildIndex = new int[cells];
        maxLeftChildIndex = new int[cells];
        maxRightChildIndex = new int[cells];

        final int packedArraySize = cells * sparseMatrixGrammar.numNonTerms();
        nonTerminalIndices = new int[packedArraySize];
        insideProbabilities = new float[packedArraySize];
        children = new int[packedArraySize];
        midpoints = new short[packedArraySize];

        temporaryCells = new PackedArrayChartCell[size][size + 1];

        // Calculate all cell offsets
        cellOffsets = new int[cells];
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end < size + 1; end++) {
                final int cellIndex = cellIndex(start, end);
                cellOffsets[cellIndex] = cellIndex * sparseMatrixGrammar.numNonTerms();
            }
        }
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int cellIndex = cellIndex(start, end);
        final int offset = cellIndex * sparseMatrixGrammar.numNonTerms();
        final int index = Arrays.binarySearch(nonTerminalIndices, offset,
            offset + numNonTerminals[cellIndex], nonTerminal);
        if (index < 0) {
            return Float.NEGATIVE_INFINITY;
        }
        return insideProbabilities[index];
    }

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        // TODO Auto-generated method stub

    }

    @Override
    public PackedArrayChartCell getCell(final int start, final int end) {
        if (temporaryCells[start][end] != null) {
            return temporaryCells[start][end];
        }

        return new PackedArrayChartCell(start, end);
    }

    /**
     * Returns the index of the specified cell in the parallel chart arrays
     * 
     * @param start
     * @param end
     * @return the index of the specified cell in the parallel chart arrays
     */
    int cellIndex(final int start, final int end) {

        if (start < 0 || start > size) {
            throw new IllegalArgumentException("Illegal start: " + start);
        }

        if (end < 0 || end > size) {
            throw new IllegalArgumentException("Illegal end: " + end);
        }

        final int row = end - start - 1;
        return size * row - ((row - 1) * row / 2) + start;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end < size + 1; end++) {
                sb.append(getCell(start, end).toString());
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    public class PackedArrayChartCell extends ChartCell {

        private final int cellIndex;
        private final int offset;

        /**
         * Temporary storage for manipulating cell entries. Indexed by parent non-terminal. Only allocated
         * when the cell is being modified
         */
        public int[] tmpChildren;
        public float[] tmpInsideProbabilities;
        public short[] tmpMidpoints;

        public PackedArrayChartCell(final int start, final int end) {
            super(start, end);

            cellIndex = cellIndex(start, end);
            offset = cellIndex * sparseMatrixGrammar.numNonTerms();
        }

        public void allocateTemporaryStorage() {
            temporaryCells[start][end] = this;

            // Allocate storage
            if (tmpChildren == null) {
                final int arraySize = sparseMatrixGrammar.numNonTerms();

                this.tmpChildren = new int[arraySize];
                this.tmpInsideProbabilities = new float[arraySize];
                Arrays.fill(tmpInsideProbabilities, Float.NEGATIVE_INFINITY);
                this.tmpMidpoints = new short[arraySize];

                // Copy from main chart array to temporary parallel array
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                    final int nonTerminal = nonTerminalIndices[i];
                    tmpChildren[nonTerminal] = children[i];
                    tmpInsideProbabilities[nonTerminal] = insideProbabilities[i];
                    tmpMidpoints[nonTerminal] = midpoints[i];
                }
            }
        }

        @Override
        public void finalizeCell() {

            // Copy and pack entries from temporary array into the main chart array
            if (tmpChildren == null) {
                return;
            }

            int nonTerminalOffset = offset;
            int tmpMinLeftChildIndex = Integer.MAX_VALUE;
            int tmpMaxLeftChildIndex = offset - 1;
            int tmpMaxRightChildIndex = offset - 1;

            for (int nonTerminal = 0; nonTerminal < tmpInsideProbabilities.length; nonTerminal++) {

                if (tmpInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {

                    nonTerminalIndices[nonTerminalOffset] = nonTerminal;
                    insideProbabilities[nonTerminalOffset] = tmpInsideProbabilities[nonTerminal];
                    children[nonTerminalOffset] = tmpChildren[nonTerminal];
                    midpoints[nonTerminalOffset] = tmpMidpoints[nonTerminal];

                    if (sparseMatrixGrammar.isValidLeftChild(nonTerminal)) {
                        if (tmpMinLeftChildIndex == Integer.MAX_VALUE) {
                            tmpMinLeftChildIndex = nonTerminalOffset;
                        }
                        tmpMaxLeftChildIndex = nonTerminalOffset;
                    }

                    if (sparseMatrixGrammar.isValidRightChild(nonTerminal)) {
                        tmpMaxRightChildIndex = nonTerminalOffset;
                    }

                    nonTerminalOffset++;
                }
            }

            numNonTerminals[cellIndex] = nonTerminalOffset - offset;
            minLeftChildIndex[cellIndex] = tmpMinLeftChildIndex;
            maxLeftChildIndex[cellIndex] = tmpMaxLeftChildIndex;
            maxRightChildIndex[cellIndex] = tmpMaxRightChildIndex;

            temporaryCells[start][end] = null;
        }

        @Override
        public float getInside(final int nonTerminal) {
            if (tmpChildren != null) {
                return tmpInsideProbabilities[nonTerminal];
            }

            // TODO Use getBestEdge() ?
            final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset
                    + numNonTerminals[cellIndex], nonTerminal);
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
                tmpChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().pack(p.leftChild,
                    (short) p.rightChild);
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

                tmpChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().pack(
                    edge.prod.leftChild, (short) edge.prod.rightChild);
                tmpInsideProbabilities[parent] = edge.inside();

                // Midpoint == end for unary productions
                tmpMidpoints[parent] = (short) edge.leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public ChartEdge getBestEdge(final int nonTermIndex) {
            int edgeChildren;
            short edgeMidpoint;

            if (tmpChildren != null) {
                if (tmpInsideProbabilities[nonTermIndex] == Float.NEGATIVE_INFINITY) {
                    return null;
                }

                edgeChildren = tmpChildren[nonTermIndex];
                edgeMidpoint = tmpMidpoints[nonTermIndex];

            } else {
                final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset
                        + numNonTerminals[cellIndex], nonTermIndex);
                if (index < 0) {
                    return null;
                }
                edgeChildren = children[index];
                edgeMidpoint = midpoints[index];
            }

            final int leftChild = sparseMatrixGrammar.cartesianProductFunction()
                .unpackLeftChild(edgeChildren);
            final short rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(
                edgeChildren);
            final PackedArrayChartCell leftChildCell = getCell(start(), edgeMidpoint);
            final PackedArrayChartCell rightChildCell = edgeMidpoint < end ? (PackedArrayChartCell) getCell(
                edgeMidpoint, end) : null;

            Production p;
            if (rightChild == Production.LEXICAL_PRODUCTION) {
                final float probability = sparseMatrixGrammar.lexicalLogProbability(nonTermIndex, leftChild);
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, probability, true);

            } else if (rightChild == Production.UNARY_PRODUCTION) {
                final float probability = sparseMatrixGrammar.unaryLogProbability(nonTermIndex, leftChild);
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, probability, false);

            } else {
                final float probability = sparseMatrixGrammar
                    .binaryLogProbability(nonTermIndex, edgeChildren);
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, rightChild, probability);
            }
            return new ChartEdge(p, leftChildCell, rightChildCell);
        }

        @Override
        public int getNumNTs() {
            if (tmpChildren != null) {
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

        /**
         * Returns the start index of this cell in the main packed array
         * 
         * @return the start index of this cell in the main packed array
         */
        public final int offset() {
            return offset;
        }

        /**
         * Returns the index of the first non-terminal in this cell which is valid as a left child. The
         * grammar must be sorted right, both, left, unary-only, as in {@link SortedGrammar}.
         * 
         * @return the index of the first non-terminal in this cell which is valid as a left child.
         */
        public final int minLeftChildIndex() {
            return minLeftChildIndex[cellIndex];
        }

        /**
         * Returns the index of the last non-terminal in this cell which is valid as a left child. The grammar
         * must be sorted right, both, left, unary-only, as in {@link SortedGrammar}.
         * 
         * @return the index of the last non-terminal in this cell which is valid as a left child.
         */
        public final int maxLeftChildIndex() {
            return maxLeftChildIndex[cellIndex];
        }

        /**
         * Returns the index of the last non-terminal in this cell which is valid as a right child. The
         * grammar must be sorted right, both, left, unary-only, as in {@link SortedGrammar}.
         * 
         * @return the index of the last non-terminal in this cell which is valid as a right child.
         */
        public final int maxRightChildIndex() {
            return maxRightChildIndex[cellIndex];
        }

        /**
         * Warning: Not truly thread-safe, since it doesn't validate that the two cells belong to the same
         * chart.
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

            if (tmpChildren == null) {
                // Format entries from the main chart array
                for (int index = offset; index < offset + numNonTerminals[cellIndex]; index++) {
                    final int childProductions = children[index];
                    final float insideProbability = insideProbabilities[index];
                    final int midpoint = midpoints[index];

                    final int nonTerminal = nonTerminalIndices[index];

                    sb.append(formatCellEntry(nonTerminal, childProductions, insideProbability, midpoint));
                }
            } else {

                // Format entries from temporary cell storage
                for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {

                    if (tmpInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                        final int childProductions = tmpChildren[nonTerminal];
                        final float insideProbability = tmpInsideProbabilities[nonTerminal];
                        final int midpoint = tmpMidpoints[nonTerminal];

                        sb
                            .append(formatCellEntry(nonTerminal, childProductions, insideProbability,
                                midpoint));
                    }
                }

            }
            return sb.toString();
        }

        private String formatCellEntry(final int nonterminal, final int childProductions,
                final float insideProbability, final int midpoint) {
            final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(
                childProductions);
            final short rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(
                childProductions);

            if (rightChild == Production.UNARY_PRODUCTION) {
                // Unary Production
                return String.format("%s -> %s (%.5f, %d)\n",
                    sparseMatrixGrammar.mapNonterminal(nonterminal), sparseMatrixGrammar
                        .mapNonterminal(leftChild), insideProbability, midpoint);
            } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                // Lexical Production
                return String.format("%s -> %s (%.5f, %d)\n",
                    sparseMatrixGrammar.mapNonterminal(nonterminal), sparseMatrixGrammar
                        .mapLexicalEntry(leftChild), insideProbability, midpoint);
            } else {
                return String.format("%s -> %s %s (%.5f, %d)\n", sparseMatrixGrammar
                    .mapNonterminal(nonterminal), sparseMatrixGrammar.mapNonterminal(leftChild),
                    sparseMatrixGrammar.mapNonterminal(rightChild), insideProbability, midpoint);
            }
        }
    }
}
