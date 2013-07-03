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
import java.io.File;
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
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.TokenClassifier.TokenClassifierType;

/**
 * Complete-closure classifier, as described in Bodenstab et al., 2011,
 * "Beam-Width Prediction for Efficient Context-Free Parsing". This implementation trains and tests complete-closure
 * models from the command-line, and training and classification can both be embedded directly into larger systems -
 * training primarily for grammar learning, and classification (naturally) for use during inference.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2013
 */
public class CompleteClosureClassifier extends BinaryClassifier<CompleteClosureSequence> {

    private static final long serialVersionUID = 1L;

    // If a grammar is specified, we'll binarize in the same direction; otherwise, the user must supply the binarization
    // direction and grammar format
    @Option(name = "-g", metaVar = "grammar", choiceGroup = "binarization", usage = "Grammar file. If specified, the vocabulary and lexicon from this grammar will be used.")
    protected File grammarFile;

    @Option(name = "-bin", metaVar = "direction", choiceGroup = "binarization", usage = "Binarization direction")
    protected Binarization binarization;

    @Option(name = "-gf", metaVar = "format", requires = "-bin", usage = "Grammar format")
    protected GrammarFormatType grammarFormat;

    // Training a POS-tagger requires an input grammar - we can test without it, but the output model won't be useful if
    // lexicon and vocabulary indices don't match
    @Option(name = "-ptti", metaVar = "iterations", requires = "-g", usage = "Train a POS tagger for n iterations. If specified, a POS-tagger will be trained before the complete-closure model.")
    private int posTaggerTrainingIterations = 0;

    @Option(name = "-ptft", requires = "-ptti", metaVar = "templates or file", usage = "POS-tagger feature templates (comma-delimited), or template file")
    private String posTaggerFeatureTemplates = new Tagger().DEFAULT_FEATURE_TEMPLATES();

    @Option(name = "-fp", aliases = "--full-pos", requires = "-g", usage = "Train POS tagger and cell classifier with full POS set (state-splits from grammar)")
    private boolean fullPosSet;

    public Tagger posTagger = null;

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
     * Default constructor
     */
    public CompleteClosureClassifier() {
    }

    /**
     * Used during parsing inference
     */
    public CompleteClosureClassifier(final Grammar grammar) {
        init(grammar);
        this.featureExtractor = new ConstituentBoundaryFeatureExtractor(featureTemplates, lexicon, decisionTreeUnkClassSet,
                vocabulary);
    }

    /**
     * For unit testing
     */
    public CompleteClosureClassifier(final String featureTemplates) {
        this.featureTemplates = featureTemplates;
    }

    /**
     * Initializes {@link CompleteClosureClassifier#lexicon}, {@link CompleteClosureClassifier#decisionTreeUnkClassSet}, and
     * {@link CompleteClosureClassifier#vocabulary} from the specified {@link Grammar}.
     * 
     * @param g
     */
    void init(final Grammar g) {
        this.lexicon = g.lexSet;
        this.lexicon.finalize();
        this.decisionTreeUnkClassSet = g.unkClassSet();
        this.decisionTreeUnkClassSet.finalize();
        this.vocabulary = fullPosSet ? g.posSymbolSet() : g.coarsePosSymbolSet();
        this.vocabulary.finalize();
        this.binarization = g.binarization();
        this.grammarFormat = g.grammarFormat;
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
        this.fullPosSet = tmp.fullPosSet;
        is.close();
    }

    @Override
    protected void run() throws Exception {
        if (trainingIterations > 0) {

            if (grammarFile != null) {
                BaseLogger.singleton().info("Reading grammar file...");
                final Grammar g = new LeftCscSparseMatrixGrammar(fileAsBufferedReader(grammarFile),
                        TokenClassifierType.DecisionTree, PerfectIntPairHashPackingFunction.class);
                init(g);

            } else {
                this.lexicon = new SymbolSet<String>();
                this.decisionTreeUnkClassSet = new SymbolSet<String>();
                this.vocabulary = new SymbolSet<String>();
            }

            train(inputAsBufferedReader());
        } else {
            readModel(new FileInputStream(modelFile));
            this.featureExtractor = new ConstituentBoundaryFeatureExtractor(featureTemplates, lexicon, decisionTreeUnkClassSet,
                    vocabulary);
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
            final CompleteClosureSequence sequence = new CompleteClosureSequence(line, binarization, grammarFormat,
                    lexicon, decisionTreeUnkClassSet, vocabulary);
            result.totalSequences++;

            for (int i = 0; i < sequence.classes.length; i++) {
                classify(sequence, i, result);
            }
        }
        result.time = System.currentTimeMillis() - t0;
        return result;
    }

