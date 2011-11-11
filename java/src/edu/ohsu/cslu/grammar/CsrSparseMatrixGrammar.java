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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Stores a sparse-matrix grammar in standard compressed-sparse-row (CSR) format
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (see {@link SparseMatrixGrammar} documentation for details).
 * 
 * TODO Try storung 2 separate matrices; one for span = 2 and one for span > 2. They might be nearly disjoint, since
 * span = 2 means both children are pre-terminals, and we assume the set of multi-word constituents is disjoint from the
 * set of pre-terminals, but the span = 2 matrix has to include any children which occur as unary parents, since those
 * NTs might be found in span-1 cells. It should still shrink the 'main' chart matrix considerably, and thus save time
 * iterating over the ruleset.
 * 
 * TODO Remove unused constructors
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 */
public class CsrSparseMatrixGrammar extends SparseMatrixGrammar {

    private static final long serialVersionUID = 1L;

    /**
     * Offsets into {@link #csrBinaryColumnIndices} for the start of each row, indexed by row index (non-terminals),
     * with one extra entry appended to prevent loops from falling off the end
     */
    public final int[] csrBinaryRowIndices;

    /**
     * Column indices of each matrix entry in {@link #csrBinaryProbabilities}. One entry for each binary rule; the same
     * size as {@link #csrBinaryProbabilities}.
     */
    public final int[] csrBinaryColumnIndices;

    /**
     * Binary rule probabilities. One entry for each binary rule. The same size as {@link #csrBinaryColumnIndices}.
     */
    public final float[] csrBinaryProbabilities;

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
    public final float[] csrUnaryProbabilities;

    public CsrSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends PackingFunction> packingFunctionClass) throws IOException {
        super(grammarFile, packingFunctionClass);

        // Bin all binary rules by parent, mapping packed children -> probability
        this.csrBinaryRowIndices = new int[numNonTerms() + 1];
        this.csrBinaryColumnIndices = new int[numBinaryProds()];
        this.csrBinaryProbabilities = new float[numBinaryProds()];

        storeBinaryRulesAsCsrMatrix(mapBinaryRulesByParent(binaryProductions), csrBinaryRowIndices,
                csrBinaryColumnIndices, csrBinaryProbabilities);

        // Store all unary rules
        this.csrUnaryRowStartIndices = new int[numNonTerms() + 1];
        this.csrUnaryColumnIndices = new short[numUnaryProds()];
        this.csrUnaryProbabilities = new float[numUnaryProds()];

        storeUnaryRulesAsCsrMatrix(csrUnaryRowStartIndices, csrUnaryColumnIndices, csrUnaryProbabilities);
    }

    public CsrSparseMatrixGrammar(final Reader grammarFile) throws IOException {
        this(grammarFile, null);
    }

    public CsrSparseMatrixGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    public CsrSparseMatrixGrammar(final Grammar g, final Class<? extends PackingFunction> functionClass) {
        super(g, functionClass);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.csrBinaryRowIndices = new int[numNonTerms() + 1];
        this.csrBinaryColumnIndices = new int[numBinaryProds()];
        this.csrBinaryProbabilities = new float[numBinaryProds()];

        storeBinaryRulesAsCsrMatrix(mapBinaryRulesByParent(binaryProductions), csrBinaryRowIndices,
                csrBinaryColumnIndices, csrBinaryProbabilities);

        // Store all unary rules
        this.csrUnaryRowStartIndices = new int[numNonTerms() + 1];
        this.csrUnaryColumnIndices = new short[numUnaryProds()];
        this.csrUnaryProbabilities = new float[numUnaryProds()];

        storeUnaryRulesAsCsrMatrix(csrUnaryRowStartIndices, csrUnaryColumnIndices, csrUnaryProbabilities);
    }

    protected CsrSparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final Class<? extends PackingFunction> functionClass, final boolean initCsrMatrices) {
        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                functionClass, initCsrMatrices);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.csrBinaryRowIndices = new int[numNonTerms() + 1];
        this.csrBinaryColumnIndices = new int[numBinaryProds()];
        this.csrBinaryProbabilities = new float[numBinaryProds()];

        // Store all unary rules
        this.csrUnaryRowStartIndices = new int[numNonTerms() + 1];
        this.csrUnaryColumnIndices = new short[numUnaryProds()];
        this.csrUnaryProbabilities = new float[numUnaryProds()];

        if (initCsrMatrices) {
            storeBinaryRulesAsCsrMatrix(mapBinaryRulesByParent(binaryProductions),
                    csrBinaryRowIndices, csrBinaryColumnIndices, csrBinaryProbabilities);
            storeUnaryRulesAsCsrMatrix(csrUnaryRowStartIndices, csrUnaryColumnIndices, csrUnaryProbabilities);
        }
    }

    public CsrSparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final Class<? extends PackingFunction> functionClass) {
        this(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                functionClass, true);
    }

    protected void storeBinaryRulesAsCsrMatrix(final Int2FloatOpenHashMap[] maps, final int[] csrRowIndices,
            final int[] csrColumnIndices, final float[] csrProbabilities) {

        // Store rules in CSR matrix
        int i = 0;
        for (int parent = 0; parent < numNonTerms(); parent++) {

            csrRowIndices[parent] = i;

            final int[] children = maps[parent].keySet().toIntArray();
            Arrays.sort(children);
            for (int j = 0; j < children.length; j++) {
                csrColumnIndices[i] = children[j];
                csrProbabilities[i++] = maps[parent].get(children[j]);
            }
        }
        csrRowIndices[csrRowIndices.length - 1] = i;
    }

    protected Int2FloatOpenHashMap[] mapBinaryRulesByParent(final ArrayList<Production> rules) {
        // Bin all rules by parent, mapping packed children -> probability
        final Int2FloatOpenHashMap[] maps = new Int2FloatOpenHashMap[numNonTerms()];
        for (int i = 0; i < numNonTerms(); i++) {
            maps[i] = new Int2FloatOpenHashMap(1000);
        }

        for (final Production p : rules) {
            maps[p.parent].put(packingFunction.pack((short) p.leftChild, (short) p.rightChild), p.prob);
        }
        return maps;
    }

    @Override
    public final float binaryLogProbability(final int parent, final int childPair) {

        for (int i = csrBinaryRowIndices[parent]; i < csrBinaryRowIndices[parent + 1]; i++) {
            final int column = csrBinaryColumnIndices[i];
            if (column == childPair) {
                return csrBinaryProbabilities[i];
            }
            if (column > childPair) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }
}
