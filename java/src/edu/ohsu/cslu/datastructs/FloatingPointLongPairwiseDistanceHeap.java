package edu.ohsu.cslu.datastructs;

/**
 * Implements {@link PairwiseDistanceHeap}, packing all three fields of each element (distance and two
 * indices) into a single long for memory efficiency.
 * 
 * This implementation stores the distance as a 32-bit float and the two indices as unsigned 16-bit integers.
 * Since we only store positive distances between 0 and 1, it's safe to do ordinal comparison using the bits
 * of an IEEE floating-point representation.
 * 
 * For a similar fixed-point implementation which allows indices larger than 65536, see
 * {@link FixedPointLongPairwiseDistanceHeap}).
 * 
 * @author Aaron Dunlop
 * @since May 8, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class FloatingPointLongPairwiseDistanceHeap extends BaseLongPairwiseDistanceHeap {

    @Override
    public void insert(int index1, int index2, float distance) {
        checkRanges(index1, index2, distance);

        if (index1 > 65536) {
            throw new IllegalArgumentException("Index out of range: " + index1);
        }

        if (index2 > 65536) {
            throw new IllegalArgumentException("Index out of range: " + index1);
        }

        // Store the floating-point distance in the top 32-bits (so the priority queue will
        // be ordered by distance) and the indices in the lower 32 bits.
        long l = ((long) Float.floatToIntBits(distance) << 32) | ((index1 & 0xffff) << 16)
                | (index2 & 0xffff);

        priorityQueue.enqueue(l);
    }

    @Override
    public int[] deleteMin() {
        final long l = priorityQueue.dequeueLong();

        // Discard the top 32 bits (the floating-point portion) and extract the two 16-bit indices
        final int i = (int) l;
        final int index1 = ((i & 0xffff0000) >> 16);
        final int index2 = (i & 0xffff);
        return new int[] { index1, index2 };
    }

    @Override
    public PairwiseDistance deleteMinDistance() {
        final long l = priorityQueue.dequeueLong();

        // Treat the top 32 bits (the floating-point portion) as the distance and extract the two
        // 16-bit indices
        final int intDistance = (int) (l >> 32);
        final float distance = Float.intBitsToFloat(intDistance);

        final int i = (int) l;
        final int index1 = ((i & 0xffff0000) >> 16);
        final int index2 = (i & 0xffff);
        return new PairwiseDistanceHeap.PairwiseDistance(index1, index2, distance);
    }
}
