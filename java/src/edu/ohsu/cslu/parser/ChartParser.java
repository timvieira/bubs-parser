package edu.ohsu.cslu.parser;

import java.util.List;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ChartParser extends Parser {

    public Chart<? extends ChartCell> chart;

    public ChartParser(final Grammar grammar) {
        this.grammar = grammar;
    }

    protected void initParser(final int sentLength) {
        chart = new Chart<ArrayChartCell>(sentLength, ArrayChartCell.class, grammar);
    }

    protected List<ChartEdge> addLexicalProductions(final Token sent[]) throws Exception {
        ChartCell cell;
        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            for (final Production lexProd : grammar.getLexProdsByToken(sent[i])) {
                cell = chart.getCell(i, i + 1);
                cell.addEdge(new ChartEdge(lexProd, cell, lexProd.prob));
            }
        }

        return null;
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
                curNode = new ParseTree(bestEdge);
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
