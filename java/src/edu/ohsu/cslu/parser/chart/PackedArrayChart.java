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
package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.Tree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.Vocabulary;
import edu.ohsu.cslu.lela.ConstrainingChart;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Stores a chart in a 4-way parallel array of:
 * <ol>
 * <li>Populated non-terminals (short)</li>
 * <li>Probabilities of those non-terminals (float)</li>
 * <li>Child pairs producing each non-terminal --- equivalent to the grammar rule (int)</li>
 * <li>Midpoints (short)</li>
 * </ol>
 * 
 * Those 4 pieces of information allow us to back-trace through the chart and construct the parse tree.
 * 
 * Each parallel array entry consumes 2 + 4 + 4 + 2 = 12 bytes
 * 
 * Individual cells in the parallel array are indexed by cell offsets of fixed length (the number of non-terminals in
 * the grammar).
 * 
 * The ancillary data structures are relatively small, so the total size consumed is approximately = n * (n-1) / 2 * V *
 * 12 bytes.
 * 
 * Similar to {@link DenseVectorChart}, but observed non-terminals are packed together in
 * {@link PackedArrayChartCell#finalizeCell()}; this packing scan and the resulting denser access to observed
 * non-terminals may prove beneficial on certain architectures.
 * 
 * Other information, including outside probabilities is optionally retained to support alternate decoding methods,
 * including max-rule (Petrov, 2007) and Minimum-Bayes-Risk (Goodman, 1996; Hollingshead and Roark, 2007).
 * 
 * @see DenseVectorChart
 * 
 * @author Aaron Dunlop
 * @since March 25, 2010
 */
public class PackedArrayChart extends ParallelArrayChart {

    /**
     * Parallel array storing non-terminals (parallel to {@link ParallelArrayChart#insideProbabilities},
     * {@link ParallelArrayChart#packedChildren}, and {@link ParallelArrayChart#midpoints}. Entries for each cell begin
     * at indices from {@link #cellOffsets}.
     */
    public final short[] nonTerminalIndices;

    /**
     * The number of non-terminals populated in each cell. Indexed by cell index ({@link #cellIndex(int, int)} ).
     */
    public final int[] numNonTerminals;

    /**
     * The index in the main chart array of the first non-terminal in each cell which is valid as a left child. Indexed
     * by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] minLeftChildIndex;

    /**
     * The index in the main chart array of the last non-terminal in each cell which is valid as a left child. Indexed
     * by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] maxLeftChildIndex;

    /**
     * The index in the main chart array of the last non-terminal in each cell which is valid as a right child. Indexed
     * by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] minRightChildIndex;

    /**
     * The index in the main chart array of the last non-terminal in each cell which is valid as a right child. Indexed
     * by cell index ({@link #cellIndex(int, int)}).
     */
    private final int[] maxRightChildIndex;

    /** The number of 'segments' dividing left children for multi-threading cartesian-product operation. */
    private final int leftChildSegments;

    /**
     * Indices in the main chart array of each 'segment', dividing left children for multi-threading cartesian-product
     * operation. Indexed by cellIndex * (segment count + 1) + segment num. e.g. if each left cell is divided into 4
     * segments ({@link #leftChildSegments} == 4), segment 3 of cell 12 will begin at
     * {@link #leftChildSegmentStartIndices}[12 * 5 + 3] and end at {@link #leftChildSegmentStartIndices} [12 * 5 + 4].
     */
    public final int[] leftChildSegmentStartIndices;

    /**
     * Stores outside probabilities for each nonterminal, when doing inside-outside inference. Parallel array to
     * {@link ParallelArrayChart#insideProbabilities}, etc.
     */
    public final float[] outsideProbabilities;

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

    protected final ThreadLocal<PackedArrayChart.TemporaryChartCell> threadLocalTemporaryCells;

    /**
     * Constructs a chart
     * 
     * @param parseTask Current task
     * @param sparseMatrixGrammar Grammar
     * @param beamWidth
     * @param lexicalRowBeamWidth
     * @param leftChildSegments The number of 'segments' to split left children into; used to multi-thread
     *            cartesian-product operation.
     */
    public PackedArrayChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar,
            final int beamWidth, final int lexicalRowBeamWidth, final int leftChildSegments) {
        super(parseTask, sparseMatrixGrammar, Math.min(beamWidth, sparseMatrixGrammar.numNonTerms()), Math.min(
                lexicalRowBeamWidth, sparseMatrixGrammar.numNonTerms()));

        numNonTerminals = new int[cells];
        minLeftChildIndex = new int[cells];
        maxLeftChildIndex = new int[cells];
        minRightChildIndex = new int[cells];
        maxRightChildIndex = new int[cells];

        nonTerminalIndices = new short[chartArraySize];

        this.leftChildSegments = leftChildSegments;
        if (leftChildSegments > 0) {
            leftChildSegmentStartIndices = new int[(cells * (leftChildSegments + 1)) + 1];
        } else {
            leftChildSegmentStartIndices = null;
        }
        reset(parseTask);

        // Temporary cell storage for each cell-level thread
        this.threadLocalTemporaryCells = new ThreadLocal<PackedArrayChart.TemporaryChartCell>() {
            @Override
            protected PackedArrayChart.TemporaryChartCell initialValue() {
                return new PackedArrayChart.TemporaryChartCell(grammar);
            }
        };

        this.outsideProbabilities = new float[chartArraySize];

        switch (parseTask.decodeMethod) {

        case Goodman:
        case SplitSum:
            final int maxcArraySize = parseTask.tokens.length * (parseTask.tokens.length + 1) / 2;
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

        case ViterbiMax:
            this.maxQ = null;
            this.maxQMidpoints = null;
            this.maxQLeftChildren = null;
            this.maxQRightChildren = null;
            this.maxcEntries = null;
            this.maxcScores = null;
            this.maxcMidpoints = null;
            this.maxcUnaryChildren = null;
            break;

        default:
            throw new UnsupportedOperationException("Unsupported decoding method: " + parseTask.decodeMethod);
        }

    }

    /**
     * Constructs a chart
     * 
     * @param parseTask Current task
     * @param sparseMatrixGrammar Grammar
     * @param beamWidth
     * @param lexicalRowBeamWidth
     */
    public PackedArrayChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar,
            final int beamWidth, final int lexicalRowBeamWidth) {
        this(parseTask, sparseMatrixGrammar, beamWidth, lexicalRowBeamWidth, 0);
    }

    /**
     * Constructs a chart for exhaustive inference.
     * 
     * @param parseTask Current task
     * @param sparseMatrixGrammar Grammar
     */
    public PackedArrayChart(final ParseTask parseTask, final SparseMatrixGrammar sparseMatrixGrammar) {
        this(parseTask, sparseMatrixGrammar, sparseMatrixGrammar.numNonTerms(), sparseMatrixGrammar.numNonTerms(), 0);
    }

    /**
     * Constructs a chart for constrained parsing (see {@link ConstrainingChart}.
     * 
     * @param size
     * @param chartArraySize
     * @param sparseMatrixGrammar
     */
    protected PackedArrayChart(final int size, final int chartArraySize, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(size, chartArraySize, sparseMatrixGrammar);

        this.nonTerminalIndices = new short[chartArraySize];
        Arrays.fill(nonTerminalIndices, Short.MIN_VALUE);

        this.threadLocalTemporaryCells = null;
        this.numNonTerminals = new int[cells];
        this.minLeftChildIndex = null;
        this.minRightChildIndex = null;
        this.maxLeftChildIndex = null;
        this.maxRightChildIndex = null;
        this.leftChildSegments = 0;
        this.leftChildSegmentStartIndices = null;

        this.outsideProbabilities = null;

        this.maxcScores = null;
        this.maxcEntries = null;
        this.maxcUnaryChildren = null;

        this.maxQ = null;
        this.maxQLeftChildren = null;
        this.maxQRightChildren = null;
        this.maxQMidpoints = null;
        this.maxcMidpoints = null;
    }

    @Override
    public void reset(final ParseTask task) {
        this.parseTask = task;
        this.size = task.sentenceLength();
        Arrays.fill(numNonTerminals, 0);
        if (leftChildSegmentStartIndices != null) {
            Arrays.fill(leftChildSegmentStartIndices, 0);
        }

        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end <= size; end++) {
                final int cellIndex = cellIndex(start, end);
                final int offset = cellOffset(start, end);

                cellOffsets[cellIndex] = offset;
                minLeftChildIndex[cellIndex] = offset;
                maxLeftChildIndex[cellIndex] = offset - 1;
                minRightChildIndex[cellIndex] = offset;
                maxRightChildIndex[cellIndex] = offset - 1;
            }
        }
    }

    @Override
    public BinaryTree<String> extractBestParse(final int start, final int end, final int parent) {
        final PackedArrayChartCell packedCell = getCell(start, end);

        if (packedCell == null) {
            return null;
        }

        // Find the index of the non-terminal in the chart storage
        final int i = Arrays.binarySearch(nonTerminalIndices, packedCell.offset, packedCell.offset
                + numNonTerminals[packedCell.cellIndex], (short) parent);
        if (i < 0) {
            return null;
        }
        final int edgeChildren = packedChildren[i];
        final short edgeMidpoint = midpoints[i];

        final BinaryTree<String> subtree = new BinaryTree<String>(sparseMatrixGrammar.nonTermSet.getSymbol(parent));
        final int leftChild = sparseMatrixGrammar.packingFunction().unpackLeftChild(edgeChildren);
        final int rightChild = sparseMatrixGrammar.packingFunction().unpackRightChild(edgeChildren);

        if (rightChild == Production.UNARY_PRODUCTION) {
            subtree.addChild(extractBestParse(start, end, leftChild));

        } else if (rightChild == Production.LEXICAL_PRODUCTION) {
            subtree.addChild(new BinaryTree<String>(sparseMatrixGrammar.lexSet.getSymbol(parseTask.tokens[start])));

        } else {
            // binary production
            subtree.addChild(extractBestParse(start, edgeMidpoint, leftChild));
            subtree.addChild(extractBestParse(edgeMidpoint, end, rightChild));
        }
        return subtree;
    }

    @Override
    public float getInside(final int start, final int end, final int nonTerminal) {
        final int cellIndex = cellIndex(start, end);
        final int offset = offset(cellIndex);
        final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                (short) nonTerminal);
        if (index < 0) {
            return Float.NEGATIVE_INFINITY;
        }
        return insideProbabilities[index];
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

        case ViterbiMax:
            // TODO Rename extractBestParse to extractViterbiParse
            return extractBestParse(grammar.startSymbol);

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
                                    + cscGrammar.lexicalLogProbability(parent, parseTask.tokens[start]));
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
            subtree.addChild(new BinaryTree<String>(sparseMatrixGrammar.lexSet.getSymbol(parseTask.tokens[start])));

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

    @Override
    public void updateInside(final int start, final int end, final int nonTerminal, final float insideProb) {
        throw new UnsupportedOperationException("Not supported by PackedArrayChart");
    }

    @Override
    public PackedArrayChartCell getCell(final int start, final int end) {
        return new PackedArrayChartCell(start, end);
    }

    public final int minLeftChildIndex(final int cellIndex) {
        return minLeftChildIndex[cellIndex];
    }

    public final int maxLeftChildIndex(final int cellIndex) {
        return maxLeftChildIndex[cellIndex];
    }

    public final int minRightChildIndex(final int cellIndex) {
        return minRightChildIndex[cellIndex];
    }

    public final int maxRightChildIndex(final int cellIndex) {
        return maxRightChildIndex[cellIndex];
    }

    public int[] numNonTerminals() {
        return numNonTerminals;
    }

    // Added by Nate
    public int cellNonTermStartIndex(final short start, final short end) {
        return offset(cellIndex(start, end));
    }

    public int cellNonTermEndIndex(final short start, final short end) {
        return cellNonTermStartIndex(start, end) + numNonTerminals()[cellIndex(start, end)] - 1;
    }

    public class PackedArrayChartCell extends ParallelArrayChartCell {

        public TemporaryChartCell tmpCell;

        public float[] tmpOutsideProbabilities;

        public PackedArrayChartCell(final int start, final int end) {
            super(start, end);
        }

        /**
         * Copy and pack entries from temporary array into the main chart array
         */
        @Override
        public void finalizeCell() {

            if (tmpCell == null) {
                return;
            }

            // Copy all populated entries from temporary storage
            boolean foundMinLeftChild = false, foundMinRightChild = false;
            int nonTerminalOffset = offset;

            minLeftChildIndex[cellIndex] = offset;
            maxLeftChildIndex[cellIndex] = offset - 1;
            minRightChildIndex[cellIndex] = offset;
            maxRightChildIndex[cellIndex] = offset - 1;

            for (short nonTerminal = 0; nonTerminal < tmpCell.insideProbabilities.length; nonTerminal++) {

                if (tmpCell.insideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {

                    nonTerminalIndices[nonTerminalOffset] = nonTerminal;
                    insideProbabilities[nonTerminalOffset] = tmpCell.insideProbabilities[nonTerminal];
                    packedChildren[nonTerminalOffset] = tmpCell.packedChildren[nonTerminal];
                    midpoints[nonTerminalOffset] = tmpCell.midpoints[nonTerminal];

                    if (sparseMatrixGrammar.isValidLeftChild(nonTerminal)) {
                        if (!foundMinLeftChild) {
                            minLeftChildIndex[cellIndex] = nonTerminalOffset;
                            foundMinLeftChild = true;
                        }
                        maxLeftChildIndex[cellIndex] = nonTerminalOffset;
                    }

                    if (sparseMatrixGrammar.isValidRightChild(nonTerminal)) {
                        if (!foundMinRightChild) {
                            minRightChildIndex[cellIndex] = nonTerminalOffset;
                            foundMinRightChild = true;
                        }
                        maxRightChildIndex[cellIndex] = nonTerminalOffset;
                    }

                    nonTerminalOffset++;
                }
            }

            numNonTerminals[cellIndex] = nonTerminalOffset - offset;
            this.tmpCell = null;
            finalizeSegmentStartIndices();
        }

        /**
         * Special-case to populate a cell with a single entry without a linear search of the temporary storage
         * 
         * @param entryNonTerminal
         * @param entryInsideProbability
         * @param entryPackedChildren
         * @param entryMidpoint
         */
        public void finalizeCell(final short entryNonTerminal, final float entryInsideProbability,
                final int entryPackedChildren, final short entryMidpoint) {

            minLeftChildIndex[cellIndex] = offset;
            maxLeftChildIndex[cellIndex] = offset - 1;
            minRightChildIndex[cellIndex] = offset;
            maxRightChildIndex[cellIndex] = offset - 1;

            nonTerminalIndices[offset] = entryNonTerminal;
            insideProbabilities[offset] = entryInsideProbability;
            packedChildren[offset] = entryPackedChildren;
            midpoints[offset] = entryMidpoint;

            if (sparseMatrixGrammar.isValidLeftChild(entryNonTerminal)) {
                maxLeftChildIndex[cellIndex] = offset;
            }

            if (sparseMatrixGrammar.isValidRightChild(entryNonTerminal)) {
                maxRightChildIndex[cellIndex] = offset;
            }

            numNonTerminals[cellIndex] = 1;
            this.tmpCell = null;
            // TODO We could probably special-case this as well
            finalizeSegmentStartIndices();
        }

        /**
         * Special-case to finalize an empty cell
         */
        public void finalizeEmptyCell() {

            minLeftChildIndex[cellIndex] = offset;
            maxLeftChildIndex[cellIndex] = offset - 1;
            minRightChildIndex[cellIndex] = offset;
            maxRightChildIndex[cellIndex] = offset - 1;

            numNonTerminals[cellIndex] = 0;
            this.tmpCell = null;
            // TODO We could probably special-case this as well
            finalizeSegmentStartIndices();
        }

        private void finalizeSegmentStartIndices() {
            if (leftChildSegmentStartIndices != null) {
                // Split up the left-child non-terminals into 'segments' for multi-threading of cartesian
                // product operation.

                // The cell population is likely to be biased toward a specific range of non-terminals, but we
                // still have to use fixed segment boundaries (instead of splitting the actual population range
                // equally) so that individual x-product threads can operate on different regions of the x-product
                // vector without interfering with one another.

                // We use equal segment ranges, with the exception of POS. POS will occur only in one midpoint
                // per cell. Since many non-terms are POS (particularly in latent-variable grammars) and the threads
                // allocated to POS would be mostly idle, we include all POS in the segment containing the first segment
                // containing POS.
                final int cellSegmentStartIndex = cellIndex * (leftChildSegments + 1);
                leftChildSegmentStartIndices[cellSegmentStartIndex] = minLeftChildIndex[cellIndex];

                final int segmentSize = (sparseMatrixGrammar.numNonTerms() - sparseMatrixGrammar.numPosSymbols)
                        / leftChildSegments + 1;
                int i = 0;

                int segmentEndNT = segmentSize;
                if (segmentEndNT >= sparseMatrixGrammar.posStart && segmentEndNT <= sparseMatrixGrammar.posEnd) {
                    segmentEndNT += sparseMatrixGrammar.posEnd - sparseMatrixGrammar.posStart + 1;
                }
                for (int j = minLeftChildIndex[cellIndex]; i < (leftChildSegments - 1)
                        && j <= maxLeftChildIndex[cellIndex]; j++) {
                    while (nonTerminalIndices[j] >= segmentEndNT) {
                        i++;
                        leftChildSegmentStartIndices[cellSegmentStartIndex + i] = j;
                        segmentEndNT += segmentSize;
                        if (segmentEndNT >= sparseMatrixGrammar.posStart && segmentEndNT <= sparseMatrixGrammar.posEnd) {
                            segmentEndNT += sparseMatrixGrammar.posEnd - sparseMatrixGrammar.posStart + 1;
                        }
                    }
                }
                Arrays.fill(leftChildSegmentStartIndices, cellSegmentStartIndex + i + 1, cellSegmentStartIndex
                        + leftChildSegments + 1, maxLeftChildIndex[cellIndex] + 1);
            }
        }

        @Override
        public float getInside(final int nonTerminal) {
            if (tmpCell != null) {
                return tmpCell.insideProbabilities[nonTerminal];
            }

            final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                    (short) nonTerminal);
            if (index < 0) {
                return Float.NEGATIVE_INFINITY;
            }
            return insideProbabilities[index];
        }

        public short getMidpoint(final short nonTerminal) {
            if (tmpCell != null) {
                return tmpCell.midpoints[nonTerminal];
            }

            final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                    nonTerminal);
            if (index < 0) {
                return -1;
            }
            return midpoints[index];
        }

        @Override
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProbability) {
            // Allow update of cells created without temporary storage, even though this will be inefficient;
            // it should be rare, and is at least useful for unit testing.
            allocateTemporaryStorage();

            final int parent = p.parent;
            numEdgesConsidered++;

            if (insideProbability > tmpCell.insideProbabilities[p.parent]) {
                if (p.isBinaryProd()) {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().pack((short) p.leftChild,
                            (short) p.rightChild);
                } else if (p.isLexProd()) {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().packLexical(p.leftChild);
                } else {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().packUnary(
                            (short) p.leftChild);
                }
                tmpCell.insideProbabilities[parent] = insideProbability;

                // Midpoint == end for unary productions
                tmpCell.midpoints[parent] = leftCell.end();

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

            if (edge.inside() > tmpCell.insideProbabilities[parent]) {

                if (edge.prod.isBinaryProd()) {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().pack(
                            (short) edge.prod.leftChild, (short) edge.prod.rightChild);
                } else if (edge.prod.isLexProd()) {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().packLexical(
                            edge.prod.leftChild);
                } else {
                    tmpCell.packedChildren[parent] = sparseMatrixGrammar.packingFunction().packUnary(
                            (short) edge.prod.leftChild);
                }
                tmpCell.insideProbabilities[parent] = edge.inside();

                // Midpoint == end for unary productions
                tmpCell.midpoints[parent] = edge.leftCell.end();

                numEdgesAdded++;
            }
        }

        @Override
        public ChartEdge getBestEdge(final int nonTerminal) {
            int edgeChildren;
            short edgeMidpoint;

            if (tmpCell != null) {
                if (tmpCell.insideProbabilities[nonTerminal] == Float.NEGATIVE_INFINITY) {
                    return null;
                }

                edgeChildren = tmpCell.packedChildren[nonTerminal];
                edgeMidpoint = tmpCell.midpoints[nonTerminal];

            } else {
                final int index = Arrays.binarySearch(nonTerminalIndices, offset, offset + numNonTerminals[cellIndex],
                        (short) nonTerminal);
                if (index < 0) {
                    return null;
                }
                edgeChildren = packedChildren[index];
                edgeMidpoint = midpoints[index];
            }

            final int leftChild = sparseMatrixGrammar.packingFunction().unpackLeftChild(edgeChildren);
            final int rightChild = sparseMatrixGrammar.packingFunction().unpackRightChild(edgeChildren);
            final PackedArrayChartCell leftChildCell = getCell(start(), edgeMidpoint);
            final PackedArrayChartCell rightChildCell = edgeMidpoint < end ? (PackedArrayChartCell) getCell(
                    edgeMidpoint, end) : null;

            Production p;
            if (rightChild == Production.LEXICAL_PRODUCTION) {
                final float probability = sparseMatrixGrammar.lexicalLogProbability((short) nonTerminal, leftChild);
                p = new Production(nonTerminal, leftChild, probability, true, sparseMatrixGrammar);

            } else if (rightChild == Production.UNARY_PRODUCTION) {
                final float probability = sparseMatrixGrammar.unaryLogProbability((short) nonTerminal,
                        (short) leftChild);
                p = new Production(nonTerminal, leftChild, probability, false, sparseMatrixGrammar);

            } else {
                final float probability = sparseMatrixGrammar.binaryLogProbability((short) nonTerminal, edgeChildren);
                p = new Production(nonTerminal, leftChild, rightChild, probability, sparseMatrixGrammar);
            }
            return new ChartEdge(p, leftChildCell, rightChildCell);
        }

        @Override
        public int getNumNTs() {
            if (tmpCell != null) {
                int count = 0;
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
                    if (tmpCell.insideProbabilities[nt] != Float.NEGATIVE_INFINITY) {
                        count++;
                    }
                }
                return count;
            }

            return numNonTerminals[cellIndex(start, end)];
        }

        @Override
        public int getNumUnfactoredNTs() {
            if (tmpCell != null) {
                int count = 0;
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
                    if (tmpCell.insideProbabilities[nt] != Float.NEGATIVE_INFINITY
                            && !grammar.nonTermSet.isFactored(nt)) {
                        count++;
                    }
                }
                return count;
            }

            int count = 0;
            for (int i = offset; i < offset + numNonTerminals[cellIndex(start, end)]; i++) {
                if (!grammar.nonTermSet.isFactored(nonTerminalIndices[i])) {
                    count++;
                }
            }
            return count;
        }

        public int leftChildren() {
            if (tmpCell != null) {
                int count = 0;
                for (int i = 0; i < tmpCell.insideProbabilities.length; i++) {
                    if (tmpCell.insideProbabilities[i] != Float.NEGATIVE_INFINITY
                            && sparseMatrixGrammar.isValidLeftChild(i)) {
                        count++;
                    }
                }
                return count;
            }

            return maxLeftChildIndex[cellIndex(start, end)] - minLeftChildIndex[cellIndex(start, end)] + 1;
        }

        public int rightChildren() {
            if (tmpCell != null) {
                int count = 0;
                for (int i = 0; i < tmpCell.insideProbabilities.length; i++) {
                    if (tmpCell.insideProbabilities[i] != Float.NEGATIVE_INFINITY
                            && sparseMatrixGrammar.isValidRightChild(i)) {
                        count++;
                    }
                }
                return count;
            }

            return maxRightChildIndex[cellIndex(start, end)] - minRightChildIndex[cellIndex(start, end)] + 1;
        }

        /**
         * Returns the index of the first non-terminal in this cell which is valid as a left child. The grammar must be
         * sorted right, both, left, unary-only, as in {@link Grammar}.
         * 
         * @return the index of the first non-terminal in this cell which is valid as a left child.
         */
        public final int minLeftChildIndex() {
            return minLeftChildIndex[cellIndex];
        }

        /**
         * Returns the index of the last non-terminal in this cell which is valid as a left child. The grammar must be
         * sorted right, both, left, unary-only, as in {@link Grammar}.
         * 
         * @return the index of the last non-terminal in this cell which is valid as a left child.
         */
        public final int maxLeftChildIndex() {
            return maxLeftChildIndex[cellIndex];
        }

        /**
         * Returns the index of the last non-terminal in this cell which is valid as a right child. The grammar must be
         * sorted right, both, left, unary-only, as in {@link Grammar}.
         * 
         * @return the index of the last non-terminal in this cell which is valid as a right child.
         */
        public final int maxRightChildIndex() {
            return maxRightChildIndex[cellIndex];
        }

        /**
         * Warning: Not truly thread-safe, since it doesn't validate that the two cells belong to the same chart.
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof PackedArrayChartCell)) {
                return false;
            }

            final PackedArrayChartCell packedArrayChartCell = (PackedArrayChartCell) o;
            return (packedArrayChartCell.start == start && packedArrayChartCell.end == end);
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public void allocateTemporaryStorage() {
            // Allocate storage
            if (tmpCell == null) {
                // this.tmpCell = threadLocalTemporaryCells.get();
                // this.tmpCell.clear();
                this.tmpCell = new TemporaryChartCell(grammar);

                // Copy from main chart array to temporary parallel array
                for (int i = offset; i < offset + numNonTerminals[cellIndex]; i++) {
                    final int nonTerminal = nonTerminalIndices[i];
                    tmpCell.packedChildren[nonTerminal] = packedChildren[i];
                    tmpCell.insideProbabilities[nonTerminal] = insideProbabilities[i];
                    tmpCell.midpoints[nonTerminal] = midpoints[i];
                }
            }

            if (parseTask.decodeMethod != DecodeMethod.ViterbiMax) {
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
        }

        @Override
        public String toString(final boolean formatFractions) {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("PackedArrayChartCell[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + sparseMatrixGrammar.numNonTerms() + ") edges\n");

            switch (parseTask.decodeMethod) {

            case ViterbiMax:
                return viterbiToString(false);

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

        public String viterbiToString(final boolean formatFractions) {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("PackedArrayChartCell[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + sparseMatrixGrammar.numNonTerms() + ") edges\n");

            if (tmpCell == null) {
                // Format entries from the main chart array
                for (int index = offset; index < offset + numNonTerminals[cellIndex]; index++) {
                    final int childProductions = packedChildren[index];
                    final float insideProbability = insideProbabilities[index];
                    final int midpoint = midpoints[index];

                    final int nonTerminal = nonTerminalIndices[index];

                    sb.append(ParallelArrayChart.formatCellEntry(sparseMatrixGrammar, nonTerminal, childProductions,
                            insideProbability, midpoint, formatFractions));
                }

            } else {
                // Format entries from temporary cell storage
                for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {

                    if (tmpCell.insideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                        sb.append(ParallelArrayChart.formatCellEntry(sparseMatrixGrammar, nonTerminal,
                                tmpCell.packedChildren[nonTerminal], tmpCell.insideProbabilities[nonTerminal],
                                tmpCell.midpoints[nonTerminal], formatFractions));
                    }
                }
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

            final int leftChild = sparseMatrixGrammar.packingFunction().unpackLeftChild(childProductions);
            final int rightChild = sparseMatrixGrammar.packingFunction().unpackRightChild(childProductions);

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
                            sparseMatrixGrammar.mapLexicalEntry(parseTask.tokens[midpoint - 1]), JUnit.fraction(score),
                            midpoint);
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
                        sparseMatrixGrammar.mapLexicalEntry(parseTask.tokens[midpoint - 1]), score, midpoint);
            } else {
                return String.format("%s -> %s %s (%.5f, %d)\n", maxcVocabulary.getSymbol(nonterminal),
                        maxcVocabulary.getSymbol(leftChild), maxcVocabulary.getSymbol(rightChild), score, midpoint);
            }
        }
    }

    public final static class TemporaryChartCell {

        public final int[] packedChildren;
        public final float[] insideProbabilities;
        public final short[] midpoints;
        private final SparseMatrixGrammar sparseMatrixGrammar;

        public TemporaryChartCell(final Grammar grammar) {
            this.packedChildren = new int[grammar.numNonTerms()];
            this.insideProbabilities = new float[grammar.numNonTerms()];
            this.midpoints = new short[grammar.numNonTerms()];
            this.sparseMatrixGrammar = grammar instanceof SparseMatrixGrammar ? (SparseMatrixGrammar) grammar : null;
            clear();
        }

        public void clear() {
            Arrays.fill(insideProbabilities, Float.NEGATIVE_INFINITY);
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(final boolean formatFractions) {
            final StringBuilder sb = new StringBuilder(128);

            for (int nonTerminal = 0; nonTerminal < sparseMatrixGrammar.numNonTerms(); nonTerminal++) {

                if (insideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                    sb.append(formatCellEntry(sparseMatrixGrammar, nonTerminal, packedChildren[nonTerminal],
                            insideProbabilities[nonTerminal], midpoints[nonTerminal], formatFractions));
                }
            }

            return sb.toString();
        }
    }
}
