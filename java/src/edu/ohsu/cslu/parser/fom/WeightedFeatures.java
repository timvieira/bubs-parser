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
package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.parser.chart.GoldChart;
import edu.ohsu.cslu.util.Strings;

public class WeightedFeatures extends FigureOfMerit {

    private static final long serialVersionUID = 1L;

    private Grammar grammar;
    private int numFeatures;
    private GoldChart goldChart;

    public WeightedFeatures(final Grammar grammar) {
        this.grammar = grammar;
        this.numFeatures = 4;
    }

    // @Override
    // public float calcFOM(final ChartEdge edge) {
    // // final double[] featVector = getFeatureVector(edge);
    // return 0; // model.score(featVector); // we want a regressor here, not a classifier
    // }

    public double[] getFeatureVector(final ChartEdge edge) {
        final double[] feats = new double[numFeatures];
        Arrays.fill(feats, 0.0);

        // TODO: extract features from edge

        return feats;
    }

    @Override
    public void train(final BufferedReader inStream) throws IOException {
        String line;
        final ParserDriver opts = new ParserDriver();
        // opts.cellSelectorType = CellSelectorType.LeftRightBottomTop;
        final TrainingParser parser = new TrainingParser(opts, (LeftListGrammar) grammar);

        while ((line = inStream.readLine()) != null) {
            final BinaryTree<String> goldTree = BinaryTree.read(line, String.class);
            goldChart = new GoldChart(goldTree, grammar);

            // fill chart
            final String sentence = Strings.join(goldTree.leafLabels(), " ");
            parser.parseSentence(sentence);

        }
    }

    private void updateModel(final ChartEdge edge, final CellChart chart) {
        final int A = edge.prod.parent, start = edge.start(), end = edge.end();
        if (edge.equals(goldChart.getCell(start, end).getBestEdge(A))) {
            // is gold edge
        } else {
            // not gold edge
        }

    }

    // private boolean isGoldEdge(final Production p, final int start, final int mid, final int end) {
    // final ChartEdge goldEdge = goldChart.getCell(start, end).getBestEdge(p.parent);
    // if (goldEdge == null)
    // return false;
    // if (!goldEdge.prod.equals(p))
    // return false;
    // if (p.isBinaryProd() && goldEdge.midpt() != mid)
    // return false;
    //
    // return true;
    // }

    protected class TrainingParser extends ChartParser<LeftListGrammar, CellChart> {

        public TrainingParser(final ParserDriver opts, final LeftListGrammar grammar) {
            super(opts, grammar);
        }

        @Override
        protected void computeInsideProbabilities(final short start, final short end) {
            final HashSetChartCell cell = chart.getCell(start, end);
            float leftInside, rightInside;
            ChartEdge edge;

            for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
                final HashSetChartCell leftCell = chart.getCell(start, mid);
                final HashSetChartCell rightCell = chart.getCell(mid, end);
                for (final int leftNT : leftCell.getLeftChildNTs()) {
                    leftInside = leftCell.getInside(leftNT);
                    for (final Production p : grammar.getBinaryProductionsWithLeftChild(leftNT)) {
                        rightInside = rightCell.getInside(p.rightChild);
                        if (rightInside > Float.NEGATIVE_INFINITY) {
                            cell.updateInside(p, leftCell, rightCell, p.prob + leftInside + rightInside);
                        }

                        edge = chart.new ChartEdge(p, leftCell, rightCell);
                        updateModel(edge, chart);
                    }
                }
            }

            for (final int childNT : cell.getNTs()) {
                for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                    cell.updateInside(p, p.prob + cell.getInside(childNT));

                    edge = chart.new ChartEdge(p, cell);
                    updateModel(edge, chart);
                }
            }
        }
    }

}
