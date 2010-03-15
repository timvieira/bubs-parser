package edu.ohsu.cslu.parser.cellselector;

import java.util.LinkedList;

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.CellChart.ChartCell;

public class LeftRightBottomTopTraversal extends CellSelector {

    private LinkedList<ChartCell> cellList;

    public LeftRightBottomTopTraversal() {
    }

    @Override
    public void init(final ChartParser parser) {
        cellList = new LinkedList<ChartCell>();
        // for (int span = 2; span <= parser.chartSize; span++) {
        for (int span = 1; span <= parser.chart.size(); span++) {
            for (int beg = 0; beg < parser.chart.size() - span + 1; beg++) { // beginning
                cellList.add(parser.chart.getCell(beg, beg + span));
            }
        }
    }

    @Override
    public short[] next() {
        final ChartCell cell = cellList.poll();
        return new short[] { (short) cell.start(), (short) cell.end() };
    }

    @Override
    public boolean hasNext() {
        return cellList.isEmpty() == false;
    }
}
