package edu.ohsu.cslu.util;

/**
 * Declares a variety of array sorting methods (mostly somewhat odd ones on specific parallel arrays, used for various NLP tasks).
 * 
 * Implementing classes will implement various sort algorithms (bitonic sort, radix sort, etc.), and some may implement said algorithms on GPU hardware.
 * 
 * This should allow us to easily plug in and compare various sorting implementations within NLP algorithms.
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface Sort {

    /**
     * Sorts the input array in-place.
     * 
     * @param array
     */
    public void sort(final int[] array);

    /**
     * Sorts a copy of the input array, leaving the original array untouched.
     * 
     * @param array
     * @return Sorted copy of the input array.
     */
    public int[] nonDestructiveSort(final int[] array);

    /**
     * Sorts a parallel array by integer keys, swapping the values from the floating-point array as well as keys.
     * 
     * @param keys
     * @param floatValues Values
     */
    public void sort(final int[] keys, final float[] floatValues);

    /**
     * Sorts a parallel array by integer keys, swapping the values from the floating-point and short arrays as well as keys.
     * 
     * @param keys
     * @param floatValues Values
     * @param shortValues Values
     */
    public void sort(final int[] keys, final float[] floatValues, final short[] shortValues);

    /**
     * Sorts a parallel array by integer keys, swapping the values from the floating-point and short arrays as well as keys. Assumes that the keys are already sorted into segments
     * (e.g. [1, 3, 5, 2, 4, 6, 7, 5, 6, 9 10]; where segments end on indices 2 and 6). The default implementation sorts ignoring segment boundaries; other implementations may use
     * the pre-sorted segments for more efficient sorting.
     * 
     * @param keys
     * @param floatValues Values
     * @param shortValues Values
     * @parma segmentBoundaries
     */
    public void sort(final int[] keys, final float[] floatValues, final short[] shortValues, final int[] segmentBoundaries);

    public abstract static class BaseSort implements Sort {

        @Override
        public int[] nonDestructiveSort(final int[] array) {
            final int[] sorted = new int[array.length];
            System.arraycopy(array, 0, sorted, 0, array.length);
            sort(sorted);
            return sorted;
        }

        public void sort(final int[] keys, final float[] floatValues, final short[] shortValues, final int[] segmentBoundaries) {
            sort(keys, floatValues, shortValues);
        }

        /**
         * Swaps x[a] with x[b].
         */
        protected final void swap(final int keys[], final float[] floatValues, final short[] shortValues, final int a, final int b) {
            final int t = keys[a];
            keys[a] = keys[b];
            keys[b] = t;

            if (floatValues != null) {
                final float tmpFloat = floatValues[a];
                floatValues[a] = floatValues[b];
                floatValues[b] = tmpFloat;
            }

            if (shortValues != null) {
                final short tmpShort = shortValues[a];
                shortValues[a] = shortValues[b];
                shortValues[b] = tmpShort;
            }
        }
    }
}
