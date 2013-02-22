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
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.berkeley.nlp.PCFGLA.smoothing.NoSmoothing;
import edu.berkeley.nlp.PCFGLA.smoothing.SmoothAcrossParentBits;
import edu.berkeley.nlp.PCFGLA.smoothing.Smoother;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;

/**
 * Reads in the Penn Treebank and generates N_GRAMMARS different grammars.
 * 
 * @author Slav Petrov
 */
public class GrammarTrainer extends BaseCommandlineTool {

    public static boolean VERBOSE = false;

    public static Random RANDOM = new Random(0);

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

    @Option(name = "-di", usage = "The number of allowed iterations in which the validation likelihood drops.")
    private int di = 6;

    @Option(name = "-filter", usage = "Filter rules with prob below this threshold")
    private double filter = 1.0e-60;

    @Option(name = "-smooth", usage = "Type of grammar smoothing used.")
    private String smooth = "SmoothAcrossParentBits";

    @Option(name = "-maxL", metaVar = "length", usage = "Maximum sentence length (Default <=10000)")
    private int maxSentenceLength = 10000;

    @Option(name = "-b", metaVar = "direction", usage = "LEFT/RIGHT Binarization")
    private Binarization binarization = Binarization.RIGHT;

    @Option(name = "-noSplit", usage = "Don't split - just load and continue training an existing grammar (true/false) (Default:false)")
    private boolean noSplit = false;

    @Option(name = "-in", usage = "Input File for Grammar")
    private String inFile = null;

    @Option(name = "-randSeed", usage = "Seed for random number generator (Two works well for English)")
    private int randSeed = 2;

    @Option(name = "-sep", usage = "Set merging threshold for grammar and lexicon separately")
    private boolean separateMergingThreshold = false;

    @Option(name = "-trainOnDevSet", usage = "Include the development set into the training set")
    private boolean trainOnDevSet = false;

    @Option(name = "-hor", metaVar = "markovization", usage = "Horizontal Markovization")
    private int horizontalMarkovization = 0;

    @Option(name = "-sub", metaVar = "states", usage = "Number of substates to split")
    private short nSubStates = 1;

    @Option(name = "-ver", metaVar = "markovization", usage = "Vertical Markovization")
    private int verticalMarkovization = 1;

    @Option(name = "-lowercase", usage = "Lowercase all words in the treebank")
    private boolean lowercase = false;

    @Option(name = "-r", metaVar = "percentage", usage = "Level of Randomness at init")
    private double randomization = 1.0;

    @Option(name = "-sm1", metaVar = "param", usage = "Lexicon smoothing parameter 1")
    private double smoothingParameter1 = 0.5;

    @Option(name = "-sm2", metaVar = "param", usage = "Lexicon smoothing parameter 2")
    private double smoothingParameter2 = 0.1;

    @Option(name = "-rare", metaVar = "threshold", usage = "Rare word threshold")
    private int rare = 20;

    // Reverse this variable, so we can continue to use a boolean
    @Option(name = "-spath", usage = "Whether or not to store the best path info (true/false) (Default: true)")
    private boolean findClosedUnaryPaths = true;

    @Option(name = "-skipSection", usage = "Skips a particular section of the WSJ training corpus (Needed for training Mark Johnsons reranker")
    private int skipSection = -1;

    @Option(name = "-skipBilingual", usage = "Skips the bilingual portion of the Chinese treebank (Needed for training the bilingual reranker")
    private boolean skipBilingual = false;

    @Option(name = "-writeIntermediateGrammars", usage = "Write intermediate (splitting and merging) grammars to disk.")
    private boolean writeIntermediateGrammars = false;

