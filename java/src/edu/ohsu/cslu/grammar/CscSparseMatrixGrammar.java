package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

/**
 * Stores a sparse-matrix grammar in compressed-sparse-column (CSC) format
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (see {@link SparseMatrixGrammar} documentation for
 * details).
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CscSparseMatrixGrammar extends SparseMatrixGrammar {

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of initial columns for each non-terminal, or -1 for
     * non-terminals which do not occur as left children. Indexed by left non-terminal. Length is 1 greater
     * than V, to simplify loops.
     */
    public final int[] cscBinaryLeftChildStartIndices;

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of final columns for each non-terminal, or -1 for
     * non-terminals which do not occur as left children. Indexed by left non-terminal. Length is 1 greater
     * than V, to simplify loops.
     */
    public final int[] cscBinaryLeftChildEndIndices;

    /**
     * Parallel array with {@link #cscBinaryPopulatedColumnOffsets}, containing indices of populated columns
     * (child pairs) and the offsets into {@link #cscBinaryRowIndices} at which each column starts. Array size
     * is the number of non-empty columns (+1 to simplify loops).
     */
    public final int[] cscBinaryPopulatedColumns;

    /**
     * Parallel array with {@link #cscBinaryPopulatedColumns} containing offsets into
     * {@link #cscBinaryRowIndices} at which each column starts.
     */
    public final int[] cscBinaryPopulatedColumnOffsets;

    /**
     * Row indices of each matrix entry in {@link #cscBinaryProbabilities}. One entry for each binary rule;
     * the same size as {@link #cscBinaryProbabilities}.
     * 
     * TODO Make this a short[]?
     */
    public final int[] cscBinaryRowIndices;

    /**
     * Binary rule probabilities One entry for each binary rule; the same size as {@link #cscBinaryRowIndices}
     * .
     */
    public final float[] cscBinaryProbabilities;

    public CscSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat, cartesianProductFunctionClass);

        final IntSet populatedBinaryColumnIndices = new IntOpenHashSet(binaryProductions.size() / 10);
        for (final Production p : binaryProductions) {
            populatedBinaryColumnIndices.add(cartesianProductFunction.pack(p.leftChild, p.rightChild));
        }
        final int[] sortedPopulatedBinaryColumnIndices = populatedBinaryColumnIndices.toIntArray();
        Arrays.sort(sortedPopulatedBinaryColumnIndices);

        // Bin all binary rules by child pair, mapping parent -> probability
        cscBinaryPopulatedColumns = new int[populatedBinaryColumnIndices.size()];
        cscBinaryPopulatedColumnOffsets = new int[cscBinaryPopulatedColumns.length + 1];
        cscBinaryRowIndices = new int[numBinaryRules()];
        cscBinaryProbabilities = new float[numBinaryRules()];

        storeRulesAsMatrix(binaryProductions, sortedPopulatedBinaryColumnIndices, cscBinaryPopulatedColumns,
            cscBinaryPopulatedColumnOffsets, cscBinaryRowIndices, cscBinaryProbabilities);

        cscBinaryLeftChildStartIndices = new int[numNonTerms() + 1];
        cscBinaryLeftChildEndIndices = new int[numNonTerms() + 1];
        Arrays.fill(cscBinaryLeftChildStartIndices, -1);
        Arrays.fill(cscBinaryLeftChildEndIndices, -1);

        final String[] sBinaryPopulatedColumnsLeft = new String[cscBinaryPopulatedColumns.length - 1];
        final String[] sBinaryPopulatedColumnsRight = new String[cscBinaryPopulatedColumns.length - 1];
        for (int i = 0; i < cscBinaryPopulatedColumns.length - 1; i++) {
            sBinaryPopulatedColumnsLeft[i] = mapNonterminal(cartesianProductFunction
                .unpackLeftChild(cscBinaryPopulatedColumns[i]));
            sBinaryPopulatedColumnsRight[i] = mapNonterminal(cartesianProductFunction
                .unpackRightChild(cscBinaryPopulatedColumns[i]));
        }
        for (int i = 0; i < cscBinaryPopulatedColumns.length; i++) {
            final int leftChild = cartesianProductFunction.unpackLeftChild(cscBinaryPopulatedColumns[i]);
            if (cscBinaryLeftChildStartIndices[leftChild] < 0) {
                cscBinaryLeftChildStartIndices[leftChild] = i;
            }
            cscBinaryLeftChildEndIndices[leftChild] = i;
        }

        final IntSet populatedUnaryColumnIndices = new IntOpenHashSet(unaryProductions.size() / 10);
        for (final Production p : unaryProductions) {
            populatedUnaryColumnIndices.add(cartesianProductFunction.packUnary(p.leftChild));
        }
        final int[] sortedPopulatedUnaryColumnIndices = populatedUnaryColumnIndices.toIntArray();
        Arrays.sort(sortedPopulatedUnaryColumnIndices);

        storeUnaryRules();

        tokenizer = new Tokenizer(lexSet);
    }

    public CscSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(grammarFile, lexiconFile, grammarFormat, null);
    }

    public CscSparseMatrixGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    /**
     * 
     * @param productions
     * @param validChildPairs Sorted array of valid child pairs
     * @param cscPopulatedColumns
     * @param cscColumnIndices
     * @param cscRowIndices
     * @param csrProbabilities
     */
    private void storeRulesAsMatrix(final Collection<Production> productions, final int[] validChildPairs,
            final int[] cscPopulatedColumns, final int[] cscColumnIndices, final int[] cscRowIndices,
            final float[] csrProbabilities) {

        final Int2ObjectOpenHashMap<Int2FloatOpenHashMap> maps = mapRulesByChildPairs(productions);

        // Store rules in CSC matrix
        int j = 0;
        for (int i = 0; i < validChildPairs.length; i++) {
            final int childPair = validChildPairs[i];

            cscPopulatedColumns[i] = childPair;
            cscColumnIndices[i] = j;

            final Int2FloatOpenHashMap map = maps.get(childPair);
            final int[] parents = map.keySet().toIntArray();
            Arrays.sort(parents);

            for (int k = 0; k < parents.length; k++) {
                cscRowIndices[j] = parents[k];
                csrProbabilities[j++] = map.get(parents[k]);
            }
        }
        cscColumnIndices[cscColumnIndices.length - 1] = j;
    }

    @Override
    public final float binaryLogProbability(final int parent, final int childPair) {

        // Find the column (child pair)
        int c = -1;
        for (int i = 0; i < cscBinaryPopulatedColumns.length; i++) {
            if (cscBinaryPopulatedColumns[i] == childPair) {
                c = i;
                break;
            }
        }
        if (c == -1) {
            return Float.NEGATIVE_INFINITY;
        }

        for (int i = cscBinaryPopulatedColumnOffsets[c]; i < cscBinaryPopulatedColumnOffsets[c + 1]; i++) {
            final int row = cscBinaryRowIndices[i];
            if (row == parent) {
                return cscBinaryProbabilities[i];
            }
            if (row > parent) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }
}
