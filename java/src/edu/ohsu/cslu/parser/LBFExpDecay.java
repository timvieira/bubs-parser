package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.ParseTree;

public class LBFExpDecay extends LBFPruneViterbi {

    boolean resultRun = false;
    Chart resultChart;

    public LBFExpDecay(final Grammar grammar, final EdgeSelector edgeFOM, final CellSelector cellSelector) {
        super(grammar, edgeFOM, cellSelector);
    }

    @Override
    protected void addEdgeCollectionToChart(final ChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean addedEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;

        final float maxPops = ParserDriver.param1;
        final int minPops = 3;

        // min/max
        // final float minPops = ParserDriver.param2;
        // maxEdgesToAdd = (int) ((chartSize - cell.width() + 1) / (float) chartSize * (maxPops - minPops) + minPops);
        // System.out.println(" max=" + maxPops + " min=" + minPops + " width=" + cell.width() + " pops=" + maxEdgesToAdd);

        // slope
        // final float slope = ParserDriver.param2;
        // maxEdgesToAdd = (int) (maxPops - (cell.width() * slope));

        // exp decay
        final float spanRatio = cell.width() / (float) chart.size();
        // maxEdgesToAdd = (int) Math.ceil(maxPops * Math.exp(-1 * spanRatio * ParserDriver.param2));

        // adpt exp decay
        // float adaptDecay = (float) Math.log(Math.max(1, chartSize - ParserDriver.param2));

        // adapt exp decay #2
        // final float adaptDecay = (float) ((float) Math.log(Math.max(1, chartSize - 2)) / Math.log(ParserDriver.param2));

        // adapt exp decay #3
        // final float adaptDecay = (float) ((float) Math.log(Math.max(1, chartSize - ParserDriver.param2)) / Math.log(5));

        final float adaptDecay = (float) ((float) Math.log(Math.max(1, chart.size() - 3)) / Math.log(5));

        // adapt exp decay strong
        // final float adaptDecay = Math.max(1, chartSize - 5) / ParserDriver.param2;

        maxEdgesToAdd = (int) Math.ceil(maxPops * Math.exp(-1 * spanRatio * adaptDecay));

        if (maxEdgesToAdd < minPops) {
            maxEdgesToAdd = minPops;
        }

        agenda = new PriorityQueue<ChartEdge>();
        for (int i = 0; i < grammar.numNonTerms(); i++) {
            if (bestEdges[i] != null && bestEdges[i].fom > bestFOM - logBeamDeltaThresh) {
                agenda.add(bestEdges[i]);
                nAgendaPush++;
            }
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd && !edgeBelowThresh) {
            edge = agenda.poll();
            if (edge.fom < bestFOM - logBeamDeltaThresh) {
                edgeBelowThresh = true;
            } else {
                addedEdge = cell.addEdge(edge);
                if (addedEdge) {
                    numAdded++;

                    // if (resultRun == true) {
                    // try {
                    // if (resultChart.getChartCell(cell.start(), cell.end()).hasEdge(edge)) {
                    // System.out.println("train: " + numAdded + " " + chartSize + " " + cell.width() + " " + (cell.width() / (float) chartSize));
                    // }
                    // } catch (final Exception e) {
                    // e.printStackTrace();
                    // }
                    // }

                    // Add unary productions to agenda so they can compete with binary productions
                    for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                        final float prob = p.prob + edge.inside;
                        unaryEdge = new ChartEdge(p, cell, prob, edgeSelector);
                        if ((bestEdges[p.parent] == null || bestEdges[p.parent].fom < unaryEdge.fom) && unaryEdge.fom > bestFOM - logBeamDeltaThresh) {
                            agenda.add(unaryEdge);
                            nAgendaPush++;
                        }
                    }
                }
            }
        }
    }

    public void runWithResult(final String sentence, final ParseTree bestParseTree) throws Exception {
        resultRun = true;
        resultChart = bestParseTree.convertToChart(grammar);
        findBestParse(sentence);
        resultRun = false;
    }
}
