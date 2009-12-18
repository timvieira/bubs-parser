package edu.ohsu.cslu.datastructs.matrices;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests shared by all floating-point matrix unit tests.
 * 
 * @author Aaron Dunlop
 * @since Nov 3, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class FloatingPointMatrixTestCase extends MatrixTestCase {

    @Before
    public void setUp() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("matrix type=" + matrixType() + " rows=3 columns=4 symmetric=false\n");
        sb.append("11.1100 22.2200 33.3300 44.4400\n");
        sb.append("55.5500 66.6600 77.7700 88.8800\n");
        sb.append("99.9900 10.0000 11.1100 12.1100\n");
        stringSampleMatrix = sb.toString();

        final float[][] sampleArray = new float[][] { { 11.11f, 22.22f, 33.33f, 44.44f },
                { 55.55f, 66.66f, 77.77f, 88.88f }, { 99.99f, 10.00f, 11.11f, 12.11f } };
        sampleMatrix = create(sampleArray, false);

        sb = new StringBuilder();
        sb.append("matrix type=" + matrixType() + " rows=3 columns=4 symmetric=false\n");
        sb.append("0.1111 0.2222 0.3333 0.4444\n");
        sb.append("0.5555 0.6666 0.7777 0.8888\n");
        sb.append("0.9999 0.1000 0.1111 0.1222\n");
        stringSampleMatrix2 = sb.toString();

        final float[][] sampleArray2 = new float[][] { { 0.1111f, 0.2222f, 0.3333f, 0.4444f },
                { 0.5555f, 0.6666f, 0.7777f, 0.8888f }, { 0.9999f, 0.1000f, 0.1111f, 0.1222f } };
        sampleMatrix2 = create(sampleArray2, false);

        sb = new StringBuilder();
        sb.append("matrix type=" + matrixType() + " rows=5 columns=5 symmetric=true\n");
        sb.append("0.0000\n");
        sb.append("11.1100 22.2200\n");
        sb.append("33.3300 44.4400 55.5500\n");
        sb.append("66.6600 77.7700 88.8800 99.9900\n");
        sb.append("10.0000 11.1100 12.2200 13.3300 14.4400\n");
        stringSymmetricMatrix = sb.toString();

        final float[][] symmetricArray = new float[][] { { 0f }, { 11.11f, 22.22f },
                { 33.33f, 44.44f, 55.55f }, { 66.66f, 77.77f, 88.88f, 99.99f },
                { 10.00f, 11.11f, 12.22f, 13.33f, 14.44f } };
        symmetricMatrix = create(symmetricArray, true);
        matrixClass = symmetricMatrix.getClass();

        sb = new StringBuilder();
        sb.append("matrix type=" + matrixType() + " rows=5 columns=5 symmetric=true\n");
        sb.append("0.0000\n");
        sb.append("0.1111 0.2222\n");
        sb.append("0.3333 0.4444 0.5555\n");
        sb.append("0.6666 0.7777 0.8888 0.9999\n");
        sb.append("0.1000 0.1111 0.1222 0.1333 0.1444\n");
        stringSymmetricMatrix2 = sb.toString();

        final float[][] symmetricArray2 = new float[][] { { 0 }, { 0.1111f, 0.2222f },
                { 0.3333f, 0.4444f, 0.5555f }, { 0.6666f, 0.7777f, 0.8888f, 0.9999f },
                { 0.1000f, 0.1111f, 0.1222f, 0.1333f, 0.1444f } };
        symmetricMatrix2 = create(symmetricArray2, true);
    }

    /**
     * Tests deserializing a matrix using a Reader
     * 
     * @throws Exception
     *             if something bad happens
     */
    @Override
    @Test
    public void testReadfromReader() throws Exception {
        final Matrix m1 = Matrix.Factory.read(stringSampleMatrix);
        assertEquals(matrixClass, m1.getClass());
        assertEquals(3, m1.rows());
        assertEquals(4, m1.columns());
        assertEquals(11.11f, m1.getFloat(0, 0), .0001f);
        assertEquals(22.22f, m1.getFloat(0, 1), .0001f);
        assertEquals(66.66f, m1.getFloat(1, 1), .0001f);
        assertEquals(77.77f, m1.getFloat(1, 2), .0001f);
        assertEquals(11.11f, m1.getFloat(2, 2), .0001f);
        assertEquals(12.11f, m1.getFloat(2, 3), .0001f);

        final Matrix m2 = Matrix.Factory.read(stringSymmetricMatrix);
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
     * @throws Exception
     *             if something bad happens
     */
    @Test
    public void testGetFloat() throws Exception {
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

    /**
     * Tests setting matrix elements
     * 
     * @throws Exception
     *             if something bad happens
     */
    @Override
    @Test
    public void testSet() throws Exception {
        assertEquals("Wrong value", 0.9999f, sampleMatrix2.getFloat(2, 0), .0001);
        sampleMatrix2.set(2, 0, 3);
        assertEquals("Wrong value", 3.0f, sampleMatrix2.getFloat(2, 0), .0001);

        assertEquals("Wrong value", 0.6666f, symmetricMatrix2.getFloat(0, 3), .0001);
        symmetricMatrix2.set(0, 3, 3);
        symmetricMatrix2.set(1, 3, 3.1f);
        assertEquals("Wrong value", 3.0f, symmetricMatrix2.getFloat(0, 3), .0001);
        assertEquals("Wrong value", 3.1f, symmetricMatrix2.getFloat(1, 3), .0001);
    }

    /**
     * Tests scalar addition
     * 
     * @throws Exception
     *             if something bad happens
     */
    @Override
    @Test
    public void testScalarAdd() throws Exception {
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
     * @throws Exception
     *             if something bad happens
     */
    @Override
    @Test
    public void testScalarMultiply() throws Exception {
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

    /**
     * Tests matrix/vector multiplication
     */
    @Override
    public void testVectorMultiply() throws Exception {
        fail("Not Implemented");
    }
}
