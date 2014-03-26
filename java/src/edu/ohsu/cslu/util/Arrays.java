/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.util;

/**
 * Various array manipulation routines.
 * 
 * @author Aaron Dunlop
 * @since May 14, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class Arrays {

    public static void fill(final int[][] array, final int val) {
        for (int i = 0; i < array.length; i++) {
            java.util.Arrays.fill(array[i], val);
        }
    }

    public static void fill(final float[][] array, final float val) {
        for (int i = 0; i < array.length; i++) {
            java.util.Arrays.fill(array[i], val);
        }
    }

    public static void fill(final short[][] array, final short val) {
        for (int i = 0; i < array.length; i++) {
            java.util.Arrays.fill(array[i], val);
        }
    }

    public static void fill(final double[][] array, final double val) {
        for (int i = 0; i < array.length; i++) {
            java.util.Arrays.fill(array[i], val);
        }
    }

    public static void fill(final boolean[][] array, final boolean val) {
        for (int i = 0; i < array.length; i++) {
            java.util.Arrays.fill(array[i], val);
        }
    }

    public static void fill(final byte[][] array, final byte val) {
        for (int i = 0; i < array.length; i++) {
            java.util.Arrays.fill(array[i], val);
        }
    }

    public static double sum(final double[] a) {
        if (a == null) {
            return 0.0;
        }
        double result = 0.0;
        for (int i = 0; i < a.length; i++) {
            result += a[i];
        }
        return result;
    }

    /**
     * Sort parallel int[] arrays (stolen from java.util.Arrays and modified to sort parallel arrays)
     */
    public static void sort(final int k[], final int v[]) {
        sort1(k, v, 0, k.length);
    }

    private static void sort1(final int x[], final int v[], final int off, final int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && x[j - 1] > x[j]; j--)
                    swap(x, v, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                final int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        final int val = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= val) {
                if (x[b] == val)
                    swap(x, v, a++, b);
                b++;
            }
            while (c >= b && x[c] >= val) {
                if (x[c] == val)
                    swap(x, v, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, v, b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        final int n = off + len;
        s = java.lang.Math.min(a - off, b - a);
        vecswap(x, v, off, b - s, s);
        s = java.lang.Math.min(d - c, n - d - 1);
        vecswap(x, v, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(x, v, off, s);
        if ((s = d - c) > 1)
            sort1(x, v, n - s, s);
    }

    /**
     * Swaps x[a] with x[b] and f[a] with f[b].
     */
    private static void swap(final int k[], final int v[], final int a, final int b) {
        final int t = k[a];
        k[a] = k[b];
        k[b] = t;

        final int t2 = v[a];
        v[a] = v[b];
        v[b] = t2;
    }

    /**
     * Swaps k[a .. (a+n-1)] with k[b .. (b+n-1)] and v[a .. (a+n-1)] with v[b .. (b+n-1)].
     */
    private static void vecswap(final int k[], final int v[], int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(k, v, a, b);
        }
    }

    /**
     * Sort parallel int[] and short[] arrays (stolen from java.util.Arrays and modified to sort parallel arrays)
     */
    public static void sort(final int k[], final short v[]) {
        sort1(k, v, 0, k.length);
    }

    private static void sort1(final int x[], final short v[], final int off, final int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && x[j - 1] > x[j]; j--)
                    swap(x, v, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                final int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        final int val = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= val) {
                if (x[b] == val)
                    swap(x, v, a++, b);
                b++;
            }
            while (c >= b && x[c] >= val) {
                if (x[c] == val)
                    swap(x, v, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, v, b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        final int n = off + len;
        s = java.lang.Math.min(a - off, b - a);
        vecswap(x, v, off, b - s, s);
        s = java.lang.Math.min(d - c, n - d - 1);
        vecswap(x, v, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(x, v, off, s);
        if ((s = d - c) > 1)
            sort1(x, v, n - s, s);
    }

    /**
     * Swaps x[a] with x[b] and f[a] with f[b].
     */
    private static void swap(final int k[], final short v[], final int a, final int b) {
        final int t = k[a];
        k[a] = k[b];
        k[b] = t;

        final short t2 = v[a];
        v[a] = v[b];
        v[b] = t2;
    }

    /**
     * Swaps k[a .. (a+n-1)] with k[b .. (b+n-1)] and v[a .. (a+n-1)] with v[b .. (b+n-1)].
     */
    private static void vecswap(final int k[], final short v[], int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(k, v, a, b);
        }
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(final int x[], final int a, final int b, final int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    /**
     * Returns the index of the median of the three indexed shorts.
     */
    private static int med3(final short x[], final int a, final int b, final int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    /**
     * Returns the index of the median of the three indexed floats.
     */
    private static int med3(final float x[], final int a, final int b, final int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    /**
     * Sort parallel int[] and float[] arrays based on the values in <code>x</code> (stolen from java.util.Arrays and
     * modified to sort parallel arrays)
     */
    public static void sort(final int[] x, final float[] f) {
        sort1(x, f, 0, x.length);
    }

    private static void sort1(final int[] x, final float[] f, final int off, final int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && x[j - 1] > x[j]; j--)
                    swap(x, f, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                final int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        final int val = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= val) {
                if (x[b] == val)
                    swap(x, f, a++, b);
                b++;
            }
            while (c >= b && x[c] >= val) {
                if (x[c] == val)
                    swap(x, f, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, f, b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        final int n = off + len;
        s = java.lang.Math.min(a - off, b - a);
        vecswap(x, f, off, b - s, s);
        s = java.lang.Math.min(d - c, n - d - 1);
        vecswap(x, f, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(x, f, off, s);
        if ((s = d - c) > 1)
            sort1(x, f, n - s, s);
    }

    /**
     * Swaps x[a] with x[b] and f[a] with f[b].
     */
    private static void swap(final int[] x, final float[] f, final int a, final int b) {
        final int t = x[a];
        x[a] = x[b];
        x[b] = t;

        final float t2 = f[a];
        f[a] = f[b];
        f[b] = t2;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)] and f[a .. (a+n-1)] with f[b .. (b+n-1)].
     */
    private static void vecswap(final int[] x, final float[] f, int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, f, a, b);
        }
    }

    /**
     * Sort parallel short[] and float[] arrays (stolen from java.util.Arrays and modified to sort parallel arrays)
     */
    public static void sort(final short[] x, final float[] f) {
        sort1(x, f, 0, x.length);
    }

    private static void sort1(final short[] x, final float[] f, final int off, final int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && x[j - 1] > x[j]; j--)
                    swap(x, f, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                final int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        final int val = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= val) {
                if (x[b] == val)
                    swap(x, f, a++, b);
                b++;
            }
            while (c >= b && x[c] >= val) {
                if (x[c] == val)
                    swap(x, f, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, f, b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        final int n = off + len;
        s = java.lang.Math.min(a - off, b - a);
        vecswap(x, f, off, b - s, s);
        s = java.lang.Math.min(d - c, n - d - 1);
        vecswap(x, f, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(x, f, off, s);
        if ((s = d - c) > 1)
            sort1(x, f, n - s, s);
    }

    /**
     * Sort parallel short[] and double[] arrays (stolen from java.util.Arrays and modified to sort parallel arrays)
     */
    public static void sort(final short[] x, final double[] f) {
        sort1(x, f, 0, x.length);
    }

    private static void sort1(final short[] x, final double[] f, final int off, final int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && x[j - 1] > x[j]; j--)
                    swap(x, f, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                final int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        final int val = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= val) {
                if (x[b] == val)
                    swap(x, f, a++, b);
                b++;
            }
            while (c >= b && x[c] >= val) {
                if (x[c] == val)
                    swap(x, f, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, f, b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        final int n = off + len;
        s = java.lang.Math.min(a - off, b - a);
        vecswap(x, f, off, b - s, s);
        s = java.lang.Math.min(d - c, n - d - 1);
        vecswap(x, f, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(x, f, off, s);
        if ((s = d - c) > 1)
            sort1(x, f, n - s, s);
    }

    /**
     * Sort parallel float[] and short[] arrays based on the values in <code>x</code> (stolen from java.util.Arrays and
     * modified to sort parallel arrays)
     */
    public static void sort(final float[] x, final short[] p) {
        sort1(x, p, 0, x.length);
    }

    private static void sort1(final float[] x, final short[] p, final int off, final int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && x[j - 1] > x[j]; j--)
                    swap(p, x, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                final int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        final float val = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= val) {
                if (x[b] == val)
                    swap(p, x, a++, b);
                b++;
            }
            while (c >= b && x[c] >= val) {
                if (x[c] == val)
                    swap(p, x, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(p, x, b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        final int n = off + len;
        s = java.lang.Math.min(a - off, b - a);
        vecswap(p, x, off, b - s, s);
        s = java.lang.Math.min(d - c, n - d - 1);
        vecswap(p, x, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(x, p, off, s);
        if ((s = d - c) > 1)
            sort1(x, p, n - s, s);
    }

    /**
     * Swaps x[a] with x[b] and f[a] with f[b].
     */
    private static void swap(final short[] x, final float[] f, final int a, final int b) {
        final short t = x[a];
        x[a] = x[b];
        x[b] = t;

        final float t2 = f[a];
        f[a] = f[b];
        f[b] = t2;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)] and f[a .. (a+n-1)] with f[b .. (b+n-1)].
     */
    private static void vecswap(final short[] x, final float[] f, int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, f, a, b);
        }
    }

    /**
     * Swaps x[a] with x[b] and f[a] with f[b].
     */
    private static void swap(final short[] x, final double[] d, final int a, final int b) {
        final short t = x[a];
        x[a] = x[b];
        x[b] = t;

        final double t2 = d[a];
        d[a] = d[b];
        d[b] = t2;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)] and f[a .. (a+n-1)] with f[b .. (b+n-1)].
     */
    private static void vecswap(final short[] x, final double[] d, int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, d, a, b);
        }
    }

    public static void reverse(final int[] array) {
        for (int i = 0, j = array.length - 1; i < array.length / 2; i++, j--) {
            final int tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    public static void reverse(final short[] array) {
        for (int i = 0, j = array.length - 1; i < array.length / 2; i++, j--) {
            final short tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    public static void reverse(final float[] array) {
        for (int i = 0, j = array.length - 1; i < array.length / 2; i++, j--) {
            final float tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    public static void reverse(final double[] array) {
        for (int i = 0, j = array.length - 1; i < array.length / 2; i++, j--) {
            final double tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    public static void reverse(final long[] array) {
        for (int i = 0, j = array.length - 1; i < array.length / 2; i++, j--) {
            final long tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    public static void reverse(final byte[] array) {
        for (int i = 0, j = array.length - 1; i < array.length / 2; i++, j--) {
            final byte tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
}
