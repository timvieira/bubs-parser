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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests common to all {@link Vector} implementations
 * 
 * @author Aaron Dunlop
 * @since Dec 8, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class VectorTestCase<V extends Vector> {

    protected String stringSampleVector;
    protected Vector sampleVector;

    @Before
    public abstract void setUp() throws Exception;

    /**
     * @return The name which the vector class under test serializes (e.g. sparse-int-vector)
     * 
     *         TODO Move this into the vector class itself and eliminate from the test hierarchy
     */
    protected abstract String serializedName();

    /**
     * Returns an instance of the class under test.
     * 
     * @param array
     * @return vector
     */
    protected V create(final float[] array) {
        try {
            return vectorClass().getConstructor(new Class[] { float[].class }).newInstance(new Object[] { array });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an instance of the class under test, populated by the supplied array.
     * 
     * @param array
     * @return vector
     */
    protected final V create(final int[] array) {

        try {
            return vectorClass().getConstructor(new Class[] { int[].class }).newInstance(new Object[] { array });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an {@link IntVector} populated by the supplied array. If the class under test is an instance of
     * {@link LargeVector}, a {@link LargeSparseIntVector} is returned, otherwise a {@link DenseIntVector}
     * 
     * @param vector
     * @return IntVector
     */
    protected final IntVector createIntVector(final int[] vector) {

        if (implementedInterfaces(vectorClass()).contains(LargeVector.class)) {
            return new LargeSparseIntVector(vector);
        }
        return new DenseIntVector(vector);
    }

    /**
     * Returns an {@link FloatVector} populated by the supplied array. If the class under test is an instance of
     * {@link LargeVector}, a {@link LargeSparseFloatVector} is returned, otherwise a {@link DenseFloatVector}
     * 
     * @param vector
     * @return FloatVector
     */
    protected final FloatVector createFloatVector(final float[] vector) {

        if (implementedInterfaces(vectorClass()).contains(LargeVector.class)) {
            return new LargeSparseFloatVector(vector);
        }
        return new DenseFloatVector(vector);
    }

    /**
     * Returns a {@link BitVector} populated by the supplied array. If the class under test is an instance of
     * {@link LargeVector}, a {@link LargeSparseBitVector} is returned, otherwise a {@link SparseBitVector}
     * 
     * @param vector
     * @return BitVector
     */
    protected final BitVector createBitVector(final long length, final long[] vector) {

        if (implementedInterfaces(vectorClass()).contains(LargeVector.class)) {
            return new LargeSparseBitVector(length, vector);
        }
        return new SparseBitVector(length, vector);
    }

    /**
     * Returns a {@link BitVector} populated by the supplied array. If the class under test is an instance of
     * {@link LargeVector}, a {@link MutableLargeSparseBitVector} is returned, otherwise a
     * {@link MutableSparseBitVector}
     * 
     * @param vector
     * @return BitVector
     */
    protected final BitVector createMutableBitVector(final long length, final long[] vector) {

        if (implementedInterfaces(vectorClass()).contains(LargeVector.class)) {
            return new MutableLargeSparseBitVector(length, vector);
        }
        return new MutableSparseBitVector(length, vector);
    }

    private Set<Class<?>> implementedInterfaces(final Class<?> c) {
        final HashSet<Class<?>> set = new HashSet<Class<?>>();
        for (final Class<?> i : c.getInterfaces()) {
            set.add(i);
            set.addAll(implementedInterfaces(i));
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    protected final Class<V> vectorClass() {
        return ((Class<V>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    /**
     * Tests deserializing a vector using a Reader
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testReadfromReader() throws Exception {
        final Vector v = Vector.Factory.read(stringSampleVector);
        assertEquals(sampleVector, v);
    }

    /**
     * Tests serializing a vector to a Writer
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testWriteToWriter() throws Exception {
        final StringWriter writer = new StringWriter();
        sampleVector.write(writer);
        assertEquals(stringSampleVector, writer.toString());
    }

    /**
     * Tests 'length' method
     * 
     * @throws Exception
     */
    @Test
    public void testLength() throws Exception {
        assertEquals("Wrong length", 11, sampleVector.length());
    }

    /**
     * Tests 'getInt' method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testGetInt() throws Exception {
        assertEquals("Wrong value", -11, sampleVector.getInt(0));
        assertEquals("Wrong value", 0, sampleVector.getInt(1));
        assertEquals("Wrong value", 11, sampleVector.getInt(2));
        assertEquals("Wrong value", 22, sampleVector.getInt(3));
        assertEquals("Wrong value", 33, sampleVector.getInt(4));
        assertEquals("Wrong value", 44, sampleVector.getInt(5));
        assertEquals("Wrong value", 56, sampleVector.getInt(6));
        assertEquals("Wrong value", 67, sampleVector.getInt(7));
        assertEquals("Wrong value", 78, sampleVector.getInt(8));
        assertEquals("Wrong value", 89, sampleVector.getInt(9));
        assertEquals("Wrong value", 100, sampleVector.getInt(10));
    }

    /**
     * Tests 'getBoolean' method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testGetBoolean() throws Exception {
        assertEquals("Wrong value", true, sampleVector.getBoolean(0));
        assertEquals("Wrong value", false, sampleVector.getBoolean(1));
        assertEquals("Wrong value", true, sampleVector.getBoolean(2));
        assertEquals("Wrong value", true, sampleVector.getBoolean(3));
        assertEquals("Wrong value", true, sampleVector.getBoolean(4));
        assertEquals("Wrong value", true, sampleVector.getBoolean(5));
        assertEquals("Wrong value", true, sampleVector.getBoolean(6));
        assertEquals("Wrong value", true, sampleVector.getBoolean(7));
        assertEquals("Wrong value", true, sampleVector.getBoolean(8));
        assertEquals("Wrong value", true, sampleVector.getBoolean(9));
        assertEquals("Wrong value", true, sampleVector.getBoolean(10));
    }

    /**
     * Tests 'getFloat' method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testGetFloat() throws Exception {
        assertEquals("Wrong value", -11, sampleVector.getFloat(0), 0.01f);
        assertEquals("Wrong value", 0, sampleVector.getFloat(1), 0.01f);
        assertEquals("Wrong value", 11, sampleVector.getFloat(2), 0.01f);
        assertEquals("Wrong value", 22, sampleVector.getFloat(3), 0.01f);
        assertEquals("Wrong value", 33, sampleVector.getFloat(4), 0.01f);
        assertEquals("Wrong value", 44, sampleVector.getFloat(5), 0.01f);
        assertEquals("Wrong value", 56, sampleVector.getFloat(6), 0.01f);
        assertEquals("Wrong value", 67, sampleVector.getFloat(7), 0.01f);
        assertEquals("Wrong value", 78, sampleVector.getFloat(8), 0.01f);
        assertEquals("Wrong value", 89, sampleVector.getFloat(9), 0.01f);
        assertEquals("Wrong value", 100, sampleVector.getFloat(10), 0.01f);
    }

    /**
     * Tests setting vector elements
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSet() throws Exception {
        assertEquals("Wrong value", 11, sampleVector.getInt(2));
        assertEquals("Wrong value", 78, sampleVector.getInt(8));
        sampleVector.set(2, 3);
        assertEquals("Wrong value", 3, sampleVector.getInt(2));
        sampleVector.set(8, 23.3f);
        assertEquals("Wrong value", 23, sampleVector.getInt(8));
    }

    /**
     * Tests extracting a subvector from an existing vector.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSubVector() throws Exception {
        // Single-element subvector
        Vector subvector = sampleVector.subVector(5, 5);
        assertEquals("Wrong length", 1, subvector.length());
        assertEquals(44, subvector.getInt(0));

        // 2-element subvector
        subvector = sampleVector.subVector(2, 3);
        assertEquals("Wrong length", 2, subvector.length());
        assertEquals(11, subvector.getInt(0));
        assertEquals(22, subvector.getInt(1));
    }

    /**
     * Tests vector addition
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testVectorAdd() throws Exception;

    /**
     * Tests element-wise multiplication
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testElementwiseMultiply() throws Exception;

    /**
     * Tests inner / dot product multiplication
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testDotProduct() throws Exception;

    /**
     * Tests {@link Vector#infinity()} method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testInfinity() throws Exception;

    /**
     * Tests {@link Vector#negativeInfinity()} method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testNegativeInfinity() throws Exception;

    /**
     * Tests Java serialization and deserialization of matrices
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSerialize() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(sampleVector);

        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        final Vector m = (Vector) ois.readObject();
        assertEquals(stringSampleVector, m.toString());
    }
}
