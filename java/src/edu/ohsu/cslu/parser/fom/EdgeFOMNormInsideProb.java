package edu.ohsu.cslu.parser.fom;

import edu.ohsu.cslu.parser.ChartEdgeWithFOM;

public class EdgeFOMNormInsideProb extends EdgeFOM {

	@Override
	public float calcFOM(ChartEdgeWithFOM edge) {
		if (edge.p.isUnaryProd()) {
			return edge.insideProb;
		} else {
			int spanLength = edge.rightCell.end - edge.leftCell.start;
			return edge.insideProb / spanLength;
		}
	}

}
