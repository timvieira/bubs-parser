package edu.ohsu.cslu.hash;

import java.util.Arrays;

import edu.ohsu.cslu.util.Math;

public class PerfectHash implements ImmutableInt2IntHash {

    private final int maxKey;
    private final int modulus;
    private final int shift;
    private final int mask;

    private final int[] r;
    private final int[] hashtable;
    private final int size;

    public PerfectHash(final int[] keys) {
        this.size = keys.length;
        this.maxKey = Math.max(keys);
        this.modulus = Math.nextPowerOf2((int) java.lang.Math.sqrt(maxKey) + 1);

        // Allocate a temporary hashtable of the maximum possible size
        final int[] tmp = new int[modulus * modulus];
        Arrays.fill(tmp, Integer.MIN_VALUE);

        shift = Math.logBase2(modulus);
        int bit = 0;
        for (int i = 0; i < shift; i++) {
            bit = bit << 1 | 0x01;
        }
        mask = bit;

        final int[] rowIndices = new int[modulus];
        final int[] rowCounts = new int[modulus];
        final int[][] squareMatrix = new int[modulus][modulus];
        for (int i = 0; i < modulus; i++) {
            rowIndices[i] = i;
            Arrays.fill(squareMatrix[i], Integer.MIN_VALUE);
        }

        for (final int key : keys) {
            final int x = key >> shift;
            final int y = key & mask;
            squareMatrix[x][y] = key;
            rowCounts[x]++;
        }
        r = new int[modulus];

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
            r[row] = findShift(tmp, squareMatrix[row]);
            for (int i = 0; i < modulus; i++) {
                if (squareMatrix[row][i] != Integer.MIN_VALUE) {
                    tmp[r[row] + i] = squareMatrix[row][i];
                }
            }
        }

        this.hashtable = new int[Math.max(r) + modulus];
        System.arraycopy(tmp, 0, hashtable, 0, hashtable.length);
    }

    private int findShift(final int[] target, final int[] merge) {
        for (int s = 0; s < target.length - merge.length - 1; s++) {
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

    public int hashcode(final int key) {
        if (key > maxKey) {
            return Integer.MIN_VALUE;
        }
        return unsafeIndex(key);
    }

    public int unsafeIndex(final int key) {
        final int x = key >> shift;
        final int y = key & mask;
        final int index = r[x] + y;
        return hashtable[index] == key ? index : Integer.MIN_VALUE;
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

    public int hashtableSize() {
        return hashtable.length;
    }

    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return String.format("hashtable size: %d modulus: %d keys: %d occupancy: %.2f%% maxkey: %d",
            hashtable.length, modulus, size, size * 100f / hashtable.length, maxKey);
    }
}
