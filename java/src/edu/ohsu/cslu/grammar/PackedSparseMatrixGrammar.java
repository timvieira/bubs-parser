package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

/**
 * Packs a sparse-matrix grammar into an array of longs
 * 
 * @author Aaron Dunlop
 * @since Dec 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class PackedSparseMatrixGrammar extends BaseSparseMatrixGrammar {

    /** Unary productions, stored in the order read in from the grammar file */
    public Production[] unaryProds;

    /** Binary productions, stored as a long followed by a float using {@link Float#floatToIntBits(float)} */
    private long[][] entries;

    private LongOpenHashSet validProductionPairs;

    private IntOpenHashSet validLeftChildren;

    private IntOpenHashSet validRightChildren;

    public PackedSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        super(grammarFile, lexiconFile, grammarFormat);

        validProductionPairs = new LongOpenHashSet(50000);
        validLeftChildren = new IntOpenHashSet(5000);
        validRightChildren = new IntOpenHashSet(5000);

        final Long2FloatOpenHashMap[] maps = new Long2FloatOpenHashMap[numNonTerms()];
        entries = new long[numNonTerms()][];
        for (int i = 0; i < numNonTerms(); i++) {
            maps[i] = new Long2FloatOpenHashMap(1000);
        }

        for (final Production p : binaryProductions) {
            maps[p.parent].put(pack(p.leftChild, p.rightChild), p.prob);
            validProductionPairs.add(pack(p.leftChild, p.rightChild));
            validLeftChildren.add(p.leftChild);
            validRightChildren.add(p.rightChild);
        }

        for (int parent = 0; parent < numNonTerms(); parent++) {

            final long[] children = maps[parent].keySet().toLongArray();
            Arrays.sort(children);
            entries[parent] = new long[children.length * 2];

            for (int j = 0; j < children.length; j++) {
                entries[parent][j * 2] = children[j];
                entries[parent][j * 2 + 1] = pack(0, maps[parent].get(children[j]));
            }
        }

        unaryProds = unaryProductions.toArray(new Production[unaryProductions.size()]);
        tokenizer = new Tokenizer(lexSet);
    }

    public PackedSparseMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    private long pack(final int leftChild, final int rightChild) {
        return ((long) leftChild) << 32 | rightChild;
    }

    private final static long pack(final int i, final float f) {
        return ((long) i) << 32 | (Float.floatToIntBits(f) & 0xffffffffl);
    }

    public long[] entries(final int parent) {
        return entries[parent];
    }

    public boolean isValidProductionPair(final long children) {
        return validProductionPairs.contains(children);
    }

    public boolean isValidProductionPair(final int leftChild, final int rightChild) {
        return isValidProductionPair(pack(leftChild, rightChild));
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
        return logProbability(parent, pack(leftChild, rightChild));
    }

    public final float logProbability(final int parent, final long children) {
        final long[] row = entries[parent];
        for (int i = 0; i < row.length; i = i + 2) {
            final long c = row[i];
            if (c == children) {
                return Float.intBitsToFloat((int) row[i + 1]);
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
        sb.append("Valid left children: " + validLeftChildren.size() + '\n');
        sb.append("Valid right children: " + validRightChildren.size() + '\n');

        int maxLeftChild = 0;
        for (final int leftChild : validLeftChildren) {
            if (leftChild > maxLeftChild) {
                maxLeftChild = leftChild;
            }
        }

        int maxRightChild = 0;
        for (final int rightChild : validRightChildren) {
            if (rightChild > maxRightChild) {
                maxRightChild = rightChild;
            }
        }

        sb.append("Max left child: " + maxLeftChild + '\n');
        sb.append("Max right child: " + maxRightChild + '\n');

        return sb.toString();
    }
}
