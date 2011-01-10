package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.ParserUtil;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

/*

 Example scores for the first sentence of WSJ section 24.  A low (negative)
 score is good (meaning it is likely the start of a constituent) while 
 a high score is bad.  

 beg        end    [unary]
 The        -535.54     280.54   293.54
 economy     157.15     150.01   432.67
 's          132.81    -125.78   217.10
 temperature 181.63    -100.70   120.67
 will       -127.94     181.23   142.86
 be         -152.23     109.31   143.28
 taken      -127.17     108.56   104.78
 from       -177.70     203.97   117.71
 several    -191.64     125.59   127.31
 vantage     126.39      70.12   265.25
 points       75.31     -70.11   150.64
 this       -190.60     184.84   150.59
 week        191.55    -171.90   193.59
 ,           157.11      82.25      Inf
 with       -158.21     155.34   154.05
 readings    -88.12      72.08  -147.34
 on         -191.74     193.42   162.00
 trade       -73.60      44.42   105.65
 ,           186.18     163.89      Inf
 output      103.79     139.51    27.98
 ,           181.12     130.75      Inf
 housing      59.41     213.51    32.62
 and         203.68     180.29   196.51
 inflation   291.98    -180.37    18.51
 .              Inf    -242.37      Inf

 */

public class CSLUTBlockedCells extends CellSelector {

    private LinkedList<ChartCell> cellList;
    private Iterator<ChartCell> cellListIterator;

    public HashMap<String, Vector<Float>> allStartScores, allEndScores, allUnaryScores;
    public Vector<Float> curStartScore, curEndScore, curUnaryScore;
    // private float cellTune, unaryTune;
    private float startThresh = Float.MAX_VALUE;
    private float endThresh = Float.MAX_VALUE;
    private float unaryThresh = Float.MAX_VALUE;
    // public int factoredCellBeamWidth = Integer.MAX_VALUE;

    private boolean[][] openAll, openFactored;

    // private boolean isGrammarLeftFactored;

    // public CSLUTBlockedCells(final BufferedReader modelStream) {
    // this(modelStream, ParserDriver.chartConstraintsThresh);
    // this(modelStream, ParserDriver.param1, ParserDriver.param2, ParserDriver.param3);
    // }

