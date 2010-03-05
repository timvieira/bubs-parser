package edu.ohsu.cslu.grammar;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;

public abstract class SortedGrammarTestCase extends GrammarTestCase {

    @Test
    public void testPack() throws Exception {
        final SparseMatrixGrammar g = (SparseMatrixGrammar) createSimpleGrammar(grammarClass());
        assertEquals(10, g.unpackLeftChild(g.pack(10, (short) 2)));
        assertEquals(2, g.unpackRightChild(g.pack(10, (short) 2)));

        assertEquals(1000, g.unpackLeftChild(g.pack(1000, (short) 2)));
        assertEquals(2, g.unpackRightChild(g.pack(1000, (short) 2)));

        assertEquals(1000, g.unpackLeftChild(g.pack(1000, (short) -1)));
        assertEquals(-1, g.unpackRightChild(g.pack(1000, (short) -1)));

        assertEquals(10, g.unpackLeftChild(g.pack(10, (short) -2)));
        assertEquals(-2, g.unpackRightChild(g.pack(10, (short) -2)));

        assertEquals(0, g.unpackLeftChild(g.pack(0, (short) -2)));
        assertEquals(-2, g.unpackRightChild(g.pack(0, (short) -2)));

        assertEquals(0, g.unpackLeftChild(g.pack(0, (short) 0)));
        assertEquals(0, g.unpackRightChild(g.pack(0, (short) 0)));

        assertEquals(0, g.unpackLeftChild(g.pack(0, (short) 2)));
        assertEquals(2, g.unpackRightChild(g.pack(0, (short) 2)));

        assertEquals(2, g.unpackLeftChild(g.pack(2, (short) 0)));
        assertEquals(0, g.unpackRightChild(g.pack(2, (short) 0)));
    }

    /**
     * Tests a _very_ simple grammar.
     * 
     * TODO Share grammar creation with GrammarTestCase
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testSimpleGrammar() throws Exception {

        final SortedGrammar simpleGrammar = (SortedGrammar) createSimpleGrammar(grammarClass());

        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "systems"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "analyst"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "arbitration"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "chef"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.lexicalLogProbability("NP", "foo"), .01f);

        assertEquals(4, simpleGrammar.numBinaryRules());
        assertEquals(1, simpleGrammar.numUnaryRules());
        assertEquals(5, simpleGrammar.numLexProds());
        assertEquals(4, simpleGrammar.numNonTerms());
        assertEquals(2, simpleGrammar.numPosSymbols());
        assertEquals(5, simpleGrammar.numLexSymbols());
        assertEquals("TOP", simpleGrammar.startSymbol());
        assertEquals("<null>", simpleGrammar.nullSymbolStr);

        assertEquals(0, simpleGrammar.rightChildOnlyStart);
        assertEquals(0, simpleGrammar.posStart);
        assertEquals(1, simpleGrammar.maxPOSIndex);
        assertEquals(2, simpleGrammar.eitherChildStart);
        assertEquals(3, simpleGrammar.leftChildOnlyStart);
        assertEquals(3, simpleGrammar.unaryChildOnlyStart);

        assertEquals(-0.693147f, simpleGrammar.logProbability("NP", "NN", "NN"), .01f);
        assertEquals(-1.203972f, simpleGrammar.logProbability("NP", "NP", "NN"), .01f);
        assertEquals(-2.302585f, simpleGrammar.logProbability("NP", "NN", "NP"), .01f);
        assertEquals(-2.302585f, simpleGrammar.logProbability("NP", "NP", "NP"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.logProbability("TOP", "NP", "NP"), .01f);
        assertEquals(0f, simpleGrammar.logProbability("TOP", "NP"), .01f);
    }

    @Test
    public void testF2_21_R2_unk() throws Exception {
        final SortedGrammar g = (SortedGrammar) createGrammar(grammarClass(), SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-unk.pcfg.gz"), SharedNlpTests
                .unitTestDataAsReader("grammars/f2-21-R2-unk.lex.gz"));
        assertEquals(11793, g.numBinaryRules());
        assertEquals(242, g.numUnaryRules());
        assertEquals(52000, g.numLexProds());
        assertEquals(2657, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", g.nullSymbolStr);

        assertEquals("Ranger", g.mapLexicalEntry(40000));
        assertEquals(-12.116870f, g.lexicalLogProbability("NNP", "Ranger"), 0.01f);

        assertEquals(0, g.rightChildOnlyStart);
        assertEquals(1, g.posStart);
        assertEquals(46, g.maxPOSIndex());
        assertEquals(47, g.eitherChildStart);
        assertEquals(71, g.leftChildOnlyStart);
        assertEquals(2656, g.unaryChildOnlyStart);

    }

    @Test
    public void testF2_21_R2_p1_unk() throws Exception {
        final SparseMatrixGrammar g = (SparseMatrixGrammar) createGrammar(grammarClass(), SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-p1-unk.pcfg.gz"), SharedNlpTests
                .unitTestDataAsReader("grammars/f2-21-R2-p1-unk.lex.gz"));
        assertEquals(22299, g.numBinaryRules());
        assertEquals(745, g.numUnaryRules());
        assertEquals(52000, g.numLexProds());
        assertEquals(6083, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", g.nullSymbolStr);

        assertEquals("3,200", g.mapLexicalEntry(40000));

        assertEquals(0, g.rightChildOnlyStart);
        assertEquals(103, g.posStart);
        assertEquals(148, g.maxPOSIndex());
        assertEquals(149, g.eitherChildStart);
        assertEquals(286, g.leftChildOnlyStart);
        assertEquals(6060, g.unaryChildOnlyStart);

        assertEquals(193, g.unpackLeftChild(g.pack(193, (short) 266)));
        assertEquals(266, g.unpackRightChild(g.pack(266, (short) 266)));
    }
}
