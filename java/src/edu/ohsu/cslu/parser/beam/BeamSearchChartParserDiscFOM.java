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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.Chart.SimpleChartEdge;
import edu.ohsu.cslu.parser.fom.DiscriminativeFOM;
import edu.ohsu.cslu.parser.fom.DiscriminativeFOM.DiscriminativeFOMSelector;
import edu.ohsu.cslu.parser.fom.TrainFOM;

/**
 * Beam search chart parser which performs grammar intersection by iterating over grammar rules matching the observed
 * non-terminals in the left child child pairs in the cartesian product of non-terminals observed in child cells.
 * 
 * @author Nathan Bodenstab
 */
public class BeamSearchChartParserDiscFOM<G extends LeftHashGrammar, C extends CellChart> extends
        BeamSearchChartParser<LeftHashGrammar, CellChart> {

    DiscriminativeFOM fomModelTraining;
    HashMap<String, LinkedList<SimpleChartEdge>> goldEdgesBySpan;
    public int numGoldRankOne, numGold, sumGoldRank;

    public BeamSearchChartParserDiscFOM(final ParserDriver opts, final LeftHashGrammar grammar,
            final DiscriminativeFOM fomModelTraining) {
        super(opts, grammar);

        this.fomModelTraining = fomModelTraining;
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);

        if (parseTask.inputTree.isBinaryTree() == false) {
            BaseLogger.singleton().severe("ERROR: input trees must be binarized.  Exiting.");
            System.exit(1);
        }
        // Trying to binarize on the fly doesn't work because there is no markovization in
        // the tree.binarize() function like there is in the grammar generating code, but that
        // uses ParseTree, not NaryTree.
        final BinaryTree<String> binaryGoldTree = parseTask.inputTree.binarize(grammar.grammarFormat,
                grammar.binarization());
        final Collection<SimpleChartEdge> goldEdges = Util.getEdgesFromTree(binaryGoldTree, grammar);
        goldEdgesBySpan = Util.edgeListBySpan(goldEdges);

        // for (final String key : goldEdgesBySpan.keySet()) {
        // for (final SimpleChartEdge e : goldEdgesBySpan.get(key)) {
        // System.out.println(key + "\t" + e.toString(grammar));
        // }
        // }
        // System.exit(1);

        numGoldRankOne = 0;
        numGold = 0;
        sumGoldRank = 0;
    }

    protected ChartEdge getOracleEdge(final short start, final short end) {
        // TODO: what to do when gold constituent isn't in beam? Huang takes the constituent with the best F1.
        // Could also just penalize all and not reward any, or reward the gold even if it isn't there.
        return null;
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {

        // Get binary gold edge from input tree
        final String key = String.format("%d,%d", cell.start(), cell.end());
        final LinkedList<SimpleChartEdge> goldEdges = goldEdgesBySpan.get(key);
        SimpleChartEdge simpleBinaryGoldEdge = null;
        if (goldEdges != null) {
            for (final SimpleChartEdge e : goldEdges) {
                if (e.mid != -1) {
                    simpleBinaryGoldEdge = e;
                    numGold += 1;
                }
            }
        }

        ChartEdge edge = agenda.poll();
        // final ChartEdge rank1edge = chart.new ChartEdge(edge.prod, edge.leftCell, edge.rightCell);
        final ChartEdge rank1edge = edge;
        ChartEdge binaryGoldEdge = null;
        while (edge != null && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
            cellPopped++;
            if (simpleBinaryGoldEdge != null && simpleBinaryGoldEdge.equals(edge)) {
                binaryGoldEdge = edge;
                sumGoldRank += cellPopped;
                if (cellPopped == 1) {
                    numGoldRankOne += 1;
                }
                if (TrainFOM.debug) {
                    System.out.format("RANK: Popped gold edge rank=%d %s\n", cellPopped, edge.toString());
                }
            }

            if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);

                // Add unary productions to agenda so they can compete with binary productions
                // No unary productions are added if CellConstraints are turned on and this cell
                // is only open to factored non-terms because getUnaryProd..() will return null
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    addEdgeToCollection(chart.new ChartEdge(p, cell));
                }
            }
            edge = agenda.poll();
        }

        if (rank1edge != null && simpleBinaryGoldEdge != null && rank1edge != binaryGoldEdge) {
            float oldFOM, newFOM;

            // reward gold edge
            if (binaryGoldEdge != null) {
                // If we found the gold edge an rank > 1 ...
                oldFOM = binaryGoldEdge.fom;
                final SparseBitVector feats = chart.getEdgeFeatures(binaryGoldEdge, true);
                fomModelTraining.trainBinaryInstance(1, feats, binaryGoldEdge.inside());
                if (TrainFOM.debug) {
                    newFOM = ((DiscriminativeFOMSelector) this.fomModel).calcFOM(new SimpleChartEdge(binaryGoldEdge),
                            binaryGoldEdge.inside());
                    System.out.format("  Reward: %s oldFOM=%f newFOM=%f featIndex=%s\n", binaryGoldEdge.toString(),
                            oldFOM, newFOM, featIndexStr(feats));
                }
            } else {
                // Gold edge was not found; improvise update by rewarding edge features but not inside prob
                if (TrainFOM.debug) {
                    System.out.format("RANK: Popped gold edge rank=NOT_FOUND %s\n",
                            simpleBinaryGoldEdge.toString(grammar));
                }
                oldFOM = ((DiscriminativeFOMSelector) this.fomModel).calcFOM(simpleBinaryGoldEdge, 0);
                final SparseBitVector feats = chart.getEdgeFeatures(simpleBinaryGoldEdge, true);
                // since we didn't find the gold edge, we don't have the inside score for it
                // so take the inside score of the rank1edge (negating the effects of the inside score
                // on the perceptron weight update)
                fomModelTraining.trainBinaryInstance(1, feats, rank1edge.inside());
                if (TrainFOM.debug) {
                    if (TrainFOM.debug) {
                        newFOM = ((DiscriminativeFOMSelector) this.fomModel).calcFOM(simpleBinaryGoldEdge, 0);
                        System.out.format("  Reward: %s (not found) oldFOM=%f newFOM=%f featIndex=%s\n",
                                simpleBinaryGoldEdge.toString(grammar), oldFOM, newFOM, featIndexStr(feats));
                    }
                }
            }

            // Penalize the first edge if it is not the gold edge AND there is a gold edge in the cell
            oldFOM = rank1edge.fom;
            final SparseBitVector feats = chart.getEdgeFeatures(rank1edge, true);
            fomModelTraining.trainBinaryInstance(-1, feats, rank1edge.inside());
            if (TrainFOM.debug) {
                newFOM = ((DiscriminativeFOMSelector) this.fomModel).calcFOM(new SimpleChartEdge(rank1edge),
                        rank1edge.inside());
                System.out.format("  PnlzR1: %s oldFOM=%f newFOM=%f featIndex=%s\n", rank1edge.toString(), oldFOM,
                        newFOM, featIndexStr(feats));
            }
        }
    }

    private String featIndexStr(final SparseBitVector feats) {
        String s = "[";
        for (final int i : feats.elements()) {
            s += i + " ";
        }
        return s + "]";
    }

    @Override
    public String getStats() {
        // return super.getStats() + " numReparses=" + numReparses;
        return String.format(" numReparses=%d numGold=%d numGoldRankOne=%d pctGoldRankOne=%1.2f", numReparses, numGold,
                numGoldRankOne, (float) (numGoldRankOne) / numGold * 100);
    }
}
