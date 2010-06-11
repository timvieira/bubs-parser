package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;

/**
 * Represents a parse chart as a parallel array including:
 * 
 * <ol>
 * <li>Probabilities of each non-terminal (float; NEGATIVE_INFINITY for unobserved non-terminals)</li>
 * <li>Child pairs producing each non-terminal --- equivalent to the grammar rule (int)</li>
 * <li>Midpoints (short)</li>
 * </ol>
 * 
 * 
 * @author Aaron Dunlop
 * @since Jun 2, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class ParallelArrayChart extends Chart {

    public final SparseMatrixGrammar sparseMatrixGrammar;

    /**
     * Start indices for each cell. Computed from cell start and end indices and stored in the chart for
     * convenience
     */
    protected final int[] cellOffsets;

    /** The number of cells in this chart */
    public final int cells;

    protected final int chartArraySize;

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
    protected ParallelArrayChart(final int size, final SparseMatrixGrammar sparseMatrixGrammar) {
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

    @Override
    public abstract ParallelArrayChartCell getCell(final int start, final int end);

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

    public final int offset(final int cellIndex) {
        return cellOffsets[cellIndex];
    }

    public final int chartArraySize() {
        return chartArraySize;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        for (int span = 1; span <= size; span++) {
            for (int start = 0; start <= size - span; start++) {
                final int end = start + span;
                sb.append(getCell(start, end).toString());
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    public abstract class ParallelArrayChartCell extends ChartCell {
        protected final int cellIndex;
        protected final int offset;

        public ParallelArrayChartCell(final int start, final int end) {
            super(start, end);

            cellIndex = cellIndex(start, end);
            offset = cellIndex * sparseMatrixGrammar.numNonTerms();
        }

        /**
         * Returns the start index of this cell in the main packed array
         * 
         * @return the start index of this cell in the main packed array
         */
        public final int offset() {
            return offset;
        }

        protected String formatCellEntry(final int nonterminal, final int childProductions,
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
