package edu.ohsu.cslu.hash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.JUnit;
import edu.ohsu.cslu.util.Math;

@RunWith(Theories.class)
public abstract class ImmutableIntPair2IntHashTestCase {

    public final static int[] keys0 = new int[] { 1, 3 };

    public final static int[] keys1 = new int[] { 0, 3, 4, 7, 10, 13, 15, 18, 19, 21, 22, 24, 26, 29, 30, 34 };

    public final static int[] keys2 = new int[] { 10, 23, 54, 77, 103, 123, 157, 118, 198, 221, 322, 324, 426, 529,
            530, 1034 };

    @DataPoint
    public final static Object[] dp0 = new Object[] { keys0 };
    @DataPoint
    public final static Object[] dp1 = new Object[] { keys1 };
    @DataPoint
    public final static Object[] dp2 = new Object[] { keys2 };
    @DataPoint
    public final static Object[] dp3 = new Object[] { keys1, 2 };
    @DataPoint
    public final static Object[] dp4 = new Object[] { keys2, 2 };
    @DataPoint
    public final static Object[] dp5 = new Object[] { keys1, 4 };
    @DataPoint
    public final static Object[] dp6 = new Object[] { keys2, 4 };

    protected abstract ImmutableIntPair2IntHash hash(int[][] keyPairs, int modulus);

    private ImmutableIntPair2IntHash hash(final Object[] datapoint, final int[][] keyPairs) {
        return (datapoint.length > 1 ? hash(keyPairs, ((Integer) datapoint[1]).intValue()) : hash(keyPairs, 0));
    }

    @Theory
    public void testHashcode(final Object[] datapoint) {
        final int[][] keyPairs = keyPairs((int[]) datapoint[0]);
        verifyHash(hash(datapoint, keyPairs), keyPairs);
    }

    @Theory
    public void testKey1(final Object[] datapoint) {
        final int[][] keyPairs = keyPairs((int[]) datapoint[0]);
        final ImmutableIntPair2IntHash hash = hash(datapoint, keyPairs);

        for (int i = 0; i < keyPairs[0].length; i++) {
            assertEquals(keyPairs[0][i], hash.key1(hash.hashcode(keyPairs[0][i], keyPairs[1][i])));
        }
    }

    @Theory
    public void testKey2(final Object[] datapoint) {
        final int[][] keyPairs = keyPairs((int[]) datapoint[0]);
        final ImmutableIntPair2IntHash hash = hash(datapoint, keyPairs);

        for (int i = 0; i < keyPairs[0].length; i++) {
            assertEquals(keyPairs[1][i], hash.key2(hash.hashcode(keyPairs[0][i], keyPairs[1][i])));
        }
    }

    private void verifyHash(final ImmutableIntPair2IntHash hash, final int[][] keyPairs) {
        final int[][] nonKeyPairs = nonKeyPairs(keyPairs);
        for (int i = 0; i < keyPairs[0].length; i++) {
            assertTrue("Expected to find " + keyPairs[0][i] + "," + keyPairs[1][i],
                    hash.hashcode(keyPairs[0][i], keyPairs[1][i]) >= 0);
        }

        for (int i = 0; i < nonKeyPairs[0].length; i++) {
            assertEquals("Did not expect to find " + nonKeyPairs[0][i] + "," + nonKeyPairs[1][i], Integer.MIN_VALUE,
                    hash.hashcode(nonKeyPairs[0][i], nonKeyPairs[1][i]));
        }
    }

    @Test
    public void testR2Grammar() throws Exception {
        verifyRecognitionMatrix("hash/f2_21_R2.rm");
    }

    @Test
    public void testBerkeleyGrammar() throws Exception {
        verifyRecognitionMatrix("hash/berkeley.rm");
    }

    private void verifyRecognitionMatrix(final String filename) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(JUnit.unitTestDataAsStream(filename)));
        final IntArrayList keyList = new IntArrayList();

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] split = line.split(",");
            assertEquals(2, split.length);
            final int i1 = Integer.parseInt(split[0]);
            final int i2 = Integer.parseInt(split[1]);
            keyList.add(i1);
            keyList.add(i2);
        }
        final int[][] keyPairs = keyPairs(keyList.toIntArray());
        final ImmutableIntPair2IntHash hash = hash(keyPairs, 0);
        verifyHash(hash, keyPairs);
    }

    private int[][] keyPairs(final int[] keys) {
        final int[][] keyPairs = new int[2][keys.length / 2];
        for (int i = 0; i < keys.length; i = i + 2) {
            keyPairs[0][i / 2] = keys[i];
            keyPairs[1][i / 2] = keys[i + 1];
        }
        return keyPairs;
    }

    private int[][] nonKeyPairs(final int[][] keyPairs) {

        final int packingShift = Math.logBase2(Math.nextPowerOf2(Math.max(keyPairs[1])));
        int m = 0;
        for (int i = 0; i < packingShift; i++) {
            m = m << 1 | 1;
        }
        final int packingMask = m;

        final IntSet keys = new IntOpenHashSet();
        for (int i = 0; i < keyPairs[0].length; i++) {
            keys.add((keyPairs[0][i]) << packingShift | (keyPairs[1][i]));
        }

        final int maxKey = Math.max(keyPairs[0]) << packingShift | Math.max(keyPairs[1]);

        final IntSet nonKeys = new IntOpenHashSet();

        // Exhaustively search space of non-keys
        for (int i = 0; i < maxKey; i++) {
            if (!keys.contains(i)) {
                nonKeys.add(i);
            }
        }

        // Return the resulting nonkey set as a 2-d array of ints
        final int[][] nonKeyPairs = new int[2][nonKeys.size()];
        int i = 0;
        for (final int nk : nonKeys) {
            nonKeyPairs[0][i] = nk >> packingShift;
            nonKeyPairs[1][i++] = nk & packingMask;
        }
        return nonKeyPairs;
    }
}
