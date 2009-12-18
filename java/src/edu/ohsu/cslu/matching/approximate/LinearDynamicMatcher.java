/**
 * LinearDynamicMatcher.java
 */
package edu.ohsu.cslu.matching.approximate;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.Arrays;
import java.util.Set;

/**
 * Linear-space dynamic-programming approximate match implementation.
 * 
 * @author Aaron Dunlop
 * @since Oct 2, 2008
 * 
 *        $Id$
 */
public class LinearDynamicMatcher extends ApproximateMatcher {

    public LinearDynamicMatcher(int substitutionCost, int deleteCost) {
        super(substitutionCost, deleteCost);
    }

    @Override
    public Int2IntMap matchEditValues(Set<String> patterns, String text, int edits) {
        final Int2IntMap matchEditValues = new Int2IntOpenHashMap();

        final char[] textChars = text.toCharArray();
        final int jSize = textChars.length + 1;

        int current[] = new int[jSize];
        int previous[] = new int[jSize];
        final int localDeleteCost = deleteCost;
        final int localSubstitutionCost = substitutionCost;

        for (final String pattern : patterns) {
            // Skip patterns shorter than the specified edit distance
            if (pattern.length() <= edits) {
                continue;
            }

            final char[] patternChars = pattern.toCharArray();
            Arrays.fill(previous, 0, previous.length - 1, 0);

            for (int i = 0; i < patternChars.length; i++) {
                current[0] = previous[0] + localDeleteCost;
                final char patternChar = patternChars[i];

                for (int j = 1; j < jSize; j++) {
                    final int prevJ = j - 1;
                    final int f1 = current[prevJ] + localDeleteCost;
                    final int f2 = previous[j] + localDeleteCost;
                    final int f3 = (patternChar != textChars[prevJ]) ? previous[prevJ]
                            + localSubstitutionCost : previous[prevJ];

                    current[j] = Math.min(f1, Math.min(f2, f3));
                }
                final int[] tmp = previous;
                previous = current;
                current = tmp;
            }

            final int maxDelta = edits * localDeleteCost;

            for (int j = pattern.length() - edits; j < jSize; j++) {
                final int lastEdits = previous[j];
                if (lastEdits <= maxDelta) {
                    // Try to eliminate spurious duplicate matches
                    if ((j <= 0 || lastEdits <= previous[j - 1])
                            && (j >= (jSize - 1) || lastEdits <= previous[j + 1])) {
                        matchEditValues.put(j, lastEdits);
                    }
                }
            }
        }
        return matchEditValues;
    }
}