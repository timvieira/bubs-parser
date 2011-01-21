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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.LabelParser;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;

public class BinaryTree<E> implements Tree<E>, Serializable {

    private final static long serialVersionUID = 369752896212698723L;

    /** Label of the root node */
    private final E label;

    /** Parent node (if any) */
    private BinaryTree<E> parent;

    private final Factorization factorization;

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

    public BinaryTree(final E label, final BinaryTree<E> parent, final Factorization factorization) {
        this.label = label;
        size = 1;
        leaves = 1;
        this.parent = parent;
        this.factorization = factorization;
    }

    public BinaryTree(final E label, final Factorization factorization) {
        this(label, null, factorization);
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
        return addChild(new BinaryTree<E>(childLabel, this, factorization));
    }

    /**
     * Adds a child to the tree
     * 
     * @param child the child to add
     * @return The newly added subtree
     */
    protected BinaryTree<E> addChild(final BinaryTree<E> child) {
        child.parent = this;
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

    public void addSubtree(final Tree<E> subtree) {
        final BinaryTree<E> typedSubtree = (BinaryTree<E>) subtree;
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

    /**
     * Used just for static parsing routines
     * 
     * @param subtree tree to add
     */
    @SuppressWarnings("unchecked")
    private void internalAddSubtree(final BinaryTree<?> subtree) {
        addSubtree((BinaryTree<E>) subtree);
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

    public boolean removeSubtree(final E childLabel) {

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

    public BinaryTree<E> parent() {
        return parent;
    }

    public BinaryTree<E> subtree(final E childLabel) {
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
    public Iterator<BinaryTree<E>> inOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderList().iterator();
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
    public Iterator<E> inOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderLabelList(new ArrayList<E>(size)).iterator();
    }

    @Override
    public Iterator<BinaryTree<E>> preOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderList(new ArrayList<BinaryTree<E>>(size)).iterator();
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
    public Iterator<E> preOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderLabelList(new ArrayList<E>(size)).iterator();
    }

    @Override
    public Iterator<BinaryTree<E>> postOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderList(new ArrayList<BinaryTree<E>>(size)).iterator();
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
    public Iterator<E> postOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderLabelList(new ArrayList<E>(size)).iterator();
    }

    private List<BinaryTree<E>> leafList(final List<BinaryTree<E>> list) {
        for (final BinaryTree<E> node : inOrderList()) {
            if (node.isLeaf()) {
                list.add(node);
            }
        }

        return list;
    }

    @Override
    public Iterator<BinaryTree<E>> leafIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return leafList(new LinkedList<BinaryTree<E>>()).iterator();
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
        for (final Iterator<E> i = inOrderLabelIterator(); i.hasNext();) {
            if (i.next().equals(l)) {
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
        for (final Iterator<BinaryTree<E>> iter = preOrderIterator(); iter.hasNext();) {
            final BinaryTree<E> node = iter.next();
            final int len = node.directUnaryChainLength();
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
    int directUnaryChainLength() {
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

    /**
     * 'Un-factors' a binary-factored parse tree by removing category split labels and flattening binary-factored
     * subtrees.
     * 
     * @param grammarFormatType Grammar format
     * @return Unfactored tree
     */
    public NaryTree<String> unfactor(final GrammarFormatType grammarFormatType) {

        final NaryTree<String> rootTree = new NaryTree<String>(grammarFormatType.unsplitNonTerminal(label.toString()));

        if (leftChild != null) {
            leftChild.unfactorChildren(rootTree, grammarFormatType);
        }
        if (rightChild != null) {
            rightChild.unfactorChildren(rootTree, grammarFormatType);
        }

        return rootTree;
    }

    private void unfactorChildren(final NaryTree<String> rootTree, final GrammarFormatType grammarFormatType) {
        // Descend through factored categories
        if (grammarFormatType.isFactored(label.toString())) {
            leftChild.unfactorChildren(rootTree, grammarFormatType);
            rightChild.unfactorChildren(rootTree, grammarFormatType);
        } else {
            rootTree.addSubtree(unfactor(grammarFormatType));
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
     * @param factorization The binary factorization method
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> BinaryTree<T> read(final InputStream inputStream, final Class<T> type,
            final Factorization factorization) throws IOException {
        return read(new InputStreamReader(inputStream), type, factorization);
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
        return read(new InputStreamReader(inputStream), type, null);
    }

    /**
     * Reads in an binary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     * @param factorization The binary factorization method
     * @throws IOException if the read fails
     */
    public static <T> BinaryTree<T> read(final InputStream inputStream, final LabelParser<T> labelParser,
            final Factorization factorization) throws IOException {
        return read(new InputStreamReader(inputStream), labelParser, factorization);
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
        return read(new InputStreamReader(inputStream), labelParser, null);
    }

    /**
     * Reads in an binary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @param type The node label class
     * @param factorization The binary factorization method
     * @return the tree
     */
    public static <T> BinaryTree<T> read(final String string, final Class<T> type, final Factorization factorization) {
        try {
            return read(new StringReader(string), type, factorization);
        } catch (final IOException e) {
            // A StringReader shouldn't ever throw an IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads in an binary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static <T> BinaryTree<T> read(final String string, final Class<T> type) {
        try {
            return read(new StringReader(string), type, null);
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
     * @param factorization The binary factorization method
     * @return the tree
     */
    public static <T> BinaryTree<T> read(final String string, final LabelParser<T> labelParser,
            final Factorization factorization) {
        try {
            return read(new StringReader(string), labelParser, factorization);
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
            return read(new StringReader(string), labelParser, null);
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
    public static <T> BinaryTree<T> read(final Reader reader, final Class<T> type, final Factorization factorization)
            throws IOException {

        try {
            final Constructor<T> labelConstructor = type.getConstructor(new Class[] { String.class });
            final LabelParser<T> labelParser = new LabelParser<T>() {
                @Override
                public T parse(final String label) throws Exception {
                    return labelConstructor.newInstance(new Object[] { label });
                }
            };
            return read(reader, labelParser, factorization);
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
    public static <T> BinaryTree<T> read(final Reader reader, final LabelParser<T> labelParser,
            final Factorization factorization) throws IOException {
        char c;

        // Discard any spaces or end-of-line characters
        while ((c = (char) reader.read()) == '\n' || c == '\r' || c == ' ') {
        }

        // We expect the first character to be '('
        if (c != '(') {
            throw new IllegalArgumentException("Bad tree format. Expected '(' but found '" + c + "'");
        }
        return readSubtree(reader, null, labelParser, factorization);
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
            final LabelParser<T> labelParser, final Factorization factorization) throws IOException {

        try {
            // Recursively read a tree from the reader.
            // TODO: This could probably be simplified and perhaps optimized
            final StringBuilder rootToken = new StringBuilder();

            // Read the root token
            for (char c = (char) reader.read(); c != ' ' && c != ')'; c = (char) reader.read()) {
                rootToken.append(c);
            }

            final BinaryTree<T> tree = new BinaryTree<T>(labelParser.parse(rootToken.toString()), factorization);

            StringBuilder childToken = new StringBuilder();

            // Read tokens and add children until we come to a ')'
            for (char c = (char) reader.read(); c != ')'; c = (char) reader.read()) {
                // Parse any subtrees we find
                if (c == '(') {
                    tree.internalAddSubtree(readSubtree(reader, tree, labelParser, factorization));
                }
                // Add any tokens we find
                else if (c == ' ') {
                    if (childToken.length() > 0) {
                        tree.addChild(new BinaryTree<T>(labelParser.parse(childToken.toString()), factorization));
                        childToken = new StringBuilder();
                    }
                } else {
                    childToken.append(c);
                }
            }

            if (childToken.length() > 0) {
                tree.addChild(new BinaryTree<T>(labelParser.parse(childToken.toString()), factorization));
            }

            tree.parent = parent;
            return tree;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static enum Factorization {
        LEFT, RIGHT
    }
}
