package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

/**
 * Stores a sparse-matrix grammar in Java-Sparse-Array format (standard compressed-sparse-row with the exception that row lengths can vary, since Java stores 2-d arrays as arrays
 * of arrays)
 * 
 * Assumes less than 2^30 total non-terminals combinations (so that a production pair will fit into a signed 32-bit int). This limit _may_ still allow more than 2^15 total
 * non-terminals, depending on the grammar's factorization. In general, we assume a left-factored grammar with many fewer valid right child productions than left children.
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CsrSparseMatrixGrammar extends BaseSparseMatrixGrammar {

    /** Unary productions, stored in the order read in from the grammar file */
    public final Production[] unaryProds;

    /** Binary rules */
    private int[][] csrBinaryRules;

    /** Binary rule probabilities */
    private float[][] csrBinaryProbabilities;

    /** Binary rules */
    private int[][] csrUnaryRules;

    /** Binary rule probabilities */
    private float[][] csrUnaryProbabilities;

    // Shift lengths and mask for packing and unpacking non-terminals into an int
    private final int leftChildShift;
    private final int rightChildShift;
    private final int mask;

    private final int validProductionPairs;

    public CsrSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        super(grammarFile, lexiconFile, grammarFormat);

        // Add 1 bit to leave empty for sign
        leftChildShift = edu.ohsu.cslu.util.Math.logBase2(leftChildOnlyStart) + 1;
        rightChildShift = 32 - leftChildShift;
        int m = 0;
        for (int i = 0; i < leftChildShift; i++) {
            m = m << 1 | 1;
        }
        mask = m;

        // Bin all binary rules by parent, mapping packed children -> probability
        csrBinaryRules = new int[numNonTerms()][];
        csrBinaryProbabilities = new float[numNonTerms()][];
        validProductionPairs = storeRulesAsMatrix(binaryProductions, csrBinaryRules, csrBinaryProbabilities);

        // And all unary rules
        csrUnaryRules = new int[numNonTerms()][];
        csrUnaryProbabilities = new float[numNonTerms()][];
        storeRulesAsMatrix(unaryProductions, csrUnaryRules, csrUnaryProbabilities);

        unaryProds = unaryProductions.toArray(new Production[unaryProductions.size()]);
        tokenizer = new Tokenizer(lexSet);
    }

    private int storeRulesAsMatrix(final Collection<Production> productions, final int[][] productionMatrix, final float[][] probabilityMatrix) {
        final IntOpenHashSet productionPairs = new IntOpenHashSet(50000);

        // Bin all binary rules by parent, mapping packed children -> probability
        final Int2FloatOpenHashMap[] maps = new Int2FloatOpenHashMap[numNonTerms()];
        for (int i = 0; i < numNonTerms(); i++) {
            maps[i] = new Int2FloatOpenHashMap(1000);
        }

        for (final Production p : productions) {
            maps[p.parent].put(pack(p.leftChild, (short) p.rightChild), p.prob);
            productionPairs.add(pack(p.leftChild, (short) p.rightChild));
        }

        // Store rules in parent bins, sorted by packed children
        for (int parent = 0; parent < numNonTerms(); parent++) {

            productionMatrix[parent] = maps[parent].keySet().toIntArray();
            Arrays.sort(productionMatrix[parent]);
            probabilityMatrix[parent] = new float[productionMatrix[parent].length];

            for (int j = 0; j < productionMatrix[parent].length; j++) {
                probabilityMatrix[parent][j] = maps[parent].get(productionMatrix[parent][j]);
            }
        }
        return productionPairs.size();
    }

    public CsrSparseMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    public final int packedArraySize() {
        return numNonTerms() << leftChildShift;
    }

    public final int pack(final int leftChild, final short rightChild) {
        return leftChild << leftChildShift | (rightChild & mask);
    }

    public final int unpackLeftChild(final int children) {
        return children >>> leftChildShift;
    }

    public final short unpackRightChild(final int children) {
        return (short) ((children << rightChildShift) >> rightChildShift);
    }

    public final int[][] binaryRuleMatrix() {
        return csrBinaryRules;
    }

    public final float[][] binaryProbabilities() {
        return csrBinaryProbabilities;
    }

    public final int[][] unaryRuleMatrix() {
        return csrUnaryRules;
    }

    public final float[][] unaryProbabilities() {
        return csrUnaryProbabilities;
    }

    @Override
    public final float logProbability(final String parent, final String leftChild, final String rightChild) {
        final int parentIndex = nonTermSet.getIndex(parent);
        final int leftChildIndex = nonTermSet.getIndex(leftChild);
        final int rightChildIndex = nonTermSet.getIndex(rightChild);

        return logProbability(parentIndex, leftChildIndex, rightChildIndex);
    }

    public final float logProbability(final int parent, final int leftChild, final int rightChild) {
        return logProbability(parent, pack(leftChild, (short) rightChild));
    }

    public final float logProbability(final int parent, final int children) {
        final int[] rowIndices = csrBinaryRules[parent];
        final float[] rowProbabilities = csrBinaryProbabilities[parent];

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
    public float logProbability(final String parent, final String child) {
        final int parentIndex = nonTermSet.getIndex(parent);
        final int leftChildIndex = nonTermSet.getIndex(child);
        final short rightChildIndex = Production.UNARY_PRODUCTION;

        final int children = pack(leftChildIndex, rightChildIndex);

        final int[] rowIndices = csrUnaryRules[parentIndex];
        final float[] rowProbabilities = csrUnaryProbabilities[parentIndex];

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
    public String getStats() {
        final StringBuilder sb = new StringBuilder(1024);
        sb.append(super.getStats());
        sb.append("Valid production pairs: " + validProductionPairs + '\n');
        sb.append("Valid left children: " + (numNonTerms() - posStart) + '\n');
        sb.append("Valid right children: " + leftChildOnlyStart + '\n');

        sb.append("Max left child: " + (numNonTerms() - 1) + '\n');
        sb.append("Max right child: " + (leftChildOnlyStart - 1) + '\n');

        return sb.toString();
    }

}
