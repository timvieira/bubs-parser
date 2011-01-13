package edu.ohsu.cslu.grammar;

public class StringProduction {
    public final String parent;
    public final String leftChild;
    public final float probability;

    public StringProduction(final String parent, final String leftChild, final float probability) {
        this.parent = parent;
        this.leftChild = leftChild;
        this.probability = probability;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s (%.3f)", parent, leftChild, probability);
    }
}