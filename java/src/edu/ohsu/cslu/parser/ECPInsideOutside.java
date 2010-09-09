package edu.ohsu.cslu.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.ProjectedGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.InOutCellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.InOutCellChart.ChartCell;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;

public class ECPInsideOutside extends ChartParser<LeftListGrammar, InOutCellChart> {

    ProjectedGrammar evalGrammar;

    public ECPInsideOutside(final ParserDriver opts, final LeftListGrammar grammar) {
        super(opts, grammar);

        evalGrammar = new ProjectedGrammar(grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        chart = new InOutCellChart(sentLength, opts.viterbiMax, this);
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {

        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);
        final LinkedList<ChartCell> topDownTraversal = new LinkedList<ChartCell>();

        initParser(sent.length);
        addLexicalProductions(sent);
        cellSelector.init(this);

        while (cellSelector.hasNext()) {
            final short[] startEnd = cellSelector.next();
            visitCell(startEnd[0], startEnd[1]);
            topDownTraversal.addFirst(chart.getCell(startEnd[0], startEnd[1]));
        }

        for (final ChartCell cell : topDownTraversal) {
            computeOutsideProbsInCell(cell);
        }

        // goodmanMaximizeLabelRecall();
        berkeleyMaxRule(sent);

        return chart.extractBestParse(chart.getRootCell(), evalGrammar.startSymbol);
    }

    // find latent grammar max rule score for theoretical
    // grammar rule A => B C by summing over all A_x => B_y C_z
    // for all x, y, and z. Label constituent with A.
    protected void berkeleyMaxRule(final int sent[]) {

        // create a new chart? A new parser with in/out FOM?
        // map down to treebank NTs by summing over all latent annotations

        final int n = chart.size();
        final float maxScore[][][] = new float[n][n + 1][evalGrammar.numNonTerms()];
        HashMap<Production, Float> evalProdSet;
        Production evalProd;
        float ruleScore;

        for (int span = 1; span <= n; span++) {
            for (int start = 0; start < n - span + 1; start++) {
                final int end = start + span;
                final ChartCell cell = chart.getCell(start, end);
                Arrays.fill(maxScore[start][end], Float.NEGATIVE_INFINITY);

                // System.out.println(" == " + start + "," + end + " ==");
                for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
                    final ChartCell leftCell = chart.getCell(start, mid);
                    final ChartCell rightCell = chart.getCell(mid, end);

                    evalProdSet = new HashMap<Production, Float>();
                    for (final int leftNT : leftCell.getLeftChildNTs()) {
                        for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                            if (rightCell.hasNT(p.rightChild)) {
                                // System.out.println(" considering: " + p + " in=" + cell.getInside(p.parent)
                                // + " out=" + cell.getOutside(p.parent));
                                ruleScore = cell.getOutside(p.parent) + p.prob + leftCell.getInside(leftNT)
                                        + rightCell.getInside(p.rightChild);
                                final int A = evalGrammar.projectNonTerm(p.parent);
                                final int B = evalGrammar.projectNonTerm(p.leftChild);
                                final int C = evalGrammar.projectNonTerm(p.rightChild);

                                evalProd = evalGrammar.new Production(A, B, C, Float.NEGATIVE_INFINITY);
                                if (evalProdSet.containsKey(evalProd)) {
                                    ruleScore = (float) ParserUtil.logSum(ruleScore, evalProdSet.get(evalProd));
                                    // System.out.println("inc rule score=" + ruleScore + " p=" + p +
                                    // " evalProd=" + evalProd);
                                }
                                evalProdSet.put(evalProd, ruleScore);

                                if (ruleScore > maxScore[start][end][A]) {
                                    maxScore[start][end][A] = ruleScore;
                                    cell.bestEdge[A] = chart.new ChartEdge(evalProd, leftCell, rightCell);
                                    // System.out.println(maxScore[start][end][A] + "\t" + cell.bestEdge[A]);
                                }
                            }
                        }
                    }
                }

                evalProdSet = new HashMap<Production, Float>();
                if (span == 1) {
                    // lexical productions
                    final int wordIndex = sent[start];
                    for (final Production lexProd : grammar.getLexicalProductionsWithChild(wordIndex)) {
                        ruleScore = cell.getOutside(lexProd.parent) + lexProd.prob; // inside=1.0
                        final int A = evalGrammar.projectNonTerm(lexProd.parent);
                        evalProd = evalGrammar.new Production(A, wordIndex, Float.NEGATIVE_INFINITY, true);
                        if (evalProdSet.containsKey(evalProd)) {
                            ruleScore = (float) ParserUtil.logSum(ruleScore, evalProdSet.get(evalProd));
                        }
                        evalProdSet.put(evalProd, ruleScore);

                        if (ruleScore > maxScore[start][end][A]) {
                            maxScore[start][end][A] = ruleScore;
                            cell.bestEdge[A] = chart.new ChartEdge(evalProd, cell);
                            // System.out.println(maxScore[start][end][A] + "\t" + cell.bestEdge[A]);
                        }
                    }
                }

                for (final int childNT : cell.getNtArray()) {
                    for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                        // System.out.println(" considering: " + p + " in=" + cell.getInside(p.parent) +
                        // " out=" + cell.getOutside(p.parent));
                        ruleScore = cell.getOutside(p.parent) + p.prob + cell.getInside(p.child());
                        final int A = evalGrammar.projectNonTerm(p.parent);
                        final int B = evalGrammar.projectNonTerm(p.child());
                        evalProd = evalGrammar.new Production(A, B, Float.NEGATIVE_INFINITY, false);
                        if (evalProdSet.containsKey(evalProd)) {
                            ruleScore = (float) ParserUtil.logSum(ruleScore, evalProdSet.get(evalProd));
                        }
                        evalProdSet.put(evalProd, ruleScore);

                        if (ruleScore > maxScore[start][end][A]) {
                            maxScore[start][end][A] = ruleScore;
                            cell.bestEdge[A] = chart.new ChartEdge(evalProd, cell);
                            // System.out.println(maxScore[start][end][A] + "\t" + cell.bestEdge[A]);
                        }
                    }
                }
            }
        }
    }

    private void addRuleScore(int A, int B, int C, float ruleScore, final HashMap<int[], Float> rules,
            final float maxScore[], final ChartCell cell) {
        A = evalGrammar.projectNonTerm(A);
        B = evalGrammar.projectNonTerm(A);
        if (C >= 0) {
            C = evalGrammar.projectNonTerm(A);
        }

        final int[] evalProd = { A, B, C };
        if (rules.containsKey(evalProd)) {
            ruleScore = (float) ParserUtil.logSum(ruleScore, rules.get(evalProd));
        }
        rules.put(evalProd, ruleScore);

        if (ruleScore > maxScore[A]) {
            maxScore[A] = ruleScore;
            cell.bestEdge[A] = chart.new ChartEdge(evalGrammar.new Production(A, B, Float.NEGATIVE_INFINITY, false),
                    cell);
            // System.out.println(maxScore[start][end][A] + "\t" + cell.bestEdge[A]);
        }

        // return ruleScore;
    }

    // See: Joshua Goodman, Parsing Algorithms and Metrics, Section 3
    protected void goodmanMaximizeLabelRecall() {

        final int n = chart.size();
        final float maxc[][] = new float[n][n + 1];
        final int bestNT[][] = new int[n][n + 1];
        for (int i = 0; i < n; i++) {
            Arrays.fill(maxc[i], Float.NEGATIVE_INFINITY);
        }

        // TODO: what about unaries?
        // TODO: map Berkeley NTs down to Treebank NTs (make sure they do this in the Berkeley parser)

        for (int span = 1; span < n; span++) {
            for (int start = 0; start < n - span + 1; start++) {
                final int end = start + span;
                final ChartCell cell = chart.getCell(start, end);

                float maxInOut = Float.NEGATIVE_INFINITY;
                for (final int nt : cell.getNtArray()) {
                    final float inOut = cell.getInside(nt) + cell.getOutside(nt);
                    if (inOut > maxInOut) {
                        maxInOut = inOut;
                        bestNT[start][end] = nt;
                    }
                }

                int bestBinaryNT = bestNT[start][end];
                final ChartEdge edge = cell.getBestEdge(bestNT[start][end]);
                if (edge.prod.isUnaryProd()) {
                    bestBinaryNT = edge.prod.child();
                }

                float maxSplit = Float.NEGATIVE_INFINITY;
                int maxSplitMid = -1;
                for (int mid = start + 1; mid < end; mid++) {
                    final float split = maxc[start][mid] + maxc[mid][end];
                    // final float split = (float) ParserUtil.logSum(maxc[start][mid], maxc[mid][end]);
                    if (split > maxSplit) {
                        maxSplit = split;
                        maxSplitMid = mid;
                    }
                }

                maxc[start][end] = maxInOut + maxSplit;
                // maxc[start][end] = (float) ParserUtil.logSum(maxInOut, maxSplit);
                final Production p = grammar.new Production(bestNT[start][end], bestNT[start][maxSplitMid],
                        bestNT[maxSplitMid][end], Float.NEGATIVE_INFINITY);
                cell.bestEdge[bestNT[start][end]] = chart.new ChartEdge(p, chart.getCell(start, maxSplitMid), chart
                        .getCell(maxSplitMid, end));
            }
        }
    }

    @Override
    protected void visitCell(final short start, final short end) {
        computeInsideProbsInCell(chart.getCell(start, end));
    }

    protected void computeInsideProbsInCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();
        float insideProb;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                    if (rightCell.hasNT(p.rightChild)) {
                        insideProb = p.prob + leftCell.getInside(leftNT) + rightCell.getInside(p.rightChild);
                        // cell.updateInside(p, leftCell, rightCell, insideProb);
                        cell.updateInside(p.parent, insideProb);
                    }
                }
            }
        }

        for (final int childNT : cell.getNtArray()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                // cell.updateInside(p, p.prob + cell.getInside(childNT));
                cell.updateInside(p.parent, p.prob + cell.getInside(childNT));
            }
        }
    }

    private void computeOutsideProbsInCell(final ChartCell cell) {
        float parentOutside, leftInside, rightInside;
        final int start = cell.start(), end = cell.end();

        // TODO: this doesn't work correctly for unary chains > 1 since the value depends
        // on the order the unary edges are traversed. What we really need to do is visit
        // the highest unary entry first, and then work our way down.
        // System.out.println("== cell [" + start + "," + end + "] ==");
        for (final int nt : cell.getNtArray()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(nt)) {
                if (p.isUnaryProd() && cell.hasNT(p.parent)) {
                    parentOutside = cell.getOutside(p.parent);
                    cell.updateOutside(nt, parentOutside + p.prob);
                }
            }

            // System.out.println("  " + grammar.mapNonterminal(nt) + " = " + (cell.getInside(nt) +
            // cell.getOutside(nt)) + " in=" + cell.getInside(nt) + " out=" +
            // cell.getOutside(nt));
        }

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
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

        // for (final ChartEdge edge : cell.getEdges()) {
        // if (edge.prod.isBinaryProd()) {
        // midpt = edge.midpt();
        // parentOutside = cell.getOutside(edge.prod.parent);
        // leftInside = edge.leftCell.getInside(edge.prod.leftChild);
        // rightInside = edge.rightCell.getInside(edge.prod.rightChild);
        //
        // leftCell.updateOutside(edge.prod.leftChild, rightInside + parentOutside + edge.prod.prob);
        // rightCell.updateOutside(edge.prod.rightChild, leftInside + parentOutside + edge.prod.prob);
        //
        // // System.out.println("binary: " + edge + "\n\tpOut=" + parentOutside + " lIn=" + leftInside +
        // " rIn=" + rightInside + " prod=" + edge.prod.prob + " lOut="
        // // + outside[start][midpt][edge.prod.leftChild] + " rOut=" +
        // outside[midpt][end][edge.prod.rightChild]);
        // }
        // }

    }

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
}
