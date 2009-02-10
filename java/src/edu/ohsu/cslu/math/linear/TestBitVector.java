package edu.ohsu.cslu.math.linear;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@link BitVector} implementations
 * 
 * @author Aaron Dunlop
 * @since Sep 11, 2008
 * 
 *        $Id$
 */
public class TestBitVector extends VectorTestCase
{
    private int[] sampleArray;

    @Override
    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vector type=bit length=35\n");
        sb.append("1 0 1 1 1 0 0 1 1 0 1 1 1 0 1 0 0 0 1 0 1 1 0 1 1 1 1 0 0 1 0 0 1 1 1\n");
        stringSampleVector = sb.toString();

        sampleArray = new int[] {1, 0, 1, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0,
                                 1, 0, 0, 1, 1, 1};
        sampleVector = new BitVector(sampleArray);

        vectorClass = BitVector.class;
    }

    @Override
    protected Vector create(float[] array)
    {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++)
        {
            intArray[i] = array[i] == 0 ? 0 : 1;
        }
        return new BitVector(intArray);
    }

    @Override
    @Test
    public void testLength() throws Exception
    {
        assertEquals("Wrong length", 35, sampleVector.length());
    }

    @Override
    @Test
    public void testGetInt() throws Exception
    {
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
    public void testGetBoolean() throws Exception
    {
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
    public void testGetFloat() throws Exception
    {
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
    public void testSet() throws Exception
    {
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
    public void testSubVector() throws Exception
    {
        // Single-element subvector
        Vector subvector = sampleVector.subVector(0, 0);
        assertEquals("Wrong length", 1, subvector.length());
        assertEquals(1, subvector.getInt(0));

        // 2-element subvector
        subvector = sampleVector.subVector(4, 5);
        assertEquals("Wrong length", 2, subvector.length());
        assertEquals(1, subvector.getInt(0));
        assertEquals(0, subvector.getInt(1));

        // And a 4-element subvector spanning an integer split-point
        subvector = sampleVector.subVector(30, 33);
        assertEquals("Wrong length", 4, subvector.length());
        assertEquals(0, subvector.getInt(0));
        assertEquals(0, subvector.getInt(1));
        assertEquals(1, subvector.getInt(2));
        assertEquals(1, subvector.getInt(3));

    }

    /**
     * Not terribly meaningful for {@link BitVector}, but we'll test it anyway.
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testMin() throws Exception
    {
        assertEquals(0f, sampleVector.min(), .01f);
        assertEquals(0, sampleVector.intMin());
        assertEquals(1, sampleVector.argMin());
    }

    /**
     * Not terribly meaningful for {@link BitVector}, but we'll test it anyway.
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testMax() throws Exception
    {
        assertEquals(1, sampleVector.max(), .01f);
        assertEquals(1, sampleVector.intMax());
        assertEquals(0, sampleVector.argMax());
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(1, sampleVector.infinity(), .001f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(0, sampleVector.negativeInfinity(), .001f);
    }

    @Override
    public void testScalarAdd() throws Exception
    {
        Vector v = sampleVector.scalarAdd(2);
        assertEquals("Wrong class", IntVector.class, v.getClass());
        assertEquals(91, v.sum(), .01f);

        v = sampleVector.scalarAdd(2.5f);
        assertEquals("Wrong class", FloatVector.class, v.getClass());
        assertEquals(108.5f, v.sum(), .01f);
    }

    @Override
    public void testScalarMultiply() throws Exception
    {
        Vector v = sampleVector.scalarMultiply(2);
        assertEquals("Wrong class", IntVector.class, v.getClass());
        assertEquals(42, v.sum(), .01f);

        v = sampleVector.scalarMultiply(2.5f);
        assertEquals("Wrong class", FloatVector.class, v.getClass());
        assertEquals(52.5f, v.sum(), .01f);
    }

    @Override
    public void testDotProduct() throws Exception
    {
        IntVector intVector = new IntVector(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                                       19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
                                                       35});
        FloatVector floatVector = new FloatVector(new float[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                                                               17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
                                                               31, 32, 33, 34, 35});

        assertEquals(377f, sampleVector.dotProduct(intVector), .01f);
        assertEquals(377f, sampleVector.dotProduct(floatVector), .01f);
    }

    @Test
    public void testAdd()
    {
        BitVector bitmap = createEmptyBitmap();
        assertFalse("Did not expect 3", bitmap.contains(3));
        bitmap.add(3);
        assertTrue("Expected 3", bitmap.contains(3));
        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 2", bitmap.contains(2));
        assertFalse("Did not expect 4", bitmap.contains(4));
    }

    @Test
    public void testArrayAddAll()
    {
        BitVector bitmap = createEmptyBitmap();
        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 3", bitmap.contains(3));
        bitmap.addAll(new int[] {1, 3});
        assertTrue("Expected 1", bitmap.contains(3));
        assertTrue("Expected 3", bitmap.contains(3));
        assertFalse("Did not expect 2", bitmap.contains(2));
        assertFalse("Did not expect 4", bitmap.contains(4));
    }

    @Test
    public void testIntSetAddAll()
    {
        BitVector bitmap = createEmptyBitmap();
        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 3", bitmap.contains(3));

        bitmap.addAll(new IntOpenHashSet(Arrays.asList(1, 3)));

        assertTrue("Expected 1", bitmap.contains(3));
        assertTrue("Expected 3", bitmap.contains(3));
        assertFalse("Did not expect 2", bitmap.contains(2));
        assertFalse("Did not expect 4", bitmap.contains(4));
    }

    @Test
    public void testRemove()
    {
        BitVector bitmap = createEmptyBitmap();
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
    public void testArrayRemoveAll()
    {
        BitVector bitmap = createEmptyBitmap();
        bitmap.addAll(new int[] {1, 2, 3, 4});
        assertTrue("Expected 1", bitmap.contains(1));
        assertTrue("Expected 3", bitmap.contains(3));

        bitmap.removeAll(new int[] {1, 3});

        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 3", bitmap.contains(3));
        assertTrue("Expected 2", bitmap.contains(2));
        assertTrue("Expected 4", bitmap.contains(4));
    }

    @Test
    public void testIntSetRemoveAll()
    {
        BitVector bitmap = createEmptyBitmap();
        bitmap.addAll(new int[] {1, 2, 3, 4});
        assertTrue("Expected 1", bitmap.contains(1));
        assertTrue("Expected 3", bitmap.contains(3));

        bitmap.removeAll(new IntOpenHashSet(Arrays.asList(1, 3)));

        assertFalse("Did not expect 1", bitmap.contains(1));
        assertFalse("Did not expect 3", bitmap.contains(3));
        assertTrue("Expected 2", bitmap.contains(2));
        assertTrue("Expected 4", bitmap.contains(4));
    }

    private BitVector createEmptyBitmap()
    {
        return new BitVector(16);
    }

}
