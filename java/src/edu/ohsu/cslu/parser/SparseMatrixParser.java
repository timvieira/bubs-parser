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
package edu.ohsu.cslu.parser;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;
import edu.ohsu.cslu.parser.ml.ConstrainedCphSpmlParser;

/**
 * Base class for all chart parsers which represent the chart as a parallel array and operate on matrix-encoded
 * grammars.
 * 
 * @author Aaron Dunlop
 */
public abstract class SparseMatrixParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        ChartParser<G, C> {

    /**
     * Configures the number of iterations through unary grammar rules during exhaustive parsing. The default is 1, but
     * (depending on the ordering of unary rules), additional iterations may recover longer unary chains.
     */
    public final static String PROPERTY_UNARY_ITERATIONS = "unaryIterations";

    private final static int UNARY_ITERATIONS = GlobalConfigProperties.singleton().getIntProperty(
            PROPERTY_UNARY_ITERATIONS, 1);

    /** The amount to increase {@link #maxLocalDelta} at each reparsing stage */
    public final static float MAX_LOCAL_DELTA_MULTIPLIER = 1.5f;

    protected int beamWidth;
    protected int lexicalRowBeamWidth;
    protected int lexicalRowUnaries;
    protected float maxLocalDelta;
    protected boolean exhaustiveSearch;

    protected final ThreadLocal<BoundedPriorityQueue> threadLocalBoundedPriorityQueue;
    protected final ThreadLocal<float[]> threadLocalTmpFoms;
    protected final ThreadLocal<TemporaryChartCell> threadLocalQueueEdges;

