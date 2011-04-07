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

public abstract class CellConstraints extends CellSelector {

    protected abstract boolean isGrammarLeftFactored();

    public abstract boolean isCellOpen(final short start, final short end);

    public abstract boolean isCellOnlyFactored(short start, short end);

    public abstract boolean isUnaryOpen(short start, short end);

    public boolean isCellClosed(final short start, final short end) {
        return !isCellOpen(start, end);
    }

    public boolean isUnaryClosed(final short start, final short end) {
        return !isUnaryOpen(start, end);
    }

    @Override
    public boolean hasCellConstraints() {
        return true;
    }

    @Override
    public int getMidStart(final short start, final short end) {
        if ((end - start) < 2 || !isCellOnlyFactored(start, end) || isGrammarLeftFactored())
            return start + 1;
        return end - 1; // only allow one midpoint
    }

    @Override
    public int getMidEnd(final short start, final short end) {
        if ((end - start) < 2 || !isCellOnlyFactored(start, end) || !isGrammarLeftFactored())
            return end - 1;
        return start + 1; // only allow one midpoint
    }

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

}
