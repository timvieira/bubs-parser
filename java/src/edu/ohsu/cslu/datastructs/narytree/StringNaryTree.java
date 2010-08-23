package edu.ohsu.cslu.datastructs.narytree;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * A simple string-only n-ary tree implementation. Does not implement pq-gram or other distance metrics.
 * Useful for reading and writing sentences without regard to the grammar they're represented in.
 * 
 * @author Aaron Dunlop
 * @since Sep 29, 2008
 * 
 *        $Id$
 */
public class StringNaryTree extends BaseNaryTree<String> {

    private final static long serialVersionUID = 369752896212698723L;

    public StringNaryTree(final String label, final StringNaryTree parent) {
        super(label, parent);
    }

    public StringNaryTree(final String label) {
        this(label, null);
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

        final List<String> childLabels = childLabels();

        // Special-case for unary productions
        if (children().size() == 1) {
            return ((StringNaryTree) childList.get(0)).headDescendant(ruleset);
        }

        // TODO: This is terribly inefficient - it requires mapping each child (O(n) and iterating
        // through childList (O(n)) for each node. A total of O(n^2)...)
        final int index = ruleset.headChild(label(), childLabels);
        return ((StringNaryTree) childList.get(index)).headDescendant(ruleset);
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
        for (StringNaryTree tree = (StringNaryTree) parent; tree != null
                && tree.headDescendant(ruleset) == this; tree = (StringNaryTree) tree.parent) {
            level--;
        }
        return level;
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param reader The stream to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static StringNaryTree read(final Reader reader) throws IOException {
        return readSubtree(reader, null);
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static StringNaryTree read(final InputStream inputStream) throws IOException {
        return readSubtree(new InputStreamReader(inputStream), null);
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static StringNaryTree read(final String string) {
        try {
            return readSubtree(new StringReader(string), null);
        } catch (final IOException e) {
            // A StringReader shouldn't ever throw an IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Recursively reads a subtree from a Reader
     * 
     * TODO Share code with the implementation in BaseNaryTree ?
     * 
     * @param reader source
     * @param parent parent tree (if any)
     * @param labelParser Parser appropriate for the labels contained in the tree
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
                        tree.addChild(new StringNaryTree(childToken.toString()));
                        childToken = new StringBuilder();
                    }
                } else {
                    childToken.append(c);
                }
            }

            if (childToken.length() > 0) {
                tree.addChild(new StringNaryTree(childToken.toString()));
            }

            tree.parent = parent;
            return tree;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
