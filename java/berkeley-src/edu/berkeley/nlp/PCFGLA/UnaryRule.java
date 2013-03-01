package edu.berkeley.nlp.PCFGLA;

import java.util.Random;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.Numberer;

/**
 * Unary Rules (with ints for parent and child)
 * 
 * @author Dan Klein
 */
public class UnaryRule extends Rule implements java.io.Serializable, Comparable<UnaryRule> {

    private static final long serialVersionUID = 2L;

    public short childState = -1;
    /**
     * NEW: scores[childSubState][parentSubState]
     */
    public double[][] scores;

    public UnaryRule(final short pState, final short cState, final double[][] scores) {
        this.parentState = pState;
        this.childState = cState;
        this.scores = scores;
    }

    public UnaryRule(final short pState, final short cState) {
        this.parentState = pState;
        this.childState = cState;
    }

    /** Copy constructor */
    public UnaryRule(final UnaryRule u) {
        this(u.parentState, u.childState, ArrayUtil.copy(u.scores));
    }

    public UnaryRule(final UnaryRule u, final double[][] newScores) {
        this(u.parentState, u.childState, newScores);
    }

    public UnaryRule(final short pState, final short cState, final short pSubStates, final short cSubStates) {
        this.parentState = pState;
        this.childState = cState;
        this.scores = new double[cSubStates][pSubStates];
    }

    @Override
    public boolean isUnary() {
        return true;
    }

    @Override
    public int hashCode() {
        return (parentState << 18) ^ (childState);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof UnaryRule) {
            final UnaryRule ur = (UnaryRule) o;
            if (parentState == ur.parentState && childState == ur.childState) {
                return true;
            }
        }
        return false;
    }

    public int compareTo(final UnaryRule o) {
        if (parentState < o.parentState) {
            return -1;
        }
        if (parentState > o.parentState) {
            return 1;
        }
        if (childState < o.childState) {
            return -1;
        }
        if (childState > o.childState) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        final Numberer n = Numberer.getGlobalNumberer("tags");
        String cState = n.symbol(childState);
        if (cState.endsWith("^g")) {
            cState = cState.substring(0, cState.length() - 2);
        }

        String pState = n.symbol(parentState);
        if (pState.endsWith("^g")) {
            pState = pState.substring(0, pState.length() - 2);
        }

        if (scores == null) {
            return pState + " -> " + cState + "\n";
        }

        final StringBuilder sb = new StringBuilder();
        for (int cS = 0; cS < scores.length; cS++) {
            if (scores[cS] == null) {
                continue;
            }

            for (int pS = 0; pS < scores[cS].length; pS++) {
                final double p = scores[cS][pS];
                if (p > 0) {
                    sb.append(String.format("%s_%d -> %s_%d %.10f\n", pState, pS, cState, cS, Math.log(p)));
                }
            }
        }
        return sb.toString();
    }

    public short getChildState() {
        return childState;
    }

    public void setScore(final int pS, final int cS, final double score) {
        // sets the score for a particular combination of substates
        scores[cS][pS] = score;
    }

    public double getScore(final int pS, final int cS) {
        // gets the score for a particular combination of substates
        if (scores[cS] == null) {
            return 0;
        }
        return scores[cS][pS];
    }

    public void setScores2(final double[][] scores) {
        this.scores = scores;
    }

    /**
     * scores[parentSubState][childSubState]
     */
    public double[][] getScores2() {
        return scores;
    }

    public void setNodes(final short pState, final short cState) {
        this.parentState = pState;
        this.childState = cState;
    }

    /**
     * @return Split rule
     */
    public UnaryRule splitRule(final short[] numSubStates, final short[] newNumSubStates, final Random random,
            final double randomness) {
        // when splitting on parent, never split on ROOT parent
        short parentSplitFactor = this.getParentState() == 0 ? (short) 1 : (short) 2;
        if (newNumSubStates[this.parentState] == numSubStates[this.parentState]) {
            parentSplitFactor = 1;
        }
        int childSplitFactor = 2;
        if (newNumSubStates[this.childState] == numSubStates[this.childState]) {
            childSplitFactor = 1;
        }
        final double[][] oldScores = this.getScores2();
        final double[][] newScores = new double[newNumSubStates[this.childState]][];

        // for all current substates
        for (short cS = 0; cS < oldScores.length; cS++) {
            if (oldScores[cS] == null)
                continue;

            for (short c = 0; c < childSplitFactor; c++) {
                final short newCS = (short) (childSplitFactor * cS + c);
                newScores[newCS] = new double[newNumSubStates[this.parentState]];
            }

            for (short pS = 0; pS < oldScores[cS].length; pS++) {
                final double score = oldScores[cS][pS];
                // split on parent
                for (short p = 0; p < parentSplitFactor; p++) {
                    final double divFactor = childSplitFactor;
                    double randomComponent = score / divFactor * randomness / 100 * (random.nextDouble() - 0.5);
                    // split on child
                    for (short c = 0; c < childSplitFactor; c++) {
                        if (c == 1) {
                            randomComponent *= -1;
                        }
                        if (childSplitFactor == 1) {
                            randomComponent = 0;
                        }
                        // divide score by divFactor because we're splitting
                        // each rule in 1/divFactor
                        final short newPS = (short) (parentSplitFactor * pS + p);
                        final short newCS = (short) (childSplitFactor * cS + c);
                        newScores[newCS][newPS] = (score / childSplitFactor + randomComponent);
                    }
                }
            }
        }
        return new UnaryRule(this, newScores);
    }

}
