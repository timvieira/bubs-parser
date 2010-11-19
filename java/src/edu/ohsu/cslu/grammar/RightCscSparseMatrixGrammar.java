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
public class RightCscSparseMatrixGrammar extends CscSparseMatrixGrammar {

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of initial columns for each non-terminal, or -1 for non-terminals
     * which do not occur as left children. Indexed by left non-terminal. Length is 1 greater than V, to simplify loops.
     */
    public final int[] cscBinaryRightChildStartIndices;

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of final columns for each non-terminal, or -1 for non-terminals
     * which do not occur as left children. Indexed by left non-terminal. Length is 1 greater than V, to simplify loops.
     */
    public final int[] cscBinaryRightChildEndIndices;

    public RightCscSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws Exception {
        super(grammarFile, cartesianProductFunctionClass);

        this.cscBinaryRightChildStartIndices = new int[numNonTerms() + 1];
        this.cscBinaryRightChildEndIndices = new int[numNonTerms() + 1];
        init();
    }

    public RightCscSparseMatrixGrammar(final Reader grammarFile) throws Exception {
        this(grammarFile, RightShiftFunction.class);
    }

    public RightCscSparseMatrixGrammar(final String grammarFile) throws Exception {
        this(new FileReader(grammarFile));
    }

    public RightCscSparseMatrixGrammar(final Grammar g, final Class<? extends CartesianProductFunction> functionClass) {
        super(g, functionClass);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.cscBinaryRightChildStartIndices = new int[numNonTerms() + 1];
        this.cscBinaryRightChildEndIndices = new int[numNonTerms() + 1];
        init();
    }

    private void init() {
        Arrays.fill(cscBinaryRightChildStartIndices, -1);
        Arrays.fill(cscBinaryRightChildEndIndices, -1);

        for (int i = 0; i < cscBinaryPopulatedColumns.length; i++) {
            final int rightChild = cartesianProductFunction.unpackRightChild(cscBinaryPopulatedColumns[i]);
            if (cscBinaryRightChildStartIndices[rightChild] < 0) {
                cscBinaryRightChildStartIndices[rightChild] = i;
            }
            cscBinaryRightChildEndIndices[rightChild] = i;
        }
    }

}
