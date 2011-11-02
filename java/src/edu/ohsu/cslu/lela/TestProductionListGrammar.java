/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.lela;

import static edu.ohsu.cslu.tests.JUnit.assertLogFractionEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
 */
@RunWith(Theories.class)
public class TestProductionListGrammar {

    @DataPoints
    public static ProductionListGrammar[] dataPoints() throws IOException {
        final ProductionListGrammar g1 = new ProductionListGrammar(new StringCountGrammar(new StringReader(
                AllLelaTests.STRING_SAMPLE_TREE), null, null));
        final ProductionListGrammar g2 = TestFractionalCountGrammar.SAMPLE_GRAMMAR().toProductionListGrammar(
                Float.NEGATIVE_INFINITY);
        return new ProductionListGrammar[] { g1, g2 };
    }

    @Theory
    public void testLexicalLogProbability(final ProductionListGrammar g) {
        assertEquals(-.4054, g.lexicalLogProbability("c", "e"), .0001f);
        assertEquals(-1.0986, g.lexicalLogProbability("c", "f"), .0001f);
        assertLogFractionEquals(0, g.lexicalLogProbability("d", "f"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.lexicalLogProbability("d", "e"), .0001f);

    }

    @Theory
    public void testUnaryLogProbability(final ProductionListGrammar g) {
        assertEquals(-.6931, g.unaryLogProbability("b", "d"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.unaryLogProbability("a", "b"), .0001f);
    }

    @Theory
    public void testBinaryLogProbability(final ProductionListGrammar g) {
        assertEquals(-1.0986f, g.binaryLogProbability("a", "a", "b"), .0001f);
        assertEquals(-1.0986f, g.binaryLogProbability("a", "c", "c"), .0001f);
        assertEquals(-.6931f, g.binaryLogProbability("b", "b", "c"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.binaryLogProbability("b", "a", "a"), .0001f);
    }

    @Theory
    public void testPcfgString(final ProductionListGrammar g) {
        final StringBuilder pcfg = new StringBuilder(1024);
        // Start Symbol
        pcfg.append("top\n");
        // Binary Rules
        pcfg.append("a -> a b -1.098612\n");
        pcfg.append("a -> a d -1.098612\n");
        pcfg.append("a -> c c -1.098612\n");
        pcfg.append("b -> b c -0.693147\n");
        // Unary Rules
        pcfg.append("top -> a 0.000000\n");
        pcfg.append("b -> d -0.693147\n");
        assertEquals(pcfg.toString(), g.pcfgString());
    }

    @Theory
    public void testLexiconString(final ProductionListGrammar g) {
        final StringBuilder lexicon = new StringBuilder(512);
        lexicon.append("c -> e -0.405465\n");
        lexicon.append("c -> f -1.098612\n");
        lexicon.append("d -> f 0.000000\n");
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

        // s, s_1, a_0, a_1, b_0, b_1
        // assertArrayEquals(new short[] { 0, 1, 0, 1, 0, 1 }, split1.vocabulary.subcategoryIndices);
        // assertArrayEquals(new short[] { 2, 2, 2, 2, 2, 2 }, split1.vocabulary.splitCount);
        // assertArrayEquals(new short[] { 0, 0, 1, 1, 2, 2 }, split1.vocabulary.baseCategoryIndices);

        // a -> a b 1/3 should be split into 8, with probability 1/12
        assertLogFractionEquals(Math.log(1f / 3 / 4), split1.binaryLogProbability("a_0", "a_0", "b_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 3 / 4), split1.binaryLogProbability("a_0", "a_1", "b_1"), .01f);

        // b -> b c 1/2 should be split into 8
        assertLogFractionEquals(Math.log(1f / 2 / 4), split1.binaryLogProbability("b_0", "b_0", "c_1"), .01f);
        assertLogFractionEquals(Math.log(1f / 2 / 4), split1.binaryLogProbability("b_1", "b_1", "c_0"), .01f);

        // b -> d 1/2 should be split into 4
        assertLogFractionEquals(Math.log(1f / 2 / 2), split1.unaryLogProbability("b_0", "d_1"), .01f);
        assertLogFractionEquals(Math.log(1f / 2 / 2), split1.unaryLogProbability("b_1", "d_0"), .01f);

        // Ensure the start symbol was _not_ split
        assertLogFractionEquals(Math.log(1f / 2), split1.unaryLogProbability("top", "a_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 2), split1.unaryLogProbability("top", "a_1"), .01f);

        // The split indices should be calculable from the pre-split indices (and vice-versa)
        assertEquals(g.vocabulary.getIndex("a") * 2, split1.vocabulary.getIndex("a_0"));
        assertEquals(g.vocabulary.getIndex("a") * 2 + 1, split1.vocabulary.getIndex("a_1"));

        assertEquals(g.vocabulary.getIndex("b") * 2, split1.vocabulary.getIndex("b_0"));
        assertEquals(g.vocabulary.getIndex("b") * 2 + 1, split1.vocabulary.getIndex("b_1"));

        // Now test re-splitting the newly-split grammar again.
        final ProductionListGrammar split2 = split1.split(zeroNoiseGenerator);

        // a -> a b 1/3 should now be split into 64, with probability 1/48
        assertLogFractionEquals(Math.log(1f / 3 / 16), split2.binaryLogProbability("a_1", "a_0", "b_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 3 / 16), split2.binaryLogProbability("a_2", "a_2", "b_3"), .01f);

        // b -> b c 1/2
        assertLogFractionEquals(Math.log(1f / 2 / 16), split2.binaryLogProbability("b_0", "b_2", "c_3"), .01f);
        assertLogFractionEquals(Math.log(1f / 2 / 16), split2.binaryLogProbability("b_2", "b_1", "c_1"), .01f);

        // b -> d 1/2 should be split into 16, with probability 1/8
        assertLogFractionEquals(Math.log(1f / 2 / 4), split2.unaryLogProbability("b_0", "d_2"), .01f);
        assertLogFractionEquals(Math.log(1f / 2 / 4), split2.unaryLogProbability("b_2", "d_3"), .01f);
    }

    /**
     * Verifies that splitting a grammar unequally (using either a biased or random noise generator) produces a grammar
     * with a valid probability distribution.
     * 
     * @param g
     */
    @Theory
    public void testUnequalSplit(final ProductionListGrammar g) {
        // Try with a slightly biased noise generator
        ProductionListGrammar split1 = g.split(new ProductionListGrammar.BiasedNoiseGenerator(0.01f));
        split1.verifyProbabilityDistribution();

        ProductionListGrammar split2 = split1.split(new ProductionListGrammar.BiasedNoiseGenerator(0.01f));
        split2.verifyProbabilityDistribution();

        // And with a _very_ biased noise generator
        split1 = g.split(new ProductionListGrammar.BiasedNoiseGenerator(0.25f));
        split1.verifyProbabilityDistribution();

        split2 = split1.split(new ProductionListGrammar.BiasedNoiseGenerator(0.25f));
        split2.verifyProbabilityDistribution();

        // And finally, with a random noise generator
        split1 = g.split(new ProductionListGrammar.RandomNoiseGenerator(0.25f));
        split1.verifyProbabilityDistribution();

        split2 = split1.split(new ProductionListGrammar.RandomNoiseGenerator(0.25f));
        split2.verifyProbabilityDistribution();
    }

    @Theory
    public void testMergeStartSymbol(final ProductionListGrammar g) {

        final ProductionListGrammar.BiasedNoiseGenerator zeroNoiseGenerator = new ProductionListGrammar.BiasedNoiseGenerator(
                0f);
        final ProductionListGrammar split1 = g.split(zeroNoiseGenerator);
        final ProductionListGrammar merged = split1.merge(new short[] { 1 });

        assertEquals(split1.vocabulary.size() - 1, merged.vocabulary.size());
        assertEquals(1, merged.vocabulary.mergedIndices.size());
        assertTrue("Expected mergedIndices to contain '0'", merged.vocabulary.mergedIndices.contains((short) 0));
        assertEquals(split1.unaryLogProbability("top", "a_0"), merged.unaryLogProbability("top", "a_0"), .01f);
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
        // assertArrayEquals(new short[] { 0, 0, 1, 2, 0, 1, 2 }, merged.vocabulary.subcategoryIndices);
        // assertArrayEquals(new short[] { 1, 3, 3, 3, 3, 3, 3 }, merged.vocabulary.splitCount);
        // assertArrayEquals(new short[] { 0, 1, 1, 1, 2, 2, 2 }, merged.vocabulary.baseCategoryIndices);

        // We now have a_0, a_1, and a_2 (which contains the former a_2 and a_3 splits)
        // And b_0 (containing the former b_0 and b_1), b_1 (formerly b_2), and b_2 (formerly b_3)

        // a_0 as parent
        //
        // for i in 0 1 2 3; do for j in 0 1 2 3; do for k in 0 1 2 3; do echo "a_$i -> a_$j a_$k";
        // done; done; done | egrep '^a_0' | tr 3 2 | sort | uniq -c
        //
        // a_2 as parent
        //
        // <same> | tr 3 2 | sort | egrep '^a_2' | uniq -c

        // a -> a b 2/6 was split by 1/16, but 2 of those merged into a_2 -> a_0 b_2
        assertLogFractionEquals(Math.log(2f / 6 / 16 * 2 / 2), merged.binaryLogProbability("a_2", "a_0", "b_2"), .01f);
        // And 8 into a_2 -> a_2 b_0
        assertLogFractionEquals(Math.log(2f / 6 / 16 * 8 / 2), merged.binaryLogProbability("a_2", "a_2", "b_0"), .01f);

        // a -> c c 1/3 was split by 1/16 and 2 merged into a_2 -> c_2 c_2
        assertLogFractionEquals(Math.log(1f / 3 / 16 * 2 / 2), merged.binaryLogProbability("a_2", "c_2", "c_2"), .01f);
        // a_1 -> c_1 c_0 was not merged
        assertLogFractionEquals(Math.log(1f / 3 / 16), split2.binaryLogProbability("a_1", "c_1", "c_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 3 / 16), merged.binaryLogProbability("a_1", "c_1", "c_0"), .01f);

        // b -> d 1/2 was split by 1/4, but 2 of those merged into b_0 -> d_0
        assertLogFractionEquals(Math.log(1f / 2 * (2f / 8)), merged.unaryLogProbability("b_0", "d_0"), .01f);
        // b_1 -> d_0 was also merged
        assertLogFractionEquals(Math.log(1f / 4 * (2f / 4)), merged.unaryLogProbability("b_1", "d_0"), .01f);
        // b_1 -> d_2 was not merged
        assertLogFractionEquals(Math.log(1f / 4 / 2), merged.unaryLogProbability("b_1", "d_1"), .01f);

        // c_1 -> e 2/3 and c_2 -> f remain unchanged
        assertLogFractionEquals(Math.log(2f / 3), merged.lexicalLogProbability("c_1", "e"), .01f);
        assertLogFractionEquals(Math.log(1f / 3), merged.lexicalLogProbability("c_2", "f"), .01f);

        // TODO
        // fail("Tests of mapping from split to un-split non-terminals not implemented");
    }
}
