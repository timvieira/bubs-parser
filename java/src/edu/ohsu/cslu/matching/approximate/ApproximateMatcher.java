/**
 * ApproximateMatcher.java
 */
package edu.ohsu.cslu.matching.approximate;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import edu.ohsu.cslu.matching.Matcher;


/**
 * Base class for all approximate matchers
 *
 * @author Aaron Dunlop
 * @since Jun 12, 2008
 *
 * $Id$
 */
public abstract class ApproximateMatcher extends Matcher
{
    protected final int deleteCost, substitutionCost;

    protected ApproximateMatcher(int substitutionCost, int deleteCost)
    {
        this.substitutionCost = substitutionCost;
        this.deleteCost = deleteCost;
    }

    /**
     * Returns the locations of matches found in the text and the edit distances for each match.
     *
     * @param patterns The patterns to search for
     * @param text The text to search
     * @param edits The number of edits to allow in matches
     * @return The ending locations of any matches found, along with edit distances for those
     *         matches.
     */
    public abstract Int2IntMap matchEditValues(Set<String> patterns, String text, int edits);

    /**
     * Returns the locations of matches found in the text.
     *
     * @param patterns The patterns to search for
     * @param text The text to search
     * @param edits The number of edits to allow in matches
     * @return The ending locations of any matches found
     */
    public IntSet matchLocations(Set<String> patterns, String text, int edits)
    {
        return matchEditValues(patterns, text, edits).keySet();
    }

    /**
     * Returns the number of matches found in the text.
     *
     * @param pattern The pattern to search for
     * @param text The text to search
     * @param edits The number of edits to allow in matches
     * @return The number of matches found
     */
    public int matches(String pattern, String text, int edits)
    {
        return matchLocations(new TreeSet<String>(Arrays.asList(pattern)), text, edits).size();
    }

    /**
     * Returns the locations of matches found in the text.
     *
     * @param pattern The pattern to search for
     * @param text The text to search
     * @param edits The number of edits to allow in matches
     * @return The start locations of any matches found
     */
    public IntSet matchLocations(String pattern, String text, int edits)
    {
        return matchLocations(new TreeSet<String>(Arrays.asList(pattern)), text, edits);
    }

    /**
     * Returns the locations of matches found in the text.
     *
     * @param patterns The patterns to search for
     * @param text The text to search
     * @param edits The number of edits to allow in matches
     * @return The start locations of matches found
     */
    public IntSet matchLocations(String[] patterns, String text, int edits)
    {
        return matchLocations(new TreeSet<String>(Arrays.asList(patterns)), text, edits);
    }

    /**
     * Returns the number of matches found in the text.
     *
     * @param patterns The patterns to search for
     * @param text The text to search
     * @param edits The number of edits to allow in matches
     * @return The number of matches found
     */
    public int matches(Set<String> patterns, String text, int edits)
    {
        return matchLocations(patterns, text, edits).size();
    }

    /**
     * Returns the number of matches found in the text.
     *
     * @param patterns The patterns to search for
     * @param text The text to search
     * @param edits The number of edits to allow in matches
     * @return The number of matches found
     */
    public int matches(String[] patterns, String text, int edits)
    {
        return matches(new TreeSet<String>(Arrays.asList(patterns)), text, edits);
    }

    /**
     * Returns the locations of matches found in the text and the edit distances for each match.
     *
     * @param patterns The patterns to search for
     * @param text The text to search
     * @param edits The number of edits to allow in matches
     * @return The ending locations of any matches found, along with edit distances for those
     *         matches.
     */
    public Int2IntMap matchEditValues(String pattern, String text, int edits)
    {
        return matchEditValues(new TreeSet<String>(Arrays.asList(pattern)), text, edits);
    }

    @Override
    public IntSet matchLocations(Set<String> patterns, String text)
    {
        return matchLocations(patterns, text, 0);
    }
}
