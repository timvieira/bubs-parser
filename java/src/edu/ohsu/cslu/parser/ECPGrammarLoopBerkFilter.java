package edu.ohsu.cslu.parser;

import java.util.Arrays;
import java.util.Collection;

import com.aliasi.util.Collections;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartCell;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public class ECPGrammarLoopBerkFilter extends CellwiseExhaustiveChartParser<GrammarByChild, CellChart> {

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

    public ECPGrammarLoopBerkFilter(final ParserOptions opts, final GrammarByChild grammar) {
        super(opts, grammar);
    }

    @Override
    public void initParser(final int sentLength) {
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

    @Override
    protected void addLexicalProductions(final int sent[]) throws Exception {
        Collection<Production> validProductions;
        ChartCell cell;

        // add lexical productions and unary productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                cell = chart.getCell(i, i + 1);
                cell.updateInside(chart.new ChartEdge(lexProd, cell));
                updateRuleConstraints(lexProd.parent, i, i + 1);

                validProductions = grammar.getUnaryProductionsWithChild(lexProd.parent);
                if (validProductions != null) {
                    for (final Production unaryProd : validProductions) {
                        cell.updateInside(chart.new ChartEdge(unaryProd, cell));
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
    protected boolean possibleRuleMidpoints(final Production p, final int beg, final int end) {
        // can this left constituent leave space for a right constituent?
        final int narrowR = narrowRExtent[beg][p.leftChild];
        if (narrowR >= end) {
            tmpNR++;
            return false;
        }

        // can this right constituent fit next to the left constituent?
        final int narrowL = narrowLExtent[end][p.rightChild];
        if (narrowL < narrowR) {
            tmpNL++;
            return false;
        }

        final int wideL = wideLExtent[end][p.rightChild];
        // minMidpoint = max(narrowR, wideL)
        final int minMidpoint = (narrowR > wideL ? narrowR : wideL);

        final int wideR = wideRExtent[beg][p.leftChild];
        // maxMidpoint = min(wideR, narrowL)
        final int maxMidpoint = (wideR < narrowL ? wideR : narrowL);

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

    protected void updateRuleConstraints(final int nonTerm, final int beg, final int end) {
        if (beg > narrowLExtent[end][nonTerm])
            narrowLExtent[end][nonTerm] = beg;
        if (beg < wideLExtent[end][nonTerm])
            wideLExtent[end][nonTerm] = beg;
        if (end < narrowRExtent[beg][nonTerm])
            narrowRExtent[beg][nonTerm] = end;
        if (end > wideRExtent[beg][nonTerm])
            wideRExtent[beg][nonTerm] = end;
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        ChartCell leftCell, rightCell;
        ChartEdge oldBestEdge;
        float prob, leftInside, rightInside;
        final int start = cell.start(), end = cell.end();
        boolean foundBetter;

        for (final Production p : grammar.getBinaryProductions()) {
            if (possibleRuleMidpoints(p, start, end)) {
                foundBetter = false;
                oldBestEdge = cell.getBestEdge(p.parent);

                // possibleMidpointMin and possibleMidpointMax are global values set by
                // calling possibleRuleMidpoints() since we can't return two ints easily
                for (int mid = possibleMidpointMin; mid <= possibleMidpointMax; mid++) {
                    leftCell = chart.getCell(start, mid);
                    leftInside = leftCell.getInside(p.leftChild);
                    if (leftInside <= Float.NEGATIVE_INFINITY)
                        continue;

                    rightCell = chart.getCell(mid, end);
                    rightInside = rightCell.getInside(p.rightChild);
                    if (rightInside <= Float.NEGATIVE_INFINITY)
                        continue;

                    prob = p.prob + leftInside + rightInside;
                    if (prob > cell.getInside(p.parent)) {
                        cell.updateInside(p, leftCell, rightCell, prob);
                        foundBetter = true;
                    }
                }

                if (foundBetter && (oldBestEdge == null)) {
                    updateRuleConstraints(p.parent, start, end);
                }
            }
        }

        for (final int childNT : Collections.toIntArray(cell.getNTs())) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                prob = p.prob + cell.getInside(childNT);
                if (prob > cell.getInside(p.parent)) {
                    cell.updateInside(chart.new ChartEdge(p, cell));
                    updateRuleConstraints(p.parent, start, end);
                }
            }
        }
    }
}
