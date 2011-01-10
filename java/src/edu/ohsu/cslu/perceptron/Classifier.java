package edu.ohsu.cslu.perceptron;

import java.io.BufferedWriter;
import java.io.IOException;

import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

public abstract class Classifier {

    protected int bins[];

    public abstract void train(final int goldClass, final SparseBitVector featureVector);

    public abstract int classify(final Vector featureVector);

    public abstract float computeLoss(final int goldClass, final int guessClass);

    // put this in the constructor
    // public abstract void readModel(BufferedReader stream) throws IOException;

    public abstract void writeModel(BufferedWriter stream) throws IOException;

    public abstract String getFeatureTemplate();

    // not sure if I want this here ...
    public abstract void setBias(final String biasString);

    // public int numModels() {
    // final int n = numClasses();
    // if (n == 2) {
    // return 1; // special case for binary classifier: only need a single model
    // }
    // return n;
    // }

    public int numClasses() {
        return bins.length + 1;
    }

    public float class2value(final int c) {
        if (c == numClasses() - 1) {
            return Integer.MAX_VALUE;
        }
        return bins[c];
    }

    public int value2class(final float value) {
        for (int i = 0; i < bins.length; i++) {
            if (value <= bins[i]) {
                return i;
            }
        }
        return numClasses() - 1;
    }
}
