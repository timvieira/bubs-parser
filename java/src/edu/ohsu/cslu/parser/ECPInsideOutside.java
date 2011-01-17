package edu.ohsu.cslu.parser;

import java.util.Arrays;
import java.util.LinkedList;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.ProjectedGrammar;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.InOutCellChart;
import edu.ohsu.cslu.parser.chart.InOutCellChart.ChartCell;

public class ECPInsideOutside extends ChartParser<LeftListGrammar, InOutCellChart> {

    ProjectedGrammar evalGrammar;

    // notes:
    // keep a single maxc[start][end][nt] array for each cell
    // for unaries, make a local copy, maxcUnary[nt] and reference
    // maxc[start][end][nt] for the binary scores. Then replace
    // maxc with maxcUnary scores that are higher. Must also maintain
    // a maxcUnaryPtr array to designate the unary child or not (-1)

    // note2:
    // maxc is just like another inside/outside pass -- we don't need
    // to know if the inside/outside probs were constructed by unary
    // or binary edges; we just do a maxc pass on the binaries, then
    // a maxc pass building unaries.

    // all maxc arrays are [start][end][nt]
    // TODO: the orignial goodman algorithm doesn't keep [nt] for each cell .. there is just one
    // TODO: add these fields to a chart
    // protected MaxcStruct[][][] maxc;

    // float unaryInside[][][];
    // float unaryOutside[][][];

    public ECPInsideOutside(final ParserDriver opts, final LeftListGrammar grammar) {
        super(opts, grammar);

        // evalGrammar = new ProjectedGrammar(grammar);
    }

    public class MaxcStruct {
        public float score;
        public int split, leftChild, rightChild;
        public int unaryChild; // -1 means no unary (or self loop to binary)
    }

    @Override
    protected void initSentence(final int[] tokens) {
        chart = new InOutCellChart(tokens, opts.viterbiMax(), this);

        // TODO: compress this so it's n*(n/2) instead of n*n
        // final int n = tokens.length + 1;
        // TODO: this should just be a new type of chart
        // maxc = new MaxcStruct[n][n][grammar.numNonTerms()];

        // for (int start = 0; start < n - 1; start++) {
        // for (int end = start + 1; end < n; end++) {
        // //maxc[start][end]
        //
        // }
        // }
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) throws Exception {
        final LinkedList<ChartCell> topDownTraversal = new LinkedList<ChartCell>();

        initSentence(tokens);
        cellSelector.initSentence(this);
        edgeSelector.init(chart);
        addLexicalProductions(tokens);

        while (cellSelector.hasNext()) {
            final short[] startEnd = cellSelector.next();
            visitCell(startEnd[0], startEnd[1]);
            topDownTraversal.addFirst(chart.getCell(startEnd[0], startEnd[1]));
        }

        for (final ChartCell cell : topDownTraversal) {
            computeOutsideProbsInCell(cell);
        }

        if (ParserDriver.param1 == -1) {
            goodmanMaximizeLabelRecall();
        } else {
            // berkeleyMaxRule(tokens);
            berkeleyMaxRuleNoMarginalize(tokens);
        }

        // return chart.extractBestParse(evalGrammar.startSymbol);
        return chart.extractBestParse(grammar.startSymbol);
    }

