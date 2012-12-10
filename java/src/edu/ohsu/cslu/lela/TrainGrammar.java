/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Language;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
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

    @Option(name = "-ebs", usage = "Run one EM iteration before splitting Markov-0 grammar")
    private boolean emBeforeSplit;

    @Option(name = "-muc", aliases = { "--merge-unary-chains" }, usage = "Collapse unary chain probabilities into single productions")
    private boolean mergeUnaryChains;

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
            unkClassMap.put(i, lexicon.addSymbol(Tokenizer.berkeleyGetSignature(lexicon.getSymbol(i), false, lexicon)));
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
        final float[] estimatedMergeCost = estimateMergeCost(cscGrammar, countGrammar);
        final int[][] ruleCountDelta = countGrammar.estimateMergeRuleCountDelta();

        // Create a merge-cost wrapper for each non-terminal under consideration for merging (odd indices only)
        final ArrayList<MergeCost> mergeCosts = new ArrayList<MergeCost>();
        for (short i = 0; i < estimatedMergeCost.length; i++) {
            final short nt = (short) (i * 2 + 1);
            mergeCosts.add(new MergeCost(estimatedMergeCost[i], nt, ruleCountDelta[i]));
        }

        // Sort the list by cost (from least costly to most)
        Collections.sort(mergeCosts);

        // Copy the least-costly indices into a new array
        final short[] mergeIndices = new short[Math.round(mergeCosts.size() * mergeFraction)];
        for (int i = 0; i < mergeIndices.length; i++) {
            mergeIndices[i] = mergeCosts.get(i).nonTerminal;
        }

        // Output the costs and potential rule-count savings for each mergeable non-terminal
        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mergeCosts.size(); i++) {
                final MergeCost mergeCost = mergeCosts.get(i);
                sb.append(String.format("%11s  %12.6f  %6d  %6d  %6d\n",
                        countGrammar.vocabulary.getSymbol(mergeCost.nonTerminal), mergeCost.cost,
                        mergeCost.binaryRuleCountDelta, mergeCost.unaryRuleCountDelta, mergeCost.lexicalRuleCountDelta));
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
    private float[] estimateMergeCost(final ConstrainedCscSparseMatrixGrammar cscGrammar,
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

    private static class MergeCost implements Comparable<MergeCost> {

        private final float cost;
        private final short nonTerminal;
        private final int binaryRuleCountDelta;
        private final int unaryRuleCountDelta;
        private final int lexicalRuleCountDelta;

        public MergeCost(final float cost, final short nonTerminal, final int[] ruleCountDelta) {
            this.cost = cost;
            this.nonTerminal = nonTerminal;
            this.binaryRuleCountDelta = ruleCountDelta[0];
            this.unaryRuleCountDelta = ruleCountDelta[1];
            this.lexicalRuleCountDelta = ruleCountDelta[2];
        }

        @Override
        public int compareTo(final MergeCost o) {
            return Float.compare(cost, o.cost);
        }

    }
}
