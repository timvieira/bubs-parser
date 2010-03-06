package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.io.FileReader;
import java.io.Reader;
import java.util.Collection;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

/**
 * Stores a grammar as a sparse matrix of probabilities.
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (so that a production pair will fit into a signed 32-bit int). This limit _may_ still allow more than 2^15 total
 * non-terminals, depending on the grammar's factorization. In general, we assume a left-factored grammar with many fewer valid right child productions than left children.
 * 
 * @author Aaron Dunlop
 * @since Dec 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */

public abstract class SparseMatrixGrammar extends SortedGrammar {

    // Shift lengths and mask for packing and unpacking non-terminals into an int
    public final int leftChildShift;
    public final int rightChildShift;
    public final int mask;
    protected final int validProductionPairs;

    public SparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);

        // Add 1 bit to leave empty for sign
        leftChildShift = edu.ohsu.cslu.util.Math.logBase2(leftChildOnlyStart) + 1;
        rightChildShift = 32 - leftChildShift;
        int m = 0;
        for (int i = 0; i < leftChildShift; i++) {
            m = m << 1 | 1;
        }
        mask = m;

        final IntOpenHashSet productionPairs = new IntOpenHashSet(50000);

        for (final Production p : binaryProductions) {
            productionPairs.add(pack(p.leftChild, (short) p.rightChild));
        }
        validProductionPairs = productionPairs.size();
    }

    public SparseMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    /**
     * Returns the log probability of the specified parent / child production
     * 
     * @param parent Parent index
     * @param children Packed children
     * @return Log probability
     */
    public abstract float binaryLogProbability(final int parent, final int children);

    @Override
    public final float binaryLogProbability(final int parent, final int leftChild, final int rightChild) {
        return binaryLogProbability(parent, pack(leftChild, (short) rightChild));
    }

    /**
     * Returns the log probability of the specified parent / child production
     * 
     * @param parent Parent index
     * @param child Child index
     * @return Log probability
     */
    @Override
    public abstract float unaryLogProbability(final int parent, final int child);

    /**
     * Returns all rules as an array of maps, indexed by parent, each of which maps the packed children to the probability.
     * 
     * @param productions Rules to be mapped
     * @return Array of maps from children -> probability
     */
    protected Int2FloatOpenHashMap[] mapRules(final Collection<Production> productions) {
        // Bin all rules by parent, mapping packed children -> probability
        final Int2FloatOpenHashMap[] maps = new Int2FloatOpenHashMap[numNonTerms()];
        for (int i = 0; i < numNonTerms(); i++) {
            maps[i] = new Int2FloatOpenHashMap(1000);
        }

        for (final Production p : productions) {
            maps[p.parent].put(pack(p.leftChild, (short) p.rightChild), p.prob);
        }
        return maps;
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
