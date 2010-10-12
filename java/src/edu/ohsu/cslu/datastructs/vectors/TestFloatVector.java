package edu.ohsu.cslu.datastructs.vectors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for {@link FloatVector}.
 * 
 * @author Aaron Dunlop
 * @since Dec 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestFloatVector extends FloatVectorTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=float length=11\n");
        sb.append("-11.000000 0.000000 11.000000 22.000000 33.000000 44.000000 56.000000 67.000000 78.000000 89.000000 100.000000\n");
        stringSampleVector = sb.toString();

        final float[] sampleArray = new float[] { -11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100 };
        sampleVector = new FloatVector(sampleArray);

        vectorClass = FloatVector.class;
    }

    @Override
    protected Vector create(final float[] array) {
        final float[] floatArray = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            floatArray[i] = array[i];
        }
        return new FloatVector(floatArray);
    }

    @Test
    public void testInPlaceVectorAdd() throws Exception {
        FloatVector vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        final IntVector intVector = new IntVector(new int[] { 1, 2, 3, 4 });

        try {
            vector.inPlaceAdd(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // {@link FloatVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        Vector sum = vector.inPlaceAdd(vector);
        assertTrue("Vector objects are not the same", sum == vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), vectorClass, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 4, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 6, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 8, sum.getFloat(3), .01f);

        // {@link IntVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        sum = vector.inPlaceAdd(intVector);
        assertTrue("Vector objects are not the same", sum == vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), vectorClass, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 4, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 6, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 8, sum.getFloat(3), .01f);

        // {@link PackedBitVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        sum = vector.inPlaceAdd(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertTrue("Vector objects are not the same", sum == vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), FloatVector.class, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0), .01f);
        assertEquals("Wrong value", 3, sum.getInt(1), .01f);
        assertEquals("Wrong value", 3, sum.getInt(2), .01f);
        assertEquals("Wrong value", 4, sum.getInt(3), .01f);

        // {@link SparseBitVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        sum = vector.inPlaceAdd(new SparseBitVector(new int[] { 1, 1, 2, 1 }, true));
        assertTrue("Vector objects are not the same", sum == vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), FloatVector.class, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 1, sum.getInt(0), .01f);
        assertEquals("Wrong value", 3, sum.getInt(1), .01f);
        assertEquals("Wrong value", 4, sum.getInt(2), .01f);
        assertEquals("Wrong value", 4, sum.getInt(3), .01f);
    }

    @Test
    public void testInPlaceElementwiseMultiply() throws Exception {
        FloatVector vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        final IntVector intVector = new IntVector(new int[] { 1, 2, 3, 4 });

        try {
            vector.inPlaceElementwiseMultiply(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // If we multiply by a {@link FloatVector}s, we should get another {@link FloatVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        Vector product = vector.inPlaceElementwiseMultiply(vector);
        assertTrue("Vector objects are not the same", product == vector);
        assertEquals("Wrong class: " + product.getClass().getName(), vectorClass, product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getFloat(0), .01f);
        assertEquals("Wrong value", 4, product.getFloat(1), .01f);
        assertEquals("Wrong value", 9, product.getFloat(2), .01f);
        assertEquals("Wrong value", 16, product.getFloat(3), .01f);

        // If we multiply by an {@link IntVector} we should get a {@link FloatVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        product = vector.inPlaceElementwiseMultiply(intVector);
        assertTrue("Vector objects are not the same", product == vector);
        assertEquals("Wrong class: " + product.getClass().getName(), vectorClass, product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getFloat(0), .01f);
        assertEquals("Wrong value", 4, product.getFloat(1), .01f);
        assertEquals("Wrong value", 9, product.getFloat(2), .01f);
        assertEquals("Wrong value", 16, product.getFloat(3), .01f);

        // If we multiply by a {@link PackedBitVector} we should get a new {@link FloatVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        product = vector.inPlaceElementwiseMultiply(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertTrue("Vector objects are not the same", product == vector);
        assertEquals("Wrong class: " + product.getClass().getName(), FloatVector.class, product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getInt(0), .01f);
        assertEquals("Wrong value", 2, product.getInt(1), .01f);
        assertEquals("Wrong value", 0, product.getInt(2), .01f);
        assertEquals("Wrong value", 0, product.getInt(3), .01f);

        // If we multiply by a {@link SparseBitVector} we should get a new {@link FloatVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        product = vector.inPlaceElementwiseMultiply(new SparseBitVector(new int[] { 1, 1, 2, 1 }, true));
        assertTrue("Vector objects are not the same", product == vector);
        assertEquals("Wrong class: " + product.getClass().getName(), FloatVector.class, product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 0, product.getInt(0), .01f);
        assertEquals("Wrong value", 2, product.getInt(1), .01f);
        assertEquals("Wrong value", 3, product.getInt(2), .01f);
        assertEquals("Wrong value", 0, product.getInt(3), .01f);
    }

    @Test
    public void testInPlaceScalarAdd() throws Exception {
        final Vector v = ((FloatVector) sampleVector).inPlaceScalarAdd(-2.5f);
        assertTrue("Vector objects are not the same", v == sampleVector);
        checkScalarAdd(v);
    }

    @Test
    public void testInPlaceAddWithBitVector() throws Exception {
        final Vector v = ((FloatVector) sampleVector).inPlaceAdd(new SparseBitVector(new boolean[] { false, true,
                false, true, false, true, false, true, false, true, false }), -1f);
        assertTrue("Vector objects are not the same", v == sampleVector);

        assertEquals("Wrong value", -11f, v.getFloat(0), .001f);
        assertEquals("Wrong value", -1f, v.getFloat(1), .001f);
        assertEquals("Wrong value", 11f, v.getFloat(2), .001f);
        assertEquals("Wrong value", 21f, v.getFloat(3), .001f);
        assertEquals("Wrong value", 33f, v.getFloat(4), .001f);
        assertEquals("Wrong value", 43f, v.getFloat(5), .001f);
        assertEquals("Wrong value", 56f, v.getFloat(6), .001f);
        assertEquals("Wrong value", 66f, v.getFloat(7), .001f);
        assertEquals("Wrong value", 78f, v.getFloat(8), .001f);
        assertEquals("Wrong value", 88f, v.getFloat(9), .001f);
        assertEquals("Wrong value", 100f, v.getFloat(10), .001f);
    }

    @Test
    public void testInPlaceScalarMultiply() throws Exception {
        final Vector v = ((FloatVector) sampleVector).inPlaceScalarMultiply(-2.5f);
        assertTrue("Vector objects are not the same", v == sampleVector);
        checkScalarMultiply(v);
    }

    /**
     * Tests in-place element-wise division
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testInPlaceElementwiseDivide() throws Exception {
        FloatVector vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });

        try {
            vector.inPlaceElementwiseDivide(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // Divide by an {@link IntVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        FloatVector quotient = vector.inPlaceElementwiseDivide(new IntVector(new int[] { 1, 3, 6, 10 }));
        assertTrue("Vector objects are not the same", quotient == vector);
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);

        // Divide by a {@link FloatVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        quotient = vector.inPlaceElementwiseDivide(new FloatVector(new float[] { 1, 3, 6, 10 }));
        assertTrue("Vector objects are not the same", quotient == vector);
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);

        // Divide by a {@link PackedIntVector}
        vector = (FloatVector) create(new float[] { 1, 2, 3, 4 });
        quotient = vector.inPlaceElementwiseDivide(new PackedIntVector(new int[] { 1, 3, 6, 10 }, 4));
        assertTrue("Vector objects are not the same", quotient == vector);
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);
    }

    /**
     * Tests in-place element-wise division
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testInPlaceElementwiseLog() throws Exception {
        final FloatVector vector = (FloatVector) create(new float[] { 1, 10, 20, 30 });
        final FloatVector log = vector.inPlaceElementwiseLog();
        assertTrue("Vector objects are not the same", log == vector);
        assertEquals("Wrong length", 4, log.length());
        assertEquals("Wrong value", Math.log(1), log.getInt(0), .01f);
        assertEquals("Wrong value", Math.log(10), log.getFloat(1), .01f);
        assertEquals("Wrong value", Math.log(20), log.getFloat(2), .01f);
        assertEquals("Wrong value", Math.log(30), log.getFloat(3), .01f);
    }
}
