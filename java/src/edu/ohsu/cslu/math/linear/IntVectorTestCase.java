package edu.ohsu.cslu.math.linear;

import static junit.framework.Assert.assertEquals;

public abstract class IntVectorTestCase extends VectorTestCase
{
    @Override
    public void testScalarAdd() throws Exception
    {
        Vector v = sampleVector.scalarAdd(1);
        assertEquals("Wrong class", IntVector.class, v.getClass());
        assertEquals("Wrong value", -10, v.getInt(0));
        assertEquals("Wrong value", 1, v.getInt(1));
        assertEquals("Wrong value", 12, v.getInt(2));
        assertEquals("Wrong value", 23, v.getInt(3));
        assertEquals("Wrong value", 34, v.getInt(4));
        assertEquals("Wrong value", 45, v.getInt(5));
        assertEquals("Wrong value", 57, v.getInt(6));
        assertEquals("Wrong value", 68, v.getInt(7));
        assertEquals("Wrong value", 79, v.getInt(8));
        assertEquals("Wrong value", 90, v.getInt(9));
        assertEquals("Wrong value", 101, v.getInt(10));

        v = sampleVector.scalarAdd(-2.5f);
        assertEquals("Wrong class", FloatVector.class, v.getClass());
        assertEquals("Wrong value", -13.5f, v.getFloat(0), .001f);
        assertEquals("Wrong value", -2.5f, v.getFloat(1), .001f);
        assertEquals("Wrong value", 8.5f, v.getFloat(2), .001f);
        assertEquals("Wrong value", 19.5f, v.getFloat(3), .001f);
        assertEquals("Wrong value", 30.5f, v.getFloat(4), .001f);
        assertEquals("Wrong value", 41.5f, v.getFloat(5), .001f);
        assertEquals("Wrong value", 53.5f, v.getFloat(6), .001f);
        assertEquals("Wrong value", 64.5f, v.getFloat(7), .001f);
        assertEquals("Wrong value", 75.5f, v.getFloat(8), .001f);
        assertEquals("Wrong value", 86.5f, v.getFloat(9), .001f);
        assertEquals("Wrong value", 97.5f, v.getFloat(10), .001f);
    }

    @Override
    public void testScalarMultiply() throws Exception
    {
        Vector v = sampleVector.scalarMultiply(3);
        assertEquals("Wrong class", IntVector.class, v.getClass());
        assertEquals("Wrong value", -33, v.getInt(0));
        assertEquals("Wrong value", 0, v.getInt(1));
        assertEquals("Wrong value", 33, v.getInt(2));
        assertEquals("Wrong value", 66, v.getInt(3));
        assertEquals("Wrong value", 99, v.getInt(4));
        assertEquals("Wrong value", 132, v.getInt(5));
        assertEquals("Wrong value", 168, v.getInt(6));
        assertEquals("Wrong value", 201, v.getInt(7));
        assertEquals("Wrong value", 234, v.getInt(8));
        assertEquals("Wrong value", 267, v.getInt(9));
        assertEquals("Wrong value", 300, v.getInt(10));

        v = sampleVector.scalarMultiply(-2.5f);
        assertEquals("Wrong class", FloatVector.class, v.getClass());
        assertEquals("Wrong value", 27.5f, v.getFloat(0), .001f);
        assertEquals("Wrong value", 0f, v.getFloat(1), .001f);
        assertEquals("Wrong value", -27.5f, v.getFloat(2), .001f);
        assertEquals("Wrong value", -55f, v.getFloat(3), .001f);
        assertEquals("Wrong value", -82.5f, v.getFloat(4), .001f);
        assertEquals("Wrong value", -110f, v.getFloat(5), .001f);
        assertEquals("Wrong value", -140f, v.getFloat(6), .001f);
        assertEquals("Wrong value", -167.5f, v.getFloat(7), .001f);
        assertEquals("Wrong value", -195f, v.getFloat(8), .001f);
        assertEquals("Wrong value", -222.5f, v.getFloat(9), .001f);
        assertEquals("Wrong value", -250f, v.getFloat(10), .001f);
    }

    @Override
    public void testDotProduct() throws Exception
    {
        Vector v = new IntVector(new int[] {1, 2, 3, 4});
        assertEquals(49, v.dotProduct(new IntVector(new int[] {4, 5, 5, 5})), .01f);
        assertEquals(49f, v.dotProduct(new FloatVector(new float[] {4, 5, 5, 5})), .01f);
        assertEquals(5f, v.dotProduct(new BitVector(new int[] {0, 1, 1, 0})), .01f);
        assertEquals(49, v.dotProduct(new PackedIntVector(new int[] {4, 5, 5, 5}, 4)), .01f);
    }
}
