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
package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;

/**
 * Exhaustive matrix-loop parser which performs grammar intersection by iterating over grammar rules matching the
 * observed non-terminals in the right child cell.
 * 
 * @author Aaron Dunlop
 */
public class RightChildLoopSpmlParser extends SparseMatrixLoopParser<RightCscSparseMatrixGrammar, DenseVectorChart> {

    public RightChildLoopSpmlParser(final ParserDriver opts, final RightCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final DenseVectorChartCell targetCell = (DenseVectorChartCell) cell;
        final short start = cell.start();
        final short end = cell.end();
        final int targetCellOffset = targetCell.offset();

        final int v = grammar.numNonTerms();

        // Iterate over all possible midpoints
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final DenseVectorChartCell leftChildCell = chart.getCell(start, midpoint);
            final DenseVectorChartCell rightChildCell = chart.getCell(midpoint, end);

            final int leftCellOffset = leftChildCell.offset();
            final int rightCellOffset = rightChildCell.offset();

            // Iterate over children in the right child cell
            for (int rightChild = 0; rightChild < v; rightChild++) {
                final float rightInsideProbability = chart.insideProbabilities[rightCellOffset + rightChild];
                if (rightInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over all grammar rules with `leftChild' as the left child
                for (int j = grammar.cscBinaryRightChildStartIndices[rightChild]; j <= grammar.cscBinaryRightChildEndIndices[rightChild]
                        && j >= 0; j++) {

                    // Unpack the grammar rule's right child
                    final int packedChildPair = grammar.cscBinaryPopulatedColumns[j];
                    final int leftChild = grammar.cartesianProductFunction().unpackLeftChild(packedChildPair);

                    // Look up the right child NT's probability in the right child cell
                    final float leftInsideProbability = chart.insideProbabilities[leftCellOffset + leftChild];

                    if (leftInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }
                    final float childProbability = leftInsideProbability + rightInsideProbability;

                    for (int entryIndex = grammar.cscBinaryPopulatedColumnOffsets[j]; entryIndex < grammar.cscBinaryPopulatedColumnOffsets[j + 1]; entryIndex++) {
                        final float jointProbability = childProbability + grammar.cscBinaryProbabilities[entryIndex];
                        final int parent = grammar.cscBinaryRowIndices[entryIndex];

                        final int targetCellParentIndex = targetCellOffset + parent;

                        if (jointProbability > chart.insideProbabilities[targetCellParentIndex]) {
                            chart.insideProbabilities[targetCellParentIndex] = jointProbability;
                            chart.packedChildren[targetCellParentIndex] = packedChildPair;
                            chart.midpoints[targetCellParentIndex] = midpoint;
                        }
                    }
                }
            }
        }

        if (collectDetailedStatistics) {
            chart.parseTask.insideBinaryNs += System.nanoTime() - t0;
        }

        // Apply unary rules
        unarySpmv(targetCell);

        targetCell.finalizeCell();
    }
}
