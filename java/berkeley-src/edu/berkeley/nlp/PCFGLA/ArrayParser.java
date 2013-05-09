package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;

import cltool4j.BaseLogger;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.ohsu.cslu.util.IEEEDoubleScaling;

/**
 * Simple mixture parser.
 */
public class ArrayParser {

    private final Lexicon lexicon;
    private final Grammar grammar;

    public ArrayParser(final Grammar grammar, final Lexicon lexicon) {
        this.grammar = grammar;
        this.lexicon = lexicon;
    }

    /**
     * Calculate the inside scores, P(words_i,j|nonterminal_i,j) of a tree given the string if words it should parse to.
     * 
     * @param tree
     * @param noSmoothing
     */
    void insidePass(final Tree<StateSet> tree, final boolean noSmoothing) {

        final ArrayList<Tree<StateSet>> children = tree.children();
        for (final Tree<StateSet> child : children) {
            if (!child.isLeaf()) {
                insidePass(child, noSmoothing);
            }
        }
        final StateSet parent = tree.label();
        final short unsplitParent = parent.getState();
        final int nParentStates = parent.numSubStates();

        if (tree.isPreTerminal()) {
            // Plays a role similar to initializeChart()
            final StateSet wordStateSet = children.get(0).label();
            final double[] lexiconScores = lexicon.score(wordStateSet, unsplitParent, noSmoothing, false);
            if (lexiconScores.length != nParentStates) {
                throw new IllegalArgumentException("Have more scores than substates!" + lexiconScores.length + " "
                        + nParentStates);
            }
            parent.setInsideScores(lexiconScores);
            parent.setInsideScoreScale(IEEEDoubleScaling.scaleArray(lexiconScores, 0));

        } else {
            switch (children.size()) {
            case 0:
                break;

            case 1: {
                final StateSet child = children.get(0).label();
                final short unsplitChild = child.getState();

                final double[] iScores = new double[nParentStates];
                final Grammar.PackedUnaryRule packedUnaryRule = grammar.getPackedUnaryScores(unsplitParent,
                        unsplitChild);

                double max = Double.NEGATIVE_INFINITY;
                for (int i = 0, j = 0; i < packedUnaryRule.ruleScores.length; i++, j += 2) {
                    final short childSplit = packedUnaryRule.substates[j];

                    final double childInside = child.insideScore(childSplit);
                    if (childInside == 0) {
                        continue;
                    }

                    final short parentSplit = packedUnaryRule.substates[j + 1];
                    final double score = iScores[parentSplit] + packedUnaryRule.ruleScores[i] * childInside;

                    iScores[parentSplit] = score;
                    if (score > max) {
                        max = score;
                    }
                }

                parent.setInsideScores(iScores);
                parent.setInsideScoreScale(IEEEDoubleScaling.scaleArray(iScores, child.insideScoreScale(), max));
                break;
            }

            case 2: {
                final StateSet leftChild = children.get(0).label();
                final StateSet rightChild = children.get(1).label();
                final short unsplitLeftChild = leftChild.getState();
                final short unsplitRightChild = rightChild.getState();

                final Grammar.PackedBinaryRule packedBinaryRule = grammar.getPackedBinaryScores(unsplitParent,
                        unsplitLeftChild, unsplitRightChild);

                final double[] iScores2 = new double[nParentStates];
                double max = Double.NEGATIVE_INFINITY;

                for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {

                    final short leftChildSplit = packedBinaryRule.substates[j];
                    final double leftChildInside = leftChild.insideScore(leftChildSplit);
                    if (leftChildInside == 0) {
                        continue;
                    }

                    final short rightChildSplit = packedBinaryRule.substates[j + 1];
                    final double rightChildInside = rightChild.insideScore(rightChildSplit);
                    if (rightChildInside == 0) {
                        continue;
                    }

                    final short parentSplit = packedBinaryRule.substates[j + 2];
                    final double score = iScores2[parentSplit] + packedBinaryRule.ruleScores[i] * leftChildInside
                            * rightChildInside;

                    iScores2[parentSplit] = score;
                    if (score > max) {
                        max = score;
                    }
                }

                parent.setInsideScores(iScores2);
                parent.setInsideScoreScale(IEEEDoubleScaling.scaleArray(iScores2, leftChild.insideScoreScale()
                        + rightChild.insideScoreScale(), max));
                break;
            }

            default:
                throw new IllegalArgumentException("Malformed tree: more than two children");
            }
        }
    }

