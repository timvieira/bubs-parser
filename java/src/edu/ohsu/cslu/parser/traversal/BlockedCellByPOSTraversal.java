package edu.ohsu.cslu.parser.traversal;

import java.util.LinkedList;

import edu.ohsu.cslu.parser.ChartCell;
import edu.ohsu.cslu.parser.ChartParser;

public class BlockedCellByPOSTraversal extends ChartTraversal {

	private LinkedList<ChartCell> cellList;
	
	public BlockedCellByPOSTraversal(ChartParser parser) {
		cellList = new LinkedList<ChartCell>();
		// build list of traversable cells based on (Kristy's?) POS tagger and cell classifier
	}
	
	@Override
	public ChartCell next() {
		return cellList.poll();
	}

	@Override
	public boolean hasNext() {
		//return parser.rootChartCell.getBestEdge(parser.grammar.startSymbol) == null;
		return !cellList.isEmpty();
	}
}
