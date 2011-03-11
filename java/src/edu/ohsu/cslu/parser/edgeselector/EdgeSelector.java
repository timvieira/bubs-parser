package edu.ohsu.cslu.parser.edgeselector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Serializable;

import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.Chart;

public abstract class EdgeSelector implements Serializable {

    static public enum EdgeSelectorType {
        Inside, NormalizedInside, BoundaryInOut, InsideWithFwdBkwd, WeightedFeatures
    }

    public abstract float calcFOM(ChartEdge edge);

    public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
        throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
    }

    public float calcLexicalFOM(final int start, final int end, final short parent, final float insideProbability) {
        throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
    }

    public void init(final Chart chart) {
        // default is to do nothing
    }

    public void train(final BufferedReader inStream) throws Exception {
        throw new Exception("Not implemented.");
    }

    public void readModel(final BufferedReader inStream) throws Exception {
        // NOTE: some models have nothing to be read
        // throw new Exception("Not implemented.");
    }

    public void writeModel(final BufferedWriter outStream) throws Exception {
        throw new Exception("Not implemented.");
    }

}
