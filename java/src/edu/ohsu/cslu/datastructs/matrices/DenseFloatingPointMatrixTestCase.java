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
package edu.ohsu.cslu.datastructs.matrices;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public abstract class DenseFloatingPointMatrixTestCase extends FloatingPointMatrixTestCase implements
        DenseMatrixTestCase {

    @Test
    public void testGetRow() {
        assertArrayEquals(new float[] { 11.11f, 22.22f, 33.33f, 44.44f }, ((DenseMatrix) sampleMatrix).getRow(0), .01f);
        assertArrayEquals(new float[] { 55.55f, 66.66f, 77.77f, 88.88f }, ((DenseMatrix) sampleMatrix).getRow(1), .01f);
        assertArrayEquals(new float[] { 0, 11.11f, 33.33f, 66.66f, 10f }, ((DenseMatrix) symmetricMatrix).getRow(0),
                .01f);
        assertArrayEquals(new float[] { 66.66f, 77.77f, 88.88f, 99.99f, 13.33f },
                ((DenseMatrix) symmetricMatrix).getRow(3), .01f);
        assertArrayEquals(new float[] { 10f, 11.11f, 12.22f, 13.33f, 14.44f },
                ((DenseMatrix) symmetricMatrix).getRow(4), .01f);
    }

    @Test
    public void testGetColumn() {
        assertArrayEquals(new float[] { 11.11f, 55.55f, 99.99f }, ((DenseMatrix) sampleMatrix).getColumn(0), .01f);
        assertArrayEquals(new float[] { 44.44f, 88.88f, 12.11f }, ((DenseMatrix) sampleMatrix).getColumn(3), .01f);
        assertArrayEquals(new float[] { 0, 11.11f, 33.33f, 66.66f, 10f }, ((DenseMatrix) symmetricMatrix).getColumn(0),
                .01f);
        assertArrayEquals(new float[] { 33.33f, 44.44f, 55.55f, 88.88f, 12.22f },
                ((DenseMatrix) symmetricMatrix).getColumn(2), .01f);
        assertArrayEquals(new float[] { 10.00f, 11.11f, 12.22f, 13.33f, 14.44f },
                ((DenseMatrix) symmetricMatrix).getColumn(4), .01f);
    }

    @Test
    public void testSetRow() {
        ((DenseMatrix) sampleMatrix).setRow(0, new int[] { 13, 14, 15, 16 });
        assertArrayEquals(new int[] { 13, 14, 15, 16 }, ((DenseMatrix) sampleMatrix).getIntRow(0));
        assertArrayEquals(new float[] { 13f, 14f, 15f, 16f }, ((DenseMatrix) sampleMatrix).getRow(0), .01f);
        ((DenseMatrix) sampleMatrix).setRow(2, new int[] { 0, 1, 2, 3 });
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, ((DenseMatrix) sampleMatrix).getIntRow(2));
        assertArrayEquals(new float[] { 0f, 1f, 2f, 3f }, ((DenseMatrix) sampleMatrix).getRow(2), .01f);

        ((DenseMatrix) sampleMatrix2).setRow(2, 2.3f);
        assertArrayEquals(new int[] { 2, 2, 2, 2 }, ((DenseMatrix) sampleMatrix2).getIntRow(2));
        assertArrayEquals(new float[] { 2.3f, 2.3f, 2.3f, 2.3f }, ((DenseMatrix) sampleMatrix2).getRow(2), .01f);

        ((DenseMatrix) symmetricMatrix).setRow(0, new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.0f });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, ((DenseMatrix) symmetricMatrix).getIntRow(0));
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.0f }, ((DenseMatrix) symmetricMatrix).getRow(0), .01f);
        ((DenseMatrix) symmetricMatrix).setRow(4, new float[] { 10.1f, 11.1f, 12.1f, 13.1f, 14.1f });
        assertArrayEquals(new int[] { 10, 11, 12, 13, 14 }, ((DenseMatrix) symmetricMatrix).getIntRow(4));
        assertArrayEquals(new float[] { 10.1f, 11.1f, 12.1f, 13.1f, 14.1f }, ((DenseMatrix) symmetricMatrix).getRow(4),
                .01f);

        ((DenseMatrix) symmetricMatrix2).setRow(4, 1.51f);
        assertArrayEquals(new int[] { 2, 2, 2, 2, 2 }, ((DenseMatrix) symmetricMatrix2).getIntRow(4));
        assertArrayEquals(new float[] { 1.51f, 1.51f, 1.51f, 1.51f, 1.51f },
                ((DenseMatrix) symmetricMatrix2).getRow(4), .01f);
    }

    @Test
    public void testSetColumn() {
        ((DenseMatrix) sampleMatrix).setColumn(0, new float[] { 13.1f, 14.1f, 15.1f });
        assertArrayEquals(new int[] { 13, 14, 15 }, ((DenseMatrix) sampleMatrix).getIntColumn(0));
        assertArrayEquals(new float[] { 13.1f, 14.1f, 15.1f }, ((DenseMatrix) sampleMatrix).getColumn(0), .01f);
        ((DenseMatrix) sampleMatrix).setColumn(2, new float[] { 0, 1.1f, 2.2f });
        assertArrayEquals(new int[] { 0, 1, 2 }, ((DenseMatrix) sampleMatrix).getIntColumn(2));
        assertArrayEquals(new float[] { 0f, 1.1f, 2.2f }, ((DenseMatrix) sampleMatrix).getColumn(2), .01f);

        ((DenseMatrix) sampleMatrix2).setColumn(2, 2.3f);
        assertArrayEquals(new int[] { 2, 2, 2 }, ((DenseMatrix) sampleMatrix2).getIntColumn(2));
        assertArrayEquals(new float[] { 2.3f, 2.3f, 2.3f }, ((DenseMatrix) sampleMatrix2).getColumn(2), .01f);

        ((DenseMatrix) symmetricMatrix).setColumn(0, new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 6 }, ((DenseMatrix) symmetricMatrix).getIntColumn(0));
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f }, ((DenseMatrix) symmetricMatrix).getColumn(0),
                .01f);
        ((DenseMatrix) symmetricMatrix).setColumn(4, new float[] { 10.1f, 11.1f, 12.1f, 13.1f, 14.1f });
        assertArrayEquals(new int[] { 10, 11, 12, 13, 14 }, ((DenseMatrix) symmetricMatrix).getIntColumn(4));
        assertArrayEquals(new float[] { 10.1f, 11.1f, 12.1f, 13.1f, 14.1f },
                ((DenseMatrix) symmetricMatrix).getColumn(4), .01f);

        ((DenseMatrix) symmetricMatrix2).setColumn(4, 1.51f);
        assertArrayEquals(new int[] { 2, 2, 2, 2, 2 }, ((DenseMatrix) symmetricMatrix2).getIntColumn(4));
        assertArrayEquals(new float[] { 1.51f, 1.51f, 1.51f, 1.51f, 1.51f },
                ((DenseMatrix) symmetricMatrix2).getColumn(4), .01f);
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
     * Tests extracting a submatrix from an existing matrix (including both symmetric and non-symmetric submatrices of a
     * symmetric matrix).
     * 
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSubMatrix() throws Exception {
        // 1 x 1 matrices
        Matrix submatrix = ((DenseMatrix) sampleMatrix).subMatrix(0, 0, 0, 0);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(11.11f, submatrix.getFloat(0, 0), .01f);

        submatrix = ((DenseMatrix) sampleMatrix).subMatrix(2, 2, 3, 3);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(12.11f, submatrix.getFloat(0, 0), .01f);

        // m x n matrix
        submatrix = ((DenseMatrix) sampleMatrix).subMatrix(0, 2, 0, 3);
        assertEquals("Wrong number of rows", 3, submatrix.rows());
        assertEquals("Wrong number of columns", 4, submatrix.columns());
        assertEquals(66.66f, submatrix.getFloat(1, 1), .01f);
        assertEquals(12.11f, submatrix.getFloat(2, 3), .01f);

        // And finally a true submatrix
        submatrix = ((DenseMatrix) sampleMatrix).subMatrix(1, 2, 1, 3);
        assertEquals("Wrong number of rows", 2, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(66.66f, submatrix.getFloat(0, 0), .01f);
        assertEquals(77.77f, submatrix.getFloat(0, 1), .01f);
        assertEquals(11.11f, submatrix.getFloat(1, 1), .01f);
        assertEquals(12.11f, submatrix.getFloat(1, 2), .01f);

        // And similar tests for symmetric matrices
        // 1 x 1 matrices
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(0, 0, 0, 0);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(0, submatrix.getFloat(0, 0), .01f);
        assertTrue(submatrix.isSymmetric());

        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(4, 4, 4, 4);
        assertEquals("Wrong number of rows", 1, submatrix.rows());
        assertEquals("Wrong number of columns", 1, submatrix.columns());
        assertEquals(14.44f, submatrix.getFloat(0, 0), .01f);
        assertTrue(submatrix.isSymmetric());

        // m x n matrix
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(0, 4, 0, 4);
        assertEquals("Wrong number of rows", 5, submatrix.rows());
        assertEquals("Wrong number of columns", 5, submatrix.columns());
        assertEquals(22.22f, submatrix.getFloat(1, 1), .01f);
        assertEquals(55.55f, submatrix.getFloat(2, 2), .01f);
        assertEquals(88.88f, submatrix.getFloat(2, 3), .01f);
        assertTrue(submatrix.isSymmetric());

        // A symmetric submatrix (taken from across the diagonal)
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(1, 3, 1, 3);
        assertEquals("Wrong number of rows", 3, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(22.22f, submatrix.getFloat(0, 0), .01f);
        assertEquals(44.44f, submatrix.getFloat(0, 1), .01f);
        assertEquals(55.55f, submatrix.getFloat(1, 1), .01f);
        assertEquals(88.88f, submatrix.getFloat(1, 2), .01f);
        assertEquals(99.99f, submatrix.getFloat(2, 2), .01f);
        assertTrue(submatrix.isSquare());
        assertTrue(submatrix.isSymmetric());

        // A square submatrix that is not symmetric
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(1, 3, 0, 2);
        assertEquals("Wrong number of rows", 3, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(11.11f, submatrix.getFloat(0, 0), .01f);
        assertEquals(22.22f, submatrix.getFloat(0, 1), .01f);
        assertEquals(44.44f, submatrix.getFloat(0, 2), .01f);
        assertEquals(66.66f, submatrix.getFloat(2, 0), .01f);
        assertEquals(77.77f, submatrix.getFloat(2, 1), .01f);
        assertEquals(88.88f, submatrix.getFloat(2, 2), .01f);
        assertTrue(submatrix.isSquare());
        assertFalse(submatrix.isSymmetric());

        // A non-square submatrix
        submatrix = ((DenseMatrix) symmetricMatrix).subMatrix(1, 2, 1, 3);
        assertEquals("Wrong number of rows", 2, submatrix.rows());
        assertEquals("Wrong number of columns", 3, submatrix.columns());
        assertEquals(22.22f, submatrix.getFloat(0, 0), .01f);
        assertEquals(44.44f, submatrix.getFloat(0, 1), .01f);
        assertEquals(77.77f, submatrix.getFloat(0, 2), .01f);
        assertEquals(55.55f, submatrix.getFloat(1, 1), .01f);
        assertEquals(88.88f, submatrix.getFloat(1, 2), .01f);
        assertFalse(submatrix.isSquare());
        assertFalse(submatrix.isSymmetric());
    }
}
