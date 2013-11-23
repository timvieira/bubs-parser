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

package edu.ohsu.cslu.perceptron;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.AdaptiveBeamClassifier.UnaryConstraintSequence;

/**
 * Complete-closure classifier, as described in Bodenstab et al., 2011,
 * "Beam-Width Prediction for Efficient Context-Free Parsing". This implementation trains and tests complete-closure
 * models from the command-line, and training and classification can both be embedded directly into larger systems -
 * training primarily for grammar learning, and classification (naturally) for use during inference.
 * 
 * Implementation note: we consider a 'true' binary classification to be a closed cell, so we generally target negative
 * recall (i.e., the number of open cells correctly classified as such). To limit inference failures, that
 * negative-recall target ({@link BinaryClassifier#targetNegativeRecall}) should be very high - e.g., .99-.999.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2013
 */
public class CompleteClosureClassifier extends BinaryClassifier<CompleteClosureSequence> {

    private static final long serialVersionUID = 2L;

    @Option(name = "-bin", metaVar = "direction", usage = "Binarization direction")
    protected Binarization binarization = Binarization.LEFT;

    @Option(name = "-ptti", metaVar = "iterations", requires = "-ti", usage = "Train a POS tagger for n iterations")
    private int posTaggerTrainingIterations = 0;

    @Option(name = "-ptft", metaVar = "templates or file", usage = "POS-tagger feature templates (comma-delimited), or template file")
    private String posTaggerFeatureTemplates = new Tagger().DEFAULT_FEATURE_TEMPLATES();

    @Option(name = "-ucti", metaVar = "iterations", usage = "Train a unary-constraint binary classifier for n iterations.")
    private int unaryConstraintClassifierTrainingIterations;

    @Option(name = "-ucft", requires = "-ucti", metaVar = "templates or file", usage = "Feature templates for unary classifier (comma-delimited or template file)")
    protected String unaryClassifierFeatureTemplates = Tagger.DEFAULT_FEATURE_TEMPLATES;

    /** Part-of-speech tagger, executed prior to cell classification to provide POS features to the beam-width model */
    public Tagger posTagger = null;

    /**
     * Unary constraint binary classifier - used only during training. Following training, {@link #finalizeModel()}
     * packs the learned parameters into {@link #parallelWeightArrayTags} and {@link #parallelWeightArray}.
     */
    private UnaryConstraintClassifier unaryConstraintClassifier;

    /**
     * Default Feature Templates:
     * 
     * <pre>
     * # Unigram POS-tag, word, and UNK, features
     * ltm2,ltm1,lt,ltp1
     * rtm1,rt,rtp1,ltp1
     * lwm1,lw,rw,rwp1
     * lum1,lu,ru,rup1
     * 
     * # Bigram tag features
     * ltm1_lt1,rt_rtp1
     * 
     * # Bigram word/UNK features
     * lwm1_lw,rw_rwp1
     * lum1_lu,ru_rup1
     * 
     * # Absolute span features
     * s2,s3,s4,s5,s10,s20,s30,s40,s50
     * 
     * # Relative span features
     * rs2,rs4,rs6,rs8,rs10
     * </pre>
     */
    @Override
    protected final String DEFAULT_FEATURE_TEMPLATES() {
        return "ltm2,ltm1,lt,ltp1,rtm1,rt,rtp1,ltp1,lwm1,lw,rw,rwp1,lum1,lu,ru,rup1,ltm1_lt,rt_rtp1,lwm1_lw,rw_rwp1,lum1_lu,ru_rup1,s2,s3,s4,s5,s10,s20,s30,s40,s50,rs2,rs4,rs6,rs8,rs10";
    }

    /**
     * Empty constructor (generally used when reading a persisted model)
     */
    public CompleteClosureClassifier() {
    }

    /**
     * Constructs from an existing grammar
     */
    public CompleteClosureClassifier(final Grammar grammar, final Tagger posTagger, final String featureTemplates) {
        if (grammar != null) {
            this.lexicon = grammar.lexSet;
            this.nonterminalVocabulary = grammar.nonTermSet;
            this.decisionTreeUnkClassSet = grammar.unkClassSet();
        } else {
            this.lexicon = new SymbolSet<String>();
            this.nonterminalVocabulary = new SymbolSet<String>();
            this.decisionTreeUnkClassSet = new SymbolSet<String>();
        }

        this.featureTemplates = featureTemplates;

        if (posTagger != null) {
            this.posTagger = posTagger;
            this.featureExtractor = new ConstituentBoundaryFeatureExtractor<CompleteClosureSequence>(featureTemplates,
                    lexicon, decisionTreeUnkClassSet, posTagger.tagSet, true);
        }
    }

