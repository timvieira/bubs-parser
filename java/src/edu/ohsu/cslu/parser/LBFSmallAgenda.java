package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;

public class LBFSmallAgenda extends LocalBestFirstChartParser {

    ChartEdge worstEdge;
    ChartEdge[] edgesInAgenda;

    public LBFSmallAgenda(final Grammar grammar, final EdgeSelector edgeSelector, final CellSelector cellSelector) {
        super(grammar, edgeSelector, cellSelector);
    }

    @Override
    protected void edgeCollectionInit() {
        agenda = new PriorityQueue<ChartEdge>();
        worstEdge = null;
        edgesInAgenda = new ChartEdge[grammar.numNonTerms()];
    }

    @Override
    protected void addEdgeToCollection(final ChartEdge edge) {

        final int parent = edge.prod.parent;
        final ChartEdge agendaEdge = edgesInAgenda[parent];
        if (agendaEdge != null) {
            if (agendaEdge.fom > edge.fom) {
                return;
            }
            agenda.remove(agendaEdge);
            agenda.add(edge);
            edgesInAgenda[edge.prod.parent] = edge;
            nAgendaPush++;
            if (agendaEdge == worstEdge) {
                resetWorstEdge();
            }
        } else {
            if (agenda.size() < maxEdgesToAdd) {
                agenda.add(edge);
                edgesInAgenda[edge.prod.parent] = edge;
                nAgendaPush++;
                if (worstEdge == null || edge.fom < worstEdge.fom) {
                    worstEdge = edge;
                }
            } else if (edge.fom > worstEdge.fom) {
                // must remove worst edge, add new edge, and find new worst edge
                agenda.remove(worstEdge); // O(lg(k))
                agenda.add(edge); // O(lg(k))
                nAgendaPush++;

                edgesInAgenda[worstEdge.prod.parent] = null;
                edgesInAgenda[edge.prod.parent] = edge;

                resetWorstEdge();
            }
        }
        // else just ignore the edge
    }

    protected void resetWorstEdge() {
        worstEdge = agenda.peek();
        for (final ChartEdge agendaEdge : agenda) { // O(k)
            if (agendaEdge.fom < worstEdge.fom) {
                worstEdge = agendaEdge;
            }
        }
    }

    @Override
    protected void addEdgeCollectionToChart(final ChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean addedEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;
        float bestFOM = Float.NEGATIVE_INFINITY;
        if (!agenda.isEmpty()) {
            bestFOM = agenda.peek().fom;
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd && !edgeBelowThresh) {
            edge = agenda.poll();
            // System.out.println(" agendaEdge: " + edge);

            if (edge.fom < bestFOM - logBeamDeltaThresh) {
                edgeBelowThresh = true;
            } else {
                addedEdge = cell.addEdge(edge);
                if (addedEdge) {
                    numAdded++;

                    // Add unary productions to agenda so they can compete with binary productions
                    for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                        final float prob = p.prob + edge.inside;
                        unaryEdge = new ChartEdge(p, cell, prob, edgeSelector);
                        if (unaryEdge.fom > bestFOM - logBeamDeltaThresh) {
                            addEdgeToCollection(unaryEdge);
                        }
                    }
                }
            }
        }
        // System.out.println("");
    }
}
