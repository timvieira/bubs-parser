package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ExhaustiveChartParser;
import edu.ohsu.cslu.parser.ParserOptions;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser;

public abstract class SparseMatrixLoopParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart>
        extends ExhaustiveChartParser<G, C> {

    public long startTime = 0;

    /**
     * True if we're collecting detailed counts of cell populations, cartesian-product sizes, etc. Set from
     * {@link ParserOptions}, but duplicated here as a final variable, so that the JIT can eliminate
     * potentially-expensive counting code when we don't need it
     * 
     * TODO Move up to {@link ExhaustiveChartParser} (or even higher) and share with
     * {@link SparseMatrixLoopParser}
     */
    protected final boolean collectDetailedStatistics;

    public SparseMatrixLoopParser(final ParserOptions opts, final G grammar) {
        super(opts, grammar);
        this.collectDetailedStatistics = opts.collectDetailedStatistics();
    }

    public SparseMatrixLoopParser(final G grammar) {
        this(new ParserOptions(), grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        startTime = System.currentTimeMillis();
    }

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely),
     * and populates this chart cell. Used to populate unary rules.
     * 
     * TODO Unify with {@link SparseMatrixVectorParser#unarySpmvMultiply(ChartCell)}
     * 
     * @param chartCell
     */
    public void applyUnaryRuleMatrix(final ChartCell chartCell) {

        if (chartCell instanceof PackedArrayChartCell) {
            final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
            packedArrayCell.allocateTemporaryStorage();

            applyUnaryRuleMatrix((short) chartCell.end(), 0, packedArrayCell.tmpPackedChildren,
                packedArrayCell.tmpInsideProbabilities, packedArrayCell.tmpMidpoints);
        } else {
            final DenseVectorChartCell denseVectorCell = (DenseVectorChartCell) chartCell;

            applyUnaryRuleMatrix((short) chartCell.end(), denseVectorCell.offset(), chart.packedChildren,
                chart.insideProbabilities, chart.midpoints);
        }

    }

    private void applyUnaryRuleMatrix(final short chartCellEnd, final int offset,
            final int[] chartCellChildren, final float[] chartCellProbabilities,
            final short[] chartCellMidpoints) {

        final int[] unaryRuleMatrixRowIndices = grammar.unaryRuleMatrixRowIndices();
        final int[] unaryRuleMatrixColumnIndices = grammar.unaryRuleMatrixColumnIndices();
        final float[] unaryRuleMatrixProbabilities = grammar.unaryRuleMatrixProbabilities();

        // Iterate over possible parents (matrix rows)
        for (int parent = 0; parent < grammar.numNonTerms(); parent++) {

            final int parentIndex = offset + parent;

            float winningProbability = chartCellProbabilities[parentIndex];
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = unaryRuleMatrixRowIndices[parent]; i < unaryRuleMatrixRowIndices[parent + 1]; i++) {

                final int grammarChildren = unaryRuleMatrixColumnIndices[i];
                final int child = grammar.cartesianProductFunction().unpackLeftChild(grammarChildren);
                final float grammarProbability = unaryRuleMatrixProbabilities[i];

                final float jointProbability = grammarProbability + chartCellProbabilities[offset + child];

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = chartCellEnd;
                }
            }

            if (winningChildren != Integer.MIN_VALUE) {
                chartCellChildren[parentIndex] = winningChildren;
                chartCellProbabilities[parentIndex] = winningProbability;
                chartCellMidpoints[parentIndex] = winningMidpoint;
            }
        }
    }
}