    /**
     * Calculate the outside scores of a tree; that is, P(nonterminal_i,j|words_0,i; words_j,end). It is calculated from
     * the inside scores of the tree.
     * 
     * Note: when calling this, call setRootOutsideScore() first.
     * 
     * @param tree
     */
    private void outsidePass(final Tree<StateSet> tree) {

        final ArrayList<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short unsplitParent = parent.getState();

        final double[] parentOutsideScores = parent.outsideScores();

        switch (children.size()) {

        case 1: {
            final StateSet child = children.get(0).label();
            final short unsplitChild = child.getState();
            final int nChildStates = child.numSubStates();
            final double[] oScores = new double[nChildStates];
            double max = Double.NEGATIVE_INFINITY;

            final Grammar.PackedUnaryRule packedUnaryRule = grammar.getPackedUnaryScores(unsplitParent, unsplitChild);

            for (int i = 0, j = 0; i < packedUnaryRule.ruleScores.length; i++, j += 2) {

                final short parentSplit = packedUnaryRule.substates[j + 1];
                final double parentOutside = parentOutsideScores[parentSplit];
                if (parentOutside == 0) {
                    continue;
                }

                final short childSplit = packedUnaryRule.substates[j];
                // Parent outside x rule
                final double jointScore = packedUnaryRule.ruleScores[i] * parentOutside;
                final double newScore = oScores[childSplit] + jointScore;

                oScores[childSplit] = newScore;
                if (newScore > max) {
                    max = newScore;
                }
            }

            child.setOutsideScores(oScores);
            child.setOutsideScoreScale(IEEEDoubleScaling.scaleArray(oScores, parent.outsideScoreScale(), max));
            break;
        }

        case 2: {
            final StateSet leftChild = children.get(0).label();
            final StateSet rightChild = children.get(1).label();

            final int nLeftChildStates = leftChild.numSubStates();
            final int nRightChildStates = rightChild.numSubStates();

            final short unsplitLeftChild = leftChild.getState();
            final short unsplitRightChild = rightChild.getState();

            final double[] lOScores = new double[nLeftChildStates];
            final double[] rOScores = new double[nRightChildStates];
            double lMax = Double.NEGATIVE_INFINITY, rMax = Double.NEGATIVE_INFINITY;

            final Grammar.PackedBinaryRule packedBinaryRule = grammar.getPackedBinaryScores(unsplitParent,
                    unsplitLeftChild, unsplitRightChild);

            // Iterate through all splits of the rule. Most will have non-0 child and parent probability (since the
            // rule currently exists in the grammar), and this iteration order is very efficient
            for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {

                final short parentSplit = packedBinaryRule.substates[j + 2];
                final double parentOutsideScore = parentOutsideScores[parentSplit];
                if (parentOutsideScore == 0) {
                    continue;
                }

                // Parent outside x rule
                final double jointRuleScore = parentOutsideScore * packedBinaryRule.ruleScores[i];

                final short leftChildSplit = packedBinaryRule.substates[j];
                final short rightChildSplit = packedBinaryRule.substates[j + 1];
                final double leftChildInsideScore = leftChild.insideScore(leftChildSplit);
                final double rightChildInsideScore = rightChild.insideScore(rightChildSplit);

                // Parent outside x rule x right-child inside
                final double jointRightScore = jointRuleScore * rightChildInsideScore;

                final double lScore = lOScores[leftChildSplit] + jointRightScore;
                lOScores[leftChildSplit] = lScore;
                if (lScore > lMax) {
                    lMax = lScore;
                }

                final double rScore = rOScores[rightChildSplit] + jointRuleScore * leftChildInsideScore;
                rOScores[rightChildSplit] = rScore;

                if (rScore > rMax) {
                    rMax = rScore;
                }
            }

            leftChild.setOutsideScores(lOScores);
            leftChild.setOutsideScoreScale(IEEEDoubleScaling.scaleArray(lOScores, parent.outsideScoreScale()
                    + rightChild.insideScoreScale(), lMax));

            rightChild.setOutsideScores(rOScores);
            rightChild.setOutsideScoreScale(IEEEDoubleScaling.scaleArray(rOScores, parent.outsideScoreScale()
                    + leftChild.insideScoreScale(), rMax));

            break;
        }

        default:
            throw new IllegalArgumentException("Malformed tree: more than two children");
        }

        for (final Tree<StateSet> child : children) {
            if (!child.isLeaf() && !child.isPreTerminal()) {
                outsidePass(child);
            }
        }
    }

