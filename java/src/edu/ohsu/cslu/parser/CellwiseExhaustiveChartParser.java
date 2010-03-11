package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public abstract class CellwiseExhaustiveChartParser extends ExhaustiveChartParser {

    public CellwiseExhaustiveChartParser(final GrammarByChild grammar, final CellSelector cellSelector) {
        super(grammar, cellSelector);
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
