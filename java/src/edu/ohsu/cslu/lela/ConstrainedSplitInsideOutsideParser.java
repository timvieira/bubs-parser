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
 * Matrix-loop parser which constrains the chart population according to the contents of a chart populated with the gold
 * tree.
 * 
 * 
 * Implementation notes:
 * 
 * --Given known child cells from the constraining chart, we need only consider a single midpoint for each cell.
 * 
 * --We do need to maintain space for a few unary productions; the first entry in each chart cell is for the top node in
 * the unary chain; any others (if populated) are unary children.
 * 
 * --The grammar intersection need only consider rules for splits of the constraining parent. We iterate over the rules
 * for all child pairs, but apply only those rules which match constraining parents.
 * 
 * TODO Try approximate log-sum methods and max-deltas from {@link Math}
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedSplitInsideOutsideParser extends
        SparseMatrixLoopParser<ConstrainedCscSparseMatrixGrammar, ConstrainedChart> implements ConstrainedChartParser {

    ConstrainingChart constrainingChart;
    final SplitVocabulary vocabulary;

    public ConstrainedSplitInsideOutsideParser(final ParserDriver opts, final ConstrainedCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
        this.vocabulary = grammar.vocabulary();
    }

    public BinaryTree<String> findBestParse(final ConstrainingChart c) {
        this.constrainingChart = c;

        // Initialize the chart
        if (chart != null
                && chart.midpoints.length >= c.midpoints.length
                && chart.nonTerminalIndices.length >= ConstrainedChart.splitChartArraySize(c.size(),
                        c.maxUnaryChainLength(), vocabulary.maxSplits)
                && chart.cellOffsets.length >= c.cellOffsets.length) {
            chart.clear(c);
        } else {
            chart = new ConstrainedChart(c, grammar);
        }

        chart.parseTask = new ParseTask(c.tokens, grammar);
        cellSelector.initSentence(this, chart.parseTask);

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
            final short start = startAndEnd[0];
            final short end = startAndEnd[1];
            final int cellIndex = chart.cellIndex(start, end);
            final int unaryChainLength = constrainingChart.unaryChainLength(cellIndex);

            if (end - start == 1) {
                // Add lexical productions to the chart from the constraining chart
                addLexicalProductions(start, end, cellIndex, unaryChainLength);
            }
            computeInsideProbabilities(start, end, cellIndex, unaryChainLength);
        }
    }

    /**
     * Adds lexical productions from the constraining chart to the current chart
     */
    protected void addLexicalProductions(final short start, final short end, final int cellIndex,
            final int unaryChainLength) {

        final int firstParentOffset = chart.offset(cellIndex) + ((unaryChainLength - 1) * vocabulary.maxSplits);

        // Beginning of cell + offset for populated unary parents
        final int constrainingEntryIndex = constrainingChart.offset(cellIndex) + unaryChainLength - 1;
        chart.midpoints[cellIndex] = end;

        final int lexicalProduction = constrainingChart.sparseMatrixGrammar.packingFunction
                .unpackLeftChild(constrainingChart.packedChildren[constrainingEntryIndex]);

        final short constrainingParent = constrainingChart.nonTerminalIndices[constrainingEntryIndex];
        final short firstParent = vocabulary.firstSplitIndices[constrainingParent];
        final short lastParent = (short) (firstParent + vocabulary.ntSplitCounts[firstParent] - 1);

        final float[] lexicalLogProbabilities = grammar.lexicalLogProbabilities(lexicalProduction);
        final short[] lexicalParents = grammar.lexicalParents(lexicalProduction);
        // For debugging with assertions turned on
        boolean foundParent = false;

        // Iterate through grammar lexicon rules matching this word.
        for (int i = 0; i < lexicalParents.length; i++) {
            final short parent = lexicalParents[i];

            if (parent < firstParent) {
                continue;

            } else if (parent > lastParent) {
                // We've passed all target parents. No need to search more grammar rules
                break;
            }

            // For debugging with assertions turned on
            assert (vocabulary.getBaseIndex(parent) == constrainingParent);
            foundParent = true;

            final int parentOffset = firstParentOffset + parent - firstParent;
            chart.nonTerminalIndices[parentOffset] = parent;
            chart.insideProbabilities[parentOffset] = lexicalLogProbabilities[i];
            chart.packedChildren[parentOffset] = chart.sparseMatrixGrammar.packingFunction
                    .packLexical(lexicalProduction);
        }
        assert foundParent;
    }

    /**
     * Computes constrained inside sum probabilities
     */
    protected void computeInsideProbabilities(final short start, final short end, final int cellIndex,
            final int unaryChainLength) {

        final PackingFunction cpf = grammar.packingFunction();

        // Binary productions
        if (end - start > 1) {
            final short midpoint = chart.midpoints[cellIndex];
            final int leftChildCellIndex = chart.cellIndex(start, midpoint);
            final int rightChildCellIndex = chart.cellIndex(midpoint, end);

            final int constrainingParentOffset = constrainingChart.offset(cellIndex)
                    + constrainingChart.unaryChainLength(cellIndex) - 1;

            final short constrainingParent = constrainingChart.nonTerminalIndices[constrainingParentOffset];
            final short constrainingLeftChild = constrainingChart.nonTerminalIndices[constrainingChart
                    .offset(leftChildCellIndex)];
            final short constrainingRightChild = constrainingChart.nonTerminalIndices[constrainingChart
                    .offset(rightChildCellIndex)];

            final int firstParentOffset = chart.offset(cellIndex) + ((unaryChainLength - 1) * vocabulary.maxSplits);
            final short firstParent = vocabulary.firstSplitIndices[constrainingChart.nonTerminalIndices[constrainingParentOffset]];
            final short lastParent = (short) (firstParent + vocabulary.ntSplitCounts[firstParent] - 1);

            final int leftCellOffset = chart.offset(leftChildCellIndex);
            final int rightCellOffset = chart.offset(rightChildCellIndex);

            // For debugging with assertions turned on
            boolean foundParent = false;

            // Iterate over all possible child pairs
            final int leftChildSplits = vocabulary.baseNtSplitCounts[constrainingLeftChild];
            final int rightChildSplits = vocabulary.baseNtSplitCounts[constrainingRightChild];

            for (int i = 0; i < leftChildSplits; i++) {

                final float leftChildInsideProbability = chart.insideProbabilities[leftCellOffset + i];
                if (leftChildInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                final short leftChild = chart.nonTerminalIndices[leftCellOffset + i];

                // And over children in the right child cell
                for (int j = 0; j < rightChildSplits; j++) {

                    final float rightChildInsideProbability = chart.insideProbabilities[rightCellOffset + j];
                    if (rightChildInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    final int column = cpf.pack(leftChild, chart.nonTerminalIndices[rightCellOffset + j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childInsideProbability = leftChildInsideProbability + rightChildInsideProbability;

                    for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {
                        final short parent = grammar.cscBinaryRowIndices[k];

                        if (parent < firstParent) {
                            continue;

                        } else if (parent > lastParent) {
                            // We've passed all target parents. No need to search more grammar rules
                            break;
                        }

                        // For debugging with assertions turned on
                        assert (vocabulary.getBaseIndex(parent) == constrainingParent);
                        foundParent = true;

                        final int parentOffset = firstParentOffset + parent - firstParent;
                        // TODO We probably don't need to store the non-terminal index anymore
                        chart.nonTerminalIndices[parentOffset] = parent;
                        chart.insideProbabilities[parentOffset] = Math.logSum(chart.insideProbabilities[parentOffset],
                                grammar.cscBinaryProbabilities[k] + childInsideProbability);
                    }
                }
            }
            assert foundParent;
        }

        // Unary productions
        // foreach unary chain height (starting from 2nd from bottom in chain; the bottom entry is the binary or lexical
        // parent)
        for (int unaryHeight = 1; unaryHeight < unaryChainLength; unaryHeight++) {

            final int firstChildOffset = chart.offset(cellIndex) + (unaryChainLength - unaryHeight)
                    * vocabulary.maxSplits;
            final int firstParentOffset = firstChildOffset - vocabulary.maxSplits;

            final int constrainingParentOffset = constrainingChart.offset(cellIndex)
                    + (unaryChainLength - unaryHeight - 1);
            final short constrainingChild = constrainingChart.nonTerminalIndices[constrainingParentOffset + 1];

            final short firstParent = vocabulary.firstSplitIndices[constrainingChart.nonTerminalIndices[constrainingParentOffset]];
            final short lastParent = (short) (firstParent + vocabulary.ntSplitCounts[firstParent] - 1);

            // For debugging with assertions turned on
            boolean foundParent = false;

            // Iterate over all possible children
            final short firstChild = vocabulary.firstSplitIndices[constrainingChild];
            final int childSplits = vocabulary.baseNtSplitCounts[constrainingChild];

            for (int i = 0; i < childSplits; i++) {

                final short child = (short) (firstChild + i);
                final float childInsideProbability = chart.insideProbabilities[firstChildOffset + i];
                if (childInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                    final short parent = grammar.cscUnaryRowIndices[j];

                    if (parent < firstParent) {
                        continue;

                    } else if (parent > lastParent) {
                        // We've passed all target parents. No need to search more grammar rules
                        break;
                    }

                    // For debugging with assertions turned on
                    assert (vocabulary.getBaseIndex(parent) == constrainingChart.nonTerminalIndices[constrainingParentOffset]);
                    foundParent = true;

                    final int parentOffset = firstParentOffset + parent - firstParent;
                    chart.nonTerminalIndices[parentOffset] = parent;
                    chart.insideProbabilities[parentOffset] = Math.logSum(chart.insideProbabilities[parentOffset],
                            grammar.cscUnaryProbabilities[j] + childInsideProbability);
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
            final int cellIndex = chart.cellIndex(startAndEnd[0], startAndEnd[1]);
            final int unaryChainLength = constrainingChart.unaryChainLength(cellIndex);

            computeOutsideProbabilities(startAndEnd[0], startAndEnd[1], cellIndex, unaryChainLength);
        }
    }

    private void computeOutsideProbabilities(final short start, final short end, final int cellIndex,
            final int unaryChainLength) {

        // Compute unary probabilities first
        computeUnaryOutsideProbabilities(cellIndex, unaryChainLength);

        // Compute outside probabilities of the top entries in each child cell. Unary probabilities in those child cells
        // will be populated later, when we reach the children. Since this calculation only covers binary probabilities,
        // we can skip lexical cells.
        if (end - start > 1) {

            final PackingFunction cpf = grammar.packingFunction();

            final short midpoint = chart.midpoints[cellIndex];
            final int leftChildCellIndex = chart.cellIndex(start, midpoint);
            final int rightChildCellIndex = chart.cellIndex(midpoint, end);

            final int constrainingParentOffset = constrainingChart.offset(cellIndex)
                    + constrainingChart.unaryChainLength(cellIndex) - 1;

            final short constrainingParent = constrainingChart.nonTerminalIndices[constrainingParentOffset];
            final short constrainingLeftChild = constrainingChart.nonTerminalIndices[constrainingChart
                    .offset(leftChildCellIndex)];
            final short constrainingRightChild = constrainingChart.nonTerminalIndices[constrainingChart
                    .offset(rightChildCellIndex)];

            final int firstParentOffset = chart.offset(cellIndex) + ((unaryChainLength - 1) * vocabulary.maxSplits);
            final short firstParent = vocabulary.firstSplitIndices[constrainingChart.nonTerminalIndices[constrainingParentOffset]];
            final short lastParent = (short) (firstParent + vocabulary.ntSplitCounts[firstParent] - 1);

            final int leftCellOffset = chart.offset(leftChildCellIndex);
            final int rightCellOffset = chart.offset(rightChildCellIndex);

            // For debugging with assertions turned on
            boolean foundParent = false;

            // Iterate over all possible child pairs
            final int leftChildSplits = vocabulary.baseNtSplitCounts[constrainingLeftChild];
            final int rightChildSplits = vocabulary.baseNtSplitCounts[constrainingRightChild];

            for (int i = 0; i < leftChildSplits; i++) {

                final int leftChildOffset = leftCellOffset + i;
                final float leftChildInsideProbability = chart.insideProbabilities[leftChildOffset];
                if (leftChildInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                float leftChildOutsideProbability = Float.NEGATIVE_INFINITY;
                final short leftChild = chart.nonTerminalIndices[leftChildOffset];

                // And over children in the right child cell
                for (int j = 0; j < rightChildSplits; j++) {

                    final int rightChildOffset = rightCellOffset + j;
                    final float rightChildInsideProbability = chart.insideProbabilities[rightChildOffset];
                    if (rightChildInsideProbability == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    final int column = cpf.pack(leftChild, chart.nonTerminalIndices[rightChildOffset]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }
                    float rightChildOutsideProbability = chart.outsideProbabilities[rightChildOffset];

                    for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {
                        final short parent = grammar.cscBinaryRowIndices[k];

                        if (parent < firstParent) {
                            continue;

                        } else if (parent > lastParent) {
                            // We've passed all target parents. No need to search more grammar rules
                            break;
                        }

                        // For debugging with assertions turned on
                        assert (vocabulary.getBaseIndex(parent) == constrainingParent);
                        foundParent = true;

                        final int parentOffset = firstParentOffset + parent - firstParent;
                        final float parentOutsideProbability = chart.outsideProbabilities[parentOffset];
                        final float grammarProbability = grammar.cscBinaryProbabilities[k];
                        // Left child: Parent outside * right sibling inside * P(parent -> children)
                        leftChildOutsideProbability = Math.logSum(leftChildOutsideProbability, parentOutsideProbability
                                + rightChildInsideProbability + grammarProbability);
                        // Right child: Parent outside * left sibling inside * P(parent -> children)
                        rightChildOutsideProbability = Math.logSum(rightChildOutsideProbability,
                                parentOutsideProbability + leftChildInsideProbability + grammarProbability);
                    }

                    chart.outsideProbabilities[rightChildOffset] = rightChildOutsideProbability;
                }

                chart.outsideProbabilities[leftChildOffset] = leftChildOutsideProbability;
            }
            assert foundParent;
        }
    }

    private void computeUnaryOutsideProbabilities(final int cellIndex, final int unaryChainLength) {

        final int offset = chart.offset(cellIndex);

        // foreach unary chain depth (starting from 2nd-to-last in the chain; the last entry is the binary child)
        for (int unaryDepth = 1; unaryDepth < unaryChainLength; unaryDepth++) {

            final int firstChildOffset = offset + unaryDepth * vocabulary.maxSplits;
            final int firstParentOffset = firstChildOffset - vocabulary.maxSplits;

            final int constrainingParentOffset = constrainingChart.offset(cellIndex) + unaryDepth - 1;
            final short constrainingChild = constrainingChart.nonTerminalIndices[constrainingParentOffset + 1];

            final short firstParent = vocabulary.firstSplitIndices[constrainingChart.nonTerminalIndices[constrainingParentOffset]];
            final short lastParent = (short) (firstParent + vocabulary.ntSplitCounts[firstParent] - 1);

            // For debugging with assertions turned on
            boolean foundChild = false;

            final short child0 = vocabulary.firstSplitIndices[constrainingChild];
            final int childSplits = vocabulary.baseNtSplitCounts[constrainingChild];

            // Iterate over all possible children
            for (int i = 0; i < childSplits; i++) {

                final short child = (short) (child0 + i);
                final int childOffset = firstChildOffset + i;

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                    final short parent = grammar.cscUnaryRowIndices[j];
                    if (parent < firstParent) {
                        continue;

                    } else if (parent > lastParent) {
                        // We've passed all target parents. No need to search more grammar rules
                        break;
                    }

                    // For debugging with assertions turned on
                    assert (vocabulary.getBaseIndex(child) == constrainingChild);
                    foundChild = true;

                    final int parentOffset = firstParentOffset + parent - firstParent;
                    chart.outsideProbabilities[childOffset] = Math.logSum(chart.outsideProbabilities[childOffset],
                            grammar.cscUnaryProbabilities[j] + chart.outsideProbabilities[parentOffset]);
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
            final short start = startAndEnd[0];
            final short end = startAndEnd[1];

            final int cellIndex = chart.cellIndex(start, end);
            final int unaryChainLength = constrainingChart.unaryChainLength(cellIndex);

            countUnaryRuleOccurrences(countGrammar, start, end, cellIndex, unaryChainLength, sentenceInsideLogProb);
            if (end - start == 1) {
                countLexicalRuleOccurrences(countGrammar, start, end, cellIndex, unaryChainLength,
                        sentenceInsideLogProb);
            } else {
                countBinaryRuleOccurrences(countGrammar, start, end, cellIndex, unaryChainLength, sentenceInsideLogProb);
            }
        }

        return countGrammar;
    }

    // TODO Could these counts be computed during the outside pass?
    private void countBinaryRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end, final int cellIndex, final int unaryChainLength, final float sentenceInsideLogProb) {

        final PackingFunction cpf = grammar.packingFunction();

        final short midpoint = chart.midpoints[cellIndex];
        final int leftChildCellIndex = chart.cellIndex(start, midpoint);
        final int rightChildCellIndex = chart.cellIndex(midpoint, end);

        final short constrainingParent = constrainingChart.nonTerminalIndices[constrainingChart.offset(cellIndex)
                + constrainingChart.unaryChainLength(cellIndex) - 1];
        final short constrainingLeftChild = constrainingChart.nonTerminalIndices[constrainingChart
                .offset(leftChildCellIndex)];
        final short constrainingRightChild = constrainingChart.nonTerminalIndices[constrainingChart
                .offset(rightChildCellIndex)];

        final int firstParentOffset = chart.offset(cellIndex) + ((unaryChainLength - 1) * vocabulary.maxSplits);
        final short firstParent = vocabulary.firstSplitIndices[constrainingParent];
        final short lastParent = (short) (firstParent + vocabulary.ntSplitCounts[firstParent] - 1);
        final short firstLeftChild = vocabulary.firstSplitIndices[constrainingLeftChild];
        final short firstRightChild = vocabulary.firstSplitIndices[constrainingRightChild];

        final int leftChildCellOffset = chart.offset(chart.cellIndex(start, midpoint));
        final int rightChildCellOffset = chart.offset(chart.cellIndex(midpoint, end));

        final int leftChildSplits = vocabulary.baseNtSplitCounts[constrainingLeftChild];
        final int rightChildSplits = vocabulary.baseNtSplitCounts[constrainingRightChild];

        // Iterate over all possible child pairs
        for (int i = 0; i < leftChildSplits; i++) {
            final short leftChild = (short) (firstLeftChild + i);
            if (leftChild < 0) {
                continue;
            }
            final float leftProbability = chart.insideProbabilities[leftChildCellOffset + i];

            // And over children in the right child cell
            for (int j = 0; j < rightChildSplits; j++) {
                final short rightChild = (short) (firstRightChild + j);
                final int column = cpf.pack(leftChild, rightChild);
                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                final float childInsideProbability = leftProbability
                        + chart.insideProbabilities[rightChildCellOffset + j];

                for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {
                    final short parent = grammar.cscBinaryRowIndices[k];

                    if (parent < firstParent) {
                        continue;

                    } else if (parent > lastParent) {
                        // We've passed all target parents. No need to search more grammar rules
                        break;
                    }

                    // Parent outside x left child inside x right child inside x production probability.
                    // Equation 1 of Petrov et al., 2006.
                    final int parentOffset = firstParentOffset + parent - firstParent;
                    final float logCount = chart.outsideProbabilities[parentOffset] + childInsideProbability
                            + grammar.cscBinaryProbabilities[k] - sentenceInsideLogProb;
                    countGrammar.incrementBinaryLogCount(parent, leftChild, rightChild, logCount);
                }
            }
        }
    }

    private void countUnaryRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end, final int cellIndex, final int unaryChainLength, final float sentenceInsideLogProb) {

        // foreach unary chain height (starting from 2nd from bottom in chain; the bottom entry is the binary or lexical
        // parent)
        for (int unaryHeight = 1; unaryHeight < unaryChainLength; unaryHeight++) {

            final int firstChildOffset = chart.offset(cellIndex) + (unaryChainLength - unaryHeight)
                    * vocabulary.maxSplits;
            final int firstParentOffset = firstChildOffset - vocabulary.maxSplits;

            final int constrainingParentOffset = constrainingChart.offset(cellIndex)
                    + (unaryChainLength - unaryHeight - 1);
            final short constrainingChild = constrainingChart.nonTerminalIndices[constrainingChart.offset(cellIndex)
                    + (unaryChainLength - unaryHeight)];

            final short firstParent = vocabulary.firstSplitIndices[constrainingChart.nonTerminalIndices[constrainingParentOffset]];
            final short lastParent = (short) (firstParent + vocabulary.ntSplitCounts[firstParent] - 1);

            final short firstChild = vocabulary.firstSplitIndices[constrainingChild];
            final int childSplits = vocabulary.baseNtSplitCounts[constrainingChild];

            // Iterate over all child slots
            for (int i = 0; i < childSplits; i++) {

                final float childInsideProbability = chart.insideProbabilities[firstChildOffset + i];
                if (childInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }
                final short child = (short) (firstChild + i);

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                    final short parent = grammar.cscUnaryRowIndices[j];

                    if (parent < firstParent) {
                        continue;

                    } else if (parent > lastParent) {
                        // We've passed all target parents. No need to search more grammar rules
                        break;
                    }

                    // Parent outside x child inside x production probability.
                    // Equation 1 of Petrov et al., 2006.
                    final int parentOffset = firstParentOffset + parent - firstParent;
                    final float logCount = chart.outsideProbabilities[parentOffset] + childInsideProbability
                            + grammar.cscUnaryProbabilities[j] - sentenceInsideLogProb;
                    countGrammar.incrementUnaryLogCount(parent, child, logCount);
                }
            }
        }
    }

    private void countLexicalRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end, final int cellIndex, final int unaryChainLength, final float sentenceInsideLogProb) {

        final int firstParentOffset = chart.offset(cellIndex) + ((unaryChainLength - 1) * vocabulary.maxSplits);

        // Beginning of cell + offset for populated unary parents
        final int constrainingParentOffset = constrainingChart.offset(cellIndex) + unaryChainLength - 1;

        final int lexicalProduction = constrainingChart.sparseMatrixGrammar.packingFunction
                .unpackLeftChild(constrainingChart.packedChildren[constrainingParentOffset]);

        final short firstParent = vocabulary.firstSplitIndices[constrainingChart.nonTerminalIndices[constrainingParentOffset]];
        final short lastParent = (short) (firstParent + vocabulary.ntSplitCounts[firstParent] - 1);
        final float[] lexicalLogProbabilities = grammar.lexicalLogProbabilities(lexicalProduction);
        final short[] lexicalParents = grammar.lexicalParents(lexicalProduction);

        // Iterate through grammar lexicon rules matching this word.
        for (int i = 0; i < lexicalParents.length; i++) {
            final short parent = lexicalParents[i];

            if (parent < firstParent) {
                continue;

            } else if (parent > lastParent) {
                // We've passed all target parents. No need to search more grammar rules
                break;

            } else {
                final int parentOffset = firstParentOffset + parent - firstParent;
                // Parent outside * child inside (1) * p(parent -> child) / p(ROOT, 0, n)
                final float logCount = chart.outsideProbabilities[parentOffset] + lexicalLogProbabilities[i]
                        - sentenceInsideLogProb;
                countGrammar.incrementLexicalLogCount(parent, lexicalProduction, logCount);
            }
        }
    }

    /**
     * Estimates the cost of merging each pair of split non-terminals, using the heuristic from Petrov et al., 2006
     * (equation 2 and following). Note: This heuristic will only allow merging the splits from the current split-merge
     * cycle. E.g., on cycle 2, we will consider merging NP_0 and NP_1, but we will not consider merging NP_0 and NP_2.
     * 
     * @param mergeCost
     * @param logSplitFraction
     */
    void countMergeCost(final float[] mergeCost, final float[] logSplitFraction) {

        cellSelector.reset();
        while (cellSelector.hasNext()) {

            final short[] startAndEnd = cellSelector.next();
            final int cellIndex = chart.cellIndex(startAndEnd[0], startAndEnd[1]);
            final int unaryChainLength = constrainingChart.unaryChainLength(cellIndex);

            // foreach unsplit parent (starting from bottom in the unary chain)
            for (int unaryHeight = 0; unaryHeight < unaryChainLength; unaryHeight++) {

                final int constrainingParentIndex = constrainingChart.offset(cellIndex)
                        + (unaryChainLength - unaryHeight - 1);
                final short constrainingParent = constrainingChart.nonTerminalIndices[constrainingParentIndex];
                final short firstParent = vocabulary.firstSplitIndices[constrainingParent];
                final int firstParentOffset = chart.offset(cellIndex) + (unaryChainLength - unaryHeight - 1)
                        * vocabulary.maxSplits;

                // Compute posterior probability of each split non-terminal
                final float[] posteriors = new float[vocabulary.baseNtSplitCounts[constrainingParent]];
                for (int i = 0; i < posteriors.length; i++) {
                    posteriors[i] = chart.insideProbabilities[firstParentOffset + i]
                            + chart.outsideProbabilities[firstParentOffset + i];
                }

                // And the posterior probability of the unsplit non-terminal - the sum of all split posteriors (P(w,T);
                // eq. 2 in Petrov 2006)
                final float PwT = Math.logSumExp(posteriors);

                // foreach split pair
                for (short i = 0; i < posteriors.length; i += 2) {
                    final short parent = (short) (firstParent + i);

                    // Compute the posterior probability of the unsplit non-terminal, with the current pair merged -
                    // P^n(w,T)

                    final float P_in1 = chart.insideProbabilities[firstParentOffset + i];
                    final float P_out1 = chart.outsideProbabilities[firstParentOffset + i];
                    final float P_in2 = chart.insideProbabilities[firstParentOffset + i + 1];
                    final float P_out2 = chart.outsideProbabilities[firstParentOffset + i + 1];

                    // If either of the parent posterior probabilities is 0, we can merge the parents without any loss
                    // at this location
                    if (P_in1 == Float.NEGATIVE_INFINITY || P_out1 == Float.NEGATIVE_INFINITY
                            || P_in2 == Float.NEGATIVE_INFINITY || P_out2 == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    final float mergedInside = Math.logSum(logSplitFraction[parent] + P_in1,
                            logSplitFraction[parent + 1] + P_in2);
                    final float mergedOutside = Math.logSum(P_out1, P_out2);

                    final float PnwT = Math.logSumExp(posteriorsWithPairMerged(posteriors, i / 2, mergedInside
                            + mergedOutside));

                    // Loss = P^n(w,T) / P(w,T)
                    final float loss = -(PnwT - PwT);
                    mergeCost[parent >> 1] += loss;
                }
            }
        }
    }

    private float[] posteriorsWithPairMerged(final float[] posteriors, final int merged, final float mergedPosterior) {
        final float[] array = new float[posteriors.length - 1];

        // Copy all splits before the merge
        if (merged > 0) {
            System.arraycopy(posteriors, 0, array, 0, merged);
        }

        // The pair we merged
        array[merged] = mergedPosterior;

        // And splits after the merge
        if (merged < posteriors.length - 2) {
            System.arraycopy(posteriors, merged + 2, array, merged + 1, posteriors.length - merged - 2);
        }
        return array;
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
