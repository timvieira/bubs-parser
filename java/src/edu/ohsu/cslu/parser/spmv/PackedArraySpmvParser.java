package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;

/**
 * Base class for CSC and CSR SpMV parsers.
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class PackedArraySpmvParser<G extends SparseMatrixGrammar> extends
        SparseMatrixVectorParser<G, PackedArrayChart> {

    protected final ExecutorService executor;
    protected LinkedList<Future<?>> currentTasks;

    protected final ThreadLocal<float[]> cpvProbabilities;
    protected final ThreadLocal<short[]> cpvMidpoints;

    public PackedArraySpmvParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);

        if (GlobalConfigProperties.singleton().containsKey(ParserDriver.OPT_ROW_THREAD_COUNT)) {
            final int rowThreads = GlobalConfigProperties.singleton().getIntProperty(ParserDriver.OPT_ROW_THREAD_COUNT);
            GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_CONFIGURED_THREAD_COUNT,
                    Integer.toString(rowThreads));

            // Configure thread pool and current-task list
            this.executor = Executors.newFixedThreadPool(rowThreads);
            this.currentTasks = new LinkedList<Future<?>>();

        } else {
            this.currentTasks = null;
            this.executor = null;
        }

        // And thread-local cartesian-product vector storage
        this.cpvProbabilities = new ThreadLocal<float[]>() {
            @Override
            protected float[] initialValue() {
                return new float[grammar.packingFunction.packedArraySize()];
            }
        };

        this.cpvMidpoints = new ThreadLocal<short[]>() {
            @Override
            protected short[] initialValue() {
                return new short[grammar.packingFunction.packedArraySize()];
            }
        };
    }

    @Override
    protected void initSentence(final int[] tokens) {
        initSentence(tokens, grammar.numNonTerms(), grammar.numNonTerms());
    }

    protected void initSentence(final int[] tokens, final int beamWidth, final int lexicalRowBeamWidth) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            chart = new PackedArrayChart(tokens, grammar, beamWidth, lexicalRowBeamWidth);
        }

        super.initSentence(tokens);
    }

    @Override
    public void waitForActiveTasks() {
        if (executor != null) {
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
                    chart.insideProbabilities, cpvProbabilities.get(), cpvMidpoints.get());
        }

        return internalCartesianProduct(start, end, start + 1, end - 1, grammar.packingFunction,
                chart.nonTerminalIndices, chart.insideProbabilities, cpvProbabilities.get(), cpvMidpoints.get());
    }

    protected final CartesianProductVector internalCartesianProduct(final int start, final int end,
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
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public void finalize() {
        shutdown();
    }
}
