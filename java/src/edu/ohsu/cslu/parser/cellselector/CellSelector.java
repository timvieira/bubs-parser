package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Iterator;

import edu.ohsu.cslu.parser.ChartParser;

public abstract class CellSelector implements Iterator<short[]> {

    public abstract void initSentence(final ChartParser<?, ?> parser);

    public void train(final BufferedReader inStream) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void readModel(final BufferedReader inStream) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void writeModel(final BufferedWriter outStream) throws Exception {
        throw new UnsupportedOperationException();
    }

    public boolean hasCellConstraints() {
        return false;
    }

    public CellConstraints getCellConstraints() {
        if (hasCellConstraints()) {
            return (CellConstraints) this;
        }
        return null;
    }

    public int getMidStart(final short start, final short end) {
        return start + 1;
    }

    public int getMidEnd(final short start, final short end) {
        return end - 1;
    }

    // iterator operations
    public abstract boolean hasNext();

    public abstract short[] next();

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void reset() {
        throw new UnsupportedOperationException();
    }

    // open to factored and non-factored productions
    // public boolean isOpenAll(final short start, final short end) {
    // return true;
    // }
    //
    // public boolean isOpenOnlyFactored(final short start, final short end) {
    // return false;
    // }
    //
    // public boolean isOpenUnary(final short start, final short end) {
    // return isOpenAll(start, end);
    // }

    /**
     * Returns true if the specified cell is 'open' only to factored parents (i.e., will never be populated with a
     * complete constituent).
     * 
     * @param start
     * @param end
     * @return true if the specified cell is 'open' only to factored parents
     */
    // public boolean factoredParentsOnly(final short start, final short end) {
    // return isOpenOnlyFactored(start, end);
    // }

    public int getCellValue(final short start, final short end) {
        return 1;
    }
}
