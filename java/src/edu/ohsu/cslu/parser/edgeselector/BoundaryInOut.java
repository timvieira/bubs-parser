package edu.ohsu.cslu.parser.edgeselector;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map.Entry;

import edu.ohsu.cslu.counters.SimpleCounter;
import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;

public final class BoundaryInOut extends EdgeSelector {

    private Grammar grammar;
    private final float leftBoundaryLogProb[][], rightBoundaryLogProb[][], posTransitionLogProb[][];

    private float outsideLeft[][], outsideRight[][];

    private IntOpenHashSet posSet = new IntOpenHashSet();
    private IntOpenHashSet clauseNonTermSet = new IntOpenHashSet();

    public BoundaryInOut(final Grammar grammar, final BufferedReader modelStream) throws IOException {
        this.grammar = grammar;
        // TODO: should we record this in Grammar instead?
        for (int ntIndex = 0; ntIndex < grammar.numNonTerms(); ntIndex++) {
            if (grammar.getNonterminal(ntIndex).isPOS()) {
                posSet.add(ntIndex);
            } else {
                clauseNonTermSet.add(ntIndex);
            }
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

    public BoundaryInOut(final Grammar grammar) throws IOException {
        this(grammar, null);
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
        final float outside = outsideLeft[edge.start() - 1 + 1][edge.prod.parent]
                + outsideRight[edge.end() + 1][edge.prod.parent];
        return edge.inside() + outside;
    }

    @Override
    public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
        // leftIndex and rightIndex have +1 because the outsideLeft and outsideRight arrays
        // are padded with a begin and end <null> value which shifts the entire array to
        // the right by one
        // final int spanLength = edge.end() - edge.start();

        final float outside = outsideLeft[start - 1 + 1][parent] + outsideRight[end + 1][parent];
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

    @Override
    public void init(final Chart chart) {
        // Computes forward-backward and left/right boundary probs across ambiguous
        // POS tags. Assumes all POS tags have already been placed in the chart and the
        // inside prob of the tag is the emission probability.

        final int fbSize = chart.size() + 2;
        final int posSize = grammar.maxPOSIndex() + 1;
        outsideLeft = new float[fbSize][grammar.numNonTerms()];
        outsideRight = new float[fbSize][grammar.numNonTerms()];
        for (int i = 0; i < fbSize; i++) {
            Arrays.fill(outsideLeft[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(outsideRight[i], Float.NEGATIVE_INFINITY);
        }

        final short nullSymbol = (short) grammar.nullSymbol;
        final short[] NULL_LIST = new short[] { nullSymbol };

        short[] prevFwdPOSList = NULL_LIST;
        float[] prevFwdScores = new float[posSize];
        prevFwdScores[nullSymbol] = 0f;

        short[] prevBkwPOSList = NULL_LIST;
        float[] prevBkwScores = new float[posSize];
        prevBkwScores[nullSymbol] = 0f;

        for (int fwdIndex = 0; fwdIndex < fbSize; fwdIndex++) {
            final int fwdChartIndex = fwdIndex - 1; // -1 because the fbChart is one off from the parser chart
            final int bkwIndex = fbSize - fwdIndex - 1;
            final int bkwChartIndex = bkwIndex - 1;

            final boolean pastEndOfChart = fwdChartIndex >= chart.size();
            if (fwdIndex > 0) {
                // moving from left to right =======>>
                final short[] fwdPOSList = pastEndOfChart ? NULL_LIST : grammar
                        .lexicalParents(chart.tokens[fwdChartIndex]);

                final float[] fwdScores = new float[posSize];
                Arrays.fill(fwdScores, Float.NEGATIVE_INFINITY);

                for (final short curPOS : fwdPOSList) {
                    final float posEmissionLogProb = pastEndOfChart ? 0f : grammar.lexicalLogProbability(curPOS,
                            chart.tokens[fwdChartIndex]);
                    float winningScore = Float.NEGATIVE_INFINITY;

                    for (final short prevPOS : prevFwdPOSList) {
                        final float score = prevFwdScores[prevPOS] + posTransitionLogProb(curPOS, prevPOS)
                                + posEmissionLogProb;
                        if (score > winningScore) {
                            winningScore = score;
                        }
                    }

                    fwdScores[curPOS] = winningScore;
                }
                prevFwdScores = fwdScores;
                prevFwdPOSList = fwdPOSList;

                // moving from right to left <<=======
                final boolean pastBeginningOfChart = bkwChartIndex < 0;
                final short[] bkwPOSList = pastBeginningOfChart ? NULL_LIST : grammar
                        .lexicalParents(chart.tokens[bkwChartIndex]);
                final float[] bkwScores = new float[posSize];
                Arrays.fill(bkwScores, Float.NEGATIVE_INFINITY);

                for (final short curPOS : bkwPOSList) {
                    final float posEmissionLogProb = pastBeginningOfChart ? 0f : grammar.lexicalLogProbability(curPOS,
                            chart.tokens[bkwChartIndex]);
                    float winningScore = Float.NEGATIVE_INFINITY;

                    for (final short prevPOS : prevBkwPOSList) {
                        final float score = prevBkwScores[prevPOS] + posTransitionLogProb(prevPOS, curPOS)
                                + posEmissionLogProb;
                        if (score > winningScore) {
                            winningScore = score;
                        }
                    }

                    bkwScores[curPOS] = winningScore;
                }
                prevBkwScores = bkwScores;
                prevBkwPOSList = bkwPOSList;
            }

            // compute left and right outside scores to be used during decoding
            // to calculate the FOM = outsideLeft[i][A] * inside[i][j][A] * outsideRight[j][A]
            for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {

                for (final short pos : prevFwdPOSList) {
                    final float score = prevFwdScores[pos] + leftBoundaryLogProb[nonTerm][pos];
                    if (score > outsideLeft[fwdIndex][nonTerm]) {
                        outsideLeft[fwdIndex][nonTerm] = score;
                    }
                }

                for (final short pos : prevBkwPOSList) {
                    final float score = prevBkwScores[pos] + rightBoundaryLogProb[pos][nonTerm];
                    if (score > outsideRight[bkwIndex][nonTerm]) {
                        outsideRight[bkwIndex][nonTerm] = score;
                    }
                }
            }
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

    @Override
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

                if (tokens[0].equals("LB")) {
                    leftBoundaryLogProb[numIndex][denomIndex] = prob;
                } else if (tokens[0].equals("RB")) {
                    rightBoundaryLogProb[numIndex][denomIndex] = prob;
                } else if (tokens[0].equals("PN")) {
                    posTransitionLogProb[numIndex][denomIndex] = prob;
                } else {
                    Log.info(5, "WARNING: ignoring line in model file '" + line + "'");
                }
            }
        }

    }

    @Override
    public void writeModel(final BufferedWriter outStream) throws IOException {

        // left boundary = P(NT | POS-1)
        for (final int leftPOSIndex : posSet) {
            final String posStr = grammar.mapNonterminal(leftPOSIndex);
            for (final int ntIndex : clauseNonTermSet) {
                final String ntStr = grammar.mapNonterminal(ntIndex);
                final float logProb = leftBoundaryLogProb[ntIndex][leftPOSIndex];
                if (logProb > Float.NEGATIVE_INFINITY) {
                    outStream.write("LB " + ntStr + " | " + posStr + " " + logProb + "\n");
                }
            }
        }

        // right boundary = P(POS+1 | NT)
        for (final int ntIndex : clauseNonTermSet) {
            final String ntStr = grammar.mapNonterminal(ntIndex);
            for (final int rightPOSIndex : posSet) {
                final String posStr = grammar.mapNonterminal(rightPOSIndex);
                final float logProb = rightBoundaryLogProb[rightPOSIndex][ntIndex];
                if (logProb > Float.NEGATIVE_INFINITY) {
                    outStream.write("RB " + posStr + " | " + ntStr + " " + logProb + "\n");
                }
            }
        }

        // pos n-gram = P(POS | POS-1)
        for (final int histPos : posSet) {
            final String histPosStr = grammar.mapNonterminal(histPos);
            for (final int pos : posSet) {
                final String posStr = grammar.mapNonterminal(pos);
                final float logProb = posTransitionLogProb[pos][histPos];
                if (logProb > Float.NEGATIVE_INFINITY) {
                    outStream.write("PN " + posStr + " | " + histPosStr + " " + logProb + "\n");
                }
            }
        }
        outStream.close();
    }

    @Override
    public void train(final BufferedReader inStream) throws Exception {
        String line, historyStr;
        final String joinString = " ";
        ParseTree tree;
        final SimpleCounterSet<String> leftBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> rightBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> posTransitionCount = new SimpleCounterSet<String>();
        final int posNgramOrder = 2;

        // TODO: note that we have to have the same training grammar as decoding grammar here
        // so the input needs to be bianarized. How are we going to get gold trees in the
        // Berkeley grammar format? Can we just use the 1-best? Or maybe an inside/outside
        // estimate. See Caraballo/Charniak 1998 for (what I think is) their inside/outside solution
        // to the same (or a similar) problem.
        while ((line = inStream.readLine()) != null) {
            tree = ParseTree.readBracketFormat(line);
            if (tree.isBinaryTree() == false) {
                Log.info(0, "ERROR: Training trees must be binarized exactly as used in decoding");
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
        final int numPOS = posSet.size();

        // System.out.println("numNT=" + numNT + " maxPOSIndex=" + maxPOSIndex + " numPOS=" + numPOS);

        // smooth counts
        leftBoundaryCount.smoothAddConst(0.5, numPOS);
        rightBoundaryCount.smoothAddConst(0.5, numNT);
        posTransitionCount.smoothAddConst(0.5, numPOS);

        // turn counts into probs

        // left boundary = P(NT | POS-1)
        for (final int leftPOSIndex : posSet) {
            final String posStr = grammar.mapNonterminal(leftPOSIndex);
            for (int ntIndex = 0; ntIndex < numNT; ntIndex++) {
                final String ntStr = grammar.mapNonterminal(ntIndex);
                leftBoundaryLogProb[ntIndex][leftPOSIndex] = (float) Math.log(leftBoundaryCount.getProb(ntStr, posStr));
            }
        }

        // right boundary = P(POS+1 | NT)
        for (int ntIndex = 0; ntIndex < grammar.numNonTerms(); ntIndex++) {
            final String ntStr = grammar.mapNonterminal(ntIndex);
            for (final int rightPOSIndex : posSet) {
                final String posStr = grammar.mapNonterminal(rightPOSIndex);
                rightBoundaryLogProb[rightPOSIndex][ntIndex] = (float) Math.log(rightBoundaryCount.getProb(posStr,
                        ntStr));
            }
        }

        // pos n-gram = P(POS | POS-1)
        for (final int histPos : posSet) {
            final String histPosStr = grammar.mapNonterminal(histPos);
            for (final int pos : posSet) {
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
}
