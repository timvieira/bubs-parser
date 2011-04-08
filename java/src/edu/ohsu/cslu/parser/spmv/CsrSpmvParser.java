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

    // CSR Unary SpMV. For now, we're using the standard CSC version, but if we want to parallelize unary processing, we
    // might want this one again.

    // @Override
    // protected void unarySpmv(final int[] chartCellChildren, final float[] chartCellProbabilities,
    // final short[] chartCellMidpoints, final int offset, final short chartCellEnd) {
    // // Iterate over possible parents (matrix rows)
    // for (int parent = 0; parent < grammar.numNonTerms(); parent++) {
    //
    // final float currentProbability = chartCellProbabilities[offset + parent];
    // float winningProbability = currentProbability;
    // short winningChild = Short.MIN_VALUE;
    // short winningMidpoint = 0;
    //
    // // Iterate over possible children of the parent (columns with non-zero entries)
    // for (int i = grammar.csrUnaryRowStartIndices[parent]; i < grammar.csrUnaryRowStartIndices[parent + 1]; i++) {
    //
    // final short child = grammar.csrUnaryColumnIndices[i];
    // final float grammarProbability = grammar.csrUnaryProbabilities[i];
    //
    // final float jointProbability = grammarProbability + chartCellProbabilities[offset + child];
    //
    // if (jointProbability > winningProbability) {
    // winningProbability = jointProbability;
    // winningChild = child;
    // winningMidpoint = chartCellEnd;
    // }
    // }
    //
    // if (winningChild != Short.MIN_VALUE) {
    // final int parentIndex = offset + parent;
    // chartCellChildren[parentIndex] = grammar.cartesianProductFunction().packUnary(winningChild);
    // chartCellProbabilities[parentIndex] = winningProbability;
    // chartCellMidpoints[parentIndex] = winningMidpoint;
    // }
    // }
    // }

    @Override
    public String getStats() {
        return super.getStats()
                + (collectDetailedStatistics ? String.format(" avgXprod=%.1f", sentenceCartesianProductSize * 1.0f
                        / chart.cells) : "");
    }
}
