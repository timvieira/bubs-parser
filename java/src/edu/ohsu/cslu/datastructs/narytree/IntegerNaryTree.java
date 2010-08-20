package edu.ohsu.cslu.datastructs.narytree;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.ohsu.cslu.common.Vocabulary;

/**
 * N-Ary tree implementation using Integers as node labels.
 * 
 * @author Aaron Dunlop
 * @since Sep 22, 2008
 * 
 *        $Id$
 */
public final class IntegerNaryTree extends BaseNaryTree<Integer> {

    public IntegerNaryTree(final int label, final BaseNaryTree<Integer> parent) {
        super(label, parent);
    }

    public IntegerNaryTree(final int label) {
        super(label);
    }

    public IntegerNaryTree(final String label, final Vocabulary vocabulary) {
        super(Integer.parseInt(label));
    }

    /**
     * @return the label of the root node
     */
    public Integer label() {
        return new Integer(label);
    }

    public String stringLabel() {
        return Integer.toString(label);
    }

    /**
     * Type-strengthen return-type
     */
    @Override
    public IntegerNaryTree addChild(final String childLabel) {
        return addChild(Integer.parseInt(childLabel));
    }

    /**
     * Type-strengthen return-type
     */
    @Override
    public IntegerNaryTree addChild(final int childLabel) {
        return (IntegerNaryTree) super.addChild(childLabel);
    }

    @Override
    public IntegerNaryTree addChild(final Integer childLabel) {
        return addChild(childLabel.intValue());
    }

    @Override
    public void addSubtree(final Tree<Integer> subtree) {
        super.addSubtree((BaseNaryTree<Integer>) subtree);
    }

    @Override
    public boolean removeChild(final Integer childLabel) {
        return removeChild(childLabel.intValue());
    }

    @Override
    public boolean removeSubtree(final Integer childLabel) {
        return removeSubtree(childLabel.intValue());
    }

    @Override
    public Iterator<Integer> inOrderLabelIterator() {
        return inOrderIntegerIterator();
    }

    @Override
    public Iterator<Integer> preOrderLabelIterator() {
        return preOrderIntegerIterator();
    }

    @Override
    public Iterator<Integer> postOrderLabelIterator() {
        return postOrderIntegerIterator();
    }

    /**
     * Type-strengthen return-type
     */
    @Override
    public IntegerNaryTree subtree(final Integer childLabel) {
        return (IntegerNaryTree) subtree(childLabel.intValue());
    }

    @Override
    public List<Integer> childLabels() {
        final ArrayList<Integer> list = new ArrayList<Integer>(childList.size());
        for (final BaseNaryTree<Integer> child : childList) {
            list.add(new Integer(child.label));
        }
        return list;
    }

    /**
     * Reads in an IntegerNaryTree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static IntegerNaryTree read(final InputStream inputStream) throws IOException {
        return (IntegerNaryTree) read(new InputStreamReader(inputStream), IntegerNaryTree.class, null);
    }

    /**
     * Reads in an IntegerNaryTree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static IntegerNaryTree read(final String string) {
        return (IntegerNaryTree) read(string, IntegerNaryTree.class, null);
    }

    /**
     * Reads in an IntegerNaryTree from a standard parenthesis-bracketed representation
     * 
     * @param reader The reader to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static IntegerNaryTree read(final Reader reader) throws IOException {
        return (IntegerNaryTree) read(reader, IntegerNaryTree.class, null);
    }
}
