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

import java.util.Collection;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class CoarseCellAgendaParser extends Parser<LeftHashGrammar> {

    float[][] maxEdgeFOM;
    PriorityQueue<HashSetChartCell> spanAgenda;
    public CellChart chart;

    public CoarseCellAgendaParser(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    protected void initParser(final int[] tokens) {
        chart = new CellChart(tokens, opts.viterbiMax(), this);
        this.maxEdgeFOM = new float[chart.size()][chart.size() + 1];
        this.spanAgenda = new PriorityQueue<HashSetChartCell>();

        // The chart is (chart.size()+1)*chart.size()/2
        for (int start = 0; start < chart.size(); start++) {
            for (int end = start + 1; end < chart.size() + 1; end++) {
                maxEdgeFOM[start][end] = Float.NEGATIVE_INFINITY;
            }
        }
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) {
        HashSetChartCell cell;

        initParser(tokens);
        addLexicalProductions(tokens);
        edgeSelector.init(chart);
        addUnaryExtensionsToLexProds();

        for (int i = 0; i < chart.size(); i++) {
            expandFrontier(chart.getCell(i, i + 1));
        }

        while (hasNext() && !chart.hasCompleteParse(grammar.startSymbol)) {
            cell = next();
            // System.out.println(" nextCell: " + cell);
            visitCell(cell);
            expandFrontier(cell);
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    protected void visitCell(final HashSetChartCell cell) {
        final int start = cell.start(), end = cell.end();
        Collection<Production> possibleProds;
        ChartEdge edge;
        final ChartEdge[] bestEdges = new ChartEdge[grammar.numNonTerms()]; // inits to null

        final int maxEdgesToAdd = (int) opts.param2;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    possibleProds = grammar.getBinaryProductionsWithChildren(leftNT, rightNT);
                    if (possibleProds != null) {
                        for (final Production p : possibleProds) {
                            edge = chart.new ChartEdge(p, leftCell, rightCell);
                            addEdgeToArray(edge, bestEdges);
                        }
                    }
                }
            }
        }

        addBestEdgesToChart(cell, bestEdges, maxEdgesToAdd);
    }

    protected void addEdgeToArray(final ChartEdge edge, final ChartEdge[] bestEdges) {
        final int parent = edge.prod.parent;
        if (bestEdges[parent] == null || edge.fom > bestEdges[parent].fom) {
            bestEdges[parent] = edge;
        }
    }

    private void addEdgeToAgenda(final ChartEdge edge, final PriorityQueue<ChartEdge> agenda) {
        agenda.add(edge);
    }

    protected void addBestEdgesToChart(final HashSetChartCell cell, final ChartEdge[] bestEdges, final int maxEdgesToAdd) {
        ChartEdge edge, unaryEdge;
        int numAdded = 0;

        final PriorityQueue<ChartEdge> agenda = new PriorityQueue<ChartEdge>();
        for (int i = 0; i < bestEdges.length; i++) {
            if (bestEdges[i] != null) {
                addEdgeToAgenda(bestEdges[i], agenda);
            }
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd) {
            edge = agenda.poll();
            // addedEdge = cell.addEdge(edge);
            // if (addedEdge) {
            final int nt = edge.prod.parent;
            final float insideProb = edge.inside();
            if (insideProb > cell.getInside(edge.prod.parent)) {
                cell.updateInside(nt, insideProb);
                // System.out.println(" addingEdge: " + edge);
                numAdded++;
                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = chart.new ChartEdge(p, cell);
                    addEdgeToAgenda(unaryEdge, agenda);
                }
            }
        }

        // TODO: should I decrease the maxEdgeFOM here according to the best edge NOT in the chart?
        // won't this just be overrun when we expand the frontier?
        if (agenda.isEmpty()) {
            maxEdgeFOM[cell.start()][cell.end()] = Float.NEGATIVE_INFINITY;
        } else {
            maxEdgeFOM[cell.start()][cell.end()] = agenda.peek().fom;
        }
    }

    protected boolean hasNext() {
        return true;
    }

    protected HashSetChartCell next() {
        // return spanAgenda.poll();
        HashSetChartCell bestSpan = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int span = 1; span <= chart.size(); span++) {
            for (int beg = 0; beg < chart.size() - span + 1; beg++) { // beginning
                if (maxEdgeFOM[beg][beg + span] > bestScore) {
                    bestScore = maxEdgeFOM[beg][beg + span];
                    bestSpan = chart.getCell(beg, beg + span);
                }
            }
        }
        return bestSpan;
    }

    protected void addLexicalProductions(final int sent[]) {
        // ChartEdge newEdge;
        HashSetChartCell cell;
        for (int i = 0; i < chart.size(); i++) {
            cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                // newEdge = chart.new ChartEdge(lexProd, chart.getCell(i, i + 1));
                // chart.getCell(i, i + 1).addEdge(newEdge);
                cell.updateInside(lexProd, lexProd.prob);
            }
        }
    }

    public void addUnaryExtensionsToLexProds() {
        for (int i = 0; i < chart.size(); i++) {
            final HashSetChartCell cell = chart.getCell(i, i + 1);
            for (final int pos : cell.getPosNTs()) {
                for (final Production unaryProd : grammar.getUnaryProductionsWithChild(pos)) {
                    // cell.addEdge(unaryProd, cell, null, cell.getBestEdge(pos).inside + unaryProd.prob);
                    cell.updateInside(unaryProd, cell.getInside(pos) + unaryProd.prob);
                }
            }
        }
    }

    // protected void addSpanToFrontier(final ChartCell span) {
    // //System.out.println("AgendaPush: " + edge.spanLength() + " " + edge.inside + " " + edge.fom);
    // if (maxEdgeFOM[span.start()][span.end()] > Float.NEGATIVE_INFINITY) {
    // spanAgenda.remove(span);
    // }
    // spanAgenda.add(span);
    // }

    protected void expandFrontier(final HashSetChartCell cell) {

        // connect edge as possible right non-term
        for (int start = 0; start < cell.start(); start++) {
            setSpanMaxEdgeFOM(chart.getCell(start, cell.start()), cell);
        }

        // connect edge as possible left non-term
        for (int end = cell.end() + 1; end <= chart.size(); end++) {
            setSpanMaxEdgeFOM(cell, chart.getCell(cell.end(), end));
        }
    }

    protected void setSpanMaxEdgeFOM(final HashSetChartCell leftCell, final HashSetChartCell rightCell) {
        ChartEdge edge;
        final int start = leftCell.start(), end = rightCell.end();
        float bestFOM = maxEdgeFOM[start][end];

        // System.out.println(" setSpanMax: " + leftCell + " && " + rightCell);

        Collection<Production> possibleProds;
        for (final int leftNT : leftCell.getLeftChildNTs()) {
            for (final int rightNT : rightCell.getRightChildNTs()) {
                possibleProds = grammar.getBinaryProductionsWithChildren(leftNT, rightNT);
                if (possibleProds != null) {
                    for (final Production p : possibleProds) {
                        // final float prob = p.prob + leftCell.getInside(leftNT) + rightCell.getInside(rightNT);
                        edge = chart.new ChartEdge(p, leftCell, rightCell);
                        // System.out.println(" considering: " + edge);
                        if (edge.fom > bestFOM) {
                            bestFOM = edge.fom;
                        }
                    }
                }
            }
        }

        if (bestFOM > maxEdgeFOM[start][end]) {
            final HashSetChartCell parentCell = chart.getCell(start, end);
            // if (maxEdgeFOM[start][end] > Float.NEGATIVE_INFINITY) {
            // spanAgenda.remove(parentCell);
            // }
            maxEdgeFOM[start][end] = bestFOM;
            parentCell.fom = bestFOM;
            // spanAgenda.add(parentCell);
            // System.out.println(" addingSpan: " + parentCell);
        }
    }

    @Override
    public String getStats() {
        // return " chartEdges=" + nChartEdges + " agendaPush=" + nAgendaPush + " agendaPop=" + nAgendaPop;
        return "";
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
