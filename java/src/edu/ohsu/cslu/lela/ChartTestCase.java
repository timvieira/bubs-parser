package edu.ohsu.cslu.lela;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Shared functionality for {@link TestConstrainedChart} and {@link TestConstrainingChart}
 */
public class ChartTestCase {

    protected final static short[][] OPEN_CELLS = new short[][] { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 4 }, { 4, 5 },
            { 0, 2 }, { 3, 5 }, { 0, 3 }, { 0, 5 } };

    protected ProductionListGrammar plGrammar0;
    protected ConstrainedInsideOutsideGrammar cscGrammar0;

    ProductionListGrammar plGrammar1;
    ConstrainedInsideOutsideGrammar cscGrammar1;

    protected short top;
    protected short a;
    protected short b;
    protected int c;
    protected int d;

    @Before
    public void setUp() throws IOException {
        // Induce a grammar from the sample tree
        final StringCountGrammar sg = new StringCountGrammar(new StringReader(AllLelaTests.STRING_SAMPLE_TREE), null,
                null);

        // Construct a SparseMatrixGrammar from the induced grammar
        plGrammar0 = new ProductionListGrammar(sg);
        cscGrammar0 = new ConstrainedInsideOutsideGrammar(plGrammar0, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);

        final SymbolSet<String> vocabulary = plGrammar0.vocabulary;
        top = (short) vocabulary.getIndex("top");
        a = (short) vocabulary.getIndex("a");
        b = (short) vocabulary.getIndex("b");
        c = plGrammar0.lexicon.getIndex("c");
        d = plGrammar0.lexicon.getIndex("d");

        // Split the grammar
        plGrammar1 = plGrammar0.split(new ProductionListGrammar.BiasedNoiseGenerator(0f));
        cscGrammar1 = new ConstrainedInsideOutsideGrammar(plGrammar1, GrammarFormatType.Berkeley,
                PerfectIntPairHashPackingFunction.class);
    }

    /**
     * @return a populated 1-split {@link ConstrainedChart}
     */
    protected ConstrainedChart create1SplitConstrainedChart() {

        // Construct a basic 0-split constraining chart
        final ConstrainingChart chart0 = new ConstrainingChart(BinaryTree.read(AllLelaTests.STRING_SAMPLE_TREE,
                String.class), cscGrammar0);

        // Create and populate a 1-split ConstrainedChart
        final ConstrainedChart chart = new ConstrainedChart(chart0, cscGrammar1);

        // 0,1
        int offset = chart.offset(chart.cellIndex(0, 1));
        chart.nonTerminalIndices[offset] = 2; // a_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;
        chart.packedChildren[offset] = cscGrammar1.packingFunction.packLexical(c);

        chart.nonTerminalIndices[offset + 1] = 3; // a_1
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;
        chart.packedChildren[offset + 1] = cscGrammar1.packingFunction.packLexical(c);

        // 1,2
        offset = chart.offset(chart.cellIndex(1, 2));
        chart.nonTerminalIndices[offset] = 2; // a_0
        chart.insideProbabilities[offset] = -2;
        chart.outsideProbabilities[offset] = -2;
        chart.packedChildren[offset] = cscGrammar1.packingFunction.packLexical(c);

        chart.nonTerminalIndices[offset + 1] = 3; // a_1 *
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;
        chart.packedChildren[offset + 1] = cscGrammar1.packingFunction.packLexical(c);

        // 2,3
        offset = chart.offset(chart.cellIndex(2, 3));
        chart.nonTerminalIndices[offset] = 4; // b_0
        chart.insideProbabilities[offset] = -2;
        chart.outsideProbabilities[offset] = -2;
        chart.packedChildren[offset] = cscGrammar1.packingFunction.packLexical(d);

        chart.nonTerminalIndices[offset + 1] = 5; // b_1 *
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;
        chart.packedChildren[offset + 1] = cscGrammar1.packingFunction.packLexical(d);

        // 3,4
        offset = chart.offset(chart.cellIndex(3, 4));
        chart.nonTerminalIndices[offset] = 4; // b_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;

        chart.nonTerminalIndices[offset + 1] = 5; // b_1
        chart.insideProbabilities[offset + 1] = -2;
        chart.outsideProbabilities[offset + 1] = -2;

        chart.nonTerminalIndices[offset + 2] = 4; // b_0
        chart.insideProbabilities[offset + 2] = -2;
        chart.outsideProbabilities[offset + 2] = -2;
        chart.packedChildren[offset + 2] = cscGrammar1.packingFunction.packLexical(d);

        chart.nonTerminalIndices[offset + 3] = 5; // b_1 *
        chart.insideProbabilities[offset + 3] = -1;
        chart.outsideProbabilities[offset + 3] = -1;
        chart.packedChildren[offset + 3] = cscGrammar1.packingFunction.packLexical(d);

        // 4,5
        offset = chart.offset(chart.cellIndex(4, 5));
        chart.nonTerminalIndices[offset] = 2; // a_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;
        chart.packedChildren[offset] = cscGrammar1.packingFunction.packLexical(d);

        chart.nonTerminalIndices[offset + 1] = 3; // a_1
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;
        chart.packedChildren[offset + 1] = cscGrammar1.packingFunction.packLexical(d);

        // 0,2
        offset = chart.offset(chart.cellIndex(0, 2));
        chart.nonTerminalIndices[offset] = 2; // a_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;

        chart.nonTerminalIndices[offset + 1] = 3; // a_1
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;

        // 3,5
        offset = chart.offset(chart.cellIndex(3, 5));
        chart.nonTerminalIndices[offset] = 4; // b_0
        chart.insideProbabilities[offset] = -2;
        chart.outsideProbabilities[offset] = -2;

        chart.nonTerminalIndices[offset + 1] = 5; // b_1 *
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;

        // 0,3
        offset = chart.offset(chart.cellIndex(0, 3));
        chart.nonTerminalIndices[offset] = 2; // a_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;

        chart.nonTerminalIndices[offset + 1] = 3; // a_1
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;

        // 0,5
        offset = chart.offset(chart.cellIndex(0, 5));
        chart.nonTerminalIndices[offset] = 0; // top
        chart.insideProbabilities[offset] = -2;
        chart.outsideProbabilities[offset] = -2;

        chart.nonTerminalIndices[offset + 2] = 2; // a_0
        chart.insideProbabilities[offset + 2] = -2;
        chart.outsideProbabilities[offset + 2] = -2;

        chart.nonTerminalIndices[offset + 3] = 3; // a_1 *
        chart.insideProbabilities[offset + 3] = -1;
        chart.outsideProbabilities[offset + 3] = -1;

        return chart;
    }
}
