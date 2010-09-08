package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import static org.junit.Assert.assertEquals;

public abstract class GrammarTestCase {

    /**
     * @return the grammar class under test
     */
    protected abstract Class<? extends Grammar> grammarClass();

    public static <C extends Grammar> C createGrammar(final Class<C> grammarClass, final Reader grammarReader)
            throws Exception {
        return grammarClass.getConstructor(new Class[] { Reader.class, Reader.class, GrammarFormatType.class })
                .newInstance(new Object[] { grammarReader, GrammarFormatType.CSLU });
    }

    public static <C extends Grammar> C createGrammar(final Class<C> grammarClass, final Reader grammarReader,
            final Class<? extends SparseMatrixGrammar.CartesianProductFunction> cartesianProductFunctionClass)
            throws Exception {

        try {
            return grammarClass.getConstructor(new Class[] { Reader.class, Class.class }).newInstance(
                    new Object[] { grammarReader, cartesianProductFunctionClass });
        } catch (final NoSuchMethodException e) {
            return grammarClass.getConstructor(new Class[] { Reader.class })
                    .newInstance(new Object[] { grammarReader });
        }
    }

    public static Grammar createSimpleGrammar(final Class<? extends Grammar> grammarClass,
            final Class<? extends CartesianProductFunction> cartesionProductFunctionClass) throws Exception {
        final StringBuilder sb = new StringBuilder(256);

        sb.append("TOP\n");
        sb.append("TOP => NP 0\n");
        sb.append("NP => NN NN -0.693147\n");
        sb.append("NP => NP NN -1.203972\n");
        sb.append("NP => NN NP -2.302585\n");
        sb.append("NP => NP NP -2.302585\n");
        // Add a fake factored category just to keep Grammar happy
        sb.append("NP => NP|NN NP -Infinity\n");

        sb.append(Grammar.DELIMITER);
        sb.append('\n');

        sb.append("NN => systems 0\n");
        sb.append("NN => analyst 0\n");
        sb.append("NN => arbitration 0\n");
        sb.append("NN => chef 0\n");
        sb.append("NN => UNK 0\n");

        return createGrammar(grammarClass, new StringReader(sb.toString()), cartesionProductFunctionClass);
    }

    public static Grammar createSimpleGrammar(final Class<? extends Grammar> grammarClass) throws Exception {
        return createSimpleGrammar(grammarClass, null);
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
