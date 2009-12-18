package edu.ohsu.cslu.datastructs.vectors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for floating-point vectors.
 * 
 * @author Aaron Dunlop
 * @since Dec 16, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class FloatVectorTestCase extends NumericVectorTestCase
{
    @Override
    public void testVectorAdd() throws Exception
    {
        final Vector vector = create(new float[] {1, 2, 3, 4});
        final IntVector intVector = new IntVector(new int[] {1, 2, 3, 4});

        try
        {
            vector.add(create(new float[] {1}));
            fail("Expected IllegalArgumentException");
        }
        catch (final IllegalArgumentException expected)
        {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // If we add two {@link FloatVector}s, we should get another {@link FloatVector}
        Vector sum = vector.add(vector);
        assertFalse("Vector objects are the same", sum == vector);
        assertEquals("Wrong class", vectorClass, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 4, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 6, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 8, sum.getFloat(3), .01f);

        // If we add an {@link IntVector} we should get a {@link FloatVector}
        sum = vector.add(intVector);
        assertFalse("Vector objects are the same", sum == vector);
        assertEquals("Wrong class", vectorClass, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 4, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 6, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 8, sum.getFloat(3), .01f);

        // If we add a {@link PackedBitVector} we should get a new {@link FloatVector}
        sum = vector.add(new PackedBitVector(new int[] {1, 1, 0, 0}));
        assertFalse("Vector objects are the same", sum == vector);
        assertTrue("Wrong class: " + sum.getClass().getName(), sum instanceof NumericVector);
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0), .01f);
        assertEquals("Wrong value", 3, sum.getInt(1), .01f);
        assertEquals("Wrong value", 3, sum.getInt(2), .01f);
        assertEquals("Wrong value", 4, sum.getInt(3), .01f);

        // If we add a {@link SparseBitVector} we should get a new {@link FloatVector}
        sum = vector.add(new SparseBitVector(new int[] {1, 1, 2, 1}, true));
        assertFalse("Vector objects are the same", sum == vector);
        assertTrue("Wrong class: " + sum.getClass().getName(), sum instanceof NumericVector);
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 1, sum.getInt(0), .01f);
        assertEquals("Wrong value", 3, sum.getInt(1), .01f);
        assertEquals("Wrong value", 4, sum.getInt(2), .01f);
        assertEquals("Wrong value", 4, sum.getInt(3), .01f);
    }

    @Override
    public void testElementwiseMultiply() throws Exception
    {
        final Vector vector = create(new float[] {1, 2, 3, 4});
        final IntVector intVector = new IntVector(new int[] {1, 2, 3, 4});

        try
        {
            vector.elementwiseMultiply(create(new float[] {1}));
            fail("Expected IllegalArgumentException");
        }
        catch (final IllegalArgumentException expected)
        {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // If we multiply by a {@link FloatVector}s, we should get another {@link FloatVector}
        Vector product = vector.elementwiseMultiply(vector);
        assertFalse("Vector objects are the same", product == vector);
        assertEquals("Wrong class", vectorClass, product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getFloat(0), .01f);
        assertEquals("Wrong value", 4, product.getFloat(1), .01f);
        assertEquals("Wrong value", 9, product.getFloat(2), .01f);
        assertEquals("Wrong value", 16, product.getFloat(3), .01f);

        // If we multiply by an {@link IntVector} we should get a {@link FloatVector}
        product = vector.elementwiseMultiply(intVector);
        assertFalse("Vector objects are the same", product == vector);
        assertEquals("Wrong class", vectorClass, product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getFloat(0), .01f);
        assertEquals("Wrong value", 4, product.getFloat(1), .01f);
        assertEquals("Wrong value", 9, product.getFloat(2), .01f);
        assertEquals("Wrong value", 16, product.getFloat(3), .01f);

        // If we multiply by a {@link PackedBitVector} we should get a new {@link FloatVector}
        product = vector.elementwiseMultiply(new PackedBitVector(new int[] {1, 1, 0, 0}));
        assertFalse("Vector objects are the same", product == vector);
        assertTrue("Wrong class: " + product.getClass().getName(), product instanceof NumericVector);
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getInt(0), .01f);
        assertEquals("Wrong value", 2, product.getInt(1), .01f);
        assertEquals("Wrong value", 0, product.getInt(2), .01f);
        assertEquals("Wrong value", 0, product.getInt(3), .01f);

        // If we multiply by a {@link SparseBitVector} we should get a new {@link FloatVector}
        product = vector.elementwiseMultiply(new SparseBitVector(new int[] {1, 1, 2, 1}, true));
        assertFalse("Vector objects are the same", product == vector);
        assertTrue("Wrong class: " + product.getClass().getName(), product instanceof NumericVector);
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 0, product.getInt(0), .01f);
        assertEquals("Wrong value", 2, product.getInt(1), .01f);
        assertEquals("Wrong value", 3, product.getInt(2), .01f);
        assertEquals("Wrong value", 0, product.getInt(3), .01f);
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
        final Vector v = sampleVector.scalarAdd(-2.5f);
        assertFalse("Vector objects are the same", v == sampleVector);
        assertEquals("Wrong class", FloatVector.class, v.getClass());
        checkScalarAdd(v);
    }

    protected void checkScalarAdd(final Vector v)
    {
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
        final Vector v = sampleVector.scalarMultiply(-2.5f);
        assertFalse("Vector objects are the same", v == sampleVector);
        assertEquals("Wrong class", FloatVector.class, v.getClass());
        checkScalarMultiply(v);
    }

    protected void checkScalarMultiply(final Vector v)
    {
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
        final Vector v = new FloatVector(new float[] {1, 2, 3, 4});
        assertEquals(49f, v.dotProduct(new IntVector(new int[] {4, 5, 5, 5})), .01f);
        assertEquals(49f, v.dotProduct(new FloatVector(new float[] {4, 5, 5, 5})), .01f);
        assertEquals(5f, v.dotProduct(new PackedBitVector(new int[] {0, 1, 1, 0})), .01f);
        assertEquals(49f, v.dotProduct(new PackedIntVector(new int[] {4, 5, 5, 5}, 4)), .01f);
    }
}
