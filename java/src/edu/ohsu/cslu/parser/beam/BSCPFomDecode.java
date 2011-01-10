package edu.ohsu.cslu.parser.beam;

import java.util.Arrays;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.ParserUtil;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BSCPFomDecode extends BSCPPruneViterbi {

    // NOTE: this isn't exactly the same as the exaustive method since
    // only the edges with the *locally* optimal FOM for each parent=A
    // are compared to each other
    float maxcFOM[][][];
    ChartEdge maxcBackptr[];
    boolean maxProduct = true;

    public BSCPFomDecode(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);

        if (ParserDriver.param3 != -1) {
            maxProduct = false;
        }
    }

    @Override
    protected void initSentence(final int[] tokens) {
        chart = new CellChart(tokens, opts.viterbiMax(), this);

        final int n = tokens.length;
        maxcFOM = new float[n][n + 1][grammar.numNonTerms()];
    }

    private void updateMaxcFOM(final ChartEdge edge) {
        final int start = edge.start();
        final int end = edge.end();
        final int A = edge.prod.parent;
        float score;

        if (edge.prod.isBinaryProd()) {
            final int midpt = edge.midpt();
            if (maxProduct) {
                score = edge.fom + maxcFOM[start][midpt][edge.prod.leftChild]
                        + maxcFOM[midpt][end][edge.prod.rightChild];
            } else {
                score = (float) ParserUtil.logSum(edge.fom, ParserUtil.logSum(
                        maxcFOM[start][midpt][edge.prod.leftChild], maxcFOM[midpt][end][edge.prod.rightChild]));
            }
        } else if (edge.prod.isUnaryProd()) {
            if (maxProduct) {
                score = edge.fom + maxcFOM[start][end][edge.prod.child()];
            } else {
                score = (float) ParserUtil.logSum(edge.fom, maxcFOM[start][end][edge.prod.child()]);
            }
        } else {
            score = edge.fom;
        }

        if (score > maxcFOM[start][end][A]) {
            maxcFOM[start][end][A] = score;
            maxcBackptr[A] = edge;
            logger.fine("" + edge);
        }
    }

    @Override
    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
        final ChartEdge unaryEdge;

        agenda = new PriorityQueue<ChartEdge>();
        for (final ChartEdge viterbiEdge : bestEdges) {
            // if (viterbiEdge != null && viterbiEdge.fom > bestFOM - beamDeltaThresh) {
            if (fomCheckAndUpdate(viterbiEdge)) {
                agenda.add(viterbiEdge);
                cellPushed++;
            }
        }

        Arrays.fill(maxcFOM[cell.start()][cell.end()], Float.NEGATIVE_INFINITY);
        maxcBackptr = new ChartEdge[grammar.numNonTerms()];

        if (cell.width() == 1) {
            for (final ChartEdge lexEdge : cell.getBestEdgeList()) {
                updateMaxcFOM(lexEdge);
            }
        }

        ChartEdge edge = agenda.poll();
        while (edge != null && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
            cellPopped++;
            updateMaxcFOM(edge);
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

        // while (agenda.isEmpty() == false && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
        // edge = agenda.poll();
        // if (edge.inside() > cell.getInside(edge.prod.parent)) {
        // cell.updateInside(edge);
        // cellPopped++;
        // updateMaxcFOM(edge);
        //
        // // Add unary productions to agenda so they can compete with binary productions
        // for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
        // unaryEdge = chart.new ChartEdge(p, cell);
        // final int nt = p.parent;
        // cellConsidered++;
        // if ((bestEdges[nt] == null || unaryEdge.fom > bestEdges[nt].fom)
        // && fomCheckAndUpdate(unaryEdge)) {
        // agenda.add(unaryEdge);
        // cellPushed++;
        // }
        // }
        // }
        // }

        cell.bestEdge = maxcBackptr;

        if (opts.collectDetailedStatistics) {
            System.out.println(cell.width() + " [" + cell.start() + "," + cell.end() + "] #pop=" + cellPopped
                    + " #push=" + cellPushed + " #considered=" + cellConsidered);
        }
    }
}
