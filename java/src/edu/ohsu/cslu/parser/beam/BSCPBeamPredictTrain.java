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
package edu.ohsu.cslu.parser.beam;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.Feature;

/*
 This class is used for two purposes:

 1) -inputTreeBeamRank prints the rank of each constituent in the input 
 tree along with other info:

 rank sentLen span start end edge

 2) Generate features to be used to train a beam-width prediction model of the form:

 rank isBinarized numConstituents isSpan1Cell : numFeats feat1 feat2 ...

 where

 rank: The integer rank of the input tree constituent in the local agenda.  If
 more than one input constituent exists (i.e., a binary and unary entry) then the
 rank is the highest of the two. A value of 0 indicates that no gold constituent
 exists in the cell.

 isBinarized: 1 if the input constituent is headed by a binarized non-terminal, 0 otherwise

 numConstituents: the number of input constituents in the chart cell

 isSpan1Cell: 1 if chart cell spans a single word, 0 otherwise

 TODO: 

 Once we start pruning the beam-width on cells, it will change
 the ranking of gold edges in higher cells.  We should be able
 to adapt to this by slowly using the perceptron predictions
 and refining them with the gold edge results over time. 

 (1) static learning of beam-width: fill all chart cells
 with all possible edges to determine the gold beam-width

 (2) graduated learning (there must be a better name for this):
 Init with all predictions for beam-width=INF.  At each cell,
 predict the beam-width and use it.  If a gold edge exists
 in the cell, but is not placed in the cell because the beam
 width is too small, penalize the cell.  

 */

public class BSCPBeamPredictTrain extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    String[] featTemplates;
    List<Feature> featList;
    ParseTask curTask;
    GoldEdgeContainer goldEdgeChart[][];
    boolean printRankInfo = ParserDriver.inputTreeBeamRank;

    public BSCPBeamPredictTrain(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
        this.featTemplates = GlobalConfigProperties.singleton().getProperty(ParserDriver.OPT_DISC_FEATURE_TEMPLATES)
                .split(",");
        this.featList = Chart.featureTemplateStrToEnum(featTemplates);
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);
        this.curTask = parseTask;

        if (parseTask.inputTree == null) {
            BaseLogger.singleton().info("ERROR: BSCPTrainFOMConfidence requires trees as input");
            System.exit(1);
        }
        if (parseTask.inputTree.isBinaryTree() == false) {
            BaseLogger.singleton().info("ERROR: BSCPTrainFOMConfidence requires input trees to be binarized");
            System.exit(1);
        }

        final int n = parseTask.sentenceLength();
        final ArrayList<NaryTree<String>> leaves = new ArrayList<NaryTree<String>>(n);
        int i = 0;
        for (final NaryTree<String> leaf : parseTask.inputTree.leafTraversal()) {
            leaves.add(i, leaf);
            i++;
        }

        if (printRankInfo) {
            System.out.println("RANK:\tRank\tSentLen\tSpan\tStart\tEnd\tEdge");
        }

        // What not use the following ???
        // Util.getEdgesFromTree(tree, grammar, includeLeaves, startSpanIndex, endSpanIndex)

        // Read gold productions from input tree and put them in an array by goldEdges[start][end]
        // Note that there can be multiple edges per span when we include unary productions.
        goldEdgeChart = new GoldEdgeContainer[n][n + 1];
        for (int start = 0; start < n; start++) {
            for (int end = start; end <= n; end++) {
                goldEdgeChart[start][end] = new GoldEdgeContainer();
            }
        }
        for (final NaryTree<String> node : parseTask.inputTree.preOrderTraversal()) {
            if (!node.isLeaf() && !node.children().get(0).isLeaf()) {
                int start = -1, mid = -1, end = -1;
                for (i = 0; i < n; i++) {
                    if (leaves.get(i) == node.leftmostLeaf()) {
                        start = i;
                    }
                    if (node.children().size() == 2 && leaves.get(i) == node.children().get(0).rightmostLeaf()) {
                        mid = i + 1;
                    }
                    if (leaves.get(i) == node.rightmostLeaf()) {
                        end = i + 1;
                    }
                }
                // System.out.println(start + " " + mid + " " + end + "\t" + node.childLabels().toString());

                Production p;
                ChartEdge edge = null;
                if (node.children().size() == 2) {
                    p = grammar.getBinaryProduction(node.label(), node.children().get(0).label(), node.children()
                            .get(1).label());
                    if (p != null) {
                        edge = chart.new ChartEdge(p, chart.getCell(start, mid), chart.getCell(mid, end));
                    }
                } else {
                    p = grammar.getUnaryProduction(node.label(), node.children().get(0).label());
                    if (p != null) {
                        edge = chart.new ChartEdge(p, chart.getCell(start, end));
                    }
                }
                if (edge != null) {
                    goldEdgeChart[start][end].edges.add(edge);
                }
            }
        }

    }

    class GoldEdgeContainer {
        List<ChartEdge> edges = new LinkedList<ChartEdge>();
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {

        final int start = cell.start(), end = cell.end();
        final List<ChartEdge> goldEdges = goldEdgeChart[start][end].edges;
        boolean goldIsFactored = false;
        int goldRank = 0;
        final int numGoldEdges = goldEdges.size();
        // final boolean hasGoldEdge = numGoldEdges > 0;
        // if (hasGoldEdge) {
        // goldRank = -1;
        // }

        ChartEdge edge = agenda.poll();
        while (edge != null && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
            cellPopped++;

            // check if we just popped a gold edge
            for (final ChartEdge goldEdge : goldEdges) {
                if (edge.equals(goldEdge)) {
                    goldRank = cellPopped; // set to the rank of the *last* gold edge (if there are more than one)
                    if (grammar.getOrAddNonterm((short) goldEdge.prod.parent).isFactored) {
                        goldIsFactored = true;
                    }
                    if (printRankInfo) {
                        System.out.println(String.format("RANK:\t%d\t%d\t%d\t%d\t%d\t%s", goldRank,
                                curTask.sentenceLength(), end - start, start, end, edge));
                    }
                }
            }

            // add edge to chart
            if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);

                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    addEdgeToCollection(chart.new ChartEdge(p, cell));
                }
            }
            edge = agenda.poll();
        }

        if (!printRankInfo) {
            // Beam-width prediction features
            final SparseBitVector cellFeats = chart.getCellFeatures(cell.start(), cell.end(), this.featList);

            // goldRank goldIsFactored numGold isBaseCell : numFeats feat1 feat2 ...
            System.out.println(String.format("DSTAT: %d %d %d %d : %d %s", goldRank, bool2int(goldIsFactored),
                    numGoldEdges, bool2int(cell.width() == 1), cellFeats.length(),
                    Util.intArray2Str(cellFeats.elements())));
        }
    }

    private int bool2int(final boolean value) {
        if (value == true) {
            return 1;
        }
        return 0;
    }
}
