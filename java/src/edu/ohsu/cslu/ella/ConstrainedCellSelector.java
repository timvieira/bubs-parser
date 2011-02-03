package edu.ohsu.cslu.ella;

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

/**
 * {@link CellSelector} implementation which constrains parsing according to a gold tree represented in a
 * {@link ConstrainedChart}.
 * 
 * @author Aaron Dunlop
 * @since Jan 15, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ConstrainedCellSelector extends CellSelector {

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
        currentCell = 0;
    }

    /**
     * @return The midpoint in the current cell (when parsing is constrained by the gold bracketing, each cell can
     *         contain only a single midpoint)
     */
    public short currentCellMidpoint() {
        return constrainingChart.midpoints[constrainingChart.cellIndex(cellIndices[currentCell][0],
                cellIndices[currentCell][1])];
    }

    /**
     * @return The midpoint in the current cell (when parsing is constrained by the gold bracketing, each cell can
     *         contain only a single midpoint)
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
}
