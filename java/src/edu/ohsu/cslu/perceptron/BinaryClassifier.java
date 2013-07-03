/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
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
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.Perceptron.LossFunction;

/**
 * Base class for binary classifiers (e.g. {@link CompleteClosureClassifier}. Includes a lot of code duplicated from
 * {@link AveragedPerceptron}, since binary classification only needs one set of weights, and a single bias.
 * 
 * @author Aaron Dunlop
 */
public abstract class BinaryClassifier<S extends BinarySequence> extends ClassifierTool<S> {

    private static final long serialVersionUID = 1L;

    @Option(name = "-b", metaVar = "bias", usage = "Biased training penalty for incorrect positive classification (to correct for imbalanced training data or downstream cost) - ratio 1:<bias>")
    protected volatile float negativeTrainingBias = 1f;

    @Option(name = "-tp", metaVar = "precision", requires = "-d", choiceGroup = "target", usage = "Target dev-set precision. If specified, after training a binary search will be performed to find the optimal bias")
    protected volatile float targetPrecision = 0f;

    @Option(name = "-tnr", metaVar = "recall", requires = "-d", choiceGroup = "target", usage = "Target dev-set negative-classification recall")
    protected volatile float targetNegativeRecall = 0f;

    @Option(name = "-lr", metaVar = "rate", usage = "Learning rate")
    private volatile float learningRate = 0.1f;

    /** Threshold above which we'll represent weight vectors sparsely */
    private static int MAX_DENSE_STORAGE_SIZE = 100 * 1024;

    /** Model parameters */
    protected FloatVector avgWeights = null;

    /**
     * Model bias. Learned in {@link #precisionBiasSearch(ArrayList, FeatureExtractor)} or
     * {@link #negativeRecallBiasSearch(ArrayList, FeatureExtractor)}.
     */
    protected float bias = 0;

    // Transient fields used only during training
    private transient FloatVector rawWeights = null;
    private transient IntVector lastAveraged = null;
    private transient int lastExampleAllUpdated = 0;

    private transient int trainExampleNumber = 0;
    private transient LossFunction lossFunction;

    protected SymbolSet<String> vocabulary;

    @Override
    public void finalizeMaps() {
        super.finalizeMaps();
        vocabulary.finalize();
    }

    @Override
    protected void readModel(final InputStream is) throws IOException, ClassNotFoundException {
        // Read in the model parameters as a temporary java serialized object and copy into this object
        final ObjectInputStream ois = new ObjectInputStream(is);
        final Model tmp = (Model) ois.readObject();
        ois.close();
        this.featureTemplates = tmp.featureTemplates;
        this.lexicon = tmp.lexicon;
        this.decisionTreeUnkClassSet = tmp.unkClassSet;
        this.vocabulary = tmp.vocabulary;
        this.avgWeights = tmp.avgWeights;
        this.bias = tmp.bias;
        is.close();
    }

    /**
     * Executes a single training step
     * 
     * @param goldClass
     * @param featureVector
     */
    protected void train(final boolean goldClass, final BitVector featureVector) {
        if (rawWeights == null) {
            // We need to initialize a new model; we depend on the FeatureExtractor to provide a vector of appropriate
            // length
            final long vectorLength = featureVector.length();
            if (vectorLength <= MAX_DENSE_STORAGE_SIZE) {
                this.rawWeights = new DenseFloatVector(vectorLength);
                this.avgWeights = new DenseFloatVector(vectorLength);
                this.lastAveraged = new DenseIntVector(vectorLength, 0);

            } else if (vectorLength <= Integer.MAX_VALUE) {
                this.rawWeights = new MutableSparseFloatVector(vectorLength);
                this.avgWeights = new MutableSparseFloatVector(vectorLength);
                this.lastAveraged = new MutableSparseIntVector(vectorLength);

            } else {
                this.rawWeights = new LargeSparseFloatVector(vectorLength);
                this.avgWeights = new LargeSparseFloatVector(vectorLength);
                this.lastAveraged = new LargeSparseIntVector(vectorLength);
            }
            this.lossFunction = new Perceptron.BiasedLoss(new float[] { negativeTrainingBias, 1 });
        }

        trainExampleNumber++;
        final float dotProduct = featureVector.dotProduct(rawWeights);
        final boolean classification = dotProduct >= 0;
        if (classification != goldClass) {
            final float loss = lossFunction.computeLoss(goldClass ? 1 : 0, goldClass ? 0 : 1);
            final float alpha = goldClass ? (loss * learningRate) : (-loss * learningRate);
            update(goldClass, featureVector, alpha);
        }
    }

