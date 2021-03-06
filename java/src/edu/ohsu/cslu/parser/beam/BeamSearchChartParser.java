/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.parser.beam;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.PriorityQueue;

import cltool4j.BaseLogger;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.PerceptronBeamWidthModel.PerceptronBeamWidth;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

/**
 * Beam search chart parser which performs grammar intersection by iterating over grammar rules matching the observed
 * non-terminals in the left child child pairs in the cartesian product of non-terminals observed in child cells.
 * 
 * @author Nathan Bodenstab
 */
public class BeamSearchChartParser<G extends LeftHashGrammar, C extends CellChart> extends
        ChartParser<LeftHashGrammar, CellChart> {

    PriorityQueue<ChartEdge> agenda;
    int origBeamWidth, beamWidth, cellPushed, cellPopped, cellConsidered, numReparses;
    float globalBestFOM, globalBeamDelta, origGlobalBeamDelta;
    float localBestFOM, localBeamDelta, origLocalBeamDelta;
    int origFactoredBeamWidth, factoredBeamWidth, reparseFactor;
    boolean hasPerceptronBeamWidth;

    public BeamSearchChartParser(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
        hasPerceptronBeamWidth = this.cellSelector instanceof PerceptronBeamWidth;

        setBeamTuneParamsFromOptions();

        BaseLogger.singleton().fine(
                "INFO: beamWidth=" + origBeamWidth + " globalDelta=" + origGlobalBeamDelta + " localDelta="
                        + origLocalBeamDelta + " factBeamWidth=" + origFactoredBeamWidth);
    }

    protected void setBeamTuneParamsFromOptions() {
        final ConfigProperties props = GlobalConfigProperties.singleton();
        origBeamWidth = props.getIntProperty("maxBeamWidth", Integer.MAX_VALUE);
        origLocalBeamDelta = props.getFloatProperty("maxLocalDelta", Float.POSITIVE_INFINITY);
        origGlobalBeamDelta = props.getFloatProperty("maxGlobalDelta", Float.POSITIVE_INFINITY);
        origFactoredBeamWidth = props.getIntProperty("maxFactoredBeamWidth", origBeamWidth);
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
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        chart = new CellChart(parseTask, this);

        final long startTimeMS = System.currentTimeMillis();
        figureOfMerit.initSentence(parseTask, chart);
        final long endTimeMS = System.currentTimeMillis();
        parseTask.fomInitMs = endTimeMS - startTimeMS;

        cellSelector.initSentence(this, parseTask);
        parseTask.ccInitMs = System.currentTimeMillis() - endTimeMS;
    }

    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {
        numReparses = 0;
        initSentence(parseTask);

        for (final Parser.ReparseStrategy.Stage stage : opts.reparseStrategy.stages()) {

            // This parser only implements beam-doubling, so ignore any other reparsing stages
            // if (stage != Parser.ReparseStrategy.Stage.DOUBLE) {
            // continue;
            // }

            updateBeamParams(parseTask);
            while (cellSelector.hasNext()) {

                final short[] startAndEnd = cellSelector.next();
                computeInsideProbabilities(chart.getCell(startAndEnd[0], startAndEnd[1]));

                parseTask.totalPushes += cellPushed;
                parseTask.totalPopulatedEdges += cellPopped;
                parseTask.totalConsidered += cellConsidered;
            }

            if (chart.hasCompleteParse(grammar.startSymbol)) {
                return chart.extractBestParse(grammar.startSymbol);
            }

            numReparses++;
        }

        return null;
    }

    protected void updateBeamParams(final ParseTask parseTask) {
        globalBestFOM = Float.NEGATIVE_INFINITY;

        reparseFactor = (int) Math.pow(2, numReparses);
        // Math.max to prevent overflow problems
        beamWidth = Math.max(origBeamWidth * reparseFactor, origBeamWidth);
        factoredBeamWidth = Math.max(origFactoredBeamWidth * reparseFactor, origFactoredBeamWidth);
        globalBeamDelta = Math.max(origGlobalBeamDelta * reparseFactor, origGlobalBeamDelta);
        localBeamDelta = Math.max(origLocalBeamDelta * reparseFactor, origLocalBeamDelta);

        // The cellSelector is initially reset when created (on the first pass) but
        // we need to reset it when we're reparsing.
        if (numReparses > 0) {
            chart = new CellChart(parseTask, this);
            cellSelector.reset();
        }

        BaseLogger.singleton().finer(
                "INFO: reparseNum=" + numReparses + " beamWidth=" + beamWidth + " globalThresh=" + globalBeamDelta
                        + " localThresh=" + localBeamDelta + " factBeamWidth=" + factoredBeamWidth);
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell c) {
        final HashSetChartCell cell = (HashSetChartCell) c;
        final short start = c.start();
        final short end = c.end();
        ChartEdge edge;
        initCell(start, end);
        final boolean hasCellConstraints = cellSelector.hasCellConstraints();

        // lexical and unary productions can't compete in the same agenda until their FOM
        // scores are changed to be comparable
        if (end - start == 1) {
            Collection<Production> lexProdSet;
            if (ParserDriver.parseFromInputTags) {
                // add only one POS => word production given by input (or 1-best) tags
                lexProdSet = new LinkedList<Production>();
                final Production lexProd = grammar.getLexicalProduction((short) chart.parseTask.inputTags[start],
                        chart.parseTask.tokens[start]);
                if (lexProd == null) {
                    // TODO: create a new lexical production with a smoothed prob, maybe from the UNK classes
                    throw new IllegalArgumentException(String.format(
                            "ERROR: lexical production %s => %s not found in grammar",
                            grammar.nonTermSet.getSymbol(chart.parseTask.inputTags[start]),
                            grammar.lexSet.getSymbol(chart.parseTask.tokens[start])));
                }
                lexProdSet.add(lexProd);
            } else {
                // add all possible POS => word productions from grammar
                lexProdSet = grammar.getLexicalProductionsWithChild(chart.parseTask.tokens[start]);
            }

            for (final Production lexProd : lexProdSet) {
                // cell.updateInside(lexProd, cell, null, lexProd.prob);
                cell.updateInside(lexProd, lexProd.prob);
                if (hasCellConstraints == false || cellSelector.isUnaryOpen(start, end)) {
                    for (final Production unaryProd : grammar.getUnaryProductionsWithChild(lexProd.parent)) {
                        addEdgeToCollection(chart.new ChartEdge(unaryProd, cell));
                    }
                }
            }
        } else {
            final int midStart = cellSelector.getMidStart(start, end);
            final int midEnd = cellSelector.getMidEnd(start, end);
            final boolean onlyFactored = hasCellConstraints && cellSelector.isCellOnlyFactored(start, end);

            for (int mid = midStart; mid <= midEnd; mid++) { // mid point
                final HashSetChartCell leftCell = chart.getCell(start, mid);
                final HashSetChartCell rightCell = chart.getCell(mid, end);
                for (final int leftNT : leftCell.getLeftChildNTs()) {
                    for (final int rightNT : rightCell.getRightChildNTs()) {
                        for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
                            if (!onlyFactored || grammar.getOrAddNonterm((short) p.parent).isFactored()) {
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
            } else if (cellSelector.isCellOnlyFactored(start, end)) {
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

        ChartEdge edge = agenda.poll();
        while (edge != null && cellPopped < beamWidth && fomCheckAndUpdate(edge)) {
            cellPopped++;

            if (collectDetailedStatistics) {
                BaseLogger.singleton().finer("Popping: " + edge.toString());
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
    }

    @Override
    public String getStats() {
        // return super.getStats() + " numReparses=" + numReparses;
        return " numReparses=" + numReparses;
    }
}
