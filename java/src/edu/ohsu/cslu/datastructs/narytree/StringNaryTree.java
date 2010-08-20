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

/**
 * A simple string-only n-ary tree implementation. Does not implement pq-gram or other distance metrics.
 * Useful for reading and writing sentences without regard to the grammar they're represented in (if the
 * grammar is known, see {@link ParseTree} instead.
 * 
 * @author Aaron Dunlop
 * @since Sep 29, 2008
 * 
 *        $Id$
 */
public class StringNaryTree implements NaryTree<String>, Serializable {

    private final static long serialVersionUID = 369752896212698723L;

    /** Label of the root node */
    protected String label;

    /** Parent node (if any) */
    protected StringNaryTree parent;

    private boolean visited;

    /**
     * All the children of this tree
     */
    protected final LinkedList<StringNaryTree> childList = new LinkedList<StringNaryTree>();

    /** Number of nodes in this tree, including this node and any subtrees */
    protected int size;

    /** Number of leaves in this tree */
    protected int leaves;

    public StringNaryTree(final String label, final StringNaryTree parent) {
        this.label = label;
        size = 1;
        leaves = 1;
        this.parent = parent;
    }

    public StringNaryTree(final String label) {
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
    public StringNaryTree addChild(final String childLabel) {
        try {
            final StringNaryTree child = new StringNaryTree(childLabel, this);
            updateSize(1, isLeaf() ? 0 : 1);
            childList.add(child);

            return child;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
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
        final StringNaryTree typedSubtree = (StringNaryTree) subtree;
        final int leavesAdded = typedSubtree.leaves - (isLeaf() ? 1 : 0);
        childList.add(typedSubtree);
        typedSubtree.parent = this;
        updateSize(typedSubtree.size, leavesAdded);
    }

    public boolean removeChild(final String childLabel) {
        int i = 0;
        for (final Iterator<StringNaryTree> iter = childList.iterator(); iter.hasNext();) {
            final StringNaryTree child = iter.next();
            if (child.label.equals(childLabel)) {
                final int leavesRemoved = (childList.size() == 1 || !child.isLeaf()) ? 0 : 1;

                iter.remove();
                child.parent = null;
                childList.addAll(i, child.childList);
                for (final StringNaryTree t : child.childList) {
                    t.parent = this;
                }
                updateSize(-1, -leavesRemoved);
                return true;
            }
            i++;
        }
        return false;
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
        for (final Iterator<StringNaryTree> i = childList.iterator(); i.hasNext();) {
            final StringNaryTree child = i.next();
            if (child.label.equals(childLabel)) {
                final int leavesRemoved = child.leaves - (isLeaf() ? 1 : 0);
                i.remove();
                updateSize(-child.size, -leavesRemoved);
                return true;
            }
        }
        return false;
    }

    public String[] childArray() {
        final String[] childArray = new String[childList.size()];
        int i = 0;
        for (final StringNaryTree child : childList) {
            childArray[i++] = child.label;
        }
        return childArray;
    }

    public List<StringNaryTree> children() {
        return new LinkedList<StringNaryTree>(childList);
    }

    public boolean isLeaf() {
        return childList.isEmpty();
    }

    public int size() {
        return size;
    }

    public int depthFromRoot() {
        int depth = 0;

        for (StringNaryTree tree = this; tree.parent != null; tree = tree.parent) {
            depth++;
        }
        return depth;
    }

    public int leaves() {
        return leaves;
    }

    public StringNaryTree parent() {
        return parent;
    }

    public StringNaryTree subtree(final String childLabel) {
        for (final StringNaryTree child : childList) {
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
     * Returns the 'head' descendant of this tree, using a head-percolation rule-set of the standard
     * Charniak/Magerman form.
     * 
     * @param ruleset head-percolation ruleset
     * @return head descendant
     */
    public StringNaryTree headDescendant(final HeadPercolationRuleset ruleset) {
        if (isLeaf()) {
            return this;
        }

        final String[] childArray = childArray();

        // Special-case for unary productions
        if (childArray.length == 1) {
            return childList.get(0).headDescendant(ruleset);
        }

        // TODO: This is terribly inefficient - it requires mapping each child (O(n) and iterating
        // through childList (O(n)) for each node. A total of O(n^2)...)
        final int index = ruleset.headChild(label(), childArray);
        return childList.get(index).headDescendant(ruleset);
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
        for (StringNaryTree tree = parent; tree != null && tree.headDescendant(ruleset) == this; tree = tree.parent) {
            level--;
        }
        return level;
    }

    @Override
    public Iterator<NaryTree<String>> inOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderList(this, new ArrayList<NaryTree<String>>(size)).iterator();
    }

    private List<NaryTree<String>> inOrderList(final StringNaryTree tree, final List<NaryTree<String>> list) {
        final Iterator<StringNaryTree> i = tree.childList.iterator();
        if (i.hasNext()) {
            inOrderList(i.next(), list);
        }

        list.add(tree);

        while (i.hasNext()) {
            inOrderList(i.next(), list);
        }

        return list;
    }

    @Override
    public Iterator<String> inOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderLabelList(this, new ArrayList<String>(size)).iterator();
    }

    @Override
    public Iterator<StringNaryTree> preOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderList(this, new ArrayList<StringNaryTree>(size)).iterator();
    }

    private List<StringNaryTree> preOrderList(final StringNaryTree tree, final List<StringNaryTree> list) {
        list.add(tree);
        for (final StringNaryTree child : tree.childList) {
            preOrderList(child, list);
        }

        return list;
    }

    @Override
    public Iterator<String> preOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderLabelList(this, new ArrayList<String>(size)).iterator();
    }

