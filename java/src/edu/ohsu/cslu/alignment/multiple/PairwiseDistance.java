/**
 * PairwiseDistance.java
 */
package edu.ohsu.cslu.alignment.multiple;

import java.util.SortedSet;

/**
 * Represents the pairwise distance between two sequences. Allows ordered storage of these pairwise
 * distances (such as in a @link {@link SortedSet}). Handles a maximum of 2^16 sequences (Note -
 * indices are currently represented as ints to avoid requiring casting, even though shorts are long
 * enough)
 * 
 * TODO: Extend to handle more than 32767 sequences.
 * 
 * @author Aaron Dunlop
 * @since Jul 9, 2008
 * 
 *        $Id$
 */
public final class PairwiseDistance implements Comparable<PairwiseDistance>
{
    public final int index1;
    public final int index2;
    private final float d;
    private final long hashCode;

    public PairwiseDistance(final int index1, final int index2, final float distance)
    {
        if (index1 > 32767 || index2 > 32767)
        {
            throw new RuntimeException("Maximum sequence count exceeded");
        }

        this.index1 = index1;
        this.index2 = index2;
        this.d = distance;
        int lowerIndex = (index1 <= index2 ? index1 : index2);
        int higherIndex = (index1 <= index2 ? index2 : index1);
        // Prioritizes distance first, but delineates between pairs by index as well.
        hashCode = ((long) distance) << 32 | lowerIndex << 16 | higherIndex;
    }

    @Override
    public int compareTo(PairwiseDistance o)
    {
        if (hashCode < o.hashCode)
        {
            return -1;
        }

        if (hashCode > o.hashCode)
        {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        PairwiseDistance o = (PairwiseDistance) obj;

        return (o.index1 == index1 && o.index2 == index2 && o.d == d);
    }

    @Override
    public String toString()
    {
        return "(" + index1 + ", " + index2 + " = " + d + ")";
    }
}