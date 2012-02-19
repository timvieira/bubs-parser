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

import java.util.Arrays;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.lela.ConstrainingChart;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;

/**
 * Produces a parse tree constrained by the gold input tree. The resulting parse will be identical to the gold input
 * tree, but split categories will be populated. E.g., NP_12 might be populated in place of NP.
 * 
 * Implementation notes:
 * 
 * This parser cannot recover unary self-chains (e.g. NP -> NP -> ...), because {@link PackedArrayChart} can only store
 * a single instance of a non-terminal in a cell. Such self-chains are fairly rare in the corpora of interest, and
 * should be pruned through a pre-processing step.
 * 
 * This implementation is quite simple and does not optimize efficiency, but constrained parsing should be quite fast
 * even so, and we can revisit efficiency if needed.
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedCphSpmlParser extends SparseMatrixLoopParser<LeftCscSparseMatrixGrammar, PackedArrayChart>
        implements ConstrainedChartParser {

    private ConstrainingChart constrainingChart;

    final LeftCscSparseMatrixGrammar baseGrammar;

    /**
     * @param opts
     * @param grammar
     */
    public ConstrainedCphSpmlParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
        this.beamWidth = lexicalRowBeamWidth = grammar.numNonTerms();
        baseGrammar = (LeftCscSparseMatrixGrammar) grammar.toUnsplitGrammar();
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);
        constrainingChart = new ConstrainingChart(parseTask.inputTree.binarize(grammar.grammarFormat,
                grammar.binarization()), baseGrammar);
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final PackedArrayChartCell targetCell = (PackedArrayChartCell) cell;
        final short start = cell.start();
        final short end = cell.end();
        targetCell.allocateTemporaryStorage();
        final TemporaryChartCell tmpCell = targetCell.tmpCell;

        final boolean factoredOnly = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end);

        final int[] binaryColumnOffsets = factoredOnly ? grammar.factoredCscBinaryColumnOffsets
                : grammar.cscBinaryColumnOffsets;
        final float[] binaryProbabilities = factoredOnly ? grammar.factoredCscBinaryProbabilities
                : grammar.cscBinaryProbabilities;
        final short[] binaryRowIndices = factoredOnly ? grammar.factoredCscBinaryRowIndices
                : grammar.cscBinaryRowIndices;

        final PackingFunction cpf = grammar.cartesianProductFunction();
        final PackedArrayChartCell constrainingCell = constrainingChart.getCell(start, end);
        final int constrainingCellIndex = constrainingChart.cellIndex(start, end);
        final int constrainingCellOffset = constrainingChart.offset(constrainingCellIndex);

        final short midpoint = constrainingChart.midpoints[constrainingCellIndex];

        final int leftCellIndex = chart.cellIndex(start, midpoint);
        final int rightCellIndex = chart.cellIndex(midpoint, end);

        // Iterate over children in the left child cell
        final int leftStart = chart.minLeftChildIndex(leftCellIndex);
        final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);

        final int rightStart = chart.minRightChildIndex(rightCellIndex);
        final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

        for (int i = leftStart; i <= leftEnd; i++) {
            final short leftChild = chart.nonTerminalIndices[i];
            final short baseLeftChild = grammar.nonTermSet.getBaseIndex(leftChild);
            final float leftProbability = chart.insideProbabilities[i];

            // And over children in the right child cell
            for (int j = rightStart; j <= rightEnd; j++) {
                final short rightChild = chart.nonTerminalIndices[j];
                final short baseRightChild = grammar.nonTermSet.getBaseIndex(rightChild);
                final int column = cpf.pack(leftChild, rightChild);

                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                final float childProbability = leftProbability + chart.insideProbabilities[j];

                for (int k = binaryColumnOffsets[column]; k < binaryColumnOffsets[column + 1]; k++) {

                    final float jointProbability = binaryProbabilities[k] + childProbability;
                    final short parent = binaryRowIndices[k];
                    final short baseParent = grammar.nonTermSet.getBaseIndex(parent);

                    // Skip this edge if it doesn't match the constraining edge
                    final int constrainingIndex = Arrays.binarySearch(constrainingChart.nonTerminalIndices,
                            constrainingCellOffset, constrainingCellOffset
                                    + constrainingChart.numNonTerminals[constrainingCellIndex], baseParent);
                    if (constrainingIndex < 0
                            || baseGrammar.packingFunction
                                    .unpackLeftChild(constrainingChart.packedChildren[constrainingIndex]) != baseLeftChild
                            || baseGrammar.packingFunction
                                    .unpackRightChild(constrainingChart.packedChildren[constrainingIndex]) != baseRightChild) {
                        continue;
                    }

                    if (jointProbability > tmpCell.insideProbabilities[parent]) {
                        tmpCell.packedChildren[parent] = column;
                        tmpCell.insideProbabilities[parent] = jointProbability;
                        tmpCell.midpoints[parent] = midpoint;
                    }
                }
            }
        }

        if (collectDetailedStatistics) {
            chart.parseTask.insideBinaryNs += System.nanoTime() - t0;
        }

        // Apply unary rules
        unaryAndPruning(targetCell, constrainingCell, start, end);

        // We need to ensure that one or more splits of the constraining non-terminal are populated in the target cell.
        // If the cell is empty, we must choose one or more such splits; without a principled way to choose between
        // those splits, we populate _all_ splits matching the constraining edge, assign probability 1 to the sum of
        // those splits, and divide the probability mass between them. This operation is in effect hallucinating
        // additional grammar rules. The proper children for those rules is also unknown, so we simply choose the first
        // populated child in the child cells.

        // final short constrainingNt = constrainingChart.nonTerminalIndices[constrainingCellIndex];
        // final short leftChild = constrainingNt = constrainingChart.nonTerminalIndices[constrainingCellIndex];
        // int count = 0;
        // for (short nt = 0; nt < grammar.nonTermSet.size(); nt++) {
        // if (grammar.nonTermSet.getBaseIndex(nt) == constrainingNt) {
        // count++;
        // }
        // }

        targetCell.finalizeCell();
    }

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely), and
     * populates this chart cell. Used to populate unary rules.
     * 
     * @param chartCell
     */
    protected void unaryAndPruning(final PackedArrayChartCell chartCell, final PackedArrayChartCell constrainingCell,
            final short start, final short end) {

        chartCell.allocateTemporaryStorage();
        final int[] chartCellChildren = chartCell.tmpCell.packedChildren;
        final short[] chartCellMidpoints = chartCell.tmpCell.midpoints;
        final float[] chartCellProbabilities = chartCell.tmpCell.insideProbabilities;

        // // Remove any non-terminals populated in tmpCell that do not match the constraining cell
        // // TODO This check could be considerably more efficient
        // for (short nt = 0; nt < chartCellProbabilities.length; nt++) {
        // final short baseNt = grammar.nonTermSet.getBaseIndex(nt);
        //
        // if (constrainingCell.getInside(baseNt) == Float.NEGATIVE_INFINITY
        // || constrainingCell.getMidpoint(baseNt) != chartCellMidpoints[nt]) {
        // chartCellProbabilities[nt] = Float.NEGATIVE_INFINITY;
        // }
        // }

        final PackingFunction cpf = grammar.cartesianProductFunction();

        final int constrainingCellIndex = constrainingChart.cellIndex(start, end);
        final int constrainingCellOffset = constrainingChart.offset(constrainingCellIndex);

        // Iterate over populated children (matrix columns)
        for (short child = 0; child < grammar.numNonTerms(); child++) {

            if (chartCellProbabilities[child] == Float.NEGATIVE_INFINITY) {
                continue;
            }
            final short baseChild = grammar.nonTermSet.getBaseIndex(child);

            // Iterate over possible parents of the child (rows with non-zero entries)
            for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                final short parent = grammar.cscUnaryRowIndices[i];
                final float grammarProbability = grammar.cscUnaryProbabilities[i];
                final short baseParent = grammar.nonTermSet.getBaseIndex(parent);

                // Skip this edge if it doesn't match the constraining edge
                final int constrainingIndex = Arrays.binarySearch(constrainingChart.nonTerminalIndices,
                        constrainingCellOffset, constrainingCellOffset
                                + constrainingChart.numNonTerminals[constrainingCellIndex], baseParent);
                if (constrainingIndex < 0
                        || baseGrammar.packingFunction
                                .unpackLeftChild(constrainingChart.packedChildren[constrainingIndex]) != baseChild) {
                    continue;
                }

                final float jointProbability = grammarProbability + chartCellProbabilities[child];
                if (jointProbability > chartCellProbabilities[parent]) {
                    chartCellProbabilities[parent] = jointProbability;
                    chartCellChildren[parent] = cpf.packUnary(child);
                    chartCellMidpoints[parent] = end;
                }
            }
        }
    }

    @Override
    protected boolean implicitPruning() {
        return true;
    }

    @Override
    public ConstrainingChart constrainingChart() {
        return constrainingChart;
    }
}
