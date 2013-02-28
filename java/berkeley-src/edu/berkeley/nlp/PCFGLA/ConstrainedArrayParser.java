package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.Numberer;
import edu.berkeley.nlp.util.PriorityQueue;
import edu.berkeley.nlp.util.StringUtils;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;

public class ConstrainedArrayParser extends ArrayParser {

    List<Integer>[][] possibleStates;
    /** inside scores; start idx, end idx, state -> logProb */
    protected double[][][][] iScore;
    /** outside scores; start idx, end idx, state -> logProb */
    protected double[][][][] oScore;
    protected short[] numSubStatesArray;
    public long totalUsedUnaries;
    public long nRules, nRulesInf;
    // the chart is now using scaled probabilities, NOT log-probs.
    protected int[][][] iScale; // for each (start,end) span there is a scaling
                                // factor
    protected int[][][] oScale;
    public Binarization binarization;

    Counter<String> stateCounter = new Counter<String>();
    Counter<String> ruleCounter = new Counter<String>();

    public boolean viterbi = false;
    /** number of times we restored unaries */
    public int nTimesRestoredUnaries;

    boolean noConstrains = false;

    protected List<String> nextSentence;
    protected int nextSentenceID;
    int myID;
    PriorityQueue<List<Tree<String>>> queue;

    public void setID(final int i, final PriorityQueue<List<Tree<String>>> q) {
        myID = i;
        queue = q;
    }

    public void setNextSentence(final List<String> nextS, final int nextID) {
        nextSentence = nextS;
        nextSentenceID = nextID;
    }

    public ConstrainedArrayParser newInstance() {
        final ConstrainedArrayParser newParser = new ConstrainedArrayParser(grammar, lexicon, numSubStatesArray);
        return newParser;
    }

    public double getLogLikelihood(final Tree<String> t) {
        System.out.println("Unsuported for now!");
        return Double.NEGATIVE_INFINITY;
    }

    public Tree<String>[] getSampledTrees(final List<String> sentence, final List<Integer>[][] pStates, final int n) {
        return null;
    }

    public void setNoConstraints(final boolean noC) {
        this.noConstrains = noC;
    }

    public List<Tree<String>> getKBestConstrainedParses(final List<String> sentence, final List<String> posTags,
            final int k) {
        return null;
    }

    public ConstrainedArrayParser() {
    }

    public ConstrainedArrayParser(final Grammar gr, final Lexicon lex, final short[] nSub) {
        super(gr, lex);
        this.numSubStatesArray = nSub;
        totalUsedUnaries = 0;
        nTimesRestoredUnaries = 0;
        nRules = 0;
        nRulesInf = 0;
        // Math.pow(GrammarTrainer.SCALE,scaleDiff);
    }

    /**
     * Print the statistics about how often each state and rule appeared.
     * 
     */
    public void printStateAndRuleTallies() {
        System.out.println("STATE TALLIES");
        for (final String state : stateCounter.keySet()) {
            System.out.println(state + " " + stateCounter.getCount(state));
        }
        System.out.println("RULE TALLIES");
        for (final String rule : ruleCounter.keySet()) {
            System.out.println(rule + " " + ruleCounter.getCount(rule));
        }
    }

