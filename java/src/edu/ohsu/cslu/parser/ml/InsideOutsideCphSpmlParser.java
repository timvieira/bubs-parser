package edu.ohsu.cslu.parser.ml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.InsideOutsideChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.util.Math;

public class InsideOutsideCphSpmlParser extends
        SparseMatrixLoopParser<InsideOutsideCscSparseMatrixGrammar, InsideOutsideChart> {

    public InsideOutsideCphSpmlParser(final ParserDriver opts, final InsideOutsideCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    public BinaryTree<String> findBestParse(final int[] tokens) {
        init(tokens);
        insidePass();

        // Outside pass
        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

        final Iterator<short[]> reverseIterator = cellSelector.reverseIterator();

        // Populate start-symbol unaries in the top cell. Assume that the top cell will be first; this might not be
        // exactly be the reverse of the original iteration order (e.g., for an agenda parser), but there isn't another
        // sensible order in which to compute outside probabilities.
        final float[] tmpOutsideProbabilities = new float[grammar.numNonTerms()];
        Arrays.fill(tmpOutsideProbabilities, Float.NEGATIVE_INFINITY);
        tmpOutsideProbabilities[grammar.startSymbol] = 0;

        while (reverseIterator.hasNext()) {
            final short[] startAndEnd = reverseIterator.next();
            computeOutsideProbabilities(startAndEnd[0], startAndEnd[1], tmpOutsideProbabilities);
            Arrays.fill(tmpOutsideProbabilities, Float.NEGATIVE_INFINITY);
        }
        if (collectDetailedStatistics) {
            currentInput.outsidePassMs = System.currentTimeMillis() - t0;
        }

        // For now, we always perform Goodman's max-recall decoding. We also need max-precision and a trade-off between
        // the two
        if (collectDetailedStatistics) {
            final long t3 = System.currentTimeMillis();
            chart.computeMaxc();
            final BinaryTree<String> parseTree = chart.extractMaxcParse(0, chart.size());
            currentInput.extractTimeMs = System.currentTimeMillis() - t3;
            return parseTree;
        }

        try {
            final FileWriter fw = new FileWriter("/tmp/io_chart.txt");
            fw.write(chart.toString());
            fw.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        chart.computeMaxc();
        return chart.extractMaxcParse(0, chart.size());
    }

    /**
     * Identical to {@link CartesianProductHashSpmlParser}, but computes sum instead of viterbi max.
     */
    @Override
    protected void computeInsideProbabilities(final short start, final short end) {

        final PackingFunction cpf = grammar.cartesianProductFunction();
        final PackedArrayChartCell targetCell = chart.getCell(start, end);
        targetCell.allocateTemporaryStorage();

        final int[] targetCellChildren = targetCell.tmpPackedChildren;
        final float[] targetCellProbabilities = targetCell.tmpInsideProbabilities;
        final short[] targetCellMidpoints = targetCell.tmpMidpoints;

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
                    final int column = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {

                        final float jointProbability = grammar.cscBinaryProbabilities[k] + childProbability;
                        final int parent = grammar.cscBinaryRowIndices[k];
                        targetCellProbabilities[parent] = Math
                                .logSum(targetCellProbabilities[parent], jointProbability);

                        // Keep track of viterbi best-path backpointers, even though we don't really have to.
                        if (jointProbability > maxInsideProbabilities[parent]) {
                            targetCellChildren[parent] = column;
                            maxInsideProbabilities[parent] = jointProbability;
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

    /**
     * To compute the outside probability of a non-terminal in a cell, we need the outside probability of the cell's
     * parent, so we process downward from the top of the chart.
     * 
     * @param start
     * @param end
     */
    private void computeOutsideProbabilities(final short start, final short end, final float[] tmpOutsideProbabilities) {

        // Left-side siblings first

        // foreach parent-start in {0..start - 1}
        for (int parentStart = 0; parentStart < start; parentStart++) {
            final int parentCellIndex = chart.cellIndex(parentStart, end);
            final int parentStartIndex = chart.offset(parentCellIndex);
            final int parentEndIndex = parentStartIndex + chart.numNonTerminals()[parentCellIndex] - 1;

            // Sibling (left) cell
            final int siblingCellIndex = chart.cellIndex(parentStart, start);
            final int siblingStartIndex = chart.minLeftChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxLeftChildIndex(siblingCellIndex);

            computeSiblingOutsideProbabilities(tmpOutsideProbabilities, grammar.rightChildPackingFunction,
                    grammar.rightChildCscBinaryProbabilities, grammar.rightChildCscBinaryRowIndices,
                    grammar.rightChildCscBinaryColumnOffsets, parentStartIndex, parentEndIndex, siblingStartIndex,
                    siblingEndIndex);
        }

        // Right-side siblings

        // foreach parent-end in {end + 1..n}
        for (int parentEnd = end + 1; parentEnd <= chart.size(); parentEnd++) {
            final int parentCellIndex = chart.cellIndex(start, parentEnd);
            final int parentStartIndex = chart.offset(parentCellIndex);
            final int parentEndIndex = parentStartIndex + chart.numNonTerminals()[parentCellIndex] - 1;

            // Sibling (right) cell
            final int siblingCellIndex = chart.cellIndex(end, parentEnd);
            final int siblingStartIndex = chart.minRightChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxRightChildIndex(siblingCellIndex);

            computeSiblingOutsideProbabilities(tmpOutsideProbabilities, grammar.leftChildPackingFunction,
                    grammar.leftChildCscBinaryProbabilities, grammar.leftChildCscBinaryRowIndices,
                    grammar.leftChildCscBinaryColumnOffsets, parentStartIndex, parentEndIndex, siblingStartIndex,
                    siblingEndIndex);
        }

        // Unary outside probabilities
        computeUnaryOutsideProbabilities(tmpOutsideProbabilities);

        chart.finalizeOutside(tmpOutsideProbabilities, chart.offset(chart.cellIndex(start, end)));
    }

    private void computeSiblingOutsideProbabilities(final float[] tmpOutsideProbabilities, final PackingFunction cpf,
            final float[] cscBinaryProbabilities, final short[] cscBinaryRowIndices, final int[] cscColumnOffsets,
            final int parentStartIndex, final int parentEndIndex, final int siblingStartIndex, final int siblingEndIndex) {

        // foreach entry in the sibling cell
        for (int i = siblingStartIndex; i <= siblingEndIndex; i++) {
            final short siblingEntry = chart.nonTerminalIndices[i];
            final float siblingInsideProbability = chart.insideProbabilities[i];

            // foreach entry in the parent cell
            for (int j = parentStartIndex; j <= parentEndIndex; j++) {

                final int column = cpf.pack(chart.nonTerminalIndices[j], siblingEntry);
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

    private void computeUnaryOutsideProbabilities(final float[] tmpOutsideProbabilities) {

        // Iterate over populated parents (matrix rows)
        for (short parent = 0; parent < grammar.numNonTerms(); parent++) {

            if (tmpOutsideProbabilities[parent] == Float.NEGATIVE_INFINITY) {
                continue;
            }

            // Iterate over possible children (columns with non-zero entries)
            for (int i = grammar.csrUnaryRowStartIndices[parent]; i < grammar.csrUnaryRowStartIndices[parent + 1]; i++) {

                final short child = grammar.csrUnaryColumnIndices[i];
                final float jointProbability = grammar.csrUnaryProbabilities[i] + tmpOutsideProbabilities[parent];
                tmpOutsideProbabilities[child] = Math.logSum(tmpOutsideProbabilities[child], jointProbability);
            }
        }
    }
}
