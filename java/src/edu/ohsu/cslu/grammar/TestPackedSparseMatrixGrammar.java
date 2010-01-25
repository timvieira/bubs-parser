package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;
import edu.ohsu.cslu.tests.SharedNlpTests;

public class TestPackedSparseMatrixGrammar extends GrammarTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return PackedSparseMatrixGrammar.class;
    }

    @Test
    public void testF2_21_R2_unk() throws Exception {
        final PackedSparseMatrixGrammar g = new PackedSparseMatrixGrammar(SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-unk.pcfg.gz"), SharedNlpTests
                .unitTestDataAsReader("grammars/f2-21-R2-unk.lex.gz"), GrammarFormatType.CSLU);
        assertEquals(11793, g.numBinaryRules());
        assertEquals(242, g.numUnaryRules());
        assertEquals(52000, g.numLexProds);
        assertEquals(2657, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", g.nullSymbolStr);

        assertEquals(0, g.rightChildOnlyStart);
        assertEquals(1, g.eitherChildStart);
        assertEquals(25, g.leftChildOnlyStart);
        assertEquals(2610, g.posStart);
        assertEquals(2656, g.unaryChildOnlyStart);

        assertEquals(2656, g.maxPOSIndex());
    }

    @Test
    public void testF2_21_R2_p1_unk() throws Exception {
        final PackedSparseMatrixGrammar g = new PackedSparseMatrixGrammar(SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-p1-unk.pcfg.gz"), SharedNlpTests
                .unitTestDataAsReader("grammars/f2-21-R2-p1-unk.lex.gz"), GrammarFormatType.CSLU);
        assertEquals(22299, g.numBinaryRules());
        assertEquals(745, g.numUnaryRules());
        assertEquals(52000, g.numLexProds);
        assertEquals(6083, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", g.nullSymbolStr);

        assertEquals(0, g.rightChildOnlyStart);
        assertEquals(103, g.eitherChildStart);
        assertEquals(240, g.leftChildOnlyStart);
        assertEquals(6014, g.posStart);
        assertEquals(6060, g.unaryChildOnlyStart);

        assertEquals(6060, g.maxPOSIndex());
    }
}
