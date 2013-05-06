/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.parser.real;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.BinaryStringProduction;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.ListGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.StringProduction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Vocabulary;
import edu.ohsu.cslu.lela.FractionalCountGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.util.StringPool;
import edu.ohsu.cslu.util.Strings;

/**
 * Stores all grammar probabilities in the real domain (as 64-bit doubles) instead of the log domain. Used for
 * inside-outside parsing, with chart probability storage scaled by scaling tools to avoid numeric underflow. The
 * approach is adapted from the Berkeley parser. Individual probability calculations are more expensive (requiring
 * double-precision floating-point multiplies instead of simple 32-bit adds), but it avoids the expense and
 * precision-loss of repeated <code>logsumexp</code> operations.
 * 
 * This alternate representation means a lot of copy-and-paste code, mostly from {@link SparseMatrixGrammar} and
 * subclasses.
 * 
 * @see RealPackedArrayChart
 * @see RealInsideOutsideCphParser
 * 
 * @author Aaron Dunlop
 * @since May 3, 2013
 */
public class RealInsideOutsideCscSparseMatrixGrammar extends Grammar {

    private final static long serialVersionUID = 4L;

    /**
     * Parallel array of lexical rules (with {@link #lexicalProbabilities}. The first array dimension is indexed by
     * lexical index; the second contains valid parent non-terminal indices (the indices are not significant)
     */
    protected final short[][] lexicalParents;

    /**
     * Parallel array of lexical rules (with {@link #lexicalParents}. The first array dimension is indexed by lexical
     * index; the second contains production probabilities (the indices are not significant)
     */
    protected final double[][] lexicalProbabilities;

    public final static String nullSymbolStr = "<null>";
    public static Production nullProduction;
    // public static float UNSEEN_LEX_PROB = GlobalConfigProperties.singleton().getFloatProperty("unseenLexProb");
    public static double UNSEEN_LEX_PROB = 0;

    // == Grammar stats ==
    public final int numPosSymbols;
    public int horizontalMarkov;
    public int verticalMarkov;
    private Binarization binarization;
    public String language;

    /** The first non-terminal valid as a left child. */
    public final short leftChildrenStart;
    /** The last non-POS non-terminal valid as a left child. */
    public final short leftChildrenEnd;
    /** The first non-POS non-terminal valid as a right child. */
    public final short rightChildrenStart;
    /** The last non-POS non-terminal valid as a right child. */
    public final short rightChildrenEnd;
    /** The first POS */
    public final short posStart;
    /** The last POS */
    public final short posEnd;

    /** The number of lexical productions */
    private final int numLexProds;

    /** Maps a pair of (short) nonterminal indices into a single 32-bit integer */
    public final PackingFunction packingFunction;

    /**
     * Offsets into {@link #cscUnaryRowIndices} for the start of each column (child), with one extra entry appended to
     * prevent loops from falling off the end. Indexed by child non-terminal, so the length is 1 greater than |V|.
     */
    public final int[] cscUnaryColumnOffsets;

    /**
     * Row indices of each matrix entry in {@link #cscUnaryProbabilities}. One entry for each unary rule; the same size
     * as {@link #cscUnaryProbabilities}.
     */
    public final short[] cscUnaryRowIndices;

    /** Unary rule probabilities. One entry for each unary rule; the same size as {@link #cscUnaryRowIndices}. */
    public final double[] cscUnaryProbabilities;

    /**
     * Maximum unary production probability for each row (child) of the CSC storage. Allows us to short-circuit some
     * loops during pruned unary processing. One entry for each unary child.
     */
    public final double[] cscMaxUnaryProbabilities;

    /**
     * Indices of the first and last non-terminals which can combine as the right sibling with each non-terminal
     * (indexed by left-child index)
     */
    public final short[] minRightSiblingIndices;
    public final short[] maxRightSiblingIndices;

    /**
     * A temporary String -> String map, used to conserve memory while reading and sorting the grammar. Usage is similar
     * to {@link String#intern()}, but we don't need to internalize Strings indefinitely (and don't want them to be
     * stored in the VM's perm-gen), so we map them ourselves and allow the map to be GC'd after we're done constructing
     * the grammar.
     */
    private StringPool tmpStringPool;

    /**
     * Temporary storage of binary productions, used only in constructors and removed to save memory after
     * initialization
     */
    protected ArrayList<Production> tmpBinaryProductions;

    /**
     * Parallel array with {@link #cscBinaryPopulatedColumnOffsets}, containing indices of populated columns (child
     * pairs) and the offsets into {@link #cscBinaryRowIndices} at which each column starts. Array size is the number of
     * non-empty columns (+1 to simplify loops).
     */
    public final int[] cscBinaryPopulatedColumns;

    /**
     * Parallel array with {@link #cscBinaryPopulatedColumns} containing offsets into {@link #cscBinaryRowIndices} at
     * which each column starts.
     */
    public final int[] cscBinaryPopulatedColumnOffsets;

    /**
     * Offsets into {@link #cscBinaryRowIndices} and {@link #cscBinaryProbabilities} of the first entry for each column;
     * indexed by column number. Array size is the number of columns (+1 to simplify loops).
     */
    public final int[] cscBinaryColumnOffsets;

    /**
     * Row indices of each matrix entry in {@link #cscBinaryProbabilities}. One entry for each binary rule; the same
     * size as {@link #cscBinaryProbabilities}.
     */
    public final short[] cscBinaryRowIndices;

    /**
     * Binary rule probabilities One entry for each binary rule; the same size as {@link #cscBinaryRowIndices} .
     */
    public final double[] cscBinaryProbabilities;

    /**
     * Parallel array with {@link #cscBinaryPopulatedColumnOffsets}, containing indices of populated columns (child
     * pairs) and the offsets into {@link #cscBinaryRowIndices} at which each column starts. Array size is the number of
     * non-empty columns (+1 to simplify loops).
     */
    public final int[] factoredCscBinaryPopulatedColumns;

    /**
     * Parallel array with {@link #cscBinaryPopulatedColumns} containing offsets into {@link #cscBinaryRowIndices} at
     * which each column starts.
     */
    public final int[] factoredCscBinaryPopulatedColumnOffsets;

    /**
     * Offsets into {@link #factoredCscBinaryRowIndices} and {@link #factoredCscBinaryProbabilities} of the first entry
     * for each column; indexed by column number. Array size is the number of columns (+1 to simplify loops).
     */
    public final int[] factoredCscBinaryColumnOffsets;

    /**
     * Row indices of each matrix entry in {@link #cscBinaryProbabilities}. One entry for each binary rule; the same
     * size as {@link #cscBinaryProbabilities}.
     */
    public final short[] factoredCscBinaryRowIndices;

    /**
     * Binary rule probabilities One entry for each binary rule; the same size as {@link #cscBinaryRowIndices} .
     */
    public final double[] factoredCscBinaryProbabilities;

