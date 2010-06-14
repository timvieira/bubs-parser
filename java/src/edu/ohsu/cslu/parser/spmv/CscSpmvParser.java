package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.CscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.parser.ParserOptions;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSC format (
 * {@link CscSparseMatrixGrammar}) and implements cross-product and SpMV multiplication in Java.
 * 
 * @see CsrSpmvParser
 * @see OpenClSpmvParser
 * 
 *      TODO Share code copied from {@link CsrSpmvParser}
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CscSpmvParser extends SparseMatrixVectorParser<CscSparseMatrixGrammar, PackedArrayChart> {
    protected int totalCartesianProductSize;
    protected long totalCartesianProductEntriesExamined;
    protected long totalValidCartesianProductEntries;

    public CscSpmvParser(final ParserOptions opts, final CscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    public CscSpmvParser(final CscSparseMatrixGrammar grammar) {
        this(new ParserOptions().setCollectDetailedStatistics(true), grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        chart = new PackedArrayChart(sentLength, grammar);
        totalCartesianProductSize = 0;
        totalCartesianProductEntriesExamined = 0;
        totalValidCartesianProductEntries = 0;
        super.initParser(sentLength);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final PackedArrayChartCell spvChartCell = chart.getCell(start, end);

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        long cartesianProductTime = 0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);

            if (collectDetailedStatistics) {
                totalCartesianProductSize += cartesianProductVector.size();
            }

            t1 = System.currentTimeMillis();
            cartesianProductTime = t1 - t0;

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmvMultiply(cartesianProductVector, spvChartCell);
        }
        final long t2 = System.currentTimeMillis();
        final long binarySpmvTime = t2 - t1;

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        unarySpmvMultiply(spvChartCell);

        final long t3 = System.currentTimeMillis();
        final long unarySpmvTime = t3 - t2;

        // Pack the temporary cell storage into the main chart array
        spvChartCell.finalizeCell();

        totalCartesianProductTime += cartesianProductTime;
        totalSpMVTime += binarySpmvTime + unarySpmvTime;
    }

    /**
     * Takes the cartesian-product of all potential child-cell combinations. Unions those cartesian-products
     * together, saving the maximum probability child combinations.
     * 
     * TODO Share with {@link CsrSpmvParser}
     * 
     * @param start
     * @param end
     * @return Unioned cartesian-product
     */
    @Override
    protected CartesianProductVector cartesianProductUnion(final int start, final int end) {

        Arrays.fill(cartesianProductMidpoints, (short) 0);
        int size = 0;

        final CartesianProductFunction cpf = grammar.cartesianProductFunction();
        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);

            final int leftStart = chart.minLeftChildIndex(leftCellIndex);
            final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            for (int i = leftStart; i <= leftEnd; i++) {
                final int leftChild = nonTerminalIndices[i];
                final float leftProbability = insideProbabilities[i];

                for (int j = rightStart; j <= rightEnd; j++) {

                    if (collectDetailedStatistics) {
                        totalCartesianProductEntriesExamined++;
                    }

                    final int childPair = cpf.pack(leftChild, nonTerminalIndices[j]);
                    if (childPair == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float jointProbability = leftProbability + insideProbabilities[j];

                    if (collectDetailedStatistics) {
                        totalValidCartesianProductEntries++;
                    }

                    // If this cartesian-product entry is not populated, we can populate it without comparing
                    // to a current probability.
                    if (cartesianProductMidpoints[childPair] == 0) {
                        cartesianProductProbabilities[childPair] = jointProbability;
                        cartesianProductMidpoints[childPair] = midpoint;

                        if (collectDetailedStatistics) {
                            size++;
                        }

                    } else {
                        if (jointProbability > cartesianProductProbabilities[childPair]) {
                            cartesianProductProbabilities[childPair] = jointProbability;
                            cartesianProductMidpoints[childPair] = midpoint;
                        }
                    }
                }
            }
        }

        return new CartesianProductVector(grammar, cartesianProductProbabilities, cartesianProductMidpoints,
            size);
    }

    @Override
    public void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final ChartCell chartCell) {

        final PackedArrayChartCell targetCell = (PackedArrayChartCell) chartCell;
        targetCell.allocateTemporaryStorage();

        binarySpmvMultiply(cartesianProductVector, targetCell.tmpPackedChildren,
            targetCell.tmpInsideProbabilities, targetCell.tmpMidpoints);
    }

    protected final void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final int[] targetCellChildren, final float[] targetCellProbabilities,
            final short[] targetCellMidpoints) {

        // Iterate over possible populated child pairs (matrix columns)
        for (int i = 0; i < grammar.cscBinaryPopulatedColumns.length; i++) {

            final int childPair = grammar.cscBinaryPopulatedColumns[i];
            final short cartesianProductMidpoint = cartesianProductVector.midpoints[childPair];

            // Skip grammar matrix columns for unpopulated cartesian-product entries
            if (cartesianProductMidpoint != 0) {
                final float cartesianProductProbability = cartesianProductVector.probabilities[childPair];

                // Iterate over possible parents of the child pair (rows with non-zero entries)
                for (int j = grammar.cscBinaryPopulatedColumnOffsets[i]; j < grammar.cscBinaryPopulatedColumnOffsets[i + 1]; j++) {

                    final float jointProbability = grammar.cscBinaryProbabilities[j]
                            + cartesianProductProbability;
                    final int parent = grammar.cscBinaryRowIndices[j];

                    if (jointProbability > targetCellProbabilities[parent]) {
                        targetCellChildren[parent] = childPair;
                        targetCellProbabilities[parent] = jointProbability;
                        targetCellMidpoints[parent] = cartesianProductMidpoint;
                    }
                }
            }
        }
    }

    @Override
    public String getStatHeader() {
        return super.getStatHeader() + ", Avg X-prod size, X-prod Entries Examined, Total X-prod Entries";
    }

    @Override
    public String getStats() {
        return super.getStats()
                + String.format(", %15.1f, %23d, %20d", totalCartesianProductSize * 1.0f / chart.cells,
                    totalCartesianProductEntriesExamined, totalValidCartesianProductEntries);
    }

}