    private boolean classify(final BitVector featureVector) {
        if (lastExampleAllUpdated < trainExampleNumber) {
            averageAllFeatures();
        }
        // TODO Remove this after debugging is complete
        // if (BaseLogger.singleton().isLoggable(Level.FINEST)) {
        // BaseLogger.singleton().finest(featureVector.toString());
        // }
        return avgWeights.dotProduct(featureVector) >= bias;
    }

    /**
     * Classifies a single entry in the specified sequence
     * 
     * @param sequence
     * @param index
     * @return Boolean classification of the specified entry in <code>sequence</code>
     */
    public boolean classify(final S sequence, final int index) {
        return classify(featureExtractor.featureVector(sequence, index));
    }

    protected void classify(final S sequence, final int index, final BinaryClassifierResult result) {

        final BitVector featureVector = featureExtractor.featureVector(sequence, index);
        sequence.predictedClasses[index] = classify(featureVector);

        if (sequence.predictedClasses[index] == true) {
            result.classifiedPositive++;
        } else {
            result.classifiedNegative++;
        }

        if (sequence.classes[index] == true) {
            result.positiveExamples++;
            if (sequence.predictedClasses[index]) {
                result.correctPositive++;
            }
        } else {
            result.negativeExamples++;
            if (!sequence.predictedClasses[index]) {
                result.correctNegative++;
            }
        }
    }

    /**
     * Classifies the supplied sequences
     * 
     * @param sequences
     * @return results of classifying the input sequences (if they contain gold classifications)
     */
    protected BinaryClassifierResult classify(final ArrayList<S> sequences) {

        final long t0 = System.currentTimeMillis();
        final BinaryClassifierResult result = new BinaryClassifierResult();

        for (final S sequence : sequences) {
            result.totalSequences++;

            for (int i = 0; i < sequence.predictedClasses.length; i++) {
                classify(sequence, i, result);
            }
        }
        result.time = System.currentTimeMillis() - t0;
        return result;
    }

    protected void update(final boolean goldClass, final BitVector featureVector, final float alpha) {

        // l = last-averaged example
        // e = current example
        // a_l = averaged weight at l
        // r_l = raw weight at l
        // a_e = Averaged weight at e = (a_l * l + r_l * e - r_l * l + alpha) / e

        // Update averaged weights first
        if (lastAveraged instanceof LargeVector) {

            // Cast all important vectors to LargeVector versions
            final LargeSparseIntVector largeLastAveraged = (LargeSparseIntVector) lastAveraged;
            final LargeVector largeAvg = (LargeVector) avgWeights;
            final LargeSparseFloatVector largeRaw = (LargeSparseFloatVector) rawWeights;

            for (final long featIndex : ((LargeBitVector) featureVector).longValues()) {

                final int l = largeLastAveraged.getInt(featIndex); // default=0

                // Update averaged weights
                final float a_l = largeAvg.getFloat(featIndex);
                final float r_l = largeRaw.getFloat(featIndex);
                final float a_e = ((a_l - r_l) * l + alpha) / trainExampleNumber + r_l;
                largeAvg.set(featIndex, a_e);

                // Update last-averaged
                largeLastAveraged.set(featIndex, trainExampleNumber);
            }

        } else {

            for (final int featIndex : ((SparseBitVector) featureVector).elements()) {

                final int l = lastAveraged.getInt(featIndex); // default=0

                final float a_l = avgWeights.getFloat(featIndex);
                final float r_l = rawWeights.getFloat(featIndex);
                final float a_e = ((a_l - r_l) * l + alpha) / trainExampleNumber + r_l;
                avgWeights.set(featIndex, a_e);

                // Update last-averaged
                lastAveraged.set(featIndex, trainExampleNumber);
            }
        }

        // And now raw weights
        rawWeights.inPlaceAdd(featureVector, alpha);
    }

