package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;

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
public class RightCscSparseMatrixGrammar extends CscSparseMatrixGrammar {

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of initial columns for each non-terminal, or -1 for
     * non-terminals which do not occur as left children. Indexed by left non-terminal. Length is 1 greater
     * than V, to simplify loops.
     */
    public final int[] cscBinaryRightChildStartIndices;

    /**
     * Indices in {@link #cscBinaryPopulatedColumns} of final columns for each non-terminal, or -1 for
     * non-terminals which do not occur as left children. Indexed by left non-terminal. Length is 1 greater
     * than V, to simplify loops.
     */
    public final int[] cscBinaryRightChildEndIndices;

    public RightCscSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat, RightShiftFunction.class);

        cscBinaryRightChildStartIndices = new int[numNonTerms() + 1];
        cscBinaryRightChildEndIndices = new int[numNonTerms() + 1];
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

    public RightCscSparseMatrixGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }
}
