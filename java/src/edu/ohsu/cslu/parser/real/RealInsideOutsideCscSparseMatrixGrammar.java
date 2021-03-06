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

package edu.ohsu.cslu.parser.real;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
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
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.ListGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.NonTerminalClass;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.StringNonTerminal;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.StringNonTerminalComparator;
import edu.ohsu.cslu.grammar.StringProduction;
import edu.ohsu.cslu.grammar.TokenClassifier;
import edu.ohsu.cslu.grammar.Vocabulary;
import edu.ohsu.cslu.lela.FractionalCountGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.util.Math;
import edu.ohsu.cslu.util.MutableEnumeration;
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

    /**
     * Parallel array of lexical rules (with {@link #lexicalProbabilities}. Real-valued inference uses the real-valued
     * probabilities, but some pruning models require log probabilities as well.
     */
    protected final float[][] lexicalLogProbabilities;

    public final static String nullSymbolStr = "<null>";
    public static Production nullProduction;

    public static float UNSEEN_LEX_LOG_PROB = -9999;
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

    // The primary unary rule storage is CSC, but during unary outside computations, it's more efficient and cleaner to
    // iterate by parent, so we duplicate unary productions in a CSR representation.
    /**
     * Offsets into {@link #csrUnaryColumnIndices} for the start of each row, indexed by row index (non-terminals)
     */
    public final int[] csrUnaryRowStartIndices;

    /**
     * Column indices of each matrix entry in {@link #csrUnaryProbabilities}. One entry for each unary rule; the same
     * size as {@link #csrUnaryProbabilities}.
     */
    public final short[] csrUnaryColumnIndices;

    /** Unary rule probabilities */
    public final double[] csrUnaryProbabilities;

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
     * A copy of the CSC grammar, mapped by right child -> parent,left child. Note that this confuses the arguments to
     * pack/unpack method of the {@link PackingFunction}, shifting the positions of parent, left child, and right child.
     * So {@link PackingFunction#pack(short, short)} should be called with (parent, left child).
     */
    public final int[] rightChildCscBinaryPopulatedColumns;
    public final int[] rightChildCscBinaryPopulatedColumnOffsets;
    public final int[] rightChildCscBinaryColumnOffsets;
    public final short[] rightChildCscBinaryRowIndices;
    public final double[] rightChildCscBinaryProbabilities;
    public final PackingFunction rightChildPackingFunction;

    /**
     * Default Constructor. This constructor does an inordinate amount of work directly in the constructor specifically
     * so we can initialize final instance variables. Making the instance variables final allows the JIT to inline them
     * everywhere we use them, improving runtime efficiency considerably.
     * 
     * Reads the grammar into memory and sorts non-terminals (V) according to their occurrence in binary rules. This can
     * allow more efficient iteration in grammar intersection (e.g., skipping NTs only valid as left children in the
     * right cell) and more efficient chart storage (e.g., omitting storage for POS NTs in chart rows >= 2).
     * 
     * @param grammarFile
     * @param tokenClassifier
     */
    public RealInsideOutsideCscSparseMatrixGrammar(final Reader grammarFile, final TokenClassifier tokenClassifier)
            throws IOException {

        final List<StringProduction> pcfgRules = new LinkedList<StringProduction>();
        final List<StringProduction> lexicalRules = new LinkedList<StringProduction>();

        this.tokenClassifier = tokenClassifier;
        BaseLogger.singleton().finer("INFO: Reading grammar ... ");
        this.tmpStringPool = new StringPool();
        this.grammarFormat = readPcfgAndLexicon(grammarFile, pcfgRules, lexicalRules);

        this.nullToken = lexSet.addSymbol(nullSymbolStr);

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
            final Constructor<?> constructor = Class.forName(comparatorClass).getConstructor(new Class[0]);
            constructor.setAccessible(true);
            comparator = (StringNonTerminalComparator) constructor.newInstance(new Object[0]);
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
        this.lexicalLogProbabilities = new float[lexSet.size()][];
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

        this.packingFunction = new PerfectIntPairHashPackingFunction(tmpBinaryProductions);

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

        // Store all unary rules
        this.csrUnaryRowStartIndices = new int[numNonTerms() + 1];
        this.csrUnaryColumnIndices = new short[numUnaryProds()];
        this.csrUnaryProbabilities = new double[numUnaryProds()];

        storeUnaryRulesAsCsrMatrix(csrUnaryRowStartIndices, csrUnaryColumnIndices, csrUnaryProbabilities);

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

        // Right child grammar
        final ArrayList<Production> binaryProductionsByRightChild = binaryProductionsByRightChild(tmpBinaryProductions);
        this.rightChildPackingFunction = new PerfectIntPairHashPackingFunction(binaryProductionsByRightChild);

        final int[] rightChildPopulatedBinaryColumnIndices = populatedBinaryColumnIndices(
                binaryProductionsByRightChild, rightChildPackingFunction);
        this.rightChildCscBinaryPopulatedColumns = new int[rightChildPopulatedBinaryColumnIndices.length];
        this.rightChildCscBinaryPopulatedColumnOffsets = new int[rightChildCscBinaryPopulatedColumns.length + 1];
        this.rightChildCscBinaryRowIndices = new short[numBinaryProds()];
        this.rightChildCscBinaryProbabilities = new double[numBinaryProds()];
        this.rightChildCscBinaryColumnOffsets = new int[rightChildPackingFunction.packedArraySize() + 1];

        storeRulesAsMatrix(binaryProductionsByRightChild, rightChildPackingFunction,
                rightChildPopulatedBinaryColumnIndices, rightChildCscBinaryPopulatedColumns,
                rightChildCscBinaryPopulatedColumnOffsets, rightChildCscBinaryColumnOffsets,
                rightChildCscBinaryRowIndices, rightChildCscBinaryProbabilities);

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
     * @param pf Packing function
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
                cscProbabilities[j++] = java.lang.Math.exp(map.get(parents[k]));
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

    private ArrayList<Production> binaryProductionsByRightChild(final ArrayList<Production> binaryProductions) {
        final ArrayList<Production> productionsByRightChild = new ArrayList<Production>(binaryProductions.size());
        for (final Production p : binaryProductions) {
            productionsByRightChild
                    .add(new Production(p.rightChild, p.parent, p.leftChild, p.prob, nonTermSet, lexSet));
        }
        return productionsByRightChild;
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

    /**
     * Populates {@link RealInsideOutsideCscSparseMatrixGrammar#lexicalProbabilities},
     * {@link RealInsideOutsideCscSparseMatrixGrammar#lexicalLogProbabilities}, and
     * {@link RealInsideOutsideCscSparseMatrixGrammar#lexicalParents}.
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
            lexicalLogProbabilities[child] = new float[lexicalProdsByChild[child].size()];
            int j = 0;
            for (final StringProduction p : lexicalProdsByChild[child]) {
                lexicalParents[child][j] = (short) nonTermSet.getIndex(p.parent);
                lexicalLogProbabilities[child][j] = p.probability;
                lexicalProbabilities[child][j++] = java.lang.Math.exp(p.probability);
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
        final Grammar grammar = SparseMatrixGrammar.read(is, new DecisionTreeTokenClassifier());
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

        return new ListGrammar(new InputStreamReader(bis), new DecisionTreeTokenClassifier());
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
     * Returns the probability of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public double lexicalProbability(final short parent, final int child) {
        final int i = Arrays.binarySearch(lexicalParents(child), parent);
        return (i < 0) ? UNSEEN_LEX_PROB : lexicalProbabilities[child][i];
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
        final int i = Arrays.binarySearch(lexicalParents(child), parent);
        return (i < 0) ? UNSEEN_LEX_LOG_PROB : lexicalLogProbabilities[child][i];
    }

    @Override
    public float[] lexicalLogProbabilities(final int child) {
        return lexicalLogProbabilities[child];
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
                    new Class[] { ArrayList.class, ArrayList.class, ArrayList.class, MutableEnumeration.class, MutableEnumeration.class,
                            GrammarFormatType.class, Class.class, boolean.class }).newInstance(
                    new Object[] { unsplitGrammar.binaryProductions(Float.NEGATIVE_INFINITY),
                            unsplitGrammar.unaryProductions(Float.NEGATIVE_INFINITY),
                            unsplitGrammar.lexicalProductions(Float.NEGATIVE_INFINITY), baseVocabulary, lexSet,
                            grammarFormat, this.packingFunction.getClass(), true });
        } catch (final Exception e) {
            try {
                return getClass().getConstructor(
                        new Class[] { ArrayList.class, ArrayList.class, ArrayList.class, MutableEnumeration.class,
                                MutableEnumeration.class, GrammarFormatType.class }).newInstance(
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

        Arrays.fill(maxCscProbabilities, 0);

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
                final double prob = java.lang.Math.exp(map.get(parents[k]));
                cscProbabilities[j++] = prob;
                if (prob > maxCscProbabilities[child]) {
                    maxCscProbabilities[child] = prob;
                }
            }
        }
        cscColumnOffsets[cscColumnOffsets.length - 1] = j;
    }

    protected void storeUnaryRulesAsCsrMatrix(final int[] rowStartIndices, final short[] columnIndices,
            final double[] probabilities) {

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
                probabilities[i++] = java.lang.Math.exp(maps[parent].get(children[j]));
            }
        }
        rowStartIndices[rowStartIndices.length - 1] = i;
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

    public abstract class PackingFunction implements Serializable {

        private static final long serialVersionUID = 1L;

        // Shift lengths and masks for packing and unpacking non-terminals into an int
        public final int shift;
        protected final int lowOrderMask;
        private final int packedArraySize;
        public final int maxPackedLexicalProduction = -numNonTerms() - 1;

        protected PackingFunction(final int maxUnshiftedNonTerminal) {
            shift = Math.logBase2(Math.nextPowerOf2(maxUnshiftedNonTerminal + 1));
            int m = 0;
            for (int i = 0; i < shift; i++) {
                m = m << 1 | 1;
            }
            lowOrderMask = m;

            packedArraySize = numNonTerms() << shift | lowOrderMask;
        }

        /**
         * Returns the array size required to store all possible child combinations.
         * 
         * @return the array size required to store all possible child combinations.
         */
        public int packedArraySize() {
            return packedArraySize;
        }

        /**
         * Returns a single int representing a child pair, the <i>column<i> of a V x V^2 grammar matrix.
         * 
         * @param leftChild
         * @param rightChild
         * @return packed representation of the specified child pair or {@link Integer#MIN_VALUE} if the pair is not
         *         permitted by the grammar.
         */
        public int pack(final short leftChild, final short rightChild) {
            return leftChild << shift | (rightChild & lowOrderMask);
        }

        /**
         * Returns a single int representing a unary production.
         * 
         * @param child
         * @return packed representation of the specified production.
         */
        public final int packUnary(final short child) {
            return -child - 1;
        }

        /**
         * Returns a single int representing a lexical production.
         * 
         * @param child
         * @return packed representation of the specified production.
         */
        public final int packLexical(final int child) {
            return maxPackedLexicalProduction - child;
        }

        /**
         * Returns the left child encoded into a packed child pair
         * 
         * @param childPair
         * @return the left child encoded into a packed child pair
         */
        public abstract int unpackLeftChild(final int childPair);

        /**
         * Returns the right child encoded into a packed child pair
         * 
         * @param childPair
         * @return the right child encoded into a packed child pair
         */
        public abstract short unpackRightChild(final int childPair);

        public final String openClPackDefine() {
            return "#define PACK ((leftNonTerminal  << " + shift + ") | (rightNonTerminal & " + lowOrderMask + "))\n"
                    + "#define PACK_UNARY -winningChild - 1\n";
        }

        public final String openClUnpackLeftChild() {
            final StringBuilder sb = new StringBuilder();
            sb.append("int unpackLeftChild(const int childPair) {\n");
            sb.append("    if (childPair < 0) {\n");
            sb.append("        // Unary or lexical production\n");
            sb.append("        if (childPair <= MAX_PACKED_LEXICAL_PRODUCTION) {\n");
            sb.append("            // Lexical production\n");
            sb.append("            return -childPair + MAX_PACKED_LEXICAL_PRODUCTION;\n");
            sb.append("        }\n");
            sb.append("        // Unary production\n");
            sb.append("        return -childPair - 1;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Left child of binary production\n");
            sb.append("    return childPair >> PACKING_SHIFT;\n");
            sb.append("}\n");
            return sb.toString();
        }
    }

    public final class PerfectIntPairHashPackingFunction extends PackingFunction {

        private static final long serialVersionUID = 1L;

        private final int maxLexicalProduction = -numNonTerms() - 1;

        private final int packedArraySize;

        /** Parallel array, indexed by k1 */
        private final int[] maxKey2;
        private final int[] k2Shifts;
        private final int[] k2Masks;

        /** Offsets of each perfect hash `segment' within {@link #hashtable}, indexed by k1. */
        private final int[] hashtableOffsets;
        private final short[] hashtable;

        /** Offsets of each perfect hash `segment' within {@link #displacementTableOffsets}, indexed by k1. */
        private final int[] displacementTable;
        private final int[] displacementTableOffsets;

        private final int size;

        public PerfectIntPairHashPackingFunction(final ArrayList<Production> binaryProductions) {
            this(binaryProductions, rightChildrenEnd);
        }

        public PerfectIntPairHashPackingFunction(final ArrayList<Production> binaryProductions,
                final int maxUnshiftedNonTerminal) {
            super(maxUnshiftedNonTerminal);

            final int[][] childPairs = new int[2][binaryProductions.size()];
            int k = 0;
            for (final Production p : binaryProductions) {
                childPairs[0][k] = p.leftChild;
                childPairs[1][k++] = p.rightChild;
            }

            // System.out.println("Hashed grammar: " + perfectHash.toString());

            final int parallelArraySize = numNonTerms() + 1; // Math.max(childPairs[0]) + 1;

            // Find unique k2 values for each k1
            final IntOpenHashSet[] k2Sets = new IntOpenHashSet[parallelArraySize];
            for (int i = 0; i < k2Sets.length; i++) {
                k2Sets[i] = new IntOpenHashSet();
            }
            for (int i = 0; i < childPairs[0].length; i++) {
                k2Sets[childPairs[0][i]].add(childPairs[1][i]);
            }

            // Calculate total key pair count
            int tmpSize = 0;
            for (int i = 0; i < k2Sets.length; i++) {
                tmpSize += k2Sets[i].size();
            }
            this.size = tmpSize;

            this.maxKey2 = new int[parallelArraySize];

            // Indexed by k1
            final int[][] k2s = new int[parallelArraySize][];
            for (int i = 0; i < k2s.length; i++) {
                k2s[i] = k2Sets[i].toIntArray();
                maxKey2[i] = Math.max(k2s[i]);
            }

            this.k2Shifts = new int[parallelArraySize];
            this.k2Masks = new int[parallelArraySize];
            this.hashtableOffsets = new int[parallelArraySize + 1];
            this.displacementTableOffsets = new int[parallelArraySize + 1];

            final int tmpArraySize = parallelArraySize * Math.max(childPairs[1]) + 1;
            final short[] tmpHashtable = new short[tmpArraySize];
            final int[] tmpDisplacementTable = new int[tmpArraySize];

            for (int k1 = 0; k1 < k2s.length; k1++) {

                final HashtableSegment hs = createPerfectHash(k2s[k1]);

                this.k2Masks[k1] = hs.hashMask;
                this.k2Shifts[k1] = hs.hashShift;

                // Record the offsets
                hashtableOffsets[k1 + 1] = hashtableOffsets[k1] + hs.hashtableSegment.length;
                displacementTableOffsets[k1 + 1] = displacementTableOffsets[k1] + hs.displacementTableSegment.length;

                // Copy the segment into the temporary hash and displacement arrays
                System.arraycopy(hs.hashtableSegment, 0, tmpHashtable, hashtableOffsets[k1], hs.hashtableSegment.length);

                for (int j = 0; j < hs.displacementTableSegment.length; j++) {
                    tmpDisplacementTable[displacementTableOffsets[k1] + j] = hashtableOffsets[k1]
                            + hs.displacementTableSegment[j];
                }
            }

            this.hashtable = new short[hashtableOffsets[parallelArraySize]];
            System.arraycopy(tmpHashtable, 0, this.hashtable, 0, this.hashtable.length);
            this.displacementTable = new int[displacementTableOffsets[parallelArraySize]];
            System.arraycopy(tmpDisplacementTable, 0, this.displacementTable, 0, this.displacementTable.length);

            this.packedArraySize = hashtableSize();

            // Compute the starting offset in binary grammar arrays of each column
            final Int2IntRBTreeMap childPairCounts = new Int2IntRBTreeMap();
            childPairCounts.defaultReturnValue(0);
            for (final Production p : binaryProductions) {
                final int column = pack((short) p.leftChild, (short) p.rightChild);
                childPairCounts.put(column, childPairCounts.get(column) + 1);
            }
        }

        private int findDisplacement(final short[] target, final short[] merge) {
            for (int s = 0; s <= target.length - merge.length; s++) {
                if (!shiftCollides(target, merge, s)) {
                    return s;
                }
            }
            throw new RuntimeException("Unable to find a successful shift");
        }

        private HashtableSegment createPerfectHash(final int[] k2s) {

            // If there are no k2 entries for this k1, return a single-entry hash segment, with a shift and
            // mask that will always resolve to the single (empty) entry
            if (k2s.length == 0) {
                return new HashtableSegment(new short[] { Short.MIN_VALUE }, 1, new int[] { 0 }, 1, 32, 0x0,
                        new short[][] { { Short.MIN_VALUE } });
            }

            // Compute the size of the square matrix (m)
            final int m = Math.nextPowerOf2((int) java.lang.Math.sqrt(Math.max(k2s)) + 1);
            final int n = m;

            // Allocate a temporary hashtable of the maximum possible size
            final short[] hashtableSegment = new short[m * n];
            Arrays.fill(hashtableSegment, Short.MIN_VALUE);

            // Allocate the displacement table (r in Getty's notation)
            final int[] displacementTableSegment = new int[m];

            // Compute shift and mask (for hashing k2, prior to displacement)
            final int hashBitShift = Math.logBase2(m);
            int tmp = 0;
            for (int j = 0; j < hashBitShift; j++) {
                tmp = tmp << 1 | 0x01;
            }
            final int hashMask = tmp;

            // Initialize the matrix
            final int[] rowIndices = new int[m];
            final int[] rowCounts = new int[m];
            final short[][] tmpMatrix = new short[m][n];
            for (int i = 0; i < m; i++) {
                rowIndices[i] = i;
                Arrays.fill(tmpMatrix[i], Short.MIN_VALUE);
            }

            // Populate the matrix, and count population of each row.
            for (int i = 0; i < k2s.length; i++) {
                final int k2 = k2s[i];
                final int x = k2 >> hashBitShift;
                final int y = k2 & hashMask;
                tmpMatrix[x][y] = (short) k2;
                rowCounts[x]++;
            }

            // Sort rows in ascending order by population (we'll iterate through the array in reverse order)
            edu.ohsu.cslu.util.Arrays.sort(rowCounts, rowIndices);

            /*
             * Store matrix rows in a single array, using the first-fit descending method. For each non-empty row:
             * 
             * 1. Displace the row right until none of its items collide with any of the items in previous rows.
             * 
             * 2. Record the displacement amount in displacementTableSegment.
             * 
             * 3. Insert this row into hashtableSegment.
             */
            for (int i = m - 1; i >= 0; i--) {
                final int row = rowIndices[i];
                displacementTableSegment[row] = findDisplacement(hashtableSegment, tmpMatrix[row]);
                for (int col = 0; col < m; col++) {
                    if (tmpMatrix[row][col] != Short.MIN_VALUE) {
                        hashtableSegment[displacementTableSegment[row] + col] = tmpMatrix[row][col];
                    }
                }
            }

            // Find the length of the segment (highest populated index in tmpHashtable + n)
            int maxPopulatedIndex = 0;
            for (int i = 0; i < hashtableSegment.length; i++) {
                if (hashtableSegment[i] != Short.MIN_VALUE) {
                    maxPopulatedIndex = i;
                }
            }
            final int segmentLength = maxPopulatedIndex + n;

            return new HashtableSegment(hashtableSegment, segmentLength, displacementTableSegment, m, hashBitShift,
                    hashMask, tmpMatrix);
        }

        /**
         * Returns true if the merged array, when shifted by s, will `collide' with the target array; i.e., if we
         * right-shift merge by s, are any populated elements of merge also populated elements of target.
         * 
         * @param target
         * @param merge
         * @param s
         * @return
         */
        private boolean shiftCollides(final short[] target, final short[] merge, final int s) {
            for (int i = 0; i < merge.length; i++) {
                if (merge[i] != Short.MIN_VALUE && target[s + i] != Short.MIN_VALUE) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public final int pack(final short leftChild, final short rightChild) {
            final int mask = k2Masks[leftChild];
            final int x = rightChild >> k2Shifts[leftChild] & mask;
            final int y = rightChild & mask;
            final int hashcode = displacementTable[displacementTableOffsets[leftChild] + x] + y;
            return hashtable[hashcode] == rightChild ? hashcode : Integer.MIN_VALUE;
        }

        public final int mask(final short leftChild) {
            return k2Masks[leftChild];
        }

        public final int offset(final short leftChild) {
            return displacementTableOffsets[leftChild];
        }

        public final int shift(final short leftChild) {
            return k2Shifts[leftChild];
        }

        public final int pack(final short rightChild, final int rightChildShift, final int mask, final int offset) {
            final int x = rightChild >> rightChildShift & mask;
            final int y = rightChild & mask;
            final int hashcode = displacementTable[offset + x] + y;
            return hashtable[hashcode] == rightChild ? hashcode : Integer.MIN_VALUE;
        }

        @Override
        public final int unpackLeftChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxLexicalProduction) {
                    // Lexical production
                    return -childPair + maxLexicalProduction;
                }
                // Unary production
                return -childPair - 1;
            }

            // Linear search hashtable offsets for index of next-lower offset
            // A binary search might be a bit more efficient, but this shouldn't be a major time consumer
            for (int k1 = 0; k1 < hashtableOffsets.length - 1; k1++) {
                if (childPair >= hashtableOffsets[k1] && childPair < hashtableOffsets[k1 + 1]) {
                    return k1;
                }
            }
            return hashtableOffsets.length - 1;
        }

        @Override
        public final short unpackRightChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxLexicalProduction) {
                    // Lexical production
                    return Production.LEXICAL_PRODUCTION;
                }
                // Unary production
                return Production.UNARY_PRODUCTION;
            }

            return hashtable[childPair];
        }

        public final int hashtableSize() {
            return hashtable.length;
        }

        public final int size() {
            return size;
        }

        public final int leftChildStart(final short leftChild) {
            return hashtableOffsets[leftChild];
        }

        @Override
        public String toString() {
            return String.format("keys: %d hashtable size: %d occupancy: %.2f%% shift-table size: %d totalMem: %d",
                    size, hashtableSize(), size * 100f / hashtableSize(), displacementTable.length, hashtable.length
                            * 2 + displacementTable.length * 4);
        }

        private final class HashtableSegment {

            final short[] hashtableSegment;
            final int[] displacementTableSegment;
            final int hashShift;
            final int hashMask;

            final short[][] squareMatrix;

            public HashtableSegment(final short[] hashtableSegment, final int segmentLength,
                    final int[] displacementTableSegment, final int displacementTableSegmentLength,
                    final int hashShift, final int hashMask, final short[][] squareMatrix) {

                this.hashtableSegment = new short[segmentLength];
                System.arraycopy(hashtableSegment, 0, this.hashtableSegment, 0, segmentLength);
                this.displacementTableSegment = new int[displacementTableSegmentLength];
                System.arraycopy(displacementTableSegment, 0, this.displacementTableSegment, 0,
                        displacementTableSegmentLength);
                this.hashShift = hashShift;
                this.hashMask = hashMask;
                this.squareMatrix = squareMatrix;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder();
                sb.append("   |");
                for (int col = 0; col < squareMatrix.length; col++) {
                    sb.append(String.format(" %2d", col));
                }
                sb.append('\n');
                sb.append(Strings.fill('-', squareMatrix.length * 3 + 4));
                sb.append('\n');
                for (int row = 0; row < squareMatrix.length; row++) {
                    sb.append(String.format("%2d |", row));
                    for (int col = 0; col < squareMatrix.length; col++) {
                        sb.append(String.format(
                                " %2s",
                                squareMatrix[row][col] == Short.MIN_VALUE ? "-" : Short
                                        .toString(squareMatrix[row][col])));
                    }
                    sb.append('\n');
                }
                sb.append("index: ");
                for (int i = 0; i < hashtableSegment.length; i++) {
                    sb.append(String.format(" %2d", i));
                }
                sb.append("\nkeys : ");
                for (int i = 0; i < hashtableSegment.length; i++) {
                    sb.append(String.format(" %2s",
                            hashtableSegment[i] == Short.MIN_VALUE ? "-" : Short.toString(hashtableSegment[i])));
                }
                sb.append('\n');
                return sb.toString();
            }
        }

        @Override
        public int packedArraySize() {
            return packedArraySize;
        }
    }
}
