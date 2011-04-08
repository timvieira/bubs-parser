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
package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;

public class ParseTree {

    public String contents;
    public LinkedList<ParseTree> children;
    public ParseTree parent = null;
    public ParseTree leftNeighbor = null, rightNeighbor = null; // used to connect lexical and POS leaf

    // entries

    public ParseTree(final String contents) {
        this(contents, null);
    }

    public ParseTree(final String contents, final ParseTree parent) {
        this.contents = contents;
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
    public static ParseTree readBracketFormat(final String str) throws Exception {
        ParseTree child;
        final Pattern WORD = Pattern.compile("\\s*([^\\s\\(\\)]*)\\s*");
        Matcher match;

        if (isBracketFormat(str) == false) {
            throw new Exception("ERROR: Expecting tree in bracket format as input, got: " + str);
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

    public void addChild(final String child) {
        addChild(new ParseTree(child));
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

    public ParseTree getUnfactoredParent(final GrammarFormatType grammarType) {
        ParseTree parentNode = this.parent;
        while (parentNode != null && grammarType.isFactored(parentNode.contents)) {
            parentNode = parentNode.parent;
        }
        return parentNode;
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

    public void replaceLeafNodes(final String[] newLeafNodes) {
        int i = 0;
        for (final ParseTree node : getLeafNodes()) {
            node.contents = newLeafNodes[i];
            i++;
        }

        assert i == newLeafNodes.length;
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

    public LinkedList<String> getLeafNodesContent() {
        final LinkedList<String> list = new LinkedList<String>();
        for (final ParseTree node : getLeafNodes()) {
            list.add(node.contents);
        }
        return list;
    }

    public static boolean isBracketFormat(String inputStr) {
        inputStr = inputStr.trim();
        if (inputStr.length() > 0 && inputStr.startsWith("(") && inputStr.endsWith(")")) {
            return true;
        }
        return false;
    }

    // public boolean hasNodeAtSpan(ParseTree toFind, int start, int end) {
    // // instead of searching the entire tree, we should be able to find a node
    // // in O(1) by looking up [start][end] incidies
    //        
    // if (indexedBySpan == false) {
    // indexBySpan();
    // }
    //        
    // if (nodesBySpan[start][end] == null) return false;
    // for (ParseTree node : nodesBySpan[start][end]) {
    // if (toFind.contents != node.contents) return false;
    // if (toFind.children.size() != node.children.size()) return false;
    // for (int i=0; i<toFind.children.size(); i++) {
    // if (toFind.children.get(i).contents != node.children.get(i).contents) return false;
    // }
    // }
    //        
    // return true;
    // }

    // public CellChart convertToChart(final Grammar grammar) throws Exception {
    //
    // // create a len+1 by len+1 chart, build ChartEdges from tree nodes and insert
    // // them into this chart so they can be accessed in O(1) by [start][end]
    // assert this.isBinaryTree() == true;
    // assert this.parent == null; // must be root so start/end indicies make sense
    //
    // final List<ParseTree> leafNodes = this.getLeafNodes();
    // int start, end, numChildren;
    // final int sentLen = leafNodes.size();
    // boolean newProd;
    //
    // // final Chart chart = new Chart(sentLen, ArrayChartCell.class, grammar);
    // if (true) {
    // throw new RuntimeException(
    // "convetToChart() does not work anymore due to changes in Chart.  Either need to create containing Parser instance, or use something other than Chart to store the edges");
    // }
    //
    // final CellChart chart = new CellChart(sentLen, true, null);
    // Production prod = null;
    // ChartEdge edge;
    //
    // for (final ParseTree node : preOrderTraversal()) {
    // // TODO: could make this O(1) instead of O(n) ...
    // start = leafNodes.indexOf(node.leftMostLeaf());
    // end = leafNodes.indexOf(node.rightMostLeaf()) + 1;
    // numChildren = node.children.size();
    // newProd = false;
    // // System.out.println("convertToChart: node=" + node.contents + " start=" + start + " end=" + end +
    // " numChildren=" + numChildren);
    //
    // if (numChildren > 0) {
    // final String A = node.contents;
    // if (numChildren == 2) {
    // final String B = node.children.get(0).contents;
    // final String C = node.children.get(1).contents;
    // prod = grammar.getBinaryProduction(A, B, C);
    // final int midpt = leafNodes.indexOf(node.children.get(0).rightMostLeaf()) + 1;
    // edge = chart.new ChartEdge(prod, chart.getCell(start, midpt), chart.getCell(midpt, end));
    // } else if (numChildren == 1) {
    // final String B = node.children.get(0).contents;
    // if (node.isPOS()) {
    // prod = grammar.getLexicalProduction(A, B);
    // } else {
    // prod = grammar.getUnaryProduction(A, B);
    // }
    // edge = chart.new ChartEdge(prod, chart.getCell(start, end), null);
    // } else {
    // throw new Exception("ERROR: Number of node children is " + node.children.size() +
    // ".  Expecting <= 2.");
    // }
    //
    // if (prod == null) {
    // Log.info(0, "WARNING: production does not exist is grammar for node: " + A + " -> " +
    // node.childrenToString());
    // return null;
    // } else if (newProd == true) {
    // Log.info(0, "WARNING: Production " + prod.toString() + " not found in grammar.  Adding...");
    // }
    //
    // chart.getCell(start, end).updateInside(edge);
    // }
    // }
    //
    // return chart;
    // }

    public String childrenToString() {
        String str = "";
        for (final ParseTree node : children) {
            str += node.contents + " ";
        }
        return str.trim();
    }

    public void tokenizeLeaves(final Grammar grammar) throws Exception {
        for (final ParseTree leaf : getLeafNodes()) {
            if (grammar.hasWord(leaf.contents) == false) {
                leaf.contents = grammar.mapLexicalEntry(grammar.tokenizer.tokenizeToIndex(leaf.contents)[0]);
            }
        }

    }
}
