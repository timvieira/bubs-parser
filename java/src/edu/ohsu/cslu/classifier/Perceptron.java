package edu.ohsu.cslu.classifier;

// http://code.google.com/p/python-perceptron/source/checkout
// LingPipe http://alias-i.com/lingpipe/docs/api/com/aliasi/classify/PerceptronClassifier.html
// LingPipe download = http://alias-i.com/lingpipe/web/download.html

public class Perceptron {

    private double learningRate, weights[];
    private int numFeatures, numClasses;

    public Perceptron(final int numFeatures) {
        // add option for different kernels?
        assert numFeatures >= 1;
        // assert numClasses >= 2; // assuming 2 classes

        this.numFeatures = numFeatures;
        // this.numClasses = numClasses;
        this.weights = new double[numFeatures];
        this.learningRate = 1.0;

        for (int i = 0; i < numFeatures; i++) {
            weights[i] = 0;
        }
    }

    public double[] getWeights() {
        return weights;
    }

    public static double kernelDotProduct(final double[] a, final double[] b) {
        assert a.length == b.length;

        double result = 0.0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }

    public int classify(final double[] inputFeatures) {
        int bestClass = -1;
        double score, bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < numClasses; i++) {
            score = kernelDotProduct(inputFeatures, weights);
            if (score > bestScore) {
                bestScore = score;
                bestClass = i;
            }
        }
        return bestClass;
    }

    public void learnOnline(final double[] inputFeatures, final int correctClass) {
        final int guessClass = classify(inputFeatures);
        if (guessClass != correctClass) {
            for (int i = 0; i < numFeatures; i++) {
                weights[i] += inputFeatures[i] * learningRate;
                weights[i] -= inputFeatures[i] * learningRate;
            }
        }
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < numClasses; i++) {
            s += "weights[" + i + "] = " + weights[i] + "\n";
        }
        return s;
    }
}
