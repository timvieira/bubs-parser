package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;

import com.aliasi.util.Collections;

import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.CellChart.ChartCell;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class ECPCellCrossMatrix extends CellwiseExhaustiveChartParser<ChildMatrixGrammar, CellChart> {

    public ECPCellCrossMatrix(final ChildMatrixGrammar grammar, final CellSelector spanSelector) {
        super(grammar, spanSelector);
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                // gramByLeft = grammarByChildMatrix.binaryProdMatrix.get(leftEdge.p.parent);
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

        for (final int childNT : Collections.toIntArray(cell.getNTs())) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                cell.updateInside(chart.new ChartEdge(p, cell));
            }
        }
    }
}
