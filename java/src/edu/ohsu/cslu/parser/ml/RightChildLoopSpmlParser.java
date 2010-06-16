package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserOptions;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;

/**
 * Right-child loop exhaustive parser using a sparse-matrix grammar representation (
 * {@link RightCscSparseMatrixGrammar}).
 * 
 * @author Aaron Dunlop
 * @since Jun 13, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class RightChildLoopSpmlParser extends
        SparseMatrixLoopParser<RightCscSparseMatrixGrammar, DenseVectorChart> {

    public RightChildLoopSpmlParser(final ParserOptions opts, final RightCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    public RightChildLoopSpmlParser(final RightCscSparseMatrixGrammar grammar) {
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

        final int v = grammar.numNonTerms();

        // Iterate over all possible midpoints
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final DenseVectorChartCell leftChildCell = chart.getCell(start, midpoint);
            final DenseVectorChartCell rightChildCell = chart.getCell(midpoint, end);

            final int leftCellOffset = leftChildCell.offset();
            final int rightCellOffset = rightChildCell.offset();

            // Iterate over children in the right child cell
            for (int rightChild = 0; rightChild < v; rightChild++) {
                final float rightInsideProbability = chart.insideProbabilities[rightCellOffset + rightChild];
                if (rightInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over all grammar rules with `leftChild' as the left child
                for (int j = grammar.cscBinaryRightChildStartIndices[rightChild]; j <= grammar.cscBinaryRightChildEndIndices[rightChild]
                        && j >= 0; j++) {

                    // Unpack the grammar rule's right child
                    final int packedChildPair = grammar.cscBinaryPopulatedColumns[j];
                    final int leftChild = grammar.cartesianProductFunction().unpackLeftChild(packedChildPair);

                    // Look up the right child NT's probability in the right child cell
                    final float leftInsideProbability = chart.insideProbabilities[leftCellOffset + leftChild];

                    if (leftInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }
                    final float childProbability = leftInsideProbability + rightInsideProbability;

                    for (int entryIndex = grammar.cscBinaryPopulatedColumnOffsets[j]; entryIndex < grammar.cscBinaryPopulatedColumnOffsets[j + 1]; entryIndex++) {
                        final float jointProbability = childProbability
                                + grammar.cscBinaryProbabilities[entryIndex];
                        final int parent = grammar.cscBinaryRowIndices[entryIndex];

                        final int targetCellParentIndex = targetCellOffset + parent;

                        if (jointProbability > chart.insideProbabilities[targetCellParentIndex]) {
                            chart.insideProbabilities[targetCellParentIndex] = jointProbability;
                            chart.packedChildren[targetCellParentIndex] = packedChildPair;
                            chart.midpoints[targetCellParentIndex] = midpoint;
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
