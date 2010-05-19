package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public abstract class TestHash {

    public final static int[] keys1 = new int[] { 0, 3, 4, 7, 10, 13, 15, 18, 19, 21, 22, 24, 26, 29, 30, 34 };

    @DataPoint
    public final static int[] keys2 = new int[] { 10, 23, 54, 77, 103, 123, 157, 118, 198, 221, 322, 324,
            426, 529, 530, 1034 };

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

    protected abstract ImmutableInt2IntHash hash(int[] keys, int modulus);

    @Theory
    public void testContainsKey(final Object[] datapoint) {
        final int[] keys = (int[]) datapoint[0];
        final IntSet nonKeys = nonKeys(keys);

        final ImmutableInt2IntHash hash = (datapoint.length > 1 ? hash(keys, ((Integer) datapoint[1])
            .intValue()) : hash(keys, 0));

        for (final int key : keys) {
            assertTrue(hash.containsKey(key));
        }
        for (final int nonkey : nonKeys) {
            assertFalse(hash.containsKey(nonkey));
        }
    }

    @Theory
    public void testIndex(final Object[] datapoint) {
        final int[] keys = (int[]) datapoint[0];
        final IntSet nonKeys = nonKeys(keys);

        final ImmutableInt2IntHash hash = (datapoint.length > 1 ? hash(keys, ((Integer) datapoint[1])
            .intValue()) : hash(keys, 0));

        for (final int key : keys) {
            assertTrue(hash.hashcode(key) >= 0);
        }
        for (final int nonkey : nonKeys) {
            assertTrue(hash.hashcode(nonkey) == Integer.MIN_VALUE);
        }
    }

    private IntSet nonKeys(final int[] keys) {
        final IntSet nonKeys = new IntOpenHashSet();
        for (final int key : keys) {
            nonKeys.add(key + 1);
            nonKeys.add(key * 2);
        }
        nonKeys.removeAll(new IntOpenHashSet(keys));
        return nonKeys;
    }
}
