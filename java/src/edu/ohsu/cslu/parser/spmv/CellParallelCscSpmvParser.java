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
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
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
public final class CellParallelCscSpmvParser extends CscSpmvParser {

    /** The number of threads configured for binary grammar intersection */
    private final int threads;

    /**
     * Offsets into {@link CsrSparseMatrixGrammar#csrBinaryRowIndices} splitting the binary rule-set into segments of
     * roughly equal size for distribution between threads. Length is {@link #threads} + 1 (to avoid falling off the
     * end)
     */
    private final int[] binaryRowSegments;

    private final ExecutorService executor;

    private final float[][] cpvProbabilities;
    private final short[][] cpvMidpoints;

    protected final ThreadLocal<TemporaryChartCell> temporaryCells;

    public CellParallelCscSpmvParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);

        // Split the binary grammar rules into segments of roughly equal size
        final int requestedThreads = GlobalConfigProperties.singleton().getIntProperty(
                ParserDriver.OPT_CELL_THREAD_COUNT);
        final int[] segments = new int[requestedThreads + 1];
        final int segmentSize = grammar.cscBinaryRowIndices.length / requestedThreads + 1;
        segments[0] = 0;
        int i = 1;
        // Examine each populated column
        for (int j = 1; j < grammar.cscBinaryPopulatedColumns.length - 1; j++) {
            if (grammar.cscBinaryPopulatedColumnOffsets[j] - grammar.cscBinaryPopulatedColumnOffsets[segments[i - 1]] >= segmentSize) {
                segments[i++] = j;
            }
        }
        segments[i] = grammar.cscBinaryPopulatedColumnOffsets.length - 1;

        this.threads = i;
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_CONFIGURED_THREAD_COUNT,
                Integer.toString(threads));
        this.binaryRowSegments = new int[i + 1];
        System.arraycopy(segments, 0, binaryRowSegments, 0, binaryRowSegments.length);

        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 1; j < binaryRowSegments.length; j++) {
                sb.append((grammar.cscBinaryPopulatedColumnOffsets[binaryRowSegments[j]] - grammar.cscBinaryPopulatedColumnOffsets[binaryRowSegments[j - 1]])
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
        for (int j = 0; j < threads; j++) {
            cpvProbabilities[j] = new float[arrayLength];
            cpvMidpoints[j] = new short[arrayLength];
        }

        // And thread-local temporary cell storage
        this.temporaryCells = new ThreadLocal<TemporaryChartCell>() {
            @Override
            protected TemporaryChartCell initialValue() {
                return new TemporaryChartCell();
            }
        };
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

        final PerfectIntPairHashPackingFunction pf = (PerfectIntPairHashPackingFunction) grammar
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
                final float[] probabilities = cpvProbabilities[i];
                final short[] midpoints = cpvMidpoints[i];

                futures[i] = executor.submit(new Callable<CartesianProductVector>() {

                    @Override
                    public edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductVector call()
                            throws Exception {
                        return internalCartesianProduct(start, end, midpointStart, midpointEnd, pf, nonTerminalIndices,
                                insideProbabilities, probabilities, midpoints);
                    }
                });
            }
        } else {
            return internalCartesianProduct(start, end, start + 1, end - 1, pf, nonTerminalIndices,
                    insideProbabilities, cpvProbabilities[0], cpvMidpoints[0]);
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
                final float[] cartesianProductProbabilities = cpvProbabilities[0];
                final short[] cartesianProductMidpoints = cpvMidpoints[0];
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

        return new CartesianProductVector(grammar, cpvProbabilities[0], cpvMidpoints[0], 0);
    }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        @SuppressWarnings("unchecked")
        final Future<TemporaryChartCell>[] futures = new Future[binaryRowSegments.length - 1];
        // Iterate over binary grammar segments
        for (int i = 0; i < threads; i++) {
            final int segmentStart = binaryRowSegments[i];
            final int segmentEnd = binaryRowSegments[i + 1];
            futures[i] = executor.submit(new Callable<TemporaryChartCell>() {

                @Override
                public TemporaryChartCell call() throws Exception {
                    final TemporaryChartCell tmpCell = temporaryCells.get();

                    binarySpmvMultiply(cartesianProductVector, grammar.cscBinaryPopulatedColumns,
                            grammar.cscBinaryPopulatedColumnOffsets, grammar.cscBinaryRowIndices,
                            grammar.cscBinaryProbabilities, tmpCell.packedChildren, tmpCell.insideProbabilities,
                            tmpCell.midpoints, segmentStart, segmentEnd);
                    return tmpCell;
                }
            });
        }

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        // Wait for each task to complete and merge results into the main temporary storage
        for (int i = 0; i < binaryRowSegments.length - 1; i++) {
            try {
                final TemporaryChartCell tmpCell = futures[i].get();
                for (int j = 0; j < tmpCell.midpoints.length; j++) {
                    if (tmpCell.insideProbabilities[j] > packedArrayCell.tmpInsideProbabilities[j]) {
                        packedArrayCell.tmpInsideProbabilities[j] = tmpCell.insideProbabilities[j];
                        packedArrayCell.tmpPackedChildren[j] = tmpCell.packedChildren[j];
                        packedArrayCell.tmpMidpoints[j] = tmpCell.midpoints[j];
                    }
                }
                tmpCell.clear();
            } catch (final InterruptedException ignore) {
            } catch (final ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    private final class TemporaryChartCell {
        final int[] packedChildren = new int[grammar.numNonTerms()];
        final float[] insideProbabilities = new float[grammar.numNonTerms()];
        final short[] midpoints = new short[grammar.numNonTerms()];

        public TemporaryChartCell() {
            clear();
        }

        public void clear() {
            Arrays.fill(insideProbabilities, Float.NEGATIVE_INFINITY);
        }
    }
}
