package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;
import edu.ohsu.cslu.parser.util.StringToMD5;

public class ParseStats {

    public String sentence;
    public String sentenceMD5;
    public ParseTree inputTree = null;
    public CellChart inputTreeChart = null;
    public String[] tokens;
    public int sentenceNumber = -1;
    public int sentenceLength = -1;
    public ParseTree parse = null;
    public String parseBracketString;
    public float insideProbability = -1 * Float.MAX_VALUE;

    public float parseTimeSec = 0;
    public float insideScore = 0;
    public long maxMemoryMB = 0;

    long startTime = System.currentTimeMillis();

    public ParseStats(String sentence) {

        try {
            // if input is a tree, extract sentence from tree
            if (ParseTree.isBracketFormat(sentence)) {
                inputTree = ParseTree.readBracketFormat(sentence);
                inputTreeChart = new CellChart(inputTree, true, null);
                sentence = ParserUtil.join(inputTree.getLeafNodesContent(), " ");
            }

            this.sentence = sentence;
            this.sentenceMD5 = StringToMD5.computeMD5(sentence);
            this.tokens = ParserUtil.tokenize(sentence);
            this.sentenceLength = tokens.length;

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return String.format("STAT: sentNum=%d  sentLen=%d md5=%s seconds=%.3f inside=%.5f", sentenceNumber,
                sentenceLength, sentenceMD5, parseTimeSec, insideProbability);
    }

    public String toStringWithParse() {
        return this.parseBracketString + "\n" + toString();
    }

    public void startTime() {
        startTime = System.currentTimeMillis();
    }

    public void stopTime() {
        parseTimeSec = (System.currentTimeMillis() - startTime) / 1000f;
    }

}