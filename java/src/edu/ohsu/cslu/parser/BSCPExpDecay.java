package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPExpDecay extends BSCPPruneViterbi {

    boolean resultRun = false;
    CellChart resultChart;

    public BSCPExpDecay(final ParserOptions opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {

        int maxPops = (int) ParserOptions.param1;
        int minPops = (int) ParserOptions.param2;
        float lambda = ParserOptions.param3;

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
