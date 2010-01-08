package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ChartEdgeWithFOM;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.util.Log;

public abstract class EdgeFOM {

	static public enum EdgeFOMType {
		Inside, NormalizedInside, BoundaryNgram
	}

	public abstract float calcFOM(ChartEdgeWithFOM edge, ChartParser parser);

	public static EdgeFOM create(final EdgeFOMType type, final Grammar grammar) {
		switch (type) {
		case Inside:
			return new EdgeFOMInsideProb();
		case NormalizedInside:
			return new EdgeFOMNormInsideProb();
		case BoundaryNgram:
			return new EdgeFOMBoundaryNgram(grammar);
		default:
			Log.info(0, "ERROR: EdgeFOM " + type + " not supported.");
			System.exit(1);
			return null;
		}
	}

	public void init(final ChartParser parser) {
		// default is to do nothing
	}

	public void train(final BufferedReader inStream) throws Exception {
		throw new Exception("Not implemented.");
	}

	public void readModel(final BufferedReader inStream) throws Exception {
		throw new Exception("Not implemented.");
	}

	public void writeModel(final BufferedWriter outStream) throws Exception {
		throw new Exception("Not implemented.");
	}

}
