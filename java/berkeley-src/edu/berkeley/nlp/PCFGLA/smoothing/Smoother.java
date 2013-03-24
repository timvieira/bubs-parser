/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryRule;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedUnaryRule;

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
     * @param packedUnaryRuleMap
     * @param packedBinaryRuleMap
     * @param splitCounts The number of splits (substates) of each unsplit non-terminal
     */
    public void smooth(Int2ObjectOpenHashMap<PackedUnaryRule> packedUnaryRuleMap,
            Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap, final short[] splitCounts);

    /**
     * Smooths a set of scores using the same smoothing parameters computed during the previous invocation of
     * {@link #smooth(Int2ObjectOpenHashMap, Int2ObjectOpenHashMap, short[])}.
     * 
     * @param tag
     * @param ruleScores
     */
    public void smooth(short tag, double[] ruleScores);
}
