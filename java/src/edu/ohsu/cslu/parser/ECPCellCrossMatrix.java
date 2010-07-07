package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class ECPCellCrossMatrix extends CellwiseExhaustiveChartParser<ChildMatrixGrammar, CellChart> {

    public ECPCellCrossMatrix(final ParserOptions opts, final ChildMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final HashSetChartCell cell) {
        final int start = cell.start(), end = cell.end();

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                // gramByLeft = grammarByChildMatrix.binaryProdMatrix.get(leftEdge.p.parent);
                final LinkedList<Production>[] gramByLeft = grammar.binaryProdMatrix[leftNT];
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    // validProductions = gramByLeft.get(rightEdge.p.parent);
                    final List<Production> validProductions = gramByLeft[rightNT];
                    if (validProductions != null) {
                        for (final Production p : validProductions) {
                            final float prob = p.prob + leftCell.getInside(leftNT)
                                    + rightCell.getInside(rightNT);
                            cell.updateInside(p, leftCell, rightCell, prob);
                        }
                    }
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
