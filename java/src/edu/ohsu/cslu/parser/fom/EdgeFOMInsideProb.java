package edu.ohsu.cslu.parser.fom;

import edu.ohsu.cslu.parser.ChartEdgeWithFOM;

public class EdgeFOMInsideProb extends EdgeFOM {

	@Override
	public float calcFOM(ChartEdgeWithFOM edge) {
		return edge.insideProb;
	}

}
