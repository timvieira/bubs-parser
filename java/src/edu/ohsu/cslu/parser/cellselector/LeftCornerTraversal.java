package edu.ohsu.cslu.parser.cellselector;

import java.util.LinkedList;

import edu.ohsu.cslu.parser.ArrayChartCell;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.util.Log;

public class LeftCornerTraversal extends CellSelector {

    private LinkedList<ArrayChartCell> cellList;

    public LeftCornerTraversal() {
    }

    @Override
    public void init(final ChartParser parser) {
        // cellList = new LinkedList<ArrayChartCell>();
        /*
         * for (int span=2; span<=this.parser.chartSize; span++) { for (int beg=0; beg<this.parser.chartSize-span+1; beg++) { // beginning
         * cellList.add(parser.chart[beg][beg+span]); } }
         */
        Log.info(0, "ERROR: LeftCornerTraversal() not implemented.");
        System.exit(1);

    }

    @Override
    public ArrayChartCell next() {
        return cellList.pollFirst();
    }

    @Override
    public boolean hasNext() {
        return !cellList.isEmpty();
    }
}
