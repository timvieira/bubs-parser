/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.hash.PerfectIntPair2IntHash;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;

/**
 * Exhaustive matrix-loop parser which performs grammar intersection by iterating over grammar rules matching the
 * observed child pairs in the cartesian product of non-terminals observed in child cells. Queries grammar using a
 * segmented perfect hash.
 * 
 * @author Aaron Dunlop
 */
public class CartesianProductLeftChildHashSpmlParser extends
        SparseMatrixLoopParser<LeftCscSparseMatrixGrammar, PackedArrayChart> {

    private final PerfectIntPair2IntHash childPair2ColumnOffsetHash;
    private final int[] hashedCscParallelArrayIndices;

    public CartesianProductLeftChildHashSpmlParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);

        final int[][] keyPairs = new int[2][grammar.cscBinaryPopulatedColumns.length];
        for (int i = 0; i < grammar.cscBinaryPopulatedColumns.length; i++) {
            keyPairs[0][i] = grammar.packingFunction().unpackLeftChild(grammar.cscBinaryPopulatedColumns[i]);
            keyPairs[1][i] = grammar.packingFunction().unpackRightChild(grammar.cscBinaryPopulatedColumns[i]);
        }

        childPair2ColumnOffsetHash = new PerfectIntPair2IntHash(keyPairs);
        hashedCscParallelArrayIndices = new int[childPair2ColumnOffsetHash.hashtableSize()];
        for (int i = 0; i < keyPairs[0].length; i++) {
            hashedCscParallelArrayIndices[childPair2ColumnOffsetHash.hashcode(keyPairs[0][i], keyPairs[1][i])] = i;
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;
        final PackingFunction cpf = grammar.packingFunction();
        final PackedArrayChartCell targetCell = (PackedArrayChartCell) cell;
        final short start = cell.start();
        final short end = cell.end();
        targetCell.allocateTemporaryStorage();
        final TemporaryChartCell tmpCell = targetCell.tmpCell;

        // Iterate over all possible midpoints
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);

            // Iterate over children in the left child cell
            final int leftStart = chart.minLeftChildIndex(leftCellIndex);
            final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            for (int i = leftStart; i <= leftEnd; i++) {
                final short leftChild = chart.nonTerminalIndices[i];

                final float leftProbability = chart.insideProbabilities[i];

                // And over children in the right child cell
                for (int j = rightStart; j <= rightEnd; j++) {

                    final int hashcode = childPair2ColumnOffsetHash.hashcode(leftChild, chart.nonTerminalIndices[j]);
                    if (hashcode < 0) {
                        continue;
                    }
                    final int index = hashedCscParallelArrayIndices[hashcode];

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = grammar.cscBinaryPopulatedColumnOffsets[index]; k < grammar.cscBinaryPopulatedColumnOffsets[index + 1]; k++) {

                        final float jointProbability = grammar.cscBinaryProbabilities[k] + childProbability;
                        final int parent = grammar.cscBinaryRowIndices[k];

                        if (jointProbability > tmpCell.insideProbabilities[parent]) {
                            tmpCell.packedChildren[parent] = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
                            tmpCell.insideProbabilities[parent] = jointProbability;
                            tmpCell.midpoints[parent] = midpoint;
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
