package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ChartParser<G extends Grammar, C extends Chart> extends Parser<G> {

    public C chart;

    public ChartParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {

        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);

        initParser(sent.length);
        addLexicalProductions(sent);
        cellSelector.init(this);

        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            visitCell(startAndEnd[0], startAndEnd[1]);
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
    protected void initParser(final int sentLength) {
        chart = (C) new CellChart(sentLength, opts.viterbiMax, this);
    }

    protected void addLexicalProductions(final int sent[]) throws Exception {
        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            final ChartCell cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
            }
            cell.finalizeCell();
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
        return chart.getStats();
    }
}
