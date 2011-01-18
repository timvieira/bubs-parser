package edu.ohsu.cslu.parser.spmv;

import cltool4j.args4j.EnumAliasMap;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;

/**
 * A class of parser which performs the grammar intersection in each cell by:
 * <ol>
 * <li>Finding the cartesian product of possible child productions in child cells across all possible midpoints.
 * <li>Multiplying that cartesian product vector by the grammar matrix (stored in a sparse format).
 * <ol>
 * 
 * Subclasses use a variety of sparse matrix grammar representations, and differ in how they perform the cartesian
 * product. Some implementations perform the vector and matrix operations on GPU hardware throgh OpenCL.
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class SparseMatrixVectorParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        SparseMatrixParser<G, C> {

    protected final float[] cartesianProductProbabilities;
    protected final short[] cartesianProductMidpoints;

    public long startTime = 0;
    public long totalCartesianProductTime = 0;
    public long totalCartesianProductUnionTime = 0;
    public long totalBinarySpMVTime = 0;
    public long totalPruningTime = 0;
    public long totalUnaryTime = 0;
    public long totalFinalizeTime = 0;

    protected int totalCartesianProductSize;
    protected long totalCellPopulation;
    protected long totalLeftChildPopulation;
    protected long totalRightChildPopulation;

    public SparseMatrixVectorParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
        cartesianProductProbabilities = new float[grammar.cartesianProductFunction().packedArraySize()];
        cartesianProductMidpoints = new short[cartesianProductProbabilities.length];
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
    protected void initSentence(final int[] tokens) {
        startTime = System.currentTimeMillis();
        if (collectDetailedStatistics) {
            totalBinarySpMVTime = 0;
            totalPruningTime = 0;
            totalUnaryTime = 0;
            totalCartesianProductTime = 0;
            totalCartesianProductUnionTime = 0;
            totalFinalizeTime = 0;
            totalCartesianProductSize = 0;
            totalCellPopulation = 0;
            totalLeftChildPopulation = 0;
            totalRightChildPopulation = 0;
        }

        chart.tokens = tokens;
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final ParallelArrayChartCell spvChartCell = chart.getCell(start, end);

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        long t2 = t0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);

            if (collectDetailedStatistics) {
                totalCartesianProductSize += cartesianProductVector.size();
                t1 = System.currentTimeMillis();
                totalCartesianProductTime += (t1 - t0);
            }

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmv(cartesianProductVector, spvChartCell);
        }

        if (collectDetailedStatistics) {
            t2 = System.currentTimeMillis();
            totalBinarySpMVTime += (t2 - t1);
        }

        // Handle unary productions
        // This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        if (!cellSelector.factoredParentsOnly(start, end)) {
            unarySpmv(spvChartCell);
        }

        if (collectDetailedStatistics) {
            totalUnaryTime += (System.currentTimeMillis() - t2);

            totalCellPopulation += spvChartCell.getNumNTs();
            if (spvChartCell instanceof PackedArrayChartCell) {
                totalLeftChildPopulation += ((PackedArrayChartCell) spvChartCell).leftChildren();
                totalRightChildPopulation += ((PackedArrayChartCell) spvChartCell).rightChildren();
            }
        }

        // Pack the temporary cell storage into the main chart array
        if (collectDetailedStatistics) {
            final long t3 = System.currentTimeMillis();
            spvChartCell.finalizeCell();
            totalFinalizeTime += (System.currentTimeMillis() - t3);
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

        final int[] chartCellChildren = packedArrayCell.tmpPackedChildren;
        final float[] chartCellProbabilities = packedArrayCell.tmpInsideProbabilities;
        final short[] chartCellMidpoints = packedArrayCell.tmpMidpoints;
        final short chartCellEnd = chartCell.end();

        unarySpmv(chartCellChildren, chartCellProbabilities, chartCellMidpoints, 0, chartCellEnd);
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

    public String getStatHeader() {
        return String.format("%8s, %5d, %9s, %10s, %8s, %12s, %11s, %8s, %7s, %7s", "Total", "Init", "EdgeInit",
                "X-product", "Bin-SpMV", "Unary", "Finalize", "Extract", "Pruning");
    }

    @Override
    public String getStats() {
        final long totalTime = System.currentTimeMillis() - startTime;
        return String.format("%8.1f, %5d, %9d, %8d, %12d, %11d, %8d, %7d, %7d", totalTime / 1000f, initTime,
                edgeSelectorInitTime, totalCartesianProductTime, totalBinarySpMVTime, totalUnaryTime,
                totalFinalizeTime, extractTime, totalPruningTime);
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
                    final int leftChild = grammar.cartesianProductFunction().unpackLeftChild(i);
                    final int rightChild = grammar.cartesianProductFunction().unpackRightChild(i);
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

    static public enum CartesianProductFunctionType {
        Simple("d", "default"), Unfiltered("u", "unfiltered"), PosFactoredFiltered("pf"), BitMatrixExactFilter("bme",
                "bitmatrixexact"), PerfectHash("ph", "perfecthash"), PerfectHash2("ph2", "perfecthash2");

        private CartesianProductFunctionType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }
}
