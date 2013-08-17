package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;
import edu.ohsu.cslu.util.IEEEDoubleScaling;

/**
 * Static methods which support merging non-terminals in a {@link Grammar}
 */
public class GrammarMerger {

    protected final static Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

    /**
     * This function was written to have the ability to also merge non-sibling pairs, however this functionality is not
     * used anymore since it seemed tricky to determine an appropriate threshold for merging non-siblings. The function
     * returns a new grammar object and changes the lexicon in place!
     * 
     * @param grammar
     * @param lexicon
     * @param mergeThesePairs
     * @param substateConditionalProbabilities
     */
    public static Grammar merge(Grammar grammar, final Lexicon lexicon, final boolean[][][] mergeThesePairs,
            final double[][] substateConditionalProbabilities) {

        final short[] numSubStatesArray = grammar.numSubStates;
        short[] newNumSubStatesArray = grammar.numSubStates;
        Grammar newGrammar = null;
        while (true) {
            // we want to continue as long as there's something to merge
            boolean somethingToMerge = false;

            for (int state = 0; state < numSubStatesArray.length; state++) {
                for (int substate1 = 0; substate1 < newNumSubStatesArray[state]; substate1++) {
                    for (int substate2 = 0; substate2 < newNumSubStatesArray[state]; substate2++) {
                        somethingToMerge |= mergeThesePairs[state][substate1][substate2];
                    }
                }
            }

            if (!somethingToMerge) {
                break;
            }

            /**
             * mergeThisIteration is which states to merge on this iteration through the loop
             */
            final boolean[][][] mergeThisIteration = new boolean[newNumSubStatesArray.length][][];
            // make mergeThisIteration a copy of mergeTheseStates
            for (int state = 0; state < numSubStatesArray.length; state++) {
                mergeThisIteration[state] = new boolean[mergeThesePairs[state].length][mergeThesePairs[state].length];
                for (int substate1 = 0; substate1 < mergeThesePairs[state].length; substate1++) {
                    for (int substate2 = 0; substate2 < mergeThesePairs[state].length; substate2++) {
                        mergeThisIteration[state][substate1][substate2] = mergeThesePairs[state][substate1][substate2];
                    }
                }
            }
            // delete all complicated merges from mergeThisIteration
            for (int state = 0; state < numSubStatesArray.length; state++) {
                final boolean[] alreadyDecidedToMerge = new boolean[mergeThesePairs[state].length];

                for (int substate1 = 0; substate1 < mergeThesePairs[state].length; substate1++) {
                    for (int substate2 = 0; substate2 < mergeThesePairs[state].length; substate2++) {

                        if (alreadyDecidedToMerge[substate1] || alreadyDecidedToMerge[substate2]) {
                            mergeThisIteration[state][substate1][substate2] = false;
                        }

                        alreadyDecidedToMerge[substate1] = alreadyDecidedToMerge[substate1]
                                || mergeThesePairs[state][substate1][substate2];
                        alreadyDecidedToMerge[substate2] = alreadyDecidedToMerge[substate2]
                                || mergeThesePairs[state][substate1][substate2];
                    }
                }
            }

            // remove merges in mergeThisIteration from mergeThesePairs
            for (int state = 0; state < numSubStatesArray.length; state++) {
                for (int substate1 = 0; substate1 < mergeThesePairs[state].length; substate1++) {
                    for (int substate2 = 0; substate2 < mergeThesePairs[state].length; substate2++) {
                        mergeThesePairs[state][substate1][substate2] = mergeThesePairs[state][substate1][substate2]
                                && !mergeThisIteration[state][substate1][substate2];
                    }
                }
            }

            newGrammar = grammar.mergeStates(mergeThisIteration, substateConditionalProbabilities);
            lexicon.mergeStates(mergeThisIteration, substateConditionalProbabilities);
            // fix merge weights
            grammar.fixMergeWeightsEtc(mergeThesePairs, substateConditionalProbabilities, mergeThisIteration);
            grammar = newGrammar;
            newNumSubStatesArray = grammar.numSubStates;
        }

        return grammar;
    }

