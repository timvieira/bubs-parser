package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class ECPGrammarLoop extends ChartParser<Grammar, CellChart> {

    public ECPGrammarLoop(final ParserDriver opts, final Grammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            // naive traversal through all grammar rules
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final Production p : grammar.getBinaryProductions()) {
                final float leftInside = leftCell.getInside(p.leftChild);
                final float rightInside = rightCell.getInside(p.rightChild);
                final float prob = p.prob + leftInside + rightInside;
                if (prob > Float.NEGATIVE_INFINITY) {
                    cell.updateInside(p, leftCell, rightCell, prob);
                }
            }
        }

        for (final int childNT : cell.getNtArray()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                cell.updateInside(chart.new ChartEdge(p, cell));
            }
        }
    }
}
