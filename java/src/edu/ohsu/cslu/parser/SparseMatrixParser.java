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

import java.util.Arrays;

import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
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

    public long sentencePruningTime = 0;

    private final ThreadLocal<BoundedPriorityQueue> threadLocalBoundedPriorityQueue;
    private final ThreadLocal<float[]> threadLocalTmpFoms;

    public SparseMatrixParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);

        final ConfigProperties props = GlobalConfigProperties.singleton();

        // Pruning Parameters
        if (props.containsKey(PROPERTY_MAX_BEAM_WIDTH) && props.getIntProperty(PROPERTY_MAX_BEAM_WIDTH) > 0) {
            this.beamWidth = props.getIntProperty(Parser.PROPERTY_MAX_BEAM_WIDTH);
            this.lexicalRowBeamWidth = props.getIntProperty(PROPERTY_LEXICAL_ROW_BEAM_WIDTH);
            this.lexicalRowUnaries = props.getIntProperty(PROPERTY_LEXICAL_ROW_UNARIES);
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

        } else {
            this.beamWidth = grammar.numNonTerms();
            this.lexicalRowBeamWidth = grammar.numNonTerms();
            this.lexicalRowUnaries = 0;
            this.maxLocalDelta = 0f;
            this.exhaustiveSearch = true;
            this.threadLocalBoundedPriorityQueue = null;
            this.threadLocalTmpFoms = null;
        }
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

            unarySpmv(packedArrayCell.tmpPackedChildren, packedArrayCell.tmpInsideProbabilities,
                    packedArrayCell.tmpMidpoints, 0, chartCell.end());
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

    // TODO Eliminate cellInsideProbabilities and cellMidpoints; use existing temporary chart cell storage instead?

    protected void unaryAndPruning(final PackedArrayChartCell spvChartCell, final short start, final short end,
            final int[] cellPackedChildren, final float[] cellInsideProbabilities, final short[] cellMidpoints) {

        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

        // For the moment, at least, we ignore factored-only cell constraints in span-1 cells
        final boolean factoredOnly = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end) && (end - start > 1);
        final boolean allowSpanOneUnaries = cellSelector.getCellConstraints().isUnaryOpen(start, end);
        final float minInsideProbability = edu.ohsu.cslu.util.Math.max(spvChartCell.tmpInsideProbabilities)
                - maxLocalDelta;

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
        final BoundedPriorityQueue q = threadLocalBoundedPriorityQueue.get();
        q.clear(cellBeamWidth);

        // FOM of each observed non-terminal (parallel to the temporary storage arrays in the chart cell which only
        // store inside probability)
        final float[] tmpFoms = threadLocalTmpFoms.get();
        Arrays.fill(tmpFoms, Float.NEGATIVE_INFINITY);

        if (end - start == 1) { // Lexical Row (span = 1)

            // Limit the queue to the number of non-unary productions allowed
            q.setMaxSize(lexicalRowBeamWidth - lexicalRowUnaries);

            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (spvChartCell.tmpInsideProbabilities[nt] > minInsideProbability) {
                    final float fom = edgeSelector.calcLexicalFOM(start, end, nt,
                            spvChartCell.tmpInsideProbabilities[nt]);
                    q.insert(nt, fom);
                    tmpFoms[nt] = fom;
                }
            }
            // Now that all lexical productions are on the queue, expand it a bit to allow space for unary productions
            q.setMaxSize(lexicalRowBeamWidth);

        } else { // Span >= 2
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (spvChartCell.tmpInsideProbabilities[nt] > minInsideProbability) {
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
        // push unary edges for each unary grammar rule with the non-terminal as a child
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
                if (!factoredOnly && allowSpanOneUnaries) {
                    // Insert all unary edges with the current parent as child into the queue
                    final short child = nt;
                    final float insideProbability = spvChartCell.tmpInsideProbabilities[child];

                    // Iterate over possible parents of the child (rows with non-zero entries)
                    for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                        final float jointProbability = grammar.cscUnaryProbabilities[i] + insideProbability;

                        if (jointProbability > minInsideProbability) {
                            final short parent = grammar.cscUnaryRowIndices[i];
                            final float parentFom = edgeSelector.calcFOM(start, end, parent, jointProbability);

                            if (parentFom > tmpFoms[parent] && parentFom > cellFoms[parent]
                                    && q.replace(parent, parentFom)) {
                                // The FOM was high enough that the edge was added to the queue; update temporary
                                // storage to reflect the new unary child and probability
                                spvChartCell.tmpPackedChildren[parent] = grammar.cartesianProductFunction().packUnary(
                                        child);
                                spvChartCell.tmpInsideProbabilities[parent] = jointProbability;
                            }
                        }
                    }
                }

                edgesPopulated++;

            } else {
                // We just re-popped a non-terminal we've already seen (meaning a unary which was added to the
                // queue). Replace the existing edge with the new unary edge.
                cellPackedChildren[nt] = spvChartCell.tmpPackedChildren[nt];
                cellInsideProbabilities[nt] = spvChartCell.tmpInsideProbabilities[nt];
                cellMidpoints[nt] = end;
                cellFoms[nt] = fom;
            }
        }

        if (collectDetailedStatistics) {
            currentInput.unaryAndPruningMs += System.currentTimeMillis() - t0;
        }
    }
}
