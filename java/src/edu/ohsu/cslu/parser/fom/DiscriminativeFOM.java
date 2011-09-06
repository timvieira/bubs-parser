package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;

import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.fom.FigureOfMerit.FOMType;
import edu.ohsu.cslu.parser.ml.InsideOutsideCphSpmlParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductFunctionType;
import edu.ohsu.cslu.perceptron.LogisticRegressor;

public class DiscriminativeFOM extends FigureOfMeritModel {

    LogisticRegressor model;
    String[] featureNames;

    public DiscriminativeFOM(final FOMType type) {
        super(type);
    }

    public DiscriminativeFOM(final FOMType type, final Grammar grammar, final BufferedReader modelStream)
            throws IOException {
        super(type);
    }

    @Override
    public FigureOfMerit createFOM() {
        switch (type) {
        case Discriminative:
            return new DiscriminativeFOMSelector();
        default:
            return super.createFOM();
        }
    }

    public void readModel(final BufferedReader inStream, final String featureString) throws IOException {
        model = LogisticRegressor.read(inStream);
        featureNames = featureString.split("\\s+");
    }

    public static void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile,
            final String featureTemplate, final boolean extractFeatures) throws Exception {

        final InsideOutsideCscSparseMatrixGrammar grammar = (InsideOutsideCscSparseMatrixGrammar) ParserDriver
                .readGrammar(grammarFile, ResearchParserType.InsideOutsideCartesianProductHash,
                        CartesianProductFunctionType.PerfectHash);
        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = LeftRightBottomTopTraversal.MODEL;
        opts.decodeMethod = DecodeMethod.Goodman;
        opts.parseFromInputTags = true;
        final InsideOutsideCphSpmlParser parser = new InsideOutsideCphSpmlParser(opts, grammar);

        final String[] featureNames = featureTemplate.split("\\s+");
        final int numModels = grammar.phraseSet.size();

        // final LinkedList<DataPoint> trainSet = new LinkedList<DataPoint>();
        String line;
        final float[] normInOutScores = new float[grammar.phraseSet.size()];
        if (extractFeatures) {
            while ((line = inStream.readLine()) != null) {
                final ParseTask result = parser.parseSentence(line);
                // System.out.println("Result:" + result.parseBracketString(false, false) + result.statsString());

                parser.cellSelector.reset();
                while (parser.cellSelector.hasNext()) {
                    final short[] startAndEnd = parser.cellSelector.next();
                    final short start = startAndEnd[0];
                    final short end = startAndEnd[1];
                    final SparseBitVector featureVector = parser.chart.getCellFeatures(start, end, featureNames);
                    Arrays.fill(normInOutScores, Float.NEGATIVE_INFINITY);
                    for (int i = 0; i < numModels; i++) {
                        final int nt = grammar.phraseSet.getSymbol(i);
                        normInOutScores[i] = parser.getInside(start, end, nt) + parser.getOutside(start, end, nt)
                                - result.insideProbability;
                    }
                    final DataPoint example = new DataPoint(new FloatVector(normInOutScores), featureVector);
                    System.out.println(example.toString());
                }
            }
        } else {
            // hack to get number of features given featureNames
            final ParseTask tmpSent = parser.parseSentence("(TOP (S (JJ dummy) (NN string)))");
            final int numFeatures = parser.chart.getCellFeatures(0, 1, featureNames).vectorLength();
            // final int numFeatures = System.out.println("numFeats=" + numFeatures);
            final LogisticRegressor model = new LogisticRegressor(numFeatures, numModels, 0.1f,
                    new edu.ohsu.cslu.perceptron.LogisticRegressor.DifferenceLoss());

            final LinkedList<String> fileList = new LinkedList<String>();
            while ((line = inStream.readLine()) != null) {
                fileList.add(line.trim());
            }

            final int numIterations = 10;
            for (int i = 0; i < numIterations; i++) {
                final float[] loss = new float[numModels];
                float totalLoss = 0;
                for (final String fileName : fileList) {
                    final BufferedReader fileStream = new BufferedReader(new InputStreamReader(
                            Util.file2inputStream(fileName)));
                    while ((line = fileStream.readLine()) != null) {
                        final DataPoint example = DataPoint.parse(line.trim(), numFeatures, numModels);
                        final float[] exampleLoss = model.train(example.targetValues, example.featureVector);
                        for (int j = 0; j < numModels; j++) {
                            if (exampleLoss[j] > Float.NEGATIVE_INFINITY) {
                                loss[j] += Math.abs(exampleLoss[j]);
                                totalLoss += Math.abs(exampleLoss[j]);
                            }
                        }
                    }
                    fileStream.close();
                }
                // System.out.println(String.format("itr=%d\tloss: %f %s", i, totalLoss, Util.floatArray2Str(loss)));
                System.out.println(String.format("itr=%d\tloss: %f", i, totalLoss));
                model.write("tmp.model." + i);
            }
        }
    }

    // public static void train2(final BufferedReader inStream, final BufferedWriter outStream, final String
    // grammarFile,
    // final String featureTemplate) throws Exception {
    // final String test = "" + "0.1 1 4 5 8 10\n" + "-15 0 1 2 3\n" + "-0.9 4 5 6 7\n" + "2.1 2 4 6 8";
    //
    // final LogisticRegressor model = new LogisticRegressor(0.1f,
    // new edu.ohsu.cslu.perceptron.LogisticRegressor.DifferenceLoss(), 11, 1);
    // for (int ittr = 0; ittr < 20; ittr++) {
    // float loss = 0;
    // for (final String line : test.split("\n")) {
    // float goldValue = 0f;
    // int i = 0;
    // final boolean tmp[] = new boolean[11];
    // for (final String tok : line.split("\\s+")) {
    // if (i == 0) {
    // goldValue = Float.parseFloat(tok);
    // } else {
    // tmp[Integer.parseInt(tok)] = true;
    // }
    // i++;
    // }
    // final SparseBitVector featVector = new SparseBitVector(tmp);
    // loss += goldValue - model.predict(0, featVector);
    // model.train(0, goldValue, featVector);
    // }
    // System.out.println("ittr=" + ittr + " loss=" + loss);
    // }
    // }

    public class DiscriminativeFOMSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;
        // private ParseContext parseTask;
        private Grammar grammar;
        private Chart chart;

        @Override
        public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
            final SparseBitVector features = this.chart.getCellFeatures(start, end, featureNames);
            // TODO: really bad! Only need to compute one, not all. Should pre-compute a lot of this
            return model.predict(features).getFloat(grammar.phraseSet.getIndex((int) parent));
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability;
        }

        @Override
        public void init(final ParseTask parseTask, final Chart chart) {
            // should divide feature vector into three parts: cell-specific, left-boundary, right-boundary
            // could then pre-compute left and right boundary scores for each NT and add them up
            // in calcFOM with cell-specific values
            // this.parseTask = parseTask;
            this.chart = chart;
            this.grammar = parseTask.grammar;
        }

    }
}

