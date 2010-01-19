package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map.Entry;

import edu.ohsu.cslu.counters.SimpleCounter;
import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.ArrayChartCell;
import edu.ohsu.cslu.parser.ChartEdgeWithFOM;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;

public class BoundaryInOut extends EdgeFOM {

    private ArrayGrammar grammar;
    private float leftBoundaryLogProb[][], rightBoundaryLogProb[][], posTransitionLogProb[][];
    private float outsideLeft[][], outsideRight[][];

    public BoundaryInOut(final ArrayGrammar grammar) {
        this.grammar = grammar;
    }

    @Override
    public float calcFOM(final ChartEdgeWithFOM edge) {

        if (edge.p.isLexProd()) {
            return edge.insideProb;
        }

        // leftIndex and rightIndex have +1 because the outsideLeft and outsideRight arrays
        // are padded with a begin and end <null> value which shifts the entire array to
        // the right by one
        final int spanLength = edge.end() - edge.start();
        final float outside = outsideLeft[edge.start() - 1 + 1][edge.p.parent] + outsideRight[edge.end() + 1][edge.p.parent];
        final float fom = edge.insideProb + outside + spanLength * ParserDriver.fudgeFactor;

        // System.out.println(calcFOMToString(edge));

        return fom;
    }

    public String calcFOMToString(final ChartEdgeWithFOM edge) {
        final int spanLength = edge.end() - edge.start();
        final float outside = outsideLeft[edge.start() - 1 + 1][edge.p.parent] + outsideRight[edge.end() + 1][edge.p.parent];
        final float fom = edge.insideProb + outside + spanLength * ParserDriver.fudgeFactor;

        // String s = "FOM: chart[" + edge.start() + "," + edge.end() + "]" + " n=" + spanLength + " p=" + edge.p.toString() + " i=" + edge.insideProb + " o=" + outside
        // + " oL[" + (edge.start() - 1 + 1) + "][" + grammar.nonTermSet.getSymbol(edge.p.parent) + "]=" + outsideLeft[edge.start() - 1 + 1][edge.p.parent] + " oR["
        // + (edge.end() + 1) + "][" + grammar.nonTermSet.getSymbol(edge.p.parent) + "]=" + outsideRight[edge.end() + 1][edge.p.parent] + " fom=" + fom;

        String s = "FOM: chart[" + edge.start() + "," + edge.end() + "]";
        s += " n=" + spanLength;
        s += " p=" + edge.p.toString();
        s += " i=" + edge.insideProb;
        s += " o=" + outside;
        s += " oL[" + (edge.start() - 1 + 1) + "][" + grammar.nonTermSet.getSymbol(edge.p.parent) + "]=" + outsideLeft[edge.start() - 1 + 1][edge.p.parent];
        s += " oR[" + (edge.end() + 1) + "][" + grammar.nonTermSet.getSymbol(edge.p.parent) + "]=" + outsideRight[edge.end() + 1][edge.p.parent];
        s += " fom=" + fom;

        return s;
    }