    @Override
    public void readModel(final InputStream is) throws IOException, ClassNotFoundException {
        // Read in the model parameters as a temporary java serialized object and copy into this object
        final ObjectInputStream ois = new ObjectInputStream(is);
        final Model tmp = (Model) ois.readObject();
        ois.close();
        this.featureTemplates = tmp.featureTemplates;
        this.avgWeights = tmp.avgWeights;
        this.bias = tmp.bias;
        this.posTagger = tmp.posTagger;
        this.unaryConstraintClassifier = tmp.unaryConstraintClassifier;
        this.binarization = tmp.binarization;

        this.decisionTreeUnkClassSet = posTagger.decisionTreeUnkClassSet;
        this.lexicon = posTagger.lexicon;

        this.featureExtractor = new ConstituentBoundaryFeatureExtractor<CompleteClosureSequence>(featureTemplates,
                lexicon, decisionTreeUnkClassSet, posTagger.tagSet, true);
        is.close();
    }

    @Override
    protected void run() throws Exception {

        if (trainingIterations > 0) {
            this.lexicon = new SymbolSet<String>();
            this.decisionTreeUnkClassSet = new SymbolSet<String>();
            train(inputAsBufferedReader());

        } else {
            readModel(new FileInputStream(modelFile));
            this.featureExtractor = new ConstituentBoundaryFeatureExtractor<CompleteClosureSequence>(featureTemplates,
                    lexicon, decisionTreeUnkClassSet, posTagger.tagSet, true);
            classify(inputAsBufferedReader());
        }
    }

    /**
     * Classifies the sequences read from <code>input</code>
     * 
     * @param input
     * @return results of classifying the input sequences (if they contain gold classifications)
     * 
     * @throws IOException if a read fails
     */
    protected BinaryClassifierResult classify(final BufferedReader input) throws IOException {

        final long t0 = System.currentTimeMillis();
        final BinaryClassifierResult result = new BinaryClassifierResult();

        for (final String line : inputLines(input)) {
            final CompleteClosureSequence sequence = new CompleteClosureSequence(line, binarization, lexicon,
                    decisionTreeUnkClassSet, posTagger.tagSet);
            result.totalSequences++;

            for (int i = 0; i < sequence.classes.length; i++) {
                classify(sequence, i, result);
            }
        }
        result.time = System.currentTimeMillis() - t0;
        outputDevsetAccuracy(1, result);
        return result;
    }

