package edu.ohsu.cslu.parser.traversal;

import java.util.Iterator;

import edu.ohsu.cslu.parser.ArrayChartCell;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.util.Log;

public abstract class ChartTraversal implements Iterator<ArrayChartCell> {

	static public enum ChartTraversalType {
		LeftRightBottomTopTraversal, LeftCornerTraversal, BlockedCellByPOSTraversal, HeuristicTraversal
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public static ChartTraversal create(final ChartTraversalType type, final ChartParser parser) {
		switch (type) {
		case LeftRightBottomTopTraversal:
			return new LeftRightBottomTopTraversal(parser);
		case LeftCornerTraversal:
			return new LeftCornerTraversal(parser);
		case BlockedCellByPOSTraversal:
			return new BlockedCellByPOSTraversal(parser);
		default:
			Log.info(0, "ERROR: ChartTraversalType " + type + " not supported.");
			System.exit(1);
			return null;
		}
	}
}
