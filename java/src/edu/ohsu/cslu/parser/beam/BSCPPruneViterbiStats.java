package edu.ohsu.cslu.parser.beam;

import java.util.List;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.util.Log;

public class BSCPPruneViterbiStats extends BSCPPruneViterbi {

    int nPopBinary, nPopUnary, nGoldEdges;

    public BSCPPruneViterbiStats(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean edgeBelowThresh = false;
        int goldRank = -1;
        cell.numEdgesAdded = 0;

        nPopBinary = 0;
        nPopUnary = 0;
        nGoldEdges = 0;

        agenda = new PriorityQueue<ChartEdge>();
        for (final ChartEdge viterbiEdge : bestEdges) {
            if (viterbiEdge != null && viterbiEdge.fom > bestFOM - beamDeltaThresh) {
                agenda.add(viterbiEdge);
                cellPushed++;
            }
        }

        if (currentInput.inputTreeChart == null) {
            Log.info(0, "ERROR: Beam Search parser with stats must provide gold trees as input");
            System.exit(1);
        }

        final List<Chart.ChartEdge> goldEdges = currentInput.inputTreeChart.getEdgeList(cell.start(), cell.end());
        boolean hasGoldEdge = false;
        nGoldEdges = goldEdges.size();
        hasGoldEdge = (nGoldEdges > 0);
        if (hasGoldEdge)
            goldRank = 99999;

        // I think a more fair comparison is when we don't stop once we reach the gold edge
        // since this effects the population of cells down stream.
        // while (agenda.isEmpty() == false && numAdded <= beamWidth && !edgeBelowThresh && !addedGoldEdges) {
        while (agenda.isEmpty() == false && cellPopped < beamWidth && !edgeBelowThresh) {
            edge = agenda.poll();

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
                        goldRank = cellPopped + 1;
                    }
                }
            }

            // add edge to chart
            if (edge.fom < bestFOM - beamDeltaThresh) {
                edgeBelowThresh = true;
            } else if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
                cellPopped++;

                if (edge.prod.isBinaryProd())
                    nPopBinary++;
                if (edge.prod.isUnaryProd())
                    nPopUnary++;

                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = chart.new ChartEdge(p, cell);
                    final int nt = p.parent;
                    if ((bestEdges[nt] == null || unaryEdge.fom > bestEdges[nt].fom)
                            && (unaryEdge.fom > bestFOM - beamDeltaThresh)) {
                        agenda.add(unaryEdge);
                    }
                }
            }
        }

        // if (nGoldEdges > 0) {
        // System.out.println("DSTAT: " + chart.size() + " " + cell.width() + " " + nGoldEdges + " " + goldRank + " "
        // + nPopBinary + " " + nPopUnary + " " + (nPopBinary + nPopUnary) + " ");
        // }
        System.out.println("DSTAT: " + goldRank + " " + getCellFeaturesStr(cell.start(), cell.end()));
    }

    final int NUM_FEATS = 4;

    public float[] getCellFeatures(final int start, final int end) {
        final float[] feats = new float[NUM_FEATS];
        feats[0] = chart.size();
        feats[1] = end - start;
        feats[2] = start;
        feats[3] = end;

        return feats;
    }

    public String getCellFeaturesStr(final int start, final int end) {
        final float[] outsideLeftPOS = ((BoundaryInOut) edgeSelector).fwdbkw[start];
        return chart.size() + " " + (end - start) + " " + start + " " + end + " " + floatArray2Str(outsideLeftPOS);
    }

    private String floatArray2Str(final float[] data) {
        String result = "";
        for (final float val : data) {
            // result += Math.pow(Math.E, val) + " ";
            result += val + " ";
        }
        return result.trim();
    }
}
