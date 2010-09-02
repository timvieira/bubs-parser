package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;

/**
 * Stores a sparse-matrix grammar in compressed-sparse-column (CSC) format
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (see {@link SparseMatrixGrammar} documentation for details).
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class LeftCscSparseMatrixGrammar extends CscSparseMatrixGrammar {

	/**
	 * Indices in {@link #cscBinaryPopulatedColumns} of initial columns for each non-terminal, or -1 for non-terminals which do not occur as left children. Indexed by left
	 * non-terminal. Length is 1 greater than V, to simplify loops.
	 */
	public final int[] cscBinaryLeftChildStartIndices;

	/**
	 * Indices in {@link #cscBinaryPopulatedColumns} of final columns for each non-terminal, or -1 for non-terminals which do not occur as left children. Indexed by left
	 * non-terminal. Length is 1 greater than V, to simplify loops.
	 */
	public final int[] cscBinaryLeftChildEndIndices;

	public LeftCscSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat,
			final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws Exception {
		super(grammarFile, lexiconFile, grammarFormat, cartesianProductFunctionClass);

		cscBinaryLeftChildStartIndices = new int[numNonTerms() + 1];
		cscBinaryLeftChildEndIndices = new int[numNonTerms() + 1];
		Arrays.fill(cscBinaryLeftChildStartIndices, -1);
		Arrays.fill(cscBinaryLeftChildEndIndices, -1);

		for (int i = 0; i < cscBinaryPopulatedColumns.length; i++) {
			final int leftChild = cartesianProductFunction.unpackLeftChild(cscBinaryPopulatedColumns[i]);
			if (cscBinaryLeftChildStartIndices[leftChild] < 0) {
				cscBinaryLeftChildStartIndices[leftChild] = i;
			}
			cscBinaryLeftChildEndIndices[leftChild] = i;
		}
	}

	public LeftCscSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
		this(grammarFile, lexiconFile, grammarFormat, null);
	}

	public LeftCscSparseMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
		this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
	}
}
