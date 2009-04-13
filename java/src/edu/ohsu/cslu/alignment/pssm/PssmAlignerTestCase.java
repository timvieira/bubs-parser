package edu.ohsu.cslu.alignment.pssm;

import static junit.framework.Assert.assertEquals;
import edu.ohsu.cslu.alignment.CharVocabulary;
import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Shared test code for various PSSM aligners
 * 
 * @author Aaron Dunlop
 * @since Mar 24, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class PssmAlignerTestCase
{
    protected final static DnaVocabulary DNA_VOCABULARY = new DnaVocabulary();

    /**
     * Returns the supplied sequence with gaps inserted to align it according to the model.
     * 
     * @param aligner
     * @param sequence
     * @param model
     * 
     * @return The supplied sequence, with gaps inserted to align it according to the model.
     */
    protected String align(PssmSequenceAligner aligner, String sequence, PssmAlignmentModel model)
    {
        CharVocabulary charVocabulary = (CharVocabulary) model.vocabularies()[0];

        MappedSequence s = charVocabulary.mapSequence(sequence);
        MappedSequence alignment = aligner.align(s, model);

        return charVocabulary.mapSequence(alignment);
    }

    /**
     * Returns an alignment of the supplied sequence with a PSSM model, possibly including gaps to
     * be inserted into the PSSM.
     * 
     * @param aligner
     * @param sequence
     * @param model
     * 
     * @return The supplied sequence, with gaps inserted to align it according to the model.
     */
    protected SequenceAlignment alignWithGaps(PssmSequenceAligner aligner, String sequence, HmmAlignmentModel model)
    {
        MappedSequence s = ((CharVocabulary) model.vocabularies()[0]).mapSequence(sequence);
        return aligner.alignWithGaps(s, model);
    }

    protected void sanityTest(BasePssmAligner aligner, PssmAlignmentModel model)
    {
        // The alignment length is fixed, so sequences of the same length should be unchanged,
        // regardless of the input characters
        assertEquals("Wrong sanity check alignment", "CCCCCCCCCCCCCCCCCCCCCCCCCC", align(aligner,
            "CCCCCCCCCCCCCCCCCCCCCCCCCC", model));
        assertEquals("Wrong sanity check alignment", "AAAAAAAAAAAAAAAAAAAAAAAAAA", align(aligner,
            "AAAAAAAAAAAAAAAAAAAAAAAAAA", model));
        assertEquals("Wrong sanity check alignment", "GGGGGGGGGGGGGGGGGGGGGGGGGG", align(aligner,
            "GGGGGGGGGGGGGGGGGGGGGGGGGG", model));
        assertEquals("Wrong sanity check alignment", "TTTTTTTTTTTTTTTTTTTTTTTTTT", align(aligner,
            "TTTTTTTTTTTTTTTTTTTTTTTTTT", model));
        assertEquals("Wrong sanity check alignment", "ACGTACGTACGTACGTACGTACGTAC", align(aligner,
            "ACGTACGTACGTACGTACGTACGTAC", model));

        // An empty sequence should map to all deletions
        assertEquals("Wrong sanity check alignment", "--------------------------", align(aligner, "", model));
    }

    protected void maximumLikelihoodTest(BasePssmAligner aligner, PssmAlignmentModel model)
    {
        assertEquals("Wrong ML alignment", "-------------------------A", align(aligner, "A", model));

        assertEquals("Wrong ML alignment", "TGA--T-CT-G--C-C-CTG--CA-C", align(aligner, "TGATCTGCCCTGCAC", model));
        assertEquals("Wrong ML alignment", "CAAC-T--T----A-C-CCT--TCTT", align(aligner, "CAACTTACCCTTCTT", model));
        assertEquals("Wrong ML alignment", "CAAC-T--T----A-C-CCTT-CA-G", align(aligner, "CAACTTACCCTTCAG", model));
    }

    protected void laplaceTest(BasePssmAligner aligner, PssmAlignmentModel model)
    {
        assertEquals("Wrong Laplace alignment", "-----------------------A--", align(aligner, "A", model));

        assertEquals("Wrong Laplace alignment", "TGA--T-CT-G--C-C-CTG--CA-C", align(aligner, "TGATCTGCCCTGCAC", model));
        assertEquals("Wrong Laplace alignment", "C-A--A-CTTA--C-C-CTT--CT-T", align(aligner, "CAACTTACCCTTCTT", model));
        assertEquals("Wrong Laplace alignment", "C-A--A-CTTA--C-C-CTT--CA-G", align(aligner, "CAACTTACCCTTCAG", model));
        assertEquals("Wrong Laplace alignment", "C-A--A-CTTA--C-C-CTT--CA-G", align(aligner, "CAACTTACCCTTCAG", model));

        // TODO: Add a test with more training and test data...
    }

    protected void pssmGapInsertionTest(FullPssmAligner aligner, HmmAlignmentModel gapInsertionModel)
    {
        // The basic aligner will just output the sequence as-is
        assertEquals("AACCG", align(aligner, "AACCG", gapInsertionModel));

        // But the aligner should insert a gap in the PSSM if we allow it.
        SequenceAlignment sequenceAlignment = alignWithGaps(aligner, "AACCG", gapInsertionModel);
        assertEquals("Wrong ML alignment", "AACCG-", ((CharVocabulary) gapInsertionModel.vocabularies()[0])
            .mapSequence(sequenceAlignment.alignedSequence()));
        SharedNlpTests.assertEquals(new int[] {2}, sequenceAlignment.gapIndices());
    }
}
