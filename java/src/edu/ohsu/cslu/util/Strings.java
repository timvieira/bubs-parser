/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;

public class Strings {

    public static String fill(final char c, final int count) {
        if (count < 0) {
            return "";
        }

        final char[] buf = new char[count];
        Arrays.fill(buf, c);
        return new String(buf);
    }

    /**
     * Merges a {@link Collection} of strings into a single string, separated by the specified delimiter.
     * 
     * @param s
     * @param delimiter
     * @return a single string, separated by the specified delimiter
     */
    public static String join(final Collection<String> s, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        final Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    /**
     * Merges a {@link Collection} of Objects into a single string, separated by the specified delimiter.
     * 
     * @param objs
     * @param delimiter
     * @return a single string, separated by the specified delimiter
     */
    public static String join(final Object[] objs, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(objs[i].toString());
            buffer.append(delimiter);
        }
        buffer.append(objs[objs.length - 1].toString());
        return buffer.toString();
    }

    /**
     * Merges an array of objects into a single string, separated by the specified delimiter.
     * 
     * @param objs
     * @param format Format as used by {@link String#format(String, Object...)}
     * @param delimiter
     * @return a single string, separated by the specified delimiter
     */
    public static String join(final Object[] objs, final String format, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(String.format(format, objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(String.format(format, objs[objs.length - 1]));
        return buffer.toString();
    }

    /**
     * Merges an array of longs into a single string, separated by the specified delimiter.
     * 
     * @param objs
     * @param delimiter
     * @return a single string, separated by the specified delimiter
     */
    public static String join(final long[] objs, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(Long.toString(objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(Long.toString(objs[objs.length - 1]));
        return buffer.toString();
    }

    /**
     * Merges an array of ints into a single string, separated by the specified delimiter.
     * 
     * @param objs
     * @param delimiter
     * @return a single string, separated by the specified delimiter
     */
    public static String join(final int[] objs, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(Integer.toString(objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(Integer.toString(objs[objs.length - 1]));
        return buffer.toString();
    }

    /**
     * Merges an array of shorts into a single string, separated by the specified delimiter.
     * 
     * @param objs
     * @param delimiter
     * @return a single string, separated by the specified delimiter
     */
    public static String join(final short[] objs, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(Short.toString(objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(Short.toString(objs[objs.length - 1]));
        return buffer.toString();
    }

    /**
     * Merges an array of doubles into a single string, separated by the specified delimiter.
     * 
     * @param objs
     * @param format Format as used by {@link String#format(String, Object...)}
     * @param delimiter
     * @return a single string, separated by the specified delimiter
     */
    public static String join(final double[] objs, final String format, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(String.format(format, objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(String.format(format, objs[objs.length - 1]));
        return buffer.toString();
    }

    /**
     * Merges an array of doubles into a single string, separated by the specified delimiter.
     * 
     * @param objs
     * @param format Format as used by {@link String#format(String, Object...)}
     * @param delimiter
     * @return a single string, separated by the specified delimiter
     */
    public static String join(final float[] objs, final String format, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(String.format(format, objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(String.format(format, objs[objs.length - 1]));
        return buffer.toString();
    }

    /**
     * Returns the array of strings computed by splitting this string around spaces (the same as calling
     * {@link String#split(String)} with " "), but faster. If <code>s</code> is null or is the empty string, a list
     * containing only the empty string is returned.
     * 
     * @param s
     * @return the array of strings computed by splitting this string around spaces (the same as calling
     *         {@link String#split(String)} with " ")
     */
    public static String[] splitOnSpace(final String s) {
        if (s == null || s.length() == 0) {
            return new String[] { "" };
        }

        final ArrayList<String> split = new ArrayList<String>();
        int i = 0, j = s.indexOf(' ');
        while (j >= 0) {
            split.add(s.substring(i, j));
            i = j + 1;
            j = s.indexOf(' ', i);
        }
        if (i < s.length()) {
            split.add(s.substring(i));
        }
        return split.toArray(new String[split.size()]);
    }

    /**
     * Parses each integer in a comma-delimited array (of the form 'x,y,z,...', or 'x, y, z, ...'
     * 
     * @param str
     * @return Integer array
     */
    public static int[] parseCommaDelimitedInts(final String str) {
        final String[] tokens = str.split(" *, *");
        final int[] array = new int[tokens.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = Integer.parseInt(tokens[i]);
        }
        return array;
    }

    /**
     * Returns the array of strings computed by splitting this string around the specified delimiter. Strings containing
     * the delimiter can be escaped with the specified escape character, and the escape character can itself be escaped
     * with '\'
     * 
     * @param s String to split
     * @param delimiter Field delimiter
     * @param escapeChar Character used to escape any strings containing the delimiter
     * @return the array of strings computed by splitting this string around the specified delimiter
     */
    public static String[] splitOn(final String s, final char delimiter, final char escapeChar) {
        if (s == null || s.length() == 0) {
            return new String[] { "" };
        }

        final ArrayList<String> split = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);

            // Escaped escape char
            if (c == '\\' && i < s.length() - 1 && s.charAt(i + 1) == escapeChar) {
                i++;
                sb.append('\'');
                continue;
            }

            // Regular escape char
            if (c == '\'') {
                escaped = !escaped;
                continue;
            }

            // Delimiter
            if (c == delimiter && !escaped) {
                split.add(sb.toString());
                sb = new StringBuilder();
                continue;
            }

            // Normal case
            sb.append(c);
        }

        split.add(sb.toString());
        return split.toArray(new String[split.size()]);
    }

    /**
     * Returns a copy of the string. If the string contains the <code>delimiter</code>, it will be escaped with the
     * specified <code>escapeChar</code>, and if it contains <code>escapeChar</code>, it will in turn be escaped with
     * '\'
     * 
     * @param s String to escape
     * @param delimiter Field delimiter
     * @param escapeChar Character used to escape any strings containing the delimiter
     * @return the array of strings computed by splitting this string around the specified delimiter
     */
    public static String escape(final String s, final char delimiter, final char escapeChar) {
        final StringBuilder sb = new StringBuilder(s.length() + 10);
        final boolean containsSpecialChar = s.indexOf(delimiter) >= 0 || s.indexOf(escapeChar) >= 0;

        if (containsSpecialChar) {
            sb.append('\'');
        }

        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == escapeChar) {
                sb.append('\\');
            }
            sb.append(c);
        }

        if (containsSpecialChar) {
            sb.append('\'');
        }
        return sb.toString();
    }

    /**
     * Splits a bracketed parse tree up into its constituent tokens (words, tags, '(', and ')')
     * 
     * @param parseTree Standard parenthesis-bracketed parse tree
     * @return tokens
     */
    public static List<String> parseTreeTokens(final String parseTree) {
        // Split the string up into tokens '(', ')', tags, and words
        final char[] charArray = parseTree.toCharArray();
        final ArrayList<String> tokens = new ArrayList<String>(charArray.length);
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            // Skip over spaces
            while (c == ' ') {
                i++;
                c = charArray[i];
            }

            if (charArray[i] == '(') {
                tokens.add("(");
            } else {
                final int start = i;
                while (c != ' ' && c != ')') {
                    i++;
                    c = charArray[i];
                }

                if (i > start) {
                    tokens.add(parseTree.substring(start, i));
                }

                if (c == ')') {
                    tokens.add(")");
                }
            }
        }

        return tokens;
    }

    /**
     * Extracts POS-tags and words only from a Penn Treebank formatted parse structure.
     * 
     * e.g.: "(JJ fruit) (NN flies) (VBD fast) (. .)"
     * 
     * @param parsedSentence Penn Treebank formatted parse tree
     * @return POS-tagged words without any other parse structure
     */
    public static String extractPos(final String parsedSentence) {
        final NaryTree<String> tree = NaryTree.read(parsedSentence, String.class);
        final StringBuilder sb = new StringBuilder(parsedSentence.length());

        for (final NaryTree<String> node : tree.inOrderTraversal()) {
            if (node.isLeaf()) {
                sb.append('(');
                sb.append(node.parent().label().toString());
                sb.append(' ');
                sb.append(node.label().toString());
                sb.append(") ");
            }
        }
        // Delete the final trailing space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    /**
     * Extracts POS-tags, words, and an additional feature indicating whether a word is the head verb from a Penn
     * Treebank formatted parse structure.
     * 
     * e.g.: "(JJ fruit NONHEAD) (NN flies NONHEAD) (VBD fast HEAD) (. . NONHEAD)"
     * 
     * @param parsedSentence Penn Treebank formatted parse tree
     * @param ruleset head-percolation rules
     * @return POS-tagged words without any other parse structure
     */
    public static String extractPosAndHead(final String parsedSentence, final HeadPercolationRuleset ruleset) {
        final NaryTree<String> tree = NaryTree.read(parsedSentence, String.class);
        final StringBuilder sb = new StringBuilder(parsedSentence.length());

        for (final NaryTree<String> node : tree.inOrderTraversal()) {
            if (node.isLeaf()) {
                sb.append('(');
                sb.append(node.parent().label().toString());
                sb.append(' ');
                sb.append(node.label().toString());
                sb.append(' ');
                sb.append(node.isHeadOfTreeRoot(ruleset) ? "HEAD" : "NONHEAD");
                sb.append(") ");
            }
        }
        // Delete the final trailing space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    /**
     * Returns a two-dimensional array of Strings, representing each of the brackets in the supplied sequence, further
     * split up into its constituent features.
     * 
     * @param sequence
     * @return
     */
    private static String[][] bracketedTags(String sequence, final char closeBracket, final String endBracketPattern,
            final String beginBracketPattern) {
        // Remove newlines and leading and trailing whitespace
        sequence = sequence.replaceAll("\n|\r", " ").trim();
        final int firstCloseBracket = sequence.indexOf(closeBracket);
        for (int i = 0; i < firstCloseBracket && i > 0; i++) {
            i = sequence.indexOf(' ', i);
        }

        final String[] split = sequence.split(endBracketPattern);
        final String[][] bracketedTags = new String[split.length][];

        for (int i = 0; i < split.length; i++) {
            bracketedTags[i] = split[i].replaceAll(beginBracketPattern, "").split(" +");
        }
        return bracketedTags;
    }

    /**
     * Returns a two-dimensional array of Strings, representing each of the brackets in the supplied sequence, further
     * split up into its constituent features.
     * 
     * @param sequence
     * @return string tokens
     */
    public static String[][] bracketedTags(final String sequence) {
        return bracketedTags(sequence, ')', " *\\)", " *\\(");
    }

    /**
     * Returns a two-dimensional array of Strings, representing each of the brackets in the supplied sequence, further
     * split up into its constituent features.
     * 
     * @param sequence
     * @return string tokens
     */
    public static String[][] squareBracketedTags(final String sequence) {
        return bracketedTags(sequence, ']', " *\\]", " *\\[");
    }

    /**
     * Returns a two-dimensional array of Strings, representing each of the slash-delimited tokens in the supplied
     * sequence, further split up into its constituent features.
     * 
     * @param sequence
     * @return string tokens
     */
    public static String[][] slashDelimitedTags(String sequence) {
        // Remove newlines and leading and trailing whitespace
        sequence = sequence.replaceAll("\n|\r", " ").trim();

        final String[] split = sequence.split(" +");
        final String[][] bracketedTags = new String[split.length][];

        for (int i = 0; i < split.length; i++) {
            bracketedTags[i] = split[i].split("/");
        }
        return bracketedTags;
    }

    /**
     * Parses a header line from a persisted type (Matrix, SimpleVocabulary, etc.) and returns the attributes
     * represented therein.
     * 
     * @param line
     * @return Map of attributes
     */
    public static Map<String, String> headerAttributes(final String line) {
        final Map<String, String> attributes = new HashMap<String, String>();
        for (final String stringAttribute : line.split(" +")) {
            if (stringAttribute.indexOf('=') >= 0) {
                final String[] split = stringAttribute.split("=");
                attributes.put(split[0], split[1]);
            }
        }
        return attributes;
    }

    /**
     * Permutes a space-delimited string
     * 
     * @param s a space-delimited string
     * @return All possible permutations of the tokens in the supplied string
     */
    public static Set<String> permuteTokens(final String s) {
        final HashSet<String> permutations = new HashSet<String>();
        recursivePermute(permutations, "", s.split(" "));
        return permutations;
    }

    /**
     * Permutes a bracketed feature list
     * 
     * @param s a bracketed feature list (e.g., "(The DT) (cow NN) (ate VBD _head_verb) (. .)")
     * @return All possible permutations of the features in the supplied string
     */
    public static Set<String> permuteFeatures(final String s) {
        return permuteFeatures(s, Integer.MAX_VALUE);
    }

    /**
     * Permutes a bracketed feature list
     * 
     * @param s a bracketed feature list (e.g., "(The DT) (cow NN) (ate VBD _head_verb) (. .)")
     * @return All possible permutations of the features in the supplied string
     */
    public static Set<String> permuteFeatures(final String s, final int maxLength) {
        final HashSet<String> permutations = new HashSet<String>();
        final String[] split = s.split("\\) *");
        if (split.length > maxLength) {
            return null;
        }
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i] + ")";
        }
        recursivePermute(permutations, "", split);
        return permutations;
    }

    /**
     * Permutes the supplied string array
     * 
     * @param permutations
     * @param prefix
     * @param suffix
     * @return All permutations of the specified suffix
     */
    private static Set<String> recursivePermute(final Set<String> permutations, final String prefix,
            final String[] suffix) {
        // Base case of length 1
        if (suffix.length == 1) {
            permutations.add(prefix.length() == 0 ? suffix[0] : prefix + " " + suffix[0]);
            return permutations;
        }

        // Call recursively for each feature in toPermute
        for (int i = 0; i < suffix.length; i++) {
            final String[] newSuffix = new String[suffix.length - 1];
            System.arraycopy(suffix, 0, newSuffix, 0, i);
            System.arraycopy(suffix, i + 1, newSuffix, i, newSuffix.length - i);

            permutations.addAll(recursivePermute(permutations, prefix.length() == 0 ? suffix[i] : prefix + " "
                    + suffix[i], newSuffix));
        }

        return permutations;
    }

    /**
     * Splits the string by whitespace into tokens, and returns all 2-token combinations
     * 
     * @param s Whitespace-delimited string
     * @return Pairs of tokens
     */
    public static Set<String> tokenPairs(final String s) {
        final String[] tokens = s.split("\\s+");
        return tokenPairs(tokens);
    }

    /**
     * Returns all 2-token combinations
     * 
     * @param tokens
     * @return Pairs of tokens
     */
    public static Set<String> tokenPairs(final String[] tokens) {
        final HashSet<String> pairs = new HashSet<String>();
        if (tokens.length == 1) {
            pairs.add(tokens[0]);
            return pairs;
        }

        for (int i = 0; i < tokens.length - 1; i++) {
            for (int j = i + 1; j < tokens.length; j++) {
                pairs.add(tokens[i] + " " + tokens[j]);
            }
        }
        return pairs;
    }

    /**
     * Splits the string by bracketed features, and returns all ordered 2-bracket combinations.
     * 
     * @param s Bracketed representation of a sequence. (e.g., "(The) (cow) (ate) (.)")
     * @return Ordered pairs of bracketings. (e.g. (The) (cow), (The) (ate), (The .), (cow) (ate) ...)
     */
    public static Set<String> featurePairs(final String s) {
        return featurePairs(s, Integer.MAX_VALUE - 1);
    }

    /**
     * Splits the string by bracketed features, and returns all ordered 2-bracket combinations.
     * 
     * @param s Bracketed representation of a sequence. (e.g., "(The) (cow) (ate) (.)")
     * @return Ordered pairs of bracketings. (e.g. (The) (cow), (The) (ate), (The .), (cow) (ate) ...)
     */
    public static Set<String> featurePairs(final String s, final int maxLength) {
        final HashSet<String> pairs = new HashSet<String>();
        final String[] elements = s.split("\\) *");
        if (elements.length == 1) {
            pairs.add(s);
            return pairs;
        }

        if (elements.length > maxLength + 1) {
            return pairs;
        }

        for (int i = 0; i < elements.length - 1; i++) {
            for (int j = i + 1; j < elements.length; j++) {
                pairs.add(elements[i] + ") " + elements[j] + ")");
            }
        }
        return pairs;
    }

    /**
     * Splits the string by bracketed features, and returns all words, assuming the word is the first feature in each
     * bracketing
     * 
     * @param bracketedFeatures Bracketed representation of a sequence. (e.g.,
     *            "(The DT) (cow NN) (ate VBD _head_verb) (. .)")
     * @return words
     */
    public static String[] words(final String bracketedFeatures) {
        final ArrayList<String> words = new ArrayList<String>();
        final String[] elements = bracketedFeatures.split("\\) *");
        for (final String element : elements) {
            final int index = element.indexOf(' ');
            final String word = index > 0 ? element.substring(1, index) : element.substring(1);
            words.add(word);
        }
        return words.toArray(new String[words.size()]);
    }

    public static String readInputStream(final InputStream is) throws IOException {
        final StringBuilder sb = new StringBuilder(4096);
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Returns a string representation of a rational approximation of the exponentiation of a real.
     * 
     * Performs brute-force search for numerators from 1 to 20. If no approximation is found within 0.01 of the
     * specified value, returns the closest approximation with a numerator of 1.
     * 
     * This hackish approximation is useful for small fractions (3/16, 5/12, etc.) and for small probabilities, which
     * can often be well-represented by a numerator of 1 and an appropriate denominator (e.g. 1/12385)
     * 
     * @param logProbability
     * @return string representation of a rational approximation of a log probability
     */
    public static String fraction(final double logProbability) {
        if (logProbability == 0f) {
            return "1";
        }
        if (logProbability == Double.NEGATIVE_INFINITY) {
            return "0";
        }

        final double exp = java.lang.Math.exp(-1.0 * logProbability);
        for (int numerator = 1; numerator <= 20; numerator++) {
            final double denominator = exp * numerator;
            if (java.lang.Math.abs(denominator - java.lang.Math.round(denominator)) <= 0.01) {
                return String.format("%d/%d", numerator, java.lang.Math.round(denominator));
            }
        }

        return String.format("1/%d", java.lang.Math.round(exp));
    }

    public static float numeralPercentage(final String token) {
        float numerals = 0;
        for (int i = 0; i < token.length(); i++) {
            if (Character.isDigit(token.charAt(i))) {
                numerals++;
            }
        }
        return numerals / token.length();
    }

    /**
     * A simple set of punctuation characters. This might need expansion for alternate languages or domains.
     * 
     * @param c
     * @return True if the supplied character is a punctuation (according to a very loosely-defined set)
     */
    public static boolean isPunctuation(final char c) {
        // TODO Handle hyphen, comma, and period separately?
        switch (c) {
        case ',':
        case '.':
        case '!':
        case '?':
        case ':':
        case ';':
        case '/':
        case '\\':
        case '#':
        case '$':
        case '%':
        case '-':
            return true;
        default:
            return false;
        }
    }

    public static float punctuationPercentage(final String token) {
        float numerals = 0;
        for (int i = 0; i < token.length(); i++) {
            if (isPunctuation(token.charAt(i))) {
                numerals++;
            }
        }
        return numerals / token.length();
    }

    // TODO Our own floating-point formatter, in case we decide to replace String.format() in ParseTask.statsString()
    // with a faster implementation.
    //
    // public static String format(final double d, final int decimalPlaces) {
    //
    // }
    //
    // public static void formatAndAppend(double d, final int decimalPlaces, final StringBuilder sb) {
    // if (d < 0) {
    // sb.append('-');
    // d = -d;
    // }
    // final long scaled = (long) (d * 1e6 + 0.5);
    // long factor = 1000000;
    // int scale = 7;
    // while (factor * 10 <= scaled) {
    // factor *= 10;
    // scale++;
    // }
    // while (scale > 0) {
    // if (scale == 6)
    // sb.append('.');
    // final long c = scaled / factor % 10;
    // factor /= 10;
    // sb.append((char) ('0' + c));
    // scale--;
    // }
    // }
}
