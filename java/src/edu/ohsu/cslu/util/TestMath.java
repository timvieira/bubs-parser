/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TestMath {

    @Test
    public void testMean() {
        assertEquals(0f, Math.mean(), .0001f);
        assertEquals(0f, Math.mean(new int[0]), .0001f);
        assertEquals(.25f, Math.mean(new int[] { -2, -1, 0, 4 }), .0001f);
    }

    @Test
    public void testMedian() {
        assertEquals(0f, Math.median(), .0001f);
        assertEquals(0f, Math.median(new int[0]), .0001f);
        assertEquals(1f, Math.median(new int[] { -2, -1, 1, 2, 4 }), .0001f);
        assertEquals(-.5f, Math.median(new int[] { -2, -1, 0, 4 }), .0001f);
    }

    @Test
    public void testMin() {
        assertEquals('a', Math.min(new char[] { 'b', 'c', 'z', 'a' }));
        assertEquals(-2, Math.min(new int[] { -2, -1, 0, 4 }));
        assertEquals(-3, Math.doubleMin(new double[] { -2, -1, 0, 1, -3 }), .001);
        assertEquals(-3f, Math.floatMin(new float[] { -2, -1, 0, 1, -3 }), .001f);
    }

    @Test
    public void testMax() {
        assertEquals('z', Math.max(new char[] { 'b', 'c', 'z', 'a' }));
        assertEquals(4, Math.max(new int[] { -2, -1, 0, 4 }));
        assertEquals(1, Math.doubleMax(new double[] { -2, -1, 0, 1, -3 }), .001);
        assertEquals(1f, Math.floatMax(new float[] { -2, -1, 0, 1, -3 }), .001f);
    }

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

    @Test
    public void testApproximateLog() {
        for (final float x : new float[] { 0f, 1f, .9999f, .999f, .99f, .9f, .5f, .1f, .01f, .001f, .0001f, .00001f,
                .000001f }) {
            assertEquals(java.lang.Math.log(x), Math.approximateLog(x), .001f);
        }
    }

    @Test
    public void testFastApproximateLog() {
        for (final float x : new float[] { 0f, 1f, .9999f, .999f, .99f, .9f, .5f, .1f, .01f, .001f, .0001f, .00001f,
                .000001f }) {
            assertEquals(java.lang.Math.log(x), Math.fastApproximateLog(x), .04f);
        }
    }

    @Test
    public void testApproximateLogSum() {
        assertEquals(java.lang.Math.log(.5),
                Math.approximateLogSum((float) java.lang.Math.log(.25), (float) java.lang.Math.log(.25)), 0.05f);
        assertEquals(Float.NEGATIVE_INFINITY, Math.logSum(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY), 0.1f);
        assertEquals(-10f, Math.approximateLogSum(Float.NEGATIVE_INFINITY, -10f), 0.1f);
        assertEquals(-10f, Math.approximateLogSum(-10f, Float.NEGATIVE_INFINITY), 0.1f);
        assertEquals(-10f, Math.approximateLogSum(-50f, -10f), 0.1f);
        assertEquals(-10f, Math.approximateLogSum(-10f, -50f), 0.1f);
    }

    @Test
    public void testLogSumExp() {
        final float a = (float) java.lang.Math.log(.25);
        assertEquals(0, Math.logSumExp(new float[] { a, a, a, a }), .001f);
    }

    @Test
    public void testApproximateLogSumExp() {
        final float a = (float) java.lang.Math.log(.25);
        assertEquals(0, Math.approximateLogSumExp(new float[] { a, a, a, a }), .05f);
    }

    @Test
    public void testLogistic() {
        // Standard logistic function
        assertEquals(0, Math.logistic(Float.NEGATIVE_INFINITY), 0.001f);
        assertEquals(.001f, Math.logistic(-1000000f), .001f);
        assertEquals(.5f, Math.logistic(0), 0.001f);
        assertEquals(.999f, Math.logistic(1000000f), .001f);
        assertEquals(1, Math.logistic(Float.POSITIVE_INFINITY), 0.001f);

        // Generalized logistic function
        assertEquals(0, Math.logistic(.05f, Float.NEGATIVE_INFINITY), 0.001f);
        assertEquals(.001f, Math.logistic(.05f, -1000000f), .001f);
        assertEquals(.007f, Math.logistic(.05f, -100f), .001f);
        assertEquals(.05f, Math.logistic(.05f, -59f), .001f);
        assertEquals(.5f, Math.logistic(.05f, 0), 0.001f);
        assertEquals(.95f, Math.logistic(.05f, 59f), .001f);
        assertEquals(.993f, Math.logistic(.05f, 100f), .001f);
        assertEquals(.999, Math.logistic(.05f, 1000000f), .001f);
        assertEquals(1, Math.logistic(.05f, Float.POSITIVE_INFINITY), 0.001f);
    }
}
