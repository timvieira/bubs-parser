package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class ECPCellCrossList extends CellwiseExhaustiveChartParser<LeftListGrammar, CellChart> {

	public ECPCellCrossList(final ParserDriver opts, final LeftListGrammar grammar) {
		super(opts, grammar);
	}

	@Override
	protected void visitCell(final HashSetChartCell cell) {
		final int start = cell.start(), end = cell.end();
		float leftInside, rightInside;

		for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
			final HashSetChartCell leftCell = chart.getCell(start, mid);
			final HashSetChartCell rightCell = chart.getCell(mid, end);
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

		for (final int childNT : cell.getNtArray()) {
			for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
				cell.updateInside(p, p.prob + cell.getInside(childNT));
			}
		}
	}
}
