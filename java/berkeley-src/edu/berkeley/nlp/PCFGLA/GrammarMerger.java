package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;
import java.util.Collections;

import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.ArrayUtil;
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
            if (!grammar.isGrammarTag(state)) {
                lexiconStates.add(tagNumberer.symbol(state), newNumSubStatesArray[state]);
            } else {
                grammarStates.add(tagNumberer.symbol(state), newNumSubStatesArray[state]);
            }
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
            // System.out.println("\nDoing one merge iteration.");
            // for (short state=0; state<numSubStatesArray.length; state++) {
            // System.out.print("\n  State "+grammar.tagNumberer.object(state));
            // for (int i=0; i<mergeThisIteration[state].length; i++){
            // for (int j=i+1; j<mergeThisIteration[state][i].length; j++){
            // if (mergeThisIteration[state][i][j])
            // System.out.print(". Merging pair ("+i+","+j+")");
            // }
            // }
            // }
            newGrammar = grammar.mergeStates(mergeThisIteration, mergeWeights);
            lexicon.mergeStates(mergeThisIteration, mergeWeights);
            // fix merge weights
            grammar.fixMergeWeightsEtc(mergeThesePairs, mergeWeights, mergeThisIteration);
            grammar = newGrammar;
            newNumSubStatesArray = grammar.numSubStates;
        }
        grammar.makeCRArrays();

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
            double ll = stateSetTree.label().getIScore(0);
            ll = Math.log(ll) + (100 * stateSetTree.label().getIScale());

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
            parser.doInsideOutsideScores(stateSetTree, false); // E
                                                               // Step
            double ll = stateSetTree.label().getIScore(0);
            ll = Math.log(ll) + (100 * stateSetTree.label().getIScale());// System.out.println(stateSetTree);
            if (Double.isInfinite(ll)) {
                System.out.println("Training sentence " + n + " is given -inf log likelihood!");
            } else {
                trainingLikelihood += ll; // there are for some reason some
                                          // sentences that are unparsable
                grammar.tallyMergeWeights(stateSetTree, mergeWeights);
            }
            n++;
        }
        System.out.println("The trainings LL before merging is " + trainingLikelihood);
        // normalize the weights
        grammar.normalizeMergeWeights(mergeWeights);

        return mergeWeights;
    }

    /**
     * @param deltas
     * @return Merge pairs
     */
    public static boolean[][][] determineMergePairs(final double[][][] deltas, final boolean separateMerge,
            final double mergingPercentage, final Grammar grammar) {
        final boolean[][][] mergeThesePairs = new boolean[grammar.numSubStates.length][][];
        final short[] numSubStatesArray = grammar.numSubStates;
        // set the threshold so that p percent of the splits are merged again.
        final ArrayList<Double> deltaSiblings = new ArrayList<Double>();
        final ArrayList<Double> deltaPairs = new ArrayList<Double>();
        final ArrayList<Double> deltaLexicon = new ArrayList<Double>();
        final ArrayList<Double> deltaGrammar = new ArrayList<Double>();
        int nSiblings = 0, nSiblingsGr = 0, nSiblingsLex = 0;
        for (int state = 0; state < mergeThesePairs.length; state++) {
            for (int sub1 = 0; sub1 < numSubStatesArray[state] - 1; sub1++) {
                if (sub1 % 2 == 0 && deltas[state][sub1][sub1 + 1] != 0) {
                    deltaSiblings.add(deltas[state][sub1][sub1 + 1]);
                    if (separateMerge) {
                        if (grammar.isGrammarTag(state)) {
                            deltaGrammar.add(deltas[state][sub1][sub1 + 1]);
                            nSiblingsGr++;
                        } else {
                            deltaLexicon.add(deltas[state][sub1][sub1 + 1]);
                            nSiblingsLex++;
                        }
                    }
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
        double thresholdGr = -1, thresholdLex = -1;
        if (separateMerge) {
            System.out.println("Going to merge " + (int) (mergingPercentage * 100) + "% of the substates siblings.");
            System.out.println("Setting the merging threshold for lexicon and grammar separately.");
            Collections.sort(deltaGrammar);
            Collections.sort(deltaLexicon);
            thresholdGr = deltaGrammar.get((int) (nSiblingsGr * mergingPercentage));
            thresholdLex = deltaLexicon.get((int) (nSiblingsLex * mergingPercentage * 1.5));
            System.out.println("Setting the threshold for lexical siblings to " + thresholdLex);
            System.out.println("Setting the threshold for grammatical siblings to " + thresholdGr);
        } else {
            // String topNmerge = CommandLineUtils.getValueOrUseDefault(input,
            // "-top", "");
            // Collections.sort(deltaPairs);
            // System.out.println(deltaPairs);
            Collections.sort(deltaSiblings);
            // if (topNmerge.equals("")) {
            System.out.println("Going to merge " + (int) (mergingPercentage * 100) + "% of the substates siblings.");
            // System.out.println("Furthermore "+(int)(mergingPercentage2*100)+"% of the non-siblings will be merged.");
            threshold = deltaSiblings.get((int) (nSiblings * mergingPercentage));
            // if (maxSubStates>2 && mergingPercentage2>0) threshold2 =
            // deltaPairs.get((int)(nPairs*mergingPercentage2));
            // } else {
            // int top = Integer.parseInt(topNmerge);
            // System.out.println("Keeping the top "+top+" substates.");
            // threshold = deltaSiblings.get(nPairs-top);
            // }
            System.out.println("Setting the threshold for siblings to " + threshold + ".");
        }
        // if (maxSubStates>2 && mergingPercentage2>0)
        // System.out.println("Setting the threshold for other pairs to "+threshold2);
        final int mergePair = 0;
        int mergeSiblings = 0;
        for (int state = 0; state < mergeThesePairs.length; state++) {
            mergeThesePairs[state] = new boolean[numSubStatesArray[state]][numSubStatesArray[state]];
            for (int i = 0; i < numSubStatesArray[state] - 1; i++) {
                if (i % 2 == 0 && deltas[state][i][i + 1] != 0) {
                    if (separateMerge) {
                        if (grammar.isGrammarTag(state))
                            mergeThesePairs[state][i][i + 1] = deltas[state][i][i + 1] <= thresholdGr;
                        else
                            mergeThesePairs[state][i][i + 1] = deltas[state][i][i + 1] <= thresholdLex;
                    } else
                        mergeThesePairs[state][i][i + 1] = deltas[state][i][i + 1] <= threshold;
                    if (mergeThesePairs[state][i][i + 1]) {
                        mergeSiblings++;
                    }
                }
                // if (mergingPercentage2>0) {
                // for (int j=i+1; j<numSubStatesArray[state]; j++) {
                // if (!(j!=i+1 && i%2!=0) && deltas[state][i][j]!=0 &&
                // deltas[state][i][j] <= threshold2){
                // mergeThesePairs[state][i][j] = true;
                // mergePair++;
                // System.out.println("Merging pair ("+i+","+j+") of state "+tagNumberer.object(state));
                // }
                // }
                // }
            }
        }
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
