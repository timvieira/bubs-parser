package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.Tree;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.Vocabulary;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Chart structure including outside probabilities and various decoding methods dependent on posterior probabilities.
 */
public class InsideOutsideChart extends PackedArrayChart {

    public final float[] outsideProbabilities;
    public final float[] decodingScores;

    // Default lambda to 0 (max-recall) if unset
    private final double lambda = GlobalConfigProperties.singleton().getFloatProperty(Parser.PROPERTY_MAXC_LAMBDA, 0f);

    /**
     * Parallel array of max-c scores (see Goodman, 1996); indexed by cellIndex. Stored as instance variables instead of
     * locals purely for debugging and visualization via {@link #toString()}.
     */
    private final short[] maxcEntries;
    private final double[] maxcScores;
    private final short[] maxcMidpoints;
    private final short[] maxcUnaryChildren;

    /**
     * Parallel array of max-q scores (see Petrov, 2007). Stored as instance variables instead of locals purely for
     * debugging and visualization via {@link #toString()}.
     * 
     * maxQ = current-cell q * child cell q's (accumulating max-rule product up the chart) All 2-d arrays indexed by
     * cellIndex and base (Markov-0) vocabulary
     */
    final float[][] maxQ;
    final short[][] maxQMidpoints;
    final short[][] maxQLeftChildren;
    final short[][] maxQRightChildren;

    private Vocabulary maxcVocabulary = sparseMatrixGrammar.nonTermSet;

    public InsideOutsideChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(parseTask, sparseMatrixGrammar);
        this.outsideProbabilities = new float[chartArraySize];
        this.decodingScores = new float[chartArraySize];