    @Override
    public void init(final ChartParser parser) {
        final double startTimeMS = System.currentTimeMillis();

        // compute forward-backwards probs across ambiguous POS tags
        // assumes all POS tags have already been placed in the chart and the
        // inside prob of the tag is the emission probability
        float score;
        final int fbSize = parser.chartSize + 2;
        final int posSize = grammar.maxPOSIndex() + 1;
        final float forward[][] = new float[fbSize][posSize];
        final float backward[][] = new float[fbSize][posSize];
        outsideLeft = new float[fbSize][grammar.numNonTerms()];
        outsideRight = new float[fbSize][grammar.numNonTerms()];
        LinkedList<Integer> prevPOSList, curPOSList;

        // TODO: we can problably make this faster by only initilizing the cells that
        // are going to be accessed (ie. only the POS cells for a given span. Not sure
        // about the outsideLeft and Right ...
        for (int i = 0; i < fbSize; i++) {
            for (int j = 0; j < posSize; j++) {
                forward[i][j] = Float.NEGATIVE_INFINITY;
                backward[i][j] = Float.NEGATIVE_INFINITY;
            }
            for (int j = 0; j < grammar.numNonTerms(); j++) {
                outsideLeft[i][j] = Float.NEGATIVE_INFINITY;
                outsideRight[i][j] = Float.NEGATIVE_INFINITY;
            }
        }

        // calculate forward probs
        prevPOSList = new LinkedList<Integer>();
        prevPOSList.add(grammar.nullSymbol);
        forward[0][grammar.nullSymbol] = (float) 0.0;
        for (int i = 1; i < fbSize; i++) {
            final int chartIndex = i - 1; // minus 1 because the fbChart is one off from the parser chart
            curPOSList = getPOSListFromChart(parser, chartIndex);
            for (final int prevPOS : prevPOSList) {
                for (final int curPOS : curPOSList) {
                    score = forward[i - 1][prevPOS] + posTransitionLogProb(curPOS, prevPOS) + posEmissionLogProb(parser, chartIndex, curPOS);
                    // System.out.println(" fw[" + (i - 1) + "][" + grammar.nonTermSet.getSymbol(curPOS) + "]=" + score + " fw[" + (i - 2) + "]["
                    // + grammar.nonTermSet.getSymbol(prevPOS) + "]=" + forward[i - 1][prevPOS] + " posTran(" + grammar.nonTermSet.getSymbol(prevPOS) + " -> "
                    // + grammar.nonTermSet.getSymbol(curPOS) + ")=" + posTransitionLogProb(curPOS, prevPOS) + " posEmis[" + chartIndex + "]["
                    // + grammar.nonTermSet.getSymbol(curPOS) + "]=" + posEmissionLogProb(parser, chartIndex, curPOS));
                    if (score > forward[i][curPOS]) {
                        forward[i][curPOS] = score;
                    }
                }
            }
            prevPOSList = curPOSList;
        }

        // calculate backwards probs
        prevPOSList = new LinkedList<Integer>();
        prevPOSList.add(grammar.nullSymbol);
        backward[fbSize - 1][grammar.nullSymbol] = (float) 0.0;
        for (int i = fbSize - 2; i >= 0; i--) {
            final int chartIndex = i - 1;
            curPOSList = getPOSListFromChart(parser, chartIndex);
            for (final int prevPOS : prevPOSList) {
                for (final int curPOS : curPOSList) {
                    score = backward[i + 1][prevPOS] + posTransitionLogProb(prevPOS, curPOS) + posEmissionLogProb(parser, chartIndex, curPOS);
                    if (score > backward[i][curPOS]) {
                        backward[i][curPOS] = score;
                    }
                }
            }
            prevPOSList = curPOSList;
        }
        // System.out.println("INFO: forward=" + forward[fbSize - 1][grammar.nullSymbol] + " backward=" + backward[0][grammar.nullSymbol]);

        // calc outside right and left probs. These are not dependent on the span
        // size so we can pre calc them before we do any parsing.
        // outside(A->B C where TL and TR are the POS tag directly left and right of the consituent)
        // outside(A->B C, TL, TR) = Fwd(TL) * P(A|TL) * P(TR|A) * Bkw(TR)
        float fbLeft, fbRight;
        for (int leftIndex = 0; leftIndex < fbSize; leftIndex++) {
            // System.out.println("i=" + leftIndex);
            final int rightIndex = fbSize - leftIndex - 1;
            for (final int pos : grammar.posSet) {
                fbLeft = forward[leftIndex][pos];
                fbRight = backward[rightIndex][pos];

                // if (fbLeft > Float.NEGATIVE_INFINITY) {
                // System.out.println("  " + grammar.nonTermSet.getSymbol(pos) + " fwd=" + forward[leftIndex][pos] + " bk=" + backward[leftIndex][pos]);
                // }
                for (int nonTerm = 0; nonTerm < grammar.numNonTerms(); nonTerm++) {
                    score = fbLeft + leftBoundaryLogProb[nonTerm][pos];
                    if (score > outsideLeft[leftIndex][nonTerm]) {
                        outsideLeft[leftIndex][nonTerm] = score;
                    }

                    score = fbRight + rightBoundaryLogProb[pos][nonTerm];
                    if (score > outsideRight[rightIndex][nonTerm]) {
                        outsideRight[rightIndex][nonTerm] = score;
                    }
                }
            }
        }

        final double totalTimeSec = (System.currentTimeMillis() - startTimeMS) / 1000.0;
        Log.info(3, "INFO: FOM.init() time = " + totalTimeSec + " seconds");
    }

