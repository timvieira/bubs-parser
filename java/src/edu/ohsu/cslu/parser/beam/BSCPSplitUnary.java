package edu.ohsu.cslu.parser.beam;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPSplitUnary extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    int unaryBeamWidth;

    public BSCPSplitUnary(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    // Instead of adding unary productions to the local agenda and
    // making them compete with binary productions, process them separately
    // after the binary processing
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {

        ChartEdge edge = agenda.poll();
        while (edge != null && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
            cellPopped++;

            if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
            }
            edge = agenda.poll();
        }

        // unary prods
        final PriorityQueue<ChartEdge> unaryAgenda = new PriorityQueue<ChartEdge>();
        unaryBeamWidth = beamWidth;

        // could skip factored non-terms here if we knew which were which
        for (final int i : grammar.phraseSet) {
            final float childInside = cell.getInside(i);
            if (childInside > Float.NEGATIVE_INFINITY) {
                for (final Production p : grammar.getUnaryProductionsWithChild(i)) {
                    final float inside = childInside + p.prob;
                    if (inside > cell.getInside(p.parent)) {
                        unaryAgenda.add(chart.new ChartEdge(p, cell));
                    }
                }
            }
        }

        int unaryPopped = 0;
        edge = unaryAgenda.poll();
        while (edge != null && unaryPopped < unaryBeamWidth) {
            unaryPopped++;
            cell.updateInside(edge);
            edge = unaryAgenda.poll();
        }
    }

}
