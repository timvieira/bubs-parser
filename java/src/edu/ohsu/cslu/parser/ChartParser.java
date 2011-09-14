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
package edu.ohsu.cslu.parser;

import java.util.logging.Level;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

public abstract class ChartParser<G extends Grammar, C extends Chart> extends Parser<G> {

    public C chart;

    public ChartParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {
        initChart(parseTask);

        insidePass();
        if (BaseLogger.singleton().isLoggable(Level.FINEST)) {
            BaseLogger.singleton().finest(chart.toString());
        }
        return extract();
    }

    /**
     * Initialize the chart structure, edge selector, and cell selector
     * 
     * @param tokens
     */
    protected void initChart(final ParseTask parseTask) {
        if (collectDetailedStatistics) {
            final long t0 = System.currentTimeMillis();
            initSentence(parseTask);
            parseTask.chartInitMs = System.currentTimeMillis() - t0;
        } else {
            initSentence(parseTask);
        }

        if (fomModel != null) {
            if (collectDetailedStatistics) {
                final long t1 = System.currentTimeMillis();
                fomModel.init(parseTask, chart);
                parseTask.fomInitMs = System.currentTimeMillis() - t1;
            } else {
                fomModel.init(parseTask, chart);
            }
        }

        if (collectDetailedStatistics) {
            final long t2 = System.currentTimeMillis();
            cellSelector.initSentence(this);
            parseTask.ccInitMs = System.currentTimeMillis() - t2;
        } else {
            cellSelector.initSentence(this);
        }
    }

    @SuppressWarnings("unchecked")
    protected void initSentence(final ParseTask parseTask) {
        chart = (C) new CellChart(parseTask, this);
    }

    protected void addLexicalProductions(final int start) {
        // add lexical productions to the a base cells of the chart
        final ChartCell cell = chart.getCell(start, start + 1);
        for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.parseTask.tokens[start])) {
            cell.updateInside(lexProd, cell, null, lexProd.prob);
        }
    }

    /**
     * Executes the inside / viterbi parsing pass
     */
    protected final void insidePass() {
        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            if (startAndEnd[1] - startAndEnd[0] == 1) {
                addLexicalProductions(startAndEnd[0]);
            }
            computeInsideProbabilities(startAndEnd[0], startAndEnd[1]);
        }
    }

    protected final BinaryTree<String> extract() {
        if (collectDetailedStatistics) {
            final long t3 = System.currentTimeMillis();
            final BinaryTree<String> parseTree = chart.extractBestParse(grammar.startSymbol);
            chart.parseTask.extractTimeMs = System.currentTimeMillis() - t3;
            return parseTree;
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    /**
     * Each subclass will implement this method to perform the inner-loop grammar intersection.
     * 
     * @param start
     * @param end
     */
    protected abstract void computeInsideProbabilities(short start, short end);

    @Override
    public float getInside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @Override
    public float getOutside(final int start, final int end, final int nt) {
        return chart.getOutside(start, end, nt);
    }

    @Override
    public String getStats() {
        return chart.getStats();
    }
}
