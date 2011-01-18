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
 * TODO When we get back to CSR (for parallelization?): Store 2 separate matrices; one for span = 2 and one for span >
 * 2. They might be nearly disjoint, since span = 2 means both children are pre-terminals, and we assume the set of
 * multi-word constituents is disjoint from the set of pre-terminals, but the span = 2 matrix has to include any
 * children which occur as unary parents, since those NTs might be found in span-1 cells. It should still shrink the
 * 'main' chart matrix considerably, and thus save time iterating over the ruleset.
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CsrSparseMatrixGrammar extends SparseMatrixGrammar {

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

    public CsrSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws IOException {
        super(grammarFile, cartesianProductFunctionClass);

        // Bin all binary rules by parent, mapping packed children -> probability
        this.csrBinaryRowIndices = new int[numNonTerms() + 1];
        this.csrBinaryColumnIndices = new int[numBinaryProds()];
        this.csrBinaryProbabilities = new float[numBinaryProds()];

        storeBinaryRulesAsCsrMatrix(csrBinaryRowIndices, csrBinaryColumnIndices, csrBinaryProbabilities);
    }

    public CsrSparseMatrixGrammar(final Reader grammarFile) throws IOException {
        this(grammarFile, null);
    }

    public CsrSparseMatrixGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    public CsrSparseMatrixGrammar(final Grammar g, final Class<? extends CartesianProductFunction> functionClass) {
        super(g, functionClass);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.csrBinaryRowIndices = new int[numNonTerms() + 1];
        this.csrBinaryColumnIndices = new int[numBinaryProds()];
        this.csrBinaryProbabilities = new float[numBinaryProds()];

        storeBinaryRulesAsCsrMatrix(csrBinaryRowIndices, csrBinaryColumnIndices, csrBinaryProbabilities);
    }

    public CsrSparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final Class<? extends CartesianProductFunction> functionClass) {
        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                functionClass);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.csrBinaryRowIndices = new int[numNonTerms() + 1];
        this.csrBinaryColumnIndices = new int[numBinaryProds()];
        this.csrBinaryProbabilities = new float[numBinaryProds()];

        storeBinaryRulesAsCsrMatrix(csrBinaryRowIndices, csrBinaryColumnIndices, csrBinaryProbabilities);
    }

    protected void storeBinaryRulesAsCsrMatrix(final int[] csrRowIndices, final int[] csrColumnIndices,
            final float[] csrProbabilities) {

        // Bin all rules by parent, mapping packed children -> probability
        final Int2FloatOpenHashMap[] maps1 = new Int2FloatOpenHashMap[numNonTerms()];
        for (int i1 = 0; i1 < numNonTerms(); i1++) {
            maps1[i1] = new Int2FloatOpenHashMap(1000);
        }

        for (final Production p : binaryProductions) {
            maps1[p.parent].put(cartesianProductFunction.pack((short) p.leftChild, (short) p.rightChild), p.prob);
        }
        final Int2FloatOpenHashMap[] maps = maps1;

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
