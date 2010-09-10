package edu.ohsu.cslu.parser.beam;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.util.ParseTree;

public class BeamSearchChartParser<G extends LeftHashGrammar, C extends CellChart> extends
        ChartParser<LeftHashGrammar, CellChart> {

    PriorityQueue<ChartEdge> agenda;
    int beamWidth, totalPushed = 0, totalPopped = 0, totalConsidered = 0;
    int cellPushed, cellPopped, cellConsidered;
    float fomInitSeconds, beamDeltaThresh;

    public BeamSearchChartParser(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);

        beamWidth = (int) ParserDriver.param1;
        if (beamWidth < 0)
            beamWidth = 9999;

        // logBeamDeltaThresh = ParserDriver.param2;
        beamDeltaThresh = 9999;
        if (beamDeltaThresh < 0)
            beamDeltaThresh = 30;
    }

    @Override
    protected void initParser(final int[] tokens) {
        chart = new CellChart(tokens, opts.viterbiMax, this);
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) throws Exception {
        initParser(tokens);
        cellSelector.init(this);

        final double startTimeMS = System.currentTimeMillis();
        edgeSelector.init(chart);
        fomInitSeconds = (float) ((System.currentTimeMillis() - startTimeMS) / 1000.0);

        // Lexical productions are done during the main loop now
        // addLexicalProductions(tokens);

        while (cellSelector.hasNext() && !chart.hasCompleteParse(grammar.startSymbol)) {
            cellPushed = 0;
            cellPopped = 0;
            cellConsidered = 0;

            final short[] startAndEnd = cellSelector.next();
            visitCell(startAndEnd[0], startAndEnd[1]);

            totalPushed += cellPushed;
            totalPopped += cellPopped;
            totalConsidered += cellConsidered;
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);
        ChartEdge edge;

        boolean onlyFactored = false;
        if (cellSelector.type == CellSelector.CellSelectorType.CSLUT) {
            onlyFactored = ((CSLUTBlockedCells) cellSelector).isCellOpenOnlyToFactored(start, end);
        }

        edgeCollectionInit();

        if (end - start == 1) {
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.tokens[start])) {
                edge = chart.new ChartEdge(lexProd, cell);
                addEdgeToCollection(edge);
            }
        } else {
            for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
                final HashSetChartCell leftCell = chart.getCell(start, mid);
                final HashSetChartCell rightCell = chart.getCell(mid, end);
                for (final int leftNT : leftCell.getLeftChildNTs()) {
                    for (final int rightNT : rightCell.getRightChildNTs()) {
                        for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
                            if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
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
        cellPushed++;
        cellConsidered++;
    }

    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean edgeBelowThresh = false;
        float bestFOM = Float.NEGATIVE_INFINITY;
        if (!agenda.isEmpty()) {
            bestFOM = agenda.peek().fom;
        }

        while (agenda.isEmpty() == false && cellPopped < beamWidth && !edgeBelowThresh) {
            edge = agenda.poll();
            if (edge.fom < bestFOM - beamDeltaThresh) {
                edgeBelowThresh = true;
            } else if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
                cellPopped++;

                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = chart.new ChartEdge(p, cell);
                    if (unaryEdge.fom > bestFOM - beamDeltaThresh) {
                        addEdgeToCollection(unaryEdge);
                    }
                }
            }
        }
    }

    @Override
    public String getStats() {
        return super.getStats() + " agendaPop=" + totalPopped + " agendaPush=" + totalPushed + " fomInitSec="
                + fomInitSeconds;
    }
}
