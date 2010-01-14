package edu.ohsu.cslu.parser.traversal;

import java.util.LinkedList;

import edu.ohsu.cslu.parser.ArrayChartCell;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.util.Log;

public class BlockedCellByPOSTraversal extends ChartTraversal {

    private LinkedList<ArrayChartCell> cellList;

    public BlockedCellByPOSTraversal(final ChartParser parser) {
        // cellList = new LinkedList<ArrayChartCell>();
        // build list of traversable cells based on (Kristy's?) POS tagger and cell classifier
        Log.info(0, "ERROR: BlockedCellByPOSTraversal() not implemented.");
        System.exit(1);
    }

    @Override
    public ArrayChartCell next() {
        return cellList.poll();
    }

    @Override
    public boolean hasNext() {
        // return parser.rootChartCell.getBestEdge(parser.grammar.startSymbol) == null;
        return !cellList.isEmpty();
    }
}
