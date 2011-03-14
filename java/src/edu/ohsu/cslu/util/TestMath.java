package edu.ohsu.cslu.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TestMath {

    @Test
    public void testPow() throws Exception {
        try {
            Math.pow(0, 0);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }

        assertEquals(0, Math.pow(0, 3));
        assertEquals(1, Math.pow(3, 0));

        assertEquals(8, Math.pow(2, 3));
        assertEquals(49, Math.pow(7, 2));
        assertEquals(-8, Math.pow(-2, 3));
        assertEquals(16, Math.pow(-2, 4));
        assertEquals(1073741824, Math.pow(2, 30));
        assertEquals(1073741824, Math.pow(8, 10));
    }

    @Test
    public void testIsPowerOf2() throws Exception {
        assertTrue(Math.isPowerOf2(0));
        assertTrue(Math.isPowerOf2(1));
        assertTrue(Math.isPowerOf2(2));
        assertTrue(Math.isPowerOf2(4));
        assertTrue(Math.isPowerOf2(65536));
        assertTrue(Math.isPowerOf2(1073741824));

        assertFalse(Math.isPowerOf2(3));
        assertFalse(Math.isPowerOf2(-1));
        assertFalse(Math.isPowerOf2(9));
    }

    @Test
    public void testLogBase2() throws Exception {
        // log(0) is undefined, but we'll treat it as 0
        assertEquals(0, Math.logBase2(0));
        assertEquals(0, Math.logBase2(1));
        assertEquals(1, Math.logBase2(2));
        assertEquals(1, Math.logBase2(3));
        assertEquals(2, Math.logBase2(4));
        assertEquals(2, Math.logBase2(5));
        assertEquals(3, Math.logBase2(8));
        assertEquals(30, Math.logBase2(1073741824));

        try {
            Math.logBase2(-1);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }
    }

    @Test
    public void testNextPowerOf2() throws Exception {
        assertEquals(0, Math.nextPowerOf2(0));
        assertEquals(1, Math.nextPowerOf2(1));
        assertEquals(2, Math.nextPowerOf2(2));
        assertEquals(4, Math.nextPowerOf2(4));
        assertEquals(8, Math.nextPowerOf2(7));
        assertEquals(8, Math.nextPowerOf2(8));
        assertEquals(1073741824, Math.nextPowerOf2(800000001));

        assertEquals(0, Math.nextPowerOf2(-1));
        assertEquals(0, Math.nextPowerOf2(-500000));
    }

    @Test
    public void testPreviousPowerOf2() throws Exception {
        assertEquals(0, Math.previousPowerOf2(0));
        assertEquals(1, Math.previousPowerOf2(1));
        assertEquals(2, Math.previousPowerOf2(2));
        assertEquals(4, Math.previousPowerOf2(4));
        assertEquals(4, Math.previousPowerOf2(5));
        assertEquals(8, Math.previousPowerOf2(8));
        assertEquals(8, Math.previousPowerOf2(15));
        assertEquals(536870912, Math.previousPowerOf2(800000001));

        assertEquals(0, Math.previousPowerOf2(-1));
        assertEquals(0, Math.previousPowerOf2(-5000000));
    }

    @Test
    public void testRoundUp() throws Exception {
        assertEquals(0, Math.roundUp(0, 1));
        assertEquals(0, Math.roundUp(0, 8));
        assertEquals(1, Math.roundUp(1, 1));
        assertEquals(2, Math.roundUp(1, 2));
        assertEquals(2, Math.roundUp(2, 2));
        assertEquals(16, Math.roundUp(13, 4));

        assertEquals(-1, Math.roundUp(-1, 1));
        assertEquals(8, Math.roundUp(-1, 8));
    }

    @Test
    public void testLogSum() {
        assertEquals(java.lang.Math.log(.5),
                Math.logSum((float) java.lang.Math.log(.25), (float) java.lang.Math.log(.25)), 0.001f);
        assertEquals(Float.NEGATIVE_INFINITY, Math.logSum(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY), 0.01f);
        assertEquals(-10f, Math.logSum(Float.NEGATIVE_INFINITY, -10f), 0.01f);
        assertEquals(-10f, Math.logSum(-10f, Float.NEGATIVE_INFINITY), 0.01f);
        assertEquals(-10f, Math.logSum(-50f, -10f), 0.01f);
        assertEquals(-10f, Math.logSum(-10f, -50f), 0.01f);
    }
}
