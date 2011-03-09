package edu.ohsu.cslu.parser.agenda;

import java.util.Collection;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.OHSUCellConstraints;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class CoarseCellAgendaParserWithCSLUT extends CoarseCellAgendaParser {

    protected OHSUCellConstraints cellConstraints;

    public CoarseCellAgendaParserWithCSLUT(final ParserDriver opts, final LeftHashGrammar grammar,
            final OHSUCellConstraints cellConstraints) {
        super(opts, grammar);
        this.cellConstraints = cellConstraints;
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) {
        HashSetChartCell cell;

        initParser(tokens);
        addLexicalProductions(tokens);
        edgeSelector.init(chart);
        cellConstraints.initSentence(chart, currentInput.sentenceNumber, currentInput.sentence);
        addUnaryExtensionsToLexProds();

        for (int i = 0; i < chart.size(); i++) {
            expandFrontier(chart.getCell(i, i + 1));
        }

        while (hasNext() && !chart.hasCompleteParse(grammar.startSymbol)) {
            cell = next();
            // System.out.println(" nextCell: " + cell);
            visitCell(cell);
            expandFrontier(cell);
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    @Override
    protected void visitCell(final HashSetChartCell cell) {
        final short start = cell.start(), end = cell.end();
        Collection<Production> possibleProds;
        ChartEdge edge;
        final ChartEdge[] bestEdges = new ChartEdge[grammar.numNonTerms()]; // inits to null

        final int maxEdgesToAdd = (int) opts.param2;
        final int midStart = cellSelector.getMidStart(start, end);
        final int midEnd = cellSelector.getMidEnd(start, end);
        final boolean onlyFactored = cellConstraints.hasCellConstraints()
                && cellConstraints.getCellConstraints().isCellOnlyFactored(start, end);

        for (int mid = midStart; mid <= midEnd; mid++) { // mid point
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    possibleProds = grammar.getBinaryProductionsWithChildren(leftNT, rightNT);
                    if (possibleProds != null) {
                        for (final Production p : possibleProds) {
                            if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
                                // final float prob = p.prob + leftCell.getInside(leftNT) +
                                // rightCell.getInside(rightNT);
                                edge = chart.new ChartEdge(p, leftCell, rightCell);
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
    protected void setSpanMaxEdgeFOM(final HashSetChartCell leftCell, final HashSetChartCell rightCell) {
        ChartEdge edge;
        final short start = leftCell.start(), end = rightCell.end();
        float bestFOM = maxEdgeFOM[start][end];

        // System.out.println(" setSpanMax: " + leftCell + " && " + rightCell);

        if (cellConstraints.hasCellConstraints() && cellConstraints.getCellConstraints().isCellOpen(start, end)) {
            Collection<Production> possibleProds;
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    possibleProds = grammar.getBinaryProductionsWithChildren(leftNT, rightNT);
                    if (possibleProds != null) {
                        for (final Production p : possibleProds) {
                            // final float prob = p.prob + leftCell.getInside(leftNT) + rightCell.getInside(rightNT);
                            edge = chart.new ChartEdge(p, leftCell, rightCell);
                            // System.out.println(" considering: " + edge);
                            if (edge.fom > bestFOM) {
                                bestFOM = edge.fom;
                            }
                        }
                    }
                }
            }
        }

        if (bestFOM > maxEdgeFOM[start][end]) {
            final HashSetChartCell parentCell = chart.getCell(start, end);
            if (maxEdgeFOM[start][end] > Float.NEGATIVE_INFINITY) {
                spanAgenda.remove(parentCell);
            }
            maxEdgeFOM[start][end] = bestFOM;
            parentCell.fom = bestFOM;
            spanAgenda.add(parentCell);
            // System.out.println(" addingSpan: " + parentCell);
        }
    }

}
