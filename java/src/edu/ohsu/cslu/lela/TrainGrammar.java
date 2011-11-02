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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Factorization;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.lela.ProductionListGrammar.NoiseGenerator;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * Learns a latent-variable grammar from a training corpus.
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 */
public class TrainGrammar extends BaseCommandlineTool {

    @Option(name = "-f", aliases = { "--factorization" }, metaVar = "type", usage = "Factorizes unfactored trees. If not specified, assumes trees are already binarized")
    Factorization factorization = null;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Grammar Format; required if factorization is specified")
    GrammarFormatType grammarFormatType = GrammarFormatType.Berkeley;

    @Option(name = "-unk", aliases = { "--unk-threshold" }, metaVar = "threshold", usage = "Learn unknown-word probabilities for words occurring <= threshold times")
    private int lexicalUnkThreshold = 5;

    @Option(name = "-noise", metaVar = "noise (0-1)", usage = "Random noise to add to rule probabilities during each split")
    private float noise = 0.01f;

    @Option(name = "-rs", aliases = { "--random-seed" }, metaVar = "seed", usage = "Random seed (default = System.currentTimeMillis())")
    private long randomSeed;

    @Option(name = "-i", aliases = { "--iterations" }, metaVar = "count", usage = "Split-merge iterations")
    private int splitMergeIterations;

    final ArrayList<NaryTree<String>> goldTrees = new ArrayList<NaryTree<String>>();
    final ArrayList<ConstrainingChart> constrainingCharts = new ArrayList<ConstrainingChart>();

    private NoiseGenerator noiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(0.01f);

    @Override
    protected void setup() {

        if (randomSeed == 0) {
            randomSeed = System.currentTimeMillis();
        }

        noiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(noise, randomSeed);
    }

    @Override
    protected void run() throws IOException {

        final BufferedReader trainingCorpusReader = new BufferedReader(new InputStreamReader(System.in));
        // Allow up to 20 MB of training data
        trainingCorpusReader.mark(20 * 1024 * 1024);

        final ProductionListGrammar plGrammar = induceGrammar(trainingCorpusReader);

        trainingCorpusReader.reset();
        loadGoldTreesAndCharts(trainingCorpusReader, plGrammar);
        trainingCorpusReader.close();

        // Run split-merge training cycles
        for (int i = 0; i < splitMergeIterations; i++) {

            // Split

            // Train the split grammar with EM

            // Merge

            // Smooth
        }

        final SymbolSet<String> lexicon = plGrammar.lexicon;

        //
        // Add UNK probabilities to lexical rules
        // Note that words that occur <= lexicalUnkThreshold times will also be included in their lexicalized form.
        //

        // Count words
        final Object2IntOpenHashMap<String> lexicalEntryCounts = new Object2IntOpenHashMap<String>();
        for (final NaryTree<String> tree : goldTrees) {
            for (final NaryTree<String> leaf : tree.leafTraversal()) {
                final String word = leaf.label();
                lexicalEntryCounts.put(word, lexicalEntryCounts.getInt(word) + 1);
            }
        }

        //
        for (final NaryTree<String> tree : goldTrees) {
            int i = 0;
            for (final NaryTree<String> leaf : tree.leafTraversal()) {
                final String word = leaf.label();
                if (lexicalEntryCounts.get(word) <= lexicalUnkThreshold) {
                    final String unkStr = Tokenizer.berkeleyGetSignature(word, i == 0, lexicon);
                    lexicalEntryCounts.put(unkStr, lexicalEntryCounts.getInt(unkStr) + 1);
                }
                i++;
            }
        }

        // Add UNK probabilities for uncommon words
        for (final Production p : plGrammar.lexicalProductions) {
            if (lexicalEntryCounts.get(lexicon.getSymbol(p.leftChild)) <= lexicalUnkThreshold) {

            }
        }
        // Re-normalize

    }

    /**
     * Induces a {@link ProductionListGrammar} from a training corpus.
     * 
     * @return ProductionListGrammar
     * @throws IOException
     */
    final ProductionListGrammar induceGrammar(final BufferedReader trainingCorpusReader) throws IOException {
        // Induce M0 grammar from training corpus
        System.out.println("Inducing M0 grammar...");
        return new ProductionListGrammar(new StringCountGrammar(trainingCorpusReader, factorization, grammarFormatType));

    }

    final void loadGoldTreesAndCharts(final BufferedReader trainingCorpusReader, final ProductionListGrammar plGrammar)
            throws IOException {

        // Convert M0 grammar to CSC format
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

            final BinaryTree<String> factoredTree = goldTree.factor(grammarFormatType, factorization);
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

        System.out.println("Reloading constraining charts...");

        // Iterate over the training corpus, parsing and replacing current ConstrainingCharts
        for (int i = 0; i < constrainingCharts.size(); i++) {
            parser.findBestParse(constrainingCharts.get(i));
            constrainingCharts.set(i, new ConstrainingChart(parser.chart, mergedGrammar));
        }
    }

    ProductionListGrammar splitGrammar(final ProductionListGrammar plGrammar) {
        System.out.println("Splitting grammar...");
        return plGrammar.split(noiseGenerator);
    }

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
