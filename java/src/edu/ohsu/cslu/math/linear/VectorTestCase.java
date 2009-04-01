package edu.ohsu.cslu.math.linear;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests shared by all vector test classes
 * 
 * @author Aaron Dunlop
 * @since Dec 8, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class VectorTestCase
{
    protected String stringSampleVector;
    protected Vector sampleVector;

    protected Class<? extends Vector> vectorClass;

    protected abstract Vector create(float[] array);

    @Before
    public abstract void setUp() throws Exception;

    /**
     * Tests deserializing a vector using a Reader
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testReadfromReader() throws Exception
    {
        Vector v = Vector.Factory.read(stringSampleVector);
        assertEquals(v, sampleVector);
    }

    /**
     * Tests serializing a vector to a Writer
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testWriteToWriter() throws Exception
    {
        StringWriter writer = new StringWriter();
        sampleVector.write(writer);
        assertEquals(stringSampleVector, writer.toString());
    }

    /**
     * Tests 'length' method
     * 
     * @throws Exception
     */
    @Test
    public void testLength() throws Exception
    {
        assertEquals("Wrong length", 11, sampleVector.length());
    }

    /**
     * Tests 'getInt' method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testGetInt() throws Exception
    {
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
    public void testGetBoolean() throws Exception
    {
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
    public void testGetFloat() throws Exception
    {
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
    public void testSet() throws Exception
    {
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
    public void testSubVector() throws Exception
    {
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
     * Tests min(), intMin(), and argMin() methods
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testMin() throws Exception
    {
        assertEquals(-11f, sampleVector.min(), .01f);
        assertEquals(-11, sampleVector.intMin());
        assertEquals(0, sampleVector.argMin());

        sampleVector.set(1, -22.0f);
        sampleVector.set(2, -44.0f);
        assertEquals(-44f, sampleVector.min(), .01f);
        assertEquals(-44, sampleVector.intMin());
        assertEquals(2, sampleVector.argMin());
    }

    /**
     * Tests max(), intMax(), and argMax() methods
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testMax() throws Exception
    {
        assertEquals(100, sampleVector.intMax());
        assertEquals(10, sampleVector.argMax());

        sampleVector.set(1, 125f);
        sampleVector.set(2, 126f);
        assertEquals(126f, sampleVector.max(), .01f);
        assertEquals(126, sampleVector.intMax());
        assertEquals(2, sampleVector.argMax());
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
     * Tests scalar addition
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testScalarAdd() throws Exception;

    /**
     * Tests scalar multiplication
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testScalarMultiply() throws Exception;

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
    public void testSerialize() throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(sampleVector);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        Vector m = (Vector) ois.readObject();
        assertEquals(stringSampleVector, m.toString());
    }
}
