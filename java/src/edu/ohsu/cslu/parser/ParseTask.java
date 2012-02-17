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
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.parser.Parser.InputFormat;
import edu.ohsu.cslu.parser.chart.Chart.RecoveryStrategy;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbResult;
import edu.ohsu.cslu.util.Strings;

// TODO: shouldn't this be an internal class of Parser?  Then it could have 
// access to general parser options, the grammar, and chart (although I think
// the chart should be handled in this class eventually)
public class ParseTask {

    /** Input sentence */
    public final String sentence;

    /** Input sentence mapped into the lexicon */
    public final int[] tokens;

    /** Gold tree */
    public NaryTree<String> inputTree = null;

    /** Gold tags */
    public final int[] inputTags;

    public int[] fomTags = null; // TODO: this should be moved to the FOM class
    public final Grammar grammar;

    //
    // Parse results
    //
    public BinaryTree<String> binaryParse = null;
    public float insideProbability = Float.NEGATIVE_INFINITY;
    private EvalbResult evalb = null;
    public String chartStats = ""; // move all of these stats into this class

    /** Recovery strategy in case of parse failure */
    public final RecoveryStrategy recoveryStrategy;
    /** Recovery parse (only populated in case of parse failure) */
    public NaryTree<String> recoveryParse = null;

    //
    // Statistics
    //
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

    /** Total time to parse the sentence. */
    public long parseTimeMs = 0;
    /** Chart initialization and lexical production time */
    public long chartInitMs = 0;
    /** Figure-of-merit initialization time */
    public long fomInitMs = 0;
    /** Cell-selector initialization time */
    public long ccInitMs = 0;
    /** Total inside-pass binary time (accumulated in nanoseconds, but reported in ms) */
    public long insideBinaryNs = 0;
    /** Total unary and pruning time (accumulated in nanoseconds, but reported in ms) */
    public long unaryAndPruningNs = 0;
    /** Total outside-pass time */
    public long outsidePassMs = 0;
    /** Time to extract the parse tree from the chart, including unfactoring, if necessary. */
    public long extractTimeMs = 0;

    long startTime;

    public ParseTask(final String input, final InputFormat inputFormat, final Grammar grammar,
            final RecoveryStrategy recoveryStrategy) {

        this.grammar = grammar;

        switch (inputFormat) {

        case Token:
            this.sentence = input.trim();
            this.inputTree = null;
            this.inputTags = null;
            break;

        case Text:
            this.sentence = Tokenizer.treebankTokenize(input.trim());
            this.inputTree = null;
            this.inputTags = null;
            break;

        case Tree: {
            this.inputTree = NaryTree.read(input.trim(), String.class);
            this.sentence = Strings.join(inputTree.leafLabels(), " ");

            // extract POS tags from tree
            this.inputTags = new int[sentence.length()];
            int i = 0;
            for (final NaryTree<String> leaf : inputTree.leafTraversal()) {
                inputTags[i] = getInputTagIndex(leaf.parent().label());
                i++;
            }
        }
            break;

        case Tagged: {
            // (DT The) (NN economy) (POS 's) (NN temperature) (MD will)
            final LinkedList<String> sentTokens = new LinkedList<String>();
            final String[] stringTokens = input.split("\\s+");
            this.inputTags = new int[stringTokens.length / 2];
            int i = 0;
            for (final String token : stringTokens) {
                if (i % 2 == 1) {
                    sentTokens.add(token.substring(0, token.length() - 1)); // remove ")"
                } else {
                    inputTags[i / 2] = grammar.nonTermSet.getIndex(token.substring(1)); // remove "("
                }
                i++;
            }
            this.sentence = Strings.join(sentTokens, " ");
            this.inputTree = null;
            break;
        }

        default:
            throw new UnsupportedOperationException("Unsupported inputFormat: " + inputFormat);
        }

        this.tokens = grammar.tokenizer.tokenizeToIndex(sentence);
        this.recoveryStrategy = recoveryStrategy;
    }

