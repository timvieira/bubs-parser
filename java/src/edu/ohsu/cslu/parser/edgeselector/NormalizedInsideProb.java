package edu.ohsu.cslu.parser.edgeselector;

import edu.ohsu.cslu.parser.ParserOptions;
import edu.ohsu.cslu.parser.CellChart.ChartEdge;

public class NormalizedInsideProb extends EdgeSelector {

    @Override
    public float calcFOM(final ChartEdge edge) {

        final int spanLength = edge.end() - edge.start();
        return edge.inside() + spanLength * ParserOptions.param1;
    }

}