    @Override
    public Iterator<StringNaryTree> postOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderList(this, new ArrayList<StringNaryTree>(size)).iterator();
    }

    private List<StringNaryTree> postOrderList(final StringNaryTree tree, final List<StringNaryTree> list) {
        for (final StringNaryTree child : tree.childList) {
            postOrderList(child, list);
        }
        list.add(tree);

        return list;
    }

    @Override
    public Iterator<String> postOrderLabelIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderLabelList(this, new ArrayList<String>(size)).iterator();
    }

    @Override
    public List<String> childLabels() {
        final ArrayList<String> list = new ArrayList<String>(childList.size());
        for (final StringNaryTree child : childList) {
            list.add(child.label);
        }
        return list;
    }

    private List<String> inOrderLabelList(final StringNaryTree tree, final List<String> list) {
        final Iterator<StringNaryTree> i = tree.childList.iterator();
        if (i.hasNext()) {
            inOrderLabelList(i.next(), list);
        }

        list.add(tree.label);

        while (i.hasNext()) {
            inOrderLabelList(i.next(), list);
        }

        return list;
    }

    public String[] inOrderArray() {
        return toStringArray(inOrderLabelIterator());
    }

    private List<String> preOrderLabelList(final StringNaryTree tree, final List<String> list) {
        list.add(tree.label);
        for (final StringNaryTree child : tree.childList) {
            preOrderLabelList(child, list);
        }

        return list;
    }

    public String[] preOrderArray() {
        return toStringArray(preOrderLabelIterator());
    }

    private List<String> postOrderLabelList(final StringNaryTree tree, final List<String> list) {
        for (final StringNaryTree child : tree.childList) {
            postOrderLabelList(child, list);
        }
        list.add(tree.label);

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
    public static StringNaryTree read(final InputStream inputStream) throws IOException {
        return read(new InputStreamReader(inputStream));
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static StringNaryTree read(final String string) {
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
    public static StringNaryTree read(final Reader reader) throws IOException {
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
    private static StringNaryTree readSubtree(final Reader reader, final StringNaryTree parent)
            throws IOException {
        try {
            // Recursively read a tree from the reader.
            // TODO: This could probably be simplified and perhaps optimized
            final StringBuilder rootToken = new StringBuilder();

            // Read the root token
            for (char c = (char) reader.read(); c != ' ' && c != ')'; c = (char) reader.read()) {
                rootToken.append(c);
            }
            final StringNaryTree tree = new StringNaryTree(rootToken.toString());

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
        writeSubtree(writer, this);
    }

    protected void writeSubtree(final Writer writer, final StringNaryTree tree) throws IOException {
        if (tree.size > 1) {
            writer.write('(');
            writer.write(tree.stringLabel());
            for (final Iterator<StringNaryTree> i = tree.childList.iterator(); i.hasNext();) {
                writer.write(' ');
                writeSubtree(writer, i.next());
            }
            writer.write(')');
        } else {
            writer.write(tree.stringLabel());
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof StringNaryTree)) {
            return false;
        }

        final StringNaryTree other = (StringNaryTree) o;

        if ((!other.label.equals(label)) || (other.childList.size() != childList.size())) {
            return false;
        }

        final Iterator<StringNaryTree> i1 = childList.iterator();
        final Iterator<StringNaryTree> i2 = other.childList.iterator();
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
