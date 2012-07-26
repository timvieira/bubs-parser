package edu.ohsu.cslu.parser.ml;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
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
    protected final void unaryAndPruning(final PackedArrayChartCell spvChartCell, final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        // For the moment, at least, we ignore factored-only cell constraints in span-1 cells
        final boolean factoredOnly = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end) && (end - start > 1);
        final boolean allowUnaries = !cellSelector.hasCellConstraints()
                || cellSelector.getCellConstraints().isUnaryOpen(start, end);
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
                    final float fom = fomModel.calcLexicalFOM(start, end, nt, maxInsideProbabilities[nt]);
                    q.insert(nt, fom);
                }
            }
            // Now that all lexical productions are on the queue, expand it a bit to allow space for unary productions
            q.setMaxSize(lexicalRowBeamWidth);

        } else { // Span >= 2
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (maxInsideProbabilities[nt] > minInsideProbability) {
                    final float fom = fomModel.calcFOM(start, end, nt, maxInsideProbabilities[nt]);
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
            q.popHead();
        }

        if (collectDetailedStatistics) {
            chart.parseTask.unaryAndPruningNs += System.nanoTime() - t0;
        }
    }

    @Override
    protected void computeSiblingOutsideProbabilities(final PackedArrayChartCell cell, final PackingFunction pf,
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

                // Parent outside x sibling inside
                final float jointProbability = chart.outsideProbabilities[j] + siblingInsideProbability;

                // foreach grammar rule matching sibling/parent pair (i.e., those which can produce entries in
                // the target cell).
                for (int k = cscColumnOffsets[column]; k < cscColumnOffsets[column + 1]; k++) {

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    final float outsideProbability = cscBinaryProbabilities[k] + jointProbability;
                    final int target = cscBinaryRowIndices[k];
                    final float outsideSum = Math.logSum(outsideProbability, cell.tmpCell.outsideProbabilities[target]);
                    cell.tmpCell.outsideProbabilities[target] = outsideSum;
                }
            }
        }
    }
}
