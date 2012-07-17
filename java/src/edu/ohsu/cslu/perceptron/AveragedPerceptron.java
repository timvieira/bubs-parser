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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.DenseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.DenseIntVector;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.LargeBitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseIntVector;
import edu.ohsu.cslu.datastructs.vectors.LargeVector;
import edu.ohsu.cslu.datastructs.vectors.MutableSparseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.MutableSparseIntVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.parser.Util;

/**
 * Represents an averaged perceptron (see Collins, 2002). The model should be trained with
 * {@link #train(int, SparseBitVector)}, and applied with {@link #classify(Vector)}.
 * 
 * NOTE: clients are responsible to include their own bias feature in the training and testing instances, meaning that
 * there is a single feature which is always on (value=1) for all instances.
 * 
 * @author Aaron Dunlop, Nathan Bodenstab
 * @since Oct 12, 2010
 */
public class AveragedPerceptron extends Perceptron {

    private static final long serialVersionUID = 1L;

    private FloatVector[] avgWeights = null;
    private IntVector lastAveraged = null; // same for every model since we update all at once
    private int lastExampleAllUpdated = 0;

    public AveragedPerceptron() {
        this(0.25f, new ZeroOneLoss(), "0", null, null);
    }

    public AveragedPerceptron(final LossFunction lossFunction, final int classes, final long features) {
        this(0.25f, lossFunction, classes, features);
    }

    public AveragedPerceptron(final float learningRate, final LossFunction lossFunction, final int classes,
            final long features) {
        super(learningRate, lossFunction, classes, features);
        this.avgWeights = new FloatVector[classes];

        if (features <= MAX_DENSE_STORAGE_SIZE) {
            for (int i = 0; i < classes; i++) {
                this.avgWeights[i] = new DenseFloatVector(features);
            }
            this.lastAveraged = new DenseIntVector(features, 0);

        } else if (features <= Integer.MAX_VALUE) {
            for (int i = 0; i < classes; i++) {
                this.avgWeights[i] = new MutableSparseFloatVector(features);
            }
            this.lastAveraged = new MutableSparseIntVector(features);

        } else {
            for (int i = 0; i < classes; i++) {
                this.avgWeights[i] = new LargeSparseFloatVector(features);
            }
            this.lastAveraged = new LargeSparseIntVector(features);
        }
    }

    public AveragedPerceptron(final int classes, final long features) {
        this(new ZeroOneLoss(), classes, features);
    }

    public AveragedPerceptron(final float learningRate, final LossFunction lossFunction, final String binsStr,
            final String featureTemplate, final float[] initialWeights) {
        super(learningRate, lossFunction, binsStr, featureTemplate, initialWeights);
    }

    public AveragedPerceptron(final BufferedReader modelFileReader) {
        try {
            this.readModel(modelFileReader);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        this.lossFunction = null;
        this.rawWeights = null;
        this.learningRate = 0;
        this.trainExampleNumber = 0;

        bias = new float[numClasses()];
        Arrays.fill(bias, 0.0f); // default to no bias
    }

    @Override
    protected void initModel(final float[] initialWeights) {
        super.initModel(initialWeights);

        avgWeights = new FloatVector[numClasses()];
        for (int i = 0; i < numClasses(); i++) {
            avgWeights[i] = new DenseFloatVector(initialWeights.clone());
        }
        lastAveraged = new DenseIntVector(initialWeights.length, 0);
    }

    /**
     * Returns the class output of the averaged perceptron model for the specified feature vector.
     * 
     * @param featureVector
     * @return the binary output of the averaged perceptron model for the specified feature vector.
     */
    @Override
    public int classify(final Vector featureVector) {
        // We don't need to rely on the user to update the final model since we can
        // keep track of it ourself. update() is only called for *incorrect* classifications
        // so if we run through additional *correct* training examples, we need to re-average
        // the model.
        // NOTE: also need to do this when writing the model
        if (lastExampleAllUpdated < trainExampleNumber) {
            averageAllFeatures();
        }
        return classify(avgWeights, featureVector);
    }

    /**
     * Returns the class output and probability score of the averaged perceptron model for the specified feature vector.
     * 
     * @param featureVector
     * @return the classification and confidence score
     */
    public ScoredClassification scoredClassify(final Vector featureVector) {
        // We don't need to rely on the user to update the final model since we can
        // keep track of it ourself. update() is only called for *incorrect* classifications
        // so if we run through additional *correct* training examples, we need to re-average
        // the model.
        // NOTE: also need to do this when writing the model
        if (lastExampleAllUpdated < trainExampleNumber) {
            averageAllFeatures();
        }
        int bestClass = -1;
        float bestScore = Float.NEGATIVE_INFINITY, secondBestScore = Float.NEGATIVE_INFINITY;
        final float[] scores = new float[avgWeights.length];
        for (int i = 0; i < avgWeights.length; i++) {
            scores[i] = featureVector.dotProduct(avgWeights[i]) + bias[i] + 0f;
            if (scores[i] > bestScore) {
                secondBestScore = bestScore;
                bestScore = scores[i];
                bestClass = i;
            } else if (secondBestScore == Float.NEGATIVE_INFINITY) {
                secondBestScore = scores[i];
            }
        }
        double expSum = 0;
        for (int i = 0; i < scores.length; i++) {
            expSum += Math.exp(scores[i] - bestScore);
        }
        return new ScoredClassification(bestClass, (float) (1.0f / expSum), bestScore - secondBestScore);
    }

    /**
     * Returns the probability score of the averaged perceptron model for the specified feature vector and class. Used
     * when the class is constrained by outside rules, but the score for that class is required.
     * 
     * @param featureVector
     * @return the classification and confidence score
     */
    public ScoredClassification scoredClassify(final Vector featureVector, final int constrainingClass) {
        // We don't need to rely on the user to update the final model since we can
        // keep track of it ourself. update() is only called for *incorrect* classifications
        // so if we run through additional *correct* training examples, we need to re-average
        // the model.
        // NOTE: also need to do this when writing the model
        if (lastExampleAllUpdated < trainExampleNumber) {
            averageAllFeatures();
        }
        float constrainingScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < avgWeights.length; i++) {
            // The derived probability of a classification is the logistic of the averaged score
            final float score = edu.ohsu.cslu.util.Math.logistic(.05f, featureVector.dotProduct(avgWeights[i])
                    + bias[i]);
            if (i == constrainingClass) {
                constrainingScore = score;
            }
        }
        return new ScoredClassification(constrainingClass, constrainingScore, 0);
    }

