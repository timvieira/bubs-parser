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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.TreeTools;
import edu.ohsu.cslu.parser.chart.Chart;

/**
 * @author Nathan Bodenstab
 */
public final class BoundaryLex extends FigureOfMeritModel {

    private Grammar grammar;
    private int lexToClassMap[] = null;
    private HashMap<String, LinkedList<Integer>> classToLexMap = null;
    private SymbolSet<String> wordClasses;
    // private Tokenizer fomTokenizer = null; // old way to map lex

    // Model params learned from training data
    private final float leftBoundaryLogProb[][], rightBoundaryLogProb[][];
    private final float unkLBLogProb[], unkRBLogProb[];

    public BoundaryLex(final FOMType type, final Grammar grammar, final BufferedReader modelStream) throws Exception {
        super(type);

        this.grammar = grammar;

        final int numNT = grammar.numNonTerms();
        final int numLex = grammar.lexSet.size();

        leftBoundaryLogProb = new float[numLex][numNT];
        rightBoundaryLogProb = new float[numLex][numNT];
        unkLBLogProb = new float[numLex];
        unkRBLogProb = new float[numNT];

        for (int i = 0; i < numLex; i++) {
            Arrays.fill(leftBoundaryLogProb[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(rightBoundaryLogProb[i], Float.NEGATIVE_INFINITY);
        }

        if (modelStream != null) {
            readModel(modelStream);
        }
    }

    public BoundaryLex(final FOMType type) {
        super(type);
        leftBoundaryLogProb = null;
        rightBoundaryLogProb = null;
        unkLBLogProb = null;
        unkRBLogProb = null;
    }

    @Override
    public FigureOfMerit createFOM() {
        return new BoundaryLexSelector();
    }

    public void readModel(final BufferedReader inStream) throws Exception {
        String line;

        while ((line = inStream.readLine()) != null) {
            // line format: label num | denom prob
            final String[] tokens = line.split("\\s+");
            if (tokens.length > 0 && !tokens[0].equals("#")) {

                try {

                    if (tokens[0].equals("LB")) {
                        // LB: nt | lex prob
                        for (final int lexIndex : getLexInClass(tokens[3])) {
                            leftBoundaryLogProb[lexIndex][getNonTermIndex(tokens[1])] = Float.parseFloat(tokens[4]);
                        }
                    } else if (tokens[0].equals("RB")) {
                        // RB: lex | nt prob;
                        for (final int lexIndex : getLexInClass(tokens[1])) {
                            rightBoundaryLogProb[lexIndex][getNonTermIndex(tokens[3])] = Float.parseFloat(tokens[4]);
                        }
                    } else if (tokens[0].equals("MAP")) {
                        // If MAP is going to be used, all MAP lines must occur before LB or RB lines
                        if (classToLexMap == null) {
                            classToLexMap = new HashMap<String, LinkedList<Integer>>();
                        }
                        final String cls = tokens[3];
                        if (!classToLexMap.containsKey(cls)) {
                            classToLexMap.put(cls, new LinkedList<Integer>());
                        }
                        classToLexMap.get(cls).add(grammar.mapLexicalEntry(tokens[1]));
                    } else if (tokens[0].equals("UNK")) {
                        if (tokens[1].equals("RB")) {
                            final int ntIndex = grammar.mapNonterminal(tokens[2]);
                            unkRBLogProb[ntIndex] = Float.parseFloat(tokens[3]);
                        } else { // LB
                            // int classIndex = wordClasses.getIndex(tokens[2]);
                            final float score = Float.parseFloat(tokens[3]);
                            if (classToLexMap != null) {
                                for (final int lexIndex : classToLexMap.get(tokens[2])) {
                                    // map prob to all lex entries that are mapped to this class for faster retrieval
                                    // during
                                    // parsing
                                    unkLBLogProb[lexIndex] = score;
                                }
                            } else {
                                unkLBLogProb[grammar.mapLexicalEntry(tokens[2])] = score;
                            }
                        }
                    } else {
                        System.err.println("WARNING: ignoring line in model file '" + line + "'");
                    }

                } catch (final Exception e) {
                    System.err.println("ERROR parsing: " + line);
                    throw e;
                }
            }
        }

        // smooth with UNK class probs
        // for (final String lexStr : grammar.lexSet) {
        // if (lexStr != Grammar.nullSymbolStr && !lexStr.startsWith("UNK")) {
        // final int lex = grammar.mapLexicalEntry(lexStr);
        // final int unkIndex = grammar.mapLexicalEntry(grammar.tokenizer.wordToUnkEntry(lexStr, false));
        // // NB: should iterate over the int values ...
        // // for (int nt : grammar.nonTermSet.values()) {
        // for (final String ntStr : grammar.nonTermSet) {
        // final int nt = getNonTermIndex(ntStr);
        // final float leftLex = leftBoundaryLogProb[lex][nt];
        // final float leftUNK = leftBoundaryLogProb[unkIndex][nt];
        // leftBoundaryLogProb[lex][nt] = fomSmoothLexTune * leftUNK + (1 - fomSmoothLexTune) * leftLex;
        //
        // final float rightLex = rightBoundaryLogProb[lex][nt];
        // final float rightUNK = leftBoundaryLogProb[unkIndex][nt];
        // rightBoundaryLogProb[lex][nt] = fomSmoothLexTune * rightUNK + (1 - fomSmoothLexTune) * rightLex;
        // }
        // }
        // }
    }

    private LinkedList<Integer> getLexInClass(final String classStr) {
        if (classToLexMap == null) {
            // there are no classes. Lex => Lex
            final LinkedList<Integer> tmpList = new LinkedList<Integer>();
            // tmpList.add(grammar.mapLexicalEntry(classStr));
            // NB: When using the SM5 grammar, all lexical items in the training set
            // were not included in the grammar. This caused the above line to return
            // -1 and failed.
            tmpList.add(grammar.tokenizer.wordToLexSetIndex(classStr, false));
            return tmpList;
        }
        return classToLexMap.get(classStr);
    }

    // private int getClassIndex(final String classStr) {
    // // if classes are going to be used, lexMap has to be fully populated
    // // before using this function.
    // if (lexMap == null) {
    // return grammar.mapLexicalEntry(classStr);
    // }
    // return wordClasses.getIndex(classStr);
    // }

    // private int getClassIndex(final int lexIndex) {
    // if (lexMap == null) {
    // return lexIndex;
    // }
    // return lexMap[lexIndex];
    // }

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
        lexToClassMap = new int[tokenizer.lexSize()];
        Arrays.fill(lexToClassMap, defaultClassIndex);
        lexToClassMap[grammar.nullSymbol()] = wordClasses.addSymbol(Grammar.nullSymbolStr);

        String line;
        final BufferedReader f = new BufferedReader(new FileReader(file));
        while ((line = f.readLine()) != null) {
            final String[] toks = line.split("[ \t]+");
            // Expecting format: <word> <class>
            if (toks.length >= 2) {
                final int word = tokenizer.wordToLexSetIndex(toks[0], false);
                final int wordClass = wordClasses.addSymbol(toks[1]);
                lexToClassMap[word] = wordClass;
                // NB: Multiple words may be mapped to the same UNK class with different clusters.
                // We could take the most frequent class from this set. Right now we are just
                // taking the last occurrence.
            } else {
                System.err.println("WARNING: Unexpected linke in lex count file: '" + line.trim() + "'");
            }
        }
    }

    // NB: lexCount and unkThresh are only used for fomTokenizer, which is only used
    // when not clustering
    // NB: We don't handle UNKs with the cluster map very well. Since we are borrowing the clusters
    // from Koo et al, they didn't cluster all of our different UNK classes (I see one UNKNOWN entry
    // in their list, but don't know how it was used). Right now, our UNK words are somewhat arbitrarily
    // assigned to classes (see comments in parseLexMapFile())
    public void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile,
            final double smoothingCount, final boolean writeCounts, final int pruneCount, final String lexCountFile,
            final int unkThresh, final String lexMapFile) throws Exception {
        String line;
        ParseTree tree;
        HashMap<String, Integer> lexCounts = null;
        final SimpleCounterSet<String> leftBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> rightBoundaryCount = new SimpleCounterSet<String>();

        grammar = ParserDriver.readGrammar(grammarFile, ResearchParserType.ECPCellCrossList, null);

        if (lexCountFile != null) {
            lexCounts = Tokenizer.readLexCountFile(lexCountFile);
            wordClasses = grammar.lexSet;
        } else if (lexMapFile != null) {
            parseLexMapFile(lexMapFile, grammar.tokenizer, "0");
        } else {
            throw new RuntimeException("Expecting lexCountFile or lexMapFile in BoundaryLex FOM training.");
        }

        while ((line = inStream.readLine()) != null) {
            tree = ParseTree.readBracketFormat(line);
            if (tree.isBinaryTree() == false) {
                TreeTools.binarizeTree(tree, grammar.binarization() == Binarization.RIGHT, grammar.horizontalMarkov,
                        grammar.verticalMarkov, false, grammar.grammarFormat);
            }

            for (final ParseTree node : tree.preOrderTraversal()) {
                if (node.isNonTerminal() == true) {
                    if (grammar.nonTermSet.containsKey(node.contents) == false) {
                        throw new IOException("Nonterminal '" + node.contents
                                + "' in input tree not found in grammar.  Exiting.");
                    }
                    final ParseTree lbNode = node.leftMostLeaf().leftNeighbor;
                    final ParseTree rbNode = node.rightMostLeaf().rightNeighbor;

                    leftBoundaryCount.increment(node.contents, lexStrToClusterStr(lbNode));
                    rightBoundaryCount.increment(lexStrToClusterStr(rbNode), node.contents);

                    // extra UNK counts when not clustering
                    if (lexCounts != null) {
                        if (lbNode != null) {
                            final String word = lbNode.contents;
                            if (!lexCounts.containsKey(word) || lexCounts.get(word) <= unkThresh) {
                                final String unkWord = grammar.tokenizer.wordToUnkString(word,
                                        lbNode.leftNeighbor == null);
                                leftBoundaryCount.increment(node.contents, unkWord);
                            }
                        }
                        if (rbNode != null) {
                            final String word = rbNode.contents;
                            if (!lexCounts.containsKey(word) || lexCounts.get(word) <= unkThresh) {
                                final String unkWord = grammar.tokenizer.wordToUnkString(word, false);
                                rightBoundaryCount.increment(unkWord, node.contents);
                            }
                        }
                    }
                }
            }
        }

        if (smoothingCount > 0) {
            leftBoundaryCount.smoothAddConst(smoothingCount, wordClasses.size());
            rightBoundaryCount.smoothAddConst(smoothingCount, grammar.phraseSet.length);
        }

        // Write model to file
        float score;
        outStream.write("# model=FOM type=BoundaryLex smoothingCount=" + smoothingCount + " pruneCount=" + pruneCount
                + " unkThresh=" + unkThresh + " lexCountFile=" + lexCountFile + " lexMapFile=" + lexMapFile + "\n");

        if (lexToClassMap != null) {
            for (int i = 0; i < lexToClassMap.length; i++) {
                outStream.write("MAP " + grammar.mapLexicalEntry(i) + " => " + wordClasses.getSymbol(lexToClassMap[i])
                        + "\n");
            }
        }

        // left boundary = P(NT | POS-1)
        // NOTE: iterating over the keySet skips all unobserved entries (even if they were
        // smoothed above)
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

        // write the unobserved transition probs once for each denominator
        // to save space in the model files
        for (final String classStr : wordClasses) {
            float unkProb = (float) Math.log(leftBoundaryCount.getProb("DOES-NOT-EXIST", classStr));
            if (unkProb == Float.NEGATIVE_INFINITY) {
                // word never observed at boundary
                unkProb = (float) Math.log(leftBoundaryCount.getProb("DOES-NOT-EXIST",
                        grammar.tokenizer.wordToUnkString(classStr, false)));
            }
            outStream.write("UNK LB " + classStr + " " + unkProb + "\n");
        }
        for (final short ntIndex : grammar.phraseSet) {
            final String ntStr = grammar.mapNonterminal(ntIndex);
            final float unkProb = (float) Math.log(rightBoundaryCount.getProb("DOES-NOT-EXIST", ntStr));
            outStream.write("UNK RB " + ntStr + " " + unkProb + "\n");
        }

        outStream.close();
    }

