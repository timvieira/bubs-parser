package edu.ohsu.cslu.perceptron;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.parser.ParserUtil;

/**
 * Represents an averaged perceptron (see Collins, 2002). The model should be trained with
 * {@link #train(int, SparseBitVector)}, and applied with {@link #classify(Vector)}.
 * 
 * NOTE: clients are responsible to include their own bias feature in the training and testing instances, meaning that
 * there is a single feature which is always on (value=1) for all instances.
 * 
 * @author Aaron Dunlop, Nathan Bodenstab
 * @since Oct 12, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class AveragedPerceptron extends Perceptron {
    private FloatVector[] avgWeights = null;
    private IntVector lastAveraged; // same for every model since we update all at once
    private int lastExampleAllUpdated = 0;

    public AveragedPerceptron() {
        this(0.1f, new ZeroOneLoss(), "0", null, null);
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

        this.lastAveraged = null;
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
            avgWeights[i] = new FloatVector(initialWeights.clone());
        }
        lastAveraged = new IntVector(initialWeights.length, 0);
    }

    /**
     * Returns the binary output of the averaged perceptron model for the specified feature vector.
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

    @Override
    protected void update(final int goldClass, final float alpha, final SparseBitVector featureVector, final int example) {
        float newAvg, oldAvgValue, oldRawValue, newRawValue;
        for (final int featIndex : featureVector.elements()) {
            final int lastAvgExample = lastAveraged.getInt(featIndex); // default=0
            if (lastAvgExample < example) {
                for (int i = 0; i < numClasses(); i++) {
                    // all values between lastAvgExample and example-1 are assumed to be unchanged
                    oldAvgValue = avgWeights[i].getFloat(featIndex);
                    oldRawValue = rawWeights[i].getFloat(featIndex);

                    if (goldClass == i) {
                        newRawValue = oldRawValue + alpha;
                    } else {
                        newRawValue = oldRawValue - alpha;
                    }

                    if (lastAvgExample == 0) {
                        newAvg = newRawValue / example;
                    } else {
                        final int numExamplesRawUnchanged = example - lastAvgExample - 1;
                        newAvg = (oldAvgValue * lastAvgExample + oldRawValue * numExamplesRawUnchanged + newRawValue * 1)
                                / example;
                    }
                    avgWeights[i].set(featIndex, newAvg);
                    rawWeights[i].set(featIndex, newRawValue);
                }
                lastAveraged.set(featIndex, example);
            }
        }
    }

    private void averageAllFeatures() {
        final boolean[] featsToUpdate = new boolean[rawWeights[0].length()];
        Arrays.fill(featsToUpdate, true);
        update(0, 0, new SparseBitVector(featsToUpdate), trainExampleNumber);

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

        // for (String line = inputReader.readLine(); line != null && !line.trim().equals("# === Perceptron Model ===");
        // line = inputReader.readLine()) {
        String[] tokens = inputReader.readLine().split("\\s");
        final int numFeatures = Integer.parseInt(tokens[0].split("=")[1]);
        // this.numClasses = Integer.parseInt(tokens[1].split("=")[1]);
        final String binsString = tokens[2].split("=")[1];
        // final String biasString = tokens[3].split("=")[1];
        // this.initBins(binsString);
        this.binsStr = binsString;
        this.bins = ParserUtil.strToIntArray(binsStr);
        this.avgWeights = new FloatVector[numClasses()];
        this.featureTemplate = inputReader.readLine().replace("featTemplate: ", "");

        for (int classIndex = 0; classIndex < numClasses(); classIndex++) {
            inputReader.readLine(); // vector type=float length=X
            tokens = inputReader.readLine().split("\\s"); // get float values for model[i]
            final float[] weights = new float[numFeatures];
            for (int i = 0; i < tokens.length; i++) {
                weights[i] = Float.parseFloat(tokens[i]);
            }
            this.avgWeights[classIndex] = new FloatVector(weights);

            // if (classIndex != numClasses() - 1) {
            inputReader.readLine(); // blank line
            // }
        }
    }

    // /**
    // * Update weights for all features found in the specified feature vector by the specified alpha
    // *
    // * @param featureVector Features to update
    // * @param alpha Update amount (generally positive for positive examples and negative for negative examples)
    // * @param example The number of examples seen in the training corpus (i.e., the index of the example which caused
    // * this update, 1-indexed).
    // */
    // private void update2(final SparseBitVector featureVector, final float alpha, final int example) {
    //
    // // if (example <= lastUpdate) {
    // // throw new IllegalArgumentException("Model updated at example " + lastUpdate
    // // + " (more recently than requested example " + example + ")");
    // // }
    // // lastUpdate = example;
    //
    // // Update the averaged model
    // final int[] features = featureVector.elements();
    // if (example == 1) {
    // averagedPerceptron.inPlaceAdd(featureVector, alpha);
    // lastAveraged.inPlaceAdd(featureVector, 1);
    // } else {
    // for (final int feature : features) {
    // final int lastAvgExample = lastAveraged.getInt(feature);
    // final float currentAverage = averagedPerceptron.getFloat(feature);
    // // Average up to the previous example
    // final float update = (rawPerceptron.getFloat(feature) - currentAverage)
    // * (example - lastAvgExample - 1) / (example - 1);
    // averagedPerceptron.set(feature, currentAverage + update);
    // lastAveraged.set(feature, example - 1);
    // }
    // }
    // // Update the raw perceptron
    // rawPerceptron.inPlaceAdd(featureVector, alpha);
    // }
    //
    // /**
    // * Compute averaged weights for any features which have been updated in the raw perceptron and not subsequently
    // * averaged. This method should be called following training and prior to testing.
    // *
    // * @param totalExamples The number of training examples seen
    // */
    // public void updateAveragedModel2() {
    //
    // final int totalExamples = trainExampleNumber;
    //
    // for (int feature = 0; feature < rawPerceptron.length(); feature++) {
    // final int la = lastAveraged.getInt(feature);
    // final float currentAverage = averagedPerceptron.getFloat(feature);
    // // Average up to the current example
    // final float update = (rawPerceptron.getFloat(feature) - currentAverage) * (totalExamples - la)
    // / totalExamples;
    // averagedPerceptron.set(feature, currentAverage + update);
    // }
    // lastAveraged.fill(totalExamples);
    // }
}