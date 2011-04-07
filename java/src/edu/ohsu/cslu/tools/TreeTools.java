/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.parser.ParseTree;

public class TreeTools extends BaseCommandlineTool {

    @Option(name = "-binarize", usage = "Binarize input trees")
    private boolean binarize = false;

    @Option(name = "-rightFactor", usage = "Right factor trees; used with -binarize.  Default=leftFactor")
    private boolean rightFactor = false;

    @Option(name = "-hMarkov", usage = "Target horizontal Markov order; used with -binarize")
    private int horizontalMarkov = 0;

    @Option(name = "-vMarkov", usage = "Target vertical Markov order; used with -binarize")
    private int verticalMarkov = 0;

    @Option(name = "-annotatePOS", usage = "Add vertical markov annotations to POS labels; used with -binarize")
    private boolean annotatePOS = false;

    @Option(name = "-unbinarize", usage = "Unbinarize input trees")
    private boolean unbinarize = false;

    @Option(name = "-grammarFormat", usage = "Grammar format of input trees to be unbinarized")
    private GrammarFormatType grammarFormat = GrammarFormatType.CSLU;

    @Option(name = "-cleanRawTreebank", usage = "Remove TMP labels, empty nodes, and X=>X unaries")
    private boolean cleanRawTreebank = false;

    @Option(name = "-countMaxSpans", usage = "Count the longest span that starts and ends at each non-terminal")
    private boolean countMaxSpans = false;

    @Option(name = "-extractUnaries", usage = "Extract unary productions from trees")
    private boolean extractUnaries = false;

    @Option(name = "-minBE", usage = "Count minimum begin and end constraints for each sentence")
    private boolean minimumBeginEndConstraints = false;

    private static BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

    // private static BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {

        for (String sentence = inputStream.readLine(); sentence != null; sentence = inputStream.readLine()) {

            final ParseTree tree = ParseTree.readBracketFormat(sentence);

            if (unbinarize) {
                // outputStream.write(sentence);
                unbinarizeTree(tree, grammarFormat);
            } else if (cleanRawTreebank) {
                // removeTmpLabels(tree);
                // removeEmptyNodes(tree);
                // removeSpuriousUnaries(tree);
                removeTmpLabelsEmptyNodesAndSpuriousUnaries(tree);
            } else if (binarize) {
                binarizeTree(tree, rightFactor, horizontalMarkov, verticalMarkov, annotatePOS, grammarFormat);
            } else if (countMaxSpans) {
                countMaxSpanPerWord();
            } else if (minimumBeginEndConstraints) {
                countMinBeginEndConstraints(tree);
            } else if (extractUnaries) {
                extractUnariesFromTree(tree);
            } else {
                System.err.println("ERROR: action required.  See -h");
                System.exit(1);
            }

            // this wasn't writing anything to stdout???
            // outputStream.write(tree.toString());
            if (extractUnaries == false) {
                System.out.println(tree.toString());
            }
        }
    }

    public static void extractUnariesFromTree(final ParseTree tree) {
        boolean unaryChain = false;
        for (final ParseTree node : tree.preOrderTraversal()) {
            // if (node.isBinaryTree() == false && (node.parent == null || node.parent.isBinaryTree())) {
            if (node.isPOS() == false && node.children.size() == 1) {
                unaryChain = true;
                // System.out.println(node.toString());
                System.out.print(node.getContents() + " -> " + node.childrenToString() + " ");
            } else {
                if (unaryChain == true) {
                    System.out.println();
                }
                unaryChain = false;
            }
        }
        if (unaryChain == true) {
            System.out.println();
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

    public static void countMaxSpanPerWord() throws Exception {
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

    private static void countMinBeginEndConstraints(final ParseTree tree) {
        final int n = tree.getLeafNodes().size();
        int i = 0;
        for (final ParseTree leaf : tree.getLeafNodes()) {
            leaf.contents = new Integer(i).toString();
            i += 1;
        }

        final boolean start[] = new boolean[n + 1];
        final boolean end[] = new boolean[n + 1];
        for (final ParseTree node : tree.preOrderTraversal()) {
            if (node.isNonTerminal()) {
                final int leftIndex = Integer.parseInt(node.rightMostLeaf().contents);
                final int rightIndex = Integer.parseInt(node.leftMostLeaf().contents) + 1;
                // int span = rightIndex - leftIndex;
                start[leftIndex] = true;
                end[rightIndex] = false;
            }
        }
        System.out.println("n=" + n + "\n" + "B:" + boolarray2str(start) + "\nE:" + boolarray2str(end));
    }

    public static String boolarray2str(final boolean[] x) {
        String s = "";
        for (int i = 0; i < x.length; i++) {
            if (x[i]) {
                s += " 1";
            } else {
                s += " 0";
            }
        }
        return s;
    }

}
