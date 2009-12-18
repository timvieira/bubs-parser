package edu.ohsu.cslu.parser.traversal;

import java.util.LinkedList;

import edu.ohsu.cslu.parser.ChartCell;
import edu.ohsu.cslu.parser.ChartParser;

public class LeftRightBottomTopTraversal extends ChartTraversal {

    private LinkedList<ChartCell> cellList;

    public LeftRightBottomTopTraversal(ChartParser parser) {

        cellList = new LinkedList<ChartCell>();
        for (int span = 2; span <= parser.chartSize; span++) {
            for (int beg = 0; beg < parser.chartSize - span + 1; beg++) { // beginning
                cellList.add(parser.chart[beg][beg + span]);
            }
        }
    }

    @Override
    public ChartCell next() {
        return cellList.pollFirst();
    }

    @Override
    public boolean hasNext() {
        return cellList.isEmpty() == false;
    }
}
