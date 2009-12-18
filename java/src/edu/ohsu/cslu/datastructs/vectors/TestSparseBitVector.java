package edu.ohsu.cslu.datastructs.vectors;

import static org.junit.Assert.assertArrayEquals;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for {@link SparseBitVector}.
 * 
 * @author Aaron Dunlop
 * @since Mar 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestSparseBitVector extends BitVectorTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=sparse-bit length=35 sparse=true\n");
        sb
            .append("0 1 2 1 3 1 4 1 7 1 8 1 10 1 11 1 12 1 14 1 18 1 20 1 21 1 23 1 24 1 25 1 26 1 29 1 32 1 33 1 34 1\n");
        stringSampleVector = sb.toString();

        final int[] sampleArray = new int[] { 0, 1, 2, 1, 3, 1, 4, 1, 7, 1, 8, 1, 10, 1, 11, 1, 12, 1, 14, 1,
                18, 1, 20, 1, 21, 1, 23, 1, 24, 1, 25, 1, 26, 1, 29, 1, 32, 1, 33, 1, 34, 1 };
        sampleVector = new SparseBitVector(sampleArray, true);

        vectorClass = SparseBitVector.class;
    }

    @Override
    protected BitVector createEmptyBitVector() {
        return new SparseBitVector();
    }

    /**
     * Verifies that the constructor corrects mis-ordered elements
     * 
     * @throws Exception
     */
    @Test
    public void testConstructors() throws Exception {
        SparseBitVector v = new SparseBitVector(new int[] { 8, 1, 5, 1, 7, 1, 3, 1 }, true);
        assertArrayEquals(new int[] { 3, 5, 7, 8 }, v.values());

        v = new SparseBitVector(new int[] { 8, 5, 7, 3 }, false);
        assertArrayEquals(new int[] { 3, 5, 7, 8 }, v.values());
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testSet() throws Exception {
        sampleVector.set(2, true);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testSetAdd() {
        ((BitVector) sampleVector).add(3);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testArrayAddAll() {
        ((BitVector) sampleVector).addAll(new int[] { 1, 3 });
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testIntSetAddAll() {
        ((BitVector) sampleVector).addAll(new IntOpenHashSet(Arrays.asList(1, 3)));
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
        ((BitVector) sampleVector).remove(2);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testArrayRemoveAll() {
        ((BitVector) sampleVector).removeAll(new int[] { 2, 3 });
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testIntSetRemoveAll() {
        ((BitVector) sampleVector).removeAll(new IntOpenHashSet(Arrays.asList(1, 3)));
    }
}
