package edu.ohsu.cslu.datastructs;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Unit tests for {@link FloatingPointLongPairwiseDistanceHeap}
 * 
 * @author Aaron Dunlop
 * @since May 8, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestFloatingPointLongPairwiseDistanceHeap extends PairwiseDistanceHeapTestCase {

    @Override
    protected PairwiseDistanceHeap create() {
        return new FloatingPointLongPairwiseDistanceHeap();
    }

    @Test
    public void testLargeDistances() {
        distanceHeap.insert(0, 1, 1000000f);
        distanceHeap.insert(0, 2, 2e12f);
        distanceHeap.insert(1, 2, 10f);

        int[] indices = distanceHeap.deleteMin();
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(2, indices[1]);

        indices = distanceHeap.deleteMin();
        assertEquals(2, indices.length);
        assertEquals(0, indices[0]);
        assertEquals(1, indices[1]);

        indices = distanceHeap.deleteMin();
        assertEquals(2, indices.length);
        assertEquals(0, indices[0]);
        assertEquals(2, indices[1]);
    }

    @Override
    @Test
    public void testIndexRange() {
        // Indices >= 0 and <= 2^16 (65536) are supported

        distanceHeap.insert(0, 65536, 0);
        distanceHeap.insert(65536, 0, 0);

        try {
            distanceHeap.insert(65537, 0, 0);
            fail("Expected IllegalArgumentException on index 65537");
        } catch (IllegalArgumentException expected) {
        }

        try {
            distanceHeap.insert(0, 65537, 0);
            fail("Expected IllegalArgumentException on index 65537");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Override
    @Test
    public void testDistanceRange() {
        // FloatingPointLongDistanceHeap supports all Float values
        distanceHeap.insert(0, 1, Float.MIN_VALUE);
        distanceHeap.insert(0, 1, Float.MAX_VALUE);
    }
}
