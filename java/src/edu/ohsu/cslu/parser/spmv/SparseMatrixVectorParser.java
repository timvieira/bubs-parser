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
package edu.ohsu.cslu.parser.spmv;

import cltool4j.args4j.EnumAliasMap;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * A class of parser which performs the grammar intersection in each cell by:
 * <ol>
 * <li>Finding the cartesian product of possible child productions in child cells across all possible midpoints.
 * <li>Multiplying that cartesian product vector by the grammar matrix (stored in a sparse format).
 * <ol>
 * 
 * Subclasses use a variety of sparse matrix grammar representations, and differ in how they perform the cartesian
 * product. Some implementations perform the vector and matrix operations on GPU hardware using OpenCL.
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 */
public abstract class SparseMatrixVectorParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        SparseMatrixParser<G, C> {

    public long startTime = 0;
    public long sentenceCartesianProductTime = 0;
    public long sentenceFinalizeTime = 0;

    protected int sentenceCartesianProductSize;
    protected long sentenceCellPopulation;
    protected long sentenceLeftChildPopulation;
    protected long sentenceRightChildPopulation;

    public static long totalCartesianProductTime = 0;
    public static long totalBinarySpmvNs = 0;

    public SparseMatrixVectorParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    /**
     * Multiplies the grammar matrix (stored sparsely) by the supplied cartesian product vector (stored densely), and
     * populates this chart cell.
     * 
     * @param cartesianProductVector
     * @param chartCell
     */
    public abstract void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell);

    @Override
    protected void initSentence(final ParseTask parseTask) {

        super.initSentence(parseTask);

        startTime = System.currentTimeMillis();
        if (collectDetailedStatistics) {
            sentenceCartesianProductTime = 0;
            sentenceFinalizeTime = 0;
            sentenceCartesianProductSize = 0;
            sentenceCellPopulation = 0;
            sentenceLeftChildPopulation = 0;
            sentenceRightChildPopulation = 0;
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {
        internalComputeInsideProbabilities((PackedArrayChartCell) cell);
    }

    protected void internalComputeInsideProbabilities(final PackedArrayChartCell spvChartCell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;
        long t1 = t0;
        long t2 = t0;

        final short start = spvChartCell.start();
        final short end = spvChartCell.end();

        // Add lexical productions for span-1 cells
        if (end - start == 1) {
            addLexicalProductions(spvChartCell);
        }

        // And perform binary grammar intersection for span > 1 cells
        else {
            final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);

            if (collectDetailedStatistics) {
                sentenceCartesianProductSize += cartesianProductVector.size();
                t1 = System.nanoTime();
                final long time = t1 - t0;
                sentenceCartesianProductTime += time;
                totalCartesianProductTime += time;
            }

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmv(cartesianProductVector, spvChartCell);
        }

        if (collectDetailedStatistics) {
            t2 = System.nanoTime();
            chart.parseTask.insideBinaryNs += t2 - t0;
            totalBinarySpmvNs += t2 - t1;
        }

        // Handle unary productions
        // This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        final boolean factoredOnly = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end);
        if (!factoredOnly) {
            unarySpmv(spvChartCell);
        }

        if (collectDetailedStatistics) {
            chart.parseTask.unaryAndPruningNs += (System.nanoTime() - t2);

            sentenceCellPopulation += spvChartCell.getNumNTs();
            if (spvChartCell instanceof PackedArrayChartCell) {
                sentenceLeftChildPopulation += spvChartCell.leftChildren();
                sentenceRightChildPopulation += spvChartCell.rightChildren();
            }
        }

        // Pack the temporary cell storage into the main chart array
        if (collectDetailedStatistics) {
            final long t3 = System.nanoTime();
            spvChartCell.finalizeCell();
            sentenceFinalizeTime += (System.nanoTime() - t3);
        } else {
            spvChartCell.finalizeCell();
        }
    }

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely), and
     * populates this chart cell. Used to populate unary rules.
     * 
     * @param chartCell
     */
    @Override
    public void unarySpmv(final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();
        final TemporaryChartCell tmpCell = packedArrayCell.tmpCell;
        unarySpmv(tmpCell.packedChildren, tmpCell.insideProbabilities, tmpCell.midpoints, 0, chartCell.end());
    }

    /**
     * Takes the cartesian product of all potential child-cell combinations. Unions those cartesian products together,
     * saving the maximum probability child combinations.
     * 
     * @param start
     * @param end
     * @return Unioned cartesian product
     */
    protected abstract CartesianProductVector cartesianProductUnion(final int start, final int end);

    @Override
    public String getStats() {
        return super.getStats()
                + (collectDetailedStatistics ? String.format(" xProductTime=%d finalizeTime=%d",
                        sentenceCartesianProductTime, sentenceFinalizeTime) : "");
    }

    public final static class CartesianProductVector {

        private final SparseMatrixGrammar grammar;
        public final float[] probabilities;
        public final short[] midpoints;
        public final int[] populatedLeftChildren;
        private int size = 0;

        public CartesianProductVector(final SparseMatrixGrammar grammar, final float[] probabilities,
                final short[] midpoints, final int[] populatedLeftChildren, final int size) {
            this.grammar = grammar;
            this.probabilities = probabilities;
            this.midpoints = midpoints;
            this.populatedLeftChildren = populatedLeftChildren;
            this.size = size;
        }

        public CartesianProductVector(final SparseMatrixGrammar grammar, final float[] probabilities,
                final short[] midpoints, final int size) {
            this(grammar, probabilities, midpoints, null, size);
        }

        public final int size() {
            return size;
        }

        public final float probability(final int children) {
            return probabilities[children];
        }

        public final short midpoint(final int children) {
            return midpoints[children];
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < probabilities.length; i++) {
                // Some parsers initialize the midpoints and use 0 as `unpopulated'. Others initialize the
                // probabilities and use Float.NEGATIVE_INFINITY. Since toString() isn't time-crucial, check
                // both.
                if (midpoints[i] != 0 && probabilities[i] != Float.NEGATIVE_INFINITY) {
                    final int leftChild = grammar.packingFunction().unpackLeftChild(i);
                    final int rightChild = grammar.packingFunction().unpackRightChild(i);
                    final int midpoint = midpoints[i];
                    final float probability = probabilities[i];

                    if (rightChild == Production.UNARY_PRODUCTION) {
                        // Unary production
                        sb.append(String.format("%s (%d) %.3f (%d)\n", grammar.mapNonterminal(leftChild), leftChild,
                                probability, midpoint));

                    } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                        // Lexical production
                        sb.append(String.format("%s (%d) %.3f (%d)\n", grammar.mapLexicalEntry(leftChild), leftChild,
                                probability, midpoint));

                    } else {
                        // Binary production
                        sb.append(String.format("%s (%d),%s (%d) %.3f (%d)\n", grammar.mapNonterminal(leftChild),
                                leftChild, grammar.mapNonterminal(rightChild), rightChild, probability, midpoint));
                    }
                }
            }
            return sb.toString();
        }
    }

    static public enum PackingFunctionType {
        Simple("d", "default"), Hash("hash"), PerfectHash("ph", "ph2", "perfecthash");

        private PackingFunctionType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }
}
