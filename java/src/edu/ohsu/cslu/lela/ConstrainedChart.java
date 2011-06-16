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

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.util.Math;

/**
 * Represents a parse chart constrained by a parent chart (e.g., for inside-outside parameter estimation constrained by
 * gold trees or (possibly) coarse-to-fine parsing).
 * 
 * @author Aaron Dunlop
 * @since Jun 2, 2010
 */
public class ConstrainedChart extends ParallelArrayChart {

    /**
     * Parallel arrays storing non-terminals and outside probabilities (parallel to
     * {@link ParallelArrayChart#insideProbabilities}, {@link ParallelArrayChart#packedChildren}, and
     * {@link ParallelArrayChart#midpoints}. Entries for each cell begin at indices from {@link #cellOffsets}.
     */
    public final short[] nonTerminalIndices;
    public final float[] outsideProbabilities;

    private final SplitVocabulary splitVocabulary;

    private final SymbolSet<String> lexicon;

    short[][] openCells;

    int maxUnaryChainLength;

    /**
     * Constructs a {@link ConstrainedChart}.
     * 
     * At most, a cell can contain:
     * 
     * 1 entry for each substate of the constraining non-terminal
     * 
     * k unary children of each of those states, where k is the maximum length of a unary chain in the constraining
     * chart.
     * 
     * So the maximum number of entries in a cell is maxSubstates * (1 + maxUnaryChainLength)
     * 
     * @param constrainingChart
     * @param sparseMatrixGrammar
     */
    protected ConstrainedChart(final ConstrainedChart constrainingChart, final SparseMatrixGrammar sparseMatrixGrammar,
            final int beamWidth) {
        super(constrainingChart.size, chartArraySize(constrainingChart.size, constrainingChart.maxUnaryChainLength,
                ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits), sparseMatrixGrammar);
        this.splitVocabulary = (SplitVocabulary) sparseMatrixGrammar.nonTermSet;
        this.beamWidth = lexicalRowBeamWidth = beamWidth;
        this.nonTerminalIndices = new short[chartArraySize];
        Arrays.fill(nonTerminalIndices, Short.MIN_VALUE);
        Arrays.fill(insideProbabilities, Float.NEGATIVE_INFINITY);
        this.outsideProbabilities = new float[insideProbabilities.length];
        Arrays.fill(outsideProbabilities, Float.NEGATIVE_INFINITY);
        this.lexicon = sparseMatrixGrammar.lexSet;
        this.openCells = constrainingChart.openCells;
        this.maxUnaryChainLength = constrainingChart.maxUnaryChainLength;

        System.arraycopy(constrainingChart.midpoints, 0, midpoints, 0, midpoints.length);

        // Calculate all cell offsets, etc
        computeOffsets();
    }

    protected ConstrainedChart(final ConstrainedChart constrainingChart, final SparseMatrixGrammar sparseMatrixGrammar) {
        this(constrainingChart, sparseMatrixGrammar, ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits
                * constrainingChart.maxUnaryChainLength);
    }

    public ConstrainedChart(final BinaryTree<String> goldTree, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(goldTree.leaves(), chartArraySize(goldTree.leaves(), goldTree.maxUnaryChainLength(),
                ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits), sparseMatrixGrammar);
        this.splitVocabulary = (SplitVocabulary) sparseMatrixGrammar.nonTermSet;
        this.lexicon = sparseMatrixGrammar.lexSet;

        this.nonTerminalIndices = new short[chartArraySize];
        Arrays.fill(nonTerminalIndices, Short.MIN_VALUE);

        this.outsideProbabilities = new float[insideProbabilities.length];
        Arrays.fill(outsideProbabilities, Float.NEGATIVE_INFINITY);

        this.maxUnaryChainLength = goldTree.maxUnaryChainLength() + 1;
        this.beamWidth = this.lexicalRowBeamWidth = ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits
                * maxUnaryChainLength;

        final IntArrayList tokenList = new IntArrayList();

        short start = 0;
        for (final BinaryTree<String> node : goldTree.preOrderTraversal()) {
            if (node.isLeaf()) {
                // Increment the start index every time we process a leaf. The lexical entry was already
                // populated (see below)
                start++;
                continue;
            }

            final int end = start + node.leaves();
            final short parent = (short) splitVocabulary.getInt(node.label());
            final int cellIndex = cellIndex(start, end);

            // Find the index of this non-terminal in the main chart array.
            // Unary children are positioned _after_ parents
            final int cellOffset = cellOffset(start, end);
            final int i = cellOffset + splitVocabulary.subcategoryIndices[parent] + splitVocabulary.maxSplits
                    * node.unaryChainDepth();

            if (node.rightChild() == null) {
                if (node.leftChild().isLeaf()) {
                    // Lexical production
                    midpoints[cellIndex] = 0;
                    final int child = lexicon.getIndex(node.leftChild().label());
                    packedChildren[i] = sparseMatrixGrammar.packingFunction.packLexical(child);

                    tokenList.add(child);
                }
            } else {
                // Binary production
                midpoints[cellIndex] = (short) (start + node.leftChild().leaves());
            }
            nonTerminalIndices[i] = parent;
            insideProbabilities[i] = 0;
        }

        // Populate openCells with the start/end pairs of each populated cell. Used by {@link
        // ConstrainedCellSelector}
        this.openCells = new short[size * 2 - 1][2];
        int i = 0;
        for (short span = 1; span <= size; span++) {
            for (short s = 0; s < size - span + 1; s++) {
                if (nonTerminalIndices[cellOffset(s, s + span)] != Short.MIN_VALUE) {
                    openCells[i][0] = s;
                    openCells[i][1] = (short) (s + span);
                    i++;
                }
            }
        }

        this.tokens = tokenList.toIntArray();

        // Calculate all cell offsets, etc
        computeOffsets();
    }

