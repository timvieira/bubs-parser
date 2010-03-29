package edu.ohsu.cslu.util;

import edu.ohsu.cslu.util.Sort.BaseSort;

public class MergeSort extends BaseSort {

    private final static int INSERTIONSORT_THRESHOLD = 7;

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
        final int[] keysClone = keys.clone();
        final float[] floatClone = floatValues != null ? (float[]) floatValues.clone() : null;
        final short[] shortClone = shortValues != null ? (short[]) shortValues.clone() : null;
        mergeSort(keysClone, keys, floatClone, floatValues, shortClone, shortValues, 0, keys.length);
    }

    /**
     * Src is the source array that starts at index 0 Dest is the (possibly larger) array destination with a
     * possible offset low is the index in dest to start sorting high is the end index in dest to end sorting
     * off is the offset to generate corresponding low, high in src
     */
    private void mergeSort(final int[] keysSrc, final int[] keysDest, final float[] floatSrc,
            final float[] floatDest, final short[] shortSrc, final short[] shortDest, final int low,
            final int high) {
        final int length = high - low;

        // Insertion sort on smallest arrays
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i = low; i < high; i++) {
                for (int j = i; j > low && (keysDest[j - 1] > keysDest[j]); j--) {
                    swap(keysDest, floatDest, shortDest, j, j - 1);
                }
            }
            return;
        }

        // Recursively sort halves of dest into src
        final int destLow = low;
        final int destHigh = high;
        final int mid = (low + high) >>> 1;
        mergeSort(keysDest, keysSrc, floatDest, floatSrc, shortDest, shortSrc, low, mid);
        mergeSort(keysDest, keysSrc, floatDest, floatSrc, shortDest, shortSrc, mid, high);

        // If list is already sorted, just copy from src to dest. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (keysSrc[mid - 1] <= keysSrc[mid]) {
            System.arraycopy(keysSrc, low, keysDest, destLow, length);

            if (floatSrc != null) {
                System.arraycopy(floatSrc, low, floatDest, destLow, length);
            }

            if (shortSrc != null) {
                System.arraycopy(shortSrc, low, shortDest, destLow, length);
            }
            return;
        }

        // Merge sorted halves (now in src) into dest
        for (int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && (keysSrc[p] <= keysSrc[q])) {
                keysDest[i] = keysSrc[p];
                if (floatSrc != null) {
                    floatDest[i] = floatSrc[p];
                }
                if (shortSrc != null) {
                    shortDest[i] = shortSrc[p];
                }

                p++;
            } else {
                keysDest[i] = keysSrc[q];

                if (floatSrc != null) {
                    floatDest[i] = floatSrc[q];
                }
                if (shortSrc != null) {
                    shortDest[i] = shortSrc[q];
                }

                q++;
            }
        }
    }

    @Override
    public void sort(final int[] keys, final float[] floatValues, final short[] shortValues,
            final int[] segmentOffsets) {
        final int[] segmentOffsets2 = new int[segmentOffsets.length + 1];
        System.arraycopy(segmentOffsets, 0, segmentOffsets2, 0, segmentOffsets.length);
        segmentOffsets2[segmentOffsets2.length - 1] = keys.length;

        final int[] keysClone = keys.clone();
        final float[] floatClone = floatValues != null ? (float[]) floatValues.clone() : null;
        final short[] shortClone = shortValues != null ? (short[]) shortValues.clone() : null;
        mergeSort(keysClone, keys, floatClone, floatValues, shortClone, shortValues, 0,
            segmentOffsets.length - 1, segmentOffsets2);
    }

    private void mergeSort(final int[] keysSrc, final int[] keysDest, final float[] floatSrc,
            final float[] floatDest, final short[] shortSrc, final short[] shortDest, final int lowSegment,
            final int highSegment, final int[] segmentOffsets) {

        if (lowSegment == highSegment) {
            return;
        }

        final int low = segmentOffsets[lowSegment];
        final int high = segmentOffsets[highSegment + 1];

        // Recursively sort halves of dest into src
        final int destLow = low;
        final int destHigh = high;
        final int midSegment = (lowSegment + highSegment) / 2;
        final int mid = segmentOffsets[midSegment + 1];
        mergeSort(keysDest, keysSrc, floatDest, floatSrc, shortDest, shortSrc, lowSegment, midSegment,
            segmentOffsets);
        mergeSort(keysDest, keysSrc, floatDest, floatSrc, shortDest, shortSrc, midSegment + 1, highSegment,
            segmentOffsets);

        // Merge sorted halves (now in src) into dest
        for (int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && (keysSrc[p] <= keysSrc[q])) {
                keysDest[i] = keysSrc[p];
                if (floatSrc != null) {
                    floatDest[i] = floatSrc[p];
                }
                if (shortSrc != null) {
                    shortDest[i] = shortSrc[p];
                }

                p++;
            } else {
                keysDest[i] = keysSrc[q];

                if (floatSrc != null) {
                    floatDest[i] = floatSrc[q];
                }
                if (shortSrc != null) {
                    shortDest[i] = shortSrc[q];
                }

                q++;
            }
        }
    }
}
