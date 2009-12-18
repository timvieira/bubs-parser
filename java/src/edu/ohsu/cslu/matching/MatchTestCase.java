package edu.ohsu.cslu.matching;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import edu.ohsu.cslu.tests.SharedNlpTests;

public class MatchTestCase {

    protected void exactMatchTest(Matcher matcher) throws IOException {
        String text, pattern;

        text = "tpyxtpzxtpyxtpr";
        pattern = "tpr";
        assertEquals("Wrong number of matches found for 'tpr'", 1, matcher.matches(pattern, text));

        text = "cbababcababc";
        pattern = "abab";
        assertEquals("Wrong number of matches found for 'abab'", 2, matcher.matches(pattern, text));

        text = "aaaaa";
        pattern = "aaa";
        assertEquals("Wrong number of matches found in worst-case", 3, matcher.matches(pattern, text));

        text = new String(SharedNlpTests.readUnitTestData("matching/lingtext.2000"));
        assertEquals("Wrong number of matches for 'consis'", 5, matcher.matches("consis", text));
        assertEquals("Wrong number of matches for 'springing'", 1, matcher.matches("springing", text));
        assertEquals("Wrong number of matches for 'stocks'", 4, matcher.matches("stocks", text));
        assertEquals("Wrong number of matches for 'ident'", 98, matcher.matches("ident", text));
        assertEquals("Wrong number of matches for 'resident'", 86, matcher.matches("resident", text));
    }

    protected void multipatternMatchTest(Matcher matcher) {
        String text = "xluxtpxtdqwtdxtpxtsyxtpxtdy";
        String[] patterns = new String[] { "xtpxtd", "pxtsyx" };
        assertEquals("Wrong number of matches found in example", 3, matcher.matches(patterns, text));

        text = "xluxtpxtdqwtdxtpxtsyxtpxtdytpxtpxt";
        patterns = new String[] { "xtpxtd", "tpxt" };
        assertEquals("Wrong number of matches found in example", 7, matcher.matches(patterns, text));
    }

    protected void locationMatchTest(Matcher matcher) throws IOException {
        String text, pattern;

        text = "tpyxtpzxtpyxtpr";
        pattern = "tpr";
        Set<Integer> matchLocations = matcher.matchLocations(pattern, text);
        assertEquals("Wrong number of matches found for 'tpr'", 1, matchLocations.size());
        assertTrue("Expected location 15 for 'tpr'", matchLocations.contains(15));

        text = "tpyxtpzxtpyxtprpy";
        String[] patterns = new String[] { "tpyx", "py" };
        matchLocations = matcher.matchLocations(patterns, text);
        assertEquals("Wrong number of matches found for 'tpyx, py'", 5, matchLocations.size());
        assertTrue("Expected location 3 for 'tpyx,py'", matchLocations.contains(3));
        assertTrue("Expected location 4 for 'tpyx,py'", matchLocations.contains(4));
        assertTrue("Expected location 11 for 'tpyx,py'", matchLocations.contains(11));
        assertTrue("Expected location 12 for 'tpyx,py'", matchLocations.contains(12));
        assertTrue("Expected location 17 for 'tpyx,py'", matchLocations.contains(17));

        text = "cbababcababc";
        pattern = "abab";
        matchLocations = matcher.matchLocations(pattern, text);
        assertEquals("Wrong number of matches found for 'abab'", 2, matchLocations.size());
        assertTrue("Expected location 6 for '" + pattern + "'", matchLocations.contains(6));
        assertTrue("Expected location 11 for '" + pattern + "'", matchLocations.contains(11));

        text = "aaaaa";
        pattern = "aaa";
        matchLocations = matcher.matchLocations(pattern, text);
        assertEquals("Wrong number of matches found for 'aaa'", 3, matchLocations.size());
        assertTrue("Expected location 3 for '" + pattern + "'", matchLocations.contains(3));
        assertTrue("Expected location 4 for '" + pattern + "'", matchLocations.contains(4));
        assertTrue("Expected location 5 for '" + pattern + "'", matchLocations.contains(5));

        text = new String(SharedNlpTests.readUnitTestData("matching/lingtext.2000"));
        pattern = "consis";
        matchLocations = matcher.matchLocations(pattern, text);
        assertEquals("Wrong number of matches for '" + pattern + "'", 5, matchLocations.size());
        assertTrue("Expected location 116345 for '" + pattern + "'", matchLocations.contains(116345));
        assertTrue("Expected location 134552 for '" + pattern + "'", matchLocations.contains(134552));
        assertTrue("Expected location 134861 for '" + pattern + "'", matchLocations.contains(134861));
        assertTrue("Expected location 136995 for '" + pattern + "'", matchLocations.contains(136995));
        assertTrue("Expected location 236665 for '" + pattern + "'", matchLocations.contains(236665));

        pattern = "springing";
        matchLocations = matcher.matchLocations(pattern, text);
        assertEquals("Wrong number of matches for '" + pattern + "'", 1, matchLocations.size());
        assertTrue("Expected location 20778 for '" + pattern + "'", matchLocations.contains(20778));

        pattern = "stocks";
        matchLocations = matcher.matchLocations(pattern, text);
        assertEquals("Wrong number of matches for '" + pattern + "'", 4, matchLocations.size());
        assertTrue("Expected location 1843 for '" + pattern + "'", matchLocations.contains(1843));
        assertTrue("Expected location 6036 for '" + pattern + "'", matchLocations.contains(6036));
        assertTrue("Expected location 27366 for '" + pattern + "'", matchLocations.contains(27366));
        assertTrue("Expected location 52520 for '" + pattern + "'", matchLocations.contains(52520));

        pattern = "and chief executive officer";
        matchLocations = matcher.matchLocations(pattern, text);
        assertEquals("Wrong number of matches for '" + pattern + "'", 7, matchLocations.size());
        assertTrue("Expected location 489 for '" + pattern + "'", matchLocations.contains(489));
        assertTrue("Expected location 37574 for '" + pattern + "'", matchLocations.contains(37574));
        assertTrue("Expected location 51155 for '" + pattern + "'", matchLocations.contains(51155));
        assertTrue("Expected location 61489 for '" + pattern + "'", matchLocations.contains(61489));
        assertTrue("Expected location 140746 for '" + pattern + "'", matchLocations.contains(140746));
        assertTrue("Expected location 152276 for '" + pattern + "'", matchLocations.contains(152276));
        assertTrue("Expected location 238854 for '" + pattern + "'", matchLocations.contains(238854));
    }
}
