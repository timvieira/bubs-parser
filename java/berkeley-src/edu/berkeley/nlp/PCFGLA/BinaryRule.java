package edu.berkeley.nlp.PCFGLA;

import java.io.Serializable;
import java.util.Random;

import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryRule;
import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.Numberer;

/**
 * Binary rules (ints for parent, left and right children)
 * 
 * @author Dan Klein
 */

public class BinaryRule extends Rule implements Serializable {

    private static final long serialVersionUID = 2L;

    public short leftChildState = -1;
    public short rightChildState = -1;

    /** Rule probabilities. Indexed by leftSubState, rightSubState, parentSubState */
    public double[][][] scores;

    public BinaryRule(final short pState, final short lState, final short rState, final double[][][] scores) {
        this.parentState = pState;
        this.leftChildState = lState;
        this.rightChildState = rState;
        this.scores = scores;
    }

    public BinaryRule(final short pState, final short lState, final short rState) {
        this.parentState = pState;
        this.leftChildState = lState;
        this.rightChildState = rState;
    }

    /** Copy constructor */
    public BinaryRule(final BinaryRule b) {
        this(b.parentState, b.leftChildState, b.rightChildState, ArrayUtil.copy(b.scores));
    }

    public BinaryRule(final BinaryRule b, final double[][][] newScores) {
        this(b.parentState, b.leftChildState, b.rightChildState, newScores);
    }

    /**
     * Copy production probabilities from a packed representation (used during splitting and merging)
     * 
     * @param packedBinaryRule
     */
    public BinaryRule(final PackedBinaryRule packedBinaryRule, final short[] splitCounts) {
        this(packedBinaryRule.unsplitParent, packedBinaryRule.unsplitLeftChild, packedBinaryRule.unsplitRightChild);

        scores = new double[splitCounts[packedBinaryRule.unsplitLeftChild]][splitCounts[packedBinaryRule.unsplitRightChild]][];

        for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {
            final short leftChildSplit = packedBinaryRule.substates[j];
            final short rightChildSplit = packedBinaryRule.substates[j + 1];
            final short parentSplit = packedBinaryRule.substates[j + 2];

            if (scores[leftChildSplit][rightChildSplit] == null) {
                scores[leftChildSplit][rightChildSplit] = new double[splitCounts[packedBinaryRule.unsplitParent]];
            }

            scores[leftChildSplit][rightChildSplit][parentSplit] = packedBinaryRule.ruleScores[i];
        }
    }

    public int key() {
        return Grammar.binaryKey(parentState, leftChildState, rightChildState);
    }

    public short getLeftChildState() {
        return leftChildState;
    }

    public short getRightChildState() {
        return rightChildState;
    }

    public void setScores2(final double[][][] scores) {
        this.scores = scores;
    }

    /**
     * scores[parentSubState][leftSubState][rightSubState] gives score for this rule
     */
    public double[][][] getScores2() {
        return scores;
    }

    /**
     * Returns a new {@link BinaryRule}, with each rule production split into 8 (or 4 if the parent is the start
     * symbol).
     * 
     * @param numSubStates
     * @param newNumSubStates
     * @param random
     * @param randomness
     * @return a new {@link BinaryRule}, with each rule production split into 8
     */
    public BinaryRule splitRule(final short[] numSubStates, final short[] newNumSubStates, final Random random,
            final double randomness) {

        // Never split the start symbol (parent state == 0)
        final int parentSplitFactor = this.getParentState() == 0 ? 1 : 2;

        final double[][][] oldScores = this.getScores2();
        final double[][][] newScores = new double[oldScores.length * 2][oldScores[0].length * 2][];

        for (short leftChildSplit = 0; leftChildSplit < oldScores.length; leftChildSplit++) {

            for (short rightChildSplit = 0; rightChildSplit < oldScores[0].length; rightChildSplit++) {

                if (oldScores[leftChildSplit][rightChildSplit] == null) {
                    continue;
                }

                // Allocate storage
                for (short lc = 0; lc < 2; lc++) {
                    for (short rc = 0; rc < 2; rc++) {
                        final short newLCS = (short) (2 * leftChildSplit + lc);
                        final short newRCS = (short) (2 * rightChildSplit + rc);
                        newScores[newLCS][newRCS] = new double[newNumSubStates[this.parentState]];
                    }
                }

                for (short parentSplit = 0; parentSplit < oldScores[leftChildSplit][rightChildSplit].length; parentSplit++) {

                    final double score = oldScores[leftChildSplit][rightChildSplit][parentSplit];

                    // Split on parent
                    for (short p = 0; p < parentSplitFactor; p++) {

                        final double leftChildRandomness = score / 4 * randomness / 100 * (random.nextDouble() - 0.5);

                        // Split on left child
                        for (short i = 0; i < 2; i++) {

                            final double randomComponentRC = score / 4 * randomness / 100 * (random.nextDouble() - 0.5);

                            // Split on right child
                            for (short j = 0; j < 2; j++) {

                                // Reverse randomness for half the rules
                                final double totalRandomness = leftChildRandomness * (i == 1 ? -1 : 1)
                                        + randomComponentRC * (j == 1 ? -1 : 1);

                                // Divide the scores by 4, since we're splitting each child production of a parent into
                                // 4
                                final int newParentSplit = parentSplitFactor * parentSplit + p;
                                final int newLeftChildSplit = 2 * leftChildSplit + i;
                                final int newRightChildSplit = 2 * rightChildSplit + j;
                                newScores[newLeftChildSplit][newRightChildSplit][newParentSplit] = score / 4
                                        + totalRandomness;
                            }
                        }
                    }
                }
            }
        }
        return new BinaryRule(this, newScores);
    }

    @Override
    public int hashCode() {
        return key();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof BinaryRule) {
            final BinaryRule br = (BinaryRule) o;
            if (parentState == br.parentState && leftChildState == br.leftChildState
                    && rightChildState == br.rightChildState) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(final double minimumRuleProbability) {

        final Numberer n = Numberer.getGlobalNumberer("tags");
        String lState = n.symbol(leftChildState);

        if (lState.endsWith("^g")) {
            lState = lState.substring(0, lState.length() - 2);
        }

        String rState = n.symbol(rightChildState);
        if (rState.endsWith("^g")) {
            rState = rState.substring(0, rState.length() - 2);
        }

        String pState = n.symbol(parentState);
        if (pState.endsWith("^g")) {
            pState = pState.substring(0, pState.length() - 2);
        }

        final StringBuilder sb = new StringBuilder();
        if (scores == null) {
            return pState + " -> " + lState + " " + rState + "\n";
        }

        for (int lS = 0; lS < scores.length; lS++) {
            for (int rS = 0; rS < scores[lS].length; rS++) {
                if (scores[lS][rS] == null) {
                    continue;
                }

                for (int pS = 0; pS < scores[lS][rS].length; pS++) {
                    final double p = scores[lS][rS][pS];
                    if (p > minimumRuleProbability) {
                        sb.append(String.format("%s_%d -> %s_%d %s_%d %.10f\n", pState, pS, lState, lS, rState, rS,
                                Math.log(p)));
                    }
                }
            }
        }
        return sb.toString();
    }
}
