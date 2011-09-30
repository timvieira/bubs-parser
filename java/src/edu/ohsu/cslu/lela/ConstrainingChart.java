package edu.ohsu.cslu.lela;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;

/**
 * Represents a parse chart populated with a constraining gold parse tree.
 * 
 * @author Aaron Dunlop
 */
public class ConstrainingChart extends PackedArrayChart {

    short[][] openCells;

    /** The length of the longest unary chain (i.e., the binary parent + any unary parents) */
    private final int maxUnaryChainLength;

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

        this.maxUnaryChainLength = goldTree.maxUnaryChainLength() + 1;
        this.beamWidth = this.lexicalRowBeamWidth = maxUnaryChainLength;

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
            final int cellIndex = cellIndex(start, end);

            // Find the index of this non-terminal in the main chart array.
            // Unary children are positioned _after_ parents
            final int cellOffset = cellOffset(start, end);
            final int i = cellOffset + node.unaryChainHeight();

            nonTerminalIndices[i] = parent;

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
                midpoints[cellIndex] = (short) (start + node.leftChild().leaves());
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

        // Calculate all cell offsets
        for (int st = 0; st < size; st++) {
            for (int end = st + 1; end <= size; end++) {
                final int cellIndex = cellIndex(st, end);
                cellOffsets[cellIndex] = cellOffset(st, end);
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

        final int cellOffset = cellOffset(start, end);
        int entryIndex = cellOffset + maxUnaryChainLength - 1;
        // Find the first populated entry
        while (nonTerminalIndices[entryIndex] < 0) {
            entryIndex--;
        }

        final BinaryTree<String> tree = new BinaryTree<String>(
                grammar.nonTermSet.getSymbol(nonTerminalIndices[entryIndex]));
        BinaryTree<String> subtree = tree;
        // Add unary productions and binary parent
        while (entryIndex > cellOffset) {
            subtree = subtree.addChild(grammar.nonTermSet.getSymbol(nonTerminalIndices[--entryIndex]));
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
     * For unit testing
     * 
     * @return maximum unary chain length
     */
    int maxUnaryChainLength() {
        return maxUnaryChainLength;
    }
}
