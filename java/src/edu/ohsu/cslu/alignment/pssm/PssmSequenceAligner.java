package edu.ohsu.cslu.alignment.pssm;

import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;

/**
 * Interface for all Position-Specific Score Matrix (PSSM) aligners. A PSSM alignment is of
 * fixed-length (the length of the score matrix). A sequence of equal or shorter length is aligned
 * using the PSSM model.
 * 
 * @author Aaron Dunlop
 * @since Nov 5, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface PssmSequenceAligner
{
    /**
     * Returns the supplied sequence with gaps inserted to align it according to the model.
     * 
     * @param sequence
     * @param model
     * @return The supplied sequence, with gaps inserted to align it according to the model.
     */
    public MappedSequence align(MappedSequence sequence, PssmAlignmentModel model);

    /**
     * Returns the supplied sequence with gaps inserted to align it according to the model.
     * 
     * @param sequence
     * @param model
     * @param features Indices of model features to make use of when aligning
     * 
     * @return The supplied sequence, with gaps inserted to align it according to the model.
     */
    public MappedSequence align(MappedSequence sequence, PssmAlignmentModel model, int[] features);

    /**
     * Aligns the supplied sequence with the PSSM, allowing gaps to be inserted into the PSSM as
     * well as into the sequence.
     * 
     * @param sequence
     * @param hmmModel
     * @param features Indices of model features to make use of when aligning
     * 
     * @return An alignment between the sequence and the PSSM.
     */
    public SequenceAlignment alignWithGaps(MappedSequence sequence, HmmAlignmentModel hmmModel, int[] features);

    /**
     * Aligns the supplied sequence with the PSSM, allowing gaps to be inserted into the PSSM as
     * well as into the sequence.
     * 
     * @param sequence
     * @param model
     * 
     * @return An alignment between the sequence and the PSSM.
     */
    public SequenceAlignment alignWithGaps(MappedSequence sequence, HmmAlignmentModel model);
}
