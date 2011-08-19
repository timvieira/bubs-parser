package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.Tree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.Vocabulary;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ml.InsideOutsideCphSpmlParser.DecodingMethod;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Chart structure including outside probabilities and various decoding methods dependent on posterior probabilities.
 */
public class InsideOutsideChart extends PackedArrayChart {

    public final float[] outsideProbabilities;
    public final float[] viterbiInsideProbabilities;

    // Default lambda to 0 (max-recall) if unset
    private final double lambda = GlobalConfigProperties.singleton().getFloatProperty(Parser.PROPERTY_MAXC_LAMBDA, 0f);

    /**
     * Parallel array of max-c scores (see Goodman, 1996). Stored as instance variables instead of locals purely for
     * debugging and visualization via {@link #toString()}.
     */
    private final short[] maxcEntries;
    private final double[] maxcScores;
    private final short[] maxcMidpoints;
    private final short[] maxcUnaryChildren;

    public InsideOutsideChart(final int[] tokens, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(tokens, sparseMatrixGrammar);
        this.outsideProbabilities = new float[chartArraySize];
        this.viterbiInsideProbabilities = new float[chartArraySize];

        final int maxcArraySize = tokens.length * (tokens.length + 1) / 2;
        this.maxcEntries = new short[maxcArraySize];
        this.maxcScores = new double[maxcArraySize];
        this.maxcMidpoints = new short[maxcArraySize];
        this.maxcUnaryChildren = new short[maxcArraySize];
    }

    public void finalizeOutside(final float[] tmpOutsideProbabilities, final int cellIndex) {

        // Copy from temporary storage all entries which have non-0 inside and outside probabilities
        final int startIndex = offset(cellIndex);
        final int endIndex = startIndex + numNonTerminals[cellIndex];

        for (int i = startIndex; i < endIndex; i++) {
            outsideProbabilities[i] = tmpOutsideProbabilities[nonTerminalIndices[i]];
        }
    }

    public void decode(final DecodingMethod decodingMethod) {
        switch (decodingMethod) {
        case Viterbi:
            // No decoding necessary
            break;

        case Goodman:
        case SplitSum:
            computeSplitSumMaxc(decodingMethod);
            break;

        default:
            throw new UnsupportedOperationException("Decoding method " + decodingMethod + " not implemented");
        }
    }

    /**
     * Computes 'maxc', per the algorithm in Figure 1 of Joshua Goodman, 1996, Parsing Algorithms and Metrics, using
     * lambda as per Equation 7, Appendix A of Hollingshead and Roark, Pipeline Iteration.
     * 
     * If decodingMethod is {@link DecodingMethod#Goodman}, the maxc computation is identical to that presented in
     * Goodman, with the addition of a unary processing step described further below. If the decodingMethod is
     * {@link DecodingMethod#SplitSum}, we instead sum over unsplit categories.
     * 
     * @param decodingMethod
     */
    private void computeSplitSumMaxc(final DecodingMethod decodingMethod) {

        final PackingFunction pf = sparseMatrixGrammar.packingFunction;
        Vocabulary vocabulary;
        Short2ShortMap nonTermMap;

        switch (decodingMethod) {
        case Goodman:
            vocabulary = sparseMatrixGrammar.nonTermSet;
            nonTermMap = new IdentityMap();
            break;

        case SplitSum:
            vocabulary = sparseMatrixGrammar.nonTermSet.baseVocabulary();
            nonTermMap = new BaseNonterminalMap(vocabulary);
            break;

        default:
            throw new UnsupportedOperationException("Decoding method " + decodingMethod + " is not supported");
        }

        // Start symbol inside-probability (e)
        final int topCellIndex = cellIndex(0, size);
        final float startSymbolInsideProbability = insideProbabilities[entryIndex(offset(topCellIndex),
                numNonTerminals[topCellIndex], (short) sparseMatrixGrammar.startSymbol)];

        Arrays.fill(maxcEntries, Short.MIN_VALUE);
        Arrays.fill(maxcScores, Double.NEGATIVE_INFINITY);
        Arrays.fill(maxcMidpoints, Short.MIN_VALUE);
        Arrays.fill(maxcUnaryChildren, Short.MIN_VALUE);

        // Span-1 cells
        for (short start = 0; start < size; start++) {
            final int cellIndex = cellIndex(start, start + 1);
            final int offset = offset(cellIndex);

            // maxc = max(posterior probability / e). If nonTermMap is an IdentityMap, this will compute Goodman's maxc;
            // if it is an instance of BaseNonterminalMap, we will instead compute sums over unsplit non-terminals
            final double[] splitSum = new double[vocabulary.size()];
            for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                final double c = Math.exp(insideProbabilities[i] + outsideProbabilities[i]
                        - startSymbolInsideProbability);
                splitSum[nonTermMap.get(nonTerminalIndices[i])] += c;
            }
            final int maxcEntry = edu.ohsu.cslu.util.Math.argmax(splitSum);
            maxcEntries[cellIndex] = (short) maxcEntry;
            maxcScores[cellIndex] = splitSum[maxcEntry];
        }

