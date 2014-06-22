/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.parser.ecp;

import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.grammar.ListGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

/**
 * Exhaustive chart parser which performs grammar intersection by iterating over a filtered list of grammar rules at
 * each midpoint.
 * 
 * @author Nathan Bodenstab
 */
public class ECPGrammarLoopBerkFilter extends ChartParser<ListGrammar, CellChart> {

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

    public ECPGrammarLoopBerkFilter(final ParserDriver opts, final ListGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    public void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);

        final int sentLength = parseTask.sentenceLength();
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
    }

    @Override
    protected void addLexicalProductions(final ChartCell c) {
        Collection<Production> validProductions;

        final HashSetChartCell cell = (HashSetChartCell) c;
        final short start = cell.start();

        for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.parseTask.tokens[start])) {
            cell.updateInside(chart.new ChartEdge(lexProd, cell));
            updateRuleConstraints(lexProd.parent, start, start + 1);

            validProductions = grammar.getUnaryProductionsWithChild(lexProd.parent);
            if (validProductions != null) {
                for (final Production unaryProd : validProductions) {
                    cell.updateInside(chart.new ChartEdge(unaryProd, cell));
                    updateRuleConstraints(unaryProd.parent, start, start + 1);
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
            return false;
        }

        // can this right constituent fit next to the left constituent?
        final int narrowL = narrowLExtent[end][p.rightChild];
        if (narrowL < narrowR) {
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
    protected void computeInsideProbabilities(final ChartCell c) {
        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final HashSetChartCell cell = (HashSetChartCell) c;
        final short start = cell.start();
        final short end = cell.end();
        HashSetChartCell leftCell, rightCell;
        ChartEdge oldBestEdge;
        float prob, leftInside, rightInside;
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

        if (collectDetailedStatistics) {
            chart.parseTask.insideBinaryNs += System.nanoTime() - t0;
        }

        for (final int childNT : cell.getNtArray()) {
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
