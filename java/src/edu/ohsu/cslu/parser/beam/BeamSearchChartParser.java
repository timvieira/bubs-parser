package edu.ohsu.cslu.parser.beam;

import java.util.Arrays;
import java.util.PriorityQueue;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CellConstraints;
import edu.ohsu.cslu.parser.cellselector.PerceptronBeamWidth;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BeamSearchChartParser<G extends LeftHashGrammar, C extends CellChart> extends
        ChartParser<LeftHashGrammar, CellChart> {

    PriorityQueue<ChartEdge> agenda;
    int origBeamWidth, beamWidth, cellPushed, cellPopped, cellConsidered, numReparses;
    float globalBestFOM, globalBeamDelta, origGlobalBeamDelta;
    float localBestFOM, localBeamDelta, origLocalBeamDelta, localWorstFOM;
    int origFactoredBeamWidth, factoredBeamWidth, reparseFactor;
    boolean hasPerceptronBeamWidth;

    int addToBeamWidth = 0;

    public BeamSearchChartParser(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
        origBeamWidth = Integer.MAX_VALUE;
        origGlobalBeamDelta = Float.POSITIVE_INFINITY;
        origLocalBeamDelta = Float.POSITIVE_INFINITY;
        hasPerceptronBeamWidth = this.cellSelector instanceof PerceptronBeamWidth;
        // hasCellConstraints = this.cellSelector instanceof OHSUCellConstraints;
        // if (hasCellConstraints) {
        // cellConstraints = (OHSUCellConstraints) cellSelector;
        // }

        setBeamTuneParams(opts.beamTune);

        BaseLogger.singleton().fine(
                "INFO: beamWidth=" + origBeamWidth + " globalDelta=" + origGlobalBeamDelta + " localDelta="
                        + origLocalBeamDelta + " factBeamWidth=" + origFactoredBeamWidth);
    }

    protected void setBeamTuneParams(final String beamTuneStr) {
        final String[] tokens = beamTuneStr.split(",");
        final float beamVals[] = new float[4];
        Arrays.fill(beamVals, Float.POSITIVE_INFINITY);

        for (int i = 0; i < tokens.length; i++) {
            if (!tokens[i].equals("") && !tokens[i].equals("INF") && !tokens[i].equals("inf")) {
                beamVals[i] = Float.parseFloat(tokens[i]);
            }
        }
        origBeamWidth = (int) beamVals[0];
        origGlobalBeamDelta = beamVals[1];
        origLocalBeamDelta = beamVals[2];
        if (beamVals[3] == Float.POSITIVE_INFINITY) {
            origFactoredBeamWidth = origBeamWidth;
        } else {
            origFactoredBeamWidth = (int) beamVals[3];
        }

        if (ParserDriver.param3 != -1) {
            addToBeamWidth = (int) ParserDriver.param3;
        }
    }

    @Override
    protected void initSentence(final int[] tokens) {
        chart = new CellChart(tokens, opts.viterbiMax(), this);
        globalBestFOM = Float.NEGATIVE_INFINITY;
        numReparses = -1;

        final double startTimeMS = System.currentTimeMillis();
        edgeSelector.init(chart);
        final double endTimeMS = System.currentTimeMillis();
        currentInput.fomInitSec = (float) ((endTimeMS - startTimeMS) / 1000.0);

        cellSelector.initSentence(this);
        currentInput.ccInitSec = (float) ((System.currentTimeMillis() - endTimeMS) / 1000.0);
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) throws Exception {
        initSentence(tokens);

        numReparses = -1;
        while (numReparses < opts.reparse && chart.hasCompleteParse(grammar.startSymbol) == false) {
            numReparses++;

            reparseFactor = (int) Math.pow(2, numReparses);
            beamWidth = origBeamWidth * reparseFactor;
            factoredBeamWidth = origFactoredBeamWidth * reparseFactor;
            globalBeamDelta = origGlobalBeamDelta * reparseFactor;
            localBeamDelta = origLocalBeamDelta * reparseFactor;

            cellSelector.reset();
            while (cellSelector.hasNext()) {

                final short[] startAndEnd = cellSelector.next();
                visitCell(startAndEnd[0], startAndEnd[1]);

                currentInput.totalPushes += cellPushed;
                currentInput.totalPops += cellPopped;
                currentInput.totalConsidered += cellConsidered;

                // if (opts.collectDetailedStatistics) {
                // final HashSetChartCell cell = chart.getCell(startAndEnd[0], startAndEnd[1]);
                // System.out.println(cell.width() + " [" + cell.start() + "," + cell.end() + "] #pop=" + cellPopped
                // + " #push=" + cellPushed + " #considered=" + cellConsidered);
                // }
            }
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);
        ChartEdge edge;
        initCell(start, end);
        final boolean hasCellConstraints = cellSelector.hasCellConstraints();
        final CellConstraints cc = cellSelector.getCellConstraints();

        // final boolean only1BestPOS = ParserDriver.param1 == 1 && (edgeSelector instanceof BoundaryInOut);

        if (end - start == 1) {
            // lexical and unary productions can't compete in the same agenda until their FOM
            // scores are changed to be comparable
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.tokens[start])) {
                // TODO: need to be able to get POS posteriors. We could use this as the FOM and rank just like others
                // if (!only1BestPOS || ((BoundaryInOut) edgeSelector).get1bestPOSTag(start) == lexProd.parent) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
                if (hasCellConstraints == false || cc.isUnaryOpen(start, end)) {
                    for (final Production unaryProd : grammar.getUnaryProductionsWithChild(lexProd.parent)) {
                        addEdgeToCollection(chart.new ChartEdge(unaryProd, cell));
                    }
                }
            }
        } else {
            final int midStart = cellSelector.getMidStart(start, end);
            final int midEnd = cellSelector.getMidEnd(start, end);
            final boolean onlyFactored = hasCellConstraints && cc.isCellOnlyFactored(start, end);

            for (int mid = midStart; mid <= midEnd; mid++) { // mid point
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

    protected void initCell(final short start, final short end) {
        agenda = new PriorityQueue<ChartEdge>();
        localBestFOM = Float.NEGATIVE_INFINITY;
        localWorstFOM = Float.POSITIVE_INFINITY;
        cellPushed = 0;
        cellPopped = 0;
        cellConsidered = 0;

        beamWidth = origBeamWidth * reparseFactor;
        if (hasPerceptronBeamWidth) {
            beamWidth = Math.min(cellSelector.getCellValue(start, end), beamWidth);
            // NOTE: adding value to beam width DOES NOT affect closed cells and also does not exceed maxBeamWidth
            // we can't simply add it to getCellValue() above because it overflows when prediction is Integer.MAX_VALUE
            beamWidth = Math.min(beamWidth + addToBeamWidth, origBeamWidth * reparseFactor);
        }
        // for PerceptronBeamWidth and CellConstraints
        if (cellSelector.hasCellConstraints() && cellSelector.getCellConstraints().isCellOnlyFactored(start, end)) {
            beamWidth = Math.min(beamWidth, factoredBeamWidth);
        }

    }

    protected boolean fomCheckAndUpdate(final ChartEdge edge) {
        final float fom = edge.fom;
        if ((fom < globalBestFOM - globalBeamDelta) || (fom < localBestFOM - localBeamDelta)) {
            return false;
        }

        if (fom > globalBestFOM) {
            globalBestFOM = fom;
            localBestFOM = fom;
        } else if (fom > localBestFOM) {
            localBestFOM = fom;
        }
        return true;
    }

    protected void addEdgeToCollection(final ChartEdge edge) {
        cellConsidered++;
        if (fomCheckAndUpdate(edge)) {
            agenda.add(edge);
            cellPushed++;
        }
    }

    protected void addEdgeCollectionToChart(final HashSetChartCell cell) {

        // int cellBeamWidth = beamWidth;
        // if (hasCellConstraints && cellConstraints.factoredParentsOnly(cell.start(), cell.end())) {
        // cellBeamWidth = factoredBeamWidth;
        // }

        ChartEdge edge = agenda.poll();
        while (edge != null && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
            cellPopped++;
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
    }

    @Override
    public String getStats() {
        return super.getStats() + " numReparses=" + numReparses;
    }
}
