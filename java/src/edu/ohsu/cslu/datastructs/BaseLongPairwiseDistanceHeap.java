package edu.ohsu.cslu.datastructs;

import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;

/**
 * Implements {@link PairwiseDistanceHeap}, packing all three fields of each element (distance and two
 * indices) into a single long for memory efficiency.
 * 
 * @author Aaron Dunlop
 * @since May 8, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class BaseLongPairwiseDistanceHeap implements PairwiseDistanceHeap {

    protected final LongHeapPriorityQueue priorityQueue = new LongHeapPriorityQueue();

    protected void checkRanges(int index1, int index2, float distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("Distance cannot be less than 0");
        }

        if (Float.isInfinite(distance) || Float.isNaN(distance)) {
            throw new IllegalArgumentException("Distance cannot be infinite or NaN");
        }

        if (index1 < 0 || index2 < 0) {
            throw new IllegalArgumentException("Indices < 0 are not supported");
        }
    }

    @Override
    public boolean isEmpty() {
        return priorityQueue.isEmpty();
    }

    @Override
    public int size() {
        return priorityQueue.size();
    }

}
