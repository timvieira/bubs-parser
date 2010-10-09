package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

public abstract class ChartParser<G extends Grammar, C extends Chart> extends Parser<G> {

    public C chart;
    protected long extractTime;
    protected long initTime;
    protected long edgeSelectorInitTime;

    public ChartParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) throws Exception {
        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;
        initParser(tokens);
        addLexicalProductions(tokens);
        cellSelector.init(this);
        if (edgeSelector != null) {
            if (collectDetailedStatistics) {
                final long t1 = System.currentTimeMillis();
                initTime = t1 - t0;
                edgeSelector.init(chart);
                edgeSelectorInitTime = System.currentTimeMillis() - t1;
            } else {
                edgeSelector.init(chart);
            }
        }

        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            visitCell(startAndEnd[0], startAndEnd[1]);
        }

        if (collectDetailedStatistics) {
            final long t2 = System.currentTimeMillis();
            final ParseTree parseTree = chart.extractBestParse(grammar.startSymbol);
            extractTime = System.currentTimeMillis() - t2;
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
    protected abstract void visitCell(short start, short end);

    @SuppressWarnings("unchecked")
    protected void initParser(final int[] tokens) {
        chart = (C) new CellChart(tokens, opts.viterbiMax(), this);
    }

    protected void addLexicalProductions(final int sent[]) {
        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            final ChartCell cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
            }
        }
    }

    @Override
    public float getInside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @Override
    public float getOutside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @Override
    public String getStats() {
        return chart.getStats() + (collectDetailedStatistics ? (" edgeInitTime=" + initTime) : "");
    }
}
