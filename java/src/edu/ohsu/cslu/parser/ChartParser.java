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
import edu.ohsu.cslu.parser.chart.Chart.RecoveryStrategy;

public abstract class ChartParser<G extends Grammar, C extends Chart> extends Parser<G> {

    public C chart;

    public ChartParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {
        initChart(parseTask);

        insidePass();
        if (BaseLogger.singleton().isLoggable(Level.ALL)) {
            BaseLogger.singleton().finest(chart.toString());
        }
        return extract(parseTask.recoveryStrategy);
    }

    @Override
    public ParseTask parseSentence(final String input, final RecoveryStrategy recoveryStrategy) {
        final ParseTask task = super.parseSentence(input, recoveryStrategy);
        if (task.binaryParse == null && recoveryStrategy != null) {
            task.recoveryParse = chart.extractRecoveryParse(recoveryStrategy);
        }
        return task;
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
                fomModel.initSentence(parseTask, chart);
                parseTask.fomInitMs = System.currentTimeMillis() - t1;
            } else {
                fomModel.initSentence(parseTask, chart);
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

    protected void addLexicalProductions(final ChartCell cell) {
        // add lexical productions to the a base cells of the chart
        if (ParserDriver.parseFromInputTags) {
            // add only one POS => word production given by input (or 1-best) tags
            final int posTag = chart.parseTask.inputTags[cell.start()];
            final int word = chart.parseTask.tokens[cell.start()];
            Production lexProd = grammar.getLexicalProduction(posTag, word);
            if (lexProd == null) {
                // TODO: create a new lexical production with a smoothed prob, maybe from the UNK classes
                // final int parent, final int child, final float prob, final boolean isLex, final Grammar grammar
                lexProd = new Production(posTag, word, 0.0f, true, grammar);
                // throw new IllegalArgumentException(String.format(
                // "ERROR: lexical production %s => %s not found in grammar",
                // grammar.nonTermSet.getSymbol(chart.parseTask.inputTags[cell.start()]),
                // grammar.lexSet.getSymbol(chart.parseTask.tokens[cell.start()])));
            }
            cell.updateInside(lexProd, cell, null, lexProd.prob);
        } else {
            // add all possible lexical productions given input word
            for (final Production lexProd : grammar
                    .getLexicalProductionsWithChild(chart.parseTask.tokens[cell.start()])) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
            }
        }
    }

    /**
     * Executes the inside / viterbi parsing pass
     */
    protected void insidePass() {
        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            final ChartCell cell = chart.getCell(startAndEnd[0], startAndEnd[1]);
            if (startAndEnd[1] - startAndEnd[0] == 1) {
                addLexicalProductions(cell);
            }
            computeInsideProbabilities(cell);
        }
    }

    protected final BinaryTree<String> extract(final RecoveryStrategy recoveryStrategy) {
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
     * @param cell The cell to populate
     */
    protected abstract void computeInsideProbabilities(ChartCell cell);

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
        // return chart.getStats();
        return "";
    }
}