    /**
     * Default Constructor. This constructor does an inordinate amount of work directly in the constructor specifically
     * so we can initialize final instance variables. Making the instance variables final allows the JIT to inline them
     * everywhere we use them, improving runtime efficiency considerably.
     * 
     * Reads the grammar into memory and sorts non-terminals (V) according to their occurrence in binary rules. This can
     * allow more efficient iteration in grammar intersection (e.g., skipping NTs only valid as left children in the
     * right cell) and more efficient chart storage (e.g., omitting storage for POS NTs in chart rows >= 2).
     */
    public RealInsideOutsideCscSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends PackingFunction> functionClass) throws IOException {

        final List<StringProduction> pcfgRules = new LinkedList<StringProduction>();
        final List<StringProduction> lexicalRules = new LinkedList<StringProduction>();

        BaseLogger.singleton().finer("INFO: Reading grammar ... ");
        this.tmpStringPool = new StringPool();
        this.grammarFormat = readPcfgAndLexicon(grammarFile, pcfgRules, lexicalRules);

        this.nullWord = lexSet.addSymbol(nullSymbolStr);

        final HashSet<String> nonTerminals = new HashSet<String>();
        final HashSet<String> pos = new HashSet<String>();

        // Process the lexical productions first. Label any non-terminals found in the lexicon as POS tags. We
        // assume that pre-terminals (POS) will only occur as parents in span-1 rows and as children in span-2
        // rows
        for (final StringProduction lexicalRule : lexicalRules) {
            nonTerminals.add(lexicalRule.parent);
            pos.add(lexicalRule.parent);
            lexSet.addSymbol(lexicalRule.leftChild);
        }
        this.numLexProds = lexicalRules.size();

        // All non-terminals
        final HashSet<String> nonPosSet = new HashSet<String>();
        final HashSet<String> rightChildrenSet = new HashSet<String>();
        final HashSet<String> leftChildrenSet = new HashSet<String>();

        // Iterate through grammar rules, populating temporary non-terminal sets
        for (final StringProduction grammarRule : pcfgRules) {

            nonTerminals.add(grammarRule.parent);
            nonTerminals.add(grammarRule.leftChild);
            nonPosSet.add(grammarRule.leftChild);

            if (grammarRule instanceof BinaryStringProduction) {
                final BinaryStringProduction bsr = (BinaryStringProduction) grammarRule;

                nonTerminals.add(bsr.rightChild);

                nonPosSet.add(bsr.rightChild);
                leftChildrenSet.add(bsr.leftChild);
                rightChildrenSet.add(bsr.rightChild);
            }
        }

        assert leftChildrenSet.size() > 0 && rightChildrenSet.size() > 0;
        this.binarization = leftChildrenSet.size() > rightChildrenSet.size() ? Binarization.LEFT : Binarization.RIGHT;

        // Special cases for the start symbol and the null symbol (used for start/end of sentence markers and
        // dummy non-terminals). Label null symbol as a POS, and start symbol as not.
        nonTerminals.add(nullSymbolStr);
        pos.add(nullSymbolStr);

        nonTerminals.add(startSymbolStr);
        nonPosSet.add(startSymbolStr);

        // Make the POS set disjoint from the other sets.
        // TODO: NB: some treebank entries are mislabeled w/ POS tags in the tree an non-terms as POS tags
        // This messes things up if we enforce disjoint sets.
        rightChildrenSet.removeAll(pos);
        leftChildrenSet.removeAll(pos);
        nonPosSet.removeAll(pos);

        // Add the NTs to `nonTermSet' in sorted order
        this.nonTermSet = new Vocabulary(grammarFormat);
        this.startSymbol = (short) nonTermSet.addSymbol(startSymbolStr);
        nonTermSet.setStartSymbol(startSymbol);

        // Note: sorting with the PosFirstComparator might speed up FOM initialization a bit, but it breaks OpenCL
        // parsers, so we default to POS-embedded. For constrained parsing, use the Lexicographic comparator.
        final String comparatorClass = "edu.ohsu.cslu.grammar.SparseMatrixGrammar$"
                + GlobalConfigProperties.singleton().getProperty(ParserDriver.OPT_NT_COMPARATOR_CLASS,
                        "PosEmbeddedComparator");
        StringNonTerminalComparator comparator;
        try {
            comparator = (StringNonTerminalComparator) Class.forName(comparatorClass).getConstructor(new Class[0])
                    .newInstance(new Object[0]);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot instantiate non-terminal comparator " + comparatorClass + " : "
                    + e.getMessage());
        }
        final TreeSet<StringNonTerminal> sortedNonTerminals = new TreeSet<StringNonTerminal>(comparator);
        for (final String nt : nonTerminals) {
            sortedNonTerminals.add(create(nt, pos, nonPosSet, rightChildrenSet));
        }
        for (final StringNonTerminal nt : sortedNonTerminals) {
            nonTermSet.addSymbol(nt.label);
        }

        this.nullSymbol = (short) nonTermSet.addSymbol(nullSymbolStr);

        // And unary and binary rules
        tmpBinaryProductions = new ArrayList<Production>();
        final ArrayList<Production> unaryProductions = new ArrayList<Production>();

        for (final StringProduction grammarRule : pcfgRules) {
            if (grammarRule instanceof BinaryStringProduction) {
                tmpBinaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild,
                        ((BinaryStringProduction) grammarRule).rightChild, grammarRule.probability, this));
            } else {
                unaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild, grammarRule.probability,
                        false, nonTermSet, lexSet));
            }
        }

        this.lexicalParents = new short[lexSet.size()][];
        this.lexicalProbabilities = new double[lexSet.size()][];
        initLexicalProbabilitiesFromStringProductions(lexicalRules);

        tmpStringPool = null; // We no longer need the String intern map, so let it be GC'd

        // Initialize indices
        final short[] startAndEndIndices = startAndEndIndices(tmpBinaryProductions, unaryProductions);
        this.leftChildrenStart = startAndEndIndices[0];
        this.leftChildrenEnd = startAndEndIndices[1];
        this.rightChildrenStart = startAndEndIndices[2];
        this.rightChildrenEnd = startAndEndIndices[3];
        this.posStart = startAndEndIndices[4];
        this.posEnd = startAndEndIndices[5];

        this.numPosSymbols = posEnd - posStart + 1;

        // Create POS-only and phrase-level-only arrays so we can store features more compactly
        initPosAndPhraseSets(pos, nonPosSet);

        this.packingFunction = createPackingFunction(functionClass, tmpBinaryProductions);

        minRightSiblingIndices = new short[numNonTerms()];
        maxRightSiblingIndices = new short[numNonTerms()];
        storeRightSiblingIndices(tmpBinaryProductions);

        // And all unary productions
        cscUnaryColumnOffsets = new int[numNonTerms() + 1];
        cscUnaryRowIndices = new short[unaryProductions.size()];
        cscUnaryProbabilities = new double[unaryProductions.size()];
        cscMaxUnaryProbabilities = new double[numNonTerms()];
        storeUnaryRulesAsCscMatrix(unaryProductions, cscUnaryColumnOffsets, cscUnaryRowIndices, cscUnaryProbabilities,
                cscMaxUnaryProbabilities);

        // All binary productions
        final int[] populatedBinaryColumnIndices = populatedBinaryColumnIndices(tmpBinaryProductions, packingFunction);
        cscBinaryPopulatedColumns = new int[populatedBinaryColumnIndices.length];
        cscBinaryPopulatedColumnOffsets = new int[cscBinaryPopulatedColumns.length + 1];
        cscBinaryRowIndices = new short[tmpBinaryProductions.size()];
        cscBinaryProbabilities = new double[tmpBinaryProductions.size()];
        cscBinaryColumnOffsets = new int[packingFunction.packedArraySize()];
        factoredCscBinaryColumnOffsets = new int[packingFunction.packedArraySize()];

        storeRulesAsMatrix(tmpBinaryProductions, packingFunction, populatedBinaryColumnIndices,
                cscBinaryPopulatedColumns, cscBinaryPopulatedColumnOffsets, cscBinaryColumnOffsets,
                cscBinaryRowIndices, cscBinaryProbabilities);

        // TODO De-duplicate; move into storeRulesAsMatrix?
        for (int i = 0; i < cscBinaryPopulatedColumns.length; i++) {
            cscBinaryColumnOffsets[cscBinaryPopulatedColumns[i]] = cscBinaryPopulatedColumnOffsets[i];
        }
        for (int i = cscBinaryColumnOffsets.length - 1, lastOffset = cscBinaryPopulatedColumnOffsets[cscBinaryPopulatedColumnOffsets.length - 1]; i > cscBinaryPopulatedColumns[0]; i--) {
            if (cscBinaryColumnOffsets[i] == 0) {
                cscBinaryColumnOffsets[i] = lastOffset;
            } else {
                lastOffset = cscBinaryColumnOffsets[i];
            }
        }

        // Factored productions only
        final Collection<Production> factoredBinaryProductions = getFactoredBinaryProductions();
        final int[] factoredPopulatedBinaryColumnIndices = populatedBinaryColumnIndices(factoredBinaryProductions,
                packingFunction);
        factoredCscBinaryPopulatedColumns = new int[factoredPopulatedBinaryColumnIndices.length];
        factoredCscBinaryPopulatedColumnOffsets = new int[factoredCscBinaryPopulatedColumns.length + 1];
        factoredCscBinaryRowIndices = new short[factoredBinaryProductions.size()];
        factoredCscBinaryProbabilities = new double[factoredBinaryProductions.size()];

        storeRulesAsMatrix(factoredBinaryProductions, packingFunction, factoredPopulatedBinaryColumnIndices,
                factoredCscBinaryPopulatedColumns, factoredCscBinaryPopulatedColumnOffsets,
                factoredCscBinaryColumnOffsets, factoredCscBinaryRowIndices, factoredCscBinaryProbabilities);

        // Allow temporary binary production array to be GC'd
        tmpBinaryProductions = null;
    }

    protected int[] populatedBinaryColumnIndices(final Collection<Production> productions, final PackingFunction pf) {
        final IntSet populatedBinaryColumnIndices = new IntOpenHashSet(productions.size() / 10);
        for (final Production p : productions) {
            populatedBinaryColumnIndices.add(pf.pack((short) p.leftChild, (short) p.rightChild));
        }
        final int[] sortedPopulatedBinaryColumnIndices = populatedBinaryColumnIndices.toIntArray();
        Arrays.sort(sortedPopulatedBinaryColumnIndices);
        return sortedPopulatedBinaryColumnIndices;
    }

    /**
     * Stores binary rules in Compressed-Sparse-Column (CSC) matrix format.
     * 
     * @param productions
     * @param pf TODO
     * @param validPackedChildPairs Sorted array of valid child pairs
     * @param cscPopulatedColumns
     * @param cscPopulatedColumnOffsets
     * @param cscColumnOffsets
     * @param cscRowIndices
     * @param cscProbabilities
     */
    protected void storeRulesAsMatrix(final Collection<Production> productions, final PackingFunction pf,
            final int[] validPackedChildPairs, final int[] cscPopulatedColumns, final int[] cscPopulatedColumnOffsets,
            final int[] cscColumnOffsets, final short[] cscRowIndices, final double[] cscProbabilities) {

        if (productions.size() == 0) {
            return;
        }

        // Bin all rules by child pair, mapping parent -> probability
        final Int2ObjectOpenHashMap<Int2FloatOpenHashMap> maps = new Int2ObjectOpenHashMap<Int2FloatOpenHashMap>(1000);
        final IntSet populatedColumnSet = new IntOpenHashSet(productions.size() / 8);

        for (final Production p : productions) {
            final int childPair = pf.pack((short) p.leftChild, (short) p.rightChild);
            populatedColumnSet.add(childPair);
            Int2FloatOpenHashMap map1 = maps.get(childPair);
            if (map1 == null) {
                map1 = new Int2FloatOpenHashMap(20);
                maps.put(childPair, map1);
            }
            map1.put(p.parent, p.prob);
        }

        // Store rules in CSC matrix
        final int[] populatedColumns = populatedColumnSet.toIntArray();
        Arrays.sort(populatedColumns);
        int j = 0;
        for (int i = 0; i < populatedColumns.length; i++) {
            final int childPair = populatedColumns[i];

            cscPopulatedColumns[i] = childPair;
            cscPopulatedColumnOffsets[i] = j;
            cscColumnOffsets[childPair] = j;

            final Int2FloatOpenHashMap map = maps.get(childPair);
            final int[] parents = map.keySet().toIntArray();
            Arrays.sort(parents);

            for (int k = 0; k < parents.length; k++) {
                cscRowIndices[j] = (short) parents[k];
                cscProbabilities[j++] = map.get(parents[k]);
            }
        }
        cscPopulatedColumnOffsets[cscPopulatedColumnOffsets.length - 1] = j;

        for (int i = cscColumnOffsets.length - 1, lastOffset = j; i > populatedColumns[0]; i--) {
            if (cscColumnOffsets[i] == 0) {
                cscColumnOffsets[i] = lastOffset;
            } else {
                lastOffset = cscColumnOffsets[i];
            }
        }

    }

    public final float binaryLogProbability(final int parent, final int childPair) {

        // Find the column (child pair)
        final int c = cscBinaryColumnOffsets[childPair];
        if (c < 0) {
            return Float.NEGATIVE_INFINITY;
        }

        for (int i = cscBinaryPopulatedColumnOffsets[c]; i < cscBinaryPopulatedColumnOffsets[c + 1]; i++) {
            final int row = cscBinaryRowIndices[i];
            if (row == parent) {
                return (float) java.lang.Math.log(cscBinaryProbabilities[i]);
            }
            if (row > parent) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public int numBinaryProds() {
        return cscBinaryProbabilities.length;
    }

    public ArrayList<Production> getBinaryProductions() {
        final ArrayList<Production> binaryProductions = new ArrayList<Production>(cscUnaryProbabilities.length);

        for (int childPair = 0; childPair < cscBinaryColumnOffsets.length - 1; childPair++) {
            final short leftChild = (short) packingFunction.unpackLeftChild(childPair);
            final short rightChild = packingFunction.unpackRightChild(childPair);

            for (int i = cscBinaryColumnOffsets[childPair]; i < cscBinaryColumnOffsets[childPair + 1]; i++) {
                binaryProductions.add(new Production(cscBinaryRowIndices[i], leftChild, rightChild,
                        (float) java.lang.Math.log(cscBinaryProbabilities[i]), this));
            }
        }
        return binaryProductions;
    }

    // Read in the grammar file.
    private GrammarFormatType readPcfgAndLexicon(final Reader grammarFile, final List<StringProduction> pcfgRules,
            final List<StringProduction> lexicalRules) throws IOException {

        GrammarFormatType gf;
        final BufferedReader br = new BufferedReader(grammarFile);
        br.mark(50);

        // Read the first line and try to guess the grammar format
        final String firstLine = br.readLine();
        if (firstLine.contains("format=Berkeley")) {
            gf = GrammarFormatType.Berkeley;
            final HashMap<String, String> keyVals = Util.readKeyValuePairs(firstLine.trim());
            startSymbolStr = keyVals.get("start");
        } else if (firstLine.matches("^[A-Z]+_[0-9]+")) {
            gf = GrammarFormatType.Berkeley;
            startSymbolStr = firstLine;
        } else if (firstLine.contains("format=CSLU") || firstLine.contains("format=BUBS")) {
            gf = GrammarFormatType.CSLU;
            // final Pattern p = Pattern.compile("^.*start=([^ ]+).*$");
            // startSymbolStr = p.matcher(firstLine).group(1);
            final HashMap<String, String> keyVals = Util.readKeyValuePairs(firstLine.trim());
            startSymbolStr = keyVals.get("start");
            try {
                this.horizontalMarkov = Integer.parseInt(keyVals.get("hMarkov"));
                this.verticalMarkov = Integer.parseInt(keyVals.get("vMarkov"));
                this.language = keyVals.get("language");
                this.binarization = Binarization.valueOf(keyVals.get("binarization"));
            } catch (final Exception e) {
                // If grammar doesn't contain these values, just ignore it.
            }
        } else if (firstLine.split(" ").length > 1) {
            // The first line was not a start symbol.
            // Roark-format assumes 'TOP'. Reset the reader and re-process that line
            gf = GrammarFormatType.Roark;
            startSymbolStr = "TOP";
            br.reset();
        } else {
            throw new IllegalArgumentException("Unexpected first line of grammar file: " + firstLine);
        }

        for (String line = br.readLine(); !line.equals(LEXICON_DELIMITER); line = br.readLine()) {
            final String[] tokens = Strings.splitOnSpace(line);

            if ((tokens.length > 0 && tokens[0].equals("#")) || line.trim().equals("")) {
                // '#' indicates a comment. Skip line.
            } else if (tokens.length == 4) {
                // Unary production: expecting: A -> B prob
                // TODO: Should we make sure there aren't any duplicates?
                pcfgRules.add(new StringProduction(tokens[0], tokens[2], Float.parseFloat(tokens[3])));
            } else if (tokens.length == 5) {
                // Binary production: expecting: A -> B C prob
                pcfgRules.add(new BinaryStringProduction(tmpStringPool.intern(tokens[0]), tmpStringPool
                        .intern(tokens[2]), tmpStringPool.intern(tokens[3]), Float.parseFloat(tokens[4])));
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar PCFG\n\t" + line);
            }
        }

        // Read Lexicon after finding DELIMITER
        for (String line = br.readLine(); line != null || lexicalRules.size() == 0; line = br.readLine()) {
            if (line != null) {
                final String[] tokens = Strings.splitOnSpace(line);
                // if ((tokens.length > 0 && tokens[0].equals("#")) || line.trim().equals("")) {
                // NB: There are lexical productions that start with '#', namely '# -> #'
                if (line.trim().equals("")) {
                    // skip blank lines
                } else if (tokens.length == 4) {
                    // expecting: A -> B prob
                    lexicalRules.add(new StringProduction(tokens[0], tokens[2], Float.parseFloat(tokens[3])));
                } else {
                    throw new IllegalArgumentException("Unexpected line in grammar lexicon\n\t" + line);
                }
            }
        }

        br.close();

        return gf;
    }

    private Binarization binarization(final Collection<Production> binaryProds) {
        for (final Production p : binaryProds) {
            if (grammarFormat.isFactored(nonTermSet.getSymbol(p.leftChild))) {
                return Binarization.LEFT;
            } else if (grammarFormat.isFactored(nonTermSet.getSymbol(p.rightChild))) {
                return Binarization.RIGHT;
            }
        }
        return null;
    }

    /**
     * Populates lexicalLogProbabilities and lexicalParents
     */
    private void initLexicalProbabilitiesFromStringProductions(final Collection<StringProduction> lexicalRules) {
        @SuppressWarnings("unchecked")
        final LinkedList<StringProduction>[] lexicalProdsByChild = new LinkedList[lexSet.size()];

        for (int i = 0; i < lexicalProdsByChild.length; i++) {
            lexicalProdsByChild[i] = new LinkedList<StringProduction>();
        }

        for (final StringProduction p : lexicalRules) {
            lexicalProdsByChild[lexSet.getIndex(p.leftChild)].add(p);
        }

        for (int child = 0; child < lexicalProdsByChild.length; child++) {
            lexicalParents[child] = new short[lexicalProdsByChild[child].size()];
            lexicalProbabilities[child] = new double[lexicalProdsByChild[child].size()];
            int j = 0;
            for (final StringProduction p : lexicalProdsByChild[child]) {
                lexicalParents[child][j] = (short) nonTermSet.getIndex(p.parent);
                lexicalProbabilities[child][j++] = p.probability;
            }
            edu.ohsu.cslu.util.Arrays.sort(lexicalParents[child], lexicalProbabilities[child]);
        }
    }

    /**
     * Populates lexicalLogProbabilities and lexicalParents
     */
    private void initLexicalProbabilitiesFromProductions(final Collection<Production> lexicalRules) {
        @SuppressWarnings("unchecked")
        final LinkedList<Production>[] lexicalProdsByChild = new LinkedList[lexSet.size()];

        for (int i = 0; i < lexicalProdsByChild.length; i++) {
            lexicalProdsByChild[i] = new LinkedList<Production>();
        }

        for (final Production p : lexicalRules) {
            lexicalProdsByChild[p.leftChild].add(p);
        }

        for (int child = 0; child < lexicalProdsByChild.length; child++) {
            lexicalParents[child] = new short[lexicalProdsByChild[child].size()];
            lexicalProbabilities[child] = new double[lexicalProdsByChild[child].size()];
            int j = 0;
            for (final Production p : lexicalProdsByChild[child]) {
                lexicalParents[child][j] = (short) p.parent;
                lexicalProbabilities[child][j++] = p.prob;
            }
            edu.ohsu.cslu.util.Arrays.sort(lexicalParents[child], lexicalProbabilities[child]);
        }
    }

    private short[] startAndEndIndices(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions) {

        short tmpLeftChildrenStart = Short.MAX_VALUE, tmpLeftChildrenEnd = 0;
        short tmpRightChildrenStart = Short.MAX_VALUE, tmpRightChildrenEnd = 0;
        short tmpPosStart = Short.MAX_VALUE, tmpPosEnd = 0;

        for (final Production p : binaryProductions) {
            if (p.leftChild < tmpLeftChildrenStart) {
                tmpLeftChildrenStart = (short) p.leftChild;
            }
            if (p.leftChild > tmpLeftChildrenEnd) {
                tmpLeftChildrenEnd = (short) p.leftChild;
            }
            if (p.rightChild < tmpRightChildrenStart) {
                tmpRightChildrenStart = (short) p.rightChild;
            }
            if (p.rightChild > tmpRightChildrenEnd) {
                tmpRightChildrenEnd = (short) p.rightChild;
            }
        }

        for (final Production p : unaryProductions) {
            if (p.leftChild < tmpLeftChildrenStart) {
                tmpLeftChildrenStart = (short) p.leftChild;
            }
            if (p.leftChild > tmpLeftChildrenEnd) {
                tmpLeftChildrenEnd = (short) p.leftChild;
            }
        }

        for (int child = 0; child < lexicalParents.length; child++) {
            for (int i = 0; i < lexicalParents[child].length; i++) {
                if (lexicalParents[child][i] < tmpPosStart) {
                    tmpPosStart = lexicalParents[child][i];
                }
                if (lexicalParents[child][i] > tmpPosEnd) {
                    tmpPosEnd = lexicalParents[child][i];
                }
            }
        }

        if (tmpPosStart > this.nullSymbol) {
            tmpPosStart = nullSymbol;
        }
        if (tmpPosEnd < this.nullSymbol) {
            tmpPosEnd = nullSymbol;
        }

        return new short[] { tmpLeftChildrenStart, tmpLeftChildrenEnd, tmpRightChildrenStart, tmpRightChildrenEnd,
                tmpPosStart, tmpPosEnd };
    }

    public static Grammar read(final String grammarFile) throws IOException, ClassNotFoundException {
        final InputStream is = Util.file2inputStream(grammarFile);
        final Grammar grammar = SparseMatrixGrammar.read(is);
        is.close();
        return grammar;
    }

    public static Grammar read(final InputStream inputStream) throws IOException, ClassNotFoundException {
        // Read the grammar in either text or binary-serialized format.
        final BufferedInputStream bis = new BufferedInputStream(inputStream);
        bis.mark(2);
        final DataInputStream dis = new DataInputStream(bis);

        // Look at the first 2 bytes of the file for the signature of a serialized java object
        final int signature = dis.readShort();
        bis.reset();

        if (signature == OBJECT_SIGNATURE) {
            final ObjectInputStream ois = new ObjectInputStream(bis);
            return (Grammar) ois.readObject();
        }

        return new ListGrammar(new InputStreamReader(bis));
    }

    /**
     * @return The number of nonterminals modeled in this grammar (|V|)
     */
    @Override
    public final int numNonTerms() {
        return nonTermSet.size();
    }

    /**
     * @return The number terminals modeled in this grammar (|T|)
     */
    @Override
    public final int numLexSymbols() {
        return lexSet.size();
    }

    @Override
    public int numUnaryProds() {
        return cscUnaryProbabilities.length;
    }

    @Override
    public int numLexProds() {
        return numLexProds;
    }

    /**
     * @return The special start symbol (S-dagger).
     */
    public final String startSymbol() {
        return nonTermSet.getSymbol(startSymbol);
    }

    /**
     * Returns the non-terminal parents for the specified child. See also {@link #lexicalLogProbabilities(int)}.
     * 
     * @param child Word, as mapped in the lexicon
     * @return All non-terminal parents for the specified child
     */
    @Override
    public final short[] lexicalParents(final int child) {
        return lexicalParents[child];
    }

    /**
     * Returns the log probabilities of the non-terminal parents returned by {@Link #lexicalParents(int)} for the
     * specified child.
     * 
     * @param child Word, as mapped in the lexicon
     * @return The log probabilities of each non-terminal parent for the specified child
     */
    public final double[] lexicalProbabilities(final int child) {
        return lexicalProbabilities[child];
    }

    public final String mapNonterminal(final int nonterminal) {
        return nonTermSet.getSymbol(nonterminal);
    }

    // TODO: can probably get rid of this and just derive it where necessary
    @Override
    public final short maxPOSIndex() {
        return posEnd;
    }

    @Override
    public boolean isLeftChild(final short nonTerminal) {
        return nonTerminal >= leftChildrenStart && nonTerminal <= leftChildrenEnd;
    }

    @Override
    public boolean isRightChild(final short nonTerminal) {
        return nonTerminal >= rightChildrenStart && nonTerminal <= rightChildrenEnd;
    }

    @Override
    public final boolean isPos(final short nonTerminal) {
        return nonTerminal >= posStart && nonTerminal <= posEnd;
    }

    /**
     * @return true if this grammar is left-factored
     */
    public boolean isLeftFactored() {
        return binarization == Binarization.LEFT;
    }

    /**
     * @return true if this grammar is right-factored
     */
    public boolean isRightFactored() {
        return binarization == Binarization.RIGHT;
    }

    /**
     * @return Binarization direction
     */
    @Override
    public Binarization binarization() {
        return binarization;
    }

    public ArrayList<Production> getFactoredBinaryProductions() {
        final ArrayList<Production> factoredProductions = new ArrayList<Production>();
        for (final Production p : getBinaryProductions()) {
            if (p.isBinaryProd() && grammarFormat.isFactored(mapNonterminal(p.parent))) {
                factoredProductions.add(p);
            }
        }
        return factoredProductions;
    }

    /**
     * @return List of binary productions
     */
    public Production getBinaryProduction(final short parent, final short leftChild, final short rightChild) {
        for (final Production p : getBinaryProductions()) {
            if (p.parent == parent && p.leftChild == leftChild && p.rightChild == rightChild) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns the log probability of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return Log probability of the specified rule.
     */
    @Override
    public float binaryLogProbability(final String parent, final String leftChild, final String rightChild) {
        if (nonTermSet.containsKey(parent) && nonTermSet.containsKey(leftChild) && nonTermSet.containsKey(rightChild)) {
            return binaryLogProbability((short) nonTermSet.getIndex(parent), (short) nonTermSet.getIndex(leftChild),
                    (short) nonTermSet.getIndex(rightChild));
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * @return List of unary productions
     */
    public ArrayList<Production> getUnaryProductions() {
        final ArrayList<Production> unaryProductions = new ArrayList<Production>(cscUnaryProbabilities.length);
        for (int child = 0; child < cscUnaryColumnOffsets.length - 1; child++) {
            for (int i = cscUnaryColumnOffsets[child]; i < cscUnaryColumnOffsets[child + 1]; i++) {
                unaryProductions.add(new Production(cscUnaryRowIndices[i], child, (float) java.lang.Math
                        .log(cscUnaryProbabilities[i]), false, this));
            }
        }
        return unaryProductions;
    }

    public Production getUnaryProduction(final short parent, final short child) {
        final float prob = unaryLogProbability(parent, child);
        return prob > Float.NEGATIVE_INFINITY ? new Production(parent, child, prob, false, this) : null;
    }

    public Production getUnaryProduction(final String A, final String B) {
        if (nonTermSet.containsKey(A) && nonTermSet.containsKey(B)) {
            return getUnaryProduction((short) nonTermSet.getIndex(A), (short) nonTermSet.getIndex(B));
        }
        return null;
    }

    /**
     * Returns the log probability of a unary rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    @Override
    public float unaryLogProbability(final String parent, final String child) {
        if (nonTermSet.containsKey(parent) && nonTermSet.containsKey(child)) {
            return unaryLogProbability((short) nonTermSet.getIndex(parent), (short) nonTermSet.getIndex(child));
        }
        return Float.NEGATIVE_INFINITY;
    }

    /*
     * Lexical Productions
     */
    public ArrayList<Production> getLexicalProductions() {
        final ArrayList<Production> lexicalProductions = new ArrayList<Production>(numLexProds);
        for (int child = 0; child < lexicalParents.length; child++) {
            for (int j = 0; j < lexicalParents[child].length; j++) {
                lexicalProductions.add(new Production(lexicalParents[child][j], child, (float) java.lang.Math
                        .log(lexicalProbabilities[child][j]), true, this));
            }
        }
        return lexicalProductions;
    }

    @Override
    public Production getLexicalProduction(final short parent, final int child) {
        for (int i = 0; i < lexicalParents[child].length; i++) {
            if (lexicalParents[child][i] == parent) {
                return new Production(parent, child, (float) java.lang.Math.log(lexicalProbabilities[child][i]), true,
                        this);
            }
        }
        return null;
    }

    public Production getLexicalProduction(final String A, final String lex) {
        if (nonTermSet.containsKey(A) && lexSet.containsKey(lex)) {
            return getLexicalProduction((short) nonTermSet.getIndex(A), lexSet.getIndex(lex));
        }
        return null;
    }

    /**
     * Returns the log probability of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public double lexicalProbability(final short parent, final int child) {
        final int i = Arrays.binarySearch(lexicalParents(child), parent);
        return (i < 0) ? UNSEEN_LEX_PROB : lexicalProbabilities[child][i];
        // return lexicalLogProbabilityMaps[child].get((short) parent);
    }

    /**
     * Returns the log probability of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    @Override
    public float lexicalLogProbability(final String parent, final String child) {
        if (nonTermSet.containsKey(parent) && lexSet.containsKey(child)) {
            return lexicalLogProbability((short) nonTermSet.getIndex(parent), lexSet.getIndex(child));
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public float lexicalLogProbability(final short parent, final int child) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float[] lexicalLogProbabilities(final int wordIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns the log probability of a rule.
     * 
     * @param p Production
     * @return Log probability of the specified rule.
     */
    protected float getProductionProb(final Production p) {
        if (p != null) {
            return p.prob;
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Returns a {@link Grammar} instance (of the same class as <b>this</b>), with all non-terminal splits collapsed.
     * Note that the current {@link Grammar} subclass must implement a constructor
     * 
     * @return A {@link Grammar} instance (of the same class as <b>this</b>), with all non-terminal splits collapsed.
     */
    public Grammar toUnsplitGrammar() {
        final Vocabulary baseVocabulary = nonTermSet.baseVocabulary();
        final FractionalCountGrammar unsplitGrammar = new FractionalCountGrammar(baseVocabulary, lexSet, null, null,
                null, 0, 0);

        for (final Production p : getBinaryProductions()) {
            final short unsplitParent = nonTermSet.getBaseIndex((short) p.parent);
            final short unsplitLeftChild = nonTermSet.getBaseIndex((short) p.leftChild);
            final short unsplitRightChild = nonTermSet.getBaseIndex((short) p.rightChild);
            unsplitGrammar.incrementBinaryCount(unsplitParent, unsplitLeftChild, unsplitRightChild,
                    java.lang.Math.exp(p.prob));
        }

        for (final Production p : getUnaryProductions()) {
            final short unsplitParent = nonTermSet.getBaseIndex((short) p.parent);
            final short unsplitChild = nonTermSet.getBaseIndex((short) p.leftChild);
            unsplitGrammar.incrementUnaryCount(unsplitParent, unsplitChild, java.lang.Math.exp(p.prob));
        }

        for (final Production p : getLexicalProductions()) {
            final short unsplitParent = nonTermSet.getBaseIndex((short) p.parent);
            unsplitGrammar.incrementLexicalCount(unsplitParent, p.leftChild, java.lang.Math.exp(p.prob));
        }

        try {
            return getClass().getConstructor(
                    new Class[] { ArrayList.class, ArrayList.class, ArrayList.class, SymbolSet.class, SymbolSet.class,
                            GrammarFormatType.class, Class.class, boolean.class }).newInstance(
                    new Object[] { unsplitGrammar.binaryProductions(Float.NEGATIVE_INFINITY),
                            unsplitGrammar.unaryProductions(Float.NEGATIVE_INFINITY),
                            unsplitGrammar.lexicalProductions(Float.NEGATIVE_INFINITY), baseVocabulary, lexSet,
                            grammarFormat, this.packingFunction.getClass(), true });
        } catch (final Exception e) {
            try {
                return getClass().getConstructor(
                        new Class[] { ArrayList.class, ArrayList.class, ArrayList.class, SymbolSet.class,
                                SymbolSet.class, GrammarFormatType.class }).newInstance(
                        new Object[] { unsplitGrammar.binaryProductions(Float.NEGATIVE_INFINITY),
                                unsplitGrammar.unaryProductions(Float.NEGATIVE_INFINITY),
                                unsplitGrammar.lexicalProductions(Float.NEGATIVE_INFINITY), baseVocabulary, lexSet,
                                grammarFormat });

            } catch (final Exception e2) {
                throw new UnsupportedOperationException(getClass() + " does not support this operation");
            }
        }
    }

    @Override
    public String getStats() {
        int nFactored = 0, nUnFactored = 0;
        for (final String nt : nonTermSet) {
            if (grammarFormat.isFactored(nt)) {
                nFactored++;
            } else {
                nUnFactored++;
            }
        }

        final StringBuilder sb = new StringBuilder(256);
        sb.append("INFO:");
        sb.append(" binaryRules=" + numBinaryProds());
        sb.append(" unaryRules=" + numUnaryProds());
        sb.append(" lexicalRules=" + numLexProds());

        sb.append(" nonTerminals=" + numNonTerms());
        sb.append(" lexicalSymbols=" + lexSet.size());
        sb.append(" posSymbols=" + numPosSymbols);
        sb.append(" maxPosIndex=" + posEnd);
        sb.append(" factoredNTs=" + nFactored);
        sb.append(" unfactoredNTs=" + nUnFactored);

        sb.append(" startSymbol=" + nonTermSet.getSymbol(startSymbol));
        sb.append(" nullSymbol=" + nonTermSet.getSymbol(nullSymbol));
        sb.append(" binarization=" + (isLeftFactored() ? "left" : "right"));
        sb.append(" grammarFormat=" + grammarFormat);
        sb.append(" packingFunction=" + packingFunction.getClass().getName());
        sb.append(" packedArraySize=" + packingFunction.packedArraySize());

        return sb.toString();
    }

    public String getStatsVerbose() {

        int nFactored = 0, nUnFactored = 0;
        for (final String nt : nonTermSet) {
            if (grammarFormat.isFactored(nt)) {
                nFactored++;
            } else {
                nUnFactored++;
            }
        }

        final StringBuilder sb = new StringBuilder(256);
        sb.append("Binary rules: " + numBinaryProds() + '\n');
        sb.append("Unary rules: " + numUnaryProds() + '\n');
        sb.append("Lexical rules: " + numLexProds() + '\n');

        sb.append("Non Terminals: " + numNonTerms() + '\n');
        sb.append("Lexical symbols: " + lexSet.size() + '\n');
        sb.append("POS symbols: " + numPosSymbols + '\n');
        sb.append("Max POS index: " + posEnd + '\n');
        sb.append("Factored NTs: " + nFactored + '\n');
        sb.append("UnFactored NTs: " + nUnFactored + '\n');

        sb.append("Start symbol: " + nonTermSet.getSymbol(startSymbol) + '\n');
        sb.append("Null symbol: " + nonTermSet.getSymbol(nullSymbol) + '\n');
        sb.append("Binarization: " + (isLeftFactored() ? "left" : "right") + '\n');
        sb.append("GrammarFormat: " + grammarFormat + '\n');

        return sb.toString();
    }

    private StringNonTerminal create(final String label, final HashSet<String> pos, final Set<String> nonPosSet,
            final Set<String> rightChildren) {
        final String internLabel = tmpStringPool.intern(label);

        if (startSymbolStr.equals(internLabel)) {
            return new StringNonTerminal(internLabel, NonTerminalClass.EITHER_CHILD);

        } else if (pos.contains(internLabel)) {
            return new StringNonTerminal(internLabel, NonTerminalClass.POS);

        } else if (nonPosSet.contains(internLabel) && !rightChildren.contains(internLabel)) {
            return new StringNonTerminal(internLabel, NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY);
        }

        return new StringNonTerminal(internLabel, NonTerminalClass.EITHER_CHILD);
    }

    private final class StringNonTerminal {

        public final String label;
        public final NonTerminalClass ntClass;

        protected StringNonTerminal(final String label, final NonTerminalClass ntClass) {
            this.label = label;
            this.ntClass = ntClass;
        }

        @Override
        public String toString() {
            return label + " " + ntClass.toString();
        }
    }

    protected abstract static class StringNonTerminalComparator implements Comparator<StringNonTerminal> {

        HashMap<NonTerminalClass, Integer> map = new HashMap<NonTerminalClass, Integer>();

        @Override
        public int compare(final StringNonTerminal o1, final StringNonTerminal o2) {
            final int i1 = map.get(o1.ntClass);
            final int i2 = map.get(o2.ntClass);

            if (i1 < i2) {
                return -1;
            } else if (i1 > i2) {
                return 1;
            }

            return o1.label.compareTo(o2.label);
        }
    }

    @SuppressWarnings("unused")
    private static class PosFirstComparator extends StringNonTerminalComparator {

        public PosFirstComparator() {
            map.put(NonTerminalClass.POS, 0);
            map.put(NonTerminalClass.EITHER_CHILD, 1);
            map.put(NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY, 1);
        }
    }

    @SuppressWarnings("unused")
    private static class PosEmbeddedComparator extends StringNonTerminalComparator {

        public PosEmbeddedComparator() {
            map.put(NonTerminalClass.EITHER_CHILD, 0);
            map.put(NonTerminalClass.POS, 1);
            map.put(NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY, 2);
        }
    }

    @SuppressWarnings("unused")
    private static class LexicographicComparator extends StringNonTerminalComparator {

        public LexicographicComparator() {
        }

        @Override
        public int compare(final StringNonTerminal o1, final StringNonTerminal o2) {
            return o1.label.compareTo(o2.label);
        }
    }

    /**
     * 1 - Left child only (and unary-only, although there shouldn't be many of those)
     * 
     * 2 - Either child (or right-child only, although we don't find many of those)
     * 
     * 3 - All POS (pre-terminals)
     */
    private enum NonTerminalClass {
        FACTORED_SIDE_CHILDREN_ONLY, EITHER_CHILD, POS;
    }

    public String toMappingString() {
        final StringBuilder sb = new StringBuilder(numNonTerms() * 25);
        for (int i = 0; i < numNonTerms(); i++) {
            sb.append(i + " -> " + nonTermSet.getSymbol(i) + '\n');
        }
        sb.append("===Lexicon===\n");
        for (int i = 0; i < numLexSymbols(); i++) {
            sb.append(i + " -> " + lexSet.getSymbol(i) + '\n');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(1024 * 1024);

        sb.append(nonTermSet.getSymbol(startSymbol) + '\n');

        for (final Production p : getBinaryProductions()) {
            sb.append(String.format("%s -> %s %s %.4f\n", nonTermSet.getSymbol(p.parent),
                    nonTermSet.getSymbol(p.leftChild), nonTermSet.getSymbol(p.rightChild), p.prob));
        }

        for (final Production p : getUnaryProductions()) {
            sb.append(String.format("%s -> %s %.4f\n", nonTermSet.getSymbol(p.parent),
                    nonTermSet.getSymbol(p.leftChild), p.prob));
        }
        sb.append("===Lexicon===\n");
        for (final Production p : getLexicalProductions()) {
            sb.append(String.format("%s -> %s %.4f\n", nonTermSet.getSymbol(p.parent), lexSet.getSymbol(p.leftChild),
                    p.prob));
        }
        return sb.toString();
    }

    public boolean isCoarseGrammar() {
        return false;
    }

    /**
     * Stores unary rules in Compressed-Sparse-Column (CSC) matrix format.
     * 
     * @param productions
     * @param cscColumnOffsets
     * @param cscRowIndices
     * @param cscProbabilities
     */
    protected void storeUnaryRulesAsCscMatrix(final Collection<Production> productions, final int[] cscColumnOffsets,
            final short[] cscRowIndices, final double[] cscProbabilities, final double[] maxCscProbabilities) {

        // Bin all rules by child, mapping parent -> probability
        final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> maps = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>(
                1000);

        for (final Production p : productions) {
            final short child = (short) p.leftChild;

            Short2FloatOpenHashMap map = maps.get(child);
            if (map == null) {
                map = new Short2FloatOpenHashMap(20);
                maps.put(child, map);
            }
            map.put((short) p.parent, p.prob);
        }

        Arrays.fill(maxCscProbabilities, Float.NEGATIVE_INFINITY);

        // Store rules in CSC matrix
        int j = 0;
        final short[] keys = maps.keySet().toShortArray();
        Arrays.sort(keys);
        for (short child = 0; child < numNonTerms(); child++) {

            cscColumnOffsets[child] = j;

            if (!maps.containsKey(child)) {
                continue;
            }

            final Short2FloatOpenHashMap map = maps.get(child);
            final short[] parents = map.keySet().toShortArray();
            Arrays.sort(parents);
            for (int k = 0; k < parents.length; k++) {
                cscRowIndices[j] = parents[k];
                final float prob = map.get(parents[k]);
                cscProbabilities[j++] = prob;
                if (prob > maxCscProbabilities[child]) {
                    maxCscProbabilities[child] = prob;
                }
            }
        }
        cscColumnOffsets[cscColumnOffsets.length - 1] = j;
    }

    @SuppressWarnings("unchecked")
    private PackingFunction createPackingFunction(Class<? extends PackingFunction> functionClass,
            final ArrayList<Production> binaryProductions) {
        try {
            if (functionClass == null) {
                functionClass = PerfectIntPairHashPackingFunction.class;
            }

            Constructor<PackingFunction> c;
            try {
                c = (Constructor<PackingFunction>) functionClass.getConstructor(SparseMatrixGrammar.class,
                        ArrayList.class);
            } catch (final NoSuchMethodException e) {
                try {
                    c = (Constructor<PackingFunction>) functionClass.getConstructor(getClass(), ArrayList.class);
                } catch (final NoSuchMethodException e2) {
                    return ((Constructor<PackingFunction>) functionClass.getConstructor(SparseMatrixGrammar.class))
                            .newInstance(this);
                }
            }
            return c.newInstance(this, binaryProductions);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Production> getUnaryProductionsWithChild(final int child) {
        throw new UnsupportedOperationException();
    }

    /**
     * This method creates {@link Production} instances on the fly, so it can be pretty inefficient. In general, it's
     * better to use {@link #lexicalParents(int)} and {@link #lexicalLogProbabilities(int)} instead.
     */
    @Override
    public Collection<Production> getLexicalProductionsWithChild(final int child) {
        final ArrayList<Production> list = new ArrayList<Production>(lexicalParents[child].length);
        for (int i = 0; i < lexicalParents[child].length; i++) {
            list.add(new Production(lexicalParents[child][i], child, (float) java.lang.Math
                    .log(lexicalProbabilities[child][i]), true, this));
        }
        return list;
    }

    // /**
    // * Returns the log probability of the specified parent / child production
    // *
    // * @param parent Parent index
    // * @param childPair Packed children
    // * @return Log probability
    // */
    // public abstract float binaryLogProbability(final int parent, final int childPair);

    /**
     * Returns the log probability of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return Log probability of the specified rule.
     */
    @Override
    public final float binaryLogProbability(final short parent, final short leftChild, final short rightChild) {
        return binaryLogProbability(parent, packingFunction.pack(leftChild, rightChild));
    }

    /**
     * Returns the log probability of the specified parent / child production
     * 
     * @param parent Parent index
     * @param child Child index
     * @return Log probability
     */
    @Override
    public final float unaryLogProbability(final short parent, final short child) {
        for (int i = cscUnaryColumnOffsets[child]; i <= cscUnaryColumnOffsets[child + 1]; i++) {
            final int row = cscUnaryRowIndices[i];
            if (row == parent) {
                return (float) java.lang.Math.log(cscUnaryProbabilities[i]);
            }
            if (row > parent) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    private void storeRightSiblingIndices(final ArrayList<Production> binaryProductions) {
        Arrays.fill(minRightSiblingIndices, Short.MAX_VALUE);
        Arrays.fill(maxRightSiblingIndices, Short.MIN_VALUE);

        for (final Production p : binaryProductions) {
            if (p.rightChild < minRightSiblingIndices[p.leftChild]) {
                minRightSiblingIndices[p.leftChild] = (short) p.rightChild;
            }
            if (p.rightChild > maxRightSiblingIndices[p.leftChild]) {
                maxRightSiblingIndices[p.leftChild] = (short) p.rightChild;
            }
        }
    }

    /**
     * Returns the packing function in use
     * 
     * @return the packing function in use
     */
    public final PackingFunction packingFunction() {
        return packingFunction;
    }

    public final boolean isValidRightChild(final int nonTerminal) {
        return nonTerminal >= rightChildrenStart && nonTerminal <= rightChildrenEnd && nonTerminal != nullSymbol;
    }

    public final boolean isValidLeftChild(final int nonTerminal) {
        return nonTerminal >= leftChildrenStart && nonTerminal <= leftChildrenEnd && nonTerminal != nullSymbol;
    }

    protected void storeUnaryRulesAsCsrMatrix(final int[] rowStartIndices, final short[] columnIndices,
            final float[] probabilities) {

        // Bin all rules by parent, mapping child -> probability
        final Short2FloatOpenHashMap[] maps = new Short2FloatOpenHashMap[numNonTerms()];
        for (int i = 0; i < numNonTerms(); i++) {
            maps[i] = new Short2FloatOpenHashMap(1000);
        }

        for (final Production p : getUnaryProductions()) {
            maps[p.parent].put((short) p.leftChild, p.prob);
        }

        // Store rules in CSR matrix
        int i = 0;
        for (int parent = 0; parent < numNonTerms(); parent++) {
            rowStartIndices[parent] = i;

            final short[] children = maps[parent].keySet().toShortArray();
            Arrays.sort(children);
            for (int j = 0; j < children.length; j++) {
                columnIndices[i] = children[j];
                probabilities[i++] = maps[parent].get(children[j]);
            }
        }
        rowStartIndices[rowStartIndices.length - 1] = i;
    }
}
