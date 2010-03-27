package edu.ohsu.cslu.util;

import edu.ohsu.cslu.util.Sort.BaseSort;

public class QuickSort extends BaseSort {

    @Override
    public void sort(final int[] array) {
        sort(array, null, null, 0, array.length);
    }

    @Override
    public void sort(final int[] keys, final float[] floatValues) {
        sort(keys, floatValues, null, 0, keys.length);
    }

    @Override
    public void sort(final int[] keys, final float[] floatValues, final short[] shortValues) {
        sort(keys, floatValues, shortValues, 0, keys.length);
    }

    private void sort(final int[] keys, final float[] floatValues, final short[] shortValues, final int off, final int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && keys[j - 1] > keys[j]; j--)
                    swap(keys, floatValues, shortValues, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                final int s = len / 8;
                l = med3(keys, l, l + s, l + 2 * s);
                m = med3(keys, m - s, m, m + s);
                n = med3(keys, n - 2 * s, n - s, n);
            }
            m = med3(keys, l, m, n); // Mid-size, med of 3
        }
        final int v = keys[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && keys[b] <= v) {
                if (keys[b] == v)
                    swap(keys, floatValues, shortValues, a++, b);
                b++;
            }
            while (c >= b && keys[c] >= v) {
                if (keys[c] == v)
                    swap(keys, floatValues, shortValues, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(keys, floatValues, shortValues, b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        final int n = off + len;
        s = java.lang.Math.min(a - off, b - a);
        vecswap(keys, floatValues, shortValues, off, b - s, s);
        s = java.lang.Math.min(d - c, n - d - 1);
        vecswap(keys, floatValues, shortValues, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort(keys, floatValues, shortValues, off, s);
        if ((s = d - c) > 1)
            sort(keys, floatValues, shortValues, n - s, s);
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private void vecswap(final int keys[], final float[] floatValues, final short[] shortValues, int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++)
            swap(keys, floatValues, shortValues, a, b);
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(final int keys[], final int a, final int b, final int c) {
        return (keys[a] < keys[b] ? (keys[b] < keys[c] ? b : keys[a] < keys[c] ? c : a) : (keys[b] > keys[c] ? b : keys[a] > keys[c] ? c : a));
    }

}