        // Span > 1 cells
        for (short span = 2; span <= size; span++) {
            for (short start = 0; start < size - span + 1; start++) {
                final int end = start + span;
                final int cellIndex = cellIndex(start, end);
                final int offset = offset(cellIndex);

                // maxg = max(posterior probability / e). If nonTermMap is an IdentityMap, this will compute Goodman's
                // maxg; if it is an instance of BaseNonterminalMap, we will instead compute sums over unsplit
                // non-terminals
                final double[] splitSumG = new double[vocabulary.size()];
                final double[] splitMaxG = new double[vocabulary.size()];
                final short[] unaryChildren = new short[vocabulary.size()];

                Arrays.fill(splitMaxG, Double.NEGATIVE_INFINITY);
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {

                    final short nt = nonTermMap.get(nonTerminalIndices[i]);

                    // Factored non-terminals do not contribute to the final parse tree, so their maxc score is 0
                    final double g = vocabulary.isFactored(nonTerminalIndices[i]) ? 0 : Math.exp(insideProbabilities[i]
                            + outsideProbabilities[i] - startSymbolInsideProbability)
                            - lambda;

                    // Bias toward recovering unary parents in the case of a tie (e.g., when ROOT and S are tied in the
                    // top cell)
                    final boolean unaryParent = pf.unpackRightChild(packedChildren[i]) == Production.UNARY_PRODUCTION;
                    if (g > splitMaxG[nt] || (g == splitMaxG[nt] && unaryParent)) {
                        splitMaxG[nt] = g;
                        maxcEntries[cellIndex] = nt;

                        // Addition to Goodman's algorithm: if the best path to the highest-scoring non-terminal was
                        // through a unary child, record that unary child as well. Note that this requires we store
                        // unary backpointers during the inside parsing pass.
                        if (unaryParent) {
                            unaryChildren[nt] = (short) pf.unpackLeftChild(packedChildren[i]);
                        } else {
                            unaryChildren[nt] = Short.MIN_VALUE;
                        }
                    }

                    splitSumG[nt] = splitSumG[nt] == Double.NEGATIVE_INFINITY ? g : splitSumG[nt] + g;
                }

                final short maxNt = (short) edu.ohsu.cslu.util.Math.argmax(splitMaxG);
                maxcEntries[cellIndex] = maxNt;
                maxcUnaryChildren[cellIndex] = unaryChildren[maxNt];
                final double maxg = splitMaxG[maxNt];

                // Iterate over possible binary child cells, to find the maximum midpoint ('max split')
                double bestSplit = Double.NEGATIVE_INFINITY;
                for (short midpoint = (short) (start + 1); midpoint < end; midpoint++) {

                    // maxc = max(posterior probability) + max(maxc(children)). Store observed midpoints for use when
                    // extracting the parse tree
                    final double split = maxcScores[cellIndex(start, midpoint)] + maxcScores[cellIndex(midpoint, end)];
                    if (split > bestSplit) {
                        bestSplit = split;
                        maxcScores[cellIndex] = maxg + split;
                        maxcMidpoints[cellIndex] = midpoint;
                    }
                }
            }
        }
    }

    /**
     * Extracts the max-recall parse (Goodman's 'Labeled Recall' algorithm), using the maxc values computed by
     * {@link #decode()}.
     * 
     * @param start
     * @param end
     * @param decodingMethod TODO
     * @return extracted binary tree
     */
    public BinaryTree<String> extract(final int start, final int end, final DecodingMethod decodingMethod) {

        Vocabulary vocabulary;
        Short2ShortMap nonTermMap;

        switch (decodingMethod) {
        case Goodman:
            vocabulary = sparseMatrixGrammar.nonTermSet;
            nonTermMap = new IdentityMap();
            break;
        case SplitSum:
            vocabulary = sparseMatrixGrammar.nonTermSet;
            nonTermMap = new BaseNonterminalMap(vocabulary);
            break;

        default:
            throw new UnsupportedOperationException("Decoding method " + decodingMethod + " is not supported");
        }

        final short startSymbol = vocabulary.startSymbol();
        final PackingFunction pf = sparseMatrixGrammar.packingFunction;

        final int cellIndex = cellIndex(start, end);
        final int offset = offset(cellIndex);
        final int numNonTerms = numNonTerminals[cellIndex];

        // Find the non-terminal which maximizes the decoding metric (e.g. Goodman's max-constituent or Petrov's
        // max-rule)
        short parent = maxcEntries[cellIndex];

        // If the maxc score of the parent is negative (except for start symbol), add a 'dummy' symbol instead, which
        // will be removed when the tree is unfactored.
        final String sParent = (maxcScores[cellIndex] > 0 || parent == startSymbol) ? vocabulary.getSymbol(parent)
                : Tree.NULL_LABEL;
        final BinaryTree<String> tree = new BinaryTree<String>(sParent);
        BinaryTree<String> subtree = tree;

        if (end - start == 1) {

            BinaryTree<String> unaryTree = subtree;

            // Find the index of the current parent in the chart storage and follow the unary productions down to the
            // lexical entry
            int i;
            for (i = entryIndex(offset, numNonTerms, parent); pf.unpackRightChild(packedChildren[i]) != Production.LEXICAL_PRODUCTION; i = entryIndex(
                    offset, numNonTerms, parent)) {
                parent = (short) pf.unpackLeftChild(packedChildren[i]);
                unaryTree = unaryTree.addChild(new BinaryTree<String>(vocabulary.getSymbol(parent)));
            }
            unaryTree.addChild(new BinaryTree<String>(sparseMatrixGrammar.lexSet.getSymbol(pf
                    .unpackLeftChild(packedChildren[i]))));
            return subtree;
        }

        final short edgeMidpoint = maxcMidpoints[cellIndex];

        if (maxcUnaryChildren[cellIndex] >= 0) {
            // Unary production - we currently only allow one level of unary in span > 1 cells.
            subtree = subtree.addChild((maxcScores[cellIndex] > 0 || parent == startSymbol) ? vocabulary
                    .getSymbol(maxcUnaryChildren[cellIndex]) : Tree.NULL_LABEL);
        }

        // Binary production
        subtree.addChild(extract(start, edgeMidpoint, decodingMethod));
        subtree.addChild(extract(edgeMidpoint, end, decodingMethod));

        return tree;
    }

    /**
     * Returns the index in the parallel chart array of the specified parent in the cell with the specified offset
     * 
     * @param offset The offset of the target cell
     * @param numNonTerminals Number of non-terminals populated in the target cell
     * @param parent The parent to search for in the target cell
     * @return the index in the parallel chart array of the specified parent in the cell with the specified offset
     */
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

    private interface Short2ShortMap {
        public short get(short k);
    }

    private final class IdentityMap implements Short2ShortMap {
        @Override
        public final short get(final short k) {
            return k;
        }
    }

    private final class BaseNonterminalMap implements Short2ShortMap {
        private final Vocabulary vocabulary;

        public BaseNonterminalMap(final Vocabulary vocabulary) {
            this.vocabulary = vocabulary;
        }

        @Override
        public short get(final short k) {
            return vocabulary.getBaseIndex(k);
        }

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
