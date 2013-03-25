/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.datastructs.narytree;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

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

import edu.ohsu.cslu.grammar.GrammarFormatType;

/**
 * Generic NaryTree implementation. The interface is defined recursively (children of an {@link NaryTree} are also
 * {@link NaryTree}s, rather than defining an explicit Node class)
 * 
 * @author Aaron Dunlop
 * @since Sep 19, 2008
 * 
 *        $Id$
 */
public class NaryTree<E> implements Tree<E>, Serializable {

    private final static long serialVersionUID = 369752896212698723L;

    /** Label of the root node */
    private E label;

    /** Parent node (if any) */
    protected NaryTree<E> parent;

    /**
     * All the children of this tree
     */
    protected final LinkedList<NaryTree<E>> childList = new LinkedList<NaryTree<E>>();

    /** Number of nodes in this tree, including this node and any subtrees */
    protected int size;

    /** Number of leaf nodes in this tree */
    protected int leaves;

    /** True if this node has been set as 'visited' */
    private boolean visited;

    public NaryTree(final E label, final NaryTree<E> parent) {
        this.label = label;
        size = 1;
        leaves = 1;
        this.parent = parent;
    }

    public NaryTree(final E label) {
        this(label, null);
    }

    /**
     * Adds a child to the tree
     * 
     * @param childLabel label of the child
     * @return The newly added subtree
     */
    public NaryTree<E> addChild(final E childLabel) {
        return addChild(new NaryTree<E>(childLabel));
    }

    @Override
    public E label() {
        return label;
    }

    @Override
    public NaryTree<E> addChild(final Tree<E> child) {
        return addChild((NaryTree<E>) child);
    }

    public NaryTree<E> addChild(final NaryTree<E> child) {
        final int leavesAdded = child.leaves - (isLeaf() ? 1 : 0);
        childList.add(child);
        child.parent = this;
        updateSize(child.size, leavesAdded);
        return child;
    }

    @Override
    public void addChildren(final Collection<E> childLabels) {
        for (final E child : childLabels) {
            addChild(child);
        }
    }

    @Override
    public void addChildren(final E[] childLabels) {
        for (final E child : childLabels) {
            addChild(child);
        }
    }

    public void addChildNodes(final Collection<NaryTree<E>> children) {
        for (final NaryTree<E> child : children) {
            addChild(child);
        }
    }

    @Override
    public List<E> childLabels() {
        final LinkedList<E> childLabels = new LinkedList<E>();
        for (final NaryTree<E> child : childList) {
            childLabels.add(child.label);
        }
        return childLabels;
    }

