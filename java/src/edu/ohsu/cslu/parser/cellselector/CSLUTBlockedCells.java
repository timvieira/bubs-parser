package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.ParserUtil;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

public class CSLUTBlockedCells extends CellSelector {

    private LinkedList<ChartCell> cellList;
    public HashMap<String, Vector<Float>> allStartScores, allEndScores, allUnaryScores;
    public Vector<Float> curStartScore, curEndScore, curUnaryScore;
    private float cellTune, unaryTune;

    private boolean[][] openAll, openFactored;

    // private boolean isGrammarLeftFactored;

    public CSLUTBlockedCells(final BufferedReader modelStream) {
        this(modelStream, ParserDriver.param1, ParserDriver.param2);
    }

    public CSLUTBlockedCells(final BufferedReader modelStream, final float cellTune, final float unaryTune) {
        this.cellTune = cellTune;
        this.unaryTune = unaryTune;
        // this.isGrammarLeftFactored = isGrammarLeftFactored;

        if (cellTune == -1)
            this.cellTune = (float) 0.3;
        if (unaryTune == -1)
            this.unaryTune = Float.MIN_VALUE;

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
        init(parser.chart, parser.currentInput.sentence, parser.grammar.isLeftFactored());
    }

    public void init(final Chart chart, final String sentence, final boolean isGrammarLeftFactored) {
        int totalCells = 0, openCells = 0, factoredCells = 0;
        final int chartSize = chart.size();
        // this.isGrammarLeftFactored = isGrammarLeftFactored;

        curStartScore = allStartScores.get(sentence);
        curEndScore = allEndScores.get(sentence);
        curUnaryScore = allUnaryScores.get(sentence);

        openAll = new boolean[chartSize][chartSize + 1];
        openFactored = new boolean[chartSize][chartSize + 1];
        cellList = new LinkedList<ChartCell>();

        // final float startThresh = getThresh(curStartScore, cellTune);
        // final float endThresh = getThresh(curEndScore, cellTune);
        final float thresh = computeThresh(curStartScore, curEndScore);

        for (int span = 1; span <= chartSize; span++) {
            for (int beg = 0; beg < chartSize - span + 1; beg++) { // beginning
                final int end = beg + span;
                openAll[beg][end] = false;
                openFactored[beg][end] = false;
                totalCells++;

                if (isGrammarLeftFactored) {
                    if (curEndScore.get(end - 1) <= thresh || span == 1) {
                        openFactored[beg][end] = true;
                        cellList.add(chart.getCell(beg, end));
                        if (curStartScore.get(beg) <= thresh || span == 1) {
                            openAll[beg][end] = true;
                        }
                    }
                } else {
                    if (curStartScore.get(beg) <= thresh || span == 1) {
                        openFactored[beg][end] = true;
                        cellList.add(chart.getCell(beg, end));
                        if (curEndScore.get(end - 1) <= thresh || span == 1) {
                            openAll[beg][end] = true;
                        }
                    }
                }

                // System.out.println("" + isGrammarLeftFactored + " [" + beg + "," + end + "] open=" +
                // openAll[beg][end]
                // + " fact=" + openFactored[beg][end] + " bScore=" + curStartScore.get(beg) + " eScore="
                // + curEndScore.get(end - 1));

                if (openAll[beg][end]) {
                    openCells++;
                } else if (openFactored[beg][end]) {
                    factoredCells++;
                }
            }
        }
        ParserDriver.getLogger().fine(
                "INFO: CSLUT cell stats: total=" + totalCells + " open=" + openCells + " openFactored=" + factoredCells
                        + " closed=" + (totalCells - openCells - factoredCells));
    }

    /*
     * given two list of floats ranging from -inf to +inf, find the threshold such that only 'pctToPrune' of the
     * *positive* entries are greater than this threshold
     */
    private float computeThresh(final Vector<Float> startScores, final Vector<Float> endScores) {
        final Vector<Float> positiveScores = new Vector<Float>();
        for (final float value : startScores) {
            if (value >= 0)
                positiveScores.add(value);
        }
        for (final float value : endScores) {
            if (value >= 0)
                positiveScores.add(value);
        }
        Collections.sort(positiveScores);

        if (positiveScores.size() == 0 || cellTune <= 0.0) {
            return 0;
        } else if (cellTune >= 1.0) {
            return positiveScores.lastElement();
        }

        return positiveScores.get((int) (positiveScores.size() * cellTune));
    }

    public boolean isCellOpen(final int start, final int end) {
        return openAll[start][end];
    }

    public boolean factoredParentsOnly(final int start, final int end) {
        return openFactored[start][end] && !openAll[start][end];
    }

    public boolean isCellClosed(final int start, final int end) {
        return !openFactored[start][end];
    }

    public float getCurStartScore(final int start) {
        return curStartScore.get(start);
    }

    public float getCurEndScore(final int end) {
        return curEndScore.get(end - 1);
    }

    public boolean unaryOpen(final int start, final int end) {
        if (end - start > 1) {
            return true;
        }
        if (curUnaryScore.get(start) >= unaryTune) {
            return true;
        }
        return false;
    }

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

        allStartScores = new HashMap<String, Vector<Float>>();
        allEndScores = new HashMap<String, Vector<Float>>();
        allUnaryScores = new HashMap<String, Vector<Float>>();

        final Vector<Float> tmpStart = new Vector<Float>();
        final Vector<Float> tmpEnd = new Vector<Float>();
        final Vector<Float> tmpUnary = new Vector<Float>();
        final LinkedList<String> tmpTokens = new LinkedList<String>();

        // line format: word startScore endScore
        // blank lines indicate end of sentence
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

                wordIndex++;
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

                wordIndex = 0;
                sentIndex++;

            }
        }
        final String sentence = ParserUtil.join(tmpTokens, " ");
        allStartScores.put(sentence, (Vector<Float>) tmpStart.clone());
        allEndScores.put(sentence, (Vector<Float>) tmpEnd.clone());
        allUnaryScores.put(sentence, (Vector<Float>) tmpUnary.clone());
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
