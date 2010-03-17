package edu.ohsu.cslu.parser.chart;

import edu.ohsu.cslu.parser.Parser;

public abstract class Chart {

    protected int size;
    protected boolean viterbiMax;
    protected Parser parser;

    protected Chart() {

    }

    public Chart(final int size, final boolean viterbiMax, final Parser parser) {
        this.size = size;
        this.viterbiMax = viterbiMax;
        this.parser = parser;
    }

    public int size() {
        return size;
    }

    public abstract void updateInside(int start, int end, int nt, float insideProb);

    public abstract float getInside(int start, int end, int nt);

    // public abstract void updateOutside(int start, int end, int nt, float insideProb);
    // public abstract float getOutside(int start, int end, int nt);

}
