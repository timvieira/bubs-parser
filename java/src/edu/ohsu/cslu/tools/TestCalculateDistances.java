package edu.ohsu.cslu.tools;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import cltool4j.ToolTestCase;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unit tests for the command-line distance calculation tool.
 * 
 * @author Aaron Dunlop
 * @since Dec 19, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestCalculateDistances extends ToolTestCase {

    @Test
    public void testNoParametersArgument() throws Exception {
        String output = executeTool(new CalculateDistances(), "-m pqgram", "(Bracketed) (input)");
        assertTrue("Missing error output", output
            .contains("P and Q parameters are required for pqgram distance calculation"));
    }

    @Test
    public void testPqgramDistanceCalculator() throws Exception {
        String output = executeToolFromFile(new CalculateDistances(), "-m pqgram -p 2,3",
            "tools/calculate-distances-pqgram.input.gz");
        String expectedOutput = new String(SharedNlpTests
            .readUnitTestData("tools/calculate-distances-pqgram.output.gz"));
        assertEquals(expectedOutput, output);
    }

    @Test
    @PerformanceTest( { "d820", "26469" })
    public void profilePqgramDistanceCalculator() throws Exception {
        long startTime = System.currentTimeMillis();
        String output = executeToolFromFile(new CalculateDistances(), "-m pqgram -p 2,3",
            "tools/calculate-distances-pqgram-profile.input.gz");
        long totalTime = System.currentTimeMillis() - startTime;
        String expectedOutput = new String(SharedNlpTests
            .readUnitTestData("tools/calculate-distances-pqgram-profile.output.gz"));
        assertEquals(expectedOutput, output);
        System.out.format("Calculated %d x %d matrix in %d ms (%.2f entries / sec)\n", 1344, 1344, totalTime,
            (1344 * 1344 / 2f / totalTime * 1000));
    }

    @Test
    public void testLevenshteinDistanceCalculator() throws Exception {
        IntMatrix intMatrix = new IntMatrix(new int[][] { { 0 }, { 3, 0 }, { 1, 3, 0 } }, true);
        assertEquals(intMatrix.toString(), executeTool(new CalculateDistances(), "-m levenshtein",
            "dance\ndancing\ndances"));

        // A couple examples from Wikipedia
        intMatrix = new IntMatrix(new int[][] { { 0 }, { 3, 0 }, { 7, 6, 0 }, { 5, 6, 3, 0 } }, true);
        assertEquals(intMatrix.toString(), executeTool(new CalculateDistances(), "-m levenshtein",
            "kitten\nsitting\nsaturday\nsunday"));
    }
}
