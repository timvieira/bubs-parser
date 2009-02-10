/**
 * FullDynamicMatcher.java
 */
package edu.ohsu.cslu.matching.approximate;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.Set;

/**
 * Full dynamic-programming approximate match implementation.
 * 
 * @author Aaron Dunlop
 * @since Oct 2, 2008
 * 
 *        $Id$
 */
public class FullDynamicMatcher extends ApproximateMatcher
{
    public FullDynamicMatcher(int substitutionCost, int deleteCost)
    {
        super(substitutionCost, deleteCost);
    }

    @Override
    public Int2IntMap matchEditValues(Set<String> patterns, String text, int edits)
    {
        final Int2IntMap matchEditValues = new Int2IntOpenHashMap();
        final int jSize = text.length() + 1;
        final char[] textChars = text.toCharArray();

        int maxISize = 0;
        for (String pattern : patterns)
        {
            if (pattern.length() >= maxISize)
            {
                maxISize = pattern.length() + 1;
            }
        }

        final int[][] editArray = new int[maxISize][jSize];

        for (final String pattern : patterns)
        {
            // Skip patterns shorter than the specified edit distance
            if (pattern.length() <= edits)
            {
                continue;
            }

            final char[] patternChars = pattern.toCharArray();
            final int iSize = patternChars.length + 1;

            // Note: Java arrays are initialized to 0, so
            // m_edits[i][0] == 0 for all i and
            // m_edits[0][j] == 0 for all j
            for (int i = 1; i < iSize; i++)
            {
                editArray[i][0] = editArray[i - 1][0] + deleteCost;
            }

            for (int i = 1; i < iSize; i++)
            {
                for (int j = 1; j < jSize; j++)
                {
                    // Find edit distance at i, j (given edit distances at [i-1, j] , [i, j-1], and
                    // [i-1, j-1]
                    final int f1 = editArray[i][j - 1] + deleteCost;
                    final int f2 = editArray[i - 1][j] + deleteCost;
                    final int m = (patternChars[i - 1] == textChars[j - 1] ? 0 : substitutionCost);
                    final int f3 = editArray[i - 1][j - 1] + m;

                    editArray[i][j] = Math.min(f1, Math.min(f2, f3));
                }
            }

            final int maxDelta = edits * deleteCost;
            final int maxI = iSize - 1;

            for (int j = pattern.length() - edits; j < jSize; j++)
            {
                int currentEdits = editArray[maxI][j];
                if (currentEdits <= maxDelta)
                {
                    // Try to eliminate spurious duplicate matches
                    if ((j <= 0 || currentEdits <= editArray[maxI][j - 1])
                        && (j >= (jSize - 1) || currentEdits <= editArray[maxI][j + 1]))
                    {
                        matchEditValues.put(j, currentEdits);
                    }
                }
            }
        }
        return matchEditValues;
    }
}