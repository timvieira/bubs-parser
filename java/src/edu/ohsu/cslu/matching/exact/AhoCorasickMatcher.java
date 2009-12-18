package edu.ohsu.cslu.matching.exact;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ohsu.cslu.matching.Matcher;

/**
 * Implements the Aho-Corasick matching algorithm. Particularly effective for matching large sets of long
 * patterns within a text. With a few patterns, this algorithm is significantly faster than the naive
 * (String.indexOf()) approach. With a single pattern, the algorithmic overhead negates the potential
 * efficiency gain. The break-even point is 'a few' patterns (exact number and length of patterns unknown).
 * 
 * Adapted from Seeger's code (which I believe was adapted from Brian's C version)
 * 
 * @author Seeger Fisher
 * @author Aaron Dunlop
 * @since Jun 16, 2008
 * 
 *        $Id$
 */
public final class AhoCorasickMatcher extends Matcher {

    @Override
    public final IntSet matchLocations(Set<String> patterns, String text) {
        ArrayList<String> patternList = new ArrayList<String>(patterns.size());
        patternList.addAll(patterns);
        return internalMatch(patternList, text).locationSet();
    }

    /**
     * Returns a set of match locations and the pattern indices of each match.
     * 
     * @param patterns
     * @param text
     * @return Map, match location -> match location
     */
    public final Set<Match> patternMatchLocations(List<String> patterns, String text) {
        return internalMatch(patterns, text).set();
    }

    /**
     * Returns a set of match locations and the pattern indices of each match.
     * 
     * @param patterns
     * @param text
     * @return Map, match location -> match location
     */
    public final Set<Match> patternMatchLocations(Set<String> patterns, String text) {
        ArrayList<String> patternList = new ArrayList<String>(patterns.size());
        patternList.addAll(patterns);
        return internalMatch(patternList, text).set();
    }

    /**
     * Returns a set of match locations and the pattern indices of each match.
     * 
     * @param patterns
     * @param text
     * @return Map, match location -> match location
     */
    public final Set<Match> patternMatchLocations(String[] patterns, String text) {
        return internalMatch(Arrays.asList(patterns), text).set();
    }

    /**
     * Returns a set of match locations and the pattern indices of each match.
     * 
     * @param patterns
     * @param text
     * @return Map, match location -> match location
     */
    private final MatchSet internalMatch(List<String> patterns, String text) {
        final PreProcess preProcess = new PreProcess(patterns);
        final MatchSet matchLocations = new MatchSet();

        // Local copies for somewhat faster access
        // Even though most of these fields are final in the PreProcess object, it seems to make a
        // measurable speed difference to have local copies. Perhaps because local objects are now
        // on the stack?
        final int n = text.length();
        final int minchar = preProcess.minPatternChar;
        final int maxchar = preProcess.maxPatternChar;
        final KeyTree root = preProcess.ktreeRoot;

        int nextlinkIndex;
        int i = 0;
        char c;

        KeyTree w = preProcess.ktreeRoot;
        KeyTree wPrime;

        do {
            while (i < n && ((c = text.charAt(i)) < maxchar) && (w != null)
                    && ((nextlinkIndex = (c - minchar)) >= 0)
                    && ((wPrime = w.nextlinks[nextlinkIndex]) != null)) {
                for (KeyTree tree = wPrime; tree != null; tree = tree.zeroLink) {
                    if (tree.index != -1) {
                        matchLocations.addMatch(tree.index, i + 1);
                        break;
                    }
                }

                w = wPrime;
                i++;
            }

            i = i - w.length + 1; // lp(w) = length of w's failure link

            // n sub w - w's failure link
            w = w.zeroLink;
            if (w == null) {
                w = root;
            }
        } while (i < n);

        return matchLocations;
    }

    // TODO: Pull this class and 'patternMatchLocations' methods up into Matcher?
    public static class Match implements Comparable<Match> {

        private final int patternIndex;
        private final int location;

        public Match(int patternIndex, int location) {
            this.patternIndex = patternIndex;
            this.location = location;
        }

        public int compareTo(Match o) {
            if (patternIndex > o.patternIndex) {
                return 1;
            }

            if (patternIndex < o.patternIndex) {
                return -1;
            }

            if (location > o.location) {
                return 1;
            }

            if (location < o.location) {
                return -1;
            }

            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Match)) {
                return false;
            }

            Match m = (Match) o;
            return (patternIndex == m.patternIndex && location == m.location);
        }

        public int getPatternIndex() {
            return patternIndex;
        }

        public int getLocation() {
            return location;
        }

        @Override
        public String toString() {
            return patternIndex + "," + location;
        }

        @Override
        public int hashCode() {
            return location * 17 + patternIndex;
        }
    }

    private class MatchSet {

        private final Set<Match> set = new HashSet<Match>();

        public void addMatch(int patternIndex, int location) {
            set.add(new Match(patternIndex, location));
        }

        public IntSet patternSet() {
            IntSet patternSet = new IntOpenHashSet();
            for (Match match : set) {
                patternSet.add(match.patternIndex);
            }
            return patternSet;
        }

        public IntSet locationSet() {
            IntSet locationSet = new IntOpenHashSet();
            for (Match match : set) {
                locationSet.add(match.location);
            }
            return locationSet;
        }

        public Set<Match> set() {
            return set;
        }
    }

    private class KeyTree {

        private KeyTree zeroLink; // Reference to nextlinks[0] - saves repeated array accesses
        private final KeyTree[] nextlinks; // array of possible continuation letters
        private final int length; // length of prefix to this point
        private int index; // if final state, word number (else -1)

        public KeyTree(int maxchar, int minchar, int wordNumber, int length) {
            index = wordNumber;
            this.length = length;
            // initialize links -- ktree.nextlinks[0] is the failure link
            nextlinks = new KeyTree[maxchar - minchar];
        }
    }

    private class PreProcess {

        private final KeyTree ktreeRoot; // root of keyword tree
        private final int maxPatternChar; // maximum character in patterns
        private final int minPatternChar; // minimum character in patterns

        public PreProcess(final Collection<String> patterns) {
            char min = Character.MAX_VALUE;
            char max = 0;

            // TODO: Is there a faster way to do this?
            for (String pattern : patterns) {
                for (int i = pattern.length() - 1; i >= 0; i--) {
                    char c = pattern.charAt(i);
                    if (c > max) {
                        max = c;
                    }
                    if (c < min) {
                        min = c;
                    }
                }
            }

            maxPatternChar = max + 1;
            minPatternChar = min - 1;
            ktreeRoot = new KeyTree(maxPatternChar, minPatternChar, -1, 0);

            // TODO: Convert patterns to char arrays?
            buildKeywordTree(patterns);
        }

        /**
         * Builds the Keyword Tree
         */
        private void buildKeywordTree(final Collection<String> patterns) {
            int maximumDepth = 0;
            int i = 0;
            for (String pattern : patterns) {
                final int depth = growKeyTree(ktreeRoot, 0, pattern, i++); // keep growing keyword
                // tree
                if (depth > maximumDepth) {
                    maximumDepth = depth;
                }
            }

            // set failure links from depth 0 to the maximum depth, in depth order
            for (int depth = 0; depth <= maximumDepth; depth++) {
                setFailureLinks(ktreeRoot, ktreeRoot, depth);
            }
        }

        // grow the keyword tree for a particular pattern
        private int growKeyTree(final KeyTree ktree, final int pos, final String pattern,
                final int patternIndex) {
            int depth = 1;
            final int len = pattern.length();
            if (pos >= len) {
                return depth;
            }

            final char tchar = pattern.charAt(pos);

            if (ktree.nextlinks[tchar - minPatternChar] == null)

            {
                ktree.nextlinks[tchar - minPatternChar] = new KeyTree(maxPatternChar, minPatternChar, -1,
                    ktree.length + 1);
            }

            if (pos == len - 1) {
                ktree.nextlinks[tchar - minPatternChar].index = patternIndex;
            }

            depth += growKeyTree(ktree.nextlinks[tchar - minPatternChar], pos + 1, pattern, patternIndex);
            return depth;
        }

        private void setFailureLinks(final KeyTree vPrime, final KeyTree root, final int depth) {
            for (int x = minPatternChar; x < maxPatternChar; x++) {
                final KeyTree v = vPrime.nextlinks[x - minPatternChar];

                if (v != null) {
                    // v is a child of vPrime and
                    // x labels the edge from vPrime to v
                    KeyTree w = vPrime.nextlinks[0]; // vPrime's failure link
                    while (w != null && w.nextlinks[x] == null) {
                        w = w.nextlinks[0];
                    }

                    v.zeroLink = v.nextlinks[0] = (w == null) ? null : w.nextlinks[x];

                    if (v.zeroLink == null) {
                        v.zeroLink = root;
                    }

                    if (v.index >= 0) {
                        setFailureLinks(v, root, depth + 1);
                    }
                }
            }
        }
    }
}