    @Override
    protected void update(final int goldClass, final int guessClass, final float alpha, final BitVector featureVector,
            final int example) {

        final FloatVector avgGuess = avgWeights[guessClass];
        final FloatVector avgGold = avgWeights[goldClass];
        final FloatVector rawGold = rawWeights[goldClass];
        final FloatVector rawGuess = rawWeights[guessClass];

        // l = last-averaged example
        // e = current example
        // A_l = averaged weight at l
        // R_l = raw weight at l
        // A_e = Averaged weight at e = (A_l * l + R_l * e - R_l * l + alpha) / e

        // Update averaged weights first
        if (lastAveraged instanceof LargeVector) {

            // Cast all important vectors to LargeVector versions
            final LargeSparseIntVector largeLastAveraged = (LargeSparseIntVector) lastAveraged;
            final LargeVector largeAvgGold = (LargeVector) avgGold;
            final LargeSparseFloatVector largeRawGold = (LargeSparseFloatVector) rawGold;
            final LargeVector largeAvgGuess = (LargeVector) avgGuess;
            final LargeSparseFloatVector largeRawGuess = (LargeSparseFloatVector) rawGuess;

            for (final long featIndex : ((LargeBitVector) featureVector).longValues()) {

                final int l = largeLastAveraged.getInt(featIndex); // default=0

                // Upweight gold class weights
                final float goldA_l = largeAvgGold.getFloat(featIndex);
                final float goldR_l = largeRawGold.getFloat(featIndex);
                final float goldA_e = ((goldA_l - goldR_l) * l + alpha) / example + goldR_l;
                largeAvgGold.set(featIndex, goldA_e);

                // Downweight guess class weights
                final float guessA_l = largeAvgGuess.getFloat(featIndex);
                final float guessR_l = largeRawGuess.getFloat(featIndex);
                final float guessA_e = ((guessA_l - guessR_l) * l - alpha) / example + guessR_l;
                largeAvgGuess.set(featIndex, guessA_e);

                // Update last-averaged
                largeLastAveraged.set(featIndex, example);
            }

        } else {

            for (final int featIndex : ((SparseBitVector) featureVector).elements()) {

                // TODO Fix
                final int l = lastAveraged.getInt(featIndex); // default=0

                // Upweight gold class weights
                final float goldA_l = avgGold.getFloat(featIndex);
                final float goldR_l = rawGold.getFloat(featIndex);
                final float goldA_e = ((goldA_l - goldR_l) * l + alpha) / example + goldR_l;
                avgGold.set(featIndex, goldA_e);

                // Downweight guess class weights
                final float guessA_l = avgGuess.getFloat(featIndex);
                final float guessR_l = rawGuess.getFloat(featIndex);
                final float guessA_e = ((guessA_l - guessR_l) * l - alpha) / example + guessR_l;
                avgGuess.set(featIndex, guessA_e);

                // Update last-averaged
                lastAveraged.set(featIndex, example);
            }
        }

        // And now raw weights
        rawGold.inPlaceAdd(featureVector, alpha);
        rawGuess.inPlaceAdd(featureVector, -alpha);
    }

