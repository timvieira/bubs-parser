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
package edu.ohsu.cslu.parser.edgeselector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map.Entry;

import edu.ohsu.cslu.counters.SimpleCounter;
import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserUtil;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;

public class BoundaryInOut extends EdgeSelectorFactory {

    private Grammar grammar;
    private int posNgramOrder = 2;

    // Model params learned from training data
    private final float leftBoundaryLogProb[][], rightBoundaryLogProb[][], posTransitionLogProb[][];

    // private IntOpenHashSet posSet = new IntOpenHashSet();
    // private IntOpenHashSet phraseNonTermSet = new IntOpenHashSet();

    public BoundaryInOut(final EdgeSelectorType type, final Grammar grammar, final BufferedReader modelStream)
            throws IOException {

        super(type);

        this.grammar = grammar;
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
    public EdgeSelector createEdgeSelector(final Grammar grammar) {
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
            final String[] tokens = ParserUtil.tokenize(line);
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
                denomStr = ParserUtil.join(denom, " ");
                denomIndex = grammar.mapNonterminal(denomStr);
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

    public void writeModel(final BufferedWriter outStream) throws IOException {

        outStream.write("# model=FOM type=BoundaryInOut boundaryNgramOrder=2 posNgramOrder=" + posNgramOrder + "\n");

        // left boundary = P(NT | POS-1)
        for (final int leftPOSIndex : grammar.posSet) {
            final String posStr = grammar.mapNonterminal(leftPOSIndex);
            for (final int ntIndex : grammar.phraseSet) {
                final String ntStr = grammar.mapNonterminal(ntIndex);
                final float logProb = leftBoundaryLogProb[ntIndex][leftPOSIndex];
                if (logProb > Float.NEGATIVE_INFINITY) {
                    outStream.write("LB " + ntStr + " | " + posStr + " " + logProb + "\n");
                }
            }
        }

        // right boundary = P(POS+1 | NT)
        for (final int ntIndex : grammar.phraseSet) {
            final String ntStr = grammar.mapNonterminal(ntIndex);
            for (final int rightPOSIndex : grammar.posSet) {
                final String posStr = grammar.mapNonterminal(rightPOSIndex);
                final float logProb = rightBoundaryLogProb[rightPOSIndex][ntIndex];
                if (logProb > Float.NEGATIVE_INFINITY) {
                    outStream.write("RB " + posStr + " | " + ntStr + " " + logProb + "\n");
                }
            }
        }

        // pos n-gram = P(POS | POS-1)
        for (final int histPos : grammar.posSet) {
            final String histPosStr = grammar.mapNonterminal(histPos);
            for (final int pos : grammar.posSet) {
                final String posStr = grammar.mapNonterminal(pos);
                final float logProb = posTransitionLogProb[pos][histPos];
                if (logProb > Float.NEGATIVE_INFINITY) {
                    outStream.write("PN " + posStr + " | " + histPosStr + " " + logProb + "\n");
                }
            }
        }
        outStream.close();
    }

    public void train(final BufferedReader inStream) throws Exception {
        String line, historyStr;
        final String joinString = " ";
        ParseTree tree;
        final SimpleCounterSet<String> leftBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> rightBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> posTransitionCount = new SimpleCounterSet<String>();

        // TODO: note that we have to have the same training grammar as decoding grammar here
        // so the input needs to be bianarized. How are we going to get gold trees in the
        // Berkeley grammar format? Can we just use the 1-best? Or maybe an inside/outside
        // estimate. See Caraballo/Charniak 1998 for (what I think is) their inside/outside solution
        // to the same (or a similar) problem.
        while ((line = inStream.readLine()) != null) {
            tree = ParseTree.readBracketFormat(line);
            if (tree.isBinaryTree() == false) {
                System.err.println("ERROR: Training trees must be binarized exactly as used in decoding");
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
        leftBoundaryCount.smoothAddConst(0.5, numPOS);
        rightBoundaryCount.smoothAddConst(0.5, numNT);
        posTransitionCount.smoothAddConst(0.5, numPOS);

        // turn counts into probs

        // left boundary = P(NT | POS-1)
        for (final int leftPOSIndex : grammar.posSet) {
            final String posStr = grammar.mapNonterminal(leftPOSIndex);
            for (int ntIndex = 0; ntIndex < numNT; ntIndex++) {
                final String ntStr = grammar.mapNonterminal(ntIndex);
                leftBoundaryLogProb[ntIndex][leftPOSIndex] = (float) Math.log(leftBoundaryCount.getProb(ntStr, posStr));
            }
        }

        // right boundary = P(POS+1 | NT)
        for (int ntIndex = 0; ntIndex < grammar.numNonTerms(); ntIndex++) {
            final String ntStr = grammar.mapNonterminal(ntIndex);
            for (final int rightPOSIndex : grammar.posSet) {
                final String posStr = grammar.mapNonterminal(rightPOSIndex);
                rightBoundaryLogProb[rightPOSIndex][ntIndex] = (float) Math.log(rightBoundaryCount.getProb(posStr,
                        ntStr));
            }
        }

        // pos n-gram = P(POS | POS-1)
        for (final int histPos : grammar.posSet) {
            final String histPosStr = grammar.mapNonterminal(histPos);
            for (final int pos : grammar.posSet) {
                final String posStr = grammar.mapNonterminal(pos);
                posTransitionLogProb[pos][histPos] = (float) Math.log(posTransitionCount.getProb(posStr, histPosStr));
            }
        }
    }

    private String convertNull(final String nonTerm) {
        if (nonTerm == null) {
            return Grammar.nullSymbolStr;
        }
        return nonTerm;
    }

    public void writeModelCounts(final BufferedWriter outStream, final SimpleCounterSet<String> leftBoundaryCount,
            final SimpleCounterSet<String> rightBoundaryCount, final SimpleCounterSet<String> posNgramCount)
            throws IOException {

        outStream.write("# columns: <type> X1 X2 ... | Y1 Y2 ... negLogProb = -1*log(P(X1,X2,..|Y1,Y2,..)) \n");
        outStream.write("# type = LB (left boundary), RB (right boundary), PN (POS n-gram)\n");

        for (final Entry<String, SimpleCounter<String>> posCounter : leftBoundaryCount.items.entrySet()) {
            for (final Entry<String, Float> ntCount : posCounter.getValue().entrySet()) {
                outStream.write("LB " + ntCount.getKey() + " " + ntCount.getValue() + "\n");
            }
        }

        for (final Entry<String, SimpleCounter<String>> ntCounter : rightBoundaryCount.items.entrySet()) {
            for (final Entry<String, Float> posCount : ntCounter.getValue().entrySet()) {
                outStream.write("RB " + posCount.getKey() + " " + posCount.getValue() + "\n");
            }
        }

        for (final Entry<String, SimpleCounter<String>> denomCounter : posNgramCount.items.entrySet()) {
            for (final Entry<String, Float> numerCount : denomCounter.getValue().entrySet()) {
                outStream.write("PN " + numerCount.getKey() + " " + numerCount.getValue() + "\n");
            }
        }

        outStream.close();
    }

    public class BoundaryInOutSelector extends EdgeSelector {

        // pre-computed left/right FOM outside scores for current sentence
        private float outsideLeft[][], outsideRight[][];
        private int bestPOSTag[];

        public BoundaryInOutSelector() {
        }

        @Override
        public float calcFOM(final ChartEdge edge) {

            if (edge.prod.isLexProd()) {
                return edge.inside();
            }

            // leftIndex and rightIndex have +1 because the outsideLeft and outsideRight arrays
            // are padded with a begin and end <null> value which shifts the entire array to
            // the right by one
            // final int spanLength = edge.end() - edge.start();
            final float outside = outsideLeft[edge.start()][edge.prod.parent]
                    + outsideRight[edge.end() + 1][edge.prod.parent];
            return edge.inside() + outside;
        }

        @Override
        public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
            // leftIndex and rightIndex have +1 because the outsideLeft and outsideRight arrays
            // are padded with a begin and end <null> value which shifts the entire array to
            // the right by one
            // final int spanLength = edge.end() - edge.start();

            final float outside = outsideLeft[start][parent] + outsideRight[end + 1][parent];
            return insideProbability + outside;
        }

        @Override
        public float calcLexicalFOM(final int start, final int end, final short parent, final float insideProbability) {
            return insideProbability;
        }

        public String calcFOMToString(final ChartEdge edge) {
            final int spanLength = edge.end() - edge.start();
            final float outside = outsideLeft[edge.start() - 1 + 1][edge.prod.parent]
                    + outsideRight[edge.end() + 1][edge.prod.parent];
            final float fom = edge.inside() + outside;

            // String s = "FOM: chart[" + edge.start() + "," + edge.end() + "]" + " n=" + spanLength + " p=" +
            // edge.p.toString() + " i=" + edge.insideProb + " o=" + outside
            // + " oL[" + (edge.start() - 1 + 1) + "][" + grammar.nonTermSet.getSymbol(edge.p.parent) + "]=" +
            // outsideLeft[edge.start() - 1 + 1][edge.p.parent] + " oR["
            // + (edge.end() + 1) + "][" + grammar.nonTermSet.getSymbol(edge.p.parent) + "]=" +
            // outsideRight[edge.end() + 1][edge.p.parent] + " fom=" + fom;

            String s = "FOM: chart[" + edge.start() + "," + edge.end() + "]";
            s += " n=" + spanLength;
            s += " p=" + edge.prod.toString();
            s += " i=" + edge.inside();
            s += " o=" + outside;
            s += " oL[" + (edge.start() - 1 + 1) + "][" + grammar.mapNonterminal(edge.prod.parent) + "]="
                    + outsideLeft[edge.start() - 1 + 1][edge.prod.parent];
            s += " oR[" + (edge.end() + 1) + "][" + grammar.mapNonterminal(edge.prod.parent) + "]="
                    + outsideRight[edge.end() + 1][edge.prod.parent];
            s += " fom=" + fom;

            return s;
        }

        public int get1bestPOSTag(final int i) {
            return bestPOSTag[i];
        }

        @Override
        public void init(final Chart chart) {
            // outsideLeftRight(chart);
            outsideLeftRightAndBestPOS(chart);
        }

        // Computes forward-backward and left/right boundary probs across ambiguous
        // POS tags. Assumes all POS tags have already been placed in the chart and the
        // inside prob of the tag is the emission probability.
        // Also computes 1-best POS tag sequence based on viterbi-max decoding
        private void outsideLeftRightAndBestPOS(final Chart chart) {

            final int fbSize = chart.size() + 2;
            final int posSize = grammar.maxPOSIndex() + 1;

            if (outsideLeft == null || outsideLeft.length < fbSize) {
                outsideLeft = new float[fbSize][grammar.numNonTerms()];
                outsideRight = new float[fbSize][grammar.numNonTerms()];
            }

            final int[][] fwdBackptr = new int[fbSize][posSize];
            bestPOSTag = new int[chart.size()];

            for (int i = 0; i < fbSize; i++) {
                Arrays.fill(outsideLeft[i], Float.NEGATIVE_INFINITY);
                Arrays.fill(outsideRight[i], Float.NEGATIVE_INFINITY);
            }

            final short nullSymbol = (short) grammar.nullSymbol;
            final short[] NULL_LIST = new short[] { nullSymbol };
            final float[] NULL_PROBABILITIES = new float[] { 0f };

            short[] prevFwdPOSList = NULL_LIST;
            float[] prevFwdScores = new float[posSize];
            prevFwdScores[nullSymbol] = 0f;

            for (int fwdIndex = 0; fwdIndex < fbSize; fwdIndex++) {
                final int fwdChartIndex = fwdIndex - 1; // -1 because the fbChart is one off from the parser chart

                if (fwdIndex > 0) {
                    // moving from left to right =======>>
                    final short[] fwdPOSList = fwdChartIndex >= chart.size() ? NULL_LIST : grammar
                            .lexicalParents(chart.tokens[fwdChartIndex]);
                    final float[] fwdPOSProbs = fwdChartIndex >= chart.size() ? NULL_PROBABILITIES : grammar
                            .lexicalLogProbabilities(chart.tokens[fwdChartIndex]);

                    final int[] currentFwdBackptr = fwdBackptr[fwdIndex];

                    final float[] fwdScores = new float[posSize];
                    Arrays.fill(fwdScores, Float.NEGATIVE_INFINITY);

                    for (int i = 0; i < fwdPOSList.length; i++) {
                        final short curPOS = fwdPOSList[i];
                        final float posEmissionLogProb = fwdPOSProbs[i];

                        for (final short prevPOS : prevFwdPOSList) {
                            final float score = prevFwdScores[prevPOS] + posTransitionLogProb[curPOS][prevPOS]
                                    + posEmissionLogProb;
                            if (score > fwdScores[curPOS]) {
                                fwdScores[curPOS] = score;
                                currentFwdBackptr[curPOS] = prevPOS;
                            }
                        }
                    }
                    prevFwdScores = fwdScores;
                    prevFwdPOSList = fwdPOSList;
                }

                // compute left and right outside scores to be used during decoding
                // to calculate the FOM = outsideLeft[i][A] * inside[i][j][A] * outsideRight[j][A]
                final float[] fwdOutsideLeft = outsideLeft[fwdIndex];
                for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {

                    for (final short pos : prevFwdPOSList) {
                        final float score = prevFwdScores[pos] + leftBoundaryLogProb[nonTerm][pos];
                        if (score > fwdOutsideLeft[nonTerm]) {
                            fwdOutsideLeft[nonTerm] = score;
                        }
                    }
                }
            }

            short[] prevBkwPOSList = NULL_LIST;
            float[] prevBkwScores = new float[posSize];
            prevBkwScores[nullSymbol] = 0f;

            for (int bkwIndex = fbSize - 1; bkwIndex >= 0; bkwIndex--) {
                if (bkwIndex < fbSize - 1) {
                    final int bkwChartIndex = bkwIndex - 1;

                    // moving from right to left <<=======
                    final short[] bkwPOSList = bkwChartIndex < 0 ? NULL_LIST : grammar
                            .lexicalParents(chart.tokens[bkwChartIndex]);
                    final float[] bkwPOSProbs = bkwChartIndex < 0 ? NULL_PROBABILITIES : grammar
                            .lexicalLogProbabilities(chart.tokens[bkwChartIndex]);
                    final float[] bkwScores = new float[posSize];
                    Arrays.fill(bkwScores, Float.NEGATIVE_INFINITY);

                    for (final short prevPOS : prevBkwPOSList) {

                        for (int i = 0; i < bkwPOSList.length; i++) {
                            final short curPOS = bkwPOSList[i];

                            final float score = prevBkwScores[prevPOS] + posTransitionLogProb[prevPOS][curPOS]
                                    + bkwPOSProbs[i];
                            if (score > bkwScores[curPOS]) {
                                bkwScores[curPOS] = score;
                            }
                        }
                    }
                    prevBkwScores = bkwScores;
                    prevBkwPOSList = bkwPOSList;
                }

                // compute right outside scores to be used during decoding
                // to calculate the FOM = outsideLeft[i][A] * inside[i][j][A] * outsideRight[j][A]
                final float[] bkwOutsideRight = outsideRight[bkwIndex];
                for (final short pos : prevBkwPOSList) {
                    for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {
                        final float score = prevBkwScores[pos] + rightBoundaryLogProb[pos][nonTerm];
                        if (score > bkwOutsideRight[nonTerm]) {
                            bkwOutsideRight[nonTerm] = score;
                        }
                    }
                }
            }

            // track backpointers to extract best POS sequence
            // start at the end of the sentence with the nullSymbol and trace backwards
            int bestPOS = nullSymbol;
            for (int i = chart.size() - 1; i >= 0; i--) {
                bestPOS = fwdBackptr[i + 2][bestPOS];
                bestPOSTag[i] = bestPOS;
                // System.out.println(i + "=" + grammar.mapNonterminal(bestPOS));
            }
        }

        public final float leftBoundaryLogProb(final int nonTerm, final int pos) {
            return leftBoundaryLogProb[nonTerm][pos];
        }

        public final float rightBoundaryLogProb(final int pos, final int nonTerm) {
            return rightBoundaryLogProb[pos][nonTerm];
        }

        public final float posTransitionLogProb(final int pos, final int histPos) {
            return posTransitionLogProb[pos][histPos];
        }

    }

    // for Beam-Width Prediction, we need the 1-best POS tags from the chart. And
    // to get these, we need to run the forward-backward algorithm with a model
    // that has POS transition probabilities. Right now this is only done for the
    // Boundary FOM, so instead of writing lots more code and creating new model files,
    // we are simply hi-jacking all of that and overwriting the calcFOM() function to
    // ignore most of the work that is done during setup.
    public class InsideWithFwdBkwd extends BoundaryInOutSelector {

        public InsideWithFwdBkwd() {
        }

        @Override
        public float calcFOM(final ChartEdge edge) {
            return edge.inside();
        }

        @Override
        public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
            return insideProbability;
        }
    }
}
