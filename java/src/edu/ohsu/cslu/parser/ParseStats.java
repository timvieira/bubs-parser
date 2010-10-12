package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.chart.GoldChart;
import edu.ohsu.cslu.tools.TreeTools;

public class ParseStats {

    public String sentence;
    public String[] strTokens;
    public int[] tokens;

    public String sentenceMD5;
    public ParseTree inputTree = null;
    public GoldChart inputTreeChart = null;
    public int sentenceNumber = -1;
    public int sentenceLength = -1;
    public ParseTree parse = null;
    public String parseBracketString;
    public float insideProbability = Float.NEGATIVE_INFINITY;

    public int totalPops = 0;
    public int totalPushes = 0;
    public int totalConsidered = 0;

    public int nLex = 0;
    public int nLexUnary = 0;
    public int nUnary = 0;
    public int nBinary = 0;

    public float parseTimeSec = 0;
    public float fomInitSec = 0;
    public float insideScore = 0;
    public long maxMemoryMB = 0;

    long startTime = System.currentTimeMillis();

    public ParseStats(String input, final Grammar grammar) {

        try {
            // if input is a tree, extract sentence from tree
            if (ParseTree.isBracketFormat(input)) {
                inputTree = ParseTree.readBracketFormat(input);
                TreeTools.binarizeTree(inputTree, grammar.isRightFactored(), grammar.horizontalMarkov(), grammar
                        .verticalMarkov(), grammar.annotatePOS(), grammar.grammarFormat);
                inputTreeChart = new GoldChart(inputTree, grammar);
                input = ParserUtil.join(inputTree.getLeafNodesContent(), " ");
            }

            this.sentence = input.trim();
            // this.sentenceMD5 = StringToMD5.computeMD5(sentence);
            this.strTokens = ParserUtil.tokenize(sentence);
            this.sentenceLength = strTokens.length;

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        // String result = String.format("STAT: sentNum=%d  sentLen=%d md5=%s seconds=%.3f inside=%.5f", sentenceNumber,
        // sentenceLength, sentenceMD5, parseTimeSec, insideProbability);
        String result = String.format("STAT: sentNum=%d  sentLen=%d seconds=%.3f inside=%.5f", sentenceNumber,
                sentenceLength, parseTimeSec, insideProbability);
        if (totalPops > 0)
            result += " pops=" + totalPops;
        if (totalPushes > 0)
            result += " pushes=" + totalPushes;
        if (totalConsidered > 0)
            result += " considered=" + totalConsidered;
        if (fomInitSec > 0)
            result += " fomInit=" + fomInitSec;
        if (nBinary > 0) {
            result += " nLex=" + nLex;
            result += " nLexUnary=" + nLexUnary;
            result += " nUnary=" + nUnary;
            result += " nBinary=" + nBinary;
        }

        return result;
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
