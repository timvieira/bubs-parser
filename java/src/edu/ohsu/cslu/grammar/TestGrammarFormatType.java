package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link GrammarFormatType}
 * 
 * @author Aaron Dunlop
 */
public class TestGrammarFormatType {
    @Test
    public void testGetBaseNT() {
        assertEquals("@NP", GrammarFormatType.Berkeley.getBaseNT("@NP_2", false));
        assertEquals("NP", GrammarFormatType.Berkeley.getBaseNT("@NP_2", true));

        // Factored but not parent-annotated
        assertEquals("NP|", GrammarFormatType.CSLU.getBaseNT("NP|<S>", false));
        assertEquals("NP", GrammarFormatType.CSLU.getBaseNT("NP|<S>", true));
        assertEquals("NP|", GrammarFormatType.CSLU.getBaseNT("NP|<S-NN>", false));
        assertEquals("NP", GrammarFormatType.CSLU.getBaseNT("NP|<S-NN>", true));

        // Factored and parent-annotated
        assertEquals("NP|", GrammarFormatType.CSLU.getBaseNT("NP^ADJP|<S-NN>", false));
        assertEquals("NP", GrammarFormatType.CSLU.getBaseNT("NP^ADJP|<S-NN>", true));
    }
}
