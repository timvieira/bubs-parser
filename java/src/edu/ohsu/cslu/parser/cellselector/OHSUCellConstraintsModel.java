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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import cltool4j.BaseLogger;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
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

public class OHSUCellConstraintsModel implements CellSelectorModel {

    // private Vector<Vector<Float>> allBeginScores = new Vector<Vector<Float>>();
    // private Vector<Vector<Float>> allEndScores = new Vector<Vector<Float>>();
    // private Vector<Vector<Float>> allUnaryScores = new Vector<Vector<Float>>();
    private Vector<float[]> allBeginScores = new Vector<float[]>();
    private Vector<float[]> allEndScores = new Vector<float[]>();
    private Vector<float[]> allUnaryScores = new Vector<float[]>();

    // Because we're getting the Cell Constraints scores pre-computed from Kristy for
    // certain sections, we can only parse sentences we have scores for. And when this
    // is done over the grid or threaded, then we need to keep track of which sentences is which
    private HashMap<String, Integer> sentToIndex = new HashMap<String, Integer>();

    // boolean[] beginClosed, endClosed, unaryClosed;
    protected boolean grammarLeftFactored;

    private float globalBegin = Float.POSITIVE_INFINITY;
    private float globalEnd = Float.POSITIVE_INFINITY;
    private float globalUnary = Float.POSITIVE_INFINITY;
    private float hpTune = Float.NaN, quadTune = Float.NaN, logTune = Float.NaN, linearTune = Float.NaN;

    protected LinkedList<ChartCell> cellList;
    protected Iterator<ChartCell> cellListIterator;

