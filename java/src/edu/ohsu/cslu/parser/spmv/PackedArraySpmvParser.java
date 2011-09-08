/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;
import java.util.LinkedList;

import jsr166y.ForkJoinPool;
import jsr166y.ForkJoinTask;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * Base class for CSC and CSR SpMV parsers.
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2010
 */
public abstract class PackedArraySpmvParser<G extends SparseMatrixGrammar> extends
        SparseMatrixVectorParser<G, PackedArrayChart> {

    protected final ForkJoinPool threadPool;
    protected final LinkedList<ForkJoinTask<?>> currentTasks;

    private final int rowThreads;

    protected final ThreadLocal<float[]> threadLocalCpvProbabilities;
    protected final ThreadLocal<short[]> threadLocalCpvMidpoints;

    public PackedArraySpmvParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);

        final ConfigProperties props = GlobalConfigProperties.singleton();

        if (props.containsKey(ParserDriver.OPT_CELL_THREAD_COUNT)
                || props.containsKey(ParserDriver.OPT_GRAMMAR_THREAD_COUNT)) {

            this.rowThreads = props.containsKey(ParserDriver.OPT_CELL_THREAD_COUNT) ? props
                    .getIntProperty(ParserDriver.OPT_CELL_THREAD_COUNT) : 1;
            final int cellThreads = props.containsKey(ParserDriver.OPT_GRAMMAR_THREAD_COUNT) ? props
                    .getIntProperty(ParserDriver.OPT_GRAMMAR_THREAD_COUNT) : 1;

            final int threads = rowThreads * cellThreads;

            GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_CONFIGURED_THREAD_COUNT,
                    Integer.toString(threads));

            // Configure thread pool and current-task list
            this.threadPool = new ForkJoinPool(threads);
            this.currentTasks = new LinkedList<ForkJoinTask<?>>();

        } else {
            this.rowThreads = 1;
            this.currentTasks = null;
            this.threadPool = null;
        }

        // And thread-local cartesian-product vector storage
        this.threadLocalCpvProbabilities = new ThreadLocal<float[]>() {

            @Override
            protected float[] initialValue() {
                return new float[grammar.packingFunction.packedArraySize()];
            }
        };

        this.threadLocalCpvMidpoints = new ThreadLocal<short[]>() {

            @Override
            protected short[] initialValue() {
                final short[] m = new short[grammar.packingFunction.packedArraySize()];
                Arrays.fill(m, (short) 0);
                return m;
            }
        };
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        final int sentLength = parseTask.sentenceLength();
        if (chart != null && chart.size() >= sentLength) {
            chart.reset(parseTask);
        } else {
            chart = new PackedArrayChart(parseTask, grammar, beamWidth, lexicalRowBeamWidth);
        }

        super.initSentence(parseTask);
    }

    @Override
    protected void computeInsideProbabilities(final short start, final short end) {

        if (threadPool != null && rowThreads > 1) {
            currentTasks.add(threadPool.submit(new Runnable() {

                @Override
                public void run() {
                    internalComputeInsideProbabilities(start, end);
                }
            }));

        } else {
            internalComputeInsideProbabilities(start, end);
        }
    }

    @Override
    public void waitForActiveTasks() {
        if (threadPool != null) {
            for (final ForkJoinTask<?> t : currentTasks) {
                t.join();
            }
            currentTasks.clear();
        }
    }

    @Override
    protected final void internalComputeInsideProbabilities(final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;
        long t1 = t0, t2 = t0;

        final PackedArrayChartCell spvChartCell = chart.getCell(start, end);

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);

            if (collectDetailedStatistics) {
                sentenceCartesianProductSize += cartesianProductVector.size();
                t1 = System.nanoTime();
                final long xProductTime = (t1 - t0);
                sentenceCartesianProductTime += xProductTime;
                totalCartesianProductTime += xProductTime;
            }

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmv(cartesianProductVector, spvChartCell);

            if (collectDetailedStatistics) {
                t2 = System.nanoTime();
                totalBinarySpmvNs += t2 - t1;
                chart.parseTask.insideBinaryNs += t2 - t0;
            }
        }

        // We don't need to process unaries in cells only open to factored non-terminals
        if (cellSelector.hasCellConstraints() && cellSelector.getCellConstraints().isCellOnlyFactored(start, end)) {
            spvChartCell.finalizeCell();

        } else {
            if (exhaustiveSearch) {
                unarySpmv(spvChartCell);
                spvChartCell.finalizeCell();

            } else {
                final int[] cellPackedChildren = new int[grammar.numNonTerms()];
                final float[] cellInsideProbabilities = new float[grammar.numNonTerms()];
                final short[] cellMidpoints = new short[grammar.numNonTerms()];
                unaryAndPruning(spvChartCell, start, end, cellPackedChildren, cellInsideProbabilities, cellMidpoints);

                spvChartCell.finalizeCell(cellPackedChildren, cellInsideProbabilities, cellMidpoints);
            }
        }

        if (collectDetailedStatistics) {
            chart.parseTask.unaryAndPruningNs = System.nanoTime() - t2;
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

        if (grammar.packingFunction instanceof PerfectIntPairHashPackingFunction) {
            return internalCartesianProduct(start, end, start + 1, end - 1,
                    (PerfectIntPairHashPackingFunction) grammar.packingFunction, chart.nonTerminalIndices,
                    chart.insideProbabilities, threadLocalCpvProbabilities.get(), threadLocalCpvMidpoints.get());
        }

        return internalCartesianProduct(start, end, start + 1, end - 1, grammar.packingFunction,
                chart.nonTerminalIndices, chart.insideProbabilities, threadLocalCpvProbabilities.get(),
                threadLocalCpvMidpoints.get());
    }

    private final CartesianProductVector internalCartesianProduct(final int start, final int end,
            final int midpointStart, final int midpointEnd, final PackingFunction pf, final short[] nonTerminalIndices,
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

                final short minRightSibling = grammar.minRightSiblingIndices[leftChild];
                final short maxRightSibling = grammar.maxRightSiblingIndices[leftChild];

                for (int j = rightStart; j <= rightEnd; j++) {
                    // Skip any right children which cannot combine with left child
                    if (nonTerminalIndices[j] < minRightSibling) {
                        continue;
                    } else if (nonTerminalIndices[j] > maxRightSibling) {
                        break;
                    }

                    final int childPair = pf.pack(leftChild, nonTerminalIndices[j]);
                    if (childPair == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float jointProbability = leftProbability + insideProbabilities[j];

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

    /**
     * Duplicate internalCartesianProduct method, specific to {@link PerfectIntPairHashPackingFunction}.
     * 
     * @param start
     * @param end
     * @param midpointStart
     * @param midpointEnd
     * @param cpf
     * @param nonTerminalIndices
     * @param insideProbabilities
     * @param probabilities
     * @param midpoints
     * @return Cartesian Product Vector
     */
    protected final CartesianProductVector internalCartesianProduct(final int start, final int end,
            final int midpointStart, final int midpointEnd, final PerfectIntPairHashPackingFunction cpf,
            final short[] nonTerminalIndices, final float[] insideProbabilities, final float[] probabilities,
            final short[] midpoints) {

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

                    final int childPair = cpf.pack(nonTerminalIndices[j], shift, mask, offset);
                    if (childPair == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float jointProbability = leftProbability + insideProbabilities[j];

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
    public void shutdown() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    @Override
    public void finalize() {
        shutdown();
    }
}
