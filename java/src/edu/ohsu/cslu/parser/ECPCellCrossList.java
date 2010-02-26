package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class ECPCellCrossList extends ExhaustiveChartParser {

    private LeftListGrammar leftListGrammar;

    public ECPCellCrossList(final LeftListGrammar grammar, final CellSelector spanSelection) {
        super(grammar, spanSelection);
        leftListGrammar = grammar;
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                for (final Production p : leftListGrammar.getBinaryProductionsWithLeftChild(leftEdge.prod.parent)) {
                    final ChartEdge rightEdge = rightCell.getBestEdge(p.rightChild);
                    if (rightEdge != null) {
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
