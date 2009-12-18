package edu.ohsu.cslu.datastructs.matrices;

import static junit.framework.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.HashSparseFloatVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.NumericVector;
import edu.ohsu.cslu.datastructs.vectors.PackedIntVector;

/**
 * Unit tests shared by all integer matrices
 * 
 * @author Aaron Dunlop
 * @since May 6, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class IntMatrixTestCase extends MatrixTestCase {
    protected int[][] sampleArray;
    protected int[][] symmetricArray;

    protected abstract Matrix create(int[][] array, boolean symmetric);

    @Before
    public void setUp() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("matrix type=" + matrixType() + " rows=3 columns=4 symmetric=false\n");
        sb.append("11  22  33  44\n");
        sb.append("56  67  78  89\n");
        sb.append("100 10  11  12\n");
        stringSampleMatrix = sb.toString();

        sampleArray = new int[][] { { 11, 22, 33, 44 }, { 56, 67, 78, 89 }, { 100, 10, 11, 12 } };
        sampleMatrix = create(sampleArray, false);

        sb = new StringBuilder();
        sb.append("matrix type=" + matrixType() + " rows=5 columns=5 symmetric=true\n");
        sb.append("0\n");
        sb.append("11  22\n");
        sb.append("33  44  56\n");
        sb.append("67  78  89  100\n");
        sb.append("10  11  12  13  14\n");
        stringSymmetricMatrix = sb.toString();

        symmetricArray = new int[][] { { 0 }, { 11, 22 }, { 33, 44, 56 }, { 67, 78, 89, 100 },
                { 10, 11, 12, 13, 14 } };
        symmetricMatrix = create(symmetricArray, true);

        matrixClass = symmetricMatrix.getClass();
    }

    /**
     * Tests deserializing a matrix using a Reader
     * 
     * TODO: Combine with MatrixTestCase.testReadFromReader?
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
        assertEquals(11, m1.getInt(0, 0));
        assertEquals(22, m1.getInt(0, 1));
        assertEquals(67, m1.getInt(1, 1));
        assertEquals(78, m1.getInt(1, 2));
        assertEquals(11, m1.getInt(2, 2));
        assertEquals(12, m1.getInt(2, 3));

        final Matrix m2 = Matrix.Factory.read(stringSymmetricMatrix);
        assertEquals(matrixClass, m2.getClass());
        assertEquals("Wrong number of rows", 5, m2.rows());
        assertEquals("Wrong number of columns", 5, m2.columns());
        assertEquals("Wrong value", 0, m2.getInt(0, 0));
        assertEquals("Wrong value", 11, m2.getInt(1, 0));
        assertEquals("Wrong value", 67, m2.getInt(3, 0));
        assertEquals("Wrong value", 78, m2.getInt(3, 1));
        assertEquals("Wrong value", 100, m2.getInt(3, 3));
        assertEquals("Wrong value", 14, m2.getInt(4, 4));
    }

    /**
     * Tests 'getFloat', including reflection across the diagonal in symmetric matrices.
     * 
     * @throws Exception
     *             if something bad happens
     */
    @Test
    public void testGetFloat() throws Exception {
        assertEquals("Wrong value", 11f, sampleMatrix.getFloat(0, 0), .0001f);
        assertEquals("Wrong value", 22f, sampleMatrix.getFloat(0, 1), .0001f);
        assertEquals("Wrong value", 56f, sampleMatrix.getFloat(1, 0), .0001f);
        assertEquals("Wrong value", 89f, sampleMatrix.getFloat(1, 3), .0001f);
        assertEquals("Wrong value", 100f, sampleMatrix.getFloat(2, 0), .0001f);
        assertEquals("Wrong value", 12f, sampleMatrix.getFloat(2, 3), .0001f);

        assertEquals("Wrong value", 0f, symmetricMatrix.getFloat(0, 0), .0001f);
        assertEquals("Wrong value", 11f, symmetricMatrix.getFloat(1, 0), .0001f);
        assertEquals("Wrong value", 67f, symmetricMatrix.getFloat(3, 0), .0001f);
        assertEquals("Wrong value", 78f, symmetricMatrix.getFloat(3, 1), .0001f);
        assertEquals("Wrong value", 100f, symmetricMatrix.getFloat(3, 3), .0001f);
        assertEquals("Wrong value", 14f, symmetricMatrix.getFloat(4, 4), .0001f);

        // And a couple values that are out of the storage area, but should be reflected about the
        // diagonal
        assertEquals("Wrong value", 67f, symmetricMatrix.getFloat(0, 3), .0001f);
        assertEquals("Wrong value", 78f, symmetricMatrix.getFloat(1, 3), .0001f);
        assertEquals("Wrong value", 89f, symmetricMatrix.getFloat(2, 3), .0001f);
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
        assertEquals("Wrong value", 100, sampleMatrix.getInt(2, 0));
        sampleMatrix.set(2, 0, 1);
        assertEquals("Wrong value", 1, sampleMatrix.getInt(2, 0));

        assertEquals("Wrong value", 67, symmetricMatrix.getInt(0, 3));
        symmetricMatrix.set(0, 3, 15);
        assertEquals("Wrong value", 15, symmetricMatrix.getInt(0, 3));

        // We expect float values to be rounded
        symmetricMatrix.set(0, 3, 13.6f);
        symmetricMatrix.set(1, 3, 13.4f);
        assertEquals("Wrong value", 14, symmetricMatrix.getInt(0, 3));
        assertEquals("Wrong value", 13, symmetricMatrix.getInt(1, 3));
    }

    @Override
    @Test
    public void testScalarAdd() throws Exception {
        Matrix m = sampleMatrix.scalarAdd(3);
        assertEquals(matrixClass, m.getClass());
        assertEquals(81, m.getInt(1, 2));
        assertEquals(15, m.getInt(2, 3));

        m = sampleMatrix.scalarAdd(2.6f);
        assertEquals(FloatMatrix.class, m.getClass());
        assertEquals(80.6, m.getFloat(1, 2), .01f);
        assertEquals(14.6, m.getFloat(2, 3), .01f);
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
        assertEquals(matrixClass, m.getClass());
        assertEquals(234, m.getInt(1, 2));
        assertEquals(36, m.getInt(2, 3));

        m = sampleMatrix.scalarMultiply(3.6f);
        assertEquals(FloatMatrix.class, m.getClass());
        assertEquals(280.8, m.getFloat(1, 2), .01f);
        assertEquals(43.2, m.getFloat(2, 3), .01f);
    }

    /**
     * Tests matrix/vector multiplication
     */
    @Override
    @Test
    public void testVectorMultiply() throws Exception {
        // Multiply by an IntVector
        NumericVector v = new IntVector(new int[] { 1, 2, 3, 4 });
        NumericVector product = sampleMatrix.multiply(v);
        assertEquals(v.getClass(), product.getClass());
        assertEquals(new IntVector(new int[] { 330, 780, 201 }), product);

        // Multiply by a PackedIntVector
        v = new PackedIntVector(new int[] { 1, 2, 3, 4 }, 16);
        product = sampleMatrix.multiply(v);
        assertEquals(v.getClass(), product.getClass());
        assertEquals(new PackedIntVector(new int[] { 330, 780, 201 }, 16), product);

        // Multiply by a FloatVector
        v = new FloatVector(new float[] { 1, 2, 3, 4 });
        product = sampleMatrix.multiply(v);
        assertEquals(v.getClass(), product.getClass());
        assertEquals(new FloatVector(new float[] { 330, 780, 201 }), product);

        // Multiply by a HashSparseFloatVector
        v = new HashSparseFloatVector(new float[] { 0, 1, 1, 2, 2, 3, 3, 4 });
        product = sampleMatrix.multiply(v);
        assertEquals(v.getClass(), product.getClass());
        assertEquals(new HashSparseFloatVector(new float[] { 0, 330, 1, 780, 2, 201 }), product);

        // TODO Add tests for all NumericVector implementations
    }

    /**
     * Tests equals() method
     * 
     * @throws Exception
     *             if something bad happens
     */
    @Test
    public void testEquals() throws Exception {
        assertEquals(sampleMatrix, create(sampleArray, false));
        assertEquals(symmetricMatrix, create(symmetricArray, true));
    }

}
