/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.parser.fom;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.TreeTools;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * A figure-of-merit model that conditions boundary probabilities on lexical tokens at or near the boundary. In many
 * cases, this is more effective than conditioning on parts-of-speech alone (as in {@link BoundaryPosModel}).
 * 
 * To alleviate sparse-data problems and estimate robust probabilities, this class also allows clustering at the lexical
 * level. See the Brown clustering algorithm (Brown et al., 1992 "Class-based n-gram models of natural language).
 * 
 * Model file format:
 * 
 * <pre>
 * # Description line(s). E.g.:
 * # model=FOM type=BoundaryLex smoothingCount=0.5 unkThresh=5 lexMapFile=/Users/aarond/Dropbox/research/models/bllip-clusters.len-8
 * MAP <token> => <class>
 * ...
 * LB <non-terminal> | <class> <log probability>
 * ...
 * RB <class> | <non-terminal> <log probability>
 * ...
 * </pre>
 * 
 * @author Nathan Bodenstab
 */
public final class BoundaryLex extends FigureOfMeritModel {

    private Grammar grammar;

    /** Maps from a class / cluster to the list of words in the cluster */
    private HashMap<String, IntArrayList> classToLexMap = null;

    // TODO In most cases, we learn these probabilities for word clusters, but we store them
    // repeatedly for each word. We should add a sentence-level initialization (like
    // BoundaryPOS), map tokens to classes, and store the learned parameters only once.
    /**
     * Probability that a lexical item will occur immediately before a labeled span. Indexed by lexical class and
     * non-terminal
     */
    private final float[][] leftBoundaryLogProb;

    /**
     * Probability that a lexical item will occur immediately after a labeled span. Indexed by lexical class and
     * non-terminal
     */
    private final float[][] rightBoundaryLogProb;

    /**
     * Probability that an unknown word will occur immediately before a labeled span. Indexed by lexical class.
     */
    private final float[] unkLBLogProb;

    /**
     * Probability that an unknown word will occur immediately after a labeled span. Indexed by non-terminal.
     */
    private final float[] unkRBLogProb;

    /** Used in training to map from words to the class of each word */
    private int lexToClassMap[] = null;

    /** Used during training to track word classes */
    private MutableEnumeration<String> wordClasses;

    /**
     * Constructor used at inference time - reads the model from <code>modelReader</code>.
     * 
     * @param type
     * @param grammar
     * @param modelReader
     * @throws IOException if unable to read the model
     */
    public BoundaryLex(final FOMType type, final Grammar grammar, final BufferedReader modelReader) throws IOException {

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

        if (modelReader != null) {
            readModel(modelReader);
        }
    }

    /**
     * Constructor for use in training
     */
    BoundaryLex() {
        super(FOMType.BoundaryLex);
        leftBoundaryLogProb = null;
        rightBoundaryLogProb = null;
        unkLBLogProb = null;
        unkRBLogProb = null;
    }

    @Override
    public FigureOfMerit createFOM() {
        return new BoundaryLexFom();
    }

    private void readModel(final BufferedReader modelReader) throws IOException {

        for (String line = modelReader.readLine(); line != null; line = modelReader.readLine()) {

            // line format: label num | denom prob
            final String[] tokens = line.split("\\s+");

            // Skip empty lines and comment lines
            if (tokens.length == 0 || tokens[0].equals("#")) {
                continue;
            }

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
                        classToLexMap = new HashMap<String, IntArrayList>();
                    }
                    final String cls = tokens[3];
                    if (!classToLexMap.containsKey(cls)) {
                        classToLexMap.put(cls, new IntArrayList());
                    }
                    classToLexMap.get(cls).add(grammar.mapLexicalEntry(tokens[1]));

                } else if (tokens[0].equals("UNK")) {
                    if (tokens[1].equals("RB")) {
                        // This should really probably be conditioned on rare-word distributions, not just
                        // the last entry in the model file for each specific non-terminal
                        final int ntIndex = grammar.mapNonterminal(tokens[2]);
                        unkRBLogProb[ntIndex] = Float.parseFloat(tokens[3]);

                    } else { // LB
                        final float score = Float.parseFloat(tokens[3]);
                        if (classToLexMap != null) {
                            if (classToLexMap.containsKey(tokens[2])) {
                                for (final int lexIndex : classToLexMap.get(tokens[2])) {
                                    // map prob to all lex entries that are mapped to this class for faster retrieval
                                    // during parsing
                                    unkLBLogProb[lexIndex] = score;
                                }
                            }
                        } else {
                            unkLBLogProb[grammar.mapLexicalEntry(tokens[2])] = score;
                        }
                    }
                } else {
                    System.err.println("WARNING: ignoring line in model file '" + line + "'");
                }

            } catch (final RuntimeException e) {
                System.err.println("ERROR parsing: " + line);
                throw e;
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

    private IntArrayList getLexInClass(final String classStr) {
        if (classToLexMap == null) {
            // there are no classes. Lex => Lex
            final IntArrayList tmpList = new IntArrayList();
            // tmpList.add(grammar.mapLexicalEntry(classStr));
            // NB: When using the SM5 grammar, all lexical items in the training set
            // were not included in the grammar. This caused the above line to return
            // -1 and failed.
            tmpList.add(grammar.tokenClassifier.lexiconIndex(classStr, false, grammar.lexSet));
            return tmpList;
        }
        return classToLexMap.get(classStr);
    }

    /**
     * @param nt
     * @return The index of the specified non-terminal
     * @throws RuntimeException if the non-terminal is not found in the grammar
     */
    private int getNonTermIndex(final String nt) {
        final int i = grammar.mapNonterminal(nt);
        if (i < 0) {
            throw new RuntimeException("ERROR: non-terminal '" + nt + "' from FOM model not found in grammar.");
        }
        return i;
    }

    private void readClusterFile(final File file, final String defaultClass) throws IOException {

        wordClasses = new MutableEnumeration<String>();
        final int defaultClassIndex = wordClasses.addSymbol(defaultClass);
        lexToClassMap = new int[grammar.lexSet.size()];
        Arrays.fill(lexToClassMap, defaultClassIndex);
        lexToClassMap[grammar.nullToken()] = wordClasses.addSymbol(Grammar.nullSymbolStr);

        final BufferedReader br = new BufferedReader(new FileReader(file));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // Expecting format: <word> <class> - split on whitespace
            final String[] split = line.split("[ \t]+");
            if (split.length >= 2) {
                final int word = grammar.tokenClassifier.lexiconIndex(split[0], false, grammar.lexSet);
                final int wordClass = wordClasses.addSymbol(split[1]);
                lexToClassMap[word] = wordClass;
                // NB: Multiple words may be mapped to the same UNK class with different clusters.
                // We could take the most frequent class from this set. Right now we are just
                // taking the last occurrence.
            } else {
                System.err.println("WARNING: Unexpected line in lex count file: '" + line.trim() + "'");
            }
        }
    }

    // NB: lexCount and unkThresh are only used for fomTokenizer, which is only used
    // when not clustering
    //
    // NB: We don't handle UNKs with the cluster map very well. Since we are borrowing the clusters
    // from Koo et al, they didn't cluster all of our different UNK classes (I see one UNKNOWN entry
    // in their list, but don't know how it was used). Right now, our UNK words are somewhat arbitrarily
    // assigned to classes (see comments in parseLexMapFile())
    public void train(final BufferedReader trainingCorpusReader, final BufferedWriter outStream,
            final String grammarFile, final double smoothingCount, final boolean writeCounts, final int unkThresh,
            final File clusterFile) throws Exception {

        // TODO clean up and simplify a bit
        Object2IntOpenHashMap<String> lexCounts = null;
        final SimpleCounterSet<String> leftBoundaryCount = new SimpleCounterSet<String>();
        final SimpleCounterSet<String> rightBoundaryCount = new SimpleCounterSet<String>();

        grammar = readGrammar(grammarFile, ResearchParserType.ECPCellCrossList, null);

        // If no cluster file was specified, we'll treat each word as its own cluster. We need to count word occurrences
        // in the training corpus.
        if (clusterFile == null) {
            // Allow up to 30 MB of training data (about double WSJ 02-21)
            trainingCorpusReader.mark(30 * 1024 * 1024);
            lexCounts = new Object2IntOpenHashMap<String>();
            for (String line = trainingCorpusReader.readLine(); line != null; line = trainingCorpusReader.readLine()) {
                try {
                    final NaryTree<String> tree = NaryTree.read(line, String.class);
                    for (final String token : tree.leafLabels()) {
                        lexCounts.add(token, 1);
                    }
                } catch (final IllegalArgumentException ignore) {
                    // Skip any lines that aren't well-formed trees. This lets us use 'normal' parser output directly,
                    // including any INFO: lines
                }
            }
            wordClasses = grammar.lexSet;

            // Reset the reader so we can reread the corpus and count boundary occurrences
            // TODO Merge all counting into a single pass
            trainingCorpusReader.reset();

        } else {
            readClusterFile(clusterFile, "0");
        }

        for (String line = trainingCorpusReader.readLine(); line != null; line = trainingCorpusReader.readLine()) {
            ParseTree tree = null;

            try {
                tree = ParseTree.readBracketFormat(line);
            } catch (final RuntimeException e) {
                // Skip malformed trees, including any INFO output from a parsing run
                continue;
            }
            if (tree.isBinaryTree() == false) {
                TreeTools.binarizeTree(tree, grammar.binarization() == Binarization.RIGHT, grammar.horizontalMarkov,
                        grammar.verticalMarkov, false, grammar.grammarFormat);
            }

            for (final ParseTree node : tree.preOrderTraversal()) {
                if (!node.isLeafOrPreterminal()) {
                    if (grammar.nonTermSet.containsKey(node.label) == false) {
                        throw new IOException("Nonterminal '" + node.label
                                + "' in input tree not found in grammar.  Exiting.");
                    }
                    final ParseTree lbNode = node.leftMostLeaf().leftNeighbor;
                    final ParseTree rbNode = node.rightMostLeaf().rightNeighbor;

                    leftBoundaryCount.increment(node.label, cluster(lbNode));
                    rightBoundaryCount.increment(cluster(rbNode), node.label);

                    // extra UNK counts when not clustering
                    if (lexCounts != null) {
                        if (lbNode != null) {
                            final String word = lbNode.label;
                            if (!lexCounts.containsKey(word) || lexCounts.get(word) <= unkThresh) {
                                final String unkWord = DecisionTreeTokenClassifier.berkeleyGetSignature(word,
                                        lbNode.leftNeighbor == null, grammar.lexSet);
                                leftBoundaryCount.increment(node.label, unkWord);
                            }
                        }
                        if (rbNode != null) {
                            final String word = rbNode.label;
                            if (!lexCounts.containsKey(word) || lexCounts.get(word) <= unkThresh) {
                                final String unkWord = DecisionTreeTokenClassifier.berkeleyGetSignature(word, false,
                                        grammar.lexSet);
                                rightBoundaryCount.increment(unkWord, node.label);
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
        outStream.write("# model=FOM type=BoundaryLex smoothingCount=" + smoothingCount + " unkThresh=" + unkThresh
                + " lexMapFile=" + clusterFile + "\n");

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
                if (score > Float.NEGATIVE_INFINITY) {
                    outStream.write(String.format("LB %s | %s %.6f\n", ntStr, lexStr, score));
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
                if (score > Float.NEGATIVE_INFINITY) {
                    outStream.write(String.format("RB %s | %s %.6f\n", lexStr, ntStr, score));
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
                        DecisionTreeTokenClassifier.berkeleyGetSignature(classStr, false, grammar.lexSet)));
            }
            outStream.write("UNK LB " + classStr + " " + unkProb + "\n");
        }
        for (final short ntIndex : grammar.phraseSet) {
            final String ntStr = grammar.mapNonterminal(ntIndex);
            final float unkProb = (float) Math.log(rightBoundaryCount.getProb("DOES-NOT-EXIST", ntStr));
            outStream.write(String.format("UNK RB %s %.6f\n", ntStr, unkProb));
        }

        outStream.close();
    }

    private String cluster(final ParseTree leaf) {
        if (leaf == null) {
            return Grammar.nullSymbolStr;
        }
        final String word = leaf.label;

        if (lexToClassMap != null) {
            final int wordIndex = grammar.tokenClassifier.lexiconIndex(word, false, grammar.lexSet);
            final int clusterIndex = lexToClassMap[wordIndex];
            return wordClasses.getSymbol(clusterIndex);
        }

        // no mapping -- just use lexical entry directly
        return word;
    }

    public final class BoundaryLexFom extends FigureOfMerit {

        private static final long serialVersionUID = 1L;
        private int tokens[];

        public BoundaryLexFom() {
        }

        @Override
        public float calcFOM(final int start, final int end, final short nt, final float insideProbability) {
            return normInside(start, end, insideProbability) + outsideLeft(start, nt) + outsideRight(end, nt);
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability;
        }

        private float outsideLeft(final int start, final int nt) {
            final int lex = start <= 0 ? grammar.nullToken() : tokens[start - 1];
            final float out = leftBoundaryLogProb[lex][nt];
            return out == Float.NEGATIVE_INFINITY ? unkLBLogProb[lex] : out;
        }

        private float outsideRight(final int end, final int nt) {
            final int lex = end >= tokens.length ? grammar.nullToken() : tokens[end];
            final float out = rightBoundaryLogProb[lex][nt];
            return out == Float.NEGATIVE_INFINITY ? unkRBLogProb[nt] : out;
        }

        @Override
        public void initSentence(final ParseTask task, final Chart chart) {
            super.initSentence(task, chart);
            this.tokens = task.tokens;
        }
    }
}
