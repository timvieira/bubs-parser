package edu.berkeley.nlp.PCFGLA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeRanking;
import edu.berkeley.nlp.PCFGLA.smoothing.NoSmoothing;
import edu.berkeley.nlp.PCFGLA.smoothing.SmoothAcrossParentBits;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.lela.FractionalCountGrammar;
import edu.ohsu.cslu.parser.cellselector.CompleteClosureModel;
import edu.ohsu.cslu.util.IEEEDoubleScaling;

/**
 * Performs split-merge grammar training.
 * 
 * Input: gold trees, one sentence per line.
 * 
 * Output: grammar training progress. At higher verbosity levels (see '-v'), detailed training information will be
 * included as well.
 * 
 * Note: grammar training is a lengthy process - generally several hours, even on current CPU architectures. Best
 * practice is to run training in the background, using tools such as 'nohup' or 'screen'.
 * 
 * 
 * Implementation Note: {@link GrammarTrainer} is based on the cltool4j command-line tool infrastructure
 * (http://code.google.com/p/cltool4j/), which provides command-line handling, threading support, and input/output
 * infrastructure.
 * 
 * == Citing ==
 * 
 * If you use the BUBS grammar trainer in research, please cite:
 * 
 * Slav Petrov, Leon Barrett, Romain Thibaux, and Dan Klein, 2006. Learning accurate, compact, and interpretable tree
 * annotation. ACL, pages 433-440.
 * 
 * and/or
 * 
 * Dunlop, 2014. Efficient Latent-variable Grammars; Learning and Inference. Ph.D. Dissertation, OHSU.
 * 
 * Further documentation is available at https://code.google.com/p/bubs-parser/
 * 
 * @author Slav Petrov
 * @author Aaron Dunlop
 */
public class GrammarTrainer extends BaseCommandlineTool {

    // TODO Make this non-static
    public static Random RANDOM;

    @Option(name = "-gd", aliases = { "--grammar-directory" }, required = true, metaVar = "directory", usage = "Output grammar directory. Each merged grammar will be output in .gz format")
    private File outputGrammarDirectory;

    @Option(name = "-gp", aliases = { "--grammar-prefix" }, metaVar = "prefix", usage = "Output grammar file prefix (e.g. 'eng.' produces 'eng.sm1.gr.gz', 'eng.sm2.gr.gz', etc.")
    private String outputGrammarPrefix = "";

    @Option(name = "-cycles", usage = "The number of split-merge cycles")
    private int splitMergeCycles = 6;

    @Option(name = "-mf", metaVar = "fraction", usage = "Fraction of new splits to re-merge in each split-merge cycle")
    private double mergeFraction = 0.5;

    @Option(name = "-d", metaVar = "file", usage = "Development-set")
    private File devSet;

    @Option(name = "-mrf", metaVar = "objective function", usage = "Merge-candidate ranking function")
    private edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeRanking mergeRankingFunction = edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeRanking.Likelihood;

    @Option(name = "-i", usage = "EM iterations per split-merge cycle")
    private int emIterationsPerCycle = 50;

    @Option(name = "-mi", metaVar = "iterations", usage = "EM iterations after merging")
    private int emIterationsAfterMerge = 20;

    @Option(name = "-si", metaVar = "iterations", usage = "EM iterations during smoothing")
    private int emIterationsWithSmoothing = 10;

    @Option(name = "-di", metaVar = "iterations", usage = "The number of allowed iterations in which the dev-set likelihood drops.")
    private int maxDroppingLLIterations = 6;

    @Option(name = "-mrp", metaVar = "threshold", usage = "Minimum rule probability (rules below this probability will be pruned)")
    private float minRuleProbability = 1.0e-11f;

    @Option(name = "-smooth", metaVar = "type", usage = "Type of grammar smoothing used.")
    private SmoothingType smooth = SmoothingType.SmoothAcrossParentBits;

    @Option(name = "-maxL", metaVar = "words", usage = "Maximum sentence length")
    private int maxSentenceLength = 10000;

    @Option(name = "-tuc", metaVar = "direction", usage = "Training corpus includes with unknown-word classes. Skips learning signature-based UNK-class rules")
    private boolean trainingCorpusIncludesUnks;

    @Option(name = "-b", metaVar = "direction", usage = "Binarization direction")
    private Binarization binarization = Binarization.LEFT;

    @Option(name = "-in", usage = "Read in previous binary-serialized grammar")
    private String inFile = null;

    @Option(name = "-rs", metaVar = "seed", usage = "Seed for random number generator")
    private int randSeed = 2;

    @Option(name = "-hor", metaVar = "markovization", usage = "Horizontal Markovization")
    private int horizontalMarkovization = 0;

    @Option(name = "-r", metaVar = "percentage", usage = "Level of Randomness at init")
    private double randomization = 1.0;

    // TODO Use s_0, s_1, and s_2 from LELA
    @Option(name = "-sm1", metaVar = "param", usage = "Lexicon smoothing parameter 1")
    private double smoothingParameter1 = 0.5;

    @Option(name = "-sm2", metaVar = "param", usage = "Lexicon smoothing parameter 2")
    private double smoothingParameter2 = 0.1;

    @Option(name = "-rare", metaVar = "threshold", usage = "Rare word threshold")
    private int rareWordThreshold = 20;

    @Option(name = "-writeIntermediateGrammars", usage = "Write intermediate grammars to disk after each smoothing cycle")
    private boolean writeIntermediateGrammars = false;

    @Option(name = "-writeSplittingGrammars", usage = "Write intermediate grammars to disk after each splitting cycle")
    private boolean writeSplittingGrammars = false;

    @Option(name = "-writeSerializedGrammars", usage = "Write Java-object-serialized grammars as well as BUBS-format")
    private boolean writeSerializedGrammars = false;

    @Option(name = "-ccClassifier", hidden = true, metaVar = "FILE", usage = "Complete closure classifier model (Java Serialized). Used in discriminative merge ranking")
    private File completeClosureClassifierFile = null;

    @Override
    protected void setup() {
        if (outputGrammarDirectory != null && !outputGrammarDirectory.exists()) {
            outputGrammarDirectory.mkdir();
        }
    }

