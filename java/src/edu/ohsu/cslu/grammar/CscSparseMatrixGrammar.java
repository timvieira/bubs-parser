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
     * Indices in {@link #cscBinaryPopulatedColumns} of initial columns for each non-terminal. Indexed by left
     * non-terminal.
     */
    private int[] cscBinaryLeftChildStartIndices;

    /**
     * Indices of populated columns (child pairs).
     */
    private int[] cscBinaryPopulatedColumns;

    /**
     * Offsets into {@link #cscBinaryRowIndices} for the start of each populated column, with one extra entry
     * appended to prevent loops from falling off the end. Length is 1 greater than
     * {@link #cscBinaryPopulatedColumns}.
     */
    private int[] cscBinaryColumnOffsets;

    /**
     * Row indices of each matrix entry in {@link #cscBinaryProbabilities}. One entry for each binary rule;
     * the same size as {@link #cscBinaryProbabilities}.
     */
    private int[] cscBinaryRowIndices;

    /**
     * Binary rule probabilities One entry for each binary rule; the same size as {@link #cscBinaryRowIndices}
     * .
     */
    private float[] cscBinaryProbabilities;

    /**
     * Indices of populated columns (children).
     */
    private int[] cscUnaryPopulatedColumns;

    /**
     * Offsets into {@link #cscUnaryRowIndices} for the start of each populated column, with one extra entry
     * appended to prevent loops from falling off the end. Length is 1 greater than
     * {@link #cscUnaryPopulatedColumns}
     */
    private int[] cscUnaryColumnOffsets;

    /**
     * Row indices of each matrix entry in {@link #cscUnaryProbabilities}. One entry for each unary rule; the
     * same size as {@link #cscUnaryProbabilities}.
     */
    private int[] cscUnaryRowIndices;

    /** Unary rule probabilities One entry for each binary rule; the same size as {@link #cscUnaryRowIndices}. */
    private float[] cscUnaryProbabilities;

    public CscSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat, cartesianProductFunctionClass);

        final IntSet populatedBinaryColumnIndices = new IntOpenHashSet(binaryProductions.size() / 10);
        for (final Production p : binaryProductions) {
            populatedBinaryColumnIndices
                .add(cartesianProductFunction.pack(p.leftChild, (short) p.rightChild));
        }
        final int[] sortedPopulatedBinaryColumnIndices = populatedBinaryColumnIndices.toIntArray();
        Arrays.sort(sortedPopulatedBinaryColumnIndices);

        // Bin all binary rules by child pair, mapping parent -> probability
        cscBinaryPopulatedColumns = new int[populatedBinaryColumnIndices.size()];
        cscBinaryColumnOffsets = new int[cscBinaryPopulatedColumns.length + 1];
        cscBinaryRowIndices = new int[numBinaryRules()];
        cscBinaryProbabilities = new float[numBinaryRules()];

        storeRulesAsMatrix(binaryProductions, sortedPopulatedBinaryColumnIndices, cscBinaryPopulatedColumns,
            cscBinaryColumnOffsets, cscBinaryRowIndices, cscBinaryProbabilities);

        cscBinaryLeftChildStartIndices = new int[numNonTerms() + 1];
        for (int i = 0; i < cscBinaryPopulatedColumns.length; i++) {
            final int leftChild = cartesianProductFunction.unpackLeftChild(cscBinaryPopulatedColumns[i]);
            cscBinaryLeftChildStartIndices[leftChild] = i;
            while (i < cscBinaryPopulatedColumns.length
                    && cartesianProductFunction.unpackLeftChild(cscBinaryPopulatedColumns[i]) == leftChild) {
                i++;
            }
        }
        for (int i = 1; i < cscBinaryLeftChildStartIndices.length; i++) {
            if (cscBinaryLeftChildStartIndices[i] == 0) {
                cscBinaryLeftChildStartIndices[i] = cscBinaryLeftChildStartIndices[i - 1];
            }
        }

        final IntSet populatedUnaryColumnIndices = new IntOpenHashSet(unaryProductions.size() / 10);
        for (final Production p : unaryProductions) {
            populatedUnaryColumnIndices.add(cartesianProductFunction.pack(p.leftChild, (short) p.rightChild));
        }
        final int[] sortedPopulatedUnaryColumnIndices = populatedUnaryColumnIndices.toIntArray();
        Arrays.sort(sortedPopulatedUnaryColumnIndices);

        // And all unary rules
        cscUnaryPopulatedColumns = new int[populatedUnaryColumnIndices.size()];
        cscUnaryColumnOffsets = new int[cscUnaryPopulatedColumns.length + 1];
        cscUnaryRowIndices = new int[numUnaryRules()];
        cscUnaryProbabilities = new float[numUnaryRules()];

        storeRulesAsMatrix(unaryProductions, sortedPopulatedUnaryColumnIndices, cscUnaryPopulatedColumns,
            cscUnaryColumnOffsets, cscUnaryRowIndices, cscUnaryProbabilities);

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

    public final int[] binaryLeftChildStartIndices() {
        return cscBinaryLeftChildStartIndices;
    }

    public final int[] binaryRuleMatrixPopulatedColumns() {
        return cscBinaryPopulatedColumns;
    }

    public final int[] binaryRuleMatrixColumnOffsets() {
        return cscBinaryColumnOffsets;
    }

    public final int[] binaryRuleMatrixRowIndices() {
        return cscBinaryRowIndices;
    }

    public final float[] binaryRuleMatrixProbabilities() {
        return cscBinaryProbabilities;
    }

    public final int[] unaryRuleMatrixPopulatedColumns() {
        return cscUnaryPopulatedColumns;
    }

    public final int[] unaryRuleMatrixColumnOffsets() {
        return cscUnaryColumnOffsets;
    }

    public final int[] unaryRuleMatrixRowIndices() {
        return cscUnaryRowIndices;
    }

    public final float[] unaryRuleMatrixProbabilities() {
        return cscUnaryProbabilities;
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

        for (int i = cscBinaryColumnOffsets[c]; i < cscBinaryColumnOffsets[c + 1]; i++) {
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

    @Override
    public final float unaryLogProbability(final int parent, final int child) {
        final int childPair = cartesianProductFunction.packUnary(child);

        // Find the column (child pair)
        int c = -1;
        for (int i = 0; i < cscUnaryPopulatedColumns.length; i++) {
            if (cscUnaryPopulatedColumns[i] == childPair) {
                c = i;
                break;
            }
        }
        if (c == -1) {
            return Float.NEGATIVE_INFINITY;
        }

        for (int i = cscUnaryColumnOffsets[c]; i < cscUnaryColumnOffsets[c + 1]; i++) {
            final int row = cscUnaryRowIndices[i];
            if (row == parent) {
                return cscUnaryProbabilities[i];
            }
            if (row > parent) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }
}
