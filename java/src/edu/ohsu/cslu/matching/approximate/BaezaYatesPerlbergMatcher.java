/**
 * BaezaYatesPerlbergMatcher.java
 */
package edu.ohsu.cslu.matching.approximate;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import edu.ohsu.cslu.matching.exact.AhoCorasickMatcher;
import edu.ohsu.cslu.matching.exact.AhoCorasickMatcher.Match;

/**
 * Implements the Baeza-Yates-Perlberg approximate match algorithm. This algorithm is very efficient given a
 * small number of edits in relation to the pattern length. For short patterns with many edits allowed, the
 * straight DP approaches may sometimes be more efficient.
 * 
 * @author Aaron Dunlop
 * @since Jun 12, 2008
 * 
 *        $Id$
 */
public class BaezaYatesPerlbergMatcher extends ApproximateMatcher {

    public BaezaYatesPerlbergMatcher(int substitutionCost, int deleteCost) {
        super(substitutionCost, deleteCost);
    }

    @Override
    public Int2IntMap matchEditValues(Set<String> patterns, String text, int edits) {
        Int2IntMap matchEditValues = new Int2IntAVLTreeMap();
        final AhoCorasickMatcher ahoCorasickMatcher = new AhoCorasickMatcher();
        final LinearDynamicMatcher dynamicMatcher = new LinearDynamicMatcher(substitutionCost, deleteCost);

        final int textLength = text.length();

        for (final String pattern : patterns) {
            // Skip patterns shorter than the specified edit distance
            if (pattern.length() <= edits) {
                continue;
            }

            // Partition pattern into edits + 1 substrings
            ArrayList<String> subPatterns = new ArrayList<String>(edits + 1);

            final int patternLength = pattern.length();
            final int subPatternLength = Math.round((float) patternLength / (edits + 1));
            int begin = 0, end = 0;
            for (int i = 0; i < edits; i++) {
                end = Math.min(begin + subPatternLength, patternLength);
                subPatterns.add(pattern.substring(begin, end));
                begin = end;
            }
            subPatterns.add(pattern.substring(begin));

            final Set<Match> ahoCorasickMatchLocations = ahoCorasickMatcher.patternMatchLocations(
                subPatterns, text);

            for (Match matchLocation : ahoCorasickMatchLocations) {
                final int t = matchLocation.getLocation();
                final int index = matchLocation.getPatternIndex();

                // Establish the earliest possible match beginning point
                final int b = Math.max(t - edits // k
                        - (index + 1) * subPatternLength, // Sum from 1 to i of pattern lengths
                    0);

                // And the latest possible ending point
                final int e = Math.min(t + edits // k
                        + ((edits - index) * subPatternLength) + 1 // Sum from i+1 to k+1 of lengths
                , textLength);

                // Do dynamic programming to find matches
                final Int2IntMap approximateMatchEditValues = dynamicMatcher.matchEditValues(pattern, text
                    .substring(b, e), edits);
                for (int key : approximateMatchEditValues.keySet()) {
                    matchEditValues.put(key + b, approximateMatchEditValues.get(key));
                }
            }
        }

        // TODO: This could be made more efficient...
        // Pull out any spurious matches found by the dynamic programming, since we may have
        // found exact matches very near one another and run the DP algorithm multiple times
        // over the same subsequence
        for (Iterator<Integer> i = matchEditValues.keySet().iterator(); i.hasNext();) {
            int matchLocation = i.next().intValue();
            int editValue = matchEditValues.get(matchLocation);
            if ((matchEditValues.containsKey(matchLocation - 1) && matchEditValues.get(matchLocation - 1) < editValue)
                    || (matchEditValues.containsKey(matchLocation + 1) && matchEditValues
                        .get(matchLocation + 1) < editValue)) {
                i.remove();
            }
        }
        return matchEditValues;
    }
}