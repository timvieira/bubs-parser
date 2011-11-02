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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Factorization;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.lela.ProductionListGrammar.NoiseGenerator;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbResult;
import edu.ohsu.cslu.util.Strings;

/**
 * Learns a latent-variable grammar from a training corpus.
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 */
public class TrainGrammar extends BaseCommandlineTool {

    @Option(name = "-b", aliases = { "--binarization" }, metaVar = "type", usage = "Binarization direction.")
    Factorization binarization = Factorization.LEFT;

    @Option(name = "-i", aliases = { "--em-iterations" }, metaVar = "count", usage = "EM iterations per split-merge cycle")
    private int emIterationsPerCycle = 50;

    @Option(name = "-c", aliases = { "--sm-cycles" }, metaVar = "cycles", usage = "Split-merge cycles")
    private int splitMergeCycles = 6;

    @Option(name = "-mrp", metaVar = "probability", usage = "Minimum rule log probability (rules with lower probability are pruned from the grammar)")
    private float minimumRuleProbability = -15f;

    @Option(name = "-gd", aliases = { "--grammar-directory" }, metaVar = "directory", usage = "Output grammar directory. Each merged grammar will be output in .gz format")
    private File outputGrammarDirectory;

    @Option(name = "-gp", aliases = { "--grammar-prefix" }, metaVar = "prefix", usage = "Output grammar file prefix (e.g. 'eng.' produces 'eng.sm1.gr.gz', 'eng.sm2.gr.gz', etc.")
    private String outputGrammarPrefix = "";

    @Option(name = "-ds", aliases = { "--dev-set" }, metaVar = "file", usage = "Dev-set trees. If specified, parse accuracy will be reported after each split-merge cycle")
    private File developmentSet;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Grammar Format; required if binarization is specified")
    GrammarFormatType grammarFormatType = GrammarFormatType.Berkeley;

    @Option(name = "-unk", aliases = { "--unk-threshold" }, metaVar = "threshold", usage = "Learn unknown-word probabilities for words occurring <= threshold times")
    private int lexicalUnkThreshold = 5;

    @Option(name = "-noise", metaVar = "noise (0-1)", usage = "Random noise to add to rule probabilities during each split")
    private float noise = 0.01f;

    @Option(name = "-rs", aliases = { "--random-seed" }, metaVar = "seed", usage = "Random seed (default = System.currentTimeMillis())")
    private long randomSeed;

    /** Maximum size of a training or development corpus in characters. Currently 20 MB */
    private final static int MAX_CORPUS_SIZE = 20 * 1024 * 1024;

    final ArrayList<NaryTree<String>> goldTrees = new ArrayList<NaryTree<String>>();
    final ArrayList<ConstrainingChart> constrainingCharts = new ArrayList<ConstrainingChart>();

    private NoiseGenerator noiseGenerator;

    @Override
    protected void setup() {

        if (randomSeed == 0) {
            randomSeed = System.currentTimeMillis();
        }

        noiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(noise, randomSeed);

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
        devCorpusReader.mark(MAX_CORPUS_SIZE);

        final ProductionListGrammar markov0Grammar = induceGrammar(trainingCorpusReader);
        ProductionListGrammar plGrammar = markov0Grammar;
        trainingCorpusReader.reset();

        loadGoldTreesAndCharts(trainingCorpusReader, plGrammar);
        trainingCorpusReader.close();

        // Run split-merge training cycles
        for (int cycle = 1; cycle <= splitMergeCycles; cycle++) {

            // Split
            BaseLogger.singleton().info(String.format("=== Cycle %d ===", cycle));
            BaseLogger.singleton().fine("Splitting grammar...");
            plGrammar = plGrammar.split(noiseGenerator);

            // Train the split grammar with EM
            for (int i = 1; i <= emIterationsPerCycle; i++) {
                final EmIterationResult result = emIteration(cscGrammar(plGrammar), minimumRuleProbability);
                BaseLogger.singleton().fine(
                        String.format("Iteration: %2d  Likelihood: %.2f", i, result.corpusLikelihood));
                plGrammar = result.plGrammar;

            }

            final ConstrainedInsideOutsideGrammar finalSplitGrammar = cscGrammar(plGrammar);

            // Merge
            plGrammar = plGrammar.merge(new short[] { 1 });

            // Add UNK productions
            plGrammar = addUnkProbabilities(plGrammar);
            final ConstrainedInsideOutsideGrammar mergedGrammar = cscGrammar(plGrammar);

            // Smooth

            // Output grammar
            if (outputGrammarDirectory != null) {
                final String filename = String.format("%ssm%d.gr.gz", outputGrammarPrefix != null ? outputGrammarPrefix
                        : "", cycle);
                final Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(
                        outputGrammarDirectory, filename))));
                w.write(plGrammar.toString());
                w.close();
            }

            reloadConstrainingCharts(finalSplitGrammar, mergedGrammar);

            if (developmentSet != null) {
                parseDevSet(devCorpusReader, mergedGrammar);
            }
        }
    }

    private void parseDevSet(final BufferedReader devCorpusReader, final ConstrainedInsideOutsideGrammar mergedGrammar)
            throws IOException {
        BaseLogger.singleton().info("Parsing development set...");
        devCorpusReader.reset();

        final CartesianProductHashSpmlParser parser = new CartesianProductHashSpmlParser(new ParserDriver(),
                mergedGrammar);
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
        BaseLogger.singleton().info(String.format("Dev-set F-score: %.2f", evalbResult.f1() * 100));
    }

    /**
     * Induces a {@link ProductionListGrammar} from a training corpus.
     * 
     * @return ProductionListGrammar
     * @throws IOException
     */
    final ProductionListGrammar induceGrammar(final BufferedReader trainingCorpusReader) throws IOException {
        // Induce M0 grammar from training corpus
        BaseLogger.singleton().info("Inducing M0 grammar...");
        return new ProductionListGrammar(new StringCountGrammar(trainingCorpusReader, binarization, grammarFormatType));

    }

    private ConstrainedInsideOutsideGrammar cscGrammar(final ProductionListGrammar plGrammar) {
        return new ConstrainedInsideOutsideGrammar(plGrammar, GrammarFormatType.Berkeley,
                PerfectIntPairHashPackingFunction.class);
    }

    final void loadGoldTreesAndCharts(final BufferedReader trainingCorpusReader, final ProductionListGrammar plGrammar)
            throws IOException {

        // Convert Markov-0 grammar to CSC format
        System.out.println("Converting to CSC format...");
        final SparseMatrixGrammar cscGrammar0 = new InsideOutsideCscSparseMatrixGrammar(plGrammar.binaryProductions,
                plGrammar.unaryProductions, plGrammar.lexicalProductions, plGrammar.vocabulary, plGrammar.lexicon,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class, true);

        // Load in constraining charts from training corpus
        System.out.println("Loading gold trees and constraining charts...");
        int count = 0;
        for (String line = trainingCorpusReader.readLine(); line != null; line = trainingCorpusReader.readLine()) {
            final NaryTree<String> goldTree = NaryTree.read(line, String.class);
            goldTrees.add(goldTree);

            final BinaryTree<String> factoredTree = goldTree.factor(grammarFormatType, binarization);
            try {
                final ConstrainingChart c = new ConstrainingChart(factoredTree, cscGrammar0);
                constrainingCharts.add(c);
                c.extractBestParse(0);
            } catch (final ArrayIndexOutOfBoundsException e) {
                System.err.println("Failed on tree " + count + "(" + factoredTree.leaves() + " words)");
                System.err.println(factoredTree.toString());
            }
            count++;
            // progressBar(count);
        }
    }

    /**
     * Parses with a split grammar and replaces the current {@link ConstrainingChart}s with 1-best parses, populated
     * with a merged version of the same grammar.
     * 
     * @param finalSplitGrammar
     * @param mergedGrammar
     */
    final void reloadConstrainingCharts(final ConstrainedInsideOutsideGrammar finalSplitGrammar,
            final ConstrainedInsideOutsideGrammar mergedGrammar) {

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        final ConstrainedInsideOutsideParser parser = new ConstrainedInsideOutsideParser(opts, finalSplitGrammar);

        BaseLogger.singleton().info("Reloading constraining charts...");

        // Iterate over the training corpus, parsing and replacing current ConstrainingCharts
        for (int i = 0; i < constrainingCharts.size(); i++) {
            parser.findBestParse(constrainingCharts.get(i));
            constrainingCharts.set(i, new ConstrainingChart(parser.chart, mergedGrammar));
        }
    }

    /**
     * Execute a single EM iteration
     * 
     * @param cscGrammar
     * @param minimumRuleLogProbability
     * @return EM result, including the newly trained {@link ProductionListGrammar}
     */
    EmIterationResult emIteration(final ConstrainedInsideOutsideGrammar cscGrammar,
            final float minimumRuleLogProbability) {

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        // opts.realSemiring = true;
        final ConstrainedInsideOutsideParser parser = new ConstrainedInsideOutsideParser(opts, cscGrammar);

        final FractionalCountGrammar countGrammar = new FractionalCountGrammar((SplitVocabulary) cscGrammar.nonTermSet,
                cscGrammar.lexSet, cscGrammar.packingFunction);

        // Iterate over the training corpus, parsing and counting rule occurrences
        int sentenceCount = 0;
        float corpusLikelihood = 0f;
        final ArrayList<ConstrainedChart> charts = new ArrayList<ConstrainedChart>();
        for (final ConstrainingChart constrainingChart : constrainingCharts) {
            parser.findBestParse(constrainingChart);
            charts.add(parser.chart);
            corpusLikelihood += parser.chart.getInside(0, parser.chart.size(), 0);
            // System.out.format("%.3f\n", parser.chart.getInside(0, parser.chart.size(), 0));
            parser.countRuleOccurrences(countGrammar);
            sentenceCount++;
            // progressBar(count);
        }

        return new EmIterationResult(countGrammar.toProductionListGrammar(minimumRuleLogProbability), corpusLikelihood,
                charts);
    }

    /**
     * Add UNK probabilities to lexical rules Note that words that occur <= lexicalUnkThreshold times will also be
     * included in their lexicalized form.
     * 
     * @param plGrammar
     * @return {@link ProductionListGrammar} including lexical UNK rules.
     */
    private ProductionListGrammar addUnkProbabilities(final ProductionListGrammar plGrammar) {

        // Count words - overall and separately when they occur as the first word in a sentence
        final Object2IntOpenHashMap<String> lexicalEntryCounts = new Object2IntOpenHashMap<String>();
        lexicalEntryCounts.defaultReturnValue(0);
        final Object2IntOpenHashMap<String> sentenceInitialLexicalEntryCounts = new Object2IntOpenHashMap<String>();
        sentenceInitialLexicalEntryCounts.defaultReturnValue(0);

        for (final NaryTree<String> tree : goldTrees) {
            int i = 0;
            for (final NaryTree<String> leaf : tree.leafTraversal()) {
                final String word = leaf.label();
                lexicalEntryCounts.put(word, lexicalEntryCounts.getInt(word) + 1);
                if (i == 0) {
                    sentenceInitialLexicalEntryCounts.put(word, sentenceInitialLexicalEntryCounts.getInt(word) + 1);
                }
            }
            i++;
        }

        // Add UNK probabilities for uncommon words
        final ArrayList<Production> unkProductions = new ArrayList<Production>();
        for (final Production p : plGrammar.lexicalProductions) {
            final String word = plGrammar.lexicon.getSymbol(p.leftChild);
            final int unkIndex = plGrammar.lexicon.addSymbol(Tokenizer.berkeleyGetSignature(word, false,
                    plGrammar.lexicon));
            final int count = lexicalEntryCounts.get(word);

            if (count <= lexicalUnkThreshold) {
                final int sentenceInitialCount = sentenceInitialLexicalEntryCounts.get(word);

                if (sentenceInitialCount == 0) {
                    // Duplicate the original lexical probability for the UNK
                    unkProductions.add(new Production(p.parent, unkIndex, p.prob, true, plGrammar.vocabulary,
                            plGrammar.lexicon));

                } else {
                    // Split the additional probability proportionally between the regular UNK- value and the
                    // sentence-initial version
                    final int initialUnkIndex = plGrammar.lexicon.addSymbol(Tokenizer.berkeleyGetSignature(word, true,
                            plGrammar.lexicon));
                    final double initialFraction = 1.0 * sentenceInitialCount / count;

                    unkProductions.add(new Production(p.parent, initialUnkIndex, (float) Math.log(Math.exp(p.prob)
                            * initialFraction), true, plGrammar.vocabulary, plGrammar.lexicon));
                    unkProductions.add(new Production(p.parent, unkIndex, (float) Math.log(Math.exp(p.prob)
                            * (1 - initialFraction)), true, plGrammar.vocabulary, plGrammar.lexicon));
                }
            }
        }

        // Re-normalize
        plGrammar.lexicalProductions.addAll(unkProductions);
        return plGrammar.normalizeProbabilities();
    }

    private double parseFScore(final Grammar grammar, final List<NaryTree<String>> goldTrees) {
        final LeftCscSparseMatrixGrammar cscGrammar = new LeftCscSparseMatrixGrammar(grammar);
        final CartesianProductHashSpmlParser parser = new CartesianProductHashSpmlParser(new ParserDriver(), cscGrammar);

        final BracketEvaluator evaluator = new BracketEvaluator();

        for (final NaryTree<String> goldTree : goldTrees) {
            // Extract tokens from training tree, parse, and evaluate
            // TODO Parse from tree instead
            final String sentence = Strings.join(goldTree.leafLabels(), " ");
            final ParseTask context = parser.parseSentence(sentence);

            if (context.binaryParse != null) {
                evaluator.evaluate(goldTree, context.binaryParse.unfactor(cscGrammar.grammarFormat));
            }
        }

        final EvalbResult evalbResult = evaluator.accumulatedResult();
        return evalbResult.f1();
    }

    public static void main(final String[] args) {
        run(args);
    }

    public static class EmIterationResult {

        final ProductionListGrammar plGrammar;
        final float corpusLikelihood;
        final ArrayList<ConstrainedChart> charts;

        public EmIterationResult(final ProductionListGrammar plGrammar, final float corpusLikelihood,
                final ArrayList<ConstrainedChart> charts) {
            this.plGrammar = plGrammar;
            this.corpusLikelihood = corpusLikelihood;
            this.charts = charts;
        }

        @Override
        public String toString() {
            return String.format("%.2f", corpusLikelihood);
        }
    }
}
