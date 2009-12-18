package edu.ohsu.cslu.datastructs.vectors;

/**
 * Unit tests for {@link HashSparseFloatVector}
 * 
 * TODO Add tests for vectors extending past the limits of an int
 * 
 * @author Aaron Dunlop
 * @since Dec 16, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestHashSparseFloatVector extends FloatVectorTestCase
{

    @Override
    public void setUp() throws Exception
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=hash-sparse-float length=11 sparse=true\n");
        sb
            .append("0 -11.000000 2 11.000000 3 22.000000 4 33.000000 5 44.000000 6 56.000000 7 67.000000 8 78.000000 9 89.000000 10 100.000000\n");
        stringSampleVector = sb.toString();

        final float[] sampleArray = new float[] {0, -11, 2, 11, 3, 22, 4, 33, 5, 44, 6, 56, 7, 67, 8, 78, 9, 89, 10,
            100};
        sampleVector = new HashSparseFloatVector(sampleArray);

        vectorClass = HashSparseFloatVector.class;
    }

    @Override
    protected Vector create(final float[] array) throws Exception
    {
        final float[] floatArray = new float[array.length * 2];
        for (int i = 0; i < array.length; i++)
        {
            floatArray[i * 2] = i;
            floatArray[i * 2 + 1] = array[i];
        }
        return new HashSparseFloatVector(floatArray);
    }
}
