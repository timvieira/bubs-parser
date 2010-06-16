package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

public abstract class SparseMatrixParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        ExhaustiveChartParser<G, C> {

    public SparseMatrixParser(final ParserOptions opts, final G grammar) {
        super(opts, grammar);
    }

    public SparseMatrixParser(final G grammar) {
        this(new ParserOptions(), grammar);
    }

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely),
     * and populates this chart cell. Used to populate unary rules.
     * 
     * @param chartCell
     */
    public void unarySpmv(final ChartCell chartCell) {

        if (chartCell instanceof PackedArrayChartCell) {
            final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
            packedArrayCell.allocateTemporaryStorage();

            unarySpmv(packedArrayCell.tmpPackedChildren, packedArrayCell.tmpInsideProbabilities,
                packedArrayCell.tmpMidpoints, 0, (short) chartCell.end());
        } else {
            final DenseVectorChartCell denseVectorCell = (DenseVectorChartCell) chartCell;

            unarySpmv(chart.packedChildren, chart.insideProbabilities, chart.midpoints, denseVectorCell
                .offset(), (short) chartCell.end());
        }
    }

    protected void unarySpmv(final int[] chartCellChildren, final float[] chartCellProbabilities,
            final short[] chartCellMidpoints, final int offset, final short chartCellEnd) {
        // Iterate over possible parents (matrix rows)
        for (int parent = 0; parent < grammar.numNonTerms(); parent++) {

            final float currentProbability = chartCellProbabilities[offset + parent];
            float winningProbability = currentProbability;
            int winningChild = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = grammar.csrUnaryRowStartIndices[parent]; i < grammar.csrUnaryRowStartIndices[parent + 1]; i++) {

                final int child = grammar.csrUnaryColumnIndices[i];
                final float grammarProbability = grammar.csrUnaryProbabilities[i];

                final float jointProbability = grammarProbability + chartCellProbabilities[offset + child];

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChild = child;
                    winningMidpoint = chartCellEnd;
                }
            }

            if (winningChild != Integer.MIN_VALUE) {
                final int parentIndex = offset + parent;
                chartCellChildren[parentIndex] = grammar.cartesianProductFunction().packUnary(winningChild);
                chartCellProbabilities[parentIndex] = winningProbability;
                chartCellMidpoints[parentIndex] = winningMidpoint;
            }
        }
    }

}