    public void printTreeStats(final ParseTree tree, final ChartParser parser) {
        final LinkedList<ParseTree> leafNodes = tree.getLeafNodes();

        for (final ParseTree node : tree.postOrderTraversal()) {
            final ParseTree leftLeaf = node.leftMostLeaf();
            final ParseTree rightLeaf = node.rightMostLeaf();
            int start = -1, end = -1, i = 0;
            for (final ParseTree leaf : leafNodes) {
                if (leaf == leftLeaf)
                    start = i;
                if (leaf == rightLeaf)
                    end = i + 1;
                i += 1;
            }

            if ((end - start >= 1) && node.isNonTerminal()) {
                if (parser.grammar.nonTermSet.hasSymbol(node.contents)) {
                    final int parentNonTermIndex = parser.grammar.nonTermSet.getIndex(node.contents);
                    final ChartEdgeWithFOM edge = (ChartEdgeWithFOM) parser.chart[start][end].getBestEdge(parentNonTermIndex);
                    if (edge == null) {
                        System.out.println("WARNING: edge[" + start + "][" + end + "][" + node.contents + "] not in chart!");
                    } else {
                        System.out.print("INPUT_TREE: ");
                        this.calcFOM(edge);
                    }
                } else {
                    System.out.println("WARNING: '" + node.contents + "' not in nonTermSet!");
                }
            }
        }
    }

    private LinkedList<Integer> getPOSListFromChart(final ChartParser parser, final int startIndex) {
        final int endIndex = startIndex + 1;
        if (startIndex < 0 || endIndex > parser.chartSize) {
            final LinkedList<Integer> posList = new LinkedList<Integer>();
            posList.addLast(grammar.nullSymbol);
            return posList;
        }
        return ((ArrayChartCell) parser.chart[startIndex][endIndex]).getPosEntries();
    }

    public float leftBoundaryLogProb(final int nonTerm, final int pos) {
        return leftBoundaryLogProb[nonTerm][pos];
    }

    public float rightBoundaryLogProb(final int pos, final int nonTerm) {
        return rightBoundaryLogProb[pos][nonTerm];
    }

    public float posTransitionLogProb(final int pos, final int histPos) {
        return posTransitionLogProb[pos][histPos];
    }

    public float posEmissionLogProb(final ChartParser parser, final int start, final Integer pos) {
        final int end = start + 1;
        if (pos == grammar.nullSymbol && (start < 0 || end > parser.chartSize)) {
            return 0; // log(1.0)
        }
        return parser.chart[start][end].getBestEdge(pos).insideProb;
    }

