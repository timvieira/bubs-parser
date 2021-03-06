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
package edu.ohsu.cslu.grammar;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * Stores a sparse-matrix grammar in compressed-sparse-column (CSC) format
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (see {@link SparseMatrixGrammar} documentation for details).
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 */
public class LeftCscSparseMatrixGrammar extends CscSparseMatrixGrammar {

    private static final long serialVersionUID = 1L;

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of initial columns for each non-terminal. Indexed by left
     * non-terminal. Length is 1 greater than V, to simplify loops.
     */
    public final int[] cscBinaryLeftChildStartIndices;

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of final columns for each non-terminal. Indexed by left
     * non-terminal. Length is 1 greater than V, to simplify loops.
     */
    public final int[] cscBinaryLeftChildEndIndices;

    public LeftCscSparseMatrixGrammar(final Reader grammarFile, final TokenClassifier tokenClassifier,
            final Class<? extends PackingFunction> packingFunctionClass) throws IOException {
        super(grammarFile, tokenClassifier, packingFunctionClass);

        this.cscBinaryLeftChildStartIndices = new int[numNonTerms() + 1];
        this.cscBinaryLeftChildEndIndices = new int[numNonTerms() + 1];
        init();
    }

    public LeftCscSparseMatrixGrammar(final Reader grammarFile, final TokenClassifier tokenClassifier)
            throws IOException {
        this(grammarFile, tokenClassifier, null);
    }

    public LeftCscSparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final MutableEnumeration<String> vocabulary, final MutableEnumeration<String> lexicon, final GrammarFormatType grammarFormat,
            final TokenClassifier tokenClassifier, final Class<? extends PackingFunction> packingFunctionClass,
            final boolean initCscMatrices) {

        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                tokenClassifier, packingFunctionClass, initCscMatrices);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.cscBinaryLeftChildStartIndices = new int[numNonTerms() + 1];
        this.cscBinaryLeftChildEndIndices = new int[numNonTerms() + 1];
        init();
    }

    public LeftCscSparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final MutableEnumeration<String> vocabulary, final MutableEnumeration<String> lexicon, final GrammarFormatType grammarFormat,
            final TokenClassifier tokenClassifier) {

        this(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                tokenClassifier, PerfectIntPairHashPackingFunction.class, true);
    }

    /**
     * For unit testing
     * 
     * @param g
     */
    public LeftCscSparseMatrixGrammar(final Grammar g) {
        this(((SparseMatrixGrammar) g).getBinaryProductions(), ((SparseMatrixGrammar) g).getUnaryProductions(),
                ((SparseMatrixGrammar) g).getLexicalProductions(), ((SparseMatrixGrammar) g).nonTermSet,
                ((SparseMatrixGrammar) g).lexSet, ((SparseMatrixGrammar) g).grammarFormat,
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class, true);
    }

    private void init() {
        Arrays.fill(cscBinaryLeftChildStartIndices, -1);
        Arrays.fill(cscBinaryLeftChildEndIndices, -1);

        for (int i = 0; i < cscBinaryPopulatedColumns.length; i++) {
            final int leftChild = packingFunction.unpackLeftChild(cscBinaryPopulatedColumns[i]);
            if (cscBinaryLeftChildStartIndices[leftChild] < 0) {
                cscBinaryLeftChildStartIndices[leftChild] = i;
            }
            cscBinaryLeftChildEndIndices[leftChild] = i;
        }
    }
}
