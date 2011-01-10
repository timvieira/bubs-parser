package edu.ohsu.cslu.parser.beam;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

/**
 * 
 * @author Nate Bodenstab This is currently slower than BSCPPruneViterbi.
 */
public class BSCPBoundedHeap extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    ChartEdge worstEdge;
    ChartEdge[] edgesInAgenda;

    public BSCPBoundedHeap(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initCell(final short start, final short end) {
        agenda = new PriorityQueue<ChartEdge>();
        worstEdge = null;
        edgesInAgenda = new ChartEdge[grammar.numNonTerms()];
    }

    // v2: keep a heap of only size k by removing the worst edge when adding a better one
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
            cellPushed++;
            if (agendaEdge == worstEdge) {
                resetWorstEdge();
            }
        } else {
            if (agenda.size() < beamWidth) {
                agenda.add(edge);
                edgesInAgenda[edge.prod.parent] = edge;
                cellPushed++;
                if (worstEdge == null || edge.fom < worstEdge.fom) {
                    worstEdge = edge;
                }
            } else if (edge.fom > worstEdge.fom) {
                // must remove worst edge, add new edge, and find new worst edge
                agenda.remove(worstEdge); // O(lg(k))
                agenda.add(edge); // O(lg(k))
                cellPushed++;

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

    // @Override
    // protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
    // ChartEdge edge, unaryEdge;
    // boolean edgeBelowThresh = false;
    //
    // while (agenda.isEmpty() == false && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
    // edge = agenda.poll();
    // if (edge.inside() > cell.getInside(edge.prod.parent)) {
    // cell.updateInside(edge);
    // cellPopped++;
    // logger.finest("" + edge);
    //
    // // Add unary productions to agenda so they can compete with binary productions
    // for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
    // unaryEdge = chart.new ChartEdge(p, cell);
    // cellConsidered++;
    // if (unaryEdge.fom > bestFOM - beamDeltaThresh) {
    // addEdgeToCollection(unaryEdge);
    // }
    // }
    // }
    // }
    // }
}
