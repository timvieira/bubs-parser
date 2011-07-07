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
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

/**
 * Beam search chart parser which prunes search space using exponential decay (see Bodenstab, 2010).
 * 
 * @author Nathan Bodenstab
 */
public class BSCPExpDecay extends BSCPPruneViterbi {

    boolean resultRun = false;
    CellChart resultChart;

    public BSCPExpDecay(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {

        int maxPops = (int) opts.param1;
        int minPops = (int) opts.param2;
        float lambda = opts.param3;

        if (maxPops < 0)
            maxPops = 15;
        if (minPops < 0)
            minPops = 3;
        if (lambda < 0)
            lambda = (float) 0.007;

        // min/max
        // final float minPops = ParserDriver.param2;
        // maxEdgesToAdd = (int) ((chartSize - cell.width() + 1) / (float) chartSize * (maxPops - minPops) +
        // minPops);
        // System.out.println(" max=" + maxPops + " min=" + minPops + " width=" + cell.width() + " pops=" +
        // maxEdgesToAdd);

        // slope
        // final float slope = ParserDriver.param2;
        // maxEdgesToAdd = (int) (maxPops - (cell.width() * slope));

        // exp decay
        // final float spanRatio = cell.width() / (float) chart.size();

        // maxEdgesToAdd = (int) Math.ceil(maxPops * Math.exp(-1 * spanRatio * ParserDriver.param2));

        // adpt exp decay
        // float adaptDecay = (float) Math.log(Math.max(1, chartSize - ParserDriver.param2));

        // adapt exp decay #2
        // final float adaptDecay = (float) ((float) Math.log(Math.max(1, chartSize - 2)) /
        // Math.log(ParserDriver.param2));

        // adapt exp decay #3
        // final float adaptDecay = (float) ((float) Math.log(Math.max(1, chartSize - ParserDriver.param2)) /
        // Math.log(5));

        // final float adaptDecay = (float) ((float) Math.log(Math.max(1, chart.size() - 3)) / Math.log(5));

        // adapt exp decay strong
        // final float adaptDecay = Math.max(1, chartSize - 5) / ParserDriver.param2;

        // maxEdgesToAdd = (int) Math.ceil(maxPops * Math.exp(-1 * spanRatio * adaptDecay));

        // used in EMNLP paper
        // N = N0 * e^(-1*lambda*s*n)
        // N = N0 * e^(-1*lambda*s*n/N0) ... the relative decay does not scale when increasing N0 unless
        // we divide by N0 in the exp(.). But this can be absorbed into lambda for the equation.
        beamWidth = (int) Math.ceil(maxPops * Math.exp(-1 * lambda * cell.width() * chart.size() / maxPops));

        if (beamWidth < minPops) {
            beamWidth = minPops;
        }

        super.addEdgeCollectionToChart(cell);
    }
}
