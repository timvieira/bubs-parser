package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * Parses using Sparse Matrix-Vector multiplication, but performs the SpMV at each midpoint instead of first unioning
 * the cartesian products and performing the SpMV once per cell.
 * 
 * @author Aaron Dunlop
 * @since Apr 13, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CsrSpmvPerMidpointParser extends CsrSpmvParser {

    public CsrSpmvPerMidpointParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final PackedArrayChartCell packedArrayCell = chart.getCell(start, end);

        packedArrayCell.allocateTemporaryStorage();

        final int[] chartCellChildren = packedArrayCell.tmpPackedChildren;
        final float[] chartCellProbabilities = packedArrayCell.tmpInsideProbabilities;
        final short[] chartCellMidpoints = packedArrayCell.tmpMidpoints;

        final int[] tmpChildren = new int[grammar.numNonTerms()];
        final float[] tmpProbabilities = new float[tmpChildren.length];
        final short[] tmpMidpoints = new short[tmpChildren.length];
        Arrays.fill(tmpProbabilities, Float.NEGATIVE_INFINITY);

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            // Iterate over all possible midpoints, unioning together the cross-product of discovered
            // non-terminals in each left/right child pair
            for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
                final long t0 = System.currentTimeMillis();

                final CartesianProductVector cartesianProductVector = cartesianProduct(start, end, midpoint);
                totalCartesianProductSize += cartesianProductVector.size();

                final long t1 = System.currentTimeMillis();
                totalCartesianProductTime += t1 - t0;

                // Multiply the unioned vector with the grammar matrix and populate the current cell with the
                // vector resulting from the matrix-vector multiplication

                binarySpmvMultiply(cartesianProductVector, tmpChildren, tmpProbabilities, tmpMidpoints);
                maxUnion(chartCellChildren, chartCellProbabilities, chartCellMidpoints, tmpChildren, tmpProbabilities,
                        tmpMidpoints);
                totalBinarySpMVTime += (System.currentTimeMillis() - t1);
            }
        }

        final long t3 = System.currentTimeMillis();
        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        unarySpmv(packedArrayCell);

        final long t4 = System.currentTimeMillis();
        totalUnaryTime += (t4 - t3);

        // Pack the temporary cell storage into the main chart array
        packedArrayCell.finalizeCell();

        totalFinalizeTime += (System.currentTimeMillis() - t4);
    }

    private void maxUnion(final int[] targetChildren, final float[] targetProbabilities, final short[] targetMidpoints,
            final int[] children2, final float[] probabilities2, final short[] midpoints2) {
        for (int i = 0; i < targetChildren.length; i++) {
            if (probabilities2[i] > targetProbabilities[i]) {
                targetChildren[i] = children2[i];
                targetProbabilities[i] = probabilities2[i];
                targetMidpoints[i] = midpoints2[i];
            }
        }
    }

    protected CartesianProductVector cartesianProduct(final int start, final int end, final short midpoint) {

        Arrays.fill(cartesianProductProbabilities, Float.NEGATIVE_INFINITY);
        int size = 0;

        final PackingFunction cpf = grammar.cartesianProductFunction();

        final PackedArrayChartCell leftCell = chart.getCell(start, midpoint);
        final PackedArrayChartCell rightCell = chart.getCell(midpoint, end);

        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        for (int i = leftCell.minLeftChildIndex(); i <= leftCell.maxLeftChildIndex(); i++) {

            final short leftChild = nonTerminalIndices[i];
            final float leftProbability = insideProbabilities[i];

            for (int j = rightCell.offset(); j <= rightCell.maxRightChildIndex(); j++) {

                final int childPair = cpf.pack(leftChild, nonTerminalIndices[j]);
                if (childPair == Integer.MIN_VALUE) {
                    continue;
                }

                totalCartesianProductEntriesExamined++;
                cartesianProductProbabilities[childPair] = leftProbability + insideProbabilities[j];
                cartesianProductMidpoints[childPair] = midpoint;
                size++;
            }
        }

        return new CartesianProductVector(grammar, cartesianProductProbabilities, cartesianProductMidpoints, size);
    }

}
