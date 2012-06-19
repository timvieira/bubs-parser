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

import edu.ohsu.cslu.datastructs.vectors.DenseIntVector;

public abstract class CellConstraints extends CellSelector {

    protected abstract boolean isGrammarLeftFactored();

    protected DenseIntVector maxSpan = null;

    /**
     * Returns true if the specified cell is 'open'.
     * 
     * @param start
     * @param end
     * @return true if the specified cell is 'open'
     */
    public abstract boolean isCellOpen(final short start, final short end);

    /**
     * Returns true if the specified cell is 'open' only to factored parents (i.e., will never be populated with a
     * complete constituent).
     * 
     * @param start
     * @param end
     * @return true if the specified cell is 'open' only to factored parents
     */
    public abstract boolean isCellOnlyFactored(short start, short end);

    /**
     * Returns true if the specified cell is 'open' to unary productions.
     * 
     * @param start
     * @param end
     * @return true if the specified cell is 'open' to unary productions.
     */
    public abstract boolean isUnaryOpen(short start, short end);

    @Override
    public boolean hasCellConstraints() {
        return constraintsEnabled;
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

    @Override
    public short getMaxSpan(final short start, final short end) {
        return maxSpan == null ? Short.MAX_VALUE : (short) maxSpan.getInt(parser.chart.cellIndex(start, end));
    }
}
