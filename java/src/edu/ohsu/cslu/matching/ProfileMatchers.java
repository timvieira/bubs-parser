package edu.ohsu.cslu.matching;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.matching.approximate.ApproximateMatcher;
import edu.ohsu.cslu.matching.approximate.BaezaYatesPerlbergMatcher;
import edu.ohsu.cslu.matching.approximate.FullDynamicMatcher;
import edu.ohsu.cslu.matching.approximate.LinearDynamicMatcher;
import edu.ohsu.cslu.matching.exact.AhoCorasickMatcher;
import edu.ohsu.cslu.matching.exact.BoyerMooreMatcher;
import edu.ohsu.cslu.matching.exact.KnuthMorrisPrattMatcher;
import edu.ohsu.cslu.matching.exact.NaiveMatcher;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.SharedNlpTests;
import edu.ohsu.cslu.tests.PerformanceTest;


/**
 * Performance tests for matchers.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@PerformanceTest
@RunWith(FilteredRunner.class)
public class ProfileMatchers
{
    public static String lingtext_full;
    public static String lingtext_2000;
    private final static Set<String> searchSet = new HashSet<String>(Arrays
        .asList(new String[] {"consis", "springing", "stocks", "strongest", "ident", "resident",
                              "the Dow Jones Industrial Average", "it", "he said ."}));

    @BeforeClass
    public static void suiteSetUp() throws FileNotFoundException, IOException
    {
        lingtext_full = new String(SharedNlpTests.readUnitTestData("matching/lingtext"));
        lingtext_2000 = new String(SharedNlpTests.readUnitTestData("matching/lingtext.2000"));
    }

    @Test
    @PerformanceTest
    public void profileNaiveMatcher()
    {
        profileExactMatcher(new NaiveMatcher(), "Naive (indexOf)", lingtext_full);
    }

    @Test
    @PerformanceTest
    public void profileBoyerMooreMatcher()
    {
        profileExactMatcher(new BoyerMooreMatcher(), "Boyer-Moore", lingtext_full);
    }

    @Test
    @PerformanceTest
    public void profileKnuthMorrisPrattMatcher()
    {
        profileExactMatcher(new KnuthMorrisPrattMatcher(), "Knuth-Morris-Pratt", lingtext_full);
    }

    @Test
    @PerformanceTest
    public void profileAhoCorasickMatcher()
    {
        profileExactMatcher(new AhoCorasickMatcher(), "Aho-Corasick", lingtext_full);
    }

    @Test
    @PerformanceTest
    public void profileFullDynamicMatcher()
    {
        profileExactMatcher(new FullDynamicMatcher(99, 100), "Full Dynamic Matcher (exact)", lingtext_2000);
        profileApproximateMatch(new FullDynamicMatcher(99, 100), "Full Dynamic Matcher");
    }

    @Test
    @PerformanceTest
    public void profileLinearDynamicMatcher()
    {
        profileExactMatcher(new LinearDynamicMatcher(99, 100), "Linear Dynamic Matcher (exact)", lingtext_2000);
        profileApproximateMatch(new LinearDynamicMatcher(99, 100), "Linear Dynamic Matcher");
    }

    @Test
    @PerformanceTest
    public void profileBaezaYatesPerlbergMatcher()
    {
        profileExactMatcher(new BaezaYatesPerlbergMatcher(99, 100), "Baeza-Yates-Perlberg Matcher (exact)",
            lingtext_2000);
        profileApproximateMatch(new BaezaYatesPerlbergMatcher(99, 100), "Baeza-Yates-Perlberg Matcher");
    }

    private void profileExactMatcher(Matcher matcher, String name, String text)
    {
        // Warmup
        for (int i = 0; i < 5; i++)
        {
            matcher.matches(searchSet, text);
        }

        // Test Run
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 25; i++)
        {
            matcher.matches(searchSet, text);
        }
        System.out.println(name + " time: " + (System.currentTimeMillis() - startTime));
    }

    public void profileApproximateMatch(ApproximateMatcher matcher, String name)
    {
        // Warmup
        for (int i = 0; i < 5; i++)
        {
            matcher.matches(searchSet, lingtext_2000, 3);
        }

        // Test Run
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 25; i++)
        {
            matcher.matches(searchSet, lingtext_2000, 3);
        }
        System.out.println(name + " time: " + (System.currentTimeMillis() - startTime));
    }
}
