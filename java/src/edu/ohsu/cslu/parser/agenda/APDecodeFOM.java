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

import cltool4j.BaseLogger;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class APDecodeFOM extends APWithMemory {

    // only change from APWithMemory is that we are using the edge's
    // FOM score instead of the inside score to determine the "best" edge
    // in the chart. This causes the edge's score to be computed as:
    // score(A-> B C, [i,j,k]) = FOM(edge) + FOM(B[i,j]) + FOM(C[j,k])

    public APDecodeFOM(final ParserDriver opts, final LeftRightListsGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) {
        ChartEdge edge;
        HashSetChartCell cell;

        initParser(tokens);
        edgeSelector.init(chart);

        addLexicalProductions(tokens);

        for (int i = 0; i < tokens.length; i++) {
            cell = chart.getCell(i, i + 1);
            for (final int nt : cell.getPosNTs()) {
                expandFrontier(nt, cell);
            }
        }

        while (!agenda.isEmpty() && !chart.hasCompleteParse(grammar.startSymbol)) {
            edge = agenda.poll(); // get and remove top agenda edge
            nAgendaPop += 1;

            cell = chart.getCell(edge.start(), edge.end());
            final int nt = edge.prod.parent;

            // System.out.println(edge + " best=" + cell.getInside(nt));

            // final float insideProb = edge.inside();
            final float fomScore = edge.fom; // ** THE CHANGE **

            if (fomScore > cell.getInside(nt)) {
                cell.bestEdge[nt] = edge;
                cell.updateInside(nt, fomScore);
                // if A->B C is added to chart but A->X Y was already in this chart cell, then the
                // first edge must have been better than the current edge because we pull edges
                // from the agenda best-first. This also means that the entire frontier
                // has already been added.
                expandFrontier(nt, cell);
                nChartEdges += 1;
            }
        }

        if (agenda.isEmpty()) {
            BaseLogger.singleton().info("WARNING: Agenda is empty.  All edges have been added to chart.");
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    @Override
    protected void addLexicalProductions(final int sent[]) {
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

}
