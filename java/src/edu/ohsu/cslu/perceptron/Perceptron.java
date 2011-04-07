/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.perceptron;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.parser.ParserUtil;

public class Perceptron extends Classifier {
    protected FloatVector[] rawWeights = null;
    protected int trainExampleNumber = 0;
    protected float learningRate;
    protected LossFunction lossFunction;
    protected String featureTemplate;

    // protected int[] bins; // classification bins ex: 0,5,10,30
    protected String binsStr;
    protected float[] bias;

    public Perceptron() {
        this(0.1f, new ZeroOneLoss(), "0", null, null);
    }

    public Perceptron(final float learningRate, final LossFunction lossFunction, final String binsStr,
            final String featureTemplate, final float[] initialWeights) {

        this.learningRate = learningRate;
        this.lossFunction = lossFunction;
        this.featureTemplate = featureTemplate;
        // initBins(binsStr);
        this.binsStr = binsStr;
        this.bins = ParserUtil.strToIntArray(binsStr);

        bias = new float[numClasses()];
        Arrays.fill(bias, 0.0f); // default to no bias

        if (initialWeights != null) {
            initModel(initialWeights);
        }
    }

    // protected void initBins(final String binsString) {
    // this.binsStr = binsString;
    // // convert comma-seperated bin list to int bin list
    // final String[] tokens = binsString.split(",");
    // bins = new int[tokens.length];
    // for (int i = 0; i < tokens.length; i++) {
    // bins[i] = Integer.parseInt(tokens[i]);
    // }
    // }

    @Override
    public void setBias(final String biasString) {
        final String[] tokens = biasString.split(",");
        if (tokens.length != numClasses()) {
            throw new IllegalArgumentException(
                    "ERROR: if bias term is specified, must contain a bias for each class in the model.  numBias="
                            + tokens.length + " numClasses=" + numClasses());
        }
        for (int i = 0; i < tokens.length; i++) {
            bias[i] = Float.parseFloat(tokens[i]);
        }
    }

    // TODO: instead of supplying initialWeights, we should init with an instance
    // of a Perceptron model
    protected void initModel(final float[] initialWeights) {
        rawWeights = new FloatVector[numClasses()];
        for (int i = 0; i < numClasses(); i++) {
            rawWeights[i] = new FloatVector(initialWeights.clone());
        }
    }

    /**
     * Returns the binary output of the raw perceptron model for the specified feature vector.
     * 
     * @param featureVector
     * @return the binary output of the raw perceptron model for the specified feature vector.
     */
    public int classify(final FloatVector[] model, final Vector featureVector) {
        int bestClass = -1;
        float score, bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < numClasses(); i++) {
            score = model[i].dotProduct(featureVector) + bias[i];
            if (score > bestScore) {
                bestScore = score;
                bestClass = i;
            }
        }
        return bestClass;
    }

    @Override
    public int classify(final Vector featureVector) {
        return classify(rawWeights, featureVector);
    }

    // also used by AveragedPerceptron
    @Override
    public void train(final int goldClass, final SparseBitVector featureVector) {

        // since we don't require a user to specify the number of features in their model
        // we need to extract that number from the training data and init the new model
        if (rawWeights == null) {
            final float[] initialWeights = new float[featureVector.vectorLength()];
            Arrays.fill(initialWeights, 0f); // init with 0-vector
            initModel(initialWeights);
        }

        // final boolean rawGuessClass = this.classifyRaw(featureVector);
        // we want to use the raw perceptron for Perceptron AND AveragedPerceptron
        final int rawGuessClass = classify(rawWeights, featureVector);
        trainExampleNumber++;

        final float loss = lossFunction.computeLoss(goldClass, rawGuessClass);
        if (loss != 0) {
            update(goldClass, loss * learningRate, featureVector, trainExampleNumber);
            // incorrect prediction; adjust model
            // if (goldClass == 1) {
            // this.update(featureVector, loss * learningRate, trainExampleNumber);
            // } else {
            // this.update(featureVector, -1 * loss * learningRate, trainExampleNumber);
            // }
        }
    }

    /**
     * Update weights for all features found in the specified feature vector by the specified alpha
     * 
     * @param featureVector Features to update
     * @param alpha Update amount (generally positive for positive examples and negative for negative examples)
     * @param example The number of examples seen in the training corpus (i.e., the index of the example which caused
     *            this update, 1-indexed).
     */
    protected void update(final int goldClass, final float alpha, final SparseBitVector featureVector, final int example) {
        for (int i = 0; i < numClasses(); i++) {
            if (i == goldClass) {
                rawWeights[i].inPlaceAdd(featureVector, alpha);
            } else {
                rawWeights[i].inPlaceAdd(featureVector, -1 * alpha);
            }
        }
    }

    public FloatVector modelWeights(final int modelIndex) {
        return rawWeights[modelIndex];
    }

    @Override
    public String toString() {
        return modelToString(rawWeights);
    }

    protected String modelToString(final FloatVector[] model) {
        String s = "# === Perceptron Model ===\n";
        s += String.format("numFeats=%d numClasses=%d bins=%s numTrainExamples=%d \n", model[0].length(), numClasses(),
                binsStr, trainExampleNumber);
        s += String.format("featTemplate: %s\n", featureTemplate);
        for (int i = 0; i < numClasses(); i++) {
            s += model[i].toString() + "\n";
        }
        return s;
    }

    @Override
    public String getFeatureTemplate() {
        return featureTemplate;
    }

    @Override
    public float computeLoss(final int goldClass, final int guessClass) {
        return lossFunction.computeLoss(goldClass, guessClass);
    }

    public static abstract class LossFunction {
        public abstract float computeLoss(int goldClass, int guessClass);
    }

    /**
     * Allow loss functions other than Zero-One loss such that some errors can be considered better or worse than
     * others.
     */
    public static class ZeroOneLoss extends LossFunction {
        @Override
        public float computeLoss(final int goldClass, final int guessClass) {
            if (goldClass == guessClass) {
                return 0f;
            }
            return 1f;
        }
    }

    // TODO: should also test out sliding scale -- more penalty the farther from the gold prediction
    public static class OverUnderLoss extends LossFunction {
        private float overPenalty, underPenalty;

        public OverUnderLoss(final float overPenalty, final float underPenalty) {
            this.overPenalty = overPenalty;
            this.underPenalty = underPenalty;
        }

        @Override
        public float computeLoss(final int goldClass, final int guessClass) {
            if (goldClass == guessClass) {
                return 0f;
            }
            if (goldClass > guessClass) {
                // high penalty if the predicted beam-width is LOWER than the gold
                return underPenalty;
            }
            // small penalty if we overestimate the beam-width
            return overPenalty;
        }
    }

    @Override
    public void writeModel(final BufferedWriter stream) throws IOException {
        stream.write(toString());
    }

    // /**
    // * For unit testing.
    // *
    // * @param feature
    // * @return raw weight for the specified feature
    // */
    // float rawFeatureWeight(final int feature) {
    // return rawPerceptron.getFloat(feature);
    // }
    //
    // /**
    // * For unit testing.
    // *
    // * @param feature
    // * @param example
    // * @return averaged weight for the specified feature at the specified example
    // */
    // float averagedFeatureWeight(final int feature, final int example) {
    // final int la = lastAveraged.getInt(feature);
    // if (la == 0) {
    // return rawPerceptron.getFloat(feature);
    // } else if (la > example) {
    // throw new IllegalArgumentException("Feature " + feature + " updated at example " + la
    // + " (more recently than requested example " + example + ")");
    // }
    //
    // final float currentAverage = averagedPerceptron.getFloat(feature);
    // // Average up to the current example
    // final float update = (rawPerceptron.getFloat(feature) - currentAverage) * (example - la) / example;
    // return currentAverage + update;
    // }
    // /**
    // * Returns the floating-point output of the raw perceptron model for the specified feature vector.
    // *
    // * @param featureVector
    // * @return the floating-point output of the averaged perceptron model for the specified feature vector.
    // */
    // private float rawFloatOutput(final Vector featureVector) {
    // return rawPerceptron.dotProduct(featureVector);
    // }
}
