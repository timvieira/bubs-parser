/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ohsu.cslu.grammar.GrammarFormatType;

public class ParseTree {

    public String label;
    public LinkedList<ParseTree> children;
    public ParseTree parent = null;
    // TODO Can we compute these at runtime when needed?
    public ParseTree leftNeighbor = null, rightNeighbor = null; // used to connect lexical and POS leaf

    // entries

    public ParseTree(final String contents) {
        this(contents, null);
    }

    public ParseTree(final String label, final ParseTree parent) {
        this.label = label;
        this.parent = parent;
        children = new LinkedList<ParseTree>();
    }

    public ParseTree(final String contents, final ParseTree parent, final LinkedList<ParseTree> children) {
        this(contents, parent);
        this.children = children;
        for (final ParseTree child : children) {
            child.parent = this;
        }
    }

    // opposite of toString()
    // example treebank string: (TOP (NP (NNP Two-Way) (NNP Street)))
    public static ParseTree readBracketFormat(final String str) {
        ParseTree child;
        final Pattern WORD = Pattern.compile("\\s*([^\\s\\(\\)]*)\\s*");
        Matcher match;

        if (isBracketFormat(str) == false) {
            throw new RuntimeException("ERROR: Expecting tree in bracket format as input, got: " + str);
        }

        int numOpen = 0, numClose = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '(')
                numOpen++;
            if (str.charAt(i) == ')')
                numClose++;
        }
        if (numOpen != numClose) {
            throw new RuntimeException("ERROR: parens not balanced; found " + numOpen + " open and " + numClose
                    + " close braces.");
        }

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
                        throw new RuntimeException("Expecting end of string but found '" + str.charAt(index) + "'");
                    }
                    stack.getLast().linkLeavesLeftRight();
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
        throw new RuntimeException();
    }

    private void addChild(final ParseTree childTree) {
        childTree.parent = this;
        children.add(childTree);
    }

    // TODO Remove if we can compute at runtime
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

    public String label() {
        return label;
    }

    // TODO Add as convenience method on Tree<E>
    public ParseTree getUnfactoredParent(final GrammarFormatType grammarType) {
        ParseTree parentNode = this.parent;
        while (parentNode != null && grammarType.isFactored(parentNode.label)) {
            parentNode = parentNode.parent;
        }
        return parentNode;
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    // TODO Add as convenience method on Tree<E>
    public boolean isLeafOrPreterminal() {
        return isLeaf() || isPOS();
    }

    // TODO Use leaves() == 1 && height() == 2 methods on Tree<E>
    public boolean isPOS() {
        if (children.size() == 1 && isLeafParent()) {
            return true;
        }
        return false;
    }

    // TODO Use height() == 2 on Tree<E>
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

    // TODO Inline in consumers
    public ParseTree leftMostPOS() {
        return leftMostLeaf().parent;
    }

    public ParseTree rightMostLeaf() {
        if (isLeaf()) {
            return this;
        }
        return children.getLast().rightMostLeaf();
    }

    // TODO Inline in consumers
    public ParseTree rightMostPOS() {
        return rightMostLeaf().parent;
    }

    // TODO Inline in consumers
    public ParseTree leftBoundaryPOS() {
        return leftMostPOS().leftNeighbor;
    }

    // TODO Inline in consumers
    public String leftBoundaryPOSContents() {
        final ParseTree pos = leftBoundaryPOS();
        if (pos == null) {
            return null;
        }
        return pos.label;
    }

    // TODO Inline in consumers
    public String rightBoundaryPOSContents() {
        final ParseTree pos = rightMostLeaf().parent;
        if (pos == null) {
            return null;
        }
        return pos.label;
    }

    public LinkedList<ParseTree> preOrderTraversal() {
        final LinkedList<ParseTree> nodes = new LinkedList<ParseTree>();

        nodes.addLast(this);
        for (final ParseTree child : children) {
            nodes.addAll(child.preOrderTraversal());
        }
        return nodes;
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(final boolean printInsideProb) {
        if (isLeaf()) {
            return label;
        }

        String s = "(" + label;
        // TODO: print NT score here if desired

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

    // TODO Add as convenience method on Tree<E>
    public void replaceLeafNodes(final String[] newLeafNodes) {
        int i = 0;
        for (final ParseTree node : getLeafNodes()) {
            node.label = newLeafNodes[i];
            i++;
        }

        assert i == newLeafNodes.length;
    }

    public int span() {
        final int n = getLeafNodes().size();
        if (n == 0) {
            return 1;
        }
        return n;
    }

    public LinkedList<ParseTree> getLeafNodes() {
        final LinkedList<ParseTree> list = new LinkedList<ParseTree>();
        for (final ParseTree node : preOrderTraversal()) {
            if (node.isLeaf()) {
                list.add(node);
            }
        }
        return list;
    }

    // TODO Inline in consumers
    public LinkedList<String> getLeafNodesContent() {
        final LinkedList<String> list = new LinkedList<String>();
        for (final ParseTree node : getLeafNodes()) {
            list.add(node.label);
        }
        return list;
    }

    // TODO Inline in consumer (ParseResult). Probably with a separate constructor for result when parsing from tree
    // input
    public static boolean isBracketFormat(String inputStr) {
        inputStr = inputStr.trim();
        if (inputStr.length() > 0 && inputStr.startsWith("(") && inputStr.endsWith(")")) {
            return true;
        }
        return false;
    }

    // TODO Inline in consumers
    public String childrenToString() {
        String str = "";
        for (final ParseTree node : children) {
            str += node.label + " ";
        }
        return str.trim();
    }
}
