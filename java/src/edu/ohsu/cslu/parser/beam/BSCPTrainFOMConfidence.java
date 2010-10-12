package edu.ohsu.cslu.parser.beam;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPTrainFOMConfidence extends BSCPPruneViterbi {

    int nPopBinary, nPopUnary, nGoldEdges;

    public BSCPTrainFOMConfidence(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);
        ChartEdge edge;

        // NOTE: we want the CSLUT span scores, but we don't want to block the cells
        // boolean onlyFactored = false;
        // if (cellSelector.type == CellSelector.CellSelectorType.CSLUT) {
        // onlyFactored = ((CSLUTBlockedCells) cellSelector).isCellOpenOnlyToFactored(start, end);
        // }

        edgeCollectionInit();

        if (end - start == 1) {
            // lexical and unary productions can't compete in the same agenda until their FOM
            // scores are changed to be comparable
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.tokens[start])) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
                for (final Production unaryProd : grammar.getUnaryProductionsWithChild(lexProd.parent)) {
                    addEdgeToCollection(chart.new ChartEdge(unaryProd, cell));
                }

            }
        } else {
            for (int mid = start + 1; mid < end; mid++) { // mid point
                final HashSetChartCell leftCell = chart.getCell(start, mid);
                final HashSetChartCell rightCell = chart.getCell(mid, end);
                for (final int leftNT : leftCell.getLeftChildNTs()) {
                    for (final int rightNT : rightCell.getRightChildNTs()) {
                        for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
                            // if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
                            edge = chart.new ChartEdge(p, leftCell, rightCell);
                            addEdgeToCollection(edge);
                            // }
                        }
                    }
                }
            }
        }

        addEdgeCollectionToChart(cell);
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
            logger.info("ERROR: Beam Search parser with stats must provide gold trees as input");
            System.exit(1);
        }

        final List<Chart.ChartEdge> goldEdges = currentInput.inputTreeChart.getEdgeList(cell.start(), cell.end());

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

        boolean hasGoldEdge = false;
        nGoldEdges = goldEdges.size();
        hasGoldEdge = (nGoldEdges > 0);
        if (hasGoldEdge) {
            goldRank = 99999;
        }

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
        System.out.println("DSTAT: " + goldRank + " " + floatArray2Str(getCellFeatures(cell.start(), cell.end())));
    }

    public Float[] getCellFeatures(final int start, final int end) {
        final List<Float> feats = new LinkedList<Float>();
        final int span = end - start;

        feats.add(((CSLUTBlockedCells) cellSelector).getCurStartScore(start));
        feats.add(((CSLUTBlockedCells) cellSelector).getCurEndScore(end));
        feats.add((float) chart.size());
        feats.add((float) span);
        feats.add(span / (float) chart.size());
        feats.add((float) int2bool(span == 1));

        return feats.toArray(new Float[feats.size()]);
    }

    // public String getCellFeaturesStr(final int start, final int end) {
    // final float[] outsideLeftPOS = ((BoundaryInOut) edgeSelector).fwdbkw[start];
    // return chart.size() + " " + (end - start) + " " + start + " " + end + " " + floatArray2Str(outsideLeftPOS);
    // }

    private String floatArray2Str(final Float[] data) {
        String result = "";
        for (final float val : data) {
            // result += Math.pow(Math.E, val) + " ";
            result += val + " ";
        }
        return result.trim();
    }

    private int int2bool(final boolean val) {
        if (val == true) {
            return 1;
        }
        return 0;
    }
}
