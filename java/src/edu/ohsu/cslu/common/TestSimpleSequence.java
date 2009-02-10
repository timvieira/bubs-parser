package edu.ohsu.cslu.common;

import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class TestSimpleSequence
{
    private final static String sentence1BracketedInput = "(The DT) (computers NNS) (will MD) (display VB) (stock NN)\n"
        + "(prices NNS) (selected VBN) (by IN) (users NNS) (. .)";
    private final static String sentence1SlashDelimitedInput = "The/DT computers/NNS will/MD display/VB stock/NN\n"
        + "prices/NNS selected/VBN by/IN users/NNS ./.";
    private final static String sentence1SquareBracketedInput = "[The DT] [computers NNS] [will MD] [display VB] [stock NN]\n"
        + "[prices NNS] [selected VBN] [by IN] [users NNS] [. .]";

    private final static String sentence1Bracketed = sentence1BracketedInput.replaceAll("\n", " ");

    private Sequence sequence1;

    @Before
    public void setUp() throws Exception
    {
        sequence1 = new SimpleSequence(sentence1BracketedInput);
    }

    @Test
    public void testConstructors() throws Exception
    {
        // Test reading a bracketed string
        assertEquals(sentence1Bracketed, new SimpleSequence(sentence1BracketedInput).toBracketedString());
        assertEquals(sentence1Bracketed, new SimpleSequence(new StringReader(sentence1BracketedInput))
            .toBracketedString());

        // And a square bracketed string
        assertEquals(sentence1Bracketed, new SimpleSequence(sentence1SquareBracketedInput).toBracketedString());
        assertEquals(sentence1Bracketed, new SimpleSequence(new StringReader(sentence1SquareBracketedInput))
            .toBracketedString());

        // And a slash-separated string
        assertEquals(sentence1Bracketed, new SimpleSequence(sentence1SlashDelimitedInput).toBracketedString());
        assertEquals(sentence1Bracketed, new SimpleSequence(new StringReader(sentence1SlashDelimitedInput))
            .toBracketedString());

        // Construct a sequence from a String array
        assertEquals("(The DT) (aliens NNS) (will AUX) (win VB) (. .)", new SimpleSequence(
            new String[][] { {"The", "DT"}, {"aliens", "NNS"}, {"will", "AUX"}, {"win", "VB"}, {".", "."}})
            .toBracketedString());
    }

    @Test
    public void testReadBracketedSequence() throws Exception
    {
        assertEquals(10, sequence1.length());
        assertEquals(2, sequence1.features());

        assertEquals("computers", sequence1.stringFeature(1, 0));
        assertEquals("NNS", sequence1.stringFeature(1, 1));

        assertEquals("by", sequence1.stringFeature(7, 0));
        assertEquals("IN", sequence1.stringFeature(7, 1));
    }

    /**
     * Tests the {@link SimpleMappedSequence#features(int[])} method.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testFeatures() throws Exception
    {
        assertEquals(sequence1, sequence1.features(new int[] {0, 1}));

        assertEquals("DT NNS MD VB NN NNS VBN IN NNS .", sequence1.features(new int[] {1}).toSlashSeparatedString());
        assertEquals("The computers will display stock prices selected by users .", sequence1.features(new int[] {0})
            .toSlashSeparatedString());
    }

    /**
     * Tests {@link SimpleSequence#subsequence(int, int)} method.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSubsequence() throws Exception
    {
        assertEquals(0, sequence1.subSequence(0, 0).length());
        assertEquals(sequence1, sequence1.subSequence(0, 10));
        assertEquals("(The DT)", sequence1.subSequence(0, 1).toBracketedString());
        assertEquals("(computers NNS) (will MD)", sequence1.subSequence(1, 3).toBracketedString());
    }

    /**
     * Tests {@link SimpleSequence#splitIntoSentences()} method.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSplitIntoSentences() throws Exception
    {
        // First, verify that it splits appropriately when given explicit <s> and </s> tags (even if
        // the sentences contained therein are malformed)
        Sequence s1 = new SimpleSequence(
            "(<s> <s>) (The DT) (dog NN) (</s> </s>) (<s> <s>) (A DT) (cat NN) (</s> </s>) (<s> <s>) (Three DT) (fish NN) (</s> </s>)");
        Sequence s2 = new SimpleSequence("(<s>) (The) (dog) (</s>) (<s>) (A) (cat) (</s>) (<s>) (Three) (fish) (</s>)");

        Sequence[] sentences = s1.splitIntoSentences();
        assertEquals(3, sentences.length);
        assertEquals(4, sentences[0].length());
        assertEquals("dog", sentences[0].stringFeature(2, 0));
        assertEquals(4, sentences[1].length());
        assertEquals("cat", sentences[1].stringFeature(2, 0));
        assertEquals(4, sentences[2].length());
        assertEquals("fish", sentences[2].stringFeature(2, 0));

        sentences = s2.splitIntoSentences();
        assertEquals(3, sentences.length);
        assertEquals(4, sentences[0].length());
        assertEquals("dog", sentences[0].stringFeature(2, 0));
        assertEquals(4, sentences[1].length());
        assertEquals("cat", sentences[1].stringFeature(2, 0));
        assertEquals(4, sentences[2].length());
        assertEquals("fish", sentences[2].stringFeature(2, 0));

        // We don't expect the same sequences without explicit start and end tags to be split at all
        assertEquals(1, new SimpleSequence("(The) (dog) (A) (cat) (Three) (fish)").splitIntoSentences().length);
        assertEquals(1, new SimpleSequence("(The DT) (dog NN) (A DT) (cat NN) (Three DT) (fish NN)")
            .splitIntoSentences().length);

        // Now test something a little harder - not comprehensive tests of all sentence-breaking
        // tasks, but tests of a couple basic cases.
        sentences = new SimpleSequence(
            "The dog at 415 4th Ave. is friendly. Is there a friendly dog at 415 4th Ave? Yes.").splitIntoSentences();
        assertEquals(8, sentences[0].length());
        assertEquals(9, sentences[1].length());
        assertEquals(1, sentences[2].length());

        // The same sequence, with punctuation tokenized
        sentences = new SimpleSequence(
            "The dog at 415 4th Ave . is friendly . Is there a friendly dog at 415 4th Ave ? Yes .")
            .splitIntoSentences();
        assertEquals(10, sentences[0].length());
        assertEquals(10, sentences[1].length());
        assertEquals(2, sentences[2].length());

        // The same sequence, with punctuation tokenized
        sentences = new SimpleSequence("(The DT) (dog NN) (at IN) (415 JJ) (4th JJ) (Ave NN) (. :) (is VBP)\n"
            + "(friendly JJ) (. .) (Is MD) (there EX) (a DT) (friendly JJ) (dog NN) (at IN) (415 JJ) (4th JJ)"
            + " (Ave NN) (? .) (Yes UH) (. .)").splitIntoSentences();
        assertEquals(10, sentences[0].length());
        assertEquals(":", sentences[0].stringFeature(6, 1));
        assertEquals(10, sentences[1].length());
        assertEquals(".", sentences[0].stringFeature(9, 1));
        assertEquals(2, sentences[2].length());
        assertEquals(".", sentences[2].stringFeature(1, 1));
    }
}
