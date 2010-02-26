package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;

import java.io.FileReader;
import java.io.Reader;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

/**
 * Stores a grammar as a sparse matrix of probabilities.
 * 
 * Matrix columns are indexed by production index (an int).
 * 
 * Matrix rows are indexed by the concatenation of left and right child indices (two 32-bit ints concatenated into a long).
 * 
 * TODO Generalize into an abstract superclass and a subclass implemented with a hash (allowing alternate sparse matrix implementations)
 * 
 * @author Aaron Dunlop
 * @since Dec 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class HashSparseMatrixGrammar extends Grammar {

    /** Binary productions, stored as a matrix */
    private Long2FloatOpenHashMap[] binaryRuleMatrix;

    private long[][] keys;

    /** Unary productions, stored in the order read in from the grammar file */
    public Production[] unaryProds;

    public HashSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    public HashSparseMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    @Override
    protected void init(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super.init(grammarFile, lexiconFile, grammarFormat);

        unaryProds = unaryProductions.toArray(new Production[unaryProductions.size()]);

        binaryRuleMatrix = new Long2FloatOpenHashMap[numNonTerms()];
        for (int i = 0; i < numNonTerms(); i++) {
            binaryRuleMatrix[i] = new Long2FloatOpenHashMap();
            binaryRuleMatrix[i].defaultReturnValue(Float.NEGATIVE_INFINITY);
        }

        for (final Production p : binaryProductions) {
            binaryRuleMatrix[p.parent].put(key(p.leftChild, p.rightChild), p.prob);
        }

        keys = new long[numNonTerms()][];
        for (int i = 0; i < binaryRuleMatrix.length; i++) {
            keys[i] = binaryRuleMatrix[i].keySet().toLongArray();
        }
    }

    private long key(final int leftChild, final int rightChild) {
        return ((long) leftChild) << 32 | rightChild;
    }

    public final Long2FloatOpenHashMap row(final int i) {
        return binaryRuleMatrix[i];
    }

    public final long[] rowKeys(final int i) {
        return keys[i];
    }

    @Override
    public final float lexicalLogProbability(final String parent, final String child) throws Exception {
        return super.lexicalLogProbability(parent, child);
    }

    @Override
    public final float logProbability(final String parent, final String leftChild, final String rightChild) throws Exception {
        final int parentIndex = nonTermSet.getIndex(parent);
        final int leftChildIndex = nonTermSet.getIndex(leftChild);
        final int rightChildIndex = nonTermSet.getIndex(rightChild);

        return logProbability(parentIndex, leftChildIndex, rightChildIndex);
    }

    public final float logProbability(final int parent, final int leftChild, final int rightChild) {
        return binaryRuleMatrix[parent].get(key(leftChild, rightChild));
    }

    public final float logProbability(final int parent, final long children) {
        return binaryRuleMatrix[parent].get(children);
    }

    @Override
    public final float logProbability(final String parent, final String child) throws Exception {
        return super.logProbability(parent, child);
    }

}