    /**
     * Computes the log-likelihood gain (or, more often, loss) over the training corpus of merging each merge candidate
     * (substate pair).
     * 
     * @param grammar
     * @param lexicon
     * @param substateConditionalProbabilities Substate probabilities, conditioned on the occurrence of the parent
     *            state. Indexed by state, substate
     * @param trainStateSetTrees Training-set trees
     * 
     * @return Merge likelihood deltas for each merge candidate (substate pair). Indexed by state, substate 1, substate
     *         2.
     */
    public static float[][][] computeMergeLikelihoodDeltas(final Grammar grammar, final Lexicon lexicon,
            final double[][] substateConditionalProbabilities, final StateSetTreeList trainStateSetTrees) {

        final ArrayParser parser = new ArrayParser(grammar, lexicon);
        final float[][][] deltas = new float[grammar.numSubStates.length][substateConditionalProbabilities[0].length][substateConditionalProbabilities[0].length];

        for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {

            parser.parse(stateSetTree, false); // E-step
            final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().insideScore(0), stateSetTree.label()
                    .insideScoreScale());

            if (!Double.isInfinite(ll)) {
                grammar.tallyMergeLikelihoodDeltas(stateSetTree, deltas, substateConditionalProbabilities);
            }
        }
        return deltas;
    }

    /**
     * Iterates over the training corpus, accumulating state and substate counts, which are returned as normalized
     * conditional probability distributions (i.e., P(X_n | X))
     * 
     * @param grammar
     * @param lexicon
     * @param trainStateSetTrees
     * @return Substate probabilities, conditioned on the occurrence of the parent state. Indexed by state, substate.
     */
    public static double[][] computeSubstateConditionalProbabilities(final Grammar grammar, final Lexicon lexicon,
            final StateSetTreeList trainStateSetTrees) {

        final double[][] substateScores = new double[grammar.numSubStates.length][edu.ohsu.cslu.util.Math
                .max(grammar.numSubStates)];
        double trainingLikelihood = 0;
        final ArrayParser parser = new ArrayParser(grammar, lexicon);
        int n = 0;

        for (final Tree<StateSet> stateSetTree : trainStateSetTrees) {
            parser.parse(stateSetTree, false); // E-step

            final double ll = IEEEDoubleScaling.logLikelihood(stateSetTree.label().insideScore(0), stateSetTree.label()
                    .insideScoreScale());

            if (Double.isInfinite(ll)) {
                System.out.println("Training sentence " + n + " is given -inf log likelihood!");
            } else {
                trainingLikelihood += ll;
                grammar.tallySubstateScores(stateSetTree, substateScores);
            }
            n++;
        }
        BaseLogger.singleton().info("Training corpus LL before merging: " + trainingLikelihood);

        // Normalize the weights to produce proper probatilities
        for (int state = 0; state < substateScores.length; state++) {
            double sum = 0;
            for (int subState = 0; subState < grammar.numSubStates[state]; subState++) {
                sum += substateScores[state][subState];
            }
            if (sum == 0) {
                sum = 1;
            }
            for (int subState = 0; subState < grammar.numSubStates[state]; subState++) {
                substateScores[state][subState] /= sum;
            }
        }

        return substateScores;
    }

    /**
     * Ranks merge candidates by a {@link MergeRankingFunction} and selects the bottom n to be merged. For the moment,
     * the return type is still a <code>boolean</code> array, to match other historical code, although returning a
     * {@link List} of {@link MergeCandidate}s might be more intuitive.
     * 
     * @param mergeLikelihoodDeltas Merge likelihood deltas (i.e. log-likelihood gain or loss) for each merge candidate
     *            (substate pair). Indexed by state, substate 1, substate 2. As computed by
     *            {@link GrammarMerger#computeMergeLikelihoodDeltas(Grammar, Lexicon, double[][], StateSetTreeList)}.
     * @param mergingPercentage
     * @param grammar
     * @param lexicon
     * @param rankingFunction
     * 
     * @return Boolean array indexed by state, substate 1, substate 2; pairs to be merged indicated by <code>true</code>
     *         values.
     */
    public static boolean[][][] selectMergePairs(final float[][][] mergeLikelihoodDeltas,
            final double mergingPercentage, final Grammar grammar, final Lexicon lexicon,
            final MergeRankingFunction rankingFunction) {

        final boolean[][][] mergeThesePairs = new boolean[grammar.numSubStates.length][][];
        final short[] numSubStatesArray = grammar.numSubStates;

        final int[][][] ruleCountDeltas = grammar.estimateMergeRuleCountDelta(lexicon);

        final ArrayList<MergeCandidate> mergeCandidates = new ArrayList<GrammarMerger.MergeCandidate>();

        for (short state = 0; state < numSubStatesArray.length; state++) {

            for (short sub1 = 0; sub1 < numSubStatesArray[state] - 1; sub1++) {

                // Always merge 'down' from substate 2 into substate 1 (sub1 % 2 == 0)
                if (sub1 % 2 == 0 && mergeLikelihoodDeltas[state][sub1][sub1 + 1] != 0) {
                    mergeCandidates.add(new MergeCandidate(state, sub1, (short) (sub1 + 1),
                            mergeLikelihoodDeltas[state][sub1][sub1 + 1], ruleCountDeltas[state][sub1 + 1]));
                }
            }
        }

        // Compute all merge costs, per the ranking function
        computeMergeCostsAndRankings(mergeCandidates, rankingFunction);

        // Order the merge candidates and select a threshold
        Collections.sort(mergeCandidates, rankingFunction.comparator());
        final int thresholdCandidate = (int) (mergeCandidates.size() * mergingPercentage);

        final float threshold = mergeCandidates.get(thresholdCandidate).mergeCost;
        BaseLogger.singleton().info("Merge threshold: " + threshold);

        // Select the bottom portion of the ranked list as the candidates to be merged
        final List<MergeCandidate> mergePairs = mergeCandidates.subList(0, thresholdCandidate + 1);

        for (int state = 0; state < mergeThesePairs.length; state++) {
            mergeThesePairs[state] = new boolean[numSubStatesArray[state]][numSubStatesArray[state]];
        }
        for (final MergeCandidate c : mergePairs) {
            mergeThesePairs[c.state][c.substate1][c.substate2] = true;
        }

        // Output merge pairs
        if (BaseLogger.singleton().isLoggable(Level.INFO)) {
            BaseLogger.singleton().info("Merging " + mergePairs.size() + " siblings.");

            for (short state = 0; state < mergeLikelihoodDeltas.length; state++) {
                for (int i = 0; i < numSubStatesArray[state]; i++) {
                    for (int j = i + 1; j < numSubStatesArray[state]; j++) {
                        if (mergeThesePairs[state][i][j])
                            BaseLogger.singleton().info(
                                    String.format("Merging %s_%d and %s_%d Cost : %f", tagNumberer.symbol(state), i,
                                            tagNumberer.symbol(state), j, mergeLikelihoodDeltas[state][i][j]));
                    }
                }
            }
        }

        return mergeThesePairs;
    }

    /**
     * Sorts the list of merge costs by various criteria and assigns ranking numbers for each (e.g. estimated likelihood
     * loss, rule count savings, etc.), then computes total merge cost for each non-terminal pair
     * 
     * @param grammar
     * @param mergeCandidates An unordered list of estimated merge costs. Note that this list will be reordered.
     * @param rankingFunction A merge-cost ranking function
     */
    private static void computeMergeCostsAndRankings(final ArrayList<MergeCandidate> mergeCandidates,
            final MergeRankingFunction rankingFunction) {

        // Estimated likelihood loss
        Collections.sort(mergeCandidates, MergeRankingFunction.Likelihood.comparator());
        for (int i = 0; i < mergeCandidates.size(); i++) {
            mergeCandidates.get(i).likelihoodLossRanking = i;
        }

        // Total rule count delta
        Collections.sort(mergeCandidates, MergeRankingFunction.TotalRuleCount.comparator());
        for (int i = 0; i < mergeCandidates.size(); i++) {
            mergeCandidates.get(i).totalRuleCountRanking = i;
        }

        for (final MergeCandidate mc : mergeCandidates) {
            mc.mergeCost = rankingFunction.objectiveFunction.mergeCost(mc);
        }
    }

    private static class MergeCandidate {

        private final short state;
        private final short substate1;
        private final short substate2;
        private final float estimatedLikelihoodDelta;
        private final int binaryRuleCountDelta;
        private final int unaryRuleCountDelta;
        private final int lexicalRuleCountDelta;

        private final int totalRuleCountDelta;

        /**
         * The ordinal ranking of this non-terminal pair within the list of {@link MergeCandidate}s if sorted by
         * {@link #estimatedLikelihoodDelta}.
         */
        private int likelihoodLossRanking;

        /**
         * The ordinal ranking of this non-terminal pair within the list of {@link MergeCandidate}s if sorted by
         * {@link #totalRuleCountDelta}.
         */
        private int totalRuleCountRanking;

        /** Combined merge cost, as assigned by a {@link MergeObjectiveFunction} */
        private float mergeCost;

        /**
         * @param state
         * @param substate1
         * @param substate2
         * @param estimatedLikelihoodDelta Estimated likelihood loss on the training corpus.
         * @param ruleCountDelta Estimated rule-count savings if this pair is merged. 3-tuple of binary, unary, and
         *            lexical counts.
         */
        public MergeCandidate(final short state, final short substate1, final short substate2,
                final float estimatedLikelihoodDelta, final int[] ruleCountDelta) {

            this.state = state;
            this.substate1 = substate1;
            this.substate2 = substate2;

            this.estimatedLikelihoodDelta = estimatedLikelihoodDelta;

            if (ruleCountDelta != null) {
                this.binaryRuleCountDelta = ruleCountDelta[0];
                this.unaryRuleCountDelta = ruleCountDelta[1];
                this.lexicalRuleCountDelta = ruleCountDelta[2];
            } else {
                this.binaryRuleCountDelta = 0;
                this.unaryRuleCountDelta = 0;
                this.lexicalRuleCountDelta = 0;
            }

            this.totalRuleCountDelta = binaryRuleCountDelta + unaryRuleCountDelta + lexicalRuleCountDelta;
        }

        @Override
        public String toString() {
            return String.format("%d  %d  %d  %14.6f  %6d  %6d  %6d  %6d  %6d  %6d  %7.2f", state, substate1,
                    substate2, estimatedLikelihoodDelta, likelihoodLossRanking, binaryRuleCountDelta,
                    unaryRuleCountDelta, lexicalRuleCountDelta, totalRuleCountDelta, totalRuleCountRanking, mergeCost);
        }
    }

    /**
     * Objective functions for non-terminal merge ranking. Each enumeration option constructs and exposes an anonymous
     * {@link Comparator} to re-rank merge candidates.
     */
    public static enum MergeRankingFunction {

        Likelihood(new MergeObjectiveFunction() {
            @Override
            public float mergeCost(final MergeCandidate mergeCost) {
                return mergeCost.estimatedLikelihoodDelta;
            }
        }),

        TotalRuleCount(new MergeObjectiveFunction() {
            @Override
            public float mergeCost(final MergeCandidate mergeCost) {
                return mergeCost.totalRuleCountDelta;
            }
        }),

        /**
         * Note: Likelihood and rule-count rankings must be computed, as in
         * {@link GrammarMerger#computeMergeCostsAndRankings(ArrayList, MergeRankingFunction)} before applying this
         * function.
         * 
         * Controlled by the configuration parameter "ruleCountLambda", ranging from 0-1. At lambda=0, merges are
         * controlled entirely by likelihood (as in {@link #Likelihood}) and at \lambda = 1, entirely by rule-count (as
         * in {@link #TotalRuleCount}).
         */
        SparsePrior(new MergeObjectiveFunction() {

            final float ruleCountLambda = GlobalConfigProperties.singleton().getFloatProperty("ruleCountLambda", 0);

            @Override
            public float mergeCost(final MergeCandidate mergeCost) {
                return mergeCost.likelihoodLossRanking * (1 - ruleCountLambda) + mergeCost.totalRuleCountRanking
                        * ruleCountLambda;
            }
        });

        /** Comparator for the specified objective function */
        public final MergeObjectiveFunction objectiveFunction;

        private MergeRankingFunction(final MergeObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
        }

        public Comparator<MergeCandidate> comparator() {
            return new Comparator<MergeCandidate>() {
                @Override
                public int compare(final MergeCandidate o1, final MergeCandidate o2) {
                    return Float.compare(objectiveFunction.mergeCost(o1), objectiveFunction.mergeCost(o2));
                }
            };
        }

        private static abstract class MergeObjectiveFunction {
            public abstract float mergeCost(MergeCandidate mergeCost);
        }
    }
}
