/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.Serializable;
import java.util.List;

import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedBinaryCount;
import edu.berkeley.nlp.PCFGLA.Grammar.PackedUnaryCount;
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

    /**
     * Note: This implementation will approximately normalize the counts, but the resulting counts will be slightly
     * un-normalized by the added pseudo-counts, so the consumer should re-normalize after smoothing.
     */
    public void smooth(final Int2ObjectOpenHashMap<PackedUnaryCount> packedUnaryCountMap,
            final Int2ObjectOpenHashMap<PackedBinaryCount> packedBinaryCountMap, final short[] splitCounts,
            final double[][] parentCounts) {

        // Smooth unary rules
        for (final int unaryKey : packedUnaryCountMap.keySet()) {

            // Copy the packed representation into an 'un-packed' 2-d matrix, indexed by child split, parent split
            final short unsplitChild = Grammar.unsplitUnaryChild(unaryKey);
            final short unsplitParent = Grammar.unsplitUnaryParent(unaryKey);
            final PackedUnaryCount packedUnaryCount = packedUnaryCountMap.get(unaryKey);

            final double[][] unsmoothedCounts = new double[splitCounts[unsplitChild]][];

            // Copy the packed representation into an 'un-packed' 2-d array
            for (int i = 0, j = 0; i < packedUnaryCount.ruleCounts.length; i++, j += 2) {
                final short childSplit = packedUnaryCount.substates[j];
                final short parentSplit = packedUnaryCount.substates[j + 1];

                // Allocate storage
                if (unsmoothedCounts[childSplit] == null) {
                    unsmoothedCounts[childSplit] = new double[splitCounts[unsplitParent]];
                }
                // Copy observed count
                unsmoothedCounts[childSplit][parentSplit] = packedUnaryCount.ruleCounts[i];
            }

            final double[][] smoothedCounts = new double[splitCounts[unsplitChild]][];

            for (int childSplit = 0; childSplit < unsmoothedCounts.length; childSplit++) {
                if (unsmoothedCounts[childSplit] == null) {
                    continue;
                }
                smoothedCounts[childSplit] = new double[unsmoothedCounts[childSplit].length];
                for (int parentSplit = 0; parentSplit < unsmoothedCounts[childSplit].length; parentSplit++) {
                    for (int altParentSplit = 0; altParentSplit < unsmoothedCounts[childSplit].length; altParentSplit++) {
                        smoothedCounts[childSplit][parentSplit] += diffWeights[unsplitParent][parentSplit][altParentSplit]
                                * unsmoothedCounts[childSplit][altParentSplit];
                    }
                }
            }

            // Re-pack into the original map
            packedUnaryCountMap.put(unaryKey, new PackedUnaryCount(unsplitParent, unsplitChild, smoothedCounts));
        }

        // Smooth binary rules
        // Smooth the packed representation
        for (final int binaryKey : packedBinaryCountMap.keySet()) {

            final short unsplitLeftChild = Grammar.unsplitLeftChild(binaryKey);
            final short unsplitRightChild = Grammar.unsplitRightChild(binaryKey);
            final short unsplitParent = Grammar.unsplitBinaryParent(binaryKey);

            // Copy the packed representation into an 'un-packed' 3-d matrix, indexed by left-child split, right-child
            // split,
            final PackedBinaryCount packedBinaryCount = packedBinaryCountMap.get(binaryKey);
            final double[][][] unsmoothedCounts = new double[splitCounts[unsplitLeftChild]][][];

            for (int i = 0, j = 0; i < packedBinaryCount.ruleCounts.length; i++, j += 3) {
                final short leftChildSplit = packedBinaryCount.substates[j];
                final short rightChildSplit = packedBinaryCount.substates[j + 1];
                final short parentSplit = packedBinaryCount.substates[j + 2];

                // Allocate storage
                if (unsmoothedCounts[leftChildSplit] == null) {
                    unsmoothedCounts[leftChildSplit] = new double[splitCounts[unsplitRightChild]][];
                }
                if (unsmoothedCounts[leftChildSplit][rightChildSplit] == null) {
                    unsmoothedCounts[leftChildSplit][rightChildSplit] = new double[splitCounts[unsplitParent]];
                }
                // Copy observed count
                unsmoothedCounts[leftChildSplit][rightChildSplit][parentSplit] = packedBinaryCount.ruleCounts[i];
            }

            // Compute smoothed counts (also in unpacked storage)
            final double[][][] smoothedCounts = new double[splitCounts[unsplitLeftChild]][splitCounts[unsplitRightChild]][];
            for (int leftChildSplit = 0; leftChildSplit < unsmoothedCounts.length; leftChildSplit++) {

                if (unsmoothedCounts[leftChildSplit] == null) {
                    continue;
                }

                for (int rightChildSplit = 0; rightChildSplit < unsmoothedCounts[leftChildSplit].length; rightChildSplit++) {

                    if (unsmoothedCounts[leftChildSplit][rightChildSplit] == null) {
                        continue; // nothing to smooth
                    }

                    // Add in diff for parent split, alt parent split * score for left-child, right child, alt parent
                    smoothedCounts[leftChildSplit][rightChildSplit] = new double[unsmoothedCounts[leftChildSplit][rightChildSplit].length];
                    for (int parentSplit = 0; parentSplit < unsmoothedCounts[leftChildSplit][rightChildSplit].length; parentSplit++) {
                        for (int altParentSplit = 0; altParentSplit < unsmoothedCounts[leftChildSplit][rightChildSplit].length; altParentSplit++) {
                            smoothedCounts[leftChildSplit][rightChildSplit][parentSplit] += diffWeights[unsplitParent][parentSplit][altParentSplit]
                                    * unsmoothedCounts[leftChildSplit][rightChildSplit][altParentSplit];
                        }
                    }
                }
            }

            // Re-pack into the original map
            packedBinaryCountMap.put(binaryKey, new PackedBinaryCount(unsplitParent, unsplitLeftChild,
                    unsplitRightChild, smoothedCounts));
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