    public boolean removeChild(final E childLabel) {
        for (final Iterator<NaryTree<E>> iter = childList.iterator(); iter.hasNext();) {
            final NaryTree<E> child = iter.next();
            if (child.label.equals(childLabel)) {
                final int leavesRemoved = child.leaves - (childList.size() == 1 ? 1 : 0);

                iter.remove();
                child.parent = null;
                updateSize(-child.size, -leavesRemoved);
                return true;
            }
        }
        return false;
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
    public NaryTree<E> withoutLabels(final Collection<E> labels) {
        return withoutLabels(new HashSet<E>(labels));
    }

    @Override
    public NaryTree<E> withoutLabels(final E[] labels) {
        final HashSet<E> set = new HashSet<E>();
        for (final E l : labels) {
            set.add(l);
        }
        return withoutLabels(set);
    }

    private NaryTree<E> withoutLabels(final HashSet<E> labelsToSkip) {
        final NaryTree<E> newTree = new NaryTree<E>(label);
        for (final Iterator<NaryTree<E>> i = childList.iterator(); i.hasNext();) {
            final NaryTree<E> child = i.next();
            if (!labelsToSkip.contains(child.label)) {
                newTree.addChild(child.withoutLabels(labelsToSkip));
            }
        }
        return newTree;
    }

    public LinkedList<NaryTree<E>> children() {
        return new LinkedList<NaryTree<E>>(childList);
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
     * It's not technically possible to do an 'in-order' traversal of an n-ary tree, since there's no guaranteed
     * position within the children in which the parent belongs. But some tree applications depend on branching in which
     * the parent branches into a left branch and one or more right branches. This iterator follows that assumption and
     * emits the parent node after the first child (if any) and before any other children.
     * 
     * @return 'in-order' {@link Iterator}
     */
    public Iterable<NaryTree<E>> inOrderTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderList();
    }

    public List<NaryTree<E>> inOrderList() {
        return inOrderList(this, new ArrayList<NaryTree<E>>(size));
    }

    private List<NaryTree<E>> inOrderList(final NaryTree<E> tree, final List<NaryTree<E>> list) {
        final Iterator<NaryTree<E>> i = tree.childList.iterator();
        if (i.hasNext()) {
            inOrderList(i.next(), list);
        }

        list.add(tree);

        while (i.hasNext()) {
            inOrderList(i.next(), list);
        }

        return list;
    }

    private List<E> inOrderLabelList(final List<E> list) {
        final Iterator<NaryTree<E>> i = childList.iterator();
        if (i.hasNext()) {
            i.next().inOrderLabelList(list);
        }

        list.add(label);

        while (i.hasNext()) {
            i.next().inOrderLabelList(list);
        }

        return list;
    }

    @Override
    public Iterable<E> inOrderLabelTraversal() {
        return inOrderLabelList(new LinkedList<E>());
    }

    public Iterable<NaryTree<E>> preOrderTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderList(new ArrayList<NaryTree<E>>(size));
    }

    private List<NaryTree<E>> preOrderList(final List<NaryTree<E>> list) {
        list.add(this);
        for (final NaryTree<E> child : childList) {
            child.preOrderList(list);
        }

        return list;
    }

    private List<E> preOrderLabelList(final List<E> list) {
        list.add(label);
        for (final NaryTree<E> child : childList) {
            child.preOrderLabelList(list);
        }

        return list;
    }

    @Override
    public Iterable<E> preOrderLabelTraversal() {
        return preOrderLabelList(new ArrayList<E>(size));
    }

