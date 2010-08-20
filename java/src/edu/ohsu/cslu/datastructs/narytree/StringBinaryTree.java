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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class StringBinaryTree implements Tree<String>, Serializable {

    private final static long serialVersionUID = 369752896212698723L;

    /** Label of the root node */
    protected String label;

    /** Parent node (if any) */
    protected StringBinaryTree parent;

    private boolean visited;

    /**
     * The children of this tree
     */
    protected StringBinaryTree leftChild;
    protected StringBinaryTree rightChild;

    /** Number of nodes in this tree, including this node and any subtrees */
    protected int size;

    /** Number of leaves in this tree */
    protected int leaves;

    public StringBinaryTree(final String label, final StringBinaryTree parent) {
        this.label = label;
        size = 1;
        leaves = 1;
        this.parent = parent;
    }

    public StringBinaryTree(final String label) {
        this(label, null);
    }

    public String label() {
        return label;
    }

    public String stringLabel() {
        return label;
    }

    /**
     * Adds a child to the tree
     * 
     * @param childLabel label of the child
     * @return The newly added subtree
     */
    public StringBinaryTree addChild(final String childLabel) {
        final StringBinaryTree child = new StringBinaryTree(childLabel, this);
        updateSize(1, isLeaf() ? 0 : 1);
        if (leftChild == null) {
            leftChild = child;
        } else if (rightChild == null) {
            rightChild = child;
        } else {
            throw new RuntimeException("Binary tree is fully populated");
        }

        return child;
    }

    @Override
    public void addChildren(final String[] childLabels) {
        for (final String child : childLabels) {
            addChild(child);
        }
    }

    @Override
    public void addChildren(final Collection<String> childLabels) {
        for (final String child : childLabels) {
            addChild(child);
        }
    }

    public void addSubtree(final Tree<String> subtree) {
        final StringBinaryTree typedSubtree = (StringBinaryTree) subtree;
        final int leavesAdded = typedSubtree.leaves - (isLeaf() ? 1 : 0);
        if (leftChild == null) {
            leftChild = typedSubtree;
        } else if (rightChild == null) {
            rightChild = typedSubtree;
        } else {
            throw new RuntimeException("Binary tree is fully populated");
        }
        typedSubtree.parent = this;
        updateSize(typedSubtree.size, leavesAdded);
    }

    public boolean removeChild(final String childLabel) {
        return removeSubtree(childLabel);
    }

    @Override
    public void removeChildren(final Collection<String> childLabels) {
        for (final String childLabel : childLabels) {
            removeChild(childLabel);
        }
    }

    @Override
    public void removeChildren(final String[] childLabels) {
        for (final String childLabel : childLabels) {
            removeChild(childLabel);
        }
    }

    public boolean removeSubtree(final String childLabel) {

        if (rightChild != null && rightChild.label.equals(childLabel)) {
            updateSize(-rightChild.size, rightChild.leaves);
            rightChild = null;
            return true;
        } else if (leftChild != null && leftChild.label.equals(childLabel)) {
            if (rightChild != null) {
                updateSize(-leftChild.size, -leftChild.leaves);
                leftChild = rightChild;
            } else {
                updateSize(-leftChild.size, -leftChild.leaves + 1);
                leftChild = null;
            }

            return true;
        }
        return false;
    }

    public String[] childArray() {
        if (rightChild == null) {
            if (leftChild == null) {
                return new String[0];
            }
            return new String[] { leftChild.label };
        }
        return new String[] { leftChild.label, rightChild.label };
    }

    public List<StringBinaryTree> children() {
        final LinkedList<StringBinaryTree> children = new LinkedList<StringBinaryTree>();
        if (leftChild != null) {
            children.add(leftChild);

            if (rightChild != null) {
                children.add(rightChild);
            }
        }
        return children;
    }

    public boolean isLeaf() {
        return leftChild == null;
    }

    public int size() {
        return size;
    }

    public int depthFromRoot() {
        int depth = 0;

        for (StringBinaryTree tree = this; tree.parent != null; tree = tree.parent) {
            depth++;
        }
        return depth;
    }

    public int leaves() {
        return leaves;
    }

    public StringBinaryTree parent() {
        return parent;
    }

    public StringBinaryTree subtree(final String childLabel) {
        if (leftChild != null && leftChild.label.equals(childLabel)) {
            return leftChild;
        } else if (rightChild != null && rightChild.label.equals(childLabel)) {
            return rightChild;
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

    @Override
    public Iterator<StringBinaryTree> inOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderList(new ArrayList<StringBinaryTree>(size)).iterator();
    }

    private List<StringBinaryTree> inOrderList(final List<StringBinaryTree> list) {

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
    public Iterator<String> inOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderLabelList(new ArrayList<String>(size)).iterator();
    }

    @Override
    public Iterator<StringBinaryTree> preOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderList(new ArrayList<StringBinaryTree>(size)).iterator();
    }

    private List<StringBinaryTree> preOrderList(final List<StringBinaryTree> list) {

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
    public Iterator<String> preOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderLabelList(new ArrayList<String>(size)).iterator();
    }

    @Override
    public Iterator<StringBinaryTree> postOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderList(new ArrayList<StringBinaryTree>(size)).iterator();
    }

    private List<StringBinaryTree> postOrderList(final List<StringBinaryTree> list) {

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
    public Iterator<String> postOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderLabelList(new ArrayList<String>(size)).iterator();
    }

    @Override
    public List<String> childLabels() {
        final ArrayList<String> list = new ArrayList<String>(2);

        if (leftChild != null) {
            list.add(leftChild.label);

            if (rightChild != null) {
                list.add(rightChild.label);
            }
        }
        list.add(label);

        return list;
    }

    private List<String> inOrderLabelList(final List<String> list) {

        if (leftChild != null) {
            leftChild.inOrderLabelList(list);
        }
        list.add(label);
        if (rightChild != null) {
            rightChild.inOrderLabelList(list);
        }

        return list;
    }

    public String[] inOrderArray() {
        return toStringArray(inOrderLabelIterator());
    }

    private List<String> preOrderLabelList(final List<String> list) {

        list.add(label);
        if (leftChild != null) {
            leftChild.preOrderLabelList(list);
        }
        if (rightChild != null) {
            rightChild.preOrderLabelList(list);
        }

        return list;
    }

    public String[] preOrderArray() {
        return toStringArray(preOrderLabelIterator());
    }

    private List<String> postOrderLabelList(final List<String> list) {

        if (leftChild != null) {
            leftChild.postOrderLabelList(list);
        }
        if (rightChild != null) {
            rightChild.postOrderLabelList(list);
        }
        list.add(label);

        return list;
    }

    public String[] postOrderArray() {
        return toStringArray(postOrderLabelIterator());
    }

    private String[] toStringArray(final Iterator<String> iter) {
        final String[] array = new String[size];
        for (int i = 0; i < size; i++) {
            array[i] = iter.next();
        }
        return array;
    }

    public void setStringLabel(final String label) {
        this.label = label;
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
    public boolean containsLabel(final String l) {
        for (final Iterator<String> i = inOrderLabelIterator(); i.hasNext();) {
            if (i.next().equals(l)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static StringBinaryTree read(final InputStream inputStream) throws IOException {
        return read(new InputStreamReader(inputStream));
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static StringBinaryTree read(final String string) {
        try {
            return read(new StringReader(string));
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
    public static StringBinaryTree read(final Reader reader) throws IOException {
        char c;

        // Discard any spaces or end-of-line characters
        while ((c = (char) reader.read()) == '\n' || c == '\r' || c == ' ') {
        }

        // We expect the first character to be '('
        if (c != '(') {
            throw new IllegalArgumentException("Bad tree format. Expected '(' but found '" + c + "'");
        }

        return readSubtree(reader, null);
    }

    /**
     * Recursively reads a subtree from a Reader
     * 
     * @param reader source
     * @param parent parent tree (if any)
     * @return subtree
     * @throws IOException if the read fails
     */
    private static StringBinaryTree readSubtree(final Reader reader, final StringBinaryTree parent)
            throws IOException {
        try {
            // Recursively read a tree from the reader.
            // TODO: This could probably be simplified and perhaps optimized
            final StringBuilder rootToken = new StringBuilder();

            // Read the root token
            for (char c = (char) reader.read(); c != ' ' && c != ')'; c = (char) reader.read()) {
                rootToken.append(c);
            }
            final StringBinaryTree tree = new StringBinaryTree(rootToken.toString());

            StringBuilder childToken = new StringBuilder();

            // Read tokens and add children until we come to a ')'
            for (char c = (char) reader.read(); c != ')'; c = (char) reader.read()) {
                // Parse any subtrees we find
                if (c == '(') {
                    tree.addSubtree(readSubtree(reader, tree));
                }
                // Add any tokens we find
                else if (c == ' ') {
                    if (childToken.length() > 0) {
                        tree.addChild(childToken.toString());
                        childToken = new StringBuilder();
                    }
                } else {
                    childToken.append(c);
                }
            }

            if (childToken.length() > 0) {
                tree.addChild(childToken.toString());
            }

            tree.parent = parent;
            return tree;
        } catch (final Exception e) {
            throw new RuntimeException(e);
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
            writer.write(stringLabel());

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
            writer.write(stringLabel());
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof StringBinaryTree)) {
            return false;
        }

        final StringBinaryTree other = (StringBinaryTree) o;

        if (!other.label.equals(label) || other.size != size) {
            return false;
        }

        final Iterator<String> i1 = inOrderLabelIterator();
        final Iterator<String> i2 = other.inOrderLabelIterator();
        while (i1.hasNext()) {
            if (!(i1.next().equals(i2.next()))) {
                return false;
            }
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

    protected abstract static class BaseIterator {

        protected Iterator<Integer> intIterator;

        public BaseIterator(final Iterator<Integer> intIterator) {
            this.intIterator = intIterator;
        }

        public boolean hasNext() {
            return intIterator.hasNext();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
