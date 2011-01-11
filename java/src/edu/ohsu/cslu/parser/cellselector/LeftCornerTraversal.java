package edu.ohsu.cslu.parser.cellselector;

import java.util.LinkedList;

import cltool4j.GlobalLogger;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

public class LeftCornerTraversal extends CellSelector {

    private LinkedList<ChartCell> cellList;

    public LeftCornerTraversal() {
    }

    @Override
    public void initSentence(final ChartParser<?, ?> parser) {
        // cellList = new LinkedList<ArrayChartCell>();
        /*
         * for (int span=2; span<=this.parser.chartSize; span++) { for (int beg=0; beg<this.parser.chartSize-span+1;
         * beg++) { // beginning cellList.add(parser.chart[beg][beg+span]); } }
         */
        GlobalLogger.singleton().info("ERROR: LeftCornerTraversal() not implemented.");
        System.exit(1);

    }

    @Override
    public short[] next() {
        final ChartCell cell = cellList.poll();
        return new short[] { (short) cell.start(), (short) cell.end() };
    }

    @Override
    public boolean hasNext() {
        return !cellList.isEmpty();
    }
}