class DataPoint {
    public FloatVector targetValues;
    public SparseBitVector featureVector;

    public DataPoint(final FloatVector targetValues, final SparseBitVector featureVector) {
        this.targetValues = targetValues;
        this.featureVector = featureVector;
    }

    @Override
    public String toString() {
        String s = "TARGETS:";
        for (int i = 0; i < targetValues.length(); i++) {
            if (targetValues.getFloat(i) > Float.NEGATIVE_INFINITY) {
                s += String.format(" %d:%f", i, targetValues.getFloat(i));
            }
        }
        s += " FEATS:";
        for (final int nt : featureVector.values()) {
            s += " " + nt;
        }
        return s;
    }

    public static DataPoint parse(final String str, final int numFeats, final int numModels) {
        final boolean[] feats = new boolean[numFeats];
        final float[] targets = new float[numModels];
        Arrays.fill(targets, Float.NEGATIVE_INFINITY);

        boolean parsingFeats = false;
        for (final String tok : str.split("\\s+")) {
            if (tok.equals("TARGETS:")) {
                parsingFeats = false;
            } else if (tok.equals("FEATS:")) {
                parsingFeats = true;
            } else if (parsingFeats) {
                feats[Integer.parseInt(tok)] = true;
            } else {
                final String[] keyValue = tok.split(":");
                targets[Integer.parseInt(keyValue[0])] = Float.parseFloat(keyValue[1]);
            }
        }
        return new DataPoint(new FloatVector(targets), new SparseBitVector(feats));
    }
}
