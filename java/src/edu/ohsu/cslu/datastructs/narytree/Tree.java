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
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generic interface for binary and n-ary trees with generic labels. Note that the methods defined here are a bit of a
 * mishmash of functional-style (immutable) and mutators. Perhaps that should be cleaned up someday. For the moment, pay
 * attention to the JavaDoc, and caveat emptor.
 * 
 * @author Aaron Dunlop
 * @since Aug 23, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface Tree<E extends Object> extends Cloneable {

    public final static String NULL_LABEL = "-NULL-";

    /**
     * @return the label of the root node
     */
    public E label();

    /**
     * Adds a child to this tree as the rightmost child
     * 
     * @param childLabel
     * @return the newly added child
     */
    public Tree<E> addChild(final E childLabel);

    /**
     * Adds a child to this tree as the rightmost child
     * 
     * @param child
     * @return the newly added child
     */
    public Tree<E> addChild(final Tree<E> child);

    /**
     * Adds children to the root node, in a left-to-right order.
     * 
     * @param childLabels labels of the children to be added
     */
    public void addChildren(final Collection<E> childLabels);

    /**
     * Adds children to the root node, in a left-to-right order.
     * 
     * @param childLabels labels of the children to be added
     */
    public void addChildren(final E[] childLabels);

    /**
     * Removes a child subtree.
     * 
     * @param childLabel the label of the child to be removed.
     * @return true if the child was found and removed.
     */
    public boolean removeChild(final E childLabel);

    /**
     * Removes all children matching the specified child labels.
     * 
     * @param childLabels the labels of the children to be removed.
     */
    public void removeChildren(final Collection<E> childLabels);

    /**
     * Removes all children matching the specified child labels.
     * 
     * @param childLabels the labels of the children to be removed.
     */
    public void removeChildren(final E[] childLabels);

    /**
     * Updates the label of a leaf
     * 
     * @param newLabel
     */
    public void setLabel(final E newLabel);

    /**
     * Returns a copy of this tree without any subtrees matching the specified labels.
     * 
     * @param labels the labels of the children to be removed.
     * @return copy of this tree without any subtrees matching the specified labels
     */
    public Tree<E> withoutLabels(final Collection<E> labels);

    /**
     * Returns a copy of this tree without any subtrees matching the specified labels.
     * 
     * @param labels the labels of the children to be removed.
     * @return copy of this tree without any subtrees matching the specified labels
     */
    public Tree<E> withoutLabels(final E[] labels);

    /**
     * @return children of the root node
     */
    public List<? extends Tree<E>> children();

    /**
     * @return labels of all children of the root node
     */
    public List<E> childLabels();

    /**
     * @return the parent of this tree (or null if this tree is the root)
     */
    public Tree<E> parent();

    /**
     * @return the label of parent of this tree (or null if this tree is the root)
     */
    public E parentLabel();

    /**
     * @param childLabel Label of a child
     * @return the subtree rooted at the leftmost child labeled with the specified label or null if no such child
     *         exists.
     */
    public Tree<E> child(E childLabel);

    /**
     * @return true if this tree is the leftmost child of its parent or the only (unary) child of a leftmost child.
     */
    public boolean isLeftmostChild();

    /**
     * @return true if this tree is the rightmost child of its parent or the only (unary) child of a rightmost child.
     */
    public boolean isRightmostChild();

    /**
     * @return the depth of this subtree within the entire tree of which it is a part (0 if this tree is the root)
     */
    public int depthFromRoot();

    /**
     * @return number of nodes contained in the tree
     */
    public int size();

    /**
     * @return the height of the tree
     */
    public int height();

    /**
     * @return number of leaf nodes in the tree
     */
    public int leaves();

    /**
     * @return the labels of all leaves of this tree.
     */
    public E[] leafLabels();

    /**
     * Replaces all leaf labels with the provided list
     * 
     * @param newLabels
     * @throws IllegalArgumentException if the length of <code>newLabels</code> is not as the number of leaves in the
     *             tree.
     */
    public void replaceLeafLabels(List<E> newLabels);

    /**
     * Replaces all leaf labels with the provided list
     * 
     * @param newLabels
     * @throws IllegalArgumentException if the length of <code>newLabels</code> is not as the number of leaves in the
     *             tree.
     */
    public void replaceLeafLabels(E[] newLabels);

    /**
     * @return The leftmost leaf descended from this tree or <code>this</code> if this tree has no children.
     */
    public Tree<E> leftmostLeaf();

    /**
     * @return The rightmost leaf descended from this tree or <code>this</code> if this tree has no children.
     */
    public Tree<E> rightmostLeaf();

    /**
     * @return True if this {@link Tree} is the immediate parent of a single leaf
     */
    public boolean isPreterminal();

    /**
     * Returns a {@link Set} containing string representations of each labeled span with height greater than the
     * specified minimum.
     * 
     * @param minHeight
     * @return a {@link Map} from string representations of each labeled span (with height greater than
     *         <code>minHeight</code>) to occurrence counts for each span (unary chains will result in duplicate spans).
     */
    public Map<String, Integer> labeledSpans(final int minHeight);

    /**
     * Returns a {@link Set} containing string representations of each labeled span with height greater than the
     * specified minimum.
     * 
     * @param minHeight
     * @param ignoredLabels Labels which should not be included in the returned set of labeled spans
     * @param equivalentLabels Labels which should be considered equivalent and substituted for one another. For any
     *            span matching a key in this map, the returned span will include the target value.
     * @return a {@link Map} from string representations of each labeled span (with height greater than
     *         <code>minHeight</code>) to occurrence counts for each span (unary chains will result in duplicate spans).
     */
    public Map<String, Integer> labeledSpans(final int minHeight, final Set<E> ignoredLabels,
            final Map<E, E> equivalentLabels);

    /**
     * Returns a copy of this tree in which each label is transformed by the supplied {@link LabelTransformer}.
     * 
     * @param t Label transformation function
     * @return a copy of this tree in which each label is transformed by the supplied {@link LabelTransformer}.
     */
    public Tree<E> transform(LabelTransformer<E> t);

    /**
     * Returns a copy of this tree with the specified labels removed. If a matching node is found of depth less than
     * <code>maxDepth</code>, the entire subtree will be removed
     * 
     * @param labelsToRemove Set of labels which should be removed
     * @param maxDepth Maximum depth of subtree which should be removed along with the matching node.
     * @throws IllegalArgumentException if the resulting root node is indeterminate
     * @return a copy of this tree with the specified labels removed
     */
    public Tree<E> removeAll(Set<E> labelsToRemove, final int maxDepth);

    /**
     * @return an {@link Iterable} over all nodes in this tree. Iteration order will be leftmost-child, head, other
     *         children (in left-to-right order).
     */
    public Iterable<? extends Tree<E>> inOrderTraversal();

    /**
     * @return an {@link Iterable} over all nodes in this tree. Iteration order will be head, followed by children (in
     *         left-to-right order).
     */
    public Iterable<? extends Tree<E>> preOrderTraversal();

    /**
     * @return an {@link Iterable} over all nodes in this tree. Iteration order will be children (in left-to-right
     *         order), followed by head.
     */
    public Iterable<? extends Tree<E>> postOrderTraversal();

    /**
     * @return an {@link Iterable} over all leaf nodes in this tree.
     */
    public Iterable<? extends Tree<E>> leafTraversal();

    /**
     * @return an {@link Iterable} over all labels in this tree. Iteration order will be leftmost-child, head, other
     *         children (in left-to-right order).
     */
    public Iterable<E> inOrderLabelTraversal();

    /**
     * @return an {@link Iterable} over all nodes in this tree. Iteration order will be head, followed by children (in
     *         left-to-right order).
     */
    public Iterable<E> preOrderLabelTraversal();

    /**
     * @return an {@link Iterable} over all labels in this tree. Iteration order will be children (in left-to-right
     *         order), followed by head.
     */
    public Iterable<E> postOrderLabelTraversal();

    /**
     * @return the labels of all preterminal nodes (i.e., the parents of each leaf).
     */
    public E[] preterminalLabels();

    /**
     * @return True if the root node is a leaf
     */
    public boolean isLeaf();

    /**
     * @return a deep copy of this tree
     */
    public Tree<E> clone();

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param outputStream The {@link OutputStream} to write to
     * @throws IOException if the write fails
     */
    public void write(OutputStream outputStream) throws IOException;

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param writer The {@link Writer} to write to
     * @throws IOException if the write fails
     */
    public void write(Writer writer) throws IOException;

    public static interface LabelTransformer<E> {
        public E transform(final E label);
    }
}
