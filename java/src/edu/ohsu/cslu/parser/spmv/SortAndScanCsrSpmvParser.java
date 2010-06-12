package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.util.RadixSort;
import edu.ohsu.cslu.util.Scanner;
import edu.ohsu.cslu.util.SerialCpuScanner;
import edu.ohsu.cslu.util.Sort;

/**
 * An implementation of {@link SparseMatrixVectorParser} which stores its chart in packed format (
 * {@link PackedArrayChart}) and performs the cartesian product using sort and scan operations. This
 * implementation performs the sort and scans serially on the CPU, but in theory, these operations should be
 * implementable efficiently on GPU hardware.
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SortAndScanCsrSpmvParser extends
        SparseMatrixVectorParser<CsrSparseMatrixGrammar, PackedArrayChart> {

    public long totalSortTime = 0;
    public long totalCountTime = 0;
    public long totalXProdTime = 0;
    public long totalScanTime = 0;
    public long totalScatterTime = 0;

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
        long cartesianProductTime = 0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);

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

        // System.out.format("Visited cell: %2d,%2d (%5d ms). Cross-product: %6d/%6d combinations (%5.0f ms, %4.2f/ms), Multiply: %5d edges (%5.0f ms, %4.2f /ms)\n",
        // start, end, t3
        // - t0, cartesianProductSize, totalProducts, cartesianProductTime, cartesianProductSize /
        // cartesianProductTime,
        // edges, spmvTime, edges / spmvTime);
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

        final long t0 = System.nanoTime() / 1000000;
        // Compute the size of the array we'll need: sum_{m=1}^{M} V_l * V_r (sizes of cartesian product for
        // all midpoints). Store offset into that array for each midpoint.
        final int[] offsets = new int[end - start];
        int totalChildPairs = 0;
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final PackedArrayChartCell leftCell = chart.getCell(start, midpoint);
            final int leftCellLeftChildren = leftCell.maxLeftChildIndex() - leftCell.minLeftChildIndex() + 1;

            final PackedArrayChartCell rightCell = chart.getCell(midpoint, end);
            final int rightCellRightChildren = rightCell.maxRightChildIndex() - rightCell.offset() + 1;

            totalChildPairs += leftCellLeftChildren * rightCellRightChildren;
            if (midpoint < (end - 1)) {
                offsets[midpoint - start + 1] = totalChildPairs;
            }
        }

        final long t1 = System.nanoTime() / 1000000;
        totalCountTime += t1 - t0;

        // Allocate parallel array for cartesian product (children, probability, midpoint)
        final int[] tmpCartesianProductChildren = new int[totalChildPairs];
        final float[] tmpCartesianProductInsideProbabilities = new float[totalChildPairs];
        final short[] tmpCartesianProductMidpoints = new short[totalChildPairs];

        // Perform cartesian product for each midpoint and store in the parallel array
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final PackedArrayChartCell leftCell = chart.getCell(start, midpoint);
            final PackedArrayChartCell rightCell = chart.getCell(midpoint, end);

            final int rightCellRightChildren = rightCell.maxRightChildIndex() - rightCell.offset() + 1;

            final short[] nonTerminalIndices = chart.nonTerminalIndices;
            final float[] insideProbabilities = chart.insideProbabilities;

            for (int i = leftCell.minLeftChildIndex(); i <= leftCell.maxLeftChildIndex(); i++) {
                final int leftChildrenProcessed = i - leftCell.minLeftChildIndex();

                final int leftChildIndex = nonTerminalIndices[i];
                final float leftProbability = insideProbabilities[i];

                for (int j = rightCell.offset(); j <= rightCell.maxRightChildIndex(); j++) {
                    final int rightChildrenProcessed = j - rightCell.offset();
                    final int cartesianProductIndex = offsets[midpoint - start] + leftChildrenProcessed
                            * rightCellRightChildren + rightChildrenProcessed;

                    tmpCartesianProductChildren[cartesianProductIndex] = grammar.cartesianProductFunction()
                        .pack(leftChildIndex, nonTerminalIndices[j]);
                    tmpCartesianProductInsideProbabilities[cartesianProductIndex] = leftProbability
                            + insideProbabilities[j];
                    tmpCartesianProductMidpoints[cartesianProductIndex] = midpoint;
                }
            }
        }

        final long t2 = System.nanoTime() / 1000000;
        totalXProdTime += t2 - t1;

        // Sort the parallel array by children (keeping probabilities and midpoints aligned with the
        // appropriate children keys)
        final Sort sorter = new RadixSort();
        sorter.sort(tmpCartesianProductChildren, tmpCartesianProductInsideProbabilities,
            tmpCartesianProductMidpoints, offsets);

        final long t3 = System.nanoTime() / 1000000;
        totalSortTime += t3 - t2;

        // Flag the last occurrence of each key
        final Scanner scanner = new SerialCpuScanner();
        final byte[] segmentFlags = new byte[totalChildPairs];
        scanner.flagEndOfKeySegments(tmpCartesianProductChildren, segmentFlags);

        // Segmented scan through the probability array, using the last occurrence flags as segment boundaries
        // and keeping the max probability. This custom segmented scan also
        // 'sums' the midpoint array, so the (already-flagged) last instance of each children key will have
        // the maximum probability and the associated midpoint.
        scanner.parallelArrayInclusiveSegmentedMax(tmpCartesianProductInsideProbabilities,
            tmpCartesianProductInsideProbabilities, tmpCartesianProductMidpoints,
            tmpCartesianProductMidpoints, segmentFlags);

        final long t4 = System.nanoTime() / 1000000;
        totalScanTime += t4 - t3;

        // Scatter the cartesian product array to a dense representation, writing only the flagged values
        Arrays.fill(cartesianProductProbabilities, Float.NEGATIVE_INFINITY);
        scanner.scatter(tmpCartesianProductInsideProbabilities, cartesianProductProbabilities,
            tmpCartesianProductChildren, segmentFlags);
        scanner.scatter(tmpCartesianProductMidpoints, cartesianProductMidpoints, tmpCartesianProductChildren,
            segmentFlags);

        int size = 0;
        for (int i = 0; i < segmentFlags.length; i++) {
            if (segmentFlags[i] != 0) {
                size++;
            }
        }

        final long t5 = System.nanoTime() / 1000000;
        totalScatterTime += t5 - t4;

        // System.out.format("Total Child Pairs: %d Size: %d, %.1f%%\n", totalChildPairs, size, size * 100f /
        // totalChildPairs);

        return new CartesianProductVector(grammar, cartesianProductProbabilities, cartesianProductMidpoints,
            size);
    }

    @Override
    public void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        final int[] binaryRuleMatrixRowIndices = grammar.binaryRuleMatrixRowIndices();
        final int[] binaryRuleMatrixColumnIndices = grammar.binaryRuleMatrixColumnIndices();
        final float[] binaryRuleMatrixProbabilities = grammar.binaryRuleMatrixProbabilities();

        final float[] tmpCrossProductProbabilities = cartesianProductVector.probabilities;
        final short[] tmpCrossProductMidpoints = cartesianProductVector.midpoints;

        final int[] chartCellChildren = packedArrayCell.tmpPackedChildren;
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

                final float cartesianProductProbability = tmpCrossProductProbabilities[grammarChildren];
                final float jointProbability = grammarProbability + cartesianProductProbability;

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

        final int[] chartCellChildren = packedArrayCell.tmpPackedChildren;
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
                final int child = grammar.cartesianProductFunction().unpackLeftChild(grammarChildren);
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

    @Override
    public String getStatHeader() {
        return String.format("%8s, %9s, %8s, %6s, %6s, %6s, %6s, %7s, %5s", "Total", "X-product", "X-union",
            "Count", "X-prod", "Sort", "Scan", "Scatter", "SpMV");
    }

    @Override
    public String getStats() {
        final long totalTime = System.currentTimeMillis() - startTime;
        return String.format("%8.1f, %9d, %8d, %6d, %6d, %6d, %6d, %7d, %5d", totalTime / 1000f,
            totalCartesianProductTime, totalCartesianProductUnionTime, totalCountTime, totalXProdTime,
            totalSortTime, totalScanTime, totalScatterTime, totalSpMVTime);
    }
}
