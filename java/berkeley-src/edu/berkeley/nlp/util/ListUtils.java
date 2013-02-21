package edu.berkeley.nlp.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

public class ListUtils {
    public static ArrayList<Double> toList(final double[] xs) {
        final ArrayList<Double> list = new ArrayList();
        for (final double x : xs)
            list.add(x);
        return list;
    }

    public static <T> ArrayList<T> toList(final Iterable<T> it) {
        if (it instanceof ArrayList)
            return (ArrayList) it;
        final ArrayList<T> list = new ArrayList<T>();
        for (final T x : it)
            list.add(x);
        return list;
    }

    public static <T> ArrayList<T> newList(final T... list) {
        return new ArrayList<T>(Arrays.asList(list));
    }

    public static <T> ArrayList<T> newListFill(final T x, final int n) {
        final ArrayList<T> list = new ArrayList<T>(n);
        for (int i = 0; i < n; i++)
            list.add(x);
        return list;
    }

    public static int maxStringLength(final List<String> strings) {
        int l = 0;
        for (final String s : strings)
            l = Math.max(l, s.length());
        return l;
    }

    public static <T> Map<T, Integer> buildHistogram(final Collection<T> c) {
        final Map<T, Integer> counts = new HashMap<T, Integer>();
        for (final T x : c)
            MapUtils.incr(counts, x);
        return counts;
    }

    public static <T> void randomPermute(final List<T> l, final Random rand) {
        for (int i = 0; i < l.size(); i++) {
            final int j = i + rand.nextInt(l.size() - i);
            final T x = l.get(i);
            l.set(i, l.get(j));
            l.set(j, x);
        }
    }

    public static <T> T getLast(final List<T> l) {
        return get(l, -1);
    }

    public static <T> T get(final List<T> l, final int i) {
        return get(l, i, null);
    }

    public static <T> T get(final List<T> l, int i, final T defValue) {
        if (i < 0)
            i += l.size();
        if (i < 0 || i >= l.size())
            return defValue;
        return l.get(i);
    }

    public static <T> T removeLast(final List<T> l) {
        return l.remove(l.size() - 1);
    }

    public static <T> T getLast(final T[] l) {
        return get(l, -1);
    }

    public static <T> T get(final T[] l, final int i) {
        return get(l, i, null);
    }

    public static <T> T get(final T[] l, int i, final T defValue) {
        if (i < 0)
            i += l.length;
        if (i < 0 || i >= l.length)
            return defValue;
        return l[i];
    }

    public static double get(final double[] l, int i, final double defValue) {
        if (i < 0)
            i += l.length;
        if (i < 0 || i >= l.length)
            return defValue;
        return l[i];
    }

    public static <T> int indexOf(final T[] v, final T x) {
        if (x == null) {
            for (int i = 0; i < v.length; i++)
                if (v[i] == null)
                    return i;
        } else {
            for (int i = 0; i < v.length; i++)
                if (x.equals(v[i]))
                    return i;
        }
        return -1;
    }

    public static int indexOf(final int[] v, final int x) {
        for (int i = 0; i < v.length; i++)
            if (x == v[i])
                return i;
        return -1;
    }

    public static <T> int countOf(final T[] v, final T x) {
        int n = 0;
        if (x == null) {
            for (int i = 0; i < v.length; i++)
                if (v[i] == null)
                    n++;
        } else {
            for (int i = 0; i < v.length; i++)
                if (x.equals(v[i]))
                    n++;
        }
        return n;
    }

    public static int countOf(final boolean[] v, final boolean x) {
        int n = 0;
        for (int i = 0; i < v.length; i++)
            if (x == v[i])
                n++;
        return n;
    }

    // Return the array (0, 1, 2, ..., n-1)
    public static int[] identityMapArray(final int n) {
        final int[] arr = new int[n];
        for (int i = 0; i < n; i++)
            arr[i] = i;
        return arr;
    }

    public static int minIndex(final double[] list) {
        int bi = -1;
        for (int i = 0; i < list.length; i++)
            if (bi == -1 || list[i] < list[bi])
                bi = i;
        return bi;
    }

