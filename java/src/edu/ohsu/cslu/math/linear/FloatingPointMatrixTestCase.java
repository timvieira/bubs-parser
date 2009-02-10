package edu.ohsu.cslu.math.linear;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;


import static junit.framework.Assert.assertEquals;

/**
 * Unit tests shared by all floating-point matrix unit tests.
 * 
 * @author Aaron Dunlop
 * @since Nov 3, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class FloatingPointMatrixTestCase extends MatrixTestCase
{
    /**
     * Tests deserializing a matrix using a Reader
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testReadfromReader() throws Exception
    {
        Matrix m1 = Matrix.Factory.read(stringSampleMatrix);
        assertEquals(matrixClass, m1.getClass());
        assertEquals(3, m1.rows());
        assertEquals(4, m1.columns());
        assertEquals(11.11f, m1.getFloat(0, 0), .0001f);
        assertEquals(22.22f, m1.getFloat(0, 1), .0001f);
        assertEquals(66.66f, m1.getFloat(1, 1), .0001f);
        assertEquals(77.77f, m1.getFloat(1, 2), .0001f);
        assertEquals(11.11f, m1.getFloat(2, 2), .0001f);
        assertEquals(12.11f, m1.getFloat(2, 3), .0001f);

        Matrix m2 = Matrix.Factory.read(stringSymmetricMatrix);
        assertEquals(matrixClass, m2.getClass());
        assertEquals("Wrong number of rows", 5, m2.rows());
        assertEquals("Wrong number of columns", 5, m2.columns());
        assertEquals("Wrong value", 0, m2.getFloat(0, 0), .0001f);
        assertEquals("Wrong value", 11.11f, m2.getFloat(1, 0), .0001f);
        assertEquals("Wrong value", 66.66f, m2.getFloat(3, 0), .0001f);
        assertEquals("Wrong value", 77.77f, m2.getFloat(3, 1), .0001f);
        assertEquals("Wrong value", 99.99f, m2.getFloat(3, 3), .0001f);
        assertEquals("Wrong value", 14.44f, m2.getFloat(4, 4), .0001f);
    }

    /**
     * Tests 'getFloat', including reflection across the diagonal in symmetric matrices.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testGetFloat() throws Exception
    {
        assertEquals("Wrong value", 11.11f, sampleMatrix.getFloat(0, 0), .0001);
        assertEquals("Wrong value", 22.22f, sampleMatrix.getFloat(0, 1), .0001);
        assertEquals("Wrong value", 55.55f, sampleMatrix.getFloat(1, 0), .0001);
        assertEquals("Wrong value", 88.88f, sampleMatrix.getFloat(1, 3), .0001);
        assertEquals("Wrong value", 99.99f, sampleMatrix.getFloat(2, 0), .0001);
        assertEquals("Wrong value", 12.11f, sampleMatrix.getFloat(2, 3), .0001);

        assertEquals("Wrong value", 0.1111f, sampleMatrix2.getFloat(0, 0), .0001);
        assertEquals("Wrong value", 0.2222f, sampleMatrix2.getFloat(0, 1), .0001);
        assertEquals("Wrong value", 0.5555f, sampleMatrix2.getFloat(1, 0), .0001);
        assertEquals("Wrong value", 0.8888f, sampleMatrix2.getFloat(1, 3), .0001);
        assertEquals("Wrong value", 0.9999f, sampleMatrix2.getFloat(2, 0), .0001);
        assertEquals("Wrong value", 0.1222f, sampleMatrix2.getFloat(2, 3), .0001);

        assertEquals("Wrong value", 0, symmetricMatrix2.getFloat(0, 0), .0001);
        assertEquals("Wrong value", 0.1111f, symmetricMatrix2.getFloat(1, 0), .0001);
        assertEquals("Wrong value", 0.6666f, symmetricMatrix2.getFloat(3, 0), .0001);
        assertEquals("Wrong value", 0.7777f, symmetricMatrix2.getFloat(3, 1), .0001);
        assertEquals("Wrong value", 0.9999f, symmetricMatrix2.getFloat(3, 3), .0001);
        assertEquals("Wrong value", 0.1444f, symmetricMatrix2.getFloat(4, 4), .0001);

        // And a couple values that are out of the storage area, but should be reflected about the
        // diagonal
        assertEquals("Wrong value", 0.6666f, symmetricMatrix2.getFloat(0, 3), .0001);
        assertEquals("Wrong value", 0.7777f, symmetricMatrix2.getFloat(1, 3), .0001);
        assertEquals("Wrong value", 0.8888f, symmetricMatrix2.getFloat(2, 3), .0001);
    }

    @Test
    public void testGetRow()
    {
        SharedNlpTests.assertEquals(new float[] {11.11f, 22.22f, 33.33f, 44.44f}, sampleMatrix.getRow(0), .01f);
        SharedNlpTests.assertEquals(new float[] {55.55f, 66.66f, 77.77f, 88.88f}, sampleMatrix.getRow(1), .01f);
        SharedNlpTests.assertEquals(new float[] {0, 11.11f, 33.33f, 66.66f, 10f}, symmetricMatrix.getRow(0), .01f);
        SharedNlpTests.assertEquals(new float[] {66.66f, 77.77f, 88.88f, 99.99f, 13.33f}, symmetricMatrix.getRow(3), .01f);
        SharedNlpTests.assertEquals(new float[] {10f, 11.11f, 12.22f, 13.33f, 14.44f}, symmetricMatrix.getRow(4), .01f);
    }

    @Test
    public void testGetColumn()
    {
        SharedNlpTests.assertEquals(new float[] {11.11f, 55.55f, 99.99f}, sampleMatrix.getColumn(0), .01f);
        SharedNlpTests.assertEquals(new float[] {44.44f, 88.88f, 12.11f}, sampleMatrix.getColumn(3), .01f);
        SharedNlpTests.assertEquals(new float[] {0, 11.11f, 33.33f, 66.66f, 10f}, symmetricMatrix.getColumn(0), .01f);
        SharedNlpTests.assertEquals(new float[] {33.33f, 44.44f, 55.55f, 88.88f, 12.22f}, symmetricMatrix.getColumn(2), .01f);
        SharedNlpTests.assertEquals(new float[] {10.00f, 11.11f, 12.22f, 13.33f, 14.44f}, symmetricMatrix.getColumn(4), .01f);
    }

    /**
     * Tests setting matrix elements
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testSet() throws Exception
    {
        assertEquals("Wrong value", 0.9999f, sampleMatrix2.getFloat(2, 0), .0001);
        sampleMatrix2.set(2, 0, 3);
        assertEquals("Wrong value", 3.0f, sampleMatrix2.getFloat(2, 0), .0001);

        assertEquals("Wrong value", 0.6666f, symmetricMatrix2.getFloat(0, 3), .0001);
        symmetricMatrix2.set(0, 3, 3);
        symmetricMatrix2.set(1, 3, 3.1f);
        assertEquals("Wrong value", 3.0f, symmetricMatrix2.getFloat(0, 3), .0001);
        assertEquals("Wrong value", 3.1f, symmetricMatrix2.getFloat(1, 3), .0001);
    }

    @Override
    @Test
    public void testSetRow()
    {
        sampleMatrix.setRow(0, new int[] {13, 14, 15, 16});
        SharedNlpTests.assertEquals(new int[] {13, 14, 15, 16}, sampleMatrix.getIntRow(0));
        SharedNlpTests.assertEquals(new float[] {13f, 14f, 15f, 16f}, sampleMatrix.getRow(0), .01f);
        sampleMatrix.setRow(2, new int[] {0, 1, 2, 3});
        SharedNlpTests.assertEquals(new int[] {0, 1, 2, 3}, sampleMatrix.getIntRow(2));
        SharedNlpTests.assertEquals(new float[] {0f, 1f, 2f, 3f}, sampleMatrix.getRow(2), .01f);

        sampleMatrix2.setRow(2, 2.3f);
        SharedNlpTests.assertEquals(new int[] {2, 2, 2, 2}, sampleMatrix2.getIntRow(2));
        SharedNlpTests.assertEquals(new float[] {2.3f, 2.3f, 2.3f, 2.3f}, sampleMatrix2.getRow(2), .01f);

        symmetricMatrix.setRow(0, new float[] {1.1f, 2.2f, 3.3f, 4.4f, 5.0f});
        SharedNlpTests.assertEquals(new int[] {1, 2, 3, 4, 5}, symmetricMatrix.getIntRow(0));
        SharedNlpTests.assertEquals(new float[] {1.1f, 2.2f, 3.3f, 4.4f, 5.0f}, symmetricMatrix.getRow(0), .01f);
        symmetricMatrix.setRow(4, new float[] {10.1f, 11.1f, 12.1f, 13.1f, 14.1f});
        SharedNlpTests.assertEquals(new int[] {10, 11, 12, 13, 14}, symmetricMatrix.getIntRow(4));
        SharedNlpTests.assertEquals(new float[] {10.1f, 11.1f, 12.1f, 13.1f, 14.1f}, symmetricMatrix.getRow(4), .01f);

        symmetricMatrix2.setRow(4, 1.51f);
        SharedNlpTests.assertEquals(new int[] {2, 2, 2, 2, 2}, symmetricMatrix2.getIntRow(4));
        SharedNlpTests.assertEquals(new float[] {1.51f, 1.51f, 1.51f, 1.51f, 1.51f}, symmetricMatrix2.getRow(4), .01f);
    }

    @Override
    @Test
    public void testSetColumn()
    {
        sampleMatrix.setColumn(0, new float[] {13.1f, 14.1f, 15.1f});
        SharedNlpTests.assertEquals(new int[] {13, 14, 15}, sampleMatrix.getIntColumn(0));
        SharedNlpTests.assertEquals(new float[] {13.1f, 14.1f, 15.1f}, sampleMatrix.getColumn(0), .01f);
        sampleMatrix.setColumn(2, new float[] {0, 1.1f, 2.2f});
        SharedNlpTests.assertEquals(new int[] {0, 1, 2}, sampleMatrix.getIntColumn(2));
        SharedNlpTests.assertEquals(new float[] {0f, 1.1f, 2.2f}, sampleMatrix.getColumn(2), .01f);

        sampleMatrix2.setColumn(2, 2.3f);
        SharedNlpTests.assertEquals(new int[] {2, 2, 2}, sampleMatrix2.getIntColumn(2));
        SharedNlpTests.assertEquals(new float[] {2.3f, 2.3f, 2.3f}, sampleMatrix2.getColumn(2), .01f);

        symmetricMatrix.setColumn(0, new float[] {1.1f, 2.2f, 3.3f, 4.4f, 5.5f});
        SharedNlpTests.assertEquals(new int[] {1, 2, 3, 4, 6}, symmetricMatrix.getIntColumn(0));
        SharedNlpTests.assertEquals(new float[] {1.1f, 2.2f, 3.3f, 4.4f, 5.5f}, symmetricMatrix.getColumn(0), .01f);
        symmetricMatrix.setColumn(4, new float[] {10.1f, 11.1f, 12.1f, 13.1f, 14.1f});
        SharedNlpTests.assertEquals(new int[] {10, 11, 12, 13, 14}, symmetricMatrix.getIntColumn(4));
        SharedNlpTests.assertEquals(new float[] {10.1f, 11.1f, 12.1f, 13.1f, 14.1f}, symmetricMatrix.getColumn(4), .01f);

        symmetricMatrix2.setColumn(4, 1.51f);
        SharedNlpTests.assertEquals(new int[] {2, 2, 2, 2, 2}, symmetricMatrix2.getIntColumn(4));
        SharedNlpTests.assertEquals(new float[] {1.51f, 1.51f, 1.51f, 1.51f, 1.51f}, symmetricMatrix2.getColumn(4), .01f);
    }

    /**
     * Tests scalar addition
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testScalarAdd() throws Exception
    {
        Matrix m = sampleMatrix.scalarAdd(3);
        assertEquals(80.77f, m.getFloat(1, 2), .01f);
        assertEquals(81, m.getInt(1, 2));
        assertEquals(15.11f, m.getFloat(2, 3), .01f);
        assertEquals(15, m.getInt(2, 3));

        m = sampleMatrix.scalarAdd(2.6f);
        assertEquals(80.37f, m.getFloat(1, 2), .01f);
        assertEquals(80, m.getInt(1, 2));
        assertEquals(14.71f, m.getFloat(2, 3), .01f);
        assertEquals(15, m.getInt(2, 3));
    }

    /**
     * Tests scalar multiplication
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testScalarMultiply() throws Exception
    {
        Matrix m = sampleMatrix.scalarMultiply(3);
        assertEquals(233.31f, m.getFloat(1, 2), .01f);
        assertEquals(233, m.getInt(1, 2));
        assertEquals(36.33f, m.getFloat(2, 3), .01f);
        assertEquals(36, m.getInt(2, 3));

        m = sampleMatrix.scalarMultiply(2.6f);
        assertEquals(202.202, m.getFloat(1, 2), .01f);
        assertEquals(202, m.getInt(1, 2));
        assertEquals(31.486f, m.getFloat(2, 3), .01f);
        assertEquals(31, m.getInt(2, 3));
    }
}
