package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

/**
 * Stores a sparse-matrix grammar in Java-Sparse-Array format (standard compressed-sparse-row with the exception that row lengths can vary, since Java stores 2-d arrays as arrays
 * of arrays)
 * 
 * Assumes less than 32767 total non-terminals (so that a production pair will fit into a signed 32-bit int)
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CsrSparseMatrixGrammar extends BaseSparseMatrixGrammar {

    private int[][] csrChildrenIndices;
    private float[][] probabilities;
    public final int leftChildShift;
    private final int rightChildShift;
    private final int mask;

    private IntOpenHashSet validProductionPairs;

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

        validProductionPairs = new IntOpenHashSet(50000);

        final Int2FloatOpenHashMap[] maps = new Int2FloatOpenHashMap[numNonTerms()];
        csrChildrenIndices = new int[numNonTerms()][];
        probabilities = new float[numNonTerms()][];
        for (int i = 0; i < numNonTerms(); i++) {
            maps[i] = new Int2FloatOpenHashMap(1000);
        }

        for (final Production p : binaryProductions) {
            maps[p.parent].put(pack(p.leftChild, (short) p.rightChild), p.prob);
            validProductionPairs.add(pack(p.leftChild, (short) p.rightChild));
        }

        for (int parent = 0; parent < numNonTerms(); parent++) {

            csrChildrenIndices[parent] = maps[parent].keySet().toIntArray();
            Arrays.sort(csrChildrenIndices[parent]);
            probabilities[parent] = new float[csrChildrenIndices[parent].length];

            for (int j = 0; j < csrChildrenIndices[parent].length; j++) {
                probabilities[parent][j] = maps[parent].get(csrChildrenIndices[parent][j]);
            }
        }
    }

    public CsrSparseMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
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

    public final int[] children(final int parent) {
        return csrChildrenIndices[parent];
    }

    public final float[] probabilities(final int parent) {
        return probabilities[parent];
    }

    public boolean isValidProductionPair(final short leftChild, final short rightChild) {
        return isValidProductionPair(pack(leftChild, rightChild));
    }

    public boolean isValidProductionPair(final int children) {
        return validProductionPairs.contains(children);
    }

    public int validProductionPairs() {
        return validProductionPairs.size();
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
        final int[] rowIndices = csrChildrenIndices[parent];
        final float[] rowProbabilities = probabilities[parent];

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
        sb.append("Valid production pairs: " + validProductionPairs.size() + '\n');
        sb.append("Valid left children: " + (numNonTerms() - posStart) + '\n');
        sb.append("Valid right children: " + leftChildOnlyStart + '\n');

        sb.append("Max left child: " + (numNonTerms() - 1) + '\n');
        sb.append("Max right child: " + (leftChildOnlyStart - 1) + '\n');

        return sb.toString();
    }

}