    protected void createArrays() {
        // zero out some stuff first in case we recently ran out of memory and
        // are reallocating
        clearArrays();

        // allocate just the parts of iScore and oScore used (end > start, etc.)
        // System.out.println("initializing iScore arrays with length " + length
        // + " and numStates " + numStates);
        iScore = new double[length][length + 1][][];
        oScore = new double[length][length + 1][][];
        iScale = new int[length][length + 1][];
        oScale = new int[length][length + 1][];
        for (int start = 0; start < length; start++) { // initialize for all POS
                                                       // tags so that we can
                                                       // use the lexicon
            final int end = start + 1;
            iScore[start][end] = new double[numStates][];
            oScore[start][end] = new double[numStates][];
            iScale[start][end] = new int[numStates];
            oScale[start][end] = new int[numStates];
            for (int state = 0; state < numStates; state++) {
                iScore[start][end][state] = new double[numSubStatesArray[state]];
                oScore[start][end][state] = new double[numSubStatesArray[state]];
                Arrays.fill(iScore[start][end][state], Float.NEGATIVE_INFINITY);
                Arrays.fill(oScore[start][end][state], Float.NEGATIVE_INFINITY);
            }
        }

        for (int start = 0; start < length; start++) {
            for (int end = start + 2; end <= length; end++) {
                iScore[start][end] = new double[numStates][];
                oScore[start][end] = new double[numStates][];
                iScale[start][end] = new int[numStates];
                oScale[start][end] = new int[numStates];
                List<Integer> pStates = null;
                if (noConstrains) {
                    pStates = new ArrayList<Integer>();
                    for (int i = 0; i < numStates; i++) {
                        pStates.add(i);
                    }
                } else {
                    pStates = possibleStates[start][end];
                }

                for (final int state : pStates) {
                    iScore[start][end][state] = new double[numSubStatesArray[state]];
                    oScore[start][end][state] = new double[numSubStatesArray[state]];
                    Arrays.fill(iScore[start][end][state], Float.NEGATIVE_INFINITY);
                    Arrays.fill(oScore[start][end][state], Float.NEGATIVE_INFINITY);
                }
                if (start == 0 && end == length) {
                    if (pStates.size() == 0)
                        System.out.println("no states span the entire tree!");
                    if (iScore[start][end][0] == null)
                        System.out.println("ROOT does not span the entire tree!");
                }
            }
        }
        narrowRExtent = new int[length + 1][numStates];
        wideRExtent = new int[length + 1][numStates];
        narrowLExtent = new int[length + 1][numStates];
        wideLExtent = new int[length + 1][numStates];

        for (int loc = 0; loc <= length; loc++) {
            Arrays.fill(narrowLExtent[loc], -1); // the rightmost left with
                                                 // state s ending at i that
                                                 // we can get is the
                                                 // beginning
            Arrays.fill(wideLExtent[loc], length + 1); // the leftmost left with
                                                       // state s ending at i
                                                       // that we can get is
                                                       // the end
            Arrays.fill(narrowRExtent[loc], length + 1); // the leftmost right
                                                         // with state s
                                                         // starting at i
                                                         // that we can get
                                                         // is the end
            Arrays.fill(wideRExtent[loc], -1); // the rightmost right with state
                                               // s starting at i that we can
                                               // get is the beginning
        }
    }

    void initializeChart(final List<String> sentence) {
        // for simplicity the lexicon will store words and tags as strings,
        // while the grammar will be using integers -> Numberer()
        int start = 0;
        int end = start + 1;
        for (final String word : sentence) {
            end = start + 1;
            for (short tag = 0; tag < grammar.numSubStates.length; tag++) {
                if (grammar.isGrammarTag[tag])
                    continue;
                // List<Integer> possibleSt = possibleStates[start][end];
                // for (int tag : possibleSt){
                narrowRExtent[start][tag] = end;
                narrowLExtent[end][tag] = start;
                wideRExtent[start][tag] = end;
                wideLExtent[end][tag] = start;
                final double[] lexiconScores = lexicon.score(word, tag, start, false, false);
                for (short n = 0; n < numSubStatesArray[tag]; n++) {
                    final double prob = lexiconScores[n];
                    /*
                     * if (prob>0){ prob = -10; System.out.println("Should never happen! Log-Prob > 0!!!" );
                     * System.out.println("Word "+word+" Tag "+(String)tagNumberer .object(tag)+" prob "+prob); }
                     */
                    iScore[start][end][tag][n] = prob;
                    /*
                     * UnaryRule[] unaries = grammar.getClosedUnaryRulesByChild(state); for (int r = 0; r <
                     * unaries.length; r++) { UnaryRule ur = unaries[r]; int parentState = ur.parent; double pS =
                     * (double) ur.score; double tot = prob + pS; if (tot > iScore[start][end][parentState]) {
                     * iScore[start][end][parentState] = tot; narrowRExtent[start][parentState] = end;
                     * narrowLExtent[end][parentState] = start; wideRExtent[start][parentState] = end;
                     * wideLExtent[end][parentState] = start; } }
                     */
                }
            }
            start++;
        }
    }

