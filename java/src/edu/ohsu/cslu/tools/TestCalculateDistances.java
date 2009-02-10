package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.math.linear.Matrix;
import edu.ohsu.cslu.parsing.trees.BaseNaryTree;
import edu.ohsu.cslu.parsing.trees.ParseTree;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.SharedNlpTests;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tools.CalculateDistances.PqgramDistanceCalculator;


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
public class TestCalculateDistances
{
    private final int p = 2;
    private final int q = 3;
    private final ArrayList<String> stringSentences = new ArrayList<String>(1500);
    SimpleVocabulary vocabulary;

    @Before
    public void setUp() throws IOException
    {
        StringBuilder sb = new StringBuilder(512000);
        BufferedReader r = new BufferedReader(new InputStreamReader(SharedNlpTests
            .unitTestDataAsStream("parsing/f24.topos.txt.gz")));
        for (String line = r.readLine(); line != null; line = r.readLine())
        {
            sb.append(line);
            sb.append('\n');
        }

        String f24 = sb.toString();
        vocabulary = SimpleVocabulary.induce(f24);

        List<ParseTree> trees = new ArrayList<ParseTree>(1500);
        r = new BufferedReader(new StringReader(f24));
        for (String line = r.readLine(); line != null; line = r.readLine())
        {
            ParseTree tree = ParseTree.read(line, vocabulary);
            trees.add(tree);
            stringSentences.add(line);
        }

        // Pre-calculate all pq-gram profiles to save time during distance calculations
        BaseNaryTree.PqgramProfile[] profiles = new BaseNaryTree.PqgramProfile[trees.size()];
        for (int i = 0; i < trees.size(); i++)
        {
            profiles[i] = trees.get(i).pqgramProfile(p, q);
        }
    }

    @Test
    public void testPqgramDistanceCalculator()
    {
        PqgramDistanceCalculator calculator = new PqgramDistanceCalculator(p, q, vocabulary);
        for (int i = 0; i < 50; i++)
        {
            calculator.addElement(stringSentences.get(i));
        }

        Matrix matrix = calculator.distance();
        assertEquals(1776.1742f, matrix.sum(), .01f);
    }

    @Test
    @PerformanceTest( {"dell", "14688"})
    public void profilePqgramDistanceCalculator()
    {
        long startTime = System.currentTimeMillis();
        PqgramDistanceCalculator calculator = new PqgramDistanceCalculator(p, q, vocabulary);
        for (String sentence : stringSentences)
        {
            calculator.addElement(sentence);
        }

        Matrix matrix = calculator.distance();
        assertEquals(1277073.8f, matrix.sum(), .01f);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.format("Calculated %d x %d matrix in %d ms (%.2f entries / sec)\n", matrix.rows(), matrix.columns(),
            totalTime, (matrix.rows() * matrix.columns() / 2f / totalTime * 1000));
    }
}
