package edu.berkeley.nlp.PCFGLA;

import java.util.List;

import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;

/**
 * Simple mixture parser.
 */
public class ArrayParser {

    protected Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

    Lexicon lexicon;

    int numStates;
    int maxNSubStates;
    int[] idxC;
    double[] scoresToAdd;
    int touchedRules;
    double[] tmpCountsArray;
    Grammar grammar;
    int[] stateClass;

    public ArrayParser(final Grammar gr, final Lexicon lex) {
        this.touchedRules = 0;
        this.grammar = gr;
        this.lexicon = lex;
        this.tagNumberer = Numberer.getGlobalNumberer("tags");
        this.numStates = gr.numStates;
        this.maxNSubStates = gr.maxSubStates();
        this.idxC = new int[maxNSubStates];
        this.scoresToAdd = new double[maxNSubStates];
        tmpCountsArray = new double[scoresToAdd.length * scoresToAdd.length * scoresToAdd.length];
    }

    /**
     * Calculate the inside scores, P(words_i,j|nonterminal_i,j) of a tree given the string if words it should parse to.
     * 
     * @param tree
     * @param noSmoothing
     * @param spanScores
     */
    void doInsideScores(final Tree<StateSet> tree, final boolean noSmoothing, final double[][][] spanScores) {

        if (tree.isLeaf()) {
            return;
        }

        final List<Tree<StateSet>> children = tree.children();
        for (final Tree<StateSet> child : children) {
            doInsideScores(child, noSmoothing, spanScores);
        }
        final StateSet parent = tree.label();
        final short unsplitParent = parent.getState();
        final int nParentStates = parent.numSubStates();
        if (tree.isPreTerminal()) {
            // Plays a role similar to initializeChart()
            final StateSet wordStateSet = tree.children().get(0).label();
            final double[] lexiconScores = lexicon.score(wordStateSet, unsplitParent, noSmoothing, false);
            if (lexiconScores.length != nParentStates) {
                System.out.println("Have more scores than substates!" + lexiconScores.length + " " + nParentStates);// truncate
                                                                                                                    // the
                                                                                                                    // array
            }
            parent.setIScores(lexiconScores);
            parent.scaleIScores(0);
        } else {
            switch (children.size()) {
            case 0:
                break;
            case 1:
                final StateSet child = children.get(0).label();
                final short unsplitChild = child.getState();
                // final int nChildStates = child.numSubStates();

                final double[] iScores = new double[nParentStates];
                // for (int j = 0; j < nChildStates; j++) {
                // if (uscores[j] != null) { // check whether one of the
                // // parents can produce this
                // // child
                // final double cS = child.getIScore(j);
                // if (cS == 0)
                // continue;
                // for (int i = 0; i < nParentStates; i++) {
                // final double rS = uscores[j][i]; // rule score
                // if (rS == 0)
                // continue;
                // final double res = rS * cS;
                // iScores[i] += res;
                // }
                // }
                // }
                final Grammar.PackedUnaryRule packedUnaryRule = grammar.getPackedUnaryScores(unsplitParent,
                        unsplitChild);
                for (int i = 0, j = 0; i < packedUnaryRule.ruleScores.length; i++, j += 2) {
                    final short childSplit = packedUnaryRule.substates[j];
                    final short parentSplit = packedUnaryRule.substates[j + 1];

                    final double childInsideScore = child.getIScore(childSplit);
                    iScores[parentSplit] += packedUnaryRule.ruleScores[i] * childInsideScore;
                }

                parent.setIScores(iScores);
                parent.scaleIScores(child.getIScale());
                break;
            case 2:
                final StateSet leftChild = children.get(0).label();
                final StateSet rightChild = children.get(1).label();
                // final int nLeftChildStates = leftChild.numSubStates();
                // final int nRightChildStates = rightChild.numSubStates();
                final short unsplitLeftChild = leftChild.getState();
                final short unsplitRightChild = rightChild.getState();

                final Grammar.PackedBinaryRule packedBinaryRule = grammar.getPackedBinaryScores(unsplitParent,
                        unsplitLeftChild, unsplitRightChild);
                // final double[][][] bscores = grammar.getBinaryScore(unsplitParent, unsplitLeftChild,
                // unsplitRightChild);

                final double[] iScores2 = new double[nParentStates];
                // final double[] iScores2Clone = new double[nParentStates];

                for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {
                    final short leftChildSplit = packedBinaryRule.substates[j];
                    final short rightChildSplit = packedBinaryRule.substates[j + 1];
                    final short parentSplit = packedBinaryRule.substates[j + 2];

                    final double leftChildInsideScore = leftChild.getIScore(leftChildSplit);
                    final double rightChildInsideScore = rightChild.getIScore(rightChildSplit);

                    iScores2[parentSplit] += packedBinaryRule.ruleScores[i] * leftChildInsideScore
                            * rightChildInsideScore;
                }

                // for (int j = 0; j < nLeftChildStates; j++) {
                // final double leftChildInsideScore = leftChild.getIScore(j);
                // if (leftChildInsideScore == 0)
                // continue;
                // for (int k = 0; k < nRightChildStates; k++) {
                // final double rightChildInsideScore = rightChild.getIScore(k);
                // if (rightChildInsideScore == 0)
                // continue;
                // if (bscores[j][k] != null) { // check whether one of the
                // // parents can produce
                // // these kids
                // for (int i = 0; i < nParentStates; i++) {
                // final double ruleScore = bscores[j][k][i];
                // if (ruleScore == 0)
                // continue;
                // final double res = ruleScore * leftChildInsideScore * rightChildInsideScore;
                // /*
                // * if (res == 0) { System.out.println("Prevented an underflow: rS "
                // * +rS+" lcS "+lcS+" rcS "+rcS); res = Double.MIN_VALUE; }
                // */
                // iScores2Clone[i] += res;
                // }
                // }
                // }
                // }
                //
                // assertArrayEquals(iScores2Clone, iScores2, 1e-15);

                if (spanScores != null) {
                    for (int i = 0; i < nParentStates; i++) {
                        iScores2[i] *= spanScores[parent.from][parent.to][stateClass[unsplitParent]];
                    }
                }

                parent.setIScores(iScores2);
                parent.scaleIScores(leftChild.getIScale() + rightChild.getIScale());
                break;
            default:
                throw new Error("Malformed tree: more than two children");
            }
        }
    }

