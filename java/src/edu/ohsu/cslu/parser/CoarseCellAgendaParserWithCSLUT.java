package edu.ohsu.cslu.parser;

import java.util.Collection;
import java.util.List;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.ParseTree;

public class CoarseCellAgendaParserWithCSLUT extends CoarseCellAgendaParser {

    protected CSLUTBlockedCells cslutScores;

    public CoarseCellAgendaParserWithCSLUT(final Grammar grammar, final EdgeSelector edgeSelector, final CSLUTBlockedCells cslutScores) {
        super(grammar, edgeSelector);
        this.cslutScores = cslutScores;
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {
        ChartCell cell;
        final Token sent[] = grammar.tokenize(sentence);
        currentSentence = sentence;

        initParser(sent.length);
        addLexicalProductions(sent);
        edgeSelector.init(this);
        cslutScores.init(this);
        addUnaryExtensionsToLexProds();

        for (int i = 0; i < chart.size(); i++) {
            expandFrontier(chart.getCell(i, i + 1));
        }

        while (hasNext() && !hasCompleteParse()) {
            cell = next();
            // System.out.println(" nextCell: " + cell);
            visitCell(cell);
            expandFrontier(cell);
        }

        return extractBestParse();
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();
        Collection<Production> possibleProds;
        ChartEdge edge;
        final ChartEdge[] bestEdges = new ChartEdge[grammar.numNonTerms()]; // inits to null

        final int maxEdgesToAdd = (int) ParserDriver.param2;
        final boolean onlyFactored = cslutScores.isCellOpenOnlyToFactored(start, end);

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                    possibleProds = grammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent);
                    if (possibleProds != null) {
                        for (final Production p : possibleProds) {
                            if (!onlyFactored || grammar.isFactoredNonTerm(p.parent)) {
                                final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                                edge = new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
                                addEdgeToArray(edge, bestEdges);
                            }
                        }
                    }
                }
            }
        }

        addBestEdgesToChart(cell, bestEdges, maxEdgesToAdd);
    }

    @Override
    protected void setSpanMaxEdgeFOM(final ChartCell leftCell, final ChartCell rightCell) {
        ChartEdge edge;
        final int start = leftCell.start(), end = rightCell.end();
        float bestFOM = maxEdgeFOM[start][end];

        // System.out.println(" setSpanMax: " + leftCell + " && " + rightCell);

        if (cslutScores.isCellOpen(start, end)) {
            final List<ChartEdge> leftEdgeList = leftCell.getBestLeftEdges();
            final List<ChartEdge> rightEdgeList = rightCell.getBestRightEdges();
            Collection<Production> possibleProds;
            if (rightEdgeList.size() > 0 && leftEdgeList.size() > 0) {
                for (final ChartEdge leftEdge : leftEdgeList) {
                    for (final ChartEdge rightEdge : rightEdgeList) {
                        possibleProds = grammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent);
                        if (possibleProds != null) {
                            for (final Production p : possibleProds) {
                                final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                                edge = new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
                                // System.out.println(" considering: " + edge);
                                if (edge.fom > bestFOM) {
                                    bestFOM = edge.fom;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bestFOM > maxEdgeFOM[start][end]) {
            final ChartCell parentCell = chart.getCell(start, end);
            if (maxEdgeFOM[start][end] > Float.NEGATIVE_INFINITY) {
                spanAgenda.remove(parentCell);
            }
            maxEdgeFOM[start][end] = bestFOM;
            parentCell.figureOfMerit = bestFOM;
            spanAgenda.add(parentCell);
            // System.out.println(" addingSpan: " + parentCell);
        }
    }

}
