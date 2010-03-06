package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class ECPGrammarLoop extends ExhaustiveChartParser {

    public ECPGrammarLoop(final GrammarByChild grammar, final CellSelector spanSelector) {
        super(grammar, spanSelector);
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            // naive traversal through all grammar rules
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final Production p : grammar.getBinaryProductions()) {
                final ChartEdge leftEdge = leftCell.getBestEdge(p.leftChild);
                final ChartEdge rightEdge = rightCell.getBestEdge(p.rightChild);
                if ((leftEdge != null) && (rightEdge != null)) {
                    final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                    cell.addEdge(p, leftCell, rightCell, prob);
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
