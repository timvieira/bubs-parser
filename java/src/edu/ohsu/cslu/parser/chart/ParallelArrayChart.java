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

import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.lela.ConstrainedChart;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;

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
 */
public abstract class ParallelArrayChart extends Chart {

    public final SparseMatrixGrammar sparseMatrixGrammar;

    /**
     * The maximum number of entries allowed per cell. For exhaustive search, this must be equal to the size of the
     * grammar's vocabulary, but for pruned search, we can limit cell population, reducing the chart's memory footprint
     */
    protected int beamWidth;
    protected int lexicalRowBeamWidth;

    /**
     * Start indices for each cell. Computed from cell start and end indices and stored in the chart for convenience
     */
    public final int[] cellOffsets;

    /** The number of cells in this chart */
    public final int cells;

    protected final int chartArraySize;

    /**
     * Parallel arrays storing non-terminals, inside probabilities, and the grammar rules and midpoints which produced
     * them. Entries for each cell begin at indices from {@link #cellOffsets}.
     */
    public final float[] insideProbabilities;
    public final int[] packedChildren;
    public final short[] midpoints;

    /**
     * Constructs a chart
     * 
     * @param parseTask Parser state
     * @param sparseMatrixGrammar Grammar
     * @param beamWidth The maximum number of entries allowed in a chart cell
     */
    protected ParallelArrayChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar,
            final int beamWidth, final int lexicalRowBeamWidth) {

        super(parseTask, sparseMatrixGrammar);
        this.sparseMatrixGrammar = sparseMatrixGrammar;
        this.beamWidth = Math.min(beamWidth, sparseMatrixGrammar.numNonTerms());
        this.lexicalRowBeamWidth = Math.min(lexicalRowBeamWidth, sparseMatrixGrammar.numNonTerms());

        cells = size * (size + 1) / 2;

        this.chartArraySize = chartArraySize(this.size, this.beamWidth, this.lexicalRowBeamWidth);
        this.insideProbabilities = new float[chartArraySize];
        Arrays.fill(insideProbabilities, Float.NEGATIVE_INFINITY);
        this.packedChildren = new int[chartArraySize];
        this.midpoints = new short[chartArraySize];

        this.cellOffsets = new int[cells];

        // Calculate all cell offsets, etc
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end <= size; end++) {
                final int cellIndex = cellIndex(start, end);
                cellOffsets[cellIndex] = cellOffset(start, end);
            }
        }
    }

    /**
     * Constructs a chart for exhaustive parsing (beamWidth = |V|)
     * 
     * @param parseTask parser state
     * @param sparseMatrixGrammar
     */
    protected ParallelArrayChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar) {
        this(parseTask, sparseMatrixGrammar, sparseMatrixGrammar.numNonTerms(), sparseMatrixGrammar.numNonTerms());
    }

    /**
     * Constructs a chart for Constrained parsing (see {@link ConstrainedChart}).
     * 
     * @param size
     * @param chartArraySize
     * @param sparseMatrixGrammar
     */
    protected ParallelArrayChart(final int size, final int chartArraySize, final SparseMatrixGrammar sparseMatrixGrammar) {
        this.grammar = this.sparseMatrixGrammar = sparseMatrixGrammar;
        this.size = size;
        this.chartArraySize = chartArraySize;

        this.cells = size * (size + 1) / 2;
        this.cellOffsets = new int[cells];

        this.insideProbabilities = new float[chartArraySize];
        this.packedChildren = new int[chartArraySize];
        this.midpoints = new short[cells];
    }

    @Override
    public abstract ParallelArrayChartCell getCell(final int start, final int end);

    /**
     * Returns the offset of the specified cell in the parallel chart arrays (note that this computation must agree with
     * that of {@link #cellIndex(int, int)}
     * 
     * @param start
     * @param end
     * @return the offset of the specified cell in the parallel chart arrays
     */
    protected int cellOffset(final int start, final int end) {

        if (start < 0 || start > size) {
            throw new IllegalArgumentException("Illegal start: " + start);
        }

        if (end <= start || end > size) {
            throw new IllegalArgumentException("Illegal end: " + end);
        }

        final int priorCellBeamWidths = cellIndex(start, end) * this.beamWidth;
        // If this cell is in the lexical row, we've seen 'start' prior lexical entries; otherwise we've seen the one in
        // this diagonal too, so 'start + 1'
        final int priorLexicalCells = (end - start == 1) ? start : start + 1;
        return priorCellBeamWidths + priorLexicalCells * (this.lexicalRowBeamWidth - this.beamWidth);
    }

    public final int offset(final int cellIndex) {
        return cellOffsets[cellIndex];
    }

    public final int chartArraySize() {
        return chartArraySize;
    }

    public int chartArraySize(final int newSize, final int newBeamWidth, final int newLexicalRowBeamWidth) {
        return newSize * newLexicalRowBeamWidth + (cells - size) * newBeamWidth;
    }

    public int beamWidth() {
        return beamWidth;
    }

    public int lexicalRowBeamWidth() {
        return lexicalRowBeamWidth;
    }

    /**
     * Re-initializes the chart data structures, facilitating reuse of the chart for multiple sentences. Subclasses must
     * ensure that the data structure state following {@link #reset(ParseTask, int, int)} is identical to that of a
     * newly constructed chart.
     * 
     * @param task
     * @param newBeamWidth
     * @param newLexicalRowBeamWidth
     */
    public void reset(final ParseTask task, final int newBeamWidth, final int newLexicalRowBeamWidth) {
        this.beamWidth = newBeamWidth;
        this.lexicalRowBeamWidth = newLexicalRowBeamWidth;
        reset(task);
    }

    @Override
    public String toString() {
        return toString(false, false);
    }

    public String toString(final boolean formatFractions, final boolean includeEmptyCells) {
        final StringBuilder sb = new StringBuilder();

        for (int span = 1; span <= size; span++) {
            for (int start = 0; start <= size - span; start++) {
                final int end = start + span;
                final ParallelArrayChartCell cell = getCell(start, end);
                if (cell.getNumNTs() > 0 || includeEmptyCells) {
                    sb.append(cell.toString(formatFractions));
                    sb.append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    protected static String formatCellEntry(final SparseMatrixGrammar g, final int nonterminal,
            final int childProductions, final float insideProbability, final int midpoint, final boolean formatFractions) {
        return formatCellEntry(g, nonterminal, childProductions, insideProbability, Float.NEGATIVE_INFINITY, midpoint,
                formatFractions);
    }

    protected static String formatCellEntry(final SparseMatrixGrammar g, final int nonterminal,
            final int childProductions, final float insideProbability, final float outsideProbability,
            final int midpoint, final boolean formatFractions) {

        final String sOutsideProbability = outsideProbability != Float.NEGATIVE_INFINITY ? String.format("%.5s",
                outsideProbability) : "-";
        // Goodman, SplitSum, and Max-Rule decoding don't record children
        if (childProductions == 0 || childProductions == Integer.MIN_VALUE) {
            return String.format("%s -> ? (%.5f, %s, %d)\n", g.mapNonterminal(nonterminal), insideProbability,
                    sOutsideProbability, midpoint);
        }

        final int leftChild = g.packingFunction().unpackLeftChild(childProductions);
        final int rightChild = g.packingFunction().unpackRightChild(childProductions);

        if (rightChild == Production.UNARY_PRODUCTION) {
            // Unary Production
            return String.format("%s -> %s (%.5f, %s, %d)\n", g.mapNonterminal(nonterminal),
                    g.mapNonterminal(leftChild), insideProbability, sOutsideProbability, midpoint);
        } else if (rightChild == Production.LEXICAL_PRODUCTION) {
            // Lexical Production
            return String.format("%s -> %s (%.5f, %s, %d)\n", g.mapNonterminal(nonterminal),
                    g.mapLexicalEntry(leftChild), insideProbability, sOutsideProbability, midpoint);
        } else {
            return String.format("%s -> %s %s (%.5f, %s, %d)\n", g.mapNonterminal(nonterminal),
                    g.mapNonterminal(leftChild), g.mapNonterminal(rightChild), insideProbability, sOutsideProbability,
                    midpoint);
        }
    }

    public abstract class ParallelArrayChartCell extends ChartCell {

        public TemporaryChartCell tmpCell;

        protected final int cellIndex;
        protected final int offset;

        protected ParallelArrayChartCell(final int start, final int end) {
            super(start, end);

            cellIndex = cellIndex(start, end);
            offset = cellOffsets[cellIndex];
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
         * Special-case to populate a cell with a single entry without a linear search of the temporary storage
         * 
         * @param entryNonTerminal
         * @param entryInsideProbability
         * @param entryPackedChildren
         * @param entryMidpoint
         */
        public abstract void finalizeCell(final short entryNonTerminal, final float entryInsideProbability,
                final int entryPackedChildren, final short entryMidpoint);

        /**
         * Special-case to finalize an empty cell
         */
        public abstract void finalizeEmptyCell();

        public abstract String toString(final boolean formatFractions);

        /**
         * Populates lexical production probabilities.
         * 
         * @param child
         * @param parents
         * @param probabilities
         */
        public abstract void storeLexicalProductions(final int child, final short[] parents, final float[] probabilities);

        @Override
        public String toString() {
            return toString(false);
        }
    }
}
