package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;

public class LBFWeakThresh extends LBFPruneViterbi {

    public LBFWeakThresh(final Grammar grammar, final EdgeSelector edgeSelector, final CellSelector cellSelector) {
        super(grammar, edgeSelector, cellSelector);
    }

    @Override
    protected void addEdgeCollectionToChart(final ChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean addedEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;
        boolean pastMaxEdges = false;
        float weakThresh = Float.POSITIVE_INFINITY;

        agenda = new PriorityQueue<ChartEdge>();
        int j = 0;
        for (int i = 0; i < grammar.numNonTerms(); i++) {
            final ChartEdge curEdge = bestEdges[i];
            if (curEdge != null && (curEdge.fom > bestFOM - logBeamDeltaThresh) && (!pastMaxEdges || curEdge.fom > weakThresh)) {
                agenda.add(bestEdges[i]);
                nAgendaPush++;
                j++;

                if (j <= maxEdgesToAdd && curEdge.fom < weakThresh) {
                    weakThresh = curEdge.fom;
                    if (j == maxEdgesToAdd) {
                        pastMaxEdges = true;
                    }
                }
            }
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd && !edgeBelowThresh) {
            edge = agenda.poll();
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
                        if ((bestEdges[p.parent] == null || bestEdges[p.parent].fom < unaryEdge.fom) && (unaryEdge.fom > bestFOM - logBeamDeltaThresh)
                                && (!pastMaxEdges || unaryEdge.fom > weakThresh)) {
                            agenda.add(unaryEdge);
                            nAgendaPush++;
                        }
                    }
                }
            }
        }
    }
}
