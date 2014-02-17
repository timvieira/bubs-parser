package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeRanking.MergeObjectiveFunction;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;
import edu.ohsu.cslu.util.IEEEDoubleScaling;

/**
 * Static methods which support merging non-terminals in a {@link Grammar}
 */
public class GrammarMerger {

    protected final static Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

    final static float EFFICIENCY_LAMBDA = GlobalConfigProperties.singleton().getFloatProperty("efficiencyLambda", 0);

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
     * Ranks merge candidates by a {@link MergeRanking} and selects the bottom n to be merged. For the moment, the
     * return type is still a <code>boolean</code> array, to match other historical code, although returning a
     * {@link List} of {@link MergeCandidate}s might be more intuitive.
     * 
     * @param grammar
     * @param lexicon
     * @param substateConditionalProbabilities TODO
     * @param mergeLikelihoodDeltas Merge likelihood deltas (i.e. log-likelihood gain or loss) for each merge candidate
     *            (substate pair). Indexed by state, substate 1, substate 2. As computed by
     *            {@link GrammarMerger#computeMergeLikelihoodDeltas(Grammar, Lexicon, double[][], StateSetTreeList)}.
     * @param ruleCountDeltas Change in total rule-count for each merge candidate. Indexed by state, substate 1,
     *            substate 2. As computed by {@link Grammar#estimateMergeRuleCountDeltas(Lexicon)}.
     * @param mergingPercentage
     * @param rankingFunction
     * @param cycle The current training cycle (1-based)
     * 
     * @return Boolean array indexed by state, substate 1, substate 2; pairs to be merged indicated by <code>true</code>
     *         values.
     */
    public static boolean[][][] selectMergePairs(final Grammar grammar, final Lexicon lexicon,
            final double[][] substateConditionalProbabilities, final float[][][] mergeLikelihoodDeltas,
            final int[][][] ruleCountDeltas, final double mergingPercentage, final MergeRanking rankingFunction,
            final int cycle, final float minimumRuleProbability) {

        final boolean[][][] mergeThesePairs = new boolean[grammar.numSubStates.length][][];
        final short[] numSubStatesArray = grammar.numSubStates;

        final ArrayList<MergeCandidate> mergeCandidates = new ArrayList<GrammarMerger.MergeCandidate>();

        for (short state = 0; state < numSubStatesArray.length; state++) {

            for (short sub1 = 0; sub1 < numSubStatesArray[state] - 1; sub1++) {

                // Always merge 'down' from substate 2 into substate 1 (sub1 % 2 == 0)
                if (sub1 % 2 == 0 && mergeLikelihoodDeltas[state][sub1][sub1 + 1] != 0) {
                    mergeCandidates.add(new MergeCandidate(state, sub1, (short) (sub1 + 1), grammar.isPos(state),
                            -mergeLikelihoodDeltas[state][sub1][sub1 + 1], ruleCountDeltas[state][sub1 + 1]));
                }
            }
        }

        // Compute all merge costs, per the ranking function
        computeMergeCostsAndRankings(grammar, lexicon, substateConditionalProbabilities, mergeCandidates,
                rankingFunction, cycle, minimumRuleProbability);

        // Order the merge candidates (least valuable, or most-mergeable, candidates first; most-valuable last) and
        // select a threshold
        Collections.sort(mergeCandidates, rankingFunction.comparator());
        final int thresholdCandidate = (int) (mergeCandidates.size() * mergingPercentage);

        final float threshold = rankingFunction.objectiveFunction.mergeCost(mergeCandidates.get(thresholdCandidate));
        BaseLogger.singleton().info("Merge threshold: " + threshold);

        // Select the bottom portion of the ranked list as the candidates to be merged
        final List<MergeCandidate> selectedMergeCandidates = mergeCandidates.subList(0, thresholdCandidate + 1);

        for (int state = 0; state < mergeThesePairs.length; state++) {
            mergeThesePairs[state] = new boolean[numSubStatesArray[state]][numSubStatesArray[state]];
        }

        BaseLogger.singleton().info("Merging " + selectedMergeCandidates.size() + " siblings.");
        for (final MergeCandidate c : selectedMergeCandidates) {
            mergeThesePairs[c.state][c.substate1][c.substate2] = true;

            // Output merge pairs
            final float mergeCost = rankingFunction.objectiveFunction.mergeCost(c);
            BaseLogger.singleton().info(
                    String.format("Merging %s_%d and %s_%d Cost : %f", tagNumberer.symbol(c.state), c.substate1,
                            tagNumberer.symbol(c.state), c.substate2, mergeCost));
        }

        return mergeThesePairs;
    }

    /**
     * Sorts the list of merge costs by various criteria and assigns ranking numbers for each (e.g. estimated likelihood
     * loss, rule count savings, etc.), then computes total merge cost for each non-terminal pair
     * 
     * @param grammar
     * @param substateConditionalProbabilities TODO
     * @param mergeCandidates An unordered list of estimated merge costs. Note that this list will be reordered.
     * @param rankingFunction A merge-cost ranking function
     * @param cycle The current training cycle (1-based)
     * @param minimumRuleProbability
     */
    private static void computeMergeCostsAndRankings(final Grammar grammar, final Lexicon lexicon,
            final double[][] substateConditionalProbabilities, final ArrayList<MergeCandidate> mergeCandidates,
            final MergeRanking rankingFunction, final int cycle, final float minimumRuleProbability) {

        final long t0 = System.currentTimeMillis();

        // Estimated likelihood loss is an inexpensive calculation and is prepopulated in the MergeCandidates. This
        // ranking is used as an input by several MergeObjectiveFunctions, so we populate it before computing other
        // rankings
        Collections.sort(mergeCandidates, MergeCandidate.estimatedLikelihoodComparator());
        for (int i = 0; i < mergeCandidates.size(); i++) {
            mergeCandidates.get(i).estimatedAccuracyRanking = i;
        }

        final MergeObjectiveFunction objectiveFunction = rankingFunction.objectiveFunction();
        objectiveFunction.initMergeCycle(grammar, lexicon, cycle);
        objectiveFunction
                .initMergeCandidates(mergeCandidates, substateConditionalProbabilities, minimumRuleProbability);

        BaseLogger.singleton().info(
                String.format("Examined %d merge candidates in %.3f seconds", mergeCandidates.size(),
                        (System.currentTimeMillis() - t0) / 1000f));

    }

    static class MergeCandidate {

        final short state;
        final short substate1;
        final short substate2;
        private final float estimatedLikelihoodDelta;

        final int binaryRuleCountDelta;
        final int unaryRuleCountDelta;
        final int lexicalRuleCountDelta;

        private final int totalRuleCountDelta;

        final int vPosDelta, vPhraseDelta;
        float medRowDensityDelta, medColDensityDelta;

        /**
         * The ordinal ranking of this non-terminal pair within the list of {@link MergeCandidate}s if sorted by (an
         * estimate of) accuracy. e.g., {@link #estimatedLikelihoodDelta} or {@link #estimatedAccuracyDelta}.
         */
        private int estimatedAccuracyRanking;

        /**
         * The ordinal ranking of this non-terminal pair within the list of {@link MergeCandidate}s if sorted by (an
         * estimate of) inference speed. e.g., {@link #totalRuleCountDelta} or {@link #estimatedInferenceSpeedDelta}
         */
        private int estimatedSpeedRanking;

        /** Estimated accuracy change (e.g. -{@link #estimatedLikelihoodDelta} or actual measured F1) */
        float estimatedAccuracyDelta;

        /**
         * Estimated parsing speed change (e.g. {@link #totalRuleCountDelta} a modeled prediction of w/s, or actual
         * measured speed)
         */
        float estimatedInferenceSpeedDelta;

        /**
         * @param state
         * @param substate1
         * @param substate2
         * @param estimatedLikelihoodDelta Estimated likelihood loss on the training corpus.
         * @param ruleCountDelta Estimated rule-count savings if this pair is merged. 3-tuple of binary, unary, and
         *            lexical counts.
         */
        public MergeCandidate(final short state, final short substate1, final short substate2, final boolean isPos,
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

            if (isPos) {
                vPosDelta = -1;
                vPhraseDelta = 0;
            } else {
                vPhraseDelta = -1;
                vPosDelta = 0;
            }

            this.totalRuleCountDelta = binaryRuleCountDelta + unaryRuleCountDelta + lexicalRuleCountDelta;
        }

        public int estimatedAccuracyRanking() {
            return estimatedAccuracyRanking;
        }

        public void setEstimatedAccuracyRanking(final int estimatedAccuracyRanking) {
            this.estimatedAccuracyRanking = estimatedAccuracyRanking;
        }

        public int estimatedSpeedRanking() {
            return estimatedSpeedRanking;
        }

        public void setEstimatedSpeedRanking(final int estimatedSpeedRanking) {
            this.estimatedSpeedRanking = estimatedSpeedRanking;
        }

        @Override
        public String toString() {
            final Numberer numberer = Numberer.getGlobalNumberer("tags");
            return String
                    .format("%19s  LL=%-14.6f  Bin=%-6d  Un=%-6d  Lex=%-6d  Tot=%-6d  Acc_delta=%-9.3f  Acc_rank=%-6d  Speed_delta=%-9.3f  Sp_rank=%d",
                            String.format("%s_%d / %s_%d", numberer.symbol(state), substate1, numberer.symbol(state),
                                    substate2), estimatedLikelihoodDelta, binaryRuleCountDelta, unaryRuleCountDelta,
                            lexicalRuleCountDelta, totalRuleCountDelta, -estimatedAccuracyDelta * 100f,
                            estimatedAccuracyRanking, -estimatedInferenceSpeedDelta, estimatedSpeedRanking);
        }

        public static Comparator<MergeCandidate> estimatedLikelihoodComparator() {
            return new Comparator<MergeCandidate>() {
                @Override
                public int compare(final MergeCandidate o1, final MergeCandidate o2) {
                    return -Float.compare(o1.estimatedLikelihoodDelta, o2.estimatedLikelihoodDelta);
                }
            };
        }

        public static Comparator<MergeCandidate> estimatedAccuracyComparator() {
            return new Comparator<MergeCandidate>() {
                @Override
                public int compare(final MergeCandidate o1, final MergeCandidate o2) {
                    return Float.compare(o1.estimatedAccuracyDelta, o2.estimatedAccuracyDelta);
                }
            };
        }

        public static Comparator<MergeCandidate> estimatedSpeedComparator() {
            return new Comparator<MergeCandidate>() {
                @Override
                public int compare(final MergeCandidate o1, final MergeCandidate o2) {
                    return Float.compare(o1.estimatedInferenceSpeedDelta, o2.estimatedInferenceSpeedDelta);
                }
            };
        }
    }

    /**
     * Objective functions for non-terminal merge ranking. Each enumeration option provides a
     * {@link MergeObjectiveFunction} to estimate the cost of re-merging a {@link MergeCandidate}. Simple ranking
     * methods use anonymous implementations directly inline; more complex implementations reference separate classes
     * (e.g. {@link ModeledMergeObjectiveFunction} and {@link DiscriminativeMergeObjectiveFunction}).
     */
    public static enum MergeRanking {

        /**
         * Ignores {@link GrammarMerger#EFFICIENCY_LAMBDA} and ranks by estimated likelihood loss
         */
        Likelihood(new MergeObjectiveFunction() {
            @Override
            public float mergeCost(final MergeCandidate mergeCandidate) {
                return -mergeCandidate.estimatedLikelihoodDelta;
            }

            @Override
            public void initMergeCandidates(final List<MergeCandidate> mergeCandidates,
                    final double[][] substateConditionalProbabilities, final float minimumRuleProbability) {

                // We always populate and rank by estimated likelihood loss, so there's nothing else to do here
            }
        }),

        /**
         * Incorporates |P| as an estimate of inference speed and likelihood loss as an estimate of accuracy into the
         * objective function.
         * 
         * Controlled by the configuration parameter "ruleCountLambda", ranging from 0-1. At lambda=0, merges are
         * controlled entirely by likelihood (as in {@link #Likelihood}) and at \lambda = 1, entirely by rule-count (as
         * in {@link #TotalRuleCount}).
         */
        TotalRuleCount(new MergeObjectiveFunction() {

            @Override
            public void initMergeCandidates(final List<MergeCandidate> mergeCandidates,
                    final double[][] substateConditionalProbabilities, final float minimumRuleProbability) {

                // Note: estimated likelihood loss rankings are already populated

                // Rank by total rule count delta
                Collections.sort(mergeCandidates, new Comparator<MergeCandidate>() {

                    @Override
                    public int compare(final MergeCandidate o1, final MergeCandidate o2) {
                        return Integer.compare(o1.totalRuleCountDelta, o2.totalRuleCountDelta);
                    }
                });

                for (int i = 0; i < mergeCandidates.size(); i++) {
                    mergeCandidates.get(i).estimatedSpeedRanking = i;
                }
            }
        }),

        /**
         * Ranks by a modeled merge objective function (per a linear model fit on previous trials).
         * 
         * Controlled by the configuration parameter "efficiencyLambda", ranging from 0-1. At lambda=0, merges are
         * controlled entirely by likelihood (as in {@link #Likelihood}) and at \lambda = 1, entirely by rule-count (as
         * in {@link #TotalRuleCount}).
         */
        Modeled(new ModeledMergeObjectiveFunction()),

        /**
         * Ranks by actual inference accuracy and speed
         */
        Discriminative(new DiscriminativeMergeObjectiveFunction());

        /**
         * Objective function for the chosen objective.
         */
        private final MergeObjectiveFunction objectiveFunction;

        private MergeRanking(final MergeObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
        }

        public final MergeObjectiveFunction objectiveFunction() {
            return objectiveFunction;
        }

        /**
         * Ranks the candidates by merge cost (lowest-cost first)
         * 
         * @return A comparator which ranks {@link MergeCandidate}s by cost, lowest-cost merges first.
         */
        public Comparator<MergeCandidate> comparator() {

            return new Comparator<MergeCandidate>() {
                @Override
                public int compare(final MergeCandidate o1, final MergeCandidate o2) {
                    return Float.compare(objectiveFunction.mergeCost(o1), objectiveFunction.mergeCost(o2));
                }
            };

        }

        static abstract class MergeObjectiveFunction {

            public float mergeCost(final MergeCandidate mergeCandidate) {
                return mergeCandidate.estimatedAccuracyRanking * (1 - EFFICIENCY_LAMBDA)
                        + mergeCandidate.estimatedSpeedRanking * EFFICIENCY_LAMBDA;
            }

            /**
             * Computes and populates {@link MergeCandidate} rankings, using
             * {@link MergeCandidate#setEstimatedAccuracyRanking(int)} and
             * {@link MergeCandidate#setEstimatedSpeedRanking(int)}. Subclasses may implement this with simple heuristic
             * estimates (as per the likelihood-loss estimate of {@link MergeRanking#Likelihood}, or with more
             * sophisticated methods as in {@link MergeRanking#Modeled} or {@link MergeRanking#Discriminative}.
             * 
             * @param mergeCandidates
             * @param substateConditionalProbabilities
             * @param minimumRuleProbability
             */
            public void initMergeCandidates(final List<MergeCandidate> mergeCandidates,
                    final double[][] substateConditionalProbabilities, final float minimumRuleProbability) {
            }

            /**
             * Subclasses should override this method to perform any initialization required before
             * {@link #initMergeCandidates(List, double[][])} (e.g., analyzing the fully-split grammar).
             * 
             * @param grammar
             * @param lexicon
             * @param cycle
             */
            public void initMergeCycle(final Grammar grammar, final Lexicon lexicon, final int cycle) {
            }
        }
    }
}
