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
import edu.ohsu.cslu.grammar.CoarseGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserUtil;
import edu.ohsu.cslu.parser.fom.FigureOfMerit.FOMType;

/**
 * Implements Caraballo and Charniak's (1998) boundary in-out figure-of-merit.
 * 
 * @author Nathan Bodenstab
 */
public final class BoundaryInOut extends FigureOfMeritFactory {

    private Grammar grammar;
    private int posNgramOrder = 2;
    private CoarseGrammar coarseGrammar = null;

    // Model params learned from training data
    private final float leftBoundaryLogProb[][], rightBoundaryLogProb[][], posTransitionLogProb[][];

    public BoundaryInOut(final FOMType type, final Grammar grammar, final BufferedReader modelStream)
            throws IOException {

        super(type);

        this.grammar = grammar;
        if (grammar.isCoarseGrammar()) {
            coarseGrammar = (CoarseGrammar) grammar;
        }
        final int numNT = grammar.numNonTerms();
        final int maxPOSIndex = grammar.maxPOSIndex();
        leftBoundaryLogProb = new float[numNT][maxPOSIndex + 1];
        rightBoundaryLogProb = new float[maxPOSIndex + 1][numNT];
        posTransitionLogProb = new float[maxPOSIndex + 1][maxPOSIndex + 1];

        // Init values to log(0) = -Inf
        for (int i = 0; i < numNT; i++) {
            Arrays.fill(leftBoundaryLogProb[i], Float.NEGATIVE_INFINITY);
        }
        for (int i = 0; i < maxPOSIndex + 1; i++) {
            Arrays.fill(rightBoundaryLogProb[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(posTransitionLogProb[i], Float.NEGATIVE_INFINITY);
        }

        if (modelStream != null) {
            readModel(modelStream);
        }
    }

    @Override
    public FigureOfMerit createEdgeSelector(final Grammar grammar) {
        switch (type) {
        case BoundaryInOut:
            return new BoundaryInOutSelector();
        case InsideWithFwdBkwd:
            return new InsideWithFwdBkwd();
        default:
            return super.createEdgeSelector(grammar);
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

                numStr = ParserUtil.join(numerator, " ");
                numIndex = grammar.mapNonterminal(numStr);
                if (numIndex < 0) {
                    BaseLogger.singleton().info(
                            "ERROR: non-terminal '" + numStr + "' from FOM model not found in grammar.");
                    System.exit(1);
                }
                denomStr = ParserUtil.join(denom, " ");
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
                    leftBoundaryLogProb[numIndex][denomIndex] = prob;
                } else if (tokens[0].equals("RB")) {
                    rightBoundaryLogProb[numIndex][denomIndex] = prob;
                } else if (tokens[0].equals("PN")) {
                    posTransitionLogProb[numIndex][denomIndex] = prob;
                } else {
                    System.err.println("WARNING: ignoring line in model file '" + line + "'");
                }
            }
        }
    }

    public void train(final BufferedReader inStream, final BufferedWriter outStream) throws IOException {
        train(inStream, outStream, 0.5, false);
    }

    public void train(final BufferedReader inStream, final BufferedWriter outStream, final double smoothingCount,
            final boolean writeCounts) throws IOException {
        String line, historyStr;
        final String joinString = " ";
        ParseTree tree;
        final SimpleCounterSet<String> leftBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> rightBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> posTransitionCount = new SimpleCounterSet<String>();

        // TODO: note that we have to have the same training grammar as decoding grammar here
        // so the input needs to be bianarized. If we are parsing with the Berkeley latent-variable
        // grammar then we can't work with Gold trees. As an approximation we will take the
        // 1-best from the Berkeley parser output (although constraining the coarse labels to match
        // the true gold tree would pobably be a little better)
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

                // leftBoundary = P(N[i:j] | POS[i-1]) = #(N[i:j], POS[i-1]) / #(*[i:*], POS[i-1])
                // riteBoundary = P(POS[j+1] | N[i:j]) = #(N[i:j], POS[j+1]) / #(N[*:j], *[j+1])
                // bi-gram POS model for POS[i-1:j+1]
                // counts we need:
                // -- #(N[i:j], POS[i-1])
                // -- #(*[i:*], POS[i-1]) -- number of times POS occurs just to the left of any span

                if (node.isNonTerminal() == true) {
                    if (grammar.nonTermSet.contains(node.contents) == false) {
                        throw new IOException("Nonterminal '" + node.contents
                                + "' in input tree not found in grammar.  Exiting.");
                    }
                    leftBoundaryCount.increment(node.contents, convertNull(node.leftBoundaryPOSContents()));
                    rightBoundaryCount.increment(convertNull(node.rightBoundaryPOSContents()), node.contents);
                }
            }

