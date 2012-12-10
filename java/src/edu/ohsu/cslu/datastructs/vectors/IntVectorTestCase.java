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
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

/**
 * Tests shared between {@link DenseIntVector} and {@link PackedIntVector}
 * 
 * @author Aaron Dunlop
 * @since Mar 28, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class IntVectorTestCase<V extends IntVector> extends NumericVectorTestCase<V> {

    @Override
    public void setUp() throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=" + serializedName() + " length=11 sparse="
                + ((sampleVector instanceof SparseVector) ? "true\n" : "false\n"));
        sb.append("-11 0 11 22 33 44 56 67 78 89 100\n");
        stringSampleVector = sb.toString();

        final int[] sampleArray = new int[] { -11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100 };
        sampleVector = create(sampleArray);
    }

    @Override
    public void testVectorAdd() throws Exception {
        final Vector vector = create(new int[] { 1, 2, 3, 4 });
        final DenseIntVector intVector = new DenseIntVector(new int[] { 1, 2, 3, 4 });
        final DenseFloatVector floatVector = new DenseFloatVector(new float[] { 4, 3, 2, 1 });

        try {
            vector.add(create(new int[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // If we add two instances of the same class, we should get another instance of that class
        Vector sum = vector.add(vector);
        assertEquals("Wrong class: " + sum.getClass().getName(), vectorClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0));
        assertEquals("Wrong value", 4, sum.getInt(1));
        assertEquals("Wrong value", 6, sum.getInt(2));
        assertEquals("Wrong value", 8, sum.getInt(3));

        // If we add an {@link IntVector} we should get a new {@link IntVector}
        sum = intVector.add(intVector);
        assertEquals("Wrong class: " + sum.getClass().getName(), DenseIntVector.class, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0));
        assertEquals("Wrong value", 4, sum.getInt(1));
        assertEquals("Wrong value", 6, sum.getInt(2));
        assertEquals("Wrong value", 8, sum.getInt(3));

        // If we add a {@link FloatVector} we should get a {@link FloatVector}
        sum = intVector.add(floatVector);
        assertEquals("Wrong class: " + DenseFloatVector.class, DenseFloatVector.class, sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 5, sum.getFloat(0), .01f);
        assertEquals("Wrong value", 5, sum.getFloat(1), .01f);
        assertEquals("Wrong value", 5, sum.getFloat(2), .01f);
        assertEquals("Wrong value", 5, sum.getFloat(3), .01f);

        // If we add a {@link PackedBitVector} we should get a new instance of the same class
        sum = vector.add(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertEquals("Wrong class: " + sum.getClass().getName(), vector.getClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 2, sum.getInt(0));
        assertEquals("Wrong value", 3, sum.getInt(1));
        assertEquals("Wrong value", 3, sum.getInt(2));
        assertEquals("Wrong value", 4, sum.getInt(3));

        // If we add a {@link SparseBitVector} we should get a new instance of the same class
        sum = vector.add(new SparseBitVector(4, new int[] { 1, 2 }));
        assertEquals("Wrong class: " + sum.getClass().getName(), vector.getClass(), sum.getClass());
        assertEquals("Wrong length", 4, sum.length());
        assertEquals("Wrong value", 1, sum.getInt(0));
        assertEquals("Wrong value", 3, sum.getInt(1));
        assertEquals("Wrong value", 4, sum.getInt(2));
        assertEquals("Wrong value", 4, sum.getInt(3));
    }

    @Override
    public void testScalarAdd() throws Exception {
        NumericVector v = ((NumericVector) sampleVector).scalarAdd(1);
        assertEquals("Wrong class", sampleVector.getClass(), v.getClass());
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

        v = ((NumericVector) sampleVector).scalarAdd(-2.5f);
        assertEquals("Wrong class", DenseFloatVector.class, v.getClass());
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
    public void testElementwiseMultiply() throws Exception {
        final Vector vector = create(new int[] { 1, 2, 3, 4 });
        final DenseIntVector intVector = new DenseIntVector(new int[] { 1, 2, 3, 4 });
        final DenseFloatVector floatVector = new DenseFloatVector(new float[] { 4, 3, 2, 1 });

        try {
            vector.elementwiseMultiply(create(new int[] { 1 }));
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // If we multiply two instances of the same class, we should get another instance of that
        // class
        Vector product = vector.elementwiseMultiply(vector);
        assertEquals("Wrong class: " + product.getClass().getName(), vectorClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getInt(0));
        assertEquals("Wrong value", 4, product.getInt(1));
        assertEquals("Wrong value", 9, product.getInt(2));
        assertEquals("Wrong value", 16, product.getInt(3));

        // If we multiply by an {@link IntVector} we should get a new instance of the same class
        product = vector.elementwiseMultiply(intVector);
        assertEquals("Wrong class: " + product.getClass().getName(), vector.getClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getInt(0));
        assertEquals("Wrong value", 4, product.getInt(1));
        assertEquals("Wrong value", 9, product.getInt(2));
        assertEquals("Wrong value", 16, product.getInt(3));

        // If we multiply by a {@link FloatVector} we should get a {@link FloatVector}
        product = vector.elementwiseMultiply(floatVector);
        assertTrue("Wrong class: " + product.getClass(), product instanceof FloatVector);
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 4, product.getFloat(0), .01f);
        assertEquals("Wrong value", 6, product.getFloat(1), .01f);
        assertEquals("Wrong value", 6, product.getFloat(2), .01f);
        assertEquals("Wrong value", 4, product.getFloat(3), .01f);

        // If we multiply by a {@link PackedBitVector} we should get a new instance of the same
        // class
        product = vector.elementwiseMultiply(new PackedBitVector(new int[] { 1, 1, 0, 0 }));
        assertEquals("Wrong class: " + product.getClass().getName(), vector.getClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 1, product.getInt(0));
        assertEquals("Wrong value", 2, product.getInt(1));
        assertEquals("Wrong value", 0, product.getInt(2));
        assertEquals("Wrong value", 0, product.getInt(3));

        // If we multiply by a {@link SparseBitVector} we should get a new instance of the same
        // class
        product = vector.elementwiseMultiply(new SparseBitVector(4, new int[] { 1, 2 }));
        assertEquals("Wrong class: " + product.getClass().getName(), vector.getClass(), product.getClass());
        assertEquals("Wrong length", 4, product.length());
        assertEquals("Wrong value", 0, product.getInt(0));
        assertEquals("Wrong value", 2, product.getInt(1));
        assertEquals("Wrong value", 3, product.getInt(2));
        assertEquals("Wrong value", 0, product.getInt(3));
    }

    @Override
    public void testScalarMultiply() throws Exception {
        NumericVector v = ((NumericVector) sampleVector).scalarMultiply(3);
        assertEquals("Wrong class", vectorClass(), v.getClass());
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

        v = ((NumericVector) sampleVector).scalarMultiply(-2.5f);
        assertEquals("Wrong class", DenseFloatVector.class, v.getClass());
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
        final Vector v = new DenseIntVector(new int[] { 1, 2, 3, 4 });
        assertEquals(49, v.dotProduct(new DenseIntVector(new int[] { 4, 5, 5, 5 })), .01f);
        assertEquals(49f, v.dotProduct(new DenseFloatVector(new float[] { 4, 5, 5, 5 })), .01f);
        assertEquals(5f, v.dotProduct(new PackedBitVector(new int[] { 0, 1, 1, 0 })), .01f);
        assertEquals(49, v.dotProduct(new PackedIntVector(new int[] { 4, 5, 5, 5 }, 4)), .01f);
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(Integer.MAX_VALUE, sampleVector.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(Integer.MIN_VALUE, sampleVector.negativeInfinity(), .01f);
    }
}
