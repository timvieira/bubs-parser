package edu.ohsu.cslu.parser.util;

import java.io.BufferedReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

public class ParserUtil {

    // TODO Replace with String.split() ?
    // parse 'line' into tokens and return them in an array
    public static String[] tokenize(final String line) {
        final StringTokenizer st = new StringTokenizer(line);
        final int numTokens = st.countTokens();
        final String[] tokens = new String[numTokens];

        int i = 0;
        while (st.hasMoreTokens()) {
            tokens[i++] = st.nextToken();
        }

        return tokens;
    }

    public static String join(final Collection<String> s, final String delimiter) {
        final StringBuffer buffer = new StringBuffer();
        final Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    public static double logSum(final double a, final double b) {
        // NOTE: these conditions were necessary when multiplying multiple values
        // of Float.NEGATIVE_INFINITY for the result of a or b because logSum was
        // returning NaN
        if (a <= Double.NEGATIVE_INFINITY) {
            return b;
        }
        if (b <= Double.NEGATIVE_INFINITY) {
            return a;
        }

        if (a > b) {
            return a + Math.log(Math.exp(b - a) + 1);
        }
        return b + Math.log(Math.exp(a - b) + 1);
    }

    public static void constituentSpanCountForKristy() throws Exception {
        // to compute cell constraint scores for kristy's code, we need to
        // provide the length of the longest span that starts and and longest
        // span that ends at each word in the training/dev/test corpora
        final HashMap<String, Integer> leftMax = new HashMap<String, Integer>();
        final HashMap<String, Integer> rightMax = new HashMap<String, Integer>();

        final BufferedReader stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        for (String line = stdin.readLine(); line != null; line = stdin.readLine()) {
            final ParseTree tree = ParseTree.readBracketFormat(line);
            ParseTree node;
            for (final ParseTree leaf : tree.getLeafNodes()) {

                // find the longest span to the left
                // move up the tree if it's a unary node or we're the right-most child
                node = leaf;
                while (node.parent != null && node.parent.children.getLast() == node) {
                    node = node.parent;
                }
                final int left = node.getLeafNodes().size();

                // find the longest span to the right
                node = leaf;
                while (node.parent != null && node.parent.children.getFirst() == node) {
                    node = node.parent;
                }
                final int right = node.getLeafNodes().size();

                final Integer curLeft = leftMax.get(leaf.contents);
                if (curLeft == null || left > curLeft) {
                    leftMax.put(leaf.contents, left);
                }

                final Integer curRight = rightMax.get(leaf.contents);
                if (curRight == null || right > curRight) {
                    rightMax.put(leaf.contents, right);
                }
            }

        }

        for (final String word : leftMax.keySet()) {
            System.out.println(word + "\t" + leftMax.get(word) + "\t" + rightMax.get(word));
        }

    }
}
