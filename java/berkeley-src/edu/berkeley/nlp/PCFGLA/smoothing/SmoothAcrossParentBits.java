/**
 * 
 */
package edu.berkeley.nlp.PCFGLA.smoothing;

import java.io.Serializable;
import java.util.List;

import edu.berkeley.nlp.PCFGLA.BinaryCounterTable;
import edu.berkeley.nlp.PCFGLA.BinaryRule;
import edu.berkeley.nlp.PCFGLA.UnaryCounterTable;
import edu.berkeley.nlp.PCFGLA.UnaryRule;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;

/**
 * @author leon
 * 
 */
public class SmoothAcrossParentBits implements Smoother, Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    double same;
    double[][][] diffWeights;
    double weightBasis = 0.5;
    double totalWeight;

    public SmoothAcrossParentBits copy() {
        return new SmoothAcrossParentBits(same, diffWeights, weightBasis, totalWeight);
    }

    public SmoothAcrossParentBits(final double smooth, final Tree<Short>[] splitTrees) {
        // does not smooth across top-level split, otherwise smooths uniformly

        same = 1 - smooth;
        // int maxNBits = (int)Math.round(Math.log(maxSubstates)/Math.log(2));

        final int nStates = splitTrees.length;
        diffWeights = new double[nStates][][];
        for (short state = 0; state < nStates; state++) {
            Tree<Short> splitTree = splitTrees[state];
            final List<Short> allSubstates = splitTree.getYield();
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
                // TODO: weighted smoothing

                // descend down to first split first
                while (splitTree.getChildren().size() == 1) {
                    splitTree = splitTree.getChildren().get(0);
                }
                // for (short substate=0; substate<nSubstates; substate++){
                // for (int branch=0; branch<2; branch++){
                // List<Short> substatesInBranch =
                // splitTree.getChildren().get(branch).getYield();
                // if (substatesInBranch.contains(substate)){
                // totalWeight = 0;
                // fillWeightsArray(state,substate,1.0,splitTree.getChildren().get(branch));
                // // normalize the weights
                // if (totalWeight==0) continue;
                // for (short substate2 = 0; substate2<nSubstates; substate2++){
                // if (substate==substate2) continue;
                // diffWeights[state][substate][substate2] /= totalWeight;
                // diffWeights[state][substate][substate2] *= smooth;
                // }
                // }
                // //else - dont smooth across top-level branch
                // }
                // }

                for (int branch = 0; branch < 2; branch++) {
                    // compute weights for substates in top-level branch
                    final List<Short> substatesInBranch = splitTree.getChildren().get(branch).getYield();
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
        /*
         * diffWeights = new double[maxNBits+1]; for (int i=0; i<=maxNBits; i++) { diffWeights[i] =
         * Math.pow(2,-i+1)*smooth/maxNBits; }
         */
    }

    /**
     * @param same2
     * @param diffWeights2
     * @param weightBasis2
     * @param totalWeight2
     */
    public SmoothAcrossParentBits(final double same2, final double[][][] diffWeights2, final double weightBasis2,
            final double totalWeight2) {
        this.same = same2;
        this.diffWeights = diffWeights2;
        this.weightBasis = weightBasis2;
        this.totalWeight = totalWeight2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.berkeley.nlp.PCFGLA.smoothing.Smoother#smooth(edu.berkeley.nlp.util .UnaryCounterTable,
     * edu.berkeley.nlp.util.BinaryCounterTable)
     */
    public void smooth(final UnaryCounterTable unaryCounter, final BinaryCounterTable binaryCounter) {
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
    }

    public void smooth(final short tag, final double[] scores) {
        final double[] scopy = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            for (int k = 0; k < scores.length; k++) {
                scopy[i] += diffWeights[tag][i][k] * scores[k];
            }
        }
        for (int i = 0; i < scores.length; i++) {
            // if (scores[i]==0) continue;
            scores[i] = scopy[i];
        }
    }

    public void updateWeights(final int[][] toSubstateMapping) {
        final double[][][] newWeights = new double[toSubstateMapping.length][][];
        for (int state = 0; state < toSubstateMapping.length; state++) {
            final int nSub = toSubstateMapping[state][0];
            newWeights[state] = new double[nSub][nSub];
            if (nSub == 1) {
                newWeights[state][0][0] = 1.0;
                continue;
            }
            final double[] total = new double[nSub];
            for (int substate1 = 0; substate1 < diffWeights[state].length; substate1++) {
                for (int substate2 = 0; substate2 < diffWeights[state].length; substate2++) {
                    newWeights[state][toSubstateMapping[state][substate1 + 1]][toSubstateMapping[state][substate2 + 1]] += diffWeights[state][substate1][substate2];
                    total[toSubstateMapping[state][substate1 + 1]] += diffWeights[state][substate1][substate2];
                }
            }
            for (int substate1 = 0; substate1 < nSub; substate1++) {
                for (int substate2 = 0; substate2 < nSub; substate2++) {
                    newWeights[state][substate1][substate2] /= total[substate1];
                }
            }
        }
        diffWeights = newWeights;
    }

    public Smoother remapStates(final Numberer thisNumberer, final Numberer newNumberer) {
        final SmoothAcrossParentBits remappedSmoother = copy();
        remappedSmoother.diffWeights = new double[newNumberer.size()][][];
        for (int s = 0; s < newNumberer.size(); s++) {
            final int translatedState = translateState(s, newNumberer, thisNumberer);
            if (translatedState >= 0) {
                remappedSmoother.diffWeights[s] = diffWeights[translatedState];
            } else {
                remappedSmoother.diffWeights[s] = new double[1][1];
            }
        }
        return remappedSmoother;
    }

    private short translateState(final int state, final Numberer baseNumberer, final Numberer translationNumberer) {
        final String symbol = baseNumberer.symbol(state);
        if (translationNumberer.hasSeen(symbol)) {
            return (short) translationNumberer.number(symbol);
        }
        return (short) -1;
    }
}
