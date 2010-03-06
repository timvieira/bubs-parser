package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;

public class LBFPruneViterbi extends LocalBestFirstChartParser {

    ChartEdge[] bestEdges;
    float bestFOM;

    public LBFPruneViterbi(final LeftHashGrammar grammar, final EdgeSelector edgeSelector, final CellSelector spanSelector) {
        super(grammar, edgeSelector, spanSelector);
    }

    @Override
    protected void edgeCollectionInit() {
        bestEdges = new ChartEdge[grammar.numNonTerms()];
        bestFOM = Float.NEGATIVE_INFINITY;
    }

    @Override
    protected void addEdgeToCollection(final ChartEdge edge) {
        final int parent = edge.prod.parent;
        if (bestEdges[parent] == null || bestEdges[parent].fom < edge.fom) {
            bestEdges[parent] = edge;

            if (edge.fom > bestFOM) {
                bestFOM = edge.fom;
            }
        }
    }

    @Override
    protected void addEdgeCollectionToChart(final ChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean addedEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;

        agenda = new PriorityQueue<ChartEdge>();
        for (int i = 0; i < grammar.numNonTerms(); i++) {
            if (bestEdges[i] != null && bestEdges[i].fom > bestFOM - logBeamDeltaThresh) {
                agenda.add(bestEdges[i]);
                nAgendaPush++;
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
                        if ((bestEdges[p.parent] == null || bestEdges[p.parent].fom < unaryEdge.fom) && unaryEdge.fom > bestFOM - logBeamDeltaThresh) {
                            agenda.add(unaryEdge);
                            nAgendaPush++;
                        }
                    }
                }
            }
        }
    }
}