    @SuppressWarnings("null")
    @Override
    protected void train(final BufferedReader input) throws IOException {

        this.lexicon.defaultReturnValue(Grammar.nullSymbolStr);
        this.decisionTreeUnkClassSet.defaultReturnValue(Grammar.nullSymbolStr);
        this.vocabulary.defaultReturnValue(Grammar.nullSymbolStr);

        final long startTime = System.currentTimeMillis();
        final ArrayList<CompleteClosureSequence> trainingCorpusSequences = new ArrayList<CompleteClosureSequence>();
        final ArrayList<CompleteClosureSequence> devCorpusSequences = new ArrayList<CompleteClosureSequence>();

        if (posTaggerTrainingIterations != 0) {
            this.posTagger = new Tagger(posTaggerFeatureTemplates, lexicon, decisionTreeUnkClassSet, vocabulary);
        }

        final ArrayList<TagSequence> taggerTrainingCorpusSequences = posTagger != null ? new ArrayList<TagSequence>()
                : null;
        final ArrayList<TagSequence> taggerDevCorpusSequences = posTagger != null ? new ArrayList<TagSequence>() : null;

        //
        // Read in the training corpus and map each token. For some classifiers, we also pre-compute all features, but
        // for cell classification, the number of instances is quadratic, and the memory consumption is problematic.
        // Most of the features are related to the linear token/tag sequence, so we could in theory create a more
        // complex feature storage system to reuse those feature values. But until it becomes a problem, it's easier to
        // just extract features repeatedly during training.
        //
        for (final String line : inputLines(input)) {
            try {
                trainingCorpusSequences.add(new CompleteClosureSequence(line, binarization, grammarFormat, lexicon,
                        decisionTreeUnkClassSet, vocabulary));
                if (posTagger != null) {
                    taggerTrainingCorpusSequences.add(new TagSequence(line, posTagger));
                }
            } catch (final IllegalArgumentException ignore) {
                // Skip malformed trees (e.g. INFO lines from parser output)
            }
        }
        finalizeMaps();

        //
        // If specified, train a POS-tagger. We'll use output from that tagger instead of gold POS-tags when
        // training the cell classifier, and output both models together
        //
        if (posTagger != null) {
            BaseLogger.singleton().info("Training POS tagger for " + posTaggerTrainingIterations + " iterations");
            posTagger.train(taggerTrainingCorpusSequences, taggerDevCorpusSequences, posTaggerTrainingIterations);
        }

        featureExtractor = new ConstituentBoundaryFeatureExtractor(featureTemplates, lexicon, decisionTreeUnkClassSet, vocabulary);

        //
        // Tag the training sequences with the trained POS tagger
        //
        if (posTagger != null) {
            BaseLogger.singleton().info("Tagging training corpus with the new POS tagger model");
            for (int i = 0; i < trainingCorpusSequences.size(); i++) {
                final CompleteClosureSequence ccs = trainingCorpusSequences.get(i);
                ccs.posTags = posTagger.classify(taggerTrainingCorpusSequences.get(i));
            }
        }

        // Read in the dev set
        if (devSet != null) {
            for (final String line : fileLines(devSet)) {

                final CompleteClosureSequence ccs = new CompleteClosureSequence(line, binarization, grammarFormat,
                        lexicon, decisionTreeUnkClassSet, vocabulary);
                devCorpusSequences.add(ccs);

                if (posTagger != null) {
                    ccs.posTags = posTagger.classify(new TagSequence(line, posTagger));
                }
            }
        }

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
            negativeRecallBiasSearch(devCorpusSequences, featureExtractor);
        }

        //
        // Write out the model file to disk
        //
        if (modelFile != null) {
            if (posTagger == null) {
                // A complete-closure model without an associated tagger isn't useful for downstream processing)
                throw new IllegalArgumentException("Cannot serialize " + this.getClass().getName()
                        + " without training an associated tagger.");
            }
            final FileOutputStream fos = new FileOutputStream(modelFile);
            new ObjectOutputStream(fos)
                    .writeObject(new Model(posTagger, featureTemplates, avgWeights, bias, fullPosSet));
            fos.close();
        }

        BaseLogger.singleton().info(
                String.format("Time: %d seconds\n", (System.currentTimeMillis() - startTime) / 1000));
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

    @Override
    protected void negativeRecallBiasSearch(final ArrayList<CompleteClosureSequence> devCorpusSequences,
            final FeatureExtractor<CompleteClosureSequence> fe) {

        super.negativeRecallBiasSearch(devCorpusSequences, fe);
        evaluateDevset(devCorpusSequences);
    }

    private void evaluateDevset(final ArrayList<CompleteClosureSequence> devCorpusSequences) {
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

    public static void main(final String[] args) {
        run(args);
    }

    protected static class Model implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Tagger posTagger;
        final String featureTemplates;
        final FloatVector avgWeights;
        final float bias;
        final boolean fullPosSet;

        protected Model(final Tagger posTagger, final String featureTemplates, final FloatVector avgWeights,
                final float bias, final boolean fullPosSet) {
            this.posTagger = posTagger;
            this.featureTemplates = featureTemplates;
            this.avgWeights = avgWeights;
            this.bias = bias;
            this.fullPosSet = fullPosSet;
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