    public SparseMatrixParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);

        initDefaultPruningParams();

        // If we're pruning, initialize the thread-local pruning storage
        if (beamWidth > 0 || implicitPruning()) {

            this.threadLocalBoundedPriorityQueue = new ThreadLocal<BoundedPriorityQueue>() {
                @Override
                protected BoundedPriorityQueue initialValue() {
                    return new BoundedPriorityQueue(Math.max(beamWidth, lexicalRowBeamWidth), grammar);
                }
            };
            this.threadLocalTmpFoms = new ThreadLocal<float[]>() {
                @Override
                protected float[] initialValue() {
                    return new float[grammar.numNonTerms()];
                }
            };
            this.threadLocalQueueEdges = new ThreadLocal<TemporaryChartCell>() {
                @Override
                protected TemporaryChartCell initialValue() {
                    return new TemporaryChartCell(grammar, false);
                }
            };

        } else {
            this.threadLocalBoundedPriorityQueue = null;
            this.threadLocalTmpFoms = null;
            this.threadLocalQueueEdges = null;
        }
    }

    private void initDefaultPruningParams() {

        final ConfigProperties props = GlobalConfigProperties.singleton();

        // Pruning Parameters
        if (props.containsKey(PROPERTY_MAX_BEAM_WIDTH) && props.getIntProperty(PROPERTY_MAX_BEAM_WIDTH) > 0) {
            this.beamWidth = props.getIntProperty(Parser.PROPERTY_MAX_BEAM_WIDTH);
            this.lexicalRowBeamWidth = props.getIntProperty(PROPERTY_LEXICAL_ROW_BEAM_WIDTH, beamWidth);
            this.lexicalRowUnaries = props.getIntProperty(PROPERTY_LEXICAL_ROW_UNARIES, lexicalRowBeamWidth / 3);
            this.maxLocalDelta = props.getFloatProperty(PROPERTY_MAX_LOCAL_DELTA, 15f);
            this.exhaustiveSearch = false;
        } else {
            this.beamWidth = grammar.numNonTerms();
            this.lexicalRowBeamWidth = grammar.numNonTerms();
            this.lexicalRowUnaries = grammar.numNonTerms();
            this.maxLocalDelta = 0f;
            this.exhaustiveSearch = true;
        }
    }

    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {

        // return super.findBestParse(parseTask);
        initChart(parseTask);
        parseTask.reparseStages = -1;

        for (final Parser.ReparseStrategy.Stage stage : opts.reparseStrategy.stages()) {

            final long stageStartTime = System.currentTimeMillis();
            parseTask.reparseStages++;

            switch (stage) {
            case NORMAL:
                initDefaultPruningParams();
                break;

            case FIXED_BEAM:
                initSentence(parseTask, beamWidth, lexicalRowBeamWidth, lexicalRowUnaries, maxLocalDelta, 0);
                cellSelector.reset(false);
                break;

            case DOUBLE:
                // Skip this doubling if it results in exhaustive parsing. We'll get to EXHAUSTIVE later if it's
                // included in the hierarchy.
                if ((beamWidth << 1) >= grammar.nonTermSet.size()) {
                    continue;
                }
                initSentence(parseTask, beamWidth << 1, lexicalRowBeamWidth << 1, lexicalRowUnaries << 1, maxLocalDelta
                        * MAX_LOCAL_DELTA_MULTIPLIER, 0);
                cellSelector.reset(false);
                break;

            case EXHAUSTIVE:
                initSentence(parseTask, grammar.nonTermSet.size(), grammar.nonTermSet.size(),
                        grammar.nonTermSet.size(), Float.MAX_VALUE, 0);
                cellSelector.reset(false);
                break;
            }

            insidePass();

            if (BaseLogger.singleton().isLoggable(Level.ALL)) {
                BaseLogger.singleton().finest(chart.toString());
            }

            if (chart.hasCompleteParse(grammar.startSymbol)) {
                BaseLogger.singleton().finer(
                        String.format("INFO: stage=%s time=%d success=true", stage.toString(),
                                System.currentTimeMillis() - stageStartTime));
                return chart.extractBestParse(grammar.startSymbol);
            }
            BaseLogger.singleton().finer(
                    String.format("INFO: stage=%s time=%d success=false", stage.toString(), System.currentTimeMillis()
                            - stageStartTime));
        }

        return extract(parseTask.recoveryStrategy);
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        initSentence(parseTask, beamWidth, lexicalRowBeamWidth, lexicalRowUnaries, maxLocalDelta, 0);
    }

    protected void initSentence(final ParseTask parseTask, final int newBeamWidth, final int newLexicalRowBeamWidth,
            final int newLexicalRowUnaries, final float newMaxLocalDelta, final int leftChildSegments) {

        // Set beam width parameters
        if (newBeamWidth >= grammar.nonTermSet.size()) {
            this.beamWidth = this.lexicalRowBeamWidth = this.lexicalRowUnaries = grammar.nonTermSet.size();
            this.exhaustiveSearch = true;
        } else {
            this.beamWidth = newBeamWidth;
        }
        this.lexicalRowBeamWidth = Math.min(newLexicalRowBeamWidth, grammar.nonTermSet.size());
        this.lexicalRowUnaries = Math.min(newLexicalRowUnaries, grammar.nonTermSet.size());
        this.maxLocalDelta = newMaxLocalDelta;

        // Replace the pruning priority queue if the beam width has increased beyond its capacity
        if (threadLocalBoundedPriorityQueue != null
                && Math.max(newBeamWidth, newLexicalRowBeamWidth) > threadLocalBoundedPriorityQueue.get().size()) {
            threadLocalBoundedPriorityQueue.set(new BoundedPriorityQueue(
                    Math.max(newBeamWidth, newLexicalRowBeamWidth), grammar));
        }

        if (chart != null
                && chart.chartArraySize() >= ParallelArrayChart.chartArraySize(parseTask.sentenceLength(),
                        this.beamWidth, this.lexicalRowBeamWidth)
                && chart.maxCells >= parseTask.sentenceLength() * (parseTask.sentenceLength() + 1) / 1) {
            chart.reset(parseTask, this.beamWidth, this.lexicalRowBeamWidth);
        } else {
            // Construct a chart of the appropriate type
            try {
                final Class<C> chartClass = chartClass();
                chartClass.getConstructors();
                try {
                    // First, try for a constructor that takes tokens, grammar, beamWidth, lexicalRowBeamWidth, and
                    // leftChildSegments
                    chart = chartClass
                            .getConstructor(
                                    new Class[] { ParseTask.class, SparseMatrixGrammar.class, int.class, int.class,
                                            int.class }).newInstance(
                                    new Object[] { parseTask, grammar, beamWidth, lexicalRowBeamWidth,
                                            leftChildSegments });

                } catch (final NoSuchMethodException e) {
                    try {
                        // Next, try for a constructor without leftChildSegments)
                        chart = chartClass.getConstructor(
                                new Class[] { ParseTask.class, SparseMatrixGrammar.class, int.class, int.class })
                                .newInstance(new Object[] { parseTask, grammar, beamWidth, lexicalRowBeamWidth });
                    } catch (final NoSuchMethodException e2) {
                        // And finally, a constructor that takes only tokens and grammar
                        chart = chartClass.getConstructor(new Class[] { ParseTask.class, SparseMatrixGrammar.class })
                                .newInstance(new Object[] { parseTask, grammar });
                    }
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void addLexicalProductions(final ChartCell cell) {
        if (ParserDriver.parseFromInputTags) {
            super.addLexicalProductions(cell);
        } else {
            final short start = cell.start();
            final int child = chart.parseTask.tokens[start];
            final short[] parents = grammar.lexicalParents(child);
            final float[] lexicalLogProbabilities = grammar.lexicalLogProbabilities(child);
            ((ParallelArrayChartCell) cell).storeLexicalProductions(child, parents, lexicalLogProbabilities);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<C> chartClass() throws NoSuchMethodException {

        for (Class<?> c = getClass(); c != SparseMatrixParser.class; c = c.getSuperclass()) {
            if (!(c.getGenericSuperclass() instanceof ParameterizedType)) {
                continue;
            }
            final Type[] typeArguments = ((ParameterizedType) c.getGenericSuperclass()).getActualTypeArguments();
            for (int i = 0; i < typeArguments.length; i++) {
                if (typeArguments[i] instanceof Class && Chart.class.isAssignableFrom((Class<?>) typeArguments[i])) {
                    return (Class<C>) typeArguments[i];
                }
            }
        }
        throw new NoSuchMethodException("Cannot find chart class for parser class: " + getClass().getName());
    }

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely), and
     * populates this chart cell. Used to populate unary rules.
     * 
     * @param chartCell
     */
    public void unarySpmv(final ChartCell chartCell) {

        if (chartCell instanceof PackedArrayChartCell) {
            final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
            packedArrayCell.allocateTemporaryStorage();

            unarySpmv(packedArrayCell.tmpCell.packedChildren, packedArrayCell.tmpCell.insideProbabilities,
                    packedArrayCell.tmpCell.midpoints, 0, chartCell.end());
        } else {
            final DenseVectorChartCell denseVectorCell = (DenseVectorChartCell) chartCell;

            unarySpmv(chart.packedChildren, chart.insideProbabilities, chart.midpoints, denseVectorCell.offset(),
                    chartCell.end());
        }
    }

    protected void unarySpmv(final int[] chartCellChildren, final float[] chartCellProbabilities,
            final short[] chartCellMidpoints, final int offset, final short chartCellEnd) {

        final PackingFunction cpf = grammar.packingFunction();

        for (int iteration = 0; iteration < UNARY_ITERATIONS; iteration++) {
            // Iterate over populated children (matrix columns)
            for (short child = 0; child < grammar.numNonTerms(); child++) {

                final int childOffset = offset + child;
                if (chartCellProbabilities[childOffset] == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                    final short parent = grammar.cscUnaryRowIndices[i];
                    final int parentOffset = offset + parent;
                    final float grammarProbability = grammar.cscUnaryProbabilities[i];

                    final float jointProbability = grammarProbability + chartCellProbabilities[childOffset];
                    if (jointProbability > chartCellProbabilities[parentOffset]) {
                        chartCellProbabilities[parentOffset] = jointProbability;
                        chartCellChildren[parentOffset] = cpf.packUnary(child);
                        chartCellMidpoints[parentOffset] = chartCellEnd;
                    }
                }

                if (collectDetailedStatistics) {
                    chart.parseTask.nUnaryConsidered += (grammar.cscUnaryColumnOffsets[child + 1] - grammar.cscUnaryColumnOffsets[child]);
                }
            }
        }
    }

    protected void unaryAndPruning(final ParallelArrayChartCell spvChartCell, final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final TemporaryChartCell tmpCell = spvChartCell.tmpCell;

        final int cellBeamWidth = Math.min(cellSelector.getBeamWidth(spvChartCell.cellIndex),
                (end - start == 1 ? lexicalRowBeamWidth : beamWidth));
        if (cellBeamWidth == 1) {
            // Special-case when we are pruning down to only a single entry. We can't add any unary productions, so just
            // choose the NT with the maximum FOM.
            float maxFom = Float.NEGATIVE_INFINITY;
            short maxNt = -1;
            if (end - start == 1) { // Lexical Row (span = 1)
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
                    final float fom = figureOfMerit.calcLexicalFOM(start, end, nt, tmpCell.insideProbabilities[nt]);
                    if (fom > maxFom) {
                        maxFom = fom;
                        maxNt = nt;
                    }
                }
            } else {
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
                    final float fom = figureOfMerit.calcFOM(start, end, nt, tmpCell.insideProbabilities[nt]);
                    if (fom > maxFom) {
                        maxFom = fom;
                        maxNt = nt;
                    }
                }
            }
            if (maxNt >= 0) {
                spvChartCell.finalizeCell(maxNt, tmpCell.insideProbabilities[maxNt], tmpCell.packedChildren[maxNt],
                        tmpCell.midpoints[maxNt]);
            } else {
                spvChartCell.finalizeEmptyCell();
            }

        } else {
            // General-case unary processing and pruning
            unaryAndPruning(tmpCell, cellBeamWidth, start, end);
            spvChartCell.finalizeCell();
        }

        if (collectDetailedStatistics) {
            chart.parseTask.unaryAndPruningNs += System.nanoTime() - t0;
        }
    }

    protected final void unaryAndPruning(final TemporaryChartCell tmpCell, final int cellBeamWidth, final short start,
            final short end) {

        // Ignore factored-only cell constraints in span-1 cells
        final boolean allowUnaries = !cellSelector.hasCellConstraints() || cellSelector.isUnaryOpen(start, end)
                && !(cellSelector.isCellOnlyFactored(start, end) && (end - start > 1));
        final float minInsideProbability = edu.ohsu.cslu.util.Math.floatMax(tmpCell.insideProbabilities)
                - maxLocalDelta;

        /*
         * Populate the chart cell with the most probable n edges (n = beamWidth).
         * 
         * This operation depends on 3 data structures:
         * 
         * A) The temporary edge storage already populated with binary inside probabilities and (viterbi) backpointers
         * 
         * B) A bounded priority queue of non-terminal indices, prioritized by their figure-of-merit scores (q)
         * 
         * C) A parallel array of edges. We will pop a limited number of edges off the priority queue into this array,
         * so this storage represents the actual cell population. (parallel array in tmpCell)
         * 
         * First, we push all binary edges onto the priority queue (if we're pruning significantly, most will not make
         * the queue). We then begin popping edges off the queue. With each edge popped, we 1) Add the edge to the array
         * of cell edges (C); and 2) Iterate through unary grammar rules with the edge parent as a child, inserting any
         * resulting unary edges to the queue. This insertion replaces the existing queue entry for the parent
         * non-terminal, if greater, and updates the inside probability and backpointer in (A).
         */

        // Push all binary or lexical edges onto a bounded priority queue
        final BoundedPriorityQueue q = threadLocalBoundedPriorityQueue.get();
        q.clear(cellBeamWidth);

        // Packed children and probabilities currently on the queue. Initially copied from cell temporary storage, but
        // updated as unaries are pushed onto the queue
        final TemporaryChartCell queueEdges = threadLocalQueueEdges.get();

        if (end - start == 1) { // Lexical Row (span = 1)

            // Limit the queue to the number of non-unary productions allowed
            q.setMaxSize(lexicalRowBeamWidth - lexicalRowUnaries);

            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                // Skip edges that don't meet the maximum delta
                if (tmpCell.insideProbabilities[nt] > minInsideProbability) {
                    final float fom = figureOfMerit.calcLexicalFOM(start, end, nt, tmpCell.insideProbabilities[nt]);
                    // Skip storing edges that didn't make it into the bounded queue
                    if (q.insert(nt, fom)) {
                        queueEdges.packedChildren[nt] = tmpCell.packedChildren[nt];
                        queueEdges.insideProbabilities[nt] = tmpCell.insideProbabilities[nt];
                        queueEdges.midpoints[nt] = tmpCell.midpoints[nt];
                    }
                }
            }
            // Now that all lexical productions are on the queue, expand it a bit to allow space for unary productions
            q.setMaxSize(lexicalRowBeamWidth);

        } else { // Span >= 2
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {

                // Skip edges that don't meet the maximum delta
                if (tmpCell.insideProbabilities[nt] > minInsideProbability) {
                    final float fom = figureOfMerit.calcFOM(start, end, nt, tmpCell.insideProbabilities[nt]);
                    // Skip storing edges that didn't make it into the bounded queue
                    if (q.insert(nt, fom)) {
                        queueEdges.packedChildren[nt] = tmpCell.packedChildren[nt];
                        queueEdges.insideProbabilities[nt] = tmpCell.insideProbabilities[nt];
                        queueEdges.midpoints[nt] = tmpCell.midpoints[nt];
                    }
                }
            }
        }

        if (q.size() == 0) {
            return;
        }

        // FOM of each non-terminal populated in the cell (i.e., each NT which survived pruning and was popped from the
        // queue). Parallel array to the temporary storage arrays in the chart cell which only store inside probability.
        final float[] cellFoms = threadLocalTmpFoms.get();
        Arrays.fill(cellFoms, Float.NEGATIVE_INFINITY);

        // Clear out the temporary cell. We've stored copies of all edges which made it onto the queue.
        Arrays.fill(tmpCell.insideProbabilities, Float.NEGATIVE_INFINITY);

        // Pop edges off the queue until we fill the beam width. With each non-terminal popped off the queue,
        // push unary edges for each unary grammar rule with the non-terminal as a child
        for (int edgesPopulated = 0; edgesPopulated < cellBeamWidth && q.size() > 0;) {

            final int headIndex = q.headIndex();
            final short nt = q.nts[headIndex];
            final float fom = q.foms[headIndex];
            q.popHead();

            if (tmpCell.insideProbabilities[nt] == Float.NEGATIVE_INFINITY) {
                // We're adding an edge that wasn't previously populated
                edgesPopulated++;
            }

            if (fom > cellFoms[nt]) {
                // Add or replace the edge in temporary chart storage
                tmpCell.packedChildren[nt] = queueEdges.packedChildren[nt];
                tmpCell.insideProbabilities[nt] = queueEdges.insideProbabilities[nt];
                tmpCell.midpoints[nt] = queueEdges.midpoints[nt];
                cellFoms[nt] = fom;

                // Process unary edges and add to the queue.
                if (allowUnaries) {
                    // Insert all unary edges with the current parent as child into the queue
                    final short child = nt;
                    final float childInsideProbability = tmpCell.insideProbabilities[child];

                    // Skip the grammar loop if there are no grammar rules that will meet the inside-probability delta
                    // cutoff
                    if (childInsideProbability + grammar.cscMaxUnaryProbabilities[child] < minInsideProbability) {
                        continue;
                    }

                    // Iterate over possible parents of the child (rows with non-zero entries)
                    for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                        final float jointProbability = grammar.cscUnaryProbabilities[i] + childInsideProbability;

                        if (jointProbability > minInsideProbability) {
                            final short parent = grammar.cscUnaryRowIndices[i];
                            final float parentFom = figureOfMerit.calcFOM(start, end, parent, jointProbability);

                            if (parentFom > cellFoms[parent] && q.replace(parent, parentFom)) {

                                // The FOM was high enough that the edge was added to the queue; update temporary
                                // storage to reflect the new unary child and probability
                                queueEdges.packedChildren[parent] = grammar.packingFunction().packUnary(child);
                                queueEdges.insideProbabilities[parent] = jointProbability;
                                queueEdges.midpoints[parent] = end;
                            }
                        }
                    }

                    if (collectDetailedStatistics) {
                        chart.parseTask.nUnaryConsidered += (grammar.cscUnaryColumnOffsets[child + 1] - grammar.cscUnaryColumnOffsets[child]);
                    }
                }
            }
        }
    }

    /**
     * @return True if this {@link Parser} implementation does implicit pruning (regardless of configuration
     *         properties); e.g. {@link ConstrainedCphSpmlParser}.
     */
    protected boolean implicitPruning() {
        return false;
    }
}
