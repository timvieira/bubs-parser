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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ShortMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;

/**
 * Represents a parse chart populated with a constraining gold parse tree.
 * 
 * @author Aaron Dunlop
 */
public class ConstrainingChart extends PackedArrayChart {

    public int[] tokens;

    short[][] openCells;

    /** Indices of parent cells, indexed by child cell cellIndex. Used for outside-probability computation. */
    short[] parentCellIndices;

    /** Indices of sibling cells, indexed by cellIndex. Used for outside-probability computation. */
    short[] siblingCellIndices;

    /** Length of unary chain for each cell. 1 <= unaryChainLength <= maxUnaryChainLength */
    public final byte[] unaryChainLength;

    /**
     * The length of the longest unary chain (i.e., the binary parent + any unary parents) 1 <= maxUnaryChainLength <= n
     */
    protected int maxUnaryChainLength;

    /**
     * Populates a chart based on a gold tree, with one entry per cell (+ unary productions, if any). This chart can
     * then be used to constrain parses with a split grammar.
     * 
     * @param goldTree
     * @param baseGrammar
     */
    public ConstrainingChart(final BinaryTree<String> goldTree, final SparseMatrixGrammar baseGrammar) {

        super(goldTree.leaves(), ConstrainedChart.chartArraySize(goldTree.leaves(), goldTree.maxUnaryChainLength()),
                baseGrammar);

        this.unaryChainLength = new byte[size * (size + 1) / 2];
        reset(goldTree, baseGrammar);
    }

    public void reset(final BinaryTree<String> goldTree, final SparseMatrixGrammar baseGrammar) {

        this.size = goldTree.leaves();
        this.parentCellIndices = new short[maxCells];
        Arrays.fill(parentCellIndices, (short) -1);
        this.siblingCellIndices = new short[maxCells];
        Arrays.fill(siblingCellIndices, (short) -1);

        Arrays.fill(nonTerminalIndices, Short.MIN_VALUE);
        Arrays.fill(unaryChainLength, (byte) 0);

        this.maxUnaryChainLength = goldTree.maxUnaryChainLength() + 1;
        this.beamWidth = this.lexicalRowBeamWidth = 1;
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
            final short parent = (short) baseGrammar.nonTermSet.getInt(node.label());
            final short cellIndex = (short) cellIndex(start, end);
            final int unaryChainHeight = node.unaryChainHeight();
            if (unaryChainLength[cellIndex] == 0) {
                unaryChainLength[cellIndex] = (byte) (unaryChainHeight + 1);
            }
            numNonTerminals[cellIndex] = unaryChainLength[cellIndex];

            // Find the index of this non-terminal in the main chart array.
            // Unary children are positioned _after_ parents
            final int cellOffset = cellOffset(start, end);
            final int i = cellOffset + unaryChainLength[cellIndex] - unaryChainHeight - 1;

            nonTerminalIndices[i] = parent;
            insideProbabilities[i] = 0;

            if (node.rightChild() == null) {
                if (node.leftChild().isLeaf()) {
                    // Lexical production
                    midpoints[cellIndex] = (short) (start + 1);
                    final int child = baseGrammar.lexSet.getIndex(node.leftChild().label());
                    packedChildren[i] = baseGrammar.packingFunction.packLexical(child);
                    tokenList.add(child);
                } else {
                    // Unary production
                    final short child = (short) baseGrammar.nonTermSet.getIndex(node.leftChild().label());
                    packedChildren[i] = baseGrammar.packingFunction.packUnary(child);
                }
            } else {
                // Binary production
                final short leftChild = (short) baseGrammar.nonTermSet.getIndex(node.leftChild().label());
                final short rightChild = (short) baseGrammar.nonTermSet.getIndex(node.rightChild().label());
                packedChildren[i] = baseGrammar.packingFunction.pack(leftChild, rightChild);
                final short midpoint = (short) (start + node.leftChild().leaves());
                midpoints[cellIndex] = midpoint;

                final short leftChildCellIndex = (short) cellIndex(start, midpoint);
                final short rightChildCellIndex = (short) cellIndex(midpoint, end);
                parentCellIndices[leftChildCellIndex] = cellIndex;
                siblingCellIndices[leftChildCellIndex] = rightChildCellIndex;

                parentCellIndices[rightChildCellIndex] = cellIndex;
                siblingCellIndices[rightChildCellIndex] = leftChildCellIndex;
            }
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
        calculateCellOffsets();
    }

