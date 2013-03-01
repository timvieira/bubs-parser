package edu.berkeley.nlp.PCFGLA;

import java.util.List;

import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;

/**
 * Simple mixture parser.
 */
public class ArrayParser {

    protected final short ZERO = 0, ONE = 1;

    protected Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

    /** Inside scores, indexed by start, end, state */
    protected double[][][] iScore;

    /** Outside scores, indexed by start, end, state */
    protected double[][][] oScore;

    /** The rightmost left extent of state <em>s</em> ending at position <em>i</em> */
    protected int[][] narrowLExtent = null;

    /** The leftmost left extent of state <em>s</em> ending at position <em>i</em> */
    protected int[][] wideLExtent = null;

    /** The leftmost right extent of state <em>s</em> starting at position <em>i</em> */
    protected int[][] narrowRExtent = null;

    /** The rightmost right extent of state <em>s</em> starting at position <em>i</em> */
    protected int[][] wideRExtent = null;

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

    /**
     * Calculate the inside scores, P(words_i,j|nonterminal_i,j) of a tree given the string if words it should parse to.
     * 
     * @param tree
     * @param noSmoothing
     * @param spanScores
     */
    void doInsideScores(final Tree<StateSet> tree, final boolean noSmoothing, final double[][][] spanScores) {

        if (tree.isLeaf()) {
            return;
        }

        final List<Tree<StateSet>> children = tree.children();
        for (final Tree<StateSet> child : children) {
            doInsideScores(child, noSmoothing, spanScores);
        }
        final StateSet parent = tree.label();
        final short pState = parent.getState();
        final int nParentStates = parent.numSubStates();
        if (tree.isPreTerminal()) {
            // Plays a role similar to initializeChart()
            final StateSet wordStateSet = tree.children().get(0).label();
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
                final StateSet child = children.get(0).label();
                final short cState = child.getState();
                final int nChildStates = child.numSubStates();
                final double[][] uscores = grammar.getUnaryScore(pState, cState);
                final double[] iScores = new double[nParentStates];
                for (int j = 0; j < nChildStates; j++) {
                    if (uscores[j] != null) { // check whether one of the
                                              // parents can produce this
                                              // child
                        final double cS = child.getIScore(j);
                        if (cS == 0) {
                            continue;
                        }
                        for (int i = 0; i < nParentStates; i++) {
                            final double rS = uscores[j][i]; // rule score
                            if (rS == 0) {
                                continue;
                            }
                            final double res = rS * cS;
                            /*
                             * if (res == 0) { System.out.println("Prevented an underflow: rS " +rS+" cS "+cS); res =
                             * Double.MIN_VALUE; }
                             */
                            iScores[i] += res;
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
                final StateSet leftChild = children.get(0).label();
                final StateSet rightChild = children.get(1).label();
                final int nLeftChildStates = leftChild.numSubStates();
                final int nRightChildStates = rightChild.numSubStates();
                final short lState = leftChild.getState();
                final short rState = rightChild.getState();
                final double[][][] bscores = grammar.getBinaryScore(pState, lState, rState);
                final double[] iScores2 = new double[nParentStates];
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
        tree.label().setOScore(0, 1);
        tree.label().setOScale(0);
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

        if (tree.isLeaf() || tree.isPreTerminal()) {
            return;
        }

        final List<Tree<StateSet>> children = tree.children();
        final StateSet parent = tree.label();
        final short pState = parent.getState();
        final int nParentStates = parent.numSubStates();
        // this sets the outside scores for the children
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
            final StateSet child = children.get(0).label();
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
            final StateSet leftChild = children.get(0).label();
            final StateSet rightChild = children.get(1).label();
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

    public void doInsideOutsideScores(final Tree<StateSet> tree, final boolean noSmoothing) {
        doInsideScores(tree, noSmoothing, null);
        setRootOutsideScore(tree);
        doOutsideScores(tree, false, null);
    }
}
