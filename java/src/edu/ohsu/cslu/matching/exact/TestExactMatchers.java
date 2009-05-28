/**
 * TestExactMatch.java
 */
package edu.ohsu.cslu.matching.exact;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import edu.ohsu.cslu.matching.MatchTestCase;
import edu.ohsu.cslu.matching.Matcher;
import edu.ohsu.cslu.matching.exact.AhoCorasickMatcher.Match;

/**
 * Tests Naive, Boyer-Moore, Knuth-Morris-Pratt, and Aho-Corasick matchers.
 * 
 * @author Aaron Dunlop
 * @since Jun 12, 2008
 * 
 *        $Id$
 */
public class TestExactMatchers extends MatchTestCase
{
    @Test
    public void testNaiveMatcher() throws IOException
    {
        Matcher matcher = new NaiveMatcher();
        exactMatchTest(matcher);
        multipatternMatchTest(matcher);
        locationMatchTest(matcher);
    }

    @Test
    public void testBoyerMooreMatcher() throws IOException
    {
        Matcher matcher = new BoyerMooreMatcher();
        exactMatchTest(matcher);
        multipatternMatchTest(matcher);
        locationMatchTest(matcher);
    }

    @Test
    public void testKnuthMorrisPrattMatcher() throws IOException
    {
        Matcher matcher = new KnuthMorrisPrattMatcher();
        exactMatchTest(matcher);
        multipatternMatchTest(matcher);
        locationMatchTest(matcher);
    }

    @Test
    public void testAhoCorasickMatcher() throws IOException
    {
        AhoCorasickMatcher matcher = new AhoCorasickMatcher();
        exactMatchTest(matcher);
        multipatternMatchTest(matcher);
        locationMatchTest(matcher);

        String text = "tpyxtpzxtpyxtprpy";
        TreeSet<String> patterns = new TreeSet<String>();
        patterns.add("py");
        patterns.add("tpyx");
        patterns.add("yx");

        Set<Match> matchSet = matcher.patternMatchLocations(patterns, text);
        assertEquals("Wrong number of matches found for 'tpyx, py'", 7, matchSet.size());
        assertTrue("Expected location 3 for 'py'", matchSet.contains(new Match(0, 3)));
        assertTrue("Expected location 4 for 'yx'", matchSet.contains(new Match(2, 4)));
        assertTrue("Expected location 4 for 'tpyx'", matchSet.contains(new Match(1, 4)));
        assertTrue("Expected location 11 for 'py'", matchSet.contains(new Match(0, 11)));
        assertTrue("Expected location 12 for 'yx'", matchSet.contains(new Match(2, 12)));
        assertTrue("Expected location 12 for 'tpyx'", matchSet.contains(new Match(1, 12)));
        assertTrue("Expected location 17 for 'py'", matchSet.contains(new Match(0, 17)));
    }
}