    public void parse(final Tree<StateSet> tree, final boolean noSmoothing) {
        if (tree.isLeaf()) {
            return;
        }
        insidePass(tree, noSmoothing);
        tree.label().setOutsideScores(new double[] { 1 });
        tree.label().setOutsideScoreScale(0);
        if (tree.isPreTerminal()) {
            return;
        }
        outsidePass(tree);
    }

    /**
     * Calculate the outside scores of a tree (as per {@link #outsidePass(Tree)} and counts rule occurrences in the new
     * grammar.
     * 
     * Lots of copy-and-paste code from {@link #outsidePass(Tree)}, but it avoids some additional conditionals inside
     * the tight inner loop.
     * 
     * @param tree
     * @param newGrammar
     * @param treeInsideScore
     * @param treeScale
     */
    private void outsidePassAndCount(final Tree<StateSet> tree, final Grammar newGrammar, final double treeInsideScore,
            final int treeScale) {

        final ArrayList<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short unsplitParent = parent.getState();

        final double[] parentOutsideScores = parent.outsideScores();

        switch (children.size()) {

        case 1: {
            final StateSet child = children.get(0).label();
            final short unsplitChild = child.getState();
            final int nChildStates = child.numSubStates();
            final double[] oScores = new double[nChildStates];
            double max = Double.NEGATIVE_INFINITY;

            final Grammar.PackedUnaryRule packedUnaryRule = grammar.getPackedUnaryScores(unsplitParent, unsplitChild);
            final Grammar.PackedUnaryCount packedUnaryCount = newGrammar.getPackedUnaryCount(unsplitParent,
                    unsplitChild);

            final double treeScalingFactor = IEEEDoubleScaling.scalingMultiplier(parent.outsideScoreScale()
                    + child.insideScoreScale() - treeScale)
                    / treeInsideScore;

            for (int i = 0, j = 0; i < packedUnaryRule.ruleScores.length; i++, j += 2) {

                final short parentSplit = packedUnaryRule.substates[j + 1];
                final double parentOutside = parentOutsideScores[parentSplit];
                if (parentOutside == 0) {
                    continue;
                }

                final short childSplit = packedUnaryRule.substates[j];
                // Parent outside x rule
                final double jointScore = packedUnaryRule.ruleScores[i] * parentOutside;
                final double newScore = oScores[childSplit] + jointScore;

                oScores[childSplit] = newScore;
                if (newScore > max) {
                    max = newScore;
                }

                packedUnaryCount.ruleCounts[i] += jointScore * child.insideScore(childSplit) * treeScalingFactor;
            }

            child.setOutsideScores(oScores);
            child.setOutsideScoreScale(IEEEDoubleScaling.scaleArray(oScores, parent.outsideScoreScale(), max));
            break;
        }

        case 2: {
            final StateSet leftChild = children.get(0).label();
            final StateSet rightChild = children.get(1).label();

            final int nLeftChildStates = leftChild.numSubStates();
            final int nRightChildStates = rightChild.numSubStates();

            final short unsplitLeftChild = leftChild.getState();
            final short unsplitRightChild = rightChild.getState();

            final double[] lOScores = new double[nLeftChildStates];
            final double[] rOScores = new double[nRightChildStates];
            double lMax = Double.NEGATIVE_INFINITY, rMax = Double.NEGATIVE_INFINITY;

            final Grammar.PackedBinaryRule packedBinaryRule = grammar.getPackedBinaryScores(unsplitParent,
                    unsplitLeftChild, unsplitRightChild);
            final Grammar.PackedBinaryCount packedBinaryCount = newGrammar.getPackedBinaryCount(unsplitParent,
                    unsplitLeftChild, unsplitRightChild);

            final double treeScalingFactor = IEEEDoubleScaling.scalingMultiplier(parent.outsideScoreScale()
                    + leftChild.insideScoreScale() + rightChild.insideScoreScale() - treeScale)
                    / treeInsideScore;

            // Iterate through all splits of the rule. Most will have non-0 child and parent probability (since the
            // rule currently exists in the grammar), and this iteration order is very efficient
            for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {

                final short parentSplit = packedBinaryRule.substates[j + 2];
                final double parentOutsideScore = parentOutsideScores[parentSplit];
                if (parentOutsideScore == 0) {
                    continue;
                }

                // Parent outside x rule
                final double jointRuleScore = parentOutsideScore * packedBinaryRule.ruleScores[i];

                final short leftChildSplit = packedBinaryRule.substates[j];
                final short rightChildSplit = packedBinaryRule.substates[j + 1];
                final double leftChildInsideScore = leftChild.insideScore(leftChildSplit);
                final double rightChildInsideScore = rightChild.insideScore(rightChildSplit);

                // Parent outside x rule x right-child inside
                final double jointRightScore = jointRuleScore * rightChildInsideScore;

                final double lScore = lOScores[leftChildSplit] + jointRightScore;
                lOScores[leftChildSplit] = lScore;
                if (lScore > lMax) {
                    lMax = lScore;
                }

                final double rScore = rOScores[rightChildSplit] + jointRuleScore * leftChildInsideScore;
                rOScores[rightChildSplit] = rScore;

                if (rScore > rMax) {
                    rMax = rScore;
                }

                packedBinaryCount.ruleCounts[i] += jointRightScore * leftChildInsideScore * treeScalingFactor;
            }

            leftChild.setOutsideScores(lOScores);
            leftChild.setOutsideScoreScale(IEEEDoubleScaling.scaleArray(lOScores, parent.outsideScoreScale()
                    + rightChild.insideScoreScale(), lMax));

            rightChild.setOutsideScores(rOScores);
            rightChild.setOutsideScoreScale(IEEEDoubleScaling.scaleArray(rOScores, parent.outsideScoreScale()
                    + leftChild.insideScoreScale(), rMax));

            break;
        }

        default:
            throw new IllegalArgumentException("Malformed tree: more than two children");
        }

        for (final Tree<StateSet> child : children) {
            if (!child.isLeaf() && !child.isPreTerminal()) {
                outsidePassAndCount(child, newGrammar, treeInsideScore, treeScale);
            }
        }
    }

    public void parseAndCount(final Tree<StateSet> tree, final boolean noSmoothing, final Grammar newGrammar) {
        if (tree.isLeaf()) {
            return;
        }
        insidePass(tree, noSmoothing);

        final double treeInsideScore = tree.label().insideScore(0);
        final int treeScale = tree.label().insideScoreScale();

        if (treeInsideScore == 0) {
            BaseLogger.singleton().config("Skipping a 0-probability tree.");
            return;
        }
        if (tree.isLeaf() || tree.isPreTerminal()) {
            BaseLogger.singleton().finer("Skipping single-word tree");
            return;
        }

        tree.label().setOutsideScores(new double[] { 1 });
        tree.label().setOutsideScoreScale(0);
        if (tree.isPreTerminal()) {
            return;
        }
        outsidePassAndCount(tree, newGrammar, treeInsideScore, treeScale);
    }
}
