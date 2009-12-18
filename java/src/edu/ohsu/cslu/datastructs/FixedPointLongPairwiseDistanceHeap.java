package edu.ohsu.cslu.datastructs;

/**
 * Implements {@link PairwiseDistanceHeap}, packing all three fields of each element (distance and two
 * indices) into a single long for memory efficiency.
 * 
 * This implementation stores the distance as a 16-bit fixed-point and the two indices as unsigned 24-bit
 * integers (for a similar floating-point implementation, see {@link FloatingPointLongPairwiseDistanceHeap}).
 * 
 * @author Aaron Dunlop
 * @since May 8, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class FixedPointLongPairwiseDistanceHeap extends BaseLongPairwiseDistanceHeap {

    private final static float DIVISOR = 32768f;

    @Override
    public void insert(int index1, int index2, float distance) {
        checkRanges(index1, index2, distance);

        if (distance > 1) {
            throw new IllegalArgumentException("Distance cannot be greater than 1");
        }

        if (index1 > 16777216) {
            throw new IllegalArgumentException("Index out of range: " + index1);
        }

        if (index2 > 16777216) {
            throw new IllegalArgumentException("Index out of range: " + index1);
        }

        // Store the distance in the top 16-bits (so the priority queue will
        // be ordered by distance) and the indices in the lower 48 bits.
        final long l = (((long) (distance * DIVISOR)) << 48) | ((index1 & 0xffffffl) << 24)
                | (index2 & 0xffffff);

        priorityQueue.enqueue(l);
    }

    @Override
    public int[] deleteMin() {
        final long l = priorityQueue.dequeueLong();

        // Discard the top 16 bits (the fixed-point portion) and extract the two 24-bit indices
        final int i = (int) l;
        final int index1 = ((i & 0xffff0000) >> 24);
        final int index2 = (i & 0xffff);
        return new int[] { index1, index2 };
    }

    @Override
    public PairwiseDistance deleteMinDistance() {
        final long l = priorityQueue.dequeueLong();

        // Discard the top 16 bits (the fixed-point portion) and extract the two 24-bit indices
        final int shortDistance = (int) (l >> 48);
        final float distance = shortDistance / DIVISOR;

        final int i = (int) l;
        final int index1 = (int) ((i & 0xffffff000000l) >> 24);
        final int index2 = (i & 0xffffff);
        return new PairwiseDistanceHeap.PairwiseDistance(index1, index2, distance);
    }
}
