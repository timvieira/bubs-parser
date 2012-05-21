package edu.ohsu.cslu.perceptron;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.vectors.DenseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.parser.Util;

public class LogisticRegressor implements Serializable {

    private static final long serialVersionUID = 1L;

    protected FloatVector[] weights = null;
    // protected FloatMatrix modelMatrix;
    protected float learningRate;
    protected LossFunction lossFunction;
    protected int numFeatures;
    protected int numModels;
    public String featureString;

    public LogisticRegressor(final int numFeatures, final int numModels) {
        this(numFeatures, numModels, 0.1f, new DifferenceLoss());
    }

    public LogisticRegressor(final int numFeatures, final int numModels, final float learningRate,
            final LossFunction lossFunction) {
        this.learningRate = learningRate;
        this.lossFunction = lossFunction;
        this.numFeatures = numFeatures;
        this.numModels = numModels;
        this.weights = new FloatVector[numModels];
        for (int i = 0; i < numModels; i++) {
            this.weights[i] = new DenseFloatVector(numFeatures);
        }
    }

    public float[] train(final FloatVector goldValues, final SparseBitVector featureVector) {
        final float[] lossPerModel = new float[numModels];
        final FloatVector predictedValues = predict(featureVector);
        for (int i = 0; i < numModels; i++) {
            lossPerModel[i] = lossFunction.computeLoss(goldValues.getFloat(i), predictedValues.getFloat(i));
            if (lossPerModel[i] != 0) {
                weights[i].inPlaceAdd(featureVector, learningRate * lossPerModel[i]);
            }
        }
        return lossPerModel;
    }

    public FloatVector predict(final SparseBitVector featureVector) {
        final FloatVector result = new DenseFloatVector(numModels);
        for (int i = 0; i < numModels; i++) {
            result.set(i, weights[i].dotProduct(featureVector));
        }
        return result;
    }

    public static abstract class LossFunction implements Serializable {

        private static final long serialVersionUID = 1L;

        public abstract float computeLoss(float goldValue, float guessValue);
    }

    public static class DifferenceLoss extends LossFunction {

        private static final long serialVersionUID = 1L;

        @Override
        public float computeLoss(final float goldValue, final float guessValue) {
            return goldValue - guessValue;
        }
    }

    public void write(final BufferedWriter file, final String featureTemplate) throws IOException {
        // final BufferedWriter file = new BufferedWriter(new FileWriter(fileName));
        file.write("model=LogisticRegressor numModels=" + numModels + " numFeatures=" + numFeatures + " feats="
                + featureTemplate + "\n");
        for (int i = 0; i < numModels; i++) {
            file.write(String.format("model:%d ", i));
            for (int j = 0; j < numFeatures; j++) {
                final float weight = weights[i].getFloat(j);
                if (weight != 0f) {
                    file.write(String.format("%d:%.4f ", j, weight));
                }
            }
            file.write("\n");
        }
        file.close();
    }

    public static LogisticRegressor read(final BufferedReader inStream) throws NumberFormatException, IOException {
        LogisticRegressor regressor = null;
        String line;
        boolean firstLine = true;
        while ((line = inStream.readLine()) != null) {
            if (firstLine) {
                final HashMap<String, String> keyValue = Util.readKeyValuePairs(line);
                final int numFeatures = Integer.parseInt(keyValue.get("numFeatures"));
                final int numModels = Integer.parseInt(keyValue.get("numModels"));
                regressor = new LogisticRegressor(numFeatures, numModels);
                regressor.featureString = line.split("feats=")[1].trim();
                firstLine = false;
                BaseLogger.singleton().fine(
                        "Reading LogisticRegressor model with " + numModels + " models and " + numFeatures
                                + " features ... ");
            } else {
                final String toks[] = line.split("\\s+");
                final int modelIndex = Integer.parseInt(toks[0].split(":")[1]);
                for (final String tok : toks) {
                    final String[] keyValue = tok.split(":");
                    if (!keyValue[0].equals("model")) {
                        try {
                            regressor.weights[modelIndex].set(Integer.parseInt(keyValue[0]),
                                    Float.parseFloat(keyValue[1]));
                        } catch (final Exception e) {
                            System.out.println("modelIndex=" + modelIndex + " " + tok);
                            System.exit(1);
                        }
                    }
                }
            }
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
