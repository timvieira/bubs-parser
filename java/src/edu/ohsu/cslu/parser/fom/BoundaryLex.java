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
package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.TreeTools;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.fom.FigureOfMerit.FOMType;

/**
 * @author Nathan Bodenstab
 */
public final class BoundaryLex extends FigureOfMeritModel {

    private Grammar grammar;
    private float OOV_SCORE;
    private int lexMap[] = null;
    private SymbolSet<String> wordClasses;
    private Tokenizer fomTokenizer = null; // old way to map lex

    // Model params learned from training data
    private final float leftBoundaryLogProb[][], rightBoundaryLogProb[][];// , bigramLogProb[][];

    // private SimpleCounterSet<Integer> bigramLogProb;

    public BoundaryLex(final FOMType type, final Grammar grammar, final BufferedReader modelStream) throws IOException {
        super(type);

        this.grammar = grammar;

        final int numNT = grammar.numNonTerms();
        final int numLex = grammar.lexSet.size();
        // TODO: only need number of classes here ... but would have to parse file first

        leftBoundaryLogProb = new float[numLex][numNT];
        rightBoundaryLogProb = new float[numLex][numNT];
        // bigramLogProb = new SimpleCounterSet<Integer>();
        // bigramLogProb = new float[numLex][numLex];

        OOV_SCORE = GlobalConfigProperties.singleton().getFloatProperty("boundaryLexOOV");
        for (int i = 0; i < numLex; i++) {
            Arrays.fill(leftBoundaryLogProb[i], OOV_SCORE);
            Arrays.fill(rightBoundaryLogProb[i], OOV_SCORE);
            // Arrays.fill(bigramLogProb[i], Float.NEGATIVE_INFINITY);
        }

        if (modelStream != null) {
            readModel(modelStream);
        }
    }

    public BoundaryLex(final FOMType type) {
        super(type);
        leftBoundaryLogProb = null;
        rightBoundaryLogProb = null;
    }

    @Override
    public FigureOfMerit createFOM() {
        switch (type) {
        case BoundaryLex:
            return new BoundaryLexSelector();
        default:
            return super.createFOM();
        }
    }

    public void readModel(final BufferedReader inStream) throws IOException {
        String line;

        if (lexMap == null) {
            lexMap = new int[grammar.lexSet.size()];
            wordClasses = new SymbolSet<String>();
        }

        while ((line = inStream.readLine()) != null) {
            // line format: label num | denom prob
            final String[] tokens = line.split("\\s+");
            if (tokens.length > 0 && !tokens[0].equals("#")) {

                if (tokens[0].equals("LB")) {
                    // LB: nt | lex prob
                    leftBoundaryLogProb[wordClasses.addSymbol(tokens[3])][getNonTermIndex(tokens[1])] = Float
                            .parseFloat(tokens[4]);
                    ;
                } else if (tokens[0].equals("RB")) {
                    // RB: lex | nt prob;
                    rightBoundaryLogProb[wordClasses.addSymbol(tokens[1])][getNonTermIndex(tokens[3])] = Float
                            .parseFloat(tokens[4]);
                } else if (tokens[0].equals("PN")) {
                    // PN: lex | lexHist prob
                    // bigramLogProb.increment(getLexIndex(denomStr), getLexIndex(numStr), prob);
                } else if (tokens[0].equals("MAP")) {
                    final int word = grammar.mapLexicalEntry(tokens[1]);
                    final int wordClass = wordClasses.addSymbol(tokens[3]);
                    lexMap[word] = wordClass;
                } else {
                    System.err.println("WARNING: ignoring line in model file '" + line + "'");
                }
            }
        }
    }

    private int getNonTermIndex(final String nt) {
        final int i = grammar.mapNonterminal(nt);
        if (i < 0) {
            throw new RuntimeException("ERROR: non-terminal '" + nt + "' from FOM model not found in grammar.");
        }
        return i;
    }

    // private int getLexIndex(final String lex) {
    // final int i = grammar.mapLexicalEntry(lex);
    // if (i < 0) {
    // throw new RuntimeException("ERROR: lexical entry '" + lex + "' from FOM model not found in grammar.");
    // }
    // return i;
    // }

