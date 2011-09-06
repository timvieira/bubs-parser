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

import java.util.List;
import java.util.PriorityQueue;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.Chart;

/*

 Once we start pruning the beam-width on cells, it will change
 the ranking of gold edges in higher cells.  We should be able
 to adapt to this by slowly using the perceptron predictions
 and refining them with the gold edge results over time. 

 TODO: (1) static learning of beam-width: fill all chart cells
 with all possible edges to determine the gold beam-width

 (2) graduated learning (there must be a better name for this):
 Init with all predictions for beam-width=INF.  At each cell,
 predict the beam-width and use it.  If a gold edge exists
 in the cell, but is not placed in the cell because the beam
 width is too low, penalize the cell (gold-rank=INF).  

 */

public class BSCPBeamConfTrain extends BSCPPruneViterbi {

    // int nPopBinary, nPopUnary, nGoldEdges;

    // Perceptron perceptron;
    String featTemplate;

    public BSCPBeamConfTrain(final ParserDriver opts, final LeftHashGrammar grammar, final String featTemplate) {
        super(opts, grammar);
        this.featTemplate = featTemplate;
    }

    // @Override
    // protected void visitCell(final short start, final short end) {
    // final HashSetChartCell cell = chart.getCell(start, end);
    // ChartEdge edge;
    //
    // // NOTE: we want the CSLUT span scores, but we don't want to block the cells
    // // boolean onlyFactored = false;
    // // if (cellSelector.type == CellSelector.CellSelectorType.CSLUT) {
    // // onlyFactored = ((CSLUTBlockedCells) cellSelector).isCellOpenOnlyToFactored(start, end);
    // // }
    //
    // initCell();
    //
    // if (end - start == 1) {
    // // lexical and unary productions can't compete in the same agenda until their FOM
    // // scores are changed to be comparable
    // for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.tokens[start])) {
    // cell.updateInside(lexProd, cell, null, lexProd.prob);
    // for (final Production unaryProd : grammar.getUnaryProductionsWithChild(lexProd.parent)) {
    // addEdgeToCollection(chart.new ChartEdge(unaryProd, cell));
    // }
    //
    // }
    // } else {
    // for (int mid = start + 1; mid < end; mid++) { // mid point
    // final HashSetChartCell leftCell = chart.getCell(start, mid);
    // final HashSetChartCell rightCell = chart.getCell(mid, end);
    // for (final int leftNT : leftCell.getLeftChildNTs()) {
    // for (final int rightNT : rightCell.getRightChildNTs()) {
    // for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
    // // if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
    // edge = chart.new ChartEdge(p, leftCell, rightCell);
    // addEdgeToCollection(edge);
    // // }
    // }
    // }
    // }
    // }
    // }
    //
    // addEdgeCollectionToChart(cell);
    // }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);

