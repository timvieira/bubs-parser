/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import java.io.Serializable;

import edu.berkeley.nlp.PCFGLA.BinaryCounterTable;
import edu.berkeley.nlp.PCFGLA.UnaryCounterTable;

/**
 * Noop implementation of {@link Smoother}
 */
public class NoSmoothing implements Smoother, Serializable {

    private static final long serialVersionUID = 1L;

    public void smooth(final UnaryCounterTable unaryCounter, final BinaryCounterTable binaryCounter) {
    }

    public void smooth(final short tag, final double[] ruleScores) {
    }
}
