/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.agenda;

import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

/**
 * @author Nathan Bodenstab
 */
public class APWithMemory extends AgendaParser {

    private ChartEdge agendaMemory[][][];

    public APWithMemory(final ParserDriver opts, final LeftRightListsGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initParser(final ParseTask parseTask) {
        super.initParser(parseTask);

        // TODO: this can be half the size since we only need to allocate space for chart cells that exist
        agendaMemory = new ChartEdge[parseTask.sentenceLength() + 1][parseTask.sentenceLength() + 1][grammar
                .numNonTerms()];
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