    public Iterable<NaryTree<E>> postOrderTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderList(new ArrayList<NaryTree<E>>(size));
    }

    private List<NaryTree<E>> postOrderList(final List<NaryTree<E>> list) {
        for (final NaryTree<E> child : childList) {
            child.postOrderList(list);
        }
        list.add(this);

        return list;
    }

    private List<E> postOrderLabelList(final List<E> list) {
        for (final NaryTree<E> child : childList) {
            child.postOrderLabelList(list);
        }
        list.add(label);

        return list;
    }

    @Override
    public Iterable<E> postOrderLabelTraversal() {
        return postOrderLabelList(new LinkedList<E>());
    }

    private <L extends List<NaryTree<E>>> L leafList(final L list) {
        for (final NaryTree<E> node : inOrderList()) {
            if (node.isLeaf()) {
                list.add(node);
            }
        }

        return list;
    }

    @Override
    public Iterable<NaryTree<E>> leafTraversal() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return leafList(new LinkedList<NaryTree<E>>());
    }

    public LinkedList<NaryTree<E>> leafList() {
        return leafList(new LinkedList<NaryTree<E>>());
    }

    @Override
    public boolean isLeaf() {
        return size == 1;
    }

    @Override
    public boolean isLeftmostChild() {
        NaryTree<E> child = this;

        // Trace unary chains upward
        while (child.parent != null && child.parent.childList.size() == 1) {
            child = child.parent;
        }

        return child.parent != null && child == child.parent.childList.getFirst();
    }

    @Override
    public boolean isRightmostChild() {
        NaryTree<E> child = this;

        // Trace unary chains upward
        while (child.parent != null && child.parent.childList.size() == 1) {
            child = child.parent;
        }

        return child.parent != null && child == child.parent.childList.getLast();
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
        for (final NaryTree<E> child : childList) {
            maxChildHeight = Math.max(maxChildHeight, child.height());
        }
        return maxChildHeight + 1;
    }

    @Override
    public int depthFromRoot() {
        int depth = 0;

        for (NaryTree<?> tree = this; tree.parent != null; tree = tree.parent) {
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
        for (final NaryTree<E> leaf : leafTraversal()) {
            leafLabels[i++] = leaf.label;
        }
        return leafLabels;
    }

    /**
     * @return the parent of this tree (or null if this tree is the root)
     */
    public NaryTree<E> parent() {
        return parent;
    }

    /**
     * @return True if this node has been visited
     */
    public boolean isVisited() {
        return visited;
    }

    /**
     * @param visited
     */
    public void setVisited(final boolean visited) {
        this.visited = visited;
    }

    /**
     * @param childLabel Label of a child
     * @return the subtree rooted at the leftmost child labeled with the specified label or null if no such child
     *         exists.
     */
    public NaryTree<E> child(final E childLabel) {
        for (final NaryTree<E> child : childList) {
            if (child.label.equals(childLabel)) {
                return child;
            }
        }
        return null;
    }

    protected void updateSize(final int childrenAdded, final int leavesAdded) {
        size += childrenAdded;
        leaves += leavesAdded;

        if (parent != null) {
            parent.updateSize(childrenAdded, leavesAdded);
        }
    }

    /**
     * @return True if the tree rooted at this node contains no branches of order > 2.
     */
    public boolean isBinaryTree() {
        if (childList.size() > 2) {
            return false;
        }

        for (final NaryTree<E> child : childList) {
            if (!child.isBinaryTree()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public E parentLabel() {
        return parent != null ? parent.label : null;
    }

    @Override
    public Tree<E> leftmostLeaf() {
        if (childList.isEmpty()) {
            return this;
        }
        return childList.getFirst().leftmostLeaf();
    }

    @Override
    public Tree<E> rightmostLeaf() {
        if (childList.isEmpty()) {
            return this;
        }
        return childList.getLast().rightmostLeaf();
    }

    @Override
    public void replaceLeafLabels(final List<E> newLabels) {
        if (newLabels.size() != leaves) {
            throw new IllegalArgumentException("Length mismatch");
        }
        final Iterator<NaryTree<E>> leafIterator = leafTraversal().iterator();
        for (final E newLabel : newLabels) {
            leafIterator.next().label = newLabel;
        }
    }

    @Override
    public void replaceLeafLabels(final E[] newLabels) {
        if (newLabels.length != leaves) {
            throw new IllegalArgumentException("Length mismatch");
        }
        final Iterator<NaryTree<E>> leafIterator = leafTraversal().iterator();
        for (final E newLabel : newLabels) {
            leafIterator.next().label = newLabel;
        }
    }

    @Override
    public NaryTree<E> transform(final LabelTransformer<E> t) {
        return transform(this, t);
    }

    private NaryTree<E> transform(final NaryTree<E> node, final LabelTransformer<E> t) {
        final NaryTree<E> tree = new NaryTree<E>(t.transform(node.label));
        for (final NaryTree<E> child : node.children()) {
            tree.addChild(transform(child, t));
        }
        return tree;
    }

    @Override
    public NaryTree<E> removeAll(final Set<E> labelsToRemove, final int maxDepth) {

        if (labelsToRemove.contains(label)) {
            if (maxDepth > height() || childList.size() == 0 || childList.size() > 1) {
                throw new IllegalArgumentException("Indeterminate root node");
            }
            return childList.get(0).removeAll(labelsToRemove, maxDepth);
        }
        final NaryTree<E> copy = new NaryTree<E>(label);

        for (final NaryTree<E> child : childList) {

            if (labelsToRemove.contains(child.label)) {
                // If child height > maxDepth, skip the child, but add the grandchildren to the new copied tree
                if (child.height() > maxDepth) {
                    for (final NaryTree<E> grandchild : child.childList) {
                        copy.addChild(grandchild.removeAll(labelsToRemove, maxDepth));
                    }
                }
            } else {
                copy.addChild(child.removeAll(labelsToRemove, maxDepth));
            }
        }
        return copy;
    }

    public BinaryTree<String> binarize(final GrammarFormatType grammarFormatType, final Binarization binarization) {

        final BinaryTree<String> binaryTreeRoot = new BinaryTree<String>(label.toString());
        BinaryTree<String> binaryTree = binaryTreeRoot;

        if (childList.size() > 0) {
            final LinkedList<NaryTree<E>> queue = new LinkedList<NaryTree<E>>(childList);

            switch (binarization) {
            case RIGHT:
                // Add the first child as the left child of the binary tree
                binaryTree.addChild(queue.remove().binarize(grammarFormatType, binarization));

                // If there are 2 or more subsequent children, create factored child trees for them
                while (queue.size() > 1) {
                    final BinaryTree<String> subtree = new BinaryTree<String>(
                            grammarFormatType.factoredNonTerminal(label.toString()));
                    subtree.addChild(queue.remove().binarize(grammarFormatType, binarization));
                    binaryTree.addChild(subtree);
                    binaryTree = subtree;
                }

                // Add the last child
                if (queue.size() > 0) {
                    binaryTree.addChild(queue.remove().binarize(grammarFormatType, binarization));
                }
                break;

            case LEFT:

                // 2 or fewer children
                if (queue.size() <= 2) {
                    final BinaryTree<String> subtree = new BinaryTree<String>(label.toString());
                    subtree.addChild(queue.remove().binarize(grammarFormatType, binarization));
                    if (queue.size() > 0) {
                        subtree.addChild(queue.remove().binarize(grammarFormatType, binarization));
                    }
                    return subtree;

                }

                // 3 or more children

                // Add the first two as children of a factored subtree
                binaryTree = new BinaryTree<String>(grammarFormatType.factoredNonTerminal(label.toString()));
                binaryTree.addChild(queue.remove().binarize(grammarFormatType, binarization));
                binaryTree.addChild(queue.remove().binarize(grammarFormatType, binarization));

                // Create factored trees (with the current factored binary tree as left child) until we have only a
                // single remaining child
                while (queue.size() > 1) {
                    final BinaryTree<String> subtree = new BinaryTree<String>(
                            grammarFormatType.factoredNonTerminal(label.toString()));
                    subtree.addChild(binaryTree);
                    subtree.addChild(queue.remove().binarize(grammarFormatType, binarization));
                    binaryTree = subtree;
                }

                // Add the factored subtree and the last child as children of the root tree
                binaryTreeRoot.addChild(binaryTree);
                binaryTreeRoot.addChild(queue.remove().binarize(grammarFormatType, binarization));
                break;
            }
        }

        return binaryTreeRoot;
    }

    /**
     * Returns the 'head' descendant of this tree, using a head-percolation rule-set of the standard Charniak/Magerman
     * form.
     * 
     * @param ruleset head-percolation ruleset
     * @return head descendant
     */
    public NaryTree<E> headDescendant(final HeadPercolationRuleset ruleset) {
        if (isLeaf()) {
            return this;
        }

        final List<E> childLabels = childLabels();

        // Special-case for unary productions
        if (children().size() == 1) {
            return (childList.get(0)).headDescendant(ruleset);
        }

        // TODO: This is terribly inefficient - it requires mapping each child (O(n) and iterating
        // through childList (O(n)) for each node. A total of O(n^2)...)
        @SuppressWarnings("unchecked")
        final int index = ruleset.headChild((String) label(), (List<String>) childLabels);
        return (childList.get(index)).headDescendant(ruleset);
    }

    /**
     * TODO: This probably isn't the best way to model head percolation
     * 
     * @param ruleset head-percolation ruleset
     * @return true if this tree is the head of the tree it is rooted in
     */
    public boolean isHeadOfTreeRoot(final HeadPercolationRuleset ruleset) {
        return headLevel(ruleset) == 0;
    }

    /**
     * TODO: This probably isn't the best way to model head percolation
     * 
     * @param ruleset head-percolation ruleset
     * @return the depth in the tree for which this node is the head (possibly its own depth)
     */
    public int headLevel(final HeadPercolationRuleset ruleset) {
        if (!isLeaf()) {
            return -1;
        }

        // TODO: Terribly, horribly inefficient. But (again), we can tune later
        int level = depthFromRoot();
        for (@SuppressWarnings("unchecked")
        NaryTree<String> tree = (NaryTree<String>) parent; tree != null && tree.headDescendant(ruleset) == this; tree = tree.parent) {
            level--;
        }
        return level;
    }

    /**
     * @return The number of nodes descending <i>directly</i> from this node in a unary chain, excluding lexical
     *         entries.
     */
    public int unaryChainHeight() {
        int length = 0;
        NaryTree<E> node = this;
        while (node.childList.size() == 1 && !node.childList.getFirst().isLeaf()) {
            length++;
            node = node.childList.getFirst();
        }
        return length;
    }

    /**
     * @return The depth of this node in a unary chain (0 if the parent node has a right child)
     */
    public int unaryChainDepth() {
        int length = 0;
        NaryTree<E> node = this;
        while (node.parent != null && node.parent.childList.size() == 1) {
            length++;
            node = node.parent;
        }
        return length;
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
            for (final Iterator<NaryTree<E>> i = childList.iterator(); i.hasNext();) {
                writer.write(' ');
                i.next().writeSubtree(writer);
            }
            writer.write(')');
        } else {
            writer.write(label.toString());
        }
    }

    /**
     * Calculates the pq-gram tree similarity metric between two trees. See Augsten, Bohlen, Gamper, 2005.
     * 
     * @param p parameter
     * @param q parameter
     * @return tree similarity
     */
    public float pqgramDistance(final NaryTree<E> other, final int p, final int q) {
        return PqgramProfile.pqgramDistance(pqgramProfile(p, q), other.pqgramProfile(p, q));
    }

    /**
     * Implements pq-gram profile as per Augsten, Bohlen, Gamper 2005, page 306 (used to calculate pq-gram distance)
     * 
     * @param p parameter
     * @param q parameter
     * @return profile
     */
    public PqgramProfile<E> pqgramProfile(final int p, final int q) {
        final PqgramProfile<E> profile = new PqgramProfile<E>();
        pqgramProfile(p, q, profile, this, new ShiftRegister<E>(p));
        return profile;
    }

    /**
     * Implements pq-gram profile as per Augsten, Bohlen, Gamper 2005, page 306 (used to calculate pq-gram distance)
     * 
     * @param p parameter
     * @param q parameter
     * @param profile Current profile
     * @param r Current tree
     * @param anc Current shift register
     */
    private void pqgramProfile(final int p, final int q, final PqgramProfile<E> profile, final NaryTree<E> r,
            ShiftRegister<E> anc) {
        anc = anc.shift(r.label);
        ShiftRegister<E> sib = new ShiftRegister<E>(q);

        if (r.isLeaf()) {
            profile.add(anc.concat(sib));
        } else {
            for (final NaryTree<E> c : r.childList) {
                sib = sib.shift(c.label);
                profile.add(anc.concat(sib));
                pqgramProfile(p, q, profile, c, anc);
            }

            for (int k = 1; k < q; k++) {
                sib = sib.shift();
                profile.add(anc.concat(sib));
            }
        }
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

    private void addLabeledSpans(final HashMap<String, Integer> labeledSpans, final NaryTree<E> node, int start,
            final int minHeight, final Set<E> ignoredLabels, final Map<E, E> equivalentLabels) {

        if ((minHeight <= 1 || node.height() >= minHeight)
                && (ignoredLabels == null || !ignoredLabels.contains(node.label))) {
            final String key = node.labeledSpan(start, ignoredLabels, equivalentLabels);
            if (labeledSpans.containsKey(key)) {
                labeledSpans.put(key, labeledSpans.get(key) + 1);
            } else {
                labeledSpans.put(key, 1);
            }
        }

        for (final NaryTree<E> child : node.childList) {
            addLabeledSpans(labeledSpans, child, start, minHeight, ignoredLabels, equivalentLabels);
            start += child.leaves;
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

    public E[] preterminalLabels() {
        @SuppressWarnings("unchecked")
        final E[] labels = (E[]) Array.newInstance(label.getClass(), leaves);
        int i = 0;
        for (final NaryTree<E> leaf : leafTraversal()) {
            labels[i++] = leaf.parentLabel();
        }
        return labels;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object o) {
        if (!(o instanceof NaryTree)) {
            return false;
        }

        final NaryTree<E> other = (NaryTree<E>) o;

        if (!other.label.equals(label) || (other.childList.size() != childList.size())) {
            return false;
        }

        final Iterator<NaryTree<E>> i1 = childList.iterator();
        final Iterator<NaryTree<E>> i2 = other.childList.iterator();
        while (i1.hasNext()) {
            if (!(i1.next().equals(i2.next()))) {
                return false;
            }
        }

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NaryTree<E> clone() {
        try {
            return (NaryTree<E>) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
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
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> NaryTree<T> read(final InputStream inputStream, final Class<T> type) throws IOException {
        return read(new InputStreamReader(inputStream), type);
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> NaryTree<T> read(final InputStream inputStream, final LabelParser<T> labelParser)
            throws IOException {
        return read(new InputStreamReader(inputStream), labelParser);
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static <T> NaryTree<T> read(final String string, final Class<T> type) {
        try {
            final StringReader r = new StringReader(string);
            final NaryTree<T> tree = read(r, type);
            if (r.read() >= 0) {
                // We expect a single tree, but we didn't consume the entire string; this may be bracketed text
                throw new IllegalArgumentException("Badly-formatted tree");
            }
            return tree;
        } catch (final IOException e) {
            // A StringReader shouldn't ever throw an IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     */
    public static <T> NaryTree<T> read(final String string, final LabelParser<T> labelParser) {
        try {
            return read(new StringReader(string), labelParser);
        } catch (final IOException e) {
            // A StringReader shouldn't ever throw an IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param reader The reader to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> NaryTree<T> read(final Reader reader, final Class<T> type) throws IOException {

        try {
            final Constructor<T> labelConstructor = type.getConstructor(new Class[] { String.class });
            final LabelParser<T> labelParser = new LabelParser<T>() {
                @Override
                public T parse(final String label) throws Exception {
                    return labelConstructor.newInstance(new Object[] { label });
                }
            };
            return read(reader, labelParser);
        } catch (final IllegalArgumentException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
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
    public static <T> NaryTree<T> read(final Reader reader, final LabelParser<T> labelParser) throws IOException {
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
    private static <T> NaryTree<T> readSubtree(final Reader reader, final NaryTree<T> parent,
            final LabelParser<T> labelParser) throws IOException {

        try {
            // Recursively read a tree from the reader.
            // TODO: This could probably be simplified and perhaps optimized
            final StringBuilder rootToken = new StringBuilder();

            final char EOF = (char) -1;

            // Read the root token
            for (char c = (char) reader.read(); c != ' ' && c != ')' && c != EOF; c = (char) reader.read()) {
                rootToken.append(c);
            }

            final NaryTree<T> tree = new NaryTree<T>(labelParser.parse(rootToken.toString()));

            StringBuilder childToken = new StringBuilder();

            // Read tokens and add children until we come to a ')'
            for (char c = (char) reader.read(); c != ')' && c != EOF; c = (char) reader.read()) {
                // Parse any subtrees we find
                if (c == '(') {
                    tree.addChild(readSubtree(reader, tree, labelParser));
                }
                // Add any tokens we find
                else if (c == ' ') {
                    if (childToken.length() > 0) {
                        tree.addChild(new NaryTree<T>(labelParser.parse(childToken.toString())));
                        childToken = new StringBuilder();
                    }
                } else {
                    childToken.append(c);
                }
            }

            if (childToken.length() > 0) {
                tree.addChild(new NaryTree<T>(labelParser.parse(childToken.toString())));
            }

            tree.parent = parent;
            return tree;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class PqgramProfile<E> implements Cloneable {

        private final Object2IntOpenHashMap<ShiftRegister<E>> map;
        private int size;

        public PqgramProfile() {
            map = new Object2IntOpenHashMap<ShiftRegister<E>>();
        }

        private PqgramProfile(final Object2IntOpenHashMap<ShiftRegister<E>> map) {
            this.map = map;
        }

        public void add(final ShiftRegister<E> register) {
            size++;
            map.put(register, map.getInt(register) + 1);
        }

        public void addAll(final Collection<? extends ShiftRegister<E>> c) {
            for (final ShiftRegister<E> register : c) {
                add(register);
            }
        }

        public void clear() {
            map.clear();
            size = 0;
        }

        public boolean contains(final ShiftRegister<E> register) {
            return map.containsKey(register);
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public PqgramProfile<E> intersection(final PqgramProfile<E> o) {
            final PqgramProfile<E> intersection = new PqgramProfile<E>();

            for (final ShiftRegister<E> r : map.keySet()) {
                final int count = Math.min(map.getInt(r), o.map.getInt(r));

                if (count > 0) {
                    intersection.map.put(r, count);
                    intersection.size += count;
                }
            }

            return intersection;
        }

        public int intersectionSize(final PqgramProfile<E> o) {
            int intersectionSize = 0;

            for (final ShiftRegister<E> r : map.keySet()) {
                intersectionSize += Math.min(map.getInt(r), o.map.getInt(r));
            }

            return intersectionSize;
        }

        public int size() {
            return size;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof PqgramProfile<?>)) {
                return false;
            }

            final PqgramProfile<E> bag = (PqgramProfile<E>) o;

            for (final ShiftRegister<E> r : map.keySet()) {
                if (map.getInt(r) != bag.map.getInt(r)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public PqgramProfile<E> clone() {
            return new PqgramProfile<E>(map.clone());
        }

        @Override
        public String toString() {
            return map.toString();
        }

        /**
         * Calculates the pq-gram tree similarity metric between two trees. See Augsten, Bohlen, Gamper, 2005.
         * 
         * @param profile1 Bag profile of a tree
         * @param profile2 Bag profile of a tree
         * @return tree similarity
         */
        public final static <E> float pqgramDistance(final PqgramProfile<E> profile1, final PqgramProfile<E> profile2) {
            final int bagUnionCardinality = profile1.size() + profile2.size();
            final int bagIntersectionCardinality = profile1.intersectionSize(profile2);

            return 1f - 2f * bagIntersectionCardinality / bagUnionCardinality;
        }
    }

    public static interface LabelParser<T extends Object> {
        public T parse(String label) throws Exception;
    }

    /**
     * Grammar binarization directions (i.e., when binarizing a n-ary rule, the side on which factored categories are
     * placed)
     */
    public static enum Binarization {
        /** Factored categories on the left */
        LEFT,
        /** Factored categories on the right */
        RIGHT;
    }
}
