package edu.ohsu.cslu.datastructs.vectors;

public abstract class SparseIntVectorTestCase<V extends IntVector> extends IntVectorTestCase<V> {

    @Override
    public void setUp() throws Exception {
        final int[] sampleArray = new int[] { -11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100 };
        sampleVector = create(sampleArray);

        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=" + serializedName() + " length=11 sparse="
                + ((sampleVector instanceof SparseVector) ? "true\n" : "false\n"));
        sb.append("0 -11 1 0 2 11 3 22 4 33 5 44 6 56 7 67 8 78 9 89 10 100\n");
        stringSampleVector = sb.toString();
    }

}
