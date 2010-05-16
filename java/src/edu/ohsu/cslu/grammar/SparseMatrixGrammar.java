package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.Collection;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.PackedBitVector;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.hash.PerfectHash;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.util.Math;

/**
 * Stores a grammar as a sparse matrix of probabilities.
 * 
 * Assumes fewer than 2^30 total non-terminals combinations (so that a production pair will fit into a signed
 * 32-bit int). This limit _may_ still allow more than 2^15 total non-terminals, depending on the grammar's
 * factorization. In general, we assume a left-factored grammar with many fewer valid right child productions
 * than left children.
 * 
 * @author Aaron Dunlop
 * @since Dec 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */

public abstract class SparseMatrixGrammar extends SortedGrammar {

    protected final int validProductionPairs;

    protected final CartesianProductFunction cartesianProductFunction;

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
    public abstract float unaryLogProbability(final int parent, final int child);

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
        return (nonTerminal >= cartesianProductFunction.rightChildStart()
                && nonTerminal < cartesianProductFunction.rightChildEnd() && nonTerminal != nullSymbol);
    }

    @Override
    public final boolean isValidLeftChild(final int nonTerminal) {
        return (nonTerminal >= cartesianProductFunction.leftChildStart()
                && nonTerminal < cartesianProductFunction.leftChildEnd() && nonTerminal != nullSymbol);
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
        sb.append("Valid production pairs: " + validProductionPairs + '\n');
        sb.append("Valid left children: " + (numNonTerms() - normalPosStart) + '\n');
        sb.append("Valid right children: " + leftChildOnlyStart + '\n');

        sb.append("Max left child: " + (numNonTerms() - 1) + '\n');
        sb.append("Max right child: " + (leftChildOnlyStart - 1) + '\n');

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

        /**
         * Returns the index of first non-terminal valid as a left child.
         * 
         * @return the index of first non-terminal valid as a left child.
         */
        public int leftChildStart();

        /**
         * Returns the index of first child not valid as a left child (the exclusive end of the left-child
         * range).
         * 
         * @return the index of first child not valid as a left child (the exclusive end of the left-child
         *         range).
         */
        public int leftChildEnd();

        /**
         * Returns the index of first non-terminal valid as a right child.
         * 
         * @return the index of first non-terminal valid as a right child.
         */
        public int rightChildStart();

        /**
         * Returns the index of first child not valid as a left child (the exclusive end of the left-child
         * range).
         * 
         * @return the index of first child not valid as a left child (the exclusive end of the left-child
         *         range).
         */
        public int rightChildEnd();

        public abstract String openClPackDefine();
    }

    protected abstract class ShiftFunction implements CartesianProductFunction {
        // Shift lengths and masks for packing and unpacking non-terminals into an int
        public final int shift;
        protected final int lowOrderMask;
        private final int packedArraySize;

        protected ShiftFunction(final int maxShiftedNonTerminal) {
            shift = Math.logBase2(Math.nextPowerOf2(maxShiftedNonTerminal));
            int m = 0;
            for (int i = 0; i < shift; i++) {
                m = m << 1 | 1;
            }
            lowOrderMask = m;

            packedArraySize = numNonTerms() << shift;
        }

        @Override
        public int packedArraySize() {
            return packedArraySize;
        }
    }

    public abstract class LeftShiftFunction extends ShiftFunction {
        private final int maxLexicalProduction = -numNonTerms() - 1;

        public LeftShiftFunction(final int maxShiftedNonTerminal) {
            super(maxShiftedNonTerminal);
        }

        @Override
        public int pack(final int leftChild, final int rightChild) {
            return leftChild << shift | (rightChild & lowOrderMask);
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
            return childPair >> shift;
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
            return childPair & lowOrderMask;
        }

        @Override
        public final String openClPackDefine() {
            return "#define PACK ((validLeftChildren[leftChildIndex] << " + shift
                    + ") | (validRightChildren[rightChildIndex] & " + lowOrderMask + "))";
        }
    }

    public final class DefaultFunction extends LeftShiftFunction {

        public DefaultFunction() {
            super(leftChildOnlyStart);
        }

        @Override
        public final int pack(final int leftChild, final int rightChild) {
            final int childPair = leftChild << shift | (rightChild & lowOrderMask);
            return childPair > packedArraySize() ? Integer.MIN_VALUE : childPair;
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
    }

    // public class RightShiftFunction extends DefaultFunction {
    //
    // public RightShiftFunction() {
    // super(numNonTerms());
    // }
    //
    // @Override
    // public final int pack(final int leftChild, final int rightChild) {
    // return rightChild << leftShift | (leftChild & rightMask);
    // }
    //
    // @Override
    // public final int unpackLeftChild(final int childPair) {
    // final int lower = childPair & rightMask;
    //
    // // Handle negative lower values
    // if ((lower & rightNegativeBit) == rightNegativeBit) {
    // return lower | leftMask;
    // }
    // return lower;
    // }
    //
    // @Override
    // public final int unpackRightChild(final int childPair) {
    // return childPair >> leftShift;
    // }
    //
    // @Override
    // public String openClPackDefine() {
    // return "#define PACK ((validLeftChildren[leftChildIndex] << " + leftShift
    // + ") | (validRightChildren[rightChildIndex] & " + rightMask + "))";
    // }
    // }

    public final class UnfilteredFunction extends LeftShiftFunction {
        private final int numNonTerms;

        public UnfilteredFunction() {
            super(numNonTerms());
            this.numNonTerms = numNonTerms();
        }

        @Override
        public final int leftChildStart() {
            return 0;
        }

        @Override
        public final int leftChildEnd() {
            return numNonTerms;
        }

        @Override
        public final int rightChildStart() {
            return 0;
        }

        @Override
        public final int rightChildEnd() {
            return numNonTerms;
        }
    }

    public final class PosFactoredFilterFunction extends LeftShiftFunction {

        public PosFactoredFilterFunction() {
            super(leftChildOnlyStart);
        }

        @Override
        public int pack(final int leftChild, final int rightChild) {
            if (leftChild >= leftFactoredStart && leftChild < normalLeftChildStart
                    && rightChild >= posNonFactoredStart && rightChild < normalPosStart) {
                return Integer.MIN_VALUE;
            }

            return leftChild << shift | (rightChild & lowOrderMask);
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
    }

    public final class BitVectorExactFilterFunction extends LeftShiftFunction {

        /**
         * {@link BitVector} of child pairs found in binary grammar rules.
         * 
         * TODO This should really be implemented as a PackedBitMatrix, if we had such a class
         */
        private final PackedBitVector validChildPairs;

        public BitVectorExactFilterFunction() {
            super(leftChildOnlyStart);

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
