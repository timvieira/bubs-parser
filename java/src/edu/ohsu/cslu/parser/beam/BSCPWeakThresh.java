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
package edu.ohsu.cslu.parser.beam;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public class BSCPWeakThresh extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    float localWorstFOM;

    public BSCPWeakThresh(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initCell(final short start, final short end) {
        super.initCell(start, end);
        localWorstFOM = Float.POSITIVE_INFINITY;
    }

    // track the worst FOM score in the agenda while we add edges. This
    // eliminates about 10% of the pushes, but actually ends up slowing the
    // parser down a little for the Berkeley grammar w/ BoundaryInOut FOM
    @Override
    protected void addEdgeToCollection(final ChartEdge edge) {
        cellConsidered++;
        if (fomCheckAndUpdate(edge)) {
            if (agenda.size() < beamWidth || edge.fom > localWorstFOM) {
                agenda.add(edge);
                cellPushed++;

                if (edge.fom < localWorstFOM) {
                    localWorstFOM = edge.fom;
                }
            }
        }
    }
}
