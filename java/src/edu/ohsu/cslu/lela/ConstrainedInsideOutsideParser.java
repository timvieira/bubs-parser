/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.lela;

import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.ml.ConstrainedChartParser;
import edu.ohsu.cslu.parser.ml.SparseMatrixLoopParser;
import edu.ohsu.cslu.util.Math;

/**
 * Matrix-loop inside-outside parser which constrains the chart population according to the contents of a previous chart
 * 
 * TODO Try approximate log-sum methods and max-deltas from {@link Math}
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedInsideOutsideParser extends
        SparseMatrixLoopParser<ConstrainedInsideOutsideGrammar, ConstrainedChart> implements ConstrainedChartParser {

    ConstrainingChart constrainingChart;

    public ConstrainedInsideOutsideParser(final ParserDriver opts, final ConstrainedInsideOutsideGrammar grammar) {
        super(opts, grammar);
    }

    public BinaryTree<String> findBestParse(final ConstrainingChart c) {
        this.constrainingChart = c;

        // Initialize the chart
        if (chart != null
                && chart.midpoints.length >= c.midpoints.length
                && chart.nonTerminalIndices.length >= ConstrainedChart
                        .chartArraySize(c.size(), c.maxUnaryChainLength())
                && chart.cellOffsets.length >= c.cellOffsets.length) {
            chart.clear(c);
        } else {
            chart = new ConstrainedChart(c, grammar, 1);
        }

        chart.parseTask = new ParseTask(c.tokens, grammar);
        cellSelector.initSentence(this);

        // Compute inside and outside probabilities
        insidePass();
        outsidePass();

        return chart.extractBestParse(0, chart.size(), grammar.startSymbol);
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
     * Adds lexical productions from the constraining chart to the current chart
     */
    protected void addLexicalProductions(final short start, final short end) {

        final int cellIndex = chart.cellIndex(start, start + 1);

        final int constrainingOffset = constrainingChart.offset(cellIndex);

        // Find the lexical production in the constraining chart
        int unaryChainLength = chart.maxUnaryChainLength - 1;
        while (unaryChainLength > 0 && constrainingChart.nonTerminalIndices[constrainingOffset + unaryChainLength] < 0) {
            unaryChainLength--;
        }

        final int parent0Offset = chart.offset(cellIndex) + unaryChainLength;

        // Beginning of cell + offset for populated unary parents
        final int constrainingEntryIndex = constrainingChart.offset(cellIndex) + unaryChainLength;
        chart.midpoints[cellIndex] = end;

        final int lexicalProduction = constrainingChart.sparseMatrixGrammar.packingFunction
                .unpackLeftChild(constrainingChart.packedChildren[constrainingEntryIndex]);

        final short parent0 = constrainingChart.nonTerminalIndices[constrainingEntryIndex];
        final float[] lexicalLogProbabilities = grammar.lexicalLogProbabilities(lexicalProduction);
        final short[] lexicalParents = grammar.lexicalParents(lexicalProduction);
        // For debugging with assertions turned on
        boolean foundParent = false;

        // Iterate through grammar lexicon rules matching this word.
        for (int i = 0; i < lexicalLogProbabilities.length; i++) {
            final short parent = lexicalParents[i];
            // We're only looking for one parent
            if (parent < parent0) {
                continue;

            } else if (parent == parent0) {
                foundParent = true;
                chart.nonTerminalIndices[parent0Offset] = parent;
                chart.insideProbabilities[parent0Offset] = lexicalLogProbabilities[i];
                chart.packedChildren[parent0Offset] = chart.sparseMatrixGrammar.packingFunction
                        .packLexical(lexicalProduction);

            } else {
                // We've passed the target. No need to search more grammar rules
                break;
            }
        }
        assert foundParent;
    }

    /**
     * Computes constrained inside sum probabilities
     */
    protected void computeInsideProbabilities(final short start, final short end) {

        final PackingFunction cpf = grammar.packingFunction();

        final int cellIndex = chart.cellIndex(start, end);
        final int offset = chart.offset(cellIndex);
        // 0 <= unaryLevels < maxUnaryChainLength
        final int unaryLevels = constrainingChart.unaryChainLength(cellIndex) - 1;

        // Binary productions
        if (end - start > 1) {
            final int parent0Offset = chart.offset(cellIndex) + unaryLevels;
            final short parent0 = constrainingChart.nonTerminalIndices[parent0Offset];
            final short midpoint = chart.midpoints[cellIndex];

            final int leftCellOffset = chart.offset(chart.cellIndex(start, midpoint));
            final int rightCellOffset = chart.offset(chart.cellIndex(midpoint, end));
            // For debugging with assertions turned on
            boolean foundParent = false;

            final short leftChild = chart.nonTerminalIndices[leftCellOffset];
            final int column = cpf.pack(leftChild, chart.nonTerminalIndices[rightCellOffset]);
            final float childInsideProbability = chart.insideProbabilities[leftCellOffset]
                    + chart.insideProbabilities[rightCellOffset];

            for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {
                final short parent = grammar.cscBinaryRowIndices[k];

                // We're only looking for one parent
                if (parent < parent0) {
                    continue;

                } else if (parent == parent0) {
                    foundParent = true;
                    chart.nonTerminalIndices[parent0Offset] = parent;
                    chart.insideProbabilities[parent0Offset] = Math.logSum(chart.insideProbabilities[parent0Offset],
                            grammar.cscBinaryProbabilities[k] + childInsideProbability);

                } else {
                    // We've passed all target parents. No need to search more grammar rules
                    break;
                }
            }
            assert foundParent;
        }

        // Unary productions
        // foreach unary chain depth (starting from 2nd from bottom in chain; the bottom entry is the binary or lexical
        // parent)
        final int initialChildIndex = offset + unaryLevels;
        for (int childIndex = initialChildIndex; childIndex > offset; childIndex--) {

            final int parentIndex = childIndex - 1;
            final short parent = constrainingChart.nonTerminalIndices[parentIndex];
            // For debugging with assertions turned on
            boolean foundParent = false;

            // Iterate over all child slots
            final short child = constrainingChart.nonTerminalIndices[childIndex];
            final float childInsideProbability = chart.insideProbabilities[childIndex];
            if (childInsideProbability == Float.NEGATIVE_INFINITY) {
                continue;
            }

            // Iterate over possible parents of the child (rows with non-zero entries)
            for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                final short grammarParent = grammar.cscUnaryRowIndices[j];

                // We're only looking for one parent
                if (grammarParent < parent) {
                    continue;

                } else if (grammarParent == parent) {
                    foundParent = true;
                    final float unaryProbability = grammar.cscUnaryProbabilities[j] + childInsideProbability;
                    chart.nonTerminalIndices[parentIndex] = grammarParent;
                    chart.insideProbabilities[parentIndex] = Math.logSum(chart.insideProbabilities[parentIndex],
                            unaryProbability);

                } else {
                    // We've passed all target parents. No need to search more grammar rules
                    break;
                }
            }
            assert foundParent;
        }
    }

    /**
     * Executes the outside parsing pass (populating {@link ConstrainedChart#outsideProbabilities})
     */
    private void outsidePass() {

        final Iterator<short[]> reverseIterator = cellSelector.reverseIterator();

        // Populate start-symbol outside probability in the top cell.
        chart.outsideProbabilities[chart.offset(chart.cellIndex(0, chart.size()))] = 0;

        while (reverseIterator.hasNext()) {
            final short[] startAndEnd = reverseIterator.next();
            computeOutsideProbabilities(startAndEnd[0], startAndEnd[1]);
        }
    }

    private void computeOutsideProbabilities(final short start, final short end) {

        final int cellIndex = chart.cellIndex(start, end);

        // The entry we're computing is the top entry in the target cell
        final int entry0Offset = chart.offset(cellIndex);

        // The parent is the bottom entry in the parent cell
        final int parentCellIndex = chart.parentCellIndices[cellIndex];
        // The top cell won't have a parent, but we still want to compute unary outside probabilities
        if (parentCellIndex >= 0) {
            final int parent0Offset = chart.offset(parentCellIndex) + (chart.unaryChainLength(parentCellIndex) - 1);

            // And the sibling is the top entry in the sibling cell
            final short siblingCellIndex = chart.siblingCellIndices[cellIndex];
            final int sibling0Offset = chart.offset(siblingCellIndex);

            if (siblingCellIndex > cellIndex) {
                // Process a left-side sibling
                computeSiblingOutsideProbabilities(entry0Offset, parent0Offset, sibling0Offset,
                        grammar.leftChildCscBinaryColumnOffsets, grammar.leftChildCscBinaryRowIndices,
                        grammar.leftChildCscBinaryProbabilities, grammar.leftChildPackingFunction);
            } else {
                // Process a right-side sibling
                computeSiblingOutsideProbabilities(entry0Offset, parent0Offset, sibling0Offset,
                        grammar.rightChildCscBinaryColumnOffsets, grammar.rightChildCscBinaryRowIndices,
                        grammar.rightChildCscBinaryProbabilities, grammar.rightChildPackingFunction);
            }
        }

        // Unary outside probabilities
        computeUnaryOutsideProbabilities(cellIndex);

    }

    private void computeSiblingOutsideProbabilities(final int entryOffset, final int parentIndex,
            final int siblingIndex, final int[] cscBinaryColumnOffsets, final short[] cscBinaryRowIndices,
            final float[] cscBinaryProbabilities, final PackingFunction cpf) {

        final float sentenceInsideProbability = chart.getInside(0, chart.size(), 0);

        final short entry = constrainingChart.nonTerminalIndices[entryOffset];
        // For debugging with assertions turned on
        boolean foundEntry = false;

        final short siblingEntry = chart.nonTerminalIndices[siblingIndex];
        final float siblingInsideProbability = chart.insideProbabilities[siblingIndex];

        final short parent = chart.nonTerminalIndices[parentIndex];
        final int column = cpf.pack(parent, siblingEntry);
        final float jointProbability = siblingInsideProbability + chart.outsideProbabilities[parentIndex];

        // Iterate over grammar rules matching the parent and sibling
        for (int k = cscBinaryColumnOffsets[column]; k < cscBinaryColumnOffsets[column + 1]; k++) {
            final short grammarEntry = cscBinaryRowIndices[k];

            // We're only looking for one entry
            if (grammarEntry < entry) {
                continue;

            } else if (grammarEntry == entry) {
                foundEntry = true;
                chart.outsideProbabilities[entryOffset] = Math.logSum(chart.outsideProbabilities[entryOffset],
                        cscBinaryProbabilities[k] + jointProbability);

            } else {
                // We've passed all target entries. No need to search more grammar rules
                break;
            }
        }
        assert foundEntry;
    }

    private void computeUnaryOutsideProbabilities(final int cellIndex) {

        final float sentenceInsideProbability = chart.getInside(0, chart.size(), 0);
        final int offset = chart.offset(cellIndex);

        // foreach unary chain depth (starting from 2nd from top in chain; the top entry is the binary child)
        final int bottomChildOffset = offset + chart.unaryChainLength(cellIndex) - 1;
        for (int parentIndex = offset; parentIndex < bottomChildOffset; parentIndex++) {

            final short parent = constrainingChart.nonTerminalIndices[parentIndex];
            // For debugging with assertions turned on
            boolean foundChild = false;

            final int childIndex = parentIndex + 1;
            final short child = chart.nonTerminalIndices[childIndex];
            if (child < 0) {
                continue;
            }

            // Iterate over possible parents of the child (rows with non-zero entries)
            for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                final short grammarParent = grammar.cscUnaryRowIndices[j];

                // We're only looking for two parents
                if (grammarParent < parent) {
                    continue;

                } else if (grammarParent == parent) {
                    foundChild = true;
                    chart.outsideProbabilities[childIndex] = Math.logSum(chart.outsideProbabilities[childIndex],
                            grammar.cscUnaryProbabilities[j] + chart.outsideProbabilities[parentIndex]);

                } else {
                    // We've passed the target parent. No need to search more grammar rules
                    break;
                }
            }
            assert foundChild;
        }
    }

    /**
     * Counts rule occurrences in the current chart.
     * 
     * @param countGrammar The grammar to populate with rule counts
     * @return countGrammar
     */
    FractionalCountGrammar countRuleOccurrences(final FractionalCountGrammar countGrammar) {
        cellSelector.reset();
        final float sentenceInsideLogProb = chart.getInside(0, chart.size(), 0);
        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            countUnaryRuleOccurrences(countGrammar, startAndEnd[0], startAndEnd[1], sentenceInsideLogProb);
            if (startAndEnd[1] - startAndEnd[0] == 1) {
                countLexicalRuleOccurrences(countGrammar, startAndEnd[0], startAndEnd[1], sentenceInsideLogProb);
            } else {
                countBinaryRuleOccurrences(countGrammar, startAndEnd[0], startAndEnd[1], sentenceInsideLogProb);
            }
        }

        return countGrammar;
    }

    // TODO Could these counts be computed during the outside pass?
    private void countBinaryRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end, final float sentenceInsideLogProb) {

        final PackingFunction cpf = grammar.packingFunction();

        final int cellIndex = chart.cellIndex(start, end);
        // 0 <= unaryLevels < maxUnaryChainLength
        final int unaryLevels = constrainingChart.unaryChainLength(cellIndex) - 1;

        // Binary productions
        final int parent0Offset = chart.offset(cellIndex) + unaryLevels;
        final short parent0 = constrainingChart.nonTerminalIndices[parent0Offset];
        final short midpoint = chart.midpoints[cellIndex];

        final int leftCellOffset = chart.offset(chart.cellIndex(start, midpoint));
        final int rightCellOffset = chart.offset(chart.cellIndex(midpoint, end));

        final short leftChild = chart.nonTerminalIndices[leftCellOffset];
        final float leftProbability = chart.insideProbabilities[leftCellOffset];

        final int column = cpf.pack(leftChild, chart.nonTerminalIndices[rightCellOffset]);
        final float childInsideProbability = leftProbability + chart.insideProbabilities[rightCellOffset];

        for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {
            final short parent = grammar.cscBinaryRowIndices[k];

            // We're only looking for one parent
            if (parent < parent0) {
                continue;

            } else if (parent == parent0) {
                // Parent outside x left child inside x right child inside x production probability.
                // Equation 1 of Petrov et al., 2006.
                final float logCount = chart.outsideProbabilities[parent0Offset] + childInsideProbability
                        + grammar.cscBinaryProbabilities[k] - sentenceInsideLogProb;
                countGrammar.incrementBinaryLogCount(parent, column, logCount);

            } else {
                // We've passed the target parent. No need to search more grammar rules
                break;
            }
        }
    }

    private void countUnaryRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end, final float sentenceInsideLogProb) {

        final int cellIndex = chart.cellIndex(start, end);
        // 0 <= unaryLevels < maxUnaryChainLength
        final int unaryLevels = constrainingChart.unaryChainLength(cellIndex) - 1;

        final int offset = chart.offset(cellIndex);
        final int initialChildIndex = offset + unaryLevels;

        // foreach unary chain depth (starting from 2nd from bottom in chain; the bottom entry is the binary or lexical
        // parent)
        for (int child0Offset = initialChildIndex; child0Offset > offset; child0Offset--) {

            final int parent0Offset = child0Offset - 1;
            final short parent0 = constrainingChart.nonTerminalIndices[parent0Offset];

            // Iterate over all child slots
            final short child0 = constrainingChart.nonTerminalIndices[child0Offset];
            for (short i = 0; i < unaryLevels; i++) {

                final short child = (short) (child0 + i);
                final float childInsideProbability = chart.insideProbabilities[child0Offset + i];
                if (childInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                    final short parent = grammar.cscUnaryRowIndices[j];

                    // We're only looking for one parent
                    if (parent < parent0) {
                        continue;

                    } else if (parent == parent0) {
                        // Parent outside x child inside x production probability. From equation 1 of Petrov et al.,
                        // 2006
                        final float logCount = chart.outsideProbabilities[parent0Offset] + childInsideProbability
                                + grammar.cscUnaryProbabilities[j] - sentenceInsideLogProb;
                        countGrammar.incrementUnaryLogCount(parent, child, logCount);

                    } else {
                        // We've passed the target parent. No need to search more grammar rules
                        break;
                    }
                }
            }
        }
    }

    private void countLexicalRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end, final float sentenceInsideLogProb) {

        final int cellIndex = chart.cellIndex(start, start + 1);

        final int constrainingOffset = constrainingChart.offset(cellIndex);

        // Find the lexical production in the constraining chart
        int unaryChainLength = chart.maxUnaryChainLength - 1;
        while (unaryChainLength > 0 && constrainingChart.nonTerminalIndices[constrainingOffset + unaryChainLength] < 0) {
            unaryChainLength--;
        }

        final int parent0Offset = chart.offset(cellIndex) + unaryChainLength;

        // Beginning of cell + offset for populated unary parents
        // final int firstPosOffset = chart.offset(cellIndex) + unaryChainLength *
        // splitVocabulary.maxSplits;
        final int constrainingEntryIndex = constrainingChart.offset(cellIndex) + unaryChainLength;
        chart.midpoints[cellIndex] = end;

        final int lexicalProduction = constrainingChart.sparseMatrixGrammar.packingFunction
                .unpackLeftChild(constrainingChart.packedChildren[constrainingEntryIndex]);

        final short parent0 = constrainingChart.nonTerminalIndices[constrainingEntryIndex];
        final float[] lexicalLogProbabilities = grammar.lexicalLogProbabilities(lexicalProduction);
        final short[] lexicalParents = grammar.lexicalParents(lexicalProduction);

        // Iterate through grammar lexicon rules matching this word.
        for (int i = 0; i < lexicalLogProbabilities.length; i++) {
            final short parent = lexicalParents[i];

            // We're only looking for one parent
            if (parent < parent0) {
                continue;

            } else if (parent == parent0) {
                // Parent outside x production probability (child inside = 1 for lexical entries)
                // From Equation 1 of Petrov et al., 2006
                final float logCount = chart.outsideProbabilities[parent0Offset] + lexicalLogProbabilities[i]
                        - sentenceInsideLogProb;
                countGrammar.incrementLexicalLogCount(parent, lexicalProduction, logCount);

            } else {
                // We've passed the target parent. No need to search more grammar rules
                break;
            }
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstrainingChart constrainingChart() {
        return constrainingChart;
    }
}
