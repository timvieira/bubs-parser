/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.hash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.JUnit;

@RunWith(Theories.class)
public abstract class ImmutableInt2IntHashTestCase {

    public final static int[] keys1 = new int[] { 0, 3, 4, 7, 10, 13, 15, 18, 19, 21, 22, 24, 26, 29, 30, 34 };

    @DataPoint
    public final static int[] keys2 = new int[] { 10, 23, 54, 77, 103, 123, 157, 118, 198, 221, 322, 324, 426, 529,
            530, 1034 };

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
    public void testHashcode(final Object[] datapoint) {
        final int[] keys = (int[]) datapoint[0];

        final ImmutableInt2IntHash hash = (datapoint.length > 1 ? hash(keys, ((Integer) datapoint[1]).intValue())
                : hash(keys, 0));

        verifyHash(hash, keys);
    }

    @Test
    public void testR2Grammar() throws Exception {
        final BufferedReader br = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("hash/f2_21_R2.rm")));
        final IntArrayList keyList = new IntArrayList();

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] split = line.split(",");
            assertEquals(2, split.length);
            final int i1 = Integer.parseInt(split[0]);
            final int i2 = Integer.parseInt(split[1]);
            keyList.add(i1 << 7 | i2);
        }
        final int[] keys = keyList.toIntArray();
        final ImmutableInt2IntHash hash = hash(keys, 0);
        verifyHash(hash, keys);
    }

    protected void verifyHash(final ImmutableInt2IntHash hash, final int[] keys) {
        final IntSet nonKeys = nonKeys(keys);
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
