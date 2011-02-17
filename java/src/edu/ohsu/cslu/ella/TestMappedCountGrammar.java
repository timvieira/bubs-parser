package edu.ohsu.cslu.ella;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.tests.Assert;

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
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });

        // Build up the same grammar as that induced from the tree in AllElviTests
        final MappedCountGrammar g = new MappedCountGrammar(vocabulary, lexicon);
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

    @Test
    public void testFractionalCounts() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });
        final MappedCountGrammar mcg = new MappedCountGrammar(vocabulary, lexicon);

        // top -> a 1
        mcg.incrementUnaryCount("top", "a", 1);

        // a -> a b 5/12
        // a -> a a 3/12
        // a -> c 3/12
        // a -> d 1/12
        mcg.incrementBinaryCount("a", "a", "b", 1.5f);
        mcg.incrementBinaryCount("a", "a", "b", 1.0f);
        mcg.incrementBinaryCount("a", "a", "a", 1.5f);
        mcg.incrementLexicalCount("a", "c", .5f);
        mcg.incrementLexicalCount("a", "c", 1.0f);
        mcg.incrementLexicalCount("a", "d", .5f);

        // b -> b a 7/16
        // b -> b 3/16
        // b -> c 5/16
        // b -> d 1/16
        mcg.incrementBinaryCount("b", "b", "a", 3.5f);
        mcg.incrementUnaryCount("b", "b", 1.5f);
        mcg.incrementLexicalCount("b", "c", 2.5f);
        mcg.incrementLexicalCount("b", "d", .5f);

        final ProductionListGrammar plg = new ProductionListGrammar(mcg);
        Assert.assertLogFractionEquals(0, plg.unaryLogProbability("top", "a"), 0.01f);

        Assert.assertLogFractionEquals(Math.log(5.0 / 12), plg.binaryLogProbability("a", "a", "b"), 0.01f);
        Assert.assertLogFractionEquals(Math.log(3.0 / 12), plg.binaryLogProbability("a", "a", "a"), 0.01f);
        Assert.assertLogFractionEquals(Math.log(3.0 / 12), plg.lexicalLogProbability("a", "c"), 0.01f);
        Assert.assertLogFractionEquals(Math.log(1.0 / 12), plg.lexicalLogProbability("a", "d"), 0.01f);

        Assert.assertLogFractionEquals(Math.log(7.0 / 16), plg.binaryLogProbability("b", "b", "a"), 0.01f);
        Assert.assertLogFractionEquals(Math.log(3.0 / 16), plg.unaryLogProbability("b", "b"), 0.01f);
        Assert.assertLogFractionEquals(Math.log(5.0 / 16), plg.lexicalLogProbability("b", "c"), 0.01f);
        Assert.assertLogFractionEquals(Math.log(1.0 / 16), plg.lexicalLogProbability("b", "d"), 0.01f);
    }
}
