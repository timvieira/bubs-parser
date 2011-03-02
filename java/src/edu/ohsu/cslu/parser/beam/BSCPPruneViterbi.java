package edu.ohsu.cslu.parser.beam;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPPruneViterbi extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    ChartEdge[] bestEdges;

    public BSCPPruneViterbi(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
        bestEdges = new ChartEdge[grammar.numNonTerms()];
    }

    @Override
    protected void initCell(final short start, final short end) {
        super.initCell(start, end);
        Arrays.fill(bestEdges, null);
    }

    @Override
    protected boolean fomCheckAndUpdate(final ChartEdge edge) {
        final int parent = edge.prod.parent;
        return super.fomCheckAndUpdate(edge) && (bestEdges[parent] == null || edge.fom >= bestEdges[parent].fom);
    }

    @Override
    protected void addEdgeToCollection(final ChartEdge edge) {
        cellConsidered++;
        if (fomCheckAndUpdate(edge)) {
            bestEdges[edge.prod.parent] = edge;
        }
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {

        // int cellBeamWidth = beamWidth;
        // if (hasCellConstraints && cellConstraints.factoredParentsOnly(cell.start(), cell.end())) {
        // cellBeamWidth = factoredBeamWidth;
        // }

        agenda = new PriorityQueue<ChartEdge>();
        // agenda = new CircularBoundedHeap(beamWidth); // No speed gain with CircularBoundedHeap
        // printBestEdgeStats(cell);

        // I tried clearing bestEdges while we iterate through them, but this affects
        // the unary fomCheck and doesn't really gain us any speed
        for (final ChartEdge viterbiEdge : bestEdges) {
            if (viterbiEdge != null && fomCheckAndUpdate(viterbiEdge)) {
                agenda.add(viterbiEdge);
                cellPushed++;
            }
        }

        ChartEdge edge = agenda.poll();
        while (edge != null && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
            cellPopped++;
            if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
                cellPopped++;
                BaseLogger.singleton().finest("" + edge);

                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    cellConsidered++;
                    final ChartEdge unaryEdge = chart.new ChartEdge(p, cell);
                    if (fomCheckAndUpdate(unaryEdge)) {
                        agenda.add(unaryEdge);
                        cellPushed++;
                    }
                }
            }
            edge = agenda.poll();
        }
    }

    private void printBestEdgeStats(final HashSetChartCell cell) {
        System.out.println("INFO: [" + cell.start() + "," + cell.end() + "] agendaSize=" + agenda.size()
                + " globalBestFOM=" + globalBestFOM);
        if (agenda.size() >= 2) {
            final ChartEdge bestEdge = agenda.poll();
            final float score1 = bestEdge.fom;
            final float score2 = agenda.peek().fom;
            agenda.add(bestEdge);
            final LinkedList<edu.ohsu.cslu.parser.chart.Chart.ChartEdge> goldEdges = currentInput.inputTreeChart
                    .getEdgeList(cell.start(), cell.end());
            final boolean hasGold = goldEdges.size() > 0;
            final boolean underThresh = fomCheckAndUpdate(bestEdge);

            System.out.println("INFO: agendaOneTwo gold=" + hasGold + " prune1=" + underThresh + " " + score1 + " "
                    + score2 + " " + (score2 - score1));
        }
    }
}
