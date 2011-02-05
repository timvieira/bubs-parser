package edu.ohsu.cslu.ella;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link ProductionListGrammar}.
 * 
 * @author Aaron Dunlop
 * @since Jan 12, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(Theories.class)
public class TestProductionListGrammar {

    @DataPoints
    public static ProductionListGrammar[] dataPoints() throws IOException {
        final ProductionListGrammar g1 = new ProductionListGrammar(new StringCountGrammar(new StringReader(
                AllEllaTests.STRING_SAMPLE_TREE), null, null, 1));
        final ProductionListGrammar g2 = new ProductionListGrammar(TestMappedCountGrammar.SAMPLE_MAPPED_GRAMMAR());
        return new ProductionListGrammar[] { g1, g2 };
    }

    @Theory
    public void testLexicalLogProbability(final ProductionListGrammar g) {
        assertEquals(-1.098612, g.lexicalLogProbability("a", "c"), .0001f);
        assertEquals(-1.791759, g.lexicalLogProbability("a", "d"), .0001f);
        assertEquals(-0.693147, g.lexicalLogProbability("b", "d"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.lexicalLogProbability("b", "c"), .0001f);

    }

    @Theory
    public void testUnaryLogProbability(final ProductionListGrammar g) {
        assertEquals(-1.386294, g.unaryLogProbability("b", "b"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.unaryLogProbability("a", "b"), .0001f);
    }

    @Theory
    public void testBinaryLogProbability(final ProductionListGrammar g) {
        assertEquals(-1.098612, g.binaryLogProbability("a", "a", "b"), .0001f);
        assertEquals(-1.791759, g.binaryLogProbability("a", "a", "a"), .0001f);
        assertEquals(-1.386294, g.binaryLogProbability("b", "b", "a"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.binaryLogProbability("b", "a", "a"), .0001f);
    }

    @Theory
    public void testPcfgString(final ProductionListGrammar g) {
        final StringBuilder pcfg = new StringBuilder(1024);
        // Start Symbol
        pcfg.append("top\n");
        // Binary Rules
        pcfg.append("a -> a a -1.791759\n");
        pcfg.append("a -> a b -1.098612\n");
        pcfg.append("b -> b a -1.386294\n");
        // Unary Rules
        pcfg.append("top -> a 0.000000\n");
        pcfg.append("b -> b -1.386294\n");
        assertEquals(pcfg.toString(), g.pcfgString());
    }

    @Theory
    public void testLexiconString(final ProductionListGrammar g) {
        final StringBuilder lexicon = new StringBuilder(512);
        lexicon.append("a -> c -1.098612\n");
        lexicon.append("a -> d -1.791759\n");
        lexicon.append("b -> d -0.693147\n");
        assertEquals(lexicon.toString(), g.lexiconString());
    }

    /**
     * Tests a binary split of each non-terminal.
     */
    @Theory
    public void testSplitStates(final ProductionListGrammar g) {

        final ProductionListGrammar.BiasedNoiseGenerator zeroNoiseGenerator = new ProductionListGrammar.BiasedNoiseGenerator(
                0f);
        final ProductionListGrammar split1 = g.split(zeroNoiseGenerator);

        // s, a_0, a_1, b_0, b_1
        assertArrayEquals(new short[] { 0, 0, 1, 0, 1 }, split1.vocabulary.subcategoryIndices);

        // a -> a b 2/6 should be split into 8
        assertEquals(Math.log(2f / 6 / 8), split1.binaryLogProbability("a_0", "a_0", "b_0"), .01f);
        assertEquals(Math.log(2f / 6 / 8), split1.binaryLogProbability("a_0", "a_1", "b_1"), .01f);

        // b -> b a 1/4 should be split into 8
        assertEquals(Math.log(1f / 4 / 8), split1.binaryLogProbability("b_0", "b_0", "a_1"), .01f);
        assertEquals(Math.log(1f / 4 / 8), split1.binaryLogProbability("b_1", "b_1", "a_0"), .01f);

        // b -> b 1/4 should be split into 4
        assertEquals(Math.log(1f / 4 / 4), split1.unaryLogProbability("b_0", "b_1"), .01f);
        assertEquals(Math.log(1f / 4 / 4), split1.unaryLogProbability("b_1", "b_0"), .01f);

        // Ensure the start symbol was _not_ split (although we'll leave an unused integer index (1), just to make other
        // indices easy to calculate.
        assertEquals(Math.log(1f / 2), split1.unaryLogProbability("top", "a_0"), .01f);
        assertEquals(Math.log(1f / 2), split1.unaryLogProbability("top", "a_1"), .01f);

        // The split indices should be calculable from the pre-split indices (and vice-versa)
        assertEquals(g.vocabulary.getIndex("a") * 2 - 1, split1.vocabulary.getIndex("a_0"));
        assertEquals(g.vocabulary.getIndex("a") * 2, split1.vocabulary.getIndex("a_1"));

        assertEquals(g.vocabulary.getIndex("b") * 2 - 1, split1.vocabulary.getIndex("b_0"));
        assertEquals(g.vocabulary.getIndex("b") * 2, split1.vocabulary.getIndex("b_1"));

        // Now test re-splitting the newly-split grammar again.
        final ProductionListGrammar split2 = split1.split(zeroNoiseGenerator);

        // s, a_0, a_1, a_2, a_3, b_0, b_1, b_2, b_3
        assertArrayEquals(new short[] { 0, 0, 1, 2, 3, 0, 1, 2, 3 }, split2.vocabulary.subcategoryIndices);

        // a -> a b 2/6 should now be split into 64
        assertEquals(Math.log(2f / 6 / 64), split2.binaryLogProbability("a_1", "a_0", "b_0"), .01f);
        assertEquals(Math.log(2f / 6 / 64), split2.binaryLogProbability("a_2", "a_2", "b_3"), .01f);

        // b -> b a 1/4
        assertEquals(Math.log(1f / 4 / 64), split2.binaryLogProbability("b_0", "b_2", "a_3"), .01f);
        assertEquals(Math.log(1f / 4 / 64), split2.binaryLogProbability("b_2", "b_1", "a_1"), .01f);

        // b -> b 1/4 should be split into 16
        assertEquals(Math.log(1f / 4 / 16), split2.unaryLogProbability("b_0", "b_2"), .01f);
        assertEquals(Math.log(1f / 4 / 16), split2.unaryLogProbability("b_2", "b_3"), .01f);
    }

    @Theory
    public void testMerge(final ProductionListGrammar g) {

        final ProductionListGrammar.BiasedNoiseGenerator zeroNoiseGenerator = new ProductionListGrammar.BiasedNoiseGenerator(
                0f);

        // Split the grammar 2X
        final ProductionListGrammar split2 = g.split(zeroNoiseGenerator).split(zeroNoiseGenerator);
        // Now re-merge a_3 into a_2 and b_1 into b_0
        final short[] indices = new short[] { (short) split2.vocabulary.getIndex("a_3"),
                (short) split2.vocabulary.getIndex("b_1") };
        final ProductionListGrammar merged = split2.merge(indices);

        // s, a_0, a_1, a_2, b_0, b_1, b_2
        assertArrayEquals(new short[] { 0, 0, 1, 2, 0, 1, 2 }, merged.vocabulary.subcategoryIndices);

        // We now have a_0, a_1, and a_2 (which contains the former a_2 and a_3 splits)
        // And b_0 (containing the former b_0 and b_1), b_1 (formerly b_2), and b_2 (formerly b_3)

        // a -> a b 2/6 was split into 64, but 4 of those merged into a_2 -> a_2 b_2
        assertEquals(Math.log(2f / 6 / 64 * 4), merged.binaryLogProbability("a_2", "a_2", "b_2"), .01f);
        // a -> a a 1/6 was split into 64 and 8 merged into a_2 -> a_2 a_2
        assertEquals(Math.log(1f / 6 / 64 * 8), merged.binaryLogProbability("a_2", "a_2", "a_2"), .01f);
        // a_1 -> a_1 a_0 was not merged
        assertEquals(Math.log(1f / 6 / 64), merged.binaryLogProbability("a_1", "a_1", "a_0"), .01f);

        // b -> b a 1/4 was split in 64, but 4 of those merged into b_0 -> b_1 a_2
        assertEquals(Math.log(1f / 4 / 64 * 4), merged.binaryLogProbability("b_0", "b_1", "a_2"), .01f);

        // b -> b 1/4 was split in 16, but 4 of those merged into b_0 -> b_0
        assertEquals(Math.log(1f / 4 / 16 * 4), merged.unaryLogProbability("b_0", "b_0"), .01f);
        // b_1 -> b_2 was not merged
        assertEquals(Math.log(1f / 4 / 16), merged.unaryLogProbability("b_1", "b_1"), .01f);

        // a -> c 2/6 was split into 4, but 2 of those merged into a_2 -> c
        assertEquals(Math.log(2f / 6 / 4 * 2), merged.lexicalLogProbability("a_2", "c"), .01f);
        // But a_1 -> c remains fully split
        assertEquals(Math.log(2f / 6 / 4), merged.lexicalLogProbability("a_1", "c"), .01f);

        // TODO
        // fail("Tests of mapping from split to un-split non-terminals not implemented");
    }
}
