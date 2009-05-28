package edu.ohsu.cslu.datastructs;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.PairwiseDistanceHeap.PairwiseDistance;

/**
 * Unit tests for {@link PairwiseDistanceHeap} implementations.
 * 
 * @author Aaron Dunlop
 * @since May 8, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class PairwiseDistanceHeapTestCase
{
    protected PairwiseDistanceHeap distanceHeap;

    @Before
    public void setUp()
    {
        distanceHeap = create();
    }

    /**
     * Returns a {@link PairwiseDistanceHeap} implementation of the appropriate class
     * 
     * @return a {@link PairwiseDistanceHeap} implementation of the appropriate class
     */
    protected abstract PairwiseDistanceHeap create();

    /**
     * Tests boundaries of the range of index values supported by the implementation under test
     */
    @Test
    public abstract void testIndexRange();

    /**
     * Tests boundaries of the range of distance values supported by the implementation under test
     */
    @Test
    public abstract void testDistanceRange();

    @Test
    public void testInsert()
    {
        assertEquals("Wrong size", 0, distanceHeap.size());
        distanceHeap.insert(0, 1, .5f);
        assertEquals("Wrong size", 1, distanceHeap.size());

        // Distances < 0 or Infinite are not valid
        try
        {
            distanceHeap.insert(0, 1, -.5f);
            fail("Expected IllegalArgumentException on distance < 0");
        }
        catch (IllegalArgumentException expected)
        {}

        try
        {
            distanceHeap.insert(0, 1, Float.POSITIVE_INFINITY);
            fail("Expected IllegalArgumentException on infinite distance");
        }
        catch (IllegalArgumentException expected)
        {}

        try
        {
            distanceHeap.insert(0, 1, Float.NaN);
            fail("Expected IllegalArgumentException on Nan distance");
        }
        catch (IllegalArgumentException expected)
        {}

        // Indices < 0 are not valid
        try
        {
            distanceHeap.insert(-1, 0, 0);
            fail("Expected IllegalArgumentException on index -1");
        }
        catch (IllegalArgumentException expected)
        {}

        try
        {
            distanceHeap.insert(0, -1, 0);
            fail("Expected IllegalArgumentException on index -1");
        }
        catch (IllegalArgumentException expected)
        {}
    }

    @Test
    public void testDeleteMin()
    {
        distanceHeap.insert(0, 1, .5f);
        distanceHeap.insert(0, 2, .7f);
        distanceHeap.insert(1, 2, .2f);

        int[] indices = distanceHeap.deleteMin();
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(2, indices[1]);
        assertEquals("Wrong size", 2, distanceHeap.size());

        indices = distanceHeap.deleteMin();
        assertEquals(2, indices.length);
        assertEquals(0, indices[0]);
        assertEquals(1, indices[1]);
        assertEquals("Wrong size", 1, distanceHeap.size());

        indices = distanceHeap.deleteMin();
        assertEquals(2, indices.length);
        assertEquals(0, indices[0]);
        assertEquals(2, indices[1]);
        assertEquals("Wrong size", 0, distanceHeap.size());

        try
        {
            distanceHeap.deleteMin();
            fail("Expected NoSuchElementException when attempting to delete from an empty heap");
        }
        catch (NoSuchElementException expected)
        {}
    }

    @Test
    public void testDeleteMinDistance()
    {
        distanceHeap.insert(0, 1, .5f);
        distanceHeap.insert(0, 2, .7f);
        distanceHeap.insert(1, 2, .2f);

        PairwiseDistance d = distanceHeap.deleteMinDistance();
        assertEquals(1, d.index1);
        assertEquals(2, d.index2);
        assertEquals(.2f, d.distance, .01f);
        assertEquals("Wrong size", 2, distanceHeap.size());

        d = distanceHeap.deleteMinDistance();
        assertEquals(0, d.index1);
        assertEquals(1, d.index2);
        assertEquals(.5f, d.distance, .01f);
        assertEquals("Wrong size", 1, distanceHeap.size());

        d = distanceHeap.deleteMinDistance();
        assertEquals(0, d.index1);
        assertEquals(2, d.index2);
        assertEquals(.7f, d.distance, .01f);
        assertEquals("Wrong size", 0, distanceHeap.size());

        try
        {
            distanceHeap.deleteMin();
            fail("Expected NoSuchElementException when attempting to delete from an empty heap");
        }
        catch (NoSuchElementException expected)
        {}
    }

    @Test
    public void testSize()
    {
        assertEquals("Wrong size", 0, distanceHeap.size());
        distanceHeap.insert(0, 0, 0);
        assertEquals("Wrong size", 1, distanceHeap.size());
        distanceHeap.insert(0, 1, .5f);
        assertEquals("Wrong size", 2, distanceHeap.size());

        distanceHeap.deleteMin();
        assertEquals("Wrong size", 1, distanceHeap.size());
        distanceHeap.deleteMin();
        assertEquals("Wrong size", 0, distanceHeap.size());
    }

    @Test
    public void testIsEmpty()
    {
        assertTrue("Expected heap to be empty", distanceHeap.isEmpty());
        distanceHeap.insert(0, 0, 0);
        assertFalse("Did not expect heap to be empty", distanceHeap.isEmpty());
        distanceHeap.deleteMin();
        assertTrue("Expected heap to be empty", distanceHeap.isEmpty());
    }
}
