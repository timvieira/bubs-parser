package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParserUtil;

public class CSLUTBlockedCells extends CellSelector {

    private LinkedList<ChartCell> cellList;
    public HashMap<String, Vector<Float>> allStartScore, allEndScore;
    public Vector<Float> curStartScore, curEndScore;

    private boolean[][] isOpen;
    private boolean[][] onlyFactored;

    public CSLUTBlockedCells(final BufferedReader modelStream) {
        try {
            readModel(modelStream);
        } catch (final NumberFormatException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(final ChartParser<?, ?> parser) {
        // throw new Exception("should use init(parser, sentLen, pctBlock) instead.");
        init(parser.chart, parser.currentInput.sentence);
    }

    private float getThresh(final Vector<Float> scores, final float pctToPrune) {

        final List<Float> tmpSet = new Vector<Float>();
        for (final float value : scores) {
            if (value >= 0) {
                tmpSet.add(value);
            }
        }
        Collections.sort(tmpSet);

        if (tmpSet.size() == 0) {
            return 0;
        }

        int threshIndex = (int) (tmpSet.size() * pctToPrune);

        if (threshIndex >= tmpSet.size()) {
            threshIndex = tmpSet.size() - 1;
        }
        if (threshIndex < 0) {
            threshIndex = 0;
        }

        return tmpSet.get(threshIndex);
    }

    public void init(final Chart chart, final String sentence) {
        curStartScore = allStartScore.get(sentence);
        curEndScore = allEndScore.get(sentence);
        float spanStartScore, spanEndScore;
        int totalCells = 0, openCells = 0, factoredCells = 0;
        final int chartSize = chart.size();

        isOpen = new boolean[chartSize][chartSize + 1];
        onlyFactored = new boolean[chartSize][chartSize + 1];
        cellList = new LinkedList<ChartCell>();

        final float startThresh = getThresh(curStartScore, ParserDriver.param1);
        final float endThresh = getThresh(curEndScore, ParserDriver.param2);

        for (int span = 1; span <= chartSize; span++) {
            for (int beg = 0; beg < chartSize - span + 1; beg++) { // beginning
                isOpen[beg][beg + span] = false;
                onlyFactored[beg][beg + span] = false;
                spanStartScore = curStartScore.get(beg);
                spanEndScore = curEndScore.get(beg + span - 1);
                totalCells++;

                // special case for span == 1 since the CSLUT model isn't made for these.
                if (spanStartScore <= startThresh || span == 1) {
                    cellList.add(chart.getCell(beg, beg + span));
                    isOpen[beg][beg + span] = true;
                    if (spanEndScore <= endThresh || span == 1) {
                        openCells++;
                    } else {
                        factoredCells++;
                        onlyFactored[beg][beg + span] = true;
                    }
                }

            }
        }
        Log.info(2, "INFO: CSLUT cell stats: total=" + totalCells + " open=" + openCells + " openFactored="
                + factoredCells + " closed=" + (totalCells - openCells - factoredCells));
    }

    public boolean isCellOpen(final int start, final int end) {
        return isOpen[start][end];
    }

    public boolean isCellOpenOnlyToFactored(final int start, final int end) {
        return onlyFactored[start][end];
    }

    public boolean isCellClosed(final int start, final int end) {
        return isOpen[start][end] == false;
    }

    // private class SortBucket implements Comparable<SortBucket> {
    // public float score;
    // public int start, end;
    //
    // public SortBucket(final float score, final int start, final int end) {
    // this.score = score;
    // this.start = start;
    // this.end = end;
    // }
    //
    // @Override
    // public int compareTo(final SortBucket o) {
    // return Float.compare(score, o.score);
    // }
    //
    // }

    @Override
    public short[] next() {
        final ChartCell cell = cellList.poll();
        return new short[] { (short) cell.start(), (short) cell.end() };
    }

    @Override
    public boolean hasNext() {
        return !cellList.isEmpty();
    }

    // TODO: this is not a good system. To access the correct entries when processing
    // over the grid, I need to hash by the sentence string. And I don't even do that well
    @Override
    @SuppressWarnings("unchecked")
    public void readModel(final BufferedReader inStream) throws NumberFormatException, IOException {
        String line;
        int sentIndex = 0, wordIndex = 0;

        allStartScore = new HashMap<String, Vector<Float>>();
        allEndScore = new HashMap<String, Vector<Float>>();

        final Vector<Float> tmpStart = new Vector<Float>();
        final Vector<Float> tmpEnd = new Vector<Float>();
        final LinkedList<String> tmpTokens = new LinkedList<String>();

        // line format: word startScore endScore
        // blank lines indicate end of sentence
        while ((line = inStream.readLine()) != null) {
            final String[] tokens = ParserUtil.tokenize(line);
            if (tokens.length > 0) {
                tmpTokens.add(tokens[0]);
                tmpStart.add(Float.parseFloat(tokens[1]));
                tmpEnd.add(Float.parseFloat(tokens[2]));

                wordIndex++;
            } else {
                // new sentence
                final String sentence = ParserUtil.join(tmpTokens, " ");
                allStartScore.put(sentence, (Vector<Float>) tmpStart.clone());
                allEndScore.put(sentence, (Vector<Float>) tmpEnd.clone());
                tmpTokens.clear();
                tmpStart.clear();
                tmpEnd.clear();

                wordIndex = 0;
                sentIndex++;

            }
        }
        final String sentence = ParserUtil.join(tmpTokens, " ");
        allStartScore.put(sentence, (Vector<Float>) tmpStart.clone());
        allEndScore.put(sentence, (Vector<Float>) tmpEnd.clone());
    }
}
