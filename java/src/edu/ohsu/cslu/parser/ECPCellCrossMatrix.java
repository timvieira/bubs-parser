package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class ECPCellCrossMatrix extends ExhaustiveChartParser {

    final ChildMatrixGrammar childMatrixGrammar;

    public ECPCellCrossMatrix(final ChildMatrixGrammar grammar, final CellSelector spanSelector) {
        super(grammar, spanSelector);
        childMatrixGrammar = grammar;
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                // gramByLeft = grammarByChildMatrix.binaryProdMatrix.get(leftEdge.p.parent);
                final LinkedList<Production>[] gramByLeft = childMatrixGrammar.binaryProdMatrix[leftEdge.prod.parent];
                for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                    // validProductions = gramByLeft.get(rightEdge.p.parent);
                    final List<Production> validProductions = gramByLeft[rightEdge.prod.parent];
                    if (validProductions != null) {
                        for (final Production p : validProductions) {
                            final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                            cell.addEdge(p, leftCell, rightCell, prob);
                        }
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