    public CSLUTBlockedCells(final BufferedReader modelStream, final String threshString) {
        if (threshString != null) {
            final String[] tokens = threshString.split(",");
            if (tokens.length > 0) {
                this.startThresh = Integer.parseInt(tokens[0]);
            }
            if (tokens.length > 1) {
                this.endThresh = Integer.parseInt(tokens[1]);
            }
            if (tokens.length > 2) {
                this.unaryThresh = Integer.parseInt(tokens[2]);
            }
            // if (tokens.length > 3) {
            // this.factoredCellBeamWidth = Integer.parseInt(tokens[3]);
            // }
        }

        try {
            readModel(modelStream);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initSentence(final ChartParser<?, ?> parser) {
        init(parser.chart, parser.currentInput.sentence, parser.grammar.isLeftFactored());
    }

    public void init(final Chart chart, final String sentence, final boolean isGrammarLeftFactored) {
        int totalCells = 0, openCells = 0, factoredCells = 0;
        final int chartSize = chart.size();

        curStartScore = allStartScores.get(sentence);
        curEndScore = allEndScores.get(sentence);
        curUnaryScore = allUnaryScores.get(sentence);

        if (curStartScore == null || curEndScore == null) {
            throw new Error("ERROR: Sentence not found in Cell Constraints input: '" + sentence + "'");
        }

        openAll = new boolean[chartSize][chartSize + 1];
        openFactored = new boolean[chartSize][chartSize + 1];
        cellList = new LinkedList<ChartCell>();

        // final float startThresh = getThresh(curStartScore, cellTune);
        // final float endThresh = getThresh(curEndScore, cellTune);
        // TODO: re-do thresh ... see computeThresh()
        // final float thresh = computeThresh(curStartScore, curEndScore);

        String perCellStats = "";
        for (int span = 1; span <= chartSize; span++) {
            for (int beg = 0; beg < chartSize - span + 1; beg++) { // beginning
                final int end = beg + span;
                openAll[beg][end] = false;
                openFactored[beg][end] = false;
                totalCells++;

                if (isGrammarLeftFactored) {
                    if (curEndScore.get(end - 1) <= endThresh || span == 1) {
                        openFactored[beg][end] = true;
                        cellList.add(chart.getCell(beg, end));
                        if (curStartScore.get(beg) <= startThresh || span == 1) {
                            openAll[beg][end] = true;
                        }
                    }
                } else {
                    if (curStartScore.get(beg) <= startThresh || span == 1) {
                        openFactored[beg][end] = true;
                        cellList.add(chart.getCell(beg, end));
                        if (curEndScore.get(end - 1) <= endThresh || span == 1) {
                            openAll[beg][end] = true;
                        }
                    }
                }

                // System.out.println("" + isGrammarLeftFactored + " [" + beg + "," + end + "] open=" +
                // openAll[beg][end]
                // + " fact=" + openFactored[beg][end] + " bScore=" + curStartScore.get(beg) + " eScore="
                // + curEndScore.get(end - 1));

                // ignore if closed (default)
                if (openAll[beg][end]) {
                    perCellStats += String.format("%d,%d=%d ", beg, end, 4); // open
                } else if (openFactored[beg][end]) {
                    perCellStats += String.format("%d,%d=%d ", beg, end, 2); // open only factored
                }

                if (openAll[beg][end]) {
                    openCells++;
                } else if (openFactored[beg][end]) {
                    factoredCells++;
                }
            }
        }

        cellListIterator = cellList.iterator();

        ParserDriver.getLogger().finer("INFO: CellConstraints: " + perCellStats);
        ParserDriver.getLogger().info(
                "INFO: CellConstraints: total=" + totalCells + " open=" + openCells + " openFactored=" + factoredCells
                        + " closed=" + (totalCells - openCells - factoredCells));
    }

    /*
     * given two list of floats ranging from -inf to +inf, find the threshold such that only 'pctToPrune' of the
     * *positive* entries are greater than this threshold
     * 
     * TODO: I think we could get better results by supplying absolute Start/End thresh values since (1) cells should be
     * pruned globally relative to the classifier score, not locally ... I think Brian/Kristy did this so they could
     * guarantee the linear constraint per sentence. (2) Start and End are completely separate classifiers and their
     * scores probably shouldn't be compared.
     * 
     * To implement the above, we don't use this function at all and require the user to specify two thresh values:
     * startThresh and endThresh
     */
    // private float computeThresh(final Vector<Float> startScores, final Vector<Float> endScores) {
    // final Vector<Float> positiveScores = new Vector<Float>();
    // for (final float value : startScores) {
    // if (value >= 0)
    // positiveScores.add(value);
    // }
    // for (final float value : endScores) {
    // if (value >= 0)
    // positiveScores.add(value);
    // }
    // Collections.sort(positiveScores);
    //
    // if (positiveScores.size() == 0 || cellTune <= 0.0) {
    // return 0;
    // } else if (cellTune >= 1.0) {
    // return positiveScores.lastElement();
    // }
    //
    // return positiveScores.get((int) (positiveScores.size() * cellTune));
    // }

    @Override
    public boolean isOpenAll(final short start, final short end) {
        return openAll[start][end];
    }

    @Override
    public boolean isOpenOnlyFactored(final short start, final short end) {
        return openFactored[start][end] && !openAll[start][end];
    }

    @Override
    public boolean isOpenUnary(final short start, final short end) {
        if (end - start > 1) {
            return true;
        }
        if (curUnaryScore.get(start) < unaryThresh) {
            return true;
        }
        return false;
    }

    // @Override
    // public boolean factoredParentsOnly(final int start, final int end) {
    // return openFactored[start][end] && !openAll[start][end];
    // }

    // public boolean isCellClosed(final int start, final int end) {
    // return !openFactored[start][end];
    // }

    public float getCurStartScore(final int start) {
        return curStartScore.get(start);
    }

    public float getCurEndScore(final int end) {
        return curEndScore.get(end - 1);
    }

    @Override
    public short[] next() {
        // final ChartCell cell = cellList.poll();
        final ChartCell cell = cellListIterator.next();
        return new short[] { (short) cell.start(), (short) cell.end() };
    }

    @Override
    public boolean hasNext() {
        // return !cellList.isEmpty();
        return cellListIterator.hasNext();
    }

    @Override
    public void reset() {
        cellListIterator = cellList.iterator();
    }

    // TODO: this is not a good system. To access the correct entries when processing
    // over the grid, I need to hash by the sentence string. And I don't even do that well
    @Override
    @SuppressWarnings("unchecked")
    public void readModel(final BufferedReader inStream) throws NumberFormatException, IOException {

        ParserDriver.getLogger().fine(
                "CellConstraints: startThresh=" + this.startThresh + " endThresh=" + this.endThresh + " unaryThresh="
                        + this.unaryThresh);
        ParserDriver.getLogger().fine("CellConstraints: Reading model ...");
        // HashMap<String, Vector<Vector<Float>>> ccScores = new HashMap<String, Vector<Vector<Float>>>();

        allStartScores = new HashMap<String, Vector<Float>>();
        allEndScores = new HashMap<String, Vector<Float>>();
        allUnaryScores = new HashMap<String, Vector<Float>>();

        final LinkedList<String> tmpTokens = new LinkedList<String>();
        final Vector<Float> tmpStart = new Vector<Float>();
        final Vector<Float> tmpEnd = new Vector<Float>();
        final Vector<Float> tmpUnary = new Vector<Float>();

        // line format: word startScore endScore [unaryScore]
        // blank lines indicate end of sentence
        String line;
        while ((line = inStream.readLine()) != null) {
            final String[] tokens = ParserUtil.tokenize(line);
            if (tokens.length > 0) {
                tmpTokens.add(tokens[0]);
                tmpStart.add(Float.parseFloat(tokens[1]));
                tmpEnd.add(Float.parseFloat(tokens[2]));
                if (tokens.length > 3) {
                    tmpUnary.add(Float.parseFloat(tokens[3]));
                } else {
                    tmpUnary.add(Float.MAX_VALUE);
                }
            } else {
                // new sentence
                final String sentence = ParserUtil.join(tmpTokens, " ");
                allStartScores.put(sentence, (Vector<Float>) tmpStart.clone());
                allEndScores.put(sentence, (Vector<Float>) tmpEnd.clone());
                allUnaryScores.put(sentence, (Vector<Float>) tmpUnary.clone());
                tmpTokens.clear();
                tmpStart.clear();
                tmpEnd.clear();
                tmpUnary.clear();
            }
        }
        final String sentence = ParserUtil.join(tmpTokens, " ");
        allStartScores.put(sentence, (Vector<Float>) tmpStart.clone());
        allEndScores.put(sentence, (Vector<Float>) tmpEnd.clone());
        allUnaryScores.put(sentence, (Vector<Float>) tmpUnary.clone());

        ParserDriver.getLogger().fine("CellConstraints: done.");
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

    // private float getThresh(final Vector<Float> scores, final float pctToPrune) {
    //
    // final List<Float> tmpSet = new Vector<Float>();
    // for (final float value : scores) {
    // if (value >= 0) {
    // tmpSet.add(value);
    // }
    // }
    // Collections.sort(tmpSet);
    //
    // if (tmpSet.size() == 0) {
    // return 0;
    // }
    //
    // int threshIndex = (int) (tmpSet.size() * pctToPrune);
    //
    // if (threshIndex >= tmpSet.size()) {
    // threshIndex = tmpSet.size() - 1;
    // }
    // if (threshIndex < 0) {
    // threshIndex = 0;
    // }
    //
    // return tmpSet.get(threshIndex);
    // }
}