    /**
     * Calculate the outside scores of a tree; that is, P(nonterminal_i,j|words_0,i; words_j,end). It is calculate from
     * the inside scores of the tree.
     * 
     * <p>
     * Note: when calling this, call setRootOutsideScore() first.
     * 
     * @param tree
     */
    void doOutsideScores(final Tree<StateSet> tree, boolean unaryAbove, final double[][][] spanScores) {

        if (tree.isLeaf()) {
            return;
        }

        final List<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short unsplitParent = parent.getState();
        final int nParentStates = parent.numSubStates();
        // this sets the outside scores for the children
        if (tree.isPreTerminal()) {

        } else {
            final double[] parentOutsideScores = parent.getOScores();
            if (spanScores != null && !unaryAbove) {
                for (int i = 0; i < nParentStates; i++) {
                    parentOutsideScores[i] *= spanScores[parent.from][parent.to][stateClass[unsplitParent]];
                }
            }
            switch (children.size()) {
            case 0:
                // Nothing to do
                break;
            case 1:
                final StateSet child = children.get(0).label();
                final short unsplitChild = child.getState();
                final int nChildStates = child.numSubStates();
                // UnaryRule uR = new UnaryRule(pState,cState);
                final double[] oScores = new double[nChildStates];
                // final double[][] uscores = grammar.getUnaryScore(unsplitParent, unsplitChild);
                // for (int j = 0; j < nChildStates; j++) {
                // if (uscores[j] != null) {
                // double childScore = 0;
                // for (int i = 0; i < nParentStates; i++) {
                // final double pS = parentOutsideScores[i];
                // if (pS == 0)
                // continue;
                // final double rS = uscores[j][i]; // rule score
                // if (rS == 0)
                // continue;
                // childScore += pS * rS;
                // }
                // oScores[j] = childScore;
                // }
                // }

                final Grammar.PackedUnaryRule packedUnaryRule = grammar.getPackedUnaryScores(unsplitParent,
                        unsplitChild);
                for (int i = 0, j = 0; i < packedUnaryRule.ruleScores.length; i++, j += 2) {
                    final short childSplit = packedUnaryRule.substates[j];
                    final short parentSplit = packedUnaryRule.substates[j + 1];

                    final double parentOutsideScore = parentOutsideScores[parentSplit];
                    oScores[childSplit] += packedUnaryRule.ruleScores[i] * parentOutsideScore;
                }

                child.setOScores(oScores);
                child.scaleOScores(parent.getOScale());
                unaryAbove = true;
                break;
            case 2:
                final StateSet leftChild = children.get(0).label();
                final StateSet rightChild = children.get(1).label();

                final int nLeftChildStates = leftChild.numSubStates();
                final int nRightChildStates = rightChild.numSubStates();

                final short unsplitLeftChild = leftChild.getState();
                final short unsplitRightChild = rightChild.getState();

                final double[] lOScores = new double[nLeftChildStates];
                final double[] rOScores = new double[nRightChildStates];
                // final double[] lOScoresClone = new double[nLeftChildStates];
                // final double[] rOScoresClone = new double[nRightChildStates];

                final Grammar.PackedBinaryRule packedBinaryRule = grammar.getPackedBinaryScores(unsplitParent,
                        unsplitLeftChild, unsplitRightChild);
                // Iterate through all splits of the rule. Most will have non-0 child and parent probability (since the
                // rule currently exists in the grammar), and this iteration order is very efficient
                for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {
                    final short leftChildSplit = packedBinaryRule.substates[j];
                    final short rightChildSplit = packedBinaryRule.substates[j + 1];
                    final short parentSplit = packedBinaryRule.substates[j + 2];

                    final double leftChildInsideScore = leftChild.getIScore(leftChildSplit);
                    final double rightChildInsideScore = rightChild.getIScore(rightChildSplit);

                    lOScores[leftChildSplit] += parentOutsideScores[parentSplit] * packedBinaryRule.ruleScores[i]
                            * rightChildInsideScore;
                    rOScores[rightChildSplit] += parentOutsideScores[parentSplit] * packedBinaryRule.ruleScores[i]
                            * leftChildInsideScore;
                }

                // final double[][][] bscores = grammar.getBinaryScore(unsplitParent, unsplitLeftChild,
                // unsplitRightChild);
                //
                // for (int j = 0; j < nLeftChildStates; j++) {
                // final double leftChildInsideScore = leftChild.getIScore(j);
                // double leftScore = 0;
                //
                // for (int k = 0; k < nRightChildStates; k++) {
                // final double rightChildInsideScore = rightChild.getIScore(k);
                // if (bscores[j][k] != null) {
                // for (int i = 0; i < nParentStates; i++) {
                // final double parentOutsideScore = parentOutsideScores[i];
                // if (parentOutsideScore == 0)
                // continue;
                // final double ruleScore = bscores[j][k][i];
                // if (ruleScore == 0)
                // continue;
                // leftScore += parentOutsideScore * ruleScore * rightChildInsideScore;
                // rOScoresClone[k] += parentOutsideScore * ruleScore * leftChildInsideScore;
                // }
                // }
                // lOScoresClone[j] = leftScore;
                // }
                // }
                //
                // assertArrayEquals(lOScoresClone, lOScores, 1e-15);
                // assertArrayEquals(rOScoresClone, rOScores, 1e-15);

                leftChild.setOScores(lOScores);
                leftChild.scaleOScores(parent.getOScale() + rightChild.getIScale());
                rightChild.setOScores(rOScores);
                rightChild.scaleOScores(parent.getOScale() + leftChild.getIScale());
                unaryAbove = false;
                break;
            default:
                throw new Error("Malformed tree: more than two children");
            }
            for (final Tree<StateSet> child : children) {
                doOutsideScores(child, unaryAbove, spanScores);
            }
        }
    }

    public void doInsideOutsideScores(final Tree<StateSet> tree, final boolean noSmoothing) {
        doInsideScores(tree, noSmoothing, null);
        tree.label().setOScore(0, 1);
        tree.label().setOScale(0);
        doOutsideScores(tree, false, null);
    }
}
