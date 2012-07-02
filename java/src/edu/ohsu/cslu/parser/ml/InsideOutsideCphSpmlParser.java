package edu.ohsu.cslu.parser.ml;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.util.Math;

/**
 * Populates the parse chart with inside-outside probabilities, summing probability mass for each nonterminal from all
 * applicable grammar rules.
 * 
 * @see {@link ViterbiInOutCphSpmlParser}
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
        targetCell.allocateTemporaryStorage();

        final float[] targetCellProbabilities = targetCell.tmpCell.insideProbabilities;

        final float[] maxInsideProbabilities = new float[targetCellProbabilities.length];
        Arrays.fill(maxInsideProbabilities, Float.NEGATIVE_INFINITY);

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
                        targetCellProbabilities[parent] = Math
                                .logSum(targetCellProbabilities[parent], jointProbability);
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
    protected void computeSiblingOutsideProbabilities(final float[] tmpOutsideProbabilities, final PackingFunction pf,
            final float[] cscBinaryProbabilities, final short[] cscBinaryRowIndices, final int[] cscColumnOffsets,
            final int parentStartIndex, final int parentEndIndex, final int siblingStartIndex, final int siblingEndIndex) {

        // foreach entry in the sibling cell
        for (int i = siblingStartIndex; i <= siblingEndIndex; i++) {
            final short siblingEntry = chart.nonTerminalIndices[i];
            final float siblingInsideProbability = chart.insideProbabilities[i];

            // foreach entry in the parent cell
            for (int j = parentStartIndex; j <= parentEndIndex; j++) {

                final int column = pf.pack(chart.nonTerminalIndices[j], siblingEntry);
                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                final float jointProbability = siblingInsideProbability + chart.outsideProbabilities[j];

                // foreach grammar rule matching sibling/parent pair (i.e., those which can produce entries in
                // the target cell).
                // TODO Constrain this iteration to entries with non-0 inside probability (e.g. with a merge
                // with insideProbability array)?
                for (int k = cscColumnOffsets[column]; k < cscColumnOffsets[column + 1]; k++) {

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    final float outsideProbability = cscBinaryProbabilities[k] + jointProbability;
                    final int target = cscBinaryRowIndices[k];
                    final float outsideSum = Math.logSum(outsideProbability, tmpOutsideProbabilities[target]);
                    tmpOutsideProbabilities[target] = outsideSum;
                }
            }
        }
    }
}
