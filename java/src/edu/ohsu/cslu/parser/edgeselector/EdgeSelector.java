package edu.ohsu.cslu.parser.edgeselector;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.util.Log;

public abstract class EdgeSelector {

    static public enum EdgeSelectorType {
        Inside, NormalizedInside, BoundaryInOut, WeightedFeatures
    }

    public abstract float calcFOM(ChartEdge edge);

    public static EdgeSelector create(final EdgeSelectorType type, final BufferedReader fomModelStream, final Grammar grammar) {
        switch (type) {
            case Inside:
                return new InsideProb();
            case NormalizedInside:
                return new NormalizedInsideProb();
            case BoundaryInOut:
                return new BoundaryInOut(grammar, fomModelStream);
            case WeightedFeatures:
                return new WeightedFeatures(grammar);
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
