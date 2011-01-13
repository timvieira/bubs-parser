package edu.ohsu.cslu.grammar;

public final class BinaryStringProduction extends StringProduction {
    public final String rightChild;

    public BinaryStringProduction(final String parent, final String leftChild, final String rightChild,
            final float probability) {
        super(parent, leftChild, probability);
        this.rightChild = rightChild;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s %s (%.3f)", parent, leftChild, rightChild, probability);
    }
}