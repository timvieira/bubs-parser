/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.lela;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Language;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.lela.FractionalCountGrammar.NoiseGenerator;
import edu.ohsu.cslu.lela.FractionalCountGrammar.RandomNoiseGenerator;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbResult;
import edu.ohsu.cslu.util.Strings;

/**
 * Learns a latent-variable grammar from a training corpus using the approach from Petrov et al., 2006
 * "Learning Accurate, Compact, and Interpretable Tree Annotation"
 * 
 * @author Aaron Dunlop
 */
public class TrainGrammar extends BaseCommandlineTool {

    @Option(name = "-gd", aliases = { "--grammar-directory" }, required = true, metaVar = "directory", usage = "Output grammar directory. Each merged grammar will be output in .gz format")
    private File outputGrammarDirectory;

    @Option(name = "-gp", aliases = { "--grammar-prefix" }, metaVar = "prefix", usage = "Output grammar file prefix (e.g. 'eng.' produces 'eng.sm1.gr.gz', 'eng.sm2.gr.gz', etc.")
    private String outputGrammarPrefix = "";

    @Option(name = "-uncommon", metaVar = "threshold", usage = "Smooth production probabilities for uncommon words")
    private int uncommonWordThreshold = 100;

    @Option(name = "-rare", metaVar = "threshold", usage = "Count tag -> word probabilities for really rare words, for smoothing into uncommon words and UNK-classes")
    private int rareWordThreshold = 20;

    // Note: Final grammar accuracy doesn't appear too sensitive to any of these smoothing parameters

    @Option(name = "-s0", metaVar = "s0", usage = "Common word smoothing parameter")
    private float s_0 = 1f;

    @Option(name = "-s1", metaVar = "s1", usage = "Uncommon word smoothing parameter")
    private float s_1 = 2f;

    @Option(name = "-s2", metaVar = "s2", usage = "Unseen word smoothing parameter")
    private float s_2 = 1f;

    @Option(name = "-c", aliases = { "--sm-cycles" }, metaVar = "cycles", usage = "Split-merge cycles")
    private int splitMergeCycles = 6;

    @Option(name = "-i", aliases = { "--em-iterations" }, metaVar = "count", usage = "EM iterations per split-merge cycle")
    private int emIterationsPerCycle = 50;

    @Option(name = "-mf", aliases = { "--merge-fraction" }, metaVar = "fraction", usage = "Fraction of new splits to re-merge in each split-merge cycle")
    float mergeFraction = 0.5f;

    @Option(name = "-ami", aliases = { "--after-merge" }, metaVar = "count", usage = "EM iterations after merge")
    private int emIterationsAfterMerge = 20;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Grammar Format; required if binarization is specified")
    GrammarFormatType grammarFormatType = GrammarFormatType.Berkeley;

    @Option(name = "-ot", aliases = { "--open-class-threshold" }, metaVar = "threshold", usage = "Learn unknown-word probabilities for tags producing at least n words")
    private int openClassPreterminalThreshold = 50;

    @Option(name = "-l", aliases = { "--language" }, metaVar = "language", usage = "Language. Output in grammar file headers.")
    private Language language = Language.English;

    @Option(name = "-b", aliases = { "--binarization" }, metaVar = "type", usage = "Binarization direction.")
    Binarization binarization = Binarization.LEFT;

    @Option(name = "-mrp", metaVar = "probability", usage = "Minimum rule log probability (rules with lower probability are pruned from the grammar)")
    private float minimumRuleLogProbability = -140f;

    @Option(name = "-noise", metaVar = "noise (0-1)", usage = "Random noise to add to rule probabilities during each split")
    private float noise = 0.01f;

    @Option(name = "-rs", aliases = { "--random-seed" }, metaVar = "seed", usage = "Random seed (default = System.currentTimeMillis())")
    private long randomSeed;

    @Option(name = "-ds", aliases = { "--dev-set" }, metaVar = "file", usage = "Dev-set trees. If specified, parse accuracy will be reported after each split-merge cycle")
    private File developmentSet;

    @Option(name = "-dpf", aliases = { "--prune-fraction" }, metaVar = "fraction", usage = "Pruning fraction (of non-terminals) for dev-set parsing")
    private float devsetParsePruneFraction = 0.6f;

    @Option(name = "-mr", metaVar = "objective function", usage = "Merge-candidate ranking function")
    private MergeRanking mergeRanking = MergeRanking.Likelihood;

    @Option(name = "-ebs", usage = "Run one EM iteration before splitting Markov-0 grammar")
    private boolean emBeforeSplit;

    @Option(name = "-muc", aliases = { "--merge-unary-chains" }, usage = "Collapse unary chain probabilities into single productions")
    private boolean mergeUnaryChains;

    /**
     * Configuration property key for the weight of estimated likelihood loss when ordering merge candidates. See also
     * {@link #OPT_RULE_COUNT_LAMBDA}.
     */
    public final static String OPT_LIKELIHOOD_LAMBDA = "likelihoodLambda";

    /**
     * Configuration property key for the weight of estimated rule count savings when ordering merge candidates. See
     * also {@link #OPT_LIKELIHOOD_LAMBDA}.
     */
    public final static String OPT_RULE_COUNT_LAMBDA = "ruleCountLambda";

    /** Maximum size of a training or development corpus in characters. Currently 20 MB */
    private final static int MAX_CORPUS_SIZE = 20 * 1024 * 1024;

    final ArrayList<NaryTree<String>> goldTrees = new ArrayList<NaryTree<String>>();
    final ArrayList<ConstrainingChart> constrainingCharts = new ArrayList<ConstrainingChart>();

    long parseTime = 0, countTime = 0;

    /** Total counts of all words observed in the training corpus. Used for lexicon smoothing */
    Int2IntOpenHashMap corpusWordCounts;
    /**
     * Total counts of all words observed in sentence-initial position in the training corpus. Used for lexicon
     * smoothing
     */
    Int2IntOpenHashMap sentenceInitialWordCounts;

    NoiseGenerator noiseGenerator;

    @Override
    protected void setup() {

        // Initialize here instead of above, to avoid printing a large random number when we output command-line usage
        if (randomSeed == 0) {
            randomSeed = System.currentTimeMillis();
        }

        noiseGenerator = new RandomNoiseGenerator(randomSeed, noise);

        if (outputGrammarDirectory != null && !outputGrammarDirectory.exists()) {
            outputGrammarDirectory.mkdir();
        }
    }

    @Override
    protected void run() throws IOException {

        train(new BufferedReader(new InputStreamReader(System.in)),
                developmentSet != null ? fileAsBufferedReader(developmentSet) : null);
    }

    void train(final BufferedReader trainingCorpusReader, final BufferedReader devCorpusReader) throws IOException {

        trainingCorpusReader.mark(MAX_CORPUS_SIZE);
        if (devCorpusReader != null) {
            devCorpusReader.mark(MAX_CORPUS_SIZE);
        }

        // Induce M0 grammar from training corpus
        BaseLogger.singleton().info("Inducing M0 grammar...");
        final StringCountGrammar scg = new StringCountGrammar(trainingCorpusReader, binarization, grammarFormatType);
        final FractionalCountGrammar markov0Grammar = scg.toFractionalCountGrammar(uncommonWordThreshold,
                rareWordThreshold);
        corpusWordCounts = scg.wordCounts(markov0Grammar.lexicon);
        sentenceInitialWordCounts = scg.sentenceInitialWordCounts(markov0Grammar.lexicon);

        // writeGrammarToFile("m0.gr.gz",
        // markov0Grammar.addUnkCounts(unkClassMap(markov0Grammar.lexicon), openClassPreterminalThreshold));

        BaseLogger.singleton().config("Markov-0 grammar size: " + grammarSummaryString(markov0Grammar));
        FractionalCountGrammar currentGrammar = markov0Grammar;
        trainingCorpusReader.reset();
        loadGoldTreesAndConstrainingCharts(trainingCorpusReader, currentGrammar);
        trainingCorpusReader.close();

        if (emBeforeSplit) {
            final ConstrainedCscSparseMatrixGrammar cscM0Grammar = cscGrammar(currentGrammar);

            final ParserDriver opts = new ParserDriver();
            opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
            final ConstrainedSplitInsideOutsideParser parser = new ConstrainedSplitInsideOutsideParser(opts,
                    cscM0Grammar);
            final FractionalCountGrammar countGrammar = new FractionalCountGrammar(cscM0Grammar.nonTermSet,
                    cscM0Grammar.lexSet, cscM0Grammar.packingFunction, corpusWordCounts, sentenceInitialWordCounts,
                    uncommonWordThreshold, rareWordThreshold);

            // Iterate over the training corpus, parsing and counting rule occurrences
            for (int i = 0; i < constrainingCharts.size(); i++) {
                parser.findBestParse(constrainingCharts.get(i));
                parser.countRuleOccurrences(countGrammar);
            }

            currentGrammar = countGrammar;

            // reloadConstrainingCharts(cscM0Grammar, cscGrammar(currentGrammar));

            BaseLogger.singleton()
                    .config("Pre-split grammar after 1 EM cycle: " + grammarSummaryString(currentGrammar));
        }

        // Run split-merge training cycles
        for (int cycle = 1; cycle <= splitMergeCycles; cycle++) {

            final long t0 = System.currentTimeMillis();

            //
            // Split
            //
            BaseLogger.singleton().info(String.format("=== Cycle %d ===", cycle));
            currentGrammar = currentGrammar.split(noiseGenerator);
            BaseLogger.singleton().config("Split grammar size: " + grammarSummaryString(currentGrammar));

            // At verbose logging levels, write the split grammar before EM
            if (BaseLogger.singleton().isLoggable(Level.FINE)) {
                writeGrammarToFile(String.format("split%d.gr.gz", cycle), currentGrammar);
            }

            //
            // Train the split grammar with EM
            //
            for (int i = 1; i <= emIterationsPerCycle; i++) {
                final EmIterationResult result = emIteration(currentGrammar, minimumRuleLogProbability);
                logEmIteration(result, i);
                currentGrammar = result.countGrammar;
            }
            BaseLogger.singleton().config("Learned grammar size: " + grammarSummaryString(currentGrammar));

            // At verbose log levels, write pre-merge grammar to file
            if (BaseLogger.singleton().isLoggable(Level.FINE)) {
                writeGrammarToFile(String.format("em%d.gr.gz", cycle), currentGrammar);
            }

            //
            // Estimate likelihood loss of re-merging and merge least costly splits
            //
            final ConstrainedCscSparseMatrixGrammar premergeCscGrammar = cscGrammar(currentGrammar);
            currentGrammar = merge(currentGrammar, premergeCscGrammar);
            BaseLogger.singleton().config("Merged grammar size:  " + grammarSummaryString(currentGrammar));

            // At verbose logging levels, write the merged grammar before post-merge EM and UNK productions
            if (BaseLogger.singleton().isLoggable(Level.FINE)) {
                writeGrammarToFile(String.format("merge%d.gr.gz", cycle), currentGrammar);
            }

            //
            // Run some more EM iterations on merged grammar
            //
            BaseLogger.singleton().info("Post-merge EM");
            for (int i = 1; i <= emIterationsAfterMerge; i++) {
                final EmIterationResult result = emIteration(currentGrammar, minimumRuleLogProbability);
                logEmIteration(result, i);
                currentGrammar = result.countGrammar;
            }

            // Add UNK productions
            final FractionalCountGrammar grammarWithUnks = currentGrammar.addUnkCounts(
                    unkClassMap(currentGrammar.lexicon), openClassPreterminalThreshold, s_0, s_1, s_2);

            // Collapse unary chains
            if (mergeUnaryChains) {
                grammarWithUnks.mergeUnaryChains();
            }

            //
            // TODO Smooth
            //

            // Write merged grammar
            // TODO Prune, output lexicon
            writeGrammarToFile(String.format("sm%d.gr.gz", cycle), grammarWithUnks);

            // Output dev-set parse accuracy
            if (developmentSet != null) {
                parseDevSet(devCorpusReader, cscGrammar(grammarWithUnks));
            }

            BaseLogger.singleton().info(
                    String.format("Completed cycle %d in %.2f s", cycle, (System.currentTimeMillis() - t0) / 1000f));
        }
    }

    private ConstrainedCscSparseMatrixGrammar cscGrammar(final FractionalCountGrammar countGrammar) {
        return new ConstrainedCscSparseMatrixGrammar(countGrammar, GrammarFormatType.Berkeley,
                PerfectIntPairHashPackingFunction.class);
    }

    private Int2IntOpenHashMap unkClassMap(final SymbolSet<String> lexicon) {
        final Int2IntOpenHashMap unkClassMap = new Int2IntOpenHashMap();
        for (int i = 0; i < lexicon.size(); i++) {
            unkClassMap.put(i, lexicon.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(lexicon.getSymbol(i), false, lexicon)));
        }
        return unkClassMap;
    }

    final void loadGoldTreesAndConstrainingCharts(final BufferedReader trainingCorpusReader,
            final FractionalCountGrammar markov0Grammar) throws IOException {

        // Convert Markov-0 grammar to CSC format
        final SparseMatrixGrammar cscGrammar0 = cscGrammar(markov0Grammar);

        // Load in constraining charts from training corpus
        BaseLogger.singleton().info("Loading gold trees and constraining charts...");

        for (String line = trainingCorpusReader.readLine(); line != null; line = trainingCorpusReader.readLine()) {
            final NaryTree<String> goldTree = NaryTree.read(line, String.class);
            goldTrees.add(goldTree);

            final BinaryTree<String> factoredTree = goldTree.binarize(grammarFormatType, binarization);
            constrainingCharts.add(new ConstrainingChart(factoredTree, cscGrammar0));
        }
    }

    /**
     * Execute a single EM iteration
     * 
     * @param currentGrammar
     * @param minimumRuleLogProb
     * @return EM result, including the newly trained {@link FractionalCountGrammar}
     */
    EmIterationResult emIteration(final FractionalCountGrammar currentGrammar, final float minimumRuleLogProb) {

        final long t0 = System.currentTimeMillis();
        final ConstrainedCscSparseMatrixGrammar cscGrammar = cscGrammar(currentGrammar);

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        final ConstrainedSplitInsideOutsideParser parser = new ConstrainedSplitInsideOutsideParser(opts, cscGrammar);

        final FractionalCountGrammar countGrammar = new FractionalCountGrammar(cscGrammar.nonTermSet,
                cscGrammar.lexSet, cscGrammar.packingFunction, corpusWordCounts, sentenceInitialWordCounts,
                uncommonWordThreshold, rareWordThreshold);

        final long t1 = System.currentTimeMillis();
        // Iterate over the training corpus, parsing and counting rule occurrences
        double corpusLikelihood = 0f;
        for (int i = 0; i < constrainingCharts.size(); i++) {
            // TODO Remove detailed timing instrumentation
            final long t00 = System.nanoTime();
            parser.findBestParse(constrainingCharts.get(i));
            final long t01 = System.nanoTime();
            parseTime += (t01 - t00);
            corpusLikelihood += parser.chart.getInside(0, parser.chart.size(), 0);
            parser.countRuleOccurrences(countGrammar);
            countTime += (System.nanoTime() - t01);

        }
        final long t2 = System.currentTimeMillis();

        // TODO Combine these two steps, or operate directly on the grammar, to avoid some unnecessary grammar cloning
        // Prune rules below the minimum probability threshold
        final FractionalCountGrammar prunedGrammar = countGrammar.clone(minimumRuleLogProb);

        // Smooth uncommon-word counts
        final FractionalCountGrammar smoothedGrammar = prunedGrammar.smooth(openClassPreterminalThreshold, s_0, s_1,
                s_2);

        return new EmIterationResult(smoothedGrammar, corpusLikelihood, (int) (t2 - t1),
                (int) (System.currentTimeMillis() - t2 + t1 - t0));
    }

    /**
     * For unit testing
     * 
     * @param countGrammar
     * @return Copy of the supplied grammar with the less-beneficial non-terminals merged
     */
    FractionalCountGrammar merge(final FractionalCountGrammar countGrammar) {
        return merge(countGrammar, cscGrammar(countGrammar));
    }

    /**
     * Returns a copy of the supplied count grammar with the less-beneficial non-terminals merged
     * 
     * @param countGrammar
     * @param cscGrammar {@link ConstrainedCscSparseMatrixGrammar} version of <code>countGrammar</code>
     * @return Copy of the supplied count grammar with the less-beneficial non-terminals merged
     */
    private FractionalCountGrammar merge(final FractionalCountGrammar countGrammar,
            final ConstrainedCscSparseMatrixGrammar cscGrammar) {

        // Special-case - just merge TOP_0
        if (mergeFraction == 0) {
            final FractionalCountGrammar mergedGrammar = countGrammar.merge(new short[] { 1 });
            BaseLogger.singleton()
                    .config("Merged 1 nonterminal. Grammar size:  " + grammarSummaryString(mergedGrammar));
            return mergedGrammar;
        }

        // Estimate the merge cost
        final float[] estimatedMergeLikelihoodLoss = estimateLikelihoodLoss(cscGrammar, countGrammar);
        final int[][] ruleCountDelta = countGrammar.estimateMergeRuleCountDelta();

        // Create a merge-cost wrapper for each non-terminal under consideration for merging (odd indices only)
        final ArrayList<MergeCost> mergeCosts = new ArrayList<MergeCost>();
        for (short i = 0; i < estimatedMergeLikelihoodLoss.length; i++) {
            final short nt = (short) (i * 2 + 1);
            mergeCosts.add(new MergeCost(countGrammar.vocabulary.getSymbol(nt), nt, estimatedMergeLikelihoodLoss[i],
                    ruleCountDelta[i]));
        }

        // Sort the list of merge costs by various criteria and assign rankings for use by the final comparator.
        computeMergeCosts(mergeCosts);

        // Sort the list by cost (from least costly to most), according to the specified objective function.
        Collections.sort(mergeCosts, mergeRanking.comparator());

        // Copy the least-costly indices into a new array (round the number to merge down)
        final short[] mergeIndices = new short[(int) (mergeCosts.size() * mergeFraction)];
        for (int i = 0; i < mergeIndices.length; i++) {
            mergeIndices[i] = mergeCosts.get(i).nonTerminalIndex;
        }

        // Output the costs and potential rule-count savings for each mergeable non-terminal
        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mergeCosts.size(); i++) {
                sb.append(mergeCosts.get(i).toString());
                sb.append('\n');

                // Label the cut-point
                if (i == mergeIndices.length) {
                    sb.append("--------\n");
                }
            }
            BaseLogger.singleton().fine("Merge Costs:");
            BaseLogger.singleton().fine(sb.toString());
        }

        // Perform the merge
        final FractionalCountGrammar mergedGrammar = countGrammar.merge(mergeIndices);
        BaseLogger.singleton().config(
                "Merged " + mergeIndices.length + " nonterminals. Grammar size:  "
                        + grammarSummaryString(mergedGrammar));
        return mergedGrammar;
    }

    /**
     * Estimate the likelihood loss for each nonterminal if it is re-merged with its split sibling.
     * 
     * @param cscGrammar
     * @return Array of estimated likelihood losses for each split nonterminal if merged with its sibling (an array 1/2
     *         the size of the non-terminal set)
     */
    private float[] estimateLikelihoodLoss(final ConstrainedCscSparseMatrixGrammar cscGrammar,
            final FractionalCountGrammar countGrammar) {

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        final ConstrainedSplitInsideOutsideParser parser = new ConstrainedSplitInsideOutsideParser(opts, cscGrammar);

        // Compute log(p_1), log(p_2) for each split pair based on relative frequency counts of each
        final float[] logSplitFraction = countGrammar.logSplitFraction();

        // Iterate over training corpus and compute merge cost
        final float[] mergeCost = new float[cscGrammar.nonTermSet.size() / 2];

        // Iterate over the training corpus, parsing and counting rule occurrences
        @SuppressWarnings("unused")
        int sentenceCount = 0;
        for (final ConstrainingChart constrainingChart : constrainingCharts) {
            parser.findBestParse(constrainingChart);
            parser.countMergeCost(mergeCost, logSplitFraction);
            sentenceCount++;
        }
        return mergeCost;
    }

    /**
     * Sorts the list of merge costs by various criteria and assigns ranking numbers for each (e.g. estimated likelihood
     * loss, rule count savings, etc.), then computes total merge cost for each non-terminal pair
     * 
     * @param mergeCosts An unordered list of estimated merge costs. Note that this list will be reordered.
     */
    private void computeMergeCosts(final ArrayList<MergeCost> mergeCosts) {

        // Estimated likelihood loss
        Collections.sort(mergeCosts, MergeRanking.Likelihood.comparator());
        for (int i = 0; i < mergeCosts.size(); i++) {
            mergeCosts.get(i).likelihoodLossRanking = i;
        }

        // Total rule count delta
        Collections.sort(mergeCosts, MergeRanking.TotalRuleCount.comparator());
        for (int i = 0; i < mergeCosts.size(); i++) {
            mergeCosts.get(i).totalRuleCountRanking = i;
        }

        for (final MergeCost mc : mergeCosts) {
            mc.mergeCost = mergeRanking.objectiveFunction.mergeCost(mc);
        }
    }

    private void parseDevSet(final BufferedReader devCorpusReader, final ConstrainedCscSparseMatrixGrammar mergedGrammar)
            throws IOException {

        final long t0 = System.currentTimeMillis();
        BaseLogger.singleton().info("Parsing development set...");
        devCorpusReader.reset();

        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAX_BEAM_WIDTH,
                Integer.toString(Math.round(devsetParsePruneFraction * mergedGrammar.nonTermSet.size())));

        final ParserDriver opts = new ParserDriver();
        opts.fomModel = new InsideProb();
        final CartesianProductHashSpmlParser parser = new CartesianProductHashSpmlParser(opts, mergedGrammar);
        final BracketEvaluator evaluator = new BracketEvaluator();

        for (String line = devCorpusReader.readLine(); line != null; line = devCorpusReader.readLine()) {
            final NaryTree<String> goldTree = NaryTree.read(line, String.class);

            // Extract tokens from training tree, parse, and evaluate
            final String sentence = Strings.join(goldTree.leafLabels(), " ");
            final ParseTask context = parser.parseSentence(sentence);

            if (context.binaryParse != null) {
                evaluator.evaluate(goldTree, context.binaryParse.unfactor(GrammarFormatType.Berkeley));
            }
        }

        final EvalbResult evalbResult = evaluator.accumulatedResult();
        BaseLogger.singleton()
                .config(String.format("Parsed development set in %d ms", System.currentTimeMillis() - t0));
        BaseLogger.singleton().info(String.format("Dev-set F-score: %.2f", evalbResult.f1() * 100));
    }

    /**
     * Reports the result of an EM iteration to the user via {@link BaseLogger}
     * 
     * @param result
     * @param i
     */
    private void logEmIteration(final EmIterationResult result, final int i) {
        if (BaseLogger.singleton().isLoggable(Level.CONFIG)) {
            BaseLogger.singleton().config(
                    String.format("Iteration: %2d  Likelihood: %.2f  EM Time: %5dms  Grammar Time: %4dms  "
                            + grammarSummaryString(result.countGrammar), i, result.corpusLikelihood, result.emTime,
                            result.grammarConversionTime));
        } else {
            BaseLogger.singleton().info(
                    String.format("Iteration: %2d  Likelihood: %.2f  EM Time: %5dms  Grammar Time: %dms", i,
                            result.corpusLikelihood, result.emTime, result.grammarConversionTime));
        }
    }

    /**
     * Writes a grammar file to disk
     * 
     * @param filename
     * @param grammar
     * @throws IOException
     */
    private void writeGrammarToFile(String filename, final FractionalCountGrammar grammar) throws IOException {

        filename = (outputGrammarPrefix != null ? outputGrammarPrefix : "") + filename;

        if (outputGrammarDirectory != null) {
            final Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(
                    outputGrammarDirectory, filename))));
            grammar.write(new PrintWriter(w), false, language, grammarFormatType, rareWordThreshold);
            w.close();
        }
    }

    /**
     * Returns a short summary of statistics about the supplied grammar
     * 
     * @param grammar
     * @return A short summary of statistics about the supplied grammar
     */
    private String grammarSummaryString(final FractionalCountGrammar grammar) {
        return String.format("%4d nonterminals  %7d binary rules  %6d unary rules  %7d lexical rules",
                grammar.vocabulary.size(), grammar.binaryRules(), grammar.unaryRules(), grammar.lexicalRules());
    }

    public static void main(final String[] args) {
        run(args);
    }

    public static class EmIterationResult {

        final FractionalCountGrammar countGrammar;
        final double corpusLikelihood;
        final int emTime;
        final int grammarConversionTime;

        public EmIterationResult(final FractionalCountGrammar countGrammar, final double corpusLikelihood,
                final int emTime, final int grammarConversionTime) {
            this.countGrammar = countGrammar;
            this.corpusLikelihood = corpusLikelihood;
            this.emTime = emTime;
            this.grammarConversionTime = grammarConversionTime;
        }

        @Override
        public String toString() {
            return String.format("%.2f", corpusLikelihood);
        }
    }

    private static class MergeCost {

        private final String nonTerminal;
        private final short nonTerminalIndex;
        private final float estimatedLikelihoodLoss;
        private final int binaryRuleCountDelta;
        private final int unaryRuleCountDelta;
        private final int lexicalRuleCountDelta;

        private final int totalRuleCountDelta;

        /**
         * The ordinal ranking of this non-terminal pair within the list of {@link MergeCost}s if sorted by
         * {@link #estimatedLikelihoodLoss}.
         */
        private int likelihoodLossRanking;

        /**
         * The ordinal ranking of this non-terminal pair within the list of {@link MergeCost}s if sorted by
         * {@link #totalRuleCountDelta}.
         */
        private int totalRuleCountRanking;

        /** Combined merge cost, as assigned by a {@link MergeObjectiveFunction} */
        private float mergeCost;

        /**
         * @param nonTerminal
         * @param nonTerminalIndex
         * @param estimatedLikelihoodLoss Estimated likelihood loss on the training corpus.
         * @param ruleCountDelta Estimated rule-count savings if this pair is merged. 3-tuple of binary, unary, and
         *            lexical counts.
         */
        public MergeCost(final String nonTerminal, final short nonTerminalIndex, final float estimatedLikelihoodLoss,
                final int[] ruleCountDelta) {

            this.nonTerminal = nonTerminal;
            this.nonTerminalIndex = nonTerminalIndex;

            this.estimatedLikelihoodLoss = estimatedLikelihoodLoss;

            this.binaryRuleCountDelta = ruleCountDelta[0];
            this.unaryRuleCountDelta = ruleCountDelta[1];
            this.lexicalRuleCountDelta = ruleCountDelta[2];

            this.totalRuleCountDelta = binaryRuleCountDelta + unaryRuleCountDelta + lexicalRuleCountDelta;
        }

        @Override
        public String toString() {
            return String.format("%11s  %14.6f  %6d  %6d  %6d  %6d  %6d  %6d  %7.2f", nonTerminal,
                    estimatedLikelihoodLoss, likelihoodLossRanking, binaryRuleCountDelta, unaryRuleCountDelta,
                    lexicalRuleCountDelta, totalRuleCountDelta, totalRuleCountRanking, mergeCost);
        }
    }

    /**
     * Objective functions for non-terminal merge ranking. Each enumeration option constructs and exposes an anonymous
     * {@link Comparator} to re-rank merge candidates.
     */
    private static enum MergeRanking {

        Likelihood(new MergeObjectiveFunction() {
            @Override
            public float mergeCost(final MergeCost mergeCost) {
                return mergeCost.estimatedLikelihoodLoss;
            }
        }),

        TotalRuleCount(new MergeObjectiveFunction() {
            @Override
            public float mergeCost(final MergeCost mergeCost) {
                return mergeCost.totalRuleCountDelta;
            }
        }),

        /**
         * Note: Likelihood and rule-count rankings must be computed, as in
         * {@link TrainGrammar#computeMergeCosts(ArrayList)} before applying this function
         */
        Combined(new MergeObjectiveFunction() {

            final float likelihoodLambda = GlobalConfigProperties.singleton()
                    .getFloatProperty(OPT_LIKELIHOOD_LAMBDA, 1);
            final float ruleCountLambda = GlobalConfigProperties.singleton().getFloatProperty(OPT_RULE_COUNT_LAMBDA, 1);

            @Override
            public float mergeCost(final MergeCost mergeCost) {
                return mergeCost.likelihoodLossRanking * likelihoodLambda + mergeCost.totalRuleCountRanking
                        * ruleCountLambda;
            }
        });

        /** Comparator for the specified objective function */
        public final MergeObjectiveFunction objectiveFunction;

        private MergeRanking(final MergeObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
        }

        public Comparator<MergeCost> comparator() {
            return new Comparator<MergeCost>() {
                @Override
                public int compare(final MergeCost o1, final MergeCost o2) {
                    return Float.compare(objectiveFunction.mergeCost(o1), objectiveFunction.mergeCost(o2));
                }
            };
        }

        private static abstract class MergeObjectiveFunction {
            public abstract float mergeCost(MergeCost mergeCost);
        }

    }

}
