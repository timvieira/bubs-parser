package edu.ohsu.cslu.parser;

import java.util.Arrays;
import java.util.List;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ECPGramLoopBerkFilter extends ExhaustiveChartParser {

    // tracks the spans of nonTerms in the chart so we don't have to consider them
    // in the inner loop of fillChart()
    // wideLeft <--- narrowLeft <--- [nonTerm,sentIndex] ---> narrowRight ---> wideRight
    protected int[][] narrowLExtent = null;
    protected int[][] wideLExtent = null;
    protected int[][] narrowRExtent = null;
    protected int[][] wideRExtent = null;

    // static protected Pair<Integer,Integer> midpointMinMax;
    private int possibleMidpointMin = -1;
    private int possibleMidpointMax = -1;

    private int tmpNL, tmpNR, tmpWR;

    public ECPGramLoopBerkFilter(Grammar grammar, ChartTraversalType traversalType) {
        super(grammar, traversalType);
    }

    @Override
    public ParseTree findMLParse(String sentence) throws Exception {
        return findParse(sentence);
    }

    public void initParser(int sentLength) {
        super.initParser(sentLength);

        narrowRExtent = new int[sentLength + 1][grammar.numNonTerms()];
        wideRExtent = new int[sentLength + 1][grammar.numNonTerms()];
        narrowLExtent = new int[sentLength + 1][grammar.numNonTerms()];
        wideLExtent = new int[sentLength + 1][grammar.numNonTerms()];

        for (int i = 0; i <= sentLength; i++) {
            Arrays.fill(narrowLExtent[i], -1);
            Arrays.fill(wideLExtent[i], sentLength + 1);
            Arrays.fill(narrowRExtent[i], sentLength + 1);
            Arrays.fill(wideRExtent[i], -1);
        }

        tmpNL = tmpNR = tmpWR = 0;
    }

    protected void addLexicalProductions(Token sent[]) throws Exception {
        List<Production> validProductions;
        float edgeLogProb;

        // add lexical productions and unary productions to the base cells of the chart
        for (int i = 0; i < this.chartSize; i++) {
            for (Production lexProd : grammar.getLexProdsForToken(sent[i])) {
                chart[i][i + 1].addEdge(new ChartEdge(lexProd, chart[i][i + 1], lexProd.prob));
                updateRuleConstraints(lexProd.parent, i, i + 1);

                validProductions = grammar.getUnaryProdsWithChild(lexProd.parent);
                if (validProductions != null) {
                    for (Production unaryProd : validProductions) {
                        edgeLogProb = unaryProd.prob + lexProd.prob;
                        chart[i][i + 1].addEdge(new ChartEdge(unaryProd, chart[i][i + 1], edgeLogProb));
                        updateRuleConstraints(unaryProd.parent, i, i + 1);
                    }
                }
            }
        }
    }

    // given production A -> B C, check if this rule can fit into the chart given
    // the spans of B and C that are already in the chart:
    // B[beg] --> narrowRight --> wideRight
    // || possible midpts ||
    // wideLeft <-- narrowLeft <-- C[end]
    protected boolean possibleRuleMidpoints(Production p, int beg, int end) {
        // can this left constituent leave space for a right constituent?
        int narrowR = narrowRExtent[beg][p.leftChild];
        if (narrowR >= end) {
            tmpNR++;
            return false;
        }

        // can this right constituent fit next to the left constituent?
        int narrowL = narrowLExtent[end][p.rightChild];
        if (narrowL < narrowR) {
            tmpNL++;
            return false;
        }

        int wideL = wideLExtent[end][p.rightChild];
        // minMidpoint = max(narrowR, wideL)
        int minMidpoint = (narrowR > wideL ? narrowR : wideL);

        int wideR = wideRExtent[beg][p.leftChild];
        // maxMidpoint = min(wideR, narrowL)
        int maxMidpoint = (wideR < narrowL ? wideR : narrowL);

        // can the constituents stretch far enough to reach each other?
        if (minMidpoint > maxMidpoint) {
            tmpWR++;
            return false;
        }

        // set global values since we can't return two ints efficiently
        possibleMidpointMin = minMidpoint;
        possibleMidpointMax = maxMidpoint;
        return true;
    }

    protected void updateRuleConstraints(int nonTerm, int beg, int end) {
        if (beg > narrowLExtent[end][nonTerm])
            narrowLExtent[end][nonTerm] = beg;
        if (beg < wideLExtent[end][nonTerm])
            wideLExtent[end][nonTerm] = beg;
        if (end < narrowRExtent[beg][nonTerm])
            narrowRExtent[beg][nonTerm] = end;
        if (end > wideRExtent[beg][nonTerm])
            wideRExtent[beg][nonTerm] = end;

        /*
         * if (beg > narrowLExtent[end][nonTerm]) { narrowLExtent[end][nonTerm] = beg;
         * wideLExtent[end][nonTerm] = beg; } else if (beg < wideLExtent[end][nonTerm]) {
         * wideLExtent[end][nonTerm] = beg; }
         * 
         * if (end < narrowRExtent[beg][nonTerm]) { narrowRExtent[beg][nonTerm] = end;
         * wideRExtent[beg][nonTerm] = end; } else if (end > wideRExtent[beg][nonTerm]) {
         * wideRExtent[beg][nonTerm] = end; }
         */
    }

    /*
     * public String getStats() { String result="STAT: narrowRight: " + tmpNR; result += "STAT:   wideRight: "
     * + tmpWR; result += "STAT:  narrowLeft: " + tmpNL; result += "STAT:    wideLeft: " + tmpWL; return
     * result; }
     */

    protected void visitCell(ChartCell cell) {
        ChartCell leftCell, rightCell;
        ChartEdge leftEdge, rightEdge, parentEdge, oldBestEdge;
        float prob;
        int start = cell.start;
        int end = cell.end;
        boolean foundBetter, edgeWasAdded;

        for (Production p : grammar.binaryProds) {
            if (possibleRuleMidpoints(p, start, end)) {
                foundBetter = false;
                oldBestEdge = cell.getBestEdge(p.parent);

                // possibleMidpointMin and possibleMidpointMax are global values set by
                // calling possibleRuleMidpoints() since we can't return two ints easily
                for (int mid = possibleMidpointMin; mid <= possibleMidpointMax; mid++) {
                    leftCell = chart[start][mid];
                    leftEdge = leftCell.getBestEdge(p.leftChild);
                    if (leftEdge == null)
                        continue;

                    rightCell = chart[mid][end];
                    rightEdge = rightCell.getBestEdge(p.rightChild);
                    if (rightEdge == null)
                        continue;

                    prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                    edgeWasAdded = cell.addEdge(p, prob, leftCell, rightCell);
                    foundBetter = (foundBetter || edgeWasAdded);
                }

                if (foundBetter && (oldBestEdge == null)) {
                    updateRuleConstraints(p.parent, start, end);
                }
            }
        }

        for (Production p : grammar.unaryProds) {
            parentEdge = cell.getBestEdge(p.leftChild);
            if ((parentEdge != null) && (parentEdge.p.isUnaryProd() == false)) {
                prob = p.prob + parentEdge.insideProb;
                // the child cell is also the parent cell for unary productions
                edgeWasAdded = cell.addEdge(new ChartEdge(p, cell, prob));
                if (edgeWasAdded) {
                    updateRuleConstraints(p.parent, start, end);
                }
            }
        }
    }
}
