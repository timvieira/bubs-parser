package edu.ohsu.cslu.parser;

import java.util.Collection;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPOnlineBeam extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    float bestFOM, onlineBeam;
    int numEdgesAdded;

    public BSCPOnlineBeam(final ParserOptions opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final HashSetChartCell cell) {
        final int start = cell.start(), end = cell.end();
        final int spanWidth = end - start;
        Collection<Production> possibleProds;
        ChartEdge edge;

        boolean onlyFactored = false;
        if (cellSelector.type == CellSelector.CellSelectorType.CSLUT) {
            onlyFactored = ((CSLUTBlockedCells) cellSelector).isCellOpenOnlyToFactored(start, end);
        }

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
            for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
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
        if (edge.fom < bestFOM - beamDeltaThresh) {
            return false;
        }
        // if (cell.addEdge(edge) == false) {
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
