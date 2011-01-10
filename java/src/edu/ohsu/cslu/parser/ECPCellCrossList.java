package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class ECPCellCrossList extends ChartParser<LeftListGrammar, CellChart> {

    public ECPCellCrossList(final ParserDriver opts, final LeftListGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);
        float leftInside, rightInside;

        // final boolean onlyFactored = (hasCellConstraints && cellConstraints.factoredParentsOnly(start, end));
        final boolean onlyFactored = cellSelector.isOpenOnlyFactored(start, end);

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                leftInside = leftCell.getInside(leftNT);
                for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                    if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
                        rightInside = rightCell.getInside(p.rightChild);
                        if (rightInside > Float.NEGATIVE_INFINITY) {
                            cell.updateInside(p, leftCell, rightCell, p.prob + leftInside + rightInside);
                            currentInput.totalConsidered++;
                        }
                    }
                }
            }
        }

        // if (!hasCellConstraints || cellConstraints.unaryOpen(start, end)) {
        if (cellSelector.isOpenUnary(start, end)) {
            for (final int childNT : cell.getNtArray()) {
                for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                    cell.updateInside(p, p.prob + cell.getInside(childNT));
                    currentInput.totalConsidered++;
                }
            }
        }
    }
}
