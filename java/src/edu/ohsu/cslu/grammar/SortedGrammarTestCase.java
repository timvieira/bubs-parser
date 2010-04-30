package edu.ohsu.cslu.grammar;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.tests.SharedNlpTests;

@RunWith(Theories.class)
public abstract class SortedGrammarTestCase extends GrammarTestCase {

    @DataPoint
    public final static Class<? extends CartesianProductFunction> UNFILTERED = SparseMatrixGrammar.UnfilteredFunction.class;
    @DataPoint
    public final static Class<? extends CartesianProductFunction> DEFAULT = SparseMatrixGrammar.DefaultFunction.class;
//    @DataPoint
//    public final static Class<? extends CartesianProductFunction> RIGHT_SHIFT = SparseMatrixGrammar.RightShiftFunction.class;
    @DataPoint
    public final static Class<? extends CartesianProductFunction> POS_AND_FACTORED = SparseMatrixGrammar.PosFactoredFilterFunction.class;
    @DataPoint
    public final static Class<? extends CartesianProductFunction> BIT_VECTOR_EXACT = SparseMatrixGrammar.BitVectorExactFilterFunction.class;

    // TODO Move this into its own class (SparseMatrixGrammarTestCase?)
    @Theory
    public void testPack(final Class<? extends CartesianProductFunction> cartesianProductFunctionClass)
            throws Exception {
        final SparseMatrixGrammar g = (SparseMatrixGrammar) createSimpleGrammar(grammarClass(),
            cartesianProductFunctionClass);
        final CartesianProductFunction f = g.cartesianProductFunction;
        assertEquals(4, f.unpackLeftChild(f.pack(4, (short) 2)));
        assertEquals(2, f.unpackRightChild(f.pack(4, (short) 2)));

        assertEquals(2, f.unpackLeftChild(f.pack(2, (short) 2)));
        assertEquals(2, f.unpackRightChild(f.pack(2, (short) 2)));

        assertEquals(4, f.unpackLeftChild(f.pack(4, (short) -1)));
        assertEquals(-1, f.unpackRightChild(f.pack(4, (short) -1)));

        assertEquals(9, f.unpackLeftChild(f.pack(9, (short) -2)));
        assertEquals(-2, f.unpackRightChild(f.pack(9, (short) -2)));

        assertEquals(0, f.unpackLeftChild(f.pack(0, (short) -2)));
        assertEquals(-2, f.unpackRightChild(f.pack(0, (short) -2)));

        assertEquals(0, f.unpackLeftChild(f.pack(0, (short) 0)));
        assertEquals(0, f.unpackRightChild(f.pack(0, (short) 0)));

        assertEquals(0, f.unpackLeftChild(f.pack(0, (short) 2)));
        assertEquals(2, f.unpackRightChild(f.pack(0, (short) 2)));

        assertEquals(2, f.unpackLeftChild(f.pack(2, (short) 0)));
        assertEquals(0, f.unpackRightChild(f.pack(2, (short) 0)));

        // And a couple tests with a larger grammar
        final SparseMatrixGrammar g2 = (SparseMatrixGrammar) createGrammar(grammarClass(), SharedNlpTests
            .unitTestDataAsReader("grammars/f2-21-R2-p1-unk.pcfg.gz"), SharedNlpTests
            .unitTestDataAsReader("grammars/f2-21-R2-p1-unk.lex.gz"));
        final CartesianProductFunction f2 = g2.cartesianProductFunction;
        assertEquals(2, f2.unpackLeftChild(f2.pack(2, (short) 0)));
        assertEquals(0, f2.unpackRightChild(f2.pack(2, (short) 0)));

        assertEquals(5000, f2.unpackLeftChild(f2.pack(5000, (short) 0)));
        assertEquals(0, f2.unpackRightChild(f2.pack(5000, (short) 0)));

        assertEquals(5000, f2.unpackLeftChild(f2.pack(5000, (short) 250)));
        assertEquals(250, f2.unpackRightChild(f2.pack(5000, (short) 250)));

        assertEquals(5000, f2.unpackLeftChild(f2.pack(5000, (short) -1)));
        assertEquals(-1, f2.unpackRightChild(f2.pack(5000, (short) -1)));

        assertEquals(5000, f2.unpackLeftChild(f2.pack(5000, (short) -2)));
        assertEquals(-2, f2.unpackRightChild(f2.pack(5000, (short) -2)));
    }

