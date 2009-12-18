/**
 *
 */
package edu.ohsu.cslu.matching;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for all exact Matchers.
 * 
 * @author Aaron Dunlop
 * @since Oct 2, 2008
 * 
 *        $Id$
 */
public abstract class Matcher {

    protected final static int MAX_TO_STRING_LENGTH = 4096;

    protected Matcher() {
    }

    /**
     * Returns the locations of matches found in the text.
     * 
     * @param patterns
     *            The patterns to search for
     * @param text
     *            The text to search
     * @return The start locations of any matches found
     */
    public abstract IntSet matchLocations(Set<String> patterns, String text);

    /**
     * Returns the locations of matches found in the text.
     * 
     * @param pattern
     *            The pattern to search for
     * @param text
     *            The text to search
     * @return The start locations of any matches found
     */
    public IntSet matchLocations(String pattern, String text) {

        return matchLocations(new TreeSet<String>(Arrays.asList(pattern)), text);
    }

    /**
     * Returns the locations of matches found in the text.
     * 
     * @param patterns
     *            The patterns to search for
     * @param text
     *            The text to search
     * @return The start locations of matches found
     */
    public IntSet matchLocations(String[] patterns, String text) {
        return matchLocations(new TreeSet<String>(Arrays.asList(patterns)), text);
    }

    /**
     * Returns the number of matches found in the text.
     * 
     * @param patterns
     *            The patterns to search for
     * @param text
     *            The text to search
     * @return The number of matches found
     */
    public int matches(Set<String> patterns, String text) {
        return matchLocations(patterns, text).size();
    }

    /**
     * Returns the number of matches found in the text.
     * 
     * @param patterns
     *            The patterns to search for
     * @param text
     *            The text to search
     * @return The number of matches found
     */
    public int matches(String[] patterns, String text) {
        return matches(new TreeSet<String>(Arrays.asList(patterns)), text);
    }

    /**
     * Returns the number of matches found in the text.
     * 
     * @param pattern
     *            The pattern to search for
     * @param text
     *            The text to search
     * @return The number of matches found
     */
    public int matches(String pattern, String text) {
        return matches(new TreeSet<String>(Arrays.asList(pattern)), text);
    }
}