/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.Serializable;

import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryRule;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedUnaryRule;

/**
 * Noop implementation of {@link Smoother}
 */
public class NoSmoothing implements Smoother, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public void smooth(final Int2ObjectOpenHashMap<PackedUnaryRule> packedUnaryRuleMap,
            final Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap, final short[] splitCounts) {
    }

    @Override
    public void smooth(final short tag, final double[] ruleScores) {
    }
}
