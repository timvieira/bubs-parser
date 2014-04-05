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
import java.io.Serializable;
import java.util.Arrays;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.DenseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.MutableSparseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.parser.Util;

public class Perceptron extends Classifier {

    private static final long serialVersionUID = 1L;

    public static int MAX_DENSE_STORAGE_SIZE = 3000000;

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

    public Perceptron(final float learningRate, final LossFunction lossFunction, final int classes, final long features) {

        this.learningRate = learningRate;
        this.lossFunction = lossFunction;
        bias = new float[classes];
        Arrays.fill(bias, 0.0f); // default to no bias

        rawWeights = new FloatVector[classes];
        for (int i = 0; i < classes; i++) {
            if (features <= MAX_DENSE_STORAGE_SIZE) {
                rawWeights[i] = new DenseFloatVector((int) features);
            } else if (features <= Integer.MAX_VALUE) {
                rawWeights[i] = new MutableSparseFloatVector((int) features);
            } else {
                rawWeights[i] = new LargeSparseFloatVector(features);
            }
        }
    }

    public Perceptron(final float learningRate, final LossFunction lossFunction, final String binsStr,
            final String featureTemplate, final float[] initialWeights) {

        this.learningRate = learningRate;
        this.lossFunction = lossFunction;
        this.featureTemplate = featureTemplate;
        // initBins(binsStr);
        this.binsStr = binsStr;
        this.bins = Util.strToIntArray(binsStr);

        bias = new float[numClasses()];
        Arrays.fill(bias, 0.0f); // default to no bias

        if (initialWeights != null) {
            initModel(initialWeights);
        }
    }

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
        // TODO Handle large feature-spaces here
        rawWeights = new FloatVector[numClasses()];
        for (int i = 0; i < numClasses(); i++) {
            rawWeights[i] = new DenseFloatVector(initialWeights.clone());
        }
    }

    protected void initModel(final int numFeatures) {
        final float[] initialWeights = new float[numFeatures];
        Arrays.fill(initialWeights, 0f); // init with 0-vector
        initModel(initialWeights);
    }

    /**
     * Returns the 1-best class output of the raw perceptron model for the specified feature vector.
     * 
     * @param featureVector
     * @return the 1-best class output of the raw perceptron model for the specified feature vector.
     */
    public short classify(final FloatVector[] model, final Vector featureVector) {
        short bestClass = -1;
        float score, bestScore = Float.NEGATIVE_INFINITY;
        for (short i = 0; i < model.length; i++) {
            score = featureVector.dotProduct(model[i]) + bias[i];
            if (score > bestScore) {
                bestScore = score;
                bestClass = i;
            }
        }
        return bestClass;
    }

    @Override
    public short classify(final Vector featureVector) {
        return classify(rawWeights, featureVector);
    }

    /**
     * Returns all classes, ranked by the raw perceptron model for the specified feature vector.
     * 
     * @param featureVector
     * @return all classes, ranked by the raw perceptron model for the specified feature vector.
     */
    public short[] rank(final Vector featureVector) {
        return scoredRank(featureVector).classes;
    }

    /**
     * Returns all classes, ranked by the raw perceptron model for the specified feature vector.
     * 
     * @param featureVector
     * @return all classes, ranked by the raw perceptron model for the specified feature vector.
     */
    public ScoredRanking scoredRank(final Vector featureVector) {
        return scoredRank(rawWeights, featureVector);
    }

    /**
     * Returns all classes, ranked by the raw perceptron model for the specified feature vector.
     * 
     * @param model The selected model
     * @param featureVector
     * @return all classes, ranked by the raw perceptron model for the specified feature vector.
     */
    protected ScoredRanking scoredRank(final FloatVector[] model, final Vector featureVector) {

        final short[] classes = new short[model.length];
        final float[] scores = new float[model.length];

        for (short i = 0; i < model.length; i++) {
            classes[i] = i;
            scores[i] = featureVector.dotProduct(model[i]) + bias[i];
        }
        return new ScoredRanking(classes, scores);
    }

    // also used by AveragedPerceptron
    @Override
    public void train(final int goldClass, final BitVector featureVector) {

        // since we don't require a user to specify the number of features in their model
        // we need to extract that number from the training data and init the new model
        if (rawWeights == null) {
            initModel(new float[(int) featureVector.length()]);
        }

        // final boolean rawGuessClass = this.classifyRaw(featureVector);
        // we want to use the raw perceptron for Perceptron AND AveragedPerceptron
        final int rawGuessClass = classify(rawWeights, featureVector);
        trainExampleNumber++;

        final float loss = lossFunction.computeLoss(goldClass, rawGuessClass);
        if (loss != 0) {
            update(goldClass, rawGuessClass, loss * learningRate, featureVector, trainExampleNumber);
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
    protected void update(final int goldClass, final int guessClass, final float alpha, final BitVector featureVector,
            final int example) {
        // Upweight gold class weights
        rawWeights[goldClass].inPlaceAdd(featureVector, alpha);

        // Downweight guess class weights
        rawWeights[guessClass].inPlaceAdd(featureVector, -alpha);
    }

    public FloatVector modelWeights(final int modelIndex) {
        return rawWeights[modelIndex];
    }

    @Override
    public String toString() {
        return modelToString(rawWeights);
    }

    protected String modelToString(final FloatVector[] model) {
        final StringBuilder sb = new StringBuilder((int) (model.length * model[0].length() * 8));

        sb.append("# === Perceptron Model ===\n");
        sb.append(String.format("numFeats=%d numClasses=%d bins=%s numTrainExamples=%d \n", model[0].length(),
                model.length, binsStr, trainExampleNumber));
        sb.append(String.format("featTemplate: %s\n", featureTemplate));
        for (int i = 0; i < model.length; i++) {
            sb.append(model[i].toString());
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public String getFeatureTemplate() {
        return featureTemplate;
    }

    @Override
    public float computeLoss(final int goldClass, final int guessClass) {
        return lossFunction.computeLoss(goldClass, guessClass);
    }

    /**
     * Abstracts the concept of a 'loss' function. For balanced classification problems, {@link ZeroOneLoss} usually
     * works nicely, but for problems in which a misclassification in one direction or the other is more costly, other
     * loss functions may be more appropriate.
     */
    public static abstract class LossFunction implements Serializable {

        private static final long serialVersionUID = 1L;

        public abstract float computeLoss(int goldClass, int guessClass);
    }

    /**
     * An equally-balanced loss function
     */
    public static class ZeroOneLoss extends LossFunction {

        private static final long serialVersionUID = 1L;

        @Override
        public float computeLoss(final int goldClass, final int guessClass) {
            if (goldClass == guessClass) {
                return 0f;
            }
            return 1f;
        }
    }

    /**
     * An unbalanced loss function
     */
    public static class BiasedLoss extends LossFunction {

        private static final long serialVersionUID = 1L;

        private final float loss[];

        public BiasedLoss(final float[] loss) {
            this.loss = loss;
        }

        @Override
        public float computeLoss(final int goldClass, final int guessClass) {
            if (goldClass == guessClass) {
                return 0f;
            }
            return loss[goldClass];
        }
    }

    // TODO: should also test out sliding scale -- more penalty the farther from the gold prediction
    public static class OverUnderLoss extends LossFunction {

        private static final long serialVersionUID = 1L;

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

    public static class ScoredRanking {
        public final short[] classes;
        public final float[] scores;

        public ScoredRanking(final short[] classes, final float[] scores) {
            this.classes = classes;
            this.scores = scores;
            edu.ohsu.cslu.util.Arrays.sort(scores, classes);
            edu.ohsu.cslu.util.Arrays.reverse(scores);
            edu.ohsu.cslu.util.Arrays.reverse(classes);
        }
    }
}
