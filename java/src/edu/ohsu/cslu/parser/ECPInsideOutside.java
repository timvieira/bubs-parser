package edu.ohsu.cslu.parser;

import java.util.Arrays;
import java.util.LinkedList;

import com.aliasi.util.Collections;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.InOutCellChart;
import edu.ohsu.cslu.parser.chart.InOutCellChart.ChartCell;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ECPInsideOutside extends CellwiseExhaustiveChartParser<LeftListGrammar, InOutCellChart> {

    public ECPInsideOutside(final ParserOptions opts, final LeftListGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        chart = new InOutCellChart(sentLength, opts.viterbiMax, this);
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {

        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);

        initParser(sent.length);
        addLexicalProductions(sent);
        cellSelector.init(this);

        final LinkedList<ChartCell> topDownTraversal = new LinkedList<ChartCell>();
        while (cellSelector.hasNext()) {
            final short[] startEnd = cellSelector.next();
            final ChartCell cell = chart.getCell(startEnd[0], startEnd[1]);
            visitCell(cell);
            topDownTraversal.addFirst(cell);
        }

        for (final ChartCell cell : topDownTraversal) {
            computeOutsideProbsInCell(cell);
        }

        goodmanMaximizeLabelRecall();

        return extractBestParse();
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
            for (int start = 1; start < n - span + 1; start++) {
                final int end = start + span;
                final ChartCell cell = chart.getCell(start, end);

                float maxInOut = Float.NEGATIVE_INFINITY;
                for (final int nt : cell.getNTs()) {
                    final float inOut = cell.getInside(nt) + cell.getOutside(nt);
                    if (inOut > maxInOut) {
                        maxInOut = inOut;
                        bestNT[start][end] = nt;
                    }
                }

                float maxSplit = Float.NEGATIVE_INFINITY;
                int maxSplitMid = -1;
                for (int mid = start + 1; mid < end; mid++) {
                    final float split = maxc[start][mid] + maxc[mid][end];
                    if (split > maxSplit) {
                        maxSplit = split;
                        maxSplitMid = mid;
                    }
                }

                maxc[start][end] = maxInOut + maxSplit;
                final Production p = grammar.new Production(bestNT[start][end], bestNT[start][maxSplitMid], bestNT[maxSplitMid][end], Float.NEGATIVE_INFINITY);
                cell.bestEdge[bestNT[start][end]] = chart.new ChartEdge(p, chart.getCell(start, maxSplitMid), chart.getCell(maxSplitMid, end));
            }
        }
    }

    @Override
    protected void visitCell(final edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell cell) {
        computeInsideProbsInCell((ChartCell) cell);
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

        for (final int childNT : Collections.toIntArray(cell.getNTs())) {
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
        for (final int nt : cell.getNTs()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(nt)) {
                if (p.isUnaryProd() && cell.hasNT(p.parent)) {
                    parentOutside = cell.getOutside(p.parent);
                    cell.updateOutside(nt, parentOutside + p.prob);
                }
            }

            // System.out.println("  " + grammar.mapNonterminal(nt) + " = " + (cell.getInside(nt) + cell.getOutside(nt)) + " in=" + cell.getInside(nt) + " out=" +
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
        // // System.out.println("binary: " + edge + "\n\tpOut=" + parentOutside + " lIn=" + leftInside + " rIn=" + rightInside + " prod=" + edge.prod.prob + " lOut="
        // // + outside[start][midpt][edge.prod.leftChild] + " rOut=" + outside[midpt][end][edge.prod.rightChild]);
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
    // cellOut[parentEdge.prod.rightChild] = (float) ParserUtil.logSum(cellOut[parentEdge.prod.rightChild], logProb);
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
    // cellOut[parentEdge.prod.rightChild] = (float) ParserUtil.logSum(cellOut[parentEdge.prod.rightChild], logProb);
    // }
    // }
    // }
    // }
}
