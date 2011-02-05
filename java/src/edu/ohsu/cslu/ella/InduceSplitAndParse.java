package edu.ohsu.cslu.ella;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.LinkedList;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree.Factorization;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;

public class InduceSplitAndParse extends BaseCommandlineTool {

    @Option(name = "-f", aliases = { "--factorization" }, metaVar = "type", usage = "Factorizes unfactored trees. If not specified, assumes trees are already binarized")
    private Factorization factorization = null;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Grammar Format (required if factorization is specified)")
    private Grammar.GrammarFormatType grammarFormatType = null;

    @Option(name = "-unk", aliases = { "--unk-threshold" }, metaVar = "threshold", usage = "The number of observations of a word required in order to add it to the lexicon.")
    private int lexicalUnkThreshold = 1;

    @Option(name = "-tc", aliases = { "--training-corpus" }, metaVar = "corpus", usage = "Training corpus")
    private File trainingCorpus;

    @Option(name = "-dt", aliases = { "--collect-detailed-statistics" }, usage = "Collect detailed timing statistics")
    private boolean collectDetailedTimings;

    @Override
    protected void run() throws Exception {

        // Induce M0 grammar from training corpus
        final long t0 = System.currentTimeMillis();
        System.out.println("Inducing M0 grammar...");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        // Allow up to 20 MB of training data
        br.mark(20 * 1024 * 1024);

        final StringCountGrammar sg0 = new StringCountGrammar(br, factorization, grammarFormatType, lexicalUnkThreshold);
        final ProductionListGrammar plGrammar0 = new ProductionListGrammar(sg0);
        final long t1 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms", t1 - t0));

        // Convert M0 grammar to CSR format
        System.out.println("Converting to CSR format...");
        final CsrSparseMatrixGrammar csrGrammar0 = new CsrSparseMatrixGrammar(plGrammar0.binaryProductions,
                plGrammar0.unaryProductions, plGrammar0.lexicalProductions, plGrammar0.vocabulary, plGrammar0.lexicon,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);
        final long t2 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms", t2 - t1));

        // Load in constraining charts from training corpus
        if (trainingCorpus != null) {
            br = new BufferedReader(new FileReader(trainingCorpus));
        } else {
            br.reset();
        }
        System.out.println("Loading constraining charts...");
        final LinkedList<ConstrainedChart> constrainingCharts = new LinkedList<ConstrainedChart>();
        int count = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final BinaryTree<String> goldTree = NaryTree.read(line, String.class).factor(grammarFormatType,
                    factorization);
            try {
                final ConstrainedChart c = new ConstrainedChart(goldTree, csrGrammar0);
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
        final ConstrainedCsrSparseMatrixGrammar csrGrammar1 = new ConstrainedCsrSparseMatrixGrammar(
                plGrammar1.binaryProductions, plGrammar1.unaryProductions, plGrammar1.lexicalProductions,
                plGrammar1.vocabulary, plGrammar1.lexicon, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);
        final long t5 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms", t5 - t4));

        System.out.println("Grammar summary: " + csrGrammar1.cartesianProductFunction.toString());

        // Parse the entire training corpus with the split-1 grammar
        System.out.println("Parsing with split grammar...");
        count = 0;
        final ParserDriver opts = new ParserDriver();
        opts.cellSelector = new ConstrainedCellSelector();
        final ConstrainedCsrSpmvParser parser1 = new ConstrainedCsrSpmvParser(opts, csrGrammar1, collectDetailedTimings);

        for (final ConstrainedChart constrainingChart : constrainingCharts) {
            parser1.findBestParse(constrainingChart);
            constrainingChart.extractBestParse(0);
            count++;
            progressBar(count);
        }
        System.out.println();
        final long t6 = System.currentTimeMillis();
        System.out.println(String.format("Time: %d ms (%.2f sentences / sec)", t6 - t5, count * 1000.0 / (t6 - t5)));

        if (collectDetailedTimings) {
            System.out.println("Init Time (ms)            : " + parser1.totalInitializationTime / 1000000);
            System.out.println("Lex-prod Time (ms)        : " + parser1.totalLexProdTime / 1000000);
            System.out.println("X-product Time (ms)       : " + parser1.totalConstrainedXproductTime / 1000000);
            System.out.println("X-product Fill Time (ms)  : " + parser1.totalXproductFillTime / 1000000);
            // System.out.println("Cell Visit Time (ms) : " + parser1.totalVisitTime / 1000000);
            System.out.println("Binary SpMV Time (ms)     : " + parser1.totalConstrainedBinaryTime / 1000000);
            System.out.println("Unary SpMV Time (ms)      : " + parser1.totalConstrainedUnaryTime / 1000000);
            System.out.println("Extraction Time (ms)      : " + parser1.totalExtractionTime / 1000000);
        }
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
