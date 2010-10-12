package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartEdge;

public class ECPCellCrossMatrix extends ChartParser<ChildMatrixGrammar, CellChart> {

    public ECPCellCrossMatrix(final ParserDriver opts, final ChildMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                // gramByLeft = GrammarMatrix.binaryProdMatrix.get(leftEdge.p.parent);
                final LinkedList<Production>[] gramByLeft = grammar.binaryProdMatrix[leftNT];
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    // validProductions = gramByLeft.get(rightEdge.p.parent);
                    final List<Production> validProductions = gramByLeft[rightNT];
                    if (validProductions != null) {
                        for (final Production p : validProductions) {
                            final float prob = p.prob + leftCell.getInside(leftNT) + rightCell.getInside(rightNT);
                            cell.updateInside(p, leftCell, rightCell, prob);
                        }
                    }
                }
            }
        }

        for (final int childNT : cell.getNtArray()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                cell.updateInside(new ChartEdge(p, cell));
            }
        }
    }
}