        if (parseTask.inputTree == null) {
            BaseLogger.singleton().info("ERROR: BSCPTrainFOMConfidence requires gold trees as input");
            System.exit(1);
        }
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {

        agenda = new PriorityQueue<ChartEdge>();
        for (final ChartEdge viterbiEdge : bestEdges) {
            if (viterbiEdge != null && this.fomCheckAndUpdate(viterbiEdge)) {
                agenda.add(viterbiEdge);
                cellPushed++;
            }
        }

        if (true)
            throw new IllegalArgumentException(
                    "InputTreeChart no longer exists.  Add code to BinaryTree to get nodes from a span");

        final List<Chart.ChartEdge> goldEdges = null;
        // final List<Chart.ChartEdge> goldEdges = (List<edu.ohsu.cslu.parser.chart.Chart.ChartEdge>)
        // currentInput.inputTreeChart
        // .getEdgeList(cell.start(), cell.end()).clone();

        // remove lexical entries (we're adding them all by default) but keep unaries in span=1 cells
        if (cell.width() == 1) {
            int i = 0;
            while (i < goldEdges.size()) {
                if (goldEdges.get(i).prod.isLexProd()) {
                    goldEdges.remove(i);
                } else {
                    i++;
                }
            }
        }

        boolean goldIsFactored = false;
        for (final Chart.ChartEdge goldEdge : goldEdges) {
            if (grammar.getNonterminal(goldEdge.prod.parent).isFactored) {
                goldIsFactored = true;
            }
        }

        int goldRank = 0;
        final int numGoldEdges = goldEdges.size();
        final boolean hasGoldEdge = numGoldEdges > 0;
        if (hasGoldEdge) {
            goldRank = 99999;
        }

        ChartEdge edge = agenda.poll();
        while (edge != null && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
            cellPopped++;

            // check if we just popped a gold edge
            if (hasGoldEdge) {
                Chart.ChartEdge goldEdgeAdded = null;
                for (final Chart.ChartEdge goldEdge : goldEdges) {
                    if (edge.equals(goldEdge)) {
                        goldEdgeAdded = goldEdge;
                    }
                }
                if (goldEdgeAdded != null) {
                    goldEdges.remove(goldEdgeAdded);
                    if (goldEdges.size() == 0) {
                        goldRank = cellPopped;
                    }
                }
            }

            // add edge to chart
            if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);

                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    cellConsidered++;
                    final ChartEdge unaryEdge = chart.new ChartEdge(p, cell);
                    if (fomCheckAndUpdate(unaryEdge)) {
                        agenda.add(unaryEdge);
                        cellPushed++;
                    }
                }
            }
            edge = agenda.poll();
        }

        final SparseBitVector cellFeats = chart.getCellFeatures(cell.start(), cell.end(),
                this.featTemplate.split("\\s+"));

        // goldRank goldIsFactored numGold isBaseCell : numFeats feat1 feat2 ...
        System.out.println(String.format("DSTAT: %d %d %d %d : %d %s", goldRank, bool2int(goldIsFactored),
                numGoldEdges, bool2int(cell.width() == 1), cellFeats.vectorLength(),
                Util.intArray2Str(cellFeats.elements())));
        // System.out.println(String.format("DSTAT: %d : %d %s", goldRank, cellFeats.vectorLength(),
        // ParserUtil.intArray2Str(cellFeats.elements())));

        // perceptron.learnOnline(cellFeats, rank2bool(goldRank));
    }

    // // for cell[i,j] that spans words w_i to w_j, we add the POS tags directly
    // // to the inside and outside of the constituent boundaries: i-1,i,j,j+1
    // public float[] getCellFeatures(final int start, final int end) {
    // final List<Float> feats = new ArrayList<Float>();
    // final int sentLen = this.currentInput.sentenceLength;
    //
    // // surrounding POS tags
    // feats.addAll(getPOSIndexFeatureArray(start - 2));
    // feats.addAll(getPOSIndexFeatureArray(start - 1));
    // feats.addAll(getPOSIndexFeatureArray(start));
    // feats.addAll(getPOSIndexFeatureArray(start + 1));
    //
    // feats.addAll(getPOSIndexFeatureArray(end - 1));
    // feats.addAll(getPOSIndexFeatureArray(end));
    // feats.addAll(getPOSIndexFeatureArray(end + 1));
    // feats.addAll(getPOSIndexFeatureArray(end + 2));
    //
    // final int span = end - start;
    // for (int i = 1; i <= 5; i++) {
    // feats.add(new Float(bool2int(span == i))); // span length 1-5
    // feats.add(new Float(bool2int(span >= i * 10))); // span > 10,20,30,40,50
    // feats.add(new Float(bool2int(span / sentLen >= i / 5.0))); // relative span width btwn 0 and 1
    // }
    //
    // // TOP cell
    // feats.add(new Float(bool2int(span == sentLen)));
    //
    // // edge cells ... probably get this from <none> POS tags
    // feats.add(new Float(bool2int(start == 0)));
    // feats.add(new Float(bool2int(end == sentLen)));
    //
    // // Turn ArrayList<Float> into float[]
    // final float[] tmp = new float[feats.size()];
    // for (int i = 0; i < feats.size(); i++) {
    // tmp[i] = feats.get(i);
    // }
    // return tmp;
    // }

    private int bool2int(final boolean value) {
        if (value == true) {
            return 1;
        }
        return 0;
    }

    // // I can't believe we have to jump through these hoops to get a variable sized float vector
    // private List<Float> getPOSIndexFeatureArray(final int start) {
    // final List<Float> feats = new ArrayList<Float>(posMap.size());
    // for (int i = 0; i < posMap.size(); i++) {
    // feats.add(new Float(0));
    // }
    // feats.set(getPOSIndex(start), new Float(1));
    // return feats;
    // }

    // private String floatArray2Str(final float[] data) {
    // String result = "";
    // for (final float val : data) {
    // result += val + " ";
    // }
    // return result.trim();
    // }

    // private int rank2bool(final int rank) {
    // if (rank <= 0) {
    // return 0;
    // }
    // return 1;
    // }

    // private String floatArray2Str(final Float[] data) {
    // String result = "";
    // for (final float val : data) {
    // // result += Math.pow(Math.E, val) + " ";
    // result += val + " ";
    // }
    // return result.trim();
    // }
    //
    // private int int2bool(final boolean val) {
    // if (val == true) {
    // return 1;
    // }
    // return 0;
    // }

    // public Float[] getCellFeatures(final int start, final int end) {
    // final List<Float> feats = new LinkedList<Float>();
    // final int span = end - start;
    //
    // feats.add(((CSLUTBlockedCells) cellSelector).getCurStartScore(start));
    // feats.add(((CSLUTBlockedCells) cellSelector).getCurEndScore(end));
    // feats.add((float) chart.size());
    // feats.add((float) span);
    // feats.add(span / (float) chart.size());
    // feats.add((float) int2bool(span == 1));
    //
    // return feats.toArray(new Float[feats.size()]);
    // }
}
