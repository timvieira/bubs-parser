package edu.berkeley.nlp.syntax;

import edu.berkeley.nlp.util.Numberer;

/**
 * A scored node in a parse tree. Score tables are set with {@link StateSet#setInsideScores(double[])} and
 * {@link StateSet#setOutsideScores(double[])}. Scores are deallocated using {@link #deallocateScores()} (to free
 * memory) when iterating over the tree.
 * 
 * @author Slav Petrov
 * @author Romain Thibaux
 */
public final class StateSet {

    /** Scaled inside probabilities for each substate */
    double[] insideScores;
    /** Scaled outside probabilities for each substate */
    double[] outsideScores;

    /** Inside scale */
    int insideScoreScale;

    /** Outside scale */
    int outsideScoreScale;

    /** the word of this node, if it is a terminal node; else null */
    private String word;

    private final short state;
    private final short numSubStates;

    /** Start and end indices of the sentence span covered by this node */
    public short from, to;

    public StateSet(final short state, final short nSubStates) {
        this.numSubStates = nSubStates;
        this.state = state;
    }

    public StateSet(final short s, final short nSubStates, final String word, final short from, final short to) {
        this.numSubStates = nSubStates;
        this.state = s;
        this.word = word;
        this.from = from;
        this.to = to;

    }

    public StateSet(final StateSet oldS, final short nSubStates) {
        this.numSubStates = nSubStates;
        this.state = oldS.state;
        this.word = oldS.word;
        this.from = oldS.from;
        this.to = oldS.to;
    }

    /**
     * Clears out score storage to free memory
     */
    public void deallocateScores() {
        insideScores = null;
        outsideScores = null;
    }

    @Override
    public String toString() {
        if (word != null) {
            return word + " " + from + "-" + to;// + " " + substates.length;
        }

        return Numberer.getGlobalNumberer("tags").symbol(state) + " ";
    }

    public final short getState() {
        return state;
    }

    public final double insideScore(final int i) {
        return insideScores[i];
    }

    public final double outsideScore(final int i) {
        return outsideScores[i];
    }

    public final double[] outsideScores() {
        return outsideScores;
    }

    public final void setInsideScores(final double[] insideScores) {
        this.insideScores = insideScores;
    }

    public final void setOutsideScores(final double[] outsideScores) {
        this.outsideScores = outsideScores;
    }

    public final int numSubStates() {
        return numSubStates;
    }

    public String getWord() {
        return word;
    }

    public int insideScoreScale() {
        return insideScoreScale;
    }

    public void setInsideScoreScale(final int scale) {
        insideScoreScale = scale;
    }

    public int outsideScoreScale() {
        return outsideScoreScale;
    }

    public void setOutsideScoreScale(final int scale) {
        outsideScoreScale = scale;
    }
}
