package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.hash.PerfectIntPair2IntHash;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * Parser implementation which loops over all combinations of left and right child cell populations (cartesian product
 * of observed left and right non-terminals) and probes into the grammar for each combination using a lookup into a
 * segmented perfect hash.
 * 
 * @author Aaron Dunlop
 * @since Jun 14, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CartesianProductLeftChildHashSpmlParser extends
        SparseMatrixLoopParser<LeftCscSparseMatrixGrammar, PackedArrayChart> {

    private final PerfectIntPair2IntHash childPair2ColumnOffsetHash;
    private final int[] hashedCscParallelArrayIndices;

    public CartesianProductLeftChildHashSpmlParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);

        final int[][] keyPairs = new int[2][grammar.cscBinaryPopulatedColumns.length];
        for (int i = 0; i < grammar.cscBinaryPopulatedColumns.length; i++) {
            keyPairs[0][i] = grammar.cartesianProductFunction().unpackLeftChild(grammar.cscBinaryPopulatedColumns[i]);
            keyPairs[1][i] = grammar.cartesianProductFunction().unpackRightChild(grammar.cscBinaryPopulatedColumns[i]);
        }

        childPair2ColumnOffsetHash = new PerfectIntPair2IntHash(keyPairs);
        hashedCscParallelArrayIndices = new int[childPair2ColumnOffsetHash.hashtableSize()];
        for (int i = 0; i < keyPairs[0].length; i++) {
            hashedCscParallelArrayIndices[childPair2ColumnOffsetHash.hashcode(keyPairs[0][i], keyPairs[1][i])] = i;
        }
    }

    @Override
    protected void initSentence(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            // TODO Consolidate chart construction in a superclass using the genericized grammar
            chart = new PackedArrayChart(tokens, grammar);
        }
        super.initSentence(tokens);
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

                        if (jointProbability > targetCellProbabilities[parent]) {
                            targetCellChildren[parent] = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
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
