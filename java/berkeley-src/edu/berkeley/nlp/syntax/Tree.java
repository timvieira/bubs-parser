package edu.berkeley.nlp.syntax;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.berkeley.nlp.util.MyMethod;

/**
 * Represent linguistic trees, with each node consisting of a label and a list of children.
 * 
 * @author Dan Klein
 * 
 *         Added function to get a map of subtrees to constituents.
 */
public class Tree<L> implements Serializable, Comparable<Tree<L>>, Iterable<Tree<L>> {

    private static final long serialVersionUID = 1L;

    L label;

    List<Tree<L>> children;

    public void setChild(final int i, final Tree<L> child) {
        children.set(i, child);
    }

    public void setChildren(final List<Tree<L>> c) {
        this.children = c;
    }

    public List<Tree<L>> getChildren() {
        return children;
    }

    public Tree<L> getChild(final int i) {
        return children.get(i);
    }

    public L getLabel() {
        return label;
    }

    public boolean isLeaf() {
        return getChildren().isEmpty();
    }

    public boolean isPreTerminal() {
        return getChildren().size() == 1 && getChildren().get(0).isLeaf();
    }

    public List<L> getYield() {
        final List<L> yield = new ArrayList<L>();
        appendYield(this, yield);
        return yield;
    }

    private static <L> int appendConstituent(final Tree<L> tree, final Map<Tree<L>, Constituent<L>> constituents,
            final int index) {
        if (tree.isLeaf()) {
            final Constituent<L> c = new Constituent<L>(tree.getLabel(), index, index);
            constituents.put(tree, c);
            return 1; // Length of a leaf constituent
        }

        int nextIndex = index;
        for (final Tree<L> kid : tree.getChildren()) {
            nextIndex += appendConstituent(kid, constituents, nextIndex);
        }
        final Constituent<L> c = new Constituent<L>(tree.getLabel(), index, nextIndex - 1);
        constituents.put(tree, c);
        return nextIndex - index; // Length of a leaf constituent
    }

    private static <L> int appendConstituent(final Tree<L> tree, final Collection<Constituent<L>> constituents,
            final int index) {
        if (tree.isLeaf() || tree.isPreTerminal()) {
            final Constituent<L> c = new Constituent<L>(tree.getLabel(), index, index);
            constituents.add(c);
            return 1; // Length of a leaf constituent
        }

        int nextIndex = index;
        for (final Tree<L> kid : tree.getChildren()) {
            nextIndex += appendConstituent(kid, constituents, nextIndex);
        }
        final Constituent<L> c = new Constituent<L>(tree.getLabel(), index, nextIndex - 1);
        constituents.add(c);
        return nextIndex - index; // Length of a leaf constituent
    }

    private static <L> void appendNonTerminals(final Tree<L> tree, final List<Tree<L>> yield) {
        if (tree.isLeaf()) {

            return;
        }
        yield.add(tree);
        for (final Tree<L> child : tree.getChildren()) {
            appendNonTerminals(child, yield);
        }
    }

    public List<Tree<L>> getTerminals() {
        final List<Tree<L>> yield = new ArrayList<Tree<L>>();
        appendTerminals(this, yield);
        return yield;
    }

    private static <L> void appendTerminals(final Tree<L> tree, final List<Tree<L>> yield) {
        if (tree.isLeaf()) {
            yield.add(tree);
            return;
        }
        for (final Tree<L> child : tree.getChildren()) {
            appendTerminals(child, yield);
        }
    }

    /**
     * Clone the structure of the tree. Unfortunately, the new labels are copied by reference from the current tree.
     * 
     * @return A shallow copy of this {@link Tree}
     */
    public Tree<L> shallowClone() {
        final ArrayList<Tree<L>> newChildren = new ArrayList<Tree<L>>(children.size());
        for (final Tree<L> child : children) {
            newChildren.add(child.shallowClone());
        }
        return new Tree<L>(label, newChildren);
    }

    /**
     * Return a clone of just the root node of this tree (with no children)
     * 
     * @return a clone of just the root node of this tree (with no children)
     */
    public Tree<L> shallowCloneJustRoot() {

        return new Tree<L>(label);
    }

    private static <L> void appendYield(final Tree<L> tree, final List<L> yield) {
        if (tree.isLeaf()) {
            yield.add(tree.getLabel());
            return;
        }
        for (final Tree<L> child : tree.getChildren()) {
            appendYield(child, yield);
        }
    }

    public List<L> getPreTerminalYield() {
        final List<L> yield = new ArrayList<L>();
        appendPreTerminalYield(this, yield);
        return yield;
    }

