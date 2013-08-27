package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;

import edu.berkeley.nlp.syntax.Tree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;

/**
 * Class which contains code for annotating and binarizing trees for the parser's use, and debinarizing and unannotating
 * them for scoring.
 */
public class TreeAnnotations implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Binarize a tree with the given binarization style; e.g. head binarization, left binarization, etc.
     * 
     * @param tree
     * @param binarization The type of binarization used.
     * @return Binarized tree
     */
    public static Tree<String> binarizeTree(final Tree<String> tree, final Binarization binarization) {
        switch (binarization) {
        case LEFT:
            return leftBinarizeTree(tree);
        case RIGHT:
            return rightBinarizeTree(tree);
        }
        return null;
    }

    static Tree<String> forgetLabels(final Tree<String> tree, final int nHorizontalAnnotation) {
        if (nHorizontalAnnotation == -1)
            return tree;
        String transformedLabel = tree.label();
        if (tree.isLeaf()) {
            return new Tree<String>(transformedLabel);
        }
        // the location of the farthest _
        int firstCutIndex = transformedLabel.indexOf('_');
        final int keepBeginning = firstCutIndex;
        // will become -1 when the end of the line is reached
        int secondCutIndex = transformedLabel.indexOf('_', firstCutIndex + 1);
        // the location of the second farthest _
        int cutIndex = secondCutIndex;
        while (secondCutIndex != -1) {
            cutIndex = firstCutIndex;
            firstCutIndex = secondCutIndex;
            secondCutIndex = transformedLabel.indexOf('_', firstCutIndex + 1);
        }

        if (nHorizontalAnnotation == 0) {
            cutIndex = transformedLabel.indexOf('>') - 1;
            if (cutIndex > 0)
                transformedLabel = transformedLabel.substring(0, cutIndex);
        } else if (cutIndex > 0 && !tree.isLeaf()) {
            if (nHorizontalAnnotation == 2) {
                transformedLabel = transformedLabel.substring(0, keepBeginning) + transformedLabel.substring(cutIndex);
            } else if (nHorizontalAnnotation == 1) {
                transformedLabel = transformedLabel.substring(0, keepBeginning)
                        + transformedLabel.substring(firstCutIndex);
            } else {
                throw new Error("code does not exist to horizontally annotate at level " + nHorizontalAnnotation);
            }
        }
        final ArrayList<Tree<String>> transformedChildren = new ArrayList<Tree<String>>();
        for (final Tree<String> child : tree.children()) {
            transformedChildren.add(forgetLabels(child, nHorizontalAnnotation));
        }

        return new Tree<String>(transformedLabel, transformedChildren);
    }

    static Tree<String> rightBinarizeTree(final Tree<String> tree) {
        final String label = tree.label();
        final ArrayList<Tree<String>> children = tree.children();
        if (tree.isLeaf()) {
            return new Tree<String>(label);
        }

        if (children.size() == 1) {
            final ArrayList<Tree<String>> newChildren = new ArrayList<Tree<String>>(1);
            newChildren.add(rightBinarizeTree(children.get(0)));
            return new Tree<String>(label, newChildren);
        }
        // otherwise, it's a binary-or-more local tree, so decompose it into a
        // sequence of binary and unary trees.
        final String intermediateLabel = "@" + label + "->";
        final Tree<String> intermediateTree = rightBinarizeTreeHelper(tree, 0, intermediateLabel);
        return new Tree<String>(label, intermediateTree.children());
    }

    private static Tree<String> rightBinarizeTreeHelper(final Tree<String> tree, final int numChildrenGenerated,
            final String intermediateLabel) {
        final Tree<String> leftTree = tree.children().get(numChildrenGenerated);
        final ArrayList<Tree<String>> children = new ArrayList<Tree<String>>(2);
        children.add(rightBinarizeTree(leftTree));
        if (numChildrenGenerated == tree.children().size() - 2) {
            children.add(rightBinarizeTree(tree.children().get(numChildrenGenerated + 1)));
        } else if (numChildrenGenerated < tree.children().size() - 2) {
            final Tree<String> rightTree = rightBinarizeTreeHelper(tree, numChildrenGenerated + 1, intermediateLabel
                    + "_" + leftTree.label());
            children.add(rightTree);
        }
        return new Tree<String>(intermediateLabel, children);
    }

    static Tree<String> leftBinarizeTree(final Tree<String> tree) {
        final String label = tree.label();
        final ArrayList<Tree<String>> children = tree.children();
        if (tree.isLeaf())
            return new Tree<String>(label);
        else if (children.size() == 1) {
            final ArrayList<Tree<String>> newChildren = new ArrayList<Tree<String>>(1);
            newChildren.add(leftBinarizeTree(children.get(0)));
            return new Tree<String>(label, newChildren);
        }
        // otherwise, it's a binary-or-more local tree, so decompose it into a
        // sequence of binary and unary trees.
        final String intermediateLabel = "@" + label + "->";
        final Tree<String> intermediateTree = leftBinarizeTreeHelper(tree, children.size() - 1, intermediateLabel);
        return new Tree<String>(label, intermediateTree.children());
    }

    private static Tree<String> leftBinarizeTreeHelper(final Tree<String> tree, final int numChildrenLeft,
            final String intermediateLabel) {
        final Tree<String> rightTree = tree.children().get(numChildrenLeft);
        final ArrayList<Tree<String>> children = new ArrayList<Tree<String>>(2);
        if (numChildrenLeft == 1) {
            children.add(leftBinarizeTree(tree.children().get(numChildrenLeft - 1)));
        } else if (numChildrenLeft > 1) {
            final Tree<String> leftTree = leftBinarizeTreeHelper(tree, numChildrenLeft - 1, intermediateLabel + "_"
                    + rightTree.label());
            children.add(leftTree);
        }
        children.add(leftBinarizeTree(rightTree));
        return new Tree<String>(intermediateLabel, children);
    }
}
