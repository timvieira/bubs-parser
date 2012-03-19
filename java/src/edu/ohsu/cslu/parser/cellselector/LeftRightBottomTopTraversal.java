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

import edu.ohsu.cslu.parser.ChartParser;

/**
 * Traverses each chart row sequentially from from left to right, beginning at the bottom (span-1) row and working
 * upward through each row in order.
 * 
 * @author Nathan Bodenstab
 * @since Dec 17, 2009
 */
public class LeftRightBottomTopTraversal extends CellSelector {

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
        super.initSentence(p);
        final short sentenceLength = (short) p.chart.size();

        openCells = sentenceLength * (sentenceLength + 1) / 2;
        if (cellIndices == null || cellIndices.length < openCells * 2) {
            cellIndices = new short[openCells * 2];
        }

        int i = 0;
        for (short span = 1; span <= sentenceLength; span++) {
            for (short start = 0; start < sentenceLength - span + 1; start++) { // beginning
                cellIndices[i++] = start;
                cellIndices[i++] = (short) (start + span);
            }
        }
    }
}
