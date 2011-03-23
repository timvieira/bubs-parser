package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
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

    // TODO Remove - add threads to PackedArraySpmvParser.executor instead
    private final ExecutorService cellExecutor;

    // private Future<?> cpvClearTask;

    protected final TemporaryChartCell[] temporaryCells;

    public CellParallelCscSpmvParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);

        final ConfigProperties props = GlobalConfigProperties.singleton();
        // Split the binary grammar rules into segments of roughly equal size
        final int requestedThreads = props.getIntProperty(ParserDriver.OPT_CELL_THREAD_COUNT);
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
        final int configuredThreads = props.containsKey(ParserDriver.OPT_ROW_THREAD_COUNT) ? props
                .getIntProperty(ParserDriver.OPT_ROW_THREAD_COUNT) * threads : threads;
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_CONFIGURED_THREAD_COUNT,
                Integer.toString(configuredThreads));

        this.binaryRowSegments = new int[i + 1];
        System.arraycopy(segments, 0, binaryRowSegments, 0, binaryRowSegments.length);

        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 1; j < binaryRowSegments.length; j++) {
                sb.append((grammar.cscBinaryPopulatedColumnOffsets[binaryRowSegments[j]] - grammar.cscBinaryPopulatedColumnOffsets[binaryRowSegments[j - 1]])
                        + " ");
            }
            BaseLogger.singleton().fine("CSC Binary Grammar segments of length: " + sb.toString());
        }

        // Configure a thread pool
        this.cellExecutor = Executors.newFixedThreadPool(threads);

        // Preallocate temporary cell storage
        this.temporaryCells = new TemporaryChartCell[threads];
        for (int j = 0; j < threads; j++) {
            temporaryCells[j] = new TemporaryChartCell();
        }
    }

    @Override
    protected void initSentence(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            chart = new PackedArrayChart(tokens, grammar, beamWidth, lexicalRowBeamWidth, threads);
        }

        super.initSentence(tokens);
    }

    // @Override
    // protected void visitCell(final short start, final short end) {
    //
    // // Wait for the array fill task
    // if (cpvClearTask != null) {
    // try {
    // cpvClearTask.get();
    // } catch (final InterruptedException e) {
    // e.printStackTrace();
    // } catch (final ExecutionException e) {
    // e.printStackTrace();
    // }
    // }
    //
    // // TODO Auto-generated method stub
    // super.visitCell(start, end);
    //
    // // Schedule a task to clear the cartesian-product vector storage
    // cellExecutor.execute(new Runnable() {
    // @Override
    // public void run() {
    // Arrays.fill(threadLocalCpvMidpoints.get(), (short) 0);
    // }
    // });
    // }

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

        final float[] cpvProbabilities = threadLocalCpvProbabilities.get();
        final short[] cpvMidpoints = threadLocalCpvMidpoints.get();

        Arrays.fill(cpvMidpoints, (short) 0);

        // // Don't bother multi-threading for fewer than 4 midpoints
        // if (end - start - 1 < 4) {
        // return internalCartesianProduct(start, end, start + 1, end - 1, pf, nonTerminalIndices,
        // insideProbabilities, cpvProbabilities, cpvMidpoints);
        // }

        // Perform cartesian-product operation for each left-child segment
        final Future<?>[] futures = new Future[threads];

        for (int i = 0; i < threads; i++) {
            final int segment = i;
            futures[i] = cellExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    cartesianProductSegment(start, end, segment, pf, nonTerminalIndices, insideProbabilities,
                            cpvProbabilities, cpvMidpoints);
                }
            });
        }

        // Wait for CPV tasks to complete.
        for (int i = 0; i < threads; i++) {
            try {
                futures[i].get();
            } catch (final InterruptedException ignore) {
            } catch (final ExecutionException e) {
                e.printStackTrace();
            }
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

            final int leftChildrenStartIndex = chart.leftChildSegmentStartIndices[leftCellIndex * (threads + 1)
                    + segment];
            final int leftChildrenEndIndex = chart.leftChildSegmentStartIndices[leftCellIndex * (threads + 1) + segment
                    + 1];

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            for (int i = leftChildrenStartIndex; i <= leftChildrenEndIndex; i++) {
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
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final Future<?>[] futures = new Future[threads];
        // Iterate over binary grammar segments
        for (int i = 0; i < threads; i++) {
            final int segmentStart = binaryRowSegments[i];
            final int segmentEnd = binaryRowSegments[i + 1];
            final TemporaryChartCell tmpCell = temporaryCells[i];

            if (cellSelector.hasCellConstraints()
                    && cellSelector.getCellConstraints().isCellOnlyFactored(chartCell.start(), chartCell.end())) {
                futures[i] = cellExecutor.submit(new Runnable() {

                    @Override
                    public void run() {
                        // Multiply by the factored grammar rule matrix
                        binarySpmvMultiply(cartesianProductVector, grammar.factoredCscBinaryPopulatedColumns,
                                grammar.factoredCscBinaryPopulatedColumnOffsets, grammar.factoredCscBinaryRowIndices,
                                grammar.factoredCscBinaryProbabilities, tmpCell.packedChildren,
                                tmpCell.insideProbabilities, tmpCell.midpoints, segmentStart, segmentEnd);
                    }
                });
            } else {
                futures[i] = cellExecutor.submit(new Runnable() {

                    @Override
                    public void run() {
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

            System.arraycopy(temporaryCells[0].insideProbabilities, 0, packedArrayCell.tmpInsideProbabilities, 0,
                    temporaryCells[0].insideProbabilities.length);
            System.arraycopy(temporaryCells[0].packedChildren, 0, packedArrayCell.tmpPackedChildren, 0,
                    temporaryCells[0].packedChildren.length);
            System.arraycopy(temporaryCells[0].midpoints, 0, packedArrayCell.tmpMidpoints, 0,
                    temporaryCells[0].midpoints.length);
            temporaryCells[0].clear();

            // packedArrayCell.tmpInsideProbabilities = tmpCell0.insideProbabilities;
            // packedArrayCell.tmpPackedChildren = tmpCell0.packedChildren;
            // packedArrayCell.tmpMidpoints = tmpCell0.midpoints;

            // Wait for each other task to complete and merge results into the main temporary storage
            for (int i = 1; i < threads; i++) {
                futures[i].get();
                final TemporaryChartCell tmpCell = temporaryCells[i];
                for (int j = 0; j < tmpCell.insideProbabilities.length; j++) {
                    if (tmpCell.insideProbabilities[j] > packedArrayCell.tmpInsideProbabilities[j]) {
                        packedArrayCell.tmpInsideProbabilities[j] = tmpCell.insideProbabilities[j];
                        packedArrayCell.tmpPackedChildren[j] = tmpCell.packedChildren[j];
                        packedArrayCell.tmpMidpoints[j] = tmpCell.midpoints[j];
                    }
                }
                tmpCell.clear();
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        cellExecutor.shutdown();
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
