/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;

/**
 * Populates the parse chart with inside-outside probabilities, choosing the 1-best probability for each nonterminal
 * (i.e., the Viterbi inside scores, followed by Viterbi outside scores as well).
 * 
 * @see InsideOutsideCphSpmlParser
 * 
 * @author Aaron Dunlop
 */
public class ViterbiInOutCphSpmlParser extends BaseIoCphSpmlParser {

    public ViterbiInOutCphSpmlParser(final ParserDriver opts, final InsideOutsideCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    /**
     * Identical to {@link CartesianProductHashSpmlParser}, with the exception of tracking backpointers (children and
     * midpoints), which are not required for non-Viterbi decoding methods.
     */
    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final PackedArrayChartCell targetCell = (PackedArrayChartCell) cell;
        final short start = cell.start();
        final short end = cell.end();
        targetCell.allocateTemporaryStorage();

        final TemporaryChartCell tmpCell = targetCell.tmpCell;

        final boolean factoredOnly = cellSelector.hasCellConstraints() && cellSelector.isCellOnlyFactored(start, end);

        final int[] binaryColumnOffsets = factoredOnly ? grammar.factoredCscBinaryColumnOffsets
                : grammar.cscBinaryColumnOffsets;
        final float[] binaryProbabilities = factoredOnly ? grammar.factoredCscBinaryProbabilities
                : grammar.cscBinaryProbabilities;
        final short[] binaryRowIndices = factoredOnly ? grammar.factoredCscBinaryRowIndices
                : grammar.cscBinaryRowIndices;

        final PackingFunction pf = grammar.packingFunction();

        // Iterate over all possible midpoints
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            if (end - start > cellSelector.getMaxSpan(start, end)) {
                continue;
            }

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
                    final int column = pf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = binaryColumnOffsets[column]; k < binaryColumnOffsets[column + 1]; k++) {

                        final float jointProbability = binaryProbabilities[k] + childProbability;
                        final short parent = binaryRowIndices[k];

                        if (jointProbability > tmpCell.insideProbabilities[parent]) {
                            tmpCell.insideProbabilities[parent] = jointProbability;
                        }
                    }
                }
            }

            if (collectDetailedStatistics) {
                chart.parseTask.nBinaryConsidered += (leftEnd - leftStart + 1) * (rightEnd - rightStart + 1);
            }
        }

        if (collectDetailedStatistics) {
            chart.parseTask.insideBinaryNs += System.nanoTime() - t0;
        }

        // Apply unary rules (retaining only 1-best probabilities for unary parents, and only if that probability is
        // greater than the sum of all probabilities for that non-terminal as a binary parent)
        if (exhaustiveSearch) {
            unarySpmv(targetCell);
            targetCell.finalizeCell();
        } else {
            unaryAndPruning(targetCell, start, end);
            targetCell.finalizeCell();
        }
    }

    @Override
    protected final void computeOutsideProbabilities(final PackedArrayChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final short start = cell.start();
        final short end = cell.end();

        // Allocate temporary storage and populate start-symbol probability in the top cell
        cell.allocateTemporaryStorage(true, false);
        if (start == 0 && end == chart.size()) {
            cell.tmpCell.outsideProbabilities[grammar.startSymbol] = 0;
        }

        // Left-side siblings first

        // foreach parent-start in {0..start - 1}
        for (int parentStart = 0; parentStart < start; parentStart++) {
            final PackedArrayChartCell parentCell = chart.getCell(parentStart, end);
            parentCell.allocateTemporaryStorage(true, true);
            final float[] parentOutsideProbabilities = parentCell.tmpCell.outsideProbabilities;

            // Sibling (left) cell
            final int siblingCellIndex = chart.cellIndex(parentStart, start);
            final int siblingStartIndex = chart.minLeftChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxLeftChildIndex(siblingCellIndex);

            computeLeftSiblingOutsideProbabilities(cell.tmpCell.outsideProbabilities, cell.minRightChildIndex(),
                    cell.maxRightChildIndex(), siblingStartIndex, siblingEndIndex, parentOutsideProbabilities);
        }

        // Right-side siblings

        // foreach parent-end in {end + 1..n}
        for (int parentEnd = end + 1; parentEnd <= chart.size(); parentEnd++) {
            final PackedArrayChartCell parentCell = chart.getCell(start, parentEnd);
            parentCell.allocateTemporaryStorage(true, true);
            final float[] parentOutsideProbabilities = parentCell.tmpCell.outsideProbabilities;

            // Sibling (right) cell
            final int siblingCellIndex = chart.cellIndex(end, parentEnd);
            final int siblingStartIndex = chart.minRightChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxRightChildIndex(siblingCellIndex);

            computeRightSiblingOutsideProbabilities(cell.tmpCell.outsideProbabilities, cell.minLeftChildIndex(),
                    cell.maxLeftChildIndex(), siblingStartIndex, siblingEndIndex, parentOutsideProbabilities);
        }

        // Unary outside probabilities
        if (collectDetailedStatistics) {
            final long t1 = System.nanoTime();
            chart.parseTask.outsideBinaryNs += t1 - t0;
            computeUnaryOutsideProbabilities(cell.tmpCell.outsideProbabilities);
            chart.parseTask.outsideUnaryNs += System.nanoTime() - t1;
        } else {
            computeUnaryOutsideProbabilities(cell.tmpCell.outsideProbabilities);
        }

        cell.finalizeCell();
    }

    private void computeLeftSiblingOutsideProbabilities(final float[] outsideProbabilities, final int targetStart,
            final int targetEnd, final int siblingStart, final int siblingEnd, final float[] parentOutsideProbabilities) {

        final PackingFunction pf = grammar.packingFunction();

        // Iterate over entries in the left sibling cell
        for (int i = siblingStart; i <= siblingEnd; i++) {
            final short leftSibling = chart.nonTerminalIndices[i];
            final float siblingInsideProbability = chart.insideProbabilities[i];

            // And over entries in the target cell
            for (int j = targetStart; j <= targetEnd; j++) {
                final short entry = chart.nonTerminalIndices[j];
                final int column = pf.pack(leftSibling, entry);
                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {

                    final int parent = grammar.cscBinaryRowIndices[k];
                    // Skip log-sum calculations for parents with 0 outside probability
                    if (parentOutsideProbabilities[parent] == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    // Viterbi outside probability = max(production probability x parent outside x sibling inside)
                    final float outsideProbability = grammar.cscBinaryProbabilities[k]
                            + parentOutsideProbabilities[parent] + siblingInsideProbability;

                    if (outsideProbability > outsideProbabilities[entry]) {
                        outsideProbabilities[entry] = outsideProbability;
                    }
                }
            }
        }
    }

    private void computeRightSiblingOutsideProbabilities(final float[] outsideProbabilities, final int targetStart,
            final int targetEnd, final int siblingStart, final int siblingEnd, final float[] parentOutsideProbabilities) {

        final PackingFunction pf = grammar.packingFunction();

        // Iterate over entries in the left sibling cell
        for (int i = siblingStart; i <= siblingEnd; i++) {
            final short rightSibling = chart.nonTerminalIndices[i];
            final float siblingInsideProbability = chart.insideProbabilities[i];

            // And over entries in the target cell
            for (int j = targetStart; j <= targetEnd; j++) {
                final short entry = chart.nonTerminalIndices[j];
                final int column = pf.pack(entry, rightSibling);
                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {

                    final int parent = grammar.cscBinaryRowIndices[k];
                    // Skip log-sum calculations for parents with 0 outside probability
                    if (parentOutsideProbabilities[parent] == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    // Viterbi outside probability = max(production probability x parent outside x sibling inside)
                    final float outsideProbability = grammar.cscBinaryProbabilities[k]
                            + parentOutsideProbabilities[parent] + siblingInsideProbability;

                    if (outsideProbability > outsideProbabilities[entry]) {
                        outsideProbabilities[entry] = outsideProbability;
                    }
                }
            }
        }
    }
}
