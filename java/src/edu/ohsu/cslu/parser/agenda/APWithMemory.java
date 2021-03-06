/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.parser.agenda;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * @author Nathan Bodenstab
 */
public class APWithMemory extends AgendaParser {

    private float agendaMemory[][][];

    public APWithMemory(final ParserDriver opts, final LeftRightListsGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initParser(final ParseTask parseTask) {
        super.initParser(parseTask);
        final int sentLen = parseTask.sentenceLength();

        agendaMemory = new float[sentLen + 1][sentLen + 1][];

        for (short span = 1; span <= sentLen; span++) {
            for (short start = 0; start < sentLen - span + 1; start++) { // beginning
                final int end = start + span;
                agendaMemory[start][end] = new float[grammar.numNonTerms()];
                Arrays.fill(agendaMemory[start][end], Float.NEGATIVE_INFINITY);
            }
        }
    }

    @Override
    protected void addEdgeToFrontier(final Production p, final int start, final int mid, final int end) {
        final int nt = p.parent;
        float edgeInside;

        // NB: adding this check doubles the speed and slightly increases acc (on first 10 sent
        // of sec 22 w/ sm6 grammar and boundary FOM). Not sure why. It prevents us from doing
        // over-parsing ...
        // if (chart.getCell(start, end).getBestEdge(p.parent) != null)
        // return;

        if (mid < 0) {
            edgeInside = chart.getInside(start, end, p.leftChild) + p.prob;
        } else {
            edgeInside = chart.getInside(start, mid, p.leftChild) + chart.getInside(mid, end, p.rightChild) + p.prob;
        }

        if (edgeInside > agendaMemory[start][end][nt]) {
            super.addEdgeToFrontier(p, start, mid, end);
            agendaMemory[start][end][nt] = edgeInside;
        }

        // if (p.isLexProd()) {
        // fom = this.fomModel.calcLexicalFOM(start, end, (short) p.parent, edgeInside);
        // } else {
        // fom = this.fomModel.calcFOM(start, end, (short) p.parent, edgeInside);
        // }
        //
        // if (fom > agendaMemory[start][end][nt]) {
        // super.addEdgeToFrontier(p, start, mid, end);
        // agendaMemory[start][end][nt] = fom;
        // }
    }
}
