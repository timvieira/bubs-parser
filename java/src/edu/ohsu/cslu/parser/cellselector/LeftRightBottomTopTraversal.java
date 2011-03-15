package edu.ohsu.cslu.parser.cellselector;

import edu.ohsu.cslu.parser.ChartParser;

/**
 * Traverses each chart row sequentially from from left to right, beginning at the bottom (span-1) row and working
 * upward through each row in order.
 * 
 * @author Nathan Bodenstab
 * @since Dec 17, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class LeftRightBottomTopTraversal extends CellSelector {

    private short[][] cellIndices;
    private int nextCell = 0;
    private int cells;

    /** The size of the sentences */
    private short sentenceLength;

    private ChartParser<?, ?> parser;

    public static CellSelectorFactory FACTORY = new CellSelectorFactory() {
        @Override
        public CellSelector createCellSelector() {
            return new LeftRightBottomTopTraversal();
        }
    };

    public LeftRightBottomTopTraversal() {
    }

    @Override
    public void initSentence(final ChartParser<?, ?> p) {
        this.parser = p;
        sentenceLength = (short) p.chart.size();
        cells = sentenceLength * (sentenceLength + 1) / 2;
        if (cellIndices == null || cellIndices.length < cells) {
            cellIndices = new short[cells][2];
        }
        nextCell = 0;
        int i = 0;
        for (short span = 1; span <= sentenceLength; span++) {
            for (short start = 0; start < sentenceLength - span + 1; start++) { // beginning
                cellIndices[i++] = new short[] { start, (short) (start + span) };
            }
        }
    }

    @Override
    public short[] next() {
        return cellIndices[nextCell++];
    }

    @Override
    public boolean hasNext() {
        // In left-to-right and bottom-to-top traversal, each row depends on the row below. Wait for active tasks (if
        // any) before proceeding on to the next row and before returning false when parsing is complete.
        if (nextCell >= 1 && cellIndices[nextCell - 1][1] == sentenceLength) {
            parser.waitForActiveTasks();
        }
        return nextCell < cells;
    }

    @Override
    public void reset() {
        nextCell = 0;
    }
}
