package edu.ohsu.cslu.parser.edgeselector;

import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public class InsideProb extends EdgeSelector {

    @Override
    public float calcFOM(final ChartEdge edge) {
        return edge.inside();
    }

    @Override
    public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
        return insideProbability;
    }

    @Override
    public float calcLexicalFOM(final int start, final int end, final short parent, final float insideProbability) {
        return insideProbability;
    }
}
