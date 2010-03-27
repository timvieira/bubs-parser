package edu.ohsu.cslu.util;

import java.util.Arrays;

/**
 * Implements {@link Sort} using a bitonic sort. Bitonic sort is intended for parallel implementation, and is not very efficient when single-threaded.
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class BitonicSort extends Sort.BaseSort implements Sort {

    private final static boolean ASCENDING = true, DESCENDING = false;

    @Override
    public void sort(final int[] array) {
        sort(array, null, null);
    }

    @Override
    public void sort(final int[] keys, final float[] floatValues) {
        sort(keys, floatValues, null);
    }

    @Override
    public void sort(final int[] keys, final float[] floatValues, final short[] shortValues) {
        // Bitonic sort requires an array of a power-of-2 length. So we allocate such an array, copy the original array into it, fill the remainder with Integer.MAX_VALUE, sort,
        // and then copy the result back to the original array. Note that this could break if the original array contains any Integer.MAX_VALUE entries.
        final int[] powerOf2Keys = new int[Math.nextPowerOf2(keys.length)];
        System.arraycopy(keys, 0, powerOf2Keys, 0, keys.length);
        Arrays.fill(powerOf2Keys, keys.length, powerOf2Keys.length, Integer.MAX_VALUE);

        float[] powerOf2FloatValues = null;
        if (floatValues != null) {
            powerOf2FloatValues = new float[Math.nextPowerOf2(keys.length)];
            System.arraycopy(floatValues, 0, powerOf2FloatValues, 0, keys.length);
        }

        short[] powerOf2ShortValues = null;
        if (shortValues != null) {
            powerOf2ShortValues = new short[Math.nextPowerOf2(keys.length)];
            System.arraycopy(shortValues, 0, powerOf2ShortValues, 0, keys.length);
        }

        bitonicSort(powerOf2Keys, powerOf2FloatValues, powerOf2ShortValues, 0, powerOf2Keys.length, ASCENDING);

        System.arraycopy(powerOf2Keys, 0, keys, 0, keys.length);
        if (floatValues != null) {
            System.arraycopy(powerOf2FloatValues, 0, floatValues, 0, floatValues.length);
        }
        if (shortValues != null) {
            System.arraycopy(powerOf2ShortValues, 0, shortValues, 0, shortValues.length);
        }
    }

    private void bitonicSort(final int[] keys, final float[] floatValues, final short[] shortValues, final int start, final int n, final boolean direction) {
        if (n > 1) {
            final int k = n / 2;
            bitonicSort(keys, floatValues, shortValues, start, k, ASCENDING);
            bitonicSort(keys, floatValues, shortValues, start + k, k, DESCENDING);
            bitonicMerge(keys, floatValues, shortValues, start, n, direction);
        }
    }

    private void bitonicMerge(final int[] keys, final float[] floatValues, final short[] shortValues, final int start, final int n, final boolean direction) {
        if (n > 1) {
            final int k = n / 2;

            for (int i = start; i < start + k; i++) {
                if (direction == (keys[i] > keys[i + k])) {
                    swap(keys, floatValues, shortValues, i, i + k);
                }
            }

            bitonicMerge(keys, floatValues, shortValues, start, k, direction);
            bitonicMerge(keys, floatValues, shortValues, start + k, k, direction);
        }
    }
}
