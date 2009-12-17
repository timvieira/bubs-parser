package edu.ohsu.cslu.parser.fom;

import edu.ohsu.cslu.parser.ChartEdgeWithFOM;
import edu.ohsu.cslu.parser.util.Log;

public abstract class EdgeFOM {

	static public enum EdgeFOMType {
		Inside,
		NormalizedInside,
		BoundaryTrigram
	}
	
	public abstract float calcFOM(ChartEdgeWithFOM edge);
	
	public static EdgeFOM create(EdgeFOMType type) {
		switch (type) {
			case Inside: return new EdgeFOMInsideProb();
			case NormalizedInside: return new EdgeFOMNormInsideProb();
			case BoundaryTrigram:
			default:
				Log.info(0, "ERROR: EdgeFOM "+type+" not supported.");
				System.exit(0);
				return null;
		}
	}

	public void train(String fileName) {

	}

	public void readFromFile(String fileName) {

	}

	public void writeToFile(String fileName) {

	}

}
