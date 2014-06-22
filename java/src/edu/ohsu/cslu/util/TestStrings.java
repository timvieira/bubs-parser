/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.cjunit.FilteredRunner;
import org.cjunit.PerformanceTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.datastructs.narytree.CharniakHeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;

@RunWith(FilteredRunner.class)
public class TestStrings {

    // TODO: Implement tests for extractPos and parseTreeTokens?

    @Test
    public void testExtractPos() throws Exception {
        String parseTree = "(TOP (S (NP (NN Delivery)) (VP (AUX is) (S (VP (TO to)"
                + " (VP (VB begin) (PP (IN in) (NP (JJ early) (CD 1991))))))) (. .)))";
        String pos = "(NN Delivery) (AUX is) (TO to) (VB begin) (IN in) (JJ early) (CD 1991) (. .)";
        assertEquals(pos, Strings.extractPos(parseTree));

        parseTree = "(TOP (S (SBAR (IN Although) (S (NP (JJ preliminary) (NNS findings))"
                + " (VP (AUX were) (VP (VBN reported) (ADVP (NP (QP (RBR more) (IN than) (DT a))"
                + " (NN year)) (IN ago)))))) (, ,) (NP (DT the) (JJS latest) (NNS results))"
                + " (VP (VBP appear) (PP (IN in) (NP (NP (NP (NP (NN today) (POS 's)) (NNP New)"
                + " (NNP England) (NNP Journal)) (PP (IN of) (NP (NNP Medicine)))) (, ,)"
                + " (NP (NP (DT a) (NN forum)) (ADJP (JJ likely) (S (VP (TO to) (VP (VB bring)"
                + " (NP (JJ new) (NN attention)) (PP (TO to) (NP (DT the) (NN problem))))))))))) (. .)))";
        pos = "(IN Although) (JJ preliminary) (NNS findings) (AUX were)"
                + " (VBN reported) (RBR more) (IN than) (DT a) (NN year) (IN ago) (, ,) (DT the)"
                + " (JJS latest) (NNS results) (VBP appear) (IN in) (NN today) (POS 's) (NNP New)"
                + " (NNP England) (NNP Journal) (IN of) (NNP Medicine) (, ,) (DT a) (NN forum)"
                + " (JJ likely) (TO to) (VB bring) (JJ new) (NN attention) (TO to) (DT the)" + " (NN problem) (. .)";
        assertEquals(pos, Strings.extractPos(parseTree));
    }

    @Test
    public void testExtractPosAndHead() throws Exception {
        final HeadPercolationRuleset ruleset = new CharniakHeadPercolationRuleset();
        String parseTree = "(TOP (S (NP (NN Delivery)) (VP (AUX is) (S (VP (TO to)"
                + " (VP (VB begin) (PP (IN in) (NP (JJ early) (CD 1991))))))) (. .)))";
        String pos = "(NN Delivery NONHEAD) (AUX is HEAD) (TO to NONHEAD) (VB begin NONHEAD) (IN in NONHEAD) (JJ early NONHEAD) (CD 1991 NONHEAD) (. . NONHEAD)";
        assertEquals(pos, Strings.extractPosAndHead(parseTree, ruleset));

        parseTree = "(TOP (S (SBAR (IN Although) (S (NP (JJ preliminary) (NNS findings))"
                + " (VP (AUX were) (VP (VBN reported) (ADVP (NP (QP (RBR more) (IN than) (DT a))"
                + " (NN year)) (IN ago)))))) (, ,) (NP (DT the) (JJS latest) (NNS results))"
                + " (VP (VBP appear) (PP (IN in) (NP (NP (NP (NP (NN today) (POS 's)) (NNP New)"
                + " (NNP England) (NNP Journal)) (PP (IN of) (NP (NNP Medicine)))) (, ,)"
                + " (NP (NP (DT a) (NN forum)) (ADJP (JJ likely) (S (VP (TO to) (VP (VB bring)"
                + " (NP (JJ new) (NN attention)) (PP (TO to) (NP (DT the) (NN problem))))))))))) (. .)))";
        pos = "(IN Although NONHEAD) (JJ preliminary NONHEAD) (NNS findings NONHEAD)"
                + " (AUX were NONHEAD) (VBN reported NONHEAD) (RBR more NONHEAD) (IN than NONHEAD)"
                + " (DT a NONHEAD) (NN year NONHEAD) (IN ago NONHEAD) (, , NONHEAD) (DT the NONHEAD)"
                + " (JJS latest NONHEAD) (NNS results NONHEAD) (VBP appear HEAD) (IN in NONHEAD)"
                + " (NN today NONHEAD) (POS 's NONHEAD) (NNP New NONHEAD) (NNP England NONHEAD)"
                + " (NNP Journal NONHEAD) (IN of NONHEAD) (NNP Medicine NONHEAD) (, , NONHEAD)"
                + " (DT a NONHEAD) (NN forum NONHEAD) (JJ likely NONHEAD) (TO to NONHEAD)"
                + " (VB bring NONHEAD) (JJ new NONHEAD) (NN attention NONHEAD) (TO to NONHEAD)"
                + " (DT the NONHEAD) (NN problem NONHEAD) (. . NONHEAD)";
        assertEquals(pos, Strings.extractPosAndHead(parseTree, ruleset));
    }

    @Test
    public void testBracketedTags() throws Exception {
        checkStringFeatures(Strings.bracketedTags("(foo 1) (bar 2)\n(foobar 3)"));

        String[][] tags = Strings.bracketedTags("(foo)");
        assertEquals(1, tags.length);
        assertEquals(1, tags[0].length);
        assertEquals("foo", tags[0][0]);

        tags = Strings.bracketedTags("(foo 1)");
        assertEquals(1, tags.length);
        assertEquals(2, tags[0].length);
        assertEquals("foo", tags[0][0]);
        assertEquals("1", tags[0][1]);
    }

    @Test
    public void testSquareBracketedTags() throws Exception {
        checkStringFeatures(Strings.squareBracketedTags("[foo 1] [bar 2]\n[foobar 3]"));
    }

    @Test
    public void testSlashDelimitedTags() throws Exception {
        checkStringFeatures(Strings.slashDelimitedTags("foo/1 bar/2\nfoobar/3"));
    }

    private void checkStringFeatures(final String[][] tokens) {
        assertEquals("foo", tokens[0][0]);
        assertEquals("1", tokens[0][1]);
        assertEquals("bar", tokens[1][0]);
        assertEquals("2", tokens[1][1]);
        assertEquals("foobar", tokens[2][0]);
        assertEquals("3", tokens[2][1]);
    }

    public void testTokenPairs() throws Exception {
        // Single-token string
        assertEquals(new TreeSet<String>(Arrays.asList(new String[] { "foo" })), Strings.tokenPairs("foo"));

        // Two-token string
        assertEquals(new TreeSet<String>(Arrays.asList(new String[] { "foo bar" })), Strings.tokenPairs("foo bar"));

        final Set<String> pairs = Strings.tokenPairs("this is a test");
        assertEquals(6, pairs.size());
        assertTrue(pairs.contains("this is"));
        assertTrue(pairs.contains("this a"));
        assertTrue(pairs.contains("this test"));
        assertTrue(pairs.contains("is a"));
        assertTrue(pairs.contains("is test"));
        assertTrue(pairs.contains("a test"));
    }

    @Test
    public void testFeaturePairs() throws Exception {
        // Single-element string
        assertEquals(new TreeSet<String>(Arrays.asList(new String[] { "(foo)" })), Strings.featurePairs("(foo)"));

        // Two-element string
        assertEquals(new TreeSet<String>(Arrays.asList(new String[] { "(foo) (bar)" })),
                Strings.featurePairs("(foo) (bar)"));

        final Set<String> pairs = Strings.featurePairs("(This _capitalized) (is) (a) (test)");
        assertEquals(6, pairs.size());
        assertTrue(pairs.contains("(This _capitalized) (is)"));
        assertTrue(pairs.contains("(This _capitalized) (a)"));
        assertTrue(pairs.contains("(This _capitalized) (test)"));
        assertTrue(pairs.contains("(is) (a)"));
        assertTrue(pairs.contains("(is) (test)"));
        assertTrue(pairs.contains("(a) (test)"));
    }

    @Test
    public void testPermuteFeatures() throws Exception {
        final Set<String> permutations = Strings
                .permuteFeatures("(Entire _capitalized) (day) (para-sailing _hyphenated)");
        assertEquals(6, permutations.size());
        assertTrue(permutations.contains("(day) (para-sailing _hyphenated) (Entire _capitalized)"));
        assertTrue(permutations.contains("(para-sailing _hyphenated) (day) (Entire _capitalized)"));
        assertTrue(permutations.contains("(para-sailing _hyphenated) (day) (Entire _capitalized)"));
    }

    @Test
    public void testSplitOnSpace() throws Exception {
        assertArrayEquals("a b".split(" "), Strings.splitOnSpace("a b"));
        assertArrayEquals("a b c".split(" "), Strings.splitOnSpace("a b c"));
        assertArrayEquals("aa bbb cccc".split(" "), Strings.splitOnSpace("aa bbb cccc"));
        assertArrayEquals(" aa bbb cccc".split(" "), Strings.splitOnSpace(" aa bbb cccc"));
        assertArrayEquals("aa bbb cccc ".split(" "), Strings.splitOnSpace("aa bbb cccc "));
        assertArrayEquals("aa  bbb  cccc".split(" "), Strings.splitOnSpace("aa  bbb  cccc"));

        // The empty string
        assertArrayEquals("".split(" "), Strings.splitOnSpace(""));

        // Ignore these boundary cases...
        // assertArrayEquals(" ".split(" "), Strings.splitOnSpace(" "));
        // assertArrayEquals("  ".split(" "), Strings.splitOnSpace("  "));
    }

    @Test
    public void testSplitOn() throws Exception {
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitOn("a,b", ',', '\''));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitOn("a,b,c", ',', '\''));
        assertArrayEquals(new String[] { "aa", "bbbb", "cccc" }, Strings.splitOn("aa,bbbb,cccc", ',', '\''));
        assertArrayEquals(new String[] { "a", "b,", "c" }, Strings.splitOn("a,'b,',c", ',', '\''));
        assertArrayEquals(new String[] { "a", ",", "b" }, Strings.splitOn("a,',',b", ',', '\''));
        assertArrayEquals(new String[] { "a", "''", "b" }, Strings.splitOn("a,\'\\''\\',b", ',', '\''));
        assertArrayEquals(new String[] { "M'Bow", "98", "8" }, Strings.splitOn("'M\\'Bow',98,8", ',', '\''));

        // The empty string
        assertArrayEquals("".split(" "), Strings.splitOn("", ',', '\''));
    }

    @Test
    public void testEscape() throws Exception {
        assertEquals("foo", Strings.escape("foo", ',', '\''));
        assertEquals("'foo,bar'", Strings.escape("foo,bar", ',', '\''));
        assertEquals("'\\'foo\\',bar'", Strings.escape("'foo',bar", ',', '\''));
        assertEquals("'foo,\\'bar\\''", Strings.escape("foo,'bar'", ',', '\''));

        // Ensure strings containing the escape char are themselves escaped
        assertEquals("'\\'bar\\''", Strings.escape("'bar'", ',', '\''));
        assertEquals("'\\'\\''", Strings.escape("''", ',', '\''));
    }

    @Test
    @PerformanceTest({ "mbp", "1000", "mbp2012", "1300" })
    public void profilePermuteFeatures() throws Exception {
        Strings.permuteFeatures("(%) (rate) (capped) (one-year) (adjustable) (rate) (mortgages)");
    }
}
