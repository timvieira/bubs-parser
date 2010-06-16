package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.PackedBitVector;
import edu.ohsu.cslu.hash.PerfectInt2IntHash;
import edu.ohsu.cslu.hash.SegmentedPerfectIntPair2IntHash;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.util.Math;

/**
 * Stores a grammar as a sparse matrix of probabilities.
 * 
 * Assumes fewer than 2^15 total non-terminals (so that a production pair will fit into a signed 32-bit int).
 * 
 * Implementations may store the binary rule matrix in a variety of formats (e.g. CSR (
 * {@link CsrSparseMatrixGrammar}) or CSC ({@link LeftCscSparseMatrixGrammar})).
 * 
 * The unary rules are always stored in CSR format, with rows denoting rule parents and columns rule children.
 * We anticipate relatively few unary rules (the Berkeley grammar has only ~100k), and iteration order doesn't
 * matter greatly. CSC format would allow us to iterate through only the populated children, but CSR format
 * makes parallelization much simpler (e.g., we can execute one thread per row (parent); since we're only
 * updating the parent probability, we do not need to synchronize with other threads).
 * 
 * @author Aaron Dunlop
 * @since Dec 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */

public abstract class SparseMatrixGrammar extends SortedGrammar {

    protected final int validProductionPairs;

    protected final CartesianProductFunction cartesianProductFunction;

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