    public static int maxIndex(final int[] list) {
        int bi = -1;
        for (int i = 0; i < list.length; i++)
            if (bi == -1 || list[i] > list[bi])
                bi = i;
        return bi;
    }

    public static int maxIndex(final double[] list) {
        int bi = -1;
        for (int i = 0; i < list.length; i++)
            if (bi == -1 || list[i] > list[bi])
                bi = i;
        return bi;
    }

    public static double max(final double[] list) {
        double m = Double.NEGATIVE_INFINITY;
        for (final double x : list)
            m = Math.max(m, x);
        return m;
    }

    public static double max(final double[][] mat) {
        double m = Double.NEGATIVE_INFINITY;
        for (final double[] list : mat)
            for (final double x : list)
                m = Math.max(m, x);
        return m;
    }

    public static int max(final int[] list) {
        int m = Integer.MIN_VALUE;
        for (final int x : list)
            m = Math.max(m, x);
        return m;
    }

    public static int sum(final int[] list) {
        int sum = 0;
        for (final int x : list)
            sum += x;
        return sum;
    }

    public static double mean(final double[] list) {
        return sum(list) / list.length;
    }

    public static double sum(final double[] list) {
        double sum = 0;
        for (final double x : list)
            sum += x;
        return sum;
    }

    public static double sum(final double[][] list) {
        double sum = 0;
        for (final double[] x : list)
            sum += sum(x);
        return sum;
    }

    public static double sum(final List<Double> list) {
        double sum = 0;
        for (final double x : list)
            sum += x;
        return sum;
    }

    public static double[] expMut(final double[] list) {
        for (int i = 0; i < list.length; i++)
            list[i] = Math.exp(list[i]);
        return list;
    }

    public static double[] exp(final double[] list) {
        final double[] newlist = new double[list.length];
        for (int i = 0; i < list.length; i++)
            newlist[i] = Math.exp(list[i]);
        return newlist;
    }

    public static double[] log(final double[] list) {
        final double[] newlist = new double[list.length];
        for (int i = 0; i < list.length; i++)
            newlist[i] = Math.log(list[i]);
        return newlist;
    }

    // Return a permuted array.
    // Example:
    // data = (A, B, C), perm = (2, 0, 1)
    // newData = (C, A, B)
    public static int[] applyPermutation(final int[] data, final int[] perm) {
        assert data.length == perm.length;
        final int[] newData = new int[data.length];
        for (int i = 0; i < data.length; i++)
            newData[i] = data[perm[i]];
        return newData;
    }

    public static double[] applyPermutation(final double[] data, final int[] perm) {
        assert data.length == perm.length;
        final double[] newData = new double[data.length];
        for (int i = 0; i < data.length; i++)
            newData[i] = data[perm[i]];
        return newData;
    }

    public static <T> T[] applyPermutation(final T[] data, final int[] perm) {
        assert data.length == perm.length;
        final T[] newData = newArray(data);
        for (int i = 0; i < data.length; i++)
            newData[i] = data[perm[i]];
        return newData;
    }

    public static double[] applyInversePermutation(final double[] data, final int[] perm) {
        assert data.length == perm.length;
        final double[] newData = new double[data.length];
        for (int i = 0; i < data.length; i++)
            newData[perm[i]] = data[i];
        return newData;
    }

    public static int[] inversePermutation(final int[] perm) {
        // perm could be partial (with -1 entries)
        final int[] newperm = newInt(perm.length, -1);
        for (int i = 0; i < perm.length; i++)
            if (perm[i] != -1)
                newperm[perm[i]] = i;
        return newperm;
    }

    public static void assertIsPermutation(final int[] perm) {
        final boolean hit[] = new boolean[perm.length];
        for (final int i : perm) {
            assert !hit[i];
            hit[i] = true;
        }
    }

    public static int[] append(final int[] a, final int[] b) {
        final int[] c = new int[a.length + b.length];
        int j = 0;
        for (int i = 0; i < a.length; i++, j++)
            c[j] = a[i];
        for (int i = 0; i < b.length; i++, j++)
            c[j] = b[i];
        return c;
    }

    public static double[] append(final double[] a, final double[] b) {
        final double[] c = new double[a.length + b.length];
        int j = 0;
        for (int i = 0; i < a.length; i++, j++)
            c[j] = a[i];
        for (int i = 0; i < b.length; i++, j++)
            c[j] = b[i];
        return c;
    }

    public static <T> T[] append(final T[] a, final T[] b) {
        final T[] c = newArray(a.length + b.length, a[0]);
        int j = 0;
        for (int i = 0; i < a.length; i++, j++)
            c[j] = a[i];
        for (int i = 0; i < b.length; i++, j++)
            c[j] = b[i];
        return c;
    }

    public static Integer[] toObjArray(final int[] v) {
        final Integer[] newv = new Integer[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = v[i];
        return newv;
    }

    public static Double[] toObjArray(final double[] v) {
        final Double[] newv = new Double[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = v[i];
        return newv;
    }

    public static Double[][] toObjArray(final double[][] v) {
        final Double[][] newv = new Double[v.length][];
        for (int i = 0; i < v.length; i++) {
            newv[i] = new Double[v[i].length];
            for (int j = 0; j < v[i].length; j++)
                newv[i][j] = v[i][j];
        }
        return newv;
    }

    public static Integer[][] toObjArray(final int[][] v) {
        final Integer[][] newv = new Integer[v.length][];
        for (int i = 0; i < v.length; i++) {
            newv[i] = new Integer[v[i].length];
            for (int j = 0; j < v[i].length; j++)
                newv[i][j] = v[i][j];
        }
        return newv;
    }

    public static <T> Object[] toObjectArray(final T[] v) {
        final Object[] newv = new Object[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = v[i];
        return newv;
    }

    public static boolean[] shallowClone(final boolean[] v) {
        if (v == null)
            return null;
        return v.clone();
    }

    public static int[] shallowClone(final int[] v) {
        if (v == null)
            return null;
        return v.clone();
    }

    public static double[][] shallowClone(final double[][] v) {
        if (v == null)
            return null;
        final double[][] newv = new double[v.length][];
        for (int i = 0; i < v.length; i++)
            newv[i] = shallowClone(v[i]);
        return newv;
    }

    public static double[] shallowClone(final double[] v) {
        if (v == null)
            return null;
        return v.clone();
    }

    public static <T> T[][] shallowClone(final T[][] v) {
        if (v == null)
            return null;
        final T[][] newv = newArray(v);
        for (int i = 0; i < v.length; i++) {
            newv[i] = newArray(v[i]);
            for (int j = 0; j < v[i].length; j++)
                newv[i][j] = v[i][j]; // Don't make a copy
        }
        return newv;
    }

    public static <T> T[] shallowClone(final T[] v) {
        if (v == null)
            return null;
        final T[] newv = newArray(v);
        for (int i = 0; i < v.length; i++)
            newv[i] = v[i]; // Don't make a copy
        return newv;
    }

    public static <T> T[] deepClone(final T[] v) {
        if (v == null)
            return null;
        final T[] newv = newArray(v);
        for (int i = 0; i < v.length; i++)
            newv[i] = ((DeepCloneable<T>) v[i]).deepClone();
        return newv;
    }

    public static <T> List<T> deepClone(final List<T> v) {
        if (v == null)
            return null;
        final List<T> newv = new ArrayList();
        for (final T x : v)
            newv.add(((DeepCloneable<T>) x).deepClone());
        return newv;
    }

    public static <T> T[] newArray(final T[] v) {
        return (T[]) Array.newInstance(v.getClass().getComponentType(), v.length);
    }

    public static int[][] newInt(final int nr, final int nc, final int x) {
        final int[][] v = new int[nr][nc];
        for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++)
                v[r][c] = x;
        return v;
    }

    public static double[][] newDouble(final int nr, final int nc, final double x) {
        final double[][] v = new double[nr][nc];
        for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++)
                v[r][c] = x;
        return v;
    }

    public static double[][] newDouble(final int nr, final int[] nc, final double x) {
        final double[][] v = new double[nr][];
        for (int r = 0; r < nr; r++) {
            v[r] = new double[nc[r]];
            for (int c = 0; c < nc[r]; c++)
                v[r][c] = x;
        }
        return v;
    }

    public static double[][][] newDouble(final int nr, final int nc, final int nk, final double x) {
        final double[][][] v = new double[nr][nc][nk];
        for (int r = 0; r < nr; r++)
            for (int c = 0; c < nc; c++)
                for (int k = 0; k < nk; k++)
                    v[r][c][k] = x;
        return v;
    }

    public static double[] newDouble(final int n, final double x) {
        final double[] v = new double[n];
        Arrays.fill(v, x);
        return v;
    }

    public static int[] newInt(final int n, final int x) {
        final int[] v = new int[n];
        Arrays.fill(v, x);
        return v;
    }

    public static interface Generator<T> {
        public T generate(int i);
    }

    public static <T> T[] newArray(final int n, final Class c, final Generator<T> gen) {
        final T[] a = (T[]) Array.newInstance(c, n);
        for (int i = 0; i < n; i++)
            a[i] = gen.generate(i);
        return a;
    }

    public static <T> T[] newArray(final int n, final T x) {
        final T[] a = (T[]) Array.newInstance(x.getClass(), n);
        for (int i = 0; i < n; i++)
            a[i] = x;
        return a;
    }

    public static double[] mult(final double f, final double[] vec) {
        final double[] newVec = new double[vec.length];
        for (int i = 0; i < vec.length; i++)
            newVec[i] = f * vec[i];
        return newVec;
    }

    public static void multMut(final double[] vec, final double f) {
        for (int i = 0; i < vec.length; i++)
            vec[i] *= f;
    }

    public static void multMut(final double[][] mat, final double f) {
        for (final double[] vec : mat)
            multMut(vec, f);
    }

    // v1 += factor * v2
    public static double[] incr(final double[] v1, final double factor, final double[] v2) {
        for (int i = 0; i < v1.length; i++)
            v1[i] += factor * v2[i];
        return v1;
    }

    public static double[] incr(final double[] v1, final double x) {
        for (int i = 0; i < v1.length; i++)
            v1[i] += x;
        return v1;
    }

    public static int[] set(final int[] v, final int x) {
        for (int i = 0; i < v.length; i++)
            v[i] = x;
        return v;
    }

    public static int[] set(final int[] v, final int x[], final int n) {
        for (int i = 0; i < n; i++)
            v[i] = x[i];
        return v;
    }

    public static int[] set(final int[] v, final int x[]) {
        return set(v, x, v.length);
    }

    public static double[] set(final double[] v, final double x[]) {
        for (int i = 0; i < v.length; i++)
            v[i] = x[i];
        return v;
    }

    public static double[] set(final double[] v, final double x) {
        for (int i = 0; i < v.length; i++)
            v[i] = x;
        return v;
    }

    public static double[][] set(final double[][] v, final double[][] x) {
        for (int i = 0; i < v.length; i++)
            for (int j = 0; j < v[i].length; j++)
                v[i][j] = x[i][j];
        return v;
    }

    public static double[][] set(final double[][] v, final double x) {
        for (int i = 0; i < v.length; i++)
            set(v[i], x);
        return v;
    }

    public static double[][][] set(final double[][][] v, final double x) {
        for (int i = 0; i < v.length; i++)
            set(v[i], x);
        return v;
    }

    public static double[][][][] set(final double[][][][] v, final double x) {
        for (int i = 0; i < v.length; i++)
            set(v[i], x);
        return v;
    }

    public static double[][][][][] set(final double[][][][][] v, final double x) {
        for (int i = 0; i < v.length; i++)
            set(v[i], x);
        return v;
    }

    public static double[] add(final double[] v1, final double[] v2) {
        final double[] sumv = new double[v1.length];
        for (int i = 0; i < v1.length; i++)
            sumv[i] = v1[i] + v2[i];
        return sumv;
    }

    public static double[] addMut(final double[] v1, final double[] v2) {
        for (int i = 0; i < v1.length; i++)
            v1[i] += v2[i];
        return v1;
    }

    public static double[] add(final double[] v1, final int[] v2) {
        final double[] sumv = new double[v1.length];
        for (int i = 0; i < v1.length; i++)
            sumv[i] = v1[i] + v2[i];
        return sumv;
    }

    public static double[] sub(final double[] v1, final double[] v2) {
        final double[] sumv = new double[v1.length];
        for (int i = 0; i < v1.length; i++)
            sumv[i] = v1[i] - v2[i];
        return sumv;
    }

    public static double[] sub(final double[] v1, final double x) {
        return add(v1, -x);
    }

    public static double[] add(final double[] v1, final double x) {
        final double[] sumv = new double[v1.length];
        for (int i = 0; i < v1.length; i++)
            sumv[i] = v1[i] + x;
        return sumv;
    }

    public static double[] mult(final double[] v1, final double[] v2) {
        final double[] v = new double[v1.length];
        for (int i = 0; i < v1.length; i++)
            v[i] = v1[i] * v2[i];
        return v;
    }

    public static double[] multMut(final double[] v1, final double[] v2) {
        for (int i = 0; i < v1.length; i++)
            v1[i] *= v2[i];
        return v1;
    }

    public static double dot(final double[] v1, final double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++)
            sum += v1[i] * v2[i];
        return sum;
    }

    public static double[] sq(final double[] v) {
        final double[] newv = new double[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = v[i] * v[i];
        return newv;
    }

    public static double[] sqrt(final double[] v) {
        final double[] newv = new double[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = Math.sqrt(v[i]);
        return newv;
    }

    public static double[] reverse(final double[] v) {
        final double[] newv = new double[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = v[v.length - i - 1];
        return newv;
    }

    public static int[] reverse(final int[] v) {
        final int[] newv = new int[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = v[v.length - i - 1];
        return newv;
    }

    // public static int[] toArray(List<Integer> list) {
    // int[] array = new int[list.size()];
    // for(int i = 0; i < array.length; i++)
    // array[i] = list.get(i);
    // return array;
    // }
    // public static double[] toArray(List<Double> list) {
    // double[] array = new double[list.size()];
    // for(int i = 0; i < array.length; i++)
    // array[i] = list.get(i);
    // return array;
    // }
    // public static String[] toArray(List<String> list) {
    // String[] data = new String[list.size()];
    // for(int i = 0; i < data.length; i++)
    // data[i] = list.get(i);
    // return data;
    // }
    // public static int[][] toArray(List<int[]> list) {
    // int[][] data = new int[list.size()][];
    // for(int i = 0; i < data.length; i++)
    // data[i] = list.get(i);
    // return data;
    // }

    public static double[] concat(final double[] v1, final double[] v2) {
        final double[] v = new double[v1.length + v2.length];
        for (int i = 0; i < v1.length; i++)
            v[i] = v1[i];
        for (int i = 0; i < v2.length; i++)
            v[v1.length + i] = v2[i];
        return v;
    }

    public static <T> T[] concat(final T[] v1, final T[] v2) {
        final T[] v = newArray(v1.length + v2.length, v1.length > 0 ? v1[0] : v2[0]);
        for (int i = 0; i < v1.length; i++)
            v[i] = v1[i];
        for (int i = 0; i < v2.length; i++)
            v[v1.length + i] = v2[i];
        return v;
    }

    // Take subsequence [start, end)
    public static String[] subArray(final String[] v, final int start) {
        return subArray(v, start, v.length);
    }

    public static String[] subArray(final String[] v, final int start, final int end) {
        final String[] subv = new String[end - start];
        for (int i = start; i < end; i++)
            subv[i - start] = v[i];
        return subv;
    }

    public static double[] subArray(final double[] v, final int start) {
        return subArray(v, start, v.length);
    }

    public static double[] subArray(final double[] v, final int start, final int end) {
        final double[] subv = new double[end - start];
        for (int i = start; i < end; i++)
            subv[i - start] = v[i];
        return subv;
    }

    public static int[] subArray(final int[] v, final int start) {
        return subArray(v, start, v.length);
    }

    public static int[] subArray(final int[] v, final int start, final int end) {
        final int[] subv = new int[end - start];
        for (int i = start; i < end; i++)
            subv[i - start] = v[i];
        return subv;
    }

    public static <T> T[] subArray(final T[] v, final int start, final int end) {
        final T[] subv = newArray(end - start, v[0]);
        for (int i = start; i < end; i++)
            subv[i - start] = v[i];
        return subv;
    }

    public static <T> T[] subArray(final T[] v, final List<Integer> indices) {
        final T[] newv = newArray(indices.size(), v[0]);
        for (int i = 0; i < indices.size(); i++)
            newv[i] = v[indices.get(i)];
        return newv;
    }

    public static <T> List<T> subArray(final List<T> v, final int[] indices) {
        final List<T> newv = new ArrayList();
        for (final int i : indices)
            if (i != -1)
                newv.add(v.get(i));
        return newv;
    }

    // If bounds are invalid, clip them.
    public static <T> List<T> subList(final List<T> list, final int start) {
        return subList(list, start, list.size());
    }

    public static <T> List<T> subList(final List<T> list, int start, int end) {
        if (end < 0)
            end += list.size();
        if (start < 0)
            start += list.size();
        start = bound(start, 0, list.size());
        end = bound(end, 0, list.size());
        return list.subList(start, end);
    }

    private static int bound(final int x, final int lower, final int upper) {
        if (x < lower)
            return lower;
        if (x > upper)
            return upper;
        return x;
    }

    public static <T> void partialSort(final List<T> list, final int numTop, final Comparator<? super T> c) {
        final Object[] a = list.toArray();
        partialSort(a, numTop, (Comparator) c);
        final ListIterator<T> i = list.listIterator();
        for (int j = 0; j < a.length; j++) {
            i.next();
            i.set((T) a[j]);
        }
    }

    public static <T> void partialSort(final T[] list, final int numTop, final Comparator<? super T> c) {
        // Select out the numTop-th ranked element
        // TODO
        Arrays.sort(list, c); // For now, sort everything
    }

    // Return the indices: the first element contains the smallest
    public static int[] sortedIndices(final double[] list, final boolean reverse) {
        final int n = list.length;
        // Sort
        final List<Pair<Double, Integer>> pairList = new ArrayList<Pair<Double, Integer>>(n);
        for (int i = 0; i < n; i++)
            pairList.add(new Pair<Double, Integer>(list[i], i));
        Collections.sort(pairList, reverse ? new Pair.ReverseFirstComparator<Double, Integer>()
                : new Pair.FirstComparator<Double, Integer>());
        // Extract the indices
        final int[] indices = new int[n];
        for (int i = 0; i < n; i++)
            indices[i] = pairList.get(i).getSecond();
        return indices;
    }

    public static int[] sortedIndices(final int[] list, final boolean reverse) {
        final int n = list.length;
        // Sort
        final List<Pair<Integer, Integer>> pairList = new ArrayList<Pair<Integer, Integer>>(n);
        for (int i = 0; i < n; i++)
            pairList.add(new Pair<Integer, Integer>(list[i], i));
        Collections.sort(pairList, reverse ? new Pair.ReverseFirstComparator<Integer, Integer>()
                : new Pair.FirstComparator<Integer, Integer>());
        // Extract the indices
        final int[] indices = new int[n];
        for (int i = 0; i < n; i++)
            indices[i] = pairList.get(i).getSecond();
        return indices;
    }

    public static <T> int[] toInt(final T[] v) {
        if (v == null)
            return null;
        final int[] newv = new int[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = ((Integer) v[i]);
        return newv;
    }

    public static int[] toInt(final boolean[] v) {
        final int[] newv = new int[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = v[i] ? 1 : 0;
        return newv;
    }

    public static double[] toDouble(final int[] v) {
        final double[] newv = new double[v.length];
        for (int i = 0; i < v.length; i++)
            newv[i] = v[i];
        return newv;
    }

    public static boolean equals(final int[] a, final int[] b) {
        if (a.length != b.length)
            return false;
        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                return false;
        return true;
    }

    public static double[] getCol(final double[][] mat, final int c) {
        final double[] v = new double[mat.length];
        for (int r = 0; r < v.length; r++)
            v[r] = mat[r][c];
        return v;
    }

    public static void setCol(final double[][] mat, final int c, final double[] v) {
        for (int r = 0; r < v.length; r++)
            mat[r][c] = v[r];
    }
}
