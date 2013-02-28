package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;
import edu.berkeley.nlp.util.ScalingTools;

/**
 * Simple mixture parser.
 */
public class ArrayParser {

    protected final short ZERO = 0, ONE = 1;

    protected Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

    // inside scores
    protected double[][][] iScore; // start idx, end idx, state -> logProb

    // outside scores
    protected double[][][] oScore; // start idx, end idx, state -> logProb

    protected int[][] narrowLExtent = null; // the rightmost left extent of
                                            // state
                                            // s ending at position i

    protected int[][] wideLExtent = null; // the leftmost left extent of state s
                                          // ending at position i

    protected int[][] narrowRExtent = null; // the leftmost right extent of
                                            // state
                                            // s starting at position i

    protected int[][] wideRExtent = null; // the rightmost right extent of state
                                          // s
                                          // starting at position i

    protected short length;

    protected int arraySize = 0;

    protected int myMaxLength = 200;

    Lexicon lexicon;

    int numStates;
    int maxNSubStates;
    int[] idxC;
    double[] scoresToAdd;
    int touchedRules;
    double[] tmpCountsArray;
    Grammar grammar;
    int[] stateClass;

    public ArrayParser() {
    }

    public ArrayParser(final Grammar gr, final Lexicon lex) {
        this.touchedRules = 0;
        this.grammar = gr;
        this.lexicon = lex;
        this.tagNumberer = Numberer.getGlobalNumberer("tags");
        this.numStates = gr.numStates;
        this.maxNSubStates = maxSubStates(gr);
        this.idxC = new int[maxNSubStates];
        this.scoresToAdd = new double[maxNSubStates];
        tmpCountsArray = new double[scoresToAdd.length * scoresToAdd.length * scoresToAdd.length];

        // System.out.println("This grammar has " + numStates
        // + " states and a total of " + grammar.totalSubStates() +
        // " substates.");
    }

    // belongs in the grammar but i didnt want to change the signature for
    // now...
    public int maxSubStates(final Grammar grammar) {
        int max = 0;
        for (int i = 0; i < numStates; i++) {
            if (grammar.numSubStates[i] > max)
                max = grammar.numSubStates[i];
        }
        return max;
    }

    public boolean hasParse() {
        if (length > arraySize) {
            return false;
        }
        return (iScore[0][length][tagNumberer.number("ROOT")] > Double.NEGATIVE_INFINITY);
    }

