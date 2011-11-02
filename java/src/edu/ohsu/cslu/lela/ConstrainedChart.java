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

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * Represents a parse chart constrained by a parent chart for inside-outside parameter estimation constrained by gold
 * trees.
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedChart extends ConstrainingChart {

    /**
     * Outside probabilities (parallel to {@link ParallelArrayChart#insideProbabilities},
     * {@link ParallelArrayChart#packedChildren}, and {@link PackedArrayChart#nonTerminalIndices}. For constrained
     * parsing, the midpoints are fixed, so the {@Link ParallelArrayChart#midpoints} array is unused. Entries for
     * each cell begin at indices from {@link #cellOffsets}.
     */
    public final float[] outsideProbabilities;

    protected final SplitVocabulary splitVocabulary;

    /**
     * Constructs a {@link ConstrainedChart} based on a chart containing a sentence parsed with the parent grammar.
     * 
     * At most, a cell can contain:
     * 
     * 2 entries for the substates of the constraining non-terminal
     * 
     * k unary children of each of those states, where k is the maximum length of a unary chain in the constraining
     * chart.
     * 
     * So the maximum number of entries in a cell is 2 * (1 + maxUnaryChainLength)
     * 
     * @param constrainingChart
     * @param sparseMatrixGrammar
     */
    protected ConstrainedChart(final ConstrainingChart constrainingChart, final SparseMatrixGrammar sparseMatrixGrammar) {

        super(constrainingChart, chartArraySize(constrainingChart.size(), constrainingChart.maxUnaryChainLength()),
                sparseMatrixGrammar);

        this.splitVocabulary = (SplitVocabulary) sparseMatrixGrammar.nonTermSet;
        this.outsideProbabilities = new float[insideProbabilities.length];
        clear(constrainingChart);

        // Calculate all cell offsets, etc
        computeOffsets();
    }

    static int chartArraySize(final int size, final int maxUnaryChainLength) {
        return (size * (size + 1) / 2) * (maxUnaryChainLength + 1) * 2;
    }

    @Override
    public void reset(final ParseTask task) {
        throw new UnsupportedOperationException();
    }

    public void clear(final ConstrainingChart constrainingChart) {
        this.size = constrainingChart.size();
        this.beamWidth = lexicalRowBeamWidth = 2 * constrainingChart.maxUnaryChainLength();
        this.openCells = constrainingChart.openCells;
        this.parentCellIndices = constrainingChart.parentCellIndices;
        this.siblingCellIndices = constrainingChart.siblingCellIndices;
        this.maxUnaryChainLength = constrainingChart.maxUnaryChainLength;

        System.arraycopy(constrainingChart.unaryChainLength, 0, unaryChainLength, 0,
                constrainingChart.unaryChainLength.length);

        final int fillLength = chartArraySize(constrainingChart.size(), constrainingChart.maxUnaryChainLength());
        Arrays.fill(nonTerminalIndices, 0, fillLength, Short.MIN_VALUE);
        Arrays.fill(packedChildren, 0, fillLength, 0);
        Arrays.fill(insideProbabilities, 0, fillLength, Float.NEGATIVE_INFINITY);
        Arrays.fill(outsideProbabilities, 0, fillLength, Float.NEGATIVE_INFINITY);
        System.arraycopy(constrainingChart.midpoints, 0, midpoints, 0, constrainingChart.midpoints.length);

        computeOffsets();
    }

    // TODO Unused?
    // void shiftCellEntriesDownward(final int topEntryOffset) {
    // for (int unaryDepth = maxUnaryChainLength - 2; unaryDepth >= 0; unaryDepth--) {
    // final int origin = topEntryOffset + unaryDepth * splitVocabulary.maxSplits;
    // final int destination = origin + splitVocabulary.maxSplits;
    //
    // nonTerminalIndices[destination] = nonTerminalIndices[origin];
    // insideProbabilities[destination] = insideProbabilities[origin];
    // packedChildren[destination] = packedChildren[origin];
    // }
    // nonTerminalIndices[topEntryOffset] = Short.MIN_VALUE;
    // insideProbabilities[topEntryOffset] = Float.NEGATIVE_INFINITY;
    // packedChildren[topEntryOffset] = 0;
    // }

    private void computeOffsets() {
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end <= size; end++) {
                final int cellIndex = cellIndex(start, end);
                cellOffsets[cellIndex] = cellOffset(start, end);
            }
        }
    }

    /**
     * @param cellOffset
     * @return The unary chain depth of a cell, including the binary or lexical production. e.g. unaryChainDepth(a ->
     *         foo) = 2 and unaryChainDepth(s -> a -> a -> a b) = 3
     */
    int unaryChainDepth(final int cellOffset) {

        for (int unaryDepth = 0; unaryDepth < maxUnaryChainLength; unaryDepth++) {
            if (nonTerminalIndices[cellOffset + (unaryDepth << 1)] < 0) {
                return unaryDepth;
            }
        }
        return maxUnaryChainLength;
    }

    @Override
    public BinaryTree<String> extractBestParse(final int start, final int end, final int parent) {
        return extractInsideParse(start, end);
    }

    @Override
    public BinaryTree<String> extractInsideParse(final int start, final int end) {

        final int cellIndex = cellIndex(start, end);
        final int cellOffset = cellOffset(start, end);
        int entry0Offset = cellOffset;
        int entryOffset = (insideProbabilities[entry0Offset + 1] > insideProbabilities[entry0Offset]) ? entry0Offset + 1
                : entry0Offset;

        final BinaryTree<String> tree = new BinaryTree<String>(
                grammar.nonTermSet.getSymbol(nonTerminalIndices[entryOffset]));
        BinaryTree<String> subtree = tree;

        // Add unary productions and binary parent
        while (entry0Offset < cellOffset + unaryChainLength[cellIndex] * 2 - 2) {
            entry0Offset += 2;
            entryOffset = (insideProbabilities[entry0Offset + 1] > insideProbabilities[entry0Offset]) ? entry0Offset + 1
                    : entry0Offset;
            subtree = subtree.addChild(grammar.nonTermSet.getSymbol(nonTerminalIndices[entryOffset]));
        }

        if (packedChildren[entryOffset] < 0) {
            // Lexical production
            final String sChild = grammar.lexSet.getSymbol(sparseMatrixGrammar.cartesianProductFunction()
                    .unpackLeftChild(packedChildren[entryOffset]));
            subtree.addChild(new BinaryTree<String>(sChild));
        } else {
            // Binary production
            final short edgeMidpoint = midpoints[cellIndex(start, end)];
            subtree.addChild(extractInsideParse(start, edgeMidpoint));
            subtree.addChild(extractInsideParse(edgeMidpoint, end));
        }

        return tree;
    }

    public void countRuleObservations(final FractionalCountGrammar g) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int offset = cellOffset(start, end);

        for (int i = offset; i < offset + beamWidth; i++) {
            if (nonTerminalIndices[i] == nonTerminal) {
                return insideProbabilities[i];
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * For unit testing
     * 
     * @param start
     * @param end
     * @param nonTerminal
     * @param unaryHeight 0 <= unaryDepth < maxUnaryChainHeight
     * @return
     */
    float getInside(final int start, final int end, final int nonTerminal, final int unaryHeight) {
        final int index = cellOffset(start, end) + ((maxUnaryChainLength - unaryHeight) << 1);

        for (int i = index; i < index + beamWidth; i++) {
            if (nonTerminalIndices[i] == nonTerminal) {
                return insideProbabilities[i];
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    public float getOutside(final int start, final int end, final short nonTerminal) {
        final int offset = cellOffset(start, end);

        // We could compute the possible indices directly, but it's a very short range to linear search, and
        // this method
        // should only be called in testing
        for (int i = offset; i < offset + beamWidth; i++) {
            if (nonTerminalIndices[i] == nonTerminal) {
                return outsideProbabilities[i];
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * For unit testing
     * 
     * @param start
     * @param end
     * @param nonTerminal
     * @param unaryHeight 0 <= unaryDepth < maxUnaryChainHeight
     * @return
     */
    float getOutside(final int start, final int end, final int nonTerminal, final int unaryHeight) {
        final int index = cellOffset(start, end) + ((maxUnaryChainLength - unaryHeight) << 1);

        for (int i = index; i < index + beamWidth; i++) {
            if (nonTerminalIndices[i] == nonTerminal) {
                return outsideProbabilities[i];
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public int maxUnaryChainLength() {
        return maxUnaryChainLength;
    }

    // /**
    // * Populates a new {@link ConstrainedChart} with a re-merged grammar, including all merged non-terminals populated
    // * in the pre-merge chart.
    // *
    // * @param mergedGrammar
    // * @return Chart containing merged versions of the current chart's pre-merge non-terminals.
    // */
    // public ConstrainedChart merge(final SparseMatrixGrammar mergedGrammar) {
    //
    // final SplitVocabulary mergedVocabulary = (SplitVocabulary) mergedGrammar.nonTermSet;
    // final ConstrainedChart mergedChart = new ConstrainedChart(this, mergedGrammar, mergedVocabulary.maxSplits
    // * maxUnaryChainLength);
    //
    // // Iterate over each open cell
    // for (final short[] startAndEnd : openCells) {
    //
    // // Iterate over each entry in the existing chart
    // for (int unaryDepth = 0; unaryDepth < maxUnaryChainLength; unaryDepth++) {
    //
    // final int cellIndex = cellIndex(startAndEnd[0], startAndEnd[1]);
    // final int offset = offset(cellIndex) + splitVocabulary.maxSplits * unaryDepth;
    // final int mergedOffset = mergedChart.offset(cellIndex) + mergedVocabulary.maxSplits * unaryDepth;
    //
    // // Stop when we reach an un-populated unary depth
    // if (nonTerminalIndices[offset] < 0) {
    // break;
    // }
    //
    // for (int i = offset; i < offset + splitVocabulary.maxSplits; i++) {
    //
    // final short parent = nonTerminalIndices[i];
    // if (parent < 0) {
    // continue;
    // }
    // final float probability = insideProbabilities[i];
    //
    // // Record the parent non-terminal and sum the probabilities
    // final short mergedParent = mergedVocabulary.mergedIndices.get(parent);
    // final int mergedEntryIndex = mergedOffset + mergedVocabulary.subcategoryIndices[mergedParent];
    // mergedChart.nonTerminalIndices[mergedEntryIndex] = mergedParent;
    //
    // final float currentMergedProbability = mergedChart.insideProbabilities[mergedEntryIndex];
    // mergedChart.insideProbabilities[mergedEntryIndex] = currentMergedProbability == Float.NEGATIVE_INFINITY ?
    // probability
    // : Math.logSum(currentMergedProbability, probability);
    //
    // // Record packed children
    // if (packedChildren[i] < 0) {
    // // A lexical entry is the same regardless of grammar
    // final int leftChild = sparseMatrixGrammar.packingFunction.unpackLeftChild(packedChildren[i]);
    // mergedChart.packedChildren[mergedEntryIndex] = mergedGrammar.packingFunction
    // .packLexical(leftChild);
    // }
    // }
    // }
    // }
    // return mergedChart;
    // }

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        throw new UnsupportedOperationException("Not supported by ConstrainedChart");
    }

    @Override
    public String toString(final boolean formatFractions) {
        final StringBuilder sb = new StringBuilder(1024);

        for (int span = 1; span <= size; span++) {
            for (int start = 0; start <= size - span; start++) {
                final int end = start + span;
                final int cellIndex = cellIndex(start, end);
                final int offset0 = offset(cellIndex);
                final StringBuilder sb2 = new StringBuilder(128);

                // Format unary parents first, followed by the two bottom entries
                final int bottomEntryOffset = offset0 + (unaryChainLength(cellIndex) - 1) * 2;
                for (int offset = offset0; offset < bottomEntryOffset; offset += 2) {
                    sb2.append(formatEntries(offset, true, formatFractions));
                }
                sb2.append(formatEntries(bottomEntryOffset, false, formatFractions));
                sb2.append("\n\n");

                // Skip empty cells
                if (sb2.length() > 0) {
                    sb.append("ConstrainingChartCell[" + start + "][" + end + "]\n");
                    sb.append(sb2.toString());
                }
            }
        }

        return sb.toString();
    }

    protected String formatEntries(final int offset, final boolean unary, final boolean formatFractions) {
        final StringBuilder sb = new StringBuilder(128);
        if (nonTerminalIndices[offset] >= 0) {
            sb.append(formatCellEntry(nonTerminalIndices[offset], packedChildren[offset], unary,
                    insideProbabilities[offset], outsideProbabilities[offset], formatFractions));
        }
        if (nonTerminalIndices[offset + 1] >= 0) {
            sb.append(formatCellEntry(nonTerminalIndices[offset + 1], packedChildren[offset + 1], unary,
                    insideProbabilities[offset + 1], outsideProbabilities[offset + 1], formatFractions));
        }
        return sb.toString();
    }

    protected String formatCellEntry(final int nonterminal, final int childProductions, final boolean unary,
            final float insideProbability, final float outsideProbability, final boolean formatFractions) {

        final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(childProductions);
        final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(childProductions);

        if (rightChild == Production.LEXICAL_PRODUCTION) {
            // Lexical Production
            return String.format("%s -> %s (%.5f,%.5f)\n", sparseMatrixGrammar.mapNonterminal(nonterminal),
                    sparseMatrixGrammar.mapLexicalEntry(leftChild), insideProbability, outsideProbability);
        } else if (unary) {
            // Unary Production
            return String.format("%s -> unary (%.5f,%.5f)\n", sparseMatrixGrammar.mapNonterminal(nonterminal),
                    insideProbability, outsideProbability);
        } else {
            return String.format("%s -> binary (%.5f,%.5f)\n", sparseMatrixGrammar.mapNonterminal(nonterminal),
                    insideProbability, outsideProbability);
        }
    }
}
