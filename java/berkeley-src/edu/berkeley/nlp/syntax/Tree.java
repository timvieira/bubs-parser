package edu.berkeley.nlp.syntax;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import edu.berkeley.nlp.util.MyMethod;

/**
 * Represent linguistic trees, with each node consisting of a label and a list of children.
 * 
 * @author Dan Klein
 * 
 *         Added function to get a map of subtrees to constituents.
 */
public class Tree<L> implements Serializable, Comparable<Tree<L>> {

    private static final long serialVersionUID = 1L;

    L label;

    List<Tree<L>> children;

    public Tree(final L label, final List<Tree<L>> children) {
        this.label = label;
        this.children = children;
    }

    public Tree(final L label) {
        this.label = label;
        this.children = Collections.emptyList();
    }

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
     * @return
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
     * @return
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
     * @return
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        if (!(obj instanceof Tree))
            return false;
        final Tree<L> other = (Tree<L>) obj;
        if (!this.label.equals(other.label))
            return false;
        if (this.getChildren().size() != other.getChildren().size())
            return false;
        for (int i = 0; i < getChildren().size(); ++i) {

            if (!getChildren().get(i).equals(other.getChildren().get(i)))
                return false;
        }
        return true;

    }

    public int compareTo(final Tree<L> o) {
        if (!(o.getLabel() instanceof Comparable && getLabel() instanceof Comparable))
            throw new IllegalArgumentException("Tree labels are not comparable");
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
}
