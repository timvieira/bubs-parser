/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.parser.spmv;

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
 */
public final class GrammarParallelCsrSpmvParser extends CsrSpmvParser {

    /** The number of threads configured for binary grammar intersection */
    private final int threads;

    /**
     * Offsets into {@link CsrSparseMatrixGrammar#csrBinaryRowOffsets} splitting the binary rule-set into segments of
     * roughly equal size for distribution between threads. Length is {@link #threads} + 1 (to avoid falling off the
     * end)
     */
    private final int[] binaryRowSegments;

    private final ExecutorService executor;

    private final float[][] cpvProbabilities;
    private final short[][] cpvMidpoints;

    public GrammarParallelCsrSpmvParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);

        // Split the binary grammar rules into segments of roughly equal size
        final int requestedThreads = GlobalConfigProperties.singleton().getIntProperty(
                ParserDriver.OPT_GRAMMAR_THREAD_COUNT);
        final int[] segments = new int[requestedThreads + 1];
        final int segmentSize = grammar.csrBinaryColumnIndices.length / requestedThreads + 1;
        segments[0] = 0;
        int i = 1;
        for (int j = 1; j < grammar.csrBinaryRowOffsets.length; j++) {
            if (grammar.csrBinaryRowOffsets[j] - grammar.csrBinaryRowOffsets[segments[i - 1]] >= segmentSize) {
                segments[i++] = j;
            }
        }
        segments[i] = grammar.csrBinaryRowOffsets.length - 1;

        this.threads = i;
        GlobalConfigProperties.singleton().setProperty(ParserDriver.RUNTIME_CONFIGURED_THREAD_COUNT,
                Integer.toString(threads));
        this.binaryRowSegments = new int[i + 1];
        System.arraycopy(segments, 0, binaryRowSegments, 0, binaryRowSegments.length);

        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 1; j < binaryRowSegments.length; j++) {
                sb.append((grammar.csrBinaryRowOffsets[binaryRowSegments[j]] - grammar.csrBinaryRowOffsets[binaryRowSegments[j - 1]])
                        + " ");
            }
            BaseLogger.singleton().fine("CSR Binary Grammar segments of length: " + sb.toString());
        }

        // Configure a thread pool
        executor = Executors.newFixedThreadPool(threads);

        // Pre-allocate cartesian-product vector arrays for each thread
        cpvProbabilities = new float[threads][];
        cpvMidpoints = new short[threads][];
        final int arrayLength = grammar.packingFunction().packedArraySize();
        for (int j = 0; j < threads; j++) {
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

        final PerfectIntPairHashPackingFunction pf = (PerfectIntPairHashPackingFunction) grammar
                .packingFunction();
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
                    public edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductVector call() {
                        return internalCartesianProduct(start, end, midpointStart, midpointEnd, pf, nonTerminalIndices,
                                insideProbabilities, probabilities, midpoints);
                    }
                });
            }
        } else {
            return internalCartesianProduct(start, end, start + 1, end - 1, pf, nonTerminalIndices,
                    insideProbabilities, cpvProbabilities[0], cpvMidpoints[0]);
        }

        // Wait for the first task to complete (the first one uses the 'main' arrays, so we can't begin the
        // merge until
        // it is complete)
        try {
            futures[0].get();
        } catch (final InterruptedException ignore) {
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }

        // Wait for other tasks to complete.
        // Copy the probabilities and midpoints of the first task directly to the main array and merge each
        // subsequent
        // task into that array.
        // TODO This merge could be parallelized by splitting the vector array in segments
        for (int i = 1; i < threads; i++) {
            try {
                final CartesianProductVector partialCpv = futures[i].get();
                final float[] cartesianProductProbabilities = cpvProbabilities[0];
                final short[] cartesianProductMidpoints = cpvMidpoints[0];
                // Merge partial cartesian-product vector into the main vector (tropical semiring - choose
                // maximum
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
            for (int i = grammar.csrBinaryRowOffsets[parent]; i < grammar.csrBinaryRowOffsets[parent + 1]; i++) {
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
                packedArrayCell.tmpCell.packedChildren[parent] = winningChildren;
                packedArrayCell.tmpCell.insideProbabilities[parent] = winningProbability;
                packedArrayCell.tmpCell.midpoints[parent] = winningMidpoint;
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            executor.shutdown();
        } catch (final Exception ignore) {
        }
    }
}