    public void showScores(final double[][][][] scores, final String title) {
        System.out.println(title);
        for (int diff = 1; diff <= length; diff++) {
            for (int start = 0; start < (length - diff + 1); start++) {
                final int end = start + diff;
                System.out.print("[" + start + " " + end + "]: ");
                // List<Integer> possibleSt = possibleStates[start][end];
                List<Integer> possibleSt = null;
                if (noConstrains) {
                    possibleSt = new ArrayList<Integer>();
                    for (int i = 0; i < numStates; i++) {
                        possibleSt.add(i);
                    }
                } else {
                    possibleSt = possibleStates[start][end];
                }
                for (final int state : possibleSt) {
                    if (scores[start][end][state] != null) {
                        for (int s = 0; s < grammar.numSubStates[state]; s++) {
                            final Numberer n = grammar.tagNumberer;
                            System.out.print("("
                                    + StringUtils.escapeString(n.symbol(state).toString(), new char[] { '\"' }, '\\')
                                    + "[" + s + "] " + scores[start][end][state][s] + ")");
                        }
                    }
                }
                System.out.println();
            }
        }
    }

    /**
     * Return the single best parse. Note that the returned tree may be missing intermediate nodes in a unary chain
     * because it parses with a unary-closed grammar.
     */
    public Tree<String> extractBestParse(final int gState, final int gp, final int start, final int end,
            final List<String> sentence) {
        // find sources of inside score
        // no backtraces so we can speed up the parsing for its primary use
        final double bestScore = iScore[start][end][gState][gp];
        final String goalStr = (String) tagNumberer.symbol(gState);
        // System.out.println("Looking for "+goalStr+" from "+start+" to "+end+" with score "+
        // bestScore+".");
        if (end - start == 1) {
            // if the goal state is a preterminal state, then it can't transform
            // into
            // anything but the word below it
            // if (lexicon.getAllTags().contains(gState)) {
            if (!grammar.isGrammarTag[gState]) {
                final List<Tree<String>> child = new ArrayList<Tree<String>>();
                child.add(new Tree<String>(sentence.get(start)));
                return new Tree<String>(goalStr, child);
            }

            // if the goal state is not a preterminal state, then find a way to
            // transform it into one
            double veryBestScore = Double.NEGATIVE_INFINITY;
            int newIndex = -1;
            final UnaryRule[] unaries = grammar.getClosedViterbiUnaryRulesByParent(gState);
            for (int r = 0; r < unaries.length; r++) {
                final UnaryRule ur = unaries[r];
                final int cState = ur.childState;
                final double[][] scores = ur.getScores2();
                for (int cp = 0; cp < scores.length; cp++) {
                    if (scores[cp] == null)
                        continue;
                    final double ruleScore = iScore[start][end][cState][cp] + scores[cp][gp];
                    if ((ruleScore >= veryBestScore) && (gState != cState || gp != cp)
                            && (!grammar.isGrammarTag[ur.getChildState()])) {
                        // && lexicon.getAllTags().contains(cState)) {
                        veryBestScore = ruleScore;
                        newIndex = cState;
                    }
                }
            }
            final List<Tree<String>> child1 = new ArrayList<Tree<String>>();
            child1.add(new Tree<String>(sentence.get(start)));
            final String goalStr1 = (String) tagNumberer.symbol(newIndex);
            if (goalStr1 == null)
                System.out.println("goalStr1==null with newIndex==" + newIndex + " goalStr==" + goalStr);
            final List<Tree<String>> child = new ArrayList<Tree<String>>();
            child.add(new Tree<String>(goalStr1, child1));
            return new Tree<String>(goalStr, child);
        }

        // check binaries first
        for (int split = start + 1; split < end; split++) {
            // for (Iterator binaryI = grammar.bRuleIteratorByParent(gState,
            // gp); binaryI.hasNext();) {
            // BinaryRule br = (BinaryRule) binaryI.next();
            final BinaryRule[] parentRules = grammar.splitRulesWithP(gState);
            for (int i = 0; i < parentRules.length; i++) {
                final BinaryRule br = parentRules[i];

                final int lState = br.leftChildState;
                if (iScore[start][split][lState] == null)
                    continue;

                final int rState = br.rightChildState;
                if (iScore[split][end][rState] == null)
                    continue;

                // new: iterate over substates
                final double[][][] scores = br.getScores2();
                for (int lp = 0; lp < scores.length; lp++) {
                    for (int rp = 0; rp < scores[lp].length; rp++) {
                        if (scores[lp][rp] == null)
                            continue;
                        final double score = scores[lp][rp][gp] + iScore[start][split][lState][lp]
                                + iScore[split][end][rState][rp];
                        if (matches(score, bestScore)) {
                            // build binary split
                            final Tree<String> leftChildTree = extractBestParse(lState, lp, start, split, sentence);
                            final Tree<String> rightChildTree = extractBestParse(rState, rp, split, end, sentence);
                            final List<Tree<String>> children = new ArrayList<Tree<String>>();
                            children.add(leftChildTree);
                            children.add(rightChildTree);
                            final Tree<String> result = new Tree<String>(goalStr, children);
                            // System.out.println("Binary node: "+result);
                            // result.setScore(score);
                            return result;
                        }
                    }
                }
            }
        }
        // check unaries
        // for (Iterator unaryI = grammar.uRuleIteratorByParent(gState, gp);
        // unaryI.hasNext();) {
        // UnaryRule ur = (UnaryRule) unaryI.next();
        final UnaryRule[] unaries = grammar.getClosedViterbiUnaryRulesByParent(gState);
        for (int r = 0; r < unaries.length; r++) {
            final UnaryRule ur = unaries[r];
            final int cState = ur.childState;

            if (iScore[start][end][cState] == null)
                continue;

            // new: iterate over substates
            final double[][] scores = ur.getScores2();
            for (int cp = 0; cp < scores.length; cp++) {
                if (scores[cp] == null)
                    continue;
                final double score = scores[cp][gp] + iScore[start][end][cState][cp];
                if ((cState != ur.parentState || cp != gp) && matches(score, bestScore)) {
                    // build unary
                    final Tree<String> childTree = extractBestParse(cState, cp, start, end, sentence);
                    final List<Tree<String>> children = new ArrayList<Tree<String>>();
                    children.add(childTree);
                    final Tree<String> result = new Tree<String>(goalStr, children);
                    // System.out.println("Unary node: "+result);
                    // result.setScore(score);
                    return result;
                }
            }
        }
        System.err.println("Warning: could not find the optimal way to build state " + goalStr + " spanning from "
                + start + " to " + end + ".");
        return null;
    }

