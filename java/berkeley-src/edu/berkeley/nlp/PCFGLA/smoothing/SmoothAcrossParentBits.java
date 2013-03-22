/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import edu.berkeley.nlp.PCFGLA.BinaryCounterTable;
import edu.berkeley.nlp.PCFGLA.BinaryRule;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryRule;
import edu.berkeley.nlp.PCFGLA.UnaryCounterTable;
import edu.berkeley.nlp.PCFGLA.UnaryRule;
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

    public void smooth(final UnaryCounterTable unaryCounter, final BinaryCounterTable binaryCounter,
            final Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap, final short[] splitCounts) {

        for (final UnaryRule r : unaryCounter.keySet()) {
            final double[][] scores = unaryCounter.getCount(r);
            final double[][] scopy = new double[scores.length][];
            final short pState = r.parentState;
            for (int j = 0; j < scores.length; j++) {
                if (scores[j] == null)
                    continue; // nothing to smooth

                scopy[j] = new double[scores[j].length];
                for (int i = 0; i < scores[j].length; i++) {
                    for (int k = 0; k < scores[j].length; k++) {
                        scopy[j][i] += diffWeights[pState][i][k] * scores[j][k];
                    }
                }
            }
            unaryCounter.setCount(r, scopy);
        }

        for (final BinaryRule r : binaryCounter.keySet()) {
            final double[][][] scores = binaryCounter.getCount(r);
            final double[][][] scopy = new double[scores.length][scores[0].length][];
            final short pState = r.parentState;
            for (int j = 0; j < scores.length; j++) { // j = parent split
                for (int l = 0; l < scores[j].length; l++) { // l = left child split
                    if (scores[j][l] == null)
                        continue; // nothing to smooth

                    scopy[j][l] = new double[scores[j][l].length];
                    for (int i = 0; i < scores[j][l].length; i++) { // i = right child split
                        for (int k = 0; k < scores[j][l].length; k++) { // k = alternate right child
                            // j (parent), l (left child), i (right child) : add in diff for rule parent, right child,
                            // alt right child * score for j (parent), l (left child), alt right child
                            scopy[j][l][i] += diffWeights[pState][i][k] * scores[j][l][k];
                        }
                    }
                }
            }
            binaryCounter.setCount(r, scopy);
        }

        // Smooth the packed representation too
        if (packedBinaryRuleMap != null) {
            for (final int binaryKey : packedBinaryRuleMap.keySet()) {

                final short unsplitParent = Grammar.unsplitParent(binaryKey);
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
    }

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
