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
 * @see OpenClSparseMatrixVectorParser
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

    public CscSpmvParser(final CscSparseMatrixGrammar grammar) {
        super(grammar);
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

            totalCartesianProductSize += cartesianProductVector.size();

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
     * @param start
     * @param end
     * @return Unioned cross-product
     */
    @Override
    protected CartesianProductVector cartesianProductUnion(final int start, final int end) {

        if (cartesianProductProbabilities == null) {
            cartesianProductProbabilities = new float[grammar.cartesianProductFunction().packedArraySize()];
            cartesianProductMidpoints = new short[cartesianProductProbabilities.length];
        }

        Arrays.fill(cartesianProductProbabilities, Float.NEGATIVE_INFINITY);
        int size = 0;

        final CartesianProductFunction cpf = grammar.cartesianProductFunction();

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final PackedArrayChartCell leftCell = chart.getCell(start, midpoint);
            final PackedArrayChartCell rightCell = chart.getCell(midpoint, end);

            final int[] nonTerminalIndices = chart.nonTerminalIndices;
            final float[] insideProbabilities = chart.insideProbabilities;

            for (int i = leftCell.minLeftChildIndex(); i <= leftCell.maxLeftChildIndex(); i++) {

                final int leftChild = nonTerminalIndices[i];
                final float leftProbability = insideProbabilities[i];

                for (int j = rightCell.offset(); j <= rightCell.maxRightChildIndex(); j++) {

                    final int childPair = cpf.pack(leftChild, (short) nonTerminalIndices[j]);
                    totalCartesianProductEntriesExamined++;

                    if (!cpf.isValid(childPair)) {
                        continue;
                    }

                    totalValidCartesianProductEntries++;

                    final float jointProbability = leftProbability + insideProbabilities[j];
                    final float currentProbability = cartesianProductProbabilities[childPair];

                    if (jointProbability > currentProbability) {
                        cartesianProductProbabilities[childPair] = jointProbability;
                        cartesianProductMidpoints[childPair] = midpoint;

                        if (currentProbability == Float.NEGATIVE_INFINITY) {
                            size++;
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

        final float[] localCrossProductProbabilities = cartesianProductVector.probabilities;
        final short[] localCrossProductMidpoints = cartesianProductVector.midpoints;

        // Iterate over possible populated child pairs (matrix columns)
        for (int i = 0; i < binaryRuleMatrixPopulatedColumns.length; i++) {

            final int childPair = binaryRuleMatrixPopulatedColumns[i];
            final float cartesianProductProbability = localCrossProductProbabilities[childPair];
            final short cartesianProductMidpoint = localCrossProductMidpoints[childPair];

            // Iterate over possible parents of the child pair (rows with non-zero entries)
            for (int j = binaryRuleMatrixColumnOffsets[i]; j < binaryRuleMatrixColumnOffsets[i + 1]; j++) {

                final int parent = binaryRuleMatrixRowIndices[j];
                final float jointProbability = binaryRuleMatrixProbabilities[j] + cartesianProductProbability;

                if (jointProbability > productProbabilities[parent]) {
                    productChildren[parent] = childPair;
                    productProbabilities[parent] = jointProbability;
                    productMidpoints[parent] = cartesianProductMidpoint;
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
        return super.getStatHeader() + ", Avg X-prod Size, Total X-prod Entries";
    }

    @Override
    public String getStats() {
        return super.getStats()
                + String.format(", %15.1f, %20d, %20d", totalCartesianProductSize * 1.0f / chart.cells,
                    totalCartesianProductEntriesExamined, totalValidCartesianProductEntries);
    }

}
