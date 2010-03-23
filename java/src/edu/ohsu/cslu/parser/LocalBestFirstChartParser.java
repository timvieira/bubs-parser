package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.util.ParseTree;

public class LocalBestFirstChartParser<G extends LeftHashGrammar, C extends CellChart> extends ChartParser<LeftHashGrammar, CellChart> {

    int nAgendaPush;
    float fomInitSeconds;
    PriorityQueue<ChartEdge> agenda;

    int maxEdgesToAdd;
    float logBeamDeltaThresh;

    public LocalBestFirstChartParser(final ParserOptions opts, final LeftHashGrammar grammar) {
        super(opts, grammar);

        maxEdgesToAdd = (int) ParserOptions.param1;
        if (maxEdgesToAdd < 0)
            maxEdgesToAdd = 10;

        // logBeamDeltaThresh = ParserDriver.param2;
        logBeamDeltaThresh = 9999;
        if (logBeamDeltaThresh < 0)
            logBeamDeltaThresh = 30;
    }

    @Override
    protected void initParser(final int sentLength) {
        chart = new CellChart(sentLength, opts.viterbiMax, this);
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {
        HashSetChartCell cell;
        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);
        currentSentence = sentence;

        initParser(sent.length);
        cellSelector.init(this);
        addLexicalProductions(sent);

        final double startTimeMS = System.currentTimeMillis();
        edgeSelector.init(this);
        fomInitSeconds = (float) ((System.currentTimeMillis() - startTimeMS) / 1000.0);

        nAgendaPush = 0;
        while (cellSelector.hasNext() && !hasCompleteParse()) {
            final short[] startAndEnd = cellSelector.next();
            cell = chart.getCell(startAndEnd[0], startAndEnd[1]);
            visitCell(cell);
        }

        return extractBestParse();
    }

    @Override
    protected void addLexicalProductions(final int sent[]) throws Exception {
        HashSetChartCell cell;

        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                cell.updateInside(chart.new ChartEdge(lexProd, cell));
            }
        }
    }

    protected void visitCell(final HashSetChartCell cell) {
        final int start = cell.start(), end = cell.end();
        ChartEdge edge;

        boolean onlyFactored = false;
        if (cellSelector.type == CellSelector.CellSelectorType.CSLUT) {
            onlyFactored = ((CSLUTBlockedCells) cellSelector).isCellOpenOnlyToFactored(start, end);
        }

        edgeCollectionInit();

        if (end - start == 1) {
            for (final int pos : cell.getPosNTs()) {
                for (final Production p : grammar.getUnaryProductionsWithChild(pos)) {
                    // final float prob = p.prob + cell.getBestEdge(pos).inside;
                    edge = chart.new ChartEdge(p, cell);
                    addEdgeToCollection(edge);
                }
            }
        } else {
            for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
                final HashSetChartCell leftCell = chart.getCell(start, mid);
                final HashSetChartCell rightCell = chart.getCell(mid, end);
                for (final int leftNT : leftCell.getLeftChildNTs()) {
                    for (final int rightNT : rightCell.getRightChildNTs()) {
                        for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
                            if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
                                // final float prob = p.prob + leftCell.getInside(leftNT) + rightCell.getInside(rightNT);
                                edge = chart.new ChartEdge(p, leftCell, rightCell);
                                addEdgeToCollection(edge);
                            }
                        }
                    }
                }
            }
        }

        addEdgeCollectionToChart(cell);
    }

    protected void edgeCollectionInit() {
        agenda = new PriorityQueue<ChartEdge>();
    }

    protected void addEdgeToCollection(final ChartEdge edge) {
        agenda.add(edge);
        nAgendaPush++;
    }

    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;
        float bestFOM = Float.NEGATIVE_INFINITY;
        if (!agenda.isEmpty()) {
            bestFOM = agenda.peek().fom;
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd && !edgeBelowThresh) {
            edge = agenda.poll();
            if (edge.fom < bestFOM - logBeamDeltaThresh) {
                edgeBelowThresh = true;
            } else if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
                numAdded++;

                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = chart.new ChartEdge(p, cell);
                    if (unaryEdge.fom > bestFOM - logBeamDeltaThresh) {
                        addEdgeToCollection(unaryEdge);
                    }
                }
            }
        }
    }

    @Override
    public String getStats() {
        return super.getStats() + " agendaPush=" + nAgendaPush + " fomInitSec=" + fomInitSeconds;
    }
}
