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
package edu.ohsu.cslu.lela;

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelectorModel;

/**
 * {@link CellSelector} implementation which constrains parsing according to a gold tree represented in a
 * {@link ConstrainedChart}.
 * 
 * @author Aaron Dunlop
 * @since Jan 15, 2011
 */
public class ConstrainedCellSelector extends CellSelector {

    public static CellSelectorModel MODEL = new CellSelectorModel() {

        @Override
        public CellSelector createCellSelector() {
            return new ConstrainedCellSelector();
        }
    };

    private short[][] cellIndices;
    private int currentCell = -1;
    private ConstrainedChart constrainingChart;

    @Override
    public void initSentence(final ChartParser<?, ?> parser) {
        final ConstrainedCsrSpmvParser constrainedParser = (ConstrainedCsrSpmvParser) parser;
        this.constrainingChart = constrainedParser.constrainingChart;
        this.cellIndices = constrainingChart.openCells;
        currentCell = -1;
    }

    // @Override
    // public boolean isOpenAll(final short start, final short end) {
    // // TODO Auto-generated method stub
    // return super.isOpenAll(start, end);
    // }
    //
    // @Override
    // public boolean isOpenOnlyFactored(final short start, final short end) {
    // // TODO Auto-generated method stub
    // return super.isOpenOnlyFactored(start, end);
    // }

    @Override
    public short[] next() {
        return cellIndices[++currentCell];
    }

    @Override
    public boolean hasNext() {
        return currentCell < cellIndices.length - 1;
    }

    @Override
    public void reset() {
        currentCell = -1;
    }

    /**
     * @return The midpoint in the current cell (when parsing is constrained by the gold bracketing, each cell
     *         can contain only a single midpoint)
     */
    public short currentCellMidpoint() {
        return constrainingChart.midpoints[constrainingChart.cellIndex(cellIndices[currentCell][0],
            cellIndices[currentCell][1])];
    }

    /**
     * @return The midpoint in the current cell (when parsing is constrained by the gold bracketing, each cell
     *         can contain only a single midpoint)
     */
    public int currentCellUnaryChainDepth() {
        return constrainingChart.unaryChainDepth(constrainingChart.offset(constrainingChart.cellIndex(
            cellIndices[currentCell][0], cellIndices[currentCell][1])));
    }

    /**
     * @return All non-terminals populated in the current cell of the constraining chart
     */
    public short[] constrainingChartNonTerminalIndices() {
        return constrainingChart.nonTerminalIndices;
    }

    /**
     * @return All children populated in the current cell of the constraining chart
     */
    public int[] constrainingChartPackedChildren() {
        return constrainingChart.packedChildren;
    }

    /**
     * @return Offset of the current cell in the constraining chart
     */
    public int constrainingCellOffset() {
        return constrainingChart.cellOffsets[constrainingChart.cellIndex(cellIndices[currentCell][0],
            cellIndices[currentCell][1])];
    }

    /**
     * @return Offset of the current cell's left child in the constraining chart
     */
    public int constrainingLeftChildCellOffset() {
        return constrainingChart.cellOffsets[constrainingChart.cellIndex(cellIndices[currentCell][0],
            currentCellMidpoint())];
    }
}
