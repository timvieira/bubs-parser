package edu.berkeley.nlp.PCFGLA;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import cltool4j.BaseLogger;
import edu.berkeley.nlp.PCFGLA.smoothing.Smoother;
import edu.berkeley.nlp.math.SloppyMath;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.IEEEDoubleScaling;
import edu.berkeley.nlp.util.Numberer;

/**
 * Simple implementation of a PCFG grammar, offering the ability to look up rules by their child symbols. Rule
 * probability estimates are just relative frequency estimates off of training trees.
 */

public class Grammar implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    // TODO Remove?
    public boolean[] isGrammarTag;

    /** Records the binary rules (before splitting) */
    private List<BinaryRule>[] binaryRulesWithParent;

    /** Used after splitting */
    private BinaryRule[][] splitRulesWithP;

    private List<UnaryRule>[] unaryRulesWithParent;
    private List<UnaryRule>[] unaryRulesWithC;
    private List<UnaryRule>[] closedViterbiRulesWithParent = null;

    /** the number of states */
    public short numStates;

    /** the number of substates per state */
    public short[] numSubStates;

    // private Int2ObjectOpenHashMap<BinaryRule> binaryRuleMap;
    private final Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap = new Int2ObjectOpenHashMap<Grammar.PackedBinaryRule>();
    private final Int2ObjectOpenHashMap<PackedBinaryCount> packedBinaryCountMap = new Int2ObjectOpenHashMap<Grammar.PackedBinaryCount>();

    private Int2ObjectOpenHashMap<UnaryRule> unaryRuleMap;
    private Int2ObjectOpenHashMap<PackedUnaryRule> packedUnaryRuleMap;

    private UnaryCounterTable unaryRuleCounter = null;

    // private BinaryCounterTable binaryRuleCounter = null;

    protected final Numberer tagNumberer;

    public double minRuleProbability;

    public Smoother smoother = null;

    /**
     * A policy giving what state to go to next, starting from a given state, going to a given state. This array is
     * indexed by the start state, the end state, the start substate, and the end substate.
     */
    private int[][] closedViterbiPaths = null;
    private int[][] closedSumPaths = null;

    public Tree<Short>[] splitTrees;

    /**
     * Rather than calling some all-in-one constructor that takes a list of trees as training data, you call Grammar()
     * to create an empty grammar, call tallyTree() repeatedly to include all the training data, then call optimize() to
     * take it into account.
     * 
     * @param previousGrammar This is the previous grammar. We use this to copy the split trees that record how each
     *            state is split recursively.
     */
    public Grammar(final Grammar previousGrammar, final short[] numSubStates) {
        this.tagNumberer = Numberer.getGlobalNumberer("tags");
        this.smoother = previousGrammar.smoother;
        this.minRuleProbability = previousGrammar.minRuleProbability;

        unaryRuleCounter = new UnaryCounterTable(numSubStates);
        // binaryRuleCounter = new BinaryCounterTable(numSubStates);
        numStates = (short) numSubStates.length;
        this.numSubStates = numSubStates;

        splitTrees = previousGrammar.splitTrees;

        //
        // Create empty counters for all rules in the previous grammar
        //

        // Note: we could omit entries for rules with 0 probability in the previous grammar, but the indexing would
        // no longer match, and that would make subsequent counting more complex. Instead, we'll drop those rules
        // when creating packed-rule representations after counting (the counts will be 0).

        for (final int binaryKey : previousGrammar.packedBinaryRuleMap.keySet()) {
            final PackedBinaryRule previousRule = previousGrammar.packedBinaryRuleMap.get(binaryKey);
            packedBinaryCountMap.put(binaryKey, new PackedBinaryCount(previousRule.unsplitParent,
                    previousRule.unsplitLeftChild, previousRule.unsplitRightChild, previousRule.substates));
        }

        init();
    }

    /**
     * Construct a new {@link Grammar} instance without a previous grammar. We'll only do this when inducing the first
     * Markov-0 model.
     * 
     * Note: we'll leave {@link #packedBinaryCountMap} empty, and populate it in {@link #countUnsplitTree(Tree)}
     * 
     * @param numSubStates
     * @param smoother
     * @param minRuleProbability
     */
    @SuppressWarnings("unchecked")
    public Grammar(final short[] numSubStates, final Smoother smoother, final double minRuleProbability) {

        this.tagNumberer = Numberer.getGlobalNumberer("tags");
        this.smoother = smoother;
        this.minRuleProbability = minRuleProbability;

        unaryRuleCounter = new UnaryCounterTable(numSubStates);
        // binaryRuleCounter = new BinaryCounterTable(numSubStates);
        numStates = (short) numSubStates.length;
        this.numSubStates = numSubStates;

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

        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        // binaryRuleMap = new Int2ObjectOpenHashMap<BinaryRule>();
        unaryRuleMap = new Int2ObjectOpenHashMap<UnaryRule>();
        binaryRulesWithParent = new List[numStates];
        unaryRulesWithParent = new List[numStates];
        unaryRulesWithC = new List[numStates];
        closedViterbiRulesWithParent = new List[numStates];
        isGrammarTag = new boolean[numStates];

        closedViterbiPaths = new int[numStates][numStates];
        closedSumPaths = new int[numStates][numStates];

        for (short s = 0; s < numStates; s++) {
            binaryRulesWithParent[s] = new ArrayList<BinaryRule>();
            unaryRulesWithParent[s] = new ArrayList<UnaryRule>();
            unaryRulesWithC[s] = new ArrayList<UnaryRule>();
            closedViterbiRulesWithParent[s] = new ArrayList<UnaryRule>();

            final double[][] scores = new double[numSubStates[s]][numSubStates[s]];
            for (int i = 0; i < scores.length; i++) {
                scores[i][i] = 1;
            }
            final UnaryRule selfR = new UnaryRule(s, s, scores);
            relaxViterbiRule(selfR);
        }
    }

    /**
     * Returns an integer key representing the specified binary rule. Assumes that the number of unsplit nonterminals
     * will be <= 2^10 (1024).
     * 
     * @param unsplitParent
     * @param unsplitLeftChild
     * @param unsplitRightChild
     * @return an integer key representing the specified binary rule
     */
    public static int binaryKey(final short unsplitParent, final short unsplitLeftChild, final short unsplitRightChild) {
        assert unsplitParent < 1024;
        assert unsplitLeftChild < 1024;
        assert unsplitRightChild < 1024;

        return (unsplitParent << 20) | (unsplitLeftChild << 10) | unsplitRightChild;
    }

    /**
     * Returns the unsplit parent encoded into a binary key (as per {@link #binaryKey(short, short, short)}).
     * 
     * @param binaryKey
     * @return the unsplit parent encoded into a binary key
     */
    public static short unsplitParent(final int binaryKey) {
        return (short) (binaryKey >> 20);
    }

    /**
     * Returns the unsplit left child encoded into a binary key (as per {@link #binaryKey(short, short, short)}).
     * 
     * @param binaryKey
     * @return the unsplit left child encoded into a binary key
     */
    public static short unsplitLeftChild(final int binaryKey) {
        return (short) ((binaryKey >> 10) & 0x3ff);
    }

    /**
     * Returns the unsplit right child encoded into a binary key (as per {@link #binaryKey(short, short, short)}).
     * 
     * @param binaryKey
     * @return the unsplit right child encoded into a binary key
     */
    public static short unsplitRightChild(final int binaryKey) {
        return (short) (binaryKey & 0x3ff);
    }

    /**
     * Returns an integer key representing the specified unary rule. Assumes that the number of unsplit nonterminals
     * will be <= 2^10 (1024).
     * 
     * @param unsplitParent
     * @param unsplitChild
     * @return an integer key representing the specified unary rule
     */
    private int unaryKey(final short unsplitParent, final short unsplitChild) {
        assert unsplitParent < 1024;
        assert unsplitChild < 1024;

        return (unsplitParent << 10) | unsplitChild;
    }

    private void addBinary(final BinaryRule br) {
        binaryRulesWithParent[br.parentState].add(br);
        // binaryRuleMap.put(binaryKey(br.parentState, br.leftChildState, br.rightChildState), br);
    }

    private void addUnary(final UnaryRule ur) {
        if (!unaryRulesWithParent[ur.parentState].contains(ur)) {
            unaryRulesWithParent[ur.parentState].add(ur);
            unaryRulesWithC[ur.childState].add(ur);
            unaryRuleMap.put(unaryKey(ur.parentState, ur.childState), ur);
        }
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
            for (final UnaryRule r : closedViterbiRulesWithParent[state]) {
                out.print(r.toString());
            }
        }
        out.flush();
    }

    public void setSmoother(final Smoother smoother) {
        this.smoother = smoother;
    }

    public void optimize(final double randomness) {

        init();

        // TODO Split this into a separate method - randomness is only != 0 on the very first run (with the Markov-0
        // grammar)
        if (randomness > 0.0) {
            final Random random = GrammarTrainer.RANDOM;

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

            for (final int binaryKey : packedBinaryCountMap.keySet()) {
                final PackedBinaryCount packedBinaryCount = packedBinaryCountMap.get(binaryKey);
                for (int i = 0; i < packedBinaryCount.ruleCounts.length; i++) {
                    packedBinaryCount.ruleCounts[i] += random.nextDouble() * randomness;
                }
            }

            // // TODO Remove this
            // for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
            // final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
            // for (int i = 0; i < binaryCounts.length; i++) {
            // for (int j = 0; j < binaryCounts[i].length; j++) {
            // if (binaryCounts[i][j] == null)
            // binaryCounts[i][j] = new double[numSubStates[binaryRule.getParentState()]];
            // for (int k = 0; k < binaryCounts[i][j].length; k++) {
            // final double r = random.nextDouble() * randomness;
            // binaryCounts[i][j][k] += r;
            // }
            // }
            // }
            // binaryRuleCounter.setCount(binaryRule, binaryCounts);
            // }
            // packedBinaryCountMap.clear();
            //
            // for (final BinaryRule rule : binaryRuleCounter.entries.keySet()) {
            // final int binaryKey = binaryKey(rule.parentState, rule.leftChildState, rule.rightChildState);
            // packedBinaryCountMap.put(binaryKey, new PackedBinaryCount(rule.parentState, rule.leftChildState,
            // rule.rightChildState, binaryRuleCounter.getCount(rule)));
            // }
        }

        // if (randomness == 0) {
        // assertBinaryCountsEqual();
        // }

        normalizeCounts();
        smooth();

        // assertBinaryRulesEqual();
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
    }

    /**
     * Smooth the rule probabilities in {@link #packedBinaryRuleMap}
     */
    public void smooth() {
        smoother.smooth(unaryRuleCounter, null, packedBinaryRuleMap, numSubStates);
        // normalizeCounts();

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

        // TODO Copy induced binary productions from counts to (new) rules
        // for (final int binaryKey : packedBinaryCountMap.keySet()) {
        // final PackedBinaryCount packedBinaryCount = packedBinaryCountMap.get(binaryKey);
        // final short unsplitParent = unsplitParent(binaryKey);
        //
        // for (int i = 0, j = 0; i < packedBinaryCount.ruleCounts.length; i++, j += 3) {
        // final short parentSplit = packedBinaryCount.substates[j + 2];
        // final double normalizedCount = (packedBinaryCount.ruleCounts[i] / parentCounts[unsplitParent][parentSplit]);
        //
        // if (normalizedCount >= minRuleProbability && !SloppyMath.isVeryDangerous(normalizedCount)) {
        // packedBinaryCount.ruleCounts[i] = normalizedCount;
        // } else {
        // packedBinaryCount.ruleCounts[i] = 0;
        // }
        // }
        // }

        // // TODO Remove
        // for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
        // final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
        // for (int i = 0; i < binaryCounts.length; i++) {
        // for (int j = 0; j < binaryCounts[i].length; j++) {
        // if (binaryCounts[i][j] == null)
        // continue;
        // /**
        // * allZero records if all probabilities are 0. If so, we want to null out the matrix element.
        // */
        // double allZero = 0;
        // int k = 0;
        // while (allZero == 0 && k < binaryCounts[i][j].length) {
        // allZero += binaryCounts[i][j][k++];
        // }
        // if (allZero == 0) {
        // binaryCounts[i][j] = null;
        // }
        // }
        // }
        // binaryRule.setScores2(binaryCounts);
        // addBinary(binaryRule);
        // }

        // Reset all counters:
        unaryRuleCounter = new UnaryCounterTable(numSubStates);
        // binaryRuleCounter = new BinaryCounterTable(numSubStates);
    }

    /**
     * Normalize the unary & binary counts from {@link #packedUnaryCountMap} and {@link #packedBinaryCountMap},
     * populating the resulting probabilities into {@link #packedUnaryRuleMap} and {@link #packedBinaryRuleMap}
     */
    public void normalizeCounts() {

        final double[][] parentCounts = observedParentCounts();

        // turn the rule scores into fractions
        for (final UnaryRule unaryRule : unaryRuleCounter.keySet()) {
            final double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
            final int parentState = unaryRule.getParentState();
            final int nParentSubStates = numSubStates[parentState];
            final int nChildStates = numSubStates[unaryRule.childState];

            boolean allZero = true;
            for (int j = 0; j < nChildStates; j++) {
                if (unaryCounts[j] == null)
                    continue;
                for (int parentSplit = 0; parentSplit < nParentSubStates; parentSplit++) {
                    if (parentCounts[parentState][parentSplit] != 0) {
                        double nVal = (unaryCounts[j][parentSplit] / parentCounts[parentState][parentSplit]);
                        if (nVal < minRuleProbability || SloppyMath.isVeryDangerous(nVal)) {
                            nVal = 0;
                        }
                        unaryCounts[j][parentSplit] = nVal;
                    }
                    allZero = allZero && (unaryCounts[j][parentSplit] == 0);
                }
            }
            if (allZero) {
                throw new IllegalArgumentException("Maybe an underflow? Rule: " + unaryRule + "\n");
            }
        }

        for (final int binaryKey : packedBinaryCountMap.keySet()) {
            final PackedBinaryCount packedCount = packedBinaryCountMap.get(binaryKey);
            final PackedBinaryRule packedRule = new PackedBinaryRule(packedCount, parentCounts, minRuleProbability);
            packedBinaryRuleMap.put(binaryKey, packedRule);
        }

        // // TODO Remove
        // for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
        // final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
        // final int parentState = binaryRule.parentState;
        // final int nParentSubStates = numSubStates[parentState];
        //
        // for (int j = 0; j < binaryCounts.length; j++) {
        // for (int k = 0; k < binaryCounts[j].length; k++) {
        // if (binaryCounts[j][k] == null) {
        // continue;
        // }
        //
        // for (int parentSplit = 0; parentSplit < nParentSubStates; parentSplit++) {
        // if (parentCounts[parentState][parentSplit] != 0) {
        // double nVal = (binaryCounts[j][k][parentSplit] / parentCounts[parentState][parentSplit]);
        // if (nVal < minRuleProbability || SloppyMath.isVeryDangerous(nVal)) {
        // nVal = 0;
        // }
        // binaryCounts[j][k][parentSplit] = nVal;
        // }
        // }
        // }
        // }
        // }
        //
        // assertBinaryRulesEqual();
    }

    /**
     * Returns the total of all (fractional) parent counts observed in the training corpus, as an array indexed by
     * unsplit parent and parent split.
     * 
     * @return the total of all (fractional) parent counts observed in the training corpus
     */
    private double[][] observedParentCounts() {

        // Indexed by parent state, parent split
        final double[][] parentCounts = new double[numStates][];
        for (int unsplitParent = 0; unsplitParent < numStates; unsplitParent++) {
            parentCounts[unsplitParent] = new double[numSubStates[unsplitParent]];
        }

        for (final UnaryRule unaryRule : unaryRuleCounter.keySet()) {
            final double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
            final int parentState = unaryRule.getParentState();

            if (unaryRule.childState == parentState) {
                continue;
            }

            for (int childSplit = 0; childSplit < unaryCounts.length; childSplit++) {
                if (unaryCounts[childSplit] == null) {
                    continue;
                }

                for (int parentSplit = 0; parentSplit < parentCounts[parentState].length; parentSplit++) {
                    parentCounts[parentState][parentSplit] += unaryCounts[childSplit][parentSplit];
                }
            }
        }

        for (final int binaryKey : packedBinaryCountMap.keySet()) {
            final short unsplitParent = unsplitParent(binaryKey);
            final PackedBinaryCount packedBinaryCount = packedBinaryCountMap.get(binaryKey);
            for (int i = 0, j = 0; i < packedBinaryCount.ruleCounts.length; i++, j += 3) {
                final short parentSplit = packedBinaryCount.substates[j + 2];
                parentCounts[unsplitParent][parentSplit] += packedBinaryCount.ruleCounts[i];
            }
        }

        // for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
        //
        // final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
        // final int parentState = binaryRule.parentState;
        //
        // for (int leftChildSplit = 0; leftChildSplit < binaryCounts.length; leftChildSplit++) {
        // for (int rightChildSplit = 0; rightChildSplit < binaryCounts[leftChildSplit].length; rightChildSplit++) {
        //
        // if (binaryCounts[leftChildSplit][rightChildSplit] == null) {
        // continue;
        // }
        //
        // for (int parentSplit = 0; parentSplit < parentCounts[parentState].length; parentSplit++) {
        // parentCounts[parentState][parentSplit] += binaryCounts[leftChildSplit][rightChildSplit][parentSplit];
        // }
        // }
        // }
        // }

        return parentCounts;
    }

    /**
     * Accumulates counts for a {@link Tree} containing split states
     * 
     * @param tree
     */
    public void countSplitTree(final Tree<StateSet> tree, final Grammar previousGrammar) {

        // Skip 1-word trees
        if (tree.isLeaf() || tree.isPreTerminal()) {
            return;
        }

        // Check that the top node is not split (it has only one substate)
        final StateSet node = tree.label();
        if (node.numSubStates() != 1) {
            throw new IllegalArgumentException("The start symbol should never be split");
        }

        // The inside score of the start symbol is the (log) probability of the tree
        final double treeProbability = node.getIScore(0);
        final int tree_scale = node.getIScale();

        if (treeProbability == 0) {
            BaseLogger.singleton().config("Skipping a 0-probability tree.");
            return;
        }
        countSplitTree(tree, treeProbability, tree_scale, previousGrammar);
    }

    /**
     * Accumulates counts for a subtree containing split states
     * 
     * @param tree
     * @param treeInsideScore
     * @param tree_scale
     * @param previousGrammar
     */
    private void countSplitTree(final Tree<StateSet> tree, double treeInsideScore, final int tree_scale,
            final Grammar previousGrammar) {

        if (tree.isLeaf() || tree.isPreTerminal()) {
            return;
        }

        if (treeInsideScore == 0) {
            treeInsideScore = 1;
        }

        final List<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short unsplitParent = parent.getState();
        final int nParentSubStates = numSubStates[unsplitParent];

        switch (children.size()) {

        case 1:
            final StateSet child = children.get(0).label();
            final short unsplitChild = child.getState();
            final int nChildSubStates = numSubStates[unsplitChild];
            final UnaryRule urule = new UnaryRule(unsplitParent, unsplitChild);

            double[][] ucounts = unaryRuleCounter.getCount(urule);
            if (ucounts == null) {
                ucounts = new double[nChildSubStates][];
                unaryRuleCounter.setCount(urule, ucounts);
            }
            double scalingFactor = IEEEDoubleScaling.scalingMultiplier(parent.getOScale() + child.getIScale()
                    - tree_scale);

            final Grammar.PackedUnaryRule packedUnaryRule = previousGrammar.getPackedUnaryScores(unsplitParent,
                    unsplitChild);

            // Iterate through all splits of the rule. Most will have non-0 child and parent probability (since the
            // rule currently exists in the grammar), and this iteration order is very efficient
            for (int i = 0, j = 0; i < packedUnaryRule.ruleScores.length; i++, j += 2) {
                final short childSplit = packedUnaryRule.substates[j];
                final short parentSplit = packedUnaryRule.substates[j + 1];

                final double parentOutsideScore = parent.getOScore(parentSplit);
                final double childInsideScore = child.getIScore(childSplit);

                final double scaledRuleCount = (packedUnaryRule.ruleScores[i] * childInsideScore / treeInsideScore)
                        * scalingFactor * parentOutsideScore;

                if (ucounts[childSplit] == null) {
                    ucounts[childSplit] = new double[nParentSubStates];
                }
                ucounts[childSplit][parentSplit] += scaledRuleCount;
            }

            break;

        case 2:
            final StateSet leftChild = children.get(0).label();
            final short unsplitLeftChild = leftChild.getState();
            final StateSet rightChild = children.get(1).label();
            final short unsplitRightChild = rightChild.getState();
            final int nLeftChildSubStates = numSubStates[unsplitLeftChild];
            final int nRightChildSubStates = numSubStates[unsplitRightChild];

            scalingFactor = IEEEDoubleScaling.scalingMultiplier(parent.getOScale() + leftChild.getIScale()
                    + rightChild.getIScale() - tree_scale);

            final BinaryRule brule = new BinaryRule(unsplitParent, unsplitLeftChild, unsplitRightChild);
            // double[][][] bcounts = binaryRuleCounter.getCount(brule);
            // if (bcounts == null) {
            // bcounts = new double[nLeftChildSubStates][nRightChildSubStates][];
            // binaryRuleCounter.setCount(brule, bcounts);
            // }

            final Grammar.PackedBinaryRule packedBinaryRule = previousGrammar.getPackedBinaryScores(unsplitParent,
                    unsplitLeftChild, unsplitRightChild);
            final Grammar.PackedBinaryCount packedBinaryCount = packedBinaryCountMap.get(binaryKey(unsplitParent,
                    unsplitLeftChild, unsplitRightChild));

            // Iterate through all splits of the rule. Most will have non-0 child and parent probability (since the
            // rule currently exists in the grammar), and this iteration order is very efficient
            for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {
                final short leftChildSplit = packedBinaryRule.substates[j];
                final short rightChildSplit = packedBinaryRule.substates[j + 1];
                final short parentSplit = packedBinaryRule.substates[j + 2];

                final double leftChildInsideScore = leftChild.getIScore(leftChildSplit);
                final double rightChildInsideScore = rightChild.getIScore(rightChildSplit);

                final double parentOutsideScore = parent.getOScore(parentSplit);

                final double scaledRuleCount = (packedBinaryRule.ruleScores[i] * leftChildInsideScore / treeInsideScore)
                        * rightChildInsideScore * scalingFactor * parentOutsideScore;

                packedBinaryCount.ruleCounts[i] += scaledRuleCount;
                // if (bcounts[leftChildSplit][rightChildSplit] == null) {
                // bcounts[leftChildSplit][rightChildSplit] = new double[nParentSubStates];
                // }
                // bcounts[leftChildSplit][rightChildSplit][parentSplit] += scaledRuleCount;
            }

            // packedBinaryCount.assertEquals(bcounts);

            break;

        default:
            throw new IllegalArgumentException("Malformed tree: more than two children");
        }

        for (final Tree<StateSet> child : children) {
            countSplitTree(child, treeInsideScore, tree_scale, previousGrammar);
        }
    }

    /**
     * Accumulates counts for a {@link Tree} containing unsplit states (Markov-0 node labels)
     * 
     * @param tree
     */
    public void countUnsplitTree(final Tree<StateSet> tree) {

        // The lexicon will handle preterminals
        if (tree.isLeaf() || tree.isPreTerminal()) {
            return;
        }

        final List<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short unsplitParent = parent.getState();
        final int nParentSubStates = parent.numSubStates(); // numSubStates[parentState];

        switch (children.size()) {

        case 1:
            final StateSet child = children.get(0).label();
            final short childState = child.getState();
            final int nChildSubStates = child.numSubStates(); // numSubStates[childState];
            final double[][] counts = new double[nChildSubStates][nParentSubStates];
            final UnaryRule urule = new UnaryRule(unsplitParent, childState, counts);
            unaryRuleCounter.incrementCount(urule, 1.0);
            break;

        case 2:
            final StateSet leftChild = children.get(0).label();
            final short unsplitLeftChild = leftChild.getState();
            final StateSet rightChild = children.get(1).label();
            final short unsplitRightChild = rightChild.getState();
            final int nLeftChildSubStates = leftChild.numSubStates();
            final int nRightChildSubStates = rightChild.numSubStates();

            final double[][][] bcounts = new double[nLeftChildSubStates][nRightChildSubStates][nParentSubStates];
            final BinaryRule brule = new BinaryRule(unsplitParent, unsplitLeftChild, unsplitRightChild, bcounts);
            // binaryRuleCounter.incrementCount(brule, 1.0);

            final int binaryKey = binaryKey(unsplitParent, unsplitLeftChild, unsplitRightChild);
            PackedBinaryCount packedCount = packedBinaryCountMap.get(binaryKey);
            if (packedCount == null) {
                // Construct a packed counter with only one entry(since this will only be used on unsplit trees)
                packedCount = new PackedBinaryCount(unsplitParent, unsplitLeftChild, unsplitRightChild);
                packedBinaryCountMap.put(binaryKey, packedCount);
            }
            packedCount.ruleCounts[0] += 1;

            break;

        default:
            throw new Error("Malformed tree: more than two children");
        }

        for (final Tree<StateSet> child : children) {
            countUnsplitTree(child);
        }

        // assertBinaryCountsEqual();
    }

    public void computePairsOfUnaries() {

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
                    closedSumPaths[parentState][childState] = bestSumIntermed;
                }
                if (bestMaxIntermed > -2) {
                    resultRmax.setScores2(scoresMax);
                    closedViterbiRulesWithParent[parentState].add(resultRmax);
                    closedViterbiPaths[parentState][childState] = bestMaxIntermed;
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
        closedViterbiRulesWithParent[rule.parentState].add(rule);
    }

    /**
     * Populates the "splitRules" accessor lists using the existing rule lists. If the state is synthetic, these lists
     * contain all rules for the state. If the state is NOT synthetic, these lists contain only the rules in which both
     * children are not synthetic.
     * <p>
     * <i>This method must be called before the grammar is used, either after training or deserializing grammar.</i>
     * 
     * TODO Only used in {@link #splitRulesWithP(int)} and {@link WriteGrammarToTextFile}. Consolidate.
     */
    public void splitRules() {

        if (binaryRulesWithParent == null) {
            return;
        }

        splitRulesWithP = new BinaryRule[numStates][];

        for (int state = 0; state < numStates; state++) {
            splitRulesWithP[state] = binaryRulesWithParent[state].toArray(new BinaryRule[0]);
        }
        // we don't need the original lists anymore
        binaryRulesWithParent = null;
    }

    // TODO Only used in writeData() and toString(). Consolidate there?
    public BinaryRule[] splitRulesWithP(final int state) {

        if (splitRulesWithP == null) {
            splitRules();
        }

        if (state >= splitRulesWithP.length) {
            return new BinaryRule[0];
        }

        return splitRulesWithP[state];
    }

    public PackedUnaryRule getPackedUnaryScores(final short unsplitParent, final short unsplitChild) {

        if (packedUnaryRuleMap == null) {

            // Populate the packed representation of all binary rules
            packedUnaryRuleMap = new Int2ObjectOpenHashMap<Grammar.PackedUnaryRule>();

            for (final int unaryKey : unaryRuleMap.keySet()) {
                final UnaryRule rule = unaryRuleMap.get(unaryKey);
                packedUnaryRuleMap.put(unaryKey, new PackedUnaryRule(rule.getScores2(), numSubStates[unsplitParent],
                        numSubStates[unsplitChild]));
            }
        }

        return packedUnaryRuleMap.get(unaryKey(unsplitParent, unsplitChild));
    }

    public PackedBinaryRule getPackedBinaryScores(final short unsplitParent, final short unsplitLeftChild,
            final short unsplitRightChild) {

        return packedBinaryRuleMap.get(binaryKey(unsplitParent, unsplitLeftChild, unsplitRightChild));
    }

    // // TODO Remove
    // private void assertBinaryCountsEqual() {
    // for (final BinaryRule binaryRule : binaryRuleCounter.keySet()) {
    // final int binaryKey = binaryKey(binaryRule.parentState, binaryRule.leftChildState,
    // binaryRule.rightChildState);
    // final PackedBinaryCount packedBinaryCount = packedBinaryCountMap.get(binaryKey);
    //
    // final double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
    //
    // for (int i = 0, j = 0; i < packedBinaryCount.ruleCounts.length; i++, j += 3) {
    // final short leftChildSplit = packedBinaryCount.substates[j];
    // final short rightChildSplit = packedBinaryCount.substates[j + 1];
    // final short parentSplit = packedBinaryCount.substates[j + 2];
    //
    // assertEquals(binaryCounts[leftChildSplit][rightChildSplit][parentSplit],
    // packedBinaryCount.ruleCounts[i], 1e-10);
    // }
    // }
    // }
    //
    // // TODO Remove
    // private void assertBinaryRulesEqual() {
    // assertBinaryRulesEqual(binaryRuleMap, packedBinaryRuleMap);
    // }

    // public static void assertBinaryRulesEqual(final Int2ObjectOpenHashMap<BinaryRule> binaryRuleMap,
    // final Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap) {
    // for (final int binaryKey : binaryRuleMap.keySet()) {
    // final BinaryRule binaryRule = binaryRuleMap.get(binaryKey);
    // final PackedBinaryRule packedBinaryRule = packedBinaryRuleMap.get(binaryKey);
    //
    // for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {
    // final short leftChildSplit = packedBinaryRule.substates[j];
    // final short rightChildSplit = packedBinaryRule.substates[j + 1];
    // final short parentSplit = packedBinaryRule.substates[j + 2];
    //
    // assertEquals(binaryRule.scores[leftChildSplit][rightChildSplit][parentSplit],
    // packedBinaryRule.ruleScores[i], 1e-10);
    // }
    // }
    // }

    /**
     * Split all substates into two new ones. This produces a new Grammar with updated rules.
     * 
     * @param randomness percent randomness applied in splitting rules
     * @return a new grammar, with all states split in 2
     */
    public Grammar splitAllStates(final double randomness, final int[] counts) {
        final short[] newNumSubStates = new short[numSubStates.length];
        newNumSubStates[0] = 1; // never split ROOT
        for (short i = 1; i < numSubStates.length; i++) {
            newNumSubStates[i] = (short) (numSubStates[i] * 2);
        }

        // create the new grammar
        final Grammar newGrammar = new Grammar(this, newNumSubStates);
        final Random random = GrammarTrainer.RANDOM;

        for (final int binaryKey : packedBinaryRuleMap.keySet()) {
            final PackedBinaryRule packedRule = packedBinaryRuleMap.get(binaryKey);
            final BinaryRule unpackedRule = new BinaryRule(packedRule, numSubStates);
            final BinaryRule splitRule = unpackedRule.splitRule(numSubStates, newNumSubStates, random, randomness);

            newGrammar.packedBinaryRuleMap.put(binaryKey, new PackedBinaryRule(packedRule.unsplitParent,
                    packedRule.unsplitLeftChild, packedRule.unsplitRightChild, splitRule.scores));
        }

        for (final UnaryRule oldRule : unaryRuleMap.values()) {
            newGrammar.addUnary(oldRule.splitRule(numSubStates, newNumSubStates, random, randomness));
        }

        newGrammar.isGrammarTag = this.isGrammarTag;
        newGrammar.extendSplitTrees(splitTrees, numSubStates);
        newGrammar.computePairsOfUnaries();

        return newGrammar;
    }

    @SuppressWarnings("unchecked")
    private void extendSplitTrees(final Tree<Short>[] trees, final short[] oldNumSubStates) {
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

        if (tree.isLeaf()) {
            return;
        }

        final StateSet label = tree.label();
        final short state = label.getState();
        final double probs[] = new double[label.numSubStates()];
        double total = 0;

        for (short i = 0; i < label.numSubStates(); i++) {
            final double posteriorScore = label.getIScore(i) * label.getOScore(i);
            probs[i] = posteriorScore;
            total += posteriorScore;
        }

        if (total == 0) {
            total = 1;
        }

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
        final Grammar mergedGrammar = new Grammar(this, newNumSubStates);

        for (final int binaryKey : packedBinaryRuleMap.keySet()) {
            final PackedBinaryRule packedRule = packedBinaryRuleMap.get(binaryKey);
            final BinaryRule oldRule = new BinaryRule(packedRule, numSubStates);

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

            final BinaryRule mergedRule = new BinaryRule(oldRule);
            mergedRule.setScores2(newScores);
            mergedGrammar.packedBinaryRuleMap.put(binaryKey, new PackedBinaryRule(packedRule.unsplitParent,
                    packedRule.unsplitLeftChild, packedRule.unsplitRightChild, mergedRule.scores));
        }

        for (final UnaryRule oldRule : unaryRuleMap.values()) {

            final short pS = oldRule.getParentState(), cS = oldRule.getChildState();

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

            final UnaryRule newRule = new UnaryRule(oldRule);
            newRule.setScores2(newScores);
            mergedGrammar.addUnary(newRule);
        }

        mergedGrammar.pruneSplitTree(partners, mapping);
        mergedGrammar.isGrammarTag = this.isGrammarTag;

        return mergedGrammar;
    }

    /**
     * @param mergeThesePairs
     * @param partners
     */
    private void pruneSplitTree(final short[][][] partners, final short[][] mapping) {
        for (int tag = 0; tag < splitTrees.length; tag++) {
            final Tree<Short> splitTree = splitTrees[tag];

            final int maxDepth = splitTree.height();

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

    // // Unused, but might be useful
    // public static void checkNormalization(final Grammar grammar) {
    // final double[][] psum = new double[grammar.numSubStates.length][];
    // for (int pS = 0; pS < grammar.numSubStates.length; pS++) {
    // psum[pS] = new double[grammar.numSubStates[pS]];
    // }
    // final boolean[] sawPS = new boolean[grammar.numSubStates.length];
    //
    // for (final UnaryRule ur : grammar.unaryRuleMap.values()) {
    // final int pS = ur.getParentState();
    // sawPS[pS] = true;
    // final int cS = ur.getChildState();
    // final double[][] scores = ur.getScores2();
    // for (int ci = 0; ci < grammar.numSubStates[cS]; ci++) {
    // if (scores[ci] == null) {
    // continue;
    // }
    // for (int pi = 0; pi < grammar.numSubStates[pS]; pi++) {
    // psum[pS][pi] += scores[ci][pi];
    // }
    // }
    // }
    //
    // for (final BinaryRule br : grammar.binaryRuleMap.values()) {
    // final int pS = br.getParentState();
    // sawPS[pS] = true;
    // final int lcS = br.getLeftChildState();
    // final int rcS = br.getRightChildState();
    // final double[][][] scores = br.getScores2();
    // for (int lci = 0; lci < grammar.numSubStates[lcS]; lci++) {
    // for (int rci = 0; rci < grammar.numSubStates[rcS]; rci++) {
    // if (scores[lci][rci] == null)
    // continue;
    // for (int pi = 0; pi < grammar.numSubStates[pS]; pi++) {
    // psum[pS][pi] += scores[lci][rci][pi];
    // }
    // }
    // }
    // }
    //
    // for (int pS = 0; pS < grammar.numSubStates.length; pS++) {
    // if (!sawPS[pS]) {
    // continue;
    // }
    // for (int pi = 0; pi < grammar.numSubStates[pS]; pi++) {
    // if (Math.abs(1 - psum[pS][pi]) > 0.001)
    // System.out.println(" state " + pS + " substate " + pi + " gives bad psum: " + psum[pS][pi]);
    // }
    // }
    // }

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

    public int maxSubStates() {
        int max = 0;
        for (int i = 0; i < numSubStates.length; i++) {
            if (numSubStates[i] > max)
                max = numSubStates[i];
        }
        return max;
    }

    @Override
    public String toString() {
        return toString(-1, 0);
    }

    public String toString(final int nLex, final double minimumRuleProbability) {

        final StringBuilder sb = new StringBuilder();

        // Count the total number of rules
        int nBinary = 0, nUnary = 0;

        for (int state = 0; state < numStates; state++) {
            // TODO Fix these counts
            nBinary += splitRulesWithP(state).length;
            nUnary += closedViterbiRulesWithParent[state].size();
        }
        sb.append(String
                .format("lang=%s format=Berkeley unkThresh=%s start=%s hMarkov=%s vMarkov=%s date=%s vocabSize=%d nBinary=%d nUnary=%d nLex=%d\n",
                        "UNK", "UNK", "ROOT_0", "UNK", "UNK", new SimpleDateFormat("yyyy/mm/dd").format(new Date()),
                        numStates, nBinary, nUnary, nLex));

        final List<String> ruleStrings = new ArrayList<String>();

        for (final int binaryKey : packedBinaryRuleMap.keySet()) {
            ruleStrings.add(packedBinaryRuleMap.get(binaryKey).toString(minimumRuleProbability));
        }

        for (int state = 0; state < numStates; state++) {
            for (final UnaryRule r : closedViterbiRulesWithParent[state]) {
                ruleStrings.add(r.toString(minimumRuleProbability));
            }
        }

        Collections.sort(ruleStrings);
        for (final String ruleString : ruleStrings) {
            sb.append(ruleString);
        }
        return sb.toString();
    }

    /**
     * Represents the splits of a coarse binary rule (e.g. for NP -> DT NN, NP_0 -> DT_0 NN_0, NP_1 -> DT_0 NN_1, etc.)
     * Packed into a compact parallel array representation.
     * 
     * After learning production probabilities (and pruning low-probability rules), we pack them into this
     * representation for efficient iteration in {@link ArrayParser#doInsideScores(Tree, boolean, double[][][])}.
     * {@link ArrayParser#doInsideOutsideScores(Tree, boolean)}, and
     * {@link Grammar#countSplitTree(Tree, double, int, Grammar)}.
     */
    public static class PackedBinaryRule implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Production probabilities for each split rule, sorted by by parent split, left child split, right child split */
        public final double[] ruleScores;

        public final short unsplitParent;
        public final short unsplitLeftChild;
        public final short unsplitRightChild;

        /**
         * Parent, left child, and right child substates for each rule. Something of a parallel array to
         * {@link #ruleScores}, but each entry in {@link #ruleScores} corresponds to 3 in {@link #substates}, so it is
         * 3x as long
         */
        public final short[] substates;

        /**
         * Copies from a {@link PackedBinaryCount} and normalizes
         * 
         * @param binaryCount
         * @param parentCounts
         * @param minRuleProbability
         */
        public PackedBinaryRule(final PackedBinaryCount binaryCount, final double[][] parentCounts,
                final double minRuleProbability) {

            this.unsplitParent = binaryCount.unsplitParent;
            this.unsplitLeftChild = binaryCount.unsplitLeftChild;
            this.unsplitRightChild = binaryCount.unsplitRightChild;

            // Record the number of non-0 observation counts so we can allocate an array of the proper length
            int observedCounts = 0;
            for (int i = 0, j = 0; i < binaryCount.ruleCounts.length; i++, j += 3) {
                final short parentSplit = binaryCount.substates[j + 2];
                final double normalizedProbability = (binaryCount.ruleCounts[i] / parentCounts[unsplitParent][parentSplit]);

                if (normalizedProbability >= minRuleProbability && !SloppyMath.isVeryDangerous(normalizedProbability)) {
                    observedCounts++;
                }
            }

            // Create and populate the parallel array
            this.ruleScores = new double[observedCounts];
            this.substates = new short[observedCounts * 3];
            for (int i = 0, j = 0, x = 0, y = 0; i < binaryCount.ruleCounts.length; i++, j += 3) {

                if (binaryCount.ruleCounts[i] > 0) {
                    final short parentSplit = binaryCount.substates[j + 2];
                    final double normalizedProbability = (binaryCount.ruleCounts[i] / parentCounts[unsplitParent][parentSplit]);

                    if (normalizedProbability >= minRuleProbability
                            && !SloppyMath.isVeryDangerous(normalizedProbability)) {
                        this.ruleScores[x] = normalizedProbability;
                        this.substates[y] = binaryCount.substates[j];
                        this.substates[y + 1] = binaryCount.substates[j + 1];
                        this.substates[y + 2] = binaryCount.substates[j + 2];
                        x++;
                        y += 3;
                    }
                }
            }
        }

        /**
         * Copies from the {@link BinaryRule} representation
         * 
         * TODO Remove at some point? Can we eliminate the un-packed rule representation?
         * 
         * @param unsplitParent
         * @param unsplitLeftChild
         * @param unsplitRightChild
         * @param ruleScores
         */
        public PackedBinaryRule(final short unsplitParent, final short unsplitLeftChild, final short unsplitRightChild,
                final double[][][] ruleScores) {

            this.unsplitParent = unsplitParent;
            this.unsplitLeftChild = unsplitLeftChild;
            this.unsplitRightChild = unsplitRightChild;

            // Count the total number of non-0 production probabilities (so we can size the parallel arrays properly)
            int totalRules = 0;
            for (short splitLeftChild = 0; splitLeftChild < ruleScores.length; splitLeftChild++) {
                final double[][] leftChildSplitRules = ruleScores[splitLeftChild];
                if (leftChildSplitRules == null) {
                    continue;
                }

                for (short splitRightChild = 0; splitRightChild < leftChildSplitRules.length; splitRightChild++) {
                    final double[] rightChildSplitRules = leftChildSplitRules[splitRightChild];
                    if (rightChildSplitRules == null) {
                        continue;
                    }

                    for (short splitParent = 0; splitParent < rightChildSplitRules.length; splitParent++) {

                        if (rightChildSplitRules[splitParent] > 0) {
                            totalRules++;
                        }
                    }
                }
            }

            this.ruleScores = new double[totalRules];
            this.substates = new short[totalRules * 3];

            // Populate them in order in the new arrays, sorted by left child, right child, parent
            int offset = 0;
            for (short splitLeftChild = 0; splitLeftChild < ruleScores.length; splitLeftChild++) {
                final double[][] leftChildSplitRules = ruleScores[splitLeftChild];
                if (leftChildSplitRules == null) {
                    continue;
                }

                for (short splitRightChild = 0; splitRightChild < leftChildSplitRules.length; splitRightChild++) {
                    final double[] rightChildSplitRules = leftChildSplitRules[splitRightChild];
                    if (rightChildSplitRules == null) {
                        continue;
                    }

                    for (short splitParent = 0; splitParent < rightChildSplitRules.length; splitParent++) {

                        if (rightChildSplitRules[splitParent] > 0) {
                            this.ruleScores[offset] = ruleScores[splitLeftChild][splitRightChild][splitParent];
                            final int substateOffset = offset * 3;
                            this.substates[substateOffset] = splitLeftChild;
                            this.substates[substateOffset + 1] = splitRightChild;
                            this.substates[substateOffset + 2] = splitParent;
                            offset++;
                        }
                    }
                }
            }
        }

        public String toString(final double minimumRuleProbability) {

            final Numberer n = Numberer.getGlobalNumberer("tags");
            String sLeftChild = n.symbol(unsplitLeftChild);

            if (sLeftChild.endsWith("^g")) {
                sLeftChild = sLeftChild.substring(0, sLeftChild.length() - 2);
            }

            String sRightChild = n.symbol(unsplitRightChild);
            if (sRightChild.endsWith("^g")) {
                sRightChild = sRightChild.substring(0, sRightChild.length() - 2);
            }

            String sParent = n.symbol(unsplitParent);
            if (sParent.endsWith("^g")) {
                sParent = sParent.substring(0, sParent.length() - 2);
            }

            final StringBuilder sb = new StringBuilder();

            for (int i = 0, j = 0; i < ruleScores.length; i++, j += 3) {
                final double prob = ruleScores[i];
                if (prob > minimumRuleProbability) {
                    final short leftChildSplit = substates[j];
                    final short rightChildSplit = substates[j + 1];
                    final short parentSplit = substates[j + 2];
                    sb.append(String.format("%s_%d -> %s_%d %s_%d %.10f\n", sParent, parentSplit, sLeftChild,
                            leftChildSplit, sRightChild, rightChildSplit, Math.log(prob)));
                }
            }
            return sb.toString();
        }
    }

    /**
     * Represents the (fractional) observation counts of each split of a coarse binary rule (e.g. for NP -> DT NN, NP_0
     * -> DT_0 NN_0, NP_1 -> DT_0 NN_1, etc.) Packed into a compact parallel array representation.
     * 
     * Accumulated during counting in {@link Grammar#countSplitTree(Tree, double, int, Grammar)} and used to construct a
     * new {@link PackedBinaryRule}.
     */
    public static class PackedBinaryCount implements Serializable {

        private static final long serialVersionUID = 1L;

        public final short unsplitParent;
        public final short unsplitLeftChild;
        public final short unsplitRightChild;

        /** Fractional observation counts for each rule */
        public final double[] ruleCounts;

        /**
         * Parent, left child, and right child substates for each rule. Something of a parallel array to
         * {@link #ruleCounts}, but each entry in {@link #ruleCounts} corresponds to 3 in {@link #substates}, so it is
         * 3x as long.
         */
        public final short[] substates;

        public PackedBinaryCount(final short unsplitParent, final short unsplitLeftChild,
                final short unsplitRightChild, final short[] substates) {
            this.substates = substates;
            this.ruleCounts = new double[substates.length / 3];

            this.unsplitParent = unsplitParent;
            this.unsplitLeftChild = unsplitLeftChild;
            this.unsplitRightChild = unsplitRightChild;
        }

        /**
         * Special-case for counting unsplit trees (a single 'split' for each state).
         */
        public PackedBinaryCount(final short unsplitParent, final short unsplitLeftChild, final short unsplitRightChild) {
            this(unsplitParent, unsplitLeftChild, unsplitRightChild, new short[] { 0, 0, 0 });
        }

        // // TODO Remove after we get rid of un-packed count representation. This is copy-and-paste from
        // PackedBinaryRule
        // public PackedBinaryCount(final short unsplitParent, final short unsplitLeftChild,
        // final short unsplitRightChild, final double[][][] oldRuleCounts) {
        //
        // this.unsplitParent = unsplitParent;
        // this.unsplitLeftChild = unsplitLeftChild;
        // this.unsplitRightChild = unsplitRightChild;
        //
        // // Count the total number of non-0 production probabilities (so we can size the parallel arrays properly)
        // int totalRules = 0;
        // for (short splitLeftChild = 0; splitLeftChild < oldRuleCounts.length; splitLeftChild++) {
        // final double[][] leftChildSplitRules = oldRuleCounts[splitLeftChild];
        // if (leftChildSplitRules == null) {
        // continue;
        // }
        //
        // for (short splitRightChild = 0; splitRightChild < leftChildSplitRules.length; splitRightChild++) {
        // final double[] rightChildSplitRules = leftChildSplitRules[splitRightChild];
        // if (rightChildSplitRules == null) {
        // continue;
        // }
        //
        // for (short splitParent = 0; splitParent < rightChildSplitRules.length; splitParent++) {
        //
        // if (rightChildSplitRules[splitParent] > 0) {
        // totalRules++;
        // }
        // }
        // }
        // }
        //
        // this.ruleCounts = new double[totalRules];
        // this.substates = new short[totalRules * 3];
        //
        // // Populate them in order in the new arrays, sorted by left child, right child, parent
        // int offset = 0;
        // for (short splitLeftChild = 0; splitLeftChild < oldRuleCounts.length; splitLeftChild++) {
        // final double[][] leftChildSplitRules = oldRuleCounts[splitLeftChild];
        // if (leftChildSplitRules == null) {
        // continue;
        // }
        //
        // for (short splitRightChild = 0; splitRightChild < leftChildSplitRules.length; splitRightChild++) {
        // final double[] rightChildSplitRules = leftChildSplitRules[splitRightChild];
        // if (rightChildSplitRules == null) {
        // continue;
        // }
        //
        // for (short splitParent = 0; splitParent < rightChildSplitRules.length; splitParent++) {
        //
        // if (rightChildSplitRules[splitParent] > 0) {
        // this.ruleCounts[offset] = oldRuleCounts[splitLeftChild][splitRightChild][splitParent];
        // final int substateOffset = offset * 3;
        // this.substates[substateOffset] = splitLeftChild;
        // this.substates[substateOffset + 1] = splitRightChild;
        // this.substates[substateOffset + 2] = splitParent;
        // offset++;
        // }
        // }
        // }
        // }
        // // assertEquals(oldRuleCounts);
        // }
        //
        // public void assertEquals(final double[][][] oldRuleCounts) {
        //
        // // Verify the number of populated counts
        // int populatedCounts = 0, oldPopulatedCounts = 0;
        // for (short splitLeftChild = 0; splitLeftChild < oldRuleCounts.length; splitLeftChild++) {
        // if (oldRuleCounts[splitLeftChild] == null) {
        // continue;
        // }
        // for (short splitRightChild = 0; splitRightChild < oldRuleCounts[splitLeftChild].length; splitRightChild++) {
        // if (oldRuleCounts[splitLeftChild][splitRightChild] == null) {
        // continue;
        // }
        // for (short splitParent = 0; splitParent < oldRuleCounts[splitLeftChild][splitRightChild].length;
        // splitParent++) {
        // if (oldRuleCounts[splitLeftChild][splitRightChild][splitParent] > 0) {
        // oldPopulatedCounts++;
        // }
        // }
        // }
        // }
        // for (int i = 0; i < ruleCounts.length; i++) {
        // if (ruleCounts[i] > 0) {
        // populatedCounts++;
        // }
        // }
        // Assert.assertEquals(oldPopulatedCounts, populatedCounts);
        //
        // for (int offset = 0; offset < ruleCounts.length; offset++) {
        // final short splitLeftChild = substates[offset * 3];
        // final short splitRightChild = substates[offset * 3 + 1];
        // final short splitParent = substates[offset * 3 + 2];
        // final double delta = ruleCounts[offset] * 1e-10;
        // Assert.assertEquals(oldRuleCounts[splitLeftChild][splitRightChild][splitParent], ruleCounts[offset],
        // delta);
        // }
        // }
    }

    /**
     * Represents the splits of a coarse unary rule (e.g. for NP -> NN, NP_0 -> NN_0, NP_1 -> NN_1, etc.) Packed into a
     * compact parallel array representation.
     * 
     * After learning production probabilities (and pruning low-probability rules), we pack them into this
     * representation for efficient iteration in {@link ArrayParser#doInsideScores(Tree, boolean, double[][][])}.
     * {@link ArrayParser#doInsideOutsideScores(Tree, boolean)}, and
     * {@link Grammar#countSplitTree(Tree, double, int, Grammar)}.
     */
    public static class PackedUnaryRule implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Production probabilities for each split rule, sorted by by parent split, left child split, right child split */
        public final double[] ruleScores;

        /**
         * Parent, left child, and right child substates for each rule. Something of a parallel array to
         * {@link #ruleScores}, but each entry in {@link #ruleScores} corresponds to 2 in {@link #substates}, so it is
         * 2x as long.
         */
        public final short[] substates;

        public PackedUnaryRule(final double[][] ruleScores, final int parentSplits, final int childSplits) {

            // Count the total number of non-0 production probabilities (so we can size the parallel arrays properly)
            int totalRules = 0;
            for (short splitChild = 0; splitChild < ruleScores.length; splitChild++) {
                final double[] childSplitRules = ruleScores[splitChild];
                if (childSplitRules == null) {
                    continue;
                }

                for (short splitParent = 0; splitParent < childSplitRules.length; splitParent++) {

                    if (childSplitRules[splitParent] > 0) {
                        totalRules++;
                    }
                }
            }

            this.ruleScores = new double[totalRules];
            this.substates = new short[totalRules * 3];

            // Populate them in order in the new arrays, sorted by parent, left child, right child
            int offset = 0;
            for (short splitChild = 0; splitChild < ruleScores.length; splitChild++) {
                final double[] childSplitRules = ruleScores[splitChild];
                if (childSplitRules == null) {
                    continue;
                }

                for (short splitParent = 0; splitParent < childSplitRules.length; splitParent++) {

                    if (childSplitRules[splitParent] > 0) {
                        this.ruleScores[offset] = ruleScores[splitChild][splitParent];
                        final int substateOffset = offset * 2;
                        this.substates[substateOffset] = splitChild;
                        this.substates[substateOffset + 1] = splitParent;
                        offset++;
                    }
                }
            }
        }
    }
}