    private void averageAllFeatures() {

        if (lastAveraged instanceof LargeVector) {
            final LargeVector largeLastAveraged = (LargeVector) lastAveraged;
            for (final long featIndex : lastAveraged.populatedDimensions()) {

                final int lastAvgExample = largeLastAveraged.getInt(featIndex); // default=0

                if (lastAvgExample < trainExampleNumber) {
                    // all values between lastAvgExample and example are assumed to be unchanged
                    final float oldAvgValue = ((LargeVector) avgWeights).getFloat(featIndex);
                    final float diff = ((LargeVector) rawWeights).getFloat(featIndex) - oldAvgValue;

                    if (diff != 0) {
                        final float avgUpdate = diff * (trainExampleNumber - lastAvgExample) / trainExampleNumber;
                        ((LargeVector) avgWeights).set(featIndex, oldAvgValue + avgUpdate);
                    }
                    largeLastAveraged.set(featIndex, trainExampleNumber);
                }
            }
        } else {
            for (final long featIndex : lastAveraged.populatedDimensions()) {
                final int intFeatIndex = (int) featIndex;
                final int lastAvgExample = lastAveraged.getInt(intFeatIndex); // default=0

                if (lastAvgExample < trainExampleNumber) {
                    // all values between lastAvgExample and example are assumed to be unchanged
                    final float oldAvgValue = avgWeights.getFloat(intFeatIndex);
                    final float diff = rawWeights.getFloat(intFeatIndex) - oldAvgValue;

                    if (diff != 0) {
                        final float avgUpdate = diff * (trainExampleNumber - lastAvgExample) / trainExampleNumber;
                        avgWeights.set(intFeatIndex, oldAvgValue + avgUpdate);
                    }
                    lastAveraged.set(intFeatIndex, trainExampleNumber);
                }
            }
        }

        // manually record when we last updated all features. Check during
        // classification and model writing to ensure model is up-to-date
        lastExampleAllUpdated = trainExampleNumber;
    }

    /**
     * Performs a binary search to find a bias yielding the desired precision
     * 
     * @param devCorpusSequences
     * @param fe feature extractor
     */
    protected void precisionBiasSearch(final ArrayList<S> devCorpusSequences, final FeatureExtractor<S> fe) {

        BaseLogger.singleton().info(String.format("Performing bias search for precision %.5f", targetPrecision));

        // Short-circuit search if we're not making material changes in bias (this can happen if
        final float MIN_BIAS_DELTA = .01f;

        // Set a stopping criteria - e.g., if prec=.98, we'll find a bias that produces .979 <= p <= .981
        final float epsilon = (1 - targetPrecision) / 20;

        BinaryClassifierResult result = classify(devCorpusSequences);

        //
        // Binary search over bias settings, until we find the desired precision
        //
        float lowBias = avgWeights.min() * fe.templateCount();
        float highBias = avgWeights.max() * fe.templateCount();

        this.bias = 0;
        for (float p = result.precision(); p < targetPrecision - epsilon || p > targetPrecision + epsilon
                || Float.isNaN(p);) {

            final float prevBias = bias;
            bias = lowBias + (highBias - lowBias) / 2;

            // Exit if we're not changing bias measurably
            if (Math.abs(bias - prevBias) < MIN_BIAS_DELTA) {
                BaseLogger.singleton().info(String.format("Converged at bias=%.5f", bias));
                break;
            }

            // Classify the dev-set
            result = classify(devCorpusSequences);
            BaseLogger.singleton().info(
                    String.format("Bias=%.5f, P=%.5f  R=%.5f  neg-P=%.5f  neg-R=%.5f  Accuracy=%.2f", bias,
                            result.precision() * 100f, result.recall() * 100f, result.negativePrecision() * 100f,
                            result.negativeRecall() * 100f, result.accuracy() * 100f));

            p = result.precision();

            if (p < targetPrecision) {
                lowBias = bias;
            } else {
                highBias = bias;
            }
        }
    }

