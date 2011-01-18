package edu.ohsu.cslu.ella;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Unit tests for {Wlink {@link ConstrainedChart}.
 * 
 * @author Aaron Dunlop
 * @since Jan 15, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestConstrainedChart {

    /**
     * Tests constructing a {@link ConstrainedChart} from a gold tree and then re-extracting that tree from the chart.
     * 
     * @throws IOException
     */
    @Test
    public void testGoldTreeConstructor() throws IOException {
        // Induce a grammar from the sample tree
        final StringCountGrammar sg = new StringCountGrammar(new StringReader(AllEllaTests.STRING_SAMPLE_TREE), null,
                null, 1);

        // Construct a SparseMatrixGrammar from the induced grammar
        final ProductionListGrammar pg = new ProductionListGrammar(sg);
        final SparseMatrixGrammar sparseMatrixGrammar = new CsrSparseMatrixGrammar(pg.binaryProductions,
                pg.unaryProductions, pg.lexicalProductions, pg.vocabulary, pg.lexicon, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);

        final ConstrainedChart cc = new ConstrainedChart(
                BinaryTree.read(AllEllaTests.STRING_SAMPLE_TREE, String.class), sparseMatrixGrammar);

        // The chart should size itself according to the longest unary chain
        assertEquals(2, cc.beamWidth());

        // Verify expected probabilities in a few cells
        final SymbolSet<String> vocabulary = pg.vocabulary;
        final int s = vocabulary.getIndex("s");
        final int a = vocabulary.getIndex("a");
        final int b = vocabulary.getIndex("b");

        assertEquals(0, cc.getInside(0, 5, s), .001f);
        assertEquals(0, cc.getInside(0, 5, a), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, cc.getInside(0, 4, b), .001f);

        assertEquals(0, cc.getInside(0, 2, a), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, cc.getInside(0, 2, b), .001f);

        assertEquals(0, cc.getInside(0, 3, a), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, cc.getInside(0, 2, b), .001f);

        assertEquals(0, cc.getInside(3, 5, b), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, cc.getInside(3, 5, a), .001f);

        // And ensure that the extracted parse matches the input gold tree
        assertEquals(AllEllaTests.STRING_SAMPLE_TREE, cc.extractBestParse(vocabulary.getIndex("s")).toString());
    }

    @Test
    public void testConstrainingChartConstructor() {
        fail("Not Implemented");
        // Create a basic constraining chart
        // Split the grammar
        // Create a chart based on the new grammar, constrained by the first chart
    }

    @Test
    public void testCountRuleObservations() {
        fail("Not Implemented");
    }
}
