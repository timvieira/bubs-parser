package edu.ohsu.cslu.ella;

import java.util.Arrays;
import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * Represents a parse chart constrained by a parent chart (e.g., for inside-outside parameter estimation constrained by
 * gold trees or (possibly) coarse-to-fine parsing).
 * 
 * @author Aaron Dunlop
 * @since Jun 2, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ConstrainedChart extends ParallelArrayChart {

    /**
     * Parallel array storing non-terminals (parallel to {@link ParallelArrayChart#insideProbabilities},
     * {@link ParallelArrayChart#packedChildren}, and {@link ParallelArrayChart#midpoints}. Entries for each cell begin
     * at indices from {@link #cellOffsets}.
     */
    public final short[] nonTerminalIndices;

    /**
     * The number of non-terminals populated in each cell. Indexed by cell index ({@link #cellIndex(int, int)} ).
     */
    private final int[] numNonTerminals;

    private final SplitVocabulary splitVocabulary;

    private final SymbolSet<String> lexicon;

    final short[][] openCells;

    // TODO Remove this large array and share a single temporary cell. Should avoid some object creation and GC
    // public final PackedArrayChartCell[][] temporaryCells;

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
     * @param tokens
     * @param sparseMatrixGrammar
     * @param maxSubstates
     * @param maxUnaryChainLength
     */
    protected ConstrainedChart(final int[] tokens, final SparseMatrixGrammar sparseMatrixGrammar,
            final int maxSubstates, final int maxUnaryChainLength) {
        super(tokens, sparseMatrixGrammar);
        this.beamWidth = lexicalRowBeamWidth = maxSubstates * (1 + maxUnaryChainLength);
        this.splitVocabulary = (SplitVocabulary) sparseMatrixGrammar.nonTermSet;
        this.numNonTerminals = new int[cells];
        this.nonTerminalIndices = new short[chartArraySize];
        Arrays.fill(nonTerminalIndices, (short) -1);
        this.lexicon = sparseMatrixGrammar.lexSet;
        this.openCells = new short[size][2];
    }

    public ConstrainedChart(final BinaryTree<String> goldTree, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(goldTree.leaves(), (goldTree.leaves() * (goldTree.leaves() + 1) / 2) * goldTree.maxUnaryChainLength()
                * ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits, sparseMatrixGrammar);
        this.splitVocabulary = (SplitVocabulary) sparseMatrixGrammar.nonTermSet;
        this.lexicon = sparseMatrixGrammar.lexSet;

        this.numNonTerminals = new int[cells];
        this.nonTerminalIndices = new short[chartArraySize];
        Arrays.fill(nonTerminalIndices, Short.MIN_VALUE);

        this.beamWidth = this.lexicalRowBeamWidth = ((SplitVocabulary) sparseMatrixGrammar.nonTermSet).maxSplits
                * goldTree.maxUnaryChainLength();

        int start = 0;
        for (final Iterator<BinaryTree<String>> iter = goldTree.preOrderIterator(); iter.hasNext();) {
            final BinaryTree<String> node = iter.next();

            if (node.isLeaf()) {
                // Increment the start index every time we process a leaf. The lexical entry was already populated (see
                // below)
                start++;
                continue;
            }

            final int end = start + node.leaves();
            final short parent = (short) splitVocabulary.getInt(node.label());
            // Find the index of this non-terminal in the main chart array.
            // Unary children are positioned _after_ parents
            final int i = cellOffset(start, end) + splitVocabulary.subcategoryIndices[parent]
                    + splitVocabulary.maxSplits * node.unaryChainDepth();

            nonTerminalIndices[i] = parent;
            insideProbabilities[i] = 0;

            if (node.rightChild() == null) {
                midpoints[i] = 0;

                if (node.leftChild().isLeaf()) {
                    // Lexical production
                    final int child = lexicon.getIndex(node.leftChild().label());
                    packedChildren[i] = sparseMatrixGrammar.cartesianProductFunction.packLexical(child);
                } else {
                    // Unary production
                    final short child = (short) splitVocabulary.getIndex(node.leftChild().label());
                    packedChildren[i] = sparseMatrixGrammar.cartesianProductFunction.packUnary(child);
                }
            } else {
                // Binary production
                midpoints[i] = (short) (start + node.leftChild().leaves());
                final short leftChild = (short) splitVocabulary.getIndex(node.leftChild().label());
                final short rightChild = (short) splitVocabulary.getIndex(node.rightChild().label());
                packedChildren[i] = sparseMatrixGrammar.cartesianProductFunction.pack(leftChild, rightChild);
            }
        }

        // Populate openCells with the start/end pairs of each populated cell. Used by {@link ConstrainedCellSelector}
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
    }

    @Override
    public void clear(final int sentenceLength) {
        this.size = sentenceLength;
        Arrays.fill(numNonTerminals, 0);

        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end <= size; end++) {
                final int cellIndex = cellIndex(start, end);
                cellOffsets[cellIndex] = cellOffset(start, end);
            }
        }
        // for (int i = 0; i < size; i++) {
        // Arrays.fill(temporaryCells[i], null);
        // }
    }

    @Override
    public ParseTree extractBestParse(final int start, final int end, final int parent) {
        final ParseTree subtree = new ParseTree(splitVocabulary.getSymbol(parent));
        final int i = cellOffset(start, end) + splitVocabulary.subcategoryIndices[parent];
        final int child = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(packedChildren[i]);
        subtree.children.add(extractBestParse(start, end, child, 1));
        return subtree;
    }

    public ParseTree extractBestParse(final int start, final int end, final int parent, final int unaryDepth) {
        // Find the index of the non-terminal in the chart storage
        final int i = cellOffset(start, end) + splitVocabulary.subcategoryIndices[parent] + unaryDepth
                * splitVocabulary.maxSplits;

        final int edgeChildren = packedChildren[i];
        final short edgeMidpoint = midpoints[i];

        final ParseTree subtree = new ParseTree(splitVocabulary.getSymbol(parent));
        final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(edgeChildren);
        final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(edgeChildren);

        if (rightChild == Production.UNARY_PRODUCTION) {
            subtree.children.add(extractBestParse(start, end, leftChild, unaryDepth + 1));

        } else if (rightChild == Production.LEXICAL_PRODUCTION) {
            subtree.addChild(new ParseTree(sparseMatrixGrammar.lexSet.getSymbol(leftChild)));

        } else {
            // binary production
            subtree.children.add(extractBestParse(start, edgeMidpoint, leftChild, 0));
            subtree.children.add(extractBestParse(edgeMidpoint, end, rightChild, 0));
        }
        return subtree;
    }

    public int[] numNonTerminals() {
        return numNonTerminals;
    }

    public void countRuleObservations(final MappedCountGrammar grammar) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int cellIndex = cellIndex(start, end);

        // We could compute the possible indices directly, but it's a very short range to linear search, and this method
        // should only be called in testing
        for (int i = cellIndex * beamWidth; i < (cellIndex + 1) * beamWidth; i++) {
            if (nonTerminalIndices[i] == nonTerminal) {
                return insideProbabilities[i];
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        throw new UnsupportedOperationException("Not supported by PackedArrayChart");
    }

    // TODO Is this ever called? Can we drop the entire ConstrainedChartCell class?
    @Override
    public ConstrainedChartCell getCell(final int start, final int end) {
        // if (temporaryCells[start][end] != null) {
        // return temporaryCells[start][end];
        // }

        return new ConstrainedChartCell(start, end);
    }

    public class ConstrainedChartCell extends ParallelArrayChartCell {

        /**
         * Temporary storage for manipulating cell entries. Indexed by parent non-terminal. Only allocated when the cell
         * is being modified
         */
        public int[] tmpPackedChildren;
        public float[] tmpInsideProbabilities;
        public short[] tmpMidpoints;

        public ConstrainedChartCell(final int start, final int end) {
            super(start, end);
        }

        public void allocateTemporaryStorage() {

            // Allocate storage
            if (tmpPackedChildren == null) {
                final int arraySize = sparseMatrixGrammar.numNonTerms();

                this.tmpPackedChildren = new int[arraySize];
                this.tmpInsideProbabilities = new float[arraySize];
                Arrays.fill(tmpInsideProbabilities, Float.NEGATIVE_INFINITY);
                this.tmpMidpoints = new short[arraySize];

                // Copy from main chart array to temporary parallel array
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                    final int nonTerminal = nonTerminalIndices[i];
                    tmpPackedChildren[nonTerminal] = packedChildren[i];
                    tmpInsideProbabilities[nonTerminal] = insideProbabilities[i];
                    tmpMidpoints[nonTerminal] = midpoints[i];
                }
            }
        }

        public void clearTemporaryStorage() {
            tmpPackedChildren = null;
            tmpInsideProbabilities = null;
            tmpMidpoints = null;
        }

        /**
         * Copy and pack entries from temporary array into the main chart array
         */
        @Override
        public void finalizeCell() {

            if (tmpPackedChildren == null) {
                return;
            }
            finalizeCell(tmpPackedChildren, tmpInsideProbabilities, tmpMidpoints);
        }

        public void finalizeCell(final int[] newPackedChildren, final float[] newInsideProbabilities,
                final short[] newMidpoints) {
            // Copy all populated entries from temporary storage
            int nonTerminalOffset = offset;

            for (short nonTerminal = 0; nonTerminal < newInsideProbabilities.length; nonTerminal++) {

                if (newInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {

                    nonTerminalIndices[nonTerminalOffset] = nonTerminal;
                    insideProbabilities[nonTerminalOffset] = newInsideProbabilities[nonTerminal];
                    packedChildren[nonTerminalOffset] = newPackedChildren[nonTerminal];
                    midpoints[nonTerminalOffset] = newMidpoints[nonTerminal];
                    nonTerminalOffset++;
                }
            }

            numNonTerminals[cellIndex] = nonTerminalOffset - offset;
            clearTemporaryStorage();
        }

        @Override
        public float getInside(final int nonTerminal) {
            if (tmpPackedChildren != null) {
                return tmpInsideProbabilities[nonTerminal];
            }

            final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                    (short) nonTerminal);
            if (index < 0) {
                return Float.NEGATIVE_INFINITY;
            }
            return insideProbabilities[index];
        }

        @Override
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProbability) {
            // Allow update of cells created without temporary storage, even though this will be inefficient;
            // it should be rare, and is at least useful for unit testing.
            allocateTemporaryStorage();

            final int parent = p.parent;
            numEdgesConsidered++;

            if (insideProbability > tmpInsideProbabilities[p.parent]) {
                if (p.isBinaryProd()) {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().pack(
                            (short) p.leftChild, (short) p.rightChild);
                } else if (p.isLexProd()) {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().packLexical(p.leftChild);
                } else {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().packUnary(
                            (short) p.leftChild);
                }
                tmpInsideProbabilities[parent] = insideProbability;

                // Midpoint == end for unary productions
                tmpMidpoints[parent] = leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public void updateInside(final ChartEdge edge) {

            // Allow update of cells created without temporary storage, even though this will be inefficient;
            // it should be rare, and is at least useful for unit testing.
            allocateTemporaryStorage();

            final int parent = edge.prod.parent;
            numEdgesConsidered++;

            if (edge.inside() > tmpInsideProbabilities[parent]) {

                if (edge.prod.isBinaryProd()) {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().pack(
                            (short) edge.prod.leftChild, (short) edge.prod.rightChild);
                } else if (edge.prod.isLexProd()) {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().packLexical(
                            edge.prod.leftChild);
                } else {
                    tmpPackedChildren[parent] = sparseMatrixGrammar.cartesianProductFunction().packUnary(
                            (short) edge.prod.leftChild);
                }
                tmpInsideProbabilities[parent] = edge.inside();

                // Midpoint == end for unary productions
                tmpMidpoints[parent] = edge.leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public ChartEdge getBestEdge(final int nonTerminal) {
            int edgeChildren;
            short edgeMidpoint;

            if (tmpPackedChildren != null) {
                if (tmpInsideProbabilities[nonTerminal] == Float.NEGATIVE_INFINITY) {
                    return null;
                }

                edgeChildren = tmpPackedChildren[nonTerminal];
                edgeMidpoint = tmpMidpoints[nonTerminal];

            } else {
                final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                        (short) nonTerminal);
                if (index < 0) {
                    return null;
                }
                edgeChildren = packedChildren[index];
                edgeMidpoint = midpoints[index];
            }

            final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(edgeChildren);
            final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(edgeChildren);
            final ConstrainedChartCell leftChildCell = getCell(start(), edgeMidpoint);
            final ConstrainedChartCell rightChildCell = edgeMidpoint < end ? (ConstrainedChartCell) getCell(
                    edgeMidpoint, end) : null;

            Production p;
            if (rightChild == Production.LEXICAL_PRODUCTION) {
                final float probability = sparseMatrixGrammar.lexicalLogProbability(nonTerminal, leftChild);
                p = new Production(nonTerminal, leftChild, probability, true, sparseMatrixGrammar);

            } else if (rightChild == Production.UNARY_PRODUCTION) {
                final float probability = sparseMatrixGrammar.unaryLogProbability(nonTerminal, leftChild);
                p = new Production(nonTerminal, leftChild, probability, false, sparseMatrixGrammar);

            } else {
                final float probability = sparseMatrixGrammar.binaryLogProbability(nonTerminal, edgeChildren);
                p = new Production(nonTerminal, leftChild, rightChild, probability, sparseMatrixGrammar);
            }
            return new ChartEdge(p, leftChildCell, rightChildCell);
        }

        @Override
        public int getNumNTs() {
            if (tmpPackedChildren != null) {
                int numNTs = 0;
                for (int i = 0; i < tmpInsideProbabilities.length; i++) {
                    if (tmpInsideProbabilities[i] != Float.NEGATIVE_INFINITY) {
                        numNTs++;
                    }
                }
                return numNTs;
            }

            return numNonTerminals[cellIndex(start, end)];
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
            final StringBuilder sb = new StringBuilder(256);

            sb.append("ConstrainedChartCell[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + sparseMatrixGrammar.numNonTerms() + ") edges\n");

            if (tmpPackedChildren == null) {
                // Format entries from the main chart array
                for (int index = offset; index < offset + numNonTerminals[cellIndex]; index++) {
                    final int childProductions = packedChildren[index];
                    final float insideProbability = insideProbabilities[index];
                    final int midpoint = midpoints[index];

                    final int nonTerminal = nonTerminalIndices[index];

                    sb.append(formatCellEntry(nonTerminal, childProductions, insideProbability, midpoint));
                }
            } else {

                // Format entries from temporary cell storage
                for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {

                    if (tmpInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                        final int childProductions = tmpPackedChildren[nonTerminal];
                        final float insideProbability = tmpInsideProbabilities[nonTerminal];
                        final int midpoint = tmpMidpoints[nonTerminal];

                        sb.append(formatCellEntry(nonTerminal, childProductions, insideProbability, midpoint));
                    }
                }

            }
            return sb.toString();
        }
    }
}
