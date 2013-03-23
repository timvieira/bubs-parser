package edu.berkeley.nlp.PCFGLA;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.berkeley.nlp.PCFGLA.smoothing.NoSmoothing;
import edu.berkeley.nlp.PCFGLA.smoothing.SmoothAcrossParentBits;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.IEEEDoubleScaling;
import edu.berkeley.nlp.util.Numberer;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.lela.FractionalCountGrammar;

/**
 * Reads in the Penn Treebank and generates N_GRAMMARS different grammars.
 * 
 * @author Slav Petrov
 */
public class GrammarTrainer extends BaseCommandlineTool {

    // TODO Make this non-static
    public static Random RANDOM;
    // TODO Remove
    public static Random RANDOM2;

    @Option(name = "-out", required = true, usage = "Output File for Grammar (Required)")
    private String outFileName;

    @Option(name = "-path", usage = "Path to Corpus")
    private String path = null;

    @Option(name = "-cycles", usage = "The number of split-merge cycles")
    private int splitMergeCycles = 6;

    @Option(name = "-mf", metaVar = "fraction", usage = "Fraction of new splits to re-merge in each split-merge cycle")
    private double mergeFraction = 0.5;

    @Option(name = "-i", usage = "EM iterations per split-merge cycle")
    private int emIterationsPerCycle = 50;

    @Option(name = "-mi", metaVar = "iterations", usage = "EM iterations after merging")
    private int emIterationsAfterMerge = 20;

    @Option(name = "-si", metaVar = "iterations", usage = "EM iterations during smoothing")
    private int emIterationsWithSmoothing = 10;

    @Option(name = "-di", usage = "The number of allowed iterations in which the dev-set likelihood drops.")
    private int maxDroppingLLIterations = 6;

    @Option(name = "-filter", usage = "Filter rules with prob below this threshold")
    private double minRuleProbability = 1.0e-60;

    @Option(name = "-smooth", usage = "Type of grammar smoothing used.")
    private SmoothingType smooth = SmoothingType.SmoothAcrossParentBits;

    @Option(name = "-maxL", metaVar = "length", usage = "Maximum sentence length (Default <=10000)")
    private int maxSentenceLength = 10000;

    @Option(name = "-b", metaVar = "direction", usage = "LEFT/RIGHT Binarization")
    private Binarization binarization = Binarization.RIGHT;

    @Option(name = "-in", usage = "Input File for Grammar")
    private String inFile = null;

    @Option(name = "-randSeed", usage = "Seed for random number generator (Two works well for English)")
    private int randSeed = 2;

    @Option(name = "-hor", metaVar = "markovization", usage = "Horizontal Markovization")
    private int horizontalMarkovization = 0;

    @Option(name = "-sub", metaVar = "states", usage = "Number of substates to split")
    private short nSubStates = 1;

    @Option(name = "-lowercase", usage = "Lowercase all words in the treebank")
    private boolean lowercase = false;

    @Option(name = "-r", metaVar = "percentage", usage = "Level of Randomness at init")
    private double randomization = 1.0;

    @Option(name = "-sm1", metaVar = "param", usage = "Lexicon smoothing parameter 1")
    private double smoothingParameter1 = 0.5;

    @Option(name = "-sm2", metaVar = "param", usage = "Lexicon smoothing parameter 2")
    private double smoothingParameter2 = 0.1;

    @Option(name = "-rare", metaVar = "threshold", usage = "Rare word threshold")
    private int rareWordThreshold = 20;

    @Option(name = "-skipSection", usage = "Skips a particular section of the WSJ training corpus (Needed for training Mark Johnsons reranker")
    private int skipSection = -1;

    @Option(name = "-skipBilingual", usage = "Skips the bilingual portion of the Chinese treebank (Needed for training the bilingual reranker")
    private boolean skipBilingual = false;

    @Option(name = "-writeIntermediateGrammars", usage = "Write intermediate (splitting and merging) grammars to disk.")
    private boolean writeIntermediateGrammars = false;