    @SuppressWarnings("unchecked")
    public SparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat, Class<? extends CartesianProductFunction> functionClass)
            throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);

        try {
            if (functionClass == null) {
                functionClass = DefaultFunction.class;
            }

            Constructor<CartesianProductFunction> c;
            try {
                c = (Constructor<CartesianProductFunction>) functionClass
                    .getConstructor(SparseMatrixGrammar.class);
            } catch (final NoSuchMethodException e) {
                c = (Constructor<CartesianProductFunction>) functionClass.getConstructor(getClass());
            }
            this.cartesianProductFunction = c.newInstance(this);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Some CartesianProductFunction implementation will duplicate this count, but it's not that expensive
        final IntOpenHashSet productionPairs = new IntOpenHashSet(50000);
        for (final Production p : binaryProductions) {
            productionPairs.add(cartesianProductFunction.pack(p.leftChild, p.rightChild));
        }
        validProductionPairs = productionPairs.size();
    }

    public SparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(grammarFile, lexiconFile, grammarFormat, DefaultFunction.class);
    }

    public SparseMatrixGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    /**
     * Returns the log probability of the specified parent / child production
     * 
     * @param parent Parent index
     * @param childPair Packed children
     * @return Log probability
     */
    public abstract float binaryLogProbability(final int parent, final int childPair);

    @Override
    public final float binaryLogProbability(final int parent, final int leftChild, final int rightChild) {
        return binaryLogProbability(parent, cartesianProductFunction.pack(leftChild, (short) rightChild));
    }

    /**
     * Returns the log probability of the specified parent / child production
     * 
     * @param parent Parent index
     * @param child Child index
     * @return Log probability
     */
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

    /**
     * Returns all rules as an array of maps, indexed by parent, each of which maps the packed children to the
     * probability.
     * 
     * @param productions Rules to be mapped
     * @return Array of maps from children -> probability
     */
    protected Int2FloatOpenHashMap[] mapRulesByParent(final Collection<Production> productions) {
        // Bin all rules by parent, mapping packed children -> probability
        final Int2FloatOpenHashMap[] maps = new Int2FloatOpenHashMap[numNonTerms()];
        for (int i = 0; i < numNonTerms(); i++) {
            maps[i] = new Int2FloatOpenHashMap(1000);
        }

        for (final Production p : productions) {
            if (p.isBinaryProd()) {
                maps[p.parent].put(cartesianProductFunction.pack(p.leftChild, p.rightChild), p.prob);
            } else if (p.isLexProd()) {
                maps[p.parent].put(cartesianProductFunction.packLexical(p.leftChild), p.prob);
            } else {
                maps[p.parent].put(cartesianProductFunction.packUnary(p.leftChild), p.prob);
            }
        }
        return maps;
    }

    /**
     * Returns all rules as an array of maps, indexed by child pair, each of which maps the parent to the
     * probability.
     * 
     * @param productions Rules to be mapped
     * @return Array of maps from children -> probability
     */
    protected Int2ObjectOpenHashMap<Int2FloatOpenHashMap> mapRulesByChildPairs(
            final Collection<Production> productions) {
        // Bin all rules by child pair, mapping parent -> probability
        final Int2ObjectOpenHashMap<Int2FloatOpenHashMap> maps = new Int2ObjectOpenHashMap<Int2FloatOpenHashMap>(
            1000);

        for (final Production p : productions) {
            int childPair;
            if (p.isBinaryProd()) {
                childPair = cartesianProductFunction.pack(p.leftChild, p.rightChild);
            } else if (p.isLexProd()) {
                childPair = cartesianProductFunction.packLexical(p.leftChild);
            } else {
                childPair = cartesianProductFunction.packUnary(p.leftChild);
            }
            Int2FloatOpenHashMap map = maps.get(childPair);
            if (map == null) {
                map = new Int2FloatOpenHashMap(20);
                maps.put(childPair, map);
            }
            map.put(p.parent, p.prob);
        }
        return maps;
    }

    protected void storeUnaryRules() {
        // And all unary rules
        csrUnaryRowIndices = new int[numNonTerms() + 1];
        csrUnaryColumnIndices = new int[numUnaryRules()];
        csrUnaryProbabilities = new float[numUnaryRules()];

        storeRulesAsCsrMatrix(unaryProductions, csrUnaryRowIndices, csrUnaryColumnIndices,
            csrUnaryProbabilities);
    }

    protected void storeRulesAsCsrMatrix(final Collection<Production> productions, final int[] csrRowIndices,
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

    public final int[] unaryRuleMatrixRowIndices() {
        return csrUnaryRowIndices;
    }

    public final int[] unaryRuleMatrixColumnIndices() {
        return csrUnaryColumnIndices;
    }

    public final float[] unaryRuleMatrixProbabilities() {
        return csrUnaryProbabilities;
    }

    /**
     * Returns the cartesian-product function in use
     * 
     * @return the cartesian-product function in use
     */
    public final CartesianProductFunction cartesianProductFunction() {
        return cartesianProductFunction;
    }

    @Override
    public final boolean isValidRightChild(final int nonTerminal) {
        return nonTerminal >= rightChildrenStart && nonTerminal <= rightChildrenEnd
                && nonTerminal != nullSymbol;
    }

    @Override
    public final boolean isValidLeftChild(final int nonTerminal) {
        return nonTerminal >= leftChildrenStart && nonTerminal <= leftChildrenEnd
                && nonTerminal != nullSymbol;
    }

    /**
     * Returns a string representation of all child pairs recognized by this grammar.
     * 
     * @return a string representation of all child pairs recognized by this grammar.
     */
    @Override
    public String recognitionMatrix() {

        final IntSet validChildPairs = new IntOpenHashSet(binaryProductions.size() / 2);
        for (final Production p : binaryProductions) {
            validChildPairs.add(cartesianProductFunction.pack(p.leftChild, p.rightChild));
        }

        final StringBuilder sb = new StringBuilder(10 * 1024);
        for (final int childPair : validChildPairs) {
            final int leftChild = cartesianProductFunction.unpackLeftChild(childPair);
            final int rightChild = cartesianProductFunction.unpackRightChild(childPair);
            sb.append(leftChild + "," + rightChild + ',' + childPair + '\n');
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public String getStats() {
        final StringBuilder sb = new StringBuilder(1024);
        sb.append(super.getStats());
        sb.append("Cartesian Product Function: " + cartesianProductFunction.getClass().getName() + '\n');
        sb.append("Packed Array Size: " + cartesianProductFunction.packedArraySize() + '\n');
        sb.append("Valid production pairs: " + validProductionPairs + '\n');
        return sb.toString();
    }

    public interface CartesianProductFunction {

        /**
         * Returns the array size required to store all possible child combinations.
         * 
         * @return the array size required to store all possible child combinations.
         */
        public abstract int packedArraySize();

        /**
         * Returns a single int representing a child pair.
         * 
         * @param leftChild
         * @param rightChild
         * @return packed representation of the specified child pair or {@link Integer#MIN_VALUE} if the pair
         *         is invalid.
         */
        public abstract int pack(final int leftChild, final int rightChild);

        /**
         * Returns a single int representing a unary production.
         * 
         * @param child
         * @return packed representation of the specified production.
         */
        public abstract int packUnary(final int child);

        /**
         * Returns a single int representing a lexical production.
         * 
         * @param child
         * @return packed representation of the specified production.
         */
        public abstract int packLexical(final int child);

        /**
         * Returns the left child encoded into a packed child pair
         * 
         * @param childPair
         * @return the left child encoded into a packed child pair
         */
        public abstract int unpackLeftChild(final int childPair);

        /**
         * Returns the right child encoded into a packed child pair
         * 
         * @param childPair
         * @return the right child encoded into a packed child pair
         */
        public abstract int unpackRightChild(final int childPair);

        public abstract String openClPackDefine();

        public abstract String openClUnpackLeftChild();
    }

    protected abstract class ShiftFunction implements CartesianProductFunction {
        // Shift lengths and masks for packing and unpacking non-terminals into an int
        public final int shift;
        protected final int lowOrderMask;
        private final int packedArraySize;

        protected ShiftFunction(final int maxUnshiftedNonTerminal) {
            shift = Math.logBase2(Math.nextPowerOf2(maxUnshiftedNonTerminal + 1));
            int m = 0;
            for (int i = 0; i < shift; i++) {
                m = m << 1 | 1;
            }
            lowOrderMask = m;

            packedArraySize = numNonTerms() << shift | lowOrderMask;
        }

        @Override
        public int packedArraySize() {
            return packedArraySize;
        }
    }

    public abstract class LeftShiftFunction extends ShiftFunction {
        public final int maxPackedLexicalProduction = -numNonTerms() - 1;

        public LeftShiftFunction(final int maxUnshiftedNonTerminal) {
            super(maxUnshiftedNonTerminal);
        }

        @Override
        public int pack(final int leftChild, final int rightChild) {
            return leftChild << shift | (rightChild & lowOrderMask);
        }

        public final int packUnary(final int child) {
            return -child - 1;
        }

        public final int packLexical(final int child) {
            return maxPackedLexicalProduction - child;
        }

        @Override
        public final int unpackLeftChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxPackedLexicalProduction) {
                    // Lexical production
                    return -childPair + maxPackedLexicalProduction;
                }
                // Unary production
                return -childPair - 1;
            }
            return childPair >> shift;
        }

        @Override
        public final int unpackRightChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxPackedLexicalProduction) {
                    // Lexical production
                    return Production.LEXICAL_PRODUCTION;
                }
                // Unary production
                return Production.UNARY_PRODUCTION;
            }
            return childPair & lowOrderMask;
        }

        @Override
        public final String openClPackDefine() {
            return "#define PACK ((leftNonTerminal  << " + shift + ") | (rightNonTerminal & " + lowOrderMask
                    + "))";
        }

        public final String openClUnpackLeftChild() {
            final StringBuilder sb = new StringBuilder();
            sb.append("int unpackLeftChild(const int childPair) {\n");
            sb.append("    if (childPair < 0) {\n");
            sb.append("        // Unary or lexical production\n");
            sb.append("        if (childPair <= MAX_PACKED_LEXICAL_PRODUCTION) {\n");
            sb.append("            // Lexical production\n");
            sb.append("            return -childPair + MAX_PACKED_LEXICAL_PRODUCTION;\n");
            sb.append("        }\n");
            sb.append("        // Unary production\n");
            sb.append("        return -childPair - 1;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Left child of binary production\n");
            sb.append("    return childPair >> PACKING_SHIFT;\n");
            sb.append("}\n");
            return sb.toString();
        }
    }

    public final class DefaultFunction extends LeftShiftFunction {

        public DefaultFunction() {
            super(rightChildrenEnd);
        }

        @Override
        public final int pack(final int leftChild, final int rightChild) {
            final int childPair = leftChild << shift | (rightChild & lowOrderMask);
            if (childPair > packedArraySize()) {
                return Integer.MIN_VALUE;
            }
            return childPair;
        }
    }

    public class RightShiftFunction extends ShiftFunction {
        public final int maxPackedLexicalProduction = -numNonTerms() - 1;

        public RightShiftFunction() {
            super(leftChildrenEnd);
        }

        @Override
        public final int pack(final int leftChild, final int rightChild) {
            final int childPair = rightChild << shift | (leftChild & lowOrderMask);
            if (childPair > packedArraySize()) {
                return Integer.MIN_VALUE;
            }
            return childPair;
        }

        public final int packUnary(final int child) {
            return -child - 1;
        }

        public final int packLexical(final int child) {
            return maxPackedLexicalProduction - child;
        }

        @Override
        public final int unpackLeftChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxPackedLexicalProduction) {
                    // Lexical production
                    return -childPair + maxPackedLexicalProduction;
                }
                // Unary production
                return -childPair - 1;
            }
            return childPair & lowOrderMask;
        }

        @Override
        public final int unpackRightChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxPackedLexicalProduction) {
                    // Lexical production
                    return Production.LEXICAL_PRODUCTION;
                }
                // Unary production
                return Production.UNARY_PRODUCTION;
            }
            return childPair >> shift;
        }

        @Override
        public final String openClPackDefine() {
            return "#define PACK ((leftNonTerminal  << " + shift + ") | (rightNonTerminal & " + lowOrderMask
                    + "))";
        }

        public final String openClUnpackLeftChild() {
            final StringBuilder sb = new StringBuilder();
            sb.append("int unpackLeftChild(const int childPair) {\n");
            sb.append("    if (childPair < 0) {\n");
            sb.append("        // Unary or lexical production\n");
            sb.append("        if (childPair <= MAX_PACKED_LEXICAL_PRODUCTION) {\n");
            sb.append("            // Lexical production\n");
            sb.append("            return -childPair + MAX_PACKED_LEXICAL_PRODUCTION;\n");
            sb.append("        }\n");
            sb.append("        // Unary production\n");
            sb.append("        return -childPair - 1;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Left child of binary production\n");
            sb.append("    return childPair & " + lowOrderMask + ";\n");
            sb.append("}\n");
            return sb.toString();
        }
    }

    public final class UnfilteredFunction extends LeftShiftFunction {
        public UnfilteredFunction() {
            super(numNonTerms());
        }
    }

    public final class BitVectorExactFilterFunction extends LeftShiftFunction {

        /**
         * {@link BitVector} of child pairs found in binary grammar rules.
         * 
         * TODO This should really be implemented as a PackedBitMatrix, if we had such a class
         */
        private final PackedBitVector validChildPairs;

        public BitVectorExactFilterFunction() {
            super(rightChildrenEnd);

            validChildPairs = new PackedBitVector(packedArraySize());

            for (final Production p : binaryProductions) {
                validChildPairs.add(internalPack(p.leftChild, p.rightChild));
            }
        }

        @Override
        public final int pack(final int leftChild, final int rightChild) {
            final int childPair = internalPack(leftChild, rightChild);
            if (!validChildPairs.contains(childPair)) {
                return Integer.MIN_VALUE;
            }
            return childPair;
        }

        private int internalPack(final int leftChild, final int rightChild) {
            return leftChild << shift | (rightChild & lowOrderMask);
        }
    }

    public final class PerfectHashFilterFunction extends ShiftFunction {

        private final int maxLexicalProduction = -numNonTerms() - 1;

        /** Perfect (collision-free) hash of all observed child pairs. */
        private final PerfectInt2IntHash perfectHash;

        private final int packedArraySize;

        public PerfectHashFilterFunction() {

            super(rightChildrenEnd);

            final IntSet childPairs = new IntOpenHashSet(binaryProductions.size());
            for (final Production p : binaryProductions) {
                childPairs.add(internalPack(p.leftChild, p.rightChild));
            }

            this.perfectHash = new PerfectInt2IntHash(childPairs.toIntArray());
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
        public int packedArraySize() {
            return packedArraySize;
        }

        @Override
        public String openClPackDefine() {
            return "";
        }

        @Override
        public String openClUnpackLeftChild() {
            return "";
        }
    }

    public final class PerfectIntPairHashFilterFunction extends ShiftFunction {

        private final int maxLexicalProduction = -numNonTerms() - 1;

        /** Perfect (collision-free) hash of all observed child pairs. */
        private final SegmentedPerfectIntPair2IntHash perfectHash;

        private final int packedArraySize;

        public PerfectIntPairHashFilterFunction() {
            super(rightChildrenEnd);

            // TODO Eliminate packedChildPairs and populate childPairs[][] directly from binaryProductions
            final IntSet packedChildPairs = new IntOpenHashSet(binaryProductions.size());
            for (final Production p : binaryProductions) {
                packedChildPairs.add(p.leftChild << shift | (p.rightChild & lowOrderMask));
            }
            final int[][] childPairs = new int[2][packedChildPairs.size()];
            int i = 0;
            for (final int packedChildPair : packedChildPairs) {
                childPairs[0][i] = packedChildPair >> shift;
                childPairs[1][i++] = packedChildPair & lowOrderMask;
            }

            this.perfectHash = new SegmentedPerfectIntPair2IntHash(childPairs, rightChildrenEnd);
            System.out.println("Hashed grammar: " + perfectHash.toString());
            this.packedArraySize = perfectHash.hashtableSize();
        }

        @Override
        public final int pack(final int leftChild, final int rightChild) {
            return perfectHash.unsafeHashcode(leftChild, rightChild);
        }

        public final int packUnary(final int child) {
            return -child - 1;
        }

        public final int packLexical(final int child) {
            return maxLexicalProduction - child;
        }

        @Override
        public final int unpackLeftChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxLexicalProduction) {
                    // Lexical production
                    return -childPair + maxLexicalProduction;
                }
                // Unary production
                return -childPair - 1;
            }

            return perfectHash.unsafeKey1(childPair);
        }

        @Override
        public final int unpackRightChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxLexicalProduction) {
                    // Lexical production
                    return Production.LEXICAL_PRODUCTION;
                }
                // Unary production
                return Production.UNARY_PRODUCTION;
            }

            return perfectHash.unsafeKey2(childPair);
        }

        @Override
        public int packedArraySize() {
            return packedArraySize;
        }

        @Override
        public String openClPackDefine() {
            return "";
        }

        @Override
        public String openClUnpackLeftChild() {
            return "";
        }
    }
}
