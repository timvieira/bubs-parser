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

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CscHashPackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * Exhaustive matrix-loop parser which performs grammar intersection by iterating over grammar rules matching the
 * observed child pairs in the cartesian product of non-terminals observed in child cells. Queries grammar using a
 * perfect hash.
 * 
 * @author Aaron Dunlop
 */
public class CartesianProductHashSpmlParser extends
        SparseMatrixLoopParser<LeftCscSparseMatrixGrammar, PackedArrayChart> {

    public CartesianProductHashSpmlParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initSentence(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            // TODO Consolidate chart construction in a superclass using the genericized grammar
            chart = new PackedArrayChart(tokens, grammar, beamWidth, lexicalRowBeamWidth);
        }
        super.initSentence(tokens);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final PackingFunction cpf = grammar.cartesianProductFunction();
        final int[] cscColumnOffsets = ((CscHashPackingFunction) cpf).cscColumnOffsets();
        final PackedArrayChartCell targetCell = chart.getCell(start, end);
        targetCell.allocateTemporaryStorage();

        final int[] targetCellChildren = targetCell.tmpPackedChildren;
        final float[] targetCellProbabilities = targetCell.tmpInsideProbabilities;
        final short[] targetCellMidpoints = targetCell.tmpMidpoints;

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
                    final int column = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = cscColumnOffsets[column]; k < cscColumnOffsets[column + 1]; k++) {

                        final float jointProbability = grammar.cscBinaryProbabilities[k] + childProbability;
                        final int parent = grammar.cscBinaryRowIndices[k];

                        if (jointProbability > targetCellProbabilities[parent]) {
                            targetCellChildren[parent] = column;
                            targetCellProbabilities[parent] = jointProbability;
                            targetCellMidpoints[parent] = midpoint;
                        }
                    }
                }
            }
        }

        // Apply unary rules
        if (exhaustiveSearch) {
            unarySpmv(targetCell);
            targetCell.finalizeCell();
        } else {
            final int[] cellPackedChildren = new int[grammar.numNonTerms()];
            final float[] cellInsideProbabilities = new float[grammar.numNonTerms()];
            final short[] cellMidpoints = new short[grammar.numNonTerms()];
            unaryAndPruning(targetCell, start, end, cellPackedChildren, cellInsideProbabilities, cellMidpoints);

            targetCell.finalizeCell(cellPackedChildren, cellInsideProbabilities, cellMidpoints);
        }
    }
}
