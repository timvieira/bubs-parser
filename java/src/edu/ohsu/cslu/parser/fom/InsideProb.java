package edu.ohsu.cslu.parser.fom;

import edu.ohsu.cslu.parser.ChartEdgeWithFOM;

public class InsideProb extends EdgeFOM {

    @Override
    public float calcFOM(final ChartEdgeWithFOM edge) {
        return edge.insideProb;
    }
}
