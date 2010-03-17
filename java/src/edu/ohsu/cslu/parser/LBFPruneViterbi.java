package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.CellChart.ChartCell;
import edu.ohsu.cslu.parser.CellChart.ChartEdge;

public class LBFPruneViterbi extends LocalBestFirstChartParser<LeftHashGrammar, CellChart> {

    ChartEdge[] bestEdges;
    float bestFOM;

    public LBFPruneViterbi(final ParserOptions opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void edgeCollectionInit() {
        bestEdges = new ChartEdge[grammar.numNonTerms()];
        bestFOM = Float.NEGATIVE_INFINITY;
    }

    @Override
    protected void addEdgeToCollection(final ChartEdge edge) {
        final int parent = edge.prod.parent;
        if (bestEdges[parent] == null || edge.fom > bestEdges[parent].fom) {
            bestEdges[parent] = edge;

            if (edge.fom > bestFOM) {
                bestFOM = edge.fom;
            }
        }
    }

    @Override
    protected void addEdgeCollectionToChart(final ChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;

        agenda = new PriorityQueue<ChartEdge>();
        for (final ChartEdge viterbiEdge : bestEdges) {
            if (viterbiEdge != null && viterbiEdge.fom > bestFOM - logBeamDeltaThresh) {
                agenda.add(viterbiEdge);
                nAgendaPush++;
            }
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd && !edgeBelowThresh) {
            edge = agenda.poll();
            if (edge.fom < bestFOM - logBeamDeltaThresh) {
                edgeBelowThresh = true;
            } else if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
                numAdded++;

                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = chart.new ChartEdge(p, cell);
                    final int nt = p.parent;
                    if ((bestEdges[nt] == null || unaryEdge.fom > bestEdges[nt].fom) && (unaryEdge.fom > bestFOM - logBeamDeltaThresh)) {
                        // if (unaryEdge.fom > bestFOM - logBeamDeltaThresh) {
                        agenda.add(unaryEdge);
                    }
                }
            }
        }
    }
}
