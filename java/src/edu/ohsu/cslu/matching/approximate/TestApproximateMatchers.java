package edu.ohsu.cslu.matching.approximate;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.matching.MatchTestCase;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.SharedNlpTests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(FilteredRunner.class)
public class TestApproximateMatchers extends MatchTestCase
{
    @Test
    public void testFullDynamicMatcher() throws IOException
    {
        ApproximateMatcher matcher = new FullDynamicMatcher(99, 100);
        exactMatchTest(matcher);
        multipatternMatchTest(matcher);
        locationMatchTest(matcher);
        approximateMatchTest(matcher);
    }

    @Test
    public void testLinearDynamicMatcher() throws IOException
    {
        ApproximateMatcher matcher = new LinearDynamicMatcher(99, 100);
        exactMatchTest(matcher);
        multipatternMatchTest(matcher);
        locationMatchTest(matcher);
        approximateMatchTest(matcher);
    }

    @Test
    public void testBaezaYatesPerlbergMatcher() throws IOException
    {
        ApproximateMatcher matcher = new BaezaYatesPerlbergMatcher(99, 100);
        exactMatchTest(matcher);
        multipatternMatchTest(matcher);
        locationMatchTest(matcher);
        approximateMatchTest(matcher);
    }

    private void approximateMatchTest(ApproximateMatcher matcher) throws IOException
    {
        String text, pattern;
        IntSet matchLocations;

        text = "tpyxtpzxtpyxtpr";
        pattern = "tpr";
        Int2IntMap matchEditDistances = matcher.matchEditValues(pattern, text, 1);
        matchLocations = matchEditDistances.keySet();
        assertEquals("Wrong number of matches found for 'tpr'", 4, matchLocations.size());
        assertLocationsIn(pattern, matchLocations, new int[] {3, 7, 11, 15});
        assertEquals(99, matchEditDistances.get(3));
        assertEquals(99, matchEditDistances.get(7));
        assertEquals(99, matchEditDistances.get(11));
        assertEquals(0, matchEditDistances.get(15));

        text = "cbababcabacc";
        pattern = "abab";
        assertEquals("Wrong number of matches found for 'abab'", 1, matcher.matches(pattern, text, 0));
        matchLocations = matcher.matchLocations(pattern, text, 1);
        assertEquals("Wrong number of matches found for 'abab'", 4, matchLocations.size());
        assertLocationsIn(pattern, matchLocations, new int[] {4, 6, 9, 11});

        // Fuller test with natural language data
        text = new String(SharedNlpTests.readUnitTestData("matching/lingtext.2000"));
        pattern = "and chief executive officer";
        matchLocations = matcher.matchLocations(pattern, text, 1);
        assertEquals("Wrong number of matches for '" + pattern + "'", 7, matchLocations.size());
        assertLocationsIn(pattern, matchLocations, new int[] {489, 37574, 51155, 61489, 140746, 152276, 238854});

        matchLocations = matcher.matchLocations(pattern, text, 3);
        assertEquals("Wrong number of matches for '" + pattern + "'", 11, matchLocations.size());
        assertLocationsIn(pattern, matchLocations, new int[] {489, 37574, 50153, 51155, 61489, 113381, 140746, 152276,
                                                              163945, 238854, 245019});

        text = "In an Oct. 19 review of The Misanthrope at Chicago 's Goodman Theatre -LRB- Revitalized Classics Take the Stage in Windy City , Leisure & Arts -RRB- , the role of Celimene , played by Kim Cattrall , was mistakenly attributed to Christina Haag .";
        pattern = "ident";
        matchLocations = matcher.matchLocations(pattern, text, 3);
        assertEquals("Wrong number of matches for '" + pattern + "'", 16, matchLocations.size());

        // And with biological data
        text = new String(SharedNlpTests.readUnitTestData("matching/wgs_caam_env.seq.70000"));
        pattern = "ccactgctcgtaagggtgacgcgaggccagatcggccttgaggtc";
        matchLocations = matcher.matchLocations(pattern, text, 1);
        assertEquals("Wrong number of matches for '" + pattern + "'", 2, matchLocations.size());
        assertLocationsIn(pattern, matchLocations, new int[] {471, 65034});

        matchLocations = matcher.matchLocations(pattern, text, 5);
        assertEquals("Wrong number of matches for '" + pattern + "'", 2, matchLocations.size());
        assertLocationsIn(pattern, matchLocations, new int[] {471, 65034});
    }

    public void assertLocationsIn(String pattern, Set<Integer> matchLocations, int[] locations)
    {
        for (int location : locations)
        {
            assertTrue("Expected location " + location + " for '" + pattern + "'", matchLocations.contains(location));
        }
    }
}
