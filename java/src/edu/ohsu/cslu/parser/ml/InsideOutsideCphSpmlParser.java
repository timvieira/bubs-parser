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

import java.util.Arrays;

import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;
import edu.ohsu.cslu.util.Math;

/**
 * Populates the parse chart with inside-outside probabilities, summing probability mass for each nonterminal from all
 * applicable grammar rules.
 * 
 * @see ViterbiInOutCphSpmlParser
 * 
 * @author Aaron Dunlop
 */
public class InsideOutsideCphSpmlParser extends BaseIoCphSpmlParser {

    public InsideOutsideCphSpmlParser(final ParserDriver opts, final InsideOutsideCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    /**
     * Identical to {@link CartesianProductHashSpmlParser}, but computes sum instead of viterbi max.
     */
    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final PackingFunction pf = grammar.packingFunction();
        final PackedArrayChartCell targetCell = (PackedArrayChartCell) cell;
        final short start = cell.start();
        final short end = cell.end();
        targetCell.allocateTemporaryStorage(HEURISTIC_OUTSIDE, false);

        final float[] targetCellProbabilities = targetCell.tmpCell.insideProbabilities;

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
                    final int column = pf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {

                        final float jointProbability = grammar.cscBinaryProbabilities[k] + childProbability;
                        final int parent = grammar.cscBinaryRowIndices[k];
                        if (APPROXIMATE_SUM) {
                            targetCellProbabilities[parent] = Math.approximateLogSum(targetCellProbabilities[parent],
                                    jointProbability, SUM_DELTA);
                        } else {
                            targetCellProbabilities[parent] = Math.logSum(targetCellProbabilities[parent],
                                    jointProbability, SUM_DELTA);
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
    protected final void unaryAndPruning(final ParallelArrayChartCell spvChartCell, final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        // For the moment, at least, we ignore factored-only cell constraints in span-1 cells
        final boolean factoredOnly = cellSelector.hasCellConstraints() && cellSelector.isCellOnlyFactored(start, end)
                && (end - start > 1);
        final boolean allowUnaries = !cellSelector.hasCellConstraints() || cellSelector.isUnaryOpen(start, end);
        final float minInsideProbability = edu.ohsu.cslu.util.Math.floatMax(spvChartCell.tmpCell.insideProbabilities)
                - maxLocalDelta;

        // We will push all binary or lexical edges onto a bounded priority queue, and then (if unaries are allowed),
        // add those edges as well.
        final int cellBeamWidth = (end - start == 1 ? lexicalRowBeamWidth : java.lang.Math.min(
                cellSelector.getBeamWidth(start, end), beamWidth));
        final BoundedPriorityQueue q = threadLocalBoundedPriorityQueue.get();
        q.clear(cellBeamWidth);

        final float[] maxInsideProbabilities = new float[grammar.numNonTerms()];
        System.arraycopy(spvChartCell.tmpCell.insideProbabilities, 0, maxInsideProbabilities, 0,
                maxInsideProbabilities.length);

        // If unaries are allowed in this cell, compute unary probabilities for all possible parents
        if (!factoredOnly && allowUnaries) {
            final float[] unaryInsideProbabilities = new float[grammar.numNonTerms()];
            Arrays.fill(unaryInsideProbabilities, Float.NEGATIVE_INFINITY);
            final float[] viterbiUnaryInsideProbabilities = new float[grammar.numNonTerms()];
            Arrays.fill(viterbiUnaryInsideProbabilities, Float.NEGATIVE_INFINITY);
            final int[] viterbiUnaryPackedChildren = new int[grammar.numNonTerms()];

            for (short child = 0; child < grammar.numNonTerms(); child++) {
                final float insideProbability = spvChartCell.tmpCell.insideProbabilities[child];
                if (insideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                    final float unaryProbability = grammar.cscUnaryProbabilities[i] + insideProbability;
                    final short parent = grammar.cscUnaryRowIndices[i];

                    unaryInsideProbabilities[parent] = Math.logSum(unaryInsideProbabilities[parent], unaryProbability);

                    if (unaryProbability > viterbiUnaryInsideProbabilities[parent]) {
                        viterbiUnaryInsideProbabilities[parent] = unaryProbability;
                        viterbiUnaryPackedChildren[parent] = grammar.packingFunction.packUnary(child);
                    }
                }
            }

            // Retain the greater of the binary and unary inside probabilities and the appropriate backpointer (biasing
            // toward recovering unaries in the case of a tie)
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (unaryInsideProbabilities[nt] != Float.NEGATIVE_INFINITY
                        && unaryInsideProbabilities[nt] >= maxInsideProbabilities[nt]) {
                    maxInsideProbabilities[nt] = unaryInsideProbabilities[nt];
                    spvChartCell.tmpCell.packedChildren[nt] = viterbiUnaryPackedChildren[nt];
                }
            }
        }

        // Push all observed edges (binary, unary, or lexical) onto a bounded priority queue
        if (end - start == 1) { // Lexical Row (span = 1)

            // Limit the queue to the number of non-unary productions allowed
            q.setMaxSize(lexicalRowBeamWidth - lexicalRowUnaries);

            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (maxInsideProbabilities[nt] > minInsideProbability) {
                    final float fom = figureOfMerit.calcLexicalFOM(start, end, nt, maxInsideProbabilities[nt]);
                    q.insert(nt, fom);
                }
            }
            // Now that all lexical productions are on the queue, expand it a bit to allow space for unary productions
            q.setMaxSize(lexicalRowBeamWidth);

        } else { // Span >= 2
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (maxInsideProbabilities[nt] > minInsideProbability) {
                    final float fom = figureOfMerit.calcFOM(start, end, nt, maxInsideProbabilities[nt]);
                    q.insert(nt, fom);
                }
            }
        }

        Arrays.fill(spvChartCell.tmpCell.insideProbabilities, Float.NEGATIVE_INFINITY);

        // Pop n edges off the queue into the temporary cell storage.
        for (final int edgesPopulated = 0; edgesPopulated < cellBeamWidth && q.size() > 0;) {

            final int headIndex = q.headIndex();
            final short nt = q.nts[headIndex];
            spvChartCell.tmpCell.insideProbabilities[nt] = maxInsideProbabilities[nt];
            if (HEURISTIC_OUTSIDE) {
                spvChartCell.tmpCell.outsideProbabilities[nt] = figureOfMerit.calcFOM(start, end, nt,
                        maxInsideProbabilities[nt]) - maxInsideProbabilities[nt];
            }
            q.popHead();
        }

        if (collectDetailedStatistics) {
            chart.parseTask.unaryAndPruningNs += System.nanoTime() - t0;
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

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    final float outsideProbability = grammar.cscBinaryProbabilities[k]
                            + parentOutsideProbabilities[parent] + siblingInsideProbability;
                    if (APPROXIMATE_SUM) {
                        outsideProbabilities[entry] = Math.logSum(outsideProbability, outsideProbabilities[entry],
                                SUM_DELTA);
                    } else {
                        outsideProbabilities[entry] = Math.logSum(outsideProbability, outsideProbabilities[entry],
                                SUM_DELTA);
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

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    final float outsideProbability = grammar.cscBinaryProbabilities[k]
                            + parentOutsideProbabilities[parent] + siblingInsideProbability;
                    if (APPROXIMATE_SUM) {
                        outsideProbabilities[entry] = Math.logSum(outsideProbability, outsideProbabilities[entry],
                                SUM_DELTA);
                    } else {
                        outsideProbabilities[entry] = Math.logSum(outsideProbability, outsideProbabilities[entry],
                                SUM_DELTA);
                    }
                }
            }
        }
    }
}
