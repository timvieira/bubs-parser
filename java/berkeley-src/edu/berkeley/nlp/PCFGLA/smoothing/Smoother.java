/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import edu.berkeley.nlp.PCFGLA.BinaryCounterTable;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryRule;
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
     * @param packedBinaryRuleMap
     * @param splitCounts The number of splits (substates) of each unsplit non-terminal
     */
    public void smooth(UnaryCounterTable unaryCounter, BinaryCounterTable binaryCounter,
            Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap, final short[] splitCounts);

    /**
     * Smooths a set of scores using the same smoothing parameters computed during the previous invocation of
     * {@link #smooth(UnaryCounterTable, BinaryCounterTable)}.
     * 
     * @param tag
     * @param ruleScores
     */
    public void smooth(short tag, double[] ruleScores);
}
