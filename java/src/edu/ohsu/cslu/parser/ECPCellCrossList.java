package edu.ohsu.cslu.parser;

import com.aliasi.util.Collections;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.CellChart.ChartCell;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class ECPCellCrossList extends CellwiseExhaustiveChartParser<LeftListGrammar, CellChart> {

    public ECPCellCrossList(final LeftListGrammar grammar, final CellSelector spanSelection) {
        super(grammar, spanSelection);
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();
        float leftInside, rightInside;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                leftInside = leftCell.getInside(leftNT);
                for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                    rightInside = rightCell.getInside(p.rightChild);
                    if (rightInside > Float.NEGATIVE_INFINITY) {
                        cell.updateInside(p, leftCell, rightCell, p.prob + leftInside + rightInside);
                    }
                }
            }
        }

        for (final int childNT : Collections.toIntArray(cell.getNTs())) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                cell.updateInside(p, p.prob + cell.getInside(childNT));
            }
        }
    }
}
