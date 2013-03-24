package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;
import java.util.Collections;

import cltool4j.BaseLogger;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.IEEEDoubleScaling;
import edu.berkeley.nlp.util.Numberer;
import edu.berkeley.nlp.util.PriorityQueue;

public class GrammarMerger {

    /**
     * @param grammar
     * @param newGrammar
     */
    public static void printMergingStatistics(final Grammar grammar, final Grammar newGrammar) {
        final PriorityQueue<String> lexiconStates = new PriorityQueue<String>();
        final PriorityQueue<String> grammarStates = new PriorityQueue<String>();
        final short[] numSubStatesArray = grammar.numSubStates;
        final short[] newNumSubStatesArray = newGrammar.numSubStates;
        final Numberer tagNumberer = grammar.tagNumberer;
        for (short state = 0; state < numSubStatesArray.length; state++) {
            System.out.print("\nState " + tagNumberer.symbol(state) + " had " + numSubStatesArray[state]
                    + " substates and now has " + newNumSubStatesArray[state] + ".");
            // This is broken, but we're going to change it anyway...
            // if (!grammar.isGrammarTag(state)) {
            lexiconStates.add(tagNumberer.symbol(state), newNumSubStatesArray[state]);
            // } else {
            // grammarStates.add(tagNumberer.symbol(state), newNumSubStatesArray[state]);
            // }
        }

        System.out.print("\n");
        System.out.println("Lexicon: " + lexiconStates.toString());
        System.out.println("Grammar: " + grammarStates.toString());
    }

    /**
     * This function was written to have the ability to also merge non-sibling pairs, however this functionality is not
     * used anymore since it seemed tricky to determine an appropriate threshold for merging non-siblings. The function
     * returns a new grammar object and changes the lexicon in place!
     * 
     * @param grammar
     * @param lexicon
     * @param mergeThesePairs
     * @param mergeWeights
     */
    public static Grammar doTheMerges(Grammar grammar, final Lexicon lexicon, final boolean[][][] mergeThesePairs,
            final double[][] mergeWeights) {

        final short[] numSubStatesArray = grammar.numSubStates;
        short[] newNumSubStatesArray = grammar.numSubStates;
        Grammar newGrammar = null;
        while (true) {
            // we want to continue as long as there's something to merge
            boolean somethingToMerge = false;
            for (int tag = 0; tag < numSubStatesArray.length; tag++) {
                for (int i = 0; i < newNumSubStatesArray[tag]; i++) {
                    for (int j = 0; j < newNumSubStatesArray[tag]; j++) {
                        somethingToMerge = somethingToMerge || mergeThesePairs[tag][i][j];
                    }
                }
            }
            if (!somethingToMerge)
                break;
            /**
             * mergeThisIteration is which states to merge on this iteration through the loop
             */
            final boolean[][][] mergeThisIteration = new boolean[newNumSubStatesArray.length][][];
            // make mergeThisIteration a copy of mergeTheseStates
            for (int tag = 0; tag < numSubStatesArray.length; tag++) {
                mergeThisIteration[tag] = new boolean[mergeThesePairs[tag].length][mergeThesePairs[tag].length];
                for (int i = 0; i < mergeThesePairs[tag].length; i++) {
                    for (int j = 0; j < mergeThesePairs[tag].length; j++) {
                        mergeThisIteration[tag][i][j] = mergeThesePairs[tag][i][j];
                    }
                }
            }
            // delete all complicated merges from mergeThisIteration
            for (int tag = 0; tag < numSubStatesArray.length; tag++) {
                final boolean[] alreadyDecidedToMerge = new boolean[mergeThesePairs[tag].length];
                for (int i = 0; i < mergeThesePairs[tag].length; i++) {
                    for (int j = 0; j < mergeThesePairs[tag].length; j++) {
                        if (alreadyDecidedToMerge[i] || alreadyDecidedToMerge[j])
                            mergeThisIteration[tag][i][j] = false;
                        alreadyDecidedToMerge[i] = alreadyDecidedToMerge[i] || mergeThesePairs[tag][i][j];
                        alreadyDecidedToMerge[j] = alreadyDecidedToMerge[j] || mergeThesePairs[tag][i][j];
                    }
                }
            }
            // remove merges in mergeThisIteration from mergeThesePairs
            for (int tag = 0; tag < numSubStatesArray.length; tag++) {
                for (int i = 0; i < mergeThesePairs[tag].length; i++) {
                    for (int j = 0; j < mergeThesePairs[tag].length; j++) {
                        mergeThesePairs[tag][i][j] = mergeThesePairs[tag][i][j] && !mergeThisIteration[tag][i][j];
                    }
                }
            }
            newGrammar = grammar.mergeStates(mergeThisIteration, mergeWeights);
            lexicon.mergeStates(mergeThisIteration, mergeWeights);
            // fix merge weights
            grammar.fixMergeWeightsEtc(mergeThesePairs, mergeWeights, mergeThisIteration);
            grammar = newGrammar;
            newNumSubStatesArray = grammar.numSubStates;
        }

        return grammar;
    }

