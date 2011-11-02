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

    short[][] openCells;

    /** Indices of parent cells, indexed by child cell cellIndex. Used for outside-probability computation. */
    short[] parentCellIndices;

    /** Indices of sibling cells, indexed by cellIndex. Used for outside-probability computation. */
    short[] siblingCellIndices;

    /** Length of unary chain for each cell. 1 <= unaryChainLength <= maxUnaryChainLength */
    protected final byte[] unaryChainLength;

    /**
     * The length of the longest unary chain (i.e., the binary parent + any unary parents) 1 <= maxUnaryChainLength <= n
     */
    protected int maxUnaryChainLength;

    /**
     * Populates a chart based on a gold tree, with one entry per cell (+ unary productions, if any). This chart can
     * then be used to constrain parses with a split grammar.
     * 
     * @param goldTree
     * @param sparseMatrixGrammar
     */
    public ConstrainingChart(final BinaryTree<String> goldTree, final SparseMatrixGrammar sparseMatrixGrammar) {

        super(goldTree.leaves(), ConstrainedChart.chartArraySize(goldTree.leaves(), goldTree.maxUnaryChainLength()),
                sparseMatrixGrammar);

        this.parentCellIndices = new short[cells];
        Arrays.fill(parentCellIndices, (short) -1);
        this.siblingCellIndices = new short[cells];
        Arrays.fill(siblingCellIndices, (short) -1);

        this.maxUnaryChainLength = goldTree.maxUnaryChainLength() + 1;
        this.beamWidth = this.lexicalRowBeamWidth = maxUnaryChainLength;
        this.unaryChainLength = new byte[size * (size + 1) / 2];
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
            final short parent = (short) sparseMatrixGrammar.nonTermSet.getInt(node.label());
            final short cellIndex = (short) cellIndex(start, end);
            final int unaryChainHeight = node.unaryChainHeight();
            if (unaryChainLength[cellIndex] == 0) {
                unaryChainLength[cellIndex] = (byte) (unaryChainHeight + 1);
            }

            // Find the index of this non-terminal in the main chart array.
            // Unary children are positioned _after_ parents
            final int cellOffset = cellOffset(start, end);
            final int i = cellOffset + unaryChainLength[cellIndex] - unaryChainHeight - 1;

            nonTerminalIndices[i] = parent;
            insideProbabilities[i] = 0;

            if (node.rightChild() == null) {
                if (node.leftChild().isLeaf()) {
                    // Lexical production
                    midpoints[cellIndex] = 0;
                    final int child = sparseMatrixGrammar.lexSet.getIndex(node.leftChild().label());
                    packedChildren[i] = sparseMatrixGrammar.packingFunction.packLexical(child);

                    tokenList.add(child);
                } else {
                    // Unary production
                    final short child = (short) sparseMatrixGrammar.nonTermSet.getIndex(node.leftChild().label());
                    packedChildren[i] = sparseMatrixGrammar.packingFunction.packUnary(child);
                }
            } else {
                // Binary production
                final short leftChild = (short) sparseMatrixGrammar.nonTermSet.getIndex(node.leftChild().label());
                final short rightChild = (short) sparseMatrixGrammar.nonTermSet.getIndex(node.rightChild().label());
                packedChildren[i] = sparseMatrixGrammar.packingFunction.pack(leftChild, rightChild);
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
        calculateCellOffsets();

        this.beamWidth = constrainingChart.beamWidth;
        this.unaryChainLength = new byte[constrainingChart.unaryChainLength.length];
    }

    /**
     * Construct a {@link ConstrainingChart} and populate it from a {@link ConstrainedChart} (e.g., a chart populated by
     * a constrained parse). Populates each cell in the new chart with the highest-posterior-probability entry from the
     * {@link ConstrainedChart}.
     * 
     * @param constrainedChart
     */
    protected ConstrainingChart(final ConstrainedChart constrainedChart) {
        this(constrainedChart, (SparseMatrixGrammar) constrainedChart.grammar);
    }

    /**
     * Construct a {@link ConstrainingChart} and populate it from a {@link ConstrainedChart} (e.g., a chart populated by
     * a constrained parse). Populates each cell in the new chart with the highest-posterior-probability entry from the
     * {@link ConstrainedChart}.
     * 
     * @param constrainedChart
     * @param parent2IndexMap
     */
    protected ConstrainingChart(final ConstrainedChart constrainedChart, final SparseMatrixGrammar grammar) {

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
        this.beamWidth = this.lexicalRowBeamWidth = constrainedChart.maxUnaryChainLength;
        this.maxUnaryChainLength = constrainedChart.maxUnaryChainLength;
        this.openCells = constrainedChart.openCells;
        this.parentCellIndices = constrainedChart.parentCellIndices;
        this.siblingCellIndices = constrainedChart.siblingCellIndices;
        System.arraycopy(constrainedChart.midpoints, 0, this.midpoints, 0, constrainedChart.midpoints.length);

        calculateCellOffsets();

        final PackingFunction packingFunction = sparseMatrixGrammar.packingFunction;

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
                    insideProbabilities[offset] = 0;
                } else if (entry1Probability > Float.NEGATIVE_INFINITY) {
                    nonTerminalIndices[offset] = parent2IndexMap
                            .get(constrainedChart.nonTerminalIndices[constrainedChartOffset + 1]);
                    insideProbabilities[offset] = 0;
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

    private void calculateCellOffsets() {
        // Calculate all cell offsets
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end <= size; end++) {
                final int cellIndex = cellIndex(start, end);
                cellOffsets[cellIndex] = cellOffset(start, end);
            }
        }
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {

        final int offset = cellOffsets[cellIndex(start, end)];
        for (int i = offset; i < offset + beamWidth; i++) {
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
            final String sChild = grammar.lexSet.getSymbol(sparseMatrixGrammar.cartesianProductFunction()
                    .unpackLeftChild(packedChildren[entryIndex]));
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
    int unaryChainLength(final int cellIndex) {
        return unaryChainLength[cellIndex];
    }

    /**
     * @param start
     * @param end
     * @return The length of the unary chain in the specified cell (1 <= length <= maxUnaryChainLength).
     */
    int unaryChainLength(final int start, final int end) {
        return unaryChainLength(cellIndex(start, end));
    }

    /**
     * For unit testing
     * 
     * @return maximum unary chain length
     */
    int maxUnaryChainLength() {
        return maxUnaryChainLength;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    @Override
    public String toString(final boolean formatFractions) {
        final StringBuilder sb = new StringBuilder(1024);

        final int increment = (this instanceof ConstrainedChart) ? 2 : 1;
        for (int span = 1; span <= size; span++) {
            for (int start = 0; start <= size - span; start++) {
                final int end = start + span;
                final int cellIndex = cellIndex(start, end);
                final int offset = offset(cellIndex);

                // Skip empty cells
                if (nonTerminalIndices[offset] < 0) {
                    continue;
                }

                sb.append("ConstrainingChartCell[" + start + "][" + end + "]\n");

                // Format unary parents first, followed by the two bottom entries
                final int bottomEntryOffset = offset + (unaryChainLength(cellIndex) - 1) * increment;
                for (int offset0 = offset; offset0 < bottomEntryOffset; offset0 += increment) {
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

        final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(childProductions);
        final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(childProductions);

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
}
