package edu.ohsu.cslu.ella;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree.Factorization;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.ella.ProductionListGrammar.NoiseGenerator;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * Learns a latent-variable grammar from a training corpus.
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TrainGrammar extends BaseCommandlineTool {

    @Option(name = "-f", aliases = { "--factorization" }, metaVar = "type", usage = "Factorizes unfactored trees. If not specified, assumes trees are already binarized")
    Factorization factorization = null;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Grammar Format (required if factorization is specified)")
    GrammarFormatType grammarFormatType = null;

    @Option(name = "-unk", aliases = { "--unk-threshold" }, metaVar = "threshold", usage = "The number of observations of a word required in order to add it to the lexicon.")
    private int lexicalUnkThreshold = 1;

    @Option(name = "-noise", metaVar = "noise (0-1)", usage = "Random noise to add to rule probabilities during each split")
    private float noise = 0.01f;

    @Option(name = "-rs", aliases = { "--random-seed" }, metaVar = "seed", usage = "Random seed (default = System.currentTimeMillis())")
    private long randomSeed;

    final LinkedList<ConstrainedChart> constrainingCharts = new LinkedList<ConstrainedChart>();
    private NoiseGenerator noiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(0.01f);

    @Override
    protected void setup() throws Exception {

        if (randomSeed == 0) {
            randomSeed = System.currentTimeMillis();
        }

        noiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(noise, randomSeed);
    }

    @Override
    protected void run() throws Exception {

        final BufferedReader trainingCorpusReader = new BufferedReader(new InputStreamReader(System.in));
        // Allow up to 20 MB of training data
        trainingCorpusReader.mark(20 * 1024 * 1024);

        final ProductionListGrammar plGrammar = induceGrammar(trainingCorpusReader);

        trainingCorpusReader.reset();
        loadConstrainingCharts(trainingCorpusReader, plGrammar);
        trainingCorpusReader.close();

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
        return new ProductionListGrammar(new StringCountGrammar(trainingCorpusReader, factorization, grammarFormatType,
                lexicalUnkThreshold));

    }

    final void loadConstrainingCharts(final BufferedReader trainingCorpusReader, final ProductionListGrammar plGrammar)
            throws IOException {
        // Convert M0 grammar to CSR format
        System.out.println("Converting to CSR format...");
        final CsrSparseMatrixGrammar csrGrammar0 = new CsrSparseMatrixGrammar(plGrammar.binaryProductions,
                plGrammar.unaryProductions, plGrammar.lexicalProductions, plGrammar.vocabulary, plGrammar.lexicon,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);

        // Load in constraining charts from training corpus
        System.out.println("Loading constraining charts...");
        int count = 0;
        for (String line = trainingCorpusReader.readLine(); line != null; line = trainingCorpusReader.readLine()) {
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
            // progressBar(count);
        }
    }

    ProductionListGrammar splitGrammar(final ProductionListGrammar plGrammar) {
        System.out.println("Splitting grammar...");
        return plGrammar.split(noiseGenerator);
    }

    EmIterationResult emIteration(final ConstrainedCsrSparseMatrixGrammar csrGrammar) {
        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorFactory = ConstrainedCellSelector.FACTORY;
        opts.realSemiring = true;
        final ConstrainedCsrSpmvParser parser = new ConstrainedCsrSpmvParser(opts, csrGrammar);

        final ConstrainedCountGrammar countGrammar = new ConstrainedCountGrammar(csrGrammar);

        // Iterate over the training corpus, parsing and counting rule occurrences
        int sentenceCount = 0;
        for (final ConstrainedChart constrainingChart : constrainingCharts) {
            parser.findBestParse(constrainingChart);
            parser.countRuleOccurrences(countGrammar);
            sentenceCount++;
            // progressBar(count);
        }

        return new EmIterationResult(new ProductionListGrammar(countGrammar, csrGrammar.parentGrammar),
                Float.NEGATIVE_INFINITY);
    }

    public static void main(final String[] args) {
        run(args);
    }

    public static class EmIterationResult {
        final ProductionListGrammar plGrammar;
        final float corpusLikelihood;

        public EmIterationResult(final ProductionListGrammar plGrammar, final float corpusLikelihood) {
            this.plGrammar = plGrammar;
            this.corpusLikelihood = corpusLikelihood;
        }
    }
}
