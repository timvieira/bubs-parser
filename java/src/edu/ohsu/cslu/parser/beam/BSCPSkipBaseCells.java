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
package edu.ohsu.cslu.parser.beam;

import java.util.Collection;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

/**
 * @author Nathan Bodenstab
 */
public class BSCPSkipBaseCells extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    public BSCPSkipBaseCells(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void addLexicalProductions(final ChartCell c) {
        final HashSetChartCell cell = (HashSetChartCell) c;

        // add lexical productions to the base cells of the chart
        for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.parseTask.tokens[c.start()])) {
            cell.updateInside(chart.new ChartEdge(lexProd, cell));

            // NOTE: also adding unary prods here...should probably change the name of this
            // function and also create our own init()
            for (final Production unaryProd : grammar.getUnaryProductionsWithChild(lexProd.parent)) {
                // NOTE: not using an FOM for these edges ... just adding them all
                cell.updateInside(chart.new ChartEdge(unaryProd, cell));
            }
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell c) {
        final HashSetChartCell cell = (HashSetChartCell) c;
        final short start = c.start();
        final short end = c.end();

        Collection<Production> possibleProds;
        ChartEdge edge;

        // TODO: Can we do something faster to find the best N edges other than
        // sorting ALL of them into an agenda?
        final ChartEdge bestEdges[] = new ChartEdge[grammar.numNonTerms()]; // inits to null

        assert (end - start >= 2);
        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    possibleProds = grammar.getBinaryProductionsWithChildren(leftNT, rightNT);
                    if (possibleProds != null) {
                        for (final Production p : possibleProds) {
                            edge = chart.new ChartEdge(p, leftCell, rightCell);
                            addEdgeToArray(edge, bestEdges);
                        }
                    }
                }
            }
        }

        addBestEdgesToChart(cell, bestEdges);

    }

    private void addEdgeToArray(final ChartEdge edge, final ChartEdge[] bestEdges) {
        final int parent = edge.prod.parent;
        if (bestEdges[parent] == null || edge.fom > bestEdges[parent].fom) {
            bestEdges[parent] = edge;
        }
    }

    private void addEdgeToAgenda(final ChartEdge edge) {
        agenda.add(edge);
        cellPushed++;
        cellConsidered++;
    }

    private void addBestEdgesToChart(final HashSetChartCell cell, final ChartEdge[] bestEdges) {
        ChartEdge edge, unaryEdge;
        int numAdded = 0;

        agenda = new PriorityQueue<ChartEdge>();
        for (int i = 0; i < grammar.numNonTerms(); i++) {
            if (bestEdges[i] != null) {
                addEdgeToAgenda(bestEdges[i]);
            }
        }

        while (agenda.isEmpty() == false && numAdded <= beamWidth) {
            edge = agenda.poll();
            if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
                numAdded++;
                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = chart.new ChartEdge(p, cell);
                    addEdgeToAgenda(unaryEdge);
                }
            }
        }

    }

    @Override
    public String getStats() {

        int cells = 0, cellsVisited = 0, cellsSkipped = 0, cellVisits = 0;
        for (int span = 2; span < chart.size(); span++) {
            for (int start = 0; start < chart.size() - span + 1; start++) {
                cells++;
                final HashSetChartCell cell = chart.getCell(start, start + span);
                if (cell.numSpanVisits > 0) {
                    cellsVisited++;
                    cellVisits += cell.numSpanVisits;
                } else {
                    cellsSkipped++;
                }
            }
        }

        return super.getStats() + " agendaPush=" + cellPushed + " #cells=" + cells + " #visited=" + cellsVisited
                + " #skipped=" + cellsSkipped + " #totalVisits=" + cellVisits;
    }
}
