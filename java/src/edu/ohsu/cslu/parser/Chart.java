package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;

public abstract class Chart {

    protected int size;
    public Grammar grammar;

    protected Chart() {

    }

    public Chart(final int size, final Grammar grammar) {
        this.size = size;
        this.grammar = grammar;
    }

    public int size() {
        return size;
    }

    public abstract void updateInside(int start, int end, int nt, float insideProb);

    public abstract float getInside(int start, int end, int nt);

    // public abstract void updateOutside(int start, int end, int nt, float insideProb);
    // public abstract float getOutside(int start, int end, int nt);

}
