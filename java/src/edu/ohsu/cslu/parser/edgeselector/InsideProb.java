package edu.ohsu.cslu.parser.edgeselector;

import edu.ohsu.cslu.parser.ChartEdge;

public class InsideProb extends EdgeSelector {

    @Override
    public float calcFOM(final ChartEdge edge) {
        return edge.inside;
    }
}