    /**
     * Tests a _very_ simple grammar.
     * 
     * @throws Exception if something bad happens
     */
    @Theory
    public void testSimpleGrammar(
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws Exception {

        final SortedGrammar simpleGrammar = (SortedGrammar) createSimpleGrammar(grammarClass(),
            cartesianProductFunctionClass);

        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "systems"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "analyst"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "arbitration"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "chef"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.lexicalLogProbability("NP", "foo"), .01f);

        assertEquals(5, simpleGrammar.numBinaryRules());
        assertEquals(1, simpleGrammar.numUnaryRules());
        assertEquals(5, simpleGrammar.numLexProds());
        assertEquals(5, simpleGrammar.numNonTerms());
        assertEquals(2, simpleGrammar.numPosSymbols());
        assertEquals(5, simpleGrammar.numLexSymbols());
        assertEquals("TOP", simpleGrammar.startSymbol());
        assertEquals("<null>", Grammar.nullSymbolStr);

        assertEquals(0, simpleGrammar.rightChildOnlyStart);
        assertEquals(1, simpleGrammar.normalPosStart);
        assertEquals(2, simpleGrammar.maxPOSIndex);
        assertEquals(3, simpleGrammar.eitherChildStart);
        assertEquals(4, simpleGrammar.leftChildOnlyStart);
        assertEquals(4, simpleGrammar.unaryChildOnlyStart);

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
        testSimpleGrammar(DEFAULT);
    }

    @Test
    public void testF2_21_R2_unk() throws Exception {
        final SortedGrammar g = (SortedGrammar) createGrammar(grammarClass(), SharedNlpTests
            .unitTestDataAsReader("grammars/f2-21-R2-unk.pcfg.gz"), SharedNlpTests
            .unitTestDataAsReader("grammars/f2-21-R2-unk.lex.gz"));
        assertEquals(11793, g.numBinaryRules());
        assertEquals(242, g.numUnaryRules());
        assertEquals(52000, g.numLexProds());
        assertEquals(2657, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", Grammar.nullSymbolStr);

        assertEquals("Ranger", g.mapLexicalEntry(40000));
        assertEquals(-12.116870f, g.lexicalLogProbability("NNP", "Ranger"), 0.01f);

        assertEquals(0, g.rightChildOnlyStart);
        assertEquals(1, g.posNonFactoredStart);
        assertEquals(8, g.normalPosStart);
        assertEquals(46, g.maxPOSIndex());
        assertEquals(47, g.eitherChildStart);
        assertEquals(71, g.leftChildOnlyStart);
        assertEquals(2656, g.unaryChildOnlyStart);

    }

    @Test
    public void testF2_21_R2_p1_unk() throws Exception {
        final SparseMatrixGrammar g = (SparseMatrixGrammar) createGrammar(grammarClass(), SharedNlpTests
            .unitTestDataAsReader("grammars/f2-21-R2-p1-unk.pcfg.gz"), SharedNlpTests
            .unitTestDataAsReader("grammars/f2-21-R2-p1-unk.lex.gz"));
        assertEquals(22299, g.numBinaryRules());
        assertEquals(745, g.numUnaryRules());
        assertEquals(52000, g.numLexProds());
        assertEquals(6083, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", Grammar.nullSymbolStr);

        assertEquals("3,200", g.mapLexicalEntry(40000));

        assertEquals(0, g.rightChildOnlyStart);
        assertEquals(103, g.posNonFactoredStart);
        assertEquals(110, g.normalPosStart);
        assertEquals(148, g.maxPOSIndex());
        assertEquals(149, g.eitherChildStart);
        assertEquals(286, g.leftChildOnlyStart);
        assertEquals(6060, g.unaryChildOnlyStart);

        assertEquals(193, g.cartesianProductFunction.unpackLeftChild(g.cartesianProductFunction.pack(193,
            (short) 266)));
        assertEquals(266, g.cartesianProductFunction.unpackRightChild(g.cartesianProductFunction.pack(266,
            (short) 266)));
    }
}
