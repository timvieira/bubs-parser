/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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
 * @author Nathan Bodenstab
 * @since May 8, 2012
 */
public class CellConstraintsComboModel implements CellSelectorModel {

    private CellSelectorModel models[] = new CellSelectorModel[5];
    int numModels = 0;

    public void addModel(final CellSelectorModel m) {
        models[numModels++] = m;
    }

    @Override
    public CellSelector createCellSelector() {
        return new CellConstraintsCombo();
    }

    public class CellConstraintsCombo extends CellSelector {

        private CellSelector constraints[];

        public CellConstraintsCombo() {
            constraints = new CellSelector[numModels];
            for (int i = 0; i < numModels; i++) {
                constraints[i] = models[i].createCellSelector();
            }
        }

        @Override
        public void initSentence(final ChartParser<?, ?> p) {
            for (int i = 0; i < numModels; i++) {
                constraints[i].initSentence(p);
            }

            // this.cellIndices = constraints[1].cellIndices;
            // this.openCells = constraints[1].openCells;
        }

        @Override
        public int getBeamWidth(final short start, final short end) {
            int beam = Integer.MAX_VALUE;
            for (int i = 0; i < numModels; i++) {
                beam = Math.min(beam, constraints[i].getBeamWidth(start, end));
            }
            return beam;
        }

        @Override
        public void reset(final boolean enableConstraints) {
            for (int i = 0; i < numModels; i++) {
                constraints[i].reset(enableConstraints);
            }
        }

        // TODO Reimplement this?
        // @Override
        // protected boolean isGrammarLeftFactored() {
        // return constraints[0].isGrammarLeftFactored();
        // }

        @Override
        public boolean isCellOpen(final short start, final short end) {
            // All must be open
            boolean val = true;
            for (int i = 0; i < numModels; i++) {
                val &= constraints[i].isCellOpen(start, end);
            }
            System.out.println("CC_COMBO: [" + start + "," + end + "]=" + val);
            return val;
        }

        @Override
        public boolean isCellOnlyFactored(final short start, final short end) {
            // Only one must be onlyFactored
            boolean val = false;
            for (int i = 0; i < numModels; i++) {
                val |= constraints[i].isCellOnlyFactored(start, end);
            }
            return val;
        }

        @Override
        public boolean isUnaryOpen(final short start, final short end) {
            // All must be open
            boolean val = true;
            for (int i = 0; i < numModels; i++) {
                val &= constraints[i].isUnaryOpen(start, end);
            }
            return val;
        }

        @Override
        public String toString() {
            return "CC_STATS: combo #models=" + numModels;
        }

        // TODO: just takes combo from chart constraints. How should we combine these generally?
        @Override
        public boolean hasNext() {
            return constraints[0].hasNext();
        }

        @Override
        public short[] next() {
            // if we return a cell that has beamWidth=0 then SparseMatrixParser fails
            short[] possible = constraints[0].next();
            while (getBeamWidth(possible[0], possible[1]) == 0) {
                possible = constraints[0].next();
            }
            return possible;
        }
    }

}