    @Override
    public void run() {

        BaseLogger.singleton().config(
                String.format("Using %s binarization, randomness: %.3f  seed: %d", binarization.name(), randomization,
                        randSeed));

        RANDOM = new Random(randSeed);
        RANDOM2 = new Random(randSeed);

        int emIterations = emIterationsPerCycle;

        final double[] smoothParams = { smoothingParameter1, smoothingParameter2 };
        System.out.println("Using smoothing parameters " + smoothParams[0] + " and " + smoothParams[1]);

        Corpus corpus = new Corpus(path, false, skipSection, skipBilingual);
        List<Tree<String>> trainTrees = Corpus.binarizeAndFilterTrees(corpus.getTrainTrees(), horizontalMarkovization,
                maxSentenceLength, binarization);
        List<Tree<String>> devSetTrees = Corpus.binarizeAndFilterTrees(corpus.getDevSetTrees(),
                horizontalMarkovization, maxSentenceLength, binarization);
        Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

        if (lowercase) {
            BaseLogger.singleton().config("Lowercasing the treebank.");
            Corpus.lowercaseWords(trainTrees);
            Corpus.lowercaseWords(devSetTrees);
        }

        System.out.println("There are " + trainTrees.size() + " trees in the training set.");

        short[] numSubStatesArray = initializeSubStateArray(trainTrees, devSetTrees, tagNumberer, nSubStates);

        System.out.println("There are " + numSubStatesArray.length + " observed categories.");

        // initialize lexicon and grammar
        Lexicon lexicon = null, maxLexicon = null;
        Grammar grammar = null, maxGrammar = null;
        double maxDevSetLikelihood = Double.NEGATIVE_INFINITY;

        // If we are splitting, we load the old grammar and start off by splitting.
        int startSplit = 0;
        if (inFile != null) {
            System.out.println("Loading old grammar from " + inFile);
            startSplit = 0; // we've already trained the grammar
            final ParserData pData = ParserData.Load(inFile);
            maxGrammar = pData.gr;
            maxLexicon = pData.lex;
            numSubStatesArray = maxGrammar.numSubStates;
            grammar = maxGrammar;
            lexicon = maxLexicon;
            Numberer.setNumberers(pData.getNumbs());
            tagNumberer = Numberer.getGlobalNumberer("tags");
            System.out.println("Loading old grammar complete.");
        }

        StateSetTreeList trainStateSetTrees = new StateSetTreeList(trainTrees, numSubStatesArray, false, tagNumberer);
        StateSetTreeList devSetStateSetTrees = new StateSetTreeList(devSetTrees, numSubStatesArray, false, tagNumberer);// deletePC);

        // get rid of the old trees
        trainTrees = null;
        devSetTrees = null;
        corpus = null;

        // If we're training without loading a split grammar, then we run once
        // without splitting.
        if (inFile == null) {

            // Induce M0 grammar from training corpus
            BaseLogger.singleton().info("Inducing M0 grammar...");

            grammar = new Grammar(numSubStatesArray, new NoSmoothing(), minRuleProbability);
            final Lexicon tmp_lexicon = new Lexicon(numSubStatesArray, Lexicon.DEFAULT_SMOOTHING_CUTOFF, smoothParams,
                    new NoSmoothing(), minRuleProbability);
            for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
                tmp_lexicon.trainTree(stateSetTree, randomization, null, false, rareWordThreshold);
            }
            lexicon = new Lexicon(numSubStatesArray, Lexicon.DEFAULT_SMOOTHING_CUTOFF, smoothParams, new NoSmoothing(),
                    minRuleProbability);

            // TODO BaseLogger.singleton().config("Markov-0 grammar size: " + grammarSummaryString(markov0Grammar));

            BaseLogger.singleton().info("Performing one I/O iteration");

            for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
                lexicon.trainTree(stateSetTree, randomization, tmp_lexicon, false, rareWordThreshold);
                grammar.countUnsplitTree(stateSetTree);
            }
            lexicon.tieRareWordStats(rareWordThreshold);
            lexicon.optimize();
            grammar.optimize(randomization);

