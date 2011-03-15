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

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();
        binarySpmvMultiply(cartesianProductVector, packedArrayCell.tmpPackedChildren,
                packedArrayCell.tmpInsideProbabilities, packedArrayCell.tmpMidpoints);
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
                + (collectDetailedStatistics ? String.format(" avgXprod=%.1f", sentenceCartesianProductSize * 1.0f
                        / chart.cells) : "");
    }

}
