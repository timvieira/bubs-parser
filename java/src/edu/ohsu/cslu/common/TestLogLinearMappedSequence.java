package edu.ohsu.cslu.common;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.alignment.bio.LogLinearDnaVocabulary;
import edu.ohsu.cslu.tests.SharedNlpTests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Unit tests for {@link LogLinearMappedSequence}.
 * 
 * TODO: Merge shared test code with {@link TestLogLinearMappedSequence} (once the {@link Sequence}
 * interface hierarchy stabilizes).
 * 
 * @author Aaron Dunlop
 * @since Jan 5, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */

public class TestLogLinearMappedSequence
{
    private final DnaVocabulary dnaVocabulary = dnaVocabulary();

    private static String sentence1 = "(The _pos_DT) (computers _pos_NNS) (will _pos_MD) (display _pos_VB) (stock _pos_NN)"
        + " (prices _pos_NNS) (selected _pos_VBN) (by _pos_IN) (users _pos_NNS) (. _pos_.)";
    private static String sentence2 = "(At _pos_IN) (least _pos_JJS) (not _pos_RB) (when _pos_WRB) (you _pos_PRP)"
        + " (are _pos_AUX) (ascending _pos_VBG) (. _pos_.) (-RRB- _pos_-RRB-)";
    private static SimpleVocabulary vocabulary;

    @BeforeClass
    public static void suiteSetUp() throws Exception
    {
        vocabulary = LogLinearVocabulary.induce(sentence1 + '\n' + sentence2);
    }

    @Test
    public void testConstructors() throws Exception
    {
        LogLinearMappedSequence sequence = new LogLinearMappedSequence(new int[] {0, 1, 2, 3}, dnaVocabulary);
        assertEquals(6, sequence.features());
        assertEquals(4, sequence.length());
        SharedNlpTests.assertEquals(new int[] {0}, sequence.elementAt(0).values());
        SharedNlpTests.assertEquals(new int[] {1}, sequence.elementAt(1).values());
        SharedNlpTests.assertEquals(new int[] {2}, sequence.elementAt(2).values());
        SharedNlpTests.assertEquals(new int[] {3}, sequence.elementAt(3).values());

        int[][] array = new int[][] { {0, 1}, {1, 2}, {2, 3}, {3, 4}};
        sequence = new LogLinearMappedSequence(array, dnaVocabulary);
        assertEquals(6, sequence.features());
        assertEquals(4, sequence.length());
        SharedNlpTests.assertEquals(new int[] {0, 1}, sequence.elementAt(0).values());
        SharedNlpTests.assertEquals(new int[] {1, 2}, sequence.elementAt(1).values());
        SharedNlpTests.assertEquals(new int[] {2, 3}, sequence.elementAt(2).values());
        SharedNlpTests.assertEquals(new int[] {3, 4}, sequence.elementAt(3).values());
    }

    @Test
    public void testReadBracketedSequence() throws Exception
    {
        LogLinearMappedSequence sequence = new LogLinearMappedSequence(sentence1, vocabulary);

        assertEquals(10, sequence.length());
        assertEquals(35, sequence.features());

        // TODO: This test is probably now applicable to {@link MultipleVocabularyMappedSequence} as
        // well
        assertEquals(2, sequence.elementAt(1).values().length);
        assertTrue(sequence.elementAt(1).getBoolean(vocabulary.map("computers")));
        assertTrue(sequence.elementAt(1).getBoolean(vocabulary.map("_pos_NNS")));

        assertEquals(2, sequence.elementAt(1).values().length);
        assertTrue(sequence.elementAt(7).getBoolean(vocabulary.map("by")));
        assertTrue(sequence.elementAt(7).getBoolean(vocabulary.map("_pos_IN")));
    }

    /**
     * Tests inserting gaps into a DNA sequence
     * 
     * @throws Exception
     */
    @Test
    public void testInsertGaps() throws Exception
    {
        LogLinearMappedSequence act = (LogLinearMappedSequence) dnaVocabulary.mapSequence("ACT");
        LogLinearMappedSequence gac = (LogLinearMappedSequence) dnaVocabulary.mapSequence("GAC");

        // No gap insertion
        assertEquals("ACT", dnaVocabulary.mapSequence(act.insertGaps(new int[] {})));
        assertEquals("GAC", dnaVocabulary.mapSequence(gac.insertGaps(new int[] {})));

        // Gaps at beginning and end
        assertEquals("-ACT-", dnaVocabulary.mapSequence(act.insertGaps(new int[] {0, 3})));
        assertEquals("-GAC-", dnaVocabulary.mapSequence(gac.insertGaps(new int[] {0, 3})));

        LogLinearMappedSequence gacgac = (LogLinearMappedSequence) dnaVocabulary.mapSequence("GACGAC");
        LogLinearMappedSequence actgac = (LogLinearMappedSequence) dnaVocabulary.mapSequence("ACTGAC");

        // Inserting two gaps in the same location
        assertEquals("-GACG--AC-", dnaVocabulary.mapSequence(gacgac.insertGaps(new int[] {0, 4, 4, 6})));
        assertEquals("-ACTG--AC-", dnaVocabulary.mapSequence(actgac.insertGaps(new int[] {0, 4, 4, 6})));

        // Inserting a gap at the end without a gap at the beginning
        assertEquals("ACTG-AC-", dnaVocabulary.mapSequence(actgac.insertGaps(new int[] {4, 6})));

        // Linguistic sequences
        LogLinearMappedSequence sequence = new LogLinearMappedSequence(sentence1, vocabulary);
        sequence = (LogLinearMappedSequence) sequence.insertGaps(new int[] {0, 5});

        assertEquals(2, sequence.elementAt(2).values().length);
        assertTrue(sequence.elementAt(2).getBoolean(vocabulary.map("computers")));
        assertTrue(sequence.elementAt(2).getBoolean(vocabulary.map("_pos_NNS")));

        assertEquals(2, sequence.elementAt(9).values().length);
        assertTrue(sequence.elementAt(9).getBoolean(vocabulary.map("by")));
        assertTrue(sequence.elementAt(9).getBoolean(vocabulary.map("_pos_IN")));
    }

