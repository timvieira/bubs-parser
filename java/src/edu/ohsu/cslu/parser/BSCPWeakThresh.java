package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPWeakThresh extends BSCPPruneViterbi {

    public BSCPWeakThresh(final ParserOptions opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;
        boolean pastMaxEdges = false;
        float weakThresh = Float.POSITIVE_INFINITY;

        agenda = new PriorityQueue<ChartEdge>();
        int j = 0;
        for (int i = 0; i < grammar.numNonTerms(); i++) {
            final ChartEdge curEdge = bestEdges[i];
            if (curEdge != null && (curEdge.fom > bestFOM - beamDeltaThresh)
                    && (!pastMaxEdges || curEdge.fom > weakThresh)) {
                agenda.add(bestEdges[i]);
                nAgendaPush++;
                j++;

                if (j <= beamWidth && curEdge.fom < weakThresh) {
                    weakThresh = curEdge.fom;
                    if (j == beamWidth) {
                        pastMaxEdges = true;
                    }
                }
            }
        }

        while (agenda.isEmpty() == false && numAdded <= beamWidth && !edgeBelowThresh) {
            edge = agenda.poll();
            if (edge.fom < bestFOM - beamDeltaThresh) {
                edgeBelowThresh = true;
            } else if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
                numAdded++;

                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = chart.new ChartEdge(p, cell);
                    if ((bestEdges[p.parent] == null || bestEdges[p.parent].fom < unaryEdge.fom)
                            && (unaryEdge.fom > bestFOM - beamDeltaThresh)
                            && (!pastMaxEdges || unaryEdge.fom > weakThresh)) {
                        agenda.add(unaryEdge);
                        nAgendaPush++;
                    }
                }
            }
        }
    }
}
