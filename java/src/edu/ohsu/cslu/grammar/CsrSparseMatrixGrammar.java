package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

/**
 * Stores a sparse-matrix grammar in standard compressed-sparse-row (CSR) format
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (see {@link BaseSparseMatrixGrammar} documentation for details).
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CsrSparseMatrixGrammar extends BaseSparseMatrixGrammar {

    /**
     * Offsets into {@link #csrBinaryRowIndices} for the start of each row, indexed by row index (non-terminals), with one extra entry appended to prevent loops from falling off
     * the end
     */
    private int[] csrBinaryRowIndices;

    /** Column indices of each matrix entry in {@link #csrBinaryProbabilities}. Indexed by packed children */
    private int[] csrBinaryColumnIndices;

    /** Binary rule probabilities */
    private float[] csrBinaryProbabilities;

    /** Offsets into {@link #csrUnaryRowIndices} for the start of each row, indexed by row index (non-terminals) */
    private int[] csrUnaryRowIndices;

    /** Column indices of each matrix entry in {@link #csrUnaryProbabilities}. Indexed by packed children */
    private int[] csrUnaryColumnIndices;

    /** Binary rule probabilities */
    private float[] csrUnaryProbabilities;

    public CsrSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        super(grammarFile, lexiconFile, grammarFormat);

        // Bin all binary rules by parent, mapping packed children -> probability
        csrBinaryRowIndices = new int[numNonTerms() + 1];
        csrBinaryColumnIndices = new int[numBinaryRules()];
        csrBinaryProbabilities = new float[numBinaryRules()];

        storeRulesAsMatrix(binaryProductions, csrBinaryRowIndices, csrBinaryColumnIndices, csrBinaryProbabilities);

        // And all unary rules
        csrUnaryRowIndices = new int[numNonTerms() + 1];
        csrUnaryColumnIndices = new int[numUnaryRules()];
        csrUnaryProbabilities = new float[numUnaryRules()];

        storeRulesAsMatrix(unaryProductions, csrUnaryRowIndices, csrUnaryColumnIndices, csrUnaryProbabilities);

        tokenizer = new Tokenizer(lexSet);
    }

    public CsrSparseMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    private void storeRulesAsMatrix(final Collection<Production> productions, final int[] csrRowIndices, final int[] csrColumnIndices, final float[] csrProbabilities) {

        final Int2FloatOpenHashMap[] maps = mapRules(productions);

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

    public final int[] binaryRuleMatrixRowIndices() {
        return csrBinaryRowIndices;
    }

    public final int[] binaryRuleMatrixColumnIndices() {
        return csrBinaryColumnIndices;
    }

    public final float[] binaryRuleMatrixProbabilities() {
        return csrBinaryProbabilities;
    }

    public final int[] unaryRuleMatrixRowIndices() {
        return csrUnaryRowIndices;
    }

    public final int[] unaryRuleMatrixColumnIndices() {
        return csrUnaryColumnIndices;
    }

    public final float[] unaryRuleMatrixProbabilities() {
        return csrUnaryProbabilities;
    }

    @Override
    public final float binaryLogProbability(final int parent, final int children) {

        for (int i = csrBinaryRowIndices[parent]; i < csrBinaryRowIndices[parent + 1]; i++) {
            final int column = csrBinaryColumnIndices[i];
            if (column == children) {
                return csrBinaryProbabilities[i];
            }
            if (column > children) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public final float unaryLogProbability(final int parent, final int child) {
        final int children = pack(child, (short) Production.UNARY_PRODUCTION);

        for (int i = csrUnaryRowIndices[parent]; i <= csrUnaryRowIndices[parent + 1]; i++) {
            final int column = csrUnaryColumnIndices[i];
            if (column == children) {
                return csrUnaryProbabilities[i];
            }
            if (column > children) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }
}
