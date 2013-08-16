package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;
import edu.ohsu.cslu.lela.TrainGrammar;
import edu.ohsu.cslu.util.IEEEDoubleScaling;

/**
 * Static methods which support merging non-terminals in a {@link Grammar}
 */
public class GrammarMerger {

    protected final static Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

    /**
     * Configuration property key for the weight of estimated likelihood loss when ordering merge candidates. See also
     * {@link #OPT_RULE_COUNT_LAMBDA}.
     */
    public final static String OPT_LIKELIHOOD_LAMBDA = "likelihoodLambda";

    /**
     * Configuration property key for the weight of estimated rule count savings when ordering merge candidates. See
     * also {@link #OPT_LIKELIHOOD_LAMBDA}.
     */
    public final static String OPT_RULE_COUNT_LAMBDA = "ruleCountLambda";

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
            for (int tag = 0; tag < numSubStatesArray.length; tag++) {
                for (int i = 0; i < newNumSubStatesArray[tag]; i++) {
                    for (int j = 0; j < newNumSubStatesArray[tag]; j++) {
                        somethingToMerge |= mergeThesePairs[tag][i][j];
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
    public static double[][][] computeMergeLikelihoodDeltas(final Grammar grammar, final Lexicon lexicon,
            final double[][] substateConditionalProbabilities, final StateSetTreeList trainStateSetTrees) {

        final ArrayParser parser = new ArrayParser(grammar, lexicon);
        final double[][][] deltas = new double[grammar.numSubStates.length][substateConditionalProbabilities[0].length][substateConditionalProbabilities[0].length];

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
     * 
     * @param mergeLikelihoodDeltas Merge likelihood deltas (i.e. log-likelihood gain or loss) for each merge candidate
     *            (substate pair). Indexed by state, substate 1, substate 2. As computed by
     *            {@link GrammarMerger#computeMergeLikelihoodDeltas(Grammar, Lexicon, double[][], StateSetTreeList)}.
     * @return Boolean array indexed by state, substate 1, substate 2; pairs to be merged indicated by <code>true</code>
     *         values.
     */
    public static boolean[][][] selectMergePairs(final double[][][] mergeLikelihoodDeltas,
            final double mergingPercentage, final Grammar grammar) {

        final boolean[][][] mergeThesePairs = new boolean[grammar.numSubStates.length][][];
        final short[] numSubStatesArray = grammar.numSubStates;

        // Log-likelihood gain / loss of merging each candidate pair. Not indexed - used only to select a threshold
        final ArrayList<Double> deltaSiblings = new ArrayList<Double>();

        int nSiblings = 0;

        for (int state = 0; state < numSubStatesArray.length; state++) {

            for (int sub1 = 0; sub1 < numSubStatesArray[state] - 1; sub1++) {

                // Always merge 'down' from substate 2 into substate 1 (sub1 % 2 == 0)
                if (sub1 % 2 == 0 && mergeLikelihoodDeltas[state][sub1][sub1 + 1] != 0) {
                    deltaSiblings.add(mergeLikelihoodDeltas[state][sub1][sub1 + 1]);
                    nSiblings++;
                }
            }
        }
        Collections.sort(deltaSiblings);

        final double threshold = deltaSiblings.get((int) (nSiblings * mergingPercentage));
        BaseLogger.singleton().info("Merge threshold: " + threshold);

        int mergeSiblings = 0;
        for (int state = 0; state < numSubStatesArray.length; state++) {

            mergeThesePairs[state] = new boolean[numSubStatesArray[state]][numSubStatesArray[state]];

            for (int sub1 = 0; sub1 < numSubStatesArray[state] - 1; sub1++) {

                if (sub1 % 2 == 0 && mergeLikelihoodDeltas[state][sub1][sub1 + 1] != 0) {
                    final boolean underThreshold = (mergeLikelihoodDeltas[state][sub1][sub1 + 1] <= threshold);

                    if (underThreshold) {
                        mergeThesePairs[state][sub1][sub1 + 1] = true;
                        mergeSiblings++;
                    }
                }
            }
        }

        // Output merge pairs
        if (BaseLogger.singleton().isLoggable(Level.INFO)) {
            BaseLogger.singleton().info("Merging " + mergeSiblings + " siblings.");

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
     * @param mergeCosts An unordered list of estimated merge costs. Note that this list will be reordered.
     * @param rankingFunction A merge-cost ranking function
     */
    private void rankMergeCosts(final ArrayList<MergeCost> mergeCosts, final MergeRanking rankingFunction) {

        // Estimated likelihood loss
        Collections.sort(mergeCosts, MergeRanking.Likelihood.comparator());
        for (int i = 0; i < mergeCosts.size(); i++) {
            mergeCosts.get(i).likelihoodLossRanking = i;
        }

        // Total rule count delta
        Collections.sort(mergeCosts, MergeRanking.TotalRuleCount.comparator());
        for (int i = 0; i < mergeCosts.size(); i++) {
            mergeCosts.get(i).totalRuleCountRanking = i;
        }

        for (final MergeCost mc : mergeCosts) {
            mc.mergeCost = rankingFunction.objectiveFunction.mergeCost(mc);
        }
    }

    private static class MergeCost {

        private final short state;
        private final short substate;
        private final float estimatedLikelihoodLoss;
        private final int binaryRuleCountDelta;
        private final int unaryRuleCountDelta;
        private final int lexicalRuleCountDelta;

        private final int totalRuleCountDelta;

        /**
         * The ordinal ranking of this non-terminal pair within the list of {@link MergeCost}s if sorted by
         * {@link #estimatedLikelihoodLoss}.
         */
        private int likelihoodLossRanking;

        /**
         * The ordinal ranking of this non-terminal pair within the list of {@link MergeCost}s if sorted by
         * {@link #totalRuleCountDelta}.
         */
        private int totalRuleCountRanking;

        /** Combined merge cost, as assigned by a {@link MergeObjectiveFunction} */
        private float mergeCost;

        /**
         * @param state
         * @param substate
         * @param estimatedLikelihoodLoss Estimated likelihood loss on the training corpus.
         * @param ruleCountDelta Estimated rule-count savings if this pair is merged. 3-tuple of binary, unary, and
         *            lexical counts.
         */
        public MergeCost(final short state, final short substate, final float estimatedLikelihoodLoss,
                final int[] ruleCountDelta) {

            this.state = state;
            this.substate = substate;

            this.estimatedLikelihoodLoss = estimatedLikelihoodLoss;

            this.binaryRuleCountDelta = ruleCountDelta[0];
            this.unaryRuleCountDelta = ruleCountDelta[1];
            this.lexicalRuleCountDelta = ruleCountDelta[2];

            this.totalRuleCountDelta = binaryRuleCountDelta + unaryRuleCountDelta + lexicalRuleCountDelta;
        }

        @Override
        public String toString() {
            return String.format("%d  %d  %14.6f  %6d  %6d  %6d  %6d  %6d  %6d  %7.2f", state, substate,
                    estimatedLikelihoodLoss, likelihoodLossRanking, binaryRuleCountDelta, unaryRuleCountDelta,
                    lexicalRuleCountDelta, totalRuleCountDelta, totalRuleCountRanking, mergeCost);
        }
    }

    /**
     * Objective functions for non-terminal merge ranking. Each enumeration option constructs and exposes an anonymous
     * {@link Comparator} to re-rank merge candidates.
     */
    private static enum MergeRanking {

        Likelihood(new MergeObjectiveFunction() {
            @Override
            public float mergeCost(final MergeCost mergeCost) {
                return mergeCost.estimatedLikelihoodLoss;
            }
        }),

        TotalRuleCount(new MergeObjectiveFunction() {
            @Override
            public float mergeCost(final MergeCost mergeCost) {
                return mergeCost.totalRuleCountDelta;
            }
        }),

        /**
         * Note: Likelihood and rule-count rankings must be computed, as in
         * {@link TrainGrammar#computeMergeCosts(ArrayList)} before applying this function
         */
        Combined(new MergeObjectiveFunction() {

            final float likelihoodLambda = GlobalConfigProperties.singleton()
                    .getFloatProperty(OPT_LIKELIHOOD_LAMBDA, 1);
            final float ruleCountLambda = GlobalConfigProperties.singleton().getFloatProperty(OPT_RULE_COUNT_LAMBDA, 1);

            @Override
            public float mergeCost(final MergeCost mergeCost) {
                return mergeCost.likelihoodLossRanking * likelihoodLambda + mergeCost.totalRuleCountRanking
                        * ruleCountLambda;
            }
        });

        /** Comparator for the specified objective function */
        public final MergeObjectiveFunction objectiveFunction;

        private MergeRanking(final MergeObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
        }

        public Comparator<MergeCost> comparator() {
            return new Comparator<MergeCost>() {
                @Override
                public int compare(final MergeCost o1, final MergeCost o2) {
                    return Float.compare(objectiveFunction.mergeCost(o1), objectiveFunction.mergeCost(o2));
                }
            };
        }

        private static abstract class MergeObjectiveFunction {
            public abstract float mergeCost(MergeCost mergeCost);
        }

    }
}
