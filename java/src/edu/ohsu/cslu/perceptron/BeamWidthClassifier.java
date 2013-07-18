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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ShortAVLTreeMap;

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
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.DenseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.DenseIntVector;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.LargeBitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseIntVector;
import edu.ohsu.cslu.datastructs.vectors.LargeVector;
import edu.ohsu.cslu.datastructs.vectors.MutableSparseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.MutableSparseIntVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CompleteClosureModel;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.perceptron.MulticlassClassifier.MulticlassClassifierResult;
import edu.ohsu.cslu.perceptron.Perceptron.LossFunction;

/**
 * Beam-width prediction model, as described in Bodenstab et al., 2011,
 * "Beam-Width Prediction for Efficient Context-Free Parsing". Briefly, a beam-width model predicts, for each cell, the
 * accuracy of the prioritization model (or figure-of-merit - see {@link FigureOfMeritModel}). In cells where the FOM
 * prediction is very accurate, a smaller beam is sufficient (perhaps as small as 1 - i.e., the non-terminal ranked
 * first by the FOM's is the only one retained). The beam-width model may also predict a beam of 0 - closing the cell,
 * and thus generalizing complete-closure ({@link CompleteClosureClassifier}). This implementation trains and tests
 * beam-width models from the command-line, and training and classification can both be embedded directly into larger
 * systems - training primarily for grammar learning, and classification (naturally) for use during inference.
 * 
 * Implemented as an ordered chain of binary classifiers - e.g., beam=0 (cell closed), beam=1, beam=2, beam=4,
 * beam=MAX_BEAM_WIDTH.
 * 
 * This class (unfortunately) includes a lot of code duplicated from {@link BinaryClassifier},
 * {@link CompleteClosureClassifier}, and {@link MulticlassClassifier} but those systems are specific enough to their
 * own tasks that it wasn't easy to directly share the implementation.
 * 
 * Training a beam-width model is a multi-step process. First, we train a part-of-speech (POS) tagger. The predicted POS
 * tags are used as features for subsequent stages (and for classification during inference). We then train a
 * complete-closure (CC) model. While the CC model is not strictly necessary, it greatly speeds inference during the
 * subsequent beam-width training stage, and produces cell populations very similar to those of the beam-width model,
 * improving beam-width prediction.
 * 
 * @author Aaron Dunlop
 * @since Jul 11, 2013
 */
public class BeamWidthClassifier extends ClassifierTool<BeamWidthSequence> {

    private static final long serialVersionUID = 1L;

    @Option(name = "-beams", metaVar = "beams", separator = ",", usage = "Class boundary beam widths (excluding max value)")
    protected short[] classBoundaryBeamWidths = new short[] { 0, 1, 2, 4 };

    // If a grammar is specified, we'll binarize in the same direction; otherwise, the user must supply the binarization
    // direction and grammar format
    @Option(name = "-g", metaVar = "grammar", choiceGroup = "binarization", usage = "Grammar file. If specified, the vocabulary and lexicon from this grammar will be used.")
    protected File grammarFile;

    @Option(name = "-fom", metaVar = "model", usage = "FOM model file. If specified, the FOM will be used to prioritize non-terminals within each cell")
    protected String fomTypeOrModel = "Inside";

    @Option(name = "-bin", metaVar = "direction", choiceGroup = "binarization", usage = "Binarization direction")
    protected Binarization binarization;

    @Option(name = "-gf", metaVar = "format", requires = "-bin", usage = "Grammar format")
    protected GrammarFormatType grammarFormat;

    // Training the POS-tagger and factored-only taggers requires an input grammar - we can test without it, but the
    // output model won't be useful if lexicon and vocabulary indices don't match
    @Option(name = "-ptti", metaVar = "iterations", requires = "-g", usage = "Train a POS tagger for n iterations.")
    private int posTaggerTrainingIterations = 3;

    @Option(name = "-foti", metaVar = "iterations", requires = "-g", usage = "Train a factored-only binary classifier for n iterations.")
    private int factoredOnlyClassifierTrainingIterations;

    @Option(name = "-ptft", requires = "-ptti", metaVar = "templates or file", usage = "POS-tagger feature templates (comma-delimited), or template file")
    private String posTaggerFeatureTemplates = new Tagger().DEFAULT_FEATURE_TEMPLATES();

    @Option(name = "-b", metaVar = "bias", usage = "Biased training penalty for underestimation of beam. To correct for imbalanced training data and downstream cost) - ratio 1:<bias>")
    protected volatile float negativeTrainingBias = 1f;

    @Option(name = "-tnr", metaVar = "recall", requires = "-d", usage = "Target dev-set negative-classification recall")
    protected volatile float targetNegativeRecall = 0f;

    @Option(name = "-ccti", metaVar = "iterations", requires = "-ptti", usage = "Train the CC model  n iterations")
    private int ccClassifierTrainingIterations = 2;

    protected SymbolSet<String> vocabulary;

    private Grammar grammar;

    /** Part-of-speech tagger, executed prior to cell classification to provide POS features to the beam-width model */
    public Tagger posTagger;

    /** Factored-only binary classifier */
    private FactoredOnlyClassifier factoredOnlyClassifier;

    /**
     * Maps beam width to the associated class (as defined by {@link #classBoundaryBeamWidths}). E.g., if the class
     * boundaries are 0,1,3,6, {@link #beamWidthClasses} will be populated with {0 => 0, 1 => 1, 2 => 2, 3 => 2, 4 => 3,
     * 5 => 3, 6 => 3}, and the default return value (for beams > 6) will be 4 (beam = {@link Short#MAX_VALUE}).
     */
    protected volatile short[] beamWidthClasses;