    private void parseLexMapFile(final String file, final Tokenizer tokenizer, final String defaultClass)
            throws IOException {
        wordClasses = new SymbolSet<String>();
        final int defaultClassIndex = wordClasses.addSymbol(defaultClass);
        lexMap = new int[tokenizer.lexSize()];
        Arrays.fill(lexMap, defaultClassIndex);
        lexMap[grammar.nullWord] = wordClasses.addSymbol(Grammar.nullSymbolStr);

        String line;
        final BufferedReader f = new BufferedReader(new FileReader(file));
        while ((line = f.readLine()) != null) {
            final String[] toks = line.split("[ \t]+");
            // Expecting format: <word> <class>
            if (toks.length >= 2) {
                final int word = tokenizer.wordToLexSetIndex(toks[0], false);
                final int wordClass = wordClasses.addSymbol(toks[1]);
                lexMap[word] = wordClass;
                // NB: Multiple words may be mapped to the same UNK class with different clusters.
                // We could take the most frequent class from this set. Right now we are just
                // taking the last occurance.
            } else {
                System.err.println("WARNING: Unexpected linke in lex count file: '" + line.trim() + "'");
            }
        }
    }

    public void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile,
            final double smoothingCount, final boolean writeCounts, final int posNgramOrder, final int pruneCount,
            final String lexCountFile, final int unkThresh, final String lexMapFile) throws Exception {
        String line, historyStr;
        final String joinString = " ";
        ParseTree tree;
        final SimpleCounterSet<String> leftBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> rightBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> bigramCount = new SimpleCounterSet<String>();

        grammar = ParserDriver.readGrammar(grammarFile, ResearchParserType.ECPCellCrossList, null);

        if (lexCountFile != null) {
            fomTokenizer = new Tokenizer(lexCountFile, unkThresh);
        } else if (lexMapFile != null) {
            parseLexMapFile(lexMapFile, grammar.tokenizer, "0");
        } else {
            throw new RuntimeException("Expecting lexCountFile or lexMapFile in BoundaryLex FOM training.");
        }

        while ((line = inStream.readLine()) != null) {
            tree = ParseTree.readBracketFormat(line);
            if (tree.isBinaryTree() == false) {
                // System.err.println("ERROR: Training trees must be binarized exactly as used in decoding grammar");
                // System.exit(1);
                TreeTools.binarizeTree(tree, grammar.isRightFactored(), grammar.horizontalMarkov,
                        grammar.verticalMarkov, false, grammar.grammarFormat);
            }
            // tree.linkLeavesLeftRight();
            for (final ParseTree node : tree.preOrderTraversal()) {

                if (node.isNonTerminal() == true) {
                    if (grammar.nonTermSet.contains(node.contents) == false) {
                        throw new IOException("Nonterminal '" + node.contents
                                + "' in input tree not found in grammar.  Exiting.");
                    }

                    leftBoundaryCount.increment(node.contents, mapLex(node.leftMostLeaf().leftNeighbor));
                    rightBoundaryCount.increment(mapLex(node.rightMostLeaf().rightNeighbor), node.contents);
                }
            }

            // n-gram POS prob
            final LinkedList<String> history = new LinkedList<String>();
            for (int i = 0; i < posNgramOrder - 1; i++) {
                history.addLast(Grammar.nullSymbolStr); // pad history with nulls (for beginning of string)
            }

            // iterate through leaf nodes using .rightNeighbor
            for (ParseTree leafNode = tree.leftMostLeaf(); leafNode != null; leafNode = leafNode.rightNeighbor) {
                historyStr = Util.join(history, joinString);
                final String word = mapLex(leafNode);
                bigramCount.increment(word, historyStr);
                history.removeFirst();
                history.addLast(word);
            }

            // finish up with final transition to <null>
            historyStr = Util.join(history, joinString);
            bigramCount.increment(Grammar.nullSymbolStr, historyStr);
        }

        if (smoothingCount > 0) {
            throw new RuntimeException(
                    "Smoothing param found to be "
                            + smoothingCount
                            + " but explicit smoothing not implemented for BoundaryLex FOM because model size becomes too large.");
        }
        // final int numNT = grammar.numNonTerms();
        // final int numLex = grammar.lexSet.size();
        // smooth counts
        // if (smoothingCount > 0) {
        // leftBoundaryCount.smoothAddConst(smoothingCount, numLex);
        // rightBoundaryCount.smoothAddConst(smoothingCount, numNT);
        // bigramCount.smoothAddConst(smoothingCount, numLex);
        // }

        // Write model to file
        float score;
        outStream.write("# model=FOM type=BoundaryLex boundaryNgramOrder=2 lexNgramOrder=" + posNgramOrder + "\n");

        if (lexMap != null) {
            for (int i = 0; i < lexMap.length; i++) {
                outStream.write("MAP " + grammar.mapLexicalEntry(i) + " => " + wordClasses.getSymbol(lexMap[i]) + "\n");
            }
        }

        // left boundary = P(NT | POS-1)
        for (final String lexStr : leftBoundaryCount.items.keySet()) {
            for (final Map.Entry<String, Float> entry : leftBoundaryCount.items.get(lexStr).entrySet()) {
                final String ntStr = entry.getKey();
                final int count = (int) leftBoundaryCount.getCount(ntStr, lexStr);
                if (writeCounts) {
                    score = count;
                } else {
                    score = (float) Math.log(leftBoundaryCount.getProb(ntStr, lexStr));
                }
                if (score > Float.NEGATIVE_INFINITY && count >= pruneCount) {
                    outStream.write("LB " + ntStr + " | " + lexStr + " " + score + "\n");
                }
            }
        }

        // right boundary = P(POS+1 | NT)
        for (final String ntStr : rightBoundaryCount.items.keySet()) {
            for (final Map.Entry<String, Float> entry : rightBoundaryCount.items.get(ntStr).entrySet()) {
                final String lexStr = entry.getKey();
                final int count = (int) rightBoundaryCount.getCount(lexStr, ntStr);
                if (writeCounts) {
                    score = count;
                } else {
                    score = (float) Math.log(rightBoundaryCount.getProb(lexStr, ntStr));
                }
                if (score > Float.NEGATIVE_INFINITY && count >= pruneCount) {
                    outStream.write("RB " + lexStr + " | " + ntStr + " " + score + "\n");
                }
            }
        }

        // pos n-gram = P(POS | POS-1)
        // for (final String histLex : bigramCount.items.keySet()) {
        // for (final Map.Entry<String, Float> entry : bigramCount.items.get(histLex).entrySet()) {
        // final String lex = entry.getKey();
        // final int count = (int) bigramCount.getCount(lex, histLex);
        // if (writeCounts) {
        // score = count;
        // } else {
        // score = (float) Math.log(bigramCount.getProb(lex, histLex));
        // }
        // if (score > Float.NEGATIVE_INFINITY && count >= pruneCount) {
        // outStream.write("PN " + lex + " | " + histLex + " " + score + "\n");
        // }
        // }
        // }

        outStream.close();
    }

    private String mapLex(final ParseTree leaf) {
        if (leaf == null) {
            return Grammar.nullSymbolStr;
        }
        // Not computing if sentence initial because word in the model are isolated
        // and never observed in context
        // final boolean sentenceInitial = (leaf.leftNeighbor == null);
        return mapLex(leaf.contents);
    }

    private int mapLexToIndex(final String word) {
        if (word == null) {
            return grammar.nullWord;
        }
        if (fomTokenizer != null) {
            // Pretty much the identity mapping.
            // Use the lexical set from the FOM model (potentially more restrictive) to
            // choose if words are lexicalized or converted to UNK-xxx. But after this choice
            // is made, convert words to an entry in the grammar lexicon (which may or may not
            // be different).
            if (fomTokenizer.hasWord(word)) {
                // return grammar.tokenizer.wordToLexSetEntry(word, false);
                return grammar.tokenizer.wordToLexSetIndex(word, false);
            }
            // return grammar.tokenizer.wordToUnkEntry(word, false);
            return grammar.mapLexicalEntry(grammar.tokenizer.wordToUnkEntry(word, false));
        }

        // Otherwise, use the lexMap
        final int wordIndex = grammar.tokenizer.wordToLexSetIndex(word, false);
        return lexMap[wordIndex];
        // final int classIndex = lexMap[wordIndex];
        // return wordClasses.getSymbol(classIndex);
    }

    private String mapLex(final String word) {
        if (fomTokenizer != null) {
            return grammar.mapLexicalEntry(mapLexToIndex(word));
        }
        return wordClasses.getSymbol(mapLexToIndex(word));
    }

    public class BoundaryLexSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;

        // pre-computed left/right FOM outside scores for current sentence
        // private float outsideLeft[][], outsideRight[][];
        private int tokens[];

        public BoundaryLexSelector() {
        }

        @Override
        public float calcFOM(final int start, final int end, final short nt, final float insideProbability) {
            // String left = Grammar.nullSymbolStr;
            // if (start > 0) {
            // left = grammar.mapLexicalEntry(tokens[start - 1]);
            // }
            // String right = Grammar.nullSymbolStr;
            // if (end < tokens.length) {
            // right = grammar.mapLexicalEntry(tokens[end]);
            // }
            // System.out.println(grammar.mapNonterminal(parent) + "[" + start + "," + end + "]: left[" + left + "]="
            // + outsideLeft(start, parent) + " right[" + right + "]=" + outsideRight(end, parent));

            return insideProbability + outsideLeft(start, nt) + outsideRight(end, nt);
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability;
        }

        private float outsideLeft(final int start, final int nt) {
            if (start == 0) {
                return leftBoundaryLogProb[grammar.nullWord][nt];
            }
            return leftBoundaryLogProb[tokens[start - 1]][nt];
        }

        private float outsideRight(final int end, final int nt) {
            if (end == tokens.length) {
                return rightBoundaryLogProb[grammar.nullWord][nt];
            }
            return rightBoundaryLogProb[tokens[end]][nt];
        }

        /**
         * Computes forward-backward and left/right boundary probs across ambiguous POS tags. Also computes 1-best POS
         * tag sequence based on viterbi-max decoding
         */
        @Override
        public void initSentence(final ParseTask task, final Chart chart) {
            this.tokens = task.tokens;

            // final int sentLen = task.sentenceLength();
            // final int fbSize = sentLen + 2;

            // if (outsideLeft == null || outsideLeft.length < fbSize) {
            // outsideLeft = new float[fbSize + 10][grammar.numNonTerms()];
            // outsideRight = new float[fbSize + 10][grammar.numNonTerms()];
            // }

            // for (int i = 0; i < fbSize; i++) {
            // Arrays.fill(outsideLeft[i], Float.NEGATIVE_INFINITY);
            // Arrays.fill(outsideRight[i], Float.NEGATIVE_INFINITY);
            // }

            // final float ngramScore[] = new float[fbSize];
            // ngramScore[0] = 0f;
            // for (int i = 1; i < fbSize; i++) {
            // final int prevWord = fbIndexToWordIndex(i - 1, task.tokens);
            // final int word = fbIndexToWordIndex(i, task.tokens);
            // float prob = bigramLogProb.getCount(prevWord, word);
            // // TODO: Should smooth unobserved bigrams in the model
            // if (prob == 0) {
            // prob = OOV_SCORE;
            // }
            // ngramScore[i] = ngramScore[i - 1] + prob;
            // }

            // for (int i = 0; i < fbSize; i++) {
            // final int word = fbIndexToWordIndex(i, task.tokens);
            // // final float ngramFwd = ngramScore[i];
            // // final float ngramBkw = ngramScore[fbSize - 1] - ngramScore[i];
            // for (int nt = 0; nt < grammar.numNonTerms(); nt++) {
            // // outsideLeft[i][nt] = ngramFwd + leftBoundaryLogProb[word][nt];
            // // outsideRight[i][nt] = ngramBkw + rightBoundaryLogProb[word][nt];
            // outsideLeft[i][nt] = leftBoundaryLogProb[word][nt];
            // outsideRight[i][nt] = rightBoundaryLogProb[word][nt];
            // //System.out.println("LB: outLeft[" + i + "][" + grammar.mapNonterminal(nt) + "] = leftBound["
            // // + grammar.mapLexicalEntry(word) + "][" + grammar.mapNonterminal(nt) + "]");
            // //System.out.println("LB: outRite[" + i + "][" + grammar.mapNonterminal(nt) + "] = riteBound["
            // // + grammar.mapLexicalEntry(word) + "][" + grammar.mapNonterminal(nt) + "]");
            // }
            // }
        }
    }
}
