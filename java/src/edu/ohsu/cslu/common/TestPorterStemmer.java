package edu.ohsu.cslu.common;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(FilteredRunner.class)
public class TestPorterStemmer
{
    @Test
    public void testStem()
    {
        PorterStemmer stemmer = new PorterStemmer();

        assertEquals("like", stemmer.stemWord("liking"));

        // Test something the stemmer won't be able to stem
        assertEquals("aboveboard", stemmer.stemWord("aboveboard"));

        // And verify that sentence stemming produces the same thing that word stemming does
        assertEquals("redefin", stemmer.stemWord("redefine"));
        assertEquals("redefin like", stemmer.stemSentence("redefine liking"));
    }
}