    public ParseTask(final String input, final InputFormat inputFormat, final Grammar grammar) {
        this(input, inputFormat, grammar, null);
    }

    public ParseTask(final int[] tokens, final Grammar grammar) {
        this.tokens = tokens;
        this.grammar = grammar;
        this.sentence = null;
        this.inputTags = null;
        this.fomTags = null;
        this.recoveryStrategy = null;
    }

    protected int getInputTagIndex(final String posStr) {
        final int index = grammar.nonTermSet.getIndex(posStr);
        if (index == -1 && ParserDriver.parseFromInputTags) {
            throw new IllegalArgumentException("-parseFromInputTags specified but input tag '" + posStr
                    + "' not found in grammar");
        }
        return index;
    }

    public String statsString() {
        final StringBuilder result = new StringBuilder(128);
        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            result.append(String.format("\nINFO: sentLen=%d time=%d inside=%.5f %s", sentenceLength(), parseTimeMs,
                    insideProbability, chartStats));
            if (evalb != null) {
                result.append(String.format(
                        " f1=%.2f prec=%.2f recall=%.2f matched=%d goldBrackets=%d parseBrackets=%d", evalb.f1() * 100,
                        evalb.precision() * 100, evalb.recall() * 100, evalb.matchedBrackets, evalb.goldBrackets,
                        evalb.parseBrackets));
            }
        }

        if (BaseLogger.singleton().isLoggable(Level.FINER)) {
            result.append(String
                    .format(" pops=%d pushes=%d considered=%d nLex=%d nLexUnary=%d nUnary=%d nBinary=%d chartInit=%d fomInit=%d cellSelectorInit=%d insideBinary=%d unaryAndPruning=%d outsidePass=%d extract=%d",
                            totalPops, totalPushes, totalConsidered, nLex, nLexUnaryConsidered, nUnaryConsidered,
                            nBinaryConsidered, chartInitMs, fomInitMs, ccInitMs, insideBinaryNs / 1000000,
                            unaryAndPruningNs / 1000000, outsidePassMs, extractTimeMs));
        }

        return result.toString();
    }

    public String parseBracketString(final boolean binaryTree, final boolean printUnkLabels,
            final HeadPercolationRuleset headRules) {
        if (binaryParse == null) {
            if (recoveryStrategy != null) {
                return naryParse().toString();
            }
            return "()";
        }
        if (printUnkLabels == false) {
            binaryParse.replaceLeafLabels(sentence.split("\\s+"));
        }
        if (binaryTree) {
            return binaryParse.toString();
        }

        if (headRules != null) {
            // Output head rules
            return naryParseWithHeadLabels(headRules).toString();
        }

        // Otherwise, just return the nary parse tree
        return naryParse().toString();
    }

    public String parseBracketString(final boolean binaryTree) {
        return parseBracketString(binaryTree, false, null);
    }

    public NaryTree<String> naryParseWithHeadLabels(final HeadPercolationRuleset headRules) {
        final NaryTree<String> tree = naryParse();

        for (final NaryTree<String> node : tree.preOrderTraversal()) {
            // Skip leaf and preterminal nodes
            if (node.height() > 2) {
                node.setLabel(node.label() + ' ' + headRules.headChild(node.label(), node.childLabels()));
            }
        }
        return tree;
    }

    public NaryTree<String> naryParse() {
        if (binaryParse == null) {
            return recoveryParse;
        }
        return binaryParse.unfactor(grammar.grammarFormat);
    }

    public void startTime() {
        startTime = System.currentTimeMillis();
    }

    public void stopTime() {
        parseTimeMs = System.currentTimeMillis() - startTime;
    }

    public int sentenceLength() {
        return tokens.length;
    }

    public boolean parseFailed() {
        return binaryParse == null && recoveryParse == null;
    }

    public void evaluate(final BracketEvaluator evaluator) {
        if (inputTree != null && !parseFailed()) {
            evalb = evaluator.evaluate(inputTree, naryParse());
        }
    }
}