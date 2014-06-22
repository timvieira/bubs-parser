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
     * n entries for the substates of the constraining non-terminal
     * 
     * k unary children of each of those states, where k is the maximum length of a unary chain in the constraining
     * chart.
     * 
     * So the maximum number of entries in a cell is n * (1 + maxUnaryChainLength)
     * 
     * @param constrainingChart
     * @param sparseMatrixGrammar
     */
    protected ConstrainedChart(final ConstrainingChart constrainingChart, final SparseMatrixGrammar sparseMatrixGrammar) {
        this(constrainingChart, sparseMatrixGrammar, ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits);
    }

    /**
     * Constructs a {@link ConstrainedChart} based on a chart containing a sentence parsed with the parent grammar.
     * 
     * At most, a cell can contain:
     * 
     * <code>splits</code> entries for the substates of the constraining non-terminal
     * 
     * k unary children of each of those states, where k is the maximum length of a unary chain in the constraining
     * chart.
     * 
     * So the maximum number of entries in a cell is splits * (1 + maxUnaryChainLength)
     * 
     * @param constrainingChart
     * @param sparseMatrixGrammar
     * @param splits
     */
    protected ConstrainedChart(final ConstrainingChart constrainingChart,
            final SparseMatrixGrammar sparseMatrixGrammar, final int splits) {

        super(constrainingChart, splitChartArraySize(constrainingChart.size(), constrainingChart.maxUnaryChainLength(),
                splits), sparseMatrixGrammar);

        this.splitVocabulary = (SplitVocabulary) sparseMatrixGrammar.nonTermSet;
        this.outsideProbabilities = new float[insideProbabilities.length];
        this.beamWidth = lexicalRowBeamWidth = splits;
        clear(constrainingChart);

        // Calculate all cell offsets, etc
        computeOffsets();
    }

    static int splitChartArraySize(final int size, final int maxUnaryChainLength, final int splits) {
        return (size * (size + 1) / 2) * (maxUnaryChainLength + 1) * splits;
    }

    public static int chartArraySize(final int size, final int maxUnaryChainLength) {
        return splitChartArraySize(size, maxUnaryChainLength, 2);
    }

    @Override
    public void reset(final ParseTask task) {
        throw new UnsupportedOperationException();
    }

    public void clear(final ConstrainingChart constrainingChart) {
        this.size = constrainingChart.size();
        this.openCells = constrainingChart.openCells;
        this.parentCellIndices = constrainingChart.parentCellIndices;
        this.siblingCellIndices = constrainingChart.siblingCellIndices;
        this.maxUnaryChainLength = constrainingChart.maxUnaryChainLength;

        System.arraycopy(constrainingChart.unaryChainLength, 0, unaryChainLength, 0,
                constrainingChart.unaryChainLength.length);

        final int fillLength = splitChartArraySize(constrainingChart.size(), constrainingChart.maxUnaryChainLength(),
                ((SplitVocabulary) grammar.nonTermSet).maxSplits);
        Arrays.fill(nonTerminalIndices, 0, fillLength, Short.MIN_VALUE);
        Arrays.fill(packedChildren, 0, fillLength, 0);
        Arrays.fill(insideProbabilities, 0, fillLength, Float.NEGATIVE_INFINITY);
        Arrays.fill(outsideProbabilities, 0, fillLength, Float.NEGATIVE_INFINITY);
        System.arraycopy(constrainingChart.midpoints, 0, midpoints, 0, constrainingChart.midpoints.length);

        computeOffsets();
    }

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
        int entryOffset = maxInsideProbabilityEntry(entry0Offset);
        final int maxSplits = ((SplitVocabulary) grammar.nonTermSet).maxSplits;

        final BinaryTree<String> tree = new BinaryTree<String>(
                grammar.nonTermSet.getSymbol(nonTerminalIndices[entryOffset]));
        BinaryTree<String> subtree = tree;

        // Add unary productions and binary parent
        while (entry0Offset < cellOffset + (unaryChainLength[cellIndex] - 1) * maxSplits) {
            entry0Offset += maxSplits;
            entryOffset = maxInsideProbabilityEntry(entry0Offset);
            subtree = subtree.addChild(grammar.nonTermSet.getSymbol(nonTerminalIndices[entryOffset]));
        }

        if (packedChildren[entryOffset] < 0) {
            // Lexical production
            final String sChild = grammar.lexSet.getSymbol(sparseMatrixGrammar.packingFunction().unpackLeftChild(
                    packedChildren[entryOffset]));
            subtree.addChild(new BinaryTree<String>(sChild));
        } else {
            // Binary production
            final short edgeMidpoint = midpoints[cellIndex(start, end)];
            subtree.addChild(extractInsideParse(start, edgeMidpoint));
            subtree.addChild(extractInsideParse(edgeMidpoint, end));
        }

        return tree;
    }

    public int maxInsideProbabilityEntry(final int offset) {
        int entryIndex = offset;
        for (int i = entryIndex + 1; i < offset + beamWidth; i++) {
            if (insideProbabilities[i] > insideProbabilities[entryIndex]) {
                entryIndex = i;
            }
        }
        return entryIndex;
    }

    public BinaryTree<String> extractViterbiParse(final int start, final int end, int parent) {

        final int cellIndex = cellIndex(start, end);
        final int cellOffset = cellOffset(start, end);
        int entry0Offset = cellOffset;
        int entryOffset = (nonTerminalIndices[entry0Offset] == parent) ? entry0Offset : entry0Offset + 1;

        final BinaryTree<String> tree = new BinaryTree<String>(
                grammar.nonTermSet.getSymbol(nonTerminalIndices[entryOffset]));
        BinaryTree<String> subtree = tree;

        // Add unary productions and binary parent
        while (entry0Offset < cellOffset + unaryChainLength[cellIndex] * (beamWidth - 1)) {
            entry0Offset += beamWidth;
            entryOffset = (insideProbabilities[entry0Offset + 1] > insideProbabilities[entry0Offset]) ? entry0Offset + 1
                    : entry0Offset;
            subtree = subtree.addChild(grammar.nonTermSet.getSymbol(nonTerminalIndices[entryOffset]));
            parent = sparseMatrixGrammar.packingFunction().unpackLeftChild(packedChildren[entryOffset]);
        }

        if (packedChildren[entryOffset] < 0) {
            // Lexical production
            final String sChild = grammar.lexSet.getSymbol(sparseMatrixGrammar.packingFunction().unpackLeftChild(
                    packedChildren[entryOffset]));
            subtree.addChild(new BinaryTree<String>(sChild));
        } else {
            // Binary production
            final short edgeMidpoint = midpoints[cellIndex(start, end)];
            subtree.addChild(extractViterbiParse(start, edgeMidpoint, sparseMatrixGrammar.packingFunction()
                    .unpackLeftChild(packedChildren[entryOffset])));
            subtree.addChild(extractViterbiParse(edgeMidpoint, end, sparseMatrixGrammar.packingFunction()
                    .unpackRightChild(packedChildren[entryOffset])));
        }

        return tree;
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int offset = cellOffset(start, end);

        for (int i = offset; i < offset + (beamWidth * maxUnaryChainLength); i++) {
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
     * @return Inside (log) probability
     */
    float getInside(final int start, final int end, final int nonTerminal, final int unaryHeight) {
        final int index = cellOffset(start, end) + ((maxUnaryChainLength - unaryHeight) << 1);

        for (int i = index; i < index + (beamWidth * maxUnaryChainLength); i++) {
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
        for (int i = offset; i < offset + (beamWidth * maxUnaryChainLength); i++) {
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
     * @return Outside (log) probability
     */
    float getOutside(final int start, final int end, final int nonTerminal, final int unaryHeight) {
        final int index = cellOffset(start, end) + ((maxUnaryChainLength - unaryHeight) << 1);

        for (int i = index; i < index + (beamWidth * maxUnaryChainLength); i++) {
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

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        throw new UnsupportedOperationException("Not supported by ConstrainedChart");
    }

    @Override
    public String toString(final boolean formatFractions, final boolean includeEmptyCells) {
        final StringBuilder sb = new StringBuilder(1024);

        for (int span = 1; span <= size; span++) {
            for (int start = 0; start <= size - span; start++) {
                final int end = start + span;
                final int cellIndex = cellIndex(start, end);
                if (unaryChainLength[cellIndex] == 0) {
                    continue;
                }

                final int cellOffset = offset(cellIndex);
                final StringBuilder sb2 = new StringBuilder(128);

                // Format unary parents first, followed by the bottom entry(ies)
                final int bottomEntryOffset = cellOffset + (unaryChainLength[cellIndex] - 1) * beamWidth;
                for (int offset = cellOffset; offset < bottomEntryOffset; offset += beamWidth) {
                    sb2.append(formatEntries(offset, true, formatFractions));
                }
                sb2.append(formatEntries(bottomEntryOffset, false, formatFractions));
                sb2.append("\n\n");

                // Optionally skip empty cells
                if (sb2.length() > 2 || includeEmptyCells) {
                    sb.append("ConstrainedChartCell[" + start + "][" + end + "]\n");
                    sb.append(sb2.toString());
                }
            }
        }

        return sb.toString();
    }

    private String formatEntries(final int offset, final boolean unary, final boolean formatFractions) {
        final StringBuilder sb = new StringBuilder(128);

        for (int i = 0; i < ((SplitVocabulary) grammar.nonTermSet).maxSplits; i++) {
            if (insideProbabilities[offset + i] != Float.NEGATIVE_INFINITY) {
                sb.append(formatCellEntry(nonTerminalIndices[offset + i], packedChildren[offset + i], unary,
                        insideProbabilities[offset + i], outsideProbabilities[offset + i], formatFractions));
            }
        }
        return sb.toString();
    }

    private String formatCellEntry(final int nonterminal, final int childProductions, final boolean unary,
            final float insideProbability, final float outsideProbability, final boolean formatFractions) {

        final int leftChild = sparseMatrixGrammar.packingFunction().unpackLeftChild(childProductions);
        final int rightChild = sparseMatrixGrammar.packingFunction().unpackRightChild(childProductions);

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
