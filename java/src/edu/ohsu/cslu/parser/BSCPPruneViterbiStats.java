package edu.ohsu.cslu.parser;

import java.util.List;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.util.Log;

public class BSCPPruneViterbiStats extends BSCPPruneViterbi {

	int nPopBinary, nPopUnary, nGoldEdges;

	public BSCPPruneViterbiStats(final ParserDriver opts, final LeftHashGrammar grammar) {
		super(opts, grammar);
	}

	@Override
	protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
		ChartEdge edge, unaryEdge;
		boolean edgeBelowThresh = false;
		int numAdded = 0, goldRank = -1;
		cell.numEdgesAdded = 0;

		nPopBinary = 0;
		nPopUnary = 0;
		nGoldEdges = 0;

		agenda = new PriorityQueue<ChartEdge>();
		for (final ChartEdge viterbiEdge : bestEdges) {
			if (viterbiEdge != null && viterbiEdge.fom > bestFOM - beamDeltaThresh) {
				agenda.add(viterbiEdge);
				nAgendaPush++;
			}
		}

		if (inputTreeChart == null) {
			Log.info(0, "ERROR: Beam Search parser with stats must provide gold trees as input");
		}
		final List<ChartEdge> goldEdges = inputTreeChart.getCell(cell.start(), cell.end()).getBestEdgeList();
		boolean hasGoldEdge = false;
		nGoldEdges = goldEdges.size();
		if (nGoldEdges > 0) {
			hasGoldEdge = true;
			// for (final ChartEdge x : goldEdges) {
			// System.out.println("Gold edges: " + x);
			// }
		}

		// I think a more fair comparison is when we don't stop once we reach the gold edge
		// since this effects the population of cells down stream.
		// while (agenda.isEmpty() == false && numAdded <= beamWidth && !edgeBelowThresh && !addedGoldEdges) {
		while (agenda.isEmpty() == false && numAdded <= beamWidth && !edgeBelowThresh) {
			edge = agenda.poll();

			if (hasGoldEdge) {
				ChartEdge goldEdgeAdded = null;
				for (final ChartEdge goldEdge : goldEdges) {
					if (edge.equals(goldEdge)) {
						goldEdgeAdded = goldEdge;
					}
				}
				if (goldEdgeAdded != null) {
					goldEdges.remove(goldEdgeAdded);
					if (goldEdges.size() == 0) {
						goldRank = numAdded + 1;
					}
				}
			}

			if (edge.fom < bestFOM - beamDeltaThresh) {
				edgeBelowThresh = true;
			} else if (edge.inside() > cell.getInside(edge.prod.parent)) {
				cell.updateInside(edge);
				numAdded++;

				if (edge.prod.isBinaryProd())
					nPopBinary++;
				if (edge.prod.isUnaryProd())
					nPopUnary++;

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

		if (nGoldEdges > 0) {
			System.out.println("DSTAT: " + this.chart.size() + " " + cell.width() + " " + nGoldEdges + " " + goldRank + " " + nPopBinary + " " + nPopUnary + " "
					+ (nPopBinary + nPopUnary) + " ");
		}
	}
}
