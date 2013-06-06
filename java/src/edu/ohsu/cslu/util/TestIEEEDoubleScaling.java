/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */

package edu.ohsu.cslu.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link IEEEDoubleScaling}.
 * 
 * @author Aaron Dunlop
 * @since Mar 19, 2013
 */
public class TestIEEEDoubleScaling {

    @Test
    public void testScaleArray() {
        final double[] d = new double[] { 1e-150, 1e-149, 1e-125, 1e-110 };
        final double[] d2 = d.clone();

        final int expectedScale = (int) java.lang.Math.round(-110 / java.lang.Math.log10(IEEEDoubleScaling.SCALE1)) + 1;
        assertEquals(expectedScale, IEEEDoubleScaling.scaleArray(d2, 0));

        for (int i = 0; i < d.length; i++) {
            assertEquals(d[i] * java.lang.Math.pow(IEEEDoubleScaling.SCALE1, expectedScale), d2[i], .1f);
        }
    }

    @Test
    public void testLogLikelihood() {

        final double score = 1e-10;
        assertEquals(java.lang.Math.log(score), IEEEDoubleScaling.logLikelihood(score, 0), .01);

        assertEquals(java.lang.Math.log(score) - IEEEDoubleScaling.LN_S, IEEEDoubleScaling.logLikelihood(score, -1),
                .01);
        assertEquals(java.lang.Math.log(score) - IEEEDoubleScaling.LN_S * 2,
                IEEEDoubleScaling.logLikelihood(score, -2), .01);
        assertEquals(java.lang.Math.log(score) - IEEEDoubleScaling.LN_S * 5,
                IEEEDoubleScaling.logLikelihood(score, -5), .01);

        assertEquals(java.lang.Math.log(score) + IEEEDoubleScaling.LN_S, IEEEDoubleScaling.logLikelihood(score, 1), .01);
        assertEquals(java.lang.Math.log(score) + IEEEDoubleScaling.LN_S * 2, IEEEDoubleScaling.logLikelihood(score, 2),
                .01);
        assertEquals(java.lang.Math.log(score) + IEEEDoubleScaling.LN_S * 5, IEEEDoubleScaling.logLikelihood(score, 5),
                .01);
    }

    @Test
    public void testUnscale() {

        final double score = 1e-10;
        assertEquals(score, IEEEDoubleScaling.unscale(score, 0), .01);

        assertEquals(score / IEEEDoubleScaling.SCALE1, IEEEDoubleScaling.unscale(score, -1), 1e-20);
        assertEquals(score / IEEEDoubleScaling.SCALE2, IEEEDoubleScaling.unscale(score, -2), 1e-20);
        assertEquals(score / IEEEDoubleScaling.SCALE3 / IEEEDoubleScaling.SCALE2, IEEEDoubleScaling.unscale(score, -5),
                1e-20);

        assertEquals(score * IEEEDoubleScaling.SCALE1, IEEEDoubleScaling.unscale(score, 1), 1e20);
        assertEquals(score * IEEEDoubleScaling.SCALE2, IEEEDoubleScaling.unscale(score, 2), 1e70);
        assertEquals(score * IEEEDoubleScaling.SCALE3 * IEEEDoubleScaling.SCALE2, IEEEDoubleScaling.unscale(score, 5),
                1e200);
    }
}
