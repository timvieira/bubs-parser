package edu.ohsu.cslu.perceptron;

import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

/**
 * Represents an averaged perceptron (see Collins, 2002). The model should be trained with
 * {@link #update(SparseBitVector, float, int)}, finalized with {@link #updateAveragedModel(int)}, and applied with
 * either {@link #averagedFloatOutput(Vector)} or {@link #averagedBinaryOutput(Vector)}.
 * 
 * @author Aaron Dunlop
 * @since Oct 12, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class PerceptronModel {
    private final FloatVector rawPerceptron;
    private final FloatVector averagedPerceptron;
    private final IntVector lastAveraged;
    private int lastUpdate = 0;

    public PerceptronModel(final int features, final float initialWeight) {
        this.rawPerceptron = new FloatVector(features, initialWeight);
        this.averagedPerceptron = new FloatVector(features, initialWeight);
        this.lastAveraged = new IntVector(features, 0);
    }

    /**
     * Returns the floating-point output of the raw perceptron model for the specified feature vector.
     * 
     * @param featureVector
     * @return the floating-point output of the averaged perceptron model for the specified feature vector.
     */
    public float rawFloatOutput(final Vector featureVector) {
        return averagedPerceptron.dotProduct(featureVector);
    }

    /**
     * Returns the floating-point output of the averaged perceptron model for the specified feature vector.
     * 
     * @param featureVector
     * @return the floating-point output of the averaged perceptron model for the specified feature vector.
     */
    public float averagedFloatOutput(final Vector featureVector) {
        return averagedPerceptron.dotProduct(featureVector);
    }

    /**
     * Returns the binary output of the raw perceptron model for the specified feature vector. Convenience method;
     * equivalent to ({@link #averagedFloatOutput(Vector)} > 0).
     * 
     * @param featureVector
     * @return the binary output of the averaged perceptron model for the specified feature vector.
     */
    public boolean rawBinaryOutput(final Vector featureVector) {
        return averagedPerceptron.dotProduct(featureVector) > 0;
    }

    /**
     * Returns the binary output of the averaged perceptron model for the specified feature vector. Convenience method;
     * equivalent to ({@link #averagedFloatOutput(Vector)} > 0).
     * 
     * @param featureVector
     * @return the binary output of the averaged perceptron model for the specified feature vector.
     */
    public boolean averagedBinaryOutput(final Vector featureVector) {
        return averagedPerceptron.dotProduct(featureVector) > 0;
    }

    /**
     * Update weights for all features found in the specified feature vector by the specified alpha
     * 
     * @param featureVector Features to update
     * @param alpha Update amount (generally positive for positive examples and negative for negative examples)
     * @param example The number of examples seen in the training corpus (i.e., the index of the example which caused
     *            this update, 1-indexed).
     */
    public void update(final SparseBitVector featureVector, final float alpha, final int example) {

        if (example <= lastUpdate) {
            throw new IllegalArgumentException("Model updated at example " + lastUpdate
                    + " (more recently than requested example " + example + ")");
        }
        lastUpdate = example;

        // Update the averaged model
        final int[] features = featureVector.elements();
        if (example == 1) {
            averagedPerceptron.inPlaceAdd(featureVector, alpha);
            lastAveraged.inPlaceAdd(featureVector, 1);
        } else {
            for (final int feature : features) {
                final int la = lastAveraged.getInt(feature);
                final float currentAverage = averagedPerceptron.getFloat(feature);
                // Average up to the previous example
                final float update = (rawPerceptron.getFloat(feature) - currentAverage) * (example - la - 1)
                        / (example - 1);
                averagedPerceptron.set(feature, currentAverage + update);
                lastAveraged.set(feature, example - 1);
            }
        }
        // Update the raw perceptron
        rawPerceptron.inPlaceAdd(featureVector, alpha);
    }

    /**
     * Compute averaged weights for any features which have been updated in the raw perceptron and not subsequently
     * averaged. This method should be called following training and prior to testing.
     * 
     * @param totalExamples The number of training examples seen
     */
    public void updateAveragedModel(final int totalExamples) {
        for (int feature = 0; feature < rawPerceptron.length(); feature++) {
            final int la = lastAveraged.getInt(feature);
            final float currentAverage = averagedPerceptron.getFloat(feature);
            // Average up to the current example
            final float update = (rawPerceptron.getFloat(feature) - currentAverage) * (totalExamples - la)
                    / totalExamples;
            averagedPerceptron.set(feature, currentAverage + update);
        }
        lastAveraged.fill(totalExamples);
    }

    /**
     * For unit testing.
     * 
     * @return The current averaged perceptron model as a {@link FloatVector}. Note that this model will not update
     *         until {@link #updateAveragedModel(int)} is called, and thus may not be up-to-date during training.
     */
    FloatVector averagedPerceptron() {
        return averagedPerceptron;
    }

    /**
     * For unit testing.
     * 
     * @param feature
     * @return raw weight for the specified feature
     */
    float rawFeatureWeight(final int feature) {
        return rawPerceptron.getFloat(feature);
    }

    /**
     * For unit testing.
     * 
     * @param feature
     * @param example
     * @return averaged weight for the specified feature at the specified example
     */
    float averagedFeatureWeight(final int feature, final int example) {
        final int la = lastAveraged.getInt(feature);
        if (la == 0) {
            return rawPerceptron.getFloat(feature);
        } else if (la > example) {
            throw new IllegalArgumentException("Feature " + feature + " updated at example " + la
                    + " (more recently than requested example " + example + ")");
        }

        final float currentAverage = averagedPerceptron.getFloat(feature);
        // Average up to the current example
        final float update = (rawPerceptron.getFloat(feature) - currentAverage) * (example - la) / example;
        return currentAverage + update;
    }
}