package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.fom.EdgeFOM;

public class ChartEdgeWithFOM extends ChartEdge implements Comparable<ChartEdgeWithFOM> {

	public float figureOfMerit;

	public ChartEdgeWithFOM(final Production p, final ArrayChartCell leftCell, final ArrayChartCell rightCell, final float insideScore, final EdgeFOM edgeFOM, final ChartParser parser) {
		super(p, leftCell, rightCell, insideScore);
		this.figureOfMerit = edgeFOM.calcFOM(this, parser);
	}

	public ChartEdgeWithFOM(final Production p, final ArrayChartCell childCell, final float insideScore, final EdgeFOM edgeFOM, final ChartParser parser) {
		super(p, childCell, insideScore);
		this.figureOfMerit = edgeFOM.calcFOM(this, parser);
	}

	// public void setFOM(EdgeFOM edgeFOM) {
	// this.figureOfMerit = edgeFOM.calcFOM(this);
	// }

	@Override
	public int compareTo(final ChartEdgeWithFOM otherEdge) {
		if (this.equals(otherEdge)) {
			return 0;
		} else if (figureOfMerit > otherEdge.figureOfMerit) {
			return -1;
		} else {
			return 1;
		}
	}

	public int spanLength() {
		if (leftCell == null) {
			return 1;
		} else if (rightCell == null) {
			return leftCell.end - leftCell.start;
		}
		return rightCell.end - leftCell.start;
	}

	@Override
	public String toString() {
		return super.toString() + "fom=" + figureOfMerit;
	}

}
