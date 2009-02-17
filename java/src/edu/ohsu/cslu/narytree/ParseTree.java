package edu.ohsu.cslu.narytree;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.ohsu.cslu.common.Vocabulary;

/**
 * N-ary parse tree implementation based on {@link NaryTree} structure.
 * 
 * @author Aaron Dunlop
 * @since Sep 25, 2008
 * 
 *        $Id$
 */
public final class ParseTree extends BaseNaryTree<String>
{
    private final static long serialVersionUID = 369752896212698724L;

    private final Vocabulary vocabulary;

    public ParseTree(final String label, final BaseNaryTree<String> parent, Vocabulary vocabulary)
    {
        super(vocabulary.map(label), parent);
        this.vocabulary = vocabulary;
    }

    protected ParseTree(final String label, final BaseNaryTree<String> parent)
    {
        super(((ParseTree) parent).vocabulary.map(label), parent);
        this.vocabulary = ((ParseTree) parent).vocabulary;
    }

    public ParseTree(int label, BaseNaryTree<String> parent)
    {
        super(label, parent);
        this.vocabulary = ((ParseTree) parent).vocabulary;
    }

    public ParseTree(String label, Vocabulary vocabulary)
    {
        super(vocabulary.map(label), null);
        this.vocabulary = vocabulary;
    }

    public String label()
    {
        return vocabulary.map(label);
    }

    public String stringLabel()
    {
        return vocabulary.map(label);
    }

    /**
     * Returns the 'head' descendant of this tree, using a head-percolation rule-set of the standard
     * Magerman/Charniak form.
     * 
     * @param ruleset Head-percolation ruleset
     * @return head descendant
     */
    public ParseTree headDescendant(HeadPercolationRuleset ruleset)
    {
        if (isLeaf())
        {
            return this;
        }

        int[] childArray = childArray();

        // Special-case for unary productions
        if (childArray.length == 1)
        {
            return ((ParseTree) childList.get(0)).headDescendant(ruleset);
        }

        // TODO: This is terribly inefficient - it requires mapping each child (O(n) and iterating
        // through childList (O(n)) for each node. A total of O(n^2)...)
        int index = ruleset.headChild(label(), vocabulary.map(childArray));
        return ((ParseTree) childList.get(index)).headDescendant(ruleset);
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
        for (ParseTree tree = (ParseTree) parent; tree != null && tree.headDescendant(ruleset) == this; tree = (ParseTree) tree.parent)
        {
            level--;
        }
        return level;
    }

    /**
     * Type-strengthen return-type
     */
    @Override
    public ParseTree addChild(final String childLabel)
    {
        return addChild(vocabulary.map(childLabel));
    }

    /**
     * Type-strengthen return-type
     */
    @Override
    public ParseTree addChild(final int childLabel)
    {
        return (ParseTree) super.addChild(childLabel);
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
    public void addSubtree(NaryTree<String> subtree)
    {
        super.addSubtree((BaseNaryTree<String>) subtree);
    }

    @Override
    public Iterator<String> inOrderLabelIterator()
    {
        return new StringIterator(inOrderIntegerIterator(), vocabulary);
    }

    @Override
    public Iterator<String> postOrderLabelIterator()
    {
        return new StringIterator(postOrderIntegerIterator(), vocabulary);
    }

    @Override
    public Iterator<String> preOrderLabelIterator()
    {
        return new StringIterator(preOrderIntegerIterator(), vocabulary);
    }

    @Override
    public boolean removeChild(String childLabel)
    {
        return removeChild(vocabulary.map(childLabel));
    }

    @Override
    public void removeChildren(String[] childLabels)
    {
        for (String childLabel : childLabels)
        {
            removeChild(vocabulary.map(childLabel));
        }
    }

    @Override
    public boolean removeSubtree(String childLabel)
    {
        return removeSubtree(vocabulary.map(childLabel));
    }

    @Override
    public ParseTree subtree(String childLabel)
    {
        return (ParseTree) subtree(vocabulary.map(childLabel));
    }

    @Override
    public List<String> childLabels()
    {
        ArrayList<String> list = new ArrayList<String>(childList.size());
        for (BaseNaryTree<String> child : childList)
        {
            list.add(vocabulary.map(child.label));
        }
        return list;
    }

    private static class StringIterator extends BaseLabelIterator implements Iterator<String>
    {
        private final Vocabulary vocabulary;

        public StringIterator(Iterator<Integer> intIterator, Vocabulary vocabulary)
        {
            super(intIterator);
            this.vocabulary = vocabulary;
        }

        @Override
        public String next()
        {
            return vocabulary.map(intIterator.next().intValue());
        }
    }

    /**
     * Reads in an ParseTree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @param vocabulary Vocabulary to initialize the tree using
     * @return the tree
     * @throws IOException if the read fails
     */
    public static ParseTree read(InputStream inputStream, Vocabulary vocabulary) throws IOException
    {
        return (ParseTree) read(new InputStreamReader(inputStream), ParseTree.class, vocabulary);
    }

    /**
     * Reads in an ParseTree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @param vocabulary Grammar to initialize the tree using
     * @return the tree
     */
    public static ParseTree read(String string, Vocabulary vocabulary)
    {
        return (ParseTree) read(string, ParseTree.class, vocabulary);
    }

    /**
     * Reads in an ParseTree from a standard parenthesis-bracketed representation
     * 
     * @param reader The reader to read from
     * @param vocabulary Grammar to initialize the tree using
     * @return the tree
     * @throws IOException if the read fails
     */
    public static ParseTree read(Reader reader, Vocabulary vocabulary) throws IOException
    {
        return (ParseTree) read(reader, ParseTree.class, vocabulary);
    }
}
