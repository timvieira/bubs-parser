/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.ecp;

import java.util.Collection;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.StringEdge;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

/**
 * Exhaustive chart parser which performs grammar intersection by iterating over grammar rules matching the observed
 * left child non-terminals.
 * 
 * @author Nathan Bodenstab
 */
public class ECPCellCrossListTreeConstrained extends ChartParser<LeftListGrammar, CellChart> {

    private long numConsidered = 0;
    BinaryTree<String> binaryGoldTree;

    public ECPCellCrossListTreeConstrained(final ParserDriver opts, final LeftListGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);
        numConsidered = 0;

        if (parseTask.inputTree == null) {
            BaseLogger.singleton().info("ERROR: Tree-Constrained CYK must have trees as input.  Exiting.");
            System.exit(1);
        }

        binaryGoldTree = parseTask.inputTree.binarize(grammar.grammarFormat, grammar.binarization());
    }

    public boolean fineEdgeMatchesCoarseEdge(final Production fine, final StringEdge coarse) {
        // if (mid != coarse.mid) return false;
        final String coarseA = grammar.grammarFormat.getBaseNT(grammar.mapNonterminal((short) fine.parent), false);
        if (!coarseA.equals(coarse.p.parent))
            return false;
        if (fine.isLexProd()) {
            // don't force a match on lexical items in case they aren't in the grammar; getting the POS tag right is
            // good enough
            // if (!grammar.mapLexicalEntry(fine.leftChild).equals(coarse.p.leftChild))
            // return false;
        } else {
            String rhs = grammar.grammarFormat.getBaseNT(grammar.mapNonterminal((short) fine.leftChild), false);
            if (fine.isBinaryProd()) {
                rhs += " " + grammar.grammarFormat.getBaseNT(grammar.mapNonterminal((short) fine.rightChild), false);
            }
            if (!rhs.equals(coarse.p.leftChild))
                return false;
        }
        return true;
    }

    public boolean findEdgeInCoarseEdgeSet(final Production fine, final Collection<StringEdge> coarseCollection) {
        for (final StringEdge coarse : coarseCollection) {
            // System.out.println("  comp: " + fine.toString() + "\t" + coarse.toString());
            if (fineEdgeMatchesCoarseEdge(fine, coarse)) {
                // System.out.println("  chart: " + fine.toString());
                return true;
            }
        }
        return false;
    }

    @Override
    protected void addLexicalProductions(final ChartCell cell) {
        final Collection<StringEdge> goldProds = Util.getStringEdgesFromTree(binaryGoldTree, grammar, true,
                cell.start(), cell.end());
        for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.parseTask.tokens[cell.start()])) {
            if (findEdgeInCoarseEdgeSet(lexProd, goldProds)) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
            }
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell c) {
        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final Collection<StringEdge> goldEdges = Util.getStringEdgesFromTree(binaryGoldTree, grammar, true, c.start(),
                c.end());
        if (goldEdges.size() == 0)
            return;

        final HashSetChartCell cell = (HashSetChartCell) c;
        final short start = cell.start();
        final short end = cell.end();
        float leftInside, rightInside;

        // final int midStart = cellSelector.getMidStart(start, end);
        // final int midEnd = cellSelector.getMidEnd(start, end);
        final boolean onlyFactored = cellSelector.hasCellConstraints() && cellSelector.isCellOnlyFactored(start, end);

        boolean addedEdge = false;
        int mid = -1;
        for (final StringEdge e : goldEdges) {
            if (e.mid != -1)
                mid = e.mid;
        }
        // for (int mid = midStart; mid <= midEnd; mid++) { // mid point
        if (mid >= 0) {
            // System.out.println("CELL: [" + cell.start() + "," + mid + "," + cell.end() + "]");
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                leftInside = leftCell.getInside(leftNT);
                for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                    if (!findEdgeInCoarseEdgeSet(p, goldEdges))
                        continue;
                    numConsidered++;
                    if (!onlyFactored || grammar.getOrAddNonterm((short) p.parent).isFactored()) {
                        rightInside = rightCell.getInside(p.rightChild);
                        if (rightInside > Float.NEGATIVE_INFINITY) {
                            addedEdge = true;
                            cell.updateInside(p, leftCell, rightCell, p.prob + leftInside + rightInside);
                        }
                    }
                }
            }
        }

        if (collectDetailedStatistics) {
            chart.parseTask.insideBinaryNs += System.nanoTime() - t0;
        }

        if (cellSelector.hasCellConstraints() == false || cellSelector.isUnaryOpen(start, end)) {
            for (int maxChain = 0; maxChain < 3; maxChain++) {
                for (final int childNT : cell.getNtArray()) {
                    for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                        if (!findEdgeInCoarseEdgeSet(p, goldEdges))
                            continue;
                        numConsidered++;
                        addedEdge = true;
                        cell.updateInside(p, p.prob + cell.getInside(childNT));
                    }
                }
            }
        }

        // if (addedEdge == false) {
        // System.out.println("WARN: no edges added for [" + cell.start() + "," + cell.end() + "]");
        // }
    }

    @Override
    public String getStats() {
        return super.getStats() + "numConsidered=" + numConsidered;
    }
}
