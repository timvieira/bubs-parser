package edu.ohsu.cslu.datastructs.matrices;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unit tests shared by all matrix test classes
 * 
 * @author Aaron Dunlop
 * @since Nov 3, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class MatrixTestCase
{
    protected String stringSampleMatrix;
    protected Matrix sampleMatrix;

    protected String stringSymmetricMatrix;
    protected Matrix symmetricMatrix;

    protected String stringSampleMatrix2;
    protected Matrix sampleMatrix2;

    protected String stringSymmetricMatrix2;
    protected Matrix symmetricMatrix2;

    protected Class<? extends Matrix> matrixClass;

    protected abstract Matrix create(float[][] array);

    /**
     * Tests deserializing a matrix using a Reader
     * 
     * @throws Exception if something bad happens
     */
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
     * Tests serializing a matrix to a Writer
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testWriteToWriter() throws Exception
    {
        StringWriter writer = new StringWriter();
        sampleMatrix.write(writer);
        assertEquals(stringSampleMatrix, writer.toString());

        writer = new StringWriter();
        symmetricMatrix.write(writer);
        assertEquals(stringSymmetricMatrix, writer.toString());

        // Some matrix classes don't really need additional tests with sampleMatrix2 or
        // symmetricMatrix2
        if (sampleMatrix2 != null)
        {
            writer = new StringWriter();
            sampleMatrix2.write(writer);
            assertEquals(stringSampleMatrix2, writer.toString());
        }

        if (symmetricMatrix2 != null)
        {
            writer = new StringWriter();
            symmetricMatrix2.write(writer);
            assertEquals(stringSymmetricMatrix2, writer.toString());
        }
    }

    @Test
    public void testDimensions() throws Exception
    {
        assertEquals("Wrong number of rows", 3, sampleMatrix.rows());
        assertEquals("Wrong number of columns", 4, sampleMatrix.columns());

        assertEquals("Wrong number of rows", 5, symmetricMatrix.rows());
        assertEquals("Wrong number of columns", 5, symmetricMatrix.columns());
    }

    /**
     * Tests 'getInt', including reflection across the diagonal in symmetric matrices.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testGetInt() throws Exception
    {
        assertEquals("Wrong value", 11, sampleMatrix.getInt(0, 0));
        assertEquals("Wrong value", 22, sampleMatrix.getInt(0, 1));
        assertEquals("Wrong value", 56, sampleMatrix.getInt(1, 0));
        assertEquals("Wrong value", 89, sampleMatrix.getInt(1, 3));
        assertEquals("Wrong value", 100, sampleMatrix.getInt(2, 0));
        assertEquals("Wrong value", 12, sampleMatrix.getInt(2, 3));

        assertEquals("Wrong value", 0, symmetricMatrix.getInt(0, 0));
        assertEquals("Wrong value", 11, symmetricMatrix.getInt(1, 0));
        assertEquals("Wrong value", 67, symmetricMatrix.getInt(3, 0));
        assertEquals("Wrong value", 78, symmetricMatrix.getInt(3, 1));
        assertEquals("Wrong value", 100, symmetricMatrix.getInt(3, 3));
        assertEquals("Wrong value", 14, symmetricMatrix.getInt(4, 4));

        // And a couple values that are out of the storage area, but should be reflected about the
        // diagonal
        assertEquals("Wrong value", 67, symmetricMatrix.getInt(0, 3));
        assertEquals("Wrong value", 78, symmetricMatrix.getInt(1, 3));
        assertEquals("Wrong value", 89, symmetricMatrix.getInt(2, 3));
    }

    @Test
    public void testGetIntRow()
    {
        SharedNlpTests.assertEquals(new int[] {11, 22, 33, 44}, sampleMatrix.getIntRow(0));
        SharedNlpTests.assertEquals(new int[] {100, 10, 11, 12}, sampleMatrix.getIntRow(2));
        SharedNlpTests.assertEquals(new int[] {0, 11, 33, 67, 10}, symmetricMatrix.getIntRow(0));
        SharedNlpTests.assertEquals(new int[] {67, 78, 89, 100, 13}, symmetricMatrix.getIntRow(3));
        SharedNlpTests.assertEquals(new int[] {10, 11, 12, 13, 14}, symmetricMatrix.getIntRow(4));
    }

    @Test
    public void testGetIntColumn()
    {
        SharedNlpTests.assertEquals(new int[] {11, 56, 100}, sampleMatrix.getIntColumn(0));
        SharedNlpTests.assertEquals(new int[] {44, 89, 12}, sampleMatrix.getIntColumn(3));
        SharedNlpTests.assertEquals(new int[] {0, 11, 33, 67, 10}, symmetricMatrix.getIntColumn(0));
        SharedNlpTests.assertEquals(new int[] {33, 44, 56, 89, 12}, symmetricMatrix.getIntColumn(2));
        SharedNlpTests.assertEquals(new int[] {10, 11, 12, 13, 14}, symmetricMatrix.getIntColumn(4));
    }

    /**
     * Tests setting matrix elements
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testSet() throws Exception;

    /**
     * Tests setRow() method.
     */
    @Test
    public abstract void testSetRow();

    /**
     * Tests setColumn() method
     */
    @Test
    public abstract void testSetColumn();

    /**
     * Tests extracting a submatrix from an existing matrix (including both symmetric and
     * non-symmetric submatrices of a symmetric matrix).
     * 
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSubMatrix() throws Exception
    {
        // 1 x 1 matrices
        Matrix submatrix = sampleMatrix.subMatrix(0, 0, 0, 0);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(11, submatrix.getInt(0, 0));

        submatrix = sampleMatrix.subMatrix(2, 2, 3, 3);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(12, submatrix.getInt(0, 0));

        // m x n matrix
        submatrix = sampleMatrix.subMatrix(0, 2, 0, 3);
        assertEquals("Wrong number of rows", 3, submatrix.rows());
        assertEquals("Wrong number of columns", 4, submatrix.columns());
        assertEquals(67, submatrix.getInt(1, 1));
        assertEquals(12, submatrix.getInt(2, 3));

        // And finally a true submatrix
        submatrix = sampleMatrix.subMatrix(1, 2, 1, 3);
        assertEquals("Wrong number of rows", 2, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(67, submatrix.getInt(0, 0));
        assertEquals(78, submatrix.getInt(0, 1));
        assertEquals(11, submatrix.getInt(1, 1));
        assertEquals(12, submatrix.getInt(1, 2));

        // And similar tests for symmetric matrices
        // 1 x 1 matrices
        submatrix = symmetricMatrix.subMatrix(0, 0, 0, 0);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(0, submatrix.getInt(0, 0));
        assertTrue(submatrix.isSymmetric());

        submatrix = symmetricMatrix.subMatrix(4, 4, 4, 4);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(14, submatrix.getInt(0, 0));
        assertTrue(submatrix.isSymmetric());

        // m x n matrix
        submatrix = symmetricMatrix.subMatrix(0, 4, 0, 4);
        assertEquals("Wrong number of rows", 5, submatrix.rows());
        assertEquals("Wrong number of columns", 5, submatrix.columns());
        assertEquals(22, submatrix.getInt(1, 1));
        assertEquals(56, submatrix.getInt(2, 2));
        assertEquals(89, submatrix.getInt(2, 3));
        assertTrue(submatrix.isSymmetric());

        // A symmetric submatrix (taken from across the diagonal)
        submatrix = symmetricMatrix.subMatrix(1, 3, 1, 3);
        assertEquals("Wrong number of rows", 3, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(22, submatrix.getInt(0, 0));
        assertEquals(44, submatrix.getInt(0, 1));
        assertEquals(56, submatrix.getInt(1, 1));
        assertEquals(89, submatrix.getInt(1, 2));
        assertEquals(100, submatrix.getInt(2, 2));
        assertTrue(submatrix.isSquare());
        assertTrue(submatrix.isSymmetric());

        // A square submatrix that is not symmetric
        submatrix = symmetricMatrix.subMatrix(1, 3, 0, 2);
        assertEquals("Wrong number of rows", 3, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(11, submatrix.getInt(0, 0));
        assertEquals(22, submatrix.getInt(0, 1));
        assertEquals(44, submatrix.getInt(0, 2));
        assertEquals(67, submatrix.getInt(2, 0));
        assertEquals(78, submatrix.getInt(2, 1));
        assertEquals(89, submatrix.getInt(2, 2));
        assertTrue(submatrix.isSquare());
        assertFalse(submatrix.isSymmetric());

        // A non-square submatrix
        submatrix = symmetricMatrix.subMatrix(1, 2, 1, 3);
        assertEquals("Wrong number of rows", 2, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(22, submatrix.getInt(0, 0));
        assertEquals(44, submatrix.getInt(0, 1));
        assertEquals(78, submatrix.getInt(0, 2));
        assertEquals(56, submatrix.getInt(1, 1));
        assertEquals(89, submatrix.getInt(1, 2));
        assertFalse(submatrix.isSquare());
        assertFalse(submatrix.isSymmetric());
    }

    /**
     * Tests min(), intMin(), argMin(), and rowArgMin() methods
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testMin() throws Exception
    {
        assertEquals(10f, sampleMatrix.min(), .01f);
        assertEquals(10, sampleMatrix.intMin());
        SharedNlpTests.assertEquals(new int[] {2, 1}, sampleMatrix.argMin());

        sampleMatrix.set(1, 1, -3.0f);
        sampleMatrix.set(2, 3, -4.0f);
        assertEquals(-4f, sampleMatrix.min(), .01f);
        assertEquals(-4, sampleMatrix.intMin());
        SharedNlpTests.assertEquals(new int[] {2, 3}, sampleMatrix.argMin());

        assertEquals(1, sampleMatrix.rowArgMin(1));
        assertEquals(3, sampleMatrix.rowArgMin(2));

        assertEquals(0.0f, symmetricMatrix.min(), .01f);
        assertEquals(0, symmetricMatrix.intMin());
        SharedNlpTests.assertEquals(new int[] {0, 0}, symmetricMatrix.argMin());

        symmetricMatrix.set(2, 3, -5);
        assertEquals(-5.0f, symmetricMatrix.min(), .01f);
        assertEquals(-5, symmetricMatrix.intMin());
        SharedNlpTests.assertEquals(new int[] {3, 2}, symmetricMatrix.argMin());

        assertEquals(3, symmetricMatrix.rowArgMin(2));
        assertEquals(0, symmetricMatrix.rowArgMin(4));
    }

    /**
     * Tests max(), intMax(), argMax(), and rowArgMax() methods
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testMax() throws Exception
    {
        assertEquals(100, sampleMatrix.intMax());
        SharedNlpTests.assertEquals(new int[] {2, 0}, sampleMatrix.argMax());

        sampleMatrix.set(1, 1, 125f);
        sampleMatrix.set(2, 1, 126f);
        assertEquals(126f, sampleMatrix.max(), .01f);
        assertEquals(126, sampleMatrix.intMax());
        SharedNlpTests.assertEquals(new int[] {2, 1}, sampleMatrix.argMax());

        assertEquals(1, sampleMatrix.rowArgMax(1));
        assertEquals(1, sampleMatrix.rowArgMax(2));

        assertEquals(100, symmetricMatrix.intMax());
        SharedNlpTests.assertEquals(new int[] {3, 3}, symmetricMatrix.argMax());

        symmetricMatrix.set(2, 3, 125f);
        assertEquals(125f, symmetricMatrix.max(), .01f);
        assertEquals(125, symmetricMatrix.intMax());
        SharedNlpTests.assertEquals(new int[] {3, 2}, symmetricMatrix.argMax());

        assertEquals(3, symmetricMatrix.rowArgMax(2));
        assertEquals(4, symmetricMatrix.rowArgMax(4));
    }

    /**
     * Tests scalar addition
     * 
     * @throws Exception if something bad happens
     */
    public abstract void testScalarAdd() throws Exception;

    /**
     * Tests scalar multiplication
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testScalarMultiply() throws Exception;

    /**
     * Tests {@link Matrix#infinity()} method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testInfinity() throws Exception;

    /**
     * Tests {@link Matrix#negativeInfinity()} method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testNegativeInfinity() throws Exception;

    /**
     * Tests transposition of a matrix.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testTranspose() throws Exception
    {
        Matrix transposed = sampleMatrix.transpose();
        assertEquals(matrixClass, transposed.getClass());
        assertEquals("Wrong number of rows", 4, transposed.rows());
        assertEquals("Wrong number of columns", 3, transposed.columns());
        assertEquals("Wrong value", 11, transposed.getInt(0, 0));
        assertEquals("Wrong value", 44, transposed.getInt(3, 0));
        assertEquals("Wrong value", 100, transposed.getInt(0, 2));
        assertEquals("Wrong value", 78, transposed.getInt(2, 1));
        assertEquals("Wrong value", 12, transposed.getInt(3, 2));

        transposed = symmetricMatrix.transpose();
        assertEquals(matrixClass, transposed.getClass());
        assertEquals(5, transposed.rows());
        assertEquals(5, transposed.columns());
        assertEquals(0, transposed.getInt(0, 0));
        assertEquals(11, transposed.getInt(1, 0));
        assertEquals(67, transposed.getInt(3, 0));
        assertEquals(78, transposed.getInt(3, 1));
        assertEquals(100, transposed.getInt(3, 3));
        assertEquals(14, transposed.getInt(4, 4));
    }

    @Test
    public void testIsSquare() throws Exception
    {
        assertFalse(sampleMatrix.isSquare());
        assertTrue(symmetricMatrix.isSquare());

        Matrix squareMatrix = create(new float[][] { {1, 2}, {3, 4}});
        assertTrue(squareMatrix.isSquare());
    }

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

        oos.writeObject(sampleMatrix);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        Matrix m = (Matrix) ois.readObject();
        assertEquals(stringSampleMatrix, m.toString());
    }
}
