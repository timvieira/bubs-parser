package edu.ohsu.cslu.parser;

import java.util.Collection;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;

public class LBFOnlineBeam extends LocalBestFirstChartParser {

    float bestFOM, onlineBeam;
    int numEdgesAdded;

    public LBFOnlineBeam(final LeftHashGrammar grammar, final EdgeSelector edgeSelector, final CellSelector cellSelector) {
        super(grammar, edgeSelector, cellSelector);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void visitCell(final ChartCell cell) {
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
            for (final int pos : cell.getPosEntries()) {
                for (final Production p : grammar.getUnaryProductionsWithChild(pos)) {
                    final float prob = p.prob + cell.getBestEdge(pos).inside;
                    edge = new ChartEdge(p, cell, prob, edgeSelector);
                    processEdge(edge, cell);
                }
            }
        } else {
            for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
                final ChartCell leftCell = chart.getCell(start, mid);
                final ChartCell rightCell = chart.getCell(mid, end);
                for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                    for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                        possibleProds = grammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent);
                        if (possibleProds != null) {
                            for (final Production p : possibleProds) {
                                if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
                                    final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                                    edge = new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
                                    processEdge(edge, cell);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void processEdge(final ChartEdge edge, final ChartCell cell) {
        final boolean addedEdge = addEdgeToChart(edge, cell);
        if (addedEdge) {
            // Add unary productions to agenda so they can compete with binary productions
            for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                final float prob = p.prob + edge.inside;
                final ChartEdge unaryEdge = new ChartEdge(p, cell, prob, edgeSelector);
                addEdgeToChart(unaryEdge, cell);
            }
        }
    }

    protected boolean addEdgeToChart(final ChartEdge edge, final ChartCell cell) {
        // if (edge.figureOfMerit < bestFOM - onlineBeam) {
        if (edge.fom < bestFOM - logBeamDeltaThresh) {
            return false;
        }
        if (cell.addEdge(edge) == false) {
            return false;
        }
        if (edge.fom > bestFOM) {
            bestFOM = edge.fom;
        }

        // numEdgesAdded++;
        // onlineBeam = ParserDriver.param2 / numEdgesAdded;

        return true;
    }
}
