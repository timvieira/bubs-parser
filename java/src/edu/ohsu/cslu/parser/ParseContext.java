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
package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.parser.Parser.InputFormat;
import edu.ohsu.cslu.parser.chart.GoldChart;
import edu.ohsu.cslu.util.Evalb.EvalbResult;
import edu.ohsu.cslu.util.Strings;

public class ParseContext {

    public String sentence;
    public String[] strTokens;
    public int[] tokens;

    public String sentenceMD5;
    // public BinaryTree<String> inputTree = null;
    public NaryTree<String> inputTree = null;
    public GoldChart inputTreeChart = null;
    public int sentenceNumber = -1;
    public int sentenceLength = -1;
    public BinaryTree<String> binaryParse = null;
    public NaryTree<String> naryParse = null;
    public String parseBracketString = "()";
    public float insideProbability = Float.NEGATIVE_INFINITY;
    public String parserStats = null;
    public EvalbResult evalb = null;

    public long totalPops = 0;
    public long totalPushes = 0;
    public long totalConsidered = 0;

    public int nLex = 0; // num considered == num in chart
    public int nLexUnary = 0;
    public long nLexUnaryConsidered = 0;
    public long nUnaryConsidered = 0;
    public long nBinaryConsidered = 0;

    public float insideScore = 0;
    public long maxMemoryMB = 0;

    /** Total time to parse the sentence (note that this time is in seconds rather than milliseconds). */
    public float parseTimeSec = 0;
    /** Chart initialization and lexical production time */
    public long chartInitMs = 0;
    /** Figure-of-merit initialization time */
    public long fomInitMs = 0;
    /** Cell-selector initialization time */
    public long ccInitMs = 0;
    /** Total unary and pruning time */
    public long unaryAndPruningMs = 0;
    /** Total outside-pass */
    public long outsidePassMs = 0;
    /** Time to extract the parse tree from the chart, including unfactoring, if necessary. */
    public long extractTimeMs = 0;

    long startTime;

    public ParseContext(final String input, final InputFormat inputFormat, final Grammar grammar) {
        try {
            // TODO We don't really need to trim both here and in Parser.parseSentence()
            if (inputFormat == InputFormat.Token) {
                this.sentence = input.trim();
            } else if (inputFormat == InputFormat.Text) {
                this.sentence = Tokenizer.treebankTokenize(input.trim());
            } else if (inputFormat == InputFormat.Tree) {
                this.inputTree = NaryTree.read(input.trim(), String.class);
                this.sentence = Strings.join(inputTree.leafLabels(), " ");
            }
            this.strTokens = sentence.split("\\s+");
            this.sentenceLength = strTokens.length;

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    // // public ParseContext(final BinaryTree<String> tree, final Grammar grammar) {
    // public ParseContext(final NaryTree<String> tree, final Grammar grammar) {
    //
    // try {
    // // inputTreeChart = new GoldChart(inputTree, grammar);
    //
    // this.strTokens = tree.leafLabels();
    // this.sentence = Strings.join(strTokens, " ");
    // this.sentenceLength = strTokens.length;
    // this.inputTree = tree;
    //
    // } catch (final Exception e) {
    // e.printStackTrace();
    // }
    // }

    @Override
    public String toString() {
        final String result = String
                .format("INFO: sentNum=%d  sentLen=%d seconds=%.3f inside=%.5f pops=%d pushes=%d considered=%d chartInit=%d, fomInit=%d ccInit=%d unaryAndPruning=%d outsidePass=%d extract=%d nLex=%d nLexUnary=%d nUnary=%d nBinary=%d",
                        sentenceNumber, sentenceLength, parseTimeSec, insideProbability, totalPops, totalPushes,
                        totalConsidered, chartInitMs, fomInitMs, ccInitMs, unaryAndPruningMs, outsidePassMs,
                        extractTimeMs, nLex, nLexUnaryConsidered, nUnaryConsidered, nBinaryConsidered);

        if (evalb != null) {
            return result
                    + String.format(" f1=%.2f prec=%.2f recall=%.2f matched=%d gold=%d parse=%d", evalb.f1() * 100,
                            evalb.precision() * 100, evalb.recall() * 100, evalb.matchedBrackets, evalb.goldBrackets,
                            evalb.parseBrackets);
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
