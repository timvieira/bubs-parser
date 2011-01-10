package edu.ohsu.cslu.parser.cellselector;

import edu.ohsu.cslu.parser.ChartParser;

public class LeftRightBottomTopTraversal extends CellSelector {

    private short[][] cellIndices;
    private int currentCell = 0;
    private int cells;

    public LeftRightBottomTopTraversal() {
    }

    @Override
    public void initSentence(final ChartParser<?, ?> parser) {
        final int n = parser.chart.size();
        cells = n * (n + 1) / 2;
        if (cellIndices == null || cellIndices.length < cells) {
            cellIndices = new short[cells][2];
        }
        currentCell = 0;
        int i = 0;
        for (short span = 1; span <= n; span++) {
            for (short start = 0; start < n - span + 1; start++) { // beginning
                cellIndices[i++] = new short[] { start, (short) (start + span) };
            }
        }
    }

    @Override
    public short[] next() {
        return cellIndices[currentCell++];
    }

    @Override
    public boolean hasNext() {
        return currentCell < cells;
    }

    @Override
    public void reset() {
        currentCell = 0;
    }
}
