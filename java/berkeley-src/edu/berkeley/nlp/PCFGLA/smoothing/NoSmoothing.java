/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import java.io.Serializable;

import edu.berkeley.nlp.PCFGLA.BinaryCounterTable;
import edu.berkeley.nlp.PCFGLA.UnaryCounterTable;
import edu.berkeley.nlp.util.Numberer;

/**
 * Noop implementation of {@link Smoother}
 * 
 * @author leon
 */
public class NoSmoothing implements Smoother, Serializable {

    private static final long serialVersionUID = 1L;

    public void smooth(final UnaryCounterTable unaryCounter, final BinaryCounterTable binaryCounter) {
    }

    public void smooth(final short tag, final double[] ruleScores) {
    }

    public Smoother copy() {
        return this;
    }

    public void updateWeights(final int[][] toSubstateMapping) {
    }

    public Smoother remapStates(final Numberer thisNumberer, final Numberer newNumberer) {
        return null;
    }

}