    /**
     * Used by {@link ConstrainedChart} constructor
     */
    ConstrainingChart(final ConstrainingChart constrainingChart, final int chartArraySize,
            final SparseMatrixGrammar sparseMatrixGrammar) {

        super(constrainingChart.size(), chartArraySize, sparseMatrixGrammar);
        this.beamWidth = 1;
        this.unaryChainLength = new byte[constrainingChart.unaryChainLength.length];
        calculateCellOffsets();
    }

    /**
     * Construct a {@link ConstrainingChart} and populate it from a {@link ConstrainedChart} (e.g., a chart populated by
     * a constrained parse). Populates each cell in the new chart with the highest-posterior-probability entry from the
     * {@link ConstrainedChart}.
     * 
     * @param constrainedChart
     */
    protected ConstrainingChart(final ConstrainedChart constrainedChart) {
        this(constrainedChart, (SparseMatrixGrammar) constrainedChart.grammar, false);
    }

    /**
     * Construct a {@link ConstrainingChart} and populate it from a {@link ConstrainedChart} (e.g., a chart populated by
     * a constrained parse). Populates the new chart with either 1) the highest-posterior-probability entry from the
     * each cell of the {@link ConstrainedChart} or 2) the 1-best Viterbi parse from the {@link ConstrainedChart}.
     * 
     * @param constrainedChart
     * @param grammar
     * @param viterbi Populate the new chart with the Viterbi 1-best tree from the {@link ConstrainedChart}
     */
    protected ConstrainingChart(final ConstrainedChart constrainedChart, final SparseMatrixGrammar grammar,
            final boolean viterbi) {

        super(constrainedChart.size(), constrainedChart.chartArraySize / 2, grammar);

        Short2ShortMap parent2IndexMap = ((SplitVocabulary) grammar.nonTermSet).parent2IndexMap;
        if (parent2IndexMap == null) {
            // Construct an identity map to simplify chart population code below
            parent2IndexMap = new Short2ShortOpenHashMap();
            for (short i = 0; i < grammar.nonTermSet.size(); i++) {
                parent2IndexMap.put(i, i);
            }
        }

        this.unaryChainLength = constrainedChart.unaryChainLength;
        this.beamWidth = this.lexicalRowBeamWidth = 1;
        this.maxUnaryChainLength = constrainedChart.maxUnaryChainLength;
        this.openCells = constrainedChart.openCells;
        this.parentCellIndices = constrainedChart.parentCellIndices;
        this.siblingCellIndices = constrainedChart.siblingCellIndices;
        System.arraycopy(constrainedChart.midpoints, 0, this.midpoints, 0, constrainedChart.midpoints.length);

        calculateCellOffsets();

        if (viterbi) {
            populateViterbiParse(constrainedChart, parent2IndexMap, 0, constrainedChart.size(), 0);
            return;
        }

        final PackingFunction packingFunction = sparseMatrixGrammar.packingFunction;

        // Populate each cell with the highest posterior probability entry from the analogous cell
        for (final short[] startAndEnd : openCells) {
            final int cellIndex = cellIndex(startAndEnd[0], startAndEnd[1]);
            final int constrainedChartBaseOffset = constrainedChart.offset(cellIndex);
            final int baseOffset = offset(cellIndex);

            for (int unaryChainHeight = unaryChainLength[cellIndex] - 1; unaryChainHeight >= 0; unaryChainHeight--) {
                final int constrainedChartOffset = constrainedChartBaseOffset + (unaryChainHeight << 1);
                final int offset = baseOffset + unaryChainHeight;

                final float entry0Probability = constrainedChart.insideProbabilities[constrainedChartOffset]
                        + constrainedChart.outsideProbabilities[constrainedChartOffset];
                final float entry1Probability = constrainedChart.insideProbabilities[constrainedChartOffset + 1]
                        + constrainedChart.outsideProbabilities[constrainedChartOffset + 1];

                if (entry1Probability == Float.NEGATIVE_INFINITY || entry0Probability >= entry1Probability) {
                    nonTerminalIndices[offset] = parent2IndexMap
                            .get(constrainedChart.nonTerminalIndices[constrainedChartOffset]);
                    insideProbabilities[offset] = entry0Probability;
                } else if (entry1Probability > Float.NEGATIVE_INFINITY) {
                    nonTerminalIndices[offset] = parent2IndexMap
                            .get(constrainedChart.nonTerminalIndices[constrainedChartOffset + 1]);
                    insideProbabilities[offset] = entry1Probability;
                } else {
                    continue;
                }

                if (unaryChainHeight == unaryChainLength[cellIndex] - 1) {
                    // Bottom Entry
                    if (startAndEnd[1] - startAndEnd[0] == 1) {
                        // Lexical parent
                        final int lexicalEntryOffset = constrainedChart.nonTerminalIndices[constrainedChartOffset] >= 0 ? constrainedChartOffset
                                : constrainedChartOffset + 1;
                        final int lexicalEntry = constrainedChart.sparseMatrixGrammar.packingFunction
                                .unpackLeftChild(constrainedChart.packedChildren[lexicalEntryOffset]);
                        packedChildren[offset] = packingFunction.packLexical(lexicalEntry);
                    } else {
                        // Binary parent
                        final short midpoint = midpoints[cellIndex];
                        final short leftChild = nonTerminalIndices[cellOffset(startAndEnd[0], midpoint)];
                        final short rightChild = nonTerminalIndices[cellOffset(midpoint, startAndEnd[1])];
                        packedChildren[offset] = packingFunction.pack(leftChild, rightChild);
                    }
                } else {
                    // Unary parent
                    packedChildren[offset] = packingFunction.packUnary(nonTerminalIndices[offset + 1]);
                }
            }
        }
    }

