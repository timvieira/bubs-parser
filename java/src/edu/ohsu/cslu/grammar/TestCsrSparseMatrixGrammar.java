package edu.ohsu.cslu.grammar;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;

public class TestCsrSparseMatrixGrammar extends SortedGrammarTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return CsrSparseMatrixGrammar.class;
    }

    @Test
    public void testPack() throws Exception {
        final CsrSparseMatrixGrammar g = (CsrSparseMatrixGrammar) createSimpleGrammar(grammarClass());
        assertEquals(10, g.unpackLeftChild(g.pack((short) 10, (short) 2)));
        assertEquals(2, g.unpackRightChild(g.pack((short) 10, (short) 2)));

        assertEquals(1000, g.unpackLeftChild(g.pack((short) 1000, (short) 2)));
        assertEquals(2, g.unpackRightChild(g.pack((short) 1000, (short) 2)));

        assertEquals(1000, g.unpackLeftChild(g.pack((short) 1000, (short) -1)));
        assertEquals(-1, g.unpackRightChild(g.pack((short) 1000, (short) -1)));

        assertEquals(10, g.unpackLeftChild(g.pack((short) 10, (short) -2)));
        assertEquals(-2, g.unpackRightChild(g.pack((short) 10, (short) -2)));

        assertEquals(0, g.unpackLeftChild(g.pack((short) 0, (short) -2)));
        assertEquals(-2, g.unpackRightChild(g.pack((short) 0, (short) -2)));

        assertEquals(0, g.unpackLeftChild(g.pack((short) 0, (short) 0)));
        assertEquals(0, g.unpackRightChild(g.pack((short) 0, (short) 0)));

        assertEquals(0, g.unpackLeftChild(g.pack((short) 0, (short) 2)));
        assertEquals(2, g.unpackRightChild(g.pack((short) 0, (short) 2)));

        assertEquals(2, g.unpackLeftChild(g.pack((short) 2, (short) 0)));
        assertEquals(0, g.unpackRightChild(g.pack((short) 2, (short) 0)));
    }

    @Override
    @Test
    public void testF2_21_R2_p1_unk() throws Exception {
    // TODO: Avoid copy-and-paste from SortedGrammarTestCase
        final CsrSparseMatrixGrammar g = (CsrSparseMatrixGrammar) createGrammar(grammarClass(), SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-p1-unk.pcfg.gz"),
                SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-p1-unk.lex.gz"));
        assertEquals(22299, g.numBinaryRules());
        assertEquals(745, g.numUnaryRules());
        assertEquals(52000, g.numLexProds);
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

        assertEquals(193, g.unpackLeftChild(g.pack((short) 193, (short) 266)));
        assertEquals(266, g.unpackRightChild(g.pack((short) 266, (short) 266)));
    }
}