    @Override
    protected void train(final BufferedReader input) throws IOException {

        this.lexicon.defaultReturnValue(Grammar.nullSymbolStr);
        this.decisionTreeUnkClassSet.defaultReturnValue(Grammar.nullSymbolStr);
        final SymbolSet<String> posTagSet = new SymbolSet<String>();
        posTagSet.defaultReturnValue(Grammar.nullSymbolStr);

        final long startTime = System.currentTimeMillis();
        final ArrayList<CompleteClosureSequence> trainingCorpusSequences = new ArrayList<CompleteClosureSequence>();
        final ArrayList<CompleteClosureSequence> devCorpusSequences = new ArrayList<CompleteClosureSequence>();

        this.posTagger = new Tagger(posTaggerFeatureTemplates, lexicon, decisionTreeUnkClassSet, posTagSet);

        final ArrayList<MulticlassTagSequence> taggerTrainingCorpusSequences = new ArrayList<MulticlassTagSequence>();
        final ArrayList<MulticlassTagSequence> taggerDevCorpusSequences = new ArrayList<MulticlassTagSequence>();

        if (unaryConstraintClassifierTrainingIterations > 0) {
            input.mark(30 * 1024 * 1024);
        }

        //
        // Read in the training corpus and map each token. For some classifiers, we also pre-compute all features, but
        // for cell classification, the number of instances is quadratic, and the memory consumption is problematic.
        // Most of the features are related to the linear token/tag sequence, so we could in theory create a more
        // complex feature storage system to reuse those feature values. But until it becomes a problem, it's easier to
        // just extract features repeatedly during training.
        //
        for (final String line : inputLines(input)) {
            try {
                trainingCorpusSequences.add(new CompleteClosureSequence(line, binarization, lexicon,
                        decisionTreeUnkClassSet, posTagSet));
                taggerTrainingCorpusSequences.add(new MulticlassTagSequence(line, posTagger));

            } catch (final IllegalArgumentException ignore) {
                // Skip malformed trees (e.g. INFO lines from parser output)
            }
        }
        finalizeMaps();

        //
        // Train a POS-tagger. We'll use output from that tagger instead of gold POS-tags when training the cell
        // classifier, and output both models together
        //
        BaseLogger.singleton().info("Training POS tagger for " + posTaggerTrainingIterations + " iterations");
        posTagger.train(taggerTrainingCorpusSequences, taggerDevCorpusSequences, posTaggerTrainingIterations);

        featureExtractor = new ConstituentBoundaryFeatureExtractor<CompleteClosureSequence>(featureTemplates,
                posTagger.lexicon, posTagger.decisionTreeUnkClassSet, posTagger.tagSet, true);

        //
        // Tag the training sequences with the trained POS tagger (the resulting tags will be more accurate than
        // test-time tagging, but somewhat less than just using gold tags)
        //
        if (posTagger != null) {
            BaseLogger.singleton().info("Tagging training corpus with the new POS tagger model");
            for (int i = 0; i < trainingCorpusSequences.size(); i++) {
                final CompleteClosureSequence ccs = trainingCorpusSequences.get(i);
                ccs.posTags = posTagger.classify(taggerTrainingCorpusSequences.get(i));
            }
        }

        ArrayList<BinaryTagSequence> unaryConstraintTrainingCorpusSequences = null;
        ArrayList<BinaryTagSequence> unaryConstraintDevCorpusSequences = null;
        if (unaryConstraintClassifierTrainingIterations > 0) {
            this.unaryConstraintClassifier = new UnaryConstraintClassifier(unaryClassifierFeatureTemplates, lexicon,
                    decisionTreeUnkClassSet);
            unaryConstraintTrainingCorpusSequences = new ArrayList<BinaryTagSequence>();
            unaryConstraintDevCorpusSequences = new ArrayList<BinaryTagSequence>();

            input.reset();

            for (final String line : inputLines(input)) {
                final UnaryConstraintSequence unaryConstraintSequence = new UnaryConstraintSequence(line, binarization,
                        unaryConstraintClassifier);
                unaryConstraintSequence.mappedPosSymbols = posTagger
                        .classify(new MulticlassTagSequence(line, posTagger));
                unaryConstraintTrainingCorpusSequences.add(unaryConstraintSequence);
            }
        }

        // Read in the dev set
        if (devSet != null) {
            for (final String line : fileLines(devSet)) {

                final CompleteClosureSequence ccs = new CompleteClosureSequence(line, binarization, posTagger.lexicon,
                        posTagger.decisionTreeUnkClassSet, posTagger.tagSet);
                devCorpusSequences.add(ccs);
                ccs.posTags = posTagger.classify(new MulticlassTagSequence(line, posTagger));

                if (unaryConstraintDevCorpusSequences != null) {
                    final UnaryConstraintSequence unaryConstraintSequence = new UnaryConstraintSequence(line,
                            binarization, unaryConstraintClassifier);
                    unaryConstraintSequence.mappedPosSymbols = ccs.posTags;
                    unaryConstraintDevCorpusSequences.add(unaryConstraintSequence);
                }
            }
        }

        //
        // Train the complete closure model
        //
        train(trainingCorpusSequences, devCorpusSequences);

        // And the unary constraint model
        if (unaryConstraintClassifierTrainingIterations > 0) {
            BaseLogger.singleton().info(
                    "Training the unary constraint model for " + unaryConstraintClassifierTrainingIterations
                            + " iterations.");

            unaryConstraintClassifier.trainingIterations = unaryConstraintClassifierTrainingIterations;
            unaryConstraintClassifier.negativeTrainingBias = negativeTrainingBias;
            unaryConstraintClassifier.targetNegativeRecall = targetNegativeRecall;
            unaryConstraintClassifier.train(unaryConstraintTrainingCorpusSequences, unaryConstraintDevCorpusSequences);
        }

        //
        // Write out the model file to disk
        //
        if (modelFile != null) {
            final FileOutputStream fos = new FileOutputStream(modelFile);
            new ObjectOutputStream(fos).writeObject(new Model(posTagger, unaryConstraintClassifier, featureTemplates,
                    avgWeights, bias, binarization));
            fos.close();
        }

        BaseLogger.singleton().info(
                String.format("Time: %d seconds\n", (System.currentTimeMillis() - startTime) / 1000));
    }

