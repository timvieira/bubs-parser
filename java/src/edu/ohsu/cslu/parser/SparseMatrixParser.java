package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

public abstract class SparseMatrixParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        ChartParser<G, C> {

    public SparseMatrixParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely), and
     * populates this chart cell. Used to populate unary rules.
     * 
     * @param chartCell
     */
    public void unarySpmv(final ChartCell chartCell) {

        if (chartCell instanceof PackedArrayChartCell) {
            final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
            packedArrayCell.allocateTemporaryStorage();

            unarySpmv(packedArrayCell.tmpPackedChildren, packedArrayCell.tmpInsideProbabilities,
                    packedArrayCell.tmpMidpoints, 0, chartCell.end());
        } else {
            final DenseVectorChartCell denseVectorCell = (DenseVectorChartCell) chartCell;

            unarySpmv(chart.packedChildren, chart.insideProbabilities, chart.midpoints, denseVectorCell.offset(),
                    chartCell.end());
        }
    }

    protected void unarySpmv(final int[] chartCellChildren, final float[] chartCellProbabilities,
            final short[] chartCellMidpoints, final int offset, final short chartCellEnd) {

        final PackingFunction cpf = grammar.cartesianProductFunction();

        // Iterate over populated children (matrix columns)
        for (short child = 0; child < grammar.numNonTerms(); child++) {

            final int childOffset = offset + child;
            if (chartCellProbabilities[childOffset] == Float.NEGATIVE_INFINITY) {
                continue;
            }

            // Iterate over possible parents of the child (rows with non-zero entries)
            for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                final short parent = grammar.cscUnaryRowIndices[i];
                final int parentOffset = offset + parent;
                final float grammarProbability = grammar.cscUnaryProbabilities[i];

                final float jointProbability = grammarProbability + chartCellProbabilities[childOffset];
                if (jointProbability > chartCellProbabilities[parentOffset]) {
                    chartCellProbabilities[parentOffset] = jointProbability;
                    chartCellChildren[parentOffset] = cpf.packUnary(child);
                    chartCellMidpoints[parentOffset] = chartCellEnd;
                }
            }
        }
    }
}
