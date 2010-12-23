package edu.ohsu.cslu.parser.spmv;


import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSC format (
 * {@link LeftCscSparseMatrixGrammar}) and implements cross-product and SpMV multiplication in Java.
 * 
 * @see CsrSpmvParser
 * @see OpenClSpmvParser
 * 
 *      TODO Share code copied from {@link CsrSpmvParser}
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CscSpmvParser extends PackedArraySpmvParser<LeftCscSparseMatrixGrammar> {

    public CscSpmvParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final PackedArrayChartCell targetCell = (PackedArrayChartCell) chartCell;
        targetCell.allocateTemporaryStorage();

        if (cellSelector.factoredParentsOnly(chartCell.start(), chartCell.end())) {
            binarySpmvMultiply(cartesianProductVector, grammar.factoredCscBinaryPopulatedColumns,
                    grammar.factoredCscBinaryPopulatedColumnOffsets, grammar.factoredCscBinaryRowIndices,
                    grammar.factoredCscBinaryProbabilities, targetCell.tmpPackedChildren,
                    targetCell.tmpInsideProbabilities, targetCell.tmpMidpoints);
        } else {
            binarySpmvMultiply(cartesianProductVector, grammar.cscBinaryPopulatedColumns,
                    grammar.cscBinaryPopulatedColumnOffsets, grammar.cscBinaryRowIndices,
                    grammar.cscBinaryProbabilities, targetCell.tmpPackedChildren, targetCell.tmpInsideProbabilities,
                    targetCell.tmpMidpoints);
        }
    }

    private void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final int[] grammarCscBinaryPopulatedColumns, final int[] grammarCscBinaryPopulatedColumnOffsets,
            final short[] grammarCscBinaryRowIndices, final float[] grammarCscBinaryProbabilities,
            final int[] targetCellChildren, final float[] targetCellProbabilities, final short[] targetCellMidpoints) {

        // Iterate over possible populated child pairs (matrix columns)
        for (int i = 0; i < grammarCscBinaryPopulatedColumns.length; i++) {

            // TODO Try iterating through the midpoints array first and only look up the childPair for populated
            // columns. Even though some entries will be impossible, the cache-efficiency of in-order iteration might be
            // a win?
            final int childPair = grammarCscBinaryPopulatedColumns[i];
            final short cartesianProductMidpoint = cartesianProductVector.midpoints[childPair];

            // Skip grammar matrix columns for unpopulated cartesian-product entries
            if (cartesianProductMidpoint == 0) {
                continue;
            }
            final float cartesianProductProbability = cartesianProductVector.probabilities[childPair];

            // Iterate over possible parents of the child pair (rows with non-zero entries)
            for (int j = grammarCscBinaryPopulatedColumnOffsets[i]; j < grammarCscBinaryPopulatedColumnOffsets[i + 1]; j++) {

                final float jointProbability = grammarCscBinaryProbabilities[j] + cartesianProductProbability;
                final int parent = grammarCscBinaryRowIndices[j];

                if (jointProbability > targetCellProbabilities[parent]) {
                    targetCellChildren[parent] = childPair;
                    targetCellProbabilities[parent] = jointProbability;
                    targetCellMidpoints[parent] = cartesianProductMidpoint;
                }
            }
        }
    }

    @Override
    protected void unarySpmv(final int[] chartCellChildren, final float[] chartCellProbabilities,
            final short[] chartCellMidpoints, final int offset, final short chartCellEnd) {

        final CartesianProductFunction cpf = grammar.cartesianProductFunction();

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

    @Override
    public String getStatHeader() {
        return super.getStatHeader()
                + ", Avg X-prod size, X-prod Entries Examined, Total X-prod Entries, Cells,   Total C, Total C_l, Total C_r";
    }

    @Override
    public String getStats() {
        return super.getStats()
                + String.format(", %15.1f, %23d, %20d, %6d, %10d, %10d, %10d", totalCartesianProductSize * 1.0f
                        / chart.cells, totalCartesianProductEntriesExamined, totalValidCartesianProductEntries,
                        chart.cells, totalCellPopulation, totalLeftChildPopulation, totalRightChildPopulation);
    }

}