    public OHSUCellConstraintsModel(final BufferedReader modelStream, final boolean grammarLeftFactored) {
        this.grammarLeftFactored = grammarLeftFactored;
        try {
            final ConfigProperties props = GlobalConfigProperties.singleton();
            parseConstraintArgs(props.getProperty("chartConstraintsTune"));
            BaseLogger.singleton().fine(
                    "CC_PARAM: startThresh=" + globalBegin + " endThresh=" + globalEnd + " unaryThresh=" + globalUnary
                            + " hpTune=" + hpTune + " quadTune=" + quadTune + " logTune=" + logTune + " linearTune="
                            + linearTune);

            readModel(modelStream);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void parseConstraintArgs(final String args) {
        final String[] tokens = args.split(",");
        int i = 0;
        while (i < tokens.length) {
            if (tokens[i].equals("A")) {
                // Absolute: A,b,e,u
                globalBegin = Util.str2float(tokens[++i]);
                globalEnd = Util.str2float(tokens[++i]);
                globalUnary = Util.str2float(tokens[++i]);
            } else if (tokens[i].equals("P")) {
                // High Precision: P,x [x in 0...1]
                hpTune = Util.str2float(tokens[++i]);
                if (hpTune < 0.0 || hpTune > 1.0) {
                    BaseLogger.singleton().severe(
                            "High Precision constraints param must be btwn 0 and 1 inclusive.  Found value: " + hpTune);
                }
            } else if (tokens[i].equals("Q")) {
                // Quadratic: Q,x [x is integer; n*x open begin/end positions]
                quadTune = Integer.parseInt(tokens[++i]);
                if (quadTune <= 0) {
                    BaseLogger.singleton().severe("O(N^2) constraints param must be an integer greater than zero");
                }
            } else if (tokens[i].equals("O")) {
                logTune = Integer.parseInt(tokens[++i]);
                if (quadTune <= 0) {
                    BaseLogger.singleton().severe("O(NlogN) constraints param must be an integer greater than zero");
                }
            } else if (tokens[i].equals("L")) {
                // Linear: L,x [x is integer; x open begin/end positions]
                linearTune = Integer.parseInt(tokens[++i]);
                if (linearTune <= 0) {
                    BaseLogger.singleton().severe("O(N) constraints param must be an integer greater than zero");
                }
            } else {
                BaseLogger.singleton().severe("CC tune option not recognized.  Exiting.");
            }
            i += 1;
        }
    }

    public float[] floatListToArray(final List<Float> items) {
        final float[] x = new float[items.size()];
        int i = 0;
        for (final Float val : items) {
            x[i++] = val;
        }
        return x;
    }

    public void readModel(final BufferedReader inStream) throws Exception {
        final List<Float> beginScores = new LinkedList<Float>();
        final List<Float> endScores = new LinkedList<Float>();
        final List<Float> unaryScores = new LinkedList<Float>();
        final List<String> sentenceTokens = new LinkedList<String>();
        boolean firstLine = true;
        String line = null;
        while ((line = inStream.readLine()) != null) {
            line = line.trim();
            if (line.equals("") || firstLine) {
                // new sentence
                if (!firstLine) {
                    sentToIndex.put(Util.join(sentenceTokens, " "), allBeginScores.size());
                    allBeginScores.add(floatListToArray(beginScores));
                    allEndScores.add(floatListToArray(endScores));
                    allUnaryScores.add(floatListToArray(unaryScores));
                }
                firstLine = false;
                sentenceTokens.clear();
                beginScores.clear();
                endScores.clear();
                unaryScores.clear();
            }

            if (!line.equals("")) {
                final String[] tokens = line.split("[ \t]+");
                if (tokens.length >= 3) {
                    sentenceTokens.add(tokens[0]);
                    beginScores.add(Float.parseFloat(tokens[1]));
                    endScores.add(Float.parseFloat(tokens[2]));
                    if (tokens.length == 4) {
                        unaryScores.add(Float.parseFloat(tokens[3]));
                    }
                } else {
                    throw new Exception("ERROR: incorrect format for cellConstraintsFile on line: '" + line + "'");
                }
            }
        }
        if (sentenceTokens.size() > 0) {
            sentToIndex.put(Util.join(sentenceTokens, " "), allBeginScores.size());
            allBeginScores.add(floatListToArray(beginScores));
            allEndScores.add(floatListToArray(endScores));
            allUnaryScores.add(floatListToArray(unaryScores));
        }
    }

    // A threshold of 0 is the Bayes decision boundary. Move X% of closed
    // begin/end positions to open to increase precision (X=0 is original Bayes
    // decision boundary; X=1 results in no constraints).
    public static float computeHighPrecisionThresh(final float[] bScores, final float[] eScores, final float hpTune) {
        final List<Float> positiveScores = new LinkedList<Float>();
        for (int i = 0; i < bScores.length; i++) {
            if (bScores[i] >= 0)
                positiveScores.add(bScores[i]);
            if (eScores[i] >= 0)
                positiveScores.add(eScores[i]);
        }
        Collections.sort(positiveScores);

        final int index = (int) (positiveScores.size() * (1 - hpTune));
        if (index == 0)
            return 0;
        if (index == positiveScores.size())
            return Float.POSITIVE_INFINITY;
        return positiveScores.get(index);
    }

    // Open the minimal number of begin/end constraints such that K*N chart cells
    // are open to all constituents (leading to O(N^2) complexity.
    public static float computeQuadraticThresh(final float[] bScores, final float[] eScores, final float quadTune) {
        final int sentLen = bScores.length;
        final LinkedList<TagScore> allScores = new LinkedList<TagScore>();
        for (int i = 0; i < sentLen; i++) {
            allScores.add(new TagScore(bScores[i], i, -1));
            allScores.add(new TagScore(eScores[sentLen - i - 1], -1, sentLen - i));
        }
        Collections.sort(allScores);

        // We need to widen the threshold until a fixed number of cells in the chart
        // are open. This requires computing the effects of opening each word position
        // to the new begin or end constraint.
        final boolean beginOpen[] = new boolean[sentLen];
        final boolean endOpen[] = new boolean[sentLen + 1];
        final int targetNumOpen = (int) (quadTune * sentLen);
        int numOpen = 0;
        TagScore s = null;
        while (numOpen < targetNumOpen && allScores.size() > 0) {
            s = allScores.poll();
            if (s.end == -1) {
                beginOpen[s.start] = true;
                for (int end = s.start + 2; end <= sentLen; end++) {
                    if (endOpen[end]) {
                        numOpen++;
                    }
                }
            } else {
                endOpen[s.end] = true;
                for (int start = 0; start < s.end - 1; start++) {
                    if (beginOpen[start]) {
                        numOpen++;
                    }
                }
            }
        }

        if (s == null) {
            return Float.POSITIVE_INFINITY;
        }
        return s.score;
    }

    // Given ranked begin OR end classification scores, open the K highest-scoring
    // word positions, where K is a tuning param. This results in O(N) parsing complexity
    // if K is constant or O(Nlog^2 N) if K is O(log N).
    public static float computeFixedOpenConstraints(final float[] scores, final int fixedOpenTune) {
        if (fixedOpenTune >= scores.length)
            return Float.POSITIVE_INFINITY;
        final float[] tmp = scores.clone();
        Arrays.sort(tmp);
        return tmp[fixedOpenTune];
    }

    private static class TagScore implements Comparable<TagScore> {

        public float score;
        public int start, end;

        public TagScore(final float score, final int start, final int end) {
            this.score = score;
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(final TagScore o) {
            if (o.score < this.score)
                return 1;
            return -1;
        }
    }

    public CellSelector createCellSelector() {
        return new OHSUCellConstraints();
    }

    public class OHSUCellConstraints extends CellConstraints {

        float[] beginScores, endScores, unaryScores;
        float beginThresh, endThresh, unaryThresh;

        @Override
        public void initSentence(final ChartParser<?, ?> p) {
            // might have to hash the sentence number for the grid
            initSentence(p.chart, p.chart.parseTask.sentence);
        }

        public void initSentence(final Chart chart, final String sentence) {
            final Integer sentNumber = sentToIndex.get(sentence);
            if (sentNumber == null) {
                throw new IllegalArgumentException("ERROR: Sentence not found in Cell Constraints model:\n  "
                        + sentence);
            }
            beginScores = allBeginScores.get(sentNumber);
            endScores = allEndScores.get(sentNumber);
            unaryScores = allUnaryScores.get(sentNumber);
            final int sentLen = beginScores.length;

            beginThresh = globalBegin;
            endThresh = globalEnd;
            unaryThresh = globalUnary;

            if (!Float.isNaN(hpTune)) {
                final float precisionThresh = computeHighPrecisionThresh(beginScores, endScores, hpTune);
                beginThresh = Math.min(beginThresh, precisionThresh);
                endThresh = Math.min(endThresh, precisionThresh);
            }
            if (!Float.isNaN(quadTune)) {
                final float quadThresh = computeQuadraticThresh(beginScores, endScores, quadTune);
                beginThresh = Math.min(beginThresh, quadThresh);
                endThresh = Math.min(endThresh, quadThresh);
            }
            if (!Float.isNaN(logTune)) {
                final int fixedOpen = (int) (logTune * Math.log(sentLen) / Math.log(2));
                if (grammarLeftFactored) {
                    final float logThresh = computeFixedOpenConstraints(endScores, fixedOpen);
                    endThresh = Math.min(endThresh, logThresh);
                } else {
                    final float logThresh = computeFixedOpenConstraints(beginScores, fixedOpen);
                    beginThresh = Math.min(beginThresh, logThresh);
                }
            }
            if (!Float.isNaN(linearTune)) {
                if (grammarLeftFactored) {
                    final float logThresh = computeFixedOpenConstraints(endScores, (int) linearTune);
                    endThresh = Math.min(endThresh, logThresh);
                } else {
                    final float logThresh = computeFixedOpenConstraints(beginScores, (int) linearTune);
                    beginThresh = Math.min(beginThresh, logThresh);

                }
            }

            cellList = new LinkedList<ChartCell>();
            for (short span = 1; span <= sentLen; span++) {
                for (short beg = 0; beg < sentLen - span + 1; beg++) { // beginning
                    final short end = (short) (beg + span);
                    if (isCellOpen(beg, end)) {
                        cellList.add(chart.getCell(beg, end));
                    }
                }
            }
            cellListIterator = cellList.iterator();

            // int bOpen = 0, eOpen = 0;
            // for (int i = 0; i < sentLen; i++) {
            // if (beginScores[i] < beginThresh)
            // bOpen++;
            // if (endScores[i] < endThresh)
            // eOpen++;
            // }
            // System.out.println("beginOpen=" + bOpen + " endOpen=" + eOpen);

            final String ccStats = toString(true).trim();
            if (ParserDriver.chartConstraintsPrint) {
                BaseLogger.singleton().info("CC_SENT: " + sentence);
                BaseLogger.singleton().info(ccStats);
                // System.out.println("listSize=" + cellList.size());
                cellList.clear(); // do not parse sentence
            } else {
                BaseLogger.singleton().fine(ccStats.split("\n")[0]);
                BaseLogger.singleton().finer(ccStats.split("\n")[1]);
            }
        }

        @Override
        public boolean isCellOpen(final short start, final short end) {
            if (end - start == 1)
                return true;
            if (grammarLeftFactored) {
                if (endScores[end - 1] > endThresh)
                    return false;
                return true;
            }
            if (beginScores[start] > beginThresh)
                return false;
            return true;
        }

        @Override
        public boolean isCellOnlyFactored(final short start, final short end) {
            if (isCellClosed(start, end))
                return false;
            if (grammarLeftFactored) {
                if (beginScores[start] > beginThresh)
                    return true;
                return false;
            }
            if (endScores[end - 1] > endThresh)
                return true;
            return false;
        }

        @Override
        public boolean isUnaryOpen(final short start, final short end) {
            if (end - start > 1) {
                return true;
            }
            return unaryScores[start] <= unaryThresh;
        }

        public float getBeginScore(final int start) {
            return beginScores[start];
        }

        public float getEndScore(final int end) {
            return endScores[end - 1];
        }

        public float getUnaryScore(final int start) {
            return unaryScores[start];
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(final boolean perCell) {
            int total = 0, nClosed = 0, nFact = 0, nUnaryClosed = 0;
            String s = "";
            final int n = beginScores.length;
            for (int span = 1; span <= n; span++) {
                for (short beg = 0; beg < n - span + 1; beg++) {
                    final short end = (short) (beg + span);
                    total += 1;
                    if (span == 1) {
                        if (isUnaryClosed(beg, end)) {
                            s += beg + "," + end + "=3 ";
                            nUnaryClosed += 1;
                        }
                    } else {
                        if (isCellClosed(beg, end)) {
                            s += beg + "," + end + "=4 ";
                            nClosed += 1;
                        } else if (isCellOnlyFactored(beg, end)) {
                            s += beg + "," + end + "=2 ";
                            nFact += 1;
                        }
                    }
                }
            }

            final int nOpen = total - nClosed - nFact - nUnaryClosed;
            String stats = "CC_STATS: total=" + total + " nOpen=" + nOpen + " nFact=" + nFact + " nClosed=" + nClosed
                    + " nUnaryClosed=" + nUnaryClosed;
            if (perCell) {
                stats += "\nCC_CELLS: " + s;
            }
            return stats;
        }

        @Override
        public short[] next() {
            final ChartCell cell = cellListIterator.next();
            return new short[] { cell.start(), cell.end() };
        }

        @Override
        public boolean hasNext() {
            return cellListIterator.hasNext();
        }

        @Override
        public void reset() {
            cellListIterator = cellList.iterator();
        }

        @Override
        protected boolean isGrammarLeftFactored() {
            return grammarLeftFactored;
        }
    }
}
