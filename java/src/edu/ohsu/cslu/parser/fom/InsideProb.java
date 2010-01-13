package edu.ohsu.cslu.parser.fom;

import edu.ohsu.cslu.parser.ChartEdgeWithFOM;
import edu.ohsu.cslu.parser.ChartParser;

public class InsideProb extends EdgeFOM {

	@Override
	public float calcFOM(final ChartEdgeWithFOM edge, final ChartParser parser) {
		return edge.insideProb;
	}
}
