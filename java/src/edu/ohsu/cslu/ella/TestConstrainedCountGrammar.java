package edu.ohsu.cslu.ella;

import org.junit.Before;

import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Unit tests for {@link ConstrainedCountGrammar}.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestConstrainedCountGrammar extends CountGrammarTestCase {

    @Before
    public void setUp() {
        g = SAMPLE_MAPPED_GRAMMAR();
    }

    static ConstrainedCountGrammar SAMPLE_MAPPED_GRAMMAR() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });

        // Build up the same grammar as that induced from the tree in AllElviTests
        final ConstrainedCountGrammar g = new ConstrainedCountGrammar(vocabulary, lexicon);
        g.incrementUnaryCount("top", "a");
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
