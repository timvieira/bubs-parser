package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;

/**
 * Stores a chart in a 3-way parallel array indexed by non-terminal:
 * <ol>
 * <li>Probabilities of each non-terminal (float; NEGATIVE_INFINITY for unobserved non-terminals)</li>
 * <li>Child pairs producing each non-terminal --- equivalent to the grammar rule (int)</li>
 * <li>Midpoints (short)</li>
 * </ol>
 * 
 * Those 3 pieces of information allow us to back-trace through the chart and construct the parse tree.
 * 
 * Each parallel array entry consumes 4 + 4 + 2 = 10 bytes
 * 
 * Individual cells in the parallel array are indexed by cell offsets of fixed length (the number of
 * non-terminals in the grammar).
 * 
 * The ancillary data structures are relatively small, so the total size consumed is approximately = n * (n-1)
 * / 2 * V * 10 bytes.
 * 
 * Similar to {@link PackedArrayChart}, and slightly more space-efficient, but the observed non-terminals in a
 * cell are not `packed' together at the beginning of the cell's array range. This saves a packing scan in
 * {@link DenseVectorChartCell#finalizeCell()}, but may result in less efficient access when populating
 * subsequent cells.
 * 
 * @see PackedArrayChart
 * @author Aaron Dunlop
 * @since March 15, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class DenseVectorChart extends Chart {

    public final SparseMatrixGrammar sparseMatrixGrammar;

    /**
     * Start indices for each cell. Computed from cell start and end indices and stored in the chart for
     * convenience
     */
    private final int[] cellOffsets;

    /** The number of cells in this chart */
    public final int cells;

    private final int chartArraySize;

    /**
     * Parallel arrays storing non-terminals, inside probabilities, and the grammar rules and midpoints which
     * produced them. Entries for each cell begin at indices from {@link #cellOffsets}.
     */
    public final float[] insideProbabilities;
    public final int[] packedChildren;
    public final short[] midpoints;

    /**
     * Constructs a chart
     * 
     * @param size Sentence length
     * @param sparseMatrixGrammar Grammar
     */
    public DenseVectorChart(final int size, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(size, true, null);
        this.sparseMatrixGrammar = sparseMatrixGrammar;

        cells = cellIndex(0, size) + 1;

        chartArraySize = cells * sparseMatrixGrammar.numNonTerms();
        insideProbabilities = new float[chartArraySize];
        Arrays.fill(insideProbabilities, Float.NEGATIVE_INFINITY);
        packedChildren = new int[chartArraySize];
        midpoints = new short[chartArraySize];

        // Calculate all cell offsets
        cellOffsets = new int[cells];
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end < size + 1; end++) {
                final int cellIndex = cellIndex(start, end);
                cellOffsets[cellIndex] = cellIndex * sparseMatrixGrammar.numNonTerms();
            }
        }
    }

    public void clear(final int sentenceLength) {
        this.size = sentenceLength;
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int cellIndex = cellIndex(start, end);
        final int offset = cellIndex * sparseMatrixGrammar.numNonTerms();
        return insideProbabilities[offset + nonTerminal];
    }

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        throw new UnsupportedOperationException("Not supported by PackedArrayChart");
    }

    @Override
    public DenseVectorChartCell getCell(final int start, final int end) {
        return new DenseVectorChartCell(start, end);
    }

    /**
     * Returns the index of the specified cell in the parallel chart arrays
     * 
     * @param start
     * @param end
     * @return the index of the specified cell in the parallel chart arrays
     */
    public final int cellIndex(final int start, final int end) {

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

    public final int offset(final int cellIndex) {
        return cellOffsets[cellIndex];
    }

    public final int chartArraySize() {
        return chartArraySize;
    }

    public class DenseVectorChartCell extends ChartCell {

        private final int cellIndex;
        private final int offset;

        public DenseVectorChartCell(final int start, final int end) {
            super(start, end);

            cellIndex = cellIndex(start, end);
            offset = cellIndex * sparseMatrixGrammar.numNonTerms();
            if (offset < 0) {
                System.out.println("Found negative offset");
            }
        }

        // @Override
        // public void finalizeCell() {
        //
        // // Copy and pack entries from temporary array into the main chart array
        // if (tmpChildren == null) {
        // return;
        // }
        //
        // int nonTerminalOffset = offset;
        // int tmpMinLeftChildIndex = Integer.MAX_VALUE;
        // int tmpMaxLeftChildIndex = offset - 1;
        // int tmpMaxRightChildIndex = offset - 1;
        //
        // for (short nonTerminal = 0; nonTerminal < tmpInsideProbabilities.length; nonTerminal++) {
        //
        // if (tmpInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
        //
        // nonTerminalIndices[nonTerminalOffset] = nonTerminal;
        // insideProbabilities[nonTerminalOffset] = tmpInsideProbabilities[nonTerminal];
        // packedChildren[nonTerminalOffset] = tmpChildren[nonTerminal];
        // midpoints[nonTerminalOffset] = tmpMidpoints[nonTerminal];
        //
        // if (sparseMatrixGrammar.isValidLeftChild(nonTerminal)) {
        // if (tmpMinLeftChildIndex == Integer.MAX_VALUE) {
        // tmpMinLeftChildIndex = nonTerminalOffset;
        // }
        // tmpMaxLeftChildIndex = nonTerminalOffset;
        // }
        //
        // if (sparseMatrixGrammar.isValidRightChild(nonTerminal)) {
        // tmpMaxRightChildIndex = nonTerminalOffset;
        // }
        //
        // nonTerminalOffset++;
        // }
        // }
        //
        // numNonTerminals[cellIndex] = nonTerminalOffset - offset;
        // minLeftChildIndex[cellIndex] = tmpMinLeftChildIndex;
        // maxLeftChildIndex[cellIndex] = tmpMaxLeftChildIndex;
        // maxRightChildIndex[cellIndex] = tmpMaxRightChildIndex;
        //
        // temporaryCells[start][end] = null;
        // }

        @Override
        public float getInside(final int nonTerminal) {
            return insideProbabilities[offset + nonTerminal];
        }

        @Override
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProbability) {

            final int index = offset + p.parent;
            numEdgesConsidered++;

            if (insideProbability > insideProbabilities[index]) {
                if (p.isBinaryProd()) {
                    packedChildren[index] = sparseMatrixGrammar.cartesianProductFunction().pack(p.leftChild,
                        p.rightChild);
                } else if (p.isLexProd()) {
                    packedChildren[index] = sparseMatrixGrammar.cartesianProductFunction().packLexical(
                        p.leftChild);
                } else {
                    packedChildren[index] = sparseMatrixGrammar.cartesianProductFunction().packUnary(
                        p.leftChild);
                }
                insideProbabilities[index] = insideProbability;

                // Midpoint == end for unary productions
                midpoints[index] = (short) leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public void updateInside(final ChartEdge edge) {

            final int index = offset + edge.prod.parent;
            numEdgesConsidered++;

            if (edge.inside() > insideProbabilities[index]) {

                if (edge.prod.isBinaryProd()) {
                    packedChildren[index] = sparseMatrixGrammar.cartesianProductFunction().pack(
                        edge.prod.leftChild, edge.prod.rightChild);
                } else if (edge.prod.isLexProd()) {
                    packedChildren[index] = sparseMatrixGrammar.cartesianProductFunction().packLexical(
                        edge.prod.leftChild);
                } else {
                    packedChildren[index] = sparseMatrixGrammar.cartesianProductFunction().packUnary(
                        edge.prod.leftChild);
                }
                insideProbabilities[index] = edge.inside();

                // Midpoint == end for unary productions
                midpoints[index] = (short) edge.leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public ChartEdge getBestEdge(final int nonTerminal) {
            int edgeChildren;
            short edgeMidpoint;

            final int index = offset + nonTerminal;
            edgeChildren = packedChildren[index];
            edgeMidpoint = midpoints[index];

            final int leftChild = sparseMatrixGrammar.cartesianProductFunction()
                .unpackLeftChild(edgeChildren);
            final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(
                edgeChildren);
            final DenseVectorChartCell leftChildCell = getCell(start(), edgeMidpoint);
            final DenseVectorChartCell rightChildCell = edgeMidpoint < end ? (DenseVectorChartCell) getCell(
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
            int numNTs = 0;
            for (int i = offset; i < (offset + sparseMatrixGrammar.numNonTerms()); i++) {
                if (insideProbabilities[i] != Float.NEGATIVE_INFINITY) {
                    numNTs++;
                }
            }
            return numNTs;
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
         * Warning: Not truly thread-safe, since it doesn't validate that the two cells belong to the same
         * chart.
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof DenseVectorChartCell)) {
                return false;
            }

            final DenseVectorChartCell packedArrayChartCell = (DenseVectorChartCell) o;
            return (packedArrayChartCell.start == start && packedArrayChartCell.end == end);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("DenseVectorChartCell[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + sparseMatrixGrammar.numNonTerms() + ") edges\n");

            // Format entries from the main chart array
            for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {
                final int index = offset + nonTerminal;
                final float insideProbability = insideProbabilities[index];

                if (insideProbability > Float.NEGATIVE_INFINITY) {
                    final int childProductions = packedChildren[index];
                    final int midpoint = midpoints[index];
                    sb.append(formatCellEntry(index - offset, childProductions, insideProbability, midpoint));
                }
            }
            return sb.toString();
        }

        private String formatCellEntry(final int nonterminal, final int childProductions,
                final float insideProbability, final int midpoint) {
            final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(
                childProductions);
            final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(
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
