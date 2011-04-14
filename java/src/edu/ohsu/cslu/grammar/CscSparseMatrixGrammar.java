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

    private static final long serialVersionUID = 1L;

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
     */
    public final short[] cscBinaryRowIndices;

    /**
     * Binary rule probabilities One entry for each binary rule; the same size as {@link #cscBinaryRowIndices}
     * .
     */
    public final float[] cscBinaryProbabilities;

    /**
     * Parallel array with {@link #cscBinaryPopulatedColumnOffsets}, containing indices of populated columns
     * (child pairs) and the offsets into {@link #cscBinaryRowIndices} at which each column starts. Array size
     * is the number of non-empty columns (+1 to simplify loops).
     */
    public final int[] factoredCscBinaryPopulatedColumns;

    /**
     * Parallel array with {@link #cscBinaryPopulatedColumns} containing offsets into
     * {@link #cscBinaryRowIndices} at which each column starts.
     */
    public final int[] factoredCscBinaryPopulatedColumnOffsets;

    /**
     * Row indices of each matrix entry in {@link #cscBinaryProbabilities}. One entry for each binary rule;
     * the same size as {@link #cscBinaryProbabilities}.
     */
    public final short[] factoredCscBinaryRowIndices;

    /**
     * Binary rule probabilities One entry for each binary rule; the same size as {@link #cscBinaryRowIndices}
     * .
     */
    public final float[] factoredCscBinaryProbabilities;

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
    }

    private int[] populatedBinaryColumnIndices(final Collection<Production> productions) {
        final IntSet populatedBinaryColumnIndices = new IntOpenHashSet(binaryProductions.size() / 10);
        for (final Production p : productions) {
            populatedBinaryColumnIndices.add(packingFunction.pack((short) p.leftChild, (short) p.rightChild));
        }
        final int[] sortedPopulatedBinaryColumnIndices = populatedBinaryColumnIndices.toIntArray();
        Arrays.sort(sortedPopulatedBinaryColumnIndices);
        return sortedPopulatedBinaryColumnIndices;
    }

    /**
     * Stores binary rules in Compressed-Sparse-Column (CSC) matrix format.
     * 
     * @param productions
     * @param validChildPairs
     *            Sorted array of valid child pairs
     * @param cscPopulatedColumns
     * @param cscColumnIndices
     * @param cscRowIndices
     * @param cscProbabilities
     */
    private void storeRulesAsMatrix(final Collection<Production> productions, final int[] validChildPairs,
            final int[] cscPopulatedColumns, final int[] cscColumnIndices, final short[] cscRowIndices,
            final float[] cscProbabilities) {

        // Bin all rules by child pair, mapping parent -> probability
        final Int2ObjectOpenHashMap<Int2FloatOpenHashMap> maps1 = new Int2ObjectOpenHashMap<Int2FloatOpenHashMap>(
            1000);

        for (final Production p : productions) {
            final int childPair = packingFunction.pack((short) p.leftChild, (short) p.rightChild);
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