    void train(final ArrayList<CompleteClosureSequence> trainingCorpusSequences,
            final ArrayList<CompleteClosureSequence> devCorpusSequences) {
        //
        // Iterate over training corpus, training the model
        //
        BaseLogger.singleton().info("Training the complete-closure model for " + trainingIterations + " iterations.");
        for (int i = 1, j = 0; i <= trainingIterations; i++, j = 0) {

            for (final CompleteClosureSequence sequence : trainingCorpusSequences) {
                for (int k = 0; k < sequence.classes.length; k++) {
                    train(sequence.classes[k], featureExtractor.featureVector(sequence, k));
                }

                progressBar(100, 5000, j++);
            }

            // Skip the last iteration - we'll test after we finalize below
            if (!devCorpusSequences.isEmpty() && i < trainingIterations) {
                System.out.println();
                outputDevsetAccuracy(i, classify(devCorpusSequences));
            }
        }

        // Test on the dev-set
        if (!devCorpusSequences.isEmpty()) {
            System.out.println();
            outputDevsetAccuracy(trainingIterations, classify(devCorpusSequences));
        }

        //
        // Search for a bias that satisfies the requested precision or recall
        //
        if (targetPrecision != 0) {
            precisionBiasSearch(devCorpusSequences, featureExtractor);
        } else if (targetNegativeRecall != 0) {
            super.negativeRecallBiasSearch(devCorpusSequences, featureExtractor);
            evaluateDevset(devCorpusSequences);
        }
    }

    private void outputDevsetAccuracy(final int iteration, final BinaryClassifierResult result) {

        BaseLogger.singleton().info(
                String.format("Iteration=%d Devset Accuracy=%.2f  P=%.3f  R=%.3f  neg-P=%.3f  neg-R=%.3f  Time=%d\n",
                        iteration, result.accuracy() * 100f, result.precision() * 100f, result.recall() * 100f,
                        result.negativePrecision() * 100f, result.negativeRecall() * 100f, result.time));
    }

    @Override
    protected void precisionBiasSearch(final ArrayList<CompleteClosureSequence> devCorpusSequences,
            final FeatureExtractor<CompleteClosureSequence> fe) {

        super.precisionBiasSearch(devCorpusSequences, fe);
        evaluateDevset(devCorpusSequences);
    }

    void evaluateDevset(final ArrayList<CompleteClosureSequence> devCorpusSequences) {
        final long t0 = System.currentTimeMillis();
        final CompleteClosureResult result = new CompleteClosureResult();

        for (final CompleteClosureSequence sequence : devCorpusSequences) {
            result.totalSequences++;
            for (int i = 0; i < sequence.predictedClasses.length; i++) {
                classify(sequence, i, result);
            }

            for (int i = 0; i < sequence.predictedClasses.length; i++) {
                if (sequence.classes[i] == false && sequence.predictedClasses[i] == true) {
                    result.sentencesWithMisclassifiedNegative++;
                    break;
                }
            }
        }
        result.time = System.currentTimeMillis() - t0;

        // Compute and report final cell-closure statistics on the development set
        int totalWords = 0;
        for (final CompleteClosureSequence sequence : devCorpusSequences) {
            totalWords += sequence.mappedTokens.length;
        }

        // positiveExamples + negativeExamples + span-1 cells
        final int totalCells = result.positiveExamples + result.negativeExamples + totalWords;
        final int openCells = result.classifiedNegative + totalWords;

        BaseLogger.singleton().info(
                String.format("Open cells (including span-1): %d (%.3f%%)", openCells, openCells * 100f / totalCells));
        BaseLogger.singleton().info(
                String.format("Sentence-level recall (fraction with all open cells classified correctly): %.3f%%",
                        result.sentenceNegativeRecall() * 100f));
    }

    public Binarization binarization() {
        return binarization;
    }

    public UnaryConstraintClassifier unaryConstraintClassifier() {
        return unaryConstraintClassifier;
    }

    public static void main(final String[] args) {
        run(args);
    }

    protected static class Model implements Serializable {

        private static final long serialVersionUID = 3L;

        private final Tagger posTagger;
        private final UnaryConstraintClassifier unaryConstraintClassifier;
        final String featureTemplates;
        final FloatVector avgWeights;
        final float bias;
        final Binarization binarization;

        protected Model(final Tagger posTagger, final UnaryConstraintClassifier unaryConstraintClassifier,
                final String featureTemplates, final FloatVector avgWeights, final float bias,
                final Binarization binarization) {

            this.posTagger = posTagger;
            this.unaryConstraintClassifier = unaryConstraintClassifier;
            this.featureTemplates = featureTemplates;
            this.avgWeights = avgWeights;
            this.bias = bias;
            this.binarization = binarization;
        }
    }

    private static class CompleteClosureResult extends BinaryClassifierResult {

        public int sentencesWithMisclassifiedNegative;

        /**
         * @return The percentage of sentences in which all open cells were correctly classified
         */
        public float sentenceNegativeRecall() {
            return (totalSequences - sentencesWithMisclassifiedNegative) * 1f / totalSequences;
        }
    }
}
