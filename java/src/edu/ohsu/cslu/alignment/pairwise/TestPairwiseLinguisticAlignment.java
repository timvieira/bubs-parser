package edu.ohsu.cslu.alignment.pairwise;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.MatrixSubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.util.Strings;

@RunWith(FilteredRunner.class)
public class TestPairwiseLinguisticAlignment {

    private static SimpleVocabulary[] vocabularies;
    private static SubstitutionAlignmentModel simpleAlignmentModel;

    // Sentence 4
    private final static String sampleSentence4 = "(TOP (S (NP (NN Delivery)) (VP (AUX is) (S (VP (TO to)"
            + " (VP (VB begin) (PP (IN in) (NP (JJ early) (CD 1991))))))) (. .)))";
    // Sentence 16
    private final static String sampleSentence16 = "(TOP (S (NP (JJS Most)) (VP (AUX are) (VP (VBN expected)"
            + " (S (VP (TO to) (VP (VB fall) (PP (IN below) (NP (JJ previous-month) (NNS levels)))))))) (. .)))";

    @BeforeClass
    public static void suiteSetUp() throws Exception {
        // Create Vocabulary
        final StringBuilder sb = new StringBuilder(1024);
        sb.append(Strings.extractPos(sampleSentence4));
        sb.append('\n');
        sb.append(Strings.extractPos(sampleSentence16));
        vocabularies = SimpleVocabulary.induceVocabularies(sb.toString());

        // Create an alignment model based on the vocabulary - pos substitutions cost 10, word
        // substitutions 1, gap insertions 4, matches 0.
        simpleAlignmentModel = new MatrixSubstitutionAlignmentModel(new float[] { 10, 1 },
            new float[] { 4, 4 }, vocabularies);
    }

    @Test
    public void testSampleAlignment() throws Exception {
        final MappedSequence sequence1 = new MultipleVocabularyMappedSequence(Strings
            .extractPos(sampleSentence4), vocabularies);
        final MappedSequence sequence2 = new MultipleVocabularyMappedSequence(Strings
            .extractPos(sampleSentence16), vocabularies);

        final FullDynamicPairwiseAligner aligner = new FullDynamicPairwiseAligner();
        SequenceAlignment alignment = aligner.alignPair(sequence1, sequence2, simpleAlignmentModel);

        StringBuilder sb = new StringBuilder(512);
        sb.append("       NN | AUX | _- | TO |    VB | IN |    JJ |   CD | . |\n");
        sb.append(" Delivery |  is | _- | to | begin | in | early | 1991 | . |\n");
        assertEquals(sb.toString(), alignment.toString());

        // Now align the other sentence
        alignment = aligner.alignPair(sequence2, sequence1, simpleAlignmentModel);

        sb = new StringBuilder(512);
        sb.append("  JJS | AUX |      VBN | TO |   VB |    IN |             JJ |    NNS | . |\n");
        sb.append(" Most | are | expected | to | fall | below | previous-month | levels | . |\n");
        assertEquals(sb.toString(), alignment.toString());
        assertArrayEquals(new int[] { 2 }, alignment.insertedColumnIndices());

        final MultipleSequenceAlignment sequenceAlignment = new MultipleSequenceAlignment(
            new MappedSequence[] { alignment.alignedSequence(),
                    sequence1.insertGaps(alignment.insertedColumnIndices()) });
        sb = new StringBuilder(512);

        sb.append("      JJS | AUX |      VBN | TO |    VB |    IN |             JJ |    NNS | . |\n");
        sb.append("     Most | are | expected | to |  fall | below | previous-month | levels | . |\n");
        sb.append("-------------------------------------------------------------------------------\n");
        sb.append("       NN | AUX |       _- | TO |    VB |    IN |             JJ |     CD | . |\n");
        sb.append(" Delivery |  is |       _- | to | begin |    in |          early |   1991 | . |\n");
        sb.append("-------------------------------------------------------------------------------\n");
        assertEquals(sb.toString(), sequenceAlignment.toString());
    }
}