    /**
     * Return the single best parse. Note that the returned tree may be missing intermediate nodes in a unary chain
     * because it parses with a unary-closed grammar. A StateSet tree is returned, but the subState array is used in a
     * different way: it has only one entry, whose value is the substate! - dirty hack...
     */
    public Tree<StateSet> extractBestStateSetTree(final short gState, final short gp, final short start,
            final short end, final List<String> sentence) {
        // find sources of inside score
        // no backtraces so we can speed up the parsing for its primary use
        final double bestScore = iScore[start][end][gState][gp];
        // Numberer tagNumberer = Numberer.getGlobalNumberer("tags");
        // System.out.println("Looking for "+(String)tagNumberer.object(gState)+" from "+start+" to "+end+" with score "+
        // bestScore+".");
        if (end - start == 1) {
            // if the goal state is a preterminal state, then it can't transform
            // into
            // anything but the word below it
            if (!grammar.isGrammarTag(gState)) {
                final List<Tree<StateSet>> child = new ArrayList<Tree<StateSet>>();
                final StateSet node = new StateSet(ZERO, ZERO, sentence.get(start), start, end);
                child.add(new Tree<StateSet>(node));
                final StateSet root = new StateSet(gState, ONE, null, start, end);
                root.allocate();
                root.setIScore(0, gp);
                return new Tree<StateSet>(root, child);
            }

            // if the goal state is not a preterminal state, then find a way to
            // transform it into one
            double veryBestScore = Double.NEGATIVE_INFINITY;
            short newIndex = -1;
            short newSubstate = -1;
            final UnaryRule[] unaries = grammar.getClosedViterbiUnaryRulesByParent(gState);
            for (int r = 0; r < unaries.length; r++) {
                final UnaryRule ur = unaries[r];
                final short cState = ur.childState;
                final double[][] scores = ur.getScores2();
                for (short cp = 0; cp < scores.length; cp++) {
                    if (scores[cp] == null)
                        continue;
                    if (iScore[start][end][cState] == null)
                        continue;
                    final double ruleScore = iScore[start][end][cState][cp] + scores[cp][gp];
                    if ((ruleScore >= veryBestScore) && (gState != cState || gp != cp) && !grammar.isGrammarTag(cState)) {
                        veryBestScore = ruleScore;
                        newIndex = cState;
                        newSubstate = cp;
                    }
                }
            }
            final List<Tree<StateSet>> child1 = new ArrayList<Tree<StateSet>>();
            final StateSet node1 = new StateSet(ZERO, ZERO, sentence.get(start), start, end);
            child1.add(new Tree<StateSet>(node1));
            if (newIndex == -1)
                System.out.println("goalStr1==null with newIndex==" + newIndex + " goalState==" + gState);
            final List<Tree<StateSet>> child = new ArrayList<Tree<StateSet>>();
            final StateSet node = new StateSet(newIndex, ONE, null, start, end);
            node.allocate();
            node.setIScore(0, newSubstate);
            child.add(new Tree<StateSet>(node, child1));
            final StateSet root = new StateSet(gState, ONE, null, start, end);
            root.allocate();
            root.setIScore(0, gp);
            // totalUsedUnaries++;
            return new Tree<StateSet>(root, child);
        }
        // check binaries first
        double bestBScore = Double.NEGATIVE_INFINITY;
        // BinaryRule bestBRule = null;
        // short bestBLp, bestBRp;
        // TODO: fix parsing
        for (int split = start + 1; split < end; split++) {
            final BinaryRule[] parentRules = grammar.splitRulesWithP(gState);
            for (short i = 0; i < parentRules.length; i++) {
                final BinaryRule br = parentRules[i];

                final short lState = br.leftChildState;
                if (iScore[start][split][lState] == null)
                    continue;

                final short rState = br.rightChildState;
                if (iScore[split][end][rState] == null)
                    continue;

                // new: iterate over substates
                final double[][][] scores = br.getScores2();
                for (short lp = 0; lp < scores.length; lp++) {
                    for (short rp = 0; rp < scores[lp].length; rp++) {
                        if (scores[lp][rp] == null)
                            continue;
                        final double score = scores[lp][rp][gp] + iScore[start][split][lState][lp]
                                + iScore[split][end][rState][rp];
                        if (score > bestBScore)
                            bestBScore = score;
                        if (matches(score, bestScore)) {
                            // build binary split
                            final Tree<StateSet> leftChildTree = extractBestStateSetTree(lState, lp, start,
                                    (short) split, sentence);
                            final Tree<StateSet> rightChildTree = extractBestStateSetTree(rState, rp, (short) split,
                                    end, sentence);
                            final List<Tree<StateSet>> children = new ArrayList<Tree<StateSet>>();
                            children.add(leftChildTree);
                            children.add(rightChildTree);
                            final StateSet root = new StateSet(gState, ONE, null, start, end);
                            root.allocate();
                            root.setIScore(0, gp);
                            final Tree<StateSet> result = new Tree<StateSet>(root, children);
                            // System.out.println("Binary node: "+result);
                            // result.setScore(score);
                            return result;
                        }
                    }
                }
            }
        }
        double bestUScore = Double.NEGATIVE_INFINITY;
        // check unaries
        final UnaryRule[] unaries = grammar.getClosedViterbiUnaryRulesByParent(gState);
        for (short r = 0; r < unaries.length; r++) {
            final UnaryRule ur = unaries[r];
            final short cState = ur.childState;

            if (iScore[start][end][cState] == null)
                continue;

            // new: iterate over substates
            final double[][] scores = ur.getScores2();
            for (short cp = 0; cp < scores.length; cp++) {
                if (scores[cp] == null)
                    continue;
                final double rScore = scores[cp][gp];
                final double score = rScore + iScore[start][end][cState][cp];
                if (score > bestUScore)
                    bestUScore = score;
                if ((cState != ur.parentState || cp != gp) && matches(score, bestScore)) {
                    // build unary
                    final Tree<StateSet> childTree = extractBestStateSetTree(cState, cp, start, end, sentence);
                    final List<Tree<StateSet>> children = new ArrayList<Tree<StateSet>>();
                    children.add(childTree);
                    final StateSet root = new StateSet(gState, ONE, null, start, end);
                    root.allocate();
                    root.setIScore(0, gp);
                    final Tree<StateSet> result = new Tree<StateSet>(root, children);
                    // System.out.println("Unary node: "+result);
                    // result.setScore(score);
                    totalUsedUnaries++;
                    return result;
                }
            }
        }
        System.err.println("Warning: could not find the optimal way to build state " + gState + " spanning from "
                + start + " to " + end + ".");
        System.err.println("The goal score was " + bestScore + ", but the best we found was a binary rule giving "
                + bestBScore + " and a unary rule giving " + bestUScore);
        showScores(iScore, "iScores");
        return null;
    }

