package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParserUtil;
import edu.ohsu.cslu.parser.beam.BSCPPerceptronCellTrainer;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class PerceptronCellSelector extends CellSelector {

    public static boolean DEBUG;

    private float weights[];
    private int numFeats = -1;
    private float learningRate;

    // private BaseGrammar grammar;
    private OHSUCellConstraints cellConstraints;
    // private Vector<Float> cslutStartScore, cslutEndScore;
    private ChartParser<?, ?> parser;
    private PriorityQueue<HashSetChartCell> spanAgenda;
    private boolean[][] hasBeenPopped;
    private boolean[][] isInAgenda;
    private boolean trainingMode;

    public int numTotal, numCorrect;

    // We should also try to predict the number of pops as well as the span
    // Based on all of our features, I'm guessing we could learn the deficiencies
    // of our FOM function and really speed things up
    // public PerceptronSpanSelection(final BaseGrammar grammar, final CSLUTBlockedCellsTraversal cellConstraints)
    // {
    // public PerceptronSpanSelection(final CSLUTBlockedCellsTraversal cellConstraints) {
    public PerceptronCellSelector(final BufferedReader modelStream, final OHSUCellConstraints cellConstraints) {
        this.cellConstraints = cellConstraints;
        if (modelStream != null) {
            trainingMode = false;
            try {
                readModel(modelStream);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            trainingMode = true;
        }
    }

    @Override
    public void initSentence(final ChartParser<?, ?> p) {
        // run at the beginning of each sentence
        this.parser = p;
        DEBUG = (p.opts.param1 == -1);

        cellConstraints.initSentence(parser.chart, parser.currentInput.sentenceNumber, parser.currentInput.sentence);
        final int chartSize = p.chart.size();

        // inits all to false
        hasBeenPopped = new boolean[chartSize][chartSize + 1];
        isInAgenda = new boolean[chartSize][chartSize + 1];

        spanAgenda = new PriorityQueue<HashSetChartCell>();
        // // all span=1 && span=2 cells are fare game from the beginning since we've
        // // put the POS tags in the chart already
        for (int spanLength = 1; spanLength <= 2; spanLength++) {
            for (int start = 0; start < chartSize - spanLength + 1; start++) {
                if (spanLength == 1) {
                    hasBeenPopped[start][start + spanLength] = true;
                } else {
                    addSpanToAgenda((HashSetChartCell) p.chart.getCell(start, start + spanLength));
                }

                // addSpanToAgenda(parser.chart[start][start + spanLength]);
                // // base cells (spanLength == 1) are a special case because we have already
                // // added the POS tags, but we can still also add unary productions. We need
                // // to flag these cells as hasBeenPopped so that we can build binary edges
                // // on top of them without visiting the cell to add unary edges
                // if (spanLength == 1) {
                // hasBeenPopped[start][start + spanLength] = true;
                // }
            }
        }

        numTotal = 0;
        numCorrect = 0;
    }

    @Override
    public boolean hasNext() {
        return !spanAgenda.isEmpty();
    }

    @Override
    public short[] next() {
        rescoreAgendaSpans();
        final HashSetChartCell bestSpan = pollBestSpan();

        if (DEBUG) {
            System.out.println(" pop: " + bestSpan);
        }

        // A parse can get stuck going between good spans that don't build a final tree, so
        // don't allow us to visit a span more than X times. ie. force the model to pick a
        // different span. Perhaps we should add ALL edges from this span before cutting it off?
        if (bestSpan.numSpanVisits > 3) {
            isInAgenda[bestSpan.start()][bestSpan.end()] = true;
            if (DEBUG) {
                System.out.println(" blocking: " + bestSpan);
            }
        } else {
            expandFrontier(bestSpan);
        }

        return new short[] { bestSpan.start(), bestSpan.end() };
    }

    private HashSetChartCell pollBestSpan() {
        final HashSetChartCell bestSpan = spanAgenda.poll();
        isInAgenda[bestSpan.start()][bestSpan.end()] = false;
        return bestSpan;
    }

    public void decreaseLearningRate() {
        learningRate = (float) (learningRate * 0.95);
    }

    // public ChartEdge nextWithGoldCorrection(final List<ChartEdge> goldEdgeList) {
    // ChartEdge goldEdge;
    // ChartCell goldSpan, edgeSpan;
    //
    // final ChartCell guessSpan = spanAgenda.poll();
    // System.out.println("guessSpan: " + guessSpan + "  fom=" + calcFOM(guessSpan));
    // numTotal++;
    //
    // ChartEdge saveGoldEdge = null;
    // final List<ChartCell> goldSpanList = new LinkedList<ChartCell>();
    // for (final ChartEdge edge : goldEdgeList) {
    // edgeSpan = parser.chart[edge.start()][edge.end()];
    // if (guessSpan == edgeSpan) {
    // System.out.print("  * ");
    // // System.out.println(" CORRECT! ");
    // // expandFrontier(edgeSpan);
    // // numCorrect++;
    // // return edge;
    // saveGoldEdge = edge;
    // } else {
    // System.out.print("    ");
    // }
    // System.out.println("gold=" + edge + " fom=" + calcFOM(edgeSpan));
    // goldSpanList.add(edgeSpan);
    // }
    //
    // if (saveGoldEdge != null) {
    // expandFrontier(guessSpan);
    // numCorrect++;
    // return saveGoldEdge;
    // }
    //
    // // guess span was wrong...
    //
    // // pick a random gold edge
    // Collections.shuffle(goldEdgeList);
    // goldEdge = goldEdgeList.get(0);
    // goldSpan = parser.chart[goldEdge.start()][goldEdge.end()];
    //
    // spanAgenda.remove(goldSpan);
    // spanAgenda.add(guessSpan);
    //
    // // updateWeights(guessSpan, goldSpan);
    // // updateWeights(guessSpan, false);
    // // updateWeights(goldSpan, true);
    //
    // // reward all gold spans
    // // updateWeights(guessSpan, false);
    // updateWeights(guessSpan, -1 * goldSpanList.size());
    // for (final ChartCell span : goldSpanList) {
    // updateWeights(span, 1);
    // }
    //
    // expandFrontier(goldSpan);
    // return goldEdge;
    // }

    private void rescoreAgendaSpans() {
        final PriorityQueue<HashSetChartCell> tmpAgenda = new PriorityQueue<HashSetChartCell>();
        for (final HashSetChartCell span : spanAgenda) {
            calcFOM(span);
            tmpAgenda.add(span);

        }
        spanAgenda = tmpAgenda;
    }

    public HashSetChartCell nextWithGoldCorrection(final List<ChartEdge> goldEdgeList) {
        HashSetChartCell goldSpan;

        // re-score all edges. I've found out that we will keep picking bad edges even
        // though the weights for them are good because the cells inside the agenda don't change score
        // except for the one that is popped
        rescoreAgendaSpans();

        if (DEBUG) {
            for (final HashSetChartCell span : spanAgenda) {
                System.out.println("guessSpanFont: " + span + "  fom=" + span.fom);
            }
        }

        // make sure frontier always has all gold spans. This is just for debugging.
        for (final ChartEdge goldEdge : goldEdgeList) {
            goldSpan = (HashSetChartCell) parser.chart.getCell(goldEdge.start(), goldEdge.end());
            boolean frontHasGold = false;
            for (final HashSetChartCell frontSpan : spanAgenda) {
                if (frontSpan == goldSpan) {
                    frontHasGold = true;
                }
            }
            if (frontHasGold == false) {
                System.out.println("ERROR: frontier does not have gold span: " + goldSpan);
                System.exit(1);
            }
        }

        final HashSetChartCell guessSpan = pollBestSpan();

        if (DEBUG)
            System.out.println("guessSpan: " + guessSpan + "  fom=" + guessSpan.fom);

        numTotal++;

        boolean isGuessSpanCorrect = false;
        final List<HashSetChartCell> goldSpanList = new LinkedList<HashSetChartCell>();
        for (final ChartEdge edge : goldEdgeList) {
            goldSpan = (HashSetChartCell) parser.chart.getCell(edge.start(), edge.end());
            if (guessSpan == goldSpan) {
                if (DEBUG)
                    System.out.print("  * ");
                numCorrect++;
                isGuessSpanCorrect = true;
            } else {
                if (DEBUG)
                    System.out.print("    ");
            }
            if (DEBUG)
                System.out.println("gold=" + edge + " fom=" + goldSpan.fom);
            goldSpanList.add(goldSpan);
        }

        if (isGuessSpanCorrect == false) {
            // reward all gold spans
            updateWeights(guessSpan, -1 * goldSpanList.size());
            for (final HashSetChartCell span : goldSpanList) {
                updateWeights(span, 1);
            }
        }

        expandFrontier(guessSpan);
        return guessSpan;
    }

    // public ChartCell nextWithGoldCorrection(final ChartCell goldSpan) {
    // final ChartCell guessSpan = spanAgenda.poll();
    //
    // // System.out.print("[" + goldSpan.start() + "][" + goldSpan.end() + "]-[" + guessSpan.start() + "][" +
    // guessSpan.end() + "] ");
    // System.out.println("[" + goldSpan.start() + "][" + goldSpan.end() + "]=" + calcFOM(goldSpan) + "\t[" +
    // guessSpan.start() + "][" + guessSpan.end() + "]="
    // + calcFOM(guessSpan));
    //
    // if (guessSpan != goldSpan) {
    // spanAgenda.remove(goldSpan);
    // spanAgenda.add(guessSpan);
    // // updateWeights(guessSpan, goldSpan);
    // updateWeights(guessSpan, -1);
    // updateWeights(goldSpan, 1);
    // } else {
    // System.out.println(" CORRECT! ");
    // }
    //
    // expandFrontier(goldSpan);
    // return guessSpan;
    // }

    public void expandFrontier(final HashSetChartCell span) {

        hasBeenPopped[span.start()][span.end()] = true;
        // add the current span back to the agenda so we can re-visit it again in the future if we need to
        addSpanToAgenda(span);
        // spanAgenda.remove(span);

        // expand frontier of reachable spans; score them, and add them to the agenda.
        for (int start = 0; start < span.start(); start++) {
            if (hasBeenPopped[start][span.start()]) {
                addSpanToAgenda((HashSetChartCell) parser.chart.getCell(start, span.end()));
            }
        }
        for (int end = parser.chart.size(); end > span.end(); end--) {
            if (hasBeenPopped[span.end()][end]) {
                addSpanToAgenda((HashSetChartCell) parser.chart.getCell(span.start(), end));
            }
        }
    }

    public float calcFOM(final HashSetChartCell span) {
        final Boolean[] spanFeats = extractFeatures(span, parser);
        // System.out.println(" spanFeats=" + spanFeats.length + " weights=" + weights.length);
        span.fom = dotProductKernel(spanFeats, weights);
        return span.fom;
    }

    private void addSpanToAgenda(final HashSetChartCell span) {
        if (isInAgenda[span.start()][span.end()] == false) {
            calcFOM(span);
            spanAgenda.add(span);
            isInAgenda[span.start()][span.end()] = true;

            if (DEBUG) {
                System.out.println("  adding: " + span + " fom=" + span.fom);
            }
        }
    }

    public void initWeights() {
        for (int i = 0; i < numFeats; i++) {
            weights[i] = 0;
        }
    }

    private static float dotProductKernel(final Boolean[] featVect, final float[] weightVect) {
        float total = 0;
        assert featVect.length == weightVect.length;
        for (int i = 0; i < featVect.length; i++) {
            // TODO: it would be nice if we could just multiply these two where featVect would be 0 or 1
            if (featVect[i] == true) {
                total += weightVect[i];
            }
        }
        return total;
    }

    public void updateWeights(final HashSetChartCell span, final int featWeight) {
        // final int correct = boolToInt(isCorrect) * 2 - 1; // correct == -1 (false) or 1 (true)
        final Boolean[] feats = extractFeatures(span, parser);
        for (int i = 0; i < numFeats; i++) {
            // weights[i] += boolToInt(feats[i]) * correct * learningRate;
            weights[i] += boolToInt(feats[i]) * featWeight * learningRate;
        }
    }

    public void updateWeights(final HashSetChartCell guessSpan, final HashSetChartCell goldSpan) {
        if (guessSpan != goldSpan) {
            final Boolean[] guessFeats = extractFeatures(guessSpan, parser);
            final Boolean[] goldFeats = extractFeatures(goldSpan, parser);

            // System.out.println("GUES: span[" + guessSpan.start() + "][" + guessSpan.end() + "] \t" +
            // boolListToString(guessFeats));
            // System.out.println("GOLD: span[" + goldSpan.start() + "][" + goldSpan.end() + "] \t" +
            // boolListToString(goldFeats));
            // System.out.println("OLD: " + floatListToString(weights));
            // final float[] oldWeights = weights.clone();

            for (int i = 0; i < numFeats; i++) {
                weights[i] += (boolToInt(goldFeats[i]) - boolToInt(guessFeats[i])) * learningRate;
                // if (guessFeats[i] != goldFeats[i]) {
                //
                // }
                // if (guessFeats[i] == true) {
                // weights[i] -= learningRate;
                // }
                // if (goldFeats[i] == true) {
                // weights[i] += learningRate;
                // }
            }
            // System.out.println("OLD: " + floatListToString(weights));
            // for (int i = 0; i < weights.length; i++) {
            // System.out.printf("\t%3.1f\t%3.1f\n", oldWeights[i], weights[i]);
            // }
        }

    }

    private int boolToInt(final boolean value) {
        if (value == true) {
            return 1;
        }
        return 0;
    }

    // TODO: we should only need access to Chart (not ChartParser)
    private Boolean[] extractFeatures(final HashSetChartCell span, final ChartParser<?, ?> p) {
        final List<Boolean> featList = new LinkedList<Boolean>();
        // BitSet feats = new BitSet();

        // bias
        // featList.add(true);

        // // is base cell
        // if (span.width() == 1) {
        // featList.add(true);
        // } else {
        // featList.add(false);
        // }

        // top cell
        featList.add(span == p.chart.getRootCell());

        // span width
        featList.addAll(binValue(span.width(), 1, 25, 25));

        // pct span width
        final float pctSpanOfWidth = span.width() / (float) p.chart.size();
        featList.addAll(binValue(pctSpanOfWidth, 0, 1, 21));

        // number span pops
        featList.addAll(binValue(span.numSpanVisits, 0, 10, 11));

        // count possible midpoints exist which should predict how fruitful the cell is
        int numMidpts = 0;
        for (int i = span.start() + 1; i < span.end(); i++) {
            if (hasBeenPopped[span.start()][i] && hasBeenPopped[i][span.end()]) {
                numMidpts++;
            }
        }
        featList.addAll(binValue(numMidpts, 0, 25, 26));

        // pct midpoints
        final float pctMidptsOfSpan = numMidpts / (float) (span.width() - 1);
        featList.addAll(binValue(pctMidptsOfSpan, 0, 1, 21));

        // CSLUT scores
        final float startScore = cellConstraints.getBeginScore(span.start());
        featList.addAll(binValue(startScore, -500, 500, 101));

        final float endScore = cellConstraints.getEndScore(span.end() - 1); // -1 because it's s word index, not a span
        featList.addAll(binValue(endScore, -500, 500, 101));

        if (featList.size() != numFeats) {
            if (trainingMode == true) {
                BaseLogger.singleton()
                        .info(
                                "WARNING: len(featureList)=" + featList.size() + " but numFeats=" + numFeats
                                        + ".  Resizing...");
                numFeats = featList.size();
                weights = new float[numFeats];
                initWeights();
            } else {
                BaseLogger.singleton().info(
                        "ERROR: len(featureList)=" + featList.size()
                                + " but number features in model files is numFeats=" + numFeats);
                System.exit(1);
            }
        }

        // System.out.println(" numFeats=" + numFeats + " featList=" + boolListToString(featList));

        // convert to Boolean[] array
        return featList.toArray(new Boolean[numFeats]);
    }

    public List<Boolean> binValue(final float value, final int min, final int max, final int numBins) {
        // final boolean[] bins = new boolean[numBins];
        final List<Boolean> bins = new LinkedList<Boolean>();
        final float step = (float) (max - min) / (float) (numBins - 1);

        for (float thresh = min; thresh <= max; thresh += step) {
            if (value >= thresh) {
                bins.add(true);
            } else {
                bins.add(false);
            }
        }

        // System.out.println("binValue() value=" + value + " min=" + min + " max=" + max + " numBins=" +
        // numBins + " step=" + step + " bin=" + boolListToString(bins));

        return bins;
    }

    public void train(final BufferedReader inStream, final BSCPPerceptronCellTrainer parserToTrain) throws Exception {
        learningRate = (float) 1.0;
        initWeights();
        // parserToTrain.train(inStream);
        throw new RuntimeException("ERROR: PerceptronCellSelector.train() doesn't work anymore");
    }

    @Override
    public void readModel(final BufferedReader inStream) throws Exception {
        String line;
        while ((line = inStream.readLine()) != null) {
            // line format: weight1 weight2 weight3 ...
            final String[] tokens = ParserUtil.tokenize(line);
            if (tokens.length > 0 && !tokens[0].equals("#")) {
                final int numModelFeatures = tokens.length;
                numFeats = numModelFeatures;
                weights = new float[numFeats];

                for (int i = 0; i < tokens.length; i++) {
                    weights[i] = Float.parseFloat(tokens[i]);
                }
            }
        }
    }

    @Override
    public void writeModel(final BufferedWriter outStream) throws Exception {
        outStream.write(this.toString());
    }

    @Override
    public String toString() {
        String modelStr = "# numFeats=" + numFeats + "\n";
        for (int i = 0; i < numFeats; i++) {
            modelStr += weights[i] + " ";
        }
        modelStr += "\n";
        return modelStr;
    }

    // private String floatListToString(final float[] list) {
    // String str = "";
    // for (final float value : list) {
    // str += String.format("%2.2f,", value);
    // }
    // return str;
    // }
    //
    // private String boolListToString(final Boolean[] list) {
    // String str = "";
    // for (final boolean value : list) {
    // if (value == true)
    // str += "1";
    // else
    // str += "0";
    // }
    // return str;
    // }
    //
    // private String boolListToString(final List<Boolean> list) {
    // String str = "";
    // for (final boolean value : list) {
    // if (value == true)
    // str += "1";
    // else
    // str += "0";
    // }
    // return str;
    // }

}
