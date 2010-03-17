package edu.ohsu.cslu.parser;

import com.aliasi.util.Collections;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartCell;

public class ECPCellCrossHash extends CellwiseExhaustiveChartParser<LeftHashGrammar, CellChart> {

    public ECPCellCrossHash(final ParserOptions opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();
        float insideProb;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
                        insideProb = p.prob + leftCell.getInside(leftNT) + rightCell.getInside(rightNT);
                        cell.updateInside(p, leftCell, rightCell, insideProb);
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
