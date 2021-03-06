/**
 * 
 */
package edu.berkeley.nlp.PCFGLA;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;

/**
 * Essentially equivalent to a List<Tree<StateSet>>, but each Tree<StateSet> is re-built every time from the
 * corresponding Tree<String>. This saves a lot of memory at the expense of some time. Most of the code is contained in
 * the subclass StringTreeListIterator.
 * 
 * Beware of the behavior of hasNext(), which deallocates the current tree (the last one returned by next()). This is
 * PRESUMABLY when the current tree is no longer needed, but be careful.
 * 
 * @author Romain Thibaux
 */
public class StateSetTreeList extends AbstractCollection<Tree<StateSet>> {

    List<Tree<StateSet>> trees;
    static short zero = 0, one = 1;

    /**
     * 
     * @param trees
     * @param numStates
     * @param allSplitTheSame This should be true only if all states are being split the same number of times. This
     *            number is taken from numStates[0].
     * @param tagNumberer
     */
    public StateSetTreeList(final List<Tree<String>> trees, final short[] numStates, final boolean allSplitTheSame,
            final Numberer tagNumberer) {
        this.trees = new ArrayList<Tree<StateSet>>();
        for (Tree<String> tree : trees) {
            this.trees.add(stringTreeToStatesetTree(tree, numStates, allSplitTheSame, tagNumberer));
            tree = null;
        }
    }

    public StateSetTreeList(final StateSetTreeList treeList, final short[] numStates, final boolean constant) {
        this.trees = new ArrayList<Tree<StateSet>>();
        for (final Tree<StateSet> tree : treeList.trees) {
            this.trees.add(resizeStateSetTree(tree, numStates, constant));
        }
    }

    public StateSetTreeList() {
        this.trees = new ArrayList<Tree<StateSet>>();
    }

    /*
     * Deallocate the inside and outside score arrays for the whole tree
     */
    void deallocateScores(final Tree<StateSet> tree) {
        tree.label().deallocateScores();
        for (final Tree<StateSet> child : tree.children()) {
            deallocateScores(child);
        }
    }

    public class StateSetTreeListIterator implements Iterator<Tree<StateSet>> {
        Iterator<Tree<StateSet>> stringTreeListIterator;
        Tree<StateSet> currentTree;

        public StateSetTreeListIterator() {
            stringTreeListIterator = trees.iterator();
            currentTree = null;
        }

        public boolean hasNext() {
            // A somewhat crappy API, the tree is deallocated when hasNext() is called,
            // which is PRESUMABLY when the current tree is no longer needed.
            if (currentTree != null) {
                deallocateScores(currentTree);
            }
            return stringTreeListIterator.hasNext();
        }

        public Tree<StateSet> next() {
            currentTree = stringTreeListIterator.next();
            return currentTree;
        }

        public void remove() {
            stringTreeListIterator.remove();
        }
    }

    @Override
    public boolean add(final Tree<StateSet> tree) {
        return trees.add(tree);
    }

    public Tree<StateSet> get(final int i) {
        return trees.get(i);
    }

    @Override
    public int size() {
        return trees.size();
    }

    @Override
    public boolean isEmpty() {
        return trees.isEmpty();
    }

    /*
     * An iterator over the StateSet trees (which are re-built on the fly)
     */
    @Override
    public Iterator<Tree<StateSet>> iterator() {
        return new StateSetTreeListIterator();
    }

    /**
     * Convert a single Tree[String] to Tree[StateSet]
     * 
     * @param tree
     * @param numStates
     * @param tagNumberer
     * @return State-set tree
     */
    public static Tree<StateSet> stringTreeToStatesetTree(final Tree<String> tree, final short[] numStates,
            final boolean allSplitTheSame, final Numberer tagNumberer) {
        final Tree<StateSet> result = stringTreeToStatesetTree(tree, numStates, allSplitTheSame, tagNumberer, false, 0,
                tree.leafLabels().size());
        // set the positions properly:
        final List<StateSet> words = result.leafLabels();
        // for all words in sentence
        for (short position = 0; position < words.size(); position++) {
            words.get(position).from = position;
            words.get(position).to = (short) (position + 1);
        }
        return result;
    }

    private static Tree<StateSet> stringTreeToStatesetTree(final Tree<String> tree, final short[] numStates,
            final boolean allSplitTheSame, final Numberer tagNumberer, final boolean splitRoot, int from, final int to) {
        if (tree.isLeaf()) {
            final StateSet newState = new StateSet(zero, one, tree.label().intern(), (short) from, (short) to);
            return new Tree<StateSet>(newState);
        }
        short label = (short) tagNumberer.number(tree.label());
        if (label < 0)
            label = 0;
        // System.out.println(label + " " +tree.getLabel());
        if (label >= numStates.length) {
            // System.err.println("Have never seen this state before: "+tree.getLabel());
            // StateSet newState = new StateSet(zero, one,
            // tree.getLabel().intern(),(short)from,(short)to);
            // return new Tree<StateSet>(newState);
        }
        short nodeNumStates = (allSplitTheSame || numStates.length <= label) ? numStates[0] : numStates[label];
        if (!splitRoot)
            nodeNumStates = 1;
        final StateSet newState = new StateSet(label, nodeNumStates, null, (short) from, (short) to);
        final Tree<StateSet> newTree = new Tree<StateSet>(newState);
        final ArrayList<Tree<StateSet>> newChildren = new ArrayList<Tree<StateSet>>();
        for (final Tree<String> child : tree.children()) {
            final short length = (short) child.leafLabels().size();
            final Tree<StateSet> newChild = stringTreeToStatesetTree(child, numStates, allSplitTheSame, tagNumberer,
                    true, from, from + length);
            from += length;
            newChildren.add(newChild);
        }
        newTree.setChildren(newChildren);
        return newTree;
    }

    private static Tree<StateSet> resizeStateSetTree(final Tree<StateSet> tree, final short[] numStates,
            final boolean constant) {
        if (tree.isLeaf()) {
            return tree;
        }
        final short state = tree.label().getState();
        final short newNumStates = constant ? numStates[0] : numStates[state];
        final StateSet newState = new StateSet(tree.label(), newNumStates);
        final Tree<StateSet> newTree = new Tree<StateSet>(newState);
        final ArrayList<Tree<StateSet>> newChildren = new ArrayList<Tree<StateSet>>();
        for (final Tree<StateSet> child : tree.children()) {
            newChildren.add(resizeStateSetTree(child, numStates, constant));
        }
        newTree.setChildren(newChildren);
        return newTree;
    }

    /**
     * @param trees
     * @param tagNumberer
     */
    public static void initializeTagNumberer(final List<Tree<String>> trees, final Numberer tagNumberer) {
        final short[] nSub = new short[2];
        nSub[0] = 1;
        nSub[1] = 1;
        for (final Tree<String> tree : trees) {
            stringTreeToStatesetTree(tree, nSub, true, tagNumberer);
        }
    }
}