    /** Used only during training */
    private volatile CompleteClosureClassifier ccClassifier;

    @Option(name = "-lr", metaVar = "rate", usage = "Learning rate")
    private volatile float learningRate = 0.1f;

    /** Threshold above which we'll represent weight vectors sparsely */
    private static int MAX_DENSE_STORAGE_SIZE = 100 * 1024;

    /** Model parameters */
    protected transient FloatVector[] avgWeights = null;

    // Transient fields used only during training
    private transient FloatVector[] rawWeights;
    private transient IntVector[] lastAveraged;
    private transient int[] lastExampleAllUpdated;

    private transient int trainExampleNumber = 0;
    private transient LossFunction lossFunction;

    /** Used during training to find the gold beam widths */
    transient CartesianProductHashSpmlParser ccParser;

    // Maps feature index (long) -> offset in weight arrays (int)
    protected Long2IntOpenHashMap parallelArrayOffsetMap;

    /**
     * Parallel arrays of populated tag index and feature weights. Note that many features will only be observed in
     * conjunction with a subset of the tags; feature weights for other tags will be 0, and we need not store those
     * weights. To compactly represent the non-0 weights, we use parallel arrays, including a series of tag/weight
     * entries for each observed feature. For convenience, the first entry in the sequence for each feature is the
     * number of populated weights (the associated weight entry is ignored).
     * 
     * When we access a feature during tagging, we will probe the weight associated with that feature for each of the
     * (possibly many) tags (call the tag-set T). Arranging the features in separate {@link Vector}s (as in
     * {@link AveragedPerceptron} is conceptually clean and simple, but requires we probe O(T) individual HashMaps, with
     * an essentially random memory-access pattern. Aligning all weights for a specific feature together allows a single
     * hashtable lookup (in {@link #parallelArrayOffsetMap}), followed by a linear scan of these parallel arrays.
     */
    protected short[] parallelWeightArrayTags;
    protected float[] parallelWeightArray;

    /**
     * Model bias. Learned in {@link #precisionBiasSearch(ArrayList, FeatureExtractor)} or
     * {@link #negativeRecallBiasSearch(ArrayList, FeatureExtractor)}.
     */
    private float[] biases;

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
     * s1,s2,s3,s4,s5,s10,s20,s30,s40,s50
     * 
     * # Relative span features
     * rs1,rs2,rs4,rs6,rs8,rs10
     * </pre>
     */
    private final static String DEFAULT_FEATURE_TEMPLATES = "ltm2,ltm1,lt,ltp1,rtm1,rt,rtp1,ltp1,lwm1,lw,rw,rwp1,lum1,lu,ru,rup1,ltm1_lt,rt_rtp1,lwm1_lw,rw_rwp1,lum1_lu,ru_rup1,s1,s2,s3,s4,s5,s10,s20,s30,s40,s50,rs2,rs4,rs6,rs8,rs10";

    @Override
    protected final String DEFAULT_FEATURE_TEMPLATES() {
        return DEFAULT_FEATURE_TEMPLATES;
    }

    /**
     * Default constructor
     */
    public BeamWidthClassifier() {
    }

    /**
     * Used during parsing inference
     */
    public BeamWidthClassifier(final Grammar grammar) {
        init(grammar);
        this.featureExtractor = new ConstituentBoundaryFeatureExtractor<BeamWidthSequence>(featureTemplates, lexicon,
                decisionTreeUnkClassSet, grammar.coarsePosSymbolSet(), false);
    }

    /**
     * For unit testing
     */
    public BeamWidthClassifier(final String featureTemplates) {
        this.featureTemplates = featureTemplates;
    }

    @Override
    void init(final Grammar g) {
        super.init(g);
        this.grammar = g;
        this.binarization = g.binarization();
        this.grammarFormat = g.grammarFormat;
        this.vocabulary = g.nonTermSet;
        this.vocabulary.finalize();
    }

    @Override
    public void readModel(final InputStream is) throws IOException, ClassNotFoundException {
        // Read in the model parameters as a temporary java serialized object and copy into this object
        final ObjectInputStream ois = new ObjectInputStream(is);
        final Model tmp = (Model) ois.readObject();
        ois.close();
        this.vocabulary = tmp.vocabulary;
        this.posTagger = tmp.posTagger;
        this.factoredOnlyClassifier = tmp.factoredOnlyClassifier;
        this.featureTemplates = tmp.featureTemplates;
        this.parallelArrayOffsetMap = tmp.parallelArrayOffsetMap;
        this.parallelWeightArray = tmp.parallelWeightArray;
        this.parallelWeightArrayTags = tmp.parallelWeightArrayTags;
        this.biases = tmp.biases;
        this.decisionTreeUnkClassSet = tmp.posTagger.decisionTreeUnkClassSet;
        this.classBoundaryBeamWidths = tmp.classBoundaryBeamWidths;
        is.close();
    }

    @Override
    protected void run() throws Exception {

        if (trainingIterations > 0) {

            if (grammarFile != null) {
                BaseLogger.singleton().info("Reading grammar file...");
                final Grammar g = new LeftCscSparseMatrixGrammar(fileAsBufferedReader(grammarFile),
                        new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class);
                init(g);

            } else {
                this.lexicon = new SymbolSet<String>();
                this.decisionTreeUnkClassSet = new SymbolSet<String>();
                this.vocabulary = new SymbolSet<String>();
            }

            this.beamWidthClasses = new short[classBoundaryBeamWidths[classBoundaryBeamWidths.length - 1] + 1];
            for (short beam = 0, beamClass = 0; beam <= classBoundaryBeamWidths[classBoundaryBeamWidths.length - 1]; beam++) {
                beamWidthClasses[beam] = beamClass;
                if (beam == classBoundaryBeamWidths[beamClass]) {
                    beamClass++;
                }
            }

            train(inputAsBufferedReader());
        } else {
            readModel(new FileInputStream(modelFile));
            this.featureExtractor = new ConstituentBoundaryFeatureExtractor<BeamWidthSequence>(featureTemplates,
                    lexicon, decisionTreeUnkClassSet, grammar.coarsePosSymbolSet(), false);
            classify(inputAsBufferedReader());
        }
    }

