package edu.ohsu.cslu.perceptron;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;

public class LogisticRegressor {

    protected FloatVector[] rawWeights = null;
    protected float learningRate;
    protected LossFunction lossFunction;
    protected int numFeatures;
    protected int numModels;

    public LogisticRegressor(final float learningRate, final LossFunction lossFunction, final int numFeatures,
            final int numModels) {
        this.learningRate = learningRate;
        this.lossFunction = lossFunction;
        this.numFeatures = numFeatures;
        this.numModels = numModels;

        if (numFeatures > 0) {
            initWeights(numFeatures);
        }
    }

    private void initWeights(final int numFeatures) {
        this.numFeatures = numFeatures;
        rawWeights = new FloatVector[numModels];
        for (int i = 0; i < numModels; i++) {
            final float[] initialWeights = new float[numFeatures];
            Arrays.fill(initialWeights, 0f);
            rawWeights[i] = new FloatVector(initialWeights);
        }
    }

    public float[] train(final FloatVector goldValues, final SparseBitVector featureVector) {
        final float[] lossPerModel = new float[numModels];
        for (int i = 0; i < numModels; i++) {
            final float predictValue = predict(i, featureVector);
            lossPerModel[i] = lossFunction.computeLoss(goldValues.getFloat(i), predictValue);
            if (lossPerModel[i] > Float.NEGATIVE_INFINITY) {
                rawWeights[i].inPlaceAdd(featureVector, learningRate * lossPerModel[i]);
            }
        }
        return lossPerModel;
    }

    public float predict(final int modelIndex, final SparseBitVector featureVector) {
        return rawWeights[modelIndex].dotProduct(featureVector);
    }

    public static abstract class LossFunction {
        public abstract float computeLoss(float goldValue, float guessValue);
    }

    public static class DifferenceLoss extends LossFunction {
        @Override
        public float computeLoss(final float goldValue, final float guessValue) {
            return goldValue - guessValue;
        }
    }

    public void write(final String fileName) throws IOException {
        final BufferedWriter file = new BufferedWriter(new FileWriter(fileName));
        for (int i = 0; i < numModels; i++) {
            for (int j = 0; j < numFeatures; j++) {
                final float weight = rawWeights[i].getFloat(j);
                if (weight != 0f) {
                    file.write(String.format("%d:%.4f ", j, weight));
                }
            }
            file.write("\n");
        }
    }

    // public static class L1Loss extends LossFunction {
    // @Override
    // public float computeLoss(float goldValue, float guessValue) {
    // return Math.abs(goldValue - guessValue);
    // }
    // }
    //
    // public static class L2Loss extends LossFunction {
    // @Override
    // public float computeLoss(float goldValue, float guessValue) {
    // return (float) Math.pow(goldValue - guessValue, 2);
    // }
    // }

}
