/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.lela;

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelectorModel;
import edu.ohsu.cslu.parser.ml.ConstrainedChartParser;

/**
 * {@link CellSelector} implementation which constrains parsing according to a gold tree represented in a
 * {@link ConstrainedChart}.
 * 
 * @author Aaron Dunlop
 * @since Jan 15, 2011
 */
public class ConstrainedCellSelector extends CellSelector {

    public static CellSelectorModel MODEL = new CellSelectorModel() {

        private static final long serialVersionUID = 1L;

        @Override
        public CellSelector createCellSelector() {
            // ConstrainedCellSelector doesn't support child models
            return new ConstrainedCellSelector(null);
        }
    };

    protected ConstrainedCellSelector(final CellSelector child) {
        super(child);
    }

    @Override
    public void initSentence(final ChartParser<?, ?> p, final ParseTask task) {
        super.initSentence(p, task);

        final ConstrainingChart constrainingChart = ((ConstrainedChartParser) p).constrainingChart();
        final short[][] constrainingChartOpenCells = constrainingChart.openCells;

        this.openCells = constrainingChart.size() * 2 - 1;
        this.cellIndices = new short[openCells * 2];
        for (short i = 0; i < cellIndices.length / 2; i++) {
            this.cellIndices[i * 2] = constrainingChartOpenCells[i][0];
            this.cellIndices[i * 2 + 1] = constrainingChartOpenCells[i][1];
        }
    }

    @Override
    public boolean isCellOpen(final short start, final short end) {
        // This shouldn't frequently (well, ever) be called, so we don't have to worry too much about efficiency
        for (int i = 0; i < cellIndices.length; i += 2) {
            if (cellIndices[i] == start && cellIndices[i + 1] == end) {
                return true;
            }
        }
        return false;
    }
}
