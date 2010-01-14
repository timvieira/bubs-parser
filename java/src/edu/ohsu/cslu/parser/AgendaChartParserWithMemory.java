package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.parser.fom.EdgeFOM;

public class AgendaChartParserWithMemory extends AgendaChartParser {

    private ChartEdgeWithFOM agendaMemory[][][];

    public AgendaChartParserWithMemory(final GrammarByLeftNonTermList grammar, final EdgeFOM edgeFOM) {
        super(grammar, edgeFOM);
    }

    @Override
    protected void initParser(final int sentLength) {
        super.initParser(sentLength);
        agendaMemory = new ChartEdgeWithFOM[sentLength + 1][sentLength + 1][grammar.numNonTerms()];
        for (int i = 0; i < sentLength + 1; i++) {
            for (int j = 0; j < sentLength + 1; j++) {
                for (int k = 0; k < grammar.numNonTerms(); k++) {
                    agendaMemory[i][j][k] = null;
                }
            }
        }
    }

    @Override
    protected void addEdgeToFrontier(final ChartEdgeWithFOM edge) {
        final ChartEdgeWithFOM bestAgendaEdge = agendaMemory[edge.start()][edge.end()][edge.p.parent];
        if (bestAgendaEdge == null || edge.figureOfMerit > bestAgendaEdge.figureOfMerit) {
            nAgendaPush += 1;
            agenda.add(edge);
            agendaMemory[edge.start()][edge.end()][edge.p.parent] = edge;
        }
    }

}
