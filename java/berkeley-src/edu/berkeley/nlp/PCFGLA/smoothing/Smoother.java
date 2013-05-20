/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryCount;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedUnaryCount;

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
     * @param packedUnaryCountMap
     * @param packedBinaryCountMap
     * @param splitCounts The number of splits (substates) of each unsplit non-terminal
     * @param parentCounts Fractional parent counts observed in the training corpus, indexed by unsplit parent and
     *            parent split.
     */
    public void smooth(Int2ObjectOpenHashMap<PackedUnaryCount> packedUnaryCountMap,
            Int2ObjectOpenHashMap<PackedBinaryCount> packedBinaryCountMap, final short[] splitCounts,
            final double[][] parentCounts);

    /**
     * Smooths a set of scores using the same smoothing parameters computed during the previous invocation of
     * {@link #smooth(Int2ObjectOpenHashMap, Int2ObjectOpenHashMap, short[], double[][])}.
     * 
     * @param tag
     * @param ruleScores
     */
    public void smooth(short tag, double[] ruleScores);
}
