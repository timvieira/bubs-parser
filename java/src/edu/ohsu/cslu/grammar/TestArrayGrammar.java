package edu.ohsu.cslu.grammar;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;
import static org.junit.Assert.assertEquals;

public class TestArrayGrammar {
    @Test
    public void testF2_21_R2_unk() throws Exception {
        final Grammar g = new Grammar(SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-unk.gz"));
        assertEquals(11793, g.binaryProductions.size());
        assertEquals(242, g.unaryProductions.size());
        assertEquals(52000, g.numLexProds());
        assertEquals(2657, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", Grammar.nullSymbolStr);
        assertEquals(1697, g.maxPOSIndex());
    }
}
