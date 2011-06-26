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

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSC format (
 * {@link LeftCscSparseMatrixGrammar}).
 * 
 * @see CsrSpmvParser
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 */
public class CscSpmvParser extends PackedArraySpmvParser<LeftCscSparseMatrixGrammar> {

    public CscSpmvParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final PackedArrayChartCell targetCell = (PackedArrayChartCell) chartCell;
        targetCell.allocateTemporaryStorage();

        if (cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(chartCell.start(), chartCell.end())) {
            // Multiply by the factored grammar rule matrix
            binarySpmvMultiply(cartesianProductVector, grammar.factoredCscBinaryPopulatedColumns,
                    grammar.factoredCscBinaryPopulatedColumnOffsets, grammar.factoredCscBinaryRowIndices,
                    grammar.factoredCscBinaryProbabilities, targetCell.tmpPackedChildren,
                    targetCell.tmpInsideProbabilities, targetCell.tmpMidpoints, 0,
                    grammar.cscBinaryPopulatedColumns.length);
        } else {
            // Multiply by the main grammar rule matrix
            binarySpmvMultiply(cartesianProductVector, grammar.cscBinaryPopulatedColumns,
                    grammar.cscBinaryPopulatedColumnOffsets, grammar.cscBinaryRowIndices,
                    grammar.cscBinaryProbabilities, targetCell.tmpPackedChildren, targetCell.tmpInsideProbabilities,
                    targetCell.tmpMidpoints, 0, grammar.cscBinaryPopulatedColumns.length);
        }
    }

    protected final void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final int[] grammarCscBinaryPopulatedColumns, final int[] grammarCscBinaryPopulatedColumnOffsets,
            final short[] grammarCscBinaryRowIndices, final float[] grammarCscBinaryProbabilities,
            final int[] targetCellChildren, final float[] targetCellProbabilities, final short[] targetCellMidpoints,
            final int populatedColumnStartIndex, final int populatedColumnEndIndex) {

        // Iterate over possible populated child pairs (matrix columns)
        for (int i = populatedColumnStartIndex; i < populatedColumnEndIndex; i++) {

            // TODO Try iterating through the midpoints array first and only look up the childPair for
            // populated columns. Even though some entries will be impossible, the cache-efficiency of in-order
            // iteration might be a win?
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
    public String getStats() {
        return super.getStats()
                + (collectDetailedStatistics ? String.format(" avgXprod=%.1f cells=%d totalC=%d c_l=%d c_r=%d",
                        sentenceCartesianProductSize * 1.0f / chart.cells, chart.cells, sentenceCellPopulation,
                        sentenceLeftChildPopulation, sentenceRightChildPopulation, sentenceBinarySpMVTime) : "");
    }

}
