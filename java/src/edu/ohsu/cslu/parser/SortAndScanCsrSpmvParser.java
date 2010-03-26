package edu.ohsu.cslu.parser;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.util.Scanner;
import edu.ohsu.cslu.util.SerialCpuScanner;

/**
 * An implementation of {@link SparseMatrixVectorParser} which stores its chart in packed format ({@link PackedArrayChart}) and performs the cartesian product using sort and scan
 * operations. This implementation performs the sort and scans serially on the CPU, but in theory, these operations should be implementable efficiently on GPU hardware.
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SortAndScanCsrSpmvParser extends SparseMatrixVectorParser<CsrSparseMatrixGrammar, PackedArrayChart> {

    public SortAndScanCsrSpmvParser(final CsrSparseMatrixGrammar grammar) {
        super(grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        chart = new PackedArrayChart(sentLength, grammar);
        super.initParser(sentLength);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final PackedArrayChartCell spvChartCell = chart.getCell(start, end);

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        long crossProductTime = 0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CrossProductVector crossProductVector = crossProductUnion(start, end);

            t1 = System.currentTimeMillis();
            crossProductTime = t1 - t0;

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmvMultiply(crossProductVector, spvChartCell);
        }
        final long t2 = System.currentTimeMillis();
        final long binarySpmvTime = t2 - t1;

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        unarySpmvMultiply(spvChartCell);

        final long t3 = System.currentTimeMillis();
        final long unarySpmvTime = t3 - t2;

        // Pack the temporary cell storage into the main chart array
        spvChartCell.finalizeCell();

        // System.out.format("Visited cell: %2d,%2d (%5d ms). Cross-product: %6d/%6d combinations (%5.0f ms, %4.2f/ms), Multiply: %5d edges (%5.0f ms, %4.2f /ms)\n", start, end, t3
        // - t0, crossProductSize, totalProducts, crossProductTime, crossProductSize / crossProductTime, edges, spmvTime, edges / spmvTime);
        totalCartesianProductTime += crossProductTime;
        totalSpMVTime += binarySpmvTime + unarySpmvTime;
    }

    /**
     * Takes the cross-product of all potential child-cell combinations. Unions those cross-products together, saving the maximum probability child combinations.
     * 
     * @param start
     * @param end
     * @return Unioned cross-product
     */
    @Override
    protected CrossProductVector crossProductUnion(final int start, final int end) {

        // Compute the size of the array we'll need: sum_{m=1}^{M} V_l * V_r (sizes of cartesian product for all midpoints). Store offset into that array for each midpoint.
        final int[] offsets = new int[end - start - 1];
        int totalChildPairs = 0;
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final PackedArrayChartCell leftCell = chart.getCell(start, midpoint);
            final int leftCellLeftChildren = leftCell.maxLeftChildIndex() - leftCell.minLeftChildIndex() + 1;

            final PackedArrayChartCell rightCell = chart.getCell(midpoint, end);
            final int rightCellRightChildren = rightCell.maxRightChildIndex() - rightCell.offset() + 1;

            totalChildPairs += leftCellLeftChildren * rightCellRightChildren;
            if (midpoint < (end - 1)) {
                offsets[midpoint - start] = totalChildPairs;
            }
        }

        // Allocate parallel array for cartesian product (children, probability, midpoint)
        final int[] cartesianProductChildren = new int[totalChildPairs];
        final float[] cartesianProductInsideProbabilities = new float[totalChildPairs];
        final short[] cartesianProductMidpoints = new short[totalChildPairs];

        // Perform cartesian product for each midpoint and store in the parallel array
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final PackedArrayChartCell leftCell = chart.getCell(start, midpoint);
            final PackedArrayChartCell rightCell = chart.getCell(midpoint, end);

            final int rightCellRightChildren = rightCell.maxRightChildIndex() - rightCell.offset() + 1;

            final int[] nonTerminalIndices = chart.nonTerminalIndices;
            final float[] insideProbabilities = chart.insideProbabilities;

            for (int i = leftCell.minLeftChildIndex(); i <= leftCell.maxLeftChildIndex(); i++) {
                final int leftChildrenProcessed = i - leftCell.minLeftChildIndex();

                final int leftChildIndex = nonTerminalIndices[i];
                final float leftProbability = insideProbabilities[i];

                for (int j = rightCell.offset(); j <= rightCell.maxRightChildIndex(); j++) {
                    final int rightChildrenProcessed = j - rightCell.offset();
                    final int cartesianProductIndex = offsets[midpoint - start - 1] + leftChildrenProcessed * rightCellRightChildren + rightChildrenProcessed;

                    cartesianProductChildren[cartesianProductIndex] = grammar.pack(leftChildIndex, (short) nonTerminalIndices[j]);
                    cartesianProductInsideProbabilities[cartesianProductIndex] = leftProbability + insideProbabilities[j];
                    cartesianProductMidpoints[cartesianProductIndex] = midpoint;
                }
            }
        }

        // Sort the parallel array by children (keeping probabilities and midpoints aligned with the appropriate children keys)
        edu.ohsu.cslu.util.Arrays.radixSort(cartesianProductChildren, cartesianProductInsideProbabilities, cartesianProductMidpoints);

        // Flag the last occurrence of each key
        final Scanner scanner = new SerialCpuScanner();
        final byte[] segmentFlags = new byte[totalChildPairs];
        scanner.flagEndOfKeySegments(cartesianProductChildren, segmentFlags);

        // Segmented scan through the probability array, using the last occurrence flags as segment boundaries and keeping the max probability. This custom segmented scan also
        // 'sums' the midpoint array, so the (already-flagged) last instance of each children key will have the maximum probability and the associated midpoint.
        scanner.parallelArrayInclusiveSegmentedMax(cartesianProductInsideProbabilities, cartesianProductInsideProbabilities, cartesianProductMidpoints, cartesianProductMidpoints,
                segmentFlags);

        // Scatter the cartesian product array to a dense representation, writing only the flagged values
        if (crossProductProbabilities == null) {
            crossProductProbabilities = new float[grammar.packedArraySize()];
            crossProductMidpoints = new short[grammar.packedArraySize()];
        }
        Arrays.fill(crossProductProbabilities, Float.NEGATIVE_INFINITY);
        scanner.scatter(cartesianProductInsideProbabilities, crossProductProbabilities, cartesianProductChildren, segmentFlags);
        scanner.scatter(cartesianProductMidpoints, crossProductMidpoints, cartesianProductChildren, segmentFlags);

        int size = 0;
        for (int i = 0; i < segmentFlags.length; i++) {
            if (segmentFlags[i] != 0) {
                size++;
            }
        }

        System.out.format("Total Child Pairs: %d Size: %d, %.1f%%\n", totalChildPairs, size, size * 100f / totalChildPairs);

        return new CrossProductVector(grammar, crossProductProbabilities, crossProductMidpoints, size);
    }

    @Override
    public void binarySpmvMultiply(final CrossProductVector crossProductVector, final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        final int[] binaryRuleMatrixRowIndices = grammar.binaryRuleMatrixRowIndices();
        final int[] binaryRuleMatrixColumnIndices = grammar.binaryRuleMatrixColumnIndices();
        final float[] binaryRuleMatrixProbabilities = grammar.binaryRuleMatrixProbabilities();

        final float[] tmpCrossProductProbabilities = crossProductVector.probabilities;
        final short[] tmpCrossProductMidpoints = crossProductVector.midpoints;

        final int[] chartCellChildren = packedArrayCell.tmpChildren;
        final float[] chartCellProbabilities = packedArrayCell.tmpInsideProbabilities;
        final short[] chartCellMidpoints = packedArrayCell.tmpMidpoints;

        // Iterate over possible parents (matrix rows)
        for (int parent = 0; parent < grammar.numNonTerms(); parent++) {

            // Production winningProduction = null;
            float winningProbability = Float.NEGATIVE_INFINITY;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = binaryRuleMatrixRowIndices[parent]; i < binaryRuleMatrixRowIndices[parent + 1]; i++) {
                final int grammarChildren = binaryRuleMatrixColumnIndices[i];
                final float grammarProbability = binaryRuleMatrixProbabilities[i];

                final float crossProductProbability = tmpCrossProductProbabilities[grammarChildren];
                final float jointProbability = grammarProbability + crossProductProbability;

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = tmpCrossProductMidpoints[grammarChildren];
                }
            }

            if (winningProbability != Float.NEGATIVE_INFINITY) {
                chartCellChildren[parent] = winningChildren;
                chartCellProbabilities[parent] = winningProbability;
                chartCellMidpoints[parent] = winningMidpoint;
            }
        }
    }

    @Override
    public void unarySpmvMultiply(final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        final int[] unaryRuleMatrixRowIndices = grammar.unaryRuleMatrixRowIndices();
        final int[] unaryRuleMatrixColumnIndices = grammar.unaryRuleMatrixColumnIndices();
        final float[] unaryRuleMatrixProbabilities = grammar.unaryRuleMatrixProbabilities();

        final int[] chartCellChildren = packedArrayCell.tmpChildren;
        final float[] chartCellProbabilities = packedArrayCell.tmpInsideProbabilities;
        final short[] chartCellMidpoints = packedArrayCell.tmpMidpoints;
        final short chartCellEnd = (short) chartCell.end();

        // Iterate over possible parents (matrix rows)
        for (int parent = 0; parent < grammar.numNonTerms(); parent++) {

            final float currentProbability = chartCellProbabilities[parent];
            float winningProbability = currentProbability;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = unaryRuleMatrixRowIndices[parent]; i < unaryRuleMatrixRowIndices[parent + 1]; i++) {

                final int grammarChildren = unaryRuleMatrixColumnIndices[i];
                final int child = grammar.unpackLeftChild(grammarChildren);
                final float grammarProbability = unaryRuleMatrixProbabilities[i];

                final float jointProbability = grammarProbability + chartCellProbabilities[child];

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = chartCellEnd;
                }
            }

            if (winningChildren != Integer.MIN_VALUE) {
                chartCellChildren[parent] = winningChildren;
                chartCellProbabilities[parent] = winningProbability;
                chartCellMidpoints[parent] = winningMidpoint;
            }
        }
    }
}