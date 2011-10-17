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
        assertEquals("(top (a_1 (a_0 (a_0 (a_0 c) (a_1 c)) (b_1 d)) (b_1 (b_0 (b_1 d)) (a_0 d))))", constrainedChart
                .extractBestParse(0).toString());
        JUnit.assertArrayEquals(OPEN_CELLS, constrainedChart.openCells);

        // Ensure that toString() runs without an exception. We could attempt to verify the output, but that's probably
        // overkill
        constrainedChart.toString();
    }
}
