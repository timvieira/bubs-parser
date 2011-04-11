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

import java.util.Arrays;
import java.util.PriorityQueue;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CellConstraints;
import edu.ohsu.cslu.parser.cellselector.PerceptronBeamWidthFactory.PerceptronBeamWidth;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class BeamSearchChartParser<G extends LeftHashGrammar, C extends CellChart> extends
        ChartParser<LeftHashGrammar, CellChart> {

    PriorityQueue<ChartEdge> agenda;
    int origBeamWidth, beamWidth, cellPushed, cellPopped, cellConsidered, numReparses;
    float globalBestFOM, globalBeamDelta, origGlobalBeamDelta;
    float localBestFOM, localBeamDelta, origLocalBeamDelta;
    int origFactoredBeamWidth, factoredBeamWidth, reparseFactor;
    boolean hasPerceptronBeamWidth;

    // int addToBeamWidth = 0;

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

        // if (ParserDriver.param3 != -1) {
        // addToBeamWidth = (int) ParserDriver.param3;
        // }
    }

    @Override
    protected void initSentence(final int[] tokens) {
        chart = new CellChart(tokens, opts.viterbiMax(), this);
        numReparses = -1;

        final double startTimeMS = System.currentTimeMillis();
        edgeSelector.init(chart);
        final double endTimeMS = System.currentTimeMillis();
        currentInput.fomInitSec = (float) ((endTimeMS - startTimeMS) / 1000.0);

        cellSelector.initSentence(this);
        currentInput.ccInitSec = (float) ((System.currentTimeMillis() - endTimeMS) / 1000.0);
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) {
        initSentence(tokens);

        numReparses = -1;
        while (numReparses < opts.reparse && chart.hasCompleteParse(grammar.startSymbol) == false) {
            numReparses++;
            globalBestFOM = Float.NEGATIVE_INFINITY;

            reparseFactor = (int) Math.pow(2, numReparses);
            // Math.max to prevent overflow problems
            beamWidth = Math.max(origBeamWidth * reparseFactor, origBeamWidth);
            factoredBeamWidth = Math.max(origFactoredBeamWidth * reparseFactor, origFactoredBeamWidth);
            globalBeamDelta = Math.max(origGlobalBeamDelta * reparseFactor, origGlobalBeamDelta);
            localBeamDelta = Math.max(origLocalBeamDelta * reparseFactor, origLocalBeamDelta);

            BaseLogger.singleton().finest(
                "INFO: reparseNum=" + (numReparses + 1) + "beamWidth=" + beamWidth + " globalThresh="
                        + globalBeamDelta + " localThresh=" + localBeamDelta + " factBeamWidth="
                        + factoredBeamWidth);

            cellSelector.reset();
            while (cellSelector.hasNext()) {

                final short[] startAndEnd = cellSelector.next();
                visitCell(startAndEnd[0], startAndEnd[1]);

                currentInput.totalPushes += cellPushed;
                currentInput.totalPops += cellPopped;
                currentInput.totalConsidered += cellConsidered;

                // if (opts.collectDetailedStatistics) {
                // final HashSetChartCell cell = chart.getCell(startAndEnd[0], startAndEnd[1]);
                // System.out.println(cell.width() + " [" + cell.start() + "," + cell.end() + "] #pop=" +
                // cellPopped
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
                currentInput.nLex += 1;
                // TODO: need to be able to get POS posteriors. We could use this as the FOM and rank just
                // like others
                // if (!only1BestPOS || ((BoundaryInOut) edgeSelector).get1bestPOSTag(start) ==
                // lexProd.parent) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
                if (hasCellConstraints == false || cc.isUnaryOpen(start, end)) {
                    for (final Production unaryProd : grammar.getUnaryProductionsWithChild(lexProd.parent)) {
                        currentInput.nLexUnary += 1;
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
        cellPushed = 0;
        cellPopped = 0;
        cellConsidered = 0;

        if (cellSelector.hasCellConstraints()) {
            // reset beamWidth at each cell since it may be modified based on following conditions
            // Math.max to prevent overflow errors
            beamWidth = Math.max(origBeamWidth * reparseFactor, origBeamWidth);

            if (hasPerceptronBeamWidth) {
                beamWidth = Math.min(cellSelector.getBeamWidth(start, end), beamWidth);
            } else if (cellSelector.getCellConstraints().isCellOnlyFactored(start, end)) {
                beamWidth = factoredBeamWidth;
            }
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