    @Override
    public void run() {

        System.out.println("Loading trees from " + path);
        System.out.println("Will remove sentences with more than " + maxSentenceLength + " words.");

        System.out.println("Using horizontal=" + horizontalMarkovization + " and vertical=" + verticalMarkovization
                + " markovization.");

        System.out.println("Using " + binarization.name() + " binarization.");// and
                                                                              // "+annotateString+".");

        final double randomness = randomization;
        System.out.println("Using a randomness value of " + randomness);

        if (outFileName == null) {
            System.out.println("Output File name is required.");
            System.exit(-1);
        } else
            System.out.println("Using grammar output file " + outFileName + ".");

        VERBOSE = BaseLogger.singleton().isLoggable(Level.FINE);
        RANDOM = new Random(randSeed);
        System.out.println("Random number generator seeded at " + randSeed + ".");

        int emIterations = emIterationsPerCycle;

        final double[] smoothParams = { smoothingParameter1, smoothingParameter2 };
        System.out.println("Using smoothing parameters " + smoothParams[0] + " and " + smoothParams[1]);

        Corpus corpus = new Corpus(path, false, skipSection, skipBilingual);
        List<Tree<String>> trainTrees = Corpus.binarizeAndFilterTrees(corpus.getTrainTrees(), verticalMarkovization,
                horizontalMarkovization, maxSentenceLength, binarization, false, VERBOSE);
        List<Tree<String>> validationTrees = Corpus.binarizeAndFilterTrees(corpus.getValidationTrees(),
                verticalMarkovization, horizontalMarkovization, maxSentenceLength, binarization, false, VERBOSE);
        Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

        if (trainOnDevSet) {
            System.out.println("Adding devSet to training data.");
            trainTrees.addAll(validationTrees);
        }

        if (lowercase) {
            System.out.println("Lowercasing the treebank.");
            Corpus.lowercaseWords(trainTrees);
            Corpus.lowercaseWords(validationTrees);
        }

        System.out.println("There are " + trainTrees.size() + " trees in the training set.");

        if (filter > 0)
            System.out
                    .println("Will remove rules with prob under "
                            + filter
                            + ".\nEven though only unlikely rules are pruned the training LL is not guaranteed to increase in every round anymore "
                            + "(especially when we are close to converging)."
                            + "\nFurthermore it increases the variance because 'good' rules can be pruned away in early stages.");

        short[] numSubStatesArray = initializeSubStateArray(trainTrees, validationTrees, tagNumberer, nSubStates);

        if (VERBOSE) {
            for (int i = 0; i < numSubStatesArray.length; i++) {
                System.out.println("Tag " + (String) tagNumberer.object(i) + " " + i);
            }
        }

        System.out.println("There are " + numSubStatesArray.length + " observed categories.");

        // initialize lexicon and grammar
        Lexicon lexicon = null, maxLexicon = null, previousLexicon = null;
        Grammar grammar = null, maxGrammar = null, previousGrammar = null;
        double maxLikelihood = Double.NEGATIVE_INFINITY;

        // EM: iterate until the validation likelihood drops for four consecutive iterations
        int iter = 0;
        int droppingIter = 0;

        // If we are splitting, we load the old grammar and start off by splitting.
        int startSplit = 0;
        if (inFile != null) {
            System.out.println("Loading old grammar from " + inFile);
            startSplit = 0; // we've already trained the grammar
            final ParserData pData = ParserData.Load(inFile);
            maxGrammar = pData.gr;
            maxLexicon = pData.lex;
            numSubStatesArray = maxGrammar.numSubStates;
            previousGrammar = grammar = maxGrammar;
            previousLexicon = lexicon = maxLexicon;
            Numberer.setNumberers(pData.getNumbs());
            tagNumberer = Numberer.getGlobalNumberer("tags");
            System.out.println("Loading old grammar complete.");
            if (noSplit) {
                System.out.println("Will NOT split the loaded grammar.");
                startSplit = 1;
            }
        }

        if (mergeFraction > 0) {
            System.out.println("Will merge " + (int) (mergeFraction * 100) + "% of the splits in each round.");
            System.out.println("The threshold for merging lexical and phrasal categories will be set separately: "
                    + separateMergingThreshold);
        }

        StateSetTreeList trainStateSetTrees = new StateSetTreeList(trainTrees, numSubStatesArray, false, tagNumberer);
        StateSetTreeList validationStateSetTrees = new StateSetTreeList(validationTrees, numSubStatesArray, false,
                tagNumberer);// deletePC);

        // get rid of the old trees
        trainTrees = null;
        validationTrees = null;
        corpus = null;
        System.gc();

        // If we're training without loading a split grammar, then we run once
        // without splitting.
        if (inFile == null) {
            grammar = new Grammar(numSubStatesArray, findClosedUnaryPaths, new NoSmoothing(), null, filter);
            final Lexicon tmp_lexicon = new Lexicon(numSubStatesArray, Lexicon.DEFAULT_SMOOTHING_CUTOFF, smoothParams,
                    new NoSmoothing(), filter);
            final int n = 0;
            for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
                tmp_lexicon.trainTree(stateSetTree, randomness, null, false, rare);
            }
            lexicon = new Lexicon(numSubStatesArray, Lexicon.DEFAULT_SMOOTHING_CUTOFF, smoothParams, new NoSmoothing(),
                    filter);
            for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
                lexicon.trainTree(stateSetTree, randomness, tmp_lexicon, false, rare);
                grammar.tallyUninitializedStateSetTree(stateSetTree);
            }
            lexicon.tieRareWordStats(rare);
            lexicon.optimize();
            grammar.optimize(randomness);
            // System.out.println(grammar);
            previousGrammar = maxGrammar = grammar; // needed for baseline -
                                                    // when there is no EM loop
            previousLexicon = maxLexicon = lexicon;
        }

        // the main loop: split and train the grammar
        for (int splitIndex = startSplit; splitIndex < splitMergeCycles * 3; splitIndex++) {

            // now do either a merge or a split and the end a smooth
            // on odd iterations merge, on even iterations split
            String opString = "";
            if (splitIndex % 3 == 2) {// (splitIndex==numSplitTimes*2){
                if (smooth.equals("NoSmoothing"))
                    continue;
                System.out.println("Setting smoother for grammar and lexicon.");
                final Smoother grSmoother = new SmoothAcrossParentBits(0.01, maxGrammar.splitTrees);
                final Smoother lexSmoother = new SmoothAcrossParentBits(0.1, maxGrammar.splitTrees);

                maxGrammar.setSmoother(grSmoother);
                maxLexicon.setSmoother(lexSmoother);
                emIterations = emIterationsWithSmoothing;
                opString = "smoothing";
            } else if (splitIndex % 3 == 0) {
                // the case where we split
                if (noSplit)
                    continue;
                System.out.println("Before splitting, we have a total of " + maxGrammar.totalSubStates()
                        + " substates.");
                final CorpusStatistics corpusStatistics = new CorpusStatistics(tagNumberer, trainStateSetTrees);
                final int[] counts = corpusStatistics.getSymbolCounts();

                maxGrammar = maxGrammar.splitAllStates(randomness, counts);
                maxLexicon = maxLexicon.splitAllStates(counts, false, 0);
                final Smoother grSmoother = new NoSmoothing();
                final Smoother lexSmoother = new NoSmoothing();
                maxGrammar.setSmoother(grSmoother);
                maxLexicon.setSmoother(lexSmoother);
                System.out
                        .println("After splitting, we have a total of " + maxGrammar.totalSubStates() + " substates.");
                System.out
                        .println("Rule probabilities are NOT normalized in the split, therefore the training LL is not guaranteed to improve between iteration 0 and 1!");
                opString = "splitting";
                emIterations = emIterationsPerCycle;
            } else {
                if (mergeFraction == 0)
                    continue;
                // the case where we merge
                final double[][] mergeWeights = GrammarMerger.computeMergeWeights(maxGrammar, maxLexicon,
                        trainStateSetTrees);
                final double[][][] deltas = GrammarMerger.computeDeltas(maxGrammar, maxLexicon, mergeWeights,
                        trainStateSetTrees);
                final boolean[][][] mergeThesePairs = GrammarMerger.determineMergePairs(deltas,
                        separateMergingThreshold, mergeFraction, maxGrammar);

                grammar = GrammarMerger.doTheMerges(maxGrammar, maxLexicon, mergeThesePairs, mergeWeights);
                final short[] newNumSubStatesArray = grammar.numSubStates;
                trainStateSetTrees = new StateSetTreeList(trainStateSetTrees, newNumSubStatesArray, false);
                validationStateSetTrees = new StateSetTreeList(validationStateSetTrees, newNumSubStatesArray, false);

                // retrain lexicon to finish the lexicon merge (updates the
                // unknown words model)...
                lexicon = new Lexicon(newNumSubStatesArray, Lexicon.DEFAULT_SMOOTHING_CUTOFF,
                        maxLexicon.getSmoothingParams(), maxLexicon.getSmoother(), maxLexicon.getPruningThreshold());
                final double trainingLikelihood = GrammarTrainer.doOneEStep(grammar, maxLexicon, null, lexicon,
                        trainStateSetTrees, rare);
                // System.out.println("The training LL is "+trainingLikelihood);
                lexicon.optimize();// Grammar.RandomInitializationType.INITIALIZE_WITH_SMALL_RANDOMIZATION);
                                   // // M Step

                GrammarMerger.printMergingStatistics(maxGrammar, grammar);
                opString = "merging";
                maxGrammar = grammar;
                maxLexicon = lexicon;
                emIterations = emIterationsAfterMerge;
            }
            // update the substate dependent objects
            previousGrammar = grammar = maxGrammar;
            previousLexicon = lexicon = maxLexicon;
            droppingIter = 0;
            numSubStatesArray = grammar.numSubStates;
            trainStateSetTrees = new StateSetTreeList(trainStateSetTrees, numSubStatesArray, false);
            validationStateSetTrees = new StateSetTreeList(validationStateSetTrees, numSubStatesArray, false);
            maxLikelihood = calculateLogLikelihood(maxGrammar, maxLexicon, validationStateSetTrees);
            System.out.println("After " + opString + " in the " + (splitIndex / 3 + 1)
                    + "th round, we get a validation likelihood of " + maxLikelihood);
            iter = 0;

            // the inner loop: train the grammar via EM until validation
            // likelihood reliably drops
            do {
                iter += 1;
                System.out.println("Beginning iteration " + (iter - 1) + ":");

                // 1) Compute the validation likelihood of the previous
                // iteration
                System.out.print("Calculating validation likelihood...");
                final double validationLikelihood = calculateLogLikelihood(previousGrammar, previousLexicon,
                        validationStateSetTrees); // The validation LL of
                                                  // previousGrammar/previousLexicon
                System.out.println("done: " + validationLikelihood);

                // 2) Perform the E step while computing the training likelihood
                // of the previous iteration
                System.out.print("Calculating training likelihood...");
                grammar = new Grammar(grammar.numSubStates, grammar.findClosedPaths, grammar.smoother, grammar,
                        grammar.threshold);
                lexicon = new Lexicon(grammar.numSubStates, Lexicon.DEFAULT_SMOOTHING_CUTOFF,
                        lexicon.getSmoothingParams(), lexicon.getSmoother(), lexicon.getPruningThreshold());
                final double trainingLikelihood = doOneEStep(previousGrammar, previousLexicon, grammar, lexicon,
                        trainStateSetTrees, rare); // The training LL of
                                                   // previousGrammar/previousLexicon
                System.out.println("done: " + trainingLikelihood);

                // 3) Perform the M-Step
                lexicon.optimize(); // M Step
                grammar.optimize(0); // M Step

                // 4) Check whether previousGrammar/previousLexicon was in fact better than the best
                if (iter < emIterations || validationLikelihood >= maxLikelihood) {
                    maxLikelihood = validationLikelihood;
                    maxGrammar = previousGrammar;
                    maxLexicon = previousLexicon;
                    droppingIter = 0;
                } else {
                    droppingIter++;
                }

                // 5) advance the 'pointers'
                previousGrammar = grammar;
                previousLexicon = lexicon;
            } while ((droppingIter < di) && (iter < emIterations));

            // Dump a grammar file to disk from time to time
            if (writeIntermediateGrammars || "smoothing".equals(opString)) {
                final String outTmpName = outFileName + "_" + (splitIndex / 3 + 1) + "_" + opString + ".gr";
                System.out.println("Saving grammar to " + outTmpName + ".");
                writeGrammar(maxGrammar, maxLexicon, new File(outTmpName));
            }
        }

        // The last grammar/lexicon has not yet been evaluated. Even though the validation likelihood has been dropping
        // in the past few iteration, there is still a chance that the last one was in fact the best so just in case we
        // evaluate it.
        System.out.print("Calculating last validation likelihood...");
        final double validationLikelihood = calculateLogLikelihood(grammar, lexicon, validationStateSetTrees);
        System.out.println("done.\n  Iteration " + iter + " (final) gives validation likelihood "
                + validationLikelihood);
        if (validationLikelihood > maxLikelihood) {
            maxLikelihood = validationLikelihood;
            maxGrammar = previousGrammar;
            maxLexicon = previousLexicon;
        }

        BaseLogger.singleton().info("Saving grammar to " + outFileName + ".");
        System.out.println("It gives a validation data log likelihood of: " + maxLikelihood);
        writeGrammar(maxGrammar, maxLexicon, new File(outFileName));
    }

    void writeGrammar(final Grammar grammar, final Lexicon lexicon, final File f) {
        try {
            final Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)));
            w.write(grammar.toString(lexicon.getNumberOfEntries()));
            w.write("===== LEXICON =====\n");
            w.write(lexicon.toString());
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
            double ll = stateSetTree.getLabel().getIScore(0);
            ll = Math.log(ll) + (100 * stateSetTree.getLabel().getIScale());
            if ((Double.isInfinite(ll) || Double.isNaN(ll))) {
                if (VERBOSE) {
                    System.out.println("Training sentence " + n + " is given " + ll + " log likelihood!");
                    System.out.println("Root iScore " + stateSetTree.getLabel().getIScore(0) + " scale "
                            + stateSetTree.getLabel().getIScale());
                }
            } else {
                lexicon.trainTree(stateSetTree, -1, previousLexicon, true, unkThreshold);
                if (grammar != null) {
                    grammar.tallyStateSetTree(stateSetTree, previousGrammar); // E step
                }
                trainingLikelihood += ll; // there are for some reason some
                                          // sentences that are unparsable
            }
        }
        lexicon.tieRareWordStats(unkThreshold);
        return trainingLikelihood;
    }

    /**
     * @param maxGrammar
     * @param maxLexicon
     * @param validationStateSetTrees
     * @return
     */
    public static double calculateLogLikelihood(final Grammar maxGrammar, final Lexicon maxLexicon,
            final StateSetTreeList validationStateSetTrees) {
        final ArrayParser parser = new ArrayParser(maxGrammar, maxLexicon);
        int unparsable = 0;
        double maxLikelihood = 0;
        for (final Tree<StateSet> stateSetTree : validationStateSetTrees) {
            // Only the inside scores are needed here
            parser.doInsideScores(stateSetTree, false, null);
            double ll = stateSetTree.getLabel().getIScore(0);
            ll = Math.log(ll) + (100 * stateSetTree.getLabel().getIScale());

            if (Double.isInfinite(ll) || Double.isNaN(ll)) {
                unparsable++;
                // printBadLLReason(stateSetTree, lexicon);
            } else
                maxLikelihood += ll; // there are for some reason some sentences
                                     // that are unparsable
        }
        // if (unparsable>0)
        // System.out.print("Number of unparsable trees: "+unparsable+".");
        return maxLikelihood;
    }

    /**
     * @param stateSetTree
     */
    public static void printBadLLReason(final Tree<StateSet> stateSetTree, final Lexicon lexicon) {
        System.out.println(stateSetTree.toString());
        boolean lexiconProblem = false;
        final List<StateSet> words = stateSetTree.getYield();
        final Iterator<StateSet> wordIterator = words.iterator();
        for (final StateSet stateSet : stateSetTree.getPreTerminalYield()) {
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
            l = tree.getLabel().getIScore(0);
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
     * @param tree
     * @param numStates
     * @param tagNumberer
     * @return
     */

    public static short[] initializeSubStateArray(final List<Tree<String>> trainTrees,
            final List<Tree<String>> validationTrees, final Numberer tagNumberer, final short nSubStates) {
        // boolean dontSplitTags) {
        // first generate unsplit grammar and lexicon
        final short[] nSub = new short[2];
        nSub[0] = 1;
        nSub[1] = nSubStates;

        // do the validation set so that the numberer sees all tags and we can
        // allocate big enough arrays
        // note: although this variable is never read, this constructor adds the
        // validation trees into the tagNumberer as a side effect, which is
        // important
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
}
