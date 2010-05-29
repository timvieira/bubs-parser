package edu.ohsu.cslu.parser;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.CscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
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
     * Takes the cross-product of all potential child-cell combinations. Unions those cross-products together,
     * saving the maximum probability child combinations.
     *
     * TODO Share with {@link CsrSpmvParser}
     *
     * @param start
     * @param end
     * @return Unioned cross-product
     */
    @Override
    protected CartesianProductVector cartesianProductUnion(final int start, final int end) {

        Arrays.fill(cartesianProductMidpoints, (short) 0);
        int size = 0;

        final CartesianProductFunction cpf = grammar.cartesianProductFunction();
        final int[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);

            for (int i = chart.minLeftChildIndex(leftCellIndex); i <= chart.maxLeftChildIndex(leftCellIndex); i++) {
                final int leftChild = nonTerminalIndices[i];
                // final int packedLeftChild = cpf.partialPackLeft(leftChild);
                final float leftProbability = insideProbabilities[i];

                for (int j = chart.offset(rightCellIndex); j <= chart.maxRightChildIndex(rightCellIndex); j++) {

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

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        final int[] chartCellChildren = packedArrayCell.tmpChildren;
        final float[] chartCellProbabilities = packedArrayCell.tmpInsideProbabilities;
        final short[] chartCellMidpoints = packedArrayCell.tmpMidpoints;

        binarySpmvMultiply(cartesianProductVector, chartCellChildren, chartCellProbabilities,
            chartCellMidpoints);
    }

    protected final void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final int[] productChildren, final float[] productProbabilities, final short[] productMidpoints) {

        final int[] binaryRuleMatrixPopulatedColumns = grammar.binaryRuleMatrixPopulatedColumns();
        final int[] binaryRuleMatrixColumnOffsets = grammar.binaryRuleMatrixColumnOffsets();
        final int[] binaryRuleMatrixRowIndices = grammar.binaryRuleMatrixRowIndices();
        final float[] binaryRuleMatrixProbabilities = grammar.binaryRuleMatrixProbabilities();

        final float[] localCartesianProductProbabilities = cartesianProductVector.probabilities;
        final short[] localCartesianProductMidpoints = cartesianProductVector.midpoints;

        // Iterate over possible populated child pairs (matrix columns)
        for (int i = 0; i < binaryRuleMatrixPopulatedColumns.length; i++) {

            final int childPair = binaryRuleMatrixPopulatedColumns[i];
            final short cartesianProductMidpoint = localCartesianProductMidpoints[childPair];

            // Skip grammar matrix columns for unpopulated cartesian-product entries
            if (cartesianProductMidpoint != 0) {
                final float cartesianProductProbability = localCartesianProductProbabilities[childPair];

                // Iterate over possible parents of the child pair (rows with non-zero entries)
                for (int j = binaryRuleMatrixColumnOffsets[i]; j < binaryRuleMatrixColumnOffsets[i + 1]; j++) {

                    final float jointProbability = binaryRuleMatrixProbabilities[j]
                            + cartesianProductProbability;
                    final int parent = binaryRuleMatrixRowIndices[j];

                    if (jointProbability > productProbabilities[parent]) {
                        productChildren[parent] = childPair;
                        productProbabilities[parent] = jointProbability;
                        productMidpoints[parent] = cartesianProductMidpoint;
                    }
                }
            }
        }
    }

    @Override
    public void unarySpmvMultiply(final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        final int[] unaryRuleMatrixPopulatedColumns = grammar.unaryRuleMatrixPopulatedColumns();
        final int[] unaryRuleMatrixColumnOffsets = grammar.unaryRuleMatrixColumnOffsets();
        final int[] unaryRuleMatrixRowIndices = grammar.unaryRuleMatrixRowIndices();
        final float[] unaryRuleMatrixProbabilities = grammar.unaryRuleMatrixProbabilities();

        final int[] chartCellChildren = packedArrayCell.tmpChildren;
        final float[] chartCellProbabilities = packedArrayCell.tmpInsideProbabilities;
        final short[] chartCellMidpoints = packedArrayCell.tmpMidpoints;
        final short chartCellEnd = (short) chartCell.end();

        // Iterate over possible populated child pairs (matrix columns)
        for (int i = 0; i < unaryRuleMatrixPopulatedColumns.length; i++) {

            final int childPair = unaryRuleMatrixPopulatedColumns[i];
            final int child = grammar.cartesianProductFunction().unpackLeftChild(childPair);
            final float currentProbability = chartCellProbabilities[child];

            // Iterate over possible parents of the child (rows with non-zero entries)
            for (int j = unaryRuleMatrixColumnOffsets[i]; j < unaryRuleMatrixColumnOffsets[i + 1]; j++) {

                final int parent = unaryRuleMatrixRowIndices[j];
                final float jointProbability = unaryRuleMatrixProbabilities[j] + currentProbability;

                if (jointProbability > chartCellProbabilities[parent]) {
                    chartCellChildren[parent] = childPair;
                    chartCellProbabilities[parent] = jointProbability;
                    chartCellMidpoints[parent] = chartCellEnd;
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
