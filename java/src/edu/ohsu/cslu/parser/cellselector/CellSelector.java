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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import edu.ohsu.cslu.parser.ChartParser;

public abstract class CellSelector implements Iterator<short[]> {

    public abstract void initSentence(final ChartParser<?, ?> parser);

    public void train(final BufferedReader inStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void writeModel(final BufferedWriter outStream) throws IOException {
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

    /**
     * Returns true if the cell selector has more cells available. The parser should call {@link #hasNext()}
     * until it returns <code>false</code> to ensure the sentence is fully parsed.
     * 
     * @return true if the cell selector has more cells.
     */
    @Override
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
     * Returns true if the specified cell is 'open' only to factored parents (i.e., will never be populated
     * with a complete constituent).
     * 
     * @param start
     * @param end
     * @return true if the specified cell is 'open' only to factored parents
     */
    // public boolean factoredParentsOnly(final short start, final short end) {
    // return isOpenOnlyFactored(start, end);
    // }

    /**
     * Returns the beam width for the current cell. Consumers generally set the cell beam width to
     * java.lang.Math.min(getCelValue(), beamWidth), so they will not attempt to search a range larger than
     * the maximum beam width of the parser.
     * 
     * TODO The naming and interface still aren't great.
     */
    public int getBeamWidth(final short start, final short end) {
        return Integer.MAX_VALUE;
    }
}