    void initializeArrays() {
        if (length > arraySize) {
            if (length > myMaxLength) {
                throw new OutOfMemoryError("Refusal to create such large arrays.");
            }

            try {
                createArrays(length + 1);
            } catch (final OutOfMemoryError e) {
                myMaxLength = length;
                if (arraySize > 0) {
                    try {
                        createArrays(arraySize);
                    } catch (final OutOfMemoryError e2) {
                        throw new RuntimeException("CANNOT EVEN CREATE ARRAYS OF ORIGINAL SIZE!!!");
                    }
                }
                throw e;
            }
            arraySize = length + 1;
        }
        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                Arrays.fill(iScore[start][end], Double.NEGATIVE_INFINITY);
                Arrays.fill(oScore[start][end], Double.NEGATIVE_INFINITY);
            }
        }
        for (int loc = 0; loc <= length; loc++) {
            Arrays.fill(narrowLExtent[loc], -1); // the rightmost left with
                                                 // state s
                                                 // ending at i that we can
                                                 // get is
                                                 // the beginning
            Arrays.fill(wideLExtent[loc], length + 1); // the leftmost left with
                                                       // state s ending at i
                                                       // that we
                                                       // can get is the end
            Arrays.fill(narrowRExtent[loc], length + 1); // the leftmost right
                                                         // with
                                                         // state s starting
                                                         // at i
                                                         // that we can get
                                                         // is the
                                                         // end
            Arrays.fill(wideRExtent[loc], -1); // the rightmost right with state
                                               // s
                                               // starting at i that we can get
                                               // is
                                               // the beginning
        }

    }

    void initializeChart(final List<String> sentence, final boolean noSmoothing) {
        // for simplicity the lexicon will store words and tags as strings,
        // while the grammar will be using integers -> Numberer()
        int start = 0;
        int end = start + 1;
        for (final String word : sentence) {
            end = start + 1;
            // for (short tag : lexicon.getAllTags()) {
            for (short tag = 0; tag < numStates; tag++) {
                if (grammar.isGrammarTag[tag])
                    continue;
                final double prob = lexicon.score(word, tag, start, noSmoothing, false)[0];
                iScore[start][end][tag] = prob;
                narrowRExtent[start][tag] = end;
                narrowLExtent[end][tag] = start;
                wideRExtent[start][tag] = end;
                wideLExtent[end][tag] = start;
                /*
                 * UnaryRule[] unaries = grammar.getClosedUnaryRulesByChild(state); for (int r = 0; r < unaries.length;
                 * r++) { UnaryRule ur = unaries[r]; int parentState = ur.parent; double pS = (double) ur.score; double
                 * tot = prob + pS; if (tot > iScore[start][end][parentState]) { iScore[start][end][parentState] = tot;
                 * narrowRExtent[start][parentState] = end; narrowLExtent[end][parentState] = start;
                 * wideRExtent[start][parentState] = end; wideLExtent[end][parentState] = start; } }
                 */
            }
            // scaleIScores(start,end,0);
            start++;
        }
    }

    /**
     * Calculate the inside scores, P(words_i,j|nonterminal_i,j) of a tree given the string if words it should parse to.
     * 
     * @param tree
     * @param sentence
     */
    void doInsideScores(final Tree<StateSet> tree, final boolean noSmoothing, final double[][][] spanScores) {

        if (tree.isLeaf()) {
            return;
        }

        final List<Tree<StateSet>> children = tree.getChildren();
        for (final Tree<StateSet> child : children) {
            if (!child.isLeaf())
                doInsideScores(child, noSmoothing, spanScores);
        }
        final StateSet parent = tree.getLabel();
        final short pState = parent.getState();
        final int nParentStates = parent.numSubStates();
        if (tree.isPreTerminal()) {
            // Plays a role similar to initializeChart()
            final StateSet wordStateSet = tree.getChildren().get(0).getLabel();
            final double[] lexiconScores = lexicon.score(wordStateSet, pState, noSmoothing, false);
            if (lexiconScores.length != nParentStates) {
                System.out.println("Have more scores than substates!" + lexiconScores.length + " " + nParentStates);// truncate
                                                                                                                    // the
                                                                                                                    // array
            }
            parent.setIScores(lexiconScores);
            parent.scaleIScores(0);
        } else {
            switch (children.size()) {
            case 0:
                break;
            case 1:
                final StateSet child = children.get(0).getLabel();
                final short cState = child.getState();
                final int nChildStates = child.numSubStates();
                final double[][] uscores = grammar.getUnaryScore(pState, cState);
                final double[] iScores = new double[nParentStates];
                boolean foundOne = false;
                for (int j = 0; j < nChildStates; j++) {
                    if (uscores[j] != null) { // check whether one of the
                                              // parents can produce this
                                              // child
                        final double cS = child.getIScore(j);
                        if (cS == 0)
                            continue;
                        for (int i = 0; i < nParentStates; i++) {
                            final double rS = uscores[j][i]; // rule score
                            if (rS == 0)
                                continue;
                            final double res = rS * cS;
                            /*
                             * if (res == 0) { System.out.println("Prevented an underflow: rS " +rS+" cS "+cS); res =
                             * Double.MIN_VALUE; }
                             */
                            iScores[i] += res;
                            foundOne = true;
                        }
                    }
                }
                // if (debugOutput && !foundOne) {
                // System.out.println("iscore reached zero!");
                // System.out.println(grammar.getUnaryRule(pState, cState));
                // System.out.println(Arrays.toString(iScores));
                // System.out.println(ArrayUtil.toString(uscores));
                // System.out.println(Arrays.toString(child.getIScores()));
                // }
                parent.setIScores(iScores);
                parent.scaleIScores(child.getIScale());
                break;
            case 2:
                final StateSet leftChild = children.get(0).getLabel();
                final StateSet rightChild = children.get(1).getLabel();
                final int nLeftChildStates = leftChild.numSubStates();
                final int nRightChildStates = rightChild.numSubStates();
                final short lState = leftChild.getState();
                final short rState = rightChild.getState();
                final double[][][] bscores = grammar.getBinaryScore(pState, lState, rState);
                final double[] iScores2 = new double[nParentStates];
                boolean foundOne2 = false;
                for (int j = 0; j < nLeftChildStates; j++) {
                    final double lcS = leftChild.getIScore(j);
                    if (lcS == 0)
                        continue;
                    for (int k = 0; k < nRightChildStates; k++) {
                        final double rcS = rightChild.getIScore(k);
                        if (rcS == 0)
                            continue;
                        if (bscores[j][k] != null) { // check whether one of the
                                                     // parents can produce
                                                     // these kids
                            for (int i = 0; i < nParentStates; i++) {
                                final double rS = bscores[j][k][i];
                                if (rS == 0)
                                    continue;
                                final double res = rS * lcS * rcS;
                                /*
                                 * if (res == 0) { System.out.println("Prevented an underflow: rS "
                                 * +rS+" lcS "+lcS+" rcS "+rcS); res = Double.MIN_VALUE; }
                                 */
                                iScores2[i] += res;
                                foundOne2 = true;
                            }
                        }
                    }
                }
                if (spanScores != null) {
                    for (int i = 0; i < nParentStates; i++) {
                        iScores2[i] *= spanScores[parent.from][parent.to][stateClass[pState]];
                    }
                }

                // if (!foundOne2)
                // System.out.println("Did not find a way to build binary transition from "+pState+" to "+lState+" and "+rState+" "+ArrayUtil.toString(bscores));
                // if (debugOutput && !foundOne2) {
                // System.out.println("iscore reached zero!");
                // System.out.println(grammar.getBinaryRule(pState, lState, rState));
                // System.out.println(Arrays.toString(iScores2));
                // System.out.println(Arrays.toString(bscores));
                // System.out.println(Arrays.toString(leftChild.getIScores()));
                // System.out.println(Arrays.toString(rightChild.getIScores()));
                // }
                parent.setIScores(iScores2);
                parent.scaleIScores(leftChild.getIScale() + rightChild.getIScale());
                break;
            default:
                throw new Error("Malformed tree: more than two children");
            }
        }
    }

    /**
     * Set the outside score of the root node to P=1.
     * 
     * @param tree
     */
    void setRootOutsideScore(final Tree<StateSet> tree) {
        tree.getLabel().setOScore(0, 1);
        tree.getLabel().setOScale(0);
    }

    /**
     * Calculate the outside scores of a tree; that is, P(nonterminal_i,j|words_0,i; words_j,end). It is calculate from
     * the inside scores of the tree.
     * 
     * <p>
     * Note: when calling this, call setRootOutsideScore() first.
     * 
     * @param tree
     */
    void doOutsideScores(final Tree<StateSet> tree, boolean unaryAbove, final double[][][] spanScores) {
        if (tree.isLeaf())
            return;
        final List<Tree<StateSet>> children = tree.getChildren();
        final StateSet parent = tree.getLabel();
        final short pState = parent.getState();
        final int nParentStates = parent.numSubStates();
        // this sets the outside scores for the children
        if (tree.isPreTerminal()) {

        } else {
            final double[] parentScores = parent.getOScores();
            if (spanScores != null && !unaryAbove) {
                for (int i = 0; i < nParentStates; i++) {
                    parentScores[i] *= spanScores[parent.from][parent.to][stateClass[pState]];
                }
            }
            switch (children.size()) {
            case 0:
                // Nothing to do
                break;
            case 1:
                final StateSet child = children.get(0).getLabel();
                final short cState = child.getState();
                final int nChildStates = child.numSubStates();
                // UnaryRule uR = new UnaryRule(pState,cState);
                final double[][] uscores = grammar.getUnaryScore(pState, cState);
                final double[] oScores = new double[nChildStates];
                for (int j = 0; j < nChildStates; j++) {
                    if (uscores[j] != null) {
                        double childScore = 0;
                        for (int i = 0; i < nParentStates; i++) {
                            final double pS = parentScores[i];
                            if (pS == 0)
                                continue;
                            final double rS = uscores[j][i]; // rule score
                            if (rS == 0)
                                continue;
                            childScore += pS * rS;
                        }
                        oScores[j] = childScore;
                    }
                }
                child.setOScores(oScores);
                child.scaleOScores(parent.getOScale());
                unaryAbove = true;
                break;
            case 2:
                final StateSet leftChild = children.get(0).getLabel();
                final StateSet rightChild = children.get(1).getLabel();
                final int nLeftChildStates = leftChild.numSubStates();
                final int nRightChildStates = rightChild.numSubStates();
                final short lState = leftChild.getState();
                final short rState = rightChild.getState();
                // double[] leftScoresToAdd -> use childScores array instead =
                // new double[nRightChildStates * nParentStates];
                // double[][] rightScoresToAdd -> use binaryScores array instead
                // = new double[nRightChildStates][nLeftChildStates *
                // nParentStates];
                final double[][][] bscores = grammar.getBinaryScore(pState, lState, rState);
                final double[] lOScores = new double[nLeftChildStates];
                final double[] rOScores = new double[nRightChildStates];
                for (int j = 0; j < nLeftChildStates; j++) {
                    final double lcS = leftChild.getIScore(j);
                    double leftScore = 0;
                    for (int k = 0; k < nRightChildStates; k++) {
                        final double rcS = rightChild.getIScore(k);
                        if (bscores[j][k] != null) {
                            for (int i = 0; i < nParentStates; i++) {
                                final double pS = parentScores[i];
                                if (pS == 0)
                                    continue;
                                final double rS = bscores[j][k][i];
                                if (rS == 0)
                                    continue;
                                leftScore += pS * rS * rcS;
                                rOScores[k] += pS * rS * lcS;
                            }
                        }
                        lOScores[j] = leftScore;
                    }
                }
                leftChild.setOScores(lOScores);
                leftChild.scaleOScores(parent.getOScale() + rightChild.getIScale());
                rightChild.setOScores(rOScores);
                rightChild.scaleOScores(parent.getOScale() + leftChild.getIScale());
                unaryAbove = false;
                break;
            default:
                throw new Error("Malformed tree: more than two children");
            }
            for (final Tree<StateSet> child : children) {
                doOutsideScores(child, unaryAbove, spanScores);
            }
        }
    }

    public double doInsideOutsideScores(final Tree<StateSet> tree, final boolean noSmoothing,
            final double[][][] spanScores) {
        doInsideScores(tree, noSmoothing, spanScores);
        setRootOutsideScore(tree);
        doOutsideScores(tree, false, spanScores);
        final double tree_score = tree.getLabel().getIScore(0);
        final int tree_scale = tree.getLabel().getIScale();
        return Math.log(tree_score) + (ScalingTools.LOGSCALE * tree_scale);
    }

    public void doInsideOutsideScores(final Tree<StateSet> tree, final boolean noSmoothing) {
        doInsideScores(tree, noSmoothing, null);
        setRootOutsideScore(tree);
        doOutsideScores(tree, false, null);
    }

    private void createArrays(final int length) {

        // zero out some stuff first in case we recently ran out of memory and
        // are
        // reallocating
        clearArrays();

        // allocate just the parts of iScore and oScore used (end > start, etc.)
        // System.out.println("initializing iScore arrays with length " + length
        // + "
        // and numStates " + numStates);
        iScore = new double[length][length + 1][];
        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                iScore[start][end] = new double[numStates];
            }
        }
        // System.out.println("finished initializing iScore arrays");
        // System.out.println("initializing oScore arrays with length " + length
        // + "
        // and numStates " + numStates);
        oScore = new double[length][length + 1][];
        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                oScore[start][end] = new double[numStates];
            }
        }
        // System.out.println("finished initializing oScore arrays");

        // iPossibleByL = new boolean[length + 1][numStates];
        // iPossibleByR = new boolean[length + 1][numStates];
        narrowRExtent = new int[length + 1][numStates];
        wideRExtent = new int[length + 1][numStates];
        narrowLExtent = new int[length + 1][numStates];
        wideLExtent = new int[length + 1][numStates];
        /*
         * (op.doDep) { oPossibleByL = new boolean[length + 1][numStates]; oPossibleByR = new boolean[length +
         * 1][numStates];
         * 
         * oFilteredStart = new boolean[length + 1][numStates]; oFilteredEnd = new boolean[length + 1][numStates]; }
         * tags = new boolean[length + 1][numTags];
         * 
         * if (Test.lengthNormalization) { wordsInSpan = new int[length + 1][length + 1][]; for (int start = 0; start <=
         * length; start++) { for (int end = start + 1; end <= length; end++) { wordsInSpan[start][end] = new
         * int[numStates]; } } }
         */// System.out.println("ExhaustivePCFGParser constructor finished.");
    }

    protected void clearArrays() {
        iScore = oScore = null;
        // iPossibleByL = iPossibleByR = oFilteredEnd = oFilteredStart =
        // oPossibleByL = oPossibleByR = tags = null;
        narrowRExtent = wideRExtent = narrowLExtent = wideLExtent = null;
    }

    protected void restoreUnaries(final Tree<String> t) {
        // System.out.println("In restoreUnaries...");
        for (final Iterator nodeI = t.subTreeList().iterator(); nodeI.hasNext();) {
            final Tree<String> node = (Tree<String>) nodeI.next();
            // System.out.println("Doing node: "+node.getLabel());
            if (node.isLeaf() || node.isPreTerminal() || node.getChildren().size() != 1) {
                // System.out.println("Skipping node: "+node.getLabel());
                continue;
            }
            // System.out.println("Not skipping node: "+node.getLabel());
            Tree<String> parent = node;
            // Tree<String> child = node.getChildren().get(0);
            final short pLabel = (short) tagNumberer.number(parent.getLabel());
            final short cLabel = (short) tagNumberer.number(node.getChildren().get(0).getLabel());
            // List path =
            // grammar.getBestPath(stateNumberer.number(parent.getLabel().value()),
            // stateNumberer.number(child.label().value().toString()));
            // if (grammar.getUnaryScore(new UnaryRule(pLabel,cLabel))[0][0] ==
            // 0){
            // continue; }// means the rule was already in grammar
            final List<short[]> path = grammar.getBestViterbiPath(pLabel, (short) 0, cLabel, (short) 0);
            // System.out.println("Got path for "+pLabel + " to " + cLabel +
            // " via " +
            // path);
            for (int pos = 1; pos < path.size() - 1; pos++) {
                final int tmp = path.get(pos)[0];
                final int interState = tmp;
                final Tree<String> intermediate = new Tree<String>(tagNumberer.symbol(interState), parent.getChildren());
                final List<Tree<String>> children = new ArrayList<Tree<String>>();
                children.add(intermediate);
                parent.setChildren(children);
                parent = intermediate;
            }
        }
    }

    private static final double TOL = 1e-5;

    protected boolean matches(final double x, final double y) {
        return (Math.abs(x - y) / (Math.abs(x) + Math.abs(y) + 1e-10) < TOL);
    }

    /**
     * @param stateSetTree
     * @return
     */
    public void doViterbiInsideScores(final Tree<StateSet> tree) {
        if (tree.isLeaf()) {
            return;
        }
        final List<Tree<StateSet>> children = tree.getChildren();
        for (final Tree<StateSet> child : children) {
            if (tree.isLeaf())
                continue;
            doViterbiInsideScores(child);// newChildren.add(getBestViterbiDerivation(child));
        }
        final StateSet parent = tree.getLabel();
        final short pState = parent.getState();
        final int nParentStates = grammar.numSubStates[pState];// parent.numSubStates();
        double[] iScores = new double[nParentStates];
        if (tree.isPreTerminal()) {
            // Plays a role similar to initializeChart()
            final String word = tree.getChildren().get(0).getLabel().getWord();
            final int pos = tree.getChildren().get(0).getLabel().from;
            iScores = lexicon.score(word, pState, pos, false, false);
            // parent.scaleIScores(0);
        } else {
            Arrays.fill(iScores, Double.NEGATIVE_INFINITY);
            switch (children.size()) {
            case 0:
                break;
            case 1:
                final StateSet child = children.get(0).getLabel();
                final short cState = child.getState();
                final int nChildStates = child.numSubStates();
                final double[][] uscores = grammar.getUnaryScore(pState, cState);
                for (int j = 0; j < nChildStates; j++) {
                    if (uscores[j] != null) { // check whether one of the
                                              // parents can produce this
                                              // child
                        final double cS = child.getIScore(j);
                        if (cS == Double.NEGATIVE_INFINITY)
                            continue;
                        for (int i = 0; i < nParentStates; i++) {
                            final double rS = uscores[j][i]; // rule score
                            if (rS == Double.NEGATIVE_INFINITY)
                                continue;
                            final double res = rS + cS;
                            iScores[i] = Math.max(iScores[i], res);
                        }
                    }
                }
                // parent.scaleIScores(child.getIScale());
                break;
            case 2:
                final StateSet leftChild = children.get(0).getLabel();
                final StateSet rightChild = children.get(1).getLabel();
                final int nLeftChildStates = grammar.numSubStates[leftChild.getState()];// leftChild.numSubStates();
                final int nRightChildStates = grammar.numSubStates[rightChild.getState()];// rightChild.numSubStates();
                final short lState = leftChild.getState();
                final short rState = rightChild.getState();
                double[][][] bscores = grammar.getBinaryScore(pState, lState, rState);
                for (final BinaryRule br : grammar.splitRulesWithP(pState)) {
                    if (br.leftChildState != lState)
                        continue;
                    if (br.rightChildState != rState)
                        continue;
                    bscores = br.getScores2();
                }

                for (int j = 0; j < nLeftChildStates; j++) {
                    final double lcS = leftChild.getIScore(j);
                    if (lcS == Double.NEGATIVE_INFINITY)
                        continue;
                    for (int k = 0; k < nRightChildStates; k++) {
                        final double rcS = rightChild.getIScore(k);
                        if (rcS == Double.NEGATIVE_INFINITY)
                            continue;
                        if (bscores[j][k] != null) { // check whether one of the
                                                     // parents can produce
                                                     // these kids
                            for (int i = 0; i < nParentStates; i++) {
                                final double rS = bscores[j][k][i];
                                if (rS == Double.NEGATIVE_INFINITY)
                                    continue;
                                final double res = rS + lcS + rcS;
                                iScores[i] = Math.max(iScores[i], res);
                            }
                        }
                    }
                }
                // parent.scaleIScores(leftChild.getIScale()+rightChild.getIScale());
                break;
            default:
                throw new Error("Malformed tree: more than two children");
            }
        }
        parent.setIScores(iScores);
    }

    public void countPosteriors(final double[][] cumulativePosteriors, final Tree<StateSet> tree,
            final double tree_score, final int tree_scale) {

        if (tree.isLeaf())
            return;

        final StateSet node = tree.getLabel();
        final short state = node.getState();

        final int nSubStates = grammar.numSubStates[state];
        final double scalingFactor = ScalingTools.calcScaleFactor(node.getOScale() + node.getIScale() - tree_scale);

        for (short substate = 0; substate < nSubStates; substate++) {
            final double pIS = node.getIScore(substate); // Parent outside score
            if (pIS == 0) {
                continue;
            }
            final double pOS = node.getOScore(substate); // Parent outside score
            if (pOS == 0) {
                continue;
            }
            double weight = 1;
            weight = (pIS / tree_score) * scalingFactor * pOS;
            if (weight > 1.01) {
                System.out.println("Overflow when counting tags? " + weight);
                weight = 0;
            }
            cumulativePosteriors[state][substate] += weight;
        }

        for (final Tree<StateSet> child : tree.getChildren()) {
            countPosteriors(cumulativePosteriors, child, tree_score, tree_scale);
        }
    }

}
