package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;
import edu.ohsu.cslu.tests.SharedNlpTests;

public class TestArrayGrammar {
    @Test
    public void testF2_21_R2_unk() throws Exception {
        final ArrayGrammar g = new ArrayGrammar(SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-unk.pcfg.gz"), SharedNlpTests
                .unitTestDataAsReader("grammars/f2-21-R2-unk.lex.gz"), GrammarFormatType.CSLU);
        assertEquals(11793, g.binaryProds.length);
        assertEquals(242, g.unaryProds.length);
        assertEquals(52000, g.numLexProds);
        assertEquals(2657, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", g.nullSymbolStr);
        assertEquals(1697, g.maxPOSIndex());
    }
}
