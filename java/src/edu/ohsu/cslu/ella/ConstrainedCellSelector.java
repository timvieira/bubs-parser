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

    @Override
    public void initSentence(final ChartParser<?, ?> parser) {
        final ConstrainedCsrSpmvParser constrainedParser = (ConstrainedCsrSpmvParser) parser;
        this.cellIndices = constrainedParser.constrainingChart.openCells;
        currentCell = 0;
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
        return currentCell < cellIndices.length;
    }

    @Override
    public void reset() {
        currentCell = 0;
    }

    public short midpoint() {
        // TODO Implement
        return 0;
    }

    public short[] currentParents() {
        // TODO Implement
        return new short[0];
    }

}