    private static <L> void appendPreTerminalYield(final Tree<L> tree, final List<L> yield) {
        if (tree.isPreTerminal()) {
            yield.add(tree.getLabel());
            return;
        }
        for (final Tree<L> child : tree.getChildren()) {
            appendPreTerminalYield(child, yield);
        }
    }

    private static <L> void appendPreTerminals(final Tree<L> tree, final List<Tree<L>> yield) {
        if (tree.isPreTerminal()) {
            yield.add(tree);
            return;
        }
        for (final Tree<L> child : tree.getChildren()) {
            appendPreTerminals(child, yield);
        }
    }

    private static <L> void appendTreesOfDepth(final Tree<L> tree, final List<Tree<L>> yield, final int depth) {
        if (tree.getDepth() == depth) {
            yield.add(tree);
            return;
        }
        for (final Tree<L> child : tree.getChildren()) {
            appendTreesOfDepth(child, yield, depth);
        }
    }

    public List<Tree<L>> getPreOrderTraversal() {
        final ArrayList<Tree<L>> traversal = new ArrayList<Tree<L>>();
        traversalHelper(this, traversal, true);
        return traversal;
    }

    public List<Tree<L>> getPostOrderTraversal() {
        final ArrayList<Tree<L>> traversal = new ArrayList<Tree<L>>();
        traversalHelper(this, traversal, false);
        return traversal;
    }

    private static <L> void traversalHelper(final Tree<L> tree, final List<Tree<L>> traversal, final boolean preOrder) {
        if (preOrder)
            traversal.add(tree);
        for (final Tree<L> child : tree.getChildren()) {
            traversalHelper(child, traversal, preOrder);
        }
        if (!preOrder)
            traversal.add(tree);
    }

    public int getDepth() {
        int maxDepth = 0;
        for (final Tree<L> child : children) {
            final int depth = child.getDepth();
            if (depth > maxDepth)
                maxDepth = depth;
        }
        return maxDepth + 1;
    }

    public int size() {
        int sum = 0;
        for (final Tree<L> child : children) {
            sum += child.size();
        }
        return sum + 1;
    }

    public List<Tree<L>> getAtDepth(final int depth) {
        final List<Tree<L>> yield = new ArrayList<Tree<L>>();
        appendAtDepth(depth, this, yield);
        return yield;
    }

    private static <L> void appendAtDepth(final int depth, final Tree<L> tree, final List<Tree<L>> yield) {
        if (depth < 0)
            return;
        if (depth == 0) {
            yield.add(tree);
            return;
        }
        for (final Tree<L> child : tree.getChildren()) {
            appendAtDepth(depth - 1, child, yield);
        }
    }

    public void setLabel(final L label) {
        this.label = label;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toStringBuilder(sb);
        return sb.toString();
    }

    public void toStringBuilder(final StringBuilder sb) {
        if (!isLeaf())
            sb.append('(');
        if (getLabel() != null) {
            sb.append(getLabel());
        }
        if (!isLeaf()) {
            for (final Tree<L> child : getChildren()) {
                sb.append(' ');
                child.toStringBuilder(sb);
            }
            sb.append(')');
        }
    }

    /**
     * Same as toString(), but escapes terminals like so: ( becomes -LRB- ) becomes -RRB- \ becomes -BACKSLASH- ("\"
     * does not occur in PTB; this is our own convention) This is useful because otherwise it's hard to tell a "("
     * terminal from the tree's bracket structure, or tell an escaping \ from a literal.
     */
    public String toEscapedString() {
        final StringBuilder sb = new StringBuilder();
        toStringBuilderEscaped(sb);
        return sb.toString();
    }

    public void toStringBuilderEscaped(final StringBuilder sb) {
        if (!isLeaf())
            sb.append('(');
        if (getLabel() != null) {
            if (isLeaf()) {
                String escapedLabel = getLabel().toString();
                escapedLabel = escapedLabel.replaceAll("\\(", "-LRB-");
                escapedLabel = escapedLabel.replaceAll("\\)", "-RRB-");
                escapedLabel = escapedLabel.replaceAll("\\\\", "-BACKSLASH-");
                sb.append(escapedLabel);
            } else {
                sb.append(getLabel());
            }
        }
        if (!isLeaf()) {
            for (final Tree<L> child : getChildren()) {
                sb.append(' ');
                child.toStringBuilderEscaped(sb);
            }
            sb.append(')');
        }
    }

    public Tree(final L label, final List<Tree<L>> children) {
        this.label = label;
        this.children = children;
    }

    public Tree(final L label) {
        this.label = label;
        this.children = Collections.emptyList();
    }

    /**
     * Get the set of all subtrees inside the tree by returning a tree rooted at each node. These are <i>not</i> copies,
     * but all share structure. The tree is regarded as a subtree of itself.
     * 
     * @return the <code>Set</code> of all subtrees in the tree.
     */
    public Set<Tree<L>> subTrees() {
        return (Set<Tree<L>>) subTrees(new HashSet<Tree<L>>());
    }

