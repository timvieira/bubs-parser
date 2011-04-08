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

import java.util.LinkedList;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

public class LeftCornerTraversal extends CellSelector {

    private LinkedList<ChartCell> cellList;

    public static CellSelectorFactory FACTORY = new CellSelectorFactory() {
        @Override
        public CellSelector createCellSelector() {
            return new LeftCornerTraversal();
        }
    };

    public LeftCornerTraversal() {
    }

    @Override
    public void initSentence(final ChartParser<?, ?> parser) {
        // cellList = new LinkedList<ArrayChartCell>();
        /*
         * for (int span=2; span<=this.parser.chartSize; span++) { for (int beg=0; beg<this.parser.chartSize-span+1;
         * beg++) { // beginning cellList.add(parser.chart[beg][beg+span]); } }
         */
        BaseLogger.singleton().info("ERROR: LeftCornerTraversal() not implemented.");
        System.exit(1);

    }

    @Override
    public short[] next() {
        final ChartCell cell = cellList.poll();
        return new short[] { cell.start(), cell.end() };
    }

    @Override
    public boolean hasNext() {
        return !cellList.isEmpty();
    }
}