    // find latent grammar max rule score for evaluation
    // grammar rule A => B C by summing over all A_x => B_y C_z
    // for all x, y, and z. Label constituent with A.
    // protected void berkeleyMaxRule(final int sent[]) {
    //
    // // throw new UnsupportedOperationException("berkeleyMaxRule() not implemented.");
    // // NOTE: does not work
    //
    // final boolean maxRuleProduct = true;
    // final int n = chart.size();
    // final float maxcScore[][][] = new float[n][n + 1][evalGrammar.numNonTerms()];
    // HashMap<Production, Float> evalProdSet;
    // // Production evalProd;
    // // float ruleScore;
    // final float stringProb = chart.getInside(0, n, grammar.startSymbol);
    //
    // for (int span = 1; span <= n; span++) {
    // for (int start = 0; start < n - span + 1; start++) {
    // final int end = start + span;
    // final ChartCell cell = chart.getCell(start, end);
    // Arrays.fill(maxcScore[start][end], Float.NEGATIVE_INFINITY);
    //
    // for (int mid = start + 1; mid < end; mid++) { // mid point
    // final ChartCell leftCell = chart.getCell(start, mid);
    // final ChartCell rightCell = chart.getCell(mid, end);
    //
    // evalProdSet = new HashMap<Production, Float>();
    // for (final int leftNT : leftCell.getLeftChildNTs()) {
    // for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
    // if (rightCell.hasNT(p.rightChild)) {
    // // System.out.println(" considering: " + p + " in=" + cell.getInside(p.parent)
    // // + " out=" + cell.getOutside(p.parent));
    // float ruleScore = cell.getOutside(p.parent) + p.prob + leftCell.getInside(leftNT)
    // + rightCell.getInside(p.rightChild);
    // if (ruleScore > Float.NEGATIVE_INFINITY) {
    // if (evalProdSet.containsKey(p.projProd)) {
    // ruleScore = (float) ParserUtil.logSum(ruleScore, evalProdSet.get(p.projProd));
    // // System.out.println("inc rule score=" + ruleScore + " p=" + p + " evalProd="
    // // + evalProd);
    // }
    // evalProdSet.put(p.projProd, ruleScore);
    //
    // // update maxc -- intuitively, this is done after marginalizing over
    // // all the rules, but we can do it during the process since scores
    // // are always increasing
    // final float tmpMaxcScore = ruleScore + maxcScore[start][mid][p.projProd.leftChild]
    // + maxcScore[mid][end][p.projProd.rightChild];
    // if (tmpMaxcScore > maxcScore[start][end][p.projProd.parent]) {
    // maxcScore[start][end][p.projProd.parent] = tmpMaxcScore;
    // cell.bestEdge[p.projProd.parent] = chart.new ChartEdge(p.projProd, leftCell,
    // rightCell);
    // System.out.println(maxcScore[start][end][p.projProd.parent] + "\t"
    // + cell.bestEdge[p.projProd.parent]);
    // }
    // }
    // }
    // }
    // }
    //
    // // // latent variables on each productions have now been marginalized
    // // for(Map.Entry<Production, Float> entry : evalProdSet.entrySet()) {
    // // Production prod = entry.getKey();
    // // float score = entry.getValue() + maxcScore[start][mid][prod.leftChild] +
    // // maxcScore[mid][end][prod.rightChild];
    // // if (score > maxcScore[start][end][prod.parent]) {
    // // maxcScore[start][end][prod.parent] = score;
    // // cell.bestEdge[prod.parent] = chart.new ChartEdge(prod, leftCell, rightCell);
    // // // System.out.println(maxScore[start][end][A] + "\t" + cell.bestEdge[A]);
    // // }
    // // }
    // }
    //
    // evalProdSet = new HashMap<Production, Float>();
    // if (span == 1) {
    // // lexical productions
    // final int wordIndex = sent[start];
    // for (final Production lexProd : grammar.getLexicalProductionsWithChild(wordIndex)) {
    // float ruleScore = cell.getOutside(lexProd.parent) + lexProd.prob; // inside=1.0
    // if (ruleScore > Float.NEGATIVE_INFINITY) {
    // final int A = evalGrammar.projectNonTerm(lexProd.parent);
    // final Production evalProd = evalGrammar.new Production(A, wordIndex,
    // Float.NEGATIVE_INFINITY, true);
    // if (evalProdSet.containsKey(evalProd)) {
    // ruleScore = (float) ParserUtil.logSum(ruleScore, evalProdSet.get(evalProd));
    // }
    // evalProdSet.put(evalProd, ruleScore);
    //
    // // ruleScore is maxcScore for lexical prods
    // if (ruleScore > maxcScore[start][end][A]) {
    // maxcScore[start][end][A] = ruleScore;
    // cell.bestEdge[A] = chart.new ChartEdge(evalProd, cell);
    // System.out.println(maxcScore[start][end][A] + "\t" + cell.bestEdge[A]);
    // }
    // }
    // }
    // }
    //
    // // unary productions
    // final float maxcUnaryScores[] = maxcScore[start][end].clone();
    // for (final int childNT : cell.getNtArray()) {
    // for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
    // // System.out.println(" considering: " + p + " in=" + cell.getInside(p.parent) +
    // // " out=" + cell.getOutside(p.parent));
    // float ruleScore = cell.getOutside(p.parent) + p.prob + cell.getInside(p.child());
    // if (ruleScore > Float.NEGATIVE_INFINITY) {
    // if (evalProdSet.containsKey(p.projProd)) {
    // ruleScore = (float) ParserUtil.logSum(ruleScore, evalProdSet.get(p.projProd));
    // }
    // evalProdSet.put(p.projProd, ruleScore);
    //
    // final float tmpMaxcScore = ruleScore + maxcScore[start][end][p.projProd.child()];
    // if (tmpMaxcScore > maxcUnaryScores[p.projProd.parent]) {
    // maxcUnaryScores[p.projProd.parent] = tmpMaxcScore;
    // cell.bestEdge[p.projProd.parent] = chart.new ChartEdge(p.projProd, cell);
    // System.out.println(maxcScore[start][end][p.projProd.parent] + "\t"
    // + cell.bestEdge[p.projProd.parent]);
    // }
    // }
    // }
    // }
    // maxcScore[start][end] = maxcUnaryScores;
    // }
    // }
    // }

