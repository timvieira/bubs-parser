package edu.ohsu.cslu.parser.ml;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * Parser implementation which loops over all combinations of left and right child cell populations (cartesian product
 * of observed left and right non-terminals) and probes into the grammar for each combination using a binary search.
 * 
 * @author Aaron Dunlop
 * @since Jun 14, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CartesianProductBinarySearchSpmlParser extends
        SparseMatrixLoopParser<LeftCscSparseMatrixGrammar, PackedArrayChart> {

    public CartesianProductBinarySearchSpmlParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initParser(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            // TODO Consolidate chart construction in a superclass using the genericized grammar
            chart = new PackedArrayChart(tokens, grammar);
        }
        super.initParser(tokens);
    }

    protected int binarySearchStart(final int leftChild) {
        return 0;
    }

    protected int binarySearchEnd(final int leftChild) {
        return grammar.cscBinaryPopulatedColumns.length;
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final CartesianProductFunction cpf = grammar.cartesianProductFunction();
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
                final int leftChild = chart.nonTerminalIndices[i];
                final int binarySearchStart = binarySearchStart(leftChild);

                // Skip non-terminals which never occur as left children
                if (binarySearchStart < 0) {
                    continue;
                }

                final int binarySearchEnd = binarySearchEnd(leftChild);
                final float leftProbability = chart.insideProbabilities[i];

                // And over children in the right child cell
                for (int j = rightStart; j <= rightEnd; j++) {

                    final int childPair = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (childPair == Integer.MIN_VALUE) {
                        continue;
                    }

                    // Search in the grammar for the child pair
                    final int index = Arrays.binarySearch(grammar.cscBinaryPopulatedColumns, binarySearchStart,
                            binarySearchEnd, childPair);
                    if (index < 0) {
                        continue;
                    }

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = grammar.cscBinaryPopulatedColumnOffsets[index]; k < grammar.cscBinaryPopulatedColumnOffsets[index + 1]; k++) {

                        final float jointProbability = grammar.cscBinaryProbabilities[k] + childProbability;
                        final int parent = grammar.cscBinaryRowIndices[k];

                        if (jointProbability > targetCellProbabilities[parent]) {
                            targetCellChildren[parent] = childPair;
                            targetCellProbabilities[parent] = jointProbability;
                            targetCellMidpoints[parent] = midpoint;
                        }
                    }
                }
            }
        }

        // Apply unary rules
        unarySpmv(targetCell);

        targetCell.finalizeCell();
    }
}
