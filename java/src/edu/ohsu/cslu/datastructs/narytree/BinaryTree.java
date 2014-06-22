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
package edu.ohsu.cslu.datastructs.narytree;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.LabelParser;
import edu.ohsu.cslu.grammar.GrammarFormatType;

public class BinaryTree<E> implements Tree<E>, Serializable, Cloneable {

    private final static long serialVersionUID = 369752896212698723L;

    /** Label of the root node */
    private E label;

    /** Parent node (if any) */
    private BinaryTree<E> parent;

    private boolean visited;

    /**
     * The children of this tree
     */
    private BinaryTree<E> leftChild;
    private BinaryTree<E> rightChild;

    /** Number of nodes in this tree, including this node and any subtrees */
    private int size;

    /** Number of leaves in this tree */
    private int leaves;

    public BinaryTree(final E label, final BinaryTree<E> parent) {
        this.label = label;
        size = 1;
        leaves = 1;
        this.parent = parent;
    }

    public BinaryTree(final E label) {
        this(label, null);
    }

    public E label() {
        return label;
    }

    public BinaryTree<E> leftChild() {
        return leftChild;
    }

    public BinaryTree<E> rightChild() {
        return rightChild;
    }

    /**
     * Adds a child to the tree
     * 
     * @param childLabel label of the child
     * @return The newly added subtree
     */
    public BinaryTree<E> addChild(final E childLabel) {
        return addChild(new BinaryTree<E>(childLabel, this));
    }

    /**
     * Adds a child to the tree
     * 
     * @param child the child to add
     * @return The newly added subtree
     */
    public BinaryTree<E> addChild(final BinaryTree<E> child) {

        if (leftChild == null) {
            leftChild = child;
            updateSize(child.size, child.leaves - 1);
        } else if (rightChild == null) {
            rightChild = child;
            updateSize(child.size, child.leaves);
        } else {
            throw new RuntimeException("Binary tree is fully populated");
        }

        child.parent = this;
        return child;
    }

    public BinaryTree<E> addChild(final Tree<E> child) {
        return addChild((BinaryTree<E>) child);
    }

    @Override
    public void addChildren(final E[] childLabels) {
        for (final E child : childLabels) {
            addChild(child);
        }
    }

    @Override
    public void addChildren(final Collection<E> childLabels) {
        for (final E child : childLabels) {
            addChild(child);
        }
    }

    public boolean removeChild(final E childLabel) {
        return removeSubtree(childLabel);
    }

    @Override
    public void removeChildren(final Collection<E> childLabels) {
        for (final E childLabel : childLabels) {
            removeChild(childLabel);
        }
    }

    @Override
    public void removeChildren(final E[] childLabels) {
        for (final E childLabel : childLabels) {
            removeChild(childLabel);
        }
    }

    @Override
    public void setLabel(final E newLabel) {
        this.label = newLabel;
    }

    @Override
    public BinaryTree<E> withoutLabels(final Collection<E> labels) {
        return withoutLabels(new HashSet<E>(labels));
    }

    @Override
    public BinaryTree<E> withoutLabels(final E[] labels) {
        final HashSet<E> set = new HashSet<E>();
        for (final E l : labels) {
            set.add(l);
        }
        return withoutLabels(set);
    }

    private BinaryTree<E> withoutLabels(final HashSet<E> labelsToSkip) {
        final BinaryTree<E> newTree = new BinaryTree<E>(label);

        if (leftChild != null && !labelsToSkip.contains(leftChild.label)) {
            newTree.addChild(leftChild.withoutLabels(labelsToSkip));
        }
        if (rightChild != null && !labelsToSkip.contains(rightChild.label)) {
            newTree.addChild(rightChild.withoutLabels(labelsToSkip));
        }

        return newTree;
    }

    public boolean removeSubtree(final E childLabel) {

        if (rightChild != null && rightChild.label.equals(childLabel)) {
            removeRightSubtree();
            return true;
        } else if (leftChild != null && leftChild.label.equals(childLabel)) {
            removeLeftSubtree();
            return true;
        }
        return false;
    }

