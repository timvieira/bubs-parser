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
package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.datastructs.vectors.PackedBitVector;
import edu.ohsu.cslu.grammar.CoarseGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.TreeTools;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.util.Strings;

/**
 * Implements Caraballo and Charniak's (1998) boundary in-out figure-of-merit with ambiguous POS tags by running the
 * forward/backward algorithm.
 * 
 * @author Nathan Bodenstab
 */
public final class BoundaryPosModel extends FigureOfMeritModel {

    private Grammar grammar;
    private CoarseGrammar coarseGrammar = null;

    // Model params learned from training data
    private final float leftBoundaryLogProb[][], rightBoundaryLogProb[][], posTransitionLogProb[][];
    /**
     * Labels parts-of-speech for which all boundary probabilities or transition probabilities are 0, allowing us to
     * short-circuit loops during sentence initialization.
     */
    private final PackedBitVector leftBoundaryZeros, rightBoundaryZeros, posTransitionZeros;

    final short nullSymbol;
    final short[] NULL_LIST;
    final float[] NULL_PROBABILITIES;

    public BoundaryPosModel(final FOMType type, final Grammar grammar, final BufferedReader modelStream)
            throws IOException {

        super(type);

        this.grammar = grammar;
        if (grammar instanceof CoarseGrammar) {
            coarseGrammar = (CoarseGrammar) grammar;
        }

        nullSymbol = grammar.nullSymbol();
        NULL_LIST = new short[] { nullSymbol };
        NULL_PROBABILITIES = new float[] { 0f };

        final int numNT = grammar.numNonTerms();
        final int maxPOSIndex = grammar.maxPOSIndex();
        leftBoundaryLogProb = new float[maxPOSIndex + 1][numNT];
        rightBoundaryLogProb = new float[maxPOSIndex + 1][numNT];
        posTransitionLogProb = new float[maxPOSIndex + 1][maxPOSIndex + 1];

        // Init values to log(0) = -Inf. All entries in model are explicitly smoothed
        // so no score should be left with -Inf
        for (int i = 0; i < maxPOSIndex + 1; i++) {
            Arrays.fill(leftBoundaryLogProb[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(rightBoundaryLogProb[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(posTransitionLogProb[i], Float.NEGATIVE_INFINITY);
        }

        this.leftBoundaryZeros = new PackedBitVector(leftBoundaryLogProb.length);
        this.rightBoundaryZeros = new PackedBitVector(rightBoundaryLogProb.length);
        this.posTransitionZeros = new PackedBitVector(posTransitionLogProb.length);

        if (modelStream != null) {
            readModel(modelStream);
        }
    }

    @Override
    public FigureOfMerit createFOM() {
        switch (type) {
        case BoundaryPOS:
            return new BoundaryPosFom(grammar);
        default:
            BaseLogger.singleton().info("ERROR: FOMType " + type + " not supported.");
            System.exit(1);
            return null;
        }
    }

    public void readModel(final BufferedReader inStream) throws IOException {
        String line, numStr, denomStr;
        int numIndex, denomIndex;
        float prob;
        final LinkedList<String> numerator = new LinkedList<String>();
        final LinkedList<String> denom = new LinkedList<String>();
        while ((line = inStream.readLine()) != null) {
            // line format: label num1 num2 ... | den1 den2 ... prob
            final String[] tokens = Strings.splitOnSpace(line);
            if (tokens.length > 0 && !tokens[0].equals("#")) {
                numerator.clear();
                denom.clear();
                boolean foundBar = false;
                for (int i = 1; i < tokens.length - 1; i++) {
                    if (tokens[i].equals("|")) {
                        foundBar = true;
                    } else {
                        if (foundBar == false) {
                            numerator.addLast(tokens[i]);
                        } else {
                            denom.addLast(tokens[i]);
                        }
                    }
                }

                numStr = Util.join(numerator, " ");
                numIndex = grammar.mapNonterminal(numStr);
                if (numIndex < 0) {
                    BaseLogger.singleton().info(
                            "ERROR: non-terminal '" + numStr + "' from FOM model not found in grammar.");
                    System.exit(1);
                }
                denomStr = Util.join(denom, " ");
                denomIndex = grammar.mapNonterminal(denomStr);
                if (denomIndex < 0) {
                    BaseLogger.singleton().info(
                            "ERROR: non-terminal '" + denomStr + "' from FOM model not found in grammar.");
                    System.exit(1);
                }
                prob = Float.parseFloat(tokens[tokens.length - 1]);

                if (tokens[0].equals("#")) {
                    // model meta-data
                    // ex: # model=FOM type=BoundaryInOut boundaryNgramOrder=2 posNgramOrder=2
                } else if (tokens[0].equals("LB")) {
                    leftBoundaryLogProb[denomIndex][numIndex] = prob;
                } else if (tokens[0].equals("RB")) {
                    rightBoundaryLogProb[numIndex][denomIndex] = prob;
                } else if (tokens[0].equals("PN")) {
                    posTransitionLogProb[numIndex][denomIndex] = prob;
                } else {
                    System.err.println("WARNING: ignoring line in model file '" + line + "'");
                }
            }
        }

        // Populate bit vectors marking all-0 probability vectors.
        for (int nt = 0; nt < leftBoundaryLogProb.length; nt++) {
            boolean leftAll0 = true, rightAll0 = true, posTransitionAll0 = true;
            for (int i = 0; i < leftBoundaryLogProb[nt].length; i++) {
                if (leftBoundaryLogProb[nt][i] != Float.NEGATIVE_INFINITY) {
                    leftAll0 = false;
                }
                if (rightBoundaryLogProb[nt][i] != Float.NEGATIVE_INFINITY) {
                    rightAll0 = false;
                }
                if (posTransitionLogProb[nt].length > i && posTransitionLogProb[nt][i] != Float.NEGATIVE_INFINITY) {
                    posTransitionAll0 = false;
                }
            }
            leftBoundaryZeros.set(nt, leftAll0);
            rightBoundaryZeros.set(nt, rightAll0);
            posTransitionZeros.set(nt, posTransitionAll0);
        }
    }

    /**
     * Trains a boundary FOM, from a set of (binarized) training trees including all subcategories
     * 
     * TODO Make training consistent between FigureOfMeritModel implementations - static or not?
     * 
     * @param inStream
     * @param outStream
     * @param grammarFile
     * @param smoothingCount
     * @param writeCounts
     * @param posNgramOrder
     * @throws Exception
     */
    public static void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile,
            final double smoothingCount, final boolean writeCounts, final int posNgramOrder) throws Exception {
        String line, historyStr;
        final String joinString = " ";
        ParseTree tree;
        final SimpleCounterSet<String> leftBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> rightBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> posTransitionCount = new SimpleCounterSet<String>();

        // To train a BoundaryInOut FOM model we need a grammar and
        // binarized gold input trees with NTs from same grammar
        final Grammar grammar = readGrammar(grammarFile, ResearchParserType.ECPCellCrossList, null);

        // TODO: note that we have to have the same training grammar as decoding grammar here
        // so the input needs to be binarized. If we are parsing with the Berkeley latent-variable
        // grammar then we can't work with Gold trees. As an approximation we will take the
        // 1-best from the Berkeley parser output (although constraining the coarse labels to match
        // the true gold tree would probably be a little better)
        // See Caraballo/Charniak 1998 for (what I think is) their inside/outside solution
        // to the same (or a similar) problem.
        while ((line = inStream.readLine()) != null) {
            try {
                tree = ParseTree.readBracketFormat(line);
            } catch (final RuntimeException e) {
                // Skip malformed trees, including INFO output from parse runs
                continue;
            }
            if (tree.isBinaryTree() == false) {
                // System.err.println("ERROR: Training trees must be binarized exactly as used in decoding grammar");
                // System.exit(1);
                TreeTools.binarizeTree(tree, grammar.binarization() == Binarization.RIGHT, grammar.horizontalMarkov,
                        grammar.verticalMarkov, false, grammar.grammarFormat);
            }
            tree.linkLeavesLeftRight();
            for (final ParseTree node : tree.preOrderTraversal()) {

                // leftBoundary = P(N[i:j] | POS[i-1]) = #(N[i:j], POS[i-1]) / #(*[i:*], POS[i-1])
                // riteBoundary = P(POS[j+1] | N[i:j]) = #(N[i:j], POS[j+1]) / #(N[*:j], *[j+1])
                // bi-gram POS model for POS[i-1:j+1]
                // counts we need:
                // -- #(N[i:j], POS[i-1])
                // -- #(*[i:*], POS[i-1]) -- number of times POS occurs just to the left of any span

                if (node.isNonTerminal() == true) {
                    if (grammar.nonTermSet.containsKey(node.contents) == false) {
                        throw new IOException("Nonterminal '" + node.contents
                                + "' in input tree not found in grammar.  Exiting.");
                    }
                    leftBoundaryCount.increment(node.contents,
                            convertNull(node.leftBoundaryPOSContents(), Grammar.nullSymbolStr));
                    rightBoundaryCount.increment(convertNull(node.rightBoundaryPOSContents(), Grammar.nullSymbolStr),
                            node.contents);
                }
            }

            // n-gram POS prob
            final LinkedList<String> history = new LinkedList<String>();
            for (int i = 0; i < posNgramOrder - 1; i++) {
                history.addLast(Grammar.nullSymbolStr); // pad history with nulls (for beginning of string)
            }

            // iterate through POS tags using .rightNeighbor
            for (ParseTree posNode = tree.leftMostPOS(); posNode != null; posNode = posNode.rightNeighbor) {
                if (grammar.nonTermSet.containsKey(posNode.contents) == false) {
                    throw new IOException("Nonterminal '" + posNode.contents
                            + "' in input tree not found in grammar.  Exiting.");
                }
                historyStr = Util.join(history, joinString);
                posTransitionCount.increment(posNode.contents, historyStr);
                history.removeFirst();
                history.addLast(posNode.contents);
            }

            // finish up with final transition to <null>
            historyStr = Util.join(history, joinString);
            posTransitionCount.increment(Grammar.nullSymbolStr, historyStr);
        }

        final int numNT = grammar.numNonTerms();

        // smooth counts
        if (smoothingCount > 0) {
            leftBoundaryCount.smoothAddConst(smoothingCount, grammar.posSet.length);
            rightBoundaryCount.smoothAddConst(smoothingCount, numNT);
            posTransitionCount.smoothAddConst(smoothingCount, grammar.posSet.length);
        }

        // Write model to file
        float score;
        outStream.write("# model=FOM type=BoundaryInOut boundaryNgramOrder=2 posNgramOrder=" + posNgramOrder
                + " smooth=" + smoothingCount + "\n");

        // left boundary = P(NT | POS-1)
        for (final short leftPOSIndex : grammar.posSet) {
            final String posStr = grammar.mapNonterminal(leftPOSIndex);
            for (final short ntIndex : grammar.phraseSet) {
                final String ntStr = grammar.mapNonterminal(ntIndex);
                // final float logProb = leftBoundaryLogProb[ntIndex][leftPOSIndex];
                if (writeCounts) {
                    score = leftBoundaryCount.getCount(ntStr, posStr);
                } else {
                    score = (float) Math.log(leftBoundaryCount.getProb(ntStr, posStr));
                }
                if (score > Float.NEGATIVE_INFINITY) {
                    outStream.write("LB " + ntStr + " | " + posStr + " " + score + "\n");
                }
            }
        }

        // right boundary = P(POS+1 | NT)
        for (final short ntIndex : grammar.phraseSet) {
            final String ntStr = grammar.mapNonterminal(ntIndex);
            for (final short rightPOSIndex : grammar.posSet) {
                final String posStr = grammar.mapNonterminal(rightPOSIndex);
                // final float logProb = rightBoundaryLogProb[rightPOSIndex][ntIndex];
                if (writeCounts) {
                    score = rightBoundaryCount.getCount(posStr, ntStr);
                } else {
                    score = (float) Math.log(rightBoundaryCount.getProb(posStr, ntStr));
                }
                if (score > Float.NEGATIVE_INFINITY) {
                    outStream.write("RB " + posStr + " | " + ntStr + " " + score + "\n");
                }
            }
        }

        // pos n-gram = P(POS | POS-1)
        for (final short histPos : grammar.posSet) {
            final String histPosStr = grammar.mapNonterminal(histPos);
            for (final short pos : grammar.posSet) {
                final String posStr = grammar.mapNonterminal(pos);
                // final float logProb = posTransitionLogProb[pos][histPos];
                if (writeCounts) {
                    score = posTransitionCount.getCount(posStr, histPosStr);
                } else {
                    score = (float) Math.log(posTransitionCount.getProb(posStr, histPosStr));
                }
                if (score > Float.NEGATIVE_INFINITY) {
                    outStream.write("PN " + posStr + " | " + histPosStr + " " + score + "\n");
                }
            }
        }
        outStream.close();
    }

    private static String convertNull(final String nonTerm, final String replacementStr) {
        if (nonTerm == null) {
            return replacementStr;
        }
        return nonTerm;
    }

    public class BoundaryPosFom extends FigureOfMerit {

        private static final long serialVersionUID = 1L;

        // pre-computed left/right FOM outside scores for current sentence
        private float outsideLeft[][], outsideRight[][];
        private short[][] backPointer;
        private float[] scores;
        private float[] prevScores;
        private final short[] grammarPhraseSet;

        // private int bestPOSTag[];
        ParseTask parseTask;

        public BoundaryPosFom(final Grammar grammar) {
            this.grammarPhraseSet = grammar.phraseSet;
        }

        @Override
        public float calcFOM(final int start, final int end, short parent, final float insideProbability) {
            if (coarseGrammar != null) {
                parent = (short) coarseGrammar.fineToCoarseNonTerm(parent);
            }

            // leftIndex and rightIndex have +1 because the outsideLeft and outsideRight arrays
            // are padded with a begin and end <null> value which shifts the entire array to
            // the right by one
            final float outside = outsideLeft[start][parent] + outsideRight[end + 1][parent];
            return insideProbability + outside;
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability;
        }

        // TODO Appears to be unused. Remove?
        public String calcFOMToString(final int start, final int end, final short parent, final float inside) {
            Grammar fineGrammar = grammar;
            short coarseParent = parent;
            if (coarseGrammar != null) {
                coarseParent = (short) coarseGrammar.fineToCoarseNonTerm(parent);
                fineGrammar = coarseGrammar.getFineGrammar();
            }

            final int spanLength = end - start;
            final float outside = outsideLeft[start][coarseParent] + outsideRight[end + 1][coarseParent];
            final float fom = inside + outside;

            String s = "FOM: chart[" + start + "," + end + "]";
            s += " n=" + spanLength;
            // s += " p=" + edge.prod.toString();
            s += " i=" + inside;
            s += " o=" + outside;
            s += " oL[" + start + "][" + fineGrammar.mapNonterminal(parent) + "]=" + outsideLeft[start][coarseParent];
            s += " oR[" + (end + 1) + "][" + fineGrammar.mapNonterminal(parent) + "]="
                    + outsideRight[end + 1][coarseParent];
            s += " fom=" + fom;

            return s;
        }

        /**
         * Computes forward-backward and left/right boundary probs across ambiguous POS tags. Also computes 1-best POS
         * tag sequence based on viterbi-max decoding
         */
        @Override
        public void initSentence(final ParseTask task, final Chart chart) {
            this.parseTask = task;
            final int sentLen = task.sentenceLength();
            final int fbSize = sentLen + 2;
            final int posSize = grammar.maxPOSIndex() + 1;

            if (scores == null) {
                scores = new float[posSize];
                prevScores = new float[posSize];
            }

            if (outsideLeft == null || outsideLeft.length < fbSize) {
                // When we allocate 2-d arrays, make them big enough to handle a slightly longer sentence than the
                // current one
                outsideLeft = new float[fbSize + 10][grammar.numNonTerms()];
                outsideRight = new float[fbSize + 10][grammar.numNonTerms()];
                backPointer = new short[fbSize + 10][posSize];
            } else {
                // A newly-initialized array will already be 0'd, but we need to reinitialize a previously-used array
                for (int i = 0; i < fbSize; i++) {
                    Arrays.fill(backPointer[i], (short) 0);
                }
            }

            // Initialize boundary arrays, including populating start-of-sentence and end-of-sentence probabilities into
            // the leftmost forward-pass and rightmost backward pass arrays
            System.arraycopy(leftBoundaryLogProb[nullSymbol], 0, outsideLeft[0], 0, grammar.numNonTerms());
            Arrays.fill(outsideRight[0], Float.NEGATIVE_INFINITY);
            for (int i = 1; i < fbSize - 1; i++) {
                Arrays.fill(outsideLeft[i], Float.NEGATIVE_INFINITY);
                Arrays.fill(outsideRight[i], Float.NEGATIVE_INFINITY);
            }
            Arrays.fill(outsideLeft[fbSize - 1], Float.NEGATIVE_INFINITY);
            System.arraycopy(rightBoundaryLogProb[nullSymbol], 0, outsideRight[fbSize - 1], 0, grammar.numNonTerms());

            short[] prevPOSList = NULL_LIST;

            // Forward pass
            prevScores[nullSymbol] = 0f;

            for (int fwdIndex = 1; fwdIndex < fbSize; fwdIndex++) {

                // Forward-backward Chart is one off from the parser chart
                final int fwdChartIndex = fwdIndex - 1;

                final short[] lexicalParents = fwdChartIndex >= sentLen ? NULL_LIST : grammar
                        .lexicalParents(task.tokens[fwdChartIndex]);
                final float[] lexicalLogProbabilities = fwdChartIndex >= sentLen ? NULL_PROBABILITIES : grammar
                        .lexicalLogProbabilities(task.tokens[fwdChartIndex]);

                final short[] currentBackpointer = backPointer[fwdIndex];

                for (int i = 0; i < lexicalParents.length; i++) {
                    final short curPOS = lexicalParents[i];
                    if (posTransitionZeros.getBoolean(curPOS)) {
                        continue;
                    }

                    final float posEmissionLogProb = lexicalLogProbabilities[i];
                    final float[] curPosTransitionLogProb = posTransitionLogProb[curPOS];
                    float bestScore = Float.NEGATIVE_INFINITY;
                    short bestPrevPOS = -1;

                    for (final short prevPOS : prevPOSList) {
                        final float score = prevScores[prevPOS] + curPosTransitionLogProb[prevPOS] + posEmissionLogProb;
                        if (score > bestScore) {
                            bestScore = score;
                            bestPrevPOS = prevPOS;
                        }
                    }
                    scores[curPOS] = bestScore;
                    currentBackpointer[curPOS] = bestPrevPOS;
                }

                // compute left outside scores to be used during decoding
                // FOM = outsideLeft[i][A] * inside[i][j][A] * outsideRight[j][A]
                final float[] currentOutsideLeft = outsideLeft[fwdIndex];
                for (final short pos : lexicalParents) {
                    if (leftBoundaryZeros.getBoolean(pos)) {
                        continue;
                    }
                    final float posScore = scores[pos];
                    final float[] posLeftBoundaryLogProb = leftBoundaryLogProb[pos];

                    for (final short nonTerm : grammarPhraseSet) {
                        final float score = posScore + posLeftBoundaryLogProb[nonTerm];
                        if (score > currentOutsideLeft[nonTerm]) {
                            currentOutsideLeft[nonTerm] = score;
                        }
                    }
                }

                final float[] tmp = prevScores;
                prevScores = scores;
                scores = tmp;
                prevPOSList = lexicalParents;
            }

            // Backward pass
            prevPOSList = NULL_LIST;
            prevScores[nullSymbol] = 0f;

            for (int bkwIndex = fbSize - 2; bkwIndex >= 0; bkwIndex--) {

                // If we were to reverse the array iteration order, we could eliminate this call by computing
                // 'bestScore' like we do above. But that would mess up cache enough to be a net loss
                Arrays.fill(scores, Float.NEGATIVE_INFINITY);

                final int bkwChartIndex = bkwIndex - 1;
                final short[] lexicalParents = bkwChartIndex < 0 ? NULL_LIST : grammar
                        .lexicalParents(task.tokens[bkwChartIndex]);
                final float[] lexicalLogProbabilities = bkwChartIndex < 0 ? NULL_PROBABILITIES : grammar
                        .lexicalLogProbabilities(task.tokens[bkwChartIndex]);

                for (final short prevPOS : prevPOSList) {
                    if (posTransitionZeros.getBoolean(prevPOS)) {
                        continue;
                    }
                    final float prevScore = prevScores[prevPOS];
                    final float[] prevPosTransitionLogProb = posTransitionLogProb[prevPOS];
                    for (int i = 0; i < lexicalParents.length; i++) {
                        final short curPOS = lexicalParents[i];
                        final float score = prevScore + prevPosTransitionLogProb[curPOS] + lexicalLogProbabilities[i];
                        if (score > scores[curPOS]) {
                            scores[curPOS] = score;
                        }
                    }
                }

                // compute right outside scores to be used during decoding
                // FOM = outsideLeft[i][A] * inside[i][j][A] * outsideRight[j][A]
                final float[] currentOutsideRight = outsideRight[bkwIndex];
                for (final short pos : lexicalParents) {
                    if (rightBoundaryZeros.getBoolean(pos)) {
                        continue;
                    }
                    final float posScore = scores[pos];
                    final float[] posRightBoundaryLogProb = rightBoundaryLogProb[pos];

                    for (final short nonTerm : grammarPhraseSet) {
                        final float score = posScore + posRightBoundaryLogProb[nonTerm];
                        if (score > currentOutsideRight[nonTerm]) {
                            currentOutsideRight[nonTerm] = score;
                        }
                    }
                }

                final float[] tmp = prevScores;
                prevScores = scores;
                scores = tmp;
                prevPOSList = lexicalParents;
            }

            // tags from parseTask.tags are used for chart cell feature extraction when
            // using BoundaryInOut FOM. If parseFromInputTags is true, then the tags
            // from the input will already be in place. Otherwise, fill in the tags array
            // with the 1-best result from this forward-backwards run.
            if (ParserDriver.parseFromInputTags == false) {
                parseTask.posTags = new short[sentLen];
                // track backpointers to extract best POS sequence
                // start at the end of the sentence with the nullSymbol and trace backwards
                short bestPOS = nullSymbol;
                for (int i = sentLen - 1; i >= 0; i--) {
                    bestPOS = backPointer[i + 2][bestPOS];
                    parseTask.posTags[i] = grammar.posIndexMap[bestPOS];
                }
            }
        }
    }

    // for Beam-Width Prediction, we need the 1-best POS tags from the chart. And
    // to get these, we need to run the forward-backward algorithm with a model
    // that has POS transition probabilities. Right now this is only done for the
    // Boundary FOM, so instead of writing lots more code and creating new model files,
    // we are simply hi-jacking all of that and overwriting the calcFOM() function to
    // ignore most of the work that is done during setup.
    public class InsideWithFwdBkwd extends BoundaryPosFom {

        private static final long serialVersionUID = 1L;

        public InsideWithFwdBkwd() {
            super(grammar);
        }

        @Override
        public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
            return insideProbability;
        }
    }
}