        switch (parseTask.decodeMethod) {
        case Goodman:
        case SplitSum:
            final int maxcArraySize = tokens.length * (tokens.length + 1) / 2;
            this.maxcEntries = new short[maxcArraySize];
            this.maxcScores = new double[maxcArraySize];
            this.maxcMidpoints = new short[maxcArraySize];
            this.maxcUnaryChildren = new short[maxcArraySize];

            this.maxQ = null;
            this.maxQMidpoints = null;
            this.maxQLeftChildren = null;
            this.maxQRightChildren = null;
            break;

        case MaxRuleProd:
            this.maxQ = new float[cells][maxcVocabulary.size()];
            this.maxQMidpoints = new short[cells][maxcVocabulary.size()];
            this.maxQLeftChildren = new short[cells][maxcVocabulary.size()];
            this.maxQRightChildren = new short[cells][maxcVocabulary.size()];

            this.maxcEntries = null;
            this.maxcScores = null;
            this.maxcMidpoints = null;
            this.maxcUnaryChildren = null;

            break;
        default:
            throw new UnsupportedOperationException("Unsupported decoding method: " + parseTask.decodeMethod);
        }
    }

    public void finalizeOutside(final float[] tmpOutsideProbabilities, final int cellIndex) {

        // Copy from temporary storage all entries which have non-0 inside and outside probabilities
        final int startIndex = offset(cellIndex);
        final int endIndex = startIndex + numNonTerminals[cellIndex];

        for (int i = startIndex; i < endIndex; i++) {
            outsideProbabilities[i] = tmpOutsideProbabilities[nonTerminalIndices[i]];
        }
    }

    /**
     * Decodes the packed parse forest using the specified decoding method (e.g., {@link DecodeMethod#Goodman},
     * {@link DecodeMethod#MaxRuleProd}, etc.)
     * 
     * @return The extracted binary tree
     */
    public BinaryTree<String> decode() {
        switch (parseTask.decodeMethod) {

        case Goodman:
            computeGoodmanMaxc();
            return extractMaxcParse(0, size);

        case SplitSum:
            computeSplitSumMaxc();
            return extractMaxcParse(0, size);

        case MaxRuleProd:
            return decodeMaxRuleProductParse((LeftCscSparseMatrixGrammar) grammar);

        default:
            throw new UnsupportedOperationException("Decoding method " + parseTask.decodeMethod + " not implemented");
        }
    }

    /**
     * Computes 'maxc', per the algorithm in Figure 1 of Joshua Goodman, 1996, 'Parsing Algorithms and Metrics'.
     * 
     * Uses lambda as per Equation 7, Appendix A of Hollingshead and Roark, 'Pipeline Iteration'.
     * 
     */
    private void computeGoodmanMaxc() {

        final PackingFunction pf = sparseMatrixGrammar.packingFunction;
        maxcVocabulary = sparseMatrixGrammar.nonTermSet;
        initMaxc();

        // Start symbol inside-probability (e in Goodman's notation)
        final float startSymbolInsideProbability = startSymbolInsideProbability();

        for (short span = 1; span <= size; span++) {
            for (short start = 0; start < size - span + 1; start++) {
                final int end = start + span;
                final int cellIndex = cellIndex(start, end);
                final int offset = offset(cellIndex);

                // maxg = max(posterior probability / e).
                double maxg = Double.NEGATIVE_INFINITY;
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {

                    final short nt = nonTerminalIndices[i];

                    // Factored non-terminals do not contribute to the final parse tree, so their maxc score is 0
                    final double g = maxcVocabulary.isFactored(nt) ? 0 : Math.exp(insideProbabilities[i]
                            + outsideProbabilities[i] - startSymbolInsideProbability)
                            - (span > 1 ? lambda : 0);

                    // Bias toward recovering unary parents in the case of a tie (e.g., when ROOT and S are tied in the
                    // top cell)
                    final boolean unaryParent = pf.unpackRightChild(packedChildren[i]) == Production.UNARY_PRODUCTION;
                    if (g > maxg || (g == maxg && unaryParent)) {
                        maxg = g;
                        maxcEntries[cellIndex] = nt;

                        // Addition to Goodman's algorithm: if the best path to the highest-scoring non-terminal was
                        // through a unary child, record that unary child as well. Note that this requires we store
                        // unary backpointers during the inside parsing pass.
                        if (unaryParent) {
                            maxcUnaryChildren[cellIndex] = (short) pf.unpackLeftChild(packedChildren[i]);
                        } else {
                            maxcUnaryChildren[cellIndex] = Short.MIN_VALUE;
                        }
                    }
                }

                if (span == 1) {
                    maxcScores[cellIndex] = maxg;
                } else {
                    // Iterate over possible binary child cells, to find the maximum midpoint ('max split')
                    double bestSplit = Double.NEGATIVE_INFINITY;
                    for (short midpoint = (short) (start + 1); midpoint < end; midpoint++) {

                        // maxc = max(posterior probability) + max(maxc(children)). Also store midpoints for use when
                        // extracting the parse tree
                        final double split = maxcScores[cellIndex(start, midpoint)]
                                + maxcScores[cellIndex(midpoint, end)];
                        if (split > bestSplit) {
                            bestSplit = split;
                            maxcScores[cellIndex] = maxg + split;
                            maxcMidpoints[cellIndex] = midpoint;
                        }
                    }
                }
            }
        }
    }

    /**
     * Initializes maxc arrays.
     */
    private void initMaxc() {
        Arrays.fill(maxcEntries, Short.MIN_VALUE);
        Arrays.fill(maxcScores, Double.NEGATIVE_INFINITY);
        Arrays.fill(maxcMidpoints, Short.MIN_VALUE);
        Arrays.fill(maxcUnaryChildren, Short.MIN_VALUE);
    }

    /**
     * Finds the inside probability of the start symbol in the top cell.
     * 
     * @return Start-symbol inside probability
     */
    private float startSymbolInsideProbability() {
        final int topCellIndex = cellIndex(0, size);
        final int startSymbolIndex = entryIndex(offset(topCellIndex), numNonTerminals[topCellIndex],
                (short) sparseMatrixGrammar.startSymbol);
        if (startSymbolIndex < 0) {
            throw new IllegalArgumentException("Parse failure");
        }
        final float startSymbolInsideProbability = insideProbabilities[startSymbolIndex];

        return startSymbolInsideProbability;
    }

    /**
     * Sums over unsplit categories while computing 'maxc', per the algorithm in Figure 1 of Joshua Goodman, 1996,
     * 'Parsing Algorithms and Metrics'.
     * 
     * Uses lambda as per {@link #computeGoodmanMaxc()} - from Equation 7, Appendix A of Hollingshead and Roark,
     * 'Pipeline Iteration'.
     */
    private void computeSplitSumMaxc() {

        final PackingFunction pf = sparseMatrixGrammar.packingFunction;
        maxcVocabulary = sparseMatrixGrammar.nonTermSet.baseVocabulary();
        initMaxc();

        // Start symbol inside-probability (e in Goodman's notation)
        final float startSymbolInsideProbability = startSymbolInsideProbability();

        for (short span = 1; span <= size; span++) {
            for (short start = 0; start < size - span + 1; start++) {
                final int end = start + span;
                final int cellIndex = cellIndex(start, end);
                final int offset = offset(cellIndex);

                // maxg = max(posterior probability / e).
                final float[] baseSumProbabilities = new float[maxcVocabulary.size()];
                final short[] unaryChildren = new short[maxcVocabulary.size()];
                Arrays.fill(baseSumProbabilities, Float.NEGATIVE_INFINITY);
                Arrays.fill(unaryChildren, Short.MIN_VALUE);

                final float[] maxBaseProbabilities = new float[maxcVocabulary.size()];
                Arrays.fill(maxBaseProbabilities, Float.NEGATIVE_INFINITY);

                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                    final float posteriorProbability = insideProbabilities[i] + outsideProbabilities[i];
                    final short baseNt = sparseMatrixGrammar.nonTermSet.getBaseIndex(nonTerminalIndices[i]);

                    baseSumProbabilities[baseNt] = edu.ohsu.cslu.util.Math.logSum(baseSumProbabilities[baseNt],
                            posteriorProbability);

                    if (posteriorProbability > maxBaseProbabilities[baseNt]) {
                        maxBaseProbabilities[baseNt] = posteriorProbability;
                        // If the (current) maximum-probability split of the base nonterminal is a unary parent, record
                        // the base nonterminal as a unary parent with the appropriate unary child
                        if (pf.unpackRightChild(packedChildren[i]) == Production.UNARY_PRODUCTION) {
                            unaryChildren[baseNt] = sparseMatrixGrammar.nonTermSet.getBaseIndex((short) pf
                                    .unpackLeftChild(packedChildren[i]));
                        } else {
                            unaryChildren[baseNt] = Short.MIN_VALUE;
                        }
                    }
                }
                double maxg = Double.NEGATIVE_INFINITY;

                // Compute g scores for each base NT and record the max
                for (short baseNt = 0; baseNt < baseSumProbabilities.length; baseNt++) {

                    // Factored non-terminals do not contribute to the final parse tree, so their maxc score is 0
                    final double g = maxcVocabulary.isFactored(baseNt) ? 0 : Math.exp(baseSumProbabilities[baseNt]
                            - startSymbolInsideProbability)
                            - (span > 1 ? lambda : 0);

                    // Bias toward recovering unary parents in the case of a tie (e.g., when ROOT and S are tied in the
                    // top cell)
                    if (g > maxg || (g == maxg && unaryChildren[baseNt] >= 0)) {
                        maxg = g;
                        maxcEntries[cellIndex] = baseNt;
                    }
                }

                // Addition to Goodman's algorithm: if the best path to the highest-scoring non-terminal was
                // through a unary child, record that unary child as well. Note that this requires we store
                // unary backpointers during the inside parsing pass.
                maxcUnaryChildren[cellIndex] = unaryChildren[maxcEntries[cellIndex]];

                // For span-1 cells, maxc = maxg
                if (span == 1) {
                    maxcScores[cellIndex] = maxg;
                } else {
                    // For span > 1, we must iterate over possible binary child cells, to find the maximum midpoint
                    // ('max split')
                    double bestSplit = Double.NEGATIVE_INFINITY;
                    for (short midpoint = (short) (start + 1); midpoint < end; midpoint++) {

                        // maxc = max(posterior probability) + max(maxc(children)). Also store midpoints for use when
                        // extracting the parse tree
                        final double split = maxcScores[cellIndex(start, midpoint)]
                                + maxcScores[cellIndex(midpoint, end)];
                        if (split > bestSplit) {
                            bestSplit = split;
                            maxcScores[cellIndex] = maxg + split;
                            maxcMidpoints[cellIndex] = midpoint;
                        }
                    }
                }
            }
        }
    }

    /**
     * Extracts the max-recall/max-precision/combined parse (Goodman's 'Labeled Recall' algorithm), using the maxc
     * values computed by {@link #decode(DecodeMethod)}.
     * 
     * Precondition: {@link #maxcEntries} is populated with the indices of the non-terminals (in {@link #maxcVocabulary}
     * ) which maximize the decoding criteria. {@link maxcScores} is populated with the scores of those entries.
     * 
     * @param start
     * @param end
     * @return extracted binary tree
     */
    private BinaryTree<String> extractMaxcParse(final int start, final int end) {

        final short startSymbol = maxcVocabulary.startSymbol();
        final PackingFunction pf = sparseMatrixGrammar.packingFunction;

        final int cellIndex = cellIndex(start, end);
        final int offset = offset(cellIndex);
        final int numNonTerms = numNonTerminals[cellIndex];

        // Find the non-terminal which maximizes the decoding metric (e.g. Goodman's max-constituent or Petrov's
        // max-rule)
        short parent = maxcEntries[cellIndex];

        // If the maxc score of the parent is negative (except for start symbol), add a 'dummy' symbol instead, which
        // will be removed when the tree is unfactored.
        final String sParent = (maxcScores[cellIndex] > 0 || parent == startSymbol || end - start == 1) ? maxcVocabulary
                .getSymbol(parent) : Tree.NULL_LABEL;
        final BinaryTree<String> tree = new BinaryTree<String>(sParent);
        BinaryTree<String> subtree = tree;

        if (end - start == 1) {

            BinaryTree<String> unaryTree = subtree;

            if (parseTask.decodeMethod == DecodeMethod.SplitSum) {
                // Find the maximum-probability split of the unsplit parent non-terminal
                float maxSplitProb = Float.NEGATIVE_INFINITY;
                short splitParent = -1;
                for (int i = offset; i < offset + numNonTerms; i++) {
                    final float posteriorProbability = insideProbabilities[i] + outsideProbabilities[i];
                    if (posteriorProbability > maxSplitProb
                            && sparseMatrixGrammar.nonTermSet.getBaseIndex(nonTerminalIndices[i]) == parent) {
                        maxSplitProb = posteriorProbability;
                        splitParent = nonTerminalIndices[i];
                    }
                }
                parent = splitParent;
            }

            // Find the index of the current parent in the chart storage and follow the unary productions down to
            // the lexical entry
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
            subtree = subtree.addChild((maxcScores[cellIndex] > 0 || parent == startSymbol) ? maxcVocabulary
                    .getSymbol(maxcUnaryChildren[cellIndex]) : Tree.NULL_LABEL);
        }

        // Binary production
        subtree.addChild(extractMaxcParse(start, edgeMidpoint));
        subtree.addChild(extractMaxcParse(edgeMidpoint, end));

        return tree;
    }

    /**
     * Computes max-rule-product parse, as described in Figure 3 of Petrov and Klein, 1997, 'Improved Inference for
     * Unlexicalized Parsing'.
     */
    private BinaryTree<String> decodeMaxRuleProductParse(final LeftCscSparseMatrixGrammar cscGrammar) {

        final PackingFunction pf = cscGrammar.packingFunction;
        maxcVocabulary = cscGrammar.nonTermSet.baseVocabulary();

        // Start symbol inside-probability (P_in(root,0,n) in Petrov's notation)
        final float startSymbolInsideProbability = startSymbolInsideProbability();

        for (short span = 1; span <= size; span++) {
            for (short start = 0; start < size - span + 1; start++) {

                final short end = (short) (start + span);
                final int cellIndex = cellIndex(start, end);
                Arrays.fill(maxQMidpoints[cellIndex], (short) -1);
                Arrays.fill(maxQ[cellIndex], Float.NEGATIVE_INFINITY);

                // Initialize lexical entries in the score arrays - sum outside probability x production probability
                // over all nonterminal splits
                if (end - start == 1) {
                    final float[] r = new float[maxcVocabulary.size()];
                    Arrays.fill(r, Float.NEGATIVE_INFINITY);

                    final int offset = offset(cellIndex);
                    for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                        final short parent = nonTerminalIndices[i];
                        if (grammar.isPos(parent)) {
                            final short baseParent = cscGrammar.nonTermSet.getBaseIndex(parent);
                            maxQMidpoints[cellIndex][baseParent] = end;
                            // Left child is implied by marking the production as lexical. Unaries will be handled
                            // below.
                            r[baseParent] = edu.ohsu.cslu.util.Math.logSum(r[baseParent], outsideProbabilities[i]
                                    + cscGrammar.lexicalLogProbability(parent, tokens[start]));
                            maxQRightChildren[cellIndex][baseParent] = Production.LEXICAL_PRODUCTION;
                        }
                    }
                    for (int baseParent = 0; baseParent < maxcVocabulary.size(); baseParent++) {
                        if (r[baseParent] > Float.NEGATIVE_INFINITY) {
                            maxQ[cellIndex][baseParent] = r[baseParent] - startSymbolInsideProbability;
                        }
                    }
                }

                // Iterate over all possible midpoints
                for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {

                    // Since Petrov's 'r' is a sum over unsplit categories, we compute a temporary r array for each
                    // midpoint and then maximize over midpoints
                    // currentMidpointR is indexed by base parent, base left child, base right child
                    final float[][][] currentMidpointR = new float[maxcVocabulary.size()][][];

                    final int leftCellIndex = cellIndex(start, midpoint);
                    final int rightCellIndex = cellIndex(midpoint, end);

                    final int leftStart = minLeftChildIndex(leftCellIndex);
                    final int leftEnd = maxLeftChildIndex(leftCellIndex);

                    final int rightStart = minRightChildIndex(rightCellIndex);
                    final int rightEnd = maxRightChildIndex(rightCellIndex);

                    // Iterate over children in the left child cell
                    for (int i = leftStart; i <= leftEnd; i++) {
                        final short leftChild = nonTerminalIndices[i];
                        final short baseLeftChild = cscGrammar.nonTermSet.getBaseIndex(leftChild);
                        final float leftProbability = insideProbabilities[i];

                        // And over children in the right child cell
                        for (int j = rightStart; j <= rightEnd; j++) {
                            final short rightChild = nonTerminalIndices[j];
                            final int column = pf.pack(leftChild, rightChild);
                            if (column == Integer.MIN_VALUE) {
                                continue;
                            }
                            final short baseRightChild = cscGrammar.nonTermSet.getBaseIndex(rightChild);

                            final float childProbability = leftProbability + insideProbabilities[j];

                            for (int k = cscGrammar.cscBinaryColumnOffsets[column]; k < cscGrammar.cscBinaryColumnOffsets[column + 1]; k++) {

                                final short parent = cscGrammar.cscBinaryRowIndices[k];
                                final int parentIndex = entryIndex(offset(cellIndex), numNonTerminals[cellIndex],
                                        parent);
                                if (parentIndex < 0) {
                                    continue;
                                }
                                final float parentOutside = outsideProbabilities[parentIndex];
                                final short baseParent = cscGrammar.nonTermSet.getBaseIndex(parent);

                                // Allocate space in current-midpoint r array if needed
                                allocateChildArray(currentMidpointR, baseParent, baseLeftChild);

                                currentMidpointR[baseParent][baseLeftChild][baseRightChild] = edu.ohsu.cslu.util.Math
                                        .logSum(currentMidpointR[baseParent][baseLeftChild][baseRightChild],
                                                cscGrammar.cscBinaryProbabilities[k] + childProbability + parentOutside);
                            }
                        }
                    }

                    // Merge current-midpoint r array into the maxQ array
                    mergeRIntoMaxQ(midpoint, currentMidpointR, maxQ[cellIndex], maxQMidpoints[cellIndex],
                            maxQLeftChildren[cellIndex], maxQRightChildren[cellIndex], startSymbolInsideProbability);
                }

                // Compute unary scores - iterate over populated children (matrix columns). Indexed by base parent and
                // child
                final float[][] unaryR = unaryR(cscGrammar, cellIndex);

                // Replace any binary or lexical parent scores which are beat by unaries
                for (short baseParent = 0; baseParent < unaryR.length; baseParent++) {
                    final float[] parentUnaryR = unaryR[baseParent];
                    if (parentUnaryR == null) {
                        continue;
                    }
                    for (short baseChild = 0; baseChild < parentUnaryR.length; baseChild++) {
                        // Preclude unary chains. Not great, but it's one way to prevent infinite unary loops
                        if (parentUnaryR[baseChild] - startSymbolInsideProbability > maxQ[cellIndex][baseParent]
                                && maxQRightChildren[cellIndex][baseChild] != Production.UNARY_PRODUCTION) {
                            maxQ[cellIndex][baseParent] = parentUnaryR[baseChild] - startSymbolInsideProbability;
                            maxQMidpoints[cellIndex][baseParent] = end;
                            maxQLeftChildren[cellIndex][baseParent] = baseChild;
                            maxQRightChildren[cellIndex][baseParent] = Production.UNARY_PRODUCTION;
                        }
                    }
                }
            }
        }

        return extractMaxQParse(0, size, maxcVocabulary.startSymbol(), maxcVocabulary);
    }

    private float[][] unaryR(final LeftCscSparseMatrixGrammar cscGrammar, final int cellIndex) {

        final float[][] unaryR = new float[maxcVocabulary.size()][];

        // Iterate over children in the cell
        final int offset = offset(cellIndex);
        for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
            final short child = nonTerminalIndices[i];
            final short baseChild = cscGrammar.nonTermSet.getBaseIndex(child);
            final float childInsideProbability = insideProbabilities[i];

            // And over grammar rules matching the child
            for (int j = cscGrammar.cscUnaryColumnOffsets[child]; j < cscGrammar.cscUnaryColumnOffsets[child + 1]; j++) {

                final short parent = cscGrammar.cscUnaryRowIndices[j];
                final int parentIndex = entryIndex(offset(cellIndex), numNonTerminals[cellIndex], parent);
                if (parentIndex < 0) {
                    continue;
                }
                // Parent outside x production probability x child inside
                final float jointScore = outsideProbabilities[parentIndex] + cscGrammar.cscUnaryProbabilities[j]
                        + childInsideProbability;
                final short baseParent = cscGrammar.nonTermSet.getBaseIndex(parent);

                if (unaryR[baseParent] == null) {
                    unaryR[baseParent] = new float[maxcVocabulary.size()];
                    Arrays.fill(unaryR[baseParent], Float.NEGATIVE_INFINITY);
                }
                unaryR[baseParent][baseChild] = edu.ohsu.cslu.util.Math.logSum(unaryR[baseParent][baseChild],
                        jointScore);
            }
        }

        return unaryR;
    }

    private void mergeRIntoMaxQ(final short midpoint, final float[][][] currentMidpointR, final float[] cellMaxQ,
            final short[] cellMaxQMidpoints, final short[] cellMaxQLeftChildren, final short[] cellMaxQRightChildren,
            final float startSymbolInsideProbability) {

        for (int baseParent = 0; baseParent < currentMidpointR.length; baseParent++) {

            final float[][] leftChildR = currentMidpointR[baseParent];
            if (leftChildR == null) {
                continue;
            }

            float maxR = Float.NEGATIVE_INFINITY;
            short maxLeftChild = Short.MIN_VALUE;
            short maxRightChild = Short.MIN_VALUE;

            for (short baseLeftChild = 0; baseLeftChild < leftChildR.length; baseLeftChild++) {

                final float[] rightChildR = currentMidpointR[baseParent][baseLeftChild];
                if (rightChildR == null) {
                    continue;
                }

                for (short baseRightChild = 0; baseRightChild < rightChildR.length; baseRightChild++) {
                    if (rightChildR[baseRightChild] > maxR) {
                        maxR = rightChildR[baseRightChild];
                        maxLeftChild = baseLeftChild;
                        maxRightChild = baseRightChild;
                    }
                }
            }
            if (maxR - startSymbolInsideProbability > cellMaxQ[baseParent]) {
                cellMaxQ[baseParent] = maxR - startSymbolInsideProbability;
                cellMaxQMidpoints[baseParent] = midpoint;
                cellMaxQLeftChildren[baseParent] = maxLeftChild;
                cellMaxQRightChildren[baseParent] = maxRightChild;
            }
        }
    }

    private void allocateChildArray(final float[][][] currentMidpointR, final short baseParent,
            final short baseLeftChild) {
        if (currentMidpointR[baseParent] == null) {
            currentMidpointR[baseParent] = new float[maxcVocabulary.size()][];
        }
        if (currentMidpointR[baseParent][baseLeftChild] == null) {
            currentMidpointR[baseParent][baseLeftChild] = new float[maxcVocabulary.size()];
            Arrays.fill(currentMidpointR[baseParent][baseLeftChild], Float.NEGATIVE_INFINITY);
        }
    }

    private BinaryTree<String> extractMaxQParse(final int start, final int end, final int parent,
            final Vocabulary vocabulary) {

        final PackedArrayChartCell packedCell = getCell(start, end);

        if (packedCell == null) {
            return null;
        }
        final int cellIndex = packedCell.cellIndex;

        final short edgeMidpoint = maxQMidpoints[cellIndex][parent];

        final BinaryTree<String> subtree = new BinaryTree<String>(vocabulary.getSymbol(parent));
        final short leftChild = maxQLeftChildren[cellIndex][parent];
        final short rightChild = maxQRightChildren[cellIndex][parent];

        if (rightChild == Production.UNARY_PRODUCTION) {
            subtree.addChild(extractMaxQParse(start, end, leftChild, vocabulary));

        } else if (rightChild == Production.LEXICAL_PRODUCTION) {
            subtree.addChild(new BinaryTree<String>(sparseMatrixGrammar.lexSet.getSymbol(tokens[start])));

        } else {
            // binary production
            subtree.addChild(extractMaxQParse(start, edgeMidpoint, leftChild, vocabulary));
            subtree.addChild(extractMaxQParse(edgeMidpoint, end, rightChild, vocabulary));
        }
        return subtree;
    }

    /**
     * Returns the index in the parallel chart array of the specified parent in the cell with the specified offset
     * 
     * @param offset The offset of the target cell
     * @param cellPopulation Number of non-terminals populated in the target cell
     * @param parent The parent to search for in the target cell
     * @return the index in the parallel chart array of the specified parent in the cell with the specified offset
     */
    private int entryIndex(final int offset, final int cellPopulation, final short parent) {
        return Arrays.binarySearch(nonTerminalIndices, offset, offset + cellPopulation, parent);
    }

    @Override
    public InsideOutsideChartCell getCell(final int start, final int end) {
        return new InsideOutsideChartCell(start, end);
    }

    @Override
    public float getOutside(final int start, final int end, final int nonTerminal) {
        final int cellIndex = cellIndex(start, end);
        final int offset = offset(cellIndex);
        final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                (short) nonTerminal);
        if (index < 0) {
            return Float.NEGATIVE_INFINITY;
        }
        return outsideProbabilities[index];
    }

    public class InsideOutsideChartCell extends PackedArrayChartCell {

        public float[] tmpOutsideProbabilities;

        public InsideOutsideChartCell(final int start, final int end) {
            super(start, end);
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
                    + sparseMatrixGrammar.numNonTerms() + ") edges\n");

            switch (parseTask.decodeMethod) {
            case Goodman:
            case SplitSum:
                sb.append(maxcToString(formatFractions));
                break;
            case MaxRuleProd:
                sb.append(maxqToString(formatFractions));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported decoding method " + parseTask.decodeMethod);
            }
            return sb.toString();
        }

        private String maxcToString(final boolean formatFractions) {
            final StringBuilder sb = new StringBuilder(256);

            if (maxcScores[cellIndex] > Float.NEGATIVE_INFINITY) {
                if (maxcUnaryChildren[cellIndex] < 0) {
                    sb.append(String.format("  MaxC = %s (%.5f, %d)", maxcVocabulary.getSymbol(maxcEntries[cellIndex]),
                            maxcScores[cellIndex], maxcMidpoints[cellIndex]));
                } else {
                    sb.append(String.format("  MaxC = %s -> %s (%.5f, %d)",
                            maxcVocabulary.getSymbol(maxcEntries[cellIndex]),
                            maxcVocabulary.getSymbol(maxcUnaryChildren[cellIndex]), maxcScores[cellIndex],
                            maxcMidpoints[cellIndex]));
                }
            }

            sb.append('\n');

            if (tmpCell == null) {
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

                    if (tmpCell.insideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                        final int childProductions = tmpCell.packedChildren[nonTerminal];
                        final float insideProbability = tmpCell.insideProbabilities[nonTerminal];
                        final float outsideProbability = tmpOutsideProbabilities[nonTerminal];
                        final int midpoint = tmpCell.midpoints[nonTerminal];

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
                            sparseMatrixGrammar.nonTermSet.getSymbol(nonterminal),
                            sparseMatrixGrammar.nonTermSet.getSymbol(leftChild), JUnit.fraction(insideProbability),
                            midpoint, JUnit.fraction(outsideProbability));
                } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                    // Lexical Production
                    return String.format("%s -> %s (%s, %d) outside=%s\n",
                            sparseMatrixGrammar.nonTermSet.getSymbol(nonterminal),
                            sparseMatrixGrammar.mapLexicalEntry(leftChild), JUnit.fraction(insideProbability),
                            midpoint, JUnit.fraction(outsideProbability));
                } else {
                    return String.format("%s -> %s %s (%s, %d) outside=%s\n",
                            sparseMatrixGrammar.nonTermSet.getSymbol(nonterminal),
                            sparseMatrixGrammar.nonTermSet.getSymbol(leftChild),
                            sparseMatrixGrammar.nonTermSet.getSymbol(rightChild), JUnit.fraction(insideProbability),
                            midpoint, JUnit.fraction(outsideProbability));
                }
            }

            if (rightChild == Production.UNARY_PRODUCTION) {
                // Unary Production
                return String.format("%s -> %s (%.5f, %d) outside=%.5f\n",
                        sparseMatrixGrammar.nonTermSet.getSymbol(nonterminal),
                        sparseMatrixGrammar.nonTermSet.getSymbol(leftChild), insideProbability, midpoint,
                        outsideProbability);
            } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                // Lexical Production
                return String
                        .format("%s -> %s (%.5f, %d) outside=%.5f\n",
                                sparseMatrixGrammar.nonTermSet.getSymbol(nonterminal),
                                sparseMatrixGrammar.mapLexicalEntry(leftChild), insideProbability, midpoint,
                                outsideProbability);
            } else {
                return String.format("%s -> %s %s (%.5f, %d) outside=%.5f\n",
                        sparseMatrixGrammar.nonTermSet.getSymbol(nonterminal),
                        sparseMatrixGrammar.nonTermSet.getSymbol(leftChild),
                        sparseMatrixGrammar.nonTermSet.getSymbol(rightChild), insideProbability, midpoint,
                        outsideProbability);
            }
        }

        private String maxqToString(final boolean formatFractions) {
            final StringBuilder sb = new StringBuilder(256);

            for (short baseNt = 0; baseNt < maxcVocabulary.size(); baseNt++) {
                if (maxQ[cellIndex][baseNt] != Float.NEGATIVE_INFINITY) {

                    sb.append(formatCellEntry(baseNt, maxQLeftChildren[cellIndex][baseNt],
                            maxQRightChildren[cellIndex][baseNt], maxQ[cellIndex][baseNt],
                            maxQMidpoints[cellIndex][baseNt], formatFractions));
                }
            }
            return sb.toString();
        }

        private String formatCellEntry(final short nonterminal, final short leftChild, final short rightChild,
                final float score, final short midpoint, final boolean formatFractions) {

            if (formatFractions) {
                if (rightChild == Production.UNARY_PRODUCTION) {
                    // Unary Production
                    return String.format("%s -> %s (%s, %d)\n", maxcVocabulary.getSymbol(nonterminal),
                            maxcVocabulary.getSymbol(leftChild), JUnit.fraction(score), midpoint);
                } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                    // Lexical Production
                    return String.format("%s -> %s (%s, %d)\n", maxcVocabulary.getSymbol(nonterminal),
                            sparseMatrixGrammar.mapLexicalEntry(tokens[midpoint - 1]), JUnit.fraction(score), midpoint);
                } else {
                    return String.format("%s -> %s %s (%s, %d)\n", maxcVocabulary.getSymbol(nonterminal),
                            maxcVocabulary.getSymbol(leftChild), maxcVocabulary.getSymbol(rightChild),
                            JUnit.fraction(score), midpoint);
                }
            }

            if (rightChild == Production.UNARY_PRODUCTION) {
                // Unary Production
                return String.format("%s -> %s (%.5f, %d)\n", maxcVocabulary.getSymbol(nonterminal),
                        maxcVocabulary.getSymbol(leftChild), score, midpoint);
            } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                // Lexical Production
                return String.format("%s -> %s (%.5f, %d)\n", maxcVocabulary.getSymbol(nonterminal),
                        sparseMatrixGrammar.mapLexicalEntry(tokens[midpoint - 1]), score, midpoint);
            } else {
                return String.format("%s -> %s %s (%.5f, %d)\n", maxcVocabulary.getSymbol(nonterminal),
                        maxcVocabulary.getSymbol(leftChild), maxcVocabulary.getSymbol(rightChild), score, midpoint);
            }
        }
    }
}
