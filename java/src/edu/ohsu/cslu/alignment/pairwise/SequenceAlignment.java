/**
 * Alignment.java
 */
package edu.ohsu.cslu.alignment.pairwise;

import edu.ohsu.cslu.common.MappedSequence;

/**
 * Represents an alignment of a single unaligned sequence with another sequence or with an existing
 * Multiple Sequence Alignment.
 * 
 * TODO: Move to alignment package
 * 
 * @author Aaron Dunlop
 * @since Jul 9, 2008
 * 
 * @version $Revision:5203 $ $Date:2006-11-02 20:47:19 +0000 (Thu, 02 Nov 2006) $ $Author:timd $
 */
public class SequenceAlignment
{
    /** Aligned sequence */
    private final MappedSequence newlyAlignedSequence;

    /** Gaps to be inserted into already-aligned string */
    private final int[] gapIndices;

    /** Score assigned by the aligner */
    private final float score;

    public SequenceAlignment(final MappedSequence newlyAlignedSequence, final int[] gapIndices, final float score)
    {
        this.newlyAlignedSequence = newlyAlignedSequence;
        this.gapIndices = gapIndices;
        this.score = score;
    }

    /**
     * @return The newly-aligned sequence (including any gaps inserted in the alignment process).
     */
    public final MappedSequence alignedSequence()
    {
        return newlyAlignedSequence;
    }

    /**
     * @return Any columns or gaps which must be inserted in the already-aligned sequence(s) as a
     *         result of this new alignment.
     */
    public final int[] insertedColumnIndices()
    {
        return gapIndices;
    }

    /**
     * @return The score assigned by the aligner when creating this alignment
     */
    public float score()
    {
        return score;
    }

    @Override
    public String toString()
    {
        return newlyAlignedSequence.toString() + "\n";
    }
}