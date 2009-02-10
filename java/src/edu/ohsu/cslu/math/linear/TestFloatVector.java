package edu.ohsu.cslu.math.linear;

import org.junit.Before;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests for {@link FloatVector}
 * 
 * @author Aaron Dunlop
 * @since Dec 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestFloatVector extends VectorTestCase
{
    private float[] sampleArray;

    @Override
    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vector type=float length=11\n");
        sb
            .append("-11.000000 0.000000 11.000000 22.000000 33.000000 44.000000 56.000000 67.000000 78.000000 89.000000 100.000000\n");
        stringSampleVector = sb.toString();

        sampleArray = new float[] {-11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100};
        sampleVector = new FloatVector(sampleArray);

        vectorClass = FloatVector.class;
    }

    @Override
    protected Vector create(float[] array)
    {
        float[] floatArray = new float[array.length];
        for (int i = 0; i < array.length; i++)
        {
            floatArray[i] = array[i];
        }
        return new FloatVector(floatArray);
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(Float.POSITIVE_INFINITY, sampleVector.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(Float.NEGATIVE_INFINITY, sampleVector.negativeInfinity(), .01f);
    }

    @Override
    public void testScalarAdd() throws Exception
    {
        Vector v = sampleVector.scalarAdd(-2.5f);
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
        Vector v = sampleVector.scalarMultiply(-2.5f);
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
        Vector v = new FloatVector(new float[] {1, 2, 3, 4});
        assertEquals(49f, v.dotProduct(new IntVector(new int[] {4, 5, 5, 5})), .01f);
        assertEquals(49f, v.dotProduct(new FloatVector(new float[] {4, 5, 5, 5})), .01f);
        assertEquals(5f, v.dotProduct(new BitVector(new int[] {0, 1, 1, 0})), .01f);
        assertEquals(49f, v.dotProduct(new PackedIntVector(new int[] {4, 5, 5, 5}, 4)), .01f);
    }
}
