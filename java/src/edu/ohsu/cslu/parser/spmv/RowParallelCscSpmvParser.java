package edu.ohsu.cslu.parser.spmv;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;

public class RowParallelCscSpmvParser extends CscSpmvParser {

    /** The number of threads configured for cell processing */
    private final int threads;

    private final ExecutorService executor;

    private LinkedList<Future<?>> currentTasks;

    private final ThreadLocal<float[]> cpvProbabilities;
    private final ThreadLocal<short[]> cpvMidpoints;

    public RowParallelCscSpmvParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);

        this.threads = GlobalConfigProperties.singleton().getIntProperty(ParserDriver.OPT_REQUESTED_THREAD_COUNT);
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_CONFIGURED_THREAD_COUNT,
                Integer.toString(threads));
        this.currentTasks = new LinkedList<Future<?>>();

        // Configure a thread pool
        executor = Executors.newFixedThreadPool(threads);

        cpvProbabilities = new ThreadLocal<float[]>() {
            @Override
            protected float[] initialValue() {
                return new float[grammar.packingFunction.packedArraySize()];
            }
        };

        cpvMidpoints = new ThreadLocal<short[]>() {
            @Override
            protected short[] initialValue() {
                return new short[grammar.packingFunction.packedArraySize()];
            }
        };
    }

    @Override
    protected void visitCell(final short start, final short end) {

        currentTasks.add(executor.submit(new Runnable() {
            @Override
            public void run() {
                internalVisitCell(start, end);
            }
        }));
    }

    @Override
    public void waitForActiveTasks() {
        try {
            for (final Future<?> f : currentTasks) {
                f.get();
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }
        currentTasks.clear();
    }

    private void internalVisitCell(final short start, final short end) {
        final ParallelArrayChartCell spvChartCell = chart.getCell(start, end);

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        long t2 = t0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CartesianProductVector cartesianProductVector = internalCartesianProduct(start, end, start + 1,
                    end - 1, grammar.packingFunction, chart.nonTerminalIndices, chart.insideProbabilities,
                    cpvProbabilities.get(), cpvMidpoints.get());

            if (collectDetailedStatistics) {
                sentenceCartesianProductSize += cartesianProductVector.size();
                t1 = System.currentTimeMillis();
                final long time = t1 - t0;
                sentenceCartesianProductTime += time;
                totalCartesianProductTime += time;
            }

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmv(cartesianProductVector, spvChartCell);
        }

        if (collectDetailedStatistics) {
            t2 = System.currentTimeMillis();
            final long time = t2 - t1;
            sentenceBinarySpMVTime += time;
            totalBinarySpMVTime += time;
        }

        // Handle unary productions
        // This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        final boolean factoredOnly = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end);
        // if (!cellSelector.factoredParentsOnly(start, end)) {
        if (!factoredOnly) {
            unarySpmv(spvChartCell);
        }

        if (collectDetailedStatistics) {
            sentenceUnaryTime += (System.currentTimeMillis() - t2);

            sentenceCellPopulation += spvChartCell.getNumNTs();
            if (spvChartCell instanceof PackedArrayChartCell) {
                sentenceLeftChildPopulation += ((PackedArrayChartCell) spvChartCell).leftChildren();
                sentenceRightChildPopulation += ((PackedArrayChartCell) spvChartCell).rightChildren();
            }
        }

        // Pack the temporary cell storage into the main chart array
        if (collectDetailedStatistics) {
            final long t3 = System.currentTimeMillis();
            spvChartCell.finalizeCell();
            sentenceFinalizeTime += (System.currentTimeMillis() - t3);
        } else {
            spvChartCell.finalizeCell();
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void finalize() {
        shutdown();
    }
}
