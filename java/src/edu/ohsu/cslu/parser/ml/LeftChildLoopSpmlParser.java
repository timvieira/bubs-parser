package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.CscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserOptions;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;

/**
 * Left-child loop exhaustive parser using a sparse-matrix grammar representation (
 * {@link CscSparseMatrixGrammar}).
 * 
 * @author Aaron Dunlop
 * @since Jun 13, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class LeftChildLoopSpmlParser extends SparseMatrixLoopParser<CscSparseMatrixGrammar, DenseVectorChart> {

    public LeftChildLoopSpmlParser(final ParserOptions opts, final CscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    public LeftChildLoopSpmlParser(final CscSparseMatrixGrammar grammar) {
        this(new ParserOptions(), grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            chart = new DenseVectorChart(sentLength, grammar);
        }
        super.initParser(sentLength);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final DenseVectorChartCell targetCell = chart.getCell(start, end);
        final int targetCellOffset = targetCell.offset();

        // Local copies of chart storage. These shouldn't really be necessary, but the JIT doesn't always
        // figure out to inline remote references.
        final float[] chartInsideProbabilities = chart.insideProbabilities;
        final int[] chartPackedChildren = chart.packedChildren;
        final short[] chartMidpoints = chart.midpoints;

        final int v = grammar.numNonTerms();

        // Iterate over all possible midpoints
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final DenseVectorChartCell leftChildCell = chart.getCell(start, midpoint);
            final DenseVectorChartCell rightChildCell = chart.getCell(midpoint, end);

            final int leftCellOffset = leftChildCell.offset();
            final int rightCellOffset = rightChildCell.offset();

            // Iterate over children in the left child cell
            for (int leftChild = 0; leftChild < v; leftChild++) {
                final float leftInsideProbability = chartInsideProbabilities[leftCellOffset + leftChild];
                if (leftInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over all grammar rules with `leftChild' as the left child
                for (int j = grammar.cscBinaryLeftChildStartIndices[leftChild]; j <= grammar.cscBinaryLeftChildEndIndices[leftChild]
                        && j >= 0; j++) {

                    // Unpack the grammar rule's right child
                    final int packedChildPair = grammar.cscBinaryPopulatedColumns[j];
                    final int rightChild = grammar.cartesianProductFunction().unpackRightChild(
                        packedChildPair);

                    // Look up the right child NT's probability in the right child cell
                    final float rightInsideProbability = chartInsideProbabilities[rightCellOffset
                            + rightChild];

                    if (rightInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }
                    final float childProbability = leftInsideProbability + rightInsideProbability;

                    for (int entryIndex = grammar.cscBinaryPopulatedColumnOffsets[j]; entryIndex < grammar.cscBinaryPopulatedColumnOffsets[j + 1]; entryIndex++) {
                        final float jointProbability = childProbability
                                + grammar.cscBinaryProbabilities[entryIndex];
                        final int parent = grammar.cscBinaryRowIndices[entryIndex];

                        final int targetCellParentIndex = targetCellOffset + parent;

                        if (jointProbability > chartInsideProbabilities[targetCellParentIndex]) {
                            chartInsideProbabilities[targetCellParentIndex] = jointProbability;
                            chartPackedChildren[targetCellParentIndex] = packedChildPair;
                            chartMidpoints[targetCellParentIndex] = midpoint;
                        }
                    }
                }

                // grammar.binaryLeftChildStartIndices()
                // for (final int j = 0; j <= v; j++) {
                // final float rightInsideProbability = insideProbabilities[rightStart + j];
                // if (rightInsideProbability == Float.NEGATIVE_INFINITY) {
                // continue;
                // }
                // }
            }
        }

        // Apply unary rules
        applyUnaryRuleMatrix(targetCell);
    }
}