    private void averageAllFeatures() {

        if (lastAveraged instanceof LargeVector) {
            final LargeVector largeLastAveraged = (LargeVector) lastAveraged;
            for (final long featIndex : lastAveraged.populatedDimensions()) {

                final int lastAvgExample = largeLastAveraged.getInt(featIndex); // default=0

                if (lastAvgExample < trainExampleNumber) {
                    for (int i = 0; i < avgWeights.length; i++) {

                        // all values between lastAvgExample and example are assumed to be unchanged
                        final float oldAvgValue = ((LargeVector) avgWeights[i]).getFloat(featIndex);
                        final float diff = ((LargeVector) rawWeights[i]).getFloat(featIndex) - oldAvgValue;

                        if (diff != 0) {
                            final float avgUpdate = diff * (trainExampleNumber - lastAvgExample) / trainExampleNumber;
                            ((LargeVector) avgWeights[i]).set(featIndex, oldAvgValue + avgUpdate);
                        }
                    }
                    largeLastAveraged.set(featIndex, trainExampleNumber);
                }
            }
        } else {
            for (final long featIndex : rawWeights[0].populatedDimensions()) {
                final int intFeatIndex = (int) featIndex;
                final int lastAvgExample = lastAveraged.getInt(intFeatIndex); // default=0

                if (lastAvgExample < trainExampleNumber) {
                    for (int i = 0; i < avgWeights.length; i++) {

                        // all values between lastAvgExample and example are assumed to be unchanged
                        final float oldAvgValue = avgWeights[i].getFloat(intFeatIndex);
                        final float diff = rawWeights[i].getFloat(intFeatIndex) - oldAvgValue;

                        if (diff != 0) {
                            final float avgUpdate = diff * (trainExampleNumber - lastAvgExample) / trainExampleNumber;
                            avgWeights[i].set(intFeatIndex, oldAvgValue + avgUpdate);
                        }
                    }
                    lastAveraged.set(intFeatIndex, trainExampleNumber);
                }
            }
        }

        // manually record when we last updated all features. Check during
        // classification and model writing to ensure model is up-to-date
        lastExampleAllUpdated = trainExampleNumber;
    }

    @Override
    public FloatVector modelWeights(final int modelIndex) {
        return avgWeights[modelIndex];
    }

    public FloatVector rawModelWeights(final int modelIndex) {
        return rawWeights[modelIndex];
    }

    @Override
    public String toString() {
        return modelToString(avgWeights);
    }

    // # === Perceptron Model ===
    // numFeats=98695 numClasses=4 numTrainExamples=286048860
    // featTemplate: loc lt lt+1 lt+2 lt-1 lt-2 lt_lt-1 rt rt+1 rt+2 rt-1 rt-2 lw lw-1 rw rw+1
    // vector type=float length=98695
    // -578275.375000 -1054786.875000 -2024595.125000 -1110648.125000 ...
    // [blank]
    // vector type=float length=X
    // -Y Z ...
    // ...

    // NOTE: this doesn't work with the raw Perceptron model, but we should probably
    // change it so it does. Maybe both Avg and Raw models should have this.weights
    // and the Avg model has an addition rawWeights?
    private void readModel(final BufferedReader inputReader) throws IOException {
        // final BufferedReader br = new BufferedReader(inputReader);

        // read in file until we get to the model specs
        String line = inputReader.readLine();
        while (line != null && !line.trim().equals("# === Perceptron Model ===")) {
            line = inputReader.readLine();
        }
        if (line == null) {
            throw new RuntimeException("Unexpected EOF found in AveragedPerceptron model.  Exiting.");
        }

        // for (String line = inputReader.readLine(); line != null &&
        // !line.trim().equals("# === Perceptron Model ===");
        // line = inputReader.readLine()) {
        String[] tokens = inputReader.readLine().split("\\s");
        final int numFeatures = Integer.parseInt(tokens[0].split("=")[1]);
        // this.numClasses = Integer.parseInt(tokens[1].split("=")[1]);
        final String binsString = tokens[2].split("=")[1];
        // final String biasString = tokens[3].split("=")[1];
        // this.initBins(binsString);
        this.binsStr = binsString;
        this.bins = Util.strToIntArray(binsStr);
        this.avgWeights = new FloatVector[numClasses()];
        this.featureTemplate = inputReader.readLine().replace("featTemplate: ", "");

        for (int classIndex = 0; classIndex < numClasses(); classIndex++) {
            inputReader.readLine(); // vector type=float length=X
            tokens = inputReader.readLine().split("\\s"); // get float values for model[i]
            final float[] weights = new float[numFeatures];
            for (int i = 0; i < tokens.length; i++) {
                weights[i] = Float.parseFloat(tokens[i]);
            }
            this.avgWeights[classIndex] = new DenseFloatVector(weights);

            // if (classIndex != numClasses() - 1) {
            inputReader.readLine(); // blank line
            // }
        }
    }

    /**
     * Represents the class assigned by this {@link Classifier} and a score in the range 0..1
     */
    public static class ScoredClassification {
        public final int classification;
        public final float score;
        public final float margin;

        public ScoredClassification(final int classification, final float score, final float margin) {
            super();
            this.classification = classification;
            this.score = score;
            this.margin = margin;
        }
    }
}
