/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryRule;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedUnaryRule;
import edu.berkeley.nlp.syntax.Tree;

/**
 * @author leon
 * 
 */
public class SmoothAcrossParentBits implements Smoother, Serializable {

    private static final long serialVersionUID = 1L;

    private double same;

    /** Indexed by unsplit parent, parent split, parent split */
    private double[][][] diffWeights;

    public SmoothAcrossParentBits(final double smooth, final Tree<Short>[] splitTrees) {
        // does not smooth across top-level split, otherwise smooths uniformly

        same = 1 - smooth;

        final int nStates = splitTrees.length;
        diffWeights = new double[nStates][][];

        for (short state = 0; state < nStates; state++) {
            Tree<Short> splitTree = splitTrees[state];
            final List<Short> allSubstates = splitTree.leafLabels();
            int nSubstates = 1;

            for (int i = 0; i < allSubstates.size(); i++) {
                if (allSubstates.get(i) >= nSubstates)
                    nSubstates = allSubstates.get(i) + 1;
            }
            diffWeights[state] = new double[nSubstates][nSubstates];

            if (nSubstates == 1) {
                // state has only one substate -> no smoothing
                diffWeights[state][0][0] = 1.0;

            } else {
                // smooth only with ones in the same top-level branch

                // descend down to first split first
                while (splitTree.children().size() == 1) {
                    splitTree = splitTree.children().get(0);
                }

                for (int branch = 0; branch < 2; branch++) {
                    // compute weights for substates in top-level branch
                    final List<Short> substatesInBranch = splitTree.children().get(branch).leafLabels();
                    final int total = substatesInBranch.size();
                    final double normalizedSmooth = smooth / (total - 1);

                    for (final short i : substatesInBranch) {
                        for (final short j : substatesInBranch) {
                            if (i == j) {
                                diffWeights[state][i][j] = same;
                            } else {
                                diffWeights[state][i][j] = normalizedSmooth;
                            }
                        }
                    }
                }

            }
        }
    }

    public void smooth(final Int2ObjectOpenHashMap<PackedUnaryRule> packedUnaryRuleMap,
            final Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap, final short[] splitCounts) {

        // for (final UnaryRule r : unaryCounter.keySet()) {
        // final double[][] scores = unaryCounter.getCount(r);
        // final double[][] scopy = new double[scores.length][];
        // final short pState = r.parentState;
        // for (int j = 0; j < scores.length; j++) {
        // if (scores[j] == null)
        // continue; // nothing to smooth
        //
        // scopy[j] = new double[scores[j].length];
        // for (int i = 0; i < scores[j].length; i++) {
        // for (int k = 0; k < scores[j].length; k++) {
        // scopy[j][i] += diffWeights[pState][i][k] * scores[j][k];
        // }
        // }
        // }
        // unaryCounter.setCount(r, scopy);
        // }

        // Smooth binary rules
        for (final int unaryKey : packedUnaryRuleMap.keySet()) {

            final short unsplitParent = Grammar.unsplitUnaryParent(unaryKey);
            final PackedUnaryRule packedUnaryRule = packedUnaryRuleMap.get(unaryKey);
            final double[] unsmoothedCounts = packedUnaryRule.ruleScores.clone();
            Arrays.fill(packedUnaryRule.ruleScores, 0);

            for (int i = 0, j = 0; i < packedUnaryRule.ruleScores.length; i++, j += 2) {
                final short parentSplit = packedUnaryRule.substates[j + 1];

                for (int altParentSplit = 0; altParentSplit < splitCounts[unsplitParent]; altParentSplit++) {
                    packedUnaryRule.ruleScores[i] += unsmoothedCounts[i]
                            * diffWeights[unsplitParent][parentSplit][altParentSplit];
                }
            }
        }

        // Smooth binary rules
        for (final int binaryKey : packedBinaryRuleMap.keySet()) {

            final short unsplitParent = Grammar.unsplitBinaryParent(binaryKey);
            final PackedBinaryRule packedBinaryRule = packedBinaryRuleMap.get(binaryKey);
            final double[] unsmoothedCounts = packedBinaryRule.ruleScores.clone();
            Arrays.fill(packedBinaryRule.ruleScores, 0);

            for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {
                final short parentSplit = packedBinaryRule.substates[j + 2];

                for (int altParentSplit = 0; altParentSplit < splitCounts[unsplitParent]; altParentSplit++) {
                    packedBinaryRule.ruleScores[i] += unsmoothedCounts[i]
                            * diffWeights[unsplitParent][parentSplit][altParentSplit];
                }
            }
        }
    }

    // public static void assertBinaryRulesEqual(final BinaryCounterTable binaryCounterTable,
    // final Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap) {
    //
    // for (final int binaryKey : packedBinaryRuleMap.keySet()) {
    // final double[][][] scores = binaryCounterTable.getCount(new BinaryRule(Grammar.unsplitParent(binaryKey),
    // Grammar.unsplitLeftChild(binaryKey), Grammar.unsplitRightChild(binaryKey)));
    //
    // final PackedBinaryRule packedBinaryRule = packedBinaryRuleMap.get(binaryKey);
    //
    // for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {
    // final short leftChildSplit = packedBinaryRule.substates[j];
    // final short rightChildSplit = packedBinaryRule.substates[j + 1];
    // final short parentSplit = packedBinaryRule.substates[j + 2];
    //
    // assertEquals(scores[leftChildSplit][rightChildSplit][parentSplit], packedBinaryRule.ruleScores[i],
    // 1e-10);
    // }
    // }
    // }

    // TODO Cleanup and eliminate the copy
    public void smooth(final short tag, final double[] scores) {
        final double[] scopy = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            for (int k = 0; k < scores.length; k++) {
                scopy[i] += diffWeights[tag][i][k] * scores[k];
            }
        }
        for (int i = 0; i < scores.length; i++) {
            scores[i] = scopy[i];
        }
    }
}
