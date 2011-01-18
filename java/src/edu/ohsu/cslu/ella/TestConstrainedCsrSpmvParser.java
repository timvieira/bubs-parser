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
 * Unit tests for {@link ConstrainedCsrSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Jan 15, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestConstrainedCsrSpmvParser {

    @Test
    public void testConstrainedParse() throws IOException {
        fail("Not Implemented");

        // Induce a grammar from the sample tree and construct a basic constraining chart
        final StringCountGrammar sg = new StringCountGrammar(new StringReader(AllEllaTests.STRING_SAMPLE_TREE), null,
                null, 1);
        final ProductionListGrammar pg = new ProductionListGrammar(sg);
        final SparseMatrixGrammar unsplitGrammar = new CsrSparseMatrixGrammar(pg.binaryProductions,
                pg.unaryProductions, pg.lexicalProductions, pg.vocabulary, pg.lexicon, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);
        final ConstrainedChart constrainingChart = new ConstrainedChart(BinaryTree.read(
                AllEllaTests.STRING_SAMPLE_TREE, String.class), unsplitGrammar);

        // Split the grammar
        final ProductionListGrammar pg2 = pg.split(null, 0);
        final SparseMatrixGrammar splitGrammar = new CsrSparseMatrixGrammar(pg2.binaryProductions,
                pg2.unaryProductions, pg2.lexicalProductions, pg2.vocabulary, pg2.lexicon, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashFilterFunction.class);

        // Create a chart based on the new grammar, constrained by the first chart
        final ConstrainedChart constrainedChart = null;

        // Parse with the split grammar

        // Verify expected probabilities in a few cells
        final SymbolSet<String> vocabulary = pg2.vocabulary;
        final int s = vocabulary.getIndex("s");
        final int a_0 = vocabulary.getIndex("a_0");
        final int a_1 = vocabulary.getIndex("a_1");
        final int b_0 = vocabulary.getIndex("b_0");
        final int b_1 = vocabulary.getIndex("b_1");

        assertEquals(0, constrainedChart.getInside(0, 5, s), .001f);
        assertEquals(Math.log(.5), constrainedChart.getInside(0, 5, a_0), .001f);
        assertEquals(Math.log(.5), constrainedChart.getInside(0, 5, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, constrainedChart.getInside(0, 4, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, constrainedChart.getInside(0, 4, b_1), .001f);

        assertEquals(Math.log(.5), constrainedChart.getInside(0, 2, a_0), .001f);
        assertEquals(Math.log(.5), constrainedChart.getInside(0, 2, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, constrainedChart.getInside(0, 2, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, constrainedChart.getInside(0, 2, b_1), .001f);

        assertEquals(Math.log(.5), constrainedChart.getInside(3, 5, b_0), .001f);
        assertEquals(Math.log(.5), constrainedChart.getInside(3, 5, b_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, constrainedChart.getInside(3, 5, a_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, constrainedChart.getInside(3, 5, a_1), .001f);

        // And ensure that the extracted parse matches the input gold tree
        // TODO Unfactor the extracted tree to remove latent annotations
        assertEquals(AllEllaTests.STRING_SAMPLE_TREE, constrainedChart.extractBestParse(vocabulary.getIndex("s"))
                .toString());

    }
}
