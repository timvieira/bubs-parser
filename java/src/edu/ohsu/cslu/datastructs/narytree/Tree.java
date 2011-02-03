package edu.ohsu.cslu.datastructs.narytree;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Generic interface for binary and n-ary trees.
 * 
 * @author Aaron Dunlop
 * @since Aug 23, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface Tree<E extends Object> extends Cloneable {

    /**
     * @return the label of the root node
     */
    public E label();

    /**
     * Adds a child to this tree as the rightmost child
     * 
     * @param childLabel
     */
    public Tree<E> addChild(final E childLabel);

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
     * Adds a subtree as the right-most child of the root node.
     * 
     * @param subtree the child tree to be added
     */
    public void addSubtree(Tree<E> subtree);

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
     * @param childLabel Label of a child
     * @return the subtree rooted at the leftmost child labeled with the specified label or null if no such child
     *         exists.
     */
    public Tree<E> subtree(E childLabel);

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
     * @return number of leaf nodes in the tree
     */
    public int leaves();

    /**
     * @return an {@link Iterator} over all nodes in this tree. Iteration order will be leftmost-child, head, other
     *         children (in left-to-right order).
     */
    public Iterator<? extends Tree<E>> inOrderIterator();

    /**
     * @return an {@link Iterator} over all nodes in this tree. Iteration order will be head, followed by children (in
     *         left-to-right order).
     */
    public Iterator<? extends Tree<E>> preOrderIterator();

    /**
     * @return an {@link Iterator} over all nodes in this tree. Iteration order will be children (in left-to-right
     *         order), followed by head.
     */
    public Iterator<? extends Tree<E>> postOrderIterator();

    /**
     * @return an {@link Iterator} over all leaf nodes in this tree.
     */
    public Iterator<? extends Tree<E>> leafIterator();

    /**
     * @return an {@link Iterator} over all labels in this tree. Iteration order will be leftmost-child, head, other
     *         children (in left-to-right order).
     */
    public Iterator<E> inOrderLabelIterator();

    /**
     * @return an {@link Iterator} over all nodes in this tree. Iteration order will be head, followed by children (in
     *         left-to-right order).
     */
    public Iterator<E> preOrderLabelIterator();

    /**
     * @return an {@link Iterator} over all labels in this tree. Iteration order will be children (in left-to-right
     *         order), followed by head.
     */
    public Iterator<E> postOrderLabelIterator();

    /**
     * @return True if the root node is a leaf
     */
    public boolean isLeaf();

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

}
