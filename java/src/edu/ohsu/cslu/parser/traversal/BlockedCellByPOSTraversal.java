package edu.ohsu.cslu.parser.traversal;

import java.util.LinkedList;

import edu.ohsu.cslu.parser.ArrayChartCell;
import edu.ohsu.cslu.parser.ChartParser;

public class BlockedCellByPOSTraversal extends ChartTraversal {

    private LinkedList<ArrayChartCell> cellList;

    public BlockedCellByPOSTraversal(ChartParser parser) {
        cellList = new LinkedList<ArrayChartCell>();
        // build list of traversable cells based on (Kristy's?) POS tagger and cell classifier
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
