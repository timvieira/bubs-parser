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

/**
 * Stores a sparse-matrix grammar in compressed-sparse-column (CSC) format
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (see {@link SparseMatrixGrammar} documentation for details).
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 */
public class RightCscSparseMatrixGrammar extends CscSparseMatrixGrammar {

    private static final long serialVersionUID = 1L;

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of initial columns for each non-terminal, or -1 for non-terminals
     * which do not occur as right children. Indexed by right non-terminal. Length is 1 greater than V, to simplify
     * loops.
     */
    public final int[] cscBinaryRightChildStartIndices;

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of final columns for each non-terminal, or -1 for non-terminals
     * which do not occur as right children. Indexed by right non-terminal. Length is 1 greater than V, to simplify
     * loops.
     */
    public final int[] cscBinaryRightChildEndIndices;

    public RightCscSparseMatrixGrammar(final Reader grammarFile, final TokenClassifier tokenClassifier,
            final Class<? extends PackingFunction> cartesianProductFunctionClass) throws IOException {
        super(grammarFile, tokenClassifier, cartesianProductFunctionClass);

        this.cscBinaryRightChildStartIndices = new int[numNonTerms() + 1];
        this.cscBinaryRightChildEndIndices = new int[numNonTerms() + 1];
        init();
    }

    public RightCscSparseMatrixGrammar(final Reader grammarFile, final TokenClassifier tokenClassifier)
            throws IOException {
        this(grammarFile, tokenClassifier, null);
    }

    public RightCscSparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final TokenClassifier tokenClassifier, final Class<? extends PackingFunction> functionClass,
            final boolean initCscMatrices) {

        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                tokenClassifier, functionClass, initCscMatrices);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.cscBinaryRightChildStartIndices = new int[numNonTerms() + 1];
        this.cscBinaryRightChildEndIndices = new int[numNonTerms() + 1];
        init();
    }

    private void init() {
        Arrays.fill(cscBinaryRightChildStartIndices, -1);
        Arrays.fill(cscBinaryRightChildEndIndices, -1);

        for (int i = 0; i < cscBinaryPopulatedColumns.length; i++) {
            final int rightChild = packingFunction.unpackRightChild(cscBinaryPopulatedColumns[i]);
            if (cscBinaryRightChildStartIndices[rightChild] < 0) {
                cscBinaryRightChildStartIndices[rightChild] = i;
            }
            cscBinaryRightChildEndIndices[rightChild] = i;
        }
    }

}
