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
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.ListGrammar;
import edu.ohsu.cslu.parser.Util;

/**
 * Trains a perceptron model from a corpus. Features and objective tags are derived using a {@link FeatureExtractor}.
 * 
 * @author Nathan Bodenstab
 */
public class TrainPerceptron extends BaseCommandlineTool {

    @Option(name = "-g", metaVar = "file", usage = "Grammar file")
    private File grammarFile;
    private Grammar grammar;

    @Option(name = "-i", aliases = { "--iterations" }, metaVar = "count", usage = "Iterations over training corpus")
    private int iterations = 5;

    @Option(name = "-o", aliases = { "--order" }, metaVar = "order", usage = "Markov Order")
    private int markovOrder = 2;

    @Option(name = "-a", aliases = { "--alpha" }, metaVar = "value", usage = "Update step size (alpha)")
    private float alpha = 0.1f;

    @Option(name = "-d", metaVar = "file", usage = "Development set. If specified, test results are output after each training iteration.")
    private File devSet;

    @Option(name = "-overLoss", hidden = true)
    private float overPenalty = 1;

    @Option(name = "-underLoss")
    private float underPenalty = 1;

    @Option(name = "-feats", usage = "Feature template file OR feature template string: lt rt lt_lt-1 rw_rt loc ...")
    public String featTemplate = null;

    @Option(name = "-bins", usage = "Value to Class mapping bins. ex: '0,5,10,30' ")
    private String binsStr;

    @Option(name = "-multiBin", usage = "Use old multi-bin classification instead of multiple binary classifiers")
    private boolean multiBin = false;

    // TODO: This class should take a "feature template" as input and learn a model based on that
    // example: t t-1 t-2 t+1 t+2 w w-1 w+1 t_w t_t-1 t-1_t-2 ... t=tag w=word _=joint

