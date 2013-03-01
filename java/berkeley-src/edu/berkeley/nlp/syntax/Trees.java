package edu.berkeley.nlp.syntax;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import edu.berkeley.nlp.util.Filter;

/**
 * Tools for displaying, reading, and modifying trees.
 * 
 * @author Dan Klein
 */
public class Trees {

    public static interface TreeTransformer<E> {
        Tree<E> transformTree(Tree<E> tree);
    }

    public static class FunctionNodeStripper implements TreeTransformer<String> {
        public Tree<String> transformTree(final Tree<String> tree) {
            final String transformedLabel = transformLabel(tree);
            if (tree.isLeaf()) {
                return new Tree<String>(tree.label);
            }
            final List<Tree<String>> transformedChildren = new ArrayList<Tree<String>>();
            for (final Tree<String> child : tree.children()) {
                transformedChildren.add(transformTree(child));
            }
            return new Tree<String>(transformedLabel, transformedChildren);
        }

        /**
         * @param tree
         * @return The transformation of the specified tree's label
         */
        public static String transformLabel(final Tree<String> tree) {
            String transformedLabel = tree.label();
            int cutIndex = transformedLabel.indexOf('-');
            int cutIndex2 = transformedLabel.indexOf('=');
            final int cutIndex3 = transformedLabel.indexOf('^');
            if (cutIndex3 > 0 && (cutIndex3 < cutIndex2 || cutIndex2 == -1))
                cutIndex2 = cutIndex3;
            if (cutIndex2 > 0 && (cutIndex2 < cutIndex || cutIndex <= 0))
                cutIndex = cutIndex2;
            if (cutIndex > 0 && !tree.isLeaf()) {
                transformedLabel = new String(transformedLabel.substring(0, cutIndex));
            }
            return transformedLabel;
        }
    }

    public static class EmptyNodeStripper implements TreeTransformer<String> {
        public Tree<String> transformTree(final Tree<String> tree) {
            final String label = tree.label();
            if (label.equals("-NONE-")) {
                return null;
            }
            if (tree.isLeaf()) {
                return new Tree<String>(label);
            }
            final List<Tree<String>> children = tree.children();
            final List<Tree<String>> transformedChildren = new ArrayList<Tree<String>>();
            for (final Tree<String> child : children) {
                final Tree<String> transformedChild = transformTree(child);
                if (transformedChild != null)
                    transformedChildren.add(transformedChild);
            }
            if (transformedChildren.size() == 0)
                return null;
            return new Tree<String>(label, transformedChildren);
        }
    }

    public static class XOverXRemover<E> implements TreeTransformer<E> {
        public Tree<E> transformTree(final Tree<E> tree) {
            final E label = tree.label();
            List<Tree<E>> children = tree.children();
            while (children.size() == 1 && !children.get(0).isLeaf() && label.equals(children.get(0).label())) {
                children = children.get(0).children();
            }
            final List<Tree<E>> transformedChildren = new ArrayList<Tree<E>>();
            for (final Tree<E> child : children) {
                transformedChildren.add(transformTree(child));
            }
            return new Tree<E>(label, transformedChildren);
        }
    }

    public static class StandardTreeNormalizer implements TreeTransformer<String> {
        EmptyNodeStripper emptyNodeStripper = new EmptyNodeStripper();
        XOverXRemover<String> xOverXRemover = new XOverXRemover<String>();
        FunctionNodeStripper functionNodeStripper = new FunctionNodeStripper();

        public Tree<String> transformTree(Tree<String> tree) {
            tree = functionNodeStripper.transformTree(tree);
            tree = emptyNodeStripper.transformTree(tree);
            tree = xOverXRemover.transformTree(tree);
            return tree;
        }
    }

    public static class PennTreeReader implements Iterator<Tree<String>> {
        public static String ROOT_LABEL = "ROOT";

        PushbackReader in;
        Tree<String> nextTree;
        int num = 0;
        int treeNum = 0;

        private boolean lowercase = false;

        public boolean hasNext() {
            return (nextTree != null);
        }

        public Tree<String> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            final Tree<String> tree = nextTree;
            // System.out.println(tree);
            nextTree = readRootTree();

            return tree;
        }

