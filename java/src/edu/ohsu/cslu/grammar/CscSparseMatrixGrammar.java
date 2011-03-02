package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

public abstract class CscSparseMatrixGrammar extends SparseMatrixGrammar {

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
     * Row indices of each matrix entry in {@link #cscBinaryProbabilities}. One entry for each binary rule; the same
     * size as {@link #cscBinaryProbabilities}.
     */
    public final short[] cscBinaryRowIndices;

    /**
     * Binary rule probabilities One entry for each binary rule; the same size as {@link #cscBinaryRowIndices} .
     */
    public final float[] cscBinaryProbabilities;

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
     * Row indices of each matrix entry in {@link #cscBinaryProbabilities}. One entry for each binary rule; the same
     * size as {@link #cscBinaryProbabilities}.
     */
    public final short[] factoredCscBinaryRowIndices;

    /**
     * Binary rule probabilities One entry for each binary rule; the same size as {@link #cscBinaryRowIndices} .
     */
    public final float[] factoredCscBinaryProbabilities;

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

    /** Unary rule probabilities One entry for each unary rule; the same size as {@link #cscUnaryRowIndices}. */
    public final float[] cscUnaryProbabilities;

    protected CscSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends PackingFunction> cartesianProductFunctionClass) throws IOException {
        super(grammarFile, cartesianProductFunctionClass);

        // All binary productions
        final int[] populatedBinaryColumnIndices = populatedBinaryColumnIndices(binaryProductions);
        cscBinaryPopulatedColumns = new int[populatedBinaryColumnIndices.length];
        cscBinaryPopulatedColumnOffsets = new int[cscBinaryPopulatedColumns.length + 1];
        cscBinaryRowIndices = new short[numBinaryProds()];
        cscBinaryProbabilities = new float[numBinaryProds()];

        storeRulesAsMatrix(binaryProductions, populatedBinaryColumnIndices, cscBinaryPopulatedColumns,
                cscBinaryPopulatedColumnOffsets, cscBinaryRowIndices, cscBinaryProbabilities);

        // Factored productions only
        final Collection<Production> factoredBinaryProductions = getFactoredBinaryProductions();
        final int[] factoredPopulatedBinaryColumnIndices = populatedBinaryColumnIndices(factoredBinaryProductions);
        factoredCscBinaryPopulatedColumns = new int[factoredPopulatedBinaryColumnIndices.length];
        factoredCscBinaryPopulatedColumnOffsets = new int[factoredCscBinaryPopulatedColumns.length + 1];
        factoredCscBinaryRowIndices = new short[factoredBinaryProductions.size()];
        factoredCscBinaryProbabilities = new float[factoredBinaryProductions.size()];

        storeRulesAsMatrix(factoredBinaryProductions, factoredPopulatedBinaryColumnIndices,
                factoredCscBinaryPopulatedColumns, factoredCscBinaryPopulatedColumnOffsets,
                factoredCscBinaryRowIndices, factoredCscBinaryProbabilities);

        // And all unary productions
        cscUnaryColumnOffsets = new int[numNonTerms() + 1];
        cscUnaryRowIndices = new short[numUnaryProds()];
        cscUnaryProbabilities = new float[numUnaryProds()];

        storeUnaryRulesAsCscMatrix(unaryProductions, cscUnaryColumnOffsets, cscUnaryRowIndices, cscUnaryProbabilities);
    }

    protected CscSparseMatrixGrammar(final Reader grammarFile) throws IOException {
        this(grammarFile, null);
    }

    protected CscSparseMatrixGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    protected CscSparseMatrixGrammar(final Grammar g, final Class<? extends PackingFunction> functionClass) {
        super(g, functionClass);

        // Initialization code duplicated from constructor above to allow these fields to be final
        final int[] populatedBinaryColumnIndices = populatedBinaryColumnIndices(binaryProductions);
        cscBinaryPopulatedColumns = new int[populatedBinaryColumnIndices.length];
        cscBinaryPopulatedColumnOffsets = new int[cscBinaryPopulatedColumns.length + 1];
        cscBinaryRowIndices = new short[numBinaryProds()];
        cscBinaryProbabilities = new float[numBinaryProds()];

        storeRulesAsMatrix(binaryProductions, populatedBinaryColumnIndices, cscBinaryPopulatedColumns,
                cscBinaryPopulatedColumnOffsets, cscBinaryRowIndices, cscBinaryProbabilities);

        // Factored productions only
        final Collection<Production> factoredBinaryProductions = getFactoredBinaryProductions();
        final int[] factoredPopulatedBinaryColumnIndices = populatedBinaryColumnIndices(factoredBinaryProductions);
        factoredCscBinaryPopulatedColumns = new int[factoredPopulatedBinaryColumnIndices.length];
        factoredCscBinaryPopulatedColumnOffsets = new int[factoredCscBinaryPopulatedColumns.length + 1];
        factoredCscBinaryRowIndices = new short[factoredBinaryProductions.size()];
        factoredCscBinaryProbabilities = new float[factoredBinaryProductions.size()];

        storeRulesAsMatrix(factoredBinaryProductions, factoredPopulatedBinaryColumnIndices,
                factoredCscBinaryPopulatedColumns, factoredCscBinaryPopulatedColumnOffsets,
                factoredCscBinaryRowIndices, factoredCscBinaryProbabilities);

        // And all unary productions
        cscUnaryColumnOffsets = new int[numNonTerms() + 1];
        cscUnaryRowIndices = new short[numUnaryProds()];
        cscUnaryProbabilities = new float[numUnaryProds()];

        storeUnaryRulesAsCscMatrix(unaryProductions, cscUnaryColumnOffsets, cscUnaryRowIndices, cscUnaryProbabilities);
    }

    private int[] populatedBinaryColumnIndices(final Collection<Production> productions) {
        final IntSet populatedBinaryColumnIndices = new IntOpenHashSet(binaryProductions.size() / 10);
        for (final Production p : productions) {
            populatedBinaryColumnIndices.add(cartesianProductFunction.pack((short) p.leftChild, (short) p.rightChild));
        }
        final int[] sortedPopulatedBinaryColumnIndices = populatedBinaryColumnIndices.toIntArray();
        Arrays.sort(sortedPopulatedBinaryColumnIndices);
        return sortedPopulatedBinaryColumnIndices;
    }

    /**
     * Stores binary rules in Compressed-Sparse-Column (CSC) matrix format.
     * 
     * @param productions
     * @param validChildPairs Sorted array of valid child pairs
     * @param cscPopulatedColumns
     * @param cscColumnIndices
     * @param cscRowIndices
     * @param cscProbabilities
     */
    private void storeRulesAsMatrix(final Collection<Production> productions, final int[] validChildPairs,
            final int[] cscPopulatedColumns, final int[] cscColumnIndices, final short[] cscRowIndices,
            final float[] cscProbabilities) {

        // Bin all rules by child pair, mapping parent -> probability
        final Int2ObjectOpenHashMap<Int2FloatOpenHashMap> maps1 = new Int2ObjectOpenHashMap<Int2FloatOpenHashMap>(1000);

        for (final Production p : productions) {
            final int childPair = cartesianProductFunction.pack((short) p.leftChild, (short) p.rightChild);
            Int2FloatOpenHashMap map1 = maps1.get(childPair);
            if (map1 == null) {
                map1 = new Int2FloatOpenHashMap(20);
                maps1.put(childPair, map1);
            }
            map1.put(p.parent, p.prob);
        }
        final Int2ObjectOpenHashMap<Int2FloatOpenHashMap> maps = maps1;

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
                cscRowIndices[j] = (short) parents[k];
                cscProbabilities[j++] = map.get(parents[k]);
            }
        }
        cscColumnIndices[cscColumnIndices.length - 1] = j;
    }

    /**
     * Stores binary rules in Compressed-Sparse-Column (CSC) matrix format.
     * 
     * @param productions
     * @param validChildPairs Sorted array of valid child pairs
     * @param cscPopulatedColumns
     * @param cscColumnIndices
     * @param cscRowIndices
     * @param cscProbabilities
     */
    private void storeUnaryRulesAsCscMatrix(final Collection<Production> productions, final int[] cscColumnOffsets,
            final short[] cscRowIndices, final float[] cscProbabilities) {

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
                cscProbabilities[j++] = map.get(parents[k]);
            }
        }
        cscColumnOffsets[cscColumnOffsets.length - 1] = j;
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
