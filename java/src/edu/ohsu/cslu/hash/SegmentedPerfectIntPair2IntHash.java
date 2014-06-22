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

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;

import edu.ohsu.cslu.util.Math;
import edu.ohsu.cslu.util.Strings;

/**
 * Implementation of {@link ImmutableIntPair2IntHash} which creates separate perfect-hashes for each key 1 value
 * (generally the left child production in a grammar rule).
 * 
 * Generates perfect hashes using the `hash-and-displace' method originally described in Tarjan and Yao, 1979
 * "Storing a sparse table". Displaces using the standard `first-fit decreasing' method (a reasonable heuristic to find
 * appropriate displacements). The resulting hashes are not minimal, but are usually fairly compact.
 * 
 * Assumes that key-1 values will be (nearly) fully populated (that is, if k1 ranges from 1..n, |K1| ~= n). Creates a
 * separate perfect hash for each k1 value. Stores all hashes into a single parallel array, recording offsets into that
 * array for each k1.
 * 
 * When creating a perfect hash, hashed k2 values are first stored in a square matrix (see Tarjan and Yao or
 * http://www.drdobbs.com/184404506). We create this matrix as a square of a power-of-two, so that the k2 hashes can be
 * computed by bitwise AND with a mask. Subsequent hash queries can then be computed with bitwise shift and AND
 * operations. The matrix sizes (and thus the shifts and masks) vary for each k1, based on the maximum k2 observed for
 * that k1.
 * 
 * TODO Try using a non-square matrix (many hashes are fairly sparse, so a narrower matrix often results in a denser
 * hash).
 * 
 * TODO Implement {@link ImmutableShortPair2IntHash} instead of {@link ImmutableIntPair2IntHash}
 * 
 * @author Aaron Dunlop
 * @since May 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SegmentedPerfectIntPair2IntHash implements ImmutableIntPair2IntHash {

    /** Parallel array, indexed by k1 */
    private final int[] maxKey2;
    private final int[] k2Shifts;
    private final int[] k2Masks;

    /** Offsets of each perfect hash `segment' within {@link #hashtable}, indexed by k1. */
    private final int[] hashtableOffsets;
    private final short[] hashtable;

    /** Offsets of each perfect hash `segment' within {@link #displacementTableOffsets}, indexed by k1. */
    private final int[] displacementTable;
    private final int[] displacementTableOffsets;

    private final int size;

    public SegmentedPerfectIntPair2IntHash(final int[][] keyPairs) {

        final int parallelArraySize = Math.max(keyPairs[0]) + 1;

        // Find unique k2 values for each k1
        final IntOpenHashSet[] k2Sets = new IntOpenHashSet[parallelArraySize];
        for (int i = 0; i < k2Sets.length; i++) {
            k2Sets[i] = new IntOpenHashSet();
        }
        for (int i = 0; i < keyPairs[0].length; i++) {
            k2Sets[keyPairs[0][i]].add(keyPairs[1][i]);
        }

        // Calculate total key pair count
        int tmpSize = 0;
        for (int i = 0; i < k2Sets.length; i++) {
            tmpSize += k2Sets[i].size();
        }
        this.size = tmpSize;

        this.maxKey2 = new int[parallelArraySize];

        // Indexed by k1
        final int[][] k2s = new int[parallelArraySize][];
        for (int i = 0; i < k2s.length; i++) {
            k2s[i] = k2Sets[i].toIntArray();
            maxKey2[i] = Math.max(k2s[i]);
        }

        this.k2Shifts = new int[parallelArraySize];
        this.k2Masks = new int[parallelArraySize];
        this.hashtableOffsets = new int[parallelArraySize + 1];
        this.displacementTableOffsets = new int[parallelArraySize + 1];

        final int tmpArraySize = parallelArraySize * Math.max(keyPairs[1]) + 1;
        final short[] tmpHashtable = new short[tmpArraySize];
        final int[] tmpDisplacementTable = new int[tmpArraySize];

        for (int k1 = 0; k1 < k2s.length; k1++) {

            final HashtableSegment hs = createPerfectHash(k2s[k1]);

            this.k2Masks[k1] = hs.hashMask;
            this.k2Shifts[k1] = hs.hashShift;

            // Record the offsets
            hashtableOffsets[k1 + 1] = hashtableOffsets[k1] + hs.hashtableSegment.length;
            displacementTableOffsets[k1 + 1] = displacementTableOffsets[k1] + hs.displacementTableSegment.length;

            // Copy the segment into the temporary hash and displacement arrays
            System.arraycopy(hs.hashtableSegment, 0, tmpHashtable, hashtableOffsets[k1], hs.hashtableSegment.length);

            for (int j = 0; j < hs.displacementTableSegment.length; j++) {
                tmpDisplacementTable[displacementTableOffsets[k1] + j] = hashtableOffsets[k1]
                        + hs.displacementTableSegment[j];
            }
        }

        this.hashtable = new short[hashtableOffsets[parallelArraySize]];
        System.arraycopy(tmpHashtable, 0, this.hashtable, 0, this.hashtable.length);
        this.displacementTable = new int[displacementTableOffsets[parallelArraySize]];
        System.arraycopy(tmpDisplacementTable, 0, this.displacementTable, 0, this.displacementTable.length);
    }

    private int findDisplacement(final short[] target, final short[] merge) {
        for (int s = 0; s <= target.length - merge.length; s++) {
            if (!shiftCollides(target, merge, s)) {
                return s;
            }
        }
        throw new RuntimeException("Unable to find a successful shift");
    }

    private HashtableSegment createPerfectHash(final int[] k2s) {

        // If there are no k2 entries for this k1, return a single-entry hash segment, with a shift and mask
        // that will always resolve to the single (empty) entry
        if (k2s.length == 0) {
            return new HashtableSegment(new short[] { Short.MIN_VALUE }, 1, new int[] { 0 }, 1, 32, 0x0,
                    new short[][] { { Short.MIN_VALUE } });
        }

        // Compute the size of the square matrix (m)
        final int m = Math.nextPowerOf2((int) java.lang.Math.sqrt(Math.max(k2s)) + 1);
        final int n = m;

        // Allocate a temporary hashtable of the maximum possible size
        final short[] hashtableSegment = new short[m * n];
        Arrays.fill(hashtableSegment, Short.MIN_VALUE);

        // Allocate the displacement table (r in Getty's notation)
        final int[] displacementTableSegment = new int[m];

        // Compute shift and mask (for hashing k2, prior to displacement)
        final int hashBitShift = Math.logBase2(m);
        int tmp = 0;
        for (int j = 0; j < hashBitShift; j++) {
            tmp = tmp << 1 | 0x01;
        }
        final int hashMask = tmp;

        // Initialize the matrix
        final int[] rowIndices = new int[m];
        final int[] rowCounts = new int[m];
        final short[][] tmpMatrix = new short[m][n];
        for (int i = 0; i < m; i++) {
            rowIndices[i] = i;
            Arrays.fill(tmpMatrix[i], Short.MIN_VALUE);
        }

        // Populate the matrix, and count population of each row.
        for (int i = 0; i < k2s.length; i++) {
            final int k2 = k2s[i];
            final int x = k2 >> hashBitShift;
            final int y = k2 & hashMask;
            tmpMatrix[x][y] = (short) k2;
            rowCounts[x]++;
        }

        // Sort rows in ascending order by population (we'll iterate through the array in reverse order)
        edu.ohsu.cslu.util.Arrays.sort(rowCounts, rowIndices);

        /*
         * Store matrix rows in a single array, using the first-fit descending method. For each non-empty row:
         * 
         * 1. Displace the row right until none of its items collide with any of the items in previous rows.
         * 
         * 2. Record the displacement amount in displacementTableSegment.
         * 
         * 3. Insert this row into hashtableSegment.
         */
        for (int i = m - 1; i >= 0; i--) {
            final int row = rowIndices[i];
            displacementTableSegment[row] = findDisplacement(hashtableSegment, tmpMatrix[row]);
            for (int col = 0; col < m; col++) {
                if (tmpMatrix[row][col] != Short.MIN_VALUE) {
                    hashtableSegment[displacementTableSegment[row] + col] = tmpMatrix[row][col];
                }
            }
        }

        // Find the length of the segment (highest populated index in tmpHashtable + n)
        int maxPopulatedIndex = 0;
        for (int i = 0; i < hashtableSegment.length; i++) {
            if (hashtableSegment[i] != Short.MIN_VALUE) {
                maxPopulatedIndex = i;
            }
        }
        final int segmentLength = maxPopulatedIndex + n;

        return new HashtableSegment(hashtableSegment, segmentLength, displacementTableSegment, m, hashBitShift,
                hashMask, tmpMatrix);
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
    private boolean shiftCollides(final short[] target, final short[] merge, final int s) {
        for (int i = 0; i < merge.length; i++) {
            if (merge[i] != Short.MIN_VALUE && target[s + i] != Short.MIN_VALUE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashcode(final int k1, final int k2) {
        if (k2 > maxKey2[k1]) {
            return Integer.MIN_VALUE;
        }
        return unsafeHashcode(k1, k2);
    }

    public int unsafeHashcode(final int k1, final int k2) {
        final int mask = k2Masks[k1];
        final int x = k2 >> k2Shifts[k1] & mask;
        final int y = k2 & mask;
        final int hashcode = displacementTable[displacementTableOffsets[k1] + x] + y;
        return hashtable[hashcode] == k2 ? hashcode : Integer.MIN_VALUE;
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
        // Linear search hashtable offsets for index of next-lower offset
        // A binary search might be a bit more efficient, but this shouldn't be a major time consumer
        for (int k1 = 0; k1 < hashtableOffsets.length - 1; k1++) {
            if (hashcode >= hashtableOffsets[k1] && hashcode < hashtableOffsets[k1 + 1]) {
                return k1;
            }
        }
        return hashtableOffsets.length - 1;
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
        return hashtable[hashcode];
    }

    public final int hashtableSize() {
        return hashtable.length;
    }

    public final int size() {
        return size;
    }

    @Override
    public String toString() {
        return String.format("keys: %d hashtable size: %d occupancy: %.2f%% shift-table size: %d totalMem: %d", size,
                hashtableSize(), size * 100f / hashtableSize(), displacementTable.length, hashtable.length * 2
                        + displacementTable.length * 4);
    }

    private final class HashtableSegment {
        final short[] hashtableSegment;
        final int[] displacementTableSegment;
        final int hashShift;
        final int hashMask;

        final short[][] squareMatrix;

        public HashtableSegment(final short[] hashtableSegment, final int segmentLength,
                final int[] displacementTableSegment, final int displacementTableSegmentLength, final int hashShift,
                final int hashMask, final short[][] squareMatrix) {

            this.hashtableSegment = new short[segmentLength];
            System.arraycopy(hashtableSegment, 0, this.hashtableSegment, 0, segmentLength);
            this.displacementTableSegment = new int[displacementTableSegmentLength];
            System.arraycopy(displacementTableSegment, 0, this.displacementTableSegment, 0,
                    displacementTableSegmentLength);
            this.hashShift = hashShift;
            this.hashMask = hashMask;
            this.squareMatrix = squareMatrix;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("   |");
            for (int col = 0; col < squareMatrix.length; col++) {
                sb.append(String.format(" %2d", col));
            }
            sb.append('\n');
            sb.append(Strings.fill('-', squareMatrix.length * 3 + 4));
            sb.append('\n');
            for (int row = 0; row < squareMatrix.length; row++) {
                sb.append(String.format("%2d |", row));
                for (int col = 0; col < squareMatrix.length; col++) {
                    sb.append(String.format(" %2s",
                            squareMatrix[row][col] == Short.MIN_VALUE ? "-" : Short.toString(squareMatrix[row][col])));
                }
                sb.append('\n');
            }
            sb.append("index: ");
            for (int i = 0; i < hashtableSegment.length; i++) {
                sb.append(String.format(" %2d", i));
            }
            sb.append("\nkeys : ");
            for (int i = 0; i < hashtableSegment.length; i++) {
                sb.append(String.format(" %2s",
                        hashtableSegment[i] == Short.MIN_VALUE ? "-" : Short.toString(hashtableSegment[i])));
            }
            sb.append('\n');
            return sb.toString();
        }
    }
}
