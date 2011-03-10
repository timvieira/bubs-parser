package edu.ohsu.cslu.parser.spmv;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSR format ( {@link CsrSparseMatrixGrammar})
 * and implements cross-product and SpMV multiplication in Java.
 * 
 * @see OpenClSpmvParser
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CsrSpmvParser extends PackedArraySpmvParser<CsrSparseMatrixGrammar> {

    public CsrSpmvParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    // /**
    // * Takes the cross-product of all potential child-cell combinations. Unions those cross-products together, saving
    // * the maximum probability child combinations.
    // *
    // * @param start
    // * @param end
    // * @return Unioned cross-product
    // */
    // @Override
    // protected CartesianProductVector cartesianProductUnion(final int start, final int end) {
    //
    // Arrays.fill(cartesianProductMidpoints, (short) 0);
    // int size = 0;
    //
    // final CartesianProductFunction cpf = grammar.cartesianProductFunction();
    // final short[] nonTerminalIndices = chart.nonTerminalIndices;
    // final float[] insideProbabilities = chart.insideProbabilities;
    //
    // // Iterate over all possible midpoints, unioning together the cross-product of discovered
    // // non-terminals in each left/right child pair
    //
    // for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
    // final int leftCellIndex = chart.cellIndex(start, midpoint);
    // final int rightCellIndex = chart.cellIndex(midpoint, end);
    //
    // final int leftStart = chart.minLeftChildIndex(leftCellIndex);
    // final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);
    // final int rightStart = chart.minRightChildIndex(rightCellIndex);
    // final int rightEnd = chart.maxRightChildIndex(rightCellIndex);
    //
    // for (int i = leftStart; i <= leftEnd; i++) {
    // final short leftChild = nonTerminalIndices[i];
    // final float leftProbability = insideProbabilities[i];
    //
    // for (int j = rightStart; j <= rightEnd; j++) {
    //
    // if (collectDetailedStatistics) {
    // totalCartesianProductEntriesExamined++;
    // }
    //
    // final int childPair = cpf.pack(leftChild, nonTerminalIndices[j]);
    // if (childPair == Integer.MIN_VALUE) {
    // continue;
    // }
    //
    // final float jointProbability = leftProbability + insideProbabilities[j];
    //
    // if (collectDetailedStatistics) {
    // totalValidCartesianProductEntries++;
    // }
    //
    // // If this cartesian-product entry is not populated, we can populate it without comparing
    // // to a current probability.
    // if (cartesianProductMidpoints[childPair] == 0) {
    // cartesianProductProbabilities[childPair] = jointProbability;
    // cartesianProductMidpoints[childPair] = midpoint;
    //
    // if (collectDetailedStatistics) {
    // size++;
    // }
    //
    // } else {
    // if (jointProbability > cartesianProductProbabilities[childPair]) {
    // cartesianProductProbabilities[childPair] = jointProbability;
    // cartesianProductMidpoints[childPair] = midpoint;
    // }
    // }
    // }
    // }
    // }
    //
    // return new CartesianProductVector(grammar, cartesianProductProbabilities, cartesianProductMidpoints, size);
    // }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        final int[] chartCellChildren = packedArrayCell.tmpPackedChildren;
        final float[] chartCellProbabilities = packedArrayCell.tmpInsideProbabilities;
        final short[] chartCellMidpoints = packedArrayCell.tmpMidpoints;

        binarySpmvMultiply(cartesianProductVector, chartCellChildren, chartCellProbabilities, chartCellMidpoints);
    }

    protected final void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final int[] productChildren, final float[] productProbabilities, final short[] productMidpoints) {

        // Iterate over possible parents (matrix rows)
        final int v = grammar.numNonTerms();
        for (int parent = 0; parent < v; parent++) {

            float winningProbability = Float.NEGATIVE_INFINITY;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = grammar.csrBinaryRowIndices[parent]; i < grammar.csrBinaryRowIndices[parent + 1]; i++) {
                final int grammarChildren = grammar.csrBinaryColumnIndices[i];

                if (cartesianProductVector.midpoints[grammarChildren] == 0) {
                    continue;
                }

                final float jointProbability = grammar.csrBinaryProbabilities[i]
                        + cartesianProductVector.probabilities[grammarChildren];

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = cartesianProductVector.midpoints[grammarChildren];
                }
            }

            if (winningProbability != Float.NEGATIVE_INFINITY) {
                productChildren[parent] = winningChildren;
                productProbabilities[parent] = winningProbability;
                productMidpoints[parent] = winningMidpoint;
            }
        }
    }

    @Override
    public String getStats() {
        return super.getStats()
                + (collectDetailedStatistics ? String.format("avgXprod=%.1f xProdEntriesExamined=%d xProdEntries=%d",
                        totalCartesianProductSize * 1.0f / chart.cells, totalCartesianProductEntriesExamined,
                        totalValidCartesianProductEntries) : "");
    }

}
