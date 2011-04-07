package edu.ohsu.cslu.hash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

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
public abstract class ImmutableShortPair2IntHashTestCase {

    public final static short[] keys0 = new short[] { 1, 3 };

    public final static short[] keys1 = new short[] { 0, 3, 4, 7, 10, 13, 15, 18, 19, 21, 22, 24, 26, 29, 30, 34 };

    public final static short[] keys2 = new short[] { 10, 23, 54, 77, 103, 123, 157, 118, 198, 221, 322, 324, 426, 529,
            530, 1034 };

    @DataPoint
    public final static Object[] dp0 = new Object[] { keys0 };
    @DataPoint
    public final static Object[] dp1 = new Object[] { keys1 };
    @DataPoint
    public final static Object[] dp2 = new Object[] { keys2 };
    @DataPoint
    public final static Object[] dp3 = new Object[] { keys1, 8 };
    @DataPoint
    public final static Object[] dp4 = new Object[] { keys2, 16 };
    @DataPoint
    public final static Object[] dp5 = new Object[] { keys1, 4 };
    @DataPoint
    public final static Object[] dp6 = new Object[] { keys2, 8 };
    @DataPoint
    public final static Object[] dp7 = new Object[] { keys1, 64 };
    @DataPoint
    public final static Object[] dp8 = new Object[] { keys2, 64 };

    protected abstract ImmutableShortPair2IntHash hash(short[][] keyPairs, int modulus);

    private ImmutableShortPair2IntHash hash(final Object[] datapoint, final short[][] keyPairs) {
        return (datapoint.length > 1 ? hash(keyPairs, ((Integer) datapoint[1]).intValue()) : hash(keyPairs, 0));
    }

    @Theory
    public void testHashcode(final Object[] datapoint) {
        final short[][] keyPairs = keyPairs((short[]) datapoint[0]);
        verifyHash(hash(datapoint, keyPairs), keyPairs);
    }

    @Theory
    public void testKey1(final Object[] datapoint) {
        final short[][] keyPairs = keyPairs((short[]) datapoint[0]);
        final ImmutableShortPair2IntHash hash = hash(datapoint, keyPairs);

        for (int i = 0; i < keyPairs[0].length; i++) {
            assertEquals(keyPairs[0][i], hash.key1(hash.hashcode(keyPairs[0][i], keyPairs[1][i])));
        }
    }

    @Theory
    public void testKey2(final Object[] datapoint) {
        final short[][] keyPairs = keyPairs((short[]) datapoint[0]);
        final ImmutableShortPair2IntHash hash = hash(datapoint, keyPairs);

        for (int i = 0; i < keyPairs[0].length; i++) {
            assertEquals(keyPairs[1][i], hash.key2(hash.hashcode(keyPairs[0][i], keyPairs[1][i])));
        }
    }

    private void verifyHash(final ImmutableShortPair2IntHash hash, final short[][] keyPairs) {
        final short[][] nonKeyPairs = nonKeyPairs(keyPairs);
        for (int i = 0; i < keyPairs[0].length; i++) {
            assertTrue(hash.hashcode(keyPairs[0][i], keyPairs[1][i]) >= 0);
        }

        for (int i = 0; i < nonKeyPairs[0].length; i++) {
            assertEquals(Integer.MIN_VALUE, hash.hashcode(nonKeyPairs[0][i], nonKeyPairs[1][i]));
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
        final ShortArrayList keyList = new ShortArrayList();

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] split = line.split(",");
            assertEquals(2, split.length);
            final short i1 = Short.parseShort(split[0]);
            final short i2 = Short.parseShort(split[1]);
            keyList.add(i1);
            keyList.add(i2);
        }
        final short[][] keyPairs = keyPairs(keyList.toShortArray());
        final ImmutableShortPair2IntHash hash = hash(keyPairs, 0);
        verifyHash(hash, keyPairs);
    }

    private short[][] keyPairs(final short[] keys) {
        final short[][] keyPairs = new short[2][keys.length / 2];
        for (int i = 0; i < keys.length; i = i + 2) {
            keyPairs[0][i / 2] = keys[i];
            keyPairs[1][i / 2] = keys[i + 1];
        }
        return keyPairs;
    }

    private short[][] nonKeyPairs(final short[][] keyPairs) {

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
        final short[][] nonKeyPairs = new short[2][nonKeys.size()];
        int i = 0;
        for (final int nk : nonKeys) {
            nonKeyPairs[0][i] = (short) (nk >> packingShift);
            nonKeyPairs[1][i++] = (short) (nk & packingMask);
        }
        return nonKeyPairs;
    }
}
