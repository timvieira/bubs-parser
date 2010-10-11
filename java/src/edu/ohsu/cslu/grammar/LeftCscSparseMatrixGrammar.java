package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.IOException;
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
     * Indices in {@link #cscBinaryPopulatedColumns} of initial columns for each non-terminal. Indexed by left
     * non-terminal. Length is 1 greater than V, to simplify loops.
     */
    public final int[] cscBinaryLeftChildStartIndices;

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of final columns for each non-terminal. Indexed by left
     * non-terminal. Length is 1 greater than V, to simplify loops.
     */
    public final int[] cscBinaryLeftChildEndIndices;

    public LeftCscSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws IOException {
        super(grammarFile, cartesianProductFunctionClass);

        this.cscBinaryLeftChildStartIndices = new int[numNonTerms() + 1];
        this.cscBinaryLeftChildEndIndices = new int[numNonTerms() + 1];
        init();
    }

    public LeftCscSparseMatrixGrammar(final Reader grammarFile) throws IOException {
        this(grammarFile, null);
    }

    public LeftCscSparseMatrixGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    public LeftCscSparseMatrixGrammar(final Grammar g, final Class<? extends CartesianProductFunction> functionClass) {
        super(g, functionClass);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.cscBinaryLeftChildStartIndices = new int[numNonTerms() + 1];
        this.cscBinaryLeftChildEndIndices = new int[numNonTerms() + 1];
        init();
    }

    public LeftCscSparseMatrixGrammar(final Grammar g) {
        this(g, PerfectIntPairHashFilterFunction.class);
    }

    private void init() {
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

}
