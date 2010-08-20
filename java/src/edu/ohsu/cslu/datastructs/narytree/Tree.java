package edu.ohsu.cslu.datastructs.narytree;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public interface Tree<E extends Object> {
    public E label();

    public String stringLabel();

    /**
     * Adds a child to this tree as the rightmost child
     * 
     * @param childLabel
     */
    public Tree<E> addChild(final E childLabel);

    public void addChildren(final Collection<E> childLabels);

    public void addChildren(final E[] children);

    public void addSubtree(Tree<E> subtree);

    public boolean removeChild(final E childLabel);

    public void removeChildren(final Collection<E> childLabels);

    public void removeChildren(final E[] childLabels);

    public boolean removeSubtree(final E childLabel);

    public List<E> childLabels();

    public List<? extends Tree<E>> children();

    /**
     * @return the parent of this tree (or null if this tree is the root)
     */
    public Tree<E> parent();

    /**
     * @param childLabel Label of a child
     * @return the subtree rooted at the leftmost child labeled with the specified label or null if no such
     *         child exists.
     */
    public Tree<E> subtree(E childLabel);

    /**
     * @return the depth of this subtree within the entire tree of which it is a part (0 if this tree is the
     *         root)
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
     * @return an Iterator over all nodes in this tree. Iteration order will be leftmost-child, head, other
     *         children (in left-to-right order).
     */
    public Iterator<? extends Tree<E>> inOrderIterator();

    /**
     * @return an Iterator over all nodes in this tree. Iteration order will be head, followed by children (in
     *         left-to-right order).
     */
    public Iterator<? extends Tree<E>> preOrderIterator();

    /**
     * @return an Iterator over all nodes in this tree. Iteration order will be children (in left-to-right
     *         order), followed by head.
     */
    public Iterator<? extends Tree<E>> postOrderIterator();

    /**
     * @return an Iterator over all labels in this tree. Iteration order will be leftmost-child, head, other
     *         children (in left-to-right order).
     */
    public Iterator<E> inOrderLabelIterator();

    /**
     * @return an Iterator over all nodes in this tree. Iteration order will be head, followed by children (in
     *         left-to-right order).
     */
    public Iterator<E> preOrderLabelIterator();

    /**
     * @return an Iterator over all labels in this tree. Iteration order will be children (in left-to-right
     *         order), followed by head.
     */
    public Iterator<E> postOrderLabelIterator();

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
