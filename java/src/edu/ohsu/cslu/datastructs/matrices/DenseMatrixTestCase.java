package edu.ohsu.cslu.datastructs.matrices;

import org.junit.Test;

public interface DenseMatrixTestCase
{
    /**
     * Tests getRow() method.
     */
    @Test
    public void testGetRow();

    /**
     * Tests getColumn() method
     */
    @Test
    public void testGetColumn();

    /**
     * Tests getIntRow() method.
     */
    @Test
    public void testGetIntRow();

    /**
     * Tests getIntColumn() method.
     */
    @Test
    public void testGetIntColumn();

    /**
     * Tests setRow() method.
     */
    @Test
    public void testSetRow();

    /**
     * Tests setColumn() method
     */
    @Test
    public void testSetColumn();

}
