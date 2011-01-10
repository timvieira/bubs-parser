package edu.ohsu.cslu.parser.edgeselector;

import java.io.BufferedReader;
import java.io.IOException;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

// for Beam-Width Prediction, we need the 1-best POS tags from the chart.  And
// to get these, we need to run the forward-backward algorithm with a model
// that has POS transition probabilities.  Right now this is only done for the
// Boundary FOM, so instead of writing lots more code and creating new model files,
// we are simply hi-jacking all of that and overwriting the calcFOM() function to
// ignore most of the work that is done during setup.
public class InsideWithFwdBkwd extends BoundaryInOut {

    public InsideWithFwdBkwd(final Grammar grammar, final BufferedReader modelStream) throws IOException {
        super(grammar, modelStream);
    }

    @Override
    public float calcFOM(final ChartEdge edge) {
        return edge.inside();
    }

    @Override
    public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
        return insideProbability;
    }
}
