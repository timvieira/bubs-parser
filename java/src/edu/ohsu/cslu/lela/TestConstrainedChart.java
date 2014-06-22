/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.lela;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.tests.JUnit;

/**
 * Unit tests for {@link ConstrainedChart}
 * 
 * @author Aaron Dunlop
 */
public class TestConstrainedChart extends ChartTestCase {

    @Test
    public void testExtractBestParse() {
        // Create and populate a 1-split ConstrainedChart and verify that we can extract the expected tree
        final ConstrainedChart constrainedChart = create1SplitConstrainedChart();
        assertEquals("(top (a_1 (a_0 (a_0 (c_0 e) (c_1 e)) (d_1 f)) (b_1 (b_0 (d_1 f)) (c_0 f))))", constrainedChart
                .extractBestParse(0).toString());
        JUnit.assertArrayEquals(OPEN_CELLS, constrainedChart.openCells);

        // Ensure that toString() runs without an exception. We could attempt to verify the output, but that's probably
        // overkill
        constrainedChart.toString();
    }
}
