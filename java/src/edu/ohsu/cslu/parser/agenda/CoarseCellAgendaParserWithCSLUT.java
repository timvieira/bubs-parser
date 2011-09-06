/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.agenda;

import java.util.Collection;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.OHSUCellConstraintsModel.OHSUCellConstraints;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

/**
 * @author Nathan Bodenstab
 */
public class CoarseCellAgendaParserWithCSLUT extends CoarseCellAgendaParser {

    protected OHSUCellConstraints cellConstraints;

    public CoarseCellAgendaParserWithCSLUT(final ParserDriver opts, final LeftHashGrammar grammar,
            final OHSUCellConstraints cellConstraints) {
        super(opts, grammar);
        this.cellConstraints = cellConstraints;
        throw new IllegalArgumentException("Parser requires tuning and cannot be used");
    }

    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {
        HashSetChartCell cell;

        initParser(parseTask);
        addLexicalProductions(parseTask.tokens);
        fomModel.init(parseTask, chart);
        cellConstraints.initSentence(chart, parseTask.sentence);
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

        // final int maxEdgesToAdd = (int) opts.param2;
        final int maxEdgesToAdd = Integer.MAX_VALUE;

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
                            // final float prob = p.prob + leftCell.getInside(leftNT) +
                            // rightCell.getInside(rightNT);
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