    // the state set tree has nodes that are labeled with substate information
    // the substate information is the first element in the iscore array
    protected Tree<String> restoreStateSetTreeUnaries(final Tree<StateSet> t) {
        // System.out.println("In restoreUnaries...");
        // System.out.println("Doing node: "+node.getLabel());

        if (t.isLeaf()) { // shouldn't happen
            System.err.println("Tried to restore unary from a leaf...");
            return null;
        } else if (t.isPreTerminal()) { // preterminal unaries have already been
                                        // restored
            final List<Tree<String>> child = new ArrayList<Tree<String>>();
            child.add(new Tree<String>(t.getChildren().get(0).getLabel().getWord()));
            return new Tree<String>((String) tagNumberer.symbol(t.getLabel().getState()), child);
        } else if (t.getChildren().size() != 1) { // nothing to restore
            // build binary split
            final Tree<String> leftChildTree = restoreStateSetTreeUnaries(t.getChildren().get(0));
            final Tree<String> rightChildTree = restoreStateSetTreeUnaries(t.getChildren().get(1));
            final List<Tree<String>> children = new ArrayList<Tree<String>>();
            children.add(leftChildTree);
            children.add(rightChildTree);
            return new Tree<String>((String) tagNumberer.symbol(t.getLabel().getState()), children);
        } // the interesting part:
          // System.out.println("Not skipping node: "+node.getLabel());
        final StateSet parent = t.getLabel();
        final short pLabel = parent.getState();

        // System.out.println("P: "+(String)tagNumberer.object(pLabel)+" C: "+(String)tagNumberer.object(cLabel));
        final List<Tree<String>> goodChild = new ArrayList<Tree<String>>();
        goodChild.add(restoreStateSetTreeUnaries(t.getChildren().get(0)));
        // do we need a check here? if we can check whether the rule was
        // in the original grammar, then we wouldnt need the getBestPath call.
        // but getBestPath should be able to take care of that...
        // if (grammar.getUnaryScore(new UnaryRule(pLabel,cLabel))[0][0] != 0){
        // continue; }// means the rule was already in grammar

        // System.out.println("Got path: "+path);
        // if (path.size()==1) return goodChild;
        final Tree<String> result = new Tree<String>((String) tagNumberer.symbol(pLabel), goodChild);
        final Tree<String> working = result;
        // List<short[]> path = grammar.getBestViterbiPath(pLabel,pSubState,
        // cLabel,cSubState);
        // if (path.size()>2) {
        // nTimesRestoredUnaries++;
        // }
        // for (int pos=1; pos < path.size() - 1; pos++) {
        // int interState = path.get(pos)[0];
        // Tree<String> intermediate = new Tree<String>((String)
        // tagNumberer.object(interState), working.getChildren());
        // List<Tree<String>> children = new ArrayList<Tree<String>>();
        // children.add(intermediate);
        // working.setChildren(children);
        // working = intermediate;
        // }
        return working;
    }

    public void printUnaryStats() {
        System.out.println(" Used a total of " + totalUsedUnaries + " unary productions.");
        System.out.println(" restored unaries " + nTimesRestoredUnaries);
        System.out.println(" Out of " + nRules + " rules " + nRulesInf + " had probability=-Inf.");
    }

    public void projectConstraints(final boolean[][][][] allowed, final boolean allSubstatesAllowed) {
        System.err.println("Not supported!\nThis parser cannot project constraints!");
    }

    /**
     * @return the numSubStatesArray
     */
    public short[] getNumSubStatesArray() {
        return numSubStatesArray;
    }

}
