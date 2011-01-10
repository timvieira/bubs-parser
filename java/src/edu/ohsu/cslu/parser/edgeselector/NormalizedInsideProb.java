package edu.ohsu.cslu.parser.edgeselector;

import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public class NormalizedInsideProb extends InsideProb {

    @Override
    public float calcFOM(final ChartEdge edge) {

        final int spanLength = edge.end() - edge.start();
        return edge.inside() + spanLength * ParserDriver.param1;
    }

}
