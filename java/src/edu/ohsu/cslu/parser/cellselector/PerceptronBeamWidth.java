package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import cltool4j.GlobalLogger;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.ParserUtil;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.BinaryPerceptronSet;
import edu.ohsu.cslu.perceptron.Classifier;

public class PerceptronBeamWidth extends CellConstraints {

    protected Classifier beamWidthModel;
    private boolean inferFactoredCells = false, classifyBaseCells = false;
    private int beamWidthValues[][];
    private boolean onlyFactored[][];
    private LinkedList<ChartCell> cellList;
    private Iterator<ChartCell> cellListIterator;
    protected boolean grammarLeftFactored;

    public PerceptronBeamWidth(final BufferedReader modelStream, final String beamConfBias) {

        if (ParserDriver.param2 != -1) {
            inferFactoredCells = true;
        }
        // if (ParserDriver.param3 != -1) {
        // classifyBaseCells = true;
        // }
        if (inferFactoredCells == false && classifyBaseCells == true) {
            throw new IllegalArgumentException("ERROR: got that wrong -- no models -fact +base");
        }

        try {
            modelStream.mark(10000);
            String line = modelStream.readLine();
            while (line != null && !line.trim().contains("# === ")) {
                line = modelStream.readLine();
            }
            modelStream.reset();

            if (line.equals("# === BinaryPerceptronSet Model ===")) {
                beamWidthModel = new BinaryPerceptronSet(modelStream);
            } else {
                beamWidthModel = new AveragedPerceptron(modelStream);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

        // if (ParserDriver.multiBin) {
        // beamWidthModel = new AveragedPerceptron(modelStream);
        // } else {
        // beamWidthModel = new BinaryPerceptronSet(modelStream);
        // }

        if (beamConfBias != null) {
            beamWidthModel.setBias(beamConfBias);
        }

        GlobalLogger.singleton().finer(
                "INFO: beamconf: inferFactoredCells=" + ParserUtil.bool2int(inferFactoredCells) + " classifyBaseCells="
                        + ParserUtil.bool2int(classifyBaseCells));
    }

    @Override
    public void initSentence(final ChartParser<?, ?> parser) {
        grammarLeftFactored = parser.grammar.isLeftFactored();
        computeBeamWidthValues(parser);
        // init(parser.chart, parser.currentInput.sentence, parser.grammar.isLeftFactored());
    }

    private void computeBeamWidthValues(final ChartParser<?, ?> parser) {
        SparseBitVector feats;
        int guessBeamWidth, guessClass;
        final int n = parser.currentInput.sentenceLength;
        beamWidthValues = new int[n][n + 1];
        onlyFactored = new boolean[n][n + 1];
        // cellList = new LinkedList<ChartCell>();

        final int[] beamClassCounts = new int[beamWidthModel.numClasses()];
        // Arrays.fill(beamClassCounts, 0);

        // traverse in a top-down order so we can remember when we first see a non-empty cell
        // only works for right factored (berkeley) grammars right now.
        // for (int end = 1; end < n + 1; end++) {
        for (int start = 0; start < n; start++) {
            boolean foundOpenCell = false;
            // for (int start = 0; start < end; start++) {
            for (int end = n; end > start; end--) {
                if (end - start == 1 && classifyBaseCells == false) {
                    beamWidthValues[start][end] = Integer.MAX_VALUE;
                    // beamWidthValues[start][end] = maxBeamWidth;
                    // cellStats += String.format("%d,%d=%d ", start, end, maxBeamWidth);
                } else {
                    feats = parser.getCellFeatures(start, end, beamWidthModel.getFeatureTemplate());
                    guessClass = beamWidthModel.classify(feats);
                    beamClassCounts[guessClass]++;
                    // guessBeamWidth = (int) Math.min(beamWidthModel.class2value(guessClass), maxBeamWidth);
                    guessBeamWidth = (int) beamWidthModel.class2value(guessClass);

                    // need to allow factored productions for classifiers that don't predict these cells
                    if (inferFactoredCells == true && guessBeamWidth == 0 && foundOpenCell) {
                        // guessBeamWidth = maxFactoredBeamWidth;
                        guessBeamWidth = Integer.MAX_VALUE;
                        onlyFactored[start][end] = true;
                        // cellStats += String.format("%d,%d=2 ", start, end);
                    } else if (guessBeamWidth > 0) {
                        foundOpenCell = true;
                        // cellStats += String.format("%d,%d=%d ", start, end, guessBeamWidth > 0 ? 4 : 0);
                        // cellStats += String.format("%d,%d=%d ", start, end, guessBeamWidth);
                    }

                    beamWidthValues[start][end] = guessBeamWidth;
                }
            }
        }

        // init cell list here because we don't classifiy cells in bottom-up order above
        cellList = new LinkedList<ChartCell>();
        for (int span = 1; span <= n; span++) {
            for (int start = 0; start < n - span + 1; start++) { // beginning
                if (beamWidthValues[start][start + span] > 0) {
                    cellList.add(parser.chart.getCell(start, start + span));
                }
            }
        }

        String classCounts = "";
        for (int i = 0; i < beamWidthModel.numClasses(); i++) {
            classCounts += String.format(" class%d:%d", i, beamClassCounts[i]);
        }

        GlobalLogger.singleton().finer("INFO: beamconf: " + toString());
        GlobalLogger.singleton().info("INFO: beamconf: " + classCounts);
        cellListIterator = cellList.iterator();
    }

    @Override
    public boolean hasNext() {
        return cellListIterator.hasNext();
    }

    @Override
    public short[] next() {
        final ChartCell cell = cellListIterator.next();
        return new short[] { cell.start(), cell.end() };
    }

    @Override
    public void reset() {
        cellListIterator = cellList.iterator();
    }

    @Override
    public boolean isCellOpen(final short start, final short end) {
        return beamWidthValues[start][end] > 0 && onlyFactored[start][end] == false;
    }

    @Override
    public boolean isCellOnlyFactored(final short start, final short end) {
        return onlyFactored[start][end];
    }

    @Override
    public boolean isUnaryOpen(final short start, final short end) {
        return true;
    }

    @Override
    public int getCellValue(final short start, final short end) {
        return beamWidthValues[start][end];
    }

    @Override
    public String toString() {
        final int n = beamWidthValues.length;
        String cellStats = "";
        for (int start = 0; start < n; start++) {
            for (int end = n; end > start; end--) {
                final int x = beamWidthValues[start][end];
                if (x > 0) {
                    if (onlyFactored[start][end]) {
                        cellStats += String.format("%d,%d=FACT ", start, end);
                    } else {
                        cellStats += String.format("%d,%d=%d ", start, end, x);
                    }
                }
            }
        }
        return cellStats;
    }

    @Override
    protected boolean isGrammarLeftFactored() {
        return grammarLeftFactored;
    }
}