    private void removeLeftSubtree() {
        if (rightChild != null) {
            updateSize(-leftChild.size, -leftChild.leaves);
            leftChild = rightChild;
        } else {
            updateSize(-leftChild.size, -leftChild.leaves + 1);
            leftChild = null;
        }
    }

    private void removeRightSubtree() {
        updateSize(-rightChild.size, rightChild.leaves);
        rightChild = null;
    }

    public List<BinaryTree<E>> children() {
        final LinkedList<BinaryTree<E>> children = new LinkedList<BinaryTree<E>>();
        if (leftChild != null) {
            children.add(leftChild);

            if (rightChild != null) {
                children.add(rightChild);
            }
        }
        return children;
    }

    @Override
    public boolean isLeaf() {
        return leftChild == null;
    }

    public boolean isPreterminal() {
        return (leftChild != null && leftChild.leftChild == null);
    }

    public boolean isLeafOrPreterminal() {
        return (leftChild == null || leftChild.leftChild == null);
    }

    @Override
    public boolean isLeftmostChild() {
        BinaryTree<E> child = this;

        // Trace unary chains upward
        while (child.parent != null && child.parent.rightChild == null) {
            child = child.parent;
        }

        return child.parent != null && child == child.parent.leftChild;
    }

    @Override
    public boolean isRightmostChild() {
        BinaryTree<E> child = this;

        // Trace unary chains upward
        while (child.parent != null && child.parent.rightChild == null) {
            child = child.parent;
        }

        return child.parent != null && child == child.parent.rightChild;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int height() {
        if (isLeaf()) {
            return 1;
        }

        int maxChildHeight = 0;
        if (leftChild != null) {
            maxChildHeight = Math.max(maxChildHeight, leftChild.height());
        }
        if (rightChild != null) {
            maxChildHeight = Math.max(maxChildHeight, rightChild.height());
        }
        return maxChildHeight + 1;
    }

    public int depthFromRoot() {
        int depth = 0;

        for (BinaryTree<E> tree = this; tree.parent != null; tree = tree.parent) {
            depth++;
        }
        return depth;
    }

    public int leaves() {
        return leaves;
    }

    @Override
    public E[] leafLabels() {
        @SuppressWarnings("unchecked")
        final E[] leafLabels = (E[]) Array.newInstance(label.getClass(), leaves);
        int i = 0;
        for (final BinaryTree<E> leaf : leafTraversal()) {
            leafLabels[i++] = leaf.label;
        }
        return leafLabels;
    }

    public BinaryTree<E> parent() {
        return parent;
    }

    public BinaryTree<E> child(final E childLabel) {
        if (leftChild != null && leftChild.label.equals(childLabel)) {
            return leftChild;
        } else if (rightChild != null && rightChild.label.equals(childLabel)) {
            return rightChild;
        }
        return null;
    }

    @Override
    public E parentLabel() {
        return parent != null ? parent.label : null;
    }

    @Override
    public Tree<E> leftmostLeaf() {
        if (leftChild != null) {
            return leftChild.leftmostLeaf();
        }
        return this;
    }

    @Override
    public Tree<E> rightmostLeaf() {
        if (rightChild != null) {
            return rightChild.rightmostLeaf();
        }

        if (leftChild != null) {
            return leftChild.rightmostLeaf();
        }
        return this;
    }

    protected void updateSize(final int childrenAdded, final int leavesAdded) {
        size += childrenAdded;
        leaves += leavesAdded;
        if (parent != null) {
            parent.updateSize(childrenAdded, leavesAdded);
        }
    }

    @Override
    public void replaceLeafLabels(final List<E> newLabels) {
        if (newLabels.size() != leaves) {
            throw new IllegalArgumentException("Length mismatch");
        }
        final Iterator<BinaryTree<E>> leafIterator = leafTraversal().iterator();
        for (final E newLabel : newLabels) {
            leafIterator.next().label = newLabel;
        }
    }

    @Override
    public void replaceLeafLabels(final E[] newLabels) {
        if (newLabels.length != leaves) {
            throw new IllegalArgumentException("Length mismatch");
        }
        final Iterator<BinaryTree<E>> leafIterator = leafTraversal().iterator();
        for (final E newLabel : newLabels) {
            leafIterator.next().label = newLabel;
        }
    }

    @Override
    public BinaryTree<E> transform(final LabelTransformer<E> t) {
        return transform(this, t);
    }

    private BinaryTree<E> transform(final BinaryTree<E> node, final LabelTransformer<E> t) {
        final BinaryTree<E> tree = new BinaryTree<E>(t.transform(node.label));
        if (node.leftChild != null) {
            tree.addChild(transform(node.leftChild, t));
        }
        if (node.rightChild != null) {
            tree.addChild(transform(node.rightChild, t));
        }
        return tree;
    }

    @Override
    public BinaryTree<E> removeAll(final Set<E> labelsToRemove, final int maxDepth) {

        if (labelsToRemove.contains(label)) {
            if (maxDepth > height() || leftChild == null || rightChild != null) {
                throw new IllegalArgumentException("Indeterminate root node");
            }
            return leftChild.removeAll(labelsToRemove, maxDepth);
        }
        final BinaryTree<E> copy = new BinaryTree<E>(label);

        for (final BinaryTree<E> child : children()) {

            if (labelsToRemove.contains(child.label)) {
                // If child height > maxDepth, skip the child, but add the grandchildren to the new copied tree
                if (child.height() > maxDepth) {
                    for (final BinaryTree<E> grandchild : child.children()) {
                        copy.addChild(grandchild.removeAll(labelsToRemove, maxDepth));
                    }
                }
            } else {
                copy.addChild(child.removeAll(labelsToRemove, maxDepth));
            }
        }
        return copy;
    }

    @Override
    public Iterable<BinaryTree<E>> inOrderTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderList();
    }