    protected void berkeleyMaxRuleNoMarginalize(final int sent[]) {

        // logger.finer("berkeleyMaxRuleNoMarginalize()...");
        // boolean maxRuleProduct = true;

        final int n = chart.size();
        final float maxcScore[][][] = new float[n][n + 1][grammar.numNonTerms()];

        float stringProb = 0;
        if (ParserDriver.param3 == -1) {
            stringProb = chart.getInside(0, n, grammar.startSymbol);
        }

        for (int span = 1; span <= n; span++) {
            for (int start = 0; start < n - span + 1; start++) {
                final int end = start + span;
                final ChartCell cell = chart.getCell(start, end);
                Arrays.fill(maxcScore[start][end], Float.NEGATIVE_INFINITY);
                Arrays.fill(cell.bestEdge, null);

                if (span == 1) {
                    // lexical productions
                    final int wordIndex = sent[start];
                    for (final Production lexProd : grammar.getLexicalProductionsWithChild(wordIndex)) {
                        final int A = lexProd.parent;
                        final float gScore = cell.getOutside(A) + lexProd.prob - stringProb; // inside=1.0

                        if (gScore > maxcScore[start][end][A]) {
                            maxcScore[start][end][A] = gScore;
                            cell.bestEdge[A] = chart.new ChartEdge(lexProd, cell);
                            // logger.finest("maxc=" + maxcScore[start][end][A] + "\t" + cell.bestEdge[A]);
                        }
                    }
                } else {
                    // binary rules
                    for (int mid = start + 1; mid < end; mid++) {
                        final ChartCell leftCell = chart.getCell(start, mid);
                        final ChartCell rightCell = chart.getCell(mid, end);

                        for (final int leftNT : leftCell.getLeftChildNTs()) {
                            for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                                if (rightCell.hasNT(p.rightChild)) {
                                    final int A = p.parent;
                                    final float ruleScore = cell.getOutside(A) + p.prob + leftCell.getInside(leftNT)
                                            + rightCell.getInside(p.rightChild) - stringProb;
                                    final float gScore = ruleScore + maxcScore[start][mid][p.leftChild]
                                            + maxcScore[mid][end][p.rightChild];
                                    if (gScore > maxcScore[start][end][A]) {
                                        maxcScore[start][end][A] = gScore;
                                        cell.bestEdge[A] = chart.new ChartEdge(p, leftCell, rightCell);
                                        // logger.finest("maxc=" + maxcScore[start][end][A] + "\t" + cell.bestEdge[A]);
                                    }
                                }
                            }
                        }
                    }
                }

                // unary productions
                final float maxcUnaryScores[] = maxcScore[start][end].clone();
                for (final int childNT : cell.getNtArray()) {
                    for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                        final int A = p.parent;
                        final float ruleScore = cell.getOutside(A) + p.prob + cell.getInside(p.child()) - stringProb;
                        final float gScore = ruleScore + maxcScore[start][end][p.child()];

                        if (gScore > maxcUnaryScores[A]) {
                            maxcUnaryScores[A] = gScore;
                            cell.bestEdge[A] = chart.new ChartEdge(p, cell);
                            // logger.finest("maxc=" + maxcUnaryScores[A] + "\t" + cell.bestEdge[A]);
                        }
                    }
                }
                maxcScore[start][end] = maxcUnaryScores;
            }
        }
    }

    // Adaptation of Goodman's max label recall to include unaries
    // See: Joshua Goodman, Parsing Algorithms and Metrics, Section 3
    protected void goodmanMaximizeLabelRecallWithUnaries() {

        // TODO: the original goodman paper just keeps the single best NT per cell
        // but the berkeley code keeps the maxc score for all NTs. The problem is
        // unaries, but I think we can get away with keeping just one per cell if we:
        // 1) compute just binary rules for ALL nts -- save array for unary pass
        // then throw it away only keeping the best entry
        // 2) compute maxc scores for unary extensions -- if a unary extension has a
        // better maxc score than any binary rule, use it instead

        // NOTE: we could do this in one pass by using the FOM outside guess
        // while doing a bottom-up search. Should compare with actual outside scores.

        // TODO: map Berkeley NTs down to Treebank NTs (make sure they do this in the Berkeley parser)
        // TODO: need to try brian's idea of not collecting any score/penalty for factored non-terms
        // since they aren't evaluated

        // throw new UnsupportedOperationException("goodmanMaximizeLabelRecallWithUnaries() not implemented yet.");
        // NOTE: does not work

        final int n = chart.size();
        final float maxc[][] = new float[n][n + 1];
        final int bestNT[][] = new int[n][n + 1];
        for (int i = 0; i < n; i++) {
            Arrays.fill(maxc[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(bestNT[i], -1);
        }

        final float stringProb = chart.getInside(0, n, grammar.startSymbol);

        for (int span = 1; span <= n; span++) {
            for (int start = 0; start < n - span + 1; start++) {
                final int end = start + span;
                final ChartCell cell = chart.getCell(start, end);

                // find best NT in a cell
                for (int nt = 0; nt < grammar.numNonTerms(); nt++) {
                    final float normInOut = cell.getInside(nt) + cell.getOutside(nt) - stringProb;
                    if (normInOut > maxc[start][end] && nt != grammar.startSymbol) {
                        maxc[start][end] = normInOut;
                        bestNT[start][end] = nt;
                    }
                }

                if (span == 1) {
                    addBackptrToChart(start, -1, end, bestNT[start][end], chart.tokens[start],
                            Production.LEXICAL_PRODUCTION);
                } else if (maxc[start][end] > Float.NEGATIVE_INFINITY) {
                    // find best midpoint
                    float maxSplitScore = Float.NEGATIVE_INFINITY;
                    int maxSplitMid = -1;
                    for (int mid = start + 1; mid < end; mid++) {
                        final float split = (float) ParserUtil.logSum(maxc[start][mid], maxc[mid][end]);
                        if (split > maxSplitScore) {
                            maxSplitScore = split;
                            maxSplitMid = mid;
                        }
                    }

                    if (maxSplitMid > -1) {
                        // add split cost for binary rules
                        maxc[start][end] = (float) ParserUtil.logSum(maxc[start][end], maxSplitScore);
                        addBackptrToChart(start, maxSplitMid, end, bestNT[start][end], bestNT[start][maxSplitMid],
                                bestNT[maxSplitMid][end]);
                    }
                }

                if (span == n) {
                    addBackptrToChart(start, -1, end, grammar.startSymbol, bestNT[start][end],
                            Production.UNARY_PRODUCTION);
                }
            }
        }
    }

    // See: Joshua Goodman, Parsing Algorithms and Metrics, Section 3
    // http://research.microsoft.com/en-us/um/people/joshuago/maximum.ps
    protected void goodmanMaximizeLabelRecall() {

        final int n = chart.size();
        final float maxc[][] = new float[n][n + 1];
        final int bestNT[][] = new int[n][n + 1];
        for (int i = 0; i < n; i++) {
            Arrays.fill(maxc[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(bestNT[i], -1);
        }

        final float stringProb = chart.getInside(0, n, grammar.startSymbol);

        for (int span = 1; span <= n; span++) {
            for (int start = 0; start < n - span + 1; start++) {
                final int end = start + span;
                final ChartCell cell = chart.getCell(start, end);

                for (int nt = 0; nt < grammar.numNonTerms(); nt++) {
                    final float normInOut = cell.getInside(nt) + cell.getOutside(nt) - stringProb;
                    if (normInOut > maxc[start][end] && nt != grammar.startSymbol) {
                        maxc[start][end] = normInOut;
                        bestNT[start][end] = nt;
                    }
                }

                if (span == 1) {
                    addBackptrToChart(start, -1, end, bestNT[start][end], chart.tokens[start],
                            Production.LEXICAL_PRODUCTION);
                } else if (maxc[start][end] > Float.NEGATIVE_INFINITY) {
                    float maxSplitScore = Float.NEGATIVE_INFINITY;
                    int maxSplitMid = -1;
                    for (int mid = start + 1; mid < end; mid++) {
                        final float split = (float) ParserUtil.logSum(maxc[start][mid], maxc[mid][end]);
                        if (split > maxSplitScore) {
                            maxSplitScore = split;
                            maxSplitMid = mid;
                        }
                    }

                    if (maxSplitMid > -1) {
                        // add split cost for binary rules
                        maxc[start][end] = (float) ParserUtil.logSum(maxc[start][end], maxSplitScore);
                        addBackptrToChart(start, maxSplitMid, end, bestNT[start][end], bestNT[start][maxSplitMid],
                                bestNT[maxSplitMid][end]);
                    }
                }

                if (span == n) {
                    addBackptrToChart(start, -1, end, grammar.startSymbol, bestNT[start][end],
                            Production.UNARY_PRODUCTION);
                }
            }
        }
    }

    // TODO: this should all be done in a local extractParseTree() method
    private void addBackptrToChart(final int start, final int midpt, final int end, final int parent,
            final int leftChild, final int rightChild) {
        Production p;
        ChartEdge edge;
        final ChartCell curCell = chart.getCell(start, end);
        if (rightChild == Production.LEXICAL_PRODUCTION) {
            p = new Production(parent, leftChild, Float.NEGATIVE_INFINITY, true, grammar);
            edge = chart.new ChartEdge(p, curCell);
        } else if (rightChild == Production.UNARY_PRODUCTION) {
            p = new Production(parent, leftChild, Float.NEGATIVE_INFINITY, false, grammar);
            edge = chart.new ChartEdge(p, curCell);
        } else {
            p = new Production(parent, leftChild, rightChild, Float.NEGATIVE_INFINITY, grammar);
            edge = chart.new ChartEdge(p, chart.getCell(start, midpt), chart.getCell(midpt, end));
        }

        curCell.bestEdge[parent] = edge;
        // System.out.println("[" + start + "," + end + "] " + p.toString());
    }

    @Override
    protected void visitCell(final short start, final short end) {
        computeInsideProbsInCell(chart.getCell(start, end));
    }

    protected void computeInsideProbsInCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();
        float insideScore;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                    if (rightCell.hasNT(p.rightChild)) {
                        insideScore = p.prob + getInside(start, mid, leftNT) + getInside(mid, end, p.rightChild);
                        cell.updateInside(p.parent, insideScore);
                    }
                }
            }
        }

        final float unaryScores[] = new float[grammar.numNonTerms()];
        Arrays.fill(unaryScores, Float.NEGATIVE_INFINITY);
        for (final int childNT : cell.getNTs()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                insideScore = p.prob + cell.getInside(childNT);
                unaryScores[p.parent] = (float) ParserUtil.logSum(unaryScores[p.parent], insideScore);
                // cell.updateInside(p.parent, insideProb);
                // unaryInside[start][end][p.parent] = (float)
                // ParserUtil.logSum(unaryInside[start][end][p.parent],insideProb);
                // cell.addToHashSets(p.parent);
            }
        }
        for (int nt = 0; nt < grammar.numNonTerms(); nt++) {
            cell.updateInside(nt, unaryScores[nt]);
        }
    }

    private void computeOutsideProbsInCell(final ChartCell cell) {
        float parentOutside, leftInside, rightInside, outsideScore;
        final int start = cell.start(), end = cell.end();

        // TODO: if a NT has outside=-Inf then we should delete it from the cell
        // System.out.println("== cell [" + start + "," + end + "] ==");

        // out(A,s,e) == for all (X -> A); out(A)*P(X -> A)
        final float unaryScores[] = new float[grammar.numNonTerms()];
        Arrays.fill(unaryScores, Float.NEGATIVE_INFINITY);
        for (final int nt : cell.getNTs()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(nt)) {
                outsideScore = p.prob + cell.getOutside(p.parent);
                unaryScores[nt] = (float) ParserUtil.logSum(unaryScores[nt], outsideScore);
                // if (unaryOutside[start][end][p.parent] > Float.NEGATIVE_INFINITY) {
                // if (cell.hasNT(p.parent)) {
                // parentOutside = cell.getOutside(p.parent);
                // cell.updateOutside(nt, parentOutside + p.prob);
                // }
            }
        }
        for (int nt = 0; nt < grammar.numNonTerms(); nt++) {
            cell.updateOutside(nt, unaryScores[nt]);
        }

        // out(A,s,e) == for all (X -> A B) and (Y -> C A) and midpoints
        // sum: out(X)*in(B)*P(X -> A B) + out(Y)*in(C)*P(Y -> C A)
        // updating right-half of outside prob for left child, and left-half
        // for right child. The other halfs will be completed by other cells.
        for (int mid = start + 1; mid < end; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                    if (rightCell.hasNT(p.rightChild)) {
                        parentOutside = cell.getOutside(p.parent);
                        if (parentOutside > Float.NEGATIVE_INFINITY) {
                            rightInside = rightCell.getInside(p.rightChild);
                            if (rightInside > Float.NEGATIVE_INFINITY) {
                                leftCell.updateOutside(p.leftChild, p.prob + parentOutside + rightInside);
                            }

                            // TODO: is this correct? It seems like we SHOULD count for each A.
                            // don't want to double-count entries with X -> A A
                            leftInside = leftCell.getInside(p.leftChild);
                            if (leftInside > Float.NEGATIVE_INFINITY && p.leftChild != p.rightChild) {
                                rightCell.updateOutside(p.rightChild, leftInside + parentOutside + p.prob);
                            }
                        }
                    }
                }
            }
        }
    }

    // @Override
    // public float getInside(final int start, final int end, final int nt) {
    // final float unary = unaryInside[start][end][nt];
    // final float binary = chart.getInside(start, end, nt);
    // if (unary > binary) {
    // return unary;
    // }
    // return binary;
    // }
    //
    // @Override
    // public float getOutside(final int start, final int end, final int nt) {
    // final float unary = unaryOutside[start][end][nt];
    // final float binary = chart.getOutside(start, end, nt);
    // if (unary > binary) {
    // return unary;
    // }
    // return binary;
    // }

    // public ParseTree extractParseTree() {
    // // use maxc[][] instead of chart ... all functions (berkeleyMaxRule, goodman, etc)
    // // should fill maxc for this extraction process
    //
    // return null;
    // }

    // private void computeOutsideProbsInCell(final ChartCell cell) {
    // ChartCell parentCell, leftCell, rightCell;
    // float logProb, parentOutside, leftInside, rightInside;
    // final float[] cellOut = outside[cell.start()][cell.end()];
    //
    // for (int i = 0; i < grammar.numNonTerms(); i++) {
    // cellOut[i] = Float.NEGATIVE_INFINITY;
    // }
    //
    // // edge is on right
    // int midpt = cell.start();
    // for (int start = 0; start < midpt; start++) {
    // parentCell = chart.getCell(start, cell.end());
    // leftCell = chart.getCell(start, midpt);
    // // TODO: this is getting too many edges. We only need the edges that have their
    // // left child in 'cell'
    // for (final ChartEdge parentEdge : parentCell.getEdges()) {
    // // TODO: what about unaries?
    // if (parentEdge.prod.isBinaryProd() && parentEdge.midpt() == midpt) {
    // parentOutside = outside[start][cell.end()][parentEdge.prod.parent];
    // // TODO: inside is max, not sum.
    // leftInside = leftCell.getBestEdge(parentEdge.prod.leftChild).inside;
    // logProb = parentOutside + parentEdge.prod.prob + leftInside;
    // cellOut[parentEdge.prod.rightChild] = (float) ParserUtil.logSum(cellOut[parentEdge.prod.rightChild],
    // logProb);
    // }
    // }
    // }
    //
    // // edge is on left
    // midpt = cell.end();
    // for (int end = midpt + 1; end < chart.size(); end++) {
    // parentCell = chart.getCell(cell.start(), end);
    // rightCell = chart.getCell(midpt, end);
    // for (final ChartEdge parentEdge : parentCell.getEdges()) {
    // if (parentEdge.prod.isBinaryProd() && parentEdge.midpt() == midpt) {
    // parentOutside = outside[cell.start()][end][parentEdge.prod.parent];
    // rightInside = rightCell.getBestEdge(parentEdge.prod.rightChild).inside;
    // logProb = parentOutside + parentEdge.prod.prob + rightInside;
    // cellOut[parentEdge.prod.rightChild] = (float) ParserUtil.logSum(cellOut[parentEdge.prod.rightChild],
    // logProb);
    // }
    // }
    // }
    // }

    // more old stuff ...

    // // sum over latent variables
    // for (final int nt : cell.getNTs()) {
    // final float inOut = cell.getInside(nt) + cell.getOutside(nt);
    // final int evalNT = grammar.getEvalNonTerm(nt);
    // ruleScores[evalNT] = (float) ParserUtil.logSum(ruleScores[evalNT], inOut);
    //
    // // find max value
    // if (ruleScores[evalNT] > maxScore[start][end]) {
    // maxScore[start][end] = ruleScores[evalNT];
    // bestNT = evalNT;
    // }
    // }
    //
    //
    // int bestBinaryNT = bestNT[start][end];
    // final ChartEdge edge = cell.getBestEdge(bestNT[start][end]);
    // if (edge.prod.isUnaryProd()) {
    // bestBinaryNT = edge.prod.child();
    // }
    //
    // float maxSplit = Float.NEGATIVE_INFINITY;
    // int maxSplitMid = -1;
    // for (int mid = start + 1; mid < end; mid++) {
    // final float split = maxc[start][mid] + maxc[mid][end];
    // // final float split = (float) ParserUtil.logSum(maxc[start][mid], maxc[mid][end]);
    // if (split > maxSplit) {
    // maxSplit = split;
    // maxSplitMid = mid;
    // }
    // }
    //
    // maxc[start][end] = maxInOut + maxSplit;
    // // maxc[start][end] = (float) ParserUtil.logSum(maxInOut, maxSplit);
    // final Production p = grammar.new Production(bestNT[start][end], bestNT[start][maxSplitMid],
    // bestNT[maxSplitMid][end], Float.NEGATIVE_INFINITY);
    // cell.bestEdge[bestNT[start][end]] = chart.new ChartEdge(p, chart.getCell(start, maxSplitMid),
    // chart.getCell(maxSplitMid, end));
    //

    // private float berkeleySumProduct(float a, float b, boolean maxRuleProduct) {
    // if (maxRuleProduct) {
    // return a + b;
    // }
    // return (float) ParserUtil.logSum(a, b);
    // }

    // private void addRuleScore(int A, int B, int C, float ruleScore, final HashMap<int[], Float> rules,
    // final float maxScore[], final ChartCell cell) {
    // A = evalGrammar.projectNonTerm(A);
    // B = evalGrammar.projectNonTerm(A);
    // if (C >= 0) {
    // C = evalGrammar.projectNonTerm(A);
    // }
    //
    // final int[] evalProd = { A, B, C };
    // if (rules.containsKey(evalProd)) {
    // ruleScore = (float) ParserUtil.logSum(ruleScore, rules.get(evalProd));
    // }
    // rules.put(evalProd, ruleScore);
    //
    // if (ruleScore > maxScore[A]) {
    // maxScore[A] = ruleScore;
    // cell.bestEdge[A] = chart.new ChartEdge(evalGrammar.new Production(A, B, Float.NEGATIVE_INFINITY, false),
    // cell);
    // // System.out.println(maxScore[start][end][A] + "\t" + cell.bestEdge[A]);
    // }
    //
    // // return ruleScore;
    // }
}
