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
package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.Feature;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.BinaryPerceptronSet;
import edu.ohsu.cslu.perceptron.Classifier;

/**
 * Predicts cell beam width using a set of trained perceptron classifiers.
 * 
 * @author Nathan Bodenstab
 * @since 2011
 */
public class PerceptronBeamWidthModel implements CellSelectorModel {

    private static final long serialVersionUID = 1L;

    protected Classifier beamWidthModel;
    private boolean inferFactoredCells = false, classifyBaseCells = false;
    protected List<Feature> featureList;

    public PerceptronBeamWidthModel(final BufferedReader modelStream) {

        if (inferFactoredCells == false && classifyBaseCells == true) {
            throw new IllegalArgumentException("ERROR: got that wrong -- no models -fact +base");
        }

        try {
            modelStream.mark(10000);
            String line = modelStream.readLine();
            while (line != null && !line.trim().contains("# === ")) {
                // # PerceptronBeamWidthModel inferFactoredCells=0 classifyBaseCells=0
                if (line.startsWith("# PerceptronBeamWidthModel")) {
                    inferFactoredCells = line.split(" ")[2].split("=")[1].equals("1");
                    classifyBaseCells = line.split(" ")[3].split("=")[1].equals("1");
                }
                line = modelStream.readLine();
            }
            modelStream.reset();

            if ("# === BinaryPerceptronSet Model ===".equals(line)) {
                beamWidthModel = new BinaryPerceptronSet(modelStream);
            } else if ("# === Perceptron Model ===".equals(line)) {
                beamWidthModel = new AveragedPerceptron(modelStream);
            } else {
                throw new IllegalArgumentException("ERROR: Unknown beamconf model type on line: " + line);
            }
            modelStream.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        final ConfigProperties props = GlobalConfigProperties.singleton();
        final String beamModelBias = props.getProperty("beamModelBias");
        beamWidthModel.setBias(beamModelBias);

        BaseLogger.singleton().finer(
                "INFO: beamconf: inferFactoredCells=" + Util.bool2int(inferFactoredCells) + " classifyBaseCells="
                        + Util.bool2int(classifyBaseCells));

        this.featureList = Chart.featureTemplateStrToEnum(beamWidthModel.getFeatureTemplate().split("\\s+"));
    }

    public CellSelector createCellSelector() {
        return new PerceptronBeamWidth();
    }

    public class PerceptronBeamWidth extends CellConstraints {

        // TODO Switch to using single-dimensional array, indexed by cellIndex. Should be somewhat more cache-efficient
        private int beamWidthValues[][];
        private boolean onlyFactored[][];

        @Override
        public void initSentence(final ChartParser<?, ?> p) {
            super.initSentence(p);
            computeBeamWidthValues();
        }

        private void computeBeamWidthValues() {
            int guessBeamWidth, guessClass;
            final int n = parser.chart.parseTask.sentenceLength();
            // TODO Reuse existing arrays when possible
            beamWidthValues = new int[n][n + 1];
            onlyFactored = new boolean[n][n + 1];
            openCells = 0;
            int numOnlyFactored = 0, totalCells = 0;

            // Count classes if we're at a verbose logging level
            final int[] beamClassCounts = BaseLogger.singleton().isLoggable(Level.FINER) ? new int[beamWidthModel
                    .numClasses()] : null;

            // traverse in a top-down order so we can remember when we first see a non-empty cell
            // only works for right factored (berkeley) grammars right now.
            // for (int end = 1; end < n + 1; end++) {
            for (int start = 0; start < n; start++) {
                boolean foundOpenCell = false;
                // for (int start = 0; start < end; start++) {
                for (int end = n; end > start; end--) {
                    totalCells++;
                    if (end - start == 1 && classifyBaseCells == false) {
                        openCells++;
                        beamWidthValues[start][end] = Integer.MAX_VALUE;
                        // beamWidthValues[start][end] = maxBeamWidth;
                        // cellStats += String.format("%d,%d=%d ", start, end, maxBeamWidth);
                    } else {
                        final SparseBitVector feats = parser.chart.getCellFeatures(start, end, featureList);
                        // final SparseBitVector feats = parser.chart.getCellFeatures(start, end, beamWidthModel
                        // .getFeatureTemplate().split("\\s+"));
                        guessClass = beamWidthModel.classify(feats);
                        if (beamClassCounts != null) {
                            beamClassCounts[guessClass]++;
                        }
                        guessBeamWidth = (int) beamWidthModel.class2value(guessClass);

                        if (guessBeamWidth > 0) {
                            foundOpenCell = true;
                            openCells++;
                            // cellStats += String.format("%d,%d=%d ", start, end, guessBeamWidth > 0 ? 4 :
                            // 0);
                            // cellStats += String.format("%d,%d=%d ", start, end, guessBeamWidth);
                        } else if (inferFactoredCells && foundOpenCell) {
                            // need to allow factored productions for classifiers that don't predict these cells
                            // if (inferFactoredCells && guessBeamWidth == 0 && foundOpenCell) {
                            // guessBeamWidth = maxFactoredBeamWidth;
                            guessBeamWidth = Integer.MAX_VALUE;
                            onlyFactored[start][end] = true;
                            numOnlyFactored++;
                            openCells++;
                            // cellStats += String.format("%d,%d=2 ", start, end);
                        }

                        beamWidthValues[start][end] = guessBeamWidth;
                    }
                }
            }

            // init cell list here because we don't classify cells in bottom-up order above
            if (cellIndices == null || cellIndices.length < openCells * 2) {
                cellIndices = new short[openCells * 2];
            }
            int i = 0;
            for (short span = 1; span <= n; span++) {
                for (short start = 0; start < n - span + 1; start++) { // beginning
                    if (beamWidthValues[start][start + span] > 0) {
                        cellIndices[i++] = start;
                        cellIndices[i++] = (short) (start + span);
                    }
                }
            }

            if (beamClassCounts != null) {
                final StringBuilder classCounts = new StringBuilder(1024);
                for (i = 0; i < beamWidthModel.numClasses(); i++) {
                    classCounts.append(" class" + i + "=" + beamClassCounts[i]);
                }
                if (BaseLogger.singleton().isLoggable(Level.FINER)) {
                    BaseLogger.singleton().finer(
                            "INFO: beamconf: " + classCounts + " open=" + (openCells - numOnlyFactored)
                                    + " openFactored=" + numOnlyFactored + " closed=" + (totalCells - openCells));
                }
            }
            if (ParserDriver.chartConstraintsPrint) {
                BaseLogger.singleton().info("CC_SENT: " + parser.chart.parseTask.sentence);
                BaseLogger.singleton().info("CC_CELLS: " + toString());
            }
        }

        @Override
        public void reset(final boolean enableConstraints) {
            super.reset(enableConstraints);

            if (!enableConstraints) {
                // Replace cellIndices with all chart cells.
                final int sentenceLength = parser.chart.size();
                openCells = sentenceLength * (sentenceLength + 1) / 2;
                if (cellIndices == null || cellIndices.length < openCells * 2) {
                    cellIndices = new short[openCells * 2];
                }

                int i = 0;
                for (short span = 1; span <= sentenceLength; span++) {
                    for (short start = 0; start < sentenceLength - span + 1; start++) { // beginning
                        cellIndices[i++] = start;
                        cellIndices[i++] = (short) (start + span);
                    }
                }
            }
        }

        @Override
        public boolean isCellOpen(final short start, final short end) {
            return !constraintsEnabled || (beamWidthValues[start][end] > 0 && onlyFactored[start][end] == false);
        }

        @Override
        public boolean isCellOnlyFactored(final short start, final short end) {
            return !constraintsEnabled || onlyFactored[start][end];
        }

        @Override
        public boolean isUnaryOpen(final short start, final short end) {
            return true;
        }

        @Override
        public int getBeamWidth(final short start, final short end) {
            return constraintsEnabled ? beamWidthValues[start][end] : Integer.MAX_VALUE;
        }

        @Override
        public String toString() {
            final int n = beamWidthValues.length;
            final StringBuilder cellStats = new StringBuilder(4096);
            for (int start = 0; start < n; start++) {
                for (int end = n; end > start; end--) {
                    int x = beamWidthValues[start][end];
                    if (x > 5)
                        x = 5;
                    x = 5 - x;
                    if (onlyFactored[start][end]) {
                        cellStats.append(start + "," + end + "=FACT ");
                    } else if (x > 0) {
                        cellStats.append(start + "," + end + "=" + x + " ");
                    }
                }
            }
            return cellStats.toString();
        }

        @Override
        protected boolean isGrammarLeftFactored() {
            return parser.grammar.binarization() == Binarization.LEFT;
        }
    }
}
