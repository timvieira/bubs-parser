package edu.ohsu.cslu.parser.agenda;

import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public class ACPWithMemory extends AgendaChartParser {

	private ChartEdge agendaMemory[][][];

	public ACPWithMemory(final ParserDriver opts, final LeftRightListsGrammar grammar) {
		super(opts, grammar);
	}

	@Override
	protected void initParser(final int sentLength) {
		super.initParser(sentLength);

		// TODO: this can be half the size since we only need to allocate space for chart cells that exist
		agendaMemory = new ChartEdge[sentLength + 1][sentLength + 1][grammar.numNonTerms()];
	}

	@Override
	protected void addEdgeToFrontier(final ChartEdge edge) {
		final ChartEdge bestAgendaEdge = agendaMemory[edge.start()][edge.end()][edge.prod.parent];
		if (bestAgendaEdge == null || edge.fom > bestAgendaEdge.fom) {
			nAgendaPush += 1;
			agenda.add(edge);
			agendaMemory[edge.start()][edge.end()][edge.prod.parent] = edge;
		}
	}

}
