/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.Serializable;

import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryCount;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedUnaryCount;

/**
 * Noop implementation of {@link Smoother}
 */
public class NoSmoothing implements Smoother, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public void smooth(final Int2ObjectOpenHashMap<PackedUnaryCount> packedUnaryCountMap,
            final Int2ObjectOpenHashMap<PackedBinaryCount> packedBinaryCountMap, final short[] splitCounts,
            final double[][] parentCounts) {
    }

    @Override
    public void smooth(final short tag, final double[] ruleScores) {
    }
}