    /**
     * Performs a binary search to find a bias yielding the desired negative-class precision
     * 
     * @param devCorpusSequences
     * @param fe feature extractor
     */
    protected void negativeRecallBiasSearch(final ArrayList<S> devCorpusSequences, final FeatureExtractor<S> fe) {

        BaseLogger.singleton().info(
                String.format("Performing bias search for negative-classification recall %.5f", targetNegativeRecall));

        // Short-circuit search if we're not making material changes in bias (this can happen if
        final float MIN_BIAS_DELTA = .01f;

        // Set a stopping criteria - e.g., if recall=.98, we'll find a bias that produces .979 <= r <= .981
        final float epsilon = (1 - targetNegativeRecall) / 20;

        BinaryClassifierResult result = classify(devCorpusSequences);

        //
        // Binary search over bias settings, until we find the desired recall
        //
        float lowBias = avgWeights.min() * fe.templateCount();
        float highBias = avgWeights.max() * fe.templateCount();

        this.bias = 0;
        for (float r = result.negativeRecall(); r < targetNegativeRecall - epsilon
                || r > targetNegativeRecall + epsilon || Float.isNaN(r);) {

            final float prevBias = bias;
            bias = lowBias + (highBias - lowBias) / 2;

            // Exit if we're not changing bias measurably
            if (Math.abs(bias - prevBias) < MIN_BIAS_DELTA) {
                BaseLogger.singleton().info(String.format("Converged at bias=%.5f", bias));
                break;
            }

            // Classify the dev-set
            result = classify(devCorpusSequences);
            BaseLogger.singleton().info(
                    String.format("Bias=%.5f, P=%.5f  R=%.5f  neg-P=%.5f  neg-R=%.5f  Accuracy=%.2f", bias,
                            result.precision() * 100f, result.recall() * 100f, result.negativePrecision() * 100f,
                            result.negativeRecall() * 100f, result.accuracy() * 100f));

            r = result.negativeRecall();

            if (r < targetNegativeRecall) {
                lowBias = bias;
            } else {
                highBias = bias;
            }
        }

        // TODO This is specific to CompleteClosureClassifier
        // Compute and report final cell-closure statistics on the development set
        int totalWords = 0;
        for (final S sequence : devCorpusSequences) {
            totalWords += sequence.mappedTokens.length;
        }

        // positiveExamples + negativeExamples + span-1 cells
        final int totalCells = result.positiveExamples + result.negativeExamples + totalWords;
        final int openCells = result.classifiedNegative + totalWords;

        BaseLogger.singleton().info(
                String.format("Open cells (including span-1): %d (%.3f%%)", openCells, openCells * 100f / totalCells));
    }

    /**
     * Represents the result of a binary classification run and computes precision, recall, etc.
     */
    protected static class BinaryClassifierResult {

        int totalSequences = 0;
        int positiveExamples = 0, classifiedPositive = 0, correctPositive = 0;
        int negativeExamples = 0, classifiedNegative = 0, correctNegative = 0;

        long time;

        public float accuracy() {
            return (correctPositive + correctNegative) * 1f / (positiveExamples + negativeExamples);
        }

        public float precision() {
            final int incorrectPositive = negativeExamples - correctNegative;
            return correctPositive * 1f / (correctPositive + incorrectPositive);
        }

        public float recall() {
            return correctPositive * 1f / positiveExamples;
        }

        public float negativePrecision() {
            final int incorrectNegative = positiveExamples - correctPositive;
            return correctNegative * 1f / (correctNegative + incorrectNegative);
        }

        public float negativeRecall() {
            return correctNegative * 1f / negativeExamples;
        }
    }

    protected static class Model extends ClassifierTool.Model {

        private static final long serialVersionUID = 1L;
        final SymbolSet<String> vocabulary;
        final FloatVector avgWeights;
        final float bias;

        protected Model(final String featureTemplates, final SymbolSet<String> lexicon,
                final SymbolSet<String> unkClassSet, final SymbolSet<String> vocabulary, final FloatVector avgWeights,
                final float bias) {
            super(featureTemplates, lexicon, unkClassSet, null);
            this.vocabulary = vocabulary;
            this.avgWeights = avgWeights;
            this.bias = bias;
        }
    }
}
