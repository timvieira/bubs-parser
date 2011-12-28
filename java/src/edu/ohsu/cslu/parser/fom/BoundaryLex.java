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
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.fom.FigureOfMerit.FOMType;

/**
 * Implements Caraballo and Charniak's (1998) boundary in-out figure-of-merit with ambiguous POS tags by running the
 * forward/backward algorithm.
 * 
 * @author Nathan Bodenstab
 */
public final class BoundaryLex extends FigureOfMeritModel {

    private Grammar grammar;

    // Model params learned from training data
    private final float leftBoundaryLogProb[][], rightBoundaryLogProb[][], bigramLogProb[][];

    final short nullSymbol;
    final short[] NULL_LIST;
    final float[] NULL_PROBABILITIES;

    public BoundaryLex(final FOMType type, final Grammar grammar, final BufferedReader modelStream) throws IOException {

        super(type);

        this.grammar = grammar;

        nullSymbol = (short) grammar.nullSymbol;
        NULL_LIST = new short[] { nullSymbol };
        NULL_PROBABILITIES = new float[] { 0f };

        final int numNT = grammar.numNonTerms();
        final int numLex = grammar.lexSet.size();
        leftBoundaryLogProb = new float[numLex][numNT];
        rightBoundaryLogProb = new float[numLex][numNT];
        bigramLogProb = new float[numLex][numLex];

        // Init values to log(0) = -Inf
        for (int i = 0; i < numLex; i++) {
            Arrays.fill(leftBoundaryLogProb[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(rightBoundaryLogProb[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(bigramLogProb[i], Float.NEGATIVE_INFINITY);
        }

        if (modelStream != null) {
            readModel(modelStream);
        }
    }

    @Override
    public FigureOfMerit createFOM() {
        switch (type) {
        case BoundaryLex:
            return new BoundaryLexSelector();
        default:
            return super.createFOM();
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
            final String[] tokens = line.split("\\s+");
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
                    bigramLogProb[numIndex][denomIndex] = prob;
                } else {
                    System.err.println("WARNING: ignoring line in model file '" + line + "'");
                }
            }
        }
    }

    public static void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile)
            throws Exception {
        BoundaryLex.train(inStream, outStream, grammarFile, 0.5, false, 2);
    }

    public static void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile,
            final double smoothingCount, final boolean writeCounts, final int posNgramOrder) throws Exception {
        String line, historyStr;
        final String joinString = " ";
        ParseTree tree;
        final SimpleCounterSet<String> leftBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> rightBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> bigramCount = new SimpleCounterSet<String>();

        // To train a BoundaryInOut FOM model we need a grammar and
        // binarized gold input trees with NTs from same grammar
        final Grammar grammar = ParserDriver.readGrammar(grammarFile, ResearchParserType.ECPCellCrossList, null);

        // TODO: note that we have to have the same training grammar as decoding grammar here
        // so the input needs to be binarized. If we are parsing with the Berkeley latent-variable
        // grammar then we can't work with gold trees. As an approximation we will take the
        // 1-best from the Berkeley parser output (although constraining the coarse labels to match
        // the true gold tree would probably be a little better)
        // See Caraballo/Charniak 1998 for (what I think is) their inside/outside solution
        // to the same (or a similar) problem.
        while ((line = inStream.readLine()) != null) {
            tree = ParseTree.readBracketFormat(line);
            if (tree.isBinaryTree() == false) {
                System.err.println("ERROR: Training trees must be binarized exactly as used in decoding grammar");
                System.exit(1);
            }
            tree.linkLeavesLeftRight();
            for (final ParseTree node : tree.preOrderTraversal()) {

                // leftBoundary = P(N[i:j] | lex[i-1]) = #(N[i:j], lex[i-1]) / #(*[i:*], lex[i-1])
                // riteBoundary = P(lex[j+1] | N[i:j]) = #(N[i:j], lex[j+1]) / #(N[*:j], *[j+1])
                // bi-gram lex model for lex[i-1:j+1]
                // counts we need:
                // -- #(N[i:j], lex[i-1])
                // -- #(*[i:*], lex[i-1]) -- number of times lex occurs just to the left of any span

                if (node.isNonTerminal() == true) {
                    if (grammar.nonTermSet.contains(node.contents) == false) {
                        throw new IOException("Nonterminal '" + node.contents
                                + "' in input tree not found in grammar.  Exiting.");
                    }
                    leftBoundaryCount.increment(node.contents,
                            convertNull(node.leftBoundaryLexContents(), Grammar.nullSymbolStr));
                    rightBoundaryCount.increment(convertNull(node.rightBoundaryLexContents(), Grammar.nullSymbolStr),
                            node.contents);
                }
            }

            // n-gram POS prob
            final LinkedList<String> history = new LinkedList<String>();
            for (int i = 0; i < posNgramOrder - 1; i++) {
                history.addLast(Grammar.nullSymbolStr); // pad history with nulls (for beginning of string)
            }

            // iterate through POS tags using .rightNeighbor
            // for (ParseTree posNode = tree.leftMostPOS(); posNode != null; posNode = posNode.rightNeighbor) {
            for (ParseTree leafNode = tree.leftMostLeaf(); leafNode != null; leafNode = leafNode.rightNeighbor) {
                if (!grammar.lexSet.contains(leafNode.contents)) {
                    throw new IOException("Nonterminal '" + leafNode.contents
                            + "' in input tree not found in grammar.  Exiting.");
                }
                historyStr = Util.join(history, joinString);
                bigramCount.increment(leafNode.contents, historyStr);
                history.removeFirst();
                history.addLast(leafNode.contents);
            }

            // finish up with final transition to <null>
            historyStr = Util.join(history, joinString);
            bigramCount.increment(Grammar.nullSymbolStr, historyStr);
        }

        final int numNT = grammar.numNonTerms();
        final int numLex = grammar.lexSet.size();

        // smooth counts
        if (smoothingCount > 0) {
            leftBoundaryCount.smoothAddConst(smoothingCount, numLex);
            rightBoundaryCount.smoothAddConst(smoothingCount, numNT);
            bigramCount.smoothAddConst(smoothingCount, numLex);
        }

        // Write model to file
        float score;
        outStream.write("# model=FOM type=BoundaryLex boundaryNgramOrder=2 lexNgramOrder=" + posNgramOrder + "\n");

        // left boundary = P(NT | POS-1)
        for (final String lexStr : grammar.lexSet) {
            for (final int ntIndex : grammar.phraseSet) {
                final String ntStr = grammar.mapNonterminal(ntIndex);
                if (writeCounts) {
                    score = leftBoundaryCount.getCount(ntStr, lexStr);
                } else {
                    score = (float) Math.log(leftBoundaryCount.getProb(ntStr, lexStr));
                }
                if (score > Float.NEGATIVE_INFINITY) {
                    outStream.write("LB " + ntStr + " | " + lexStr + " " + score + "\n");
                }
            }
        }

        // right boundary = P(POS+1 | NT)
        for (final String lexStr : grammar.lexSet) {
            for (final int rightPOSIndex : grammar.posSet) {
                final String posStr = grammar.mapNonterminal(rightPOSIndex);
                if (writeCounts) {
                    score = rightBoundaryCount.getCount(posStr, lexStr);
                } else {
                    score = (float) Math.log(rightBoundaryCount.getProb(posStr, lexStr));
                }
                if (score > Float.NEGATIVE_INFINITY) {
                    outStream.write("RB " + posStr + " | " + lexStr + " " + score + "\n");
                }
            }
        }

        // pos n-gram = P(POS | POS-1)
        for (final String histLex : grammar.lexSet) {
            for (final String lex : grammar.lexSet) {
                if (writeCounts) {
                    score = bigramCount.getCount(lex, histLex);
                } else {
                    score = (float) Math.log(bigramCount.getProb(lex, histLex));
                }
                if (score > Float.NEGATIVE_INFINITY) {
                    outStream.write("PN " + lex + " | " + histLex + " " + score + "\n");
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

    public class BoundaryLexSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;

        // pre-computed left/right FOM outside scores for current sentence
        private float outsideLeft[][], outsideRight[][];
        private short[][] backPointer;
        private float[] scores;
        private float[] prevScores;

        // private int bestPOSTag[];
        ParseTask parseTask;

        public BoundaryLexSelector() {
        }

        @Override
        public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
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

        /**
         * Computes forward-backward and left/right boundary probs across ambiguous POS tags. Also computes 1-best POS
         * tag sequence based on viterbi-max decoding
         */
        @Override
        public void initSentence(final ParseTask task, final Chart chart) {
            final int sentLen = task.sentenceLength();
            final int fbSize = sentLen + 2;
            final int posSize = grammar.maxPOSIndex() + 1;

            if (outsideLeft == null || outsideLeft.length < fbSize) {
                outsideLeft = new float[fbSize + 10][grammar.numNonTerms()];
                outsideRight = new float[fbSize + 10][grammar.numNonTerms()];
                backPointer = new short[fbSize + 10][posSize];
            }

            if (scores == null) {
                scores = new float[posSize];
                prevScores = new float[posSize];
            }

            for (int i = 0; i < fbSize; i++) {
                Arrays.fill(outsideLeft[i], Float.NEGATIVE_INFINITY);
                Arrays.fill(outsideRight[i], Float.NEGATIVE_INFINITY);
                Arrays.fill(backPointer[i], (short) 0);
            }

            short[] prevPOSList = NULL_LIST;

            // Forward pass
            prevScores[nullSymbol] = 0f;

            System.arraycopy(leftBoundaryLogProb[nullSymbol], 0, outsideLeft[0], 0, grammar.numNonTerms());

            for (int fwdIndex = 1; fwdIndex < fbSize; fwdIndex++) {

                Arrays.fill(scores, Float.NEGATIVE_INFINITY);

                // Forward-backward Chart is one off from the parser chart
                final int fwdChartIndex = fwdIndex - 1;

                final short[] posList = fwdChartIndex >= sentLen ? NULL_LIST : grammar
                        .lexicalParents(task.tokens[fwdChartIndex]);
                final float[] fwdPOSProbs = fwdChartIndex >= sentLen ? NULL_PROBABILITIES : grammar
                        .lexicalLogProbabilities(task.tokens[fwdChartIndex]);

                final short[] currentBackpointer = backPointer[fwdIndex];

                for (int i = 0; i < posList.length; i++) {
                    final short curPOS = posList[i];
                    final float posEmissionLogProb = fwdPOSProbs[i];
                    final float[] curPosTransitionLogProb = bigramLogProb[curPOS];
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
                for (final short pos : posList) {
                    final float posScore = scores[pos];
                    final float[] posLeftBoundaryLogProb = leftBoundaryLogProb[pos];

                    for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {
                        final float score = posScore + posLeftBoundaryLogProb[nonTerm];
                        if (score > currentOutsideLeft[nonTerm]) {
                            currentOutsideLeft[nonTerm] = score;
                        }
                    }
                }

                final float[] tmp = prevScores;
                prevScores = scores;
                scores = tmp;
                prevPOSList = posList;
            }

            // Backward pass
            System.arraycopy(rightBoundaryLogProb[nullSymbol], 0, outsideRight[fbSize - 1], 0, grammar.numNonTerms());

            prevPOSList = NULL_LIST;
            prevScores[nullSymbol] = 0f;

            for (int bkwIndex = fbSize - 2; bkwIndex >= 0; bkwIndex--) {

                Arrays.fill(scores, Float.NEGATIVE_INFINITY);

                final int bkwChartIndex = bkwIndex - 1;
                final short[] posList = bkwChartIndex < 0 ? NULL_LIST : grammar
                        .lexicalParents(task.tokens[bkwChartIndex]);
                final float[] bkwPOSProbs = bkwChartIndex < 0 ? NULL_PROBABILITIES : grammar
                        .lexicalLogProbabilities(task.tokens[bkwChartIndex]);

                for (final short prevPOS : prevPOSList) {
                    final float prevScore = prevScores[prevPOS];
                    final float[] prevPosTransitionLogProb = bigramLogProb[prevPOS];

                    for (int i = 0; i < posList.length; i++) {
                        final short curPOS = posList[i];
                        final float score = prevScore + prevPosTransitionLogProb[curPOS] + bkwPOSProbs[i];
                        if (score > scores[curPOS]) {
                            scores[curPOS] = score;
                        }
                    }
                }

                // compute right outside scores to be used during decoding
                // FOM = outsideLeft[i][A] * inside[i][j][A] * outsideRight[j][A]
                final float[] currentOutsideRight = outsideRight[bkwIndex];
                for (final short pos : posList) {
                    final float posScore = scores[pos];
                    final float[] posRightBoundaryLogProb = rightBoundaryLogProb[pos];

                    for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {
                        final float score = posScore + posRightBoundaryLogProb[nonTerm];
                        if (score > currentOutsideRight[nonTerm]) {
                            currentOutsideRight[nonTerm] = score;
                        }
                    }
                }

                final float[] tmp = prevScores;
                prevScores = scores;
                scores = tmp;
                prevPOSList = posList;
            }

            // tags from parseTask.tags are used for chart cell feature extraction when
            // using BoundaryInOut FOM. If parseFromInputTags is true, then the tags
            // from the input will already be in place. Otherwise, fill in the tags array
            // with the 1-best result from this forward-backwards run.
            if (ParserDriver.parseFromInputTags == false) {
                task.fomTags = new int[sentLen];
                // track backpointers to extract best POS sequence
                // start at the end of the sentence with the nullSymbol and trace backwards
                int bestPOS = nullSymbol;
                for (int i = sentLen - 1; i >= 0; i--) {
                    bestPOS = backPointer[i + 2][bestPOS];
                    task.fomTags[i] = bestPOS;
                    // System.out.println(i + "=" + grammar.mapNonterminal(bestPOS));
                }
            }
        }
    }
}