    @Override
    public void run() throws FileNotFoundException {

        BaseLogger.singleton().config(
                String.format("Using %s binarization, randomness: %.3f  seed: %d", binarization.name(), randomization,
                        randSeed));

        RANDOM = new Random(randSeed);

        int emIterations = emIterationsPerCycle;

        final double[] smoothParams = { smoothingParameter1, smoothingParameter2 };
        System.out.println("Using smoothing parameters " + smoothParams[0] + " and " + smoothParams[1]);

        Corpus trainingCorpus = new Corpus(System.in);
        List<Tree<String>> trainTrees = Corpus.binarizeAndFilterTrees(trainingCorpus.trees(), horizontalMarkovization,
                maxSentenceLength, binarization);

        List<Tree<String>> devSetTrees = new ArrayList<Tree<String>>();
        if (devSet != null) {
            final Corpus devSetCorpus = new Corpus(new FileInputStream(devSet));
            devSetTrees = Corpus.binarizeAndFilterTrees(devSetCorpus.trees(), horizontalMarkovization,
                    maxSentenceLength, binarization);
        }

        // Special-case to initialize discriminative ranking function
        if (mergeRankingFunction == MergeRanking.Discriminative) {
            if (devSet == null) {
                throw new IllegalArgumentException(
                        "Discriminative merge ranking requires a development set (-d option)");
            }
            final DiscriminativeMergeObjectiveFunction mergeObjectiveFunction = (DiscriminativeMergeObjectiveFunction) mergeRankingFunction
                    .objectiveFunction();

            try {
                final CompleteClosureModel ccModel = new CompleteClosureModel(completeClosureClassifierFile, null);

                if (ccModel.binarization() != binarization) {
                    throw new IllegalArgumentException("Complete-closure classifier binarization ("
                            + ccModel.binarization() + ") does not match specified grammar binarization ("
                            + binarization + ")");
                }

                // Compile copies of training and development sets in String format
                final List<String> rawTrainingCorpus = new ArrayList<String>();
                for (final Tree<String> t : trainTrees) {
                    rawTrainingCorpus.add(t.toString());
                }
                final List<String> rawDevSet = new ArrayList<String>();
                for (final Tree<String> t : devSetTrees) {
                    rawDevSet.add(t.toString());
                }

                mergeObjectiveFunction.init(ccModel, rawTrainingCorpus, rawDevSet, minRuleProbability);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

        short[] numSubStatesArray = initializeSubStateArray(trainTrees, devSetTrees, tagNumberer);
        BaseLogger.singleton().info(
                "Read " + trainTrees.size() + " training trees with " + numSubStatesArray.length + " non-terminals");

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
        StateSetTreeList devSetStateSetTrees = new StateSetTreeList(devSetTrees, numSubStatesArray, false, tagNumberer);

        // get rid of the old trees
        trainTrees = null;
        devSetTrees = null;
        trainingCorpus = null;

        // If we're training without loading a split grammar, then we run once without splitting.
        if (inFile == null) {

            // Induce M0 grammar from training corpus
            BaseLogger.singleton().info("Inducing M0 grammar...");

            grammar = new Grammar(numSubStatesArray, new NoSmoothing(), minRuleProbability);
            final Lexicon tmp_lexicon = new Lexicon(numSubStatesArray, smoothParams, new NoSmoothing(),
                    !trainingCorpusIncludesUnks, minRuleProbability);
            for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
                tmp_lexicon.trainTree(stateSetTree, randomization, null, false, rareWordThreshold);
            }
            lexicon = new Lexicon(numSubStatesArray, smoothParams, new NoSmoothing(), !trainingCorpusIncludesUnks,
                    minRuleProbability);

            // TODO BaseLogger.singleton().config("Markov-0 grammar size: " + grammarSummaryString(markov0Grammar));

            BaseLogger.singleton().info("Performing one I/O iteration");

            for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
                lexicon.trainTree(stateSetTree, randomization, tmp_lexicon, false, rareWordThreshold);
                grammar.countUnsplitTree(stateSetTree);
            }
            lexicon.tieRareWordStats(rareWordThreshold);

            // remove the unlikely tags
            lexicon.removeUnlikelyTags(lexicon.threshold, -1.0);
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

        long cycleStartTime = System.currentTimeMillis();
        for (int splitIndex = startSplit; splitIndex < splitMergeCycles * 3; splitIndex++) {

            boolean smoothingStep = false, splittingStep = false;
            if (splitIndex % 3 == 0) {

                splittingStep = true;

                // Split
                cycleStartTime = System.currentTimeMillis();

                final CorpusStatistics corpusStatistics = new CorpusStatistics(tagNumberer, trainStateSetTrees);
                final int[] counts = corpusStatistics.getSymbolCounts();

                final int previousSplitCount = maxGrammar.totalSubStates();

                maxGrammar = maxGrammar.splitAllStates(randomization, counts);
                maxLexicon = maxLexicon.splitAllStates(counts, false);

                maxGrammar.setSmoother(new NoSmoothing());
                maxLexicon.setSmoother(new NoSmoothing());

                BaseLogger.singleton().info(
                        String.format("Split %d substates into %d", previousSplitCount, maxGrammar.totalSubStates()));
                emIterations = emIterationsPerCycle;

                // update the substate dependent objects
                grammar = maxGrammar;
                lexicon = maxLexicon;

            } else if (splitIndex % 3 == 1) {
                // Merge stage

                if (mergeFraction == 0) {
                    continue;
                }

                final double[][] substateProbabilities = GrammarMerger.computeSubstateConditionalProbabilities(
                        maxGrammar, maxLexicon, trainStateSetTrees);

                // Estimate likelihood gain/loss and rule-count - these computations are relatively inexpensive, so we
                // always perform them even though they will be ignored by some merge objective functions.
                final float[][][] mergeLikelihoodDeltas = GrammarMerger.computeMergeLikelihoodDeltas(maxGrammar,
                        maxLexicon, substateProbabilities, trainStateSetTrees);
                final int[][][] mergeRuleCountDeltas = maxGrammar.estimateMergeRuleCountDeltas(maxLexicon);

                final boolean[][][] mergeThesePairs = GrammarMerger.selectMergePairs(maxGrammar, maxLexicon,
                        substateProbabilities, mergeLikelihoodDeltas, mergeRuleCountDeltas, mergeFraction,
                        mergeRankingFunction, splitIndex / 3 + 1, minRuleProbability);

                grammar = GrammarMerger.merge(maxGrammar, maxLexicon, mergeThesePairs, substateProbabilities);

                trainStateSetTrees = new StateSetTreeList(trainStateSetTrees, grammar.numSubStates, false);
                devSetStateSetTrees = new StateSetTreeList(devSetStateSetTrees, grammar.numSubStates, false);

                // Retrain lexicon to finish the lexicon merge (updates the unknown-word model)
                lexicon = new Lexicon(grammar.numSubStates, maxLexicon.getSmoothingParams(), maxLexicon.getSmoother(),
                        !trainingCorpusIncludesUnks, maxLexicon.getPruningThreshold());
                final ArrayParser parser = new ArrayParser(grammar, maxLexicon);
                emIteration(parser, grammar, maxLexicon, null, lexicon, trainStateSetTrees, rareWordThreshold);
                // remove the unlikely tags
                lexicon.removeUnlikelyTags(lexicon.threshold, -1.0);

                maxGrammar = grammar;
                maxLexicon = lexicon;
                emIterations = emIterationsAfterMerge;

            } else {

                // Smooth
                smoothingStep = true;
                if (smooth == SmoothingType.None) {
                    continue;
                }

                maxGrammar.setSmoother(new SmoothAcrossParentBits(0.01, maxGrammar.splitTrees));
                maxLexicon.setSmoother(new SmoothAcrossParentBits(0.1, maxGrammar.splitTrees));
                emIterations = emIterationsWithSmoothing;

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

                grammar = result.grammar;
                lexicon = result.lexicon;

                // Record the best-performing grammar
                if (result.devSetLikelihood >= maxDevSetLikelihood) {
                    maxDevSetLikelihood = result.devSetLikelihood;
                    maxGrammar = grammar;
                    maxLexicon = lexicon;
                    droppingIterations = 0;
                } else {
                    droppingIterations++;
                }

                BaseLogger.singleton().info(
                        String.format(
                                "Iteration: %2d  Training set likelihood: %.4f  Time %d ms  nBinary=%d  nUnary=%d",
                                iteration, result.trainingSetLikelihood, result.emTime,
                                result.grammar.binaryRuleCount(minRuleProbability),
                                result.grammar.unaryRuleCount(minRuleProbability)));
            }

            if (smoothingStep) {
                BaseLogger.singleton().info(
                        String.format("Completed training cycle %d in %.1f s", (splitIndex / 3) + 1,
                                (System.currentTimeMillis() - cycleStartTime) / 1000.0));

                // Dump a grammar file to disk from time to time
                if (writeIntermediateGrammars) {
                    writeGrammar(maxGrammar, maxLexicon, (splitIndex / 3 + 1));
                }
            } else if (splittingStep && writeSplittingGrammars) {
                writeGrammar(maxGrammar, maxLexicon, (splitIndex / 3 + 1));
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

        if (maxDevSetLikelihood != 0) {
            BaseLogger.singleton().info("Dev-set log likelihood: " + maxDevSetLikelihood);
        }
        writeGrammar(maxGrammar, maxLexicon, splitMergeCycles);
    }

    /**
     * Execute a single EM iteration
     * 
     * @param currentGrammar
     * @param currentLexicon
     * @param trainStateSetTrees
     * @param devSetStateSetTrees
     * @param minimumRuleProbability
     * @return EM result, including the newly trained {@link FractionalCountGrammar}
     */
    private EmIterationResult emIteration(final int iteration, final Grammar currentGrammar,
            final Lexicon currentLexicon, final StateSetTreeList trainStateSetTrees,
            final StateSetTreeList devSetStateSetTrees, final double minimumRuleProbability) {

        final long t0 = System.currentTimeMillis();

        double devSetLikelihood = 0;

        if (devSetStateSetTrees != null && !devSetStateSetTrees.isEmpty()
                && BaseLogger.singleton().isLoggable(Level.FINER)) {
            // Compute and report the validation likelihood of the previous iteration
            devSetLikelihood = calculateLogLikelihood(currentGrammar, currentLexicon, devSetStateSetTrees);
            BaseLogger.singleton().finer(String.format("Validation set likelihood: %.3f", devSetLikelihood));
        }

        //
        // Compute training likelihood (E-step)
        //
        final Grammar newGrammar = new Grammar(currentGrammar, currentGrammar.numSubStates);
        final Lexicon newLexicon = new Lexicon(currentGrammar.numSubStates, currentLexicon.getSmoothingParams(),
                currentLexicon.getSmoother(), !trainingCorpusIncludesUnks, currentLexicon.getPruningThreshold());

        final ArrayParser parser = new ArrayParser(currentGrammar, currentLexicon);
        final double trainingLikelihood = emIteration(parser, currentGrammar, currentLexicon, newGrammar, newLexicon,
                trainStateSetTrees, rareWordThreshold);

        //
        // Maximize (M-step)
        //
        // remove the unlikely tags
        newLexicon.removeUnlikelyTags(newLexicon.threshold, -1.0);
        newGrammar.optimize(0);

        return new EmIterationResult(newGrammar, newLexicon, trainingLikelihood, devSetLikelihood,
                (int) (System.currentTimeMillis() - t0));
    }

    private double emIteration(final ArrayParser parser, final Grammar currentGrammar, final Lexicon currentLexicon,
            final Grammar newGrammar, final Lexicon newLexicon, final StateSetTreeList trainStateSetTrees,
            final int threshold) {

        double trainingLikelihood = 0;

        for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {

            if (newGrammar != null) {
                parser.parseAndCount(stateSetTree, true, newGrammar);
            } else {
                parser.parse(stateSetTree, true);
            }

            final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().insideScore(0), stateSetTree.label()
                    .insideScoreScale());

            // Skip sentences we couldn't parse
            if (Double.isInfinite(ll) || Double.isNaN(ll)) {
                continue;
            }

            newLexicon.trainTree(stateSetTree, -1, currentLexicon, true, threshold);
            trainingLikelihood += ll;
        }
        newLexicon.tieRareWordStats(rareWordThreshold);
        return trainingLikelihood;
    }

    void writeGrammar(final Grammar grammar, final Lexicon lexicon, final int cycle) {

        final String prefix = outputGrammarDirectory + "/" + outputGrammarPrefix + "_" + cycle + ".gr";

        try {
            // BUBS format (gzipped-text, UTF-8 encoded)
            final String gzFilename = prefix + ".gz";
            System.out.println("Saving grammar to " + gzFilename + ".");
            final Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(gzFilename)),
                    Charset.forName("UTF-8"));
            w.write(grammar.toString(lexicon.totalRules(minRuleProbability), minRuleProbability, rareWordThreshold,
                    horizontalMarkovization));
            w.write("===== LEXICON =====\n");
            w.write(lexicon.toString(minRuleProbability));
            w.close();

            if (writeSerializedGrammars) {
                // Write a Java Serialized Object
                final String serFilename = prefix + ".ser";
                final ParserData pData = new ParserData(lexicon, grammar, Numberer.getNumberers(),
                        grammar.numSubStates, horizontalMarkovization, binarization);

                System.out.println("Saving Java obect-serialized grammar to " + serFilename + ".");
                if (pData.Save(serFilename)) {
                    System.out.println("Saving successful.");
                } else {
                    System.out.println("Saving failed!");
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param maxGrammar
     * @param maxLexicon
     * @param corpus
     * @return The log likelihood of a corpus
     */
    private double calculateLogLikelihood(final Grammar maxGrammar, final Lexicon maxLexicon,
            final StateSetTreeList corpus) {

        // TODO Calculating log-likelihood of a dev-set fails, due to unknown rules (i.e., productions in the dev-set
        // not observed in the training corpus). So for the moment, we just skip this step.
        return Double.NEGATIVE_INFINITY;

        // final ArrayParser parser = new ArrayParser(maxGrammar, maxLexicon);
        // double totalLogLikelihood = 0;
        //
        // for (final Tree<StateSet> stateSetTree : corpus) {
        //
        // if (stateSetTree.isLeaf()) {
        // continue;
        // }
        //
        // // Only the inside scores are needed here
        // parser.insidePass(stateSetTree, false);
        // final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().insideScore(0), stateSetTree.label()
        // .insideScoreScale());
        //
        // // A few sentences are unparsable
        // if (!Double.isInfinite(ll) && !Double.isNaN(ll)) {
        // totalLogLikelihood += ll;
        // }
        // }
        // return totalLogLikelihood;
    }

    /**
     * Convert a single Tree[String] to Tree[StateSet]
     * 
     * @param trainingTrees
     * @param devSetTrees
     * @param tagNumberer
     * @return Substate array
     */

    private short[] initializeSubStateArray(final List<Tree<String>> trainingTrees,
            final List<Tree<String>> devSetTrees, final Numberer tagNumberer) {

        // first generate unsplit grammar and lexicon
        final short[] nSub = new short[] { 1, 1 };

        // do the training and validation sets, so that the numberer sees all tags and we can allocate big enough arrays
        // Note: although these variables are never read, this constructors add the validation trees into the
        // tagNumberer as a side effect, which is important
        @SuppressWarnings("unused")
        final StateSetTreeList trainStateSetTrees = new StateSetTreeList(trainingTrees, nSub, true, tagNumberer);
        @SuppressWarnings("unused")
        final StateSetTreeList devSetStateSetTrees = new StateSetTreeList(devSetTrees, nSub, true, tagNumberer);

        StateSetTreeList.initializeTagNumberer(trainingTrees, tagNumberer);
        StateSetTreeList.initializeTagNumberer(devSetTrees, tagNumberer);

        final short[] nSubStateArray = new short[tagNumberer.total()];
        Arrays.fill(nSubStateArray, (short) 1);
        nSubStateArray[0] = 1; // Start symbol

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
