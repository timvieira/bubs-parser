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
public class BSCPOnlineBeam extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    float bestFOM, onlineBeam;
    int numEdgesAdded;

    public BSCPOnlineBeam(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell c) {
        final HashSetChartCell cell = (HashSetChartCell) c;
        final short start = c.start();
        final short end = c.end();
        final int spanWidth = end - start;
        Collection<Production> possibleProds;
        ChartEdge edge;

        final int midStart = cellSelector.getMidStart(start, end);
        final int midEnd = cellSelector.getMidEnd(start, end);
        final boolean onlyFactored = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end);

        bestFOM = Float.NEGATIVE_INFINITY;
        onlineBeam = Float.NEGATIVE_INFINITY;
        numEdgesAdded = 0;

        if (spanWidth == 1) {
            for (final int pos : cell.getPosNTs()) {
                for (final Production p : grammar.getUnaryProductionsWithChild(pos)) {
                    edge = chart.new ChartEdge(p, cell);
                    processEdge(edge, cell);
                }
            }
        } else {
            for (int mid = midStart; mid <= midEnd; mid++) { // mid point
                final HashSetChartCell leftCell = chart.getCell(start, mid);
                final HashSetChartCell rightCell = chart.getCell(mid, end);
                for (final int leftNT : leftCell.getLeftChildNTs()) {
                    for (final int rightNT : rightCell.getRightChildNTs()) {
                        possibleProds = grammar.getBinaryProductionsWithChildren(leftNT, rightNT);
                        if (possibleProds != null) {
                            for (final Production p : possibleProds) {
                                if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
                                    edge = chart.new ChartEdge(p, leftCell, rightCell);
                                    processEdge(edge, cell);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void processEdge(final ChartEdge edge, final HashSetChartCell cell) {
        final boolean addedEdge = addEdgeToChart(edge, cell);
        if (addedEdge) {
            // Add unary productions to agenda so they can compete with binary productions
            for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                final ChartEdge unaryEdge = chart.new ChartEdge(p, cell);
                addEdgeToChart(unaryEdge, cell);
            }
        }
    }

    protected boolean addEdgeToChart(final ChartEdge edge, final HashSetChartCell cell) {
        if (edge.fom < bestFOM - globalBeamDelta) {
            return false;
        }
        if (edge.inside() <= cell.getInside(edge.prod.parent)) {
            return false;
        }
        cell.updateInside(edge);

        if (edge.fom > bestFOM) {
            bestFOM = edge.fom;
        }

        // numEdgesAdded++;
        // onlineBeam = ParserDriver.param2 / numEdgesAdded;

        return true;
    }
}