            maxGrammar = grammar;
            maxLexicon = lexicon;
        }

        // for (int cycle = 1; cycle <= splitMergeCycles; cycle++) {
        // //
        // // Split
        // //
        //
        // //
        // // Merge
        // //
        //
        // //
        // // Smooth
        // //
        // }

        for (int splitIndex = startSplit; splitIndex < splitMergeCycles * 3; splitIndex++) {

            // now do either a merge or a split and the end a smooth
            // on odd iterations merge, on even iterations split
            String opString = "";
            if (splitIndex % 3 == 2) {// (splitIndex==numSplitTimes*2){
                if (smooth == SmoothingType.None) {
                    continue;
                }

                maxGrammar.setSmoother(new SmoothAcrossParentBits(0.01, maxGrammar.splitTrees));
                maxLexicon.setSmoother(new SmoothAcrossParentBits(0.1, maxGrammar.splitTrees));
                emIterations = emIterationsWithSmoothing;
                opString = "smoothing";

                // update the substate dependent objects
                grammar = maxGrammar;
                lexicon = maxLexicon;

            } else if (splitIndex % 3 == 0) {
                // the case where we split
                final CorpusStatistics corpusStatistics = new CorpusStatistics(tagNumberer, trainStateSetTrees);
                final int[] counts = corpusStatistics.getSymbolCounts();

                final int previousSplitCount = maxGrammar.totalSubStates();

                maxGrammar = maxGrammar.splitAllStates(randomization, counts);
                maxLexicon = maxLexicon.splitAllStates(counts, false);

                maxGrammar.setSmoother(new NoSmoothing());
                maxLexicon.setSmoother(new NoSmoothing());

                BaseLogger.singleton().info(
                        String.format("Split %d substates into %d", previousSplitCount, maxGrammar.totalSubStates()));
                opString = "splitting";
                emIterations = emIterationsPerCycle;

                // update the substate dependent objects
                grammar = maxGrammar;
                lexicon = maxLexicon;

            } else {

                // Merge
                if (mergeFraction == 0) {
                    continue;
                }

                final double[][] mergeWeights = GrammarMerger.computeMergeWeights(maxGrammar, maxLexicon,
                        trainStateSetTrees);
                final double[][][] deltas = GrammarMerger.computeDeltas(maxGrammar, maxLexicon, mergeWeights,
                        trainStateSetTrees);
                final boolean[][][] mergeThesePairs = GrammarMerger.determineMergePairs(deltas, mergeFraction,
                        maxGrammar);

                grammar = GrammarMerger.doTheMerges(maxGrammar, maxLexicon, mergeThesePairs, mergeWeights);
                trainStateSetTrees = new StateSetTreeList(trainStateSetTrees, grammar.numSubStates, false);
                devSetStateSetTrees = new StateSetTreeList(devSetStateSetTrees, grammar.numSubStates, false);

                // Retrain lexicon to finish the lexicon merge (updates the unknown-word model)
                lexicon = new Lexicon(grammar.numSubStates, Lexicon.DEFAULT_SMOOTHING_CUTOFF,
                        maxLexicon.getSmoothingParams(), maxLexicon.getSmoother(), maxLexicon.getPruningThreshold());
                doOneEStep(grammar, maxLexicon, null, lexicon, trainStateSetTrees, rareWordThreshold);
                lexicon.optimize();

                GrammarMerger.printMergingStatistics(maxGrammar, grammar);
                opString = "merging";
                maxGrammar = grammar;
                maxLexicon = lexicon;
                emIterations = emIterationsAfterMerge;

                // update the substate dependent objects
                grammar = maxGrammar;
                lexicon = maxLexicon;
            }

            //
            // TODO Extract this as a method
            //
            trainStateSetTrees = new StateSetTreeList(trainStateSetTrees, grammar.numSubStates, false);
            devSetStateSetTrees = new StateSetTreeList(devSetStateSetTrees, grammar.numSubStates, false);
            maxDevSetLikelihood = calculateLogLikelihood(maxGrammar, maxLexicon, devSetStateSetTrees);

            // Train the grammar for a fixed number of iterations, or if using a dev-set, until dev-set likelihood
            // reliably drops
            for (int iteration = 1, droppingIterations = 0; iteration <= emIterations
                    && droppingIterations < maxDroppingLLIterations; iteration++) {

                final EmIterationResult result = emIteration(iteration, grammar, lexicon, trainStateSetTrees,
                        devSetStateSetTrees, minRuleProbability);

                // Record the best-performing grammar
                if (result.devSetLikelihood >= maxDevSetLikelihood) {
                    maxDevSetLikelihood = result.devSetLikelihood;
                    maxGrammar = grammar;
                    maxLexicon = lexicon;
                    droppingIterations = 0;
                } else {
                    droppingIterations++;
                }

                grammar = result.grammar;
                lexicon = result.lexicon;
            }
            // End method

            // Dump a grammar file to disk from time to time
            if (writeIntermediateGrammars || "smoothing".equals(opString)) {
                final String outTmpName = outFileName + "_" + (splitIndex / 3 + 1) + "_" + opString + ".gr";
                System.out.println("Saving grammar to " + outTmpName + ".");
                writeGrammar(maxGrammar, maxLexicon, new File(outTmpName));
            }
        }

        // The last grammar/lexicon has not yet been evaluated. The dev-set likelihood may have dropped in the past
        // few iteration, but even if so, there is still a chance that the last one was the best
        final double devSetLikelihood = calculateLogLikelihood(grammar, lexicon, devSetStateSetTrees);
        if (devSetLikelihood > maxDevSetLikelihood) {
            maxDevSetLikelihood = devSetLikelihood;
            maxGrammar = grammar;
            maxLexicon = lexicon;
        }

        BaseLogger.singleton().info("Saving grammar to " + outFileName + ".");
        if (maxDevSetLikelihood != 0) {
            BaseLogger.singleton().info("Dev-set log likelihood: " + maxDevSetLikelihood);
        }
        writeGrammar(maxGrammar, maxLexicon, new File(outFileName));
    }

    /**
     * Execute a single EM iteration
     * 
     * @param grammar
     * @param lexicon
     * @param trainStateSetTrees
     * @param validationStateSetTrees
     * @param minimumRuleProbability
     * @return EM result, including the newly trained {@link FractionalCountGrammar}
     */
    private EmIterationResult emIteration(final int iteration, final Grammar grammar, final Lexicon lexicon,
            final StateSetTreeList trainStateSetTrees, final StateSetTreeList validationStateSetTrees,
            final double minimumRuleProbability) {

        final long t0 = System.currentTimeMillis();

        double validationLikelihood = 0;

        if (validationStateSetTrees != null && !validationStateSetTrees.isEmpty()) {
            // Compute and report the validation likelihood of the previous iteration
            validationLikelihood = calculateLogLikelihood(grammar, lexicon, validationStateSetTrees);
            BaseLogger.singleton().info(String.format("Validation set likelihood: %.3f", validationLikelihood));
        }

        //
        // Compute training likelihood (E-step)
        //
        final Grammar newGrammar = new Grammar(grammar, grammar.numSubStates);
        final Lexicon newLexicon = new Lexicon(grammar.numSubStates, Lexicon.DEFAULT_SMOOTHING_CUTOFF,
                lexicon.getSmoothingParams(), lexicon.getSmoother(), lexicon.getPruningThreshold());

        final ArrayParser parser = new ArrayParser(grammar, lexicon);
        double trainingLikelihood = 0;

        for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {

            parser.doInsideOutsideScores(stateSetTree, true);

            final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().getIScore(0), stateSetTree.label()
                    .getIScale());

            // Skip sentences we couldn't parse
            if (Double.isInfinite(ll) || Double.isNaN(ll)) {
                continue;
            }

            // Maximize (M-step)
            newLexicon.trainTree(stateSetTree, -1, lexicon, true, rareWordThreshold);
            newGrammar.countSplitTree(stateSetTree, grammar);
            trainingLikelihood += ll;
        }
        newLexicon.tieRareWordStats(rareWordThreshold);

        BaseLogger.singleton().info(
                String.format("Iteration: %2d  Training set likelihood: %.4f", iteration, trainingLikelihood));

        // Maximize (M-step)
        newLexicon.optimize();
        newGrammar.optimize(0);

        return new EmIterationResult(newGrammar, newLexicon, trainingLikelihood, validationLikelihood,
                (int) (System.currentTimeMillis() - t0));
    }

    void writeGrammar(final Grammar grammar, final Lexicon lexicon, final File f) {
        try {
            // TODO Clone and remove unlikely rules

            // Write a Java Serialized Object
            final String serFilename = f.getParent() + "/" + f.getName().replaceAll("\\.gz", "") + ".ser";
            final ParserData pData = new ParserData(lexicon, grammar, Numberer.getNumberers(), grammar.numSubStates,
                    horizontalMarkovization, binarization);

            System.out.println("Saving grammar to " + serFilename + ".");
            if (pData.Save(serFilename)) {
                System.out.println("Saving successful.");
            } else {
                System.out.println("Saving failed!");
            }

            // And a gzipped-text representation
            final Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)));
            w.write(grammar.toString(lexicon.getNumberOfEntries(), minRuleProbability));
            w.write("===== LEXICON =====\n");
            w.write(lexicon.toString(minRuleProbability));
            w.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param previousGrammar
     * @param previousLexicon
     * @param grammar Current grammar, or null if merging (in which case we will train only the lexicon)
     * @param lexicon
     * @param trainStateSetTrees
     * @return
     */
    private static double doOneEStep(final Grammar previousGrammar, final Lexicon previousLexicon,
            final Grammar grammar, final Lexicon lexicon, final StateSetTreeList trainStateSetTrees,
            final int unkThreshold) {

        final ArrayParser parser = new ArrayParser(previousGrammar, previousLexicon);
        double trainingLikelihood = 0;
        final int n = 0;

        for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
            parser.doInsideOutsideScores(stateSetTree, true); // E step

            final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().getIScore(0), stateSetTree.label()
                    .getIScale());

            if ((Double.isInfinite(ll) || Double.isNaN(ll))) {
                BaseLogger.singleton().finer(
                        String.format("Training sentence %d :%f  Root iScore: %.3f  Scale: %d", n, ll, stateSetTree
                                .label().getIScore(0), stateSetTree.label().getIScale()));
                continue;
            }

            lexicon.trainTree(stateSetTree, -1, previousLexicon, true, unkThreshold);
            if (grammar != null) {
                grammar.countSplitTree(stateSetTree, previousGrammar); // E step
            }
            trainingLikelihood += ll; // there are for some reason some
                                      // sentences that are unparsable
        }
        lexicon.tieRareWordStats(unkThreshold);
        return trainingLikelihood;
    }

    /**
     * @param maxGrammar
     * @param maxLexicon
     * @param corpus
     * @return The log likelihood of a corpus
     */
    public static double calculateLogLikelihood(final Grammar maxGrammar, final Lexicon maxLexicon,
            final StateSetTreeList corpus) {

        final ArrayParser parser = new ArrayParser(maxGrammar, maxLexicon);
        double totalLogLikelihood = 0;

        for (final Tree<StateSet> stateSetTree : corpus) {
            // Only the inside scores are needed here
            parser.doInsideScores(stateSetTree, false, null);
            final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().getIScore(0), stateSetTree.label()
                    .getIScale());

            // A few sentences are unparsable
            if (!Double.isInfinite(ll) && !Double.isNaN(ll)) {
                totalLogLikelihood += ll;
            }
        }
        // if (unparsable>0)
        // System.out.print("Number of unparsable trees: "+unparsable+".");
        return totalLogLikelihood;
    }

    /**
     * @param stateSetTree
     */
    public static void printBadLLReason(final Tree<StateSet> stateSetTree, final Lexicon lexicon) {
        System.out.println(stateSetTree.toString());
        boolean lexiconProblem = false;
        final List<StateSet> words = stateSetTree.leafLabels();
        final Iterator<StateSet> wordIterator = words.iterator();
        for (final StateSet stateSet : stateSetTree.preterminalLabels()) {
            final String word = wordIterator.next().getWord();
            boolean lexiconProblemHere = true;
            for (int i = 0; i < stateSet.numSubStates(); i++) {
                final double score = stateSet.getIScore(i);
                if (!(Double.isInfinite(score) || Double.isNaN(score))) {
                    lexiconProblemHere = false;
                }
            }
            if (lexiconProblemHere) {
                System.out.println("LEXICON PROBLEM ON STATE " + stateSet.getState() + " word " + word);
                System.out.println("  word " + lexicon.wordCounter.getCount(stateSet.getWord()));
                for (int i = 0; i < stateSet.numSubStates(); i++) {
                    System.out.println("  tag " + lexicon.tagCounter[stateSet.getState()][i]);
                    System.out.println("  word/state/sub "
                            + lexicon.wordToTagCounters[stateSet.getState()].get(stateSet.getWord())[i]);
                }
            }
            lexiconProblem = lexiconProblem || lexiconProblemHere;
        }
        if (lexiconProblem)
            System.out.println("  the likelihood is bad because of the lexicon");
        else
            System.out.println("  the likelihood is bad because of the grammar");
    }

    /**
     * This function probably doesn't belong here, but because it should be called after {@link #updateStateSetTrees},
     * Leon left it here.
     * 
     * @param trees Trees which have already had their inside-outside probabilities calculated, as by
     *            {@link #updateStateSetTrees}.
     * @return The log likelihood of the trees.
     */
    public static double logLikelihood(final List<Tree<StateSet>> trees, final boolean verbose) {
        double likelihood = 0, l = 0;
        for (final Tree<StateSet> tree : trees) {
            l = tree.label().getIScore(0);
            if (verbose)
                System.out.println("LL is " + l + ".");
            if (Double.isInfinite(l) || Double.isNaN(l)) {
                System.out.println("LL is not finite.");
            } else {
                likelihood += l;
            }
        }
        return likelihood;
    }

    /**
     * This updates the inside-outside probabilities for the list of trees using the parser's doInsideScores and
     * doOutsideScores methods.
     * 
     * @param trees A list of binarized, annotated StateSet Trees.
     * @param parser The parser to score the trees.
     */
    public static void updateStateSetTrees(final List<Tree<StateSet>> trees, final ArrayParser parser) {
        for (final Tree<StateSet> tree : trees) {
            parser.doInsideOutsideScores(tree, false);
        }
    }

    /**
     * Convert a single Tree[String] to Tree[StateSet]
     * 
     * @param trainTrees
     * @param validationTrees
     * @param tagNumberer
     * @return Substate array
     */

    public static short[] initializeSubStateArray(final List<Tree<String>> trainTrees,
            final List<Tree<String>> validationTrees, final Numberer tagNumberer, final short nSubStates) {
        // boolean dontSplitTags) {
        // first generate unsplit grammar and lexicon
        final short[] nSub = new short[2];
        nSub[0] = 1;
        nSub[1] = nSubStates;

        // do the training and validation sets, so that the numberer sees all tags and we can allocate big enough arrays
        // Note: although these variables are never read, this constructors add the validation trees into the
        // tagNumberer as a side effect, which is important
        @SuppressWarnings("unused")
        final StateSetTreeList trainStateSetTrees = new StateSetTreeList(trainTrees, nSub, true, tagNumberer);
        @SuppressWarnings("unused")
        final StateSetTreeList validationStateSetTrees = new StateSetTreeList(validationTrees, nSub, true, tagNumberer);

        StateSetTreeList.initializeTagNumberer(trainTrees, tagNumberer);
        StateSetTreeList.initializeTagNumberer(validationTrees, tagNumberer);

        final short numStates = (short) tagNumberer.total();
        final short[] nSubStateArray = new short[numStates];
        final short two = nSubStates;
        Arrays.fill(nSubStateArray, two);
        // System.out.println("Everything is split in two except for the root.");
        nSubStateArray[0] = 1; // that's the ROOT
        return nSubStateArray;
    }

    public static void main(final String[] args) {
        run(args);
    }

    private static class EmIterationResult {
        final Grammar grammar;
        final Lexicon lexicon;
        final double trainingSetLikelihood;
        final double devSetLikelihood;
        final int emTime;

        public EmIterationResult(final Grammar grammar, final Lexicon lexicon, final double trainingSetLikelihood,
                final double validationSetLikelihood, final int emTime) {
            this.grammar = grammar;
            this.lexicon = lexicon;
            this.trainingSetLikelihood = trainingSetLikelihood;
            this.devSetLikelihood = validationSetLikelihood;
            this.emTime = emTime;
        }

    }

    private static enum SmoothingType {
        SmoothAcrossParentBits, None;
    }
}