    public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
    public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));

    @Override
    public void setup() throws Exception {
        // grammar = ParserDriver.readGrammar(grammarFile.toString(), Parser.ResearchParserType.ECPCellCrossList, null);
        grammar = ListGrammar.read(grammarFile.getName());
    }

    @Override
    protected void run() throws Exception {

        final TrainPerceptron m = new TrainPerceptron();
        m.natesTraining();
    }

    public void natesTraining() throws Exception {

        if (featTemplate == null) {
            BaseLogger.singleton().info(
                    "ERROR: Training a model from pre-computed features requires -feats to be non-empty");
            System.exit(1);
        } else if (!featTemplate.contains(" ") && new File(featTemplate).exists()) {
            final BufferedReader featFileReader = new BufferedReader(new FileReader(featTemplate));
            featTemplate = featFileReader.readLine(); // assume it just has one line
        }

        if (binsStr == null) {
            BaseLogger.singleton().info(
                    "ERROR: Training a model from pre-computed features requires -bins to be non-empty");
            System.exit(1);
        }

        final DataSet train = new DataSet(inputStream);
        final InputStream devStream = Util.file2inputStream(devSet.getAbsolutePath());
        final DataSet dev = new DataSet(new BufferedReader(new InputStreamReader(devStream)));

        Classifier model;
        if (multiBin) {
            model = new AveragedPerceptron(alpha, new Perceptron.OverUnderLoss(overPenalty, underPenalty), binsStr,
                    featTemplate, null);
        } else {
            model = new BinaryPerceptronSet(alpha, new Perceptron.OverUnderLoss(overPenalty, underPenalty), binsStr,
                    featTemplate);
        }

        // iterate over training data
        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < train.numExamples; j++) {
                final int goldClass = model.value2class(train.classification.get(j));
                model.train(goldClass, train.features.get(j));
            }

            final float trainLoss = evalDataSetAvgLoss(train, model, false);
            final float devLoss = evalDataSetAvgLoss(dev, model, false);

            System.out.format("ittr=%d\t trainLoss=%.4f\t devLoss=%.4f\n", i, trainLoss, devLoss);
        }

        evalDataSetAvgLoss(train, model, true);
        evalDataSetAvgLoss(dev, model, true);

        // System.out.print(model.toString());
        model.writeModel(outputStream);
    }

    public float evalDataSetAvgLoss(final DataSet data, final Classifier model, final boolean confMatrix) {
        final int numClasses = model.numClasses();
        final int counts[][] = new int[numClasses][numClasses];
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                counts[i][j] = 0;
            }
        }

        float loss = 0;
        for (int i = 0; i < data.numExamples; i++) {
            final int goldClass = model.value2class(data.classification.get(i));
            final int guessClass = model.classify(data.features.get(i));

            // System.out.println("gold=" + goldClass + " guess=" + guessClass);

            loss += model.computeLoss(goldClass, guessClass);
            counts[goldClass][guessClass] += 1;
        }

        if (confMatrix) {
            String s = "** Y=guess  X=gold **\n";
            int correct = 0;
            for (int y = 0; y < numClasses; y++) {
                int rowTotal = 0;
                for (int x = 0; x < numClasses; x++) {
                    s += "\t" + counts[x][y];
                    rowTotal += counts[x][y];
                }
                s += String.format("\tacc=%.2f\n", (float) counts[y][y] / rowTotal);
                correct += counts[y][y];
            }
            s += String.format("totalAcc=%.2f\n", (float) correct / data.numExamples);
            System.out.println(s);
        }

        return loss / data.numExamples;
    }

    /**
     * Represents a set of training examples
     */
    public class DataSet {

        /**
         * Parallel array of training examples; gold classes (classifications) and the feature vectors associated with
         * each example
         */
        public ArrayList<Integer> classification = new ArrayList<Integer>();
        public ArrayList<SparseBitVector> features = new ArrayList<SparseBitVector>();
        // TODO Expose this as a size() method
        public int numExamples;
        public int numFeatures = -1;

        public DataSet(final BufferedReader dataStream) throws Exception {
            readDataSet(dataStream);
            // readDataSet(is, null);
            // readOldFormat(is);
        }

        // public DataSet(final InputStream is, final Perceptron perceptron) throws Exception {
        // readDataSet(is, perceptron);
        // }

        /**
         * 
         * Expected format (goldClass : featLen posFeat1 posFeat2 ...) 9 : 98695 0 38 68 136 179 237 1067 2684 2714 2782
         * 2825 2883 7348 30622 55242 95299 0 : 98695 0 19 87 117 185 228 1342 2665 2733 2763 2831 2874 23458 31295
         * 71352 79105 2 : 98695 0 38 68 136 166 234 430 2684 2714 2782 2812 2880 7264 47405 55158 95299 ....
         */
        // private void readDataSet(final InputStream is, final Perceptron perceptron) throws Exception {
        private void readDataSet(final BufferedReader dataStream) throws Exception {
            // final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            numExamples = 0;
            for (String line = dataStream.readLine(); line != null; line = dataStream.readLine()) {
                final String[] tokens = line.split("\\s+");
                assert numFeatures == tokens.length - 1 || numFeatures == -1;
                numFeatures = Integer.parseInt(tokens[2]);
                final int numPosFeats = tokens.length - 3;
                numExamples++;

                final int goldClass = new Integer(tokens[0]);
                // if (perceptron != null) {
                // goldClass = perceptron.value2class(goldClass);
                // }
                classification.add(goldClass);

                final int feats[] = new int[numPosFeats];
                for (int i = 0; i < numPosFeats; i++) {
                    feats[i] = new Integer(tokens[i + 3]);
                }
                features.add(new SparseBitVector(numFeatures, feats));
            }
        }

        public void readOldFormat(final InputStream is) throws Exception {
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            // Expected format: classification featVal1 featVal2 featVal3 ...
            numExamples = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                final String[] tokens = line.split("\\s+");
                assert numFeatures == tokens.length - 1 || numFeatures == -1;
                numFeatures = tokens.length - 1;
                numExamples++;

                classification.add(new Integer(tokens[0]));

                final boolean feats[] = new boolean[numFeatures];
                for (int i = 0; i < numFeatures; i++) {
                    final float val = Float.parseFloat(tokens[i + 1]); // +1 because offset from
                                                                       // classification number
                    if (val == 0.0) {
                        feats[i] = false;
                    } else if (val == 1.0) {
                        feats[i] = true;
                    } else {
                        throw new Exception("ERROR: expecting binary 0/1 values in feature vector but found '"
                                + tokens[i] + "'");
                    }
                }
                features.add(new SparseBitVector(feats));
            }
        }
    }

    public String featureVectorToString(final SparseBitVector featureVector) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < markovOrder * 2 + 1; i++) {
            for (int j = 0; j < grammar.numLexSymbols(); j++) {
                final int feature = grammar.numLexSymbols() * i + j;
                if (featureVector.getBoolean(feature)) {
                    sb.append(String.format("_w_%d_%s\n", i - markovOrder, grammar.mapLexicalEntry(j)));
                }
            }
        }
        // Previous tags
        for (int i = 0; i < markovOrder * 2; i = i + 2) {
            final int trueFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i;
            sb.append(String.format("_t_%d_T : %s\n", i / 2 - markovOrder, featureVector.getBoolean(trueFeature) ? "T"
                    : "F"));
        }
        return sb.toString();
    }

    // // TODO: I'm guessing this is all temporary? I don't understand a toString() method here
    // // except for debugging ...
    // @Override
    // public String toString() {
    // final StringBuilder sb = new StringBuilder();
    // // Tokens (in markov window)
    // for (int i = 0; i < markovOrder * 2 + 1; i++) {
    // for (int j = 0; j < grammar.numLexSymbols(); j++) {
    // final int feature = grammar.numLexSymbols() * i + j;
    // // final float weight = model.averagedFeatureWeight(feature, example);
    // final float weight = model.averagedPerceptron().getFloat(feature);
    // if (weight != 0) {
    // sb.append(String.format("_w_%d_%s : %.2f\n", i - markovOrder, grammar.mapLexicalEntry(j), weight));
    // }
    // }
    // }
    // // Previous tags
    // for (int i = 0; i < markovOrder * 2; i = i + 2) {
    // final int trueFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i;
    // // final float trueWeight = model.averagedFeatureWeight(trueFeature, example);
    // final float trueWeight = model.averagedPerceptron().getFloat(trueFeature);
    // sb.append(String.format("_t_%d_T : %.2f\n", i / 2 - markovOrder, trueWeight));
    //
    // final int falseFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i + 1;
    // // final float falseWeight = model.averagedFeatureWeight(falseFeature, example);
    // final float falseWeight = model.averagedPerceptron().getFloat(falseFeature);
    // sb.append(String.format("_t_%d_F : %.2f\n", i / 2 - markovOrder, falseWeight));
    // }
    // return sb.toString();
    // }

    public static void main(final String[] args) {
        run(args);
    }
}
