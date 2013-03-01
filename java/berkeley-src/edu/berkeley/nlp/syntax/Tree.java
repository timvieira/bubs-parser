package edu.berkeley.nlp.syntax;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represent linguistic trees, with each node consisting of a label and a list of children.
 * 
 * @author Dan Klein
 */
public class Tree<L> implements Serializable {

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

    public void setChildren(final List<Tree<L>> c) {
        this.children = c;
    }

    public List<Tree<L>> children() {
        return children;
    }

    public Tree<L> getChild(final int i) {
        return children.get(i);
    }

    public L label() {
        return label;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isPreTerminal() {
        return children.size() == 1 && children.get(0).isLeaf();
    }

    public List<L> leafLabels() {
        final List<L> yield = new ArrayList<L>();
        appendYield(this, yield);
        return yield;
    }

    private static <L> void appendYield(final Tree<L> tree, final List<L> yield) {
        if (tree.isLeaf()) {
            yield.add(tree.label());
            return;
        }
        for (final Tree<L> child : tree.children()) {
            appendYield(child, yield);
        }
    }

    public List<Tree<L>> leafList() {
        final List<Tree<L>> yield = new ArrayList<Tree<L>>();
        appendTerminals(this, yield);
        return yield;
    }

    private static <L> void appendTerminals(final Tree<L> tree, final List<Tree<L>> yield) {
        if (tree.isLeaf()) {
            yield.add(tree);
            return;
        }
        for (final Tree<L> child : tree.children()) {
            appendTerminals(child, yield);
        }
    }

    /**
     * Clone the structure of the tree. Unfortunately, the new labels are copied by reference from the current tree.
     * 
     * @return A shallow clone of the tree
     */
    public Tree<L> shallowClone() {
        final ArrayList<Tree<L>> newChildren = new ArrayList<Tree<L>>(children.size());
        for (final Tree<L> child : children) {
            newChildren.add(child.shallowClone());
        }
        return new Tree<L>(label, newChildren);
    }

    public List<L> preterminalLabels() {
        final List<L> yield = new ArrayList<L>();
        appendPreTerminalYield(this, yield);
        return yield;
    }

    private static <L> void appendPreTerminalYield(final Tree<L> tree, final List<L> yield) {
        if (tree.isPreTerminal()) {
            yield.add(tree.label());
            return;
        }
        for (final Tree<L> child : tree.children()) {
            appendPreTerminalYield(child, yield);
        }
    }

    public int height() {
        int maxHeight = 0;
        for (final Tree<L> child : children) {
            final int depth = child.height();
            if (depth > maxHeight)
                maxHeight = depth;
        }
        return maxHeight + 1;
    }

    public int size() {
        int sum = 0;
        for (final Tree<L> child : children) {
            sum += child.size();
        }
        return sum + 1;
    }

    /**
     * Returns all subtrees of the specified depth
     * 
     * @param depth
     * @return All subtrees of the specified depth
     */
    public List<Tree<L>> subtrees(final int depth) {
        final List<Tree<L>> yield = new ArrayList<Tree<L>>();
        appendSubtrees(depth, this, yield);
        return yield;
    }

    private static <L> void appendSubtrees(final int depth, final Tree<L> tree, final List<Tree<L>> yield) {
        if (depth < 0) {
            return;
        }

        if (depth == 0) {
            yield.add(tree);
            return;
        }

        for (final Tree<L> child : tree.children()) {
            appendSubtrees(depth - 1, child, yield);
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

    private void toStringBuilder(final StringBuilder sb) {
        if (!isLeaf())
            sb.append('(');
        if (label() != null) {
            sb.append(label());
        }
        if (!isLeaf()) {
            for (final Tree<L> child : children()) {
                sb.append(' ');
                child.toStringBuilder(sb);
            }
            sb.append(')');
        }
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
        if (obj == null || !(obj instanceof Tree)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final Tree<L> other = (Tree<L>) obj;
        if (!this.label.equals(other.label) || this.children().size() != other.children().size()) {
            return false;
        }

        for (int i = 0; i < children().size(); ++i) {
            if (!children().get(i).equals(other.children().get(i)))
                return false;
        }
        return true;
    }
}
