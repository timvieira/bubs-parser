package edu.ohsu.cslu.parsing.trees;

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
 * A simple string-only n-ary tree implementation. Does not implement pq-gram or other distance
 * metrics. Useful for reading and writing sentences without regard to the grammar they're
 * represented in (if the grammar is known, see {@link ParseTree} instead.
 * 
 * @author Aaron Dunlop
 * @since Sep 29, 2008
 * 
 *        $Id$
 */
public class StringNaryTree implements NaryTree<String>, Serializable
{
    private final static long serialVersionUID = 369752896212698723L;

    /** Label of the root node */
    protected final String label;

    /** Parent node (if any) */
    protected StringNaryTree parent;

    /**
     * All the children of this tree
     */
    protected final LinkedList<StringNaryTree> childList = new LinkedList<StringNaryTree>();

    /** Number of nodes in this tree, including this node and any subtrees */
    protected int size;

    /** Number of leaves in this tree */
    protected int leaves;

    public StringNaryTree(final String label, final StringNaryTree parent)
    {
        this.label = label;
        size = 1;
        leaves = 1;
        this.parent = parent;
    }

    public StringNaryTree(final String label)
    {
        this(label, null);
    }

    public String label()
    {
        return label;
    }

    public String stringLabel()
    {
        return label;
    }

    /**
     * Adds a child to the tree
     * 
     * @param childLabel label of the child
     * @return The newly added subtree
     */
    public StringNaryTree addChild(final String childLabel)
    {
        try
        {
            StringNaryTree child = new StringNaryTree(childLabel, this);
            updateSize(1, isLeaf() ? 0 : 1);
            childList.add(child);

            return child;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addChildren(String[] childLabels)
    {
        for (String child : childLabels)
        {
            addChild(child);
        }
    }

    @Override
    public void addChildren(Collection<String> childLabels)
    {
        for (String child : childLabels)
        {
            addChild(child);
        }
    }

    public void addSubtree(final NaryTree<String> subtree)
    {
        StringNaryTree typedSubtree = (StringNaryTree) subtree;
        final int leavesAdded = typedSubtree.leaves - (isLeaf() ? 1 : 0);
        childList.add(typedSubtree);
        typedSubtree.parent = this;
        updateSize(typedSubtree.size, leavesAdded);
    }

    public boolean removeChild(final String childLabel)
    {
        int i = 0;
        for (Iterator<StringNaryTree> iter = childList.iterator(); iter.hasNext();)
        {
            StringNaryTree child = iter.next();
            if (child.label.equals(childLabel))
            {
                final int leavesRemoved = (childList.size() == 1 || !child.isLeaf()) ? 0 : 1;

                iter.remove();
                child.parent = null;
                childList.addAll(i, child.childList);
                for (StringNaryTree t : child.childList)
                {
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
    public void removeChildren(Collection<String> childLabels)
    {
        for (String childLabel : childLabels)
        {
            removeChild(childLabel);
        }
    }

    @Override
    public void removeChildren(String[] childLabels)
    {
        for (String childLabel : childLabels)
        {
            removeChild(childLabel);
        }
    }

    public boolean removeSubtree(final String childLabel)
    {
        for (Iterator<StringNaryTree> i = childList.iterator(); i.hasNext();)
        {
            StringNaryTree child = i.next();
            if (child.label.equals(childLabel))
            {
                final int leavesRemoved = child.leaves - (isLeaf() ? 1 : 0);
                i.remove();
                updateSize(-child.size, -leavesRemoved);
                return true;
            }
        }
        return false;
    }

    public String[] childArray()
    {
        String[] childArray = new String[childList.size()];
        int i = 0;
        for (StringNaryTree child : childList)
        {
            childArray[i++] = child.label;
        }
        return childArray;
    }

    public List<NaryTree<String>> children()
    {
        return new LinkedList<NaryTree<String>>(childList);
    }

    public boolean isLeaf()
    {
        return childList.isEmpty();
    }

    public int size()
    {
        return size;
    }

    public int depthFromRoot()
    {
        int depth = 0;

        for (StringNaryTree tree = this; tree.parent != null; tree = tree.parent)
        {
            depth++;
        }
        return depth;
    }

    public int leaves()
    {
        return leaves;
    }

    public StringNaryTree parent()
    {
        return parent;
    }

    public StringNaryTree subtree(String childLabel)
    {
        for (StringNaryTree child : childList)
        {
            if (child.label.equals(childLabel))
            {
                return child;
            }
        }
        return null;
    }

    protected void updateSize(final int childrenAdded, final int leavesAdded)
    {
        size += childrenAdded;
        leaves += leavesAdded;
        if (parent != null)
        {
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
    public StringNaryTree headDescendant(HeadPercolationRuleset rules)
    {
        if (isLeaf())
        {
            return this;
        }

        String[] childArray = childArray();

        // Special-case for unary productions
        if (childArray.length == 1)
        {
            return childList.get(0).headDescendant(rules);
        }

        // TODO: This is terribly inefficient - it requires mapping each child (O(n) and iterating
        // through childList (O(n)) for each node. A total of O(n^2)...)
        int index = rules.headChild(label(), childArray);
        return childList.get(index).headDescendant(rules);
    }

    /**
     * TODO: This probably isn't the best way to model head percolation
     * 
     * @param ruleset head-percolation ruleset
     * @return true if this tree is the head of the tree it is rooted in
     */
    public boolean isHeadOfTreeRoot(HeadPercolationRuleset ruleset)
    {
        return headLevel(ruleset) == 0;
    }

    /**
     * TODO: This probably isn't the best way to model head percolation
     * 
     * @param ruleset head-percolation ruleset
     * @return the depth in the tree for which this node is the head (possibly its own depth)
     */
    public int headLevel(HeadPercolationRuleset ruleset)
    {
        if (!isLeaf())
        {
            return -1;
        }

        // TODO: Terribly, horribly inefficient. But (again), we can tune later
        int level = depthFromRoot();
        for (StringNaryTree tree = parent; tree != null && tree.headDescendant(ruleset) == this; tree = tree.parent)
        {
            level--;
        }
        return level;
    }

    @Override
    public Iterator<NaryTree<String>> inOrderIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderList(this, new ArrayList<NaryTree<String>>(size)).iterator();
    }

    private List<NaryTree<String>> inOrderList(StringNaryTree tree, List<NaryTree<String>> list)
    {
        Iterator<StringNaryTree> i = tree.childList.iterator();
        if (i.hasNext())
        {
            inOrderList(i.next(), list);
        }

        list.add(tree);

        while (i.hasNext())
        {
            inOrderList(i.next(), list);
        }

        return list;
    }

    @Override
    public Iterator<String> inOrderLabelIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderLabelList(this, new ArrayList<String>(size)).iterator();
    }

    @Override
    public Iterator<NaryTree<String>> preOrderIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderList(this, new ArrayList<NaryTree<String>>(size)).iterator();
    }

    private List<NaryTree<String>> preOrderList(final StringNaryTree tree, final List<NaryTree<String>> list)
    {
        list.add(tree);
        for (StringNaryTree child : tree.childList)
        {
            preOrderList(child, list);
        }

        return list;
    }

    @Override
    public Iterator<String> preOrderLabelIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderLabelList(this, new ArrayList<String>(size)).iterator();
    }

    @Override
    public Iterator<NaryTree<String>> postOrderIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderList(this, new ArrayList<NaryTree<String>>(size)).iterator();
    }

    private List<NaryTree<String>> postOrderList(final StringNaryTree tree, final List<NaryTree<String>> list)
    {
        for (StringNaryTree child : tree.childList)
        {
            postOrderList(child, list);
        }
        list.add(tree);

        return list;
    }

    @Override
    public Iterator<String> postOrderLabelIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderLabelList(this, new ArrayList<String>(size)).iterator();
    }

    @Override
    public List<String> childLabels()
    {
        ArrayList<String> list = new ArrayList<String>(childList.size());
        for (StringNaryTree child : childList)
        {
            list.add(child.label);
        }
        return list;
    }

    private List<String> inOrderLabelList(StringNaryTree tree, List<String> list)
    {
        Iterator<StringNaryTree> i = tree.childList.iterator();
        if (i.hasNext())
        {
            inOrderLabelList(i.next(), list);
        }

        list.add(tree.label);

        while (i.hasNext())
        {
            inOrderLabelList(i.next(), list);
        }

        return list;
    }

    public String[] inOrderArray()
    {
        return toStringArray(inOrderLabelIterator());
    }

    private List<String> preOrderLabelList(StringNaryTree tree, List<String> list)
    {
        list.add(tree.label);
        for (StringNaryTree child : tree.childList)
        {
            preOrderLabelList(child, list);
        }

        return list;
    }

    public String[] preOrderArray()
    {
        return toStringArray(preOrderLabelIterator());
    }

    private List<String> postOrderLabelList(StringNaryTree tree, List<String> list)
    {
        for (StringNaryTree child : tree.childList)
        {
            postOrderLabelList(child, list);
        }
        list.add(tree.label);

        return list;
    }

    public String[] postOrderArray()
    {
        return toStringArray(postOrderLabelIterator());
    }

    private String[] toStringArray(Iterator<String> iter)
    {
        String[] array = new String[size];
        for (int i = 0; i < size; i++)
        {
            array[i] = iter.next();
        }
        return array;
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static StringNaryTree read(InputStream inputStream) throws IOException
    {
        return read(new InputStreamReader(inputStream));
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static StringNaryTree read(String string)
    {
        try
        {
            return read(new StringReader(string));
        }
        catch (IOException e)
        {
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
    public static StringNaryTree read(Reader reader) throws IOException
    {
        char c;

        // Discard any spaces or end-of-line characters
        while ((c = (char) reader.read()) == '\n' || c == '\r' || c == ' ')
        {}

        // We expect the first character to be '('
        if (c != '(')
        {
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
    private static StringNaryTree readSubtree(Reader reader, StringNaryTree parent) throws IOException
    {
        try
        {
            // Recursively read a tree from the reader.
            // TODO: This could probably be simplified and perhaps optimized
            StringBuilder rootToken = new StringBuilder();

            // Read the root token
            for (char c = (char) reader.read(); c != ' ' && c != ')'; c = (char) reader.read())
            {
                rootToken.append(c);
            }
            StringNaryTree tree = new StringNaryTree(rootToken.toString());

            StringBuilder childToken = new StringBuilder();

            // Read tokens and add children until we come to a ')'
            for (char c = (char) reader.read(); c != ')'; c = (char) reader.read())
            {
                // Parse any subtrees we find
                if (c == '(')
                {
                    tree.addSubtree(readSubtree(reader, tree));
                }
                // Add any tokens we find
                else if (c == ' ')
                {
                    if (childToken.length() > 0)
                    {
                        tree.addChild(childToken.toString());
                        childToken = new StringBuilder();
                    }
                }
                else
                {
                    childToken.append(c);
                }
            }

            if (childToken.length() > 0)
            {
                tree.addChild(childToken.toString());
            }

            tree.parent = parent;
            return tree;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param writer The writer to write to
     * @throws IOException if the write fails
     */
    public void write(OutputStream outputStream) throws IOException
    {
        write(new OutputStreamWriter(outputStream));
    }

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param writer The writer to write to
     * @throws IOException if the write fails
     */
    public void write(Writer writer) throws IOException
    {
        writeSubtree(writer, this);
    }

    protected void writeSubtree(Writer writer, StringNaryTree tree) throws IOException
    {
        if (tree.size > 1)
        {
            writer.write('(');
            writer.write(tree.stringLabel());
            for (Iterator<StringNaryTree> i = tree.childList.iterator(); i.hasNext();)
            {
                writer.write(' ');
                writeSubtree(writer, i.next());
            }
            writer.write(')');
        }
        else
        {
            writer.write(tree.stringLabel());
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof StringNaryTree))
        {
            return false;
        }

        StringNaryTree other = (StringNaryTree) o;

        if ((!other.label.equals(label)) || (other.childList.size() != childList.size()))
        {
            return false;
        }

        final Iterator<StringNaryTree> i1 = childList.iterator();
        final Iterator<StringNaryTree> i2 = other.childList.iterator();
        while (i1.hasNext())
        {
            if (!(i1.next().equals(i2.next())))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString()
    {
        try
        {
            Writer writer = new StringWriter();
            write(writer);
            return writer.toString();
        }
        catch (IOException e)
        {
            return "Exception in toString(): " + e.getMessage();
        }
    }

    protected abstract static class BaseIterator
    {
        protected Iterator<Integer> intIterator;

        public BaseIterator(Iterator<Integer> intIterator)
        {
            this.intIterator = intIterator;
        }

        public boolean hasNext()
        {
            return intIterator.hasNext();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }
}
