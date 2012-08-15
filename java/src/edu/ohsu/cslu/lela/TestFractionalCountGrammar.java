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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.lela.FractionalCountGrammar.ZeroNoiseGenerator;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Unit tests for {@link FractionalCountGrammar}.
 * 
 * @author Aaron Dunlop
 */
@RunWith(Theories.class)
public class TestFractionalCountGrammar extends CountGrammarTestCase {

    @DataPoints
    public static FractionalCountGrammar[] dataPoints() throws IOException {
        final FractionalCountGrammar g1 = new StringCountGrammar(new StringReader(AllLelaTests.STRING_SAMPLE_TREE),
                null, null).toFractionalCountGrammar();
        final FractionalCountGrammar g2 = SAMPLE_GRAMMAR();
        return new FractionalCountGrammar[] { g1, g2 };
    }

    @Before
    public void setUp() {
        g = SAMPLE_GRAMMAR();
    }

    static FractionalCountGrammar SAMPLE_GRAMMAR() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b", "c", "d" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "e", "f" });

        // Build up the same grammar as that induced from the tree in AllElviTests
        final FractionalCountGrammar g = new FractionalCountGrammar(vocabulary, lexicon, null);
        g.incrementUnaryCount("top", "a", 1);
        g.incrementBinaryCount("a", "a", "b", 1);
        g.incrementBinaryCount("a", "a", "d", 1);
        g.incrementBinaryCount("a", "c", "c", 1);
        g.incrementLexicalCount("c", "e", 1);
        g.incrementLexicalCount("c", "e", 1);
        g.incrementLexicalCount("d", "f", 1);
        g.incrementBinaryCount("b", "b", "c", 1);
        g.incrementUnaryCount("b", "d", 1);
        g.incrementLexicalCount("d", "f", 1);
        g.incrementLexicalCount("c", "f", 1);

        return g;
    }

    private FractionalCountGrammar grammar() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });
        final FractionalCountGrammar fcg = new FractionalCountGrammar(vocabulary, lexicon, null);

        // top -> a 1
        fcg.incrementUnaryCount("top", "a", 1);

        // a -> a b 5/12
        // a -> a a 3/12
        // a -> c 3/12
        // a -> d 1/12
        fcg.incrementBinaryCount("a", "a", "b", 1.5f);
        fcg.incrementBinaryCount("a", "a", "b", 1.0f);
        fcg.incrementBinaryCount("a", "a", "a", 1.5f);
        fcg.incrementLexicalCount("a", "c", .5f);
        fcg.incrementLexicalCount("a", "c", 1.0f);
        fcg.incrementLexicalCount("a", "d", .5f);

        // b -> b a 7/16
        // b -> b 3/16
        // b -> c 5/16
        // b -> d 1/16
        fcg.incrementBinaryCount("b", "b", "a", 3.5f);
        fcg.incrementUnaryCount("b", "b", 1.5f);
        fcg.incrementLexicalCount("b", "c", 2.5f);
        fcg.incrementLexicalCount("b", "d", .5f);
        return fcg;
    }

    @Test
    public void testFractionalCounts() {
        final FractionalCountGrammar fcg = grammar();

        final ProductionListGrammar plg = fcg.toProductionListGrammar(Float.NEGATIVE_INFINITY);
        JUnit.assertLogFractionEquals(0, plg.unaryLogProbability("top", "a"), 0.01f);

        JUnit.assertLogFractionEquals(Math.log(5f / 12), plg.binaryLogProbability("a", "a", "b"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(3f / 12), plg.binaryLogProbability("a", "a", "a"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(3f / 12), plg.lexicalLogProbability("a", "c"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(1f / 12), plg.lexicalLogProbability("a", "d"), 0.01f);

        JUnit.assertLogFractionEquals(Math.log(7f / 16), plg.binaryLogProbability("b", "b", "a"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(3f / 16), plg.unaryLogProbability("b", "b"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(5f / 16), plg.lexicalLogProbability("b", "c"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(1f / 16), plg.lexicalLogProbability("b", "d"), 0.01f);
    }

    /**
     * Tests a binary split of each non-terminal.
     */
    @Theory
    public void testSplit(final FractionalCountGrammar grammar) {

        final FractionalCountGrammar split1 = grammar.split(new ZeroNoiseGenerator());

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
        assertEquals(grammar.vocabulary.getIndex("a") * 2, split1.vocabulary.getIndex("a_0"));
        assertEquals(grammar.vocabulary.getIndex("a") * 2 + 1, split1.vocabulary.getIndex("a_1"));

        assertEquals(grammar.vocabulary.getIndex("b") * 2, split1.vocabulary.getIndex("b_0"));
        assertEquals(grammar.vocabulary.getIndex("b") * 2 + 1, split1.vocabulary.getIndex("b_1"));

        // Now test re-splitting the newly-split grammar again.
        final FractionalCountGrammar split2 = split1.split(new ZeroNoiseGenerator());

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

    @Theory
    public void testMergeStartSymbol(final FractionalCountGrammar grammar) {

        final FractionalCountGrammar split1 = grammar.split(new ZeroNoiseGenerator());
        final FractionalCountGrammar merged = split1.merge(new short[] { 1 });

        assertEquals(split1.vocabulary.size() - 1, merged.vocabulary.size());
        final SplitVocabulary mergedVocabulary = (SplitVocabulary) merged.vocabulary;
        assertEquals(1, mergedVocabulary.mergedIndices.size());
        assertTrue("Expected mergedIndices to contain '0'", mergedVocabulary.mergedIndices.contains((short) 0));
        assertEquals(split1.unaryLogProbability("top", "a_0"), merged.unaryLogProbability("top", "a_0"), .01f);
    }

    @Theory
    public void testMerge(final FractionalCountGrammar grammar) {

        // Split the grammar 2X
        final FractionalCountGrammar split2 = grammar.split(new ZeroNoiseGenerator()).split(new ZeroNoiseGenerator());
        // Now re-merge a_3 into a_2 and b_1 into b_0
        final short[] indices = new short[] { (short) split2.vocabulary.getIndex("a_3"),
                (short) split2.vocabulary.getIndex("b_1") };
        final FractionalCountGrammar merged = split2.merge(indices);

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

    // @Test
    // public void testSmooth() {
    // final MappedCountGrammar mcg = grammar();
    // mcg.smooth();
    //
    // }
}
