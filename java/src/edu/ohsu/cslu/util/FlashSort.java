package edu.ohsu.cslu.util;

import edu.ohsu.cslu.util.Sort.BaseSort;

public class FlashSort extends BaseSort {

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
        partialFlashSort(keys, floatValues, shortValues);
        insertionSort(keys, floatValues, shortValues);
    }

    private void insertionSort(final int[] keys, final float[] floatValues, final short[] shortValues) {
        int i, j, tmpKey;
        float tmpFloat = 0;
        short tmpShort = 0;

        for (i = keys.length - 3; i >= 0; i--) {
            if (keys[i + 1] < keys[i]) {
                tmpKey = keys[i];
                if (floatValues != null) {
                    tmpFloat = floatValues[i];
                }
                if (shortValues != null) {
                    tmpShort = shortValues[i];
                }

                j = i;

                while (keys[j + 1] < tmpKey) {
                    keys[j] = keys[j + 1];
                    if (floatValues != null) {
                        floatValues[j] = floatValues[j + 1];
                    }
                    if (shortValues != null) {
                        shortValues[j] = shortValues[j + 1];
                    }
                    j++;
                }

                keys[j] = tmpKey;
                if (floatValues != null) {
                    floatValues[j] = tmpFloat;
                }
                if (shortValues != null) {
                    shortValues[j] = tmpShort;
                }
            }
        }
    }

    private void partialFlashSort(final int[] keys, final float[] floatValues, final short[] shortValues) {
        final int n = keys.length;
        final int m = java.lang.Math.max(n / 20, 1);
        final int[] l = new int[m];

        int i = 0, j = 0, k = 0;
        int anmin = keys[0];
        int nmax = 0;

        for (i = 1; i < n; i++) {
            if (keys[i] < anmin)
                anmin = keys[i];
            if (keys[i] > keys[nmax])
                nmax = i;
        }

        if (anmin == keys[nmax])
            return;

        final double c1 = ((double) m - 1) / (keys[nmax] - anmin);

        for (i = 0; i < n; i++) {
            k = (int) (c1 * (keys[i] - anmin));
            l[k]++;
        }

        for (k = 1; k < m; k++) {
            l[k] += l[k - 1];
        }

        int tmpKey = keys[nmax];
        swap(keys, floatValues, shortValues, 0, nmax);

        int nmove = 0;
        int keyFlash;
        float floatFlash = 0;
        short shortFlash = 0;
        j = 0;
        k = m - 1;

        while (nmove < n - 1) {
            while (j > (l[k] - 1)) {
                j++;
                k = (int) (c1 * (keys[j] - anmin));
            }

            keyFlash = keys[j];
            if (floatValues != null) {
                floatFlash = floatValues[j];
            }
            if (shortValues != null) {
                shortFlash = shortValues[j];
            }

            while (!(j == l[k])) {
                k = (int) (c1 * (keyFlash - anmin));

                tmpKey = keys[l[k] - 1];
                keys[l[k] - 1] = keyFlash;
                keyFlash = tmpKey;

                if (floatValues != null) {
                    final float tmpFloat = floatValues[l[k] - 1];
                    floatValues[l[k] - 1] = floatFlash;
                    floatFlash = tmpFloat;
                }

                if (shortValues != null) {
                    final short tmpShort = shortValues[l[k] - 1];
                    shortValues[l[k] - 1] = shortFlash;
                    shortFlash = tmpShort;
                }

                l[k]--;
                nmove++;
            }
        }
    }

}
