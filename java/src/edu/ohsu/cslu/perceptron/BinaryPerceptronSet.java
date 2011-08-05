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
import java.io.BufferedWriter;
import java.io.IOException;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.perceptron.Perceptron.LossFunction;

public class BinaryPerceptronSet extends Classifier {

    Perceptron classifiers[];
    int numClassifiers;
    String binsStr;
    boolean smallestBeamFirst = true;

    public BinaryPerceptronSet(final float learningRate, final LossFunction lossFunction, final String binsStr,
            final String featureTemplate) {

        this.binsStr = binsStr;
        bins = Util.strToIntArray(binsStr);
        numClassifiers = bins.length;
        classifiers = new Perceptron[numClassifiers];

        for (int i = 0; i < numClassifiers; i++) {
            classifiers[i] = new AveragedPerceptron(learningRate, lossFunction, "0", featureTemplate, null);
        }

        // if (ParserDriver.param1 != -1) {
        // smallestBeamFirst = false;
        // }
    }

    public BinaryPerceptronSet(final BufferedReader stream) {
        try {
            String line = stream.readLine();
            while (line != null && !line.trim().equals("# === BinaryPerceptronSet Model ===")) {
                line = stream.readLine();
            }
            final String[] tokens = stream.readLine().split("\\s");

            bins = Util.strToIntArray(tokens[0].split("=")[1]);
            BaseLogger.singleton().fine("INFO: Reading BinaryPerceptronSet with bins=" + tokens[0].split("=")[1]);
            numClassifiers = bins.length;
            classifiers = new Perceptron[numClassifiers];
            for (int i = 0; i < numClassifiers; i++) {
                BaseLogger.singleton().finer("INFO: Reading binary model " + i);
                classifiers[i] = new AveragedPerceptron(stream);
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // there are a few options for classification: (1) find first positive classification
    // in some fixed order (0,1,2,3,4,...) or (0,15,14,13,...) or (2) find the class with
    // the highest score (although I'm not sure if these should be comparable) or (3) something
    // similar to Clark et. al and sum classifier output
    @Override
    public int classify(final Vector featureVector) {

        if (classifiers[0].classify(featureVector) == 0) { // cell is open or closed
            return 0;
        }

        if (smallestBeamFirst) {
            for (int i = 1; i < numClassifiers; i++) {
                // is beam-width <= class2val(classifier[i])
                if (classifiers[i].classify(featureVector) == 0) {
                    return i;
                }
            }
            return numClasses() - 1; // last classification of beam-width <= X was false, so return largest
                                     // beam-width
        }

        for (int i = numClassifiers - 1; i > 0; i--) {
            // is beam-width > class2val(classifier[i])
            if (classifiers[i].classify(featureVector) == 1) {
                return i + 1; // the beam-width must be larger than this bin, so return next highest bin
            }
        }
        return 1; // last classification said it's <= 1 ... must be 1
    }

    // does it make sense for a closed cell to have beam-width 0? open/closed seems like
    // a different problem than beam-width confidence ...
    // I guess what it comes down to is if we do a decision tree-like classification: (1) open
    // or closed then (2) beam-width OR if we include the closed cells in every classification step
    @Override
    public void train(final int goldClass, final SparseBitVector featureVector) {

        for (int i = 0; i < numClassifiers; i++) {
            classifiers[i].train(goldClass <= i ? 0 : 1, featureVector);
        }

        // don't train on closed cells for >=1 bins
        // for (int i = 0; i < numClassifiers; i++) {
        // if (i == 0 || goldClass > 0) {
        // classifiers[i].train(goldClass <= bins[i] ? 0 : 1, featureVector);
        // }
        // }

        // don't train on examples that are pruned in a lower class (ie. if bins 0,5,10,15
        // then 10 will train on all examples w/ goldClass > 5
        // for (int i = 0; i < numClassifiers; i++) {
        // //if (i == 0 || goldClass > bins[i - 1]) {
        // // classifiers[i].train(goldClass <= bins[i] ? 0 : 1, featureVector);
        // //}
        // if (goldClass >= i) {
        // classifiers[i].train(goldClass <= i ? 0 : 1, featureVector);
        // }
        // }
    }

    @Override
    public void setBias(final String biasString) {
        final String[] tokens = biasString.split(",");
        if (tokens.length != numClassifiers) {
            throw new IllegalArgumentException(
                    "ERROR: if BinaryPerceptronSet bias term is specified, must contain a bias for each model.  numBias="
                            + tokens.length + " numModels=" + numClassifiers);
        }
        for (int i = 0; i < tokens.length; i++) {
            classifiers[i].setBias("0," + tokens[i]);
        }
    }

    // @Override
    // public String toString() {
    // String modelStr = "";
    // for (int i = 0; i < numClassifiers; i++) {
    // modelStr += classifiers[i].toString();
    // }
    // return modelStr;
    // }

    @Override
    public float computeLoss(final int goldClass, final int guessClass) {
        // TODO: is this correct?
        return classifiers[0].computeLoss(goldClass, guessClass);
    }

    @Override
    public String getFeatureTemplate() {
        return classifiers[0].getFeatureTemplate();
    }

    @Override
    public void writeModel(final BufferedWriter stream) throws IOException {
        stream.write("# === BinaryPerceptronSet Model ===\n");
        stream.write("bins=" + binsStr + "\n\n");
        // stream.write(toString());
        for (int i = 0; i < numClassifiers; i++) {
            stream.write(classifiers[i].toString());
        }
    }
}
