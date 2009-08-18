package edu.ohsu.cslu.common;

import static junit.framework.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unit tests for {@link MultipleVocabularyMappedSequence}.
 * 
 * @author Aaron Dunlop
 * @since Jan 5, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestMultipleVocabularyMappedSequence
{
    private final static DnaVocabulary DNA_VOCABULARY = new DnaVocabulary();
    private static String sentence1 = "(The DT) (computers NNS) (will MD) (display VB) (stock NN)"
        + " (prices NNS) (selected VBN) (by IN) (users NNS) (. .)";
    private static String sentence2 = "(At IN) (least JJS) (not RB) (when WRB) (you PRP) (are AUX) (ascending VBG) (. .) (-RRB- -RRB-)";
    private static SimpleVocabulary[] simpleVocabularies;

    @BeforeClass
    public static void suiteSetUp() throws Exception
    {
        simpleVocabularies = SimpleVocabulary.induceVocabularies(sentence1 + '\n' + sentence2);
    }

    @Test
    public void testConstructors() throws Exception
    {
        MultipleVocabularyMappedSequence sequence = new MultipleVocabularyMappedSequence(new int[] {0, 1, 2, 3},
            DNA_VOCABULARY);
        assertEquals(1, sequence.featureCount());
        assertEquals(4, sequence.length());
        assertEquals(new IntVector(new int[] {0}), sequence.elementAt(0));
        assertEquals(new IntVector(new int[] {2}), sequence.elementAt(2));

        int[][] array = new int[][] { {0, 1}, {1, 2}, {2, 3}, {3, 4}};
        sequence = new MultipleVocabularyMappedSequence(array, DNA_VOCABULARY);
        assertEquals(2, sequence.featureCount());
        assertEquals(4, sequence.length());
        assertEquals(new IntVector(new int[] {0, 1}), sequence.elementAt(0));
        assertEquals(new IntVector(new int[] {1, 2}), sequence.elementAt(1));
        assertEquals(new IntVector(new int[] {2, 3}), sequence.elementAt(2));
        assertEquals(new IntVector(new int[] {3, 4}), sequence.elementAt(3));

        sequence = new MultipleVocabularyMappedSequence(new IntMatrix(array), DNA_VOCABULARY);
        assertEquals(2, sequence.featureCount());
        assertEquals(4, sequence.length());
        assertEquals(new IntVector(new int[] {0, 1}), sequence.elementAt(0));
        assertEquals(new IntVector(new int[] {1, 2}), sequence.elementAt(1));
        assertEquals(new IntVector(new int[] {2, 3}), sequence.elementAt(2));
        assertEquals(new IntVector(new int[] {3, 4}), sequence.elementAt(3));
        SharedNlpTests.assertEquals(new String[] {"G", "T"}, sequence.stringFeatures(3));

        sequence = new MultipleVocabularyMappedSequence(new int[][] {{0, 1, 2}}, DNA_VOCABULARY);
        assertEquals(3, sequence.featureCount());
        assertEquals(1, sequence.length());
        assertEquals(new IntVector(new int[] {0, 1, 2}), sequence.elementAt(0));
    }

    @Test
    public void testReadBracketedSequence() throws Exception
    {
        MultipleVocabularyMappedSequence sequence = new MultipleVocabularyMappedSequence(sentence1, simpleVocabularies);

        assertEquals(10, sequence.length());
        assertEquals(2, sequence.featureCount());

        assertEquals(3, sequence.feature(1, 0));
        assertEquals("computers", sequence.stringFeature(1, 0));
        assertEquals(3, sequence.feature(1, 1));
        assertEquals("NNS", sequence.stringFeature(1, 1));

        assertEquals(9, sequence.feature(7, 0));
        assertEquals("by", sequence.stringFeature(7, 0));
        assertEquals(8, sequence.feature(7, 1));
        assertEquals("IN", sequence.stringFeature(7, 1));

        assertEquals(new IntVector(new int[] {9, 8}), sequence.elementAt(7));
    }

    @Test
    public void testInsertGaps() throws Exception
    {
        // Test insertGaps() in DNA sequences
        MappedSequence act = DNA_VOCABULARY.mapSequence("ACT");
        MappedSequence gac = DNA_VOCABULARY.mapSequence("GAC");

        // No gap insertion
        assertEquals("ACT", DNA_VOCABULARY.mapSequence(act.insertGaps(new int[] {})));
        assertEquals("GAC", DNA_VOCABULARY.mapSequence(gac.insertGaps(new int[] {})));

        // Gaps at beginning and end
        assertEquals("-ACT-", DNA_VOCABULARY.mapSequence(act.insertGaps(new int[] {0, 3})));
        assertEquals("-GAC-", DNA_VOCABULARY.mapSequence(gac.insertGaps(new int[] {0, 3})));

        MappedSequence gacgac = DNA_VOCABULARY.mapSequence("GACGAC");
        MappedSequence actgac = DNA_VOCABULARY.mapSequence("ACTGAC");

        // Inserting two gaps in the same location
        assertEquals("-GACG--AC-", DNA_VOCABULARY.mapSequence(gacgac.insertGaps(new int[] {0, 4, 4, 6})));
        assertEquals("-ACTG--AC-", DNA_VOCABULARY.mapSequence(actgac.insertGaps(new int[] {0, 4, 4, 6})));

        // Inserting a gap at the end without a gap at the beginning
        assertEquals("ACTG-AC-", DNA_VOCABULARY.mapSequence(actgac.insertGaps(new int[] {4, 6})));

        // Linguistic sequences
        MultipleVocabularyMappedSequence sequence = new MultipleVocabularyMappedSequence(sentence1, simpleVocabularies);

        sequence = sequence.insertGaps(new int[] {0, 5});
        assertEquals(3, sequence.feature(2, 0));
        assertEquals("computers", sequence.stringFeature(2, 0));
        assertEquals(3, sequence.feature(2, 1));
        assertEquals("NNS", sequence.stringFeature(2, 1));

        assertEquals(9, sequence.feature(9, 0));
        assertEquals("by", sequence.stringFeature(9, 0));
        assertEquals(8, sequence.feature(9, 1));
        assertEquals("IN", sequence.stringFeature(9, 1));
    }

    @Test
    public void testRemoveAllGaps() throws Exception
    {

        // Gaps at beginning and end
        MappedSequence act = DNA_VOCABULARY.mapSequence("ACT").insertGaps(new int[] {0, 3});
        assertEquals("ACT", DNA_VOCABULARY.mapSequence(act.removeAllGaps()));

        MappedSequence gacgac = DNA_VOCABULARY.mapSequence("GACGAC").insertGaps(new int[] {0, 4, 4, 6});
        assertEquals("GACGAC", DNA_VOCABULARY.mapSequence(gacgac.removeAllGaps()));

        // Linguistic sequences
        MultipleVocabularyMappedSequence sequence = new MultipleVocabularyMappedSequence(sentence1, simpleVocabularies)
            .insertGaps(new int[] {0, 3, 5});

        sequence = sequence.removeAllGaps();
        assertEquals(3, sequence.feature(1, 0));
        assertEquals("computers", sequence.stringFeature(1, 0));
        assertEquals(3, sequence.feature(1, 1));
        assertEquals("NNS", sequence.stringFeature(1, 1));

        assertEquals(9, sequence.feature(7, 0));
        assertEquals("by", sequence.stringFeature(7, 0));
        assertEquals(8, sequence.feature(7, 1));
        assertEquals("IN", sequence.stringFeature(7, 1));
    }

    /**
     * Tests the {@link MultipleVocabularyMappedSequence#retainFeatures(int...)} method.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testCopyFeatures() throws Exception
    {
        Sequence sequence = new MultipleVocabularyMappedSequence(sentence1, simpleVocabularies);
        assertEquals(sequence, sequence.retainFeatures(new int[] {0, 1}));

        assertEquals("The computers will display stock prices selected by users .", sequence.retainFeatures(
            new int[] {0}).toSlashSeparatedString());
        assertEquals("DT NNS MD VB NN NNS VBN IN NNS .", sequence.retainFeatures(new int[] {1})
            .toSlashSeparatedString());
    }

    /**
     * Tests the {@link MultipleVocabularyMappedSequence#subSequence(int, int)} method.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSubsequence() throws Exception
    {
        MultipleVocabularyMappedSequence sequence = new MultipleVocabularyMappedSequence(sentence1, simpleVocabularies);

        assertEquals(0, sequence.subSequence(0, 0).length());
        assertEquals(sequence, sequence.subSequence(0, 10));
        assertEquals("(The DT)", sequence.subSequence(0, 1).toBracketedString());
        assertEquals("(computers NNS) (will MD)", sequence.subSequence(1, 3).toBracketedString());
    }

    @Test
    public void testToString() throws Exception
    {
        Sequence sequence = new MultipleVocabularyMappedSequence(new int[] {0, 1, 2, 3, 4}, new DnaVocabulary());
        assertEquals(" - | A | C | G | T |", sequence.toString());

        sequence = new MultipleVocabularyMappedSequence(new int[][] {{0, 1, 2}}, DNA_VOCABULARY);
        assertEquals(" - |\n A |\n C |", sequence.toString());

        StringBuilder sb = new StringBuilder(256);
        sb.append(" The | computers | will | display | stock | prices | selected | by | users | . |\n");
        sb.append("  DT |       NNS |   MD |      VB |    NN |    NNS |      VBN | IN |   NNS | . |");

        sequence = new MultipleVocabularyMappedSequence(sentence1, simpleVocabularies);
        assertEquals(sb.toString(), sequence.toString());

        assertEquals(sentence1, sequence.toBracketedString());
        assertEquals("The/DT computers/NNS will/MD display/VB stock/NN prices/NNS selected/VBN by/IN users/NNS ./.",
            sequence.toSlashSeparatedString());
    }
}
