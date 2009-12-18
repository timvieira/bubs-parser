/**
 *
 */
package edu.ohsu.cslu.matching.exact;

import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Set;

import edu.ohsu.cslu.matching.Matcher;

public final class NaiveMatcher extends Matcher {

    @Override
    public AbstractIntSet matchLocations(Set<String> patterns, String text) {
        AbstractIntSet m_matchLocations = new IntOpenHashSet();

        for (final String pattern : patterns) {
            for (int location = text.indexOf(pattern); location >= 0; location = text.indexOf(pattern,
                location + 1)) {
                m_matchLocations.add(location + pattern.length());
            }
        }
        return m_matchLocations;
    }
}