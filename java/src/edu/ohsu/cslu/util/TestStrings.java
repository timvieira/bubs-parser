package edu.ohsu.cslu.util;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.CharniakHeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;

public class TestStrings
{
    // TODO: Implement tests for extractPos and parseTreeTokens?

    @Test
    public void testExtractPos() throws Exception
    {
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
    public void testExtractPosAndHead() throws Exception
    {
        HeadPercolationRuleset ruleset = new CharniakHeadPercolationRuleset();
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
    public void testBracketedTags() throws Exception
    {
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
    public void testSquareBracketedTags() throws Exception
    {
        checkStringFeatures(Strings.squareBracketedTags("[foo 1] [bar 2]\n[foobar 3]"));
    }

    @Test
    public void testSlashDelimitedTags() throws Exception
    {
        checkStringFeatures(Strings.slashDelimitedTags("foo/1 bar/2\nfoobar/3"));
    }

    private void checkStringFeatures(String[][] tokens)
    {
        assertEquals("foo", tokens[0][0]);
        assertEquals("1", tokens[0][1]);
        assertEquals("bar", tokens[1][0]);
        assertEquals("2", tokens[1][1]);
        assertEquals("foobar", tokens[2][0]);
        assertEquals("3", tokens[2][1]);
    }
}
