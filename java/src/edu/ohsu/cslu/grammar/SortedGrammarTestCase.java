package edu.ohsu.cslu.grammar;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.tests.SharedNlpTests;

@RunWith(Theories.class)
public abstract class SortedGrammarTestCase extends GrammarTestCase {

    @DataPoint
    public final static Class<? extends PackingFunction> SIMPLE = SparseMatrixGrammar.LeftShiftFunction.class;

    // TODO Move this into its own class (SparseMatrixGrammarTestCase?)
    @Theory
    public void testPack(final Class<? extends PackingFunction> cartesianProductFunctionClass)
            throws Exception {
        final SparseMatrixGrammar g = (SparseMatrixGrammar) createGrammar(grammarClass(),
                GrammarTestCase.simpleGrammar(), cartesianProductFunctionClass);
        final PackingFunction f = g.cartesianProductFunction;
        assertEquals(4, f.unpackLeftChild(f.pack((short) 4, (short) 3)));
        assertEquals(3, f.unpackRightChild(f.pack((short) 4, (short) 3)));

        assertEquals(4, f.unpackLeftChild(f.packUnary((short) 4)));
        assertEquals(Production.UNARY_PRODUCTION, f.unpackRightChild(f.packUnary((short) 4)));

        assertEquals(9, f.unpackLeftChild(f.packLexical(9)));
        assertEquals(Production.LEXICAL_PRODUCTION, f.unpackRightChild(f.packLexical(9)));

        assertEquals(0, f.unpackLeftChild(f.packLexical(0)));
        assertEquals(Production.LEXICAL_PRODUCTION, f.unpackRightChild(f.packLexical(0)));

        // And a couple tests with a larger grammar
        final SparseMatrixGrammar g2 = (SparseMatrixGrammar) createGrammar(grammarClass(),
                SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-p1-unk.gz"), cartesianProductFunctionClass);
        final PackingFunction f2 = g2.cartesianProductFunction;
        assertEquals(2, f2.unpackLeftChild(f2.pack((short) 2, (short) 0)));
        assertEquals(0, f2.unpackRightChild(f2.pack((short) 2, (short) 0)));

        assertEquals(5000, f2.unpackLeftChild(f2.pack((short) 5000, (short) 0)));
        assertEquals(0, f2.unpackRightChild(f2.pack((short) 5000, (short) 0)));

        assertEquals(5000, f2.unpackLeftChild(f2.pack((short) 5000, (short) 250)));
        assertEquals(250, f2.unpackRightChild(f2.pack((short) 5000, (short) 250)));

        assertEquals(5000, f2.unpackLeftChild(f2.pack((short) 5000, (short) -1)));
        assertEquals(Production.UNARY_PRODUCTION, f2.unpackRightChild(f2.packUnary((short) 5000)));

        assertEquals(5000, f2.unpackLeftChild(f2.packLexical(5000)));
        assertEquals(Production.LEXICAL_PRODUCTION, f2.unpackRightChild(f2.packLexical(5000)));
    }

    /**
     * Tests a _very_ simple grammar.
     * 
     * @throws Exception if something bad happens
     */
    @Theory
    public void testSimpleGrammar(final Class<? extends PackingFunction> cartesianProductFunctionClass)
            throws Exception {

        final SparseMatrixGrammar simpleGrammar = (SparseMatrixGrammar) createGrammar(grammarClass(),
                GrammarTestCase.simpleGrammar());

        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "systems"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "analyst"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "arbitration"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "chef"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.lexicalLogProbability("NP", "foo"), .01f);

        assertEquals(5, simpleGrammar.numBinaryProds());
        assertEquals(1, simpleGrammar.numUnaryProds());
        assertEquals(5, simpleGrammar.numLexProds());
        assertEquals(5, simpleGrammar.numNonTerms());
        assertEquals(2, simpleGrammar.numPosSymbols());
        assertEquals(5, simpleGrammar.numLexSymbols());
        assertEquals("TOP", simpleGrammar.startSymbol());
        assertEquals("<null>", Grammar.nullSymbolStr);

        assertEquals(0, simpleGrammar.leftChildrenStart);
        assertEquals(2, simpleGrammar.leftChildrenEnd);
        assertEquals(2, simpleGrammar.rightChildrenStart);
        assertEquals(2, simpleGrammar.rightChildrenEnd);
        assertEquals(3, simpleGrammar.posStart);
        assertEquals(4, simpleGrammar.posEnd);

        assertEquals(-0.693147f, simpleGrammar.binaryLogProbability("NP", "NN", "NN"), .01f);
        assertEquals(-1.203972f, simpleGrammar.binaryLogProbability("NP", "NP", "NN"), .01f);
        assertEquals(-2.302585f, simpleGrammar.binaryLogProbability("NP", "NN", "NP"), .01f);
        assertEquals(-2.302585f, simpleGrammar.binaryLogProbability("NP", "NP", "NP"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.binaryLogProbability("TOP", "NP", "NP"), .01f);
        assertEquals(0f, simpleGrammar.unaryLogProbability("TOP", "NP"), .01f);
    }

    @Override
    @Test
    public void testSimpleGrammar() throws Exception {
        // Already handled in the testSimpleGrammar Theory
    }

    @Test
    public void testF2_21_R2_unk() throws Exception {
        final Grammar g = createGrammar(grammarClass(), SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-unk.gz"));
        assertEquals(11793, g.numBinaryProds());
        assertEquals(242, g.numUnaryProds());
        assertEquals(52000, g.numLexProds());
        assertEquals(2657, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", Grammar.nullSymbolStr);

        assertEquals("Ranger", g.mapLexicalEntry(40000));
        assertEquals(-12.116870f, g.lexicalLogProbability("NNP", "Ranger"), 0.01f);

        assertEquals(0, g.leftChildrenStart);
        assertEquals(2610, g.leftChildrenEnd);
        assertEquals(2586, g.rightChildrenStart);
        assertEquals(2610, g.rightChildrenEnd);
        assertEquals(2611, g.posStart);
        assertEquals(2656, g.posEnd);
    }

    @Test
    public void testF2_21_R2_p1_unk() throws Exception {
        final SparseMatrixGrammar g = (SparseMatrixGrammar) createGrammar(grammarClass(),
                SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-p1-unk.gz"));
        assertEquals(22299, g.numBinaryProds());
        assertEquals(745, g.numUnaryProds());
        assertEquals(52000, g.numLexProds());
        assertEquals(6083, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", Grammar.nullSymbolStr);

        assertEquals("3,200", g.mapLexicalEntry(40000));

        assertEquals(0, g.leftChildrenStart);
        assertEquals(6036, g.leftChildrenEnd);
        assertEquals(5797, g.rightChildrenStart);
        assertEquals(6036, g.rightChildrenEnd);
        assertEquals(6037, g.posStart);
        assertEquals(6082, g.posEnd);

        assertEquals(1930,
                g.cartesianProductFunction.unpackLeftChild(g.cartesianProductFunction.pack((short) 1930, (short) 250)));
        assertEquals(250,
                g.cartesianProductFunction.unpackRightChild(g.cartesianProductFunction.pack((short) 1930, (short) 250)));
    }
}
