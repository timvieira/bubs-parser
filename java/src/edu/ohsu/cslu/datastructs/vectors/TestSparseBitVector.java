package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unit tests for {@link SparseBitVector}.
 * 
 * @author Aaron Dunlop
 * @since Mar 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestSparseBitVector extends BitVectorTestCase
{
    @Override
    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vector type=sparse-bit length=35\n");
        sb.append("0 2 3 4 7 8 10 11 12 14 18 20 21 23 24 25 26 29 32 33 34\n");
        stringSampleVector = sb.toString();

        int[] sampleArray = new int[] {0, 2, 3, 4, 7, 8, 10, 11, 12, 14, 18, 20, 21, 23, 24, 25, 26, 29, 32, 33, 34};
        sampleVector = new SparseBitVector(sampleArray);

        vectorClass = SparseBitVector.class;
    }

    @Override
    protected BitVector createEmptyBitVector()
    {
        return new SparseBitVector();
    }

    /**
     * Verifies that the constructor corrects mis-ordered elements
     * 
     * @throws Exception
     */
    @Test
    public void testConstructor() throws Exception
    {
        SparseBitVector v = new SparseBitVector(new int[] {8, 5, 7, 3});
        SharedNlpTests.assertEquals(new int[] {3, 5, 7, 8}, v.values());
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testSet() throws Exception
    {
        sampleVector.set(2, true);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testSetAdd()
    {
        ((BitVector) sampleVector).add(3);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testArrayAddAll()
    {
        ((BitVector) sampleVector).addAll(new int[] {1, 3});
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testIntSetAddAll()
    {
        ((BitVector) sampleVector).addAll(new IntOpenHashSet(Arrays.asList(1, 3)));
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testRemove()
    {
        ((BitVector) sampleVector).remove(2);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testArrayRemoveAll()
    {
        ((BitVector) sampleVector).removeAll(new int[] {2, 3});
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testIntSetRemoveAll()
    {
        ((BitVector) sampleVector).removeAll(new IntOpenHashSet(Arrays.asList(1, 3)));
    }
}