    @Override
    public void readModel(final BufferedReader inStream) throws IOException {
        final int numNT = grammar.numNonTerms();
        final int maxPOSIndex = grammar.maxPOSIndex();
        // System.out.println("numNT=" + numNT + " maxPOS=" + maxPOSIndex);
        // final int numPOS = grammar.posSet.size();
        leftBoundaryLogProb = new float[numNT][maxPOSIndex + 1];
        rightBoundaryLogProb = new float[maxPOSIndex + 1][numNT];
        posTransitionLogProb = new float[maxPOSIndex + 1][maxPOSIndex + 1];

        String line, numStr, denomStr;
        int numIndex, denomIndex;
        float prob;
        while ((line = inStream.readLine()) != null) {
            // line format: label num1 num2 ... | den1 den2 ... prob
            final String[] tokens = ParserUtil.tokenize(line);
            if (tokens.length > 0 && !tokens[0].equals("#")) {
                final LinkedList<String> numerator = new LinkedList<String>();
                final LinkedList<String> denom = new LinkedList<String>();
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
                numIndex = grammar.nonTermSet.getIndex(numStr);
                denomStr = ParserUtil.join(denom, " ");
                denomIndex = grammar.nonTermSet.getIndex(denomStr);
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
        final SymbolSet<String> nonTermSet = grammar.nonTermSet;

        // left boundary = P(NT | POS-1)
        for (final int leftPOSIndex : grammar.posSet) {
            final String posStr = nonTermSet.getSymbol(leftPOSIndex);
            for (int ntIndex = 0; ntIndex < grammar.numNonTerms(); ntIndex++) {
                final String ntStr = nonTermSet.getSymbol(ntIndex);
                final float logProb = leftBoundaryLogProb[ntIndex][leftPOSIndex];
                outStream.write("LB " + ntStr + " | " + posStr + " " + logProb + "\n");
            }
        }

        // right boundary = P(POS+1 | NT)
        for (int ntIndex = 0; ntIndex < grammar.numNonTerms(); ntIndex++) {
            final String ntStr = nonTermSet.getSymbol(ntIndex);
            for (final int rightPOSIndex : grammar.posSet) {
                final String posStr = nonTermSet.getSymbol(rightPOSIndex);
                final float logProb = rightBoundaryLogProb[rightPOSIndex][ntIndex];
                outStream.write("RB " + posStr + " | " + ntStr + " " + logProb + "\n");
            }
        }

        // pos n-gram = P(POS | POS-1)
        for (final int histPos : grammar.posSet) {
            final String histPosStr = nonTermSet.getSymbol(histPos);
            for (final int pos : grammar.posSet) {
                final String posStr = nonTermSet.getSymbol(pos);
                final float logProb = posTransitionLogProb[pos][histPos];
                outStream.write("PN " + posStr + " | " + histPosStr + " " + logProb + "\n");
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
                history.addLast(grammar.nullSymbolStr); // pad history with nulls (for beginning of string)
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
            posTransitionCount.increment(grammar.nullSymbolStr, historyStr);
        }

        final int numNT = grammar.numNonTerms();
        final int maxPOSIndex = grammar.maxPOSIndex();
        final int numPOS = grammar.posSet.size();

        // System.out.println("numNT=" + numNT + " maxPOSIndex=" + maxPOSIndex + " numPOS=" + numPOS);

        // smooth counts
        leftBoundaryCount.smoothAddConst(0.5, numPOS);
        rightBoundaryCount.smoothAddConst(0.5, numNT);
        posTransitionCount.smoothAddConst(0.5, numPOS);

        // turn counts into probs
        final SymbolSet<String> nonTermSet = grammar.nonTermSet;
        leftBoundaryLogProb = new float[numNT][maxPOSIndex + 1];
        rightBoundaryLogProb = new float[maxPOSIndex + 1][numNT];
        posTransitionLogProb = new float[maxPOSIndex + 1][maxPOSIndex + 1];

        // left boundary = P(NT | POS-1)
        for (final int leftPOSIndex : grammar.posSet) {
            final String posStr = nonTermSet.getSymbol(leftPOSIndex);
            for (int ntIndex = 0; ntIndex < numNT; ntIndex++) {
                final String ntStr = nonTermSet.getSymbol(ntIndex);
                leftBoundaryLogProb[ntIndex][leftPOSIndex] = (float) Math.log(leftBoundaryCount.getProb(ntStr, posStr));
            }
        }

        // right boundary = P(POS+1 | NT)
        for (int ntIndex = 0; ntIndex < grammar.numNonTerms(); ntIndex++) {
            final String ntStr = nonTermSet.getSymbol(ntIndex);
            for (final int rightPOSIndex : grammar.posSet) {
                final String posStr = nonTermSet.getSymbol(rightPOSIndex);
                rightBoundaryLogProb[rightPOSIndex][ntIndex] = (float) Math.log(rightBoundaryCount.getProb(posStr, ntStr));
            }
        }

        // pos n-gram = P(POS | POS-1)
        for (final int histPos : grammar.posSet) {
            final String histPosStr = nonTermSet.getSymbol(histPos);
            for (final int pos : grammar.posSet) {
                final String posStr = nonTermSet.getSymbol(pos);
                posTransitionLogProb[pos][histPos] = (float) Math.log(posTransitionCount.getProb(posStr, histPosStr));
            }
        }
    }

    private String convertNull(final String nonTerm) {
        if (nonTerm == null) {
            return grammar.nullSymbolStr;
        }
        return nonTerm;
    }

    public void writeModelCounts(final BufferedWriter outStream, final SimpleCounterSet<String> leftBoundaryCount, final SimpleCounterSet<String> rightBoundaryCount,
            final SimpleCounterSet<String> posNgramCount) throws IOException {

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

    /*
     * // NOT USED ANY MORE ... BUT KEEPING AROUND FOR NOW // p.s. it's very very slow
     * 
     * public float calcFOMOnTheFly(final ChartEdgeWithFOM edge, final ChartParser parser) {
     * 
     * if (edge.p.isLexProd()) { return edge.insideProb; }
     * 
     * // doing viterbi max. no back ptrs needed because we don't need to get the best // sequence ... we just need to know what the best score is float score; final int nonTerm =
     * edge.p.parent; // final SymbolSet<String> symbols = parser.grammar.nonTermSet; float prevDpScore[] = new float[parser.grammar.numNonTerms()]; final float dpScore[] = new
     * float[parser.grammar.numNonTerms()]; final int start = edge.leftCell.start; LinkedList<Integer> prevPOSList = getPOSListFromChart(parser, start - 1); LinkedList<Integer>
     * curPOSList;
     * 
     * // System.out.println("FOM: " + edge);
     * 
     * // init with P(nt | pos-1) for (final Integer pos : prevPOSList) { prevDpScore[pos] = leftBoundaryLogProb(nonTerm, pos) + posEmissionLogProb(parser, start - 1, pos); //
     * System.out.println("  LB[" + symbols.getSymbol(nonTerm) + "," + symbols.getSymbol(pos) + "]=" + leftBoundaryLogProb(nonTerm, pos) + " pEmis[" + (start - 1) + "," // +
     * symbols.getSymbol(pos) + "]=" + posEmissionLogProb(parser, start - 1, pos)); }
     * 
     * // POS n-gram prob // System.out.println("  POS prob for [" + edge.leftCell.start + "," + (rightCell.end) + "]"); for (int i = edge.start(); i <= edge.end(); i++) {
     * curPOSList = getPOSListFromChart(parser, i); Arrays.fill(dpScore, Float.NEGATIVE_INFINITY); // System.out.println("   i=" + i + " prevPOS=" + posListToStr(prevPOSList) +
     * " curPOS=" + posListToStr(curPOSList)); for (final int prevPOS : prevPOSList) { for (final int pos : curPOSList) { score = prevDpScore[prevPOS] + posTransitionLogProb(pos,
     * prevPOS) + posEmissionLogProb(parser, i, pos); // System.out.println("     score=" + score + " prevScore[" + symbols.getSymbol(prevPOS) + "]=" + prevDpScore[prevPOS] +
     * " posT[" + symbols.getSymbol(prevPOS) // + "," + symbols.getSymbol(pos) + "]=" + posTransitionLogProb(prevPOS, pos) + " posE[" + i + "," + symbols.getSymbol(pos) + "]=" // +
     * posEmissionLogProb(parser, i, pos)); if (score > dpScore[pos]) { // System.out.println("     update: dpScore[" + symbols.getSymbol(pos) + "]=" + score + " oldScore=" +
     * dpScore[pos]); dpScore[pos] = score; } } } prevPOSList = curPOSList; prevDpScore = dpScore.clone(); // since we're reusing the same array above and filling it with NEG_INF,
     * we need to make a copy here }
     * 
     * // finalize with P(pos+1 | nt) float outsideProb, bestOutside = Float.NEGATIVE_INFINITY; for (final int pos : prevPOSList) { outsideProb = prevDpScore[pos] +
     * rightBoundaryLogProb(pos, nonTerm); if (outsideProb > bestOutside) { bestOutside = outsideProb; } }
     * 
     * return edge.insideProb + bestOutside; }
     * 
     * 
     * private String posListToStr(final LinkedList<Integer> posList) { final LinkedList<String> newList = new LinkedList<String>(); for (final int i : posList) {
     * newList.add(grammar.nonTermSet.getSymbol(i)); } return newList.toString(); }
     */

}
