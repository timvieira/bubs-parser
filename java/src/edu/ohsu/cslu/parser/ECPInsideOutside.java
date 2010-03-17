package edu.ohsu.cslu.parser;

import java.util.LinkedList;

import com.aliasi.util.Collections;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.InOutCellChart.ChartCell;
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
            topDownTraversal.addFirst(cell);
            visitCell(cell);
            topDownTraversal.addFirst(cell);
        }

        for (final ChartCell cell : topDownTraversal) {
            computeOutsideProbsInCell(cell);
        }

        return extractBestParse();
    }

    @Override
    protected void visitCell(final edu.ohsu.cslu.parser.CellChart.ChartCell cell) {
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
                        cell.updateInside(p, leftCell, rightCell, insideProb);
                    }
                }
            }
        }

        for (final int childNT : Collections.toIntArray(cell.getNTs())) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                cell.updateInside(p, p.prob + cell.getInside(childNT));
            }
        }
    }

    // Is it a problem that we are only keeping the viterbi 1-best? This allows only
    // the ML tree to get any outside prob ...
    private void computeOutsideProbsInCell(final ChartCell cell) {
        float parentOutside, leftInside, rightInside;
        final int start = cell.start(), end = cell.end();

        // TODO: this doesn't work correctly for unary chains > 1 since the value depends
        // on the order the unary edges are traversed. What we really need to do is visit
        // the highest unary entry first, and then work our way down.
        System.out.println("== cell [" + start + "," + end + "] ==");
        for (final int nt : cell.getNTs()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(nt)) {
                if (p.isUnaryProd() && cell.hasNT(p.parent)) {
                    parentOutside = cell.getOutside(p.parent);
                    cell.updateOutside(nt, parentOutside + p.prob);
                }
            }

            System.out
                    .println("  " + grammar.mapNonterminal(nt) + " = " + (cell.getInside(nt) + cell.getOutside(nt)) + " in=" + cell.getInside(nt) + " out=" + cell.getOutside(nt));
        }

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                    final ChartEdge rightEdge = rightCell.getBestEdge(p.rightChild);
                    if (rightEdge != null) {
                        parentOutside = cell.getOutside(p.parent);
                        leftInside = leftCell.getInside(p.leftChild);
                        rightInside = rightCell.getInside(p.rightChild);

                        leftCell.updateOutside(p.leftChild, rightInside + parentOutside + p.prob);
                        // don't want to double-count entries with X -> A A
                        if (p.leftChild != p.rightChild) {
                            rightCell.updateOutside(p.rightChild, leftInside + parentOutside + p.prob);
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