    private String lexStrToClusterStr(final ParseTree leaf) {
        if (leaf == null) {
            return Grammar.nullSymbolStr;
        }
        final String word = leaf.contents;

        // if (fomTokenizer != null) {
        // // Pretty much the identity mapping.
        // // Use the lexical set from the FOM model (potentially more restrictive) to
        // // choose if words are lexicalized or converted to UNK-xxx. But after this choice
        // // is made, convert words to an entry in the grammar lexicon (which may or may not
        // // be different).
        // if (fomTokenizer.hasWord(word)) {
        // // return grammar.tokenizer.wordToLexSetIndex(word, false);
        // return grammar.tokenizer.wordToLexSetEntry(word, false);
        // }
        // // return grammar.mapLexicalEntry(grammar.tokenizer.wordToUnkEntry(word, false));
        // return grammar.tokenizer.wordToUnkEntry(word, false);
        // } else
        if (lexToClassMap != null) {
            final int wordIndex = grammar.tokenizer.wordToLexSetIndex(word, false);
            final int clusterIndex = lexToClassMap[wordIndex];
            return wordClasses.getSymbol(clusterIndex);
        }

        // no mapping -- just use lexical entry directly
        return word;
    }

    public class BoundaryLexSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;
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
            // System.out.println(grammar.mapNonterminal(nt) + "[" + start + "," + end + "]: left[" + left + "]="
            // + outsideLeft(start, nt) + " right[" + right + "]=" + outsideRight(end, nt));

            return normInside(start, end, insideProbability) + outsideLeft(start, nt) + outsideRight(end, nt);
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability;
        }

        private float outsideLeft(final int start, final int nt) {
            int lex;
            if (start <= 0) {
                lex = grammar.nullSymbol();
            } else {
                lex = tokens[start - 1];
            }
            final float out = leftBoundaryLogProb[lex][nt];
            if (out == Float.NEGATIVE_INFINITY) {
                return unkLBLogProb[lex];
            }
            return out;
        }

        private float outsideRight(final int end, final int nt) {
            int lex;
            if (end >= tokens.length) {
                lex = grammar.nullSymbol();
            } else {
                lex = tokens[end];
            }
            final float out = rightBoundaryLogProb[lex][nt];
            if (out == Float.NEGATIVE_INFINITY) {
                return unkRBLogProb[nt];
            }
            return out;
        }

        @Override
        public void initSentence(final ParseTask task, final Chart chart) {
            super.initSentence(task, chart);
            this.tokens = task.tokens;
        }
    }
}
