package edu.ohsu.cslu.parser.agenda;

import java.util.Collection;
import java.util.LinkedList;

import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class ACPGhostEdges extends AgendaChartParser {

	protected LinkedList<ChartEdge>[][] needLeftGhostEdges, needRightGhostEdges;
	int nGhostEdges;

	public ACPGhostEdges(final ParserDriver opts, final LeftRightListsGrammar grammar) {
		super(opts, grammar);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void initParser(final int sentLength) {
		super.initParser(sentLength);

		needLeftGhostEdges = new LinkedList[grammar.numNonTerms()][chart.size() + 1];
		needRightGhostEdges = new LinkedList[grammar.numNonTerms()][chart.size() + 1];
		nGhostEdges = 0;
	}

	@Override
	protected void addEdgeToFrontier(final ChartEdge edge) {
		// There are a lot of unnecessary edges being added to the agenda. For example,
		// edge[i][j][A] w/ prob=0.1 can be pushed, and if it isn't popped off the agenda
		// by the time edge[i][j][A] w/ prob=0.0001 can be pushed, then it is also added
		// to the agenda
		// System.out.println("Agenda Push: "+edge);
		nAgendaPush += 1;
		agenda.add(edge);
	}

	@Override
	protected void expandFrontier(final int nt, final HashSetChartCell cell) {
		LinkedList<ChartEdge> possibleEdges;
		Collection<Production> possibleRules;
		ChartEdge curBestEdge;

		// unary edges are always possible in any cell, although we don't allow
		// unary chains
		// if (newEdge.prod.isUnaryProd() == false || newEdge.prod.isLexProd() == true) {
		for (final Production p : grammar.getUnaryProductionsWithChild(nt)) {
			addEdgeToFrontier(chart.new ChartEdge(p, cell));
		}
		// }

		if (cell == chart.getRootCell()) {
			// no ghost edges possible from here ... only add the root productions
			// actually, TOP edges will already be added in the unary step
			/*
			 * for (final Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) { if (p.parent == grammar.startSymbol) { prob = p.prob + newEdge.insideProb;
			 * addEdgeToAgenda(new ChartEdge(p, rootChartCell, rootChartCell, prob, edgeFOM)); } }
			 */
		} else {
			// connect ghost edges that need left side
			possibleEdges = needLeftGhostEdges[nt][cell.end()];
			if (possibleEdges != null) {
				for (final ChartEdge ghostEdge : possibleEdges) {
					curBestEdge = chart.getCell(cell.start(), ghostEdge.end()).getBestEdge(ghostEdge.prod.parent);
					if (curBestEdge == null) {
						// ghost edge inside prob = grammar rule prob + ONE
						// CHILD inside prob
						addEdgeToFrontier(chart.new ChartEdge(ghostEdge.prod, cell, ghostEdge.rightCell));
					}
				}
			}

			// connect ghost edges that need right side
			possibleEdges = needRightGhostEdges[nt][cell.start()];
			if (possibleEdges != null) {
				for (final ChartEdge ghostEdge : possibleEdges) {
					curBestEdge = chart.getCell(ghostEdge.start(), cell.end()).getBestEdge(ghostEdge.prod.parent);
					if (curBestEdge == null) {
						addEdgeToFrontier(chart.new ChartEdge(ghostEdge.prod, ghostEdge.leftCell, cell));
					}
				}
			}

			// create left ghost edges. Can't go left if we are on the very left
			// side of the chart
			if (cell.start() > 0) {
				possibleRules = grammar.getBinaryProductionsWithRightChild(nt);
				if (possibleRules != null) {
					for (final Production p : possibleRules) {
						if (needLeftGhostEdges[p.leftChild][cell.start()] == null) {
							needLeftGhostEdges[p.leftChild][cell.start()] = new LinkedList<ChartEdge>();
						}
						needLeftGhostEdges[p.leftChild][cell.start()].add(chart.new ChartEdge(p, null, cell));
						nGhostEdges += 1;
					}
				}
			}

			// create right ghost edges. Can't go right if we are on the very
			// right side of the chart
			if (cell.end() < chart.size() - 1) {
				possibleRules = grammar.getBinaryProductionsWithLeftChild(nt);
				if (possibleRules != null) {
					for (final Production p : possibleRules) {
						if (needRightGhostEdges[p.rightChild][cell.end()] == null) {
							needRightGhostEdges[p.rightChild][cell.end()] = new LinkedList<ChartEdge>();
						}
						needRightGhostEdges[p.rightChild][cell.end()].add(chart.new ChartEdge(p, cell, null));
						nGhostEdges += 1;
					}
				}
			}
		}
	}

	@Override
	public String getStats() {
		return super.getStats() + " ghostEdges=" + nGhostEdges;
	}

}
