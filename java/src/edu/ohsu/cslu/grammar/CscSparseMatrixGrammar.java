package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

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

    protected CscSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws IOException {
        super(grammarFile, cartesianProductFunctionClass);

        final int[] populatedBinaryColumnIndices = populatedBinaryColumnIndices();

        // Bin all binary rules by child pair, mapping parent -> probability
        cscBinaryPopulatedColumns = new int[populatedBinaryColumnIndices.length];
        cscBinaryPopulatedColumnOffsets = new int[cscBinaryPopulatedColumns.length + 1];
        cscBinaryRowIndices = new short[numBinaryProds()];
        cscBinaryProbabilities = new float[numBinaryProds()];

        storeRulesAsMatrix(binaryProductions, populatedBinaryColumnIndices, cscBinaryPopulatedColumns,
                cscBinaryPopulatedColumnOffsets, cscBinaryRowIndices, cscBinaryProbabilities);
    }

    protected CscSparseMatrixGrammar(final Reader grammarFile) throws IOException {
        this(grammarFile, null);
    }

    protected CscSparseMatrixGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    protected CscSparseMatrixGrammar(final Grammar g, final Class<? extends CartesianProductFunction> functionClass) {
        super(g, functionClass);

        // Initialization code duplicated from constructor above to allow these fields to be final
        final int[] populatedBinaryColumnIndices = populatedBinaryColumnIndices();

        // Bin all binary rules by child pair, mapping parent -> probability
        cscBinaryPopulatedColumns = new int[populatedBinaryColumnIndices.length];
        cscBinaryPopulatedColumnOffsets = new int[cscBinaryPopulatedColumns.length + 1];
        cscBinaryRowIndices = new short[numBinaryProds()];
        cscBinaryProbabilities = new float[numBinaryProds()];

        storeRulesAsMatrix(binaryProductions, populatedBinaryColumnIndices, cscBinaryPopulatedColumns,
                cscBinaryPopulatedColumnOffsets, cscBinaryRowIndices, cscBinaryProbabilities);
    }

    private int[] populatedBinaryColumnIndices() {
        final IntSet populatedBinaryColumnIndices = new IntOpenHashSet(binaryProductions.size() / 10);
        for (final Production p : binaryProductions) {
            populatedBinaryColumnIndices.add(cartesianProductFunction.pack(p.leftChild, p.rightChild));
        }
        final int[] sortedPopulatedBinaryColumnIndices = populatedBinaryColumnIndices.toIntArray();
        Arrays.sort(sortedPopulatedBinaryColumnIndices);
        return sortedPopulatedBinaryColumnIndices;
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
            final int[] cscPopulatedColumns, final int[] cscColumnIndices, final short[] cscRowIndices,
            final float[] csrProbabilities) {

        // Bin all rules by child pair, mapping parent -> probability
        final Int2ObjectOpenHashMap<Int2FloatOpenHashMap> maps1 = new Int2ObjectOpenHashMap<Int2FloatOpenHashMap>(1000);

        for (final Production p : productions) {
            int childPair1;
            if (p.isBinaryProd()) {
                childPair1 = cartesianProductFunction.pack(p.leftChild, p.rightChild);
            } else if (p.isLexProd()) {
                childPair1 = cartesianProductFunction.packLexical(p.leftChild);
            } else {
                childPair1 = cartesianProductFunction.packUnary(p.leftChild);
            }
            Int2FloatOpenHashMap map1 = maps1.get(childPair1);
            if (map1 == null) {
                map1 = new Int2FloatOpenHashMap(20);
                maps1.put(childPair1, map1);
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
