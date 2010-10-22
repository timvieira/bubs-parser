package edu.ohsu.cslu.classifier;

// http://code.google.com/p/python-perceptron/source/checkout
// LingPipe http://alias-i.com/lingpipe/docs/api/com/aliasi/classify/PerceptronClassifier.html
// LingPipe download = http://alias-i.com/lingpipe/web/download.html

public class Perceptron {

    private float learningRate, weights[];
    private int numFeatures, numClasses;

    public Perceptron(final int numFeatures) {
        // add option for different kernels?
        assert numFeatures >= 1;
        // assert numClasses >= 2; // assuming 2 classes
        this.numClasses = 2;

        this.numFeatures = numFeatures;
        this.weights = new float[numFeatures];
        this.learningRate = 1;

        // Initialize weights to zero
        for (int i = 0; i < numFeatures; i++) {
            weights[i] = 0;
        }
    }

    public float[] getWeights() {
        return weights;
    }

    public void decreaseLearningRate(final float percentage) {
        this.learningRate = this.learningRate * percentage;
    }

    public void decreaseLearningRate() {
        decreaseLearningRate(0.95f);
    }

    public static float kernelDotProduct(final float[] a, final float[] b) {
        assert a.length == b.length;

        float result = 0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }

    public int classify(final float[] inputFeatures) {
        int bestClass = -1;
        float score, bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < numClasses; i++) {
            score = kernelDotProduct(inputFeatures, weights);
            if (score > bestScore) {
                bestScore = score;
                bestClass = i;
            }
        }
        return bestClass;
    }

    // This loss function is used for predicting the beam-width within a
    // chart cell. If the predicted width is smaller than the gold value,
    // then we should incur a large loss because the gold tree will be impossible
    // to construct. If the predicted beam-width is larger than the gold, that
    // isn't as bad, and should only incur a small loss.
    public float beamWidthLoss(final int guessClass, final int goldClass) {
        if (guessClass == goldClass) {
            return 0;
        }
        if (goldClass == 1) {
            return 10; // cell has gold chart entry but beam-width prediction was 0
        }
        return 1;
    }

    public float zeroOneLoss(final int guessClass, final int goldClass) {
        if (guessClass == goldClass) {
            return 0;
        }
        return 1;
    }

    public float score(final float[] inputFeatures) {
        return kernelDotProduct(inputFeatures, weights);
    }

    public void learnOnline(final float[] inputFeatures, final int goldClass) {
        final int guessClass = classify(inputFeatures);
        final float loss = zeroOneLoss(guessClass, goldClass);
        if (loss != 0) {
            for (int i = 0; i < numFeatures; i++) {
                weights[i] -= loss * inputFeatures[i] * learningRate;
            }
        }
    }

    // public void learnOnline(final float[] gradient) {
    // for (int i = 0; i < numFeatures; i++) {
    // weights[i] -= gradient[i] * learningRate;
    // }
    // }

    public int numFeatures() {
        return numFeatures;
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < numFeatures; i++) {
            s += weights[i] + " ";
        }
        return s;
    }
}
