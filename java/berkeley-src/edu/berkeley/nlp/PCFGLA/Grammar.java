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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;

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

public class Grammar implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    public boolean[] isGrammarTag;

    /** Records the binary rules (before splitting) */
    private List<BinaryRule>[] binaryRulesWithParent;
    private List<BinaryRule>[] binaryRulesWithLC;
    private List<BinaryRule>[] binaryRulesWithRC;

    /** Used after splitting */
    private BinaryRule[][] splitRulesWithRC;
    private BinaryRule[][] splitRulesWithP;

    private List<UnaryRule>[] unaryRulesWithParent;
    private List<UnaryRule>[] unaryRulesWithC;

    /** the number of states */
    public short numStates;

    /** the number of substates per state */
    public short[] numSubStates;

    private Int2ObjectOpenHashMap<BinaryRule> binaryRuleMap;
    private Int2ObjectOpenHashMap<PackedBinaryRule> packedBinaryRuleMap;
    private Int2ObjectOpenHashMap<UnaryRule> unaryRuleMap;
    private Int2ObjectOpenHashMap<PackedUnaryRule> packedUnaryRuleMap;

    private UnaryCounterTable unaryRuleCounter = null;

    private BinaryCounterTable binaryRuleCounter = null;

    // TODO Replace with a FastUtil implementation
    private CounterMap<Integer, Integer> symbolCounter = new CounterMap<Integer, Integer>();

    protected Numberer tagNumberer;

    private List<UnaryRule>[] closedSumRulesWithParent = null;
    private List<UnaryRule>[] closedSumRulesWithChild = null;

    private List<UnaryRule>[] closedViterbiRulesWithParent = null;
    private List<UnaryRule>[] closedViterbiRulesWithChild = null;

    private UnaryRule[][] closedSumRulesWithP = null;

    private UnaryRule[][] closedViterbiRulesWithP = null;

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

    @SuppressWarnings("unchecked")
    private void init() {
        binaryRuleMap = new Int2ObjectOpenHashMap<BinaryRule>();
        unaryRuleMap = new Int2ObjectOpenHashMap<UnaryRule>();
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

        closedViterbiPaths = new int[numStates][numStates];
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
    private int binaryKey(final short unsplitParent, final short unsplitLeftChild, final short unsplitRightChild) {
        assert unsplitParent < 1024;
        assert unsplitLeftChild < 1024;
        assert unsplitRightChild < 1024;

        return (unsplitParent << 20) | (unsplitLeftChild << 10) | unsplitRightChild;
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
        binaryRulesWithLC[br.leftChildState].add(br);
        binaryRulesWithRC[br.rightChildState].add(br);
        binaryRuleMap.put(binaryKey(br.parentState, br.leftChildState, br.rightChildState), br);
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
            final UnaryRule[] unaries = this.getClosedViterbiUnaryRulesByParent(state);
            for (int r = 0; r < unaries.length; r++) {
                final UnaryRule ur = unaries[r];
                out.print(ur.toString());
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
        }

        normalize();
        smooth(); // this also adds the rules to the proper arrays
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

    public void smooth() {
        smoother.smooth(unaryRuleCounter, binaryRuleCounter);
        normalize();

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

    private void tallyStateSetTree(final Tree<StateSet> tree, double treeInsideScore, final double tree_scale,
            final Grammar old_grammar) {

        if (tree.isLeaf() || tree.isPreTerminal()) {
            return;
        }

        final List<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short unsplitParent = parent.getState();
        final int nParentSubStates = numSubStates[unsplitParent];

        switch (children.size()) {
        case 0:
            // This is a preterminal node; nothing to do
            break;

        case 1:
            final StateSet child = children.get(0).label();
            final short unsplitChild = child.getState();
            final int nChildSubStates = numSubStates[unsplitChild];
            final UnaryRule urule = new UnaryRule(unsplitParent, unsplitChild);
            // final double[][] oldUScores = old_grammar.getUnaryScore(unsplitParent, unsplitChild);

            double[][] ucounts = unaryRuleCounter.getCount(urule);
            if (ucounts == null) {
                ucounts = new double[nChildSubStates][];
            }
            double scalingFactor = ScalingTools.calcScaleFactor(parent.getOScale() + child.getIScale() - tree_scale);

            // for (short i = 0; i < nChildSubStates; i++) {
            // if (oldUScores[i] == null)
            // continue;
            // final double childInsideScore = child.getIScore(i);
            // if (childInsideScore == 0)
            // continue;
            // if (ucounts[i] == null)
            // ucounts[i] = new double[nParentSubStates];
            // for (short j = 0; j < nParentSubStates; j++) {
            // final double parentOutsideScore = parent.getOScore(j); // Parent outside score
            // if (parentOutsideScore == 0)
            // continue;
            // final double ruleScore = oldUScores[i][j];
            // if (ruleScore == 0)
            // continue;
            // if (treeInsideScore == 0)
            // treeInsideScore = 1;
            // final double logRuleCount = (ruleScore * childInsideScore / treeInsideScore) * scalingFactor
            // * parentOutsideScore;
            // ucounts[i][j] += logRuleCount;
            // }
            // }

            final Grammar.PackedUnaryRule packedUnaryRule = old_grammar.getPackedUnaryScores(unsplitParent,
                    unsplitChild);
            // Iterate through all splits of the rule. Most will have non-0 child and parent probability (since the
            // rule currently exists in the grammar), and this iteration order is very efficient
            for (int i = 0, j = 0; i < packedUnaryRule.ruleScores.length; i++, j += 2) {
                final short childSplit = packedUnaryRule.substates[j];
                final short parentSplit = packedUnaryRule.substates[j + 1];

                final double parentOutsideScore = parent.getOScore(parentSplit);
                final double childInsideScore = child.getIScore(childSplit);

                if (treeInsideScore == 0) {
                    treeInsideScore = 1;
                }
                final double scaledRuleCount = (packedUnaryRule.ruleScores[i] * childInsideScore / treeInsideScore)
                        * scalingFactor * parentOutsideScore;

                if (ucounts[childSplit] == null) {
                    ucounts[childSplit] = new double[nParentSubStates];
                }
                ucounts[childSplit][parentSplit] += scaledRuleCount;
            }

            unaryRuleCounter.setCount(urule, ucounts);
            break;

        case 2:
            final StateSet leftChild = children.get(0).label();
            final short unsplitLeftChild = leftChild.getState();
            final StateSet rightChild = children.get(1).label();
            final short unsplitRightChild = rightChild.getState();
            final int nLeftChildSubStates = numSubStates[unsplitLeftChild];
            final int nRightChildSubStates = numSubStates[unsplitRightChild];

            scalingFactor = ScalingTools.calcScaleFactor(parent.getOScale() + leftChild.getIScale()
                    + rightChild.getIScale() - tree_scale);

            final BinaryRule brule = new BinaryRule(unsplitParent, unsplitLeftChild, unsplitRightChild);
            double[][][] bcounts = binaryRuleCounter.getCount(brule);
            if (bcounts == null) {
                bcounts = new double[nLeftChildSubStates][nRightChildSubStates][];
            }
            // final double[][][] bcountsClone = bcounts.clone();

            // double[][][] oldBScores = old_grammar.getBinaryScore(unsplitParent, unsplitLeftChild, unsplitRightChild);
            final Grammar.PackedBinaryRule packedBinaryRule = old_grammar.getPackedBinaryScores(unsplitParent,
                    unsplitLeftChild, unsplitRightChild);

            // Iterate through all splits of the rule. Most will have non-0 child and parent probability (since the
            // rule currently exists in the grammar), and this iteration order is very efficient
            for (int i = 0, j = 0; i < packedBinaryRule.ruleScores.length; i++, j += 3) {
                final short leftChildSplit = packedBinaryRule.substates[j];
                final short rightChildSplit = packedBinaryRule.substates[j + 1];
                final short parentSplit = packedBinaryRule.substates[j + 2];

                final double leftChildInsideScore = leftChild.getIScore(leftChildSplit);
                final double rightChildInsideScore = rightChild.getIScore(rightChildSplit);

                final double parentOutsideScore = parent.getOScore(parentSplit); // Parent outside

                if (treeInsideScore == 0) {
                    treeInsideScore = 1;
                }
                final double scaledRuleCount = (packedBinaryRule.ruleScores[i] * leftChildInsideScore / treeInsideScore)
                        * rightChildInsideScore * scalingFactor * parentOutsideScore;

                if (bcounts[leftChildSplit][rightChildSplit] == null) {
                    bcounts[leftChildSplit][rightChildSplit] = new double[nParentSubStates];
                }
                bcounts[leftChildSplit][rightChildSplit][parentSplit] += scaledRuleCount;
            }

            // double[][][] oldBScores = old_grammar.getBinaryScore(unsplitParent, unsplitLeftChild, unsplitRightChild);
            // if (oldBScores == null) {
            // // rule was not in the grammar
            // oldBScores = new double[nLeftChildSubStates][nRightChildSubStates][nParentSubStates];
            // ArrayUtil.fill(oldBScores, 1.0);
            // }

            // //
            // for (short i = 0; i < nLeftChildSubStates; i++) {
            // final double leftChildInsideScore = leftChild.getIScore(i);
            // if (leftChildInsideScore == 0)
            // continue;
            // for (short j = 0; j < nRightChildSubStates; j++) {
            // if (oldBScores[i][j] == null)
            // continue;
            // final double rightChildInsideScore = rightChild.getIScore(j);
            // if (rightChildInsideScore == 0)
            // continue;
            // // allocate parent array
            // if (bcountsClone[i][j] == null)
            // bcountsClone[i][j] = new double[nParentSubStates];
            // for (short k = 0; k < nParentSubStates; k++) {
            // final double parentOutsideScore = parent.getOScore(k); // Parent outside
            // // score
            // if (parentOutsideScore == 0)
            // continue;
            // final double ruleScore = oldBScores[i][j][k];
            // if (ruleScore == 0)
            // continue;
            // if (treeInsideScore == 0)
            // treeInsideScore = 1;
            // final double scaledRuleCount = (ruleScore * leftChildInsideScore / treeInsideScore)
            // * rightChildInsideScore * scalingFactor * parentOutsideScore;
            // /*
            // * if (logRuleCount == 0) { System.out.println("rS "+rS+", lcIS "
            // * +lcIS+", rcIS "+rcIS+", tree_score "+tree_score+
            // * ", scalingFactor "+scalingFactor+", pOS "+pOS); System.out.println("Possibly underflow?"); //
            // * logRuleCount = Double.MIN_VALUE; }
            // */
            // bcountsClone[i][j][k] += scaledRuleCount;
            // }
            // }
            // }
            // //

            // JUnit.assertArrayEquals(bcountsClone, bcounts, 1e-15);

            binaryRuleCounter.setCount(brule, bcounts);
            break;

        default:
            throw new IllegalArgumentException("Malformed tree: more than two children");
        }

        for (final Tree<StateSet> child : children) {
            tallyStateSetTree(child, treeInsideScore, tree_scale, old_grammar);
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
        closedViterbiRulesWithP = new UnaryRule[numStates][];

        for (int i = 0; i < numStates; i++) {
            closedSumRulesWithP[i] = closedSumRulesWithParent[i].toArray(new UnaryRule[0]);
            closedViterbiRulesWithP[i] = closedViterbiRulesWithParent[i].toArray(new UnaryRule[0]);
        }
    }

    // Used by toString()
    public UnaryRule[] getClosedSumUnaryRulesByParent(final int state) {
        if (closedSumRulesWithP == null) {
            makeCRArrays();
        }
        if (state >= closedSumRulesWithP.length) {
            return new UnaryRule[0];
        }
        return closedSumRulesWithP[state];
    }

    // Used by writeData(), and thus by WriteGrammarToTextFile. Is there any practical difference between this and
    // getClosedSumUnaryRulesByParent?
    public UnaryRule[] getClosedViterbiUnaryRulesByParent(final int state) {
        if (closedViterbiRulesWithP == null) {
            makeCRArrays();
        }
        if (state >= closedViterbiRulesWithP.length) {
            return new UnaryRule[0];
        }
        return closedViterbiRulesWithP[state];
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
    }

    /**
     * Populates the "splitRules" accessor lists using the existing rule lists. If the state is synthetic, these lists
     * contain all rules for the state. If the state is NOT synthetic, these lists contain only the rules in which both
     * children are not synthetic.
     * <p>
     * <i>This method must be called before the grammar is used, either after training or deserializing grammar.</i>
     */
    public void splitRules() {

        if (binaryRulesWithParent == null) {
            return;
        }

        splitRulesWithP = new BinaryRule[numStates][];
        splitRulesWithRC = new BinaryRule[numStates][];

        for (int state = 0; state < numStates; state++) {
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

        final PackedUnaryRule packedUnaryRule = packedUnaryRuleMap.get(unaryKey(unsplitParent, unsplitChild));
        return packedUnaryRule;
    }

    public PackedBinaryRule getPackedBinaryScores(final short unsplitParent, final short unsplitLeftChild,
            final short unsplitRightChild) {

        if (packedBinaryRuleMap == null) {

            // Populate the packed representation of all binary rules
            packedBinaryRuleMap = new Int2ObjectOpenHashMap<Grammar.PackedBinaryRule>();

            for (final int binaryKey : binaryRuleMap.keySet()) {
                final BinaryRule rule = binaryRuleMap.get(binaryKey);
                packedBinaryRuleMap.put(binaryKey, new PackedBinaryRule(rule.getScores2(), numSubStates[unsplitParent],
                        numSubStates[unsplitLeftChild], numSubStates[unsplitRightChild]));
            }
        }

        final PackedBinaryRule packedBinaryRule = packedBinaryRuleMap.get(binaryKey(unsplitParent, unsplitLeftChild,
                unsplitRightChild));
        // packedBinaryRule.assertEquals(getBinaryScore(unsplitParent, unsplitLeftChild, unsplitRightChild));
        return packedBinaryRule;
    }

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
        final Grammar newGrammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);
        final Random random = GrammarTrainer.RANDOM;

        for (final BinaryRule oldRule : binaryRuleMap.values()) {
            newGrammar.addBinary(oldRule.splitRule(numSubStates, newNumSubStates, random, randomness));
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

        for (final BinaryRule oldRule : binaryRuleMap.values()) {

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
                if (scores[ci] == null) {
                    continue;
                }
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

        for (int pS = 0; pS < grammar.numSubStates.length; pS++) {
            if (!sawPS[pS]) {
                continue;
            }
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
            nUnary += getClosedViterbiUnaryRulesByParent(state).length;
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
                ruleStrings.add(r.toString(minimumRuleProbability));
            }
        }

        for (int state = 0; state < numStates; state++) {
            final UnaryRule[] unaries = this.getClosedViterbiUnaryRulesByParent(state);

            for (int r = 0; r < unaries.length; r++) {
                final UnaryRule ur = unaries[r];
                ruleStrings.add(ur.toString(minimumRuleProbability));
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
     * {@link Grammar#tallyStateSetTree(Tree, double, double, Grammar)}.
     */
    public static class PackedBinaryRule implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Production probabilities for each split rule, sorted by by parent split, left child split, right child split */
        public final double[] ruleScores;

        /**
         * Parent, left child, and right child substates for each rule. Something of a parallel array to
         * {@link #ruleScores}, but each entry in {@link #ruleScores} corresponds to 3 in {@link #substates}, so it is
         * 3x as long
         */
        public final short[] substates;

        public PackedBinaryRule(final double[][][] ruleScores, final int parentSplits, final int leftChildSplits,
                final int rightChildSplits) {

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

            // Populate them in order in the new arrays, sorted by parent, left child, right child
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

            //
            // for (int i = 0, j = 0; i < this.ruleScores.length; i++, j += 3) {
            // final short parentSplit = substates[j];
            // final short leftChildSplit = substates[j + 1];
            // final short rightChildSplit = substates[j + 2];
            //
            // assert ruleScores[leftChildSplit][rightChildSplit][parentSplit] == this.ruleScores[i];
            // }
            //
        }

        public void assertEquals(final double[][][] oldRuleScores) {

            // Verify the number of populated scores
            int count = 0;
            for (short splitLeftChild = 0; splitLeftChild < oldRuleScores.length; splitLeftChild++) {
                if (oldRuleScores[splitLeftChild] == null) {
                    continue;
                }
                for (short splitRightChild = 0; splitRightChild < oldRuleScores[splitLeftChild].length; splitRightChild++) {
                    if (oldRuleScores[splitLeftChild][splitRightChild] == null) {
                        continue;
                    }
                    for (short splitParent = 0; splitParent < oldRuleScores[splitLeftChild][splitRightChild].length; splitParent++) {
                        if (oldRuleScores[splitLeftChild][splitRightChild][splitParent] > 0) {
                            count++;
                        }
                    }
                }
            }
            Assert.assertEquals(count, ruleScores.length);

            for (int offset = 0; offset < ruleScores.length; offset++) {
                final short splitLeftChild = substates[offset * 3];
                final short splitRightChild = substates[offset * 3 + 1];
                final short splitParent = substates[offset * 3 + 2];
                final double delta = ruleScores[offset] * 1e-10;
                Assert.assertEquals(oldRuleScores[splitLeftChild][splitRightChild][splitParent], ruleScores[offset],
                        delta);
            }
        }
    }

    /**
     * Represents the splits of a coarse unary rule (e.g. for NP -> NN, NP_0 -> NN_0, NP_1 -> NN_1, etc.) Packed into a
     * compact parallel array representation.
     * 
     * After learning production probabilities (and pruning low-probability rules), we pack them into this
     * representation for efficient iteration in {@link ArrayParser#doInsideScores(Tree, boolean, double[][][])}.
     * {@link ArrayParser#doInsideOutsideScores(Tree, boolean)}, and
     * {@link Grammar#tallyStateSetTree(Tree, double, double, Grammar)}.
     */
    public static class PackedUnaryRule implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Production probabilities for each split rule, sorted by by parent split, left child split, right child split */
        public final double[] ruleScores;

        /**
         * Parent, left child, and right child substates for each rule. Something of a parallel array to
         * {@link #ruleScores}, but each entry in {@link #ruleScores} corresponds to 2 in {@link #substates}, so it is
         * 2x as long
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
