package edu.berkeley.nlp.PCFGLA;

import java.io.Serializable;
import java.util.Random;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.Numberer;

/**
 * Binary rules (ints for parent, left and right children)
 * 
 * @author Dan Klein
 */

public class BinaryRule extends Rule implements Serializable, java.lang.Comparable<BinaryRule> {

    public short leftChildState = -1;
    public short rightChildState = -1;
    /**
     * NEW: scores[leftSubState][rightSubState][parentSubState] gives score for this rule
     */
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
        // this.scores = new double[1][1][1];
    }

    /** Copy constructor */
    public BinaryRule(final BinaryRule b) {
        this(b.parentState, b.leftChildState, b.rightChildState, ArrayUtil.copy(b.scores));
    }

    public BinaryRule(final BinaryRule b, final double[][][] newScores) {
        this(b.parentState, b.leftChildState, b.rightChildState, newScores);
    }

    @Override
    public int hashCode() {
        return (parentState << 16) ^ (leftChildState << 8) ^ (rightChildState);
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

    public int compareTo(final BinaryRule o) {
        if (parentState < o.parentState) {
            return -1;
        }
        if (parentState > o.parentState) {
            return 1;
        }
        if (leftChildState < o.leftChildState) {
            return -1;
        }
        if (leftChildState > o.leftChildState) {
            return 1;
        }
        if (rightChildState < o.rightChildState) {
            return -1;
        }
        if (rightChildState > o.rightChildState) {
            return 1;
        }
        return 0;
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

    public void setNodes(final short pState, final short lState, final short rState) {
        this.parentState = pState;
        this.leftChildState = lState;
        this.rightChildState = rState;
    }

    private static final long serialVersionUID = 2L;

    public BinaryRule splitRule(final short[] numSubStates, final short[] newNumSubStates, final Random random,
            final double randomness) {
        // when splitting on parent, never split on ROOT
        int parentSplitFactor = this.getParentState() == 0 ? 1 : 2; // should

        if (newNumSubStates[this.parentState] == numSubStates[this.parentState]) {
            parentSplitFactor = 1;
        }
        int lChildSplitFactor = 2;
        if (newNumSubStates[this.leftChildState] == numSubStates[this.leftChildState]) {
            lChildSplitFactor = 1;
        }
        int rChildSplitFactor = 2;
        if (newNumSubStates[this.rightChildState] == numSubStates[this.rightChildState]) {
            rChildSplitFactor = 1;
        }

        final double[][][] oldScores = this.getScores2();
        final double[][][] newScores = new double[oldScores.length * lChildSplitFactor][oldScores[0].length
                * rChildSplitFactor][];
        // [oldScores[0][0].length * parentSplitFactor];
        // Arrays.fill(newScores,Double.NEGATIVE_INFINITY);
        // for all current substates
        for (short lcS = 0; lcS < oldScores.length; lcS++) {
            for (short rcS = 0; rcS < oldScores[0].length; rcS++) {
                if (oldScores[lcS][rcS] == null)
                    continue;

                for (short lc = 0; lc < lChildSplitFactor; lc++) {
                    for (short rc = 0; rc < rChildSplitFactor; rc++) {
                        final short newLCS = (short) (lChildSplitFactor * lcS + lc);
                        final short newRCS = (short) (rChildSplitFactor * rcS + rc);
                        newScores[newLCS][newRCS] = new double[newNumSubStates[this.parentState]];
                    }
                }

                for (short pS = 0; pS < oldScores[lcS][rcS].length; pS++) {
                    final double score = oldScores[lcS][rcS][pS];
                    // split on parent
                    for (short p = 0; p < parentSplitFactor; p++) {
                        final double divFactor = lChildSplitFactor * rChildSplitFactor;
                        double randomComponentLC = score / divFactor * randomness / 100 * (random.nextDouble() - 0.5);
                        // split on left child
                        for (short lc = 0; lc < lChildSplitFactor; lc++) {
                            // reverse the random component for half of the
                            // rules
                            if (lc == 1) {
                                randomComponentLC *= -1;
                            }
                            // don't add randomness if we're not splitting
                            if (lChildSplitFactor == 1) {
                                randomComponentLC = 0;
                            }
                            double randomComponentRC = score / divFactor * randomness / 100
                                    * (random.nextDouble() - 0.5);
                            // split on right child
                            for (short rc = 0; rc < rChildSplitFactor; rc++) {
                                // reverse the random component for half of the
                                // rules
                                if (rc == 1) {
                                    randomComponentRC *= -1;
                                }
                                // don't add randomness if we're not splitting
                                if (rChildSplitFactor == 1) {
                                    randomComponentRC = 0;
                                }
                                // set new score; divide score by 4 because
                                // we're dividing each
                                // binary rule under a parent into 4
                                final short newPS = (short) (parentSplitFactor * pS + p);
                                final short newLCS = (short) (lChildSplitFactor * lcS + lc);
                                final short newRCS = (short) (rChildSplitFactor * rcS + rc);
                                final double splitFactor = lChildSplitFactor * rChildSplitFactor;
                                newScores[newLCS][newRCS][newPS] = (score / (splitFactor) + randomComponentLC + randomComponentRC);
                            }
                        }
                    }
                }
            }
        }
        final BinaryRule newRule = new BinaryRule(this, newScores);
        return newRule;

    }

}
