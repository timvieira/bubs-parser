package edu.ohsu.cslu.parser.edgeselector;

import edu.ohsu.cslu.parser.ChartEdge;
import edu.ohsu.cslu.parser.ParserDriver;

public class NormalizedInsideProb extends EdgeSelector {

    @Override
    public float calcFOM(final ChartEdge edge) {

        final int spanLength = edge.end() - edge.start();
        return edge.inside + spanLength * ParserDriver.param1;
    }

}
