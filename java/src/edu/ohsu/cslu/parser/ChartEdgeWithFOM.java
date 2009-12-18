package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.fom.EdgeFOM;

public class ChartEdgeWithFOM extends ChartEdge implements Comparable<ChartEdgeWithFOM> {

    public float figureOfMerit;

    public ChartEdgeWithFOM(Production p, ChartCell leftCell, ChartCell rightCell, float insideScore,
            EdgeFOM edgeFOM) {
        super(p, leftCell, rightCell, insideScore);
        this.figureOfMerit = edgeFOM.calcFOM(this);
    }

    public ChartEdgeWithFOM(Production p, ChartCell childCell, float insideScore, EdgeFOM edgeFOM) {
        super(p, childCell, insideScore);
        this.figureOfMerit = edgeFOM.calcFOM(this);
    }

    // public void setFOM(EdgeFOM edgeFOM) {
    // this.figureOfMerit = edgeFOM.calcFOM(this);
    // }

    @Override
    public int compareTo(ChartEdgeWithFOM otherEdge) {
        if (this.equals(otherEdge)) {
            return 0;
        } else if (figureOfMerit > otherEdge.figureOfMerit) {
            return -1;
        } else {
            return 1;
        }
    }

}
