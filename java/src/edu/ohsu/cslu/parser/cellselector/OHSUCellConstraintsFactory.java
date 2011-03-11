package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import cltool4j.BaseLogger;
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

public class OHSUCellConstraintsFactory implements CellSelectorFactory {

    private Vector<Vector<Float>> allBeginScores = new Vector<Vector<Float>>();
    private Vector<Vector<Float>> allEndScores = new Vector<Vector<Float>>();
    private Vector<Vector<Float>> allUnaryScores = new Vector<Vector<Float>>();
    boolean[] beginClosed, endClosed, unaryClosed;
    protected boolean grammarLeftFactored;
    private int currentSentNumber;

    private float globalBegin = Float.POSITIVE_INFINITY;
    private float globalEnd = Float.POSITIVE_INFINITY;
    private float globalUnary = Float.POSITIVE_INFINITY;
    private float precisionPct = Float.NaN, linearOpen = Float.NaN;

    protected LinkedList<ChartCell> cellList;
    protected Iterator<ChartCell> cellListIterator;

    public OHSUCellConstraintsFactory(final BufferedReader modelStream, final String constraintArgs,
            final boolean grammarLeftFactored) {
        this.grammarLeftFactored = grammarLeftFactored;
        try {
            parseConstraintArgs(constraintArgs);
            BaseLogger.singleton().fine(
                    "CC_PARAM: startThresh=" + globalBegin + " endThresh=" + globalEnd + " unaryThresh=" + globalUnary
                            + " precisionPct=" + precisionPct + " linearN=" + linearOpen);

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
                globalBegin = ParserUtil.str2float(tokens[++i]);
                globalEnd = ParserUtil.str2float(tokens[++i]);
                globalUnary = ParserUtil.str2float(tokens[++i]);
            } else if (tokens[i].equals("P")) {
                precisionPct = ParserUtil.str2float(tokens[++i]);
                if (precisionPct < 0.0 || precisionPct > 1.0) {
                    BaseLogger.singleton().severe(
                            "Precision constraint must be btwn 0 and 1 inclusive.  Found value: " + precisionPct);
                }
            } else if (tokens[i].equals("N")) {
                this.linearOpen = ParserUtil.str2float(tokens[++i]);
                if (linearOpen <= 0) {
                    BaseLogger.singleton().severe("Linear open cells constrain must be greater than zero");
                }
            } else {
                BaseLogger.singleton().severe("CC tune option not recognized.  Exiting.");
            }
            i += 1;
        }
    }

    public void readModel(final BufferedReader inStream) throws Exception {
        String line = null;
        while ((line = inStream.readLine()) != null) {
            line = line.trim();
            if (line.equals("") || allBeginScores.size() == 0) {
                // new sentence
                allBeginScores.add(new Vector<Float>());
                allEndScores.add(new Vector<Float>());
                allUnaryScores.add(new Vector<Float>());
            }

            if (!line.equals("")) {
                final String[] tokens = line.split("[ \t]+");
                if (tokens.length >= 3) {
                    // ignore the word at tokens[0]
                    allBeginScores.lastElement().add(Float.parseFloat(tokens[1]));
                    allEndScores.lastElement().add(Float.parseFloat(tokens[2]));
                    if (tokens.length == 4) {
                        allUnaryScores.lastElement().add(Float.parseFloat(tokens[3]));
                    }
                } else {
                    throw new Exception("ERROR: incorrect format for cellConstraintsFile on line: '" + line + "'");
                }
            }
        }
    }

    public CellSelector createCellSelector() {
        return new OHSUCellConstraints();
    }

    public class OHSUCellConstraints extends CellConstraints {

        @Override
        public void initSentence(final ChartParser<?, ?> parser) {
            // might have to hash the sentence number for the grid
            initSentence(parser.chart, parser.currentInput.sentenceNumber, parser.currentInput.sentence);
        }

        public void initSentence(final Chart chart, final int sentNumber, final String sentence) {
            final Vector<Float> beginScores = allBeginScores.get(sentNumber);
            final Vector<Float> endScores = allEndScores.get(sentNumber);
            final Vector<Float> unaryScores = allUnaryScores.get(sentNumber);

            float beginThresh = globalBegin;
            float endThresh = globalEnd;
            final float unaryThresh = globalUnary;

            if (!Float.isNaN(precisionPct)) {
                final float precisionThresh = getPrecisionConstraints(beginScores, endScores);
                beginThresh = Math.min(beginThresh, precisionThresh);
                endThresh = Math.min(endThresh, precisionThresh);
            }
            if (!Float.isNaN(linearOpen)) {
                final float quadThresh = getQuadraticConstraints(beginScores, endScores);
                beginThresh = Math.min(beginThresh, quadThresh);
                endThresh = Math.min(endThresh, quadThresh);
            }

            beginClosed = arrayCompare(beginScores, beginThresh);
            endClosed = arrayCompare(endScores, endThresh);
            unaryClosed = arrayCompare(unaryScores, unaryThresh);

            cellList = new LinkedList<ChartCell>();
            final int sentLen = beginClosed.length;
            for (short span = 1; span <= sentLen; span++) {
                for (short beg = 0; beg < sentLen - span + 1; beg++) { // beginning
                    final short end = (short) (beg + span);
                    if (isCellOpen(beg, end)) {
                        cellList.add(chart.getCell(beg, end));
                    }
                }
            }
            cellListIterator = cellList.iterator();
            currentSentNumber = sentNumber;

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

        private boolean[] arrayCompare(final List<Float> floatVals, final float thresh) {
            final int n = floatVals.size();
            final boolean[] boolVals = new boolean[n]; // defaults to false
            for (int i = 0; i < n; i++) {
                if (floatVals.get(i) > thresh) {
                    boolVals[i] = true;
                }
            }
            return boolVals;
        }

        private float getPrecisionConstraints(final Vector<Float> bScores, final Vector<Float> eScores) {
            final Vector<Float> positiveScores = new Vector<Float>();
            for (final float value : bScores) {
                if (value >= 0)
                    positiveScores.add(value);
            }
            for (final float value : eScores) {
                if (value >= 0)
                    positiveScores.add(value);
            }
            Collections.sort(positiveScores);

            final int index = (int) (positiveScores.size() * (1 - precisionPct));
            if (index == 0)
                return 0;
            if (index == positiveScores.size())
                return Float.POSITIVE_INFINITY;
            return positiveScores.get(index);
            // float precisionThresh = 0;
            // float numToKeep = positiveScores.size() * (1 - precisionPct);
            // if (positiveScores.size() > 0) {
            // int index = (int) (positiveScores.size() * (1 - precisionPct));
            // index = Math.min(index, positiveScores.size() - 1);
            // precisionThresh = positiveScores.get(index);
            // }
            // return precisionThresh;
        }

        private float getQuadraticConstraints(final Vector<Float> bScores, final Vector<Float> eScores) {
            final int sentLen = bScores.size();
            final LinkedList<TagScore> allScores = new LinkedList<TagScore>();
            for (int i = 0; i < sentLen; i++) {
                allScores.add(new TagScore(bScores.get(i), i, -1));
                allScores.add(new TagScore(eScores.get(sentLen - i - 1), -1, sentLen - i));
            }
            Collections.sort(allScores);

            final boolean beginOpen[] = new boolean[sentLen];
            final boolean endOpen[] = new boolean[sentLen + 1];
            final int targetNumOpen = (int) (linearOpen * sentLen);
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

        private class TagScore implements Comparable<TagScore> {
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

        @Override
        public boolean isCellOpen(final short start, final short end) {
            if (end - start == 1)
                return true;
            if (grammarLeftFactored) {
                if (endClosed[end - 1])
                    return false;
                return true;
            }
            if (beginClosed[start])
                return false;
            return true;
        }

        @Override
        public boolean isCellOnlyFactored(final short start, final short end) {
            if (isCellClosed(start, end))
                return false;
            if (grammarLeftFactored) {
                if (beginClosed[start])
                    return true;
                return false;
            }
            if (endClosed[end - 1])
                return true;
            return false;
        }

        @Override
        public boolean isUnaryOpen(final short start, final short end) {
            if (end - start > 1) {
                return true;
            }
            return !unaryClosed[start];
        }

        public float getBeginScore(final int start) {
            return allBeginScores.get(currentSentNumber).get(start);
        }

        public float getEndScore(final int end) {
            return allEndScores.get(currentSentNumber).get(end - 1);
        }

        public float getUnaryScore(final int start) {
            return allUnaryScores.get(currentSentNumber).get(start);
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(final boolean perCell) {
            int total = 0, nClosed = 0, nFact = 0, nUnaryClosed = 0;
            String s = "";
            final int n = beginClosed.length;
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