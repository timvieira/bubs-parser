package edu.ohsu.cslu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.StringNaryTree;

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
     * Splits a bracketed parse tree up into its constituent tokens (words, tags, '(', and ')')
     * 
     * @param parseTree
     *            Standard parenthesis-bracketed parse tree
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
     * @param parsedSentence
     *            Penn Treebank formatted parse tree
     * @return POS-tagged words without any other parse structure
     */
    public static String extractPos(final String parsedSentence) {
        final StringNaryTree tree = StringNaryTree.read(parsedSentence);
        final StringBuilder sb = new StringBuilder(parsedSentence.length());

        for (final Iterator<NaryTree<String>> i = tree.inOrderIterator(); i.hasNext();) {
            final NaryTree<String> node = i.next();
            if (node.isLeaf()) {
                sb.append('(');
                sb.append(node.parent().stringLabel());
                sb.append(' ');
                sb.append(node.stringLabel());
                sb.append(") ");
            }
        }
        // Delete the final trailing space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    /**
     * Extracts POS-tags, words, and an additional feature indicating whether a word is the head verb from a
     * Penn Treebank formatted parse structure.
     * 
     * e.g.: "(JJ fruit NONHEAD) (NN flies NONHEAD) (VBD fast HEAD) (. . NONHEAD)"
     * 
     * @param parsedSentence
     *            Penn Treebank formatted parse tree
     * @param ruleset
     *            head-percolation rules
     * @return POS-tagged words without any other parse structure
     */
    public static String extractPosAndHead(final String parsedSentence, final HeadPercolationRuleset ruleset) {
        final StringNaryTree tree = StringNaryTree.read(parsedSentence);
        final StringBuilder sb = new StringBuilder(parsedSentence.length());

        for (final Iterator<NaryTree<String>> i = tree.inOrderIterator(); i.hasNext();) {
            final NaryTree<String> node = i.next();
            if (node.isLeaf()) {
                sb.append('(');
                sb.append(node.parent().stringLabel());
                sb.append(' ');
                sb.append(node.stringLabel());
                sb.append(' ');
                sb.append(((StringNaryTree) node).isHeadOfTreeRoot(ruleset) ? "HEAD" : "NONHEAD");
                sb.append(") ");
            }
        }
        // Delete the final trailing space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    /**
     * Returns a two-dimensional array of Strings, representing each of the brackets in the supplied sequence,
     * further split up into its constituent features.
     * 
     * @param sequence
     * @return
     */
    private static String[][] bracketedTags(String sequence, final char closeBracket,
            final String endBracketPattern, final String beginBracketPattern) {
        // Remove newlines and leading and trailing whitespace
        sequence = sequence.replaceAll("\n|\r", " ").trim();
        final int firstCloseBracket = sequence.indexOf(closeBracket);
        int tokensPerBracket = 0;
        for (int i = 0; i < firstCloseBracket && i > 0; i++) {
            i = sequence.indexOf(' ', i);
            tokensPerBracket++;
        }

        final String[] split = sequence.split(endBracketPattern);
        final String[][] bracketedTags = new String[split.length][];

        for (int i = 0; i < split.length; i++) {
            bracketedTags[i] = split[i].replaceAll(beginBracketPattern, "").split(" +");
        }
        return bracketedTags;
    }

    /**
     * Returns a two-dimensional array of Strings, representing each of the brackets in the supplied sequence,
     * further split up into its constituent features.
     * 
     * @param sequence
     * @return string tokens
     */
    public static String[][] bracketedTags(final String sequence) {
        return bracketedTags(sequence, ')', " *\\)", " *\\(");
    }

    /**
     * Returns a two-dimensional array of Strings, representing each of the brackets in the supplied sequence,
     * further split up into its constituent features.
     * 
     * @param sequence
     * @return string tokens
     */
    public static String[][] squareBracketedTags(final String sequence) {
        return bracketedTags(sequence, ']', " *\\]", " *\\[");
    }

    /**
     * Returns a two-dimensional array of Strings, representing each of the slash-delimited tokens in the
     * supplied sequence, further split up into its constituent features.
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
     * @param s
     *            a space-delimited string
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
     * @param s
     *            a bracketed feature list (e.g., "(The DT) (cow NN) (ate VBD _head_verb) (. .)")
     * @return All possible permutations of the features in the supplied string
     */
    public static Set<String> permuteFeatures(final String s) {
        final HashSet<String> permutations = new HashSet<String>();
        final String[] split = s.split("\\) *");
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

            permutations.addAll(recursivePermute(permutations, prefix.length() == 0 ? suffix[i] : prefix
                    + " " + suffix[i], newSuffix));
        }

        return permutations;
    }

    /**
     * Splits the string by whitespace into tokens, and returns all 2-token combinations
     * 
     * @param s
     *            Whitespace-delimited string
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
     * @param s
     *            Bracketed representation of a sequence. (e.g., "(The) (cow) (ate) (.)")
     * @return Ordered pairs of bracketings. (e.g. (The) (cow), (The) (ate), (The .), (cow) (ate) ...)
     */
    public static Set<String> featurePairs(final String s) {
        final HashSet<String> pairs = new HashSet<String>();
        final String[] elements = s.split("\\) *");
        if (elements.length == 1) {
            pairs.add(s);
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
     * Splits the string by bracketed features, and returns all words, assuming the word is the first feature
     * in each bracketing
     * 
     * TODO: Terrible hack
     * 
     * @param s
     *            Bracketed representation of a sequence. (e.g.,
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
}
