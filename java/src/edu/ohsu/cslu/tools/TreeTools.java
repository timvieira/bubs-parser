package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserDriver;

public class TreeTools {

    private static BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
    private static BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));

    public static void main(final String[] args) throws Exception {

        // @Option(name = "-countSpans", hidden = true, usage =
        // "Count max length of span for each word in input trees that starts or ends at each word")

        // System.out.println("args:" + args);
        // more comments ...
        final int action = (int) ParserDriver.param1;
        boolean rightFactor = false;

        for (String sentence = inputStream.readLine(); sentence != null; sentence = inputStream.readLine()) {

            final ParseTree tree = ParseTree.readBracketFormat(sentence);

            switch (action) {
            case -1:
                outputStream.write(sentence);
                unbinarizeTree(tree, GrammarFormatType.CSLU);
                break;
            case 0:
                // removeTmpLabels(tree);
                // removeEmptyNodes(tree);
                // removeSpuriousUnaries(tree);
                removeTmpLabelsEmptyNodesAndSpuriousUnaries(tree);
                break;
            case 1:
                rightFactor = true;
                //$FALL-THROUGH$
            case 2:
                final int horzMarkov = (int) ParserDriver.param2;
                final int vertMarkov = (int) ParserDriver.param3;
                final boolean annotatePOS = false;
                final GrammarFormatType gramFormat = GrammarFormatType.CSLU;
                binarizeTree(tree, rightFactor, horzMarkov, vertMarkov, annotatePOS, gramFormat);
                break;
            case 5:
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

    public static void unbinarizeTree(final ParseTree tree, final GrammarFormatType grammarFormatType) {
        int i = 0;
        while (i < tree.children.size()) {
            final ParseTree child = tree.children.get(i);
            if (grammarFormatType.isFactored(child.contents)) {
                tree.children.remove(i);
                tree.children.addAll(i, child.children);
                // 'i' remains unchanged so we will process the newly moved children next
            } else {
                i++;
                unbinarizeTree(child, grammarFormatType);
            }
        }
    }

    public static void binarizeTree(final ParseTree tree, final boolean rightFactor, final int horzMarkov,
            final int vertMarkov, final boolean annotatePOS, final GrammarFormatType grammarFormatType) {

        // vertMarkov (parent annotation)
        if (!tree.isLeaf() && !grammarFormatType.isFactored(tree.contents) && (!tree.isPOS() || annotatePOS)) {
            ParseTree parent = tree.getUnfactoredParent(grammarFormatType);
            final LinkedList<String> parentsStr = new LinkedList<String>();
            for (int i = 0; i < vertMarkov; i++) {
                if (parent != null) {
                    parentsStr.addLast(grammarFormatType.getBaseNT(parent.contents));
                    parent = parent.getUnfactoredParent(grammarFormatType);
                }
            }
            if (parentsStr.size() > 0) {
                tree.contents = grammarFormatType.createParentNT(tree.contents, parentsStr);
            }
        }

        if (tree.children.size() > 2) {
            // inherit parent annotation from unfactored parent
            final String unfactoredParent = tree.children.get(0).getUnfactoredParent(grammarFormatType).contents;
            final LinkedList<ParseTree> remainingChildren = new LinkedList<ParseTree>();
            final LinkedList<String> markovChildrenStr = new LinkedList<String>();

            while (tree.children.size() > 1) {
                if (rightFactor) {
                    // split children into two lists: [a,b,c,d] => [a,b,c] [d]
                    final int n = tree.children.size();
                    final ParseTree child = tree.children.get(n - 2);
                    remainingChildren.addFirst(child);
                    if (markovChildrenStr.size() < horzMarkov) {
                        markovChildrenStr.addFirst(child.contents);
                    }
                    tree.children.remove(n - 2);
                } else {
                    // split children into two lists: [a,b,c,d] => [a] [b,c,d]
                    final ParseTree child = tree.children.get(1);
                    remainingChildren.addLast(child);
                    if (markovChildrenStr.size() < horzMarkov) {
                        markovChildrenStr.addLast(child.contents);
                    }
                    tree.children.remove(1);
                }
            }

            final String newChildStr = grammarFormatType.createFactoredNT(unfactoredParent, markovChildrenStr);
            final ParseTree newNode = new ParseTree(newChildStr, tree, remainingChildren);
            if (rightFactor) {
                tree.children.addFirst(newNode);
            } else {
                tree.children.addLast(newNode);
            }
        }

        for (final ParseTree child : tree.children) {
            binarizeTree(child, rightFactor, horzMarkov, vertMarkov, annotatePOS, grammarFormatType);
        }
    }

    /**
     * 'Un-factors' a binary-factored parse tree by removing category split labels and flattening binary-factored
     * subtrees.
     * 
     * @param bracketedTree Bracketed string parse tree
     * @param grammarFormatType Grammar format
     * @return Bracketed string representation of the un-factored tree
     */
    public static String unfactor(final String bracketedTree, final GrammarFormatType grammarFormatType) {
        final BinaryTree<String> factoredTree = BinaryTree.read(bracketedTree, String.class);
        return factoredTree.unfactor(grammarFormatType).toString();
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
