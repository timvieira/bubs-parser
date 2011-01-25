package edu.ohsu.cslu.ella;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree.Factorization;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unit tests for {@link ConstrainedCsrSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Jan 15, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestConstrainedCsrSpmvParser {

    private ProductionListGrammar plGrammar0;
    private ProductionListGrammar plGrammar1;
    private ConstrainedChart chart0;
    private CsrSparseMatrixGrammar csrGrammar1;
    private ConstrainedCsrSpmvParser parser1;

    @Before
    public void setUp() throws IOException {
        // Induce a grammar from the sample tree and construct a basic constraining chart
        final StringCountGrammar sg = new StringCountGrammar(new StringReader(AllEllaTests.STRING_SAMPLE_TREE), null,
                null, 1);
        plGrammar0 = new ProductionListGrammar(sg);
        // Create a basic constraining chart
        final SparseMatrixGrammar unsplitGrammar = new CsrSparseMatrixGrammar(plGrammar0.binaryProductions,
                plGrammar0.unaryProductions, plGrammar0.lexicalProductions, plGrammar0.vocabulary, plGrammar0.lexicon,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);
        chart0 = new ConstrainedChart(BinaryTree.read(AllEllaTests.STRING_SAMPLE_TREE, String.class), unsplitGrammar);

        // Split the grammar
        plGrammar1 = plGrammar0.split(null, 0);
        csrGrammar1 = new CsrSparseMatrixGrammar(plGrammar1.binaryProductions, plGrammar1.unaryProductions,
                plGrammar1.lexicalProductions, plGrammar1.vocabulary, plGrammar1.lexicon, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);
    }

    /**
     * Parse with the split-1 grammar
     * 
     * @return extracted parse tree
     */
    private ParseTree parseWithGrammar1() {
        // Parse with the split-1 grammar
        // TODO It seems like the cell selector should be set directly in ConstrainedCsrSpmvParser
        final ParserDriver opts = new ParserDriver();
        opts.cellSelector = new ConstrainedCellSelector();
        parser1 = new ConstrainedCsrSpmvParser(opts, csrGrammar1);
        return parser1.findBestParse(chart0);
    }

    @Test
    public void testCsrConversion() {
        final CartesianProductFunction f = csrGrammar1.cartesianProductFunction;

        assertEquals(1, f.unpackLeftChild(f.pack((short) 1, (short) 4)));
        assertEquals(4, f.unpackRightChild(f.pack((short) 1, (short) 4)));
        assertEquals(2, f.unpackLeftChild(f.pack((short) 2, (short) 4)));
        assertEquals(4, f.unpackRightChild(f.pack((short) 2, (short) 4)));
        //
        // System.out.format("Found packed children = 1. Parent = %s (%d), left child = %s (%d), right child = %s (%d)\n",
        // csrGrammar1.nonTermSet.getSymbol(1), 1, csrGrammar1.nonTermSet.getSymbol(1), splitParent,
        // csrGrammar1.nonTermSet.getSymbol(rightChild), rightChild);
        // System.out.println("Re-packed: "
        // + csrGrammar1.cartesianProductFunction.pack((short) leftChild, (short) rightChild));

    }

    @Test
    public void test1SplitConstrainedViterbiParse() {

        final ParseTree parseTree1 = parseWithGrammar1();
        final ConstrainedChart chart1 = parser1.chart;

        // Verify expected probabilities in a few cells
        final SymbolSet<String> vocabulary = plGrammar1.vocabulary;
        final int s = vocabulary.getIndex("s");
        final int a_0 = vocabulary.getIndex("a_0");
        final int a_1 = vocabulary.getIndex("a_1");
        final int b_0 = vocabulary.getIndex("b_0");
        final int b_1 = vocabulary.getIndex("b_1");

        assertEquals(Math.log(1f / 6), chart1.getInside(0, 1, a_0), .001f);
        assertEquals(Math.log(1f / 6), chart1.getInside(0, 1, a_1), .001f);
        assertEquals(Math.log(1f / 6), chart1.getInside(1, 2, a_0), .001f);
        assertEquals(Math.log(1f / 6), chart1.getInside(1, 2, a_1), .001f);
        assertEquals(Math.log(1f / 4), chart1.getInside(2, 3, b_0), .001f);
        assertEquals(Math.log(1f / 4), chart1.getInside(2, 3, b_1), .001f);

        assertEquals(Math.log(1f / 1728), chart1.getInside(0, 2, a_0), .001f);
        assertEquals(Math.log(1f / 1728), chart1.getInside(0, 2, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 2, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 2, b_1), .001f);

        assertEquals(Math.log(1f / 24576), chart1.getInside(3, 5, b_0), .001f);
        assertEquals(Math.log(1f / 24576), chart1.getInside(3, 5, b_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(3, 5, a_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(3, 5, a_1), .001f);

        assertEquals(Math.log(1.0 / 195689447424l), chart1.getInside(0, 5, s), .001f);
        assertEquals(Math.log(1.0 / 97844723712l), chart1.getInside(0, 5, a_0), .001f);
        assertEquals(Math.log(1.0 / 97844723712l), chart1.getInside(0, 5, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 4, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 4, b_1), .001f);

        // And ensure that the extracted and unfactored parse matches the input gold tree
        final NaryTree<String> unfactoredTree = BinaryTree.read(parseTree1.toString(), String.class).unfactor(
                GrammarFormatType.Berkeley);
        assertEquals(AllEllaTests.STRING_SAMPLE_TREE, unfactoredTree.toString());
    }

    @Test
    public void test2SplitConstrainedViterbiParse() {
        // Parse with the split-1 grammar, creating a new constraining chart.
        parseWithGrammar1();

        // Split the grammar again
        // Split the grammar
        final ProductionListGrammar plGrammar2 = plGrammar1.split(null, 0);
        final CsrSparseMatrixGrammar csrGrammar2 = new CsrSparseMatrixGrammar(plGrammar2.binaryProductions,
                plGrammar2.unaryProductions, plGrammar2.lexicalProductions, plGrammar2.vocabulary, plGrammar2.lexicon,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);

        // Parse with the split-2 grammar, constrained by the split-1 chart
        // TODO It seems like the cell selector should be set directly in ConstrainedCsrSpmvParser
        final ParserDriver opts = new ParserDriver();
        opts.cellSelector = new ConstrainedCellSelector();
        final ConstrainedCsrSpmvParser parser2 = new ConstrainedCsrSpmvParser(opts, csrGrammar2);
        final ParseTree parseTree2 = parser2.findBestParse(parser1.chart);
        final ConstrainedChart chart2 = parser2.chart;

        // Verify expected probabilities in a few cells
        final SymbolSet<String> vocabulary = plGrammar2.vocabulary;
        final int a_0 = vocabulary.getIndex("a_0");
        final int a_3 = vocabulary.getIndex("a_3");
        final int b_0 = vocabulary.getIndex("b_0");
        final int b_2 = vocabulary.getIndex("b_2");

        assertEquals(Math.log(1f / 12), chart2.getInside(0, 1, a_0), .001f);
        assertEquals(Math.log(1f / 12), chart2.getInside(0, 1, a_3), .001f);
        assertEquals(Math.log(1f / 12), chart2.getInside(1, 2, a_0), .001f);
        assertEquals(Math.log(1f / 12), chart2.getInside(1, 2, a_3), .001f);
        assertEquals(Math.log(1f / 8), chart2.getInside(2, 3, b_0), .001f);
        assertEquals(Math.log(1f / 8), chart2.getInside(2, 3, b_2), .001f);

        assertEquals(Math.log(1f / 1728 / 32), chart2.getInside(0, 2, a_0), .001f);
        assertEquals(Math.log(1f / 1728 / 32), chart2.getInside(0, 2, a_3), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(0, 2, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(0, 2, b_2), .001f);

        assertEquals(Math.log(1f / 24576 / 128), chart2.getInside(3, 5, b_0), .001f);
        assertEquals(Math.log(1f / 24576 / 128), chart2.getInside(3, 5, b_2), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(3, 5, a_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(3, 5, a_3), .001f);

        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(0, 4, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(0, 4, b_2), .001f);

        // And ensure that the extracted and unfactored parse matches the input gold tree
        final NaryTree<String> unfactoredTree = BinaryTree.read(parseTree2.toString(), String.class).unfactor(
                GrammarFormatType.Berkeley);
        assertEquals(AllEllaTests.STRING_SAMPLE_TREE, unfactoredTree.toString());
    }

    @Test
    public void testWsjSubset() throws Exception {
        final String corpus = "parsing/wsj_24.mrgEC.1-20";

        // Induce a grammar from the sample tree and construct a basic constraining chart
        final StringCountGrammar sg = new StringCountGrammar(SharedNlpTests.unitTestDataAsReader(corpus),
                Factorization.RIGHT, GrammarFormatType.Berkeley, 0);
        plGrammar0 = new ProductionListGrammar(sg);
        // Create a basic constraining chart
        final SparseMatrixGrammar unsplitGrammar = new CsrSparseMatrixGrammar(plGrammar0.binaryProductions,
                plGrammar0.unaryProductions, plGrammar0.lexicalProductions, plGrammar0.vocabulary, plGrammar0.lexicon,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);

        // Split the grammar
        plGrammar1 = plGrammar0.split(null, 0);
        csrGrammar1 = new CsrSparseMatrixGrammar(plGrammar1.binaryProductions, plGrammar1.unaryProductions,
                plGrammar1.lexicalProductions, plGrammar1.vocabulary, plGrammar1.lexicon, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);

        // Parse each tree in the training corpus with the split-1 grammar
        final ParserDriver opts = new ParserDriver();
        opts.cellSelector = new ConstrainedCellSelector();
        parser1 = new ConstrainedCsrSpmvParser(opts, csrGrammar1);

        final BufferedReader br = new BufferedReader(SharedNlpTests.unitTestDataAsReader(corpus));

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final NaryTree<String> goldTree = NaryTree.read(line, String.class);
            final ConstrainedChart constrainingChart = new ConstrainedChart(goldTree.factor(GrammarFormatType.Berkeley,
                    Factorization.RIGHT), unsplitGrammar);
            final ParseTree parseTree1 = parser1.findBestParse(constrainingChart);
            final NaryTree<String> unfactoredTree = BinaryTree.read(parseTree1.toString(), String.class).unfactor(
                    GrammarFormatType.Berkeley);
            assertEquals(goldTree.toString(), unfactoredTree.toString());

        }
    }
}
