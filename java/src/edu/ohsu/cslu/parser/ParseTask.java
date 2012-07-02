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

import java.util.logging.Level;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
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
    public final String[] stringInputTags;

    public int[] fomTags = null; // TODO: this should be moved to the FOM class
    public final Grammar grammar;

    public final DecodeMethod decodeMethod;

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
    /** The total number of edges populated in the chart (including unary, binary, and lexical rules) */
    public long totalPopulatedEdges = 0;
    public long totalPushes = 0;
    public long totalConsidered = 0;

    public int nLex = 0; // num considered == num in chart
    public int nLexUnary = 0;
    public long nLexUnaryConsidered = 0;
    /** The number of unary grammar rules (edges) examined while parsing the sentence */
    public long nUnaryConsidered = 0;
    /** The number of binary grammar rules (edges) examined while parsing the sentence */
    public long nBinaryConsidered = 0;

    public float insideScore = 0;

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

    /** The number of reparsing stages required to find a valid parse */
    public short reparseStages = 0;

    long startTime;

    public ParseTask(final String input, final InputFormat inputFormat, final Grammar grammar,
            final RecoveryStrategy recoveryStrategy, final DecodeMethod decodeMethod) {

        this.grammar = grammar;
        this.decodeMethod = decodeMethod;

        switch (inputFormat) {

        case Token:
            this.sentence = input.trim();
            this.inputTree = null;
            this.inputTags = null;
            this.stringInputTags = null;
            break;

        case Text:
            this.sentence = Tokenizer.treebankTokenize(input.trim());
            this.inputTree = null;
            this.inputTags = null;
            this.stringInputTags = null;
            break;

        case Tree: {
            this.inputTree = NaryTree.read(input.trim(), String.class);
            this.sentence = Strings.join(inputTree.leafLabels(), " ");

            // extract POS tags from tree
            this.inputTags = new int[sentence.length()];
            this.stringInputTags = new String[sentence.length()];
            int i = 0;
            for (final NaryTree<String> leaf : inputTree.leafTraversal()) {
                stringInputTags[i] = leaf.parent().label();
                inputTags[i] = getInputTagIndex(stringInputTags[i]);
                i++;
            }
        }
            break;

        case Tagged: {
            // (DT The) (NN economy) (POS 's) (NN temperature) (MD will)
            final StringBuilder sb = new StringBuilder(128);
            final String[] split = input.split("\\s+");
            this.inputTags = new int[split.length / 2];
            this.stringInputTags = new String[split.length / 2];

            for (int i = 0; i < split.length; i += 2) {
                final String tag = split[i].substring(1); // remove "("
                final String token = split[i + 1].substring(0, split[i + 1].length() - 1); // remove ")"

                stringInputTags[i / 2] = tag;
                inputTags[i / 2] = grammar.nonTermSet.getIndex(tag);

                sb.append(token);
                sb.append(' ');
            }
            this.sentence = sb.substring(0, sb.length() - 1); // Remove final space
            this.inputTree = null;
            break;
        }

        default:
            throw new UnsupportedOperationException("Unsupported inputFormat: " + inputFormat);
        }

        this.tokens = grammar.tokenizer.tokenizeToIndex(sentence);
        this.recoveryStrategy = recoveryStrategy;
    }

    public ParseTask(final String input, final InputFormat inputFormat, final Grammar grammar,
            final DecodeMethod decodeMethod) {
        this(input, inputFormat, grammar, null, decodeMethod);
    }

    public ParseTask(final int[] tokens, final Grammar grammar) {
        this.tokens = tokens;
        this.grammar = grammar;
        this.sentence = null;
        this.inputTags = null;
        this.stringInputTags = null;
        this.fomTags = null;
        this.recoveryStrategy = null;
        this.decodeMethod = DecodeMethod.ViterbiMax;
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
            result.append(String.format("\nINFO: sentLen=%d time=%d inside=%.5f reparses=%d %s", sentenceLength(),
                    parseTimeMs, insideProbability, reparseStages, chartStats));
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
                            totalPopulatedEdges, totalPushes, nBinaryConsidered + nUnaryConsidered, nLex,
                            nLexUnaryConsidered, nUnaryConsidered, nBinaryConsidered, chartInitMs, fomInitMs, ccInitMs,
                            insideBinaryNs / 1000000, unaryAndPruningNs / 1000000, outsidePassMs, extractTimeMs));
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
        // If we fell all the way back to the 'recovery' parse, even though we output a parse structure, we count
        // that as a failed parse
        return binaryParse == null;
    }

    public void evaluate(final BracketEvaluator evaluator) {
        if (inputTree != null && !parseFailed()) {
            try {
                evalb = evaluator.evaluate(inputTree, naryParse());
            } catch (final Exception e) {
                BaseLogger.singleton().info("ERROR: input tree " + inputTree + " is ill-formd.  Skipping evaluation.");
                evalb = null;
            }
        }
    }
}