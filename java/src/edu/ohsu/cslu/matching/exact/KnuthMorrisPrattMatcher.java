/**
 * KnuthMorrisPrattMatcher.java
 */
package edu.ohsu.cslu.matching.exact;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Set;

import edu.ohsu.cslu.matching.Matcher;


/**
 * Implements the Knuth-Morris-Pratt exact match algorithm. Included for completeness, but
 * Boyer-Moore ({@link BoyerMooreMatcher}) appears to be consistently faster. The Boyer-Moore
 * version has been tuned and profiled more carefully, so there might be more speed to gain here,
 * but it's unlikely we'd eclipse Boyer-Moore, which is nearly triple the speed of this
 * Knuth-Morris-Pratt implementation.
 *
 * Adapted from Seeger's work, which I believe was adapted from Brian's C version.
 *
 * @author Seeger Fisher
 * @author Aaron Dunlop
 *
 * @since Jun 17, 2008
 *
 * $Id$
 */
public final class KnuthMorrisPrattMatcher extends Matcher
{
    @Override
    public IntSet matchLocations(Set<String> patterns, String text)
    {
        IntSet matchLocations = new IntOpenHashSet();

        for (final String pattern : patterns)
        {
            PreProcess preProcess = new PreProcess(pattern);

            int c = 0;
            int p = 0;
            int n = pattern.length();
            int m = text.length();

            while ((c + n - p) <= m)
            {
                while (p < n && pattern.charAt(p) == text.charAt(c))
                {
                    p++;
                    c++;
                }

                if (p == n)
                {
                    matchLocations.add(c);
                }

                if (p == 0)
                {
                    c++;
                }

                p = preProcess.F[p];
            }
        }
        return matchLocations;
    }

    private class PreProcess
    {
        private final String pattern;

        /** length of vectors */
        private final int len;

        /** Gusfield Z values */
        private final int[] Z;

        /** Knuth Morris Pratt sp' suffix length */
        private final int[] sp;

        /** Knuth Morris Pratt failure function */
        private final int[] F;

        private int matchlen(String str1, int pos1, String str2, int pos2)
        {
            int matchlen = 0;
            char[] s1 = str1.toCharArray();
            char[] s2 = str2.toCharArray();
            int l1 = str1.length();
            int l2 = str2.length();
            while (pos1 < l1 && pos2 < l2 && s1[pos1++] == s2[pos2++])
                matchlen++;
            return matchlen;
        }

        private void get_ZandFF(int FF)
        {
            int i, r = 0, l = 0, updaterl, patLen = len - 1;
            for (i = 1; i < patLen; i++)
            {
                if (FF != 0)
                    F[i] = sp[i - 1]; /* only when updating F */
                updaterl = 0;
                if (i > r)
                {
                    Z[i] = matchlen(pattern, i, pattern, 0);
                    if (Z[i] > 0)
                        updaterl = 1;
                }
                else
                {
                    if (Z[i - l] < r - i + 1)
                        Z[i] = Z[i - l];
                    else
                    {
                        Z[i] = r - i + 1;
                        Z[i] += matchlen(pattern, r + 1, pattern, r - i + 2);
                        updaterl = 1;
                    }
                }
                if (updaterl != 0)
                {
                    r = i + Z[i] - 1;
                    l = i;
                    if (FF != 0 && sp[r] == 0)
                        sp[r] = Z[i];
                }
            }
            if (FF != 0)
                F[i] = sp[i - 1];
        }

        private PreProcess(String thePattern)
        {
            super();
            this.pattern = thePattern;
            this.len = pattern.length() + 1;
            this.Z = new int[len];
            this.sp = new int[len];
            this.F = new int[len];

            initPreprocess();
        }

        private void initPreprocess()
        {
            get_ZandFF(1); /* get Knuth Morris Pratt F via Z */
        }

        @Override
        public String toString()
        {
            if (len > 100)
            {
                return "Pattern too long";
            }

            StringBuffer sb = new StringBuffer(512);

            sb.append("      ");
            for (int i = 0; i < pattern.length(); i++)
            {
                sb.append(pattern.charAt(i) + " ");
            }
            sb.append('\n');

            sb.append("Z   : ");
            for (int i = 0; i < len; i++)
            {
                sb.append(Z[i] + " ");
            }
            sb.append('\n');

            sb.append("sp  : ");
            for (int i = 0; i < len; i++)
            {
                sb.append(sp[i] + " ");
            }
            sb.append('\n');

            sb.append("F   : ");
            for (int i = 0; i < len; i++)
            {
                sb.append(F[i] + " ");
            }
            sb.append('\n');

            sb.append("badc: ");
            StringBuffer bcb = new StringBuffer(128);
            bcb.append("badc: ");

            sb.append(bcb.toString());
            sb.append('\n');

            return sb.toString();
        }
    }
}