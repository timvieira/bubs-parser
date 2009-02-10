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

    public SequenceAlignment(final MappedSequence newlyAlignedSequence, final int[] gaps)
    {
        this.newlyAlignedSequence = newlyAlignedSequence;
        gapIndices = gaps;
    }

    /**
     * @return The newly-aligned sequence.
     */
    public final MappedSequence alignedSequence()
    {
        return newlyAlignedSequence;
    }

    /**
     * @return Any gaps which must be inserted in the already-aligned sequence(s) as a result of
     *         this new alignment.
     */
    public final int[] gapIndices()
    {
        return gapIndices;
    }

    @Override
    public String toString()
    {
        return newlyAlignedSequence.toString() + "\n";
    }
}