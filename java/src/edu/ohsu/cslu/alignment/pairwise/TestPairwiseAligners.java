package edu.ohsu.cslu.alignment.pairwise;

import static junit.framework.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.MatrixSubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.alignment.character.CharSequence;
import edu.ohsu.cslu.alignment.character.CharSubstitutionAlignmentModel;
import edu.ohsu.cslu.common.LogLinearMappedSequence;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.datastructs.matrices.DenseMatrix;
import edu.ohsu.cslu.datastructs.matrices.FloatMatrix;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for pairwise aligners.
 * 
 * TODO Implement pairwise alignment model for {@link LogLinearMappedSequence}s. It's quite
 * reasonable for a {@link SubstitutionAlignmentModel} to calculate the cost of aligning two
 * log-linear feature vectors.
 * 
 * @author Aaron Dunlop
 * @since Oct 8, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestPairwiseAligners
{
    private final static DnaVocabulary DNA_VOCABULARY = new DnaVocabulary();

    private static SubstitutionAlignmentModel identityMatrixModel;

    @BeforeClass
    public static void suiteSetUp() throws Exception
    {
        final DenseMatrix alignmentMatrix = new FloatMatrix(new float[][] { {0f, 8f, 8f, 8f, 8f, 8f},
            {8f, 0f, 10f, 10f, 10f, 10f}, {8f, 10f, 0f, 10f, 10f, 10f}, {8f, 10f, 10f, 0f, 10f, 10f},
            {8f, 10f, 10f, 10f, 0f, 10f}, {8f, 10f, 10f, 10f, 10f, 0f}});
        identityMatrixModel = new MatrixSubstitutionAlignmentModel(alignmentMatrix, DNA_VOCABULARY);
    }

    private String alignStrings(final PairwiseAligner aligner, final AlignmentModel model, final String unaligned,
        final String aligned)
    {
        return DNA_VOCABULARY.mapSequence(aligner.alignPair(DNA_VOCABULARY.mapSequence(unaligned),
            DNA_VOCABULARY.mapSequence(aligned), model).alignedSequence());
    }

    @Test
    public void testFullDynamicAligner() throws Exception
    {
        final FullDynamicPairwiseAligner aligner = new FullDynamicPairwiseAligner();
        testDnaAlignment(aligner);

        // And a character-based alignment
        assertEquals("the dog\0's barking", alignStringsAsCharSequences(aligner, "the dog's barking",
            "the dog 's barking"));
    }

    private String alignStringsAsCharSequences(final PairwiseAligner aligner, final String unaligned,
        final String aligned)
    {
        final Sequence s1 = new CharSequence(unaligned);
        final Sequence s2 = new CharSequence(aligned);
        return aligner.alignPair(s1, s2, new CharSubstitutionAlignmentModel()).alignedSequence().toString();
    }

    private void testDnaAlignment(final FullDynamicPairwiseAligner aligner)
    {
        assertEquals("AC--AC", alignStrings(aligner, identityMatrixModel, "ACAC", "ACT-AC"));
        assertEquals("-CTG-", alignStrings(aligner, identityMatrixModel, "CTG", "ACTGA"));

        // Variable length aligner can handle an unaligned string longer than the aligned string
        assertEquals("ACTGA", alignStrings(aligner, identityMatrixModel, "ACTGA", "CTG"));

        assertEquals("ACXGA", alignStrings(aligner, identityMatrixModel, "ACXGA", "ACTGA"));

        assertEquals("ACTGA-", alignStrings(aligner, identityMatrixModel, "ACTGA", "CTGAC"));
        assertEquals("-CTGAC", alignStrings(aligner, identityMatrixModel, "CTGAC", "ACTGA"));

        SequenceAlignment alignment = aligner.alignPair(DNA_VOCABULARY.mapSequence("ACTGA"), DNA_VOCABULARY
            .mapSequence("CTGACT"), identityMatrixModel);
        assertEquals("ACTGA--", DNA_VOCABULARY.mapSequence(alignment.alignedSequence()));
        assertEquals(1, alignment.insertedColumnIndices().length);
        assertEquals(0, alignment.insertedColumnIndices()[0]);

        alignment = aligner.alignPair(DNA_VOCABULARY.mapSequence("CTGACT"), DNA_VOCABULARY.mapSequence("AACTGAC"),
            identityMatrixModel);
        assertEquals("--CTGACT", DNA_VOCABULARY.mapSequence(alignment.alignedSequence()));
        assertEquals(1, alignment.insertedColumnIndices().length);
        assertEquals(7, alignment.insertedColumnIndices()[0]);

        alignment = aligner.alignPair(DNA_VOCABULARY.mapSequence("CTGGACT"), DNA_VOCABULARY.mapSequence("ACTGAC"),
            identityMatrixModel);
        assertEquals("-CTGGACT", DNA_VOCABULARY.mapSequence(alignment.alignedSequence()));
        assertEquals(2, alignment.insertedColumnIndices().length);
        assertEquals(3, alignment.insertedColumnIndices()[0]);
        assertEquals(6, alignment.insertedColumnIndices()[1]);
        assertEquals(" A | C | T | - | G | A | C | - |", DNA_VOCABULARY.mapSequence("ACTGAC").insertGaps(
            alignment.insertedColumnIndices()).toString());

        // Now test using SixCharacterAlignmentModel, which penalizes gap insertion in sequences
        // longer than 6 elements
        final SixCharacterAlignmentModel model = new SixCharacterAlignmentModel(10f, DNA_VOCABULARY);
        // We expect 'normal' alignments with 5 and 6-character strings
        assertEquals("TAACG-", alignStrings(aligner, model, "TAACG", "TACG-"));
        assertEquals("TAACGG", alignStrings(aligner, model, "TAACGG", "TCACGG"));

        // And we expect bad alignments for longer strings - TAACG-X / T-ACGG-X would be a much
        // better alignment (and is in fact the alignment we expect from the more sensible model)
        assertEquals("TAACG-X", alignStrings(aligner, identityMatrixModel, "TAACGX", "TACG-X"));
        assertEquals("TAACGX", alignStrings(aligner, model, "TAACGX", "TACG-X"));
    }

    private static class SixCharacterAlignmentModel extends MatrixSubstitutionAlignmentModel
    {
        public SixCharacterAlignmentModel(final float substitutionCost, final AlignmentVocabulary vocabulary)
        {
            super(new float[] {substitutionCost}, new float[] {substitutionCost},
                new AlignmentVocabulary[] {vocabulary});
        }

        @Override
        public float gapInsertionCost(final int feature, final int sequenceLength)
        {
            float cost = super.cost(GAP_INDEX, feature);
            if (sequenceLength >= 6)
            {
                // Severely penalize gap insertion when the sequence is 6 or more elements long
                cost += 200f;
            }
            return cost;
        }

        @Override
        public float gapInsertionCost(final Vector featureVector, final int sequenceLength)
        {
            float cost = super.cost(gapVector, featureVector);
            if (sequenceLength >= 6)
            {
                // Severely penalize gap insertion when the sequence is 6 or more elements long
                cost += 200f;
            }
            return cost;
        }
    }
}
