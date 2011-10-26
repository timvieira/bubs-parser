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

import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * Base class for all chart parsers which represent the chart as a parallel array and operate on matrix-encoded
 * grammars.
 * 
 * @author Aaron Dunlop
 */
public abstract class SparseMatrixParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        ChartParser<G, C> {

    protected final int beamWidth;
    protected final int lexicalRowBeamWidth;
    protected final int lexicalRowUnaries;
    protected final float maxLocalDelta;
    protected final boolean exhaustiveSearch;

    protected final ThreadLocal<BoundedPriorityQueue> threadLocalBoundedPriorityQueue;
    protected final ThreadLocal<float[]> threadLocalTmpFoms;
    protected final ThreadLocal<TemporaryChartCell> threadLocalQueueEdges;

    public SparseMatrixParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);

        final ConfigProperties props = GlobalConfigProperties.singleton();

        // Pruning Parameters
        if (props.containsKey(PROPERTY_MAX_BEAM_WIDTH) && props.getIntProperty(PROPERTY_MAX_BEAM_WIDTH) > 0) {
            this.beamWidth = props.getIntProperty(Parser.PROPERTY_MAX_BEAM_WIDTH);
            this.lexicalRowBeamWidth = props.getIntProperty(PROPERTY_LEXICAL_ROW_BEAM_WIDTH, beamWidth);
            this.lexicalRowUnaries = props.getIntProperty(PROPERTY_LEXICAL_ROW_UNARIES, lexicalRowBeamWidth / 3);
            this.maxLocalDelta = props.getFloatProperty(PROPERTY_MAX_LOCAL_DELTA, 8f);
            this.exhaustiveSearch = false;
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
                    return new TemporaryChartCell(grammar.numNonTerms());
                }
            };

        } else {
            this.beamWidth = grammar.numNonTerms();
            this.lexicalRowBeamWidth = grammar.numNonTerms();
            this.lexicalRowUnaries = 0;
            this.maxLocalDelta = 0f;
            this.exhaustiveSearch = true;
            this.threadLocalBoundedPriorityQueue = null;
            this.threadLocalTmpFoms = null;
            this.threadLocalQueueEdges = null;
        }
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        final int sentLength = parseTask.sentenceLength();
        if (chart != null && chart.size() >= sentLength) {
            chart.reset(parseTask);
        } else {
            // Construct a chart of the appropriate type
            try {
                final Class<C> chartClass = chartClass();
                chartClass.getConstructors();
                try {
                    // First, try for a constructor that takes tokens, grammar, beamWidth, and lexicalRowBeamWidth
                    chart = chartClass.getConstructor(
                            new Class[] { ParseTask.class, SparseMatrixGrammar.class, int.class, int.class })
                            .newInstance(new Object[] { parseTask, grammar, beamWidth, lexicalRowBeamWidth });

                } catch (final NoSuchMethodException e) {
                    // If not found, use a constructor that takes only tokens and grammar
                    chart = chartClass.getConstructor(new Class[] { ParseTask.class, SparseMatrixGrammar.class })
                            .newInstance(new Object[] { parseTask, grammar });
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
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

        final PackingFunction cpf = grammar.cartesianProductFunction();

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
        }
    }

    protected void unaryAndPruning(final PackedArrayChartCell spvChartCell, final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final TemporaryChartCell tmpCell = spvChartCell.tmpCell;

        final int cellBeamWidth = (end - start == 1 ? lexicalRowBeamWidth : Math.min(
                cellSelector.getBeamWidth(start, end), beamWidth));
        if (cellBeamWidth == 1) {
            // Special-case when we are pruning down to only a single entry. We can't add any unary productions, so just
            // choose the NT with the maximum FOM.
            float maxFom = Float.NEGATIVE_INFINITY;
            short maxNt = -1;
            if (end - start == 1) { // Lexical Row (span = 1)
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
                    final float fom = fomModel.calcLexicalFOM(start, end, nt, tmpCell.insideProbabilities[nt]);
                    if (fom > maxFom) {
                        maxFom = fom;
                        maxNt = nt;
                    }
                }
            } else {
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
                    final float fom = fomModel.calcFOM(start, end, nt, tmpCell.insideProbabilities[nt]);
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

    private void unaryAndPruning(final TemporaryChartCell tmpCell, final int cellBeamWidth, final short start,
            final short end) {
        // For the moment, at least, we ignore factored-only cell constraints in span-1 cells
        final boolean allowUnaries = !cellSelector.hasCellConstraints()
                || cellSelector.getCellConstraints().isUnaryOpen(start, end)
                && !(cellSelector.getCellConstraints().isCellOnlyFactored(start, end) && (end - start > 1));
        final float minInsideProbability = edu.ohsu.cslu.util.Math.max(tmpCell.insideProbabilities) - maxLocalDelta;

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
                if (tmpCell.insideProbabilities[nt] > minInsideProbability) {
                    final float fom = fomModel.calcLexicalFOM(start, end, nt, tmpCell.insideProbabilities[nt]);
                    q.insert(nt, fom);
                    queueEdges.packedChildren[nt] = tmpCell.packedChildren[nt];
                    queueEdges.insideProbabilities[nt] = tmpCell.insideProbabilities[nt];
                    queueEdges.midpoints[nt] = tmpCell.midpoints[nt];
                }
            }
            // Now that all lexical productions are on the queue, expand it a bit to allow space for unary productions
            q.setMaxSize(lexicalRowBeamWidth);

        } else { // Span >= 2
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (tmpCell.insideProbabilities[nt] > minInsideProbability) {
                    final float fom = fomModel.calcFOM(start, end, nt, tmpCell.insideProbabilities[nt]);
                    q.insert(nt, fom);
                    queueEdges.packedChildren[nt] = tmpCell.packedChildren[nt];
                    queueEdges.insideProbabilities[nt] = tmpCell.insideProbabilities[nt];
                    queueEdges.midpoints[nt] = tmpCell.midpoints[nt];
                }
            }
        }

        if (q.size() == 0) {
            return;
        }

        // FOM of each non-terminal populated in the cell (i.e., each NT which was survived pruning and popped from the
        // queue). Parallel to the temporary storage arrays in the chart cell which only store inside probability.
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

                    // Iterate over possible parents of the child (rows with non-zero entries)
                    for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                        final float jointProbability = grammar.cscUnaryProbabilities[i] + childInsideProbability;

                        if (jointProbability > minInsideProbability) {
                            final short parent = grammar.cscUnaryRowIndices[i];
                            final float parentFom = fomModel.calcFOM(start, end, parent, jointProbability);

                            if (parentFom > cellFoms[parent] && q.replace(parent, parentFom)) {
                                // The FOM was high enough that the edge was added to the queue; update temporary
                                // storage to reflect the new unary child and probability
                                queueEdges.packedChildren[parent] = grammar.cartesianProductFunction().packUnary(child);
                                queueEdges.insideProbabilities[parent] = jointProbability;
                                tmpCell.midpoints[parent] = end;
                            }
                        }
                    }
                }
            }
        }
    }
}
