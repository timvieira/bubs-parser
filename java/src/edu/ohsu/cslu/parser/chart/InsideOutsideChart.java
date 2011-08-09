package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Chart structure including outside probabilities and various decoding methods dependent on posterior probabilities.
 */
public class InsideOutsideChart extends PackedArrayChart {

    public final float[] outsideProbabilities;

    /**
     * Parallel array of max-c scores (see Goodman, 1996). Stored as instance variables instead of locals purely for
     * debugging and visualization via {@link #toString()}.
     */
    private final short[] maxcEntries;
    private final float[] maxcScores;
    private final short[] maxcMidpoints;
    private final short[] maxcUnaryChildren;

    public InsideOutsideChart(final int[] tokens, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(tokens, sparseMatrixGrammar);
        this.outsideProbabilities = new float[chartArraySize];
        Arrays.fill(outsideProbabilities, Float.NEGATIVE_INFINITY);

        final int maxcArraySize = tokens.length * ((tokens.length + 1) / 2);
        this.maxcEntries = new short[maxcArraySize];
        this.maxcScores = new float[maxcArraySize];
        this.maxcMidpoints = new short[maxcArraySize];
        this.maxcUnaryChildren = new short[maxcArraySize];
    }

    public void finalizeOutside(final float[] tmpOutsideProbabilities, final int cellIndex) {

        // Copy from temporary storage all entries which have non-0 inside and outside probabilities
        final int startIndex = offset(cellIndex);
        final int endIndex = startIndex + numNonTerminals[cellIndex];

        for (int i = startIndex; i < endIndex; i++) {

            final int nonTerminal = nonTerminalIndices[i];
            if (tmpOutsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                outsideProbabilities[i] = tmpOutsideProbabilities[nonTerminal];
            }
        }
    }

    /**
     * Computes 'maxc', per the algorithm in Figure 1 of Joshua Goodman, 1996, Parsing Algorithms and Metrics.
     */
    public void computeMaxc() {

        // Start symbol inside-probability (e)
        final int topCellIndex = cellIndex(0, size);
        final float startSymbolInsideProbability = insideProbabilities[entryIndex(offset(topCellIndex),
                numNonTerminals[topCellIndex], (short) sparseMatrixGrammar.startSymbol)];

        Arrays.fill(maxcEntries, Short.MIN_VALUE);
        Arrays.fill(maxcScores, Float.NEGATIVE_INFINITY);
        Arrays.fill(maxcMidpoints, Short.MIN_VALUE);
        Arrays.fill(maxcUnaryChildren, Short.MIN_VALUE);

        // Span-1 cells
        for (short start = 0; start < size; start++) {
            final int cellIndex = cellIndex(start, start + 1);
            final int offset = offset(cellIndex);

            // maxc = max(posterior probability)
            for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                final float c = insideProbabilities[i] + outsideProbabilities[i];
                if (c > maxcScores[cellIndex]) {
                    maxcEntries[cellIndex] = nonTerminalIndices[i];
                    maxcScores[cellIndex] = c;
                }
            }
        }