    @Test
    public void testRemoveAllGaps() throws Exception
    {
        // Gaps at beginning and end
        MappedSequence act = dnaVocabulary.mapSequence("ACT").insertGaps(new int[] {0, 3});
        assertEquals("ACT", dnaVocabulary.mapSequence(act.removeAllGaps()));

        MappedSequence gacgac = dnaVocabulary.mapSequence("GACGAC").insertGaps(new int[] {0, 4, 4, 6});
        assertEquals("GACGAC", dnaVocabulary.mapSequence(gacgac.removeAllGaps()));

        // Linguistic sequences
        LogLinearMappedSequence sequence = (LogLinearMappedSequence) new LogLinearMappedSequence(sentence1, vocabulary)
            .insertGaps(new int[] {0, 3, 5});

        sequence = (LogLinearMappedSequence) sequence.removeAllGaps();
        assertEquals(2, sequence.elementAt(1).values().length);
        assertTrue(sequence.elementAt(1).getBoolean(vocabulary.map("computers")));
        assertTrue(sequence.elementAt(1).getBoolean(vocabulary.map("_pos_NNS")));

        assertEquals(2, sequence.elementAt(1).values().length);
        assertTrue(sequence.elementAt(7).getBoolean(vocabulary.map("by")));
        assertTrue(sequence.elementAt(7).getBoolean(vocabulary.map("_pos_IN")));
    }

    /**
     * Tests the {@link LogLinearMappedSequence#retainFeatures(int...)} method.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testRetainFeatures() throws Exception
    {
        String words = "The computers will display stock prices selected by users .";
        String pos = "_pos_DT _pos_NNS _pos_MD _pos_VB _pos_NN _pos_NNS _pos_VBN _pos_IN _pos_NNS _pos_.";
        int[] wordFeatures = vocabulary.map(words.split(" "));
        int[] posFeatures = vocabulary.map(pos.split(" "));
        int[] allFeatures = vocabulary.map((words + " " + pos).split(" "));

        Sequence sequence = new LogLinearMappedSequence(sentence1, vocabulary);
        assertEquals(sequence, sequence.retainFeatures(allFeatures));

        assertEquals(words, sequence.retainFeatures(wordFeatures).toSlashSeparatedString());
        assertEquals(pos, sequence.retainFeatures(posFeatures).toSlashSeparatedString());
    }

    /**
     * Tests the {@link LogLinearMappedSequence#subSequence(int, int)} method.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSubsequence() throws Exception
    {
        LogLinearMappedSequence sequence = new LogLinearMappedSequence(sentence1, vocabulary);

        assertEquals(0, sequence.subSequence(0, 0).length());
        assertEquals(sequence, sequence.subSequence(0, 10));
        assertEquals("(The _pos_DT)", sequence.subSequence(0, 1).toBracketedString());
        assertEquals("(computers _pos_NNS) (will _pos_MD)", sequence.subSequence(1, 3).toBracketedString());
    }

    @Test
    public void testToString() throws Exception
    {
        Sequence sequence = new LogLinearMappedSequence(new int[] {0, 1, 2, 3, 4}, dnaVocabulary);
        assertEquals(" - | A | C | G | T |", sequence.toString());

        sequence = new LogLinearMappedSequence(new int[][] {{0, 1, 2}}, dnaVocabulary);
        assertEquals(" - |\n A |\n C |", sequence.toString());

        String expected = "     The | computers |    will | display |   stock |   prices | selected |      by |    users |      . |\n"
            + " _pos_DT |  _pos_NNS | _pos_MD | _pos_VB | _pos_NN | _pos_NNS | _pos_VBN | _pos_IN | _pos_NNS | _pos_. |";

        sequence = new LogLinearMappedSequence(sentence1, vocabulary);
        assertEquals(expected, sequence.toString());

        assertEquals(sentence1, sequence.toBracketedString());
        assertEquals("The/_pos_DT computers/_pos_NNS will/_pos_MD display/_pos_VB stock/_pos_NN prices/_pos_NNS"
            + " selected/_pos_VBN by/_pos_IN users/_pos_NNS ./_pos_.", sequence.toSlashSeparatedString());
    }

    private DnaVocabulary dnaVocabulary()
    {
        // TODO: Make this abstract if/when we merge with {@link
        // TestMultipleVocabularyMappedSequence}
        return new LogLinearDnaVocabulary();
    }

}
