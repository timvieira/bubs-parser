/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.hash;

import java.util.Arrays;

import edu.ohsu.cslu.util.Math;

public class PerfectIntPair2IntHash implements ImmutableIntPair2IntHash {

    private final int maxKey2;
    private final int maxPackedKey;
    private final int modulus;

    private final int packingShift;
    private final int packingMask;

    private final int packedKeyShift;
    private final int packedKeyMask;

    private final int[] r;
    private final int[] hashtable;
    private final int size;

    /**
     * @param keyPairs Parallel array of key pairs (keyPairs[0] = key1 array, keyPairs[1] = key2 array).
     * @param modulus
     */
    public PerfectIntPair2IntHash(final int[][] keyPairs, final int modulus) {

        this.size = keyPairs[0].length;

        this.maxKey2 = Math.max(keyPairs[1]);
        this.packingShift = Math.logBase2(Math.nextPowerOf2(maxKey2));
        int m = 0;
        for (int i = 0; i < packingShift; i++) {
            m = m << 1 | 1;
        }
        this.packingMask = m;
        this.maxPackedKey = Math.max(keyPairs[0]) << packingShift | maxKey2;

        final int squareMatrixM = Math.nextPowerOf2((int) java.lang.Math.sqrt(maxPackedKey) + 1);
        this.modulus = modulus > 0 ? modulus : squareMatrixM;
        final int n = squareMatrixM * squareMatrixM / this.modulus;

        // Allocate a temporary hashtable of the maximum possible size
        final int[] tmp = new int[this.modulus * n];
        Arrays.fill(tmp, Integer.MIN_VALUE);

        this.packedKeyShift = Math.logBase2(this.modulus);
        int bit = 0;
        for (int i = 0; i < packedKeyShift; i++) {
            bit = bit << 1 | 0x01;
        }
        this.packedKeyMask = bit;

        final int[] rowIndices = new int[n];
        final int[] rowCounts = new int[n];
        final int[][] tmpMatrix = new int[n][this.modulus];
        for (int i = 0; i < n; i++) {
            rowIndices[i] = i;
            Arrays.fill(tmpMatrix[i], Integer.MIN_VALUE);
        }

        for (int i = 0; i < keyPairs[0].length; i++) {
            final int packedKey = (keyPairs[0][i] << packingShift) | (keyPairs[1][i] & packingMask);

            final int x = packedKey >> packedKeyShift;
            final int y = packedKey & packedKeyMask;
            tmpMatrix[x][y] = packedKey;
            rowCounts[x]++;
        }
        this.r = new int[n];

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
            for (int i = 0; i < this.modulus; i++) {
                if (tmpMatrix[row][i] != Integer.MIN_VALUE) {
                    tmp[r[row] + i] = tmpMatrix[row][i];
                }
            }
        }

        this.hashtable = new int[Math.max(r) + this.modulus];
        System.arraycopy(tmp, 0, hashtable, 0, hashtable.length);
    }

    public PerfectIntPair2IntHash(final int[][] keyPairs) {
        this(keyPairs, 0);
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
        final int packedKey = (key1 << packingShift) | (key2 & packingMask);

        if (key2 > maxKey2 || packedKey > maxPackedKey) {
            return Integer.MIN_VALUE;
        }

        final int x = packedKey >> packedKeyShift;
        final int y = packedKey & packedKeyMask;
        final int hashcode = r[x] + y;
        return hashtable[hashcode] == packedKey ? hashcode : Integer.MIN_VALUE;
    }

    public int unsafeHashcode(final int key1, final int key2) {
        final int packedKey = (key1 << packingShift) | (key2 & packingMask);

        final int x = packedKey >> packedKeyShift;
        final int y = packedKey & packedKeyMask;
        final int hashcode = r[x] + y;
        return hashtable[hashcode] == packedKey ? hashcode : Integer.MIN_VALUE;
    }

    @Override
    public int key1(final int hashcode) {
        if (hashcode > hashtable.length) {
            return Integer.MIN_VALUE;
        }
        return unsafeKey1(hashcode);
    }

    @Override
    public int unsafeKey1(final int hashcode) {
        return hashtable[hashcode] >> packingShift;
    }

    @Override
    public int key2(final int hashcode) {
        if (hashcode > hashtable.length) {
            return Integer.MIN_VALUE;
        }
        return unsafeKey2(hashcode);
    }

    @Override
    public int unsafeKey2(final int hashcode) {
        return hashtable[hashcode] & packingMask;
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
                .format("hashtable size: %d modulus: %d shift-table size: %d shift-table max: %d keys: %d occupancy: %.2f%% maxkey: %d",
                        hashtable.length, modulus, r.length, Math.max(r), size, size * 100f / hashtable.length,
                        maxPackedKey);
    }
}
