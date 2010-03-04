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
        decreaseLearningRate((float) 0.95);
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

    public float score(final float[] inputFeatures) {
        return kernelDotProduct(inputFeatures, weights);
    }

    public void learnOnline(final float[] inputFeatures, final int correctClass) {
        final int guessClass = classify(inputFeatures);
        if (guessClass != correctClass) {
            for (int i = 0; i < numFeatures; i++) {
                // weights[i] += inputFeatures[i] * learningRate;
                weights[i] -= inputFeatures[i] * learningRate;
            }
        }
    }

    public void learnOnline(final float[] gradient) {
        for (int i = 0; i < numFeatures; i++) {
            weights[i] -= gradient[i] * learningRate;
        }
    }

    public int numFeatures() {
        return numFeatures;
    }

    @Override
    public String toString() {
        String s = "";
        // for (int i = 0; i < numClasses; i++) {
        // s += "weights[" + i + "] = " + weights[i] + "\n";
        // }
        for (int i = 0; i < numFeatures; i++) {
            s += weights[i] + " ";
        }
        return s;
    }
}
