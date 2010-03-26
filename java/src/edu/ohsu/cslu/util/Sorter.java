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
public interface Sorter {

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
}