    /**
     * @param grammar
     * @param lexicon
     * @param mergeWeights
     * @param trainStateSetTrees
     * @return Deltas
     */
    public static double[][][] computeDeltas(final Grammar grammar, final Lexicon lexicon,
            final double[][] mergeWeights, final StateSetTreeList trainStateSetTrees) {

        final ArrayParser parser = new ArrayParser(grammar, lexicon);
        final double[][][] deltas = new double[grammar.numSubStates.length][mergeWeights[0].length][mergeWeights[0].length];

        for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {

            parser.doInsideOutsideScores(stateSetTree, false); // E-step
            final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().insideScore(0), stateSetTree.label()
                    .insideScoreScale());

            if (!Double.isInfinite(ll)) {
                grammar.tallyMergeScores(stateSetTree, deltas, mergeWeights);
            }
        }
        return deltas;
    }

    /**
     * @param grammar
     * @param lexicon
     * @param trainStateSetTrees
     * @return Merge weights
     */
    public static double[][] computeMergeWeights(final Grammar grammar, final Lexicon lexicon,
            final StateSetTreeList trainStateSetTrees) {

        final double[][] mergeWeights = new double[grammar.numSubStates.length][(int) ArrayUtil
                .max(grammar.numSubStates)];
        double trainingLikelihood = 0;
        final ArrayParser parser = new ArrayParser(grammar, lexicon);
        int n = 0;

        for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
            parser.doInsideOutsideScores(stateSetTree, false); // E-step

            final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().insideScore(0), stateSetTree.label()
                    .insideScoreScale());

            if (Double.isInfinite(ll)) {
                System.out.println("Training sentence " + n + " is given -inf log likelihood!");
            } else {
                trainingLikelihood += ll;
                grammar.tallyMergeWeights(stateSetTree, mergeWeights);
            }
            n++;
        }
        BaseLogger.singleton().info("Training corpus LL before merging: " + trainingLikelihood);
        // normalize the weights
        grammar.normalizeMergeWeights(mergeWeights);

        return mergeWeights;
    }

    /**
     * @param deltas
     * @return Merge pairs
     */
    public static boolean[][][] determineMergePairs(final double[][][] deltas, final double mergingPercentage,
            final Grammar grammar) {

        final boolean[][][] mergeThesePairs = new boolean[grammar.numSubStates.length][][];
        final short[] numSubStatesArray = grammar.numSubStates;
        final ArrayList<Double> deltaSiblings = new ArrayList<Double>();
        final ArrayList<Double> deltaPairs = new ArrayList<Double>();
        int nSiblings = 0;

        for (int state = 0; state < mergeThesePairs.length; state++) {
            for (int sub1 = 0; sub1 < numSubStatesArray[state] - 1; sub1++) {
                if (sub1 % 2 == 0 && deltas[state][sub1][sub1 + 1] != 0) {
                    deltaSiblings.add(deltas[state][sub1][sub1 + 1]);
                    nSiblings++;
                }
                for (int sub2 = sub1 + 1; sub2 < numSubStatesArray[state]; sub2++) {
                    if (!(sub2 != sub1 + 1 && sub1 % 2 != 0) && deltas[state][sub1][sub2] != 0) {
                        deltaPairs.add(deltas[state][sub1][sub2]);
                    }
                }
            }
        }
        double threshold = -1;
        Collections.sort(deltaSiblings);
        System.out.println("Going to merge " + (int) (mergingPercentage * 100) + "% of the substates siblings.");
        threshold = deltaSiblings.get((int) (nSiblings * mergingPercentage));
        System.out.println("Setting the threshold for siblings to " + threshold + ".");

        final int mergePair = 0;
        int mergeSiblings = 0;
        for (int state = 0; state < mergeThesePairs.length; state++) {
            mergeThesePairs[state] = new boolean[numSubStatesArray[state]][numSubStatesArray[state]];
            for (int i = 0; i < numSubStatesArray[state] - 1; i++) {
                if (i % 2 == 0 && deltas[state][i][i + 1] != 0) {
                    mergeThesePairs[state][i][i + 1] = deltas[state][i][i + 1] <= threshold;
                    if (mergeThesePairs[state][i][i + 1]) {
                        mergeSiblings++;
                    }
                }
            }
        }
        // TODO Replace the output with the BUBS version
        // // Output the costs and potential rule-count savings for each mergeable non-terminal
        // if (BaseLogger.singleton().isLoggable(Level.FINE)) {
        // final StringBuilder sb = new StringBuilder();
        // for (int i = 0; i < mergeCosts.size(); i++) {
        // sb.append(mergeCosts.get(i).toString());
        // sb.append('\n');
        //
        // // Label the cut-point
        // if (i == mergeIndices.length) {
        // sb.append("--------\n");
        // }
        // }
        // BaseLogger.singleton().fine("Merge Costs:");
        // BaseLogger.singleton().fine(sb.toString());
        // }
        System.out.println("Merging " + mergeSiblings + " siblings and " + mergePair + " other pairs.");
        for (short state = 0; state < deltas.length; state++) {
            System.out.print("State " + grammar.tagNumberer.symbol(state));
            for (int i = 0; i < numSubStatesArray[state]; i++) {
                for (int j = i + 1; j < numSubStatesArray[state]; j++) {
                    if (mergeThesePairs[state][i][j])
                        System.out.print(". Merging pair (" + i + "," + j + ") at cost " + deltas[state][i][j]);
                }
            }
            System.out.print(".\n");
        }
        return mergeThesePairs;
    }

}
