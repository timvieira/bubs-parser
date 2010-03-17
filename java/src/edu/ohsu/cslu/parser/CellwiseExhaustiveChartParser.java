package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.parser.CellChart.ChartCell;

public abstract class CellwiseExhaustiveChartParser<G extends GrammarByChild, C extends CellChart> extends ExhaustiveChartParser<G, C> {

    public CellwiseExhaustiveChartParser(final ParserOptions opts, final G grammar) {
        super(opts, grammar);
    }

    /**
     * Each subclass will implement this method to perform the inner-loop grammar intersection.
     * 
     * @param cell
     */
    protected abstract void visitCell(ChartCell cell);

    @Override
    protected void visitCell(final short start, final short end) {
        visitCell(chart.getCell(start, end));
    }
}