        private Tree<String> readRootTree() {
            try {
                readWhiteSpace();
                if (!isLeftParen(peek()))
                    return null;
                treeNum++;
                return readTree(true);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

        }

        private Tree<String> readTree(final boolean isRoot) throws IOException {
            readLeftParen();
            String label = readLabel();
            if (label.length() == 0 && isRoot)
                label = ROOT_LABEL;
            if (isRightParen(peek())) {
                // special case where terminal item is surround by brackets e.g.
                // '(1)'
                readRightParen();
                return new Tree<String>(label);
            }
            final List<Tree<String>> children = readChildren();
            readRightParen();
            if (!lowercase || children.size() > 0) {
                return new Tree<String>(label, children);
            }
            return new Tree<String>(label.toLowerCase().intern(), children);
        }

        private String readLabel() throws IOException {
            readWhiteSpace();
            return readText(false);
        }

        private String readText(boolean atLeastOne) throws IOException {
            final StringBuilder sb = new StringBuilder();
            int ch = in.read();
            while (atLeastOne || (!isWhiteSpace(ch) && !isLeftParen(ch) && !isRightParen(ch) && ch != -1)) {
                sb.append((char) ch);
                ch = in.read();
                atLeastOne = false;
            }

            in.unread(ch);
            return sb.toString().intern();
        }

        private List<Tree<String>> readChildren() throws IOException {
            readWhiteSpace();
            final List<Tree<String>> children = new ArrayList<Tree<String>>();
            while (!isRightParen(peek()) || children.size() == 0) {
                readWhiteSpace();
                if (isLeftParen(peek())) {
                    if (isTextParen()) {
                        children.add(readLeaf());
                    } else {
                        children.add(readTree(false));
                    }
                } else if (peek() == 65535) {
                    throw new RuntimeException("Unmatched parentheses in tree input.");
                } else {
                    children.add(readLeaf());
                }
                readWhiteSpace();
            }
            return children;
        }

        private boolean isTextParen() throws IOException {
            final int next = in.read();
            final int postnext = in.read();
            final boolean isText = isLeftParen(next) && isRightParen(postnext);
            in.unread(postnext);
            in.unread(next);
            return isText;
        }

        private int peek() throws IOException {
            final int ch = in.read();
            in.unread(ch);
            return ch;
        }

        private Tree<String> readLeaf() throws IOException {
            String label = readText(true);
            if (lowercase)
                label = label.toLowerCase();
            return new Tree<String>(label.intern());
        }

        private void readLeftParen() throws IOException {
            // System.out.println("Read left.");
            readWhiteSpace();
            final int ch = in.read();
            if (!isLeftParen(ch))
                throw new RuntimeException("Format error reading tree.");
        }

        private void readRightParen() throws IOException {
            // System.out.println("Read right.");
            readWhiteSpace();
            final int ch = in.read();
            if (!isRightParen(ch))
                throw new RuntimeException("Format error reading tree.");
        }

        private void readWhiteSpace() throws IOException {
            int ch = in.read();
            while (isWhiteSpace(ch)) {
                ch = in.read();
            }
            in.unread(ch);
        }

        private boolean isWhiteSpace(final int ch) {
            return (ch == ' ' || ch == '\t' || ch == '\f' || ch == '\r' || ch == '\n');
        }

        private boolean isLeftParen(final int ch) {
            return ch == '(';
        }

        private boolean isRightParen(final int ch) {
            return ch == ')';
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public PennTreeReader(final Reader in) {
            this(in, false);
        }

        public PennTreeReader(final Reader in, final boolean lowercase) {
            this.lowercase = lowercase;
            this.in = new PushbackReader(in, 2);
            nextTree = readRootTree();
        }

        /**
         * Reads a tree on a single line and returns null if there was a problem.
         * 
         * @param lowercase
         */
        public static Tree<String> parseEasy(final String treeString, final boolean lowercase) {
            try {
                return parseHard(treeString, lowercase);
            } catch (final RuntimeException e) {
                return null;
            }
        }

        /**
         * Reads a tree on a single line and returns null if there was a problem.
         * 
         * @param treeString
         */
        public static Tree<String> parseEasy(final String treeString) {
            return parseEasy(treeString, false);
        }

        private static Tree<String> parseHard(final String treeString, final boolean lowercase) {
            final StringReader sr = new StringReader(treeString);
            final PennTreeReader reader = new Trees.PennTreeReader(sr, lowercase);
            return reader.next();
        }
    }

    /**
     * Renderer for pretty-printing trees according to the Penn Treebank indenting guidelines (mutliline). Adapted from
     * code originally written by Dan Klein and modified by Chris Manning.
     */
    public static class PennTreeRenderer {

        /**
         * Print the tree as done in Penn Treebank merged files. The formatting should be exactly the same, but we don't
         * print the trailing whitespace found in Penn Treebank trees. The basic deviation from a bracketed indented
         * tree is to in general collapse the printing of adjacent preterminals onto one line of tags and words.
         * Additional complexities are that conjunctions (tag CC) are not collapsed in this way, and that the unlabeled
         * outer brackets are collapsed onto the same line as the next bracket down.
         */
        public static <L> String render(final Tree<L> tree) {
            final StringBuilder sb = new StringBuilder();
            renderTree(tree, 0, false, false, false, true, sb);
            sb.append('\n');
            return sb.toString();
        }

        /**
         * Display a node, implementing Penn Treebank style layout
         */
        private static <L> void renderTree(final Tree<L> tree, final int indent, final boolean parentLabelNull,
                final boolean firstSibling, final boolean leftSiblingPreTerminal, final boolean topLevel,
                final StringBuilder sb) {
            // the condition for staying on the same line in Penn Treebank
            final boolean suppressIndent = (parentLabelNull || (firstSibling && tree.isPreTerminal()) || (leftSiblingPreTerminal
                    && tree.isPreTerminal() && (tree.label() == null || !tree.label().toString().startsWith("CC"))));
            if (suppressIndent) {
                sb.append(' ');
            } else {
                if (!topLevel) {
                    sb.append('\n');
                }
                for (int i = 0; i < indent; i++) {
                    sb.append("  ");
                }
            }
            if (tree.isLeaf() || tree.isPreTerminal()) {
                renderFlat(tree, sb);
                return;
            }
            sb.append('(');
            sb.append(tree.label());
            renderChildren(tree.children(), indent + 1, tree.label() == null || tree.label().toString() == null, sb);
            sb.append(')');
        }

        private static <L> void renderFlat(final Tree<L> tree, final StringBuilder sb) {
            if (tree.isLeaf()) {
                sb.append(tree.label().toString());
                return;
            }
            sb.append('(');
            if (tree.label() == null)
                sb.append("<null>");
            else
                sb.append(tree.label().toString());
            sb.append(' ');
            sb.append(tree.children().get(0).label().toString());
            sb.append(')');
        }

        private static <L> void renderChildren(final List<Tree<L>> children, final int indent,
                final boolean parentLabelNull, final StringBuilder sb) {
            boolean firstSibling = true;
            boolean leftSibIsPreTerm = true; // counts as true at beginning
            for (final Tree<L> child : children) {
                renderTree(child, indent, parentLabelNull, firstSibling, leftSibIsPreTerm, false, sb);
                leftSibIsPreTerm = child.isPreTerminal();
                // CC is a special case
                if (child.label() != null && child.label().toString().startsWith("CC")) {
                    leftSibIsPreTerm = false;
                }
                firstSibling = false;
            }
        }
    }

    /**
     * Splices out all nodes which match the provided filter.
     * 
     * @param tree
     * @param filter
     * @return A new tree with all matching nodes spliced
     */
    public static <L> Tree<L> spliceNodes(final Tree<L> tree, final Filter<L> filter) {
        return spliceNodes(tree, filter, true);
    }

    private static <L> Tree<L> spliceNodes(final Tree<L> tree, final Filter<L> filter, final boolean splice) {
        final List<Tree<L>> rootList = spliceNodesHelper(tree, filter, splice);
        if (rootList.size() > 1)
            throw new IllegalArgumentException("spliceNodes: no unique root after splicing");
        if (rootList.size() < 1)
            return null;
        return rootList.get(0);
    }

    private static <L> List<Tree<L>> spliceNodesHelper(final Tree<L> tree, final Filter<L> filter, final boolean splice) {
        final List<Tree<L>> splicedChildren = new ArrayList<Tree<L>>();
        for (final Tree<L> child : tree.children()) {
            final List<Tree<L>> splicedChildList = spliceNodesHelper(child, filter, splice);
            splicedChildren.addAll(splicedChildList);
        }

        if (filter.accept(tree.label()) && !tree.isLeaf())
            return splice ? splicedChildren : new ArrayList<Tree<L>>();
        // assert !(tree.getLabel().equals("NP") && splicedChildren.isEmpty());
        final Tree<L> newTree = new Tree<L>(tree.label);
        newTree.setChildren(splicedChildren);
        return Collections.singletonList(newTree);
    }

    public static Tree<String> stripLeaves(final Tree<String> tree) {
        if (tree.isLeaf()) {
            throw new RuntimeException("Can't strip leaves from " + tree.toString());
        }
        if (tree.children().get(0).isLeaf()) {
            // Base case; preterminals become terminals.
            return new Tree<String>(tree.label());
        }

        final List<Tree<String>> children = new ArrayList<Tree<String>>();
        final Tree<String> newTree = new Tree<String>(tree.label());
        for (final Tree<String> child : tree.children()) {
            children.add(stripLeaves(child));
        }
        newTree.setChildren(children);
        return newTree;
    }

    public static <T> int getMaxBranchingFactor(final Tree<T> tree) {
        int max = tree.children().size();
        for (final Tree<T> child : tree.children()) {
            max = Math.max(max, getMaxBranchingFactor(child));
        }
        return max;
    }

    public interface LabelTransformer<S, T> {
        public T transform(Tree<S> node);
    }

    public static <S, T> Tree<T> transformLabels(final Tree<S> origTree, final LabelTransformer<S, T> labelTransform) {
        final T newLabel = labelTransform.transform(origTree);
        if (origTree.isLeaf()) {
            return new Tree<T>(newLabel);
        }
        final List<Tree<T>> children = new ArrayList<Tree<T>>();
        for (final Tree<S> child : origTree.children()) {
            children.add(transformLabels(child, labelTransform));
        }
        return new Tree<T>(newLabel, children);
    }
}
