package edu.berkeley.nlp.PCFGLA;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.berkeley.nlp.PCFGLA.smoothing.Smoother;
import edu.berkeley.nlp.math.SloppyMath;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.CounterMap;
import edu.berkeley.nlp.util.Numberer;
import edu.berkeley.nlp.util.ScalingTools;

/**
 * Simple implementation of a PCFG grammar, offering the ability to look up rules by their child symbols. Rule
 * probability estimates are just relative frequency estimates off of training trees.
 */
public class Grammar implements java.io.Serializable {

    /**
     * @author leon
     * 
     */
    public static enum RandomInitializationType {
        INITIALIZE_WITH_SMALL_RANDOMIZATION, INITIALIZE_LIKE_MMT
        // initialize like in the Matzuyaki, Miyao, and Tsujii paper
    }

    public static class RuleNotFoundException extends Exception {
        private static final long serialVersionUID = 2L;
    }

    public boolean[] isGrammarTag;

    private List<BinaryRule>[] binaryRulesWithParent;
    private List<BinaryRule>[] binaryRulesWithLC;
    private List<BinaryRule>[] binaryRulesWithRC;
    private BinaryRule[][] splitRulesWithLC;
    private BinaryRule[][] splitRulesWithRC;
    private BinaryRule[][] splitRulesWithP;
    private List<UnaryRule>[] unaryRulesWithParent;
    private List<UnaryRule>[] unaryRulesWithC;

    /** the number of states */
    public short numStates;

    /** the number of substates per state */
    public short[] numSubStates;

    private Map<BinaryRule, BinaryRule> binaryRuleMap;
    private Map<UnaryRule, UnaryRule> unaryRuleMap;
    private UnaryRule uSearchRule;

    private UnaryCounterTable unaryRuleCounter = null;

    private BinaryCounterTable binaryRuleCounter = null;

    // TODO Replace with a FastUtil implementation
    private CounterMap<Integer, Integer> symbolCounter = new CounterMap<Integer, Integer>();

    private static final long serialVersionUID = 1L;

    protected Numberer tagNumberer;

    private List<UnaryRule>[] closedSumRulesWithParent = null;
    private List<UnaryRule>[] closedSumRulesWithChild = null;

    private List<UnaryRule>[] closedViterbiRulesWithParent = null;
    private List<UnaryRule>[] closedViterbiRulesWithChild = null;

    private UnaryRule[][] closedSumRulesWithP = null;
    private UnaryRule[][] closedSumRulesWithC = null;

    private UnaryRule[][] closedViterbiRulesWithP = null;
    public UnaryRule[][] closedViterbiRulesWithC = null;

    private Map<UnaryRule, UnaryRule> bestViterbiRulesUnderMax = null;
    public double threshold;

    public Smoother smoother = null;

    /**
     * A policy giving what state to go to next, starting from a given state, going to a given state. This array is
     * indexed by the start state, the end state, the start substate, and the end substate.
     */
    private int[][] closedViterbiPaths = null;
    private int[][] closedSumPaths = null;

    public boolean findClosedPaths;

    public Tree<Short>[] splitTrees;

    public void clearUnaryIntermediates() {
        ArrayUtil.fill(closedSumPaths, 0);
        ArrayUtil.fill(closedViterbiPaths, 0);
    }

    public void addBinary(final BinaryRule br) {
        // System.out.println("BG adding rule " + br);
        binaryRulesWithParent[br.parentState].add(br);
        binaryRulesWithLC[br.leftChildState].add(br);
        binaryRulesWithRC[br.rightChildState].add(br);
        // allRules.add(br);
        binaryRuleMap.put(br, br);
    }

    public void addUnary(final UnaryRule ur) {
        // System.out.println(" UG adding rule " + ur);
        // closeRulesUnderMax(ur);
        if (!unaryRulesWithParent[ur.parentState].contains(ur)) {
            unaryRulesWithParent[ur.parentState].add(ur);
            unaryRulesWithC[ur.childState].add(ur);
            // allRules.add(ur);
            unaryRuleMap.put(ur, ur);
        }
    }

    public Numberer getTagNumberer() {
        return tagNumberer;
    }

    @SuppressWarnings("unchecked")
    public List<UnaryRule> getUnaryRulesByParent(final int state) {
        if (state >= unaryRulesWithParent.length) {
            return Collections.EMPTY_LIST;
        }
        return unaryRulesWithParent[state];
    }

    public void writeData(final Writer w) {
        final PrintWriter out = new PrintWriter(w);
        for (int state = 0; state < numStates; state++) {
            final BinaryRule[] parentRules = this.splitRulesWithP(state);
            for (int i = 0; i < parentRules.length; i++) {
                final BinaryRule r = parentRules[i];
                out.print(r.toString());
            }
        }
        for (int state = 0; state < numStates; state++) {
            final UnaryRule[] unaries = this.getClosedViterbiUnaryRulesByParent(state);
            for (int r = 0; r < unaries.length; r++) {
                final UnaryRule ur = unaries[r];
                out.print(ur.toString());
            }
        }
        out.flush();
    }

    @Override
    public String toString() {
        return toString(-1);
    }

    public String toString(final int nLex) {

        final StringBuilder sb = new StringBuilder();

        // Count the total number of rules
        int nBinary = 0, nUnary = 0;

        for (int state = 0; state < numStates; state++) {
            nBinary += splitRulesWithP(state).length;
            nUnary += getClosedSumUnaryRulesByParent(state).length;
        }
        sb.append(String
                .format("lang=%s format=Berkeley unkThresh=%s start=%s hMarkov=%s vMarkov=%s date=%s vocabSize=%d nBinary=%d nUnary=%d nLex=%d\n",
                        "UNK", "UNK", "ROOT_0", "UNK", "UNK", new SimpleDateFormat("yyyy/mm/dd").format(new Date()),
                        numStates, nBinary, nUnary, nLex));

        final List<String> ruleStrings = new ArrayList<String>();

        for (int state = 0; state < numStates; state++) {
            final BinaryRule[] parentRules = this.splitRulesWithP(state);
            for (int i = 0; i < parentRules.length; i++) {
                final BinaryRule r = parentRules[i];
                ruleStrings.add(r.toString());
            }
        }

        for (int state = 0; state < numStates; state++) {
            final UnaryRule[] unaries = this.getClosedSumUnaryRulesByParent(state);

            for (int r = 0; r < unaries.length; r++) {
                final UnaryRule ur = unaries[r];
                ruleStrings.add(ur.toString());
            }
        }
        Collections.sort(ruleStrings);
        for (final String ruleString : ruleStrings) {
            sb.append(ruleString);
        }
        return sb.toString();
    }

    public int getNumberOfRules() {
        int nRules = 0;
        for (int state = 0; state < numStates; state++) {
            final BinaryRule[] parentRules = this.splitRulesWithP(state);
            for (int i = 0; i < parentRules.length; i++) {
                final BinaryRule bRule = parentRules[i];
                final double[][][] scores = bRule.getScores2();
                for (int j = 0; j < scores.length; j++) {
                    for (int k = 0; k < scores[j].length; k++) {
                        if (scores[j][k] != null) {
                            nRules += scores[j][k].length;
                        }
                    }
                }
            }
            final UnaryRule[] unaries = this.getClosedSumUnaryRulesByParent(state);
            for (int r = 0; r < unaries.length; r++) {
                final UnaryRule uRule = unaries[r];
                // List<UnaryRule> unaries = this.getUnaryRulesByParent(state);
                // for (UnaryRule uRule : unaries){
                if (uRule.childState == uRule.parentState)
                    continue;
                final double[][] scores = uRule.getScores2();
                for (int j = 0; j < scores.length; j++) {
                    if (scores[j] != null) {
                        nRules += scores[j].length;
                    }
                }
            }
        }
        return nRules;
    }

    public void printUnaryRules() {
        // System.out.println("BY PARENT");
        for (int state1 = 0; state1 < numStates; state1++) {
            final List<UnaryRule> unaries = this.getUnaryRulesByParent(state1);
            for (final UnaryRule uRule : unaries) {
                final UnaryRule uRule2 = unaryRuleMap.get(uRule);
                if (!uRule.getScores2().equals(uRule2.getScores2()))
                    System.out.print("BY PARENT:\n" + uRule + "" + uRule2 + "\n");
            }
        }
        // System.out.println("VITERBI CLOSED");
        for (int state1 = 0; state1 < numStates; state1++) {
            final UnaryRule[] unaries = this.getClosedViterbiUnaryRulesByParent(state1);
            for (int r = 0; r < unaries.length; r++) {
                final UnaryRule uRule = unaries[r];
                // System.out.print(uRule);
                final UnaryRule uRule2 = unaryRuleMap.get(uRule);
                if (unariesAreNotEqual(uRule, uRule2))
                    System.out.print("VITERBI CLOSED:\n" + uRule + "" + uRule2 + "\n");
            }
        }

        for (int state1 = 0; state1 < numStates; state1++) {
            final BinaryRule[] parentRules = this.splitRulesWithP(state1);
            for (int i = 0; i < parentRules.length; i++) {
                final BinaryRule bRule = parentRules[i];
                final BinaryRule bRule2 = binaryRuleMap.get(bRule);
                if (!bRule.getScores2().equals(bRule2.getScores2()))
                    System.out.print("BINARY: " + bRule + "" + bRule2 + "\n");
            }
        }
    }

    public boolean unariesAreNotEqual(final UnaryRule u1, final UnaryRule u2) {
        // two cases:
        // 1. u2 is null and u1 is a selfRule
        if (u2 == null) {
            return false;
        }

        final double[][] s1 = u1.getScores2();
        final double[][] s2 = u2.getScores2();
        for (int i = 0; i < s1.length; i++) {
            if (s1[i] == null || s2[i] == null)
                continue;
            for (int j = 0; j < s1[i].length; j++) {
                if (s1[i][j] != s2[i][j])
                    return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void init() {
        binaryRuleMap = new HashMap<BinaryRule, BinaryRule>();
        unaryRuleMap = new HashMap<UnaryRule, UnaryRule>();
        // allRules = new ArrayList<Rule>();
        bestViterbiRulesUnderMax = new HashMap<UnaryRule, UnaryRule>();
        binaryRulesWithParent = new List[numStates];
        binaryRulesWithLC = new List[numStates];
        binaryRulesWithRC = new List[numStates];
        unaryRulesWithParent = new List[numStates];
        unaryRulesWithC = new List[numStates];
        closedSumRulesWithParent = new List[numStates];
        closedSumRulesWithChild = new List[numStates];
        closedViterbiRulesWithParent = new List[numStates];
        closedViterbiRulesWithChild = new List[numStates];
        isGrammarTag = new boolean[numStates];

        // if (findClosedPaths) {
        closedViterbiPaths = new int[numStates][numStates];
        // }
        closedSumPaths = new int[numStates][numStates];

        for (short s = 0; s < numStates; s++) {
            binaryRulesWithParent[s] = new ArrayList<BinaryRule>();
            binaryRulesWithLC[s] = new ArrayList<BinaryRule>();
            binaryRulesWithRC[s] = new ArrayList<BinaryRule>();
            unaryRulesWithParent[s] = new ArrayList<UnaryRule>();
            unaryRulesWithC[s] = new ArrayList<UnaryRule>();
            closedSumRulesWithParent[s] = new ArrayList<UnaryRule>();
            closedSumRulesWithChild[s] = new ArrayList<UnaryRule>();
            closedViterbiRulesWithParent[s] = new ArrayList<UnaryRule>();
            closedViterbiRulesWithChild[s] = new ArrayList<UnaryRule>();

            final double[][] scores = new double[numSubStates[s]][numSubStates[s]];
            for (int i = 0; i < scores.length; i++) {
                scores[i][i] = 1;
            }
            final UnaryRule selfR = new UnaryRule(s, s, scores);
            // relaxSumRule(selfR);
            relaxViterbiRule(selfR);
        }
    }

    /**
     * Rather than calling some all-in-one constructor that takes a list of trees as training data, you call Grammar()
     * to create an empty grammar, call tallyTree() repeatedly to include all the training data, then call optimize() to
     * take it into account.
     * 
     * @param oldGrammar This is the previous grammar. We use this to copy the split trees that record how each state is
     *            split recursively. These parameters are intialized if oldGrammar is null.
     */
    @SuppressWarnings("unchecked")
    public Grammar(final short[] nSubStates, final boolean findClosedPaths, final Smoother smoother,
            final Grammar oldGrammar, final double thresh) {
        this.tagNumberer = Numberer.getGlobalNumberer("tags");
        this.findClosedPaths = findClosedPaths;
        this.smoother = smoother;
        this.threshold = thresh;
        unaryRuleCounter = new UnaryCounterTable(nSubStates);
        binaryRuleCounter = new BinaryCounterTable(nSubStates);
        symbolCounter = new CounterMap<Integer, Integer>();
        numStates = (short) nSubStates.length;
        numSubStates = nSubStates;
        uSearchRule = new UnaryRule((short) 0, (short) 0);
        if (oldGrammar != null) {
            splitTrees = oldGrammar.splitTrees;
        } else {
            splitTrees = new Tree[numStates];
            boolean hasAnySplits = false;
            for (int tag = 0; !hasAnySplits && tag < numStates; tag++) {
                hasAnySplits = hasAnySplits || numSubStates[tag] > 1;
            }
            for (int tag = 0; tag < numStates; tag++) {
                final ArrayList<Tree<Short>> children = new ArrayList<Tree<Short>>(numSubStates[tag]);
                if (hasAnySplits) {
                    for (short substate = 0; substate < numSubStates[tag]; substate++) {
                        children.add(substate, new Tree<Short>(substate));
                    }
                }
                splitTrees[tag] = new Tree<Short>((short) 0, children);
            }
        }
        init();
    }

    public void setSmoother(final Smoother smoother) {
        this.smoother = smoother;
    }

    public static double generateMMTRandomNumber(final Random r) {
        double f = r.nextDouble();
        f = f * 2 - 1;
        f = f * Math.log(3);
        return Math.exp(f);
    }

    public void optimize(final double randomness) {
        // System.out.print("Optimizing Grammar...");
        init();
        // checkNumberOfSubstates();
        if (randomness > 0.0) {
            final Random random = GrammarTrainer.RANDOM;
            // switch (randomInitializationType ) {
            // case INITIALIZE_WITH_SMALL_RANDOMIZATION:
            // add randomness
            for (final UnaryRule unaryRule : unaryRuleCounter.keySet()) {
                final double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
                for (int i = 0; i < unaryCounts.length; i++) {
                    if (unaryCounts[i] == null)
                        unaryCounts[i] = new double[numSubStates[unaryRule.getParentState()]];
                    for (int j = 0; j < unaryCounts[i].length; j++) {
                        final double r = random.nextDouble() * randomness;
                        unaryCounts[i][j] += r;
                    }
                }
                unaryRuleCounter.setCount(unaryRule, unaryCounts);
            }
            for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
                final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
                for (int i = 0; i < binaryCounts.length; i++) {
                    for (int j = 0; j < binaryCounts[i].length; j++) {
                        if (binaryCounts[i][j] == null)
                            binaryCounts[i][j] = new double[numSubStates[binaryRule.getParentState()]];
                        for (int k = 0; k < binaryCounts[i][j].length; k++) {
                            final double r = random.nextDouble() * randomness;
                            binaryCounts[i][j][k] += r;
                        }
                    }
                }
                binaryRuleCounter.setCount(binaryRule, binaryCounts);
            }
            // break;
            // case INITIALIZE_LIKE_MMT:
            // //multiply by a random factor
            // for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
            // double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
            // for (int i = 0; i < unaryCounts.length; i++) {
            // if (unaryCounts[i]==null)
            // continue;
            // for (int j = 0; j < unaryCounts[i].length; j++) {
            // double r = generateMMTRandomNumber(random);
            // unaryCounts[i][j] *= r;
            // }
            // }
            // unaryRuleCounter.setCount(unaryRule, unaryCounts);
            // }
            // for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
            // double[][][] binaryCounts =
            // binaryRuleCounter.getCount(binaryRule);
            // for (int i = 0; i < binaryCounts.length; i++) {
            // for (int j = 0; j < binaryCounts[i].length; j++) {
            // if (binaryCounts[i][j]==null)
            // continue;
            // for (int k = 0; k < binaryCounts[i][j].length; k++) {
            // double r = generateMMTRandomNumber(random);
            // binaryCounts[i][j][k] *= r;
            // }
            // }
            // }
            // binaryRuleCounter.setCount(binaryRule, binaryCounts);
            // }
            // break;
            // }
        }

        // smooth
        // if (useEntropicPrior) {
        // System.out.println("\nGrammar uses entropic prior!");
        // normalizeWithEntropicPrior();
        // }
        normalize();
        smooth(false); // this also adds the rules to the proper arrays
        // System.out.println("done.");
    }

    public void removeUnlikelyRules(final double thresh, final double power) {
        // System.out.print("Removing everything below "+thresh+" and rasiing rules to the "
        // +power+"th power... ");
        for (int state = 0; state < numStates; state++) {
            for (int r = 0; r < splitRulesWithP[state].length; r++) {
                final BinaryRule rule = splitRulesWithP[state][r];
                for (int lC = 0; lC < rule.scores.length; lC++) {
                    for (int rC = 0; rC < rule.scores[lC].length; rC++) {
                        if (rule.scores[lC][rC] == null)
                            continue;
                        boolean isNull = true;
                        for (int p = 0; p < rule.scores[lC][rC].length; p++) {
                            if (rule.scores[lC][rC][p] < thresh) {
                                // System.out.print(".");
                                rule.scores[lC][rC][p] = 0;
                            } else {
                                if (power != 1)
                                    rule.scores[lC][rC][p] = Math.pow(rule.scores[lC][rC][p], power);
                                isNull = false;
                            }
                        }
                        if (isNull)
                            rule.scores[lC][rC] = null;
                    }
                }
                splitRulesWithP[state][r] = rule;
            }
            for (final UnaryRule rule : unaryRulesWithParent[state]) {
                for (int c = 0; c < rule.scores.length; c++) {
                    if (rule.scores[c] == null)
                        continue;
                    boolean isNull = true;
                    for (int p = 0; p < rule.scores[c].length; p++) {
                        if (rule.scores[c][p] <= thresh) {
                            rule.scores[c][p] = 0;
                        } else {
                            if (power != 1)
                                rule.scores[c][p] = Math.pow(rule.scores[c][p], power);
                            isNull = false;
                        }
                    }
                    if (isNull)
                        rule.scores[c] = null;
                }
            }
        }
        // System.out.print("done.\nRemoved "+removed+" out of "+total+" rules.\n");
    }

    public void smooth(final boolean noNormalize) {
        smoother.smooth(unaryRuleCounter, binaryRuleCounter);
        if (!noNormalize)
            normalize();

        // if (threshold>0){
        // removeUnlikelyRules(threshold);
        // normalize();
        // }

        // compress and add the rules
        for (final UnaryRule unaryRule : unaryRuleCounter.keySet()) {
            final double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
            for (int i = 0; i < unaryCounts.length; i++) {
                if (unaryCounts[i] == null)
                    continue;
                /**
                 * allZero records if all probabilities are 0. If so, we want to null out the matrix element.
                 */
                double allZero = 0;
                int j = 0;
                while (allZero == 0 && j < unaryCounts[i].length) {
                    allZero += unaryCounts[i][j++];
                }
                if (allZero == 0) {
                    unaryCounts[i] = null;
                }
            }
            unaryRule.setScores2(unaryCounts);
            addUnary(unaryRule);
        }
        computePairsOfUnaries();
        for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
            final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
            for (int i = 0; i < binaryCounts.length; i++) {
                for (int j = 0; j < binaryCounts[i].length; j++) {
                    if (binaryCounts[i][j] == null)
                        continue;
                    /**
                     * allZero records if all probabilities are 0. If so, we want to null out the matrix element.
                     */
                    double allZero = 0;
                    int k = 0;
                    while (allZero == 0 && k < binaryCounts[i][j].length) {
                        allZero += binaryCounts[i][j][k++];
                    }
                    if (allZero == 0) {
                        binaryCounts[i][j] = null;
                    }
                }
            }
            binaryRule.setScores2(binaryCounts);
            addBinary(binaryRule);
        }
        // Reset all counters:
        unaryRuleCounter = new UnaryCounterTable(numSubStates);
        binaryRuleCounter = new BinaryCounterTable(numSubStates);
        symbolCounter = new CounterMap<Integer, Integer>();
        /*
         * // tally usage of closed unary rule paths if (findClosedPaths) { int maxSize = numStates * numStates; int
         * size = 0; for (int i=0; i<numStates; i++) { for (int j=0; j<numStates; j++) { if
         * (closedViterbiPaths[i][j]!=null) size++; } }
         * System.out.println("Closed viterbi unary path data structure covers " + size + " / " + maxSize + " = " +
         * (((double) size) / maxSize) + " state pairs"); }
         */
        // checkNumberOfSubstates();

        // Romain: added the computation for the sum-product closure
        // TODO: fix the code and add this back in
        // sumProductClosedUnaryRulesWithParent =
        // sumProductUnaryClosure(unaryRulesWithParent);

    }

    public void clearCounts() {
        unaryRuleCounter = new UnaryCounterTable(numSubStates);
        binaryRuleCounter = new BinaryCounterTable(numSubStates);
        symbolCounter = new CounterMap<Integer, Integer>();

    }

    /**
     * Normalize the unary & binary probabilities so that they sum to 1 for each parent. The binaryRuleCounter and
     * unaryRuleCounter are assumed to contain probabilities, NOT log probabilities!
     */
    public void normalize() {
        // tally the parent counts
        tallyParentCounts();
        // turn the rule scores into fractions
        for (final UnaryRule unaryRule : unaryRuleCounter.keySet()) {
            final double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
            final int parentState = unaryRule.getParentState();
            final int nParentSubStates = numSubStates[parentState];
            final int nChildStates = numSubStates[unaryRule.childState];
            final double[] parentCount = new double[nParentSubStates];
            for (int i = 0; i < nParentSubStates; i++) {
                parentCount[i] = symbolCounter.getCount(parentState, i);
            }
            boolean allZero = true;
            for (int j = 0; j < nChildStates; j++) {
                if (unaryCounts[j] == null)
                    continue;
                for (int i = 0; i < nParentSubStates; i++) {
                    if (parentCount[i] != 0) {
                        double nVal = (unaryCounts[j][i] / parentCount[i]);
                        if (nVal < threshold || SloppyMath.isVeryDangerous(nVal))
                            nVal = 0;
                        unaryCounts[j][i] = nVal;
                    }
                    allZero = allZero && (unaryCounts[j][i] == 0);
                }
            }
            if (allZero) {
                throw new IllegalArgumentException("Maybe an underflow? Rule: " + unaryRule + "\n");
            }
            unaryRuleCounter.setCount(unaryRule, unaryCounts);
        }
        for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
            final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
            final int parentState = binaryRule.parentState;
            final int nParentSubStates = numSubStates[parentState];
            final double[] parentCount = new double[nParentSubStates];
            for (int i = 0; i < nParentSubStates; i++) {
                parentCount[i] = symbolCounter.getCount(parentState, i);
            }
            for (int j = 0; j < binaryCounts.length; j++) {
                for (int k = 0; k < binaryCounts[j].length; k++) {
                    if (binaryCounts[j][k] == null)
                        continue;
                    for (int i = 0; i < nParentSubStates; i++) {
                        if (parentCount[i] != 0) {
                            double nVal = (binaryCounts[j][k][i] / parentCount[i]);
                            if (nVal < threshold || SloppyMath.isVeryDangerous(nVal))
                                nVal = 0;
                            binaryCounts[j][k][i] = nVal;
                        }
                    }
                }
            }
            binaryRuleCounter.setCount(binaryRule, binaryCounts);
        }
    }

    /*
     * Check number of substates
     */
    public void checkNumberOfSubstates() {
        for (final UnaryRule unaryRule : unaryRuleCounter.keySet()) {
            final double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
            final int nParentSubStates = numSubStates[unaryRule.parentState];
            final int nChildSubStates = numSubStates[unaryRule.childState];
            if (unaryCounts.length != nChildSubStates) {
                System.out.println("Unary Rule " + unaryRule + " should have " + nChildSubStates + " childsubstates.");
            }
            if (unaryCounts[0] != null && unaryCounts[0].length != nParentSubStates) {
                System.out
                        .println("Unary Rule " + unaryRule + " should have " + nParentSubStates + " parentsubstates.");
            }
        }
        for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
            final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
            final int nParentSubStates = numSubStates[binaryRule.parentState];
            final int nLeftChildSubStates = numSubStates[binaryRule.leftChildState];
            final int nRightChildSubStates = numSubStates[binaryRule.rightChildState];
            if (binaryCounts.length != nLeftChildSubStates) {
                System.out.println("Unary Rule " + binaryRule + " should have " + nLeftChildSubStates
                        + " left childsubstates.");
            }
            if (binaryCounts[0].length != nRightChildSubStates) {
                System.out.println("Unary Rule " + binaryRule + " should have " + nRightChildSubStates
                        + " right childsubstates.");
            }
            if (binaryCounts[0][0] != null && binaryCounts[0][0].length != nParentSubStates) {
                System.out.println("Unary Rule " + binaryRule + " should have " + nParentSubStates
                        + " parentsubstates.");
            }
        }
        System.out.println("Done with checks.");
    }

    /**
     * Sum the parent symbol counter, symbolCounter. This is needed when the rule counters are altered, such as when
     * adding randomness in optimize().
     * <p>
     * This assumes that the unaryRuleCounter and binaryRuleCounter contain probabilities, NOT log probabilities!
     */
    private void tallyParentCounts() {
        symbolCounter = new CounterMap<Integer, Integer>();
        for (final UnaryRule unaryRule : unaryRuleCounter.keySet()) {
            final double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
            final int parentState = unaryRule.getParentState();
            isGrammarTag[parentState] = true;
            if (unaryRule.childState == parentState)
                continue;
            final int nParentSubStates = numSubStates[parentState];
            final double[] sum = new double[nParentSubStates];
            for (int j = 0; j < unaryCounts.length; j++) {
                if (unaryCounts[j] == null)
                    continue;
                for (int i = 0; i < nParentSubStates; i++) {
                    final double val = unaryCounts[j][i];
                    // if (val>=threshold)
                    sum[i] += val;
                }
            }
            for (int i = 0; i < nParentSubStates; i++) {
                symbolCounter.incrementCount(parentState, i, sum[i]);
            }

        }
        for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
            final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
            final int parentState = binaryRule.parentState;
            isGrammarTag[parentState] = true;
            final int nParentSubStates = numSubStates[parentState];
            final double[] sum = new double[nParentSubStates];
            for (int j = 0; j < binaryCounts.length; j++) {
                for (int k = 0; k < binaryCounts[j].length; k++) {
                    if (binaryCounts[j][k] == null)
                        continue;
                    for (int i = 0; i < nParentSubStates; i++) {
                        final double val = binaryCounts[j][k][i];
                        // if (val>=threshold)
                        sum[i] += val;
                    }
                }
            }
            for (int i = 0; i < nParentSubStates; i++) {
                symbolCounter.incrementCount(parentState, i, sum[i]);
            }
        }
    }

    public void tallyStateSetTree(final Tree<StateSet> tree, final Grammar old_grammar) {
        // Check that the top node is not split (it has only one substate)
        if (tree.isLeaf())
            return;
        if (tree.isPreTerminal())
            return;
        final StateSet node = tree.label();
        if (node.numSubStates() != 1) {
            System.err.println("The top symbol is split!");
            System.out.println(tree);
            System.exit(1);
        }
        // The inside score of its only substate is the (log) probability of the
        // tree
        final double tree_score = node.getIScore(0);
        final int tree_scale = node.getIScale();
        if (tree_score == 0) {
            System.out.println("Something is wrong with this tree. I will skip it.");
            return;
        }
        tallyStateSetTree(tree, tree_score, tree_scale, old_grammar);
    }

    public void tallyStateSetTree(final Tree<StateSet> tree, double tree_score, final double tree_scale,
            final Grammar old_grammar) {

        if (tree.isLeaf() || tree.isPreTerminal()) {
            return;
        }

        final List<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short parentState = parent.getState();
        final int nParentSubStates = numSubStates[parentState];
        switch (children.size()) {
        case 0:
            // This is a preterminal node; nothing to do
            break;

        case 1: // Unary
            final StateSet child = children.get(0).label();
            final short childState = child.getState();
            final int nChildSubStates = numSubStates[childState];
            final UnaryRule urule = new UnaryRule(parentState, childState);
            final double[][] oldUScores = old_grammar.getUnaryScore(urule); // rule
            // score
            double[][] ucounts = unaryRuleCounter.getCount(urule);
            if (ucounts == null)
                ucounts = new double[nChildSubStates][];
            double scalingFactor = ScalingTools.calcScaleFactor(parent.getOScale() + child.getIScale() - tree_scale);
            // if (scalingFactor==0){
            // System.out.println("p: "+parent.getOScale()+" c: "+child.getIScale()+" t:"+tree_scale);
            // }
            for (short i = 0; i < nChildSubStates; i++) {
                if (oldUScores[i] == null)
                    continue;
                final double cIS = child.getIScore(i);
                if (cIS == 0)
                    continue;
                if (ucounts[i] == null)
                    ucounts[i] = new double[nParentSubStates];
                for (short j = 0; j < nParentSubStates; j++) {
                    final double pOS = parent.getOScore(j); // Parent outside score
                    if (pOS == 0)
                        continue;
                    final double rS = oldUScores[i][j];
                    if (rS == 0)
                        continue;
                    if (tree_score == 0)
                        tree_score = 1;
                    final double logRuleCount = (rS * cIS / tree_score) * scalingFactor * pOS;
                    ucounts[i][j] += logRuleCount;
                }
            }
            // urule.setScores2(ucounts);
            unaryRuleCounter.setCount(urule, ucounts);
            break;

        case 2: // Binary
            final StateSet leftChild = children.get(0).label();
            final short lChildState = leftChild.getState();
            final StateSet rightChild = children.get(1).label();
            final short rChildState = rightChild.getState();

            final int nLeftChildSubStates = numSubStates[lChildState];
            final int nRightChildSubStates = numSubStates[rChildState];

            final BinaryRule brule = new BinaryRule(parentState, lChildState, rChildState);
            double[][][] oldBScores = old_grammar.getBinaryScore(brule);

            if (oldBScores == null) {
                // The rule was not found in the grammar
                oldBScores = new double[nLeftChildSubStates][nRightChildSubStates][nParentSubStates];
                ArrayUtil.fill(oldBScores, 1.0);
            }
            double[][][] bcounts = binaryRuleCounter.getCount(brule);
            if (bcounts == null) {
                bcounts = new double[nLeftChildSubStates][nRightChildSubStates][];
            }

            scalingFactor = ScalingTools.calcScaleFactor(parent.getOScale() + leftChild.getIScale()
                    + rightChild.getIScale() - tree_scale);

            for (short i = 0; i < nLeftChildSubStates; i++) {
                final double lcIS = leftChild.getIScore(i);
                if (lcIS == 0) {
                    continue;
                }

                for (short j = 0; j < nRightChildSubStates; j++) {
                    if (oldBScores[i][j] == null) {
                        continue;
                    }

                    final double rcIS = rightChild.getIScore(j);
                    if (rcIS == 0) {
                        continue;
                    }

                    // allocate parent array
                    if (bcounts[i][j] == null) {
                        bcounts[i][j] = new double[nParentSubStates];
                    }

                    for (short k = 0; k < nParentSubStates; k++) {
                        final double pOS = parent.getOScore(k); // Parent outside
                        // score
                        if (pOS == 0) {
                            continue;
                        }

                        final double rS = oldBScores[i][j][k];
                        if (rS == 0) {
                            continue;
                        }

                        if (tree_score == 0) {
                            tree_score = 1;
                        }

                        final double scaledRuleCount = (rS * lcIS / tree_score) * rcIS * scalingFactor * pOS;
                        bcounts[i][j][k] += scaledRuleCount;
                    }
                }
            }
            binaryRuleCounter.setCount(brule, bcounts);
            break;

        default:
            throw new IllegalArgumentException("Malformed tree: more than two children");
        }

        for (final Tree<StateSet> child : children) {
            tallyStateSetTree(child, tree_score, tree_scale, old_grammar);
        }
    }

    public void tallyUninitializedStateSetTree(final Tree<StateSet> tree) {
        if (tree.isLeaf())
            return;
        // the lexicon handles preterminal nodes
        if (tree.isPreTerminal())
            return;
        final List<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short parentState = parent.getState();
        final int nParentSubStates = parent.numSubStates(); // numSubStates[parentState];
        switch (children.size()) {
        case 0:
            // This is a leaf (a preterminal node, if we count the words
            // themselves), nothing to do
            break;
        case 1:
            final StateSet child = children.get(0).label();
            final short childState = child.getState();
            final int nChildSubStates = child.numSubStates(); // numSubStates[childState];
            final double[][] counts = new double[nChildSubStates][nParentSubStates];
            final UnaryRule urule = new UnaryRule(parentState, childState, counts);
            unaryRuleCounter.incrementCount(urule, 1.0);
            break;
        case 2:
            final StateSet leftChild = children.get(0).label();
            final short lChildState = leftChild.getState();
            final StateSet rightChild = children.get(1).label();
            final short rChildState = rightChild.getState();
            final int nLeftChildSubStates = leftChild.numSubStates(); // numSubStates[lChildState];
            final int nRightChildSubStates = rightChild.numSubStates();// numSubStates[rChildState];
            final double[][][] bcounts = new double[nLeftChildSubStates][nRightChildSubStates][nParentSubStates];
            final BinaryRule brule = new BinaryRule(parentState, lChildState, rChildState, bcounts);
            binaryRuleCounter.incrementCount(brule, 1.0);
            break;
        default:
            throw new Error("Malformed tree: more than two children");
        }

        for (final Tree<StateSet> child : children) {
            tallyUninitializedStateSetTree(child);
        }
    }

    public void makeCRArrays() {
        // int numStates = closedRulesWithParent.length;
        closedSumRulesWithP = new UnaryRule[numStates][];
        closedSumRulesWithC = new UnaryRule[numStates][];
        closedViterbiRulesWithP = new UnaryRule[numStates][];
        closedViterbiRulesWithC = new UnaryRule[numStates][];

        for (int i = 0; i < numStates; i++) {
            closedSumRulesWithP[i] = closedSumRulesWithParent[i].toArray(new UnaryRule[0]);
            closedSumRulesWithC[i] = closedSumRulesWithChild[i].toArray(new UnaryRule[0]);
            closedViterbiRulesWithP[i] = closedViterbiRulesWithParent[i].toArray(new UnaryRule[0]);
            closedViterbiRulesWithC[i] = closedViterbiRulesWithChild[i].toArray(new UnaryRule[0]);
        }
    }

    public UnaryRule[] getClosedSumUnaryRulesByParent(final int state) {
        if (closedSumRulesWithP == null) {
            makeCRArrays();
        }
        if (state >= closedSumRulesWithP.length) {
            return new UnaryRule[0];
        }
        return closedSumRulesWithP[state];
    }

    public UnaryRule[] getClosedViterbiUnaryRulesByParent(final int state) {
        if (closedViterbiRulesWithP == null) {
            makeCRArrays();
        }
        if (state >= closedViterbiRulesWithP.length) {
            return new UnaryRule[0];
        }
        return closedViterbiRulesWithP[state];
    }

    public List<short[]> getBestViterbiPath(final short pState, final short np, final short cState, final short cp) {
        final ArrayList<short[]> path = new ArrayList<short[]>();
        short[] state = new short[2];
        state[0] = pState;
        state[1] = np;
        // if we haven't built the data structure of closed paths, then
        // return the simplest possible path
        if (!findClosedPaths) {
            path.add(state);
            state = new short[2];
            state[0] = cState;
            state[1] = cp;
            path.add(state);
            return path;
        }

        // read the best paths off of the closedViterbiPaths list
        if (pState == cState && np == cp) {
            path.add(state);
            path.add(state);
            return path;
        }
        while (state[0] != cState || state[1] != cp) {
            path.add(state);
            state[0] = (short) closedViterbiPaths[state[0]][state[1]];
        }
        // add the destination state as well
        path.add(state);
        return path;
    }

    public int getUnaryIntermediate(final short start, final short end) {
        return closedSumPaths[start][end];
    }

    public void computePairsOfUnaries() {
        // closedSumRulesWithParent = closedViterbiRulesWithParent =
        // unaryRulesWithParent;
        for (short parentState = 0; parentState < numStates; parentState++) {
            for (short childState = 0; childState < numStates; childState++) {
                if (parentState == childState)
                    continue;
                final int nParentSubStates = numSubStates[parentState];
                final int nChildSubStates = numSubStates[childState];
                final UnaryRule resultRsum = new UnaryRule(parentState, childState);
                final UnaryRule resultRmax = new UnaryRule(parentState, childState);
                final double[][] scoresSum = new double[nChildSubStates][nParentSubStates];
                final double[][] scoresMax = new double[nChildSubStates][nParentSubStates];
                double maxSumScore = -1;
                short bestSumIntermed = -1;
                short bestMaxIntermed = -2;
                for (int i = 0; i < unaryRulesWithParent[parentState].size(); i++) {
                    final UnaryRule pr = unaryRulesWithParent[parentState].get(i);
                    final short state = pr.getChildState();
                    if (state == childState) {
                        double total = 0;
                        final double[][] scores = pr.getScores2();
                        for (int cp = 0; cp < nChildSubStates; cp++) {
                            if (scores[cp] == null)
                                continue;
                            for (int np = 0; np < nParentSubStates; np++) {
                                // sum over intermediate substates
                                final double sum = scores[cp][np];
                                scoresSum[cp][np] += sum;
                                total += sum;
                                if (sum > scoresMax[cp][np]) {
                                    scoresMax[cp][np] = sum;
                                    bestMaxIntermed = -1;
                                }
                            }
                        }
                        if (total > maxSumScore) {
                            bestSumIntermed = -1;
                            maxSumScore = total;
                        }
                    } else {
                        for (int j = 0; j < unaryRulesWithC[childState].size(); j++) {
                            final UnaryRule cr = unaryRulesWithC[childState].get(j);
                            if (state != cr.getParentState())
                                continue;
                            final int nMySubStates = numSubStates[state];
                            double total = 0;
                            for (int np = 0; np < nParentSubStates; np++) {
                                for (int cp = 0; cp < nChildSubStates; cp++) {
                                    // sum over intermediate substates
                                    double sum = 0;
                                    double max = 0;
                                    for (int unp = 0; unp < nMySubStates; unp++) {
                                        final double val = pr.getScore(np, unp) * cr.getScore(unp, cp);
                                        sum += val;
                                        max = Math.max(max, val);
                                    }
                                    scoresSum[cp][np] += sum;
                                    total += sum;
                                    if (max > scoresMax[cp][np]) {
                                        scoresMax[cp][np] = max;
                                        bestMaxIntermed = state;
                                    }
                                }
                            }
                            if (total > maxSumScore) {
                                maxSumScore = total;
                                bestSumIntermed = state;
                            }
                        }
                    }
                }
                if (maxSumScore > -1) {
                    resultRsum.setScores2(scoresSum);
                    addUnary(resultRsum);
                    closedSumRulesWithParent[parentState].add(resultRsum);
                    closedSumRulesWithChild[childState].add(resultRsum);
                    closedSumPaths[parentState][childState] = bestSumIntermed;
                }
                if (bestMaxIntermed > -2) {
                    resultRmax.setScores2(scoresMax);
                    // addUnary(resultR);
                    closedViterbiRulesWithParent[parentState].add(resultRmax);
                    closedViterbiRulesWithChild[childState].add(resultRmax);
                    closedViterbiPaths[parentState][childState] = bestMaxIntermed;
                    /*
                     * if (bestMaxIntermed > -1){ System.out.println("NEW RULE CREATED"); }
                     */
                }
            }
        }

    }

    /**
     * Initialize the best unary chain probabilities and paths with this rule.
     * 
     * @param rule
     */
    private void relaxViterbiRule(final UnaryRule rule) {
        bestViterbiRulesUnderMax.put(rule, rule);
        closedViterbiRulesWithParent[rule.parentState].add(rule);
        closedViterbiRulesWithChild[rule.childState].add(rule);
        if (findClosedPaths) {
            for (short i = 0; i < rule.scores.length; i++) {
                for (short j = 0; j < rule.scores[i].length; j++) {
                    final short[] pair = new short[2];
                    pair[0] = rule.childState;
                    pair[1] = j;
                    /*
                     * if (closedViterbiPaths[rule.parentState][rule.childState]== null) {
                     * closedViterbiPaths[rule.parentState][rule.childState] = new
                     * short[rule.scores.length][rule.scores[0].length][]; } closedViterbiPaths
                     * [rule.parentState][rule.childState][i][j] = pair;
                     */
                }
            }
        }
    }

    /**
     * Populates the "splitRules" accessor lists using the existing rule lists. If the state is synthetic, these lists
     * contain all rules for the state. If the state is NOT synthetic, these lists contain only the rules in which both
     * children are not synthetic.
     * <p>
     * <i>This method must be called before the grammar is used, either after training or deserializing grammar.</i>
     */
    public void splitRules() {
        // splitRulesWithLC = new BinaryRule[numStates][];
        // splitRulesWithRC = new BinaryRule[numStates][];
        // makeRulesAccessibleByChild();

        if (binaryRulesWithParent == null)
            return;
        splitRulesWithP = new BinaryRule[numStates][];
        splitRulesWithLC = new BinaryRule[numStates][];
        splitRulesWithRC = new BinaryRule[numStates][];

        for (int state = 0; state < numStates; state++) {
            splitRulesWithLC[state] = toBRArray(binaryRulesWithLC[state]);
            splitRulesWithRC[state] = toBRArray(binaryRulesWithRC[state]);
            splitRulesWithP[state] = toBRArray(binaryRulesWithParent[state]);
        }
        // we don't need the original lists anymore
        binaryRulesWithParent = null;
        binaryRulesWithLC = null;
        binaryRulesWithRC = null;
        makeCRArrays();
    }

    public BinaryRule[] splitRulesWithP(final int state) {

        if (splitRulesWithP == null) {
            splitRules();
        }

        if (state >= splitRulesWithP.length) {
            return new BinaryRule[0];
        }

        return splitRulesWithP[state];
    }

    private BinaryRule[] toBRArray(final List<BinaryRule> list) {
        // Collections.sort(list, Rule.scoreComparator()); // didn't seem to
        // help
        final BinaryRule[] array = new BinaryRule[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public double[][] getUnaryScore(final short pState, final short cState) {
        final UnaryRule r = getUnaryRule(pState, cState);
        if (r != null)
            return r.getScores2();
        if (GrammarTrainer.VERBOSE)
            System.out.println("The requested rule (" + uSearchRule + ") is not in the grammar!");
        final double[][] uscores = new double[numSubStates[cState]][numSubStates[pState]];
        ArrayUtil.fill(uscores, 0.0);
        return uscores;
    }

    /**
     * @param pState Parent state
     * @param cState Child state
     * @return The unary rule matching <code>pState</code> and <code>cState</code>
     */
    public UnaryRule getUnaryRule(final short pState, final short cState) {
        final UnaryRule uRule = new UnaryRule(pState, cState);
        final UnaryRule r = unaryRuleMap.get(uRule);
        return r;
    }

    public double[][] getUnaryScore(final UnaryRule rule) {
        final UnaryRule r = unaryRuleMap.get(rule);
        if (r != null)
            return r.getScores2();
        if (GrammarTrainer.VERBOSE)
            System.err.println("The requested rule (" + rule + ") is not in the grammar!");
        final double[][] uscores = new double[numSubStates[rule.getChildState()]][numSubStates[rule.getParentState()]];
        ArrayUtil.fill(uscores, 0.0);
        return uscores;
    }

    public double[][][] getBinaryScore(final short pState, final short lState, final short rState) {
        final BinaryRule r = getBinaryRule(pState, lState, rState);
        if (r != null) {
            return r.getScores2();
        }
        final double[][][] bscores = new double[numSubStates[lState]][numSubStates[rState]][numSubStates[pState]];
        ArrayUtil.fill(bscores, 0.0);
        return bscores;
    }

    /**
     * @param pState Parent
     * @param lState Left child
     * @param rState Right child
     * @return The binary rule matching the specified parent and child states
     */
    public BinaryRule getBinaryRule(final short pState, final short lState, final short rState) {
        final BinaryRule bRule = new BinaryRule(pState, lState, rState);
        final BinaryRule r = binaryRuleMap.get(bRule);
        return r;
    }

    public double[][][] getBinaryScore(final BinaryRule rule) {
        final BinaryRule r = binaryRuleMap.get(rule);
        if (r != null) {
            return r.getScores2();
        }

        if (GrammarTrainer.VERBOSE) {
            System.out.println("The requested rule (" + rule + ") is not in the grammar!");
        }
        final double[][][] bscores = new double[numSubStates[rule.getLeftChildState()]][numSubStates[rule
                .getRightChildState()]][numSubStates[rule.getParentState()]];
        ArrayUtil.fill(bscores, 0.0);
        return bscores;
    }

    /**
     * Split all substates into two new ones. This produces a new Grammar with updated rules.
     * 
     * @param randomness percent randomness applied in splitting rules
     * @return a new grammar, with all states split in 2
     */
    public Grammar splitAllStates(final double randomness, final int[] counts) {
        final short[] newNumSubStates = new short[numSubStates.length];
        for (short i = 0; i < numSubStates.length; i++) {
            // don't split a state into more substates than times it was
            // actaully seen

            // if (!moreSubstatesThanCounts && numSubStates[i]>=counts[i]) {
            // newNumSubStates[i]=numSubStates[i];
            // }
            // else{
            newNumSubStates[i] = (short) (numSubStates[i] * 2);
            // }
        }
        newNumSubStates[0] = 1; // never split ROOT
        // create the new grammar
        final Grammar grammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);
        final Random random = GrammarTrainer.RANDOM;

        for (final BinaryRule oldRule : binaryRuleMap.keySet()) {
            final BinaryRule newRule = oldRule.splitRule(numSubStates, newNumSubStates, random, randomness);
            grammar.addBinary(newRule);
        }

        for (final UnaryRule oldRule : unaryRuleMap.keySet()) {
            final UnaryRule newRule = oldRule.splitRule(numSubStates, newNumSubStates, random, randomness);
            grammar.addUnary(newRule);
        }
        grammar.isGrammarTag = this.isGrammarTag;
        grammar.extendSplitTrees(splitTrees, numSubStates);
        grammar.computePairsOfUnaries();
        return grammar;
    }

    @SuppressWarnings("unchecked")
    public void extendSplitTrees(final Tree<Short>[] trees, final short[] oldNumSubStates) {
        this.splitTrees = new Tree[numStates];
        for (int tag = 0; tag < splitTrees.length; tag++) {
            final Tree<Short> splitTree = trees[tag].shallowClone();
            for (final Tree<Short> leaf : splitTree.leafList()) {
                final List<Tree<Short>> children = leaf.children();
                if (numSubStates[tag] > oldNumSubStates[tag]) {
                    children.add(new Tree<Short>((short) (2 * leaf.label())));
                    children.add(new Tree<Short>((short) (2 * leaf.label() + 1)));
                } else {
                    children.add(new Tree<Short>(leaf.label()));
                }
            }
            this.splitTrees[tag] = splitTree;
        }
    }

    public int totalSubStates() {
        int count = 0;
        for (int i = 0; i < numStates; i++) {
            count += numSubStates[i];
        }
        return count;
    }

    /**
     * Tally the probability of seeing each substate. This data is needed for tallyMergeScores. mergeWeights is indexed
     * as [state][substate]. This data should be normalized before being used by another function.
     * 
     * @param tree
     * @param mergeWeights The probability of seeing substate given state.
     */
    public void tallyMergeWeights(final Tree<StateSet> tree, final double mergeWeights[][]) {
        if (tree.isLeaf())
            return;
        final StateSet label = tree.label();
        final short state = label.getState();
        final double probs[] = new double[label.numSubStates()];
        double total = 0, tmp;
        for (short i = 0; i < label.numSubStates(); i++) {
            tmp = label.getIScore(i) * label.getOScore(i);
            // TODO: put in the scale parameters???
            probs[i] = tmp;
            total += tmp;
        }
        if (total == 0)
            total = 1;
        for (short i = 0; i < label.numSubStates(); i++) {
            mergeWeights[state][i] += probs[i] / total;
        }
        for (final Tree<StateSet> child : tree.children()) {
            tallyMergeWeights(child, mergeWeights);
        }
    }

    /*
     * normalize merge weights. assumes that the mergeWeights are given as logs. the normalized weights are returned as
     * probabilities.
     */
    public void normalizeMergeWeights(final double[][] mergeWeights) {
        for (int state = 0; state < mergeWeights.length; state++) {
            double sum = 0;
            for (int subState = 0; subState < numSubStates[state]; subState++) {
                sum += mergeWeights[state][subState];
            }
            if (sum == 0)
                sum = 1;
            for (int subState = 0; subState < numSubStates[state]; subState++) {
                mergeWeights[state][subState] /= sum;
            }
        }
    }

    /**
     * Calculate the log likelihood gain of merging pairs of split states together. This information is returned in
     * deltas[state][merged substate]. It requires mergeWeights to be calculated by tallyMergeWeights.
     * 
     * @param tree
     * @param deltas The log likelihood gained by merging pairs of substates.
     * @param mergeWeights The probability of seeing substate given state.
     */
    public void tallyMergeScores(final Tree<StateSet> tree, final double[][][] deltas, final double[][] mergeWeights) {
        if (tree.isLeaf())
            return;
        final StateSet label = tree.label();
        final short state = label.getState();
        final double[] separatedScores = new double[label.numSubStates()];
        final double[] combinedScores = new double[label.numSubStates()];
        double combinedScore;
        // calculate separated scores

        double separatedScoreSum = 0, tmp;
        // don't need to deal with scale factor because we divide below
        for (int i = 0; i < label.numSubStates(); i++) {
            tmp = label.getIScore(i) * label.getOScore(i);
            combinedScores[i] = separatedScores[i] = tmp;
            separatedScoreSum += tmp;
        }
        // calculate merged scores
        for (short i = 0; i < numSubStates[state]; i++) {
            for (short j = (short) (i + 1); j < numSubStates[state]; j++) {
                final short[] map = new short[2];
                map[0] = i;
                map[1] = j;
                final double[] tmp1 = new double[2], tmp2 = new double[2];
                double mergeWeightSum = 0;
                for (int k = 0; k < 2; k++) {
                    mergeWeightSum += mergeWeights[state][map[k]];
                }
                if (mergeWeightSum == 0)
                    mergeWeightSum = 1;
                for (int k = 0; k < 2; k++) {
                    tmp1[k] = label.getIScore(map[k]) * mergeWeights[state][map[k]] / mergeWeightSum;
                    tmp2[k] = label.getOScore(map[k]);
                }
                combinedScore = (tmp1[0] + tmp1[1]) * (tmp2[0] + tmp2[1]);
                combinedScores[i] = combinedScore;
                combinedScores[j] = 0;
                if (combinedScore != 0 && separatedScoreSum != 0)
                    deltas[state][i][j] += Math.log(separatedScoreSum / ArrayUtil.sum(combinedScores));
                for (int k = 0; k < 2; k++)
                    combinedScores[map[k]] = separatedScores[map[k]];
                if (Double.isNaN(deltas[state][i][j])) {
                    System.out.println(" deltas[" + tagNumberer.symbol(state) + "][" + i + "][" + j + "] = NaN");
                    System.out.println(Arrays.toString(separatedScores) + " " + Arrays.toString(tmp1) + " "
                            + Arrays.toString(tmp2) + " " + combinedScore + " " + Arrays.toString(mergeWeights[state]));
                }
            }
        }

        for (final Tree<StateSet> child : tree.children()) {
            tallyMergeScores(child, deltas, mergeWeights);
        }
    }

    /**
     * This merges the substate pairs indicated by mergeThesePairs[state][substate pair]. It requires merge weights
     * calculated by tallyMergeWeights.
     * 
     * @param mergeThesePairs Which substate pairs to merge.
     * @param mergeWeights The probability of seeing each substate.
     */
    public Grammar mergeStates(final boolean[][][] mergeThesePairs, final double[][] mergeWeights) {
        final short[] newNumSubStates = new short[numSubStates.length];
        final short[][] mapping = new short[numSubStates.length][];
        // invariant: if partners[state][substate][0] == substate, it's the 1st
        // one
        final short[][][] partners = new short[numSubStates.length][][];
        calculateMergeArrays(mergeThesePairs, newNumSubStates, mapping, partners, numSubStates);
        // create the new grammar
        final Grammar grammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);
        // for (Rule r : allRules) {
        // if (r instanceof BinaryRule) {
        for (final BinaryRule oldRule : binaryRuleMap.keySet()) {
            // BinaryRule oldRule = r;
            final short pS = oldRule.getParentState(), lcS = oldRule.getLeftChildState(), rcS = oldRule
                    .getRightChildState();
            final double[][][] oldScores = oldRule.getScores2();
            // merge binary rule
            final double[][][] newScores = new double[newNumSubStates[lcS]][newNumSubStates[rcS]][newNumSubStates[pS]];
            for (int i = 0; i < numSubStates[pS]; i++) {
                if (partners[pS][i][0] == i) {
                    final int parentSplit = partners[pS][i].length;
                    for (int j = 0; j < numSubStates[lcS]; j++) {
                        if (partners[lcS][j][0] == j) {
                            final int leftSplit = partners[lcS][j].length;
                            for (int k = 0; k < (numSubStates[rcS]); k++) {
                                if (partners[rcS][k][0] == k) {
                                    final int rightSplit = partners[rcS][k].length;
                                    final double[][][] scores = new double[leftSplit][rightSplit][parentSplit];
                                    for (int js = 0; js < leftSplit; js++) {
                                        for (int ks = 0; ks < rightSplit; ks++) {
                                            if (oldScores[partners[lcS][j][js]][partners[rcS][k][ks]] == null)
                                                continue;
                                            for (int is = 0; is < parentSplit; is++) {
                                                scores[js][ks][is] = oldScores[partners[lcS][j][js]][partners[rcS][k][ks]][partners[pS][i][is]];
                                            }
                                        }
                                    }
                                    if (rightSplit == 2) {
                                        for (int is = 0; is < parentSplit; is++) {
                                            for (int js = 0; js < leftSplit; js++) {
                                                scores[js][0][is] = scores[js][1][is] = scores[js][0][is]
                                                        + scores[js][1][is];
                                            }
                                        }
                                    }
                                    if (leftSplit == 2) {
                                        for (int is = 0; is < parentSplit; is++) {
                                            for (int ks = 0; ks < rightSplit; ks++) {
                                                scores[0][ks][is] = scores[1][ks][is] = scores[0][ks][is]
                                                        + scores[1][ks][is];
                                            }
                                        }
                                    }
                                    if (parentSplit == 2) {
                                        for (int js = 0; js < leftSplit; js++) {
                                            for (int ks = 0; ks < rightSplit; ks++) {
                                                double mergeWeightSum = mergeWeights[pS][partners[pS][i][0]]
                                                        + mergeWeights[pS][partners[pS][i][1]];
                                                if (SloppyMath.isDangerous(mergeWeightSum))
                                                    mergeWeightSum = 1;
                                                scores[js][ks][0] = scores[js][ks][1] = ((scores[js][ks][0] * mergeWeights[pS][partners[pS][i][0]]) + (scores[js][ks][1] * mergeWeights[pS][partners[pS][i][1]]))
                                                        / mergeWeightSum;
                                            }
                                        }
                                    }
                                    for (int is = 0; is < parentSplit; is++) {
                                        for (int js = 0; js < leftSplit; js++) {
                                            for (int ks = 0; ks < rightSplit; ks++) {
                                                newScores[mapping[lcS][partners[lcS][j][js]]][mapping[rcS][partners[rcS][k][ks]]][mapping[pS][partners[pS][i][is]]] = scores[js][ks][is];
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            final BinaryRule newRule = new BinaryRule(oldRule);
            newRule.setScores2(newScores);
            grammar.addBinary(newRule);
        }
        // } else if (r instanceof UnaryRule) {
        for (final UnaryRule oldRule : unaryRuleMap.keySet()) {
            // UnaryRule oldRule = (UnaryRule) r;
            final short pS = oldRule.getParentState(), cS = oldRule.getChildState();
            // merge unary rule
            final double[][] newScores = new double[newNumSubStates[cS]][newNumSubStates[pS]];
            final double[][] oldScores = oldRule.getScores2();
            boolean allZero = true;
            for (int i = 0; i < numSubStates[pS]; i++) {
                if (partners[pS][i][0] == i) {
                    final int parentSplit = partners[pS][i].length;
                    for (int j = 0; j < numSubStates[cS]; j++) {
                        if (partners[cS][j][0] == j) {
                            final int childSplit = partners[cS][j].length;
                            final double[][] scores = new double[childSplit][parentSplit];
                            for (int js = 0; js < childSplit; js++) {
                                if (oldScores[partners[cS][j][js]] == null)
                                    continue;
                                for (int is = 0; is < parentSplit; is++) {
                                    scores[js][is] = oldScores[partners[cS][j][js]][partners[pS][i][is]];
                                }
                            }
                            if (childSplit == 2) {
                                for (int is = 0; is < parentSplit; is++) {
                                    scores[0][is] = scores[1][is] = scores[0][is] + scores[1][is];
                                }
                            }
                            if (parentSplit == 2) {
                                for (int js = 0; js < childSplit; js++) {
                                    double mergeWeightSum = mergeWeights[pS][partners[pS][i][0]]
                                            + mergeWeights[pS][partners[pS][i][1]];
                                    if (SloppyMath.isDangerous(mergeWeightSum))
                                        mergeWeightSum = 1;
                                    scores[js][0] = scores[js][1] = ((scores[js][0] * mergeWeights[pS][partners[pS][i][0]]) + (scores[js][1] * mergeWeights[pS][partners[pS][i][1]]))
                                            / mergeWeightSum;
                                }
                            }
                            for (int is = 0; is < parentSplit; is++) {
                                for (int js = 0; js < childSplit; js++) {
                                    newScores[mapping[cS][partners[cS][j][js]]][mapping[pS][partners[pS][i][is]]] = scores[js][is];
                                    allZero = allZero && (scores[js][is] == 0);
                                }
                            }
                        }
                    }
                }
            }
            // if (allZero){
            // System.out.println("Maybe an underflow? Rule: "+oldRule);
            // System.out.println(ArrayUtil.toString(newScores));
            // System.out.println(ArrayUtil.toString(oldScores));
            // System.out.println(Arrays.toString(mergeWeights[pS]));
            // }
            final UnaryRule newRule = new UnaryRule(oldRule);
            newRule.setScores2(newScores);
            grammar.addUnary(newRule);
        }
        grammar.pruneSplitTree(partners, mapping);
        grammar.isGrammarTag = this.isGrammarTag;
        grammar.closedSumRulesWithParent = grammar.closedViterbiRulesWithParent = grammar.unaryRulesWithParent;
        grammar.closedSumRulesWithChild = grammar.closedViterbiRulesWithChild = grammar.unaryRulesWithC;

        return grammar;
    }

    /**
     * @param mergeThesePairs
     * @param partners
     */
    private void pruneSplitTree(final short[][][] partners, final short[][] mapping) {
        for (int tag = 0; tag < splitTrees.length; tag++) {
            final Tree<Short> splitTree = splitTrees[tag];

            final int maxDepth = splitTree.height();

            // // Compare getAtDepth(maxDepth - 2) to preterminals()
            // final List<Tree<Short>> getAtDepth = splitTree.getAtDepth(maxDepth - 2);
            // final List<Tree<Short>> preterminals = splitTree.preterminals();
            //
            // assertEquals(getAtDepth.size(), splitTree.preterminals().size());
            // for (Iterator<Tree<Short>> iter1 = getAtDepth.iterator(), iter2 = preterminals.iterator();
            // iter1.hasNext();) {
            // assertEquals(iter1.next().label(), iter2.next().label());
            // }
            //
            for (final Tree<Short> preTerminal : splitTree.subtrees(maxDepth - 2)) {
                final List<Tree<Short>> children = preTerminal.children();
                final ArrayList<Tree<Short>> newChildren = new ArrayList<Tree<Short>>(2);
                for (int i = 0; i < children.size(); i++) {
                    final Tree<Short> child = children.get(i);
                    final int curLoc = child.label();
                    if (partners[tag][curLoc][0] == curLoc) {
                        newChildren.add(new Tree<Short>(mapping[tag][curLoc]));
                    }
                }
                preTerminal.setChildren(newChildren);
            }
        }
    }

    // Unused, but might be useful
    public static void checkNormalization(final Grammar grammar) {
        final double[][] psum = new double[grammar.numSubStates.length][];
        for (int pS = 0; pS < grammar.numSubStates.length; pS++) {
            psum[pS] = new double[grammar.numSubStates[pS]];
        }
        final boolean[] sawPS = new boolean[grammar.numSubStates.length];
        for (final UnaryRule ur : grammar.unaryRuleMap.values()) {
            final int pS = ur.getParentState();
            sawPS[pS] = true;
            final int cS = ur.getChildState();
            final double[][] scores = ur.getScores2();
            for (int ci = 0; ci < grammar.numSubStates[cS]; ci++) {
                if (scores[ci] == null)
                    continue;
                for (int pi = 0; pi < grammar.numSubStates[pS]; pi++) {
                    psum[pS][pi] += scores[ci][pi];
                }
            }
        }
        for (final BinaryRule br : grammar.binaryRuleMap.values()) {
            final int pS = br.getParentState();
            sawPS[pS] = true;
            final int lcS = br.getLeftChildState();
            final int rcS = br.getRightChildState();
            final double[][][] scores = br.getScores2();
            for (int lci = 0; lci < grammar.numSubStates[lcS]; lci++) {
                for (int rci = 0; rci < grammar.numSubStates[rcS]; rci++) {
                    if (scores[lci][rci] == null)
                        continue;
                    for (int pi = 0; pi < grammar.numSubStates[pS]; pi++) {
                        psum[pS][pi] += scores[lci][rci][pi];
                    }
                }
            }
        }
        System.out.println();
        System.out.println("Checking for substates whose probs don't sum to 1");
        for (int pS = 0; pS < grammar.numSubStates.length; pS++) {
            if (!sawPS[pS])
                continue;
            for (int pi = 0; pi < grammar.numSubStates[pS]; pi++) {
                if (Math.abs(1 - psum[pS][pi]) > 0.001)
                    System.out.println(" state " + pS + " substate " + pi + " gives bad psum: " + psum[pS][pi]);
            }
        }
    }

    /**
     * @param mergeThesePairs
     * @param newNumSubStates
     * @param mapping
     * @param partners
     */
    public static void calculateMergeArrays(final boolean[][][] mergeThesePairs, final short[] newNumSubStates,
            final short[][] mapping, final short[][][] partners, final short[] numSubStates) {
        for (short state = 0; state < numSubStates.length; state++) {
            final short mergeTarget[] = new short[mergeThesePairs[state].length];
            Arrays.fill(mergeTarget, (short) -1);
            short count = 0;
            mapping[state] = new short[numSubStates[state]];
            partners[state] = new short[numSubStates[state]][];
            for (short j = 0; j < numSubStates[state]; j++) {
                if (mergeTarget[j] != -1) {
                    mapping[state][j] = mergeTarget[j];
                } else {
                    partners[state][j] = new short[1];
                    partners[state][j][0] = j;
                    mapping[state][j] = count;
                    count++;
                    // assume we're only merging pairs, so we only see things to
                    // merge
                    // with this substate when this substate isn't being merged
                    // with anything
                    // earlier
                    for (short k = (short) (j + 1); k < numSubStates[state]; k++) {
                        if (mergeThesePairs[state][j][k]) {
                            mergeTarget[k] = mapping[state][j];
                            partners[state][j] = new short[2];
                            partners[state][j][0] = j;
                            partners[state][j][1] = k;
                            partners[state][k] = partners[state][j];
                        }
                    }
                }
            }
            newNumSubStates[state] = count;
        }
        newNumSubStates[0] = 1; // never split or merge ROOT
    }

    public void fixMergeWeightsEtc(final boolean[][][] mergeThesePairs, final double[][] mergeWeights,
            final boolean[][][] complexMergePairs) {
        final short[] newNumSubStates = new short[numSubStates.length];
        final short[][] mapping = new short[numSubStates.length][];
        // invariant: if partners[state][substate][0] == substate, it's the 1st
        // one
        final short[][][] partners = new short[numSubStates.length][][];
        calculateMergeArrays(mergeThesePairs, newNumSubStates, mapping, partners, numSubStates);
        for (int tag = 0; tag < numSubStates.length; tag++) {
            final double[] newMergeWeights = new double[newNumSubStates[tag]];
            for (int i = 0; i < numSubStates[tag]; i++) {
                newMergeWeights[mapping[tag][i]] += mergeWeights[tag][i];
            }
            mergeWeights[tag] = newMergeWeights;

            final boolean[][] newComplexMergePairs = new boolean[newNumSubStates[tag]][newNumSubStates[tag]];
            final boolean[][] newMergeThesePairs = new boolean[newNumSubStates[tag]][newNumSubStates[tag]];
            for (int i = 0; i < complexMergePairs[tag].length; i++) {
                for (int j = 0; j < complexMergePairs[tag].length; j++) {
                    newComplexMergePairs[mapping[tag][i]][mapping[tag][j]] = newComplexMergePairs[mapping[tag][i]][mapping[tag][j]]
                            || complexMergePairs[tag][i][j];
                    newMergeThesePairs[mapping[tag][i]][mapping[tag][j]] = newMergeThesePairs[mapping[tag][i]][mapping[tag][j]]
                            || mergeThesePairs[tag][i][j];
                }
            }
            complexMergePairs[tag] = newComplexMergePairs;
            mergeThesePairs[tag] = newMergeThesePairs;
        }
    }

    public final boolean isGrammarTag(final int n) {
        return isGrammarTag[n];
    }

    /**
     * @param w
     */
    public void writeSplitTrees(final Writer w) {
        final PrintWriter out = new PrintWriter(w);
        for (int state = 1; state < numStates; state++) {
            String tag = tagNumberer.symbol(state);
            if (isGrammarTag[state] && tag.endsWith("^g"))
                tag = tag.substring(0, tag.length() - 2);
            out.write(tag + "\t" + splitTrees[state].toString() + "\n");
        }
        out.flush();
        out.close();
    }

    public int[][] getClosedSumPaths() {
        return closedSumPaths;
    }

    public int maxSubStates() {
        int max = 0;
        for (int i = 0; i < numSubStates.length; i++) {
            if (numSubStates[i] > max)
                max = numSubStates[i];
        }
        return max;
    }
}
