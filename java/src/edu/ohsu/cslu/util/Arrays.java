package edu.ohsu.cslu.util;

public class Arrays {

    public static void insertGaps(final Object[] oldArray, final int[] gapIndices, final Object[] newArray, final Object gap) {
        int currentGap = 0;
        int oldJ = 0;
        for (int newJ = 0; newJ < newArray.length; newJ++) {
            if (currentGap < gapIndices.length && oldJ == gapIndices[currentGap]) {
                newArray[newJ] = gap;
                currentGap++;
            } else {
                newArray[newJ] = oldArray[oldJ++];
            }
        }
    }

    public static void insertGaps(final Object[] oldArray, final int[] gapIndices, final Object[] newArray, final Object[] gaps) {
        int currentGap = 0;
        int oldJ = 0;
        for (int newJ = 0; newJ < newArray.length; newJ++) {
            if (currentGap < gapIndices.length && oldJ == gapIndices[currentGap]) {
                newArray[newJ] = gaps[currentGap];
                currentGap++;
            } else {
                newArray[newJ] = oldArray[oldJ++];
            }
        }
    }

    /**
     * Sorts a copy of the input array, leaving the original array untouched. Radix sort runs in asymptotic O(n) time, but requires additional storage, slightly larger than the
     * array being sorted.
     * 
     * @param input
     * @return Sorted copy of the input array.
     */
    public static int[] nonDestructiveRadixSort(final int[] input) {
        final int[] sorted = new int[input.length];
        System.arraycopy(input, 0, sorted, 0, input.length);
        radixSort(sorted);
        return sorted;
    }

    /**
     * Sorts the input array. Radix sort runs in asymptotic O(n) time, but requires additional storage, slightly larger than the array being sorted.
     * 
     * @param input
     */
    public static void radixSort(final int[] input) {

        final int BITS = 4;
        final int OFFSET_TABLE_SIZE = 1 << BITS;

        int[] a = input;
        int[] b = new int[input.length];

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

    /**
     * Sorts a parallel array by integer keys, swapping the floating-point array values as well. Radix sort runs in asymptotic O(n) time, but requires additional storage, slightly
     * larger than the arrays being sorted.
     * 
     * @param keys
     * @param f Values
     */
    public static void radixSort(final int[] keys, final float[] f) {

        final int BITS = 4;
        final int OFFSET_TABLE_SIZE = 1 << BITS;

        int[] keyArrayA = keys;
        int[] keyArrayB = new int[keys.length];

        float[] floatArrayA = f;
        float[] floatArrayB = new float[f.length];

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

    /**
     * Sorts a parallel array by integer keys, swapping the floating-point and short array values as well. Radix sort runs in asymptotic O(n) time, but requires additional storage,
     * slightly larger than the arrays being sorted.
     * 
     * @param keys
     * @param f Values
     * @param s Values
     */
    public static void radixSort(final int[] keys, final float[] f, final short[] s) {

        final int BITS = 4;
        final int OFFSET_TABLE_SIZE = 1 << BITS;

        int[] keyArrayA = keys;
        int[] keyArrayB = new int[keys.length];

        float[] floatArrayA = f;
        float[] floatArrayB = new float[f.length];

        short[] shortArrayA = s;
        short[] shortArrayB = new short[s.length];

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
