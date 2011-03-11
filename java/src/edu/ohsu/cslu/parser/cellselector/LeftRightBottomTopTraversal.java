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

    public static CellSelectorFactory FACTORY = new CellSelectorFactory() {
        @Override
        public CellSelector createCellSelector() {
            return new LeftRightBottomTopTraversal();
        }
    };

    public LeftRightBottomTopTraversal() {
    }

    @Override
    public void initSentence(final ChartParser<?, ?> parser) {
        final int n = parser.chart.size();
        cells = n * (n + 1) / 2;
        if (cellIndices == null || cellIndices.length < cells) {
            cellIndices = new short[cells][2];
        }
        nextCell = 0;
        int i = 0;
        for (short span = 1; span <= n; span++) {
            for (short start = 0; start < n - span + 1; start++) { // beginning
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
        return nextCell < cells;
    }

    @Override
    public void reset() {
        nextCell = 0;
    }
}
