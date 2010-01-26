package edu.ohsu.cslu.grammar;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;
import static org.junit.Assert.assertEquals;

public abstract class SortedGrammarTestCase extends GrammarTestCase {

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

        final BaseSortedGrammar simpleGrammar = (BaseSortedGrammar) createSimpleGrammar(grammarClass());

        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "systems"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "analyst"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "arbitration"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "chef"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.lexicalLogProbability("NP", "foo"), .01f);

        assertEquals(-0.693147f, simpleGrammar.logProbability("NP", "NN", "NN"), .01f);
        assertEquals(-1.203972f, simpleGrammar.logProbability("NP", "NP", "NN"), .01f);
        assertEquals(-2.302585f, simpleGrammar.logProbability("NP", "NN", "NP"), .01f);
        assertEquals(-2.302585f, simpleGrammar.logProbability("NP", "NP", "NP"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.logProbability("TOP", "NP", "NP"), .01f);
        assertEquals(0f, simpleGrammar.logProbability("TOP", "NP"), .01f);

        assertEquals(4, simpleGrammar.numBinaryRules());
        assertEquals(1, simpleGrammar.numUnaryRules());
        assertEquals(5, simpleGrammar.numLexProds);
        assertEquals(4, simpleGrammar.numNonTerms());
        assertEquals(2, simpleGrammar.numPosSymbols());
        assertEquals(5, simpleGrammar.numLexSymbols());
        assertEquals("TOP", simpleGrammar.startSymbol());
        assertEquals("<null>", simpleGrammar.nullSymbolStr);

        assertEquals(0, simpleGrammar.rightChildOnlyStart);
        assertEquals(0, simpleGrammar.eitherChildStart);
        assertEquals(1, simpleGrammar.leftChildOnlyStart);
        assertEquals(1, simpleGrammar.posStart);
        assertEquals(3, simpleGrammar.unaryChildOnlyStart);

        assertEquals(2, simpleGrammar.maxPOSIndex());
    }

    @Test
    public void testF2_21_R2_unk() throws Exception {
        final BaseSortedGrammar g = (BaseSortedGrammar) createGrammar(grammarClass(), SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-unk.pcfg.gz"), SharedNlpTests
                .unitTestDataAsReader("grammars/f2-21-R2-unk.lex.gz"));
        assertEquals(11793, g.numBinaryRules());
        assertEquals(242, g.numUnaryRules());
        assertEquals(52000, g.numLexProds);
        assertEquals(2657, g.numNonTerms());
        assertEquals(46, g.numPosSymbols());
        assertEquals(44484, g.numLexSymbols());
        assertEquals("TOP", g.startSymbol());
        assertEquals("<null>", g.nullSymbolStr);

        assertEquals("Ranger", g.mapLexicalEntry(40000));
        assertEquals(-12.116870f, g.lexicalLogProbability("NNP", "Ranger"), 0.01f);

        assertEquals(0, g.rightChildOnlyStart);
        assertEquals(1, g.eitherChildStart);
        assertEquals(25, g.leftChildOnlyStart);
        assertEquals(2610, g.posStart);
        assertEquals(2656, g.unaryChildOnlyStart);

        assertEquals(2655, g.maxPOSIndex());
    }

    @Test
    public void testF2_21_R2_p1_unk() throws Exception {
        final BaseSortedGrammar g = (BaseSortedGrammar) createGrammar(grammarClass(), SharedNlpTests.unitTestDataAsReader("grammars/f2-21-R2-p1-unk.pcfg.gz"), SharedNlpTests
                .unitTestDataAsReader("grammars/f2-21-R2-p1-unk.lex.gz"));
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
        assertEquals(103, g.eitherChildStart);
        assertEquals(240, g.leftChildOnlyStart);
        assertEquals(6014, g.posStart);
        assertEquals(6060, g.unaryChildOnlyStart);

        assertEquals(6059, g.maxPOSIndex());
    }
}