            // n-gram POS prob
            final LinkedList<String> history = new LinkedList<String>();
            for (int i = 0; i < posNgramOrder - 1; i++) {
                history.addLast(Grammar.nullSymbolStr); // pad history with nulls (for beginning of string)
            }

            // iterate through POS tags using .rightNeighbor
            for (ParseTree posNode = tree.leftMostPOS(); posNode != null; posNode = posNode.rightNeighbor) {
                if (grammar.nonTermSet.contains(posNode.contents) == false) {
                    throw new IOException("Nonterminal '" + posNode.contents
                            + "' in input tree not found in grammar.  Exiting.");
                }
                historyStr = ParserUtil.join(history, joinString);
                posTransitionCount.increment(posNode.contents, historyStr);
                history.removeFirst();
                history.addLast(posNode.contents);
            }

            // finish up with final transition to <null>
            historyStr = ParserUtil.join(history, joinString);
            posTransitionCount.increment(Grammar.nullSymbolStr, historyStr);
        }

        final int numNT = grammar.numNonTerms();
        final int numPOS = grammar.posSet.size();

        // smooth counts
        if (smoothingCount > 0) {
            leftBoundaryCount.smoothAddConst(smoothingCount, numPOS);
            rightBoundaryCount.smoothAddConst(smoothingCount, numNT);
            posTransitionCount.smoothAddConst(smoothingCount, numPOS);
        }

        // writeModelCounts(new BufferedWriter(new FileWriter("fom.counts")), leftBoundaryCount, rightBoundaryCount,
        // posTransitionCount);

        // // turn counts into probs
        //
        // // left boundary = P(NT | POS-1)
        // for (final int leftPOSIndex : grammar.posSet) {
        // final String posStr = grammar.mapNonterminal(leftPOSIndex);
        // for (int ntIndex = 0; ntIndex < numNT; ntIndex++) {
        // final String ntStr = grammar.mapNonterminal(ntIndex);
        // leftBoundaryLogProb[ntIndex][leftPOSIndex] = (float) Math.log(leftBoundaryCount.getProb(ntStr, posStr));
        // }
        // }
        //
        // // right boundary = P(POS+1 | NT)
        // for (int ntIndex = 0; ntIndex < grammar.numNonTerms(); ntIndex++) {
        // final String ntStr = grammar.mapNonterminal(ntIndex);
        // for (final int rightPOSIndex : grammar.posSet) {
        // final String posStr = grammar.mapNonterminal(rightPOSIndex);
        // rightBoundaryLogProb[rightPOSIndex][ntIndex] = (float) Math.log(rightBoundaryCount.getProb(posStr,
        // ntStr));
        // }
        // }
        //
        // // pos n-gram = P(POS | POS-1)
        // for (final int histPos : grammar.posSet) {
        // final String histPosStr = grammar.mapNonterminal(histPos);
        // for (final int pos : grammar.posSet) {
        // final String posStr = grammar.mapNonterminal(pos);
        // posTransitionLogProb[pos][histPos] = (float) Math.log(posTransitionCount.getProb(posStr, histPosStr));
        // }
        // }

        // Write model to file
        float score;
        outStream.write("# model=FOM type=BoundaryInOut boundaryNgramOrder=2 posNgramOrder=" + posNgramOrder + "\n");

        // left boundary = P(NT | POS-1)
        for (final int leftPOSIndex : grammar.posSet) {
            final String posStr = grammar.mapNonterminal(leftPOSIndex);
            for (final int ntIndex : grammar.phraseSet) {
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
        for (final int ntIndex : grammar.phraseSet) {
            final String ntStr = grammar.mapNonterminal(ntIndex);
            for (final int rightPOSIndex : grammar.posSet) {
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
        for (final int histPos : grammar.posSet) {
            final String histPosStr = grammar.mapNonterminal(histPos);
            for (final int pos : grammar.posSet) {
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

    private String convertNull(final String nonTerm) {
        if (nonTerm == null) {
            return Grammar.nullSymbolStr;
        }
        return nonTerm;
    }

    // private void writeModelCounts(final BufferedWriter outStream, final SimpleCounterSet<String> leftBoundaryCount,
    // final SimpleCounterSet<String> rightBoundaryCount, final SimpleCounterSet<String> posTransitionCount)
    // throws IOException {
    //
    // outStream.write("# columns: <type> X1 X2 ... | Y1 Y2 ... negLogProb = -1*log(P(X1,X2,..|Y1,Y2,..)) \n");
    // outStream.write("# type = LB (left boundary), RB (right boundary), PN (POS n-gram)\n");
    //
    // for (final Entry<String, SimpleCounter<String>> posCounter : leftBoundaryCount.items.entrySet()) {
    // for (final Entry<String, Float> ntCount : posCounter.getValue().entrySet()) {
    // outStream.write("LB " + ntCount.getKey() + " " + ntCount.getValue() + "\n");
    // }
    // }
    //
    // for (final Entry<String, SimpleCounter<String>> ntCounter : rightBoundaryCount.items.entrySet()) {
    // for (final Entry<String, Float> posCount : ntCounter.getValue().entrySet()) {
    // outStream.write("RB " + posCount.getKey() + " " + posCount.getValue() + "\n");
    // }
    // }
    //
    // for (final Entry<String, SimpleCounter<String>> denomCounter : posTransitionCount.items.entrySet()) {
    // for (final Entry<String, Float> numerCount : denomCounter.getValue().entrySet()) {
    // outStream.write("PN " + numerCount.getKey() + " " + numerCount.getValue() + "\n");
    // }
    // }
    //
    // outStream.close();
    // }

    public class BoundaryInOutSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;

        // pre-computed left/right FOM outside scores for current sentence
        private float outsideLeft[][], outsideRight[][];
        private int bestPOSTag[];

        public BoundaryInOutSelector() {
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

        public int get1bestPOSTag(final int i) {
            return bestPOSTag[i];
        }

        @Override
        public void init(final int[] tokens) {
            outsideLeftRightAndBestPOS(tokens);
        }

        // Computes forward-backward and left/right boundary probs across ambiguous
        // POS tags. Also computes 1-best POS tag sequence based on viterbi-max decoding
        private void outsideLeftRightAndBestPOS(final int[] tokens) {
            final int sentLen = tokens.length;
            final int fbSize = sentLen + 2;
            final int posSize = grammar.maxPOSIndex() + 1;

            if (outsideLeft == null || outsideLeft.length < fbSize) {
                outsideLeft = new float[fbSize][grammar.numNonTerms()];
                outsideRight = new float[fbSize][grammar.numNonTerms()];
            }

            final int[][] backPointer = new int[fbSize][posSize];
            bestPOSTag = new int[sentLen];

            for (int i = 0; i < fbSize; i++) {
                Arrays.fill(outsideLeft[i], Float.NEGATIVE_INFINITY);
                Arrays.fill(outsideRight[i], Float.NEGATIVE_INFINITY);
            }

            final short nullSymbol = (short) grammar.nullSymbol;
            final short[] NULL_LIST = new short[] { nullSymbol };
            final float[] NULL_PROBABILITIES = new float[] { 0f };

            short[] prevPOSList = NULL_LIST;
            float[] scores = new float[posSize];
            float[] prevScores = new float[posSize];

            // Forward pass
            prevScores[nullSymbol] = 0f;

            for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {
                outsideLeft[0][nonTerm] = leftBoundaryLogProb[nonTerm][nullSymbol];
            }

            for (int fwdIndex = 1; fwdIndex < fbSize; fwdIndex++) {

                Arrays.fill(scores, Float.NEGATIVE_INFINITY);

                // Forward-backward Chart is one off from the parser chart
                final int fwdChartIndex = fwdIndex - 1;

                final short[] posList = fwdChartIndex >= sentLen ? NULL_LIST : grammar
                        .lexicalParents(tokens[fwdChartIndex]);
                final float[] fwdPOSProbs = fwdChartIndex >= sentLen ? NULL_PROBABILITIES : grammar
                        .lexicalLogProbabilities(tokens[fwdChartIndex]);

                final int[] currentBackpointer = backPointer[fwdIndex];

                for (int i = 0; i < posList.length; i++) {
                    final short curPOS = posList[i];
                    final float posEmissionLogProb = fwdPOSProbs[i];

                    for (final short prevPOS : prevPOSList) {
                        final float score = prevScores[prevPOS] + posTransitionLogProb[curPOS][prevPOS]
                                + posEmissionLogProb;
                        if (score > scores[curPOS]) {
                            scores[curPOS] = score;
                            currentBackpointer[curPOS] = prevPOS;
                        }
                    }
                }

                // compute left and right outside scores to be used during decoding
                // to calculate the FOM = outsideLeft[i][A] * inside[i][j][A] * outsideRight[j][A]
                final float[] fwdOutsideLeft = outsideLeft[fwdIndex];
                for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {

                    for (final short pos : posList) {
                        final float score = scores[pos] + leftBoundaryLogProb[nonTerm][pos];
                        if (score > fwdOutsideLeft[nonTerm]) {
                            fwdOutsideLeft[nonTerm] = score;
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
                final short[] posList = bkwChartIndex < 0 ? NULL_LIST : grammar.lexicalParents(tokens[bkwChartIndex]);
                final float[] bkwPOSProbs = bkwChartIndex < 0 ? NULL_PROBABILITIES : grammar
                        .lexicalLogProbabilities(tokens[bkwChartIndex]);

                for (final short prevPOS : prevPOSList) {

                    for (int i = 0; i < posList.length; i++) {
                        final short curPOS = posList[i];

                        final float score = prevScores[prevPOS] + posTransitionLogProb[prevPOS][curPOS]
                                + bkwPOSProbs[i];
                        if (score > scores[curPOS]) {
                            scores[curPOS] = score;
                        }
                    }
                }

                // compute right outside scores to be used during decoding
                // to calculate the FOM = outsideLeft[i][A] * inside[i][j][A] * outsideRight[j][A]
                final float[] bkwOutsideRight = outsideRight[bkwIndex];
                for (final short pos : posList) {
                    for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {
                        final float score = scores[pos] + rightBoundaryLogProb[pos][nonTerm];
                        if (score > bkwOutsideRight[nonTerm]) {
                            bkwOutsideRight[nonTerm] = score;
                        }
                    }
                }

                final float[] tmp = prevScores;
                prevScores = scores;
                scores = tmp;
                prevPOSList = posList;
            }

            // track backpointers to extract best POS sequence
            // start at the end of the sentence with the nullSymbol and trace backwards
            int bestPOS = nullSymbol;
            for (int i = sentLen - 1; i >= 0; i--) {
                bestPOS = backPointer[i + 2][bestPOS];
                bestPOSTag[i] = bestPOS;
                // System.out.println(i + "=" + grammar.mapNonterminal(bestPOS));
            }
        }

        // public final float leftBoundaryLogProb(final int nonTerm, final int pos) {
        // return leftBoundaryLogProb[nonTerm][pos];
        // }
        //
        // public final float rightBoundaryLogProb(final int pos, final int nonTerm) {
        // return rightBoundaryLogProb[pos][nonTerm];
        // }
        //
        // public final float posTransitionLogProb(final int pos, final int histPos) {
        // return posTransitionLogProb[pos][histPos];
        // }

    }

    // for Beam-Width Prediction, we need the 1-best POS tags from the chart. And
    // to get these, we need to run the forward-backward algorithm with a model
    // that has POS transition probabilities. Right now this is only done for the
    // Boundary FOM, so instead of writing lots more code and creating new model files,
    // we are simply hi-jacking all of that and overwriting the calcFOM() function to
    // ignore most of the work that is done during setup.
    public class InsideWithFwdBkwd extends BoundaryInOutSelector {

        private static final long serialVersionUID = 1L;

        public InsideWithFwdBkwd() {
        }

        @Override
        public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
            return insideProbability;
        }
    }
}
