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

import java.util.PriorityQueue;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

/**
 * @author Nathan Bodenstab
 */
public class AgendaParser extends Parser<LeftRightListsGrammar> {

    protected PriorityQueue<ChartEdge> agenda;
    public CellChart chart;
    protected int nAgendaPush, nAgendaPop, nChartEdges;

    public AgendaParser(final ParserDriver opts, final LeftRightListsGrammar grammar) {
        super(opts, grammar);
    }

    protected void initParser(final int[] tokens) {
        chart = new CellChart(tokens, this);

        agenda = new PriorityQueue<ChartEdge>();
        nAgendaPush = nAgendaPop = nChartEdges = 0;
    }

    @Override
    public BinaryTree<String> findBestParse(final int[] tokens) {
        ChartEdge edge;
        HashSetChartCell cell;

        initParser(tokens);
        addLexicalProductions(tokens);
        fomModel.init(parseTask);

        // for (final ChartEdge lexEdge : edgesToExpand) {
        // expandFrontier(lexEdge, chart.getCell(lexEdge.start(), lexEdge.end()));
        // }
        for (int i = 0; i < tokens.length; i++) {
            cell = chart.getCell(i, i + 1);
            for (final int nt : cell.getPosNTs()) {
                expandFrontier(nt, cell);
            }
        }

        while (!agenda.isEmpty() && !chart.hasCompleteParse(grammar.startSymbol)) {
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
            BaseLogger.singleton().info("WARNING: Agenda is empty.  All edges have been added to chart.");
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    protected void addEdgeToFrontier(final ChartEdge edge) {
        // System.out.println("AgendaPush: " + edge);
        nAgendaPush += 1;
        agenda.add(edge);
    }

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

    @Override
    public float getInside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @Override
    public float getOutside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }
}
