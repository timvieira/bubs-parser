package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Production;
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

        final int midStart = cellSelector.getMidStart(start, end);
        final int midEnd = cellSelector.getMidEnd(start, end);
        final boolean onlyFactored = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end);

        // System.out.println("start=" + start + " end=" + end + " midS=" + midStart + " midE=" + midEnd);
        for (int mid = midStart; mid <= midEnd; mid++) { // mid point
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

        int nUnaryConsidered = 0, nUnaryInCell = 0;
        if (cellSelector.hasCellConstraints() == false || cellSelector.getCellConstraints().isUnaryOpen(start, end)) {
            for (final int childNT : cell.getNtArray()) {
                for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                    if (!cell.hasNT(p.parent))
                        nUnaryInCell++;

                    cell.updateInside(p, p.prob + cell.getInside(childNT));
                    nUnaryConsidered++;
                    currentInput.totalConsidered++;
                    if (end - start == 1) {
                        currentInput.nLexUnaryConsidered++;
                    }
                }
            }
        }

        // logger.finest("STAT: UNARY: " + currentInput.sentenceLength + " " + (end - start) + " " + nUnaryConsidered +
        // " "+ nUnaryInCell);
    }
}
