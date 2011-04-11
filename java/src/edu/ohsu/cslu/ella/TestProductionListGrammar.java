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
package edu.ohsu.cslu.ella;

import static edu.ohsu.cslu.tests.JUnit.assertLogFractionEquals;
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
        final ProductionListGrammar g2 = new ProductionListGrammar(
            TestMappedCountGrammar.SAMPLE_MAPPED_GRAMMAR());
        return new ProductionListGrammar[] { g1, g2 };
    }

    @Theory
    public void testLexicalLogProbability(final ProductionListGrammar g) {
        assertEquals(-1.098612, g.lexicalLogProbability("a", "c"), .0001f);
        assertEquals(-1.791759, g.lexicalLogProbability("a", "d"), .0001f);
        assertLogFractionEquals(Math.log(1f / 2), g.lexicalLogProbability("b", "d"), .0001f);
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
        assertArrayEquals(new short[] { 1, 2, 2, 2, 2 }, split1.vocabulary.splitCount);
        assertArrayEquals(new short[] { 0, 1, 1, 2, 2 }, split1.vocabulary.baseCategoryIndices);

        // a -> a b 2/6 should be split into 8, with probability 1/4
        assertLogFractionEquals(Math.log(2f / 6 / 4), split1.binaryLogProbability("a_0", "a_0", "b_0"), .01f);
        assertLogFractionEquals(Math.log(2f / 6 / 4), split1.binaryLogProbability("a_0", "a_1", "b_1"), .01f);

        // b -> b a 1/4 should be split into 8
        assertLogFractionEquals(Math.log(1f / 4 / 4), split1.binaryLogProbability("b_0", "b_0", "a_1"), .01f);
        assertLogFractionEquals(Math.log(1f / 4 / 4), split1.binaryLogProbability("b_1", "b_1", "a_0"), .01f);

        // b -> b 1/4 should be split into 4
        assertLogFractionEquals(Math.log(1f / 4 / 2), split1.unaryLogProbability("b_0", "b_1"), .01f);
        assertLogFractionEquals(Math.log(1f / 4 / 2), split1.unaryLogProbability("b_1", "b_0"), .01f);

        // Ensure the start symbol was _not_ split
        assertLogFractionEquals(Math.log(1f / 2), split1.unaryLogProbability("top", "a_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 2), split1.unaryLogProbability("top", "a_1"), .01f);

        // The split indices should be calculable from the pre-split indices (and vice-versa)
        assertEquals(g.vocabulary.getIndex("a") * 2 - 1, split1.vocabulary.getIndex("a_0"));
        assertEquals(g.vocabulary.getIndex("a") * 2, split1.vocabulary.getIndex("a_1"));

        assertEquals(g.vocabulary.getIndex("b") * 2 - 1, split1.vocabulary.getIndex("b_0"));
        assertEquals(g.vocabulary.getIndex("b") * 2, split1.vocabulary.getIndex("b_1"));

        // Now test re-splitting the newly-split grammar again.
        final ProductionListGrammar split2 = split1.split(zeroNoiseGenerator);

        // s, a_0, a_1, a_2, a_3, b_0, b_1, b_2, b_3
        assertArrayEquals(new short[] { 0, 0, 1, 2, 3, 0, 1, 2, 3 }, split2.vocabulary.subcategoryIndices);
        assertArrayEquals(new short[] { 1, 4, 4, 4, 4, 4, 4, 4, 4 }, split2.vocabulary.splitCount);
        assertArrayEquals(new short[] { 0, 1, 1, 1, 1, 2, 2, 2, 2 }, split2.vocabulary.baseCategoryIndices);

        // a -> a b 2/6 should now be split into 64, with probability 1/16
        assertLogFractionEquals(Math.log(2f / 6 / 16), split2.binaryLogProbability("a_1", "a_0", "b_0"), .01f);
        assertLogFractionEquals(Math.log(2f / 6 / 16), split2.binaryLogProbability("a_2", "a_2", "b_3"), .01f);

        // b -> b a 1/4
        assertLogFractionEquals(Math.log(1f / 4 / 16), split2.binaryLogProbability("b_0", "b_2", "a_3"), .01f);
        assertLogFractionEquals(Math.log(1f / 4 / 16), split2.binaryLogProbability("b_2", "b_1", "a_1"), .01f);

        // b -> b 1/4 should be split into 16, with probability 1/4
        assertLogFractionEquals(Math.log(1f / 4 / 4), split2.unaryLogProbability("b_0", "b_2"), .01f);
        assertLogFractionEquals(Math.log(1f / 4 / 4), split2.unaryLogProbability("b_2", "b_3"), .01f);
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
        assertArrayEquals(new short[] { 1, 3, 3, 3, 3, 3, 3 }, merged.vocabulary.splitCount);
        assertArrayEquals(new short[] { 0, 1, 1, 1, 2, 2, 2 }, merged.vocabulary.baseCategoryIndices);

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
        assertLogFractionEquals(Math.log(2f / 6 / 16 * 2 / 2),
            merged.binaryLogProbability("a_2", "a_0", "b_2"), .01f);
        // And 8 into a_2 -> a_2 b_0
        assertLogFractionEquals(Math.log(2f / 6 / 16 * 8 / 2),
            merged.binaryLogProbability("a_2", "a_2", "b_0"), .01f);

        // a -> a a 1/6 was split by 1/16 and 8 merged into a_2 -> a_2 a_2
        assertLogFractionEquals(Math.log(1f / 6 / 16 * 8 / 2),
            merged.binaryLogProbability("a_2", "a_2", "a_2"), .01f);
        // a_1 -> a_1 a_0 was not merged
        assertLogFractionEquals(Math.log(1f / 6 / 16), split2.binaryLogProbability("a_1", "a_1", "a_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 6 / 16), merged.binaryLogProbability("a_1", "a_1", "a_0"), .01f);

        // b -> b 1/4 was split by 1/4, but 4 of those merged into b_0 -> b_0
        assertLogFractionEquals(Math.log(1f / 4 * (4f / 8)), merged.unaryLogProbability("b_0", "b_0"), .01f);
        // b_1 -> b_0 was also merged
        assertLogFractionEquals(Math.log(1f / 4 * (2f / 4)), merged.unaryLogProbability("b_1", "b_0"), .01f);
        // b_1 -> b_2 was not merged
        assertLogFractionEquals(Math.log(1f / 4 * (1f / 4)), merged.unaryLogProbability("b_1", "b_1"), .01f);

        // a_1 -> c 2/6 and a_2 -> c remain unchanged
        assertLogFractionEquals(Math.log(2f / 6), merged.lexicalLogProbability("a_1", "c"), .01f);
        assertLogFractionEquals(Math.log(2f / 6), merged.lexicalLogProbability("a_2", "c"), .01f);

        // TODO
        // fail("Tests of mapping from split to un-split non-terminals not implemented");
    }
}
