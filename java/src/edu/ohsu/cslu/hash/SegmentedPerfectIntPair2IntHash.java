package edu.ohsu.cslu.hash;

import java.util.Arrays;

import edu.ohsu.cslu.util.Math;

public class SegmentedPerfectIntPair2IntHash implements ImmutableIntPair2IntHash {

    private final int maxKey;
    private final int modulus;
    private final int shift;
    private final int mask;

    private final int[] r;
    private final int[] hashtable;
    private final int size;

    public SegmentedPerfectIntPair2IntHash(final int[][] keyPairs, final int modulus) {

        this.size = keyPairs.length;
        this.maxKey = maxKey2(keyPairs);
        final int squareMatrixM = Math.nextPowerOf2((int) java.lang.Math.sqrt(maxKey) + 1);
        this.modulus = modulus > 0 ? modulus : squareMatrixM;
        final int n = squareMatrixM * squareMatrixM / modulus;

        // Allocate a temporary hashtable of the maximum possible size
        final int[] tmp = new int[modulus * n];
        Arrays.fill(tmp, Integer.MIN_VALUE);

        shift = Math.logBase2(modulus);
        int bit = 0;
        for (int i = 0; i < shift; i++) {
            bit = bit << 1 | 0x01;
        }
        mask = bit;

        final int[] rowIndices = new int[n];
        final int[] rowCounts = new int[n];
        final int[][] tmpMatrix = new int[n][modulus];
        for (int i = 0; i < n; i++) {
            rowIndices[i] = i;
            Arrays.fill(tmpMatrix[i], Integer.MIN_VALUE);
        }

        for (final int keyPair[] : keyPairs) {
            final int key = keyPair[0] << 16 | keyPair[1];
            final int x = key >> shift;
            final int y = key & mask;
            tmpMatrix[x][y] = key;
            rowCounts[x]++;
        }
        r = new int[n];

        // Sort rows in descending order by population
        edu.ohsu.cslu.util.Arrays.sort(rowCounts, rowIndices);
        edu.ohsu.cslu.util.Arrays.reverse(rowIndices);
        edu.ohsu.cslu.util.Arrays.reverse(rowCounts);

        /*
         * First-Fit Descending Method algorithm For each non-empty row:
         * 
         * 1. shift the row right until none of its items collide with any of the items in previous rows.
         * 
         * 2. Record the shift amount in array r[].
         * 
         * 3. Insert this row into the hash table C[].
         */
        for (int row = 0; row < rowIndices.length; row++) {
            r[row] = findShift(tmp, tmpMatrix[row]);
            for (int i = 0; i < modulus; i++) {
                if (tmpMatrix[row][i] != Integer.MIN_VALUE) {
                    tmp[r[row] + i] = tmpMatrix[row][i];
                }
            }
        }

        this.hashtable = new int[Math.max(r) + modulus];
        System.arraycopy(tmp, 0, hashtable, 0, hashtable.length);
    }

    public SegmentedPerfectIntPair2IntHash(final int[][] keyPairs) {
        this(keyPairs, Math.nextPowerOf2((int) java.lang.Math.sqrt(maxKey2(keyPairs)) + 1));
    }

    private static int maxKey2(final int[][] keyPairs) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < keyPairs.length; i++) {
            if (keyPairs[i][1] > max) {
                max = keyPairs[i][1];
            }
        }
        return max;
    }

    private int findShift(final int[] target, final int[] merge) {
        for (int s = 0; s <= target.length - merge.length; s++) {
            if (!shiftCollides(target, merge, s)) {
                return s;
            }
        }
        throw new RuntimeException("Unable to find a successful shift");
    }

    /**
     * Returns true if the merged array, when shifted by s, will `collide' with the target array; i.e., if we
     * right-shift merge by s, are any populated elements of merge also populated elements of target.
     * 
     * @param target
     * @param merge
     * @param s
     * @return
     */
    private boolean shiftCollides(final int[] target, final int[] merge, final int s) {
        for (int i = 0; i < merge.length; i++) {
            if (merge[i] != Integer.MIN_VALUE && target[s + i] != Integer.MIN_VALUE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashcode(final int key1, final int key2) {
        final int key = key1 << 16 | key2;

        if (key > maxKey) {
            return Integer.MIN_VALUE;
        }
        return unsafeHashcode(key);
    }

    public int unsafeHashcode(final int key) {
        final int x = key >> shift;
        final int y = key & mask;
        final int hashcode = r[x] + y;
        return hashtable[hashcode] == key ? hashcode : Integer.MIN_VALUE;
    }

    public int key(final int hashcode) {
        if (hashcode > hashtable.length) {
            return Integer.MIN_VALUE;
        }
        return hashtable[hashcode];
    }

    public int unsafeKey(final int hashcode) {
        return hashtable[hashcode];
    }

    public boolean containsKey(final int key) {
        if (key > maxKey) {
            return false;
        }

        return unsafeContainsKey(key);
    }

    public boolean unsafeContainsKey(final int key) {
        final int x = key >> shift;
        final int y = key & mask;
        final int index = r[x] + y;
        return hashtable[index] == key;
    }

    public final int hashtableSize() {
        return hashtable.length;
    }

    public final int size() {
        return size;
    }

    @Override
    public String toString() {
        return String
            .format(
                "hashtable size: %d modulus: %d shift-table size: %d shift-table max: %d keys: %d occupancy: %.2f%% maxkey: %d",
                hashtable.length, modulus, r.length, Math.max(r), size, size * 100f / hashtable.length,
                maxKey);
    }
}
