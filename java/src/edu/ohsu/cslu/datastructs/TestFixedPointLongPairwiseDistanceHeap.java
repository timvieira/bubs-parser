package edu.ohsu.cslu.datastructs;

import static junit.framework.Assert.fail;

import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for {@link FixedPointLongPairwiseDistanceHeap}.
 * 
 * @author Aaron Dunlop
 * @since May 8, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestFixedPointLongPairwiseDistanceHeap extends PairwiseDistanceHeapTestCase {

    @Override
    protected PairwiseDistanceHeap create() {
        return new FixedPointLongPairwiseDistanceHeap();
    }

    @Override
    public void testDistanceRange() {
        // Distances >= 0 and <= 1 are supported
        distanceHeap.insert(0, 1, 0f);
        distanceHeap.insert(0, 2, 1f);

        try {
            distanceHeap.insert(0, 3, 1.1f);
            fail("Expected IllegalArgumentException on distance > 1");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Override
    public void testIndexRange() {
        // Indices >= 0 and <= 2^24 (16777216) are supported

        distanceHeap.insert(0, 16777216, 0);
        distanceHeap.insert(16777216, 0, 0);

        try {
            distanceHeap.insert(16777217, 0, 0);
            fail("Expected IllegalArgumentException on index 16777217");
        } catch (IllegalArgumentException expected) {
        }

        try {
            distanceHeap.insert(0, 16777217, 0);
            fail("Expected IllegalArgumentException on index 16777217");
        } catch (IllegalArgumentException expected) {
        }
    }

}
