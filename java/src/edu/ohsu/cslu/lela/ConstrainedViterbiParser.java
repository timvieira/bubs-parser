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
package edu.ohsu.cslu.lela;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * Matrix-loop parser which constrains the chart population according to the contents of a chart populated using the
 * parent grammar (i.e., when training a split grammar, the constraining chart is populated with 1-best parses using the
 * previous grammar)
 * 
 * Analogous to {@link Constrained2SplitInsideOutsideParser}, but performs Viterbi 1-best inference instead of summing
 * over matching children and rules. Used to populate a {@link ConstrainingChart} to constrain the next split-merge
 * cycle.
 * 
 * If we perform this parse in the real semiring, we occasionally choose a combination of node labels for which no rule
 * productions exist. This choice may produce a more accurate set of node labels (as described by Goodman), but for
 * post-split parsing, it's crucial that we be able to parse the labels represented in the new {@link ConstrainingChart}
 * .
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedViterbiParser extends Constrained2SplitInsideOutsideParser {

    public ConstrainedViterbiParser(final ParserDriver opts, final ConstrainedInsideOutsideGrammar grammar) {
        super(opts, grammar);
    }

    public Constrained2SplitChart parse(final ConstrainingChart c) {
        this.constrainingChart = c;

        // Initialize the chart
        chart = new Constrained2SplitChart(c, grammar);
        chart.parseTask = new ParseTask(c.tokens, grammar);
        cellSelector.initSentence(this, chart.parseTask);

        // Compute inside probabilities
        insidePass();

        return chart;
    }

    /**
     * Executes the inside parsing pass (populating {@link ParallelArrayChart#insideProbabilities})
     */
    @Override
    protected void insidePass() {
        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            if (startAndEnd[1] - startAndEnd[0] == 1) {
                // Add lexical productions to the chart from the constraining chart
                addLexicalProductions(startAndEnd[0], startAndEnd[1]);
            }
            computeInsideProbabilities(startAndEnd[0], startAndEnd[1]);
        }
    }

    /**
     * Computes constrained inside viterbi probabilities
     */
    @Override
    protected void computeInsideProbabilities(final short start, final short end) {

        final PackingFunction cpf = grammar.packingFunction();

        final int cellIndex = chart.cellIndex(start, end);
        final int offset = chart.offset(cellIndex);
        // 0 <= unaryLevels < maxUnaryChainLength
        final int unaryLevels = constrainingChart.unaryChainLength(cellIndex) - 1;

        // Binary productions
        if (end - start > 1) {
            final int parent0Offset = chart.offset(cellIndex) + (unaryLevels << 1);
            final int parent1Offset = parent0Offset + 1;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parent0Offset >> 1] << 1);
            final short midpoint = chart.midpoints[cellIndex];

            final int leftCellOffset = chart.offset(chart.cellIndex(start, midpoint));
            final int rightCellOffset = chart.offset(chart.cellIndex(midpoint, end));

            float maxRule = Float.NEGATIVE_INFINITY;

            // Iterate over all possible child pairs
            for (int i = leftCellOffset; i <= leftCellOffset + 1; i++) {
                final short leftChild = chart.nonTerminalIndices[i];
                if (leftChild < 0) {
                    continue;
                }
                final float leftProbability = chart.insideProbabilities[i];

                // And over children in the right child cell
                for (int j = rightCellOffset; j <= rightCellOffset + 1; j++) {
                    final int column = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childInsideProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {
                        final short parent = grammar.cscBinaryRowIndices[k];

                        // We're only looking for two parents
                        if (parent < parent0) {
                            continue;

                        } else if (parent == parent0) {
                            final float jointProbability = childInsideProbability + grammar.cscBinaryProbabilities[k];
                            if (jointProbability > chart.insideProbabilities[parent0Offset]) {
                                chart.nonTerminalIndices[parent0Offset] = parent;
                                maxRule = Math.max(maxRule, grammar.cscBinaryProbabilities[k]);
                                chart.insideProbabilities[parent0Offset] = jointProbability;
                                chart.packedChildren[parent0Offset] = column;
                            }

                        } else if (parent == parent0 + 1) {
                            final float jointProbability = childInsideProbability + grammar.cscBinaryProbabilities[k];
                            if (jointProbability > chart.insideProbabilities[parent1Offset]) {
                                chart.nonTerminalIndices[parent1Offset] = parent;
                                maxRule = Math.max(maxRule, grammar.cscBinaryProbabilities[k]);
                                chart.insideProbabilities[parent1Offset] = jointProbability;
                                chart.packedChildren[parent1Offset] = column;
                            }

                        } else {
                            // We've passed both target parents. No need to search more grammar rules
                            break;
                        }
                    }
                }
            }
            assert maxRule > -20f;
        }

        // Unary productions
        // foreach unary chain depth (starting from 2nd from bottom in chain; the bottom entry is the binary or lexical
        // parent)
        final int initialChildIndex = offset + (unaryLevels << 1);
        for (int child0Offset = initialChildIndex; child0Offset > offset; child0Offset -= 2) {

            final int parent0Offset = child0Offset - 2;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parent0Offset >> 1] << 1);
            final int parent1Offset = parent0Offset + 1;
            float maxRule = Float.NEGATIVE_INFINITY;

            // Iterate over both child slots
            final short child0 = (short) (constrainingChart.nonTerminalIndices[child0Offset >> 1] << 1);
            for (short i = 0; i <= 1; i++) {

                final short child = (short) (child0 + i);
                final float childInsideProbability = chart.insideProbabilities[child0Offset + i];
                if (childInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                    final short parent = grammar.cscUnaryRowIndices[j];

                    // We're only looking for two parents
                    if (parent < parent0) {
                        continue;

                    } else if (parent == parent0) {
                        final float unaryProbability = grammar.cscUnaryProbabilities[j] + childInsideProbability;
                        if (unaryProbability > chart.insideProbabilities[parent0Offset]) {
                            chart.nonTerminalIndices[parent0Offset] = parent;
                            maxRule = Math.max(maxRule, grammar.cscUnaryProbabilities[j]);
                            chart.insideProbabilities[parent0Offset] = unaryProbability;
                            chart.packedChildren[parent0Offset] = grammar.packingFunction.packUnary(child);
                        }

                    } else if (parent == parent0 + 1) {
                        final float unaryProbability = grammar.cscUnaryProbabilities[j] + childInsideProbability;
                        if (unaryProbability > chart.insideProbabilities[parent1Offset]) {
                            chart.nonTerminalIndices[parent1Offset] = parent;
                            maxRule = Math.max(maxRule, grammar.cscUnaryProbabilities[j]);
                            chart.insideProbabilities[parent1Offset] = unaryProbability;
                            chart.packedChildren[parent1Offset] = grammar.packingFunction.packUnary(child);
                        }

                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
                }
            }
            assert maxRule > -20f;
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {
        throw new UnsupportedOperationException();
    }
}