    /**
     * Classifies the sequences read from <code>input</code>
     * 
     * @param input
     * 
     * @throws IOException if a read fails
     */
    protected void classify(final BufferedReader input) throws IOException {

        // Not implemented at the moment. If we want to test from the command-line, we'd need to create a parser
        // instance here, parse the input tree, and classify.
        throw new UnsupportedOperationException();
        // final BeamWidthResult result = new BeamWidthResult(classBoundaryBeamWidths.length + 1);
        //
        // for (final String line : inputLines(input)) {
        // final BinaryTree<String> binaryTree = NaryTree.read(line, String.class).binarize(grammarFormat,
        // binarization);
        // final BeamWidthSequence sequence = new BeamWidthSequence(binaryTree, this);
        // result.totalSentences++;
        // classify(sequence, result);
        // }
        // outputDevsetAccuracy(1, result);
    }

    @Override
    protected void train(final BufferedReader input) throws IOException {

        // Allow up to 25 MB of input
        input.mark(25 * 1024 * 1024);

        this.lexicon.defaultReturnValue(Grammar.nullSymbolStr);
        this.decisionTreeUnkClassSet.defaultReturnValue(Grammar.nullSymbolStr);

        final long startTime = System.currentTimeMillis();

        final ArrayList<TagSequence> taggerTrainingCorpusSequences = new ArrayList<TagSequence>();
        final ArrayList<TagSequence> taggerDevCorpusSequences = new ArrayList<TagSequence>();

        final ArrayList<CompleteClosureSequence> ccTrainingCorpusSequences = new ArrayList<CompleteClosureSequence>();
        final ArrayList<CompleteClosureSequence> ccDevCorpusSequences = new ArrayList<CompleteClosureSequence>();

        this.posTagger = new Tagger(posTaggerFeatureTemplates, lexicon, decisionTreeUnkClassSet,
                grammar.coarsePosSymbolSet());
        posTagger.trainingIterations = posTaggerTrainingIterations;

        this.ccClassifier = new CompleteClosureClassifier(grammar);
        ccClassifier.posTagger = posTagger;
        ccClassifier.trainingIterations = ccClassifierTrainingIterations;
        // For the moment, at least, we'll use the same negative training bias and target negative recall for the
        // intermediate CC classifier as for the final beam-width model.
        ccClassifier.targetNegativeRecall = targetNegativeRecall;
        ccClassifier.negativeTrainingBias = negativeTrainingBias;

        BaseLogger.singleton().info("Reading and mapping the training corpus");

        //
        // Read in the training corpus and create training sequences for POS-tagger and CC training
        //
        for (final String line : inputLines(input)) {
            try {
                final BinaryTree<String> binaryTree = NaryTree.read(line, String.class).binarize(grammarFormat,
                        binarization);
                ccTrainingCorpusSequences.add(new CompleteClosureSequence(binaryTree, ccClassifier));
                taggerTrainingCorpusSequences.add(new TagSequence(line, posTagger));
            } catch (final IllegalArgumentException ignore) {
                // Skip malformed trees (e.g. INFO lines from parser output)
            }
        }
        finalizeMaps();

        // Read in the dev set
        if (devSet != null) {
            for (final String line : fileLines(devSet)) {
                final BinaryTree<String> binaryTree = NaryTree.read(line, String.class).binarize(grammarFormat,
                        binarization);
                ccDevCorpusSequences.add(new CompleteClosureSequence(binaryTree, ccClassifier));
                taggerDevCorpusSequences.add(new TagSequence(line, posTagger));
            }
        }

        //
        // Train a POS-tagger and complete-closure model. We'll use output from that tagger instead of gold POS-tags
        // when training the CC model and the beam-width model, and include the POS-tagger model in the final beam-width
        // model
        //
        BaseLogger.singleton().info("Training POS tagger for " + posTaggerTrainingIterations + " iterations");
        posTagger.train(taggerTrainingCorpusSequences, taggerDevCorpusSequences, posTaggerTrainingIterations);

        // Replace the gold POS tags in the CC sequences with predicted tags (closer to what we'll have at inference
        // time)
        for (int i = 0; i < ccTrainingCorpusSequences.size(); i++) {
            ccTrainingCorpusSequences.get(i).posTags = posTagger.classify(taggerTrainingCorpusSequences.get(i));
        }

        ccClassifier.train(ccTrainingCorpusSequences, ccDevCorpusSequences);

        //
        // Reread the training corpus, and create sequences for beam-width training. (Note: creating these sequences
        // requires parsing inference, which requires the POS-tagger and CC-classifier models, so we can't combine this
        // step with creating those sequences
        //

        // Initialize the parser and feature extractor
        try {
            final ParserDriver opts = new ParserDriver();
            opts.setGrammar(grammar);
            opts.researchParserType = ResearchParserType.CartesianProductHashMl;
            opts.cellSelectorModel = new CompleteClosureModel(ccClassifier);
            opts.fomModel = ParserDriver.readFomModel(fomTypeOrModel, null, grammar);

            this.ccParser = new CartesianProductHashSpmlParser(opts, new LeftCscSparseMatrixGrammar(grammar));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        this.featureExtractor = new ConstituentBoundaryFeatureExtractor<BeamWidthSequence>(featureTemplates, lexicon,
                decisionTreeUnkClassSet, grammar.coarsePosSymbolSet(), false);

        // Create and POS-tag sequences
        input.reset();
        final ArrayList<BeamWidthSequence> beamWidthTrainingCorpusSequences = new ArrayList<BeamWidthSequence>();
        final ArrayList<FactoredOnlySequence> factoredOnlyTrainingCorpusSequences = new ArrayList<FactoredOnlySequence>();
        readSequences(input, beamWidthTrainingCorpusSequences, factoredOnlyTrainingCorpusSequences, "training");

        final ArrayList<BeamWidthSequence> beamWidthDevCorpusSequences = new ArrayList<BeamWidthSequence>();
        final ArrayList<FactoredOnlySequence> factoredOnlyDevCorpusSequences = new ArrayList<FactoredOnlySequence>();

        // Read in the dev set
        if (devSet != null) {
            BaseLogger.singleton().info("Reading and parsing dev-set");
            readSequences(fileAsBufferedReader(devSet), beamWidthDevCorpusSequences, factoredOnlyDevCorpusSequences,
                    "dev-set");
        }

        //
        // Iterate over training corpus, training the factored-only model
        //
        if (factoredOnlyClassifierTrainingIterations > 0) {
            BaseLogger.singleton()
                    .info("Training the factored-only model for " + factoredOnlyClassifierTrainingIterations
                            + " iterations.");

            this.factoredOnlyClassifier = new FactoredOnlyClassifier(featureTemplates, lexicon, decisionTreeUnkClassSet);
            factoredOnlyClassifier.trainingIterations = factoredOnlyClassifierTrainingIterations;
            factoredOnlyClassifier.negativeTrainingBias = negativeTrainingBias;
            factoredOnlyClassifier.targetNegativeRecall = targetNegativeRecall;
            factoredOnlyClassifier.train(factoredOnlyTrainingCorpusSequences, factoredOnlyDevCorpusSequences);
        }

        //
        // Iterate over training corpus, training the beam-width model
        //
        BaseLogger.singleton().info("Training the beam-width model for " + trainingIterations + " iterations.");
        for (int i = 1, j = 0; i <= trainingIterations; i++, j = 0) {

            for (final BeamWidthSequence sequence : beamWidthTrainingCorpusSequences) {
                final int cells = sequence.sentenceLength * (sequence.sentenceLength + 1) / 2;
                for (int cellIndex = 0; cellIndex < cells; cellIndex++) {
                    train(sequence.goldClass(cellIndex), featureExtractor.featureVector(sequence, cellIndex));
                }

                progressBar(100, 5000, j++);
            }

            // Skip the last iteration - we'll test after we finalize below
            if (!beamWidthDevCorpusSequences.isEmpty() && i < trainingIterations) {
                System.out.println();
                outputDevsetAccuracy(i, classify(beamWidthDevCorpusSequences));
            }
        }

        // Store the trained model in a memory- and cache-efficient format for tagging (we do this even if we're not
        // writing out the serialized model, specifically so we can unit test train() and tag())
        finalizeModel();

        // Test on the dev-set
        if (!beamWidthDevCorpusSequences.isEmpty()) {
            System.out.println();
            outputDevsetAccuracy(trainingIterations, classify(beamWidthDevCorpusSequences));
        }

        //
        // Search for a bias that satisfies the requested precision or recall
        //
        if (targetNegativeRecall != 0) {
            negativeRecallBiasSearch(beamWidthDevCorpusSequences, featureExtractor);
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
            new ObjectOutputStream(fos).writeObject(new Model(vocabulary, posTagger, factoredOnlyClassifier,
                    classBoundaryBeamWidths, featureTemplates, parallelArrayOffsetMap, parallelWeightArrayTags,
                    parallelWeightArray, biases));
            fos.close();
        }

        BaseLogger.singleton().info(
                String.format("Time: %d seconds\n", (System.currentTimeMillis() - startTime) / 1000));
    }

    private void readSequences(final BufferedReader input, final ArrayList<BeamWidthSequence> beamWidthSequences,
            final ArrayList<FactoredOnlySequence> factoredOnlySequences, final String corpus) throws IOException {

        BaseLogger.singleton().info("Reading and parsing " + corpus + " data");
        final long t0 = System.currentTimeMillis();
        int trainingSetSentences = 0, trainingSetWords = 0, trainingSetParseFailures = 0;

        for (final String line : inputLines(input)) {
            try {
                final TagSequence tagSequence = new TagSequence(line, posTagger);

                final BinaryTree<String> binaryTree = NaryTree.read(line, String.class).binarize(grammarFormat,
                        binarization);
                trainingSetWords += binaryTree.leaves();
                trainingSetSentences++;

                try {
                    final ParseTask parseTask = ccParser.parseSentence(line);
                    if (parseTask.parseFailed()) {
                        throw new IllegalArgumentException("Parse failed");
                    }
                    final PackedArrayChart chart = ccParser.chart;

                    final BeamWidthSequence beamWidthSequence = new BeamWidthSequence(chart, binaryTree, this);
                    beamWidthSequence.posTags = posTagger.classify(tagSequence);
                    beamWidthSequences.add(beamWidthSequence);

                    final FactoredOnlySequence factoredOnlySequence = new FactoredOnlySequence(chart, binaryTree, this);
                    factoredOnlySequence.posTags = beamWidthSequence.posTags;
                    factoredOnlySequences.add(factoredOnlySequence);
                } catch (final IllegalArgumentException e) {
                    trainingSetParseFailures++;
                }
            } catch (final IllegalArgumentException ignore) {
                // Skip malformed trees (e.g. INFO lines from parser output)
            }
            progressBar(100, 5000, trainingSetSentences);
        }
        finalizeMaps();
        final long ms = System.currentTimeMillis() - t0;
        BaseLogger.singleton().info(
                String.format("Parsed %d of %d %s sentences in %d ms  (%d failures, %.1f w/s)", trainingSetSentences
                        - trainingSetParseFailures, trainingSetSentences, corpus, ms, trainingSetParseFailures,
                        trainingSetWords * 1000f / ms));
    }

    protected short classify(final float[] dotProducts) {
        // Evaluate each dot-product as an independent binary classification, searching for the first positive
        // classification
        for (short c = 0; c < dotProducts.length; c++) {
            if (dotProducts[c] + biases[c] > 0) {
                return c;
            }
        }
        return (short) dotProducts.length;
    }

    /**
     * Executes a single training step
     * 
     * @param goldClass
     * @param featureVector
     */
    protected void train(final short goldClass, final BitVector featureVector) {

        trainExampleNumber++;

        for (int beamClass = 0; beamClass < classBoundaryBeamWidths.length; beamClass++) {

            final boolean beamClassClosed = (goldClass <= beamClass);

            if (this.rawWeights == null) {
                this.rawWeights = new FloatVector[classBoundaryBeamWidths.length];
                this.avgWeights = new FloatVector[classBoundaryBeamWidths.length];
                this.lastAveraged = new IntVector[classBoundaryBeamWidths.length];
                this.lastExampleAllUpdated = new int[classBoundaryBeamWidths.length];
                this.biases = new float[classBoundaryBeamWidths.length];

                for (int i = 0; i < rawWeights.length; i++) {
                    // We need to initialize a new model; we depend on the FeatureExtractor to provide a vector of
                    // appropriate length
                    final long vectorLength = featureVector.length();
                    if (vectorLength <= MAX_DENSE_STORAGE_SIZE) {
                        this.rawWeights[i] = new DenseFloatVector(vectorLength);
                        this.avgWeights[i] = new DenseFloatVector(vectorLength);
                        this.lastAveraged[i] = new DenseIntVector(vectorLength, 0);

                    } else if (vectorLength <= Integer.MAX_VALUE) {
                        this.rawWeights[i] = new MutableSparseFloatVector(vectorLength);
                        this.avgWeights[i] = new MutableSparseFloatVector(vectorLength);
                        this.lastAveraged[i] = new MutableSparseIntVector(vectorLength);

                    } else {
                        this.rawWeights[i] = new LargeSparseFloatVector(vectorLength);
                        this.avgWeights[i] = new LargeSparseFloatVector(vectorLength);
                        this.lastAveraged[i] = new LargeSparseIntVector(vectorLength);
                    }
                }
                this.lossFunction = new Perceptron.BiasedLoss(new float[] { negativeTrainingBias, 1 });
            }

            final float dotProduct = featureVector.dotProduct(rawWeights[beamClass]);
            final boolean classification = dotProduct >= 0;
            if (classification != beamClassClosed) {
                final float loss = lossFunction.computeLoss(beamClassClosed ? 1 : 0, beamClassClosed ? 0 : 1);
                final float alpha = beamClassClosed ? (loss * learningRate) : (-loss * learningRate);
                BinaryClassifier.update(beamClassClosed, featureVector, rawWeights[beamClass], avgWeights[beamClass],
                        alpha, trainExampleNumber, lastAveraged[beamClass]);
            }
        }
    }

    private short classifyWithParallelArray(final BitVector featureVector) {

        final float[] dotProducts = new float[classBoundaryBeamWidths.length];

        // Unfortunately, we need separate cases for LargeVector and normal Vector classes
        if (featureVector instanceof LargeVector) {
            final LargeBitVector largeFeatureVector = (LargeBitVector) featureVector;

            // Iterate over each feature
            for (final long feature : largeFeatureVector.longValues()) {

                final int offset = parallelArrayOffsetMap.get(feature);
                // Skip any features that aren't populated in the model (those we didn't observe in the training
                // data)
                if (offset < 0) {
                    continue;
                }

                // The first 'tag' position denotes the number of populated weights for this feature
                final int end = offset + parallelWeightArrayTags[offset];

                // Add each non-0 weight to the appropriate dot-product
                for (int i = offset + 1; i <= end; i++) {
                    dotProducts[parallelWeightArrayTags[i]] += parallelWeightArray[i];
                }
            }
        } else {
            for (final int feature : featureVector.values()) {
                final int offset = parallelArrayOffsetMap.get(feature);
                if (offset < 0) {
                    continue;
                }
                final int end = offset + parallelWeightArrayTags[offset];
                for (int i = offset + 1; i <= end; i++) {
                    dotProducts[parallelWeightArrayTags[i]] += parallelWeightArray[i];
                }
            }
        }

        return classify(dotProducts);
    }

    private short classifyWithTrainingVectors(final BitVector featureVector) {

        final float[] dotProducts = new float[classBoundaryBeamWidths.length];
        for (int beamClass = 0; beamClass < lastExampleAllUpdated.length; beamClass++) {

            if (lastExampleAllUpdated[beamClass] < trainExampleNumber) {
                BinaryClassifier.averageAllFeatures(rawWeights[beamClass], avgWeights[beamClass],
                        lastAveraged[beamClass], trainExampleNumber);

                // manually record when we last updated all features. Check during
                // classification and model writing to ensure model is up-to-date
                lastExampleAllUpdated[beamClass] = trainExampleNumber;
            }

            dotProducts[beamClass] = avgWeights[beamClass].dotProduct(featureVector);
        }
        return classify(dotProducts);
    }

    /**
     * Classifies the supplied sequence, populating {@link BeamWidthSequence#predictedClasses}.
     * 
     * @param sequence
     */
    public void classify(final BeamWidthSequence sequence) {

        sequence.allocatePredictedClasses();

        for (int cellIndex = 0; cellIndex < sequence.predictedClasses.length; cellIndex++) {
            final BitVector featureVector = featureExtractor.featureVector(sequence, cellIndex);
            sequence.setPredictedClass(cellIndex, classifyWithParallelArray(featureVector));
        }
    }

    /**
     * Classifies the supplied <code>sequence</code>, incrementing counts in the <code>result</code>. Note that this
     * method is intended for training only, and that {@link BeamWidthSequence#predictedClasses} is <em>not</em>
     * populated.
     * 
     * @param sequence
     * @param result
     */
    private void classify(final BeamWidthSequence sequence, final BeamWidthResult result) {

        sequence.allocatePredictedClasses();
        int underestimatedBeam = 0;

        for (int cellIndex = 0; cellIndex < sequence.predictedClasses.length; cellIndex++) {
            final BitVector featureVector = featureExtractor.featureVector(sequence, cellIndex);
            sequence.setPredictedClass(cellIndex, classifyWithTrainingVectors(featureVector));

            final short goldClass = sequence.goldClass(cellIndex);
            final short predictedClass = sequence.predictedClass(cellIndex);
            result.increment(goldClass, predictedClass);
            if (predictedClass < goldClass) {
                underestimatedBeam++;
            }
        }

        result.sentences++;
        if (underestimatedBeam > 0) {
            result.sentencesWithMisclassifiedNegative++;
        }

        // Null out predicted classes
        sequence.clearPredictedClasses();
    }

    /**
     * Classifies the supplied sequences
     * 
     * @param sequences
     * @return results of classifying the input sequences (if they contain gold classifications)
     */
    protected BeamWidthResult classify(final ArrayList<BeamWidthSequence> sequences) {

        final long t0 = System.currentTimeMillis();
        final BeamWidthResult result = new BeamWidthResult(classBoundaryBeamWidths.length + 1);

        for (final BeamWidthSequence sequence : sequences) {
            result.totalSentences++;
            classify(sequence, result);
        }
        result.time = (int) (System.currentTimeMillis() - t0);
        return result;
    }

    private void finalizeModel() {

        // Average all weights
        for (int i = 0; i < avgWeights.length; i++) {
            BinaryClassifier.averageAllFeatures(rawWeights[i], avgWeights[i], lastAveraged[i], trainExampleNumber);
        }

        // And store in the compact, cache-efficient data structures
        final Long2ShortAVLTreeMap observedWeightCounts = MulticlassClassifier.observedWeightCounts(avgWeights);
        final int arraySize = MulticlassClassifier.finalizedArraySize(observedWeightCounts);

        this.parallelArrayOffsetMap = new Long2IntOpenHashMap();
        this.parallelArrayOffsetMap.defaultReturnValue(-1);
        this.parallelWeightArrayTags = new short[arraySize];
        this.parallelWeightArray = new float[arraySize];

        MulticlassClassifier.finalizeModel(avgWeights, observedWeightCounts, parallelArrayOffsetMap,
                parallelWeightArrayTags, parallelWeightArray);
    }

    // protected void update(final boolean goldClass, final BitVector featureVector, final float alpha) {
    // BinaryClassifier.update(goldClass, featureVector, rawWeights, avgWeights, alpha, trainExampleNumber,
    // lastAveraged);
    // }

    private void outputDevsetAccuracy(final int iteration, final BeamWidthResult result) {

        BaseLogger.singleton().info(
                String.format("Iteration=%d Devset Exact Match=%.2f  neg-R=%.3f  Sentence neg-R=%.3f  Time=%d",
                        iteration, result.accuracy() * 100f, result.totalNegativeRecall() * 100f,
                        result.sentenceNegativeRecall(), result.time));
    }

    /**
     * Performs a binary search to find biases for each binary classifier yielding the desired negative-class precision.
     * Mostly copied from {@link BinaryClassifier#negativeRecallBiasSearch(ArrayList, FeatureExtractor)}, but the two
     * tasks are just dissimilar enough that there isn't an easy way to share the code.
     * 
     * @param devCorpusSequences
     * @param fe feature extractor
     */
    protected void negativeRecallBiasSearch(final ArrayList<BeamWidthSequence> devCorpusSequences,
            final FeatureExtractor<BeamWidthSequence> fe) {

        // Short-circuit search if we're not making material changes in bias
        final float MIN_BIAS_DELTA = .01f;

        // Set a stopping criteria - e.g., if recall=.98, we'll find a bias that produces .979 <= r <= .981
        final float epsilon = (1 - targetNegativeRecall) / 20;

        // Perform the same search for each class
        for (int goldClass = 0; goldClass < avgWeights.length; goldClass++) {

            BaseLogger.singleton().info(
                    String.format("Performing bias search on class %d for negative-classification recall %.5f",
                            goldClass, targetNegativeRecall));

            BeamWidthResult result = classify(devCorpusSequences);

            //
            // Binary search over bias settings, until we find the desired recall
            //
            float lowBias = avgWeights[goldClass].min() * fe.templateCount();
            float highBias = avgWeights[goldClass].max() * fe.templateCount();

            for (float r = result.negativeRecall(goldClass); r < targetNegativeRecall - epsilon
                    || r > targetNegativeRecall + epsilon || Float.isNaN(r);) {

                final float prevBias = biases[goldClass];
                biases[goldClass] = lowBias + (highBias - lowBias) / 2;

                // Exit if we're not changing bias measurably
                if (Math.abs(biases[goldClass] - prevBias) < MIN_BIAS_DELTA) {
                    BaseLogger.singleton().info(String.format("Converged at bias=%.5f", biases[goldClass]));
                    break;
                }

                // Classify the dev-set
                result = classify(devCorpusSequences);
                BaseLogger.singleton().info(
                        String.format("Bias=%.5f  Exact=%.2f  P=%.3f  neg-R=%.3f  Sentence neg-R=%.3f",
                                biases[goldClass], result.accuracy() * 100f, result.precision(goldClass) * 100f,
                                result.negativeRecall(goldClass) * 100f, result.sentenceNegativeRecall() * 100f));
                r = result.negativeRecall(goldClass);

                if (r < targetNegativeRecall) {
                    highBias = biases[goldClass];
                } else {
                    lowBias = biases[goldClass];
                }
            }
        }
    }

    public FactoredOnlyClassifier factoredOnlyClassifier() {
        return factoredOnlyClassifier;
    }

    public short beamWidth(final short beamClass) {
        return beamClass >= classBoundaryBeamWidths.length ? Short.MAX_VALUE : classBoundaryBeamWidths[beamClass];
    }

    // private void evaluateDevset(final ArrayList<BeamWidthSequence> devCorpusSequences) {
    // final long t0 = System.currentTimeMillis();
    // final BeamWidthResult result = new BeamWidthResult();
    //
    // for (final BeamWidthSequence sequence : devCorpusSequences) {
    // result.totalSequences++;
    // for (int i = 0; i < sequence.predictedClasses.length; i++) {
    // classify(sequence, i, result);
    // }
    //
    // for (int i = 0; i < sequence.predictedClasses.length; i++) {
    // if (sequence.classes[i] == false && sequence.predictedClasses[i] == true) {
    // result.sentencesWithMisclassifiedNegative++;
    // break;
    // }
    // }
    // }
    // result.time = System.currentTimeMillis() - t0;
    //
    // // Compute and report final cell-closure statistics on the development set
    // int totalWords = 0;
    // for (final BeamWidthSequence sequence : devCorpusSequences) {
    // totalWords += sequence.mappedTokens.length;
    // }
    //
    // // positiveExamples + negativeExamples + span-1 cells
    // final int totalCells = result.positiveExamples + result.negativeExamples + totalWords;
    // final int openCells = result.classifiedNegative + totalWords;
    //
    // BaseLogger.singleton().info(
    // String.format("Open cells (including span-1): %d (%.3f%%)", openCells, openCells * 100f / totalCells));
    // BaseLogger.singleton().info(
    // String.format("Sentence-level recall (fraction with all open cells classified correctly): %.3f%%",
    // result.sentenceNegativeRecall() * 100f));
    // }
    //
    public static void main(final String[] args) {
        run(args);
    }

    /**
     * Represents the result of a binary classification run and computes precision, recall, etc.
     */
    private static class BeamWidthResult extends MulticlassClassifierResult {

        int[] positiveExamples, negativeExamples,
        /** Cells correctly classified as > the beam width */
        correctNegative,
        /** Cells correctly classified as <= the beam width */
        correctPositive,
        /** Cells classified in the correct class */
        exactMatch;

        int totalSentences = 0, totalCells;
        public int sentencesWithMisclassifiedNegative;

        public BeamWidthResult(final int classes) {
            super();
            this.correctNegative = new int[classes];
            this.negativeExamples = new int[classes];
            this.correctPositive = new int[classes];
            this.exactMatch = new int[classes];
            this.positiveExamples = new int[classes];
        }

        /**
         * @return The fraction of sentences in which all open cells were correctly classified
         */
        public float sentenceNegativeRecall() {
            return (totalSentences - sentencesWithMisclassifiedNegative) * 1f / totalSentences;
        }

        /**
         * Increments result counters
         * 
         * @param goldClass
         * @param predictedClass
         */
        public void increment(final int goldClass, final int predictedClass) {

            totalCells++;

            for (int currentClass = 0; currentClass < negativeExamples.length; currentClass++) {

                if (goldClass <= currentClass) {
                    // Correct classification is positive
                    positiveExamples[currentClass]++;

                    if (predictedClass <= currentClass) {
                        correctPositive[currentClass]++;
                    }

                } else {
                    // Correct classification is negative
                    negativeExamples[currentClass]++;

                    if (predictedClass > currentClass) {
                        correctNegative[currentClass]++;
                    }
                }
            }

            if (predictedClass == goldClass) {
                exactMatch[goldClass]++;
            }
        }

        public float precision(final int goldClass) {
            final int incorrectPositive = negativeExamples[goldClass] - correctNegative[goldClass];
            return correctPositive[goldClass] * 1f / (correctPositive[goldClass] + incorrectPositive);
        }

        public float negativeRecall(final int goldClass) {
            return (1f * correctNegative[goldClass] / negativeExamples[goldClass]);
        }

        @Override
        public float accuracy() {
            int totalExactMatch = 0;
            for (int i = 0; i < exactMatch.length; i++) {
                totalExactMatch += exactMatch[i];
            }
            return totalExactMatch * 1f / totalCells;
        }

        public float totalNegativeRecall() {
            int totalCorrectNegative = 0, totalNegativeExamples = 0;
            for (int i = 0; i < correctNegative.length; i++) {
                totalCorrectNegative += correctNegative[i];
                totalNegativeExamples += negativeExamples[i];
            }
            return totalCorrectNegative * 1f / totalNegativeExamples;
        }
    }

    protected class FactoredOnlyClassifier extends BinaryClassifier<FactoredOnlySequence> {

        private static final long serialVersionUID = 1L;

        /**
         * @param featureTemplates
         * @param lexicon
         * @param decisionTreeUnkClassSet
         */
        public FactoredOnlyClassifier(final String featureTemplates, final SymbolSet<String> lexicon,
                final SymbolSet<String> decisionTreeUnkClassSet) {

            this.featureTemplates = featureTemplates;
            this.lexicon = lexicon;
            this.decisionTreeUnkClassSet = decisionTreeUnkClassSet;
            // TODO We should really probably exclude span-1 cells, but that doesn't (currently) agree with the feature
            // extractors
            this.featureExtractor = new ConstituentBoundaryFeatureExtractor<FactoredOnlySequence>(featureTemplates,
                    lexicon, decisionTreeUnkClassSet, grammar.coarsePosSymbolSet(), false);
        }

        /**
         * @param trainingCorpusSequences
         * @param devCorpusSequences
         */
        public void train(final ArrayList<FactoredOnlySequence> trainingCorpusSequences,
                final ArrayList<FactoredOnlySequence> devCorpusSequences) {
            //
            // Iterate over training corpus, training the model
            //
            BaseLogger.singleton().info(
                    "Training the complete-closure model for " + trainingIterations + " iterations.");
            for (int i = 1, j = 0; i <= trainingIterations; i++, j = 0) {

                for (final FactoredOnlySequence sequence : trainingCorpusSequences) {
                    for (int k = 0; k < sequence.length(); k++) {
                        train(sequence.goldClass(k), featureExtractor.featureVector(sequence, k));
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
                    String.format(
                            "Iteration=%d Devset Accuracy=%.2f  P=%.3f  R=%.3f  neg-P=%.3f  neg-R=%.3f  Time=%d\n",
                            iteration, result.accuracy() * 100f, result.precision() * 100f, result.recall() * 100f,
                            result.negativePrecision() * 100f, result.negativeRecall() * 100f, result.time));
        }

        void evaluateDevset(final ArrayList<FactoredOnlySequence> devCorpusSequences) {
            final long t0 = System.currentTimeMillis();
            final BinaryClassifierResult result = new BinaryClassifierResult();

            int sentencesWithMisclassifiedNegative = 0;

            for (final FactoredOnlySequence sequence : devCorpusSequences) {
                result.totalSequences++;
                sequence.allocatePredictedClasses();
                for (int cellIndex = 0; cellIndex < sequence.predictedClasses.length; cellIndex++) {
                    classify(sequence, cellIndex, result);
                }

                for (int cellIndex = 0; cellIndex < sequence.predictedClasses.length; cellIndex++) {
                    if (sequence.goldClass(cellIndex) == false && sequence.predictedClass(cellIndex) == true) {
                        sentencesWithMisclassifiedNegative++;
                        break;
                    }
                }
                sequence.clearPredictedClasses();
            }
            result.time = System.currentTimeMillis() - t0;

            // Compute and report final cell-closure statistics on the development set
            int totalWords = 0;
            for (final FactoredOnlySequence sequence : devCorpusSequences) {
                totalWords += sequence.mappedTokens.length;
            }

            // positiveExamples + negativeExamples + span-1 cells
            final int totalCells = result.positiveExamples + result.negativeExamples + totalWords;
            final int factoredOnlyCells = result.classifiedPositive;

            final float sentenceNegativeRecall = (devCorpusSequences.size() - sentencesWithMisclassifiedNegative) * 1f
                    / devCorpusSequences.size();

            BaseLogger.singleton().info(
                    String.format("Factored-only cells (including span-1): %d (%.3f%%)", factoredOnlyCells,
                            factoredOnlyCells * 100f / totalCells));
            BaseLogger.singleton().info(
                    String.format("Sentence-level recall (fraction with all open cells classified correctly): %.3f%%",
                            sentenceNegativeRecall * 100f));
        }

        @Override
        protected String DEFAULT_FEATURE_TEMPLATES() {
            return DEFAULT_FEATURE_TEMPLATES;
        }

        @Override
        protected void train(final BufferedReader input) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void run() throws Exception {
            throw new UnsupportedOperationException();
        }

    }

    protected static class Model implements Serializable {

        private static final long serialVersionUID = 1L;

        private final SymbolSet<String> vocabulary;
        private final Tagger posTagger;
        private final FactoredOnlyClassifier factoredOnlyClassifier;

        private final short[] classBoundaryBeamWidths;
        private final String featureTemplates;
        private final Long2IntOpenHashMap parallelArrayOffsetMap;
        private final short[] parallelWeightArrayTags;
        private final float[] parallelWeightArray;
        private final float[] biases;

        protected Model(final SymbolSet<String> vocabulary, final Tagger posTagger,
                final FactoredOnlyClassifier factoredOnlyClassifier, final short[] classBoundaryBeamWidths,
                final String featureTemplates, final Long2IntOpenHashMap parallelArrayOffsetMap,
                final short[] parallelWeightArrayTags, final float[] parallelWeightArray, final float[] biases) {

            this.vocabulary = vocabulary;
            this.posTagger = posTagger;
            this.classBoundaryBeamWidths = classBoundaryBeamWidths;
            this.factoredOnlyClassifier = factoredOnlyClassifier;
            this.featureTemplates = featureTemplates;

            this.parallelArrayOffsetMap = parallelArrayOffsetMap;
            this.parallelWeightArrayTags = parallelWeightArrayTags;
            this.parallelWeightArray = parallelWeightArray;

            this.biases = biases;
        }
    }
}