    private List<BinaryTree<E>> inOrderList() {
        return inOrderList(new ArrayList<BinaryTree<E>>(size));
    }

    private List<BinaryTree<E>> inOrderList(final List<BinaryTree<E>> list) {

        if (leftChild != null) {
            leftChild.inOrderList(list);
        }
        list.add(this);
        if (rightChild != null) {
            rightChild.inOrderList(list);
        }

        return list;
    }

    @Override
    public Iterable<E> inOrderLabelTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderLabelList(new ArrayList<E>(size));
    }

    @Override
    public Iterable<BinaryTree<E>> preOrderTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderList(new ArrayList<BinaryTree<E>>(size));
    }

    private List<BinaryTree<E>> preOrderList(final List<BinaryTree<E>> list) {

        list.add(this);
        if (leftChild != null) {
            leftChild.preOrderList(list);
        }
        if (rightChild != null) {
            rightChild.preOrderList(list);
        }

        return list;
    }

    @Override
    public Iterable<E> preOrderLabelTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderLabelList(new ArrayList<E>(size));
    }

    @Override
    public Iterable<BinaryTree<E>> postOrderTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderList(new ArrayList<BinaryTree<E>>(size));
    }

    private List<BinaryTree<E>> postOrderList(final List<BinaryTree<E>> list) {

        if (leftChild != null) {
            leftChild.postOrderList(list);
        }
        if (rightChild != null) {
            rightChild.postOrderList(list);
        }
        list.add(this);

        return list;
    }

    @Override
    public Iterable<E> postOrderLabelTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderLabelList(new ArrayList<E>(size));
    }

    public List<BinaryTree<E>> leafList(final List<BinaryTree<E>> list) {
        for (final BinaryTree<E> node : inOrderList()) {
            if (node.isLeaf()) {
                list.add(node);
            }
        }

        return list;
    }

    @Override
    public Iterable<BinaryTree<E>> leafTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return leafList(new LinkedList<BinaryTree<E>>());
    }

    @Override
    public List<E> childLabels() {
        final ArrayList<E> list = new ArrayList<E>(2);

        if (leftChild != null) {
            list.add(leftChild.label);

            if (rightChild != null) {
                list.add(rightChild.label);
            }
        }
        list.add(label);

        return list;
    }

    private List<E> inOrderLabelList(final List<E> list) {

        if (leftChild != null) {
            leftChild.inOrderLabelList(list);
        }
        list.add(label);
        if (rightChild != null) {
            rightChild.inOrderLabelList(list);
        }

        return list;
    }

    private List<E> preOrderLabelList(final List<E> list) {

        list.add(label);
        if (leftChild != null) {
            leftChild.preOrderLabelList(list);
        }
        if (rightChild != null) {
            rightChild.preOrderLabelList(list);
        }

        return list;
    }

    private List<E> postOrderLabelList(final List<E> list) {

        if (leftChild != null) {
            leftChild.postOrderLabelList(list);
        }
        if (rightChild != null) {
            rightChild.postOrderLabelList(list);
        }
        list.add(label);

        return list;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(final boolean visited) {
        this.visited = visited;
    }

    /**
     * Returns true if this tree contains the specified label
     * 
     * @param l
     * @return true if this tree contains the specified label
     */
    public boolean containsLabel(final E l) {
        for (final E l2 : inOrderLabelTraversal()) {
            if (l2.equals(l)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The length of the longest unary chain in the tree, excluding lexical entries
     */
    public int maxUnaryChainLength() {
        // Not very efficient, but we shouldn't do this very often
        int max = 0;
        for (final BinaryTree<E> node : preOrderTraversal()) {
            final int len = node.unaryChainHeight();
            if (len > max) {
                max = len;
            }
        }
        return max;
    }

    /**
     * @return The number of nodes descending <i>directly</i> from this node in a unary chain, excluding lexical
     *         entries.
     */
    public int unaryChainHeight() {
        int length = 0;
        BinaryTree<E> node = this;
        while (node.leftChild != null && node.rightChild == null && !node.leftChild.isLeaf()) {
            length++;
            node = node.leftChild;
        }
        return length;
    }

    /**
     * @return The depth of this node in a unary chain (0 if the parent node has a right child)
     */
    public int unaryChainDepth() {
        int length = 0;
        BinaryTree<E> node = this;
        while (node.parent != null && node.parent.rightChild == null) {
            length++;
            node = node.parent;
        }
        return length;
    }

    public HashMap<String, Integer> labeledSpans(final int minHeight) {
        return labeledSpans(minHeight, null, null);
    }

    public HashMap<String, Integer> labeledSpans(final int minHeight, final Set<E> ignoredLabels,
            final Map<E, E> equivalentLabels) {

        final HashMap<String, Integer> labeledSpans = new HashMap<String, Integer>();
        addLabeledSpans(labeledSpans, this, 0, minHeight, ignoredLabels, equivalentLabels);
        return labeledSpans;
    }

    private void addLabeledSpans(final HashMap<String, Integer> labeledSpans, final BinaryTree<E> node,
            final int start, final int minHeight, final Set<E> ignoredLabels, final Map<E, E> equivalentLabels) {

        if ((minHeight <= 1 || node.height() >= minHeight)
                && (ignoredLabels == null || !ignoredLabels.contains(node.label))) {
            final String key = node.labeledSpan(start, ignoredLabels, equivalentLabels);
            if (labeledSpans.containsKey(key)) {
                labeledSpans.put(key, labeledSpans.get(key) + 1);
            } else {
                labeledSpans.put(key, 1);
            }
        }

        if (node.leftChild != null) {
            addLabeledSpans(labeledSpans, node.leftChild, start, minHeight, ignoredLabels, equivalentLabels);
        }
        if (node.rightChild != null) {
            addLabeledSpans(labeledSpans, node.rightChild, start + node.leftChild.leaves, minHeight, ignoredLabels,
                    equivalentLabels);
        }
    }

    private String labeledSpan(final int start, final Set<E> ignoredLabels, final Map<E, E> equivalentLabels) {

        final StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(start));
        sb.append(',');
        sb.append(Integer.toString(start + leaves));

        if (equivalentLabels != null && equivalentLabels.containsKey(label)) {
            sb.append(',');
            sb.append(equivalentLabels.get(label).toString());
        } else {
            if (ignoredLabels == null || !ignoredLabels.contains(label)) {
                sb.append(',');
                sb.append(label.toString());
            }
        }
        return sb.toString();
    }

    /**
     * 'Un-factors' a binary-factored parse tree by removing category split labels and flattening binary-factored
     * subtrees. Nodes matching {@link Tree#NULL_LABEL} will be omitted and flattened.
     * 
     * @param grammarFormatType Grammar format
     * @return Unfactored tree
     */
    public NaryTree<String> unfactor(final GrammarFormatType grammarFormatType) {

        final NaryTree<String> rootTree = new NaryTree<String>(grammarFormatType.getBaseNT(label.toString(), false));

        if (leftChild != null) {
            leftChild.unfactorChildren(rootTree, grammarFormatType);
        }
        if (rightChild != null) {
            rightChild.unfactorChildren(rootTree, grammarFormatType);
        }

        return rootTree;
    }

    private void unfactorChildren(final NaryTree<String> rootTree, final GrammarFormatType grammarFormatType) {

        // Don't try to unfactor leaves
        if (leftChild == null) {
            rootTree.addChild(label.toString());
            return;
        }

        // If this node is not a leaf, descend through factored categories and through any nodes matching the
        // 'omitLabels' set
        if (leftChild != null && (grammarFormatType.isFactored(label.toString()) || label.equals(NULL_LABEL))) {
            leftChild.unfactorChildren(rootTree, grammarFormatType);

            // Factored categories should _always_ have both left and right children, but when using max-constituent
            // decoding, we can occasionally mis-label a factored category as a unary parent, so we have to check.
            if (rightChild != null) {
                rightChild.unfactorChildren(rootTree, grammarFormatType);
            }
        } else {
            rootTree.addChild(unfactor(grammarFormatType));
        }
    }

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param outputStream The {@link OutputStream} to write to
     * @throws IOException if the write fails
     */
    public void write(final OutputStream outputStream) throws IOException {
        write(new OutputStreamWriter(outputStream));
    }

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param writer The {@link Writer} to write to
     * @throws IOException if the write fails
     */
    public void write(final Writer writer) throws IOException {
        writeSubtree(writer);
    }

    protected void writeSubtree(final Writer writer) throws IOException {

        if (size > 1) {
            writer.write('(');
            writer.write(label.toString());

            if (leftChild != null) {
                writer.write(' ');
                leftChild.writeSubtree(writer);
            }

            if (rightChild != null) {
                writer.write(' ');
                rightChild.writeSubtree(writer);
            }

            writer.write(')');
        } else {
            writer.write(label.toString());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public BinaryTree<E> clone() {
        try {
            return (BinaryTree<E>) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public E[] preterminalLabels() {
        @SuppressWarnings("unchecked")
        final E[] labels = (E[]) Array.newInstance(label.getClass(), leaves);
        int i = 0;
        for (final BinaryTree<E> leaf : leafTraversal()) {
            labels[i++] = leaf.parentLabel();
        }
        return labels;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof BinaryTree)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final BinaryTree<E> other = (BinaryTree<E>) o;

        if (!other.label.equals(label) || other.size != size) {
            return false;
        }

        if ((leftChild == null && other.leftChild != null) || (leftChild != null && !leftChild.equals(other.leftChild))) {
            return false;
        }

        if ((rightChild == null && other.rightChild != null)
                || (rightChild != null && !rightChild.equals(other.rightChild))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        try {
            final Writer writer = new StringWriter();
            write(writer);
            return writer.toString();
        } catch (final IOException e) {
            return "Exception in toString(): " + e.getMessage();
        }
    }

    /**
     * Reads in an binary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @param type The node label class
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> BinaryTree<T> read(final InputStream inputStream, final Class<T> type) throws IOException {
        return read(new InputStreamReader(inputStream), type);
    }

    /**
     * Reads in an binary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> BinaryTree<T> read(final InputStream inputStream, final LabelParser<T> labelParser)
            throws IOException {
        return read(new InputStreamReader(inputStream), labelParser);
    }

    /**
     * Reads in an binary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @param type The node label class
     * @return the tree
     */
    public static <T> BinaryTree<T> read(final String string, final Class<T> type) {
        try {
            return read(new StringReader(string), type);
        } catch (final IOException e) {
            // A StringReader shouldn't ever throw an IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads in an binary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     */
    public static <T> BinaryTree<T> read(final String string, final LabelParser<T> labelParser) {
        try {
            return read(new StringReader(string), labelParser);
        } catch (final IOException e) {
            // A StringReader shouldn't ever throw an IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads in an binary tree from a standard parenthesis-bracketed representation
     * 
     * @param reader The reader to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> BinaryTree<T> read(final Reader reader, final Class<T> type) throws IOException {

        try {
            final Constructor<T> labelConstructor = type.getConstructor(new Class[] { String.class });
            final LabelParser<T> labelParser = new LabelParser<T>() {
                @Override
                public T parse(final String label) throws Exception {
                    return labelConstructor.newInstance(new Object[] { label });
                }
            };
            return read(reader, labelParser);
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Recursively reads a subtree from a Reader
     * 
     * @param reader source
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> BinaryTree<T> read(final Reader reader, final LabelParser<T> labelParser) throws IOException {
        char c;

        // Discard any spaces or end-of-line characters
        while ((c = (char) reader.read()) == '\n' || c == '\r' || c == ' ') {
        }

        // We expect the first character to be '('
        if (c != '(') {
            throw new IllegalArgumentException("Bad tree format. Expected '(' but found '" + c + "'");
        }
        return readSubtree(reader, null, labelParser);
    }

    /**
     * Recursively reads a subtree from a Reader
     * 
     * @param reader source
     * @param parent parent tree (if any)
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return subtree
     * @throws IOException if the read fails
     */
    private static <T> BinaryTree<T> readSubtree(final Reader reader, final BinaryTree<T> parent,
            final LabelParser<T> labelParser) throws IOException {

        try {
            // Recursively read a tree from the reader.
            // TODO: This could probably be simplified and perhaps optimized
            final StringBuilder rootToken = new StringBuilder();

            // Read the root token
            for (char c = (char) reader.read(); c != ' ' && c != ')'; c = (char) reader.read()) {
                rootToken.append(c);
            }

            final BinaryTree<T> tree = new BinaryTree<T>(labelParser.parse(rootToken.toString()));

            StringBuilder childToken = new StringBuilder();

            // Read tokens and add children until we come to a ')'
            for (char c = (char) reader.read(); c != ')'; c = (char) reader.read()) {
                // Parse any subtrees we find
                if (c == '(') {
                    tree.addChild(readSubtree(reader, tree, labelParser));
                }
                // Add any tokens we find
                else if (c == ' ') {
                    if (childToken.length() > 0) {
                        tree.addChild(new BinaryTree<T>(labelParser.parse(childToken.toString())));
                        childToken = new StringBuilder();
                    }
                } else {
                    childToken.append(c);
                }
            }

            if (childToken.length() > 0) {
                tree.addChild(new BinaryTree<T>(labelParser.parse(childToken.toString())));
            }

            tree.parent = parent;
            return tree;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