    private void populateViterbiParse(final ConstrainedChart constrainedChart, final Short2ShortMap parent2IndexMap,
            final int start, final int end, int constrainedParent) {

        final PackingFunction pf = sparseMatrixGrammar.packingFunction;
        final PackingFunction constrainedPf = constrainedChart.sparseMatrixGrammar.packingFunction;

        final int cellIndex = cellIndex(start, end);
        final int constrainedCellOffset = constrainedChart.cellOffset(start, end);

        // Work downward through unary productions
        for (int constrainedEntry0Offset = constrainedCellOffset; constrainedEntry0Offset < constrainedCellOffset
                + unaryChainLength[cellIndex] * 2 - 2; constrainedEntry0Offset += 2) {

            final int constrainedEntryOffset = (constrainedChart.nonTerminalIndices[constrainedEntry0Offset] == constrainedParent) ? constrainedEntry0Offset
                    : constrainedEntry0Offset + 1;

            final int offset = constrainedEntryOffset >> 1;
            nonTerminalIndices[offset] = parent2IndexMap.get((short) constrainedParent);
            insideProbabilities[offset] = constrainedChart.insideProbabilities[constrainedEntryOffset];

            final int constrainedChild = constrainedPf
                    .unpackLeftChild(constrainedChart.packedChildren[constrainedEntryOffset]);
            packedChildren[offset] = pf.packUnary(parent2IndexMap.get((short) constrainedChild));
            constrainedParent = constrainedChild;
        }

        final int topEntry0Offset = constrainedCellOffset + unaryChainLength[cellIndex] * 2 - 2;
        final int constrainedEntryOffset = (constrainedChart.nonTerminalIndices[topEntry0Offset] == constrainedParent) ? topEntry0Offset
                : topEntry0Offset + 1;
        final int offset = constrainedEntryOffset >> 1;
        nonTerminalIndices[offset] = parent2IndexMap.get((short) constrainedParent);
        insideProbabilities[offset] = constrainedChart.insideProbabilities[constrainedEntryOffset];

        //
        // Add the binary or lexical parent
        //
        if (constrainedChart.packedChildren[constrainedEntryOffset] < 0) {
            // Lexical production

            final int lexicalEntry = constrainedPf
                    .unpackLeftChild(constrainedChart.packedChildren[constrainedEntryOffset]);
            packedChildren[constrainedEntryOffset >> 1] = pf.packLexical(lexicalEntry);

        } else {
            // Binary production
            final short edgeMidpoint = midpoints[cellIndex(start, end)];
            populateViterbiParse(constrainedChart, parent2IndexMap, start, edgeMidpoint,
                    constrainedPf.unpackLeftChild(constrainedChart.packedChildren[constrainedEntryOffset]));
            populateViterbiParse(constrainedChart, parent2IndexMap, edgeMidpoint, end,
                    constrainedPf.unpackRightChild(constrainedChart.packedChildren[constrainedEntryOffset]));
        }
    }

