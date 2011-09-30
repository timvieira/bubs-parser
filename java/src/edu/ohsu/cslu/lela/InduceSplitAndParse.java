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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Factorization;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;

public class InduceSplitAndParse extends BaseCommandlineTool {

    @Option(name = "-f", aliases = { "--factorization" }, metaVar = "type", usage = "Factorizes unfactored trees. If not specified, assumes trees are already binarized")
    private Factorization factorization = null;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Grammar Format (required if factorization is specified)")
    private GrammarFormatType grammarFormatType = null;

    @Option(name = "-tc", aliases = { "--training-corpus" }, metaVar = "corpus", usage = "Training corpus")
    private File trainingCorpus;

    // @Option(name = "-dt", aliases = { "--collect-detailed-statistics" }, usage =
    // "Collect detailed timing statistics")
    // private boolean collectDetailedTimings;

    @Override
    protected void run() throws IOException {

        // Induce M0 grammar from training corpus
        final long t0 = System.currentTimeMillis();
        System.out.println("Inducing M0 grammar...");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        // Allow up to 20 MB of training data
        br.mark(20 * 1024 * 1024);

        final StringCountGrammar sg0 = new StringCountGrammar(br, factorization, grammarFormatType);
        final ProductionListGrammar plGrammar0 = new ProductionListGrammar(sg0);
        final long t1 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms", t1 - t0));

        // Convert M0 grammar to CSC format
        System.out.println("Converting to CSR format...");
        final SparseMatrixGrammar cscGrammar0 = new ConstrainedInsideOutsideGrammar(plGrammar0.binaryProductions,
                plGrammar0.unaryProductions, plGrammar0.lexicalProductions, plGrammar0.vocabulary, plGrammar0.lexicon,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class, null);
        final long t2 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms", t2 - t1));

        // Load in constraining charts from training corpus
        if (trainingCorpus != null) {
            br = new BufferedReader(new FileReader(trainingCorpus));
        } else {
            br.reset();
        }
        System.out.println("Loading constraining charts...");
        final LinkedList<ConstrainingChart> constrainingCharts = new LinkedList<ConstrainingChart>();
        int count = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final BinaryTree<String> goldTree = NaryTree.read(line, String.class).factor(grammarFormatType,
                    factorization);
            try {
                final ConstrainingChart c = new ConstrainingChart(goldTree, cscGrammar0);
                constrainingCharts.add(c);
                c.extractBestParse(0);
            } catch (final ArrayIndexOutOfBoundsException e) {
                System.err.println("Failed on tree " + count + "(" + goldTree.leaves() + " words)");
                System.err.println(goldTree.toString());
            }
            count++;
            progressBar(count);
        }
        System.out.println();
        final long t3 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms (%.2f sentences / sec)", t3 - t2, count * 1000.0 / (t3 - t2)));

        // Create a split-1 grammar
        System.out.println("Splitting grammar...");
        final ProductionListGrammar plGrammar1 = plGrammar0
                .split(new ProductionListGrammar.RandomNoiseGenerator(0.01f));
        final long t4 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms", t4 - t3));

        // Convert to CSR format
        System.out.println("Converting to CSR format...");
        final ConstrainedInsideOutsideGrammar cscGrammar1 = new ConstrainedInsideOutsideGrammar(plGrammar1,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);
        final long t5 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms", t5 - t4));

        System.out.println("Grammar summary: " + cscGrammar1.packingFunction.toString());

        // Parse the entire training corpus with the split-1 grammar
        System.out.println("Parsing with split grammar...");
        count = 0;
        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        final ConstrainedInsideOutsideParser parser1 = new ConstrainedInsideOutsideParser(opts, cscGrammar1);

        for (final ConstrainingChart constrainingChart : constrainingCharts) {
            parser1.findBestParse(constrainingChart);
            constrainingChart.extractBestParse(0);
            count++;
            progressBar(count);
        }
        System.out.println();
        final long t6 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms (%.2f sentences / sec)", t6 - t5, count * 1000.0 / (t6 - t5)));

        // if (collectDetailedTimings) {
        // System.out.println("Init Time (ms)              : " + parser1.totalInitializationTime / 1000000);
        // System.out.println("Lex-prod Time (ms)          : " + parser1.totalLexProdTime / 1000000);
        // System.out.println("X-product Time (ms)         : " + parser1.totalConstrainedXproductTime / 1000000);
        // // System.out.println("X-product Fill Time (ms)  : " + parser1.totalXproductFillTime / 1000000);
        // // System.out.println("Cell Visit Time (ms) : " + parser1.totalVisitTime / 1000000);
        // System.out.println("Binary SpMV Time (ms)       : " + parser1.totalConstrainedBinaryTime / 1000000);
        // System.out.println("Unary SpMV Time (ms)        : " + parser1.totalConstrainedUnaryTime / 1000000);
        // System.out.println("Outside Time (ms)           : " + parser1.totalConstrainedOutsideTime / 1000000);
        // System.out
        // .println("Outside X-product Time (ms) : " + parser1.totalConstrainedOutsideXproductTime / 1000000);
        // System.out.println("Outside Unary Time (ms)     : " + parser1.totalConstrainedOutsideUnaryTime / 1000000);
        // System.out.println("Extraction Time (ms)        : " + parser1.totalExtractionTime / 1000000);
        // }
    }

    private void progressBar(final int count) {
        if (count % 100 == 0) {
            System.out.print('.');
        }
        if (count % 5000 == 0) {
            System.out.println();
        }
    }

    public static void main(final String[] args) {
        run(args);
    }
}
