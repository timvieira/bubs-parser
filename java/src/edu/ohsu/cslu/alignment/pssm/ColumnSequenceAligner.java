package edu.ohsu.cslu.alignment.pssm;

import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;

/**
 * Interface for all Column-wise aligners. A column-wise alignment is generally of fixed-length (the
 * length of the profile). A sequence of equal or shorter length is aligned using the
 * {@link ColumnAlignmentModel}. Note that some {@link ColumnAlignmentModel} implementations do
 * allow insertion of additional columns into the profile (generally for a sizable cost).
 * 
 * @author Aaron Dunlop
 * @since Nov 5, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface ColumnSequenceAligner
{
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
    public SequenceAlignment align(MappedSequence sequence, ColumnAlignmentModel hmmModel, int[] features);

    /**
     * Aligns the supplied sequence with the PSSM, allowing gaps to be inserted into the PSSM as
     * well as into the sequence.
     * 
     * @param sequence
     * @param model
     * 
     * @return An alignment between the sequence and the PSSM.
     */
    public SequenceAlignment align(MappedSequence sequence, ColumnAlignmentModel model);
}
