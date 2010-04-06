package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

public abstract class GrammarTestCase {

    /**
     * @return the grammar class under test
     */
    protected abstract Class<? extends Grammar> grammarClass();

    public static <C extends Grammar> C createGrammar(final Class<C> grammarClass,
            final Reader grammarReader, final Reader lexiconReader) throws Exception {
        return grammarClass.getConstructor(
            new Class[] { Reader.class, Reader.class, GrammarFormatType.class }).newInstance(
            new Object[] { grammarReader, lexiconReader, GrammarFormatType.CSLU });
    }

    public static Grammar createSimpleGrammar(final Class<? extends Grammar> grammarClass) throws Exception {
        final StringBuilder lexiconSb = new StringBuilder(256);
        lexiconSb.append("NN => systems 0\n");
        lexiconSb.append("NN => analyst 0\n");
        lexiconSb.append("NN => arbitration 0\n");
        lexiconSb.append("NN => chef 0\n");
        lexiconSb.append("NN => UNK 0\n");

        final StringBuilder grammarSb = new StringBuilder(256);
        grammarSb.append("TOP\n");
        grammarSb.append("TOP => NP 0\n");
        grammarSb.append("NP => NN NN -0.693147\n");
        grammarSb.append("NP => NP NN -1.203972\n");
        grammarSb.append("NP => NN NP -2.302585\n");
        grammarSb.append("NP => NP NP -2.302585\n");
        // Add a fake factored category just to keep Grammar happy
        grammarSb.append("NP => NP NP|NN -Infinity\n");

        return createGrammar(grammarClass, new StringReader(grammarSb.toString()), new StringReader(lexiconSb
            .toString()));
    }

    /**
     * Tests a _very_ simple grammar.
     * 
     * TODO Share grammar creation with GrammarTestCase
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSimpleGrammar() throws Exception {

        final Grammar simpleGrammar = createSimpleGrammar(grammarClass());

        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "systems"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "analyst"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "arbitration"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "chef"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.lexicalLogProbability("NP", "foo"), .01f);

        assertEquals(-0.693147f, simpleGrammar.binaryLogProbability("NP", "NN", "NN"), .01f);
        assertEquals(-1.203972f, simpleGrammar.binaryLogProbability("NP", "NP", "NN"), .01f);
        assertEquals(-2.302585f, simpleGrammar.binaryLogProbability("NP", "NN", "NP"), .01f);
        assertEquals(-2.302585f, simpleGrammar.binaryLogProbability("NP", "NP", "NP"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.binaryLogProbability("TOP", "NP", "NP"), .01f);
        assertEquals(0f, simpleGrammar.unaryLogProbability("TOP", "NP"), .01f);
    }
}
