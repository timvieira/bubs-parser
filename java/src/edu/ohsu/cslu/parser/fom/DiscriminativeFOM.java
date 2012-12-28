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

package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.beam.BeamSearchChartParserDiscFOM;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.SimpleChartEdge;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.Perceptron;

/**
 * @author Nathan Bodenstab
 * @since Apr 7, 2012
 */
public class DiscriminativeFOM extends FigureOfMeritModel {

    // Question: Do we want to just change the FOM ranking function, or actually modify
    // the inside prob (i.e., decode w/ extra info than given by the grammar)?
    // Reranking does the later to get better accuracy. But the first has potential as
    // well if we can decrease the beam-width.

    // What is our loss function? We can evaluate by taking the gold tree and the hyp
    // tree and rewarding/penalizing based on mis-matches, but this doesn't account for
    // the actual rank of constituents during parsing. We could also integrate directly
    // into the beam search and reward/penalize if

    Perceptron model;
    public static int NUM_FEATS = 1047297;
    float insideWeight = 1;
    final static boolean useAveragePerceptron = true;

    public DiscriminativeFOM(final Perceptron model) {
        super(FOMType.Discriminative);
        this.model = model;
    }

    public DiscriminativeFOM(final BufferedReader inModelStream) {
        super(FOMType.Discriminative);
        try {
            final HashMap<String, String> keyValue = Util.readKeyValuePairs(inModelStream.readLine().trim());
            insideWeight = Float.parseFloat(keyValue.get("insideWeight"));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        model = new AveragedPerceptron(inModelStream);
    }

    @Override
    public FigureOfMerit createFOM() {
        return new DiscriminativeFOMSelector();
    }

    // had BufferedReader inStream
    public static void train(final String trainFileName, final String devFileName, final BufferedWriter outStream,
            final String grammarFile, final String featureTemplate, final int numIterations, final float learningRate)
            throws Exception {

        Perceptron perceptronModel;
        if (useAveragePerceptron) {
            perceptronModel = new AveragedPerceptron(learningRate, new Perceptron.ZeroOneLoss(), "0", featureTemplate,
                    new float[NUM_FEATS]);
        } else {
            perceptronModel = new Perceptron(learningRate, new Perceptron.ZeroOneLoss(), "0", featureTemplate,
                    new float[NUM_FEATS]);
        }

        final DiscriminativeFOM fomModel = new DiscriminativeFOM(perceptronModel);

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = LeftRightBottomTopTraversal.MODEL;
        opts.fomModel = fomModel;

        // final LeftCscSparseMatrixGrammar grammar = (LeftCscSparseMatrixGrammar) ParserDriver.readGrammar(grammarFile,
        // ResearchParserType.CartesianProductHashMl, PackingFunctionType.PerfectHash);
        // final CartesianProductHashSpmlParser parser = new CartesianProductHashSpmlParser(opts, grammar);

        final LeftHashGrammar grammar = (LeftHashGrammar) ParserDriver.readGrammar(grammarFile,
                ResearchParserType.ECPCellCrossHash, null);
        final BeamSearchChartParserDiscFOM<LeftHashGrammar, CellChart> parser = new BeamSearchChartParserDiscFOM<LeftHashGrammar, CellChart>(
                opts, grammar, (DiscriminativeFOM) opts.fomModel);

        // final List<Feature> featList = Chart.featureTemplateStrToEnum(featureTemplate.split("\\s+"));

        // hack to get number of features given featureNames
        // parser.parseSentence("(TOP (S (JJ dummy) (NN string)))");
        // final int numFeatures = parser.chart.getCellFeatures(0, 1, featList).vectorLength();
        // final int numFeatures = parser.chart.getCellFeatures(0, 1, featureTemplate.split("\\s+")).vectorLength();

        String line;
        int nSent = 0;
        for (int i = 0; i < numIterations; i++) {
            int numGold = 0, numGoldRankOne = 0, sumGoldRank = 0;
            final BufferedReader inStream = new BufferedReader(new FileReader(trainFileName));
            while ((line = inStream.readLine()) != null) {
                nSent += 1;
                parser.parseSentence(line);
                numGold += parser.numGold;
                numGoldRankOne += parser.numGoldRankOne;
                sumGoldRank += parser.sumGoldRank;

                if (nSent > 0 && nSent % 100 == 0) {
                    fomModel.writeModel(new BufferedWriter(new FileWriter("discfom.itr" + i + ".sent" + nSent)));
                }

                // final BinaryTree<String> binaryGoldTree = parseTask.inputTree.binarize(grammar.grammarFormat,
                // grammar.binarization());
                // System.out.println(" gold=" + binaryGoldTree.toString() + "\n" + "parse=" + parseTask.binaryParse
                // + "\n" + parser.getStats());

            }
            inStream.close();
            System.err.println(String
                    .format("itr=%d\tnGold=%d\tnRank1=%d\tacc=%f\tsumRank=%d\tinsideWt=%f", i, numGold, numGoldRankOne,
                            numGoldRankOne / (float) numGold * 100, sumGoldRank, fomModel.insideWeight));
            fomModel.writeModel(new BufferedWriter(new FileWriter("discfom.itr" + i)));
        }
        // model.writeModel(outStream);
        // TODO: only write model values and features that have non-zero value!
        // final BufferedWriter featFile = new BufferedWriter(new FileWriter("discfom.feats"));
        // for (final String key : Chart.featHash) {
        // featFile.write(key + "\t" + Chart.featHash.getIndex(key) + "\n");
        // }
        // featFile.close();
    }

    // protected void printDevAcc(BeamSearchChartParserDiscFOM<LeftHashGrammar, CellChart> parser, String devFileName) {
    // int numGold = 0, numGoldRankOne = 0, sumGoldRank = 0;
    // final BufferedReader inStream = new BufferedReader(new FileReader(trainFileName));
    // while ((line = inStream.readLine()) != null) {
    // parser.parseSentence(line);
    // numGold += parser.numGold;
    // numGoldRankOne += parser.numGoldRankOne;
    // sumGoldRank += parser.sumGoldRank;
    // }
    // inStream.close();
    // System.err.println(String
    // .format("itr=%d\tnGold=%d\tnRank1=%d\tacc=%f\tsumRank=%d\tinsideWt=%f", i, numGold, numGoldRankOne,
    // numGoldRankOne / (float) numGold * 100, sumGoldRank, fomModel.insideWeight));
    // }

    protected void writeModel(final BufferedWriter outStream) {
        try {
            outStream.write("# model=FOM type=" + this.type + " insideWeight=" + insideWeight + " gram=? featMap=?\n");
            model.writeModel(outStream);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void trainBinaryInstance(final float alpha, final SparseBitVector features, final float inside) {
        System.err.println("Discriminative FOM no longer working.  See code.");
        System.exit(1);
        // model.trainBinary(alpha, features);
        if (useAveragePerceptron) {
            // TODO: fix
            // insideWeight = (inside * alpha + insideWeight * model.trainExampleNumber) / (model.trainExampleNumber +
            // 1);
        } else {
            insideWeight += inside * alpha;
        }
        // System.out.println(alpha + "\t" + insideWeight + "\t" + inside);
        // String featStr = "";
        // for (final int x : features.values()) {
        // featStr += x + ":1 ";
        // }
        // final int classification = alpha == 1 ? 1 : 0;
        // System.out.format("FEAT %d 0:%f %s\n", classification, inside, featStr);
    }

    public class DiscriminativeFOMSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;
        private Chart chart;

        public float calcFOM(final SimpleChartEdge e, final float insideProb) {
            return calcFOM(e.start, e.mid, e.end, e.A, e.B, e.C, insideProb);
        }

        public float calcFOM(final short start, final short mid, final short end, final short A, final short B,
                final short C, final float insideProb) {
            final SparseBitVector feats = chart.getEdgeFeatures(start, mid, end, A, B, C);
            // TODO: fix
            // return model.scoreBinary(feats) + insideProb * insideWeight;
            return 0f;
        }

        @Override
        public float calcFOM(final int start, final int end, final short parent, final float insideProb) {
            BaseLogger.singleton().severe("ERROR: calcFOM(...) with full edges should be used");
            for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                System.out.println(ste);
            }
            System.exit(1);
            return 0;
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability;
        }

        @Override
        public void initSentence(final ParseTask parseTask, final Chart c) {
            this.chart = c;
        }

    }

}
