package edu.ohsu.cslu.parser.fom;

import edu.ohsu.cslu.parser.ChartEdgeWithFOM;
import edu.ohsu.cslu.parser.ParserDriver;

public class NormalizedInsideProb extends EdgeFOM {

    @Override
    public float calcFOM(final ChartEdgeWithFOM edge) {

        final int spanLength = edge.end() - edge.start();
        return edge.insideProb + spanLength * ParserDriver.fudgeFactor;
    }

}
