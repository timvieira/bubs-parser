package edu.ohsu.cslu.datastructs;

import java.util.NoSuchElementException;

import edu.ohsu.cslu.alignment.multiple.IterativePairwiseAligner;

/**
 * Represents a priority queue of distance measures. Useful for such tasks as progressive pairwise
 * alignment, in which we align the closest two sequences, followed by the next closest unaligned,
 * and so on (see {@link IterativePairwiseAligner}.
 * 
 * @author Aaron Dunlop
 * @since May 8, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface PairwiseDistanceHeap
{
    /**
     * Inserts a new entry into this heap.
     * 
     * @param index1 Reference to the first element
     * @param index2 Reference to the second element
     * @param distance Distance between the two elements. The distance must be between 0 and 1
     *            inclusive.
     * @throws IllegalArgumentException if distance is less than 0
     */
    public void insert(final int index1, final int index2, final float distance);

    /**
     * Retrieves and deletes the closest distance stored in the heap. The pair of indices are
     * returned as a two-element int array.
     * 
     * @return the indices of the closest pair as a two-element int array.
     * @throws NoSuchElementException if the heap is empty.
     */
    public int[] deleteMin();

    /**
     * Retrieves and deletes the closest distance stored in the heap. Note that {@link #deleteMin()}
     * is generally more efficient if you do not need the actual distance.
     * 
     * @return the closest {@link PairwiseDistance}
     * @throws NoSuchElementException if the heap is empty.
     */
    public PairwiseDistance deleteMinDistance();

    /**
     * True if this heap is empty
     * 
     * @return true if this heap is empty
     */
    public boolean isEmpty();

    /**
     * Returns the number of elements in this heap
     * 
     * @return the number of elements in this heap
     */
    public int size();

    public static class PairwiseDistance
    {
        public int index1;
        public int index2;
        public float distance;

        PairwiseDistance(int index1, int index2, float distance)
        {
            this.index1 = index1;
            this.index2 = index2;
            this.distance = distance;
        }
    }
}
