package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;

import edu.ohsu.cslu.util.Math;

/**
 * Implementation of {@link ImmutableIntPair2IntHash} which creates separate perfect-hashes for each key 1
 * value (generally the left child production in a grammar rule).
 * 
 * TODO This code needs some cleanup and documentation
 * 
 * @author Aaron Dunlop
 * @since May 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SegmentedPerfectIntPair2IntHash implements ImmutableIntPair2IntHash {

    private final int maxKey2;

    // TODO Allow different moduli for each key1
    private final int modulus;

    private final int shift;
    private final int mask;

    private final int[] shiftTable;
    private final int n;
    private final int nShift;

    private final int[] hashtableOffsets;
    private final short[] hashtable;
    private final int size;

    public SegmentedPerfectIntPair2IntHash(final int[][] keyPairs, final int modulus) {

        this.size = keyPairs[0].length;

        final int arraySize = Math.max(keyPairs[0]) + 1;
        final IntOpenHashSet[] keys2 = new IntOpenHashSet[arraySize];
        for (int i = 0; i < keys2.length; i++) {
            keys2[i] = new IntOpenHashSet();
        }
        for (int i = 0; i < keyPairs[0].length; i++) {
            keys2[keyPairs[0][i]].add(keyPairs[1][i]);
        }

        this.maxKey2 = Math.max(keyPairs[1]);

        final int squareMatrixM = Math.nextPowerOf2((int) java.lang.Math.sqrt(maxKey2) + 1);
        this.modulus = modulus > 0 ? modulus : squareMatrixM;
        this.n = squareMatrixM * squareMatrixM / this.modulus;

        this.shift = Math.logBase2(this.modulus);
        this.nShift = Math.logBase2(n);

        int bit = 0;
        for (int i = 0; i < shift; i++) {
            bit = bit << 1 | 0x01;
        }
        this.mask = bit;

        this.hashtableOffsets = new int[arraySize];
        this.shiftTable = new int[arraySize << nShift];

        final short[][] hashtableSegments = new short[arraySize][];
        int hashtableLength = 0;
        for (int key1 = 0; key1 < arraySize; key1++) {
            hashtableSegments[key1] = createPerfectHash(key1, keys2[key1]);
            hashtableLength += hashtableSegments[key1].length;
            if (key1 < arraySize - 1) {
                hashtableOffsets[key1 + 1] = hashtableOffsets[key1] + hashtableSegments[key1].length;
            }
        }

        for (int key1 = 1; key1 < arraySize; key1++) {
            for (int i = 0; i < n; i++) {
                shiftTable[(key1 << nShift) + i] += hashtableOffsets[key1];
            }
        }

        this.hashtable = new short[hashtableLength];
        Arrays.fill(hashtable, Short.MIN_VALUE);
        for (int i = 0; i < arraySize; i++) {
            System.arraycopy(hashtableSegments[i], 0, hashtable, hashtableOffsets[i],
                hashtableSegments[i].length);
        }
    }

    public SegmentedPerfectIntPair2IntHash(final int[][] keyPairs) {
        this(keyPairs, 0);
    }

    private int findShift(final short[] target, final short[] merge) {
        for (int s = 0; s <= target.length - merge.length; s++) {
            if (!shiftCollides(target, merge, s)) {
                return s;
            }
        }
        throw new RuntimeException("Unable to find a successful shift");
    }

    private short[] createPerfectHash(final int key1, final IntOpenHashSet keys2) {
        // Allocate a temporary hashtable of the maximum possible size
        final short[] tmp = new short[this.modulus << nShift];
        Arrays.fill(tmp, Short.MIN_VALUE);

        final int[] rowIndices = new int[n];
        final int[] rowCounts = new int[n];
        final short[][] tmpMatrix = new short[n][this.modulus];
        for (int i = 0; i < n; i++) {
            rowIndices[i] = i;
            Arrays.fill(tmpMatrix[i], Short.MIN_VALUE);
        }

        for (final int key2 : keys2) {
            final int x = key2 >> shift;
            final int y = key2 & mask;
            tmpMatrix[x][y] = (short) key2;
            rowCounts[x]++;
        }

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
            // TODO Strange to return the hashtable segment and initialize the r segment inline
            shiftTable[(key1 << nShift) + row] = findShift(tmp, tmpMatrix[row]);
            for (int i = 0; i < this.modulus; i++) {
                if (tmpMatrix[row][i] != Short.MIN_VALUE) {
                    tmp[shiftTable[(key1 << nShift) + row] + i] = tmpMatrix[row][i];
                }
            }
        }

        final short[] hashtableSegment = new short[Math.max(shiftTable, key1 << nShift, (key1 + 1) << nShift)
                + this.modulus];
        System.arraycopy(tmp, 0, hashtableSegment, 0, hashtableSegment.length);

        return hashtableSegment;
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
    public int hashcode(final int key1, final int key2) {
        if (key2 > maxKey2) {
            return Integer.MIN_VALUE;
        }
        return unsafeHashcode(key1, key2);
    }

    public int unsafeHashcode(final int key1, final int key2) {
        final int x = key2 >> shift;
        final int y = key2 & mask;
        final int hashcode = shiftTable[(key1 << nShift) + x] + y;
        return hashtable[hashcode] == key2 ? hashcode : Integer.MIN_VALUE;
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
        for (int i = 0; i < hashtableOffsets.length - 1; i++) {
            if (hashcode >= hashtableOffsets[i] && hashcode < hashtableOffsets[i + 1]) {
                return i;
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
        return String
            .format(
                "hashtable size: %d modulus: %d shift-table size: %d keys: %d occupancy: %.2f%% maxkey2: %d totalMem: %d",
                hashtableSize(), modulus, shiftTable.length, size, size * 100f / hashtableSize(), maxKey2,
                hashtable.length * 2 + shiftTable.length * 4);
    }
}
