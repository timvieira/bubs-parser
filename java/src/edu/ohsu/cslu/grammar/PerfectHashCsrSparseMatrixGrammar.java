package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.BitVectorExactFilterFunction;
import edu.ohsu.cslu.hash.PerfectHash;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

/**
 * Stores a sparse-matrix grammar in compressed-sparse-row (CSR) format. Computes a perfect (i.e.
 * collision-free) hash over all observed child pairs, permitting compact storage of the cartesian-product
 * vector and exact filtering in comparable space to {@link BitVectorExactFilterFunction}.
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (see {@link SparseMatrixGrammar} documentation for
 * details).
 * 
 * @author Aaron Dunlop
 * @since Jan 24, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class PerfectHashCsrSparseMatrixGrammar extends SparseMatrixGrammar {

    /**
     * Offsets into {@link #csrHashedBinaryColumnIndices} for the start of each row, indexed by row index
     * (non-terminals), with one extra entry appended to prevent loops from falling off the end
     */
    private int[] csrBinaryRowIndices;

    /**
     * Hashed column indices of each matrix entry in {@link #csrBinaryProbabilities}. One entry for each
     * binary rule; the same size as {@link #csrBinaryProbabilities}.
     */
    private int[] csrHashedBinaryColumnIndices;

    /**
     * Binary rule probabilities. One entry for each binary rule. The same size as
     * {@link #csrHashedBinaryColumnIndices}.
     */
    private float[] csrBinaryProbabilities;

    /**
     * Offsets into {@link #csrUnaryColumnIndices} for the start of each row, indexed by row index
     * (non-terminals)
     */
    private int[] csrUnaryRowIndices;

    /**
     * Column indices of each matrix entry in {@link #csrUnaryProbabilities}. One entry for each unary rule;
     * the same size as {@link #csrUnaryProbabilities}.
     */
    private int[] csrUnaryColumnIndices;

    /** Unary rule probabilities */
    private float[] csrUnaryProbabilities;

    public PerfectHashCsrSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat, PerfectHashFilterFunction.class);

        // Bin all binary rules by parent, mapping packed children -> probability
        csrBinaryRowIndices = new int[numNonTerms() + 1];
        csrHashedBinaryColumnIndices = new int[numBinaryRules()];
        csrBinaryProbabilities = new float[numBinaryRules()];

        storeRulesAsMatrix(binaryProductions, csrBinaryRowIndices, csrHashedBinaryColumnIndices,
            csrBinaryProbabilities);

        // And all unary rules
        csrUnaryRowIndices = new int[numNonTerms() + 1];
        csrUnaryColumnIndices = new int[numUnaryRules()];
        csrUnaryProbabilities = new float[numUnaryRules()];

        storeRulesAsMatrix(unaryProductions, csrUnaryRowIndices, csrUnaryColumnIndices, csrUnaryProbabilities);

        tokenizer = new Tokenizer(lexSet);
    }

    public PerfectHashCsrSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(grammarFile, lexiconFile, grammarFormat, null);
    }

    public PerfectHashCsrSparseMatrixGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    private void storeRulesAsMatrix(final Collection<Production> productions, final int[] csrRowIndices,
            final int[] csrColumnIndices, final float[] csrProbabilities) {

        final Int2FloatOpenHashMap[] maps = mapRulesByParent(productions);

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
        return csrHashedBinaryColumnIndices;
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
    public final float binaryLogProbability(final int parent, final int childPair) {

        for (int i = csrBinaryRowIndices[parent]; i < csrBinaryRowIndices[parent + 1]; i++) {
            final int column = csrHashedBinaryColumnIndices[i];
            if (column == childPair) {
                return csrBinaryProbabilities[i];
            }
            if (column > childPair) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public final float unaryLogProbability(final int parent, final int child) {
        final int children = cartesianProductFunction.packUnary(child);

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

    public final class PerfectHashFilterFunction extends ShiftFunction {

        private final int maxLexicalProduction = -numNonTerms() - 1;

        /** Perfect (collision-free) hash of all observed child pairs. */
        private final PerfectHash perfectHash;

        private final int packedArraySize;

        public PerfectHashFilterFunction() {
            super(leftChildOnlyStart);

            final IntSet childPairs = new IntOpenHashSet(binaryProductions.size());
            for (final Production p : binaryProductions) {
                childPairs.add(internalPack(p.leftChild, p.rightChild));
            }

            this.perfectHash = new PerfectHash(childPairs.toIntArray());
            System.out.println("Hashed grammar: " + perfectHash.toString());
            this.packedArraySize = perfectHash.hashtableSize();
        }

        @Override
        public final int pack(final int leftChild, final int rightChild) {
            // TODO Combine shifting and masking here with the shift and mask in unsafeHashcode
            return perfectHash.unsafeHashcode(leftChild << shift | (rightChild & lowOrderMask));
        }

        private int internalPack(final int leftChild, final int rightChild) {
            return leftChild << shift | (rightChild & lowOrderMask);
        }

        public final int packUnary(final int child) {
            return -child - 1;
        }

        public final int packLexical(final int child) {
            return maxLexicalProduction - child;
        }

        @Override
        public final int unpackLeftChild(final int packedChildPair) {
            if (packedChildPair < 0) {
                // Unary or lexical production
                if (packedChildPair <= maxLexicalProduction) {
                    // Lexical production
                    return -packedChildPair + maxLexicalProduction;
                }
                // Unary production
                return -packedChildPair - 1;
            }

            final int childPair = perfectHash.unsafeKey(packedChildPair);
            return childPair >> shift;
        }

        @Override
        public final int unpackRightChild(final int packedChildPair) {
            if (packedChildPair < 0) {
                // Unary or lexical production
                if (packedChildPair <= maxLexicalProduction) {
                    // Lexical production
                    return Production.LEXICAL_PRODUCTION;
                }
                // Unary production
                return Production.UNARY_PRODUCTION;
            }

            final int childPair = perfectHash.unsafeKey(packedChildPair);
            return childPair & lowOrderMask;
        }

        @Override
        public final int leftChildStart() {
            return posStart;
        }

        @Override
        public final int leftChildEnd() {
            return unaryChildOnlyStart;
        }

        @Override
        public final int rightChildStart() {
            return rightChildOnlyStart;
        }

        @Override
        public final int rightChildEnd() {
            return leftChildOnlyStart;
        }

        @Override
        public int packedArraySize() {
            return packedArraySize;
        }

        @Override
        public String openClPackDefine() {
            return "";
        }
    }
}
