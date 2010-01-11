package edu.ohsu.cslu.parser.util;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ohsu.cslu.parser.ChartEdge;

public class ParseTree {

    public String contents;
    public LinkedList<ParseTree> children;
    public ParseTree parent = null;
    public ParseTree leftNeighbor = null, rightNeighbor = null; // used to connect lexical and POS leaf entries

    // public static ParseTree nullNode = new ParseTree("null");

    public ChartEdge chartEdge; // for convenience, but should probably change this later

    public ParseTree(final String contents) {
        this(contents, null);
    }

    public ParseTree(final String contents, final ParseTree parent) {
        this.contents = contents;
        this.parent = parent;
        children = new LinkedList<ParseTree>();
    }

    // TODO: this needs to be re-worked. We want to be able to print out the scores
    // for each subtree in the chart, but we also want this class not to be reliant
    // on the parser structure.
    public ParseTree(final ChartEdge edge) {
        this(edge.p.parentToString());
        this.chartEdge = edge;
    }

    public void addChild(final String child) {
        children.add(new ParseTree(child));
    }

    public void addChild(final ParseTree childTree) {
        childTree.parent = this;
        children.add(childTree);
    }

    public void linkLeavesLeftRight() {
        ParseTree lastLeaf = null, lastPOS = null;

        for (final ParseTree node : preOrderTraversal()) {
            if (node.isLeaf()) {
                if (lastLeaf != null) {
                    lastLeaf.rightNeighbor = node;
                }
                node.leftNeighbor = lastLeaf;
                lastLeaf = node;
            }

            if (node.isPOS()) {
                if (lastPOS != null) {
                    lastPOS.rightNeighbor = node;
                }
                node.leftNeighbor = lastPOS;
                lastPOS = node;
            }
        }
    }

    public String getContents() {
        return contents;
    }

    public String getParentContents() {
        if (parent == null) {
            return null;
        }
        return parent.getContents();
    }

    // opposite of toString()
    // example treebank string: (TOP (NP (NNP Two-Way) (NNP Street)))
    public static ParseTree readBracketFormat(final String str) throws Exception {
        ParseTree child;
        final Pattern WORD = Pattern.compile("\\s*([^\\s\\(\\)]*)\\s*");
        Matcher match;

        final LinkedList<ParseTree> stack = new LinkedList<ParseTree>();
        int index = 0;
        while (index < str.length()) {
            if (str.charAt(index) == '(') {
                // Beginning of a tree/subtree
                index++;
                match = WORD.matcher(str.substring(index));
                match.find();
                stack.add(new ParseTree(match.group().trim()));
                index += match.end();
            } else if (str.charAt(index) == ')') {
                // End of tree/subtree
                index++;
                while ((index < str.length()) && (str.charAt(index) == ' ')) {
                    index++;
                }
                if (stack.size() == 1) {
                    // we're either done or dead
                    if (index != str.length()) {
                        throw new Exception("Expecting end of string but found '" + str.charAt(index) + "'");
                    }
                    return stack.getLast();
                }
                // add last ParseTree as child to previous ParseTree
                child = stack.pollLast();
                stack.getLast().addChild(child);
            } else {
                // leaf node
                match = WORD.matcher(str.substring(index));
                match.find();
                stack.getLast().addChild(new ParseTree(match.group().trim()));
                index += match.end();
            }
        }
        throw new Exception();
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    public boolean isNonTerminal() {
        return (isLeaf() == false) && (isPOS() == false);
    }

    public boolean isPOS() {
        if (children.size() == 1 && isLeafParent()) {
            return true;
        }
        return false;
    }

    public boolean isLeafParent() {
        for (final ParseTree child : children) {
            if (child.isLeaf()) {
                return true;
            }
        }
        return false;
    }

    public ParseTree leftMostLeaf() {
        if (isLeaf()) {
            return this;
        }
        return children.getFirst().leftMostLeaf();
    }

    public ParseTree leftMostPOS() {
        return leftMostLeaf().parent;
    }

    public ParseTree rightMostLeaf() {
        if (isLeaf()) {
            return this;
        }
        return children.getLast().rightMostLeaf();
    }

    public ParseTree rightMostPOS() {
        return rightMostLeaf().parent;
    }

    public ParseTree leftBoundaryPOS() {
        return leftMostPOS().leftNeighbor;
    }

    public String leftBoundaryPOSContents() {
        final ParseTree pos = leftBoundaryPOS();
        if (pos == null) {
            return null;
        }
        return pos.contents;
    }

    public ParseTree rightBoundaryPOS() {
        return rightMostPOS().rightNeighbor;
    }

    public String rightBoundaryPOSContents() {
        final ParseTree pos = rightBoundaryPOS();
        if (pos == null) {
            return null;
        }
        return pos.contents;
    }

    public LinkedList<ParseTree> postOrderTraversal() {
        final LinkedList<ParseTree> nodes = new LinkedList<ParseTree>();

        for (final ParseTree child : children) {
            nodes.addAll(child.postOrderTraversal());
        }
        nodes.addLast(this);

        return nodes;
    }

    public LinkedList<ParseTree> preOrderTraversal() {
        final LinkedList<ParseTree> nodes = new LinkedList<ParseTree>();

        nodes.addLast(this);
        for (final ParseTree child : children) {
            nodes.addAll(child.preOrderTraversal());
        }
        return nodes;
    }

    public String toBracketFormat() {
        return toString();
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(final boolean printInsideProb) {
        if (isLeaf()) {
            return contents;
        }

        String s = "(" + contents;
        if (printInsideProb == true && chartEdge != null) {
            s += " " + Double.toString(chartEdge.insideProb);
        }

        for (final ParseTree child : children) {
            s += " " + child.toString(printInsideProb);
        }
        s += ")";
        return s;
    }

    public boolean isBinaryTree() {
        boolean isBinary = (children.size() <= 2);
        for (final ParseTree child : children) {
            isBinary = isBinary && child.isBinaryTree();
        }
        return isBinary;
    }

    public void replaceLeafNodes(final String[] newLeafNodes) {
        int i = 0;
        for (final ParseTree node : preOrderTraversal()) {
            if (node.isLeaf()) {
                node.contents = newLeafNodes[i];
                i++;
            }
        }

        assert i == newLeafNodes.length;
    }
}
