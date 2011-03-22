package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

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

    protected final int beamWidth;
    protected final int lexicalRowBeamWidth;
    protected final int lexicalRowUnaries;

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

        // Pruning Parameters
        final ConfigProperties props = GlobalConfigProperties.singleton();
        if (props.containsKey("maxBeamWidth")) {
            beamWidth = props.getIntProperty("maxBeamWidth");
            lexicalRowBeamWidth = props.getIntProperty("lexicalRowBeamWidth");
            lexicalRowUnaries = props.getIntProperty("lexicalRowUnaries");
        } else {
            beamWidth = grammar.numNonTerms();
            lexicalRowBeamWidth = grammar.numNonTerms();
            lexicalRowUnaries = 0;
        }
    }

    @Override
    protected void initSentence(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            chart = new PackedArrayChart(tokens, grammar, beamWidth, lexicalRowBeamWidth);
        }

        super.initSentence(tokens);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        if (executor != null) {
            currentTasks.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    internalVisitCell(start, end);
                }
            }));

        } else {
            internalVisitCell(start, end);
        }
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

    @Override
    protected void internalVisitCell(final short start, final short end) {

        final long t0 = System.currentTimeMillis();

        final PackedArrayChartCell spvChartCell = chart.getCell(start, end);

        long t1 = t0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);

            if (collectDetailedStatistics) {
                sentenceCartesianProductSize += cartesianProductVector.size();
                t1 = System.currentTimeMillis();
                final long xProductTime = (t1 - t0);
                sentenceCartesianProductTime += xProductTime;
                totalCartesianProductTime += xProductTime;
            }

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmv(cartesianProductVector, spvChartCell);
        }

        if (collectDetailedStatistics) {
            final long spMVTime = (System.currentTimeMillis() - t1);
            sentenceBinarySpMVTime += spMVTime;
            totalBinarySpMVTime += spMVTime;
        }

        final int[] cellPackedChildren = new int[grammar.numNonTerms()];
        final float[] cellInsideProbabilities = new float[grammar.numNonTerms()];
        final short[] cellMidpoints = new short[grammar.numNonTerms()];
        internalUnaryAndPruning(spvChartCell, start, end, cellPackedChildren, cellInsideProbabilities, cellMidpoints);

        spvChartCell.finalizeCell(cellPackedChildren, cellInsideProbabilities, cellMidpoints);
    }

    protected void internalUnaryAndPruning(final PackedArrayChartCell spvChartCell, final short start, final short end,
            final int[] cellPackedChildren, final float[] cellInsideProbabilities, final short[] cellMidpoints) {

        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

        // final boolean factoredOnly = cellSelector.factoredParentsOnly(start, end);
        final boolean factoredOnly = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end);

        /*
         * Populate the chart cell with the most probable n edges (n = beamWidth).
         * 
         * This operation depends on 3 data structures:
         * 
         * A) The temporary edge storage already populated with binary inside probabilities and (viterbi) backpointers
         * 
         * B) A bounded priority queue of non-terminal indices, prioritized by their figure-of-merit scores
         * 
         * C) A parallel array of edges. We will pop a limited number of edges off the priority queue into this array,
         * so this storage represents the actual cell population.
         * 
         * First, we push all binary edges onto the priority queue (if we're pruning significantly, most will not make
         * the queue). We then begin popping edges off the queue. With each edge popped, we 1) Add the edge to the array
         * of cell edges (C); and 2) Iterate through unary grammar rules with the edge parent as a child, inserting any
         * resulting unary edges to the queue. This insertion replaces the existing queue entry for the parent
         * non-terminal, if greater, and updates the inside probability and backpointer in (A).
         */

        // Push all binary or lexical edges onto a bounded priority queue
        final int cellBeamWidth = (end - start == 1 ? lexicalRowBeamWidth : Math.min(
                cellSelector.getBeamWidth(start, end), beamWidth));
        final BoundedPriorityQueue q = new BoundedPriorityQueue(cellBeamWidth, grammar);

        final float[] tmpFoms = new float[grammar.numNonTerms()];
        Arrays.fill(tmpFoms, Float.NEGATIVE_INFINITY);

        if (end - start == 1) {
            // Limit the queue to the number of non-unary productions allowed
            q.setMaxSize(lexicalRowBeamWidth - lexicalRowUnaries);

            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (spvChartCell.tmpInsideProbabilities[nt] != Float.NEGATIVE_INFINITY) {
                    final float fom = edgeSelector.calcLexicalFOM(start, end, nt,
                            spvChartCell.tmpInsideProbabilities[nt]);
                    q.insert(nt, fom);
                    tmpFoms[nt] = fom;
                }
            }
            // Expand the queue to allow a few entries for unary productions
            q.setMaxSize(lexicalRowBeamWidth);

        } else {
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (spvChartCell.tmpInsideProbabilities[nt] != Float.NEGATIVE_INFINITY) {
                    final float fom = edgeSelector.calcFOM(start, end, nt, spvChartCell.tmpInsideProbabilities[nt]);
                    q.insert(nt, fom);
                    tmpFoms[nt] = fom;
                }
            }
        }

        final float[] cellFoms = new float[grammar.numNonTerms()];
        Arrays.fill(cellInsideProbabilities, Float.NEGATIVE_INFINITY);
        Arrays.fill(cellFoms, Float.NEGATIVE_INFINITY);

        // Pop edges off the queue until we fill the beam width. With each non-terminal popped off the queue, push
        // unary edges for each unary grammar rule with the non-terminal as a child
        for (int edgesPopulated = 0; edgesPopulated < cellBeamWidth && q.size() > 0;) {

            final int headIndex = q.headIndex();
            final short nt = q.parentIndices[headIndex];
            final float fom = q.foms[headIndex];
            q.popHead();

            if (cellFoms[nt] == Float.NEGATIVE_INFINITY) {
                cellPackedChildren[nt] = spvChartCell.tmpPackedChildren[nt];
                cellInsideProbabilities[nt] = spvChartCell.tmpInsideProbabilities[nt];
                cellFoms[nt] = fom;
                cellMidpoints[nt] = spvChartCell.tmpMidpoints[nt];

                // Process unary edges for cells which are open to non-factored parents
                if (!factoredOnly) {
                    // Insert all unary edges with the current parent as child into the queue
                    final short child = nt;

                    // Iterate over possible parents of the child (rows with non-zero entries)
                    for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                        final short parent = grammar.cscUnaryRowIndices[i];
                        final float jointProbability = grammar.cscUnaryProbabilities[i]
                                + spvChartCell.tmpInsideProbabilities[child];
                        final float parentFom = edgeSelector.calcFOM(start, end, parent, jointProbability);

                        if (parentFom > tmpFoms[parent] && parentFom > cellFoms[parent] && q.replace(parent, parentFom)) {
                            // The FOM was high enough that the edge was added to the queue; update temporary storage
                            // (A) to reflect the new unary child and probability
                            spvChartCell.tmpPackedChildren[parent] = grammar.cartesianProductFunction()
                                    .packUnary(child);
                            spvChartCell.tmpInsideProbabilities[parent] = jointProbability;
                        }
                    }
                }

                edgesPopulated++;

            } else if (fom > cellFoms[nt]) {
                // We just re-popped a non-terminal we've already seen (meaning a unary which was added to the
                // queue). Replace the existing edge with the new unary edge.
                cellPackedChildren[nt] = spvChartCell.tmpPackedChildren[nt];
                cellInsideProbabilities[nt] = spvChartCell.tmpInsideProbabilities[nt];
                cellMidpoints[nt] = end;
                cellFoms[nt] = fom;
            }
        }

        if (collectDetailedStatistics) {
            sentencePruningTime += System.currentTimeMillis() - t0;
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