        // Span > 1 cells
        for (short span = 2; span <= size; span++) {
            for (short start = 0; start < size - span + 1; start++) {
                final int end = start + span;
                final int cellIndex = cellIndex(start, end);
                final int offset = offset(cellIndex);

                // maxg = max(posterior probability / e)
                float maxg = Float.NEGATIVE_INFINITY;
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {

                    final float g = insideProbabilities[i] + outsideProbabilities[i] - startSymbolInsideProbability;
                    if (g > maxg) {
                        maxcEntries[cellIndex] = nonTerminalIndices[i];
                        maxg = g;
                    }
                }

                if (maxg == Float.NEGATIVE_INFINITY) {
                    continue;
                }
                // Iterate over possible binary child cells, computing maxc
                final float bestSplit = Float.NEGATIVE_INFINITY;
                for (short midpoint = (short) (start + 1); midpoint < end; midpoint++) {

                    // maxc = max(posterior probability) + max(maxc()). Store observed midpoints for use when extracting
                    // the parse tree
                    final float split = maxcScores[cellIndex(start, midpoint)] + maxcScores[cellIndex(midpoint, end)];
                    if (split > bestSplit) {
                        maxcScores[cellIndex] = maxg + split;
                        maxcMidpoints[cellIndex] = midpoint;
                    }
                }

                // Addition to Goodman's algorithm: if the Viterbi best path to the highest-scoring non-terminal was
                // through a unary production, record the unary child as well. Note that this requires we store Viterbi
                // inside scores and backpointers as well as summed inside probabilities during the inside parsing pass.
                final int maxcEntryIndex = entryIndex(offset, numNonTerminals[cellIndex], maxcEntries[cellIndex]);
                if (midpoints[maxcEntryIndex] == end) {
                    maxcUnaryChildren[cellIndex] = (short) sparseMatrixGrammar.packingFunction
                            .unpackLeftChild(packedChildren[maxcEntryIndex]);
                }
            }
        }
    }

    /**
     * Extracts the max-recall parse (Goodman's 'Labeled Recall' algorithm), using the maxc values computed by
     * {@link #computeMaxc()}.
     * 
     * @param start
     * @param end
     * @return extracted binary tree
     */
    public BinaryTree<String> extractMaxRecallParse(final int start, final int end) {
        final int cellIndex = cellIndex(start, end);
        final int offset = offset(cellIndex);
        final int numNonTerms = numNonTerminals[cellIndex];

        // Find the non-terminal which maximizes Goodman's max-constituent metric
        short parent = maxcEntries[cellIndex];
        final BinaryTree<String> tree = new BinaryTree<String>(sparseMatrixGrammar.nonTermSet.getSymbol(parent));
        BinaryTree<String> subtree = tree;

        if (end - start == 1) {

            final PackingFunction pf = sparseMatrixGrammar.packingFunction;
            BinaryTree<String> unaryTree = subtree;

            // Find the index of the current parent in the chart storage and follow the unary productions down to the
            // lexical entry
            int i;
            for (i = entryIndex(offset, numNonTerms, parent); pf.unpackRightChild(packedChildren[i]) != Production.LEXICAL_PRODUCTION; i = entryIndex(
                    offset, numNonTerms, parent)) {
                parent = (short) pf.unpackLeftChild(packedChildren[i]);
                unaryTree = unaryTree
                        .addChild(new BinaryTree<String>(sparseMatrixGrammar.nonTermSet.getSymbol(parent)));
            }
            unaryTree.addChild(new BinaryTree<String>(sparseMatrixGrammar.lexSet.getSymbol(pf
                    .unpackLeftChild(packedChildren[i]))));
            return subtree;
        }

        final short edgeMidpoint = maxcMidpoints[cellIndex];

        if (maxcUnaryChildren[cellIndex] >= 0) {
            // Unary production - we currently only allow one level of unary in span > 1 cells.
            subtree = subtree.addChild(new BinaryTree<String>(sparseMatrixGrammar.nonTermSet
                    .getSymbol(maxcUnaryChildren[cellIndex])));
        }

        // Binary production
        subtree.addChild(extractMaxRecallParse(start, edgeMidpoint));
        subtree.addChild(extractMaxRecallParse(edgeMidpoint, end));

        return tree;
    }

    private int entryIndex(final int offset, final int numNonTerminals, final short parent) {
        return Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals, parent);
    }

    @Override
    public InsideOutsideChartCell getCell(final int start, final int end) {
        if (temporaryCells[start][end] != null) {
            return (InsideOutsideChartCell) temporaryCells[start][end];
        }
        return new InsideOutsideChartCell(start, end);
    }

    public class InsideOutsideChartCell extends PackedArrayChartCell {

        public float[] tmpOutsideProbabilities;

        public InsideOutsideChartCell(final int start, final int end) {
            super(start, end);
            temporaryCells[start][end] = this;
        }

        @Override
        public void allocateTemporaryStorage() {

            super.allocateTemporaryStorage();

            // Allocate outside-probability storage
            if (tmpOutsideProbabilities == null) {
                final int arraySize = sparseMatrixGrammar.numNonTerms();
                this.tmpOutsideProbabilities = new float[arraySize];
                Arrays.fill(tmpOutsideProbabilities, Float.NEGATIVE_INFINITY);

                // Copy from main chart array to temporary parallel array
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                    final int nonTerminal = nonTerminalIndices[i];
                    tmpOutsideProbabilities[nonTerminal] = outsideProbabilities[i];
                }
            }
        }

        @Override
        public String toString() {
            return toString(false);
        }

        @Override
        public String toString(final boolean formatFractions) {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("InsideOutsideChartCell[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + sparseMatrixGrammar.numNonTerms() + ") edges");

            if (maxcScores[cellIndex] > Float.NEGATIVE_INFINITY) {
                if (maxcUnaryChildren[cellIndex] == Short.MIN_VALUE) {
                    sb.append(String.format("  MaxC = %s (%.5f, %d)",
                            sparseMatrixGrammar.nonTermSet.getSymbol(maxcEntries[cellIndex]), maxcScores[cellIndex],
                            maxcMidpoints[cellIndex]));
                } else {
                    sb.append(String.format("  MaxC = %s -> %s (%.5f, %d)",
                            sparseMatrixGrammar.nonTermSet.getSymbol(maxcEntries[cellIndex]),
                            sparseMatrixGrammar.nonTermSet.getSymbol(maxcUnaryChildren[cellIndex]),
                            maxcScores[cellIndex], maxcMidpoints[cellIndex]));
                }
            }

            sb.append('\n');

            if (tmpPackedChildren == null) {
                // Format entries from the main chart array
                for (int index = offset; index < offset + numNonTerminals[cellIndex]; index++) {
                    final int childProductions = packedChildren[index];
                    final float insideProbability = insideProbabilities[index];
                    final float outsideProbability = outsideProbabilities[index];
                    final int midpoint = midpoints[index];

                    final int nonTerminal = nonTerminalIndices[index];

                    sb.append(formatCellEntry(nonTerminal, childProductions, insideProbability, midpoint,
                            outsideProbability, formatFractions));
                }
            } else {
                // Format entries from temporary cell storage
                for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {

                    if (tmpInsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                        final int childProductions = tmpPackedChildren[nonTerminal];
                        final float insideProbability = tmpInsideProbabilities[nonTerminal];
                        final float outsideProbability = tmpOutsideProbabilities[nonTerminal];
                        final int midpoint = tmpMidpoints[nonTerminal];

                        sb.append(formatCellEntry(nonTerminal, childProductions, insideProbability, midpoint,
                                outsideProbability, formatFractions));
                    }
                }
            }
            return sb.toString();
        }

        private String formatCellEntry(final int nonterminal, final int childProductions,
                final float insideProbability, final int midpoint, final float outsideProbability,
                final boolean formatFractions) {
            final int leftChild = sparseMatrixGrammar.cartesianProductFunction().unpackLeftChild(childProductions);
            final int rightChild = sparseMatrixGrammar.cartesianProductFunction().unpackRightChild(childProductions);

            if (formatFractions) {
                if (rightChild == Production.UNARY_PRODUCTION) {
                    // Unary Production
                    return String.format("%s -> %s (%s, %d) outside=%s\n",
                            sparseMatrixGrammar.mapNonterminal(nonterminal),
                            sparseMatrixGrammar.mapNonterminal(leftChild), JUnit.fraction(insideProbability), midpoint,
                            JUnit.fraction(outsideProbability));
                } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                    // Lexical Production
                    return String.format("%s -> %s (%s, %d) outside=%s\n",
                            sparseMatrixGrammar.mapNonterminal(nonterminal),
                            sparseMatrixGrammar.mapLexicalEntry(leftChild), JUnit.fraction(insideProbability),
                            midpoint, JUnit.fraction(outsideProbability));
                } else {
                    return String.format("%s -> %s %s (%s, %d) outside=%s\n",
                            sparseMatrixGrammar.mapNonterminal(nonterminal),
                            sparseMatrixGrammar.mapNonterminal(leftChild),
                            sparseMatrixGrammar.mapNonterminal(rightChild), JUnit.fraction(insideProbability),
                            midpoint, JUnit.fraction(outsideProbability));
                }
            } else {
                if (rightChild == Production.UNARY_PRODUCTION) {
                    // Unary Production
                    return String.format("%s -> %s (%.5f, %d) outside=%.5f\n",
                            sparseMatrixGrammar.mapNonterminal(nonterminal),
                            sparseMatrixGrammar.mapNonterminal(leftChild), insideProbability, midpoint,
                            outsideProbability);
                } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                    // Lexical Production
                    return String.format("%s -> %s (%.5f, %d) outside=%.5f\n",
                            sparseMatrixGrammar.mapNonterminal(nonterminal),
                            sparseMatrixGrammar.mapLexicalEntry(leftChild), insideProbability, midpoint,
                            outsideProbability);
                } else {
                    return String.format("%s -> %s %s (%.5f, %d) outside=%.5f\n",
                            sparseMatrixGrammar.mapNonterminal(nonterminal),
                            sparseMatrixGrammar.mapNonterminal(leftChild),
                            sparseMatrixGrammar.mapNonterminal(rightChild), insideProbability, midpoint,
                            outsideProbability);
                }
            }
        }
    }
}
