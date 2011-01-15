package edu.ohsu.cslu.ella;

import org.junit.Before;

import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Unit tests for {@link MappedCountGrammar}.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestMappedCountGrammar extends CountGrammarTestCase {

    @Before
    public void setUp() {
        g = SAMPLE_MAPPED_GRAMMAR();
    }

    static MappedCountGrammar SAMPLE_MAPPED_GRAMMAR() {
        final SymbolSet<String> vocabulary = new SymbolSet<String>(new String[] { "s", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });

        // Build up the same grammar as that induced from the tree in AllElviTests
        final MappedCountGrammar g = new MappedCountGrammar(vocabulary, lexicon);
        g.incrementUnaryCount("s", "a");
        g.incrementBinaryCount("a", "a", "b");
        g.incrementBinaryCount("a", "a", "b");
        g.incrementBinaryCount("a", "a", "a");
        g.incrementLexicalCount("a", "c");
        g.incrementLexicalCount("a", "c");
        g.incrementLexicalCount("b", "d");
        g.incrementBinaryCount("b", "b", "a");
        g.incrementUnaryCount("b", "b");
        g.incrementLexicalCount("b", "d");
        g.incrementLexicalCount("a", "d");

        return g;
    }
}
