package edu.ohsu.cslu.perceptron;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.parser.Util;

public class LogisticRegressor {

    protected FloatVector[] weights = null;
    // protected FloatMatrix modelMatrix;
    protected float learningRate;
    protected LossFunction lossFunction;
    protected int numFeatures;
    protected int numModels;

    public LogisticRegressor(final int numFeatures, final int numModels) {
        this(numFeatures, numModels, 0f, null);
    }

    public LogisticRegressor(final int numFeatures, final int numModels, final float learningRate,
            final LossFunction lossFunction) {
        this.learningRate = learningRate;
        this.lossFunction = lossFunction;
        this.numFeatures = numFeatures;
        this.numModels = numModels;
        this.weights = new FloatVector[numModels];
        for (int i = 0; i < numModels; i++) {
            this.weights[i] = new FloatVector(numFeatures);
        }
    }

    public float[] train(final FloatVector goldValues, final SparseBitVector featureVector) {
        final float[] lossPerModel = new float[numModels];
        final FloatVector predictedValues = predict(featureVector);
        for (int i = 0; i < numModels; i++) {
            lossPerModel[i] = lossFunction.computeLoss(goldValues.getFloat(i), predictedValues.getFloat(i));
            if (lossPerModel[i] > Float.NEGATIVE_INFINITY) {
                weights[i].inPlaceAdd(featureVector, learningRate * lossPerModel[i]);
            }
        }
        return lossPerModel;
    }

    public FloatVector predict(final SparseBitVector featureVector) {
        final FloatVector result = new FloatVector(numModels);
        for (int i = 0; i < numModels; i++) {
            result.set(i, weights[i].dotProduct(featureVector));
        }
        return result;
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
        file.write("model=LogisticRegressor numModels=" + numModels + " numFeatures=" + numFeatures + "\n");
        for (int i = 0; i < numModels; i++) {
            for (int j = 0; j < numFeatures; j++) {
                final float weight = weights[i].getFloat(j);
                if (weight != 0f) {
                    file.write(String.format("%d:%.4f ", j, weight));
                }
            }
            file.write("\n");
        }
    }

    public static LogisticRegressor read(final BufferedReader inStream) throws NumberFormatException, IOException {
        LogisticRegressor regressor = null;
        String line;
        int i = 0;
        while ((line = inStream.readLine()) != null) {
            if (i == 0) {
                final HashMap<String, String> keyValue = Util.readKeyValuePairs(line);
                final int numFeatures = Integer.parseInt(keyValue.get("numFeatures"));
                final int numModels = Integer.parseInt(keyValue.get("numModels"));
                regressor = new LogisticRegressor(numFeatures, numModels);
            } else {
                final String toks[] = line.split("\\s+");
                for (final String tok : toks) {
                    final String[] keyValue = tok.split(":");
                    regressor.weights[i + 1].set(Integer.parseInt(keyValue[0]), Float.parseFloat(keyValue[1]));
                }
            }
            i++;
        }

        return regressor;
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