    /**
     * Get the list of all subtrees inside the tree by returning a tree rooted at each node. These are <i>not</i>
     * copies, but all share structure. The tree is regarded as a subtree of itself.
     * 
     * @return the <code>List</code> of all subtrees in the tree.
     */
    public List<Tree<L>> subTreeList() {
        return (List<Tree<L>>) subTrees(new ArrayList<Tree<L>>());
    }

    /**
     * Add the set of all subtrees inside a tree (including the tree itself) to the given <code>Collection</code>.
     * 
     * @param n A collection of nodes to which the subtrees will be added
     * @return The collection parameter with the subtrees added
     */
    public Collection<Tree<L>> subTrees(final Collection<Tree<L>> n) {
        n.add(this);
        final List<Tree<L>> kids = getChildren();
        for (final Tree<L> kid : kids) {
            kid.subTrees(n);
        }
        return n;
    }

    /**
     * Returns an iterator over the nodes of the tree. This method implements the <code>iterator()</code> method
     * required by the <code>Collections</code> interface. It does a preorder (children after node) traversal of the
     * tree. (A possible extension to the class at some point would be to allow different traversal orderings via
     * variant iterators.)
     * 
     * @return An iterator over the nodes of the tree
     */
    public Iterator<Tree<L>> iterator() {
        return new TreeIterator();
    }

    private class TreeIterator implements Iterator<Tree<L>> {

        private List<Tree<L>> treeStack;

        private TreeIterator() {
            treeStack = new ArrayList<Tree<L>>();
            treeStack.add(Tree.this);
        }

        public boolean hasNext() {
            return (!treeStack.isEmpty());
        }

        public Tree<L> next() {
            final int lastIndex = treeStack.size() - 1;
            final Tree<L> tr = treeStack.remove(lastIndex);
            final List<Tree<L>> kids = tr.getChildren();
            // so that we can efficiently use one List, we reverse them
            for (int i = kids.size() - 1; i >= 0; i--) {
                treeStack.add(kids.get(i));
            }
            return tr;
        }

        /**
         * Not supported
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Applies a transformation to all labels in the tree and returns the resulting tree.
     * 
     * @param <O> Output type of the transformation
     * @param trans The transformation to apply
     * @return Transformed tree
     */
    public <O> Tree<O> transformNodes(final MyMethod<L, O> trans) {
        final ArrayList<Tree<O>> newChildren = new ArrayList<Tree<O>>(children.size());
        for (final Tree<L> child : children) {
            newChildren.add(child.transformNodes(trans));
        }
        return new Tree<O>(trans.call(label), newChildren);
    }

    /**
     * Applies a transformation to all nodes in the tree and returns the resulting tree. Different from
     * <code>transformNodes</code> in that you get the full node and not just the label
     * 
     * @param <O>
     * @param trans
     * @return The new tree resulting from the specified transform
     */
    public <O> Tree<O> transformNodesUsingNode(final MyMethod<Tree<L>, O> trans) {
        final ArrayList<Tree<O>> newChildren = new ArrayList<Tree<O>>(children.size());
        final O newLabel = trans.call(this);
        for (final Tree<L> child : children) {
            newChildren.add(child.transformNodesUsingNode(trans));
        }
        return new Tree<O>(newLabel, newChildren);
    }