    /**
     * Returns the offset of the specified cell in the parallel chart arrays (note that this computation must agree with
     * that of {@link #cellIndex(int, int)}
     * 
     * @param start
     * @param end
     * @return the offset of the specified cell in the parallel chart arrays
     */
    @Override
    protected final int cellOffset(final int start, final int end) {

        if (start < 0 || start > size) {
            throw new IllegalArgumentException("Illegal start: " + start);
        }

        if (end <= start || end > size) {
            throw new IllegalArgumentException("Illegal end: " + end);
        }

        return cellIndex(start, end) * beamWidth * maxUnaryChainLength;
    }

    private void calculateCellOffsets() {
        // Calculate all cell offsets
        for (short start = 0; start < size; start++) {
            for (short end = (short) (start + 1); end <= size; end++) {
                cellOffsets[cellIndex(start, end)] = cellOffset(start, end);
            }
        }
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {

        final int offset = cellOffsets[cellIndex(start, end)];
        for (int i = offset; i < offset + (beamWidth * maxUnaryChainLength); i++) {
            if (nonTerminalIndices[i] == nonTerminal) {
                return 0;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public BinaryTree<String> extractBestParse(final int start, final int end, final int parent) {
        return extractInsideParse(start, end);
    }

    public BinaryTree<String> extractInsideParse(final int start, final int end) {

        final int cellIndex = cellIndex(start, end);
        final int cellOffset = cellOffset(start, end);
        int entryIndex = cellOffset;

        final BinaryTree<String> tree = new BinaryTree<String>(
                grammar.nonTermSet.getSymbol(nonTerminalIndices[cellOffset]));
        BinaryTree<String> subtree = tree;

        // Add unary productions and binary parent
        while (entryIndex < cellOffset + unaryChainLength[cellIndex] - 1) {
            subtree = subtree.addChild(grammar.nonTermSet.getSymbol(nonTerminalIndices[++entryIndex]));
        }

        if (packedChildren[entryIndex] < 0) {
            // Lexical production
            final String sChild = grammar.lexSet.getSymbol(sparseMatrixGrammar.packingFunction().unpackLeftChild(
                    packedChildren[entryIndex]));
            subtree.addChild(new BinaryTree<String>(sChild));
        } else {
            // Binary production
            final short edgeMidpoint = midpoints[cellIndex(start, end)];
            subtree.addChild(extractInsideParse(start, edgeMidpoint));
            subtree.addChild(extractInsideParse(edgeMidpoint, end));
        }
        return tree;
    }

    /**
     * @param cellIndex
     * @return The length of the unary chain in the specified cell (1 <= length <= maxUnaryChainLength).
     */
    public int unaryChainLength(final int cellIndex) {
        return unaryChainLength[cellIndex];
    }

    /**
     * @param start
     * @param end
     * @return The length of the unary chain in the specified cell (1 <= length <= maxUnaryChainLength).
     */
    public int unaryChainLength(final int start, final int end) {
        return unaryChainLength(cellIndex(start, end));
    }

    /**
     * @return The length of the longest unary chain (i.e., the binary parent + any unary parents) 1 <=
     *         maxUnaryChainLength <= n
     */
    public int maxUnaryChainLength() {
        return maxUnaryChainLength;
    }

    @Override
    public String toString() {
        return toString(true, false);
    }

    @Override
    public String toString(final boolean formatFractions, final boolean includeEmptyCells) {
        final StringBuilder sb = new StringBuilder(1024);

        for (int span = 1; span <= size; span++) {
            for (int start = 0; start <= size - span; start++) {
                final int end = start + span;
                final int cellIndex = cellIndex(start, end);
                final int offset = offset(cellIndex);

                // Skip empty cells
                if (nonTerminalIndices[offset] < 0 && !includeEmptyCells) {
                    continue;
                }

                sb.append("ConstrainingChartCell " + cellIndex + " [" + start + "][" + end + "]("
                        + midpoints[cellIndex] + ")\n");

                // Format unary parents first, followed by the two bottom entries
                final int bottomEntryOffset = offset + (unaryChainLength(cellIndex) - 1);
                for (int offset0 = offset; offset0 < bottomEntryOffset; offset0++) {
                    sb.append(formatCellEntry(nonTerminalIndices[offset0], packedChildren[offset0], true,
                            insideProbabilities[offset0], formatFractions));
                }
                sb.append(formatCellEntry(nonTerminalIndices[bottomEntryOffset], packedChildren[bottomEntryOffset],
                        false, insideProbabilities[bottomEntryOffset], formatFractions));
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    protected String formatCellEntry(final int nonterminal, final int childProductions, final boolean unary,
            final float insideProbability, final boolean formatFractions) {

        final int leftChild = sparseMatrixGrammar.packingFunction().unpackLeftChild(childProductions);
        final int rightChild = sparseMatrixGrammar.packingFunction().unpackRightChild(childProductions);

        if (rightChild == Production.LEXICAL_PRODUCTION) {
            // Lexical Production
            return String.format("%s -> %s (%.5f)\n", sparseMatrixGrammar.mapNonterminal(nonterminal),
                    sparseMatrixGrammar.mapLexicalEntry(leftChild), insideProbability);
        } else if (unary) {
            // Unary Production
            return String.format("%s -> unary (%.5f)\n", sparseMatrixGrammar.mapNonterminal(nonterminal),
                    insideProbability);
        } else {
            return String.format("%s -> binary (%.5f)\n", sparseMatrixGrammar.mapNonterminal(nonterminal),
                    insideProbability);
        }
    }

    @Override
    public PackedArrayChartCell getCell(final int start, final int end) {
        final PackedArrayChartCell cell = new PackedArrayChartCell(start, end);
        cell.tmpCell = new TemporaryChartCell(grammar, false);
        final int cellIndex = cellIndex(start, end);
        for (int i = 0; i < beamWidth * maxUnaryChainLength; i++) {
            final int entryOffset = offset(cellIndex) + i;
            final short nt = nonTerminalIndices[entryOffset];
            if (nt >= 0) {
                cell.tmpCell.insideProbabilities[nt] = insideProbabilities[entryOffset];
                cell.tmpCell.midpoints[nt] = (i == unaryChainLength[cellIndex] - 1) ? midpoints[cellIndex]
                        : (short) end;
                cell.tmpCell.packedChildren[nt] = packedChildren[entryOffset];
            }
        }
        return cell;
    }
}
