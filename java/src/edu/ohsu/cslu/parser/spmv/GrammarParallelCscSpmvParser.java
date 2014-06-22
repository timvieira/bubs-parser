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

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;

/**
 * Distributes the binary SpMV operation across multiple threads.
 * 
 * @author Aaron Dunlop
 * @since Mar 10, 2011
 */
public final class GrammarParallelCscSpmvParser extends CscSpmvParser {

    /** The number of threads configured for cartesian-product and binary grammar intersection */
    private final int grammarThreads;

    /**
     * The number of tasks to split cartesian-product operation into. Normally a multiple of the number of threads.
     */
    private final int cpvSegments;

    // TODO Fix this documentation
    /**
     * Offsets into {@link CsrSparseMatrixGrammar#csrBinaryRowOffsets} splitting the binary rule-set into segments of
     * roughly equal size for distribution between threads. Length is {@link #grammarThreads} + 1 (to avoid falling off
     * the end)
     */
    private final int[] binaryRowSegments;

    private final ThreadLocal<PackedArrayChart.TemporaryChartCell[]> threadLocalTemporaryCellArrays;

    public GrammarParallelCscSpmvParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);

        final ConfigProperties props = GlobalConfigProperties.singleton();
        // Split the binary grammar rules into segments of roughly equal size
        final int requestedThreads = props.getIntProperty(ParserDriver.OPT_GRAMMAR_THREAD_COUNT);
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

        this.grammarThreads = i;
        this.cpvSegments = grammarThreads * 2;
        GlobalConfigProperties.singleton().setProperty(ParserDriver.RUNTIME_CONFIGURED_THREAD_COUNT,
                Integer.toString(props.getIntProperty(ParserDriver.OPT_CELL_THREAD_COUNT, 1) * grammarThreads));

        this.binaryRowSegments = new int[i + 1];
        System.arraycopy(segments, 0, binaryRowSegments, 0, binaryRowSegments.length);

        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 1; j < binaryRowSegments.length; j++) {
                sb.append((grammar.cscBinaryPopulatedColumnOffsets[binaryRowSegments[j]] - grammar.cscBinaryPopulatedColumnOffsets[binaryRowSegments[j - 1]])
                        + " ");
            }
            BaseLogger.singleton().fine("INFO: CSC Binary Grammar segments of length: " + sb.toString());
        }

        // Temporary cell storage for each grammar-level thread
        this.threadLocalTemporaryCellArrays = new ThreadLocal<PackedArrayChart.TemporaryChartCell[]>() {

            @Override
            protected PackedArrayChart.TemporaryChartCell[] initialValue() {
                final PackedArrayChart.TemporaryChartCell[] tcs = new PackedArrayChart.TemporaryChartCell[grammarThreads];
                for (int j = 0; j < grammarThreads; j++) {
                    tcs[j] = new PackedArrayChart.TemporaryChartCell(grammar, false);
                }
                return tcs;
            }
        };
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        initSentence(parseTask, beamWidth, lexicalRowBeamWidth, lexicalRowUnaries, maxLocalDelta);
    }

    @Override
    public int leftChildSegments() {
        return cpvSegments;
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

        final PerfectIntPairHashPackingFunction pf = (PerfectIntPairHashPackingFunction) grammar.packingFunction();
        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        final float[] cpvProbabilities = threadLocalCpvProbabilities.get();
        final short[] cpvMidpoints = threadLocalCpvMidpoints.get();

        Arrays.fill(cpvMidpoints, (short) 0);

        // // Don't bother multi-threading for fewer than 4 midpoints
        // if (end - start - 1 < 4) {
        // return internalCartesianProduct(start, end, start + 1, end - 1, pf, nonTerminalIndices,
        // insideProbabilities, cpvProbabilities, cpvMidpoints);
        // }

        // Perform cartesian-product operation for each left-child segment
        final ForkJoinTask<?>[] futures = new ForkJoinTask[cpvSegments];

        for (int i = 0; i < cpvSegments; i++) {
            final int segment = i;
            futures[i] = threadPool.submit(new Runnable() {

                @Override
                public void run() {
                    cartesianProductSegment(start, end, segment, pf, nonTerminalIndices, insideProbabilities,
                            cpvProbabilities, cpvMidpoints);
                }
            });
        }

        // Wait for CPV tasks to complete.
        for (int i = 0; i < cpvSegments; i++) {
            futures[i].join();
        }

        return new CartesianProductVector(grammar, cpvProbabilities, cpvMidpoints, 0);
    }

    private void cartesianProductSegment(final int start, final int end, final int segment,
            final PerfectIntPairHashPackingFunction cpf, final short[] chartNonTerminalIndices,
            final float[] chartInsideProbabilities, final float[] cpvProbabilities, final short[] cpvMidpoints) {

        // Iterate over all possible midpoints, populating the cartesian product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 1); midpoint <= (end - 1); midpoint++) {
            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);

            final int leftChildrenStartIndex = chart.leftChildSegmentStartIndices[leftCellIndex * (cpvSegments + 1)
                    + segment];
            final int leftChildrenEndIndex = chart.leftChildSegmentStartIndices[leftCellIndex * (cpvSegments + 1)
                    + segment + 1];

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            for (int i = leftChildrenStartIndex; i < leftChildrenEndIndex; i++) {
                final short leftChild = chartNonTerminalIndices[i];
                final float leftProbability = chartInsideProbabilities[i];
                final int mask = cpf.mask(leftChild);
                final int shift = cpf.shift(leftChild);
                final int offset = cpf.offset(leftChild);

                final short minRightSibling = grammar.minRightSiblingIndices[leftChild];
                final short maxRightSibling = grammar.maxRightSiblingIndices[leftChild];

                for (int j = rightStart; j <= rightEnd; j++) {
                    // Skip any right children which cannot combine with left child
                    if (chartNonTerminalIndices[j] < minRightSibling) {
                        continue;
                    } else if (chartNonTerminalIndices[j] > maxRightSibling) {
                        break;
                    }

                    final int childPair = cpf.pack(chartNonTerminalIndices[j], shift, mask, offset);
                    if (childPair == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float jointProbability = leftProbability + chartInsideProbabilities[j];

                    // If this cartesian-product entry is not populated, we can populate it without comparing
                    // to a current probability. The memory write is faster if we don't first have to read.
                    if (cpvMidpoints[childPair] == 0) {
                        cpvProbabilities[childPair] = jointProbability;
                        cpvMidpoints[childPair] = midpoint;

                    } else {
                        if (jointProbability > cpvProbabilities[childPair]) {
                            cpvProbabilities[childPair] = jointProbability;
                            cpvMidpoints[childPair] = midpoint;
                        }
                    }
                }
            }
        }
    }

    @Override
    public final void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final Future<?>[] futures = new Future[grammarThreads];
        final PackedArrayChart.TemporaryChartCell[] temporaryCells = threadLocalTemporaryCellArrays.get();

        // Iterate over binary grammar segments
        for (int i = 0; i < grammarThreads; i++) {
            final int segmentStart = binaryRowSegments[i];
            final int segmentEnd = binaryRowSegments[i + 1];
            final PackedArrayChart.TemporaryChartCell tmpCell = temporaryCells[i];

            if (cellSelector.hasCellConstraints()
                    && cellSelector.isCellOnlyFactored(chartCell.start(), chartCell.end())) {
                futures[i] = threadPool.submit(new Runnable() {

                    @Override
                    public void run() {
                        tmpCell.clear();
                        // Multiply by the factored grammar rule matrix
                        binarySpmvMultiply(cartesianProductVector, grammar.factoredCscBinaryPopulatedColumns,
                                grammar.factoredCscBinaryPopulatedColumnOffsets, grammar.factoredCscBinaryRowIndices,
                                grammar.factoredCscBinaryProbabilities, tmpCell.packedChildren,
                                tmpCell.insideProbabilities, tmpCell.midpoints, segmentStart, segmentEnd);
                    }
                });
            } else {
                futures[i] = threadPool.submit(new Runnable() {

                    @Override
                    public void run() {
                        tmpCell.clear();
                        // Multiply by the full grammar rule matrix
                        binarySpmvMultiply(cartesianProductVector, grammar.cscBinaryPopulatedColumns,
                                grammar.cscBinaryPopulatedColumnOffsets, grammar.cscBinaryRowIndices,
                                grammar.cscBinaryProbabilities, tmpCell.packedChildren, tmpCell.insideProbabilities,
                                tmpCell.midpoints, segmentStart, segmentEnd);
                    }
                });
            }
        }

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();
        try {
            // Wait for the first task to finish and use its arrays as the temporary cell storage
            futures[0].get();

            final TemporaryChartCell tmpCell = packedArrayCell.tmpCell;
            // TODO Eliminate this extra arraycopy
            final int arrayLength = temporaryCells[0].insideProbabilities.length;
            System.arraycopy(temporaryCells[0].insideProbabilities, 0, tmpCell.insideProbabilities, 0, arrayLength);
            System.arraycopy(temporaryCells[0].packedChildren, 0, tmpCell.packedChildren, 0, arrayLength);
            System.arraycopy(temporaryCells[0].midpoints, 0, tmpCell.midpoints, 0, arrayLength);

            // Wait for each other task to complete and merge results into the main temporary storage
            for (int i = 1; i < grammarThreads; i++) {
                futures[i].get();
                final PackedArrayChart.TemporaryChartCell threadTmpCell = temporaryCells[i];
                for (int j = 0; j < arrayLength; j++) {
                    if (threadTmpCell.insideProbabilities[j] > tmpCell.insideProbabilities[j]) {
                        tmpCell.insideProbabilities[j] = threadTmpCell.insideProbabilities[j];
                        tmpCell.packedChildren[j] = threadTmpCell.packedChildren[j];
                        tmpCell.midpoints[j] = threadTmpCell.midpoints[j];
                    }
                }
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }
    }
}