    public <O> Tree<O> transformNodesUsingNodePostOrder(final MyMethod<Tree<L>, O> trans) {
        final ArrayList<Tree<O>> newChildren = new ArrayList<Tree<O>>(children.size());
        for (final Tree<L> child : children) {
            newChildren.add(child.transformNodesUsingNode(trans));
        }
        final O newLabel = trans.call(this);
        return new Tree<O>(newLabel, newChildren);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        for (final Tree<L> child : children) {
            result = prime * result + ((child == null) ? 0 : child.hashCode());
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass() || !(obj instanceof Tree)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final Tree<L> other = (Tree<L>) obj;

        if (!this.label.equals(other.label)) {
            return false;
        }

        if (this.getChildren().size() != other.getChildren().size()) {
            return false;
        }

        for (int i = 0; i < getChildren().size(); ++i) {

            if (!getChildren().get(i).equals(other.getChildren().get(i)))
                return false;
        }

        return true;
    }

    public int compareTo(final Tree<L> o) {
        if (!(o.getLabel() instanceof Comparable && getLabel() instanceof Comparable))
            throw new IllegalArgumentException("Tree labels are not comparable");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final int cmp = ((Comparable) o.getLabel()).compareTo(getLabel());
        if (cmp != 0)
            return cmp;
        final int cmp2 = Double.compare(this.getChildren().size(), o.getChildren().size());
        if (cmp2 != 0)
            return cmp2;
        for (int i = 0; i < getChildren().size(); ++i) {

            final int cmp3 = getChildren().get(i).compareTo(o.getChildren().get(i));
            if (cmp3 != 0)
                return cmp3;
        }
        return 0;

    }

    public boolean isPhrasal() {
        return getYield().size() > 1;
    }

    public Constituent<L> getLeastCommonAncestorConstituent(final int i, final int j) {
        final List<L> yield = getYield();
        final Constituent<L> leastCommonAncestorConstituentHelper = getLeastCommonAncestorConstituentHelper(this, 0,
                yield.size(), i, j);

        return leastCommonAncestorConstituentHelper;
    }

    public Tree<L> getTopTreeForSpan(final int i, final int j) {
        final List<L> yield = getYield();
        return getTopTreeForSpanHelper(this, 0, yield.size(), i, j);
    }

    private static <L> Tree<L> getTopTreeForSpanHelper(final Tree<L> tree, final int start, final int end, final int i,
            final int j) {

        assert i <= j;
        if (start == i && end == j) {
            assert tree.getLabel().toString().matches("\\w+");
            return tree;
        }

        final Queue<Tree<L>> queue = new LinkedList<Tree<L>>();
        queue.addAll(tree.getChildren());
        int currStart = start;
        while (!queue.isEmpty()) {
            final Tree<L> remove = queue.remove();
            final List<L> currYield = remove.getYield();
            final int currEnd = currStart + currYield.size();
            if (currStart <= i && currEnd >= j)
                return getTopTreeForSpanHelper(remove, currStart, currEnd, i, j);
            currStart += currYield.size();
        }
        return null;
    }

    private static <L> Constituent<L> getLeastCommonAncestorConstituentHelper(final Tree<L> tree, final int start,
            final int end, final int i, final int j) {

        if (start == i && end == j)
            return new Constituent<L>(tree.getLabel(), start, end);

        final Queue<Tree<L>> queue = new LinkedList<Tree<L>>();
        queue.addAll(tree.getChildren());
        int currStart = start;
        while (!queue.isEmpty()) {
            final Tree<L> remove = queue.remove();
            final List<L> currYield = remove.getYield();
            final int currEnd = currStart + currYield.size();
            if (currStart <= i && currEnd >= j) {
                final Constituent<L> leastCommonAncestorConstituentHelper = getLeastCommonAncestorConstituentHelper(
                        remove, currStart, currEnd, i, j);
                if (leastCommonAncestorConstituentHelper != null) {
                    return leastCommonAncestorConstituentHelper;
                }

                break;
            }
            currStart += currYield.size();
        }
        return new Constituent<L>(tree.getLabel(), start, end);
    }

    public boolean hasUnariesOtherThanRoot() {
        assert children.size() == 1;
        return hasUnariesHelper(children.get(0));

    }

    private boolean hasUnariesHelper(final Tree<L> tree) {
        if (tree.isPreTerminal())
            return false;
        if (tree.getChildren().size() == 1)
            return true;
        for (final Tree<L> child : tree.getChildren()) {
            if (hasUnariesHelper(child))
                return true;
        }
        return false;
    }

    public boolean hasUnaryChain() {
        return hasUnaryChainHelper(this, false);
    }

    private boolean hasUnaryChainHelper(final Tree<L> tree, final boolean unaryAbove) {
        boolean result = false;
        if (tree.getChildren().size() == 1) {
            if (unaryAbove) {
                return true;
            } else if (tree.getChildren().get(0).isPreTerminal()) {
                return false;
            } else {
                return hasUnaryChainHelper(tree.getChildren().get(0), true);
            }
        }

        for (final Tree<L> child : tree.getChildren()) {
            if (!child.isPreTerminal())
                result = result || hasUnaryChainHelper(child, false);
        }
        return result;
    }

    public void removeUnaryChains() {
        removeUnaryChainHelper(this, null);
    }

    private void removeUnaryChainHelper(Tree<L> tree, final Tree<L> parent) {
        if (tree.isLeaf())
            return;
        if (tree.getChildren().size() == 1 && !tree.isPreTerminal()) {
            if (parent != null) {
                tree = tree.getChildren().get(0);
                parent.getChildren().set(0, tree);
                removeUnaryChainHelper(tree, parent);
            } else
                removeUnaryChainHelper(tree.getChildren().get(0), tree);
        } else {
            for (final Tree<L> child : tree.getChildren()) {
                if (!child.isPreTerminal())
                    removeUnaryChainHelper(child, null);
            }
        }
    }

}
