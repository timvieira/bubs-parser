/**
 * BoyerMooreMatcher.java
 */
package edu.ohsu.cslu.matching.exact;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Set;

import edu.ohsu.cslu.matching.Matcher;


/**
 * Implements the Boyer-Moore exact match algorithm. It's a very impressive algorithm, but limited
 * profiling indicates that in some cases this implementation can actually run slower than simple
 * String.indexOf(). Possibly useful, but it's always hard to predict cache behavior.
 * 
 * Adapted from Seeger's work, which I believe was adapted from Brian's C version.
 * 
 * @author Seeger Fisher
 * @author Aaron Dunlop
 * 
 * @since Jun 17, 2008
 * 
 *        $Id$
 */
public final class BoyerMooreMatcher extends Matcher
{
    @Override
    public IntSet matchLocations(Set<String> patterns, String text)
    {
        IntSet matchLocations = new IntOpenHashSet();

        int h, i, k;
        final int textLength = text.length();
        final char[] textChars = text.toCharArray();

        for (final String pattern : patterns)
        {
            final PreProcess preProcess = new PreProcess(pattern);

            // Local copies for somewhat faster access
            final int lval2 = preProcess.lval[2];
            final int n = pattern.length();

            k = n - 1;
            int matchedStart = -1, matchedEnd = -1;

            while (k < textLength)
            {
                i = n - 1;
                h = k;

                while (i >= 0 && preProcess.patternChars[i] == textChars[h])
                {
                    i--;
                    h--;

                    // Skip over matched sections
                    if (i <= matchedEnd && i > matchedStart && i > 0)
                    {
                        i = matchedStart;
                        h = h - matchedEnd + matchedStart;
                    }
                }

                if (i < 0)
                {
                    matchLocations.add(h + n + 1);
                    k = k + n - lval2 - 1;
                    // Mark matched section so we can skip it
                    matchedStart = -1;
                    matchedEnd = preProcess.lval[i + 1] - 1;
                }
                else
                {
                    char c = textChars[h];
                    int badCharacter = Math.max(1, i - (c >= preProcess.badc.length ? 0 : preProcess.badc[c]));
                    // No matched section in this case
                    matchedStart = matchedEnd = -1;

                    int goodSuffix = 1;
                    if (i != (n - 1))
                    {
                        int li = preProcess.L[i + 1];
                        if (li > 0)
                        {
                            goodSuffix = n - li - 1;

                            // Mark matched section so we can skip it
                            matchedStart = i - goodSuffix;
                            matchedEnd = n - i - 1;
                        }
                        else
                        {
                            int lv = preProcess.lval[i + 1];
                            goodSuffix = n - lv;

                            // Mark matched section so we can skip it
                            matchedStart = -1;
                            matchedEnd = lv - 1;
                        }
                    }
                    k = k + Math.max(goodSuffix, badCharacter);
                }
            }
        }

        return matchLocations;
    }

    private class PreProcess
    {
        private final String pattern;
        private final char[] patternChars;

        /** Gusfield Z values */
        private final int[] Z;

        /** weak bad character values for Boyer Moore */
        private int[] badc;

        /** strong L values for Boyer Moore */
        private final int[] L;

        /** l values for Boyer Moore */
        private final int[] lval;

        public PreProcess(String pattern)
        {
            super();
            this.pattern = pattern;
            this.patternChars = pattern.toCharArray();
            int len = pattern.length() + 1;
            this.Z = new int[len];
            this.L = new int[len];
            this.lval = new int[len];

            get_badchar(len); /* get Boyer Moore bad character values */
            get_goodsuffix(len); /* get Boyer Moore good suffix values */
        }

        private int matchlen(String s, int pos1, int pos2)
        {
            int matchlen = 0;
            int length = s.length();
            while (pos1 < length && pos2 < length && s.charAt(pos1++) == s.charAt(pos2++))
                matchlen++;
            return matchlen;
        }

        private void get_ZandFF(String s, int len, int FF)
        {
            int r = 0, l = 0, updaterl, patLen = len - 1;
            final int[] sp = new int[len];

            for (int i = 1; i < patLen; i++)
            {
                updaterl = 0;

                if (i > r)
                {
                    Z[i] = matchlen(s, i, 0);
                    if (Z[i] > 0)
                        updaterl = 1;
                }
                else
                {
                    if (Z[i - l] < r - i + 1)
                    {
                        Z[i] = Z[i - l];
                    }
                    else
                    {
                        Z[i] = r - i + 1;
                        Z[i] += matchlen(s, r + 1, r - i + 2);
                        updaterl = 1;
                    }
                }
                if (updaterl != 0)
                {
                    r = i + Z[i] - 1;
                    l = i;
                    if (FF != 0 && sp[r] == 0)
                    {
                        sp[r] = Z[i];
                    }
                }
            }
        }

        private void get_goodsuffix(int len)
        {
            int patLen = len - 1, last = 0;

            String reversePattern = new StringBuilder(pattern).reverse().toString();
            get_ZandFF(reversePattern, len, 0); /* get Z values but no F */

            for (int i = 1; i < patLen - 1; i++)
            {
                /* set good suffix values L from Z */
                L[patLen - Z[patLen - i - 1]] = i;
            }

            for (int i = patLen - 1; i >= 0; i--)
            {
                /* set good suffix values l from Z */
                int k = patLen - i - 1;
                if (Z[patLen - k - 1] == k + 1)
                {
                    lval[i] = k + 1;
                    last = k + 1;
                }
                else
                {
                    lval[i] = last;
                }
            }
        }

        // TODO: Limit badc array size to min - max range.
        private void get_badchar(int len)
        {
            int patLen = len - 1;
            int maxc = -1;
            for (int i = 0; i < patLen; i++)
            {
                if (patternChars[i] > maxc)
                {
                    maxc = patternChars[i];
                }
            }
            badc = new int[maxc + 1];

            for (int i = 0; i < patLen; i++)
            {
                int j = patternChars[i];
                badc[j] = i;
            }
        }
    }
}