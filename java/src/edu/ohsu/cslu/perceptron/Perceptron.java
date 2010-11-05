package edu.ohsu.cslu.perceptron;

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

public class Perceptron {
    protected FloatVector[] rawWeights = null;
    protected int trainExampleNumber = 0;
    protected float learningRate;
    protected LossFunction lossFunction;
    protected String featureTemplate;

    protected int[] bins; // classification bins ex: 0,5,10,30
    protected String binsStr;

    public Perceptron() {
        this(0.1f, new ZeroOneLoss(), "0", null, null);
    }

    public Perceptron(final float learningRate, final LossFunction lossFunction, final String binsStr,
            final String featureTemplate, final float[] initialWeights) {

        this.learningRate = learningRate;
        this.lossFunction = lossFunction;
        this.featureTemplate = featureTemplate;
        initBins(binsStr);

        if (initialWeights != null) {
            initModel(initialWeights);
        }
    }

    protected void initBins(final String binsString) {
        this.binsStr = binsString;
        // convert comma-seperated bin list to int bin list
        final String[] tokens = binsString.split(",");
        bins = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            bins[i] = Integer.parseInt(tokens[i]);
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
    public static int classify(final FloatVector[] model, final Vector featureVector) {
        int bestClass = -1;
        final int numClasses = model.length;
        float score, bestScore = -1 * Float.MAX_VALUE;
        for (int i = 0; i < numClasses; i++) {
            score = model[i].dotProduct(featureVector);
            if (score > bestScore) {
                bestScore = score;
                bestClass = i;
            }
        }
        return bestClass;
    }

    public int classify(final Vector featureVector) {
        return Perceptron.classify(rawWeights, featureVector);
    }

    // also used by AveragedPerceptron
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
        final int rawGuessClass = Perceptron.classify(rawWeights, featureVector);
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

    public int numClasses() {
        // return numClasses;
        return bins.length + 1;
    }

    public String featureTemplate() {
        return featureTemplate;
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