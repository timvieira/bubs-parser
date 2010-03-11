package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class ECPCellCrossHash extends CellwiseExhaustiveChartParser {

    LeftHashGrammar leftHashGrammar;

    public ECPCellCrossHash(final LeftHashGrammar grammar, final CellSelector cellSelector) {
        super(grammar, cellSelector);
        leftHashGrammar = grammar;
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                    for (final Production p : leftHashGrammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent)) {
                        final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                        cell.addEdge(p, leftCell, rightCell, prob);
                    }
                }
            }
        }

        for (final ChartEdge childEdge : cell.getEdges()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childEdge.prod.parent)) {
                final float prob = p.prob + childEdge.inside;
                cell.addEdge(new ChartEdge(p, cell, prob));
            }
        }
    }
}
