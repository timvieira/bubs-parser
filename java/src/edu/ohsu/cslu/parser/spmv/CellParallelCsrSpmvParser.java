package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * Distributes the binary SpMV operation across multiple threads.
 * 
 * @author Aaron Dunlop
 * @since Mar 10, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class CellParallelCsrSpmvParser extends CsrSpmvParser {

    /** The number of threads configured for binary grammar intersection */
    private final int threads;

    /**
     * Offsets into {@link CsrSparseMatrixGrammar#csrBinaryRowIndices} splitting the binary rule-set into segments of
     * roughly equal size for distribution between threads. Length is {@link #threads} + 1 (to avoid falling off the
     * end)
     */
    private final int[] binaryRowSegments;

    private final ExecutorService executor;

    final float[][] cpvProbabilities;
    final short[][] cpvMidpoints;

    public CellParallelCsrSpmvParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);

        // Split the binary grammar rules into segments of roughly equal size
        final int requestedThreads = GlobalConfigProperties.singleton().getIntProperty(
                ParserDriver.OPT_REQUESTED_THREAD_COUNT);
        final int[] segments = new int[requestedThreads + 1];
        final int segmentSize = grammar.csrBinaryColumnIndices.length / requestedThreads + 1;
        segments[0] = 0;
        int i = 1;
        for (int j = 1; j < grammar.csrBinaryRowIndices.length; j++) {
            if (grammar.csrBinaryRowIndices[j] - grammar.csrBinaryRowIndices[segments[i - 1]] >= segmentSize) {
                segments[i++] = j;
            }
        }
        segments[i] = grammar.csrBinaryRowIndices.length - 1;

        this.threads = i;
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_CONFIGURED_THREAD_COUNT,
                Integer.toString(threads));
        this.binaryRowSegments = new int[i + 1];
        System.arraycopy(segments, 0, binaryRowSegments, 0, binaryRowSegments.length);

        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 1; j < binaryRowSegments.length; j++) {
                sb.append((grammar.csrBinaryRowIndices[binaryRowSegments[j]] - grammar.csrBinaryRowIndices[binaryRowSegments[j - 1]])
                        + " ");
            }
            BaseLogger.singleton().fine("CSR Binary Grammar segments of length: " + sb.toString());
        }

        // Configure a thread pool
        executor = Executors.newFixedThreadPool(threads);

        // Pre-allocate cartesian-product vector arrays for each thread
        cpvProbabilities = new float[threads][];
        cpvMidpoints = new short[threads][];
        final int arrayLength = grammar.cartesianProductFunction().packedArraySize();
        for (int j = 1; j < threads; j++) {
            cpvProbabilities[j] = new float[arrayLength];
            cpvMidpoints[j] = new short[arrayLength];
        }
    }

    /**
     * Takes the cartesian-product of all potential child-cell combinations. Unions those cartesian-products together,
     * saving the maximum probability child combinations.
     * 
     * @param start
     * @param end
     * @return Unioned cartesian-product
     */
    @Override
    protected CartesianProductVector cartesianProductUnion(final int start, final int end) {

        final PerfectIntPairHashPackingFunction cpf = (PerfectIntPairHashPackingFunction) grammar
                .cartesianProductFunction();
        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        // Perform cartesian-product operation at each midpoint
        @SuppressWarnings("unchecked")
        final Future<CartesianProductVector>[] futures = new Future[threads];

        // Don't bother multi-threading for fewer than 4 midpoints
        final int segmentSize = (end - start - 2) / threads + 1;

        if (end - start - 1 > 4) {
            for (int i = 0; i < threads; i++) {

                final int midpointStart = start + 1 + segmentSize * i;
                final int midpointEnd = Math.min(midpointStart + segmentSize, end - 1);
                final float[] probabilities = i == 0 ? cartesianProductProbabilities : cpvProbabilities[i];
                final short[] midpoints = i == 0 ? cartesianProductMidpoints : cpvMidpoints[i];

                futures[i] = executor.submit(new Callable<CartesianProductVector>() {

                    @Override
                    public edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductVector call()
                            throws Exception {
                        return cartesianProduct(start, end, midpointStart, midpointEnd, cpf, nonTerminalIndices,
                                insideProbabilities, probabilities, midpoints);
                    }
                });
            }
        } else {
            return cartesianProduct(start, end, start + 1, end - 1, cpf, nonTerminalIndices, insideProbabilities,
                    cartesianProductProbabilities, cartesianProductMidpoints);
        }

        // Wait for the first task to complete (the first one uses the 'main' arrays, so we can't begin the merge until
        // it is complete)
        try {
            futures[0].get();
        } catch (final InterruptedException ignore) {
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }

        // Wait for other tasks to complete.
        // Copy the probabilities and midpoints of the first task directly to the main array and merge each subsequent
        // task into that array.
        // TODO This merge could be parallelized by splitting the vector array in segments
        for (int i = 1; i < threads; i++) {
            try {
                final CartesianProductVector partialCpv = futures[i].get();
                // Merge partial cartesian-product vector into the main vector (tropical semiring - choose maximum
                // probability)
                for (int j = 0; j < partialCpv.midpoints.length; j++) {
                    if (partialCpv.midpoints[j] != 0
                            && (cartesianProductMidpoints[j] == 0 || partialCpv.probabilities[j] > cartesianProductProbabilities[j])) {
                        cartesianProductMidpoints[j] = partialCpv.midpoints[j];
                        cartesianProductProbabilities[j] = partialCpv.probabilities[j];
                    }
                }
            } catch (final InterruptedException ignore) {
            } catch (final ExecutionException e) {
                e.printStackTrace();
            }
        }

        return new CartesianProductVector(grammar, cartesianProductProbabilities, cartesianProductMidpoints, 0);
    }

    private CartesianProductVector cartesianProduct(final int start, final int end, final int midpointStart,
            final int midpointEnd, final PerfectIntPairHashPackingFunction cpf, final short[] nonTerminalIndices,
            final float[] insideProbabilities, final float[] probabilities, final short[] midpoints) {

        Arrays.fill(midpoints, (short) 0);

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) midpointStart; midpoint <= midpointEnd; midpoint++) {
            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);

            final int leftStart = chart.minLeftChildIndex(leftCellIndex);
            final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            for (int i = leftStart; i <= leftEnd; i++) {
                final short leftChild = nonTerminalIndices[i];
                final float leftProbability = insideProbabilities[i];
                final int mask = cpf.mask(leftChild);
                final int shift = cpf.shift(leftChild);
                final int offset = cpf.offset(leftChild);

                final short minRightSibling = grammar.minRightSiblingIndices[leftChild];
                final short maxRightSibling = grammar.maxRightSiblingIndices[leftChild];

                for (int j = rightStart; j <= rightEnd; j++) {
                    // Skip any right children which cannot combine with left child
                    if (nonTerminalIndices[j] < minRightSibling) {
                        continue;
                    } else if (nonTerminalIndices[j] > maxRightSibling) {
                        break;
                    }

                    if (collectDetailedStatistics) {
                        totalCartesianProductEntriesExamined++;
                    }

                    final int childPair = cpf.pack(nonTerminalIndices[j], shift, mask, offset);
                    if (childPair == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float jointProbability = leftProbability + insideProbabilities[j];

                    if (collectDetailedStatistics) {
                        totalValidCartesianProductEntries++;
                    }

                    // If this cartesian-product entry is not populated, we can populate it without comparing
                    // to a current probability. The memory write is faster if we don't first have to read.
                    if (midpoints[childPair] == 0) {
                        probabilities[childPair] = jointProbability;
                        midpoints[childPair] = midpoint;

                    } else {
                        if (jointProbability > probabilities[childPair]) {
                            probabilities[childPair] = jointProbability;
                            midpoints[childPair] = midpoint;
                        }
                    }
                }
            }
        }
        return new CartesianProductVector(grammar, probabilities, midpoints, 0);
    }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        final Future<?>[] futures = new Future<?>[binaryRowSegments.length - 1];
        // Iterate over binary grammar segments
        for (int i = 0; i < threads; i++) {
            final int segmentStart = binaryRowSegments[i];
            final int segmentEnd = binaryRowSegments[i + 1];
            futures[i] = executor.submit(new Runnable() {

                @Override
                public void run() {
                    binarySpmvSegment(cartesianProductVector, packedArrayCell, segmentStart, segmentEnd);
                }
            });
        }

        // Wait for all tasks to complete
        for (int i = 0; i < binaryRowSegments.length - 1; i++) {
            try {
                futures[i].get();
            } catch (final InterruptedException ignore) {
            } catch (final ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void binarySpmvSegment(final CartesianProductVector cartesianProductVector,
            final PackedArrayChartCell packedArrayCell, final int segmentStart, final int segmentEnd) {

        // Iterate over possible parents (matrix rows)
        for (int parent = segmentStart; parent < segmentEnd; parent++) {

            float winningProbability = Float.NEGATIVE_INFINITY;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = grammar.csrBinaryRowIndices[parent]; i < grammar.csrBinaryRowIndices[parent + 1]; i++) {
                final int grammarChildren = grammar.csrBinaryColumnIndices[i];

                if (cartesianProductVector.midpoints[grammarChildren] == 0) {
                    continue;
                }

                final float jointProbability = grammar.csrBinaryProbabilities[i]
                        + cartesianProductVector.probabilities[grammarChildren];

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = cartesianProductVector.midpoints[grammarChildren];
                }
            }

            if (winningProbability != Float.NEGATIVE_INFINITY) {
                packedArrayCell.tmpPackedChildren[parent] = winningChildren;
                packedArrayCell.tmpInsideProbabilities[parent] = winningProbability;
                packedArrayCell.tmpMidpoints[parent] = winningMidpoint;
            }
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
