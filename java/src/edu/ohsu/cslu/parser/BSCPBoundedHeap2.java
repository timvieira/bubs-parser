package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPBoundedHeap2 extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

	ChartEdge worstEdge;
	ChartEdge[] edgesInAgenda;

	public BSCPBoundedHeap2(final ParserDriver opts, final LeftHashGrammar grammar) {
		super(opts, grammar);
	}

	@Override
	protected void edgeCollectionInit() {
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
			nAgendaPush++;
			if (agendaEdge == worstEdge) {
				resetWorstEdge();
			}
		} else {
			if (agenda.size() < beamWidth) {
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
	protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
		ChartEdge edge, unaryEdge;
		boolean edgeBelowThresh = false;
		int numAdded = 0;
		float bestFOM = Float.NEGATIVE_INFINITY;
		if (!agenda.isEmpty()) {
			bestFOM = agenda.peek().fom;
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
					if (unaryEdge.fom > bestFOM - beamDeltaThresh) {
						addEdgeToCollection(unaryEdge);
					}
				}
			}
		}
	}
}
