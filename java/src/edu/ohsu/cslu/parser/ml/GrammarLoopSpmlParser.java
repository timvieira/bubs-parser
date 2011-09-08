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

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;

/**
 * Exhaustive matrix-loop parser which performs grammar intersection by iterating over all grammar rules for each pair
 * of child cells.
 * 
 * @author Aaron Dunlop
 */
public class GrammarLoopSpmlParser extends SparseMatrixLoopParser<CsrSparseMatrixGrammar, DenseVectorChart> {

    public GrammarLoopSpmlParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void computeInsideProbabilities(final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final DenseVectorChartCell targetCell = chart.getCell(start, end);
        final int targetCellOffset = targetCell.offset();

        // Local copies of chart storage. These shouldn't really be necessary, but the JIT doesn't always
        // figure out to inline remote references.
        final float[] chartInsideProbabilities = chart.insideProbabilities;
        final int[] chartPackedChildren = chart.packedChildren;
        final short[] chartMidpoints = chart.midpoints;

        final int v = grammar.numNonTerms();

        // Iterate over all possible midpoints
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final DenseVectorChartCell leftChildCell = chart.getCell(start, midpoint);
            final DenseVectorChartCell rightChildCell = chart.getCell(midpoint, end);

            final int leftCellOffset = leftChildCell.offset();
            final int rightCellOffset = rightChildCell.offset();

            // Iterate over possible parents (matrix rows)
            for (int parent = 0; parent < v; parent++) {

                // Iterate over possible children of the parent (columns with non-zero entries)
                for (int i = grammar.csrBinaryRowIndices[parent]; i < grammar.csrBinaryRowIndices[parent + 1]; i++) {
                    final int packedChildPair = grammar.csrBinaryColumnIndices[i];

                    final int leftChild = grammar.cartesianProductFunction().unpackLeftChild(packedChildPair);
                    final int rightChild = grammar.cartesianProductFunction().unpackRightChild(packedChildPair);

                    final float leftInsideProbability = chartInsideProbabilities[leftCellOffset + leftChild];
                    if (leftInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }
                    final float rightInsideProbability = chartInsideProbabilities[rightCellOffset + rightChild];
                    if (rightInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    final float jointProbability = leftInsideProbability + rightInsideProbability
                            + grammar.csrBinaryProbabilities[i];

                    final int targetCellParentIndex = targetCellOffset + parent;

                    if (jointProbability > chartInsideProbabilities[targetCellParentIndex]) {
                        chartInsideProbabilities[targetCellParentIndex] = jointProbability;
                        chartPackedChildren[targetCellParentIndex] = packedChildPair;
                        chartMidpoints[targetCellParentIndex] = midpoint;
                    }
                }
            }
        }

        if (collectDetailedStatistics) {
            chart.parseTask.insideBinaryNs += System.nanoTime() - t0;
        }

        // Apply unary rules
        unarySpmv(targetCell);
    }
}