    static int chartArraySize(final int size, final int maxUnaryChainLength, final int maxSplits) {
        return (size * (size + 1) / 2) * (maxUnaryChainLength + 1) * maxSplits;
    }

    @Override
    public void clear(final int sentenceLength) {
        throw new UnsupportedOperationException();
    }

    public void clear(final ConstrainedChart constrainingChart) {
        this.size = constrainingChart.size;
        final short maxSplits = ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits;
        this.beamWidth = lexicalRowBeamWidth = constrainingChart.maxUnaryChainLength * maxSplits;
        final int fillLength = chartArraySize(constrainingChart.size, constrainingChart.maxUnaryChainLength, maxSplits);
        Arrays.fill(nonTerminalIndices, 0, fillLength, Short.MIN_VALUE);
        Arrays.fill(packedChildren, 0, fillLength, 0);
        Arrays.fill(outsideProbabilities, 0, fillLength, Float.NEGATIVE_INFINITY);
        computeOffsets();

        this.openCells = constrainingChart.openCells;
        this.maxUnaryChainLength = constrainingChart.maxUnaryChainLength;
    }

    void shiftCellEntriesDownward(final int topEntryOffset) {
        for (int unaryDepth = maxUnaryChainLength - 2; unaryDepth >= 0; unaryDepth--) {
            final int origin = topEntryOffset + unaryDepth * splitVocabulary.maxSplits;
            final int destination = origin + splitVocabulary.maxSplits;

            nonTerminalIndices[destination] = nonTerminalIndices[origin];
            insideProbabilities[destination] = insideProbabilities[origin];
            packedChildren[destination] = packedChildren[origin];
        }
        nonTerminalIndices[topEntryOffset] = Short.MIN_VALUE;
        insideProbabilities[topEntryOffset] = Float.NEGATIVE_INFINITY;
        packedChildren[topEntryOffset] = 0;
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
            if (nonTerminalIndices[cellOffset + unaryDepth * splitVocabulary.maxSplits] < 0) {
                return unaryDepth;
            }
        }
        return maxUnaryChainLength;
    }

    @Override
    public BinaryTree<String> extractBestParse(final int start, final int end, final int parent) {
        return extractInsideParse(start, end, 0);
    }

    private BinaryTree<String> extractInsideParse(final int start, final int end, final int unaryDepth) {

        if (unaryDepth > maxUnaryChainLength) {
            throw new IllegalArgumentException("Max unary chain length exceeded");
        }

        final int cellOffset = cellOffset(start, end);
        final int startIndex = cellOffset + unaryDepth * splitVocabulary.maxSplits;

        // Find maximum probability parent
        float maxProbability = Float.NEGATIVE_INFINITY;
        short maxProbabilityParent = Short.MIN_VALUE;
        int maxProbabilityIndex = -1;
        for (int i = startIndex; i < startIndex + splitVocabulary.splitCount[nonTerminalIndices[startIndex]]; i++) {
            if (insideProbabilities[i] > maxProbability) {
                maxProbability = insideProbabilities[i];
                maxProbabilityParent = nonTerminalIndices[i];
                maxProbabilityIndex = i;
            }
        }

        final BinaryTree<String> subtree = new BinaryTree<String>(splitVocabulary.getSymbol(maxProbabilityParent));

        if (packedChildren[maxProbabilityIndex] < 0) {
            // Lexical production
            final String sChild = lexicon.getSymbol(sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(
                    packedChildren[maxProbabilityIndex]));
            subtree.addChild(new BinaryTree<String>(sChild));
        } else if (unaryDepth < unaryChainDepth(cellOffset) - 1) {
            // Unary production
            subtree.addChild(extractInsideParse(start, end, unaryDepth + 1));
        } else {
            // Binary production
            final short edgeMidpoint = midpoints[cellIndex(start, end)];
            subtree.addChild(extractInsideParse(start, edgeMidpoint, 0));
            subtree.addChild(extractInsideParse(edgeMidpoint, end, 0));
        }
        return subtree;
    }

    public void countRuleObservations(final FractionalCountGrammar grammar) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int offset = cellOffset(start, end);

        // We could compute the possible indices directly, but it's a very short range to linear search, and
        // this method
        // should only be called in testing
        for (int i = offset; i < offset + beamWidth; i++) {
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
     * Populates a new {@link ConstrainedChart} with a re-merged grammar, including all merged non-terminals populated
     * in the pre-merge chart.
     * 
     * @param mergedGrammar
     * @return Chart containing merged versions of the current chart's pre-merge non-terminals.
     */
    public ConstrainedChart merge(final SparseMatrixGrammar mergedGrammar) {

        final SplitVocabulary mergedVocabulary = (SplitVocabulary) mergedGrammar.nonTermSet;
        final ConstrainedChart mergedChart = new ConstrainedChart(this, mergedGrammar, mergedVocabulary.maxSplits
                * maxUnaryChainLength);

        // Iterate over each open cell
        for (final short[] startAndEnd : openCells) {

            // Iterate over each entry in the existing chart
            for (int unaryDepth = 0; unaryDepth < maxUnaryChainLength; unaryDepth++) {

                final int cellIndex = cellIndex(startAndEnd[0], startAndEnd[1]);
                final int offset = offset(cellIndex) + splitVocabulary.maxSplits * unaryDepth;
                final int mergedOffset = mergedChart.offset(cellIndex) + mergedVocabulary.maxSplits * unaryDepth;

                // Stop when we reach an un-populated unary depth
                if (nonTerminalIndices[offset] < 0) {
                    break;
                }

                for (int i = offset; i < offset + splitVocabulary.maxSplits; i++) {

                    final short parent = nonTerminalIndices[i];
                    if (parent < 0) {
                        continue;
                    }
                    final float probability = insideProbabilities[i];

                    // Record the parent non-terminal and sum the probabilities
                    final short mergedParent = mergedVocabulary.mergedIndices.get(parent);
                    final int mergedEntryIndex = mergedOffset + mergedVocabulary.subcategoryIndices[mergedParent];
                    mergedChart.nonTerminalIndices[mergedEntryIndex] = mergedParent;

                    final float currentMergedProbability = mergedChart.insideProbabilities[mergedEntryIndex];
                    mergedChart.insideProbabilities[mergedEntryIndex] = currentMergedProbability == Float.NEGATIVE_INFINITY ? probability
                            : Math.logSum(currentMergedProbability, probability);

                    // Record packed children
                    if (packedChildren[i] < 0) {
                        // A lexical entry is the same regardless of grammar
                        final int leftChild = sparseMatrixGrammar.packingFunction.unpackLeftChild(packedChildren[i]);
                        mergedChart.packedChildren[mergedEntryIndex] = mergedGrammar.packingFunction
                                .packLexical(leftChild);
                    }
                }
            }
        }
        return mergedChart;
    }

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        throw new UnsupportedOperationException("Not supported by ConstrainedChart");
    }

    // TODO Is this ever called? Can we drop the entire ConstrainedChartCell class?
    @Override
    public ConstrainedChartCell getCell(final int start, final int end) {
        return new ConstrainedChartCell(start, end);
    }

    @Override
    public String toString() {
        return extractBestParse(0).toString();
    }

    public class ConstrainedChartCell extends ParallelArrayChartCell {

        public final int cellIndex;

        public ConstrainedChartCell(final int start, final int end) {
            super(start, end);
            this.cellIndex = cellIndex(start, end);
        }

        @Override
        public float getInside(final int nonTerminal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProbability) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateInside(final ChartEdge edge) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChartEdge getBestEdge(final int nonTerminal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getNumNTs() {
            return 0;
        }

        /**
         * Warning: Not truly thread-safe, since it doesn't validate that the two cells belong to the same chart.
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof ConstrainedChartCell)) {
                return false;
            }

            final ConstrainedChartCell constrainedChartCell = (ConstrainedChartCell) o;
            return (constrainedChartCell.start == start && constrainedChartCell.end == end);
        }

        @Override
        public String toString() {
            return extractBestParse(0).toString();
        }
    }
}
