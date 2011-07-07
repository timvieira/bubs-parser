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
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
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

    protected final int beamWidth;
    protected final int lexicalRowBeamWidth;
    protected final int lexicalRowUnaries;
    private final boolean exhaustiveSearch;

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

        // Pruning Parameters
        if (props.containsKey("maxBeamWidth") && props.getIntProperty("maxBeamWidth") > 0) {
            beamWidth = props.getIntProperty("maxBeamWidth");
            lexicalRowBeamWidth = props.getIntProperty("lexicalRowBeamWidth");
            lexicalRowUnaries = props.getIntProperty("lexicalRowUnaries");
            this.exhaustiveSearch = false;
        } else {
            beamWidth = grammar.numNonTerms();
            lexicalRowBeamWidth = grammar.numNonTerms();
            lexicalRowUnaries = 0;
            this.exhaustiveSearch = true;
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

        if (threadPool != null && rowThreads > 1) {
            currentTasks.add(threadPool.submit(new Runnable() {

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
        if (threadPool != null) {
            for (final ForkJoinTask<?> t : currentTasks) {
                t.join();
            }
            currentTasks.clear();
        }
    }

    @Override
    protected final void internalVisitCell(final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;
        long t1 = t0, t2 = t0;

        final PackedArrayChartCell spvChartCell = chart.getCell(start, end);

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
            t2 = System.currentTimeMillis();
            final long spMVTime = t2 - t1;
            sentenceBinarySpMVTime += spMVTime;
            totalBinarySpMVTime += spMVTime;
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
            sentenceUnaryTime = System.currentTimeMillis() - t2;
        }
    }

    protected void unaryAndPruning(final PackedArrayChartCell spvChartCell, final short start, final short end,
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

        // Pop edges off the queue until we fill the beam width. With each non-terminal popped off the queue,
        // push
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
                            // The FOM was high enough that the edge was added to the queue; update temporary
                            // storage
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
