package edu.ohsu.cslu.parser.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.parser.ParserDriver;

public class TreeTools {

    private static BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
    private static BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));

    static public enum ActionType {
        UnBinarize, TransformOrig, ConstituentSpanCount
    }

    public static void main(final String[] args) throws Exception {

        // @Option(name = "-countSpans", hidden = true, usage =
        // "Count max length of span for each word in input trees that starts or ends at each word")

        // System.out.println("args:" + args);
        ActionType action = ActionType.UnBinarize;
        if (ParserDriver.param1 > -1) {
            action = ActionType.TransformOrig;
        }

        for (String sentence = inputStream.readLine(); sentence != null; sentence = inputStream.readLine()) {

            final ParseTree tree = ParseTree.readBracketFormat(sentence);

            switch (action) {
            case UnBinarize:
                outputStream.write(sentence);
                ParseTree.unbinarizeTree(tree, GrammarFormatType.CSLU);
                break;
            case TransformOrig:
                // removeTmpLabels(tree);
                // removeEmptyNodes(tree);
                // removeSpuriousUnaries(tree);
                removeTmpLabelsEmptyNodesAndSpuriousUnaries(tree);
                break;
            case ConstituentSpanCount:
                constituentSpanCountForKristy();
                break;
            default:
                System.err.println("ERROR: action not recognized.");
                System.exit(1);
            }

            // this wasn't writing anything to stdout???
            // outputStream.write(tree.toString());
            System.out.println(tree.toString());
        }
    }

    public static void removeTmpLabelsEmptyNodesAndSpuriousUnaries(final ParseTree tree) {
        int i = 0;
        while (i < tree.children.size()) {
            final ParseTree child = tree.children.get(i);
            if (allChildrenEmptyNodes(child)) {
                tree.children.remove(i);
                i = 0; // re-process children so we can collapse spurious unaries if necessary
            } else if (!child.isLeaf() && child.contents.matches("^[A-Z]+((-|=)[A-Z0-9]+)+")) {
                // ex: NP-SBJ-1 ==> NP
                // ex: ADVP-TMP ==> ADVP
                int cutIndex = child.contents.indexOf("-");
                final int k = child.contents.indexOf("=");
                // TODO: berkeley also looks for '^' but I didn't see this anywhere in the english
                // or chinese corpora
                if (k > 0 && (k < cutIndex || cutIndex < 0)) {
                    cutIndex = k;
                }
                child.contents = child.contents.substring(0, cutIndex);
            } else if (tree.children.size() == 1 && !child.isLeaf() && (tree.contents.equals(child.contents))) {
                // if we've got a duplicate unary chain, remove it
                // ex: (NP (NP (NN xxx))) ==> (NP (NN xxx))
                tree.children = child.children;
            } else {
                i++;
                removeTmpLabelsEmptyNodesAndSpuriousUnaries(child);
            }
        }
    }

    public static void removeSpuriousUnaries(final ParseTree tree) {
        int i = 0;
        while (i < tree.children.size()) {
            final ParseTree child = tree.children.get(i);
            if (tree.children.size() == 1 && !child.isLeaf() && (tree.contents.equals(child.contents))) {
                // if we've got a duplicate unary chain, remove it
                // ex: (NP (NP (NN xxx))) ==> (NP (NN xxx))
                tree.children = child.children;
            } else {
                i++;
                removeSpuriousUnaries(child);
            }
        }
    }

    public static void removeTmpLabels(final ParseTree tree) {
        int i = 0;
        while (i < tree.children.size()) {
            final ParseTree child = tree.children.get(i);
            if (!child.isLeaf() && child.contents.matches("^[A-Z]+((-|=)[A-Z0-9]+)+")) {
                // ex: NP-SBJ-1 ==> NP
                // ex: ADVP-TMP ==> ADVP
                int cutIndex = child.contents.indexOf("-");
                final int k = child.contents.indexOf("=");
                if (k > 0 && (k < cutIndex || cutIndex < 0)) {
                    cutIndex = k;
                }
                child.contents = child.contents.substring(0, cutIndex);
            } else {
                i++;
                removeTmpLabels(child);
            }
        }
    }

    public static void removeEmptyNodes(final ParseTree tree) {
        int i = 0;
        while (i < tree.children.size()) {
            final ParseTree child = tree.children.get(i);
            if (allChildrenEmptyNodes(child)) {
                // the children will be removed as well.
                tree.children.remove(i);
                // 'i' remains unchanged so we will process the newly moved children next
            } else {
                i++;
                removeEmptyNodes(child);
            }
        }
    }

    private static boolean allChildrenEmptyNodes(final ParseTree tree) {
        if (tree.isLeaf()) {
            return false;
        } else if (tree.isPOS()) {
            if (tree.contents.equals("-NONE-")) {
                return true;
            }
            return false;
        }

        boolean allEmpty = true;
        for (final ParseTree child : tree.children) {
            allEmpty = allEmpty & allChildrenEmptyNodes(child);
            if (allEmpty == false) {
                return false;
            }
        }
        return allEmpty; // always true
    }

    public static void constituentSpanCountForKristy() throws Exception {
        // to compute cell constraint scores for kristy's code, we need to
        // provide the length of the longest span that starts and and longest
        // span that ends at each word in the training/dev/test corpora
        final HashMap<String, Integer> leftMax = new HashMap<String, Integer>();
        final HashMap<String, Integer> rightMax = new HashMap<String, Integer>();

        // final BufferedReader inputStream = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        for (String line = inputStream.readLine(); line != null; line = inputStream.readLine()) {
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
