package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

/**
 * Stores a sparse-matrix grammar in Java-Sparse-Array format (similar to standard compressed-sparse-row (CSR)
 * format with the exception that row lengths can vary, since Java stores 2-d arrays as arrays of arrays)
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (see {@link SparseMatrixGrammar} documentation for
 * details).
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class JsaSparseMatrixGrammar extends SparseMatrixGrammar {

    /** Binary rules. An array of int[] indexed by the parent non-terminal. */
    private int[][] jsaBinaryRules;

    /** Binary rule probabilities. An array of float[] indexed by the parent non-terminal. */
    private float[][] jsaBinaryProbabilities;

    /** Unary rules. An array of int[] indexed by the parent non-terminal. */
    private int[][] jsaUnaryRules;

    /** Unary rule probabilities. An array of float[] indexed by the parent non-terminal. */
    private float[][] jsaUnaryProbabilities;

    public JsaSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat, cartesianProductFunctionClass);

        // Bin all binary rules by parent, mapping packed children -> probability
        jsaBinaryRules = new int[numNonTerms()][];
        jsaBinaryProbabilities = new float[numNonTerms()][];
        storeRulesAsMatrix(binaryProductions, jsaBinaryRules, jsaBinaryProbabilities);

        // And all unary rules
        jsaUnaryRules = new int[numNonTerms()][];
        jsaUnaryProbabilities = new float[numNonTerms()][];
        storeRulesAsMatrix(unaryProductions, jsaUnaryRules, jsaUnaryProbabilities);

        tokenizer = new Tokenizer(lexSet);
    }

    public JsaSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(grammarFile, lexiconFile, grammarFormat, null);
    }

    public JsaSparseMatrixGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    private void storeRulesAsMatrix(final Collection<Production> productions, final int[][] productionMatrix,
            final float[][] probabilityMatrix) {

        final Int2FloatOpenHashMap[] maps = mapRulesByParent(productions);

        // Store rules in parent bins, sorted by packed children
        for (int parent = 0; parent < numNonTerms(); parent++) {

            productionMatrix[parent] = maps[parent].keySet().toIntArray();
            Arrays.sort(productionMatrix[parent]);
            probabilityMatrix[parent] = new float[productionMatrix[parent].length];

            for (int j = 0; j < productionMatrix[parent].length; j++) {
                probabilityMatrix[parent][j] = maps[parent].get(productionMatrix[parent][j]);
            }
        }
    }

    public final int[][] binaryRuleMatrix() {
        return jsaBinaryRules;
    }

    public final float[][] binaryProbabilities() {
        return jsaBinaryProbabilities;
    }

    public final int[][] unaryRuleMatrix() {
        return jsaUnaryRules;
    }

    public final float[][] unaryProbabilities() {
        return jsaUnaryProbabilities;
    }

    @Override
    public final float binaryLogProbability(final int parent, final int children) {
        final int[] rowIndices = jsaBinaryRules[parent];
        final float[] rowProbabilities = jsaBinaryProbabilities[parent];

        for (int i = 0; i < rowIndices.length; i++) {
            final int c = rowIndices[i];
            if (c == children) {
                return rowProbabilities[i];
            }
            if (c > children) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public final float unaryLogProbability(final int parent, final int child) {
        final short rightChildIndex = Production.UNARY_PRODUCTION;
        final int children = cartesianProductFunction.pack(child, rightChildIndex);

        final int[] rowIndices = jsaUnaryRules[parent];
        final float[] rowProbabilities = jsaUnaryProbabilities[parent];

        for (int i = 0; i < rowIndices.length; i++) {
            final int c = rowIndices[i];
            if (c == children) {
                return rowProbabilities[i];
            }
            if (c > children) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }
}
