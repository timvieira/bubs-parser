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
package edu.ohsu.cslu.datastructs.vectors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link BitVector} implementations
 * 
 * @author Aaron Dunlop
 * @since Sep 11, 2008
 * 
 *        $Id$
 */
public abstract class BitVectorTestCase<V extends BitVector> extends VectorTestCase<V> {

    @Override
    @Before
    public final void setUp() throws Exception {
        final long[] sampleArray = new long[] { 0, 2, 3, 4, 7, 8, 10, 11, 12, 14, 18, 20, 21, 23, 24, 25, 26, 29, 32,
                33, 34 };
        sampleVector = create(35, sampleArray);

        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=" + serializedName() + " length=35 sparse="
                + ((sampleVector instanceof SparseVector) ? "true\n" : "false\n"));
        sb.append("0 2 3 4 7 8 10 11 12 14 18 20 21 23 24 25 26 29 32 33 34\n");
        stringSampleVector = sb.toString();
    }

    protected BitVector create() {
        return create(0, new long[0]);
    }

    /**
     * Returns a {@link Vector} instance of the class under test.
     * 
     * @param array
     * @return vector
     */
    @Override
    protected final V create(final float[] array) {
        final LongArrayList elements = new LongArrayList();
        long length = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] != 0) {
                elements.add(i);
                length = i + 1;
            }
        }

        return create(length, elements.toLongArray());
    }

    /**
     * Returns a {@link Vector} instance of the class under test.
     * 
     * @param array
     * @return vector
     */
    protected final V create(final long length, final long[] array) {

        try {
            return vectorClass().getConstructor(new Class[] { long.class, long[].class }).newInstance(
                    new Object[] { length, array });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void testVectorAdd() throws Exception {
        final Vector vector = create(new float[] { 1, 0, 1, 1 });
        final DenseIntVector intVector = new DenseIntVector(new int[] { 1, 2, 3, 4 });
        final DenseFloatVector floatVector = new DenseFloatVector(new float[] { 4, 3, 2, 1 });

        // If we add an {@link IntVector} we should get a new {@link IntVector}
        Vector sum = intVector.add(intVector);
        assertEquals("Wrong class: " + sum.getClass().getName(), DenseIntVector.class, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0));
        assertEquals("Wrong value", 4, sum.getInt(1));
        assertEquals("Wrong value", 6, sum.getInt(2));
        assertEquals("Wrong value", 8, sum.getInt(3));

        // If we add a {@link FloatVector} we should get a {@link FloatVector}
        sum = intVector.add(floatVector);
        assertEquals("Wrong class: " + sum.getClass().getName(), DenseFloatVector.class, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 5, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 5, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 5, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 5, sum.getFloat(3), .01f);

        // If we add a {@link PackedBitVector} we should get a new {@link IntVector}
        sum = vector.add(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertEquals("Wrong class: " + sum.getClass().getName(), DenseIntVector.class, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0));
        assertEquals("Wrong value", 1, sum.getInt(1));
        assertEquals("Wrong value", 1, sum.getInt(2));
        assertEquals("Wrong value", 1, sum.getInt(3));

        // If we add a {@link SparseBitVector} we should get a new {@link IntVector}
        sum = vector.add(new SparseBitVector(4, new int[] { 1, 2 }));
        assertEquals("Wrong class: " + sum.getClass().getName(), DenseIntVector.class, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 1, sum.getInt(0));
        assertEquals("Wrong value", 1, sum.getInt(1));
        assertEquals("Wrong value", 2, sum.getInt(2));
        assertEquals("Wrong value", 1, sum.getInt(3));
    }

    @Override
    public void testElementwiseMultiply() throws Exception {
        final Vector vector = create(new float[] { 1, 0, 1, 1 });
        final IntVector intVector = createIntVector(new int[] { 1, 2, 3, 4 });
        final FloatVector floatVector = createFloatVector(new float[] { 4, 3, 2, 1 });

        // If we multiply by an {@link IntVector} we should get a new {@link IntVector}
        Vector product = vector.elementwiseMultiply(intVector);
        assertEquals("Wrong class: " + product.getClass().getName(), DenseIntVector.class, product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getInt(0));
        assertEquals("Wrong value", 0, product.getInt(1));
        assertEquals("Wrong value", 3, product.getInt(2));
        assertEquals("Wrong value", 4, product.getInt(3));

        // If we multiply by a {@link FloatVector} we should get a {@link FloatVector}
        product = vector.elementwiseMultiply(floatVector);
        assertEquals("Wrong class: " + product.getClass().getName(), DenseFloatVector.class, product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 4, product.getFloat(0), .01f);
        assertEquals("Wrong value", 0, product.getFloat(1), .01f);
        assertEquals("Wrong value", 2, product.getFloat(2), .01f);
        assertEquals("Wrong value", 1, product.getFloat(3), .01f);

        // If we multiply by a {@link SparseBitVector} we should get a new {@link SparseBitVector}
        product = vector.elementwiseMultiply(createBitVector(4, new long[] { 2 }));
        assertTrue("Wrong class: " + product.getClass(), product instanceof BitVector);
        assertEquals("Wrong value", 0, product.getInt(0));
        assertEquals("Wrong value", 0, product.getInt(1));
        assertEquals("Wrong value", 1, product.getInt(2));
        assertEquals("Wrong value", 0, product.getInt(3));

        // Multiply by a {@link MutableSparseBitVector}
        product = vector.elementwiseMultiply(createMutableBitVector(4, new long[] { 2 }));
        assertEquals("Wrong value", 0, product.getInt(0));
        assertEquals("Wrong value", 0, product.getInt(1));
        assertEquals("Wrong value", 1, product.getInt(2));
        assertEquals("Wrong value", 0, product.getInt(3));
    }

    @Override
    @Test
    public void testLength() throws Exception {
        assertEquals("Wrong length", 35, sampleVector.length());
    }

    @Override
    @Test
    public void testGetInt() throws Exception {
        assertEquals("Wrong value", 1, sampleVector.getInt(0));
        assertEquals("Wrong value", 0, sampleVector.getInt(1));
        assertEquals("Wrong value", 1, sampleVector.getInt(2));
        assertEquals("Wrong value", 1, sampleVector.getInt(3));
        assertEquals("Wrong value", 1, sampleVector.getInt(4));
        assertEquals("Wrong value", 0, sampleVector.getInt(5));
        assertEquals("Wrong value", 1, sampleVector.getInt(32));
        assertEquals("Wrong value", 1, sampleVector.getInt(33));
        assertEquals("Wrong value", 1, sampleVector.getInt(34));
    }

    @Override
    @Test
    public void testGetBoolean() throws Exception {
        assertEquals("Wrong value", true, sampleVector.getBoolean(0));
        assertEquals("Wrong value", false, sampleVector.getBoolean(1));
        assertEquals("Wrong value", true, sampleVector.getBoolean(2));
        assertEquals("Wrong value", true, sampleVector.getBoolean(3));
        assertEquals("Wrong value", true, sampleVector.getBoolean(4));
        assertEquals("Wrong value", false, sampleVector.getBoolean(5));
        assertEquals("Wrong value", true, sampleVector.getBoolean(32));
        assertEquals("Wrong value", true, sampleVector.getBoolean(33));
        assertEquals("Wrong value", true, sampleVector.getBoolean(34));
    }

    @Override
    @Test
    public void testGetFloat() throws Exception {
        assertEquals("Wrong value", 1, sampleVector.getFloat(0), .001f);
        assertEquals("Wrong value", 0, sampleVector.getFloat(1), .001f);
        assertEquals("Wrong value", 1, sampleVector.getFloat(2), .001f);
        assertEquals("Wrong value", 1, sampleVector.getFloat(3), .001f);
        assertEquals("Wrong value", 1, sampleVector.getFloat(4), .001f);
        assertEquals("Wrong value", 0, sampleVector.getFloat(5), .001f);
        assertEquals("Wrong value", 1, sampleVector.getFloat(32), .001f);
        assertEquals("Wrong value", 1, sampleVector.getFloat(33), .001f);
        assertEquals("Wrong value", 1, sampleVector.getFloat(34), .001f);
    }

    @Override
    @Test
    public void testSet() throws Exception {
        assertEquals("Wrong value", 0, sampleVector.getInt(1));
        assertEquals("Wrong value", 1, sampleVector.getInt(2));
        assertEquals("Wrong value", 1, sampleVector.getInt(33));
        sampleVector.set(2, 0);
        sampleVector.set(1, 1);
        sampleVector.set(33, 0);
        assertEquals("Wrong value", 1, sampleVector.getInt(1));
        assertEquals("Wrong value", 0, sampleVector.getInt(2));
        assertEquals("Wrong value", 0, sampleVector.getInt(33));
    }

    @Override
    @Test
    public void testSubVector() throws Exception {
        // Single-element subvector
        Vector subvector = sampleVector.subVector(0, 0);
        assertEquals("Wrong length", 1, subvector.length());
        assertEquals(1, subvector.getInt(0));

        // 2-element subvector
        subvector = sampleVector.subVector(6, 7);
        assertEquals("Wrong length", 2, subvector.length());
        assertEquals(0, subvector.getInt(0));
        assertEquals(1, subvector.getInt(1));

        // And a 4-element subvector spanning an integer split-point (in PackedBitVector)
        subvector = sampleVector.subVector(30, 33);
        assertEquals("Wrong length", 4, subvector.length());
        assertEquals(0, subvector.getInt(0));
        assertEquals(0, subvector.getInt(1));
        assertEquals(1, subvector.getInt(2));
        assertEquals(1, subvector.getInt(3));
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(1, sampleVector.infinity(), .001f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(0, sampleVector.negativeInfinity(), .001f);
    }

    @Override
    public void testDotProduct() throws Exception {
        final DenseIntVector intVector = new DenseIntVector(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35 });
        final DenseFloatVector floatVector = new DenseFloatVector(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35 });

        assertEquals(377f, sampleVector.dotProduct(intVector), .01f);
        assertEquals(377f, sampleVector.dotProduct(floatVector), .01f);
    }

    @Test
    public void testSetAdd() {
        final BitVector bitmap = create();
        assertFalse("Did not expect 3", bitmap.contains(3));
        bitmap.add(3);
        assertTrue("Expected 3", bitmap.contains(3));
        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 2", bitmap.contains(2));
        assertFalse("Did not expect 4", bitmap.contains(4));
    }

    @Test
    public void testArrayAddAll() {
        final BitVector bitmap = create();
        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 3", bitmap.contains(3));
        bitmap.addAll(new int[] { 1, 3 });
        assertTrue("Expected 1", bitmap.contains(3));
        assertTrue("Expected 3", bitmap.contains(3));
        assertFalse("Did not expect 2", bitmap.contains(2));
        assertFalse("Did not expect 4", bitmap.contains(4));
    }

    @Test
    public void testIntSetAddAll() {
        final BitVector bitmap = create();
        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 3", bitmap.contains(3));

        bitmap.addAll(new IntOpenHashSet(Arrays.asList(1, 3)));

        assertTrue("Expected 1", bitmap.contains(3));
        assertTrue("Expected 3", bitmap.contains(3));
        assertFalse("Did not expect 2", bitmap.contains(2));
        assertFalse("Did not expect 4", bitmap.contains(4));
    }

    @Test
    public void testRemove() {
        final BitVector bitmap = create();
        bitmap.add(1);
        bitmap.add(2);
        bitmap.add(3);
        bitmap.add(4);
        assertTrue("Expected 3", bitmap.contains(3));

        // We expect remove to return true when removing 3
        assertTrue(bitmap.remove(3));

        // And to return false when 3 is already removed
        assertFalse(bitmap.remove(3));

        assertFalse("Did not expect 3", bitmap.contains(3));
        assertTrue("Expected 1", bitmap.contains(1));
        assertTrue("Expected 2", bitmap.contains(2));
        assertTrue("Expected 4", bitmap.contains(4));
    }

    @Test
    public void testArrayRemoveAll() {
        final BitVector bitmap = create();
        bitmap.addAll(new int[] { 1, 2, 3, 4 });
        assertTrue("Expected 1", bitmap.contains(1));
        assertTrue("Expected 3", bitmap.contains(3));

        bitmap.removeAll(new int[] { 1, 3 });

        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 3", bitmap.contains(3));
        assertTrue("Expected 2", bitmap.contains(2));
        assertTrue("Expected 4", bitmap.contains(4));
    }

    @Test
    public void testIntSetRemoveAll() {
        final BitVector bitmap = create();
        bitmap.addAll(new int[] { 1, 2, 3, 4 });
        assertTrue("Expected 1", bitmap.contains(1));
        assertTrue("Expected 3", bitmap.contains(3));

        bitmap.removeAll(new IntOpenHashSet(Arrays.asList(1, 3)));

        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 3", bitmap.contains(3));
        assertTrue("Expected 2", bitmap.contains(2));
        assertTrue("Expected 4", bitmap.contains(4));
    }

    public void testValues() {
        final IntSet intSet = new IntOpenHashSet(((BitVector) sampleVector).values());

        assertEquals(21, intSet.size());

        assertTrue(intSet.contains(0));
        assertTrue(intSet.contains(2));
        assertTrue(intSet.contains(3));
        assertTrue(intSet.contains(4));
        assertTrue(intSet.contains(7));
        assertTrue(intSet.contains(8));
        assertTrue(intSet.contains(10));
        assertTrue(intSet.contains(11));
        assertTrue(intSet.contains(12));
        assertTrue(intSet.contains(14));
        assertTrue(intSet.contains(18));
        assertTrue(intSet.contains(20));
        assertTrue(intSet.contains(21));
        assertTrue(intSet.contains(23));
        assertTrue(intSet.contains(24));
        assertTrue(intSet.contains(25));
        assertTrue(intSet.contains(26));
        assertTrue(intSet.contains(29));
        assertTrue(intSet.contains(32));
        assertTrue(intSet.contains(33));
        assertTrue(intSet.contains(34));
    }
}
