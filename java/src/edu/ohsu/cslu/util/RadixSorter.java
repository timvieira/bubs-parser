package edu.ohsu.cslu.util;

/**
 * Implements {@link Sorter} using the radix sort algorithm. Radix sort runs in asymptotic O(n) time, but requires additional storage, slightly larger than the array being sorted.
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class RadixSorter implements Sorter {

    @Override
    public void sort(final int[] array) {

        final int BITS = 4;
        final int OFFSET_TABLE_SIZE = 1 << BITS;

        int[] a = array;
        int[] b = new int[array.length];

        int shift = 0;
        for (int mask = ~(-1 << BITS); mask != 0; mask <<= BITS, shift += BITS) {

            final int[] offsetTable = new int[OFFSET_TABLE_SIZE];

            for (int p = 0; p < a.length; ++p) {
                final int key = (a[p] & mask) >> shift;
                ++offsetTable[key];
            }

            for (int i = 1; i < offsetTable.length; i++) {
                offsetTable[i] += offsetTable[i - 1];
            }

            for (int i = a.length - 1; i >= 0; i--) {
                final int key = (a[i] & mask) >> shift;
                --offsetTable[key];
                b[offsetTable[key]] = a[i];
            }

            final int[] tmp = a;
            a = b;
            b = tmp;
        }

        // if (input != a) {
        // System.arraycopy(a, 0, input, 0, input.length);
        // }
    }

    @Override
    public int[] nonDestructiveSort(final int[] array) {
        final int[] sorted = new int[array.length];
        System.arraycopy(array, 0, sorted, 0, array.length);
        sort(sorted);
        return sorted;
    }

    @Override
    public void sort(final int[] keys, final float[] floatValues) {

        final int BITS = 4;
        final int OFFSET_TABLE_SIZE = 1 << BITS;

        int[] keyArrayA = keys;
        int[] keyArrayB = new int[keys.length];

        float[] floatArrayA = floatValues;
        float[] floatArrayB = new float[floatValues.length];

        int shift = 0;
        for (int mask = ~(-1 << BITS); mask != 0; mask <<= BITS, shift += BITS) {

            final int[] offsetTable = new int[OFFSET_TABLE_SIZE];

            for (int p = 0; p < keyArrayA.length; ++p) {
                final int key = (keyArrayA[p] & mask) >> shift;
                ++offsetTable[key];
            }

            for (int i = 1; i < offsetTable.length; i++) {
                offsetTable[i] += offsetTable[i - 1];
            }

            for (int i = keyArrayA.length - 1; i >= 0; i--) {
                final int key = (keyArrayA[i] & mask) >> shift;
                --offsetTable[key];
                keyArrayB[offsetTable[key]] = keyArrayA[i];
                floatArrayB[offsetTable[key]] = floatArrayA[i];
            }

            final int[] keyTmp = keyArrayA;
            keyArrayA = keyArrayB;
            keyArrayB = keyTmp;

            final float[] floatTmp = floatArrayA;
            floatArrayA = floatArrayB;
            floatArrayB = floatTmp;
        }

        // if (keys != keyArrayA) {
        // System.arraycopy(keyArrayA, 0, keys, 0, keys.length);
        // System.arraycopy(floatArrayA, 0, f, 0, f.length);
        // System.arraycopy(shortArrayA, 0, s, 0, s.length);
        // }
    }

    @Override
    public void sort(final int[] keys, final float[] floatValues, final short[] shortValues) {

        final int BITS = 4;
        final int OFFSET_TABLE_SIZE = 1 << BITS;

        int[] keyArrayA = keys;
        int[] keyArrayB = new int[keys.length];

        float[] floatArrayA = floatValues;
        float[] floatArrayB = new float[floatValues.length];

        short[] shortArrayA = shortValues;
        short[] shortArrayB = new short[shortValues.length];

        int shift = 0;
        for (int mask = ~(-1 << BITS); mask != 0; mask <<= BITS, shift += BITS) {

            final int[] offsetTable = new int[OFFSET_TABLE_SIZE];

            for (int p = 0; p < keyArrayA.length; ++p) {
                final int key = (keyArrayA[p] & mask) >> shift;
                ++offsetTable[key];
            }

            for (int i = 1; i < offsetTable.length; i++) {
                offsetTable[i] += offsetTable[i - 1];
            }

            for (int i = keyArrayA.length - 1; i >= 0; i--) {
                final int key = (keyArrayA[i] & mask) >> shift;
                --offsetTable[key];
                keyArrayB[offsetTable[key]] = keyArrayA[i];
                floatArrayB[offsetTable[key]] = floatArrayA[i];
                shortArrayB[offsetTable[key]] = shortArrayA[i];
            }

            final int[] keyTmp = keyArrayA;
            keyArrayA = keyArrayB;
            keyArrayB = keyTmp;

            final float[] floatTmp = floatArrayA;
            floatArrayA = floatArrayB;
            floatArrayB = floatTmp;

            final short[] shortTmp = shortArrayA;
            shortArrayA = shortArrayB;
            shortArrayB = shortTmp;
        }

        // if (keys != keyArrayA) {
        // System.arraycopy(keyArrayA, 0, keys, 0, keys.length);
        // System.arraycopy(floatArrayA, 0, f, 0, f.length);
        // System.arraycopy(shortArrayA, 0, s, 0, s.length);
        // }
    }
}
