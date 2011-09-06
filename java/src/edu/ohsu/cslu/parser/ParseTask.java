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

import java.util.LinkedList;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.parser.Parser.InputFormat;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbResult;
import edu.ohsu.cslu.util.Strings;

// TODO: shouldn't this be an internal class of Parser?  Then it could have 
// access to general parser options, the grammar, and chart (although I think
// the chart should be handled in this class eventually)
public class ParseTask {

    public String sentence;
    public int[] tokens;
    public int[] tags = null;

    public NaryTree<String> inputTree = null;
    public BinaryTree<String> binaryParse = null;
    public float insideProbability = Float.NEGATIVE_INFINITY;
    private EvalbResult evalb = null;
    public String chartStats = ""; // move all of these stats into this class

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
    public Grammar grammar;

    public ParseTask(final String input, final InputFormat inputFormat, final Grammar grammar) {
        try {
            if (inputFormat == InputFormat.Token) {
                this.sentence = input.trim();
            } else if (inputFormat == InputFormat.Text) {
                this.sentence = Tokenizer.treebankTokenize(input.trim());
            } else if (inputFormat == InputFormat.Tree) {
                this.inputTree = NaryTree.read(input.trim(), String.class);
                this.sentence = Strings.join(inputTree.leafLabels(), " ");

                if (ParserDriver.parseFromInputTags) {
                    // extract POS tags from tree
                    tags = new int[sentence.length()];
                    int i = 0;
                    for (final NaryTree<String> leaf : inputTree.leafTraversal()) {
                        tags[i] = grammar.nonTermSet.getIndex(leaf.parent().label());
                        if (tags[i] == -1) {
                            throw new IllegalArgumentException("-parseFromInputTags specified but input tag '"
                                    + leaf.parent().label() + "' not found in grammar");
                        }
                        i++;
                    }
                }
            } else if (inputFormat == InputFormat.Tagged) {
                // (DT The) (NN economy) (POS 's) (NN temperature) (MD will)
                final LinkedList<String> sentTokens = new LinkedList<String>();
                final String[] tokens = input.split("\\s+");
                tags = new int[tokens.length / 2];
                int i = 0;
                for (final String token : tokens) {
                    if (i % 2 == 1) {
                        sentTokens.add(token.substring(0, token.length() - 1)); // remove ")"
                    } else {
                        tags[i / 2] = grammar.nonTermSet.getIndex(token.substring(1)); // remove "("
                    }
                    i++;
                }
                sentence = Strings.join(sentTokens, " ");
                if (ParserDriver.parseFromInputTags == false) {
                    tags = null; // make sure we don't use these
                }
            }

            this.tokens = Grammar.tokenizer.tokenizeToIndex(sentence);
            this.grammar = grammar;

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public String statsString() {
        String result = "";
        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            result += String.format("\nINFO: sentLen=%d seconds=%.3f inside=%.5f %s ", sentenceLength(), parseTimeSec,
                    insideProbability, chartStats);
            if (evalb != null) {
                result += String.format("f1=%.2f prec=%.2f recall=%.2f matched=%d gold=%d parse=%d ", evalb.f1() * 100,
                        evalb.precision() * 100, evalb.recall() * 100, evalb.matchedBrackets, evalb.goldBrackets,
                        evalb.parseBrackets);
            }
        }

        if (BaseLogger.singleton().isLoggable(Level.FINER)) {
            // moving from ChartParser collectDetailedStatistics test
            result += String.format(" fomInitTime=%d cellSelectorInitTime=%d", fomInitMs, ccInitMs);
            result += String
                    .format("pops=%d pushes=%d considered=%d chartInit=%d, fomInit=%d ccInit=%d unaryAndPruning=%d outsidePass=%d extract=%d nLex=%d nLexUnary=%d nUnary=%d nBinary=%d",
                            totalPops, totalPushes, totalConsidered, chartInitMs, fomInitMs, ccInitMs,
                            unaryAndPruningMs, outsidePassMs, extractTimeMs, nLex, nLexUnaryConsidered,
                            nUnaryConsidered, nBinaryConsidered);
        }

        return result;
    }

    public String parseBracketString(final boolean binaryTree, final boolean printUnkLabels) {
        if (binaryParse == null) {
            return "()";
        }
        if (printUnkLabels == false) {
            binaryParse.replaceLeafLabels(sentence.split("\\s+"));
        }
        if (binaryTree) {
            return binaryParse.toString();
        }
        return naryParse().toString();
    }

    public NaryTree<String> naryParse() {
        if (binaryParse == null) {
            return null;
        }
        return binaryParse.unfactor(grammar.grammarFormat);
    }

    public void startTime() {
        startTime = System.currentTimeMillis();
    }

    public void stopTime() {
        parseTimeSec = (System.currentTimeMillis() - startTime) / 1000f;
    }

    public int sentenceLength() {
        return tokens.length;
    }

    public boolean parseFailed() {
        return binaryParse == null;
    }

    public void evaluate(final BracketEvaluator evaluator) {
        if (inputTree != null && !parseFailed()) {
            evalb = evaluator.evaluate(inputTree, naryParse());
        }

    }
}