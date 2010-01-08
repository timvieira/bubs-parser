package edu.ohsu.cslu.parser.fom;

import edu.ohsu.cslu.parser.ArrayChartCell;
import edu.ohsu.cslu.parser.ChartEdgeWithFOM;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;

public class EdgeFOMNormInsideProb extends EdgeFOM {

    @Override
    public float calcFOM(final ChartEdgeWithFOM edge, final ChartParser parser) {
        // if (edge.p.isUnaryProd()) {
        // return edge.insideProb;
        // }
        ArrayChartCell rightCell = edge.rightCell;
        if (rightCell == null) {
            rightCell = edge.leftCell;
        }

        final int spanLength = rightCell.end - edge.leftCell.start;
        // return edge.insideProb / spanLength;
        // return edge.insideProb - (float) (Math.log(spanLength) * 0.01);
        // return edge.insideProb - (float) Math.pow(0.7, spanLength);
        return edge.insideProb + ParserDriver.fudgeFactor * spanLength;
        // for f24 sent 1 and R2 grammar, !7 seems fast and ~5 seems accurate
        // Should probably come up with a formulate that is dependent on the grammar
    }

}
