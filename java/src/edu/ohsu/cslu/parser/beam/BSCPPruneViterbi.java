package edu.ohsu.cslu.parser.beam;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPPruneViterbi extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

	ChartEdge[] bestEdges;
	float bestFOM;
	int numEdgesConsidered = 0;

	public BSCPPruneViterbi(final ParserDriver opts, final LeftHashGrammar grammar) {
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
		numEdgesConsidered++;
		if (bestEdges[parent] == null || edge.fom > bestEdges[parent].fom) {
			bestEdges[parent] = edge;

			if (edge.fom > bestFOM) {
				bestFOM = edge.fom;
			}
		}
	}

	@Override
	protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
		ChartEdge edge, unaryEdge;
		boolean edgeBelowThresh = false;
		int numAdded = 0;
		cell.numEdgesAdded = 0;

		agenda = new PriorityQueue<ChartEdge>();
		for (final ChartEdge viterbiEdge : bestEdges) {
			if (viterbiEdge != null && viterbiEdge.fom > bestFOM - beamDeltaThresh) {
				agenda.add(viterbiEdge);
				nAgendaPush++;
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
					final int nt = p.parent;
					if ((bestEdges[nt] == null || unaryEdge.fom > bestEdges[nt].fom) && (unaryEdge.fom > bestFOM - beamDeltaThresh)) {
						agenda.add(unaryEdge);
					}
				}
			}
		}
		cell.numEdgesConsidered = numEdgesConsidered;
		numEdgesConsidered = 0;
	}
}
