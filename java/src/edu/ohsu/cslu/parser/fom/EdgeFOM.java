package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.parser.ChartEdgeWithFOM;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.util.Log;

public abstract class EdgeFOM {

    static public enum EdgeFOMType {
        Inside, NormalizedInside, BoundaryInOut, WeightedFeatures
    }

    public abstract float calcFOM(ChartEdgeWithFOM edge, ChartParser parser);

    public static EdgeFOM create(final EdgeFOMType type, final BufferedReader fomModelStream, final ArrayGrammar grammar) throws Exception {
        switch (type) {
        case Inside:
            return new InsideProb();
        case NormalizedInside:
            return new NormalizedInsideProb();
        case BoundaryInOut:
            final EdgeFOM model = new BoundaryInOut(grammar);
            // no input model stream when estimating the model
            if (fomModelStream != null) {
                model.readModel(fomModelStream);
            }
            return model;
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
