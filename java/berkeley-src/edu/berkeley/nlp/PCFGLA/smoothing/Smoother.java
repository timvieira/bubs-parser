/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import edu.berkeley.nlp.PCFGLA.BinaryCounterTable;
import edu.berkeley.nlp.PCFGLA.UnaryCounterTable;

/**
 * Smooths observed fractional counts during EM grammar learning or lexicon scoring.
 * 
 * @author leon
 * 
 */
public interface Smoother {

    /**
     * Smooths observed fractional counts during EM grammar training.
     * 
     * @param unaryCounter
     * @param binaryCounter
     */
    public void smooth(UnaryCounterTable unaryCounter, BinaryCounterTable binaryCounter);

    /**
     * Smooths a set of scores using the same smoothing parameters computed during the previous invocation of
     * {@link #smooth(UnaryCounterTable, BinaryCounterTable)}.
     * 
     * @param tag
     * @param ruleScores
     */
    public void smooth(short tag, double[] ruleScores);
}
