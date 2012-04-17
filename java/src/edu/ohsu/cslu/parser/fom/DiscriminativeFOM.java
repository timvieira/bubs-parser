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
import java.util.Collection;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.beam.BeamSearchChartParser;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartEdge;
import edu.ohsu.cslu.parser.chart.Chart.SimpleChartEdge;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.Perceptron;

/**
 * @author Nathan Bodenstab
 * @since Apr 7, 2012
 */
public class DiscriminativeFOM extends FigureOfMeritModel {

    AveragedPerceptron model;

    public DiscriminativeFOM(final FOMType type) {
        super(type);
    }

    @Override
    public FigureOfMerit createFOM() {
        return new DiscriminativeFOMSelector();
    }

    public void readModel(final BufferedReader inStream) {
        model = new AveragedPerceptron(inStream);
        // featureNames = model.featureString.split("\\s+");
        // System.out.println("feats=" + model.featureString);
    }

    // had BufferedReader inStream
    public static void train(final String inputFileName, final BufferedWriter outStream, final String grammarFile,
            final String featureTemplate, final int numIterations, final float learningRate) throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = LeftRightBottomTopTraversal.MODEL;
        opts.fomModel = new InsideProb();

        // final InsideOutsideCphSpmlParser parser = getTrainParser(grammarFile);
        // final InsideOutsideCscSparseMatrixGrammar grammar = (InsideOutsideCscSparseMatrixGrammar) ParserDriver
        // .readGrammar(grammarFile, ResearchParserType.InsideOutsideCartesianProductHash,
        // PackingFunctionType.PerfectHash);
        // opts.decodeMethod = DecodeMethod.Goodman;
        // opts.parseFromInputTags = true;
        // final InsideOutsideCphSpmlParser parser = new InsideOutsideCphSpmlParser(opts, grammar);

        // final LeftCscSparseMatrixGrammar grammar = (LeftCscSparseMatrixGrammar) ParserDriver.readGrammar(grammarFile,
        // ResearchParserType.CartesianProductHashMl, PackingFunctionType.PerfectHash);
        // final CartesianProductHashSpmlParser parser = new CartesianProductHashSpmlParser(opts, grammar);

        final LeftHashGrammar grammar = (LeftHashGrammar) ParserDriver.readGrammar(grammarFile,
                ResearchParserType.ECPCellCrossHash, null);
        final BeamSearchChartParser<LeftHashGrammar, CellChart> parser = new BeamSearchChartParser<LeftHashGrammar, CellChart>(
                opts, grammar);

        // final List<Feature> featList = Chart.featureTemplateStrToEnum(featureTemplate.split("\\s+"));

        // hack to get number of features given featureNames
        // parser.parseSentence("(TOP (S (JJ dummy) (NN string)))");
        // final int numFeatures = parser.chart.getCellFeatures(0, 1, featList).vectorLength();
        // final int numFeatures = parser.chart.getCellFeatures(0, 1, featureTemplate.split("\\s+")).vectorLength();

        final AveragedPerceptron model = new AveragedPerceptron(learningRate, new Perceptron.ZeroOneLoss(), "0",
                featureTemplate, null);

        String line;
        for (int i = 0; i < numIterations; i++) {
            int numGold = 0, numHyp = 0, numCorrect = 0;
            final BufferedReader inStream = new BufferedReader(new FileReader(inputFileName));
            while ((line = inStream.readLine()) != null) {
                final ParseTask parseTask = parser.parseSentence(line);
                final BinaryTree<String> binaryGoldTree = parseTask.inputTree.binarize(grammar.grammarFormat,
                        grammar.binarization());
                final Collection<SimpleChartEdge> goldEdges = Util.getEdgesFromTree(binaryGoldTree, grammar);
                System.out.println("parse=" + parseTask.binaryParse);
                if (parseTask.parseFailed())
                    continue;
                final Collection<SimpleChartEdge> hypEdges = Util.getEdgesFromTree(parseTask.binaryParse, grammar);

                for (final SimpleChartEdge goldEdge : goldEdges) {
                    numGold++;
                    if (!hypEdges.contains(goldEdge)) {
                        final SparseBitVector features = parser.chart.getEdgeFeatures(goldEdge);
                        model.train(1, features);
                    }
                }
                for (final SimpleChartEdge hypEdge : hypEdges) {
                    numHyp++;
                    if (!goldEdges.contains(hypEdge)) {
                        final SparseBitVector features = parser.chart.getEdgeFeatures(hypEdge);
                        model.train(-1, features);
                    } else {
                        numCorrect++;
                    }
                }
            }
            inStream.close();
            System.err.println(String.format("itr=%d\tnRef=%d\tnHyp=%d\tnCorrect=%d\tacc=%f", i, numGold, numHyp,
                    numCorrect, (float) (numCorrect) / (numGold)));
            model.writeModel(new BufferedWriter(new FileWriter("tmp.model." + i)));
        }
        model.writeModel(outStream);
    }

    protected ChartEdge getOracleEdge(final short start, final short end) {
        // TODO: what to do when gold constituent isn't in beam? Huang takes the constituent with the best F1.
        // Could also just penalize all and not reward any, or reward the gold even if it isn't there.
        return null;
    }

    public class DiscriminativeFOMSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;
        private Grammar grammar;
        private Chart chart;

        @Override
        public float calcFOM(final int start, final int end, final short nt, final float insideProbability) {
            // final SparseBitVector features = this.chart.getCellFeatures(start, end, featureNames);
            // final float fom = model.predict(features).getFloat(grammar.phraseSet.getIndex((int) nt));
            // return fom;
            return 0f;
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability;
        }

        @Override
        public void initSentence(final ParseTask parseTask, final Chart c) {
            this.chart = c;
            this.grammar = parseTask.grammar;
        }

    }

}
