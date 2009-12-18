package edu.ohsu.cslu.datastructs.matrices;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 * Tests for dense int matrices ({@link IntMatrix}, {@link ShortMatrix}, etc.)
 * 
 * @author Aaron Dunlop
 * @since Dec 16, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class DenseIntMatrixTestCase extends IntMatrixTestCase implements DenseMatrixTestCase {

    @Test
    public void testSetRow() {
        ((DenseMatrix) sampleMatrix).setRow(0, new int[] { 13, 14, 15, 16 });
        assertArrayEquals(new int[] { 13, 14, 15, 16 }, ((DenseMatrix) sampleMatrix).getIntRow(0));
        ((DenseMatrix) sampleMatrix).setRow(2, new int[] { 0, 1, 2, 3 });
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, ((DenseMatrix) sampleMatrix).getIntRow(2));

        ((DenseMatrix) sampleMatrix).setRow(2, 3);
        assertArrayEquals(new int[] { 3, 3, 3, 3 }, ((DenseMatrix) sampleMatrix).getIntRow(2));

        ((DenseMatrix) symmetricMatrix).setRow(0, new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, ((DenseMatrix) symmetricMatrix).getIntRow(0));
        ((DenseMatrix) symmetricMatrix).setRow(4, new int[] { 10, 11, 12, 13, 14 });
        assertArrayEquals(new int[] { 10, 11, 12, 13, 14 }, ((DenseMatrix) symmetricMatrix).getIntRow(4));

        ((DenseMatrix) symmetricMatrix).setRow(0, 10);
        assertArrayEquals(new int[] { 10, 10, 10, 10, 10 }, ((DenseMatrix) symmetricMatrix).getIntRow(0));
        ((DenseMatrix) symmetricMatrix).setRow(4, 15);
        assertArrayEquals(new int[] { 15, 15, 15, 15, 15 }, ((DenseMatrix) symmetricMatrix).getIntRow(4));
    }

    @Test
    public void testSetColumn() {
        ((DenseMatrix) sampleMatrix).setColumn(0, new int[] { 13, 14, 15 });
        assertArrayEquals(new int[] { 13, 14, 15 }, ((DenseMatrix) sampleMatrix).getIntColumn(0));
        ((DenseMatrix) sampleMatrix).setColumn(2, new int[] { 0, 1, 2 });
        assertArrayEquals(new int[] { 0, 1, 2 }, ((DenseMatrix) sampleMatrix).getIntColumn(2));

        ((DenseMatrix) sampleMatrix).setColumn(2, 3);
        assertArrayEquals(new int[] { 3, 3, 3 }, ((DenseMatrix) sampleMatrix).getIntColumn(2));

        ((DenseMatrix) symmetricMatrix).setColumn(0, new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, ((DenseMatrix) symmetricMatrix).getIntColumn(0));
        ((DenseMatrix) symmetricMatrix).setColumn(4, new int[] { 10, 11, 12, 13, 14 });
        assertArrayEquals(new int[] { 10, 11, 12, 13, 14 }, ((DenseMatrix) symmetricMatrix).getIntColumn(4));

        ((DenseMatrix) symmetricMatrix).setColumn(0, 10);
        assertArrayEquals(new int[] { 10, 10, 10, 10, 10 }, ((DenseMatrix) symmetricMatrix).getIntColumn(0));
        ((DenseMatrix) symmetricMatrix).setColumn(4, 15);
        assertArrayEquals(new int[] { 15, 15, 15, 15, 15 }, ((DenseMatrix) symmetricMatrix).getIntColumn(4));
    }

    @Test
    public void testGetRow() {
        assertArrayEquals(new float[] { 11, 22, 33, 44 }, ((DenseMatrix) sampleMatrix).getRow(0), .01f);
        assertArrayEquals(new float[] { 100, 10, 11, 12 }, ((DenseMatrix) sampleMatrix).getRow(2), .01f);
        assertArrayEquals(new float[] { 0, 11, 33, 67, 10 }, ((DenseMatrix) symmetricMatrix).getRow(0), .01f);
        assertArrayEquals(new float[] { 67, 78, 89, 100, 13 }, ((DenseMatrix) symmetricMatrix).getRow(3),
            .01f);
        assertArrayEquals(new float[] { 10, 11, 12, 13, 14 }, ((DenseMatrix) symmetricMatrix).getRow(4), .01f);
    }

    @Test
    public void testGetColumn() {
        assertArrayEquals(new float[] { 11, 56, 100 }, ((DenseMatrix) sampleMatrix).getColumn(0), .01f);
        assertArrayEquals(new float[] { 44, 89, 12 }, ((DenseMatrix) sampleMatrix).getColumn(3), .01f);
        assertArrayEquals(new float[] { 0, 11, 33, 67, 10 }, ((DenseMatrix) symmetricMatrix).getColumn(0),
            .01f);
        assertArrayEquals(new float[] { 33, 44, 56, 89, 12 }, ((DenseMatrix) symmetricMatrix).getColumn(2),
            .01f);
        assertArrayEquals(new float[] { 10, 11, 12, 13, 14 }, ((DenseMatrix) symmetricMatrix).getColumn(4),
            .01f);
    }

    @Test
    public void testGetIntRow() {
        assertArrayEquals(new int[] { 11, 22, 33, 44 }, ((DenseMatrix) sampleMatrix).getIntRow(0));
        assertArrayEquals(new int[] { 100, 10, 11, 12 }, ((DenseMatrix) sampleMatrix).getIntRow(2));
        assertArrayEquals(new int[] { 0, 11, 33, 67, 10 }, ((DenseMatrix) symmetricMatrix).getIntRow(0));
        assertArrayEquals(new int[] { 67, 78, 89, 100, 13 }, ((DenseMatrix) symmetricMatrix).getIntRow(3));
        assertArrayEquals(new int[] { 10, 11, 12, 13, 14 }, ((DenseMatrix) symmetricMatrix).getIntRow(4));
    }

    @Test
    public void testGetIntColumn() {
        assertArrayEquals(new int[] { 11, 56, 100 }, ((DenseMatrix) sampleMatrix).getIntColumn(0));
        assertArrayEquals(new int[] { 44, 89, 12 }, ((DenseMatrix) sampleMatrix).getIntColumn(3));
        assertArrayEquals(new int[] { 0, 11, 33, 67, 10 }, ((DenseMatrix) symmetricMatrix).getIntColumn(0));
        assertArrayEquals(new int[] { 33, 44, 56, 89, 12 }, ((DenseMatrix) symmetricMatrix).getIntColumn(2));
        assertArrayEquals(new int[] { 10, 11, 12, 13, 14 }, ((DenseMatrix) symmetricMatrix).getIntColumn(4));
    }

    /**
     * Tests extracting a submatrix from an existing matrix (including both symmetric and non-symmetric
     * submatrices of a symmetric matrix).
     * 
     * 
     * @throws Exception
     *             if something bad happens
     */
    @Test
    public void testSubMatrix() throws Exception {
        // 1 x 1 matrices
        Matrix submatrix = ((DenseMatrix) sampleMatrix).subMatrix(0, 0, 0, 0);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(11, submatrix.getInt(0, 0));

        submatrix = ((DenseMatrix) sampleMatrix).subMatrix(2, 2, 3, 3);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(12, submatrix.getInt(0, 0));

        // m x n matrix
        submatrix = ((DenseMatrix) sampleMatrix).subMatrix(0, 2, 0, 3);
        assertEquals("Wrong number of rows", 3, submatrix.rows());
        assertEquals("Wrong number of columns", 4, submatrix.columns());
        assertEquals(67, submatrix.getInt(1, 1));
        assertEquals(12, submatrix.getInt(2, 3));

        // And finally a true submatrix
        submatrix = ((DenseMatrix) sampleMatrix).subMatrix(1, 2, 1, 3);
        assertEquals("Wrong number of rows", 2, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(67, submatrix.getInt(0, 0));
        assertEquals(78, submatrix.getInt(0, 1));
        assertEquals(11, submatrix.getInt(1, 1));
        assertEquals(12, submatrix.getInt(1, 2));

        // And similar tests for symmetric matrices
        // 1 x 1 matrices
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(0, 0, 0, 0);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(0, submatrix.getInt(0, 0));
        assertTrue(submatrix.isSymmetric());

        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(4, 4, 4, 4);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(14, submatrix.getInt(0, 0));
        assertTrue(submatrix.isSymmetric());

        // m x n matrix
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(0, 4, 0, 4);
        assertEquals("Wrong number of rows", 5, submatrix.rows());
        assertEquals("Wrong number of columns", 5, submatrix.columns());
        assertEquals(22, submatrix.getInt(1, 1));
        assertEquals(56, submatrix.getInt(2, 2));
        assertEquals(89, submatrix.getInt(2, 3));
        assertTrue(submatrix.isSymmetric());

        // A symmetric submatrix (taken from across the diagonal)
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(1, 3, 1, 3);
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
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(1, 3, 0, 2);
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
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(1, 2, 1, 3);
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
}
