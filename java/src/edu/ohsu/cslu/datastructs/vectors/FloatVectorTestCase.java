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
import static junit.framework.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for floating-point vectors.
 * 
 * @author Aaron Dunlop
 * @since Dec 16, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class FloatVectorTestCase<V extends FloatVector> extends NumericVectorTestCase<V> {

    @Override
    public void setUp() throws Exception {
        final float[] sampleArray = new float[] { -11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100 };
        sampleVector = create(sampleArray);

        // This method is for sparse float vectors. Dense test classes will override it.
        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=" + serializedName() + " length=11 sparse=true\n");
        sb.append("0 -11.000000 1 0.000000 2 11.000000 3 22.000000 4 33.000000 5 44.000000 6 56.000000 7 67.000000 8 78.000000 9 89.000000 10 100.000000\n");
        stringSampleVector = sb.toString();
    }

    @Override
    public void testVectorAdd() throws Exception {
        final Vector vector = create(new float[] { 1, 2, 3, 4 });
        final DenseIntVector intVector = new DenseIntVector(new int[] { 1, 2, 3, 4 });

        try {
            vector.add(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // If we add two {@link FloatVector}s, we should get another {@link FloatVector}
        Vector sum = vector.add(vector);
        assertFalse("Vector objects are the same", sum == vector);
        assertEquals("Wrong class", vectorClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 4, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 6, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 8, sum.getFloat(3), .01f);

        // If we add an {@link IntVector} we should get a {@link FloatVector}
        sum = vector.add(intVector);
        assertFalse("Vector objects are the same", sum == vector);
        assertEquals("Wrong class", vectorClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 4, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 6, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 8, sum.getFloat(3), .01f);

        // If we add a {@link PackedBitVector} we should get a new {@link FloatVector}
        sum = vector.add(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertFalse("Vector objects are the same", sum == vector);
        assertTrue("Wrong class: " + sum.getClass().getName(), sum instanceof NumericVector);
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0), .01f);
        assertEquals("Wrong value", 3, sum.getInt(1), .01f);
        assertEquals("Wrong value", 3, sum.getInt(2), .01f);
        assertEquals("Wrong value", 4, sum.getInt(3), .01f);

        // If we add a {@link SparseBitVector} we should get a new {@link FloatVector}
        sum = vector.add(new SparseBitVector(4, new int[] { 1, 2 }));
        assertFalse("Vector objects are the same", sum == vector);
        assertTrue("Wrong class: " + sum.getClass().getName(), sum instanceof NumericVector);
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 1, sum.getInt(0), .01f);
        assertEquals("Wrong value", 3, sum.getInt(1), .01f);
        assertEquals("Wrong value", 4, sum.getInt(2), .01f);
        assertEquals("Wrong value", 4, sum.getInt(3), .01f);
    }

    @Override
    public void testElementwiseMultiply() throws Exception {
        final Vector vector = create(new float[] { 1, 2, 3, 4 });
        final DenseIntVector intVector = new DenseIntVector(new int[] { 1, 2, 3, 4 });

        try {
            vector.elementwiseMultiply(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // If we multiply by a {@link FloatVector}s, we should get another {@link FloatVector}
        Vector product = vector.elementwiseMultiply(vector);
        assertFalse("Vector objects are the same", product == vector);
        assertEquals("Wrong class", vectorClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getFloat(0), .01f);
        assertEquals("Wrong value", 4, product.getFloat(1), .01f);
        assertEquals("Wrong value", 9, product.getFloat(2), .01f);
        assertEquals("Wrong value", 16, product.getFloat(3), .01f);

        // If we multiply by an {@link IntVector} we should get a {@link FloatVector}
        product = vector.elementwiseMultiply(intVector);
        assertFalse("Vector objects are the same", product == vector);
        assertEquals("Wrong class", vectorClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getFloat(0), .01f);
        assertEquals("Wrong value", 4, product.getFloat(1), .01f);
        assertEquals("Wrong value", 9, product.getFloat(2), .01f);
        assertEquals("Wrong value", 16, product.getFloat(3), .01f);

        // If we multiply by a {@link PackedBitVector} we should get a new {@link FloatVector}
        product = vector.elementwiseMultiply(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertFalse("Vector objects are the same", product == vector);
        assertTrue("Wrong class: " + product.getClass().getName(), product instanceof NumericVector);
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getInt(0), .01f);
        assertEquals("Wrong value", 2, product.getInt(1), .01f);
        assertEquals("Wrong value", 0, product.getInt(2), .01f);
        assertEquals("Wrong value", 0, product.getInt(3), .01f);

        // If we multiply by a {@link SparseBitVector} we should get a new {@link FloatVector}
        product = vector.elementwiseMultiply(new SparseBitVector(new int[] { 1, 2 }));
        assertFalse("Vector objects are the same", product == vector);
        assertTrue("Wrong class: " + product.getClass().getName(), product instanceof NumericVector);
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 0, product.getInt(0), .01f);
        assertEquals("Wrong value", 2, product.getInt(1), .01f);
        assertEquals("Wrong value", 3, product.getInt(2), .01f);
        assertEquals("Wrong value", 0, product.getInt(3), .01f);
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(Float.POSITIVE_INFINITY, sampleVector.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(Float.NEGATIVE_INFINITY, sampleVector.negativeInfinity(), .01f);
    }

    @Override
    public void testScalarAdd() throws Exception {
        final NumericVector v = ((NumericVector) sampleVector).scalarAdd(-2.5f);
        assertFalse("Vector objects are the same", v == sampleVector);
        assertEquals("Wrong class", DenseFloatVector.class, v.getClass());
        checkScalarAdd(v);
    }

    protected void checkScalarAdd(final Vector v) {
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
    public void testScalarMultiply() throws Exception {
        final NumericVector v = ((NumericVector) sampleVector).scalarMultiply(-2.5f);
        assertFalse("Vector objects are the same", v == sampleVector);
        assertEquals("Wrong class", DenseFloatVector.class, v.getClass());
        checkScalarMultiply(v);
    }

    protected void checkScalarMultiply(final Vector v) {
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
    public void testDotProduct() throws Exception {
        final Vector v = new DenseFloatVector(new float[] { 1, 2, 3, 4 });
        assertEquals(49f, v.dotProduct(new DenseIntVector(new int[] { 4, 5, 5, 5 })), .01f);
        assertEquals(49f, v.dotProduct(new DenseFloatVector(new float[] { 4, 5, 5, 5 })), .01f);
        assertEquals(5f, v.dotProduct(new PackedBitVector(new int[] { 0, 1, 1, 0 })), .01f);
        assertEquals(49f, v.dotProduct(new PackedIntVector(new int[] { 4, 5, 5, 5 }, 4)), .01f);
    }

    @Test
    public void testInPlaceVectorAdd() throws Exception {
        FloatVector vector = create(new float[] { 1, 2, 3, 4 });
        final DenseIntVector intVector = new DenseIntVector(new int[] { 1, 2, 3, 4 });

        try {
            vector.inPlaceAdd(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // {@link FloatVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        Vector sum = vector.inPlaceAdd(vector);
        assertTrue("Vector objects are not the same", sum == vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), vectorClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 4, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 6, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 8, sum.getFloat(3), .01f);

        // {@link IntVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        sum = vector.inPlaceAdd(intVector);
        assertTrue("Vector objects are not the same", sum == vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), vectorClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 4, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 6, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 8, sum.getFloat(3), .01f);

        // {@link PackedBitVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        sum = vector.inPlaceAdd(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertTrue("Vector objects are not the same", sum == vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), vector.getClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0), .01f);
        assertEquals("Wrong value", 3, sum.getInt(1), .01f);
        assertEquals("Wrong value", 3, sum.getInt(2), .01f);
        assertEquals("Wrong value", 4, sum.getInt(3), .01f);

        // {@link SparseBitVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        sum = vector.inPlaceAdd(new SparseBitVector(new int[] { 1, 2 }));
        assertTrue("Vector objects are not the same", sum == vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), vector.getClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 1, sum.getInt(0), .01f);
        assertEquals("Wrong value", 3, sum.getInt(1), .01f);
        assertEquals("Wrong value", 4, sum.getInt(2), .01f);
        assertEquals("Wrong value", 4, sum.getInt(3), .01f);
    }

    @Test
    public void testInPlaceElementwiseMultiply() throws Exception {
        FloatVector vector = create(new float[] { 1, 2, 3, 4 });
        final DenseIntVector intVector = new DenseIntVector(new int[] { 1, 2, 3, 4 });

        try {
            vector.inPlaceElementwiseMultiply(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // If we multiply by a {@link FloatVector}s, we should get another {@link FloatVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        Vector product = vector.inPlaceElementwiseMultiply(vector);
        assertTrue("Vector objects are not the same", product == vector);
        assertEquals("Wrong class: " + product.getClass().getName(), vectorClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getFloat(0), .01f);
        assertEquals("Wrong value", 4, product.getFloat(1), .01f);
        assertEquals("Wrong value", 9, product.getFloat(2), .01f);
        assertEquals("Wrong value", 16, product.getFloat(3), .01f);

        // If we multiply by an {@link IntVector} we should get a {@link FloatVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        product = vector.inPlaceElementwiseMultiply(intVector);
        assertTrue("Vector objects are not the same", product == vector);
        assertEquals("Wrong class: " + product.getClass().getName(), vectorClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getFloat(0), .01f);
        assertEquals("Wrong value", 4, product.getFloat(1), .01f);
        assertEquals("Wrong value", 9, product.getFloat(2), .01f);
        assertEquals("Wrong value", 16, product.getFloat(3), .01f);

        // If we multiply by a {@link PackedBitVector} we should get a new {@link FloatVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        product = vector.inPlaceElementwiseMultiply(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertTrue("Vector objects are not the same", product == vector);
        assertEquals("Wrong class: " + product.getClass().getName(), vector.getClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getInt(0), .01f);
        assertEquals("Wrong value", 2, product.getInt(1), .01f);
        assertEquals("Wrong value", 0, product.getInt(2), .01f);
        assertEquals("Wrong value", 0, product.getInt(3), .01f);

        // If we multiply by a {@link SparseBitVector} we should get a new {@link FloatVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        product = vector.inPlaceElementwiseMultiply(new SparseBitVector(new int[] { 1, 2 }));
        assertTrue("Vector objects are not the same", product == vector);
        assertEquals("Wrong class: " + product.getClass().getName(), vector.getClass(), product.getClass());
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
        FloatVector vector = create(new float[] { 1, 2, 3, 4 });

        try {
            vector.inPlaceElementwiseDivide(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // Divide by an {@link IntVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        FloatVector quotient = vector.inPlaceElementwiseDivide(new DenseIntVector(new int[] { 1, 3, 6, 10 }));
        assertTrue("Vector objects are not the same", quotient == vector);
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);

        // Divide by a {@link FloatVector}
        vector = create(new float[] { 1, 2, 3, 4 });
        quotient = vector.inPlaceElementwiseDivide(new DenseFloatVector(new float[] { 1, 3, 6, 10 }));
        assertTrue("Vector objects are not the same", quotient == vector);
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);

        // Divide by a {@link PackedIntVector}
        vector = create(new float[] { 1, 2, 3, 4 });
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
        final FloatVector vector = create(new float[] { 1, 10, 20, 30 });
        final FloatVector log = vector.inPlaceElementwiseLog();
        assertTrue("Vector objects are not the same", log == vector);
        assertEquals("Wrong length", 4, log.length());
        assertEquals("Wrong value", Math.log(1), log.getInt(0), .01f);
        assertEquals("Wrong value", Math.log(10), log.getFloat(1), .01f);
        assertEquals("Wrong value", Math.log(20), log.getFloat(2), .01f);
        assertEquals("Wrong value", Math.log(30), log.getFloat(3), .01f);
    }

    /**
     * Tests element-wise division
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testElementwiseDivide() throws Exception {
        final FloatVector vector = create(new float[] { 1, 2, 3, 4 });

        try {
            vector.elementwiseDivide(create(new float[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // Divide by an {@link IntVector}
        FloatVector quotient = vector.elementwiseDivide(createIntVector(new int[] { 1, 3, 6, 10 }));
        assertFalse("Vector objects are the same", quotient == vector);
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);

        // Divide by a {@link FloatVector}
        quotient = vector.elementwiseDivide(createFloatVector(new float[] { 1, 3, 6, 10 }));
        assertFalse("Vector objects are the same", quotient == vector);
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);
    }

    @Test
    public void testElementwiseLog() throws Exception {
        final FloatVector vector = create(new float[] { 1, 10, 20, 30 });
        final FloatVector log = vector.elementwiseLog();
        assertFalse("Vector objects are the same", log == vector);
        assertEquals("Wrong length", 4, log.length());
        assertEquals("Wrong value", Math.log(1), log.getInt(0), .01f);
        assertEquals("Wrong value", Math.log(10), log.getFloat(1), .01f);
        assertEquals("Wrong value", Math.log(20), log.getFloat(2), .01f);
        assertEquals("Wrong value", Math.log(30), log.getFloat(3), .01f);
    }
}
