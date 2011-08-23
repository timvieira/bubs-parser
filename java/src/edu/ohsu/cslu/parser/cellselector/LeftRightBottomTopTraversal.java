/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.cellselector;

import java.util.Iterator;

import edu.ohsu.cslu.parser.ChartParser;

/**
 * Traverses each chart row sequentially from from left to right, beginning at the bottom (span-1) row and working
 * upward through each row in order.
 * 
 * @author Nathan Bodenstab
 * @since Dec 17, 2009
 */
public class LeftRightBottomTopTraversal extends CellSelector {

    private short[][] cellIndices;
    private int nextCell = 0;
    private int cells;
    private short sentenceLength;

    // TODO: If we really need a parser instance to call parser.waitForActiveTasks(), then
    // this should be passed in when the model is created, not at each sentence init.
    private ChartParser<?, ?> parser;

    public static CellSelectorModel MODEL = new CellSelectorModel() {

        @Override
        public CellSelector createCellSelector() {
            return new LeftRightBottomTopTraversal();
        }
    };

    public LeftRightBottomTopTraversal() {
    }

    // TODO: shouldn't all of this move into the constructor since we create a new one for each sentence?
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
        // In left-to-right and bottom-to-top traversal, each row depends on the row below. Wait for active
        // tasks (if any) before proceeding on to the next row and before returning false when parsing is complete.
        if (nextCell >= 1) {
            if (nextCell >= cells) {
                parser.waitForActiveTasks();
                return false;
            }
            final int nextSpan = cellIndices[nextCell][1] - cellIndices[nextCell][0];
            final int currentSpan = cellIndices[nextCell - 1][1] - cellIndices[nextCell - 1][0];
            if (nextSpan > currentSpan) {
                parser.waitForActiveTasks();
            }
        }

        return nextCell < cells;
    }

    @Override
    public void reset() {
        nextCell = 0;
    }

    @Override
    public Iterator<short[]> reverseIterator() {
        return new Iterator<short[]>() {

            private int nextCell = cells;

            @Override
            public boolean hasNext() {
                return nextCell > 0;
            }

            @Override
            public short[] next() {
                return cellIndices[--nextCell];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
