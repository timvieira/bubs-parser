/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.parser.ml;

import java.util.Arrays;
import java.util.Iterator;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * Base class for parsers implementing inside-sum or inside-outside inference rather than simple Viterbi decoding. With
 * a chart containing inside or inside-outside (posterior) probabilities, we have a choice of decoding methods (see
 * {@link DecodeMethod} and {@link PackedArrayChart}).
 * 
 * @author Aaron Dunlop
 */
public abstract class BaseIoCphSpmlParser extends
        SparseMatrixLoopParser<InsideOutsideCscSparseMatrixGrammar, PackedArrayChart> {

    // TODO Move these constant to ParserDriver if it proves useful
    public final static String PROPERTY_LOG_SUM_DELTA = "logSumDelta";
    public final static String PROPERTY_APPROXIMATE_LOG_SUM = "approxLogSum";
    public final static String PROPERTY_INSIDE_ONLY = "insideOnly";

    protected final static boolean INSIDE_ONLY = GlobalConfigProperties.singleton().getBooleanProperty(
            PROPERTY_INSIDE_ONLY, false);
    protected final static boolean APPROXIMATE_SUM = GlobalConfigProperties.singleton().getBooleanProperty(
            PROPERTY_APPROXIMATE_LOG_SUM, false);
    protected final static float SUM_DELTA = GlobalConfigProperties.singleton().getFloatProperty(
            PROPERTY_LOG_SUM_DELTA, 20f);

    public BaseIoCphSpmlParser(final ParserDriver opts, final InsideOutsideCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {
        initChart(parseTask);
        insidePass();

        if (INSIDE_ONLY) {
            // Skip outside pass, and just populate all outside probabilities with 1
            Arrays.fill(chart.outsideProbabilities, 0, chart.chartArraySize(), 0f);

        } else {
            // To compute the outside probability of a non-terminal in a cell, we need the outside probability of the
            // cell's
            // parent, so we process downward from the top of the chart.

            // Outside pass
            final Iterator<short[]> reverseIterator = cellSelector.reverseIterator();

            while (reverseIterator.hasNext()) {
                final short[] startAndEnd = reverseIterator.next();
                final PackedArrayChartCell cell = chart.getCell(startAndEnd[0], startAndEnd[1]);
                computeOutsideProbabilities(cell);
            }
        }

        if (collectDetailedStatistics) {
            final long t3 = System.currentTimeMillis();
            final BinaryTree<String> parseTree = chart.decode();
            parseTask.extractTimeMs = System.currentTimeMillis() - t3;
            return parseTree;
        }

        return chart.decode();
    }

    /**
     * We retain only 1-best unary probabilities, and only if the probability of a unary child exceeds the sum of all
     * probabilities for that non-terminal as a binary child of parent cells)
     */
    protected final void computeUnaryOutsideProbabilities(final float[] tmpOutsideProbabilities) {

        // Iterate over populated parents (matrix rows)
        for (short parent = 0; parent < grammar.numNonTerms(); parent++) {

            if (tmpOutsideProbabilities[parent] == Float.NEGATIVE_INFINITY) {
                continue;
            }

            // Iterate over possible children (columns with non-zero entries)
            for (int i = grammar.csrUnaryRowStartIndices[parent]; i < grammar.csrUnaryRowStartIndices[parent + 1]; i++) {

                final short child = grammar.csrUnaryColumnIndices[i];
                final float jointProbability = grammar.csrUnaryProbabilities[i] + tmpOutsideProbabilities[parent];
                if (jointProbability > tmpOutsideProbabilities[child]) {
                    tmpOutsideProbabilities[child] = jointProbability;
                }
            }
        }
    }

    /**
     * Populates outside probabilities in the target cell
     * 
     * @param cell
     */
    protected abstract void computeOutsideProbabilities(final PackedArrayChartCell cell);
}
