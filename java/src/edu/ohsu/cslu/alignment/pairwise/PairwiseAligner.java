package edu.ohsu.cslu.alignment.pairwise;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.common.Sequence;

/**
 * Aligns an unaligned sequence with an already-aligned sequence using a specified {@link AlignmentModel}
 * 
 * @author Aaron Dunlop
 * @since Jul 9, 2008
 * 
 *        $Id$
 */
public interface PairwiseAligner {

    /**
     * Aligns two sequences with one another. Assumes that one sequence has already been aligned with any
     * other sequences in a multiple sequence alignment and that the other should be added to that alignment.
     * 
     * @param unaligned
     *            Unaligned sequence
     * @param aligned
     *            Aligned sequence
     * @param model
     *            {@link AlignmentModel} to use when aligning
     * 
     * @return Aligned version of string, with gaps inserted as appropriate.
     */
    public SequenceAlignment alignPair(Sequence unaligned, Sequence aligned, AlignmentModel model);

}
