package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;

/**
 * Grammar loop exhaustive parser using a sparse-matrix grammar representation ( {@link CsrSparseMatrixGrammar}). Loops
 * over the entire grammar for each midpoint, probing child cells for non-terminals matching each grammar rule.
 * 
 * @author Aaron Dunlop
 * @since Jun 13, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class GrammarLoopSpmlParser extends SparseMatrixLoopParser<CsrSparseMatrixGrammar, DenseVectorChart> {

    public GrammarLoopSpmlParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initSentence(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            chart = new DenseVectorChart(tokens, grammar);
        }
        super.initSentence(tokens);
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

            // Iterate over possible parents (matrix rows)
            for (int parent = 0; parent < v; parent++) {

                // Iterate over possible children of the parent (columns with non-zero entries)
                for (int i = grammar.csrBinaryRowIndices[parent]; i < grammar.csrBinaryRowIndices[parent + 1]; i++) {
                    final int packedChildPair = grammar.csrBinaryColumnIndices[i];

                    final int leftChild = grammar.cartesianProductFunction().unpackLeftChild(packedChildPair);
                    final int rightChild = grammar.cartesianProductFunction().unpackRightChild(packedChildPair);

                    final float leftInsideProbability = chartInsideProbabilities[leftCellOffset + leftChild];
                    if (leftInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }
                    final float rightInsideProbability = chartInsideProbabilities[rightCellOffset + rightChild];
                    if (rightInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    final float jointProbability = leftInsideProbability + rightInsideProbability
                            + grammar.csrBinaryProbabilities[i];

                    final int targetCellParentIndex = targetCellOffset + parent;

                    if (jointProbability > chartInsideProbabilities[targetCellParentIndex]) {
                        chartInsideProbabilities[targetCellParentIndex] = jointProbability;
                        chartPackedChildren[targetCellParentIndex] = packedChildPair;
                        chartMidpoints[targetCellParentIndex] = midpoint;
                    }
                }
            }
        }

        // Apply unary rules
        unarySpmv(targetCell);
    }
}
