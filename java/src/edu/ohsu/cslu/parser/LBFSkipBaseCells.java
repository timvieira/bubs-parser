package edu.ohsu.cslu.parser;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;

public class LBFSkipBaseCells extends LocalBestFirstChartParser {

    public LBFSkipBaseCells(final LeftHashGrammar grammar, final EdgeSelector edgeSelector, final CellSelector cellSelector) {
        super(grammar, edgeSelector, cellSelector);
    }

    @Override
    protected List<ChartEdge> addLexicalProductions(final int sent[]) throws Exception {
        ChartCell cell;

        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                cell.addEdge(new ChartEdge(lexProd, cell, lexProd.prob));

                // NOTE: also adding unary prods here...should probably change the name of this
                // function and also create our own init()
                for (final Production unaryProd : grammar.getUnaryProductionsWithChild(lexProd.parent)) {
                    // NOTE: not using an FOM for these edges ... just adding them all
                    cell.addEdge(unaryProd, cell, null, lexProd.prob + unaryProd.prob);
                }
            }
        }
        return null;
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();
        Collection<Production> possibleProds;
        ChartEdge edge;

        // TODO: Can we do something faster to find the best N edges other than
        // sorting ALL of them into an agenda?
        final ChartEdge bestEdges[] = new ChartEdge[grammar.numNonTerms()]; // inits to null

        assert (end - start >= 2);
        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                    possibleProds = grammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent);
                    if (possibleProds != null) {
                        for (final Production p : possibleProds) {
                            final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                            edge = new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
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
        nAgendaPush++;
    }

    private void addBestEdgesToChart(final ChartCell cell, final ChartEdge[] bestEdges) {
        ChartEdge edge, unaryEdge;
        boolean addedEdge;
        int numAdded = 0;

        agenda = new PriorityQueue<ChartEdge>();
        for (int i = 0; i < grammar.numNonTerms(); i++) {
            if (bestEdges[i] != null) {
                addEdgeToAgenda(bestEdges[i]);
            }
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd) {
            edge = agenda.poll();
            addedEdge = cell.addEdge(edge);
            if (addedEdge) {
                // System.out.println(" addingEdge:" + edge);
                numAdded++;
                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    final float prob = p.prob + edge.inside;
                    unaryEdge = new ChartEdge(p, cell, prob, edgeSelector);
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
                final ChartCell cell = chart.getCell(start, start + span);
                if (cell.numSpanVisits > 0) {
                    cellsVisited++;
                    cellVisits += cell.numSpanVisits;
                } else {
                    cellsSkipped++;
                }
            }
        }

        return super.getStats() + " agendaPush=" + nAgendaPush + " fomInitSec=" + fomInitSeconds + " #cells=" + cells + " #visited=" + cellsVisited + " #skipped=" + cellsSkipped
                + " #totalVisits=" + cellVisits;
    }
}
