package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import edu.ohsu.cslu.ella.AllEllaTests;
import edu.ohsu.cslu.ella.ProductionListGrammar;
import edu.ohsu.cslu.ella.StringCountGrammar;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;

public class TestCsrSparseMatrixGrammar extends SortedGrammarTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return CsrSparseMatrixGrammar.class;
    }

    @Test
    public void testCartesianProductFunction() throws IOException {
        // Induce a grammar from a sample tree
        final StringCountGrammar sg = new StringCountGrammar(new StringReader(AllEllaTests.STRING_SAMPLE_TREE), null,
                null, 1);
        final ProductionListGrammar plGrammar0 = new ProductionListGrammar(sg);

        // Split the grammar
        final ProductionListGrammar plGrammar1 = plGrammar0.split(new ProductionListGrammar.BiasedNoiseGenerator(0f));
        final CsrSparseMatrixGrammar csrGrammar1 = new CsrSparseMatrixGrammar(plGrammar1.binaryProductions,
                plGrammar1.unaryProductions, plGrammar1.lexicalProductions, plGrammar1.vocabulary, plGrammar1.lexicon,
                GrammarFormatType.Berkeley, SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);

        final PackingFunction f = csrGrammar1.packingFunction;

        assertEquals(1, f.unpackLeftChild(f.pack((short) 1, (short) 4)));
        assertEquals(4, f.unpackRightChild(f.pack((short) 1, (short) 4)));
        assertEquals(2, f.unpackLeftChild(f.pack((short) 2, (short) 4)));
        assertEquals(4, f.unpackRightChild(f.pack((short) 2, (short) 4)));

        assertEquals(4, f.unpackLeftChild(f.packUnary((short) 4)));
        assertEquals(Production.UNARY_PRODUCTION, f.unpackRightChild(f.packUnary((short) 4)));

        assertEquals(9, f.unpackLeftChild(f.packLexical(9)));
        assertEquals(Production.LEXICAL_PRODUCTION, f.unpackRightChild(f.packLexical(9)));

        assertEquals(0, f.unpackLeftChild(f.packLexical(0)));
        assertEquals(Production.LEXICAL_PRODUCTION, f.unpackRightChild(f.packLexical(0)));
    }

}
