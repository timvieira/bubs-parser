package edu.berkeley.nlp.syntax;

import edu.berkeley.nlp.util.Numberer;
import edu.berkeley.nlp.util.ScalingTools;

/**
 * Represent parsetrees, with each node consisting of a label and a list of children. The score tables are not allocated
 * by the constructor and allocate() and deallocate() must be called manually. This is to allow more control over memory
 * usage.
 * 
 * @author Slav Petrov
 * @author Romain Thibaux
 */
public class StateSet {

    public static final double SCALE = Math.exp(100);

    double[] iScores; // the log-probabilities for each sublabels
    double[] oScores;
    int iScale;
    int oScale;

    // TODO Map this word to an integer index, and use that in Lexicon.trainTree() and Lexicon.score()
    final String word;

    private final short numSubStates;
    private final short state;
    public short from, to;

    public StateSet(final short state, final short nSubStates) {
        this.numSubStates = nSubStates;
        this.state = state;
        this.word = null;
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

    // Run allocate() before any other operation
    public void allocate() {
        iScores = new double[numSubStates];
        oScores = new double[numSubStates];
    }

    // run deallocate() if the scores are no longer needed and
    // this object will not be used for a long time
    public void deallocate() {
        iScores = null;
        oScores = null;
    }

    @Override
    public String toString() {
        if (word != null) {
            return word + " " + from + "-" + to;
        }
        return Numberer.getGlobalNumberer("tags").symbol(state) + " ";
    }

    public final short getState() {
        return state;
    }

    public final double getIScore(final int i) {
        return iScores[i];
    }

    // TODO Currently unused
    public final double[] getIScores() {
        return iScores;
    }

    public final double getOScore(final int i) {
        return oScores[i];
    }

    public final double[] getOScores() {
        return oScores;
    }

    public final void setIScores(final double[] s) {
        iScores = s;
    }

    public final void setIScore(final int i, final double s) {
        if (iScores == null)
            iScores = new double[numSubStates];
        iScores[i] = s;
    }

    public final void setOScores(final double[] s) {
        oScores = s;
    }

    public final void setOScore(final int i, final double s) {
        if (oScores == null)
            oScores = new double[numSubStates];
        oScores[i] = s;
    }

    public final int numSubStates() {
        return numSubStates;
    }

    public String getWord() {
        return word;
    }

    public void scaleIScores(final int previousScale) {
        iScale = ScalingTools.scaleArray(iScores, previousScale);
    }

    public void scaleOScores(final int previousScale) {
        oScale = ScalingTools.scaleArray(oScores, previousScale);
    }

    public int getIScale() {
        return iScale;
    }

    public void setIScale(final int scale) {
        iScale = scale;
    }

    public int getOScale() {
        return oScale;
    }

    public void setOScale(final int scale) {
        oScale = scale;
    }

    /**
     * @return a copy of this {@link StateSet}
     */
    public StateSet copy() {
        return new StateSet(this, this.numSubStates);
    }
}
