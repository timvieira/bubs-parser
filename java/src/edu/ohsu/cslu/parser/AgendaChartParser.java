package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;

public class AgendaChartParser extends ChartParser<LeftRightListsGrammar, CellChart> {

	protected PriorityQueue<ChartEdge> agenda;
	protected int nAgendaPush, nAgendaPop, nChartEdges;

	public AgendaChartParser(final ParserDriver opts, final LeftRightListsGrammar grammar) {
		super(opts, grammar);
	}

	@Override
	protected void initParser(final int sentLength) {
		// super.initParser(sentLength);
		chart = new CellChart(sentLength, opts.viterbiMax, this);

		agenda = new PriorityQueue<ChartEdge>();
		nAgendaPush = nAgendaPop = nChartEdges = 0;
	}

	public ParseTree findMLParse(final String sentence) throws Exception {
		return findBestParse(sentence);
	}

	@Override
	public ParseTree findBestParse(final String sentence) throws Exception {
		ChartEdge edge;
		HashSetChartCell cell;
		final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);

		initParser(sent.length);
		addLexicalProductions(sent);
		edgeSelector.init(this);

		// for (final ChartEdge lexEdge : edgesToExpand) {
		// expandFrontier(lexEdge, chart.getCell(lexEdge.start(), lexEdge.end()));
		// }
		for (int i = 0; i < sent.length; i++) {
			cell = chart.getCell(i, i + 1);
			for (final int nt : cell.getPosNTs()) {
				expandFrontier(nt, cell);
			}
		}

		while (!agenda.isEmpty() && !hasCompleteParse()) {
			edge = agenda.poll(); // get and remove top agenda edge
			nAgendaPop += 1;
			// System.out.println("AgendaPop: " + edge);

			cell = chart.getCell(edge.start(), edge.end());
			final int nt = edge.prod.parent;
			final float insideProb = edge.inside();
			if (insideProb > cell.getInside(nt)) {
				cell.updateInside(edge);
				// if A->B C is added to chart but A->X Y was already in this chart cell, then the
				// first edge must have been better than the current edge because we pull edges
				// from the agenda best-first. This also means that the entire frontier
				// has already been added.
				expandFrontier(nt, cell);
				nChartEdges += 1;

			}

		}

		if (agenda.isEmpty()) {
			Log.info(1, "WARNING: Agenda is empty.  All edges have been added to chart.");
		}

		// agenda.clear();
		// System.gc();
		return extractBestParse();
	}

	protected void addEdgeToFrontier(final ChartEdge edge) {
		// System.out.println("AgendaPush: " + edge);
		nAgendaPush += 1;
		agenda.add(edge);
	}

	@Override
	protected void addLexicalProductions(final int sent[]) throws Exception {
		HashSetChartCell cell;

		// add lexical productions and unary productions to the base cells of the chart
		for (int i = 0; i < chart.size(); i++) {
			cell = chart.getCell(i, i + 1);
			for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
				// Add lexical prods directly to the chart instead of to the agenda because
				// the boundary FOM (and possibly others use the surrounding POS tags to calculate
				// the fit of a new edge. If the POS tags don't exist yet (are still in the agenda)
				// it skew probs (to -Infinity) and never allow some edges that should be allowed
				cell.updateInside(lexProd, lexProd.prob);

			}
		}
	}

	// protected void expandFrontier(final ChartEdge newEdge, final ChartCell cell) {
	protected void expandFrontier(final int nt, final HashSetChartCell cell) {
		ChartEdge leftEdge, rightEdge;
		HashSetChartCell rightCell, leftCell;

		// unary edges are always possible in any cell, although we don't allow unary chains
		// NATE: update: unary chains should be fine. They will just compete on the agenda
		for (final Production p : grammar.getUnaryProductionsWithChild(nt)) {
			addEdgeToFrontier(chart.new ChartEdge(p, cell));
		}

		// connect edge as possible right non-term
		for (int beg = 0; beg < cell.start(); beg++) {
			leftCell = chart.getCell(beg, cell.start());
			for (final Production p : grammar.getBinaryProductionsWithRightChild(nt)) {
				leftEdge = leftCell.getBestEdge(p.leftChild);
				if (leftEdge != null && chart.getCell(beg, cell.end()).getBestEdge(p.parent) == null) {
					// prob = p.prob + newEdge.inside + leftEdge.inside;
					// System.out.println("LEFT:"+new ChartEdge(p, prob, leftCell, cell));
					addEdgeToFrontier(chart.new ChartEdge(p, leftCell, cell));
				}
			}
		}

		// connect edge as possible left non-term
		for (int end = cell.end() + 1; end <= chart.size(); end++) {
			rightCell = chart.getCell(cell.end(), end);
			for (final Production p : grammar.getBinaryProductionsWithLeftChild(nt)) {
				rightEdge = rightCell.getBestEdge(p.rightChild);
				if (rightEdge != null && chart.getCell(cell.start(), end).getBestEdge(p.parent) == null) {
					// prob = p.prob + rightEdge.inside + newEdge.inside;
					// System.out.println("RIGHT: "+new ChartEdge(p,prob, cell,rightCell));
					addEdgeToFrontier(chart.new ChartEdge(p, cell, rightCell));
				}
			}
		}
	}

	@Override
	public String getStats() {
		return " chartEdges=" + nChartEdges + " agendaPush=" + nAgendaPush + " agendaPop=" + nAgendaPop;
	}
}
