package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartEdge;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ChartParser<G extends GrammarByChild, C extends Chart> extends Parser<G> {

    public C chart;

    public ChartParser(final ParserOptions opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    public float getInside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @Override
    public float getOutside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @SuppressWarnings("unchecked")
    protected void initParser(final int sentLength) {
        chart = (C) new CellChart(sentLength, opts.viterbiMax, this);
    }

    protected void addLexicalProductions(final int sent[]) throws Exception {
        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            final ChartCell cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
            }
            cell.finalizeCell();
        }
    }

    public boolean hasCompleteParse() {
        return chart.getRootCell().getBestEdge(grammar.startSymbol) != null;
    }

    protected ParseTree extractBestParse() {
        return extractBestParse(chart.getRootCell(), this.grammar.startSymbol);
    }

    protected ParseTree extractBestParse(final ChartCell cell, final int nonTermIndex) {
        ChartEdge bestEdge;
        ParseTree curNode = null;

        if (cell != null) {
            bestEdge = cell.getBestEdge(nonTermIndex);
            if (bestEdge != null) {
                curNode = new ParseTree(bestEdge.prod.parentToString());
                if (bestEdge.prod.isUnaryProd()) {
                    curNode.children.add(extractBestParse(bestEdge.leftCell, bestEdge.prod.leftChild));
                } else if (bestEdge.prod.isLexProd()) {
                    curNode.addChild(new ParseTree(bestEdge.prod.childrenToString()));
                } else { // binary production
                    curNode.children.add(extractBestParse(bestEdge.leftCell, bestEdge.prod.leftChild));
                    curNode.children.add(extractBestParse(bestEdge.rightCell, bestEdge.prod.rightChild));
                }
            }
        }

        return curNode;
    }

    @Override
    public String getStats() {
        String result = "";
        int con = 0, add = 0;

        for (int start = 0; start < chart.size(); start++) {
            for (int end = start + 1; end < chart.size() + 1; end++) {
                con += chart.getCell(start, end).numEdgesConsidered;
                add += chart.getCell(start, end).numEdgesAdded;
            }
        }

        result += " chartEdges=" + add + " processedEdges=" + con;
        return result;
    }
}
