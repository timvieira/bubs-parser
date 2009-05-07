package edu.ohsu.cslu.tools;

import org.apache.commons.cli.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.common.tools.ToolTestCase;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests for the command-line distance calculation tool.
 * 
 * @author Aaron Dunlop
 * @since Dec 19, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestCalculateDistances extends ToolTestCase
{
    @Test(expected = ParseException.class)
    public void testNoParametersArgument() throws Exception
    {
        executeTool("-m pqgram", "(Bracketed) (input)");
    }

    @Test
    public void testPqgramDistanceCalculator() throws Exception
    {
        String output = executeToolFromFile("-m pqgram -p 2,3", "tools/calculate-distances-pqgram.input.gz");
        String expectedOutput = new String(SharedNlpTests
            .readUnitTestData("tools/calculate-distances-pqgram.output.gz"));
        assertEquals(expectedOutput, output);
    }

    @Test
    @PerformanceTest( {"d820", "26469"})
    public void profilePqgramDistanceCalculator() throws Exception
    {
        long startTime = System.currentTimeMillis();
        String output = executeToolFromFile("-m pqgram -p 2,3", "tools/calculate-distances-pqgram-profile.input.gz");
        long totalTime = System.currentTimeMillis() - startTime;
        String expectedOutput = new String(SharedNlpTests
            .readUnitTestData("tools/calculate-distances-pqgram-profile.output.gz"));
        assertEquals(expectedOutput, output);
        System.out.format("Calculated %d x %d matrix in %d ms (%.2f entries / sec)\n", 1344, 1344, totalTime,
            (1344 * 1344 / 2f / totalTime * 1000));
    }

    @Test
    public void testLevenshteinDistanceCalculator() throws Exception
    {
        IntMatrix intMatrix = new IntMatrix(new int[][] { {0}, {3, 0}, {1, 3, 0}}, true);
        assertEquals(intMatrix.toString(), executeTool("-m levenshtein", "dance\ndancing\ndances"));

        // A couple examples from Wikipedia
        intMatrix = new IntMatrix(new int[][] { {0}, {3, 0}, {7, 6, 0}, {5, 6, 3, 0}}, true);
        assertEquals(intMatrix.toString(), executeTool("-m levenshtein", "kitten\nsitting\nsaturday\nsunday"));
    }

    @Override
    protected BaseCommandlineTool tool()
    {
        return new CalculateDistances();
    }
}
