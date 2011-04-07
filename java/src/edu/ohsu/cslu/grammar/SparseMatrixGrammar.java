/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.util.Math;
import edu.ohsu.cslu.util.Strings;

/**
 * Stores a grammar as a sparse matrix of probabilities.
 * 
 * Assumes fewer than 2^15 total non-terminals (so that a production pair will fit into a signed 32-bit int).
 * 
 * Implementations may store the binary rule matrix in a variety of formats (e.g. CSR ( {@link CsrSparseMatrixGrammar})
 * or CSC ({@link LeftCscSparseMatrixGrammar})).
 * 
 * The unary rules are always stored in CSR format, with rows denoting rule parents and columns rule children. We
 * anticipate relatively few unary rules (the Berkeley grammar has only ~100k), and iteration order doesn't matter
 * greatly. CSC format would allow us to iterate through only the populated children, but CSR format makes
 * parallelization much simpler (e.g., we can execute one thread per row (parent); since we're only updating the parent
 * probability, we do not need to synchronize with other threads).
 * 
 * @author Aaron Dunlop
 * @since Dec 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */

public abstract class SparseMatrixGrammar extends Grammar {

    public final PackingFunction packingFunction;

    /**
     * Offsets into {@link #cscUnaryRowIndices} for the start of each column (child), with one extra entry appended to
     * prevent loops from falling off the end. Indexed by child non-terminal, so the length is 1 greater than |V|.
     */
    public final int[] cscUnaryColumnOffsets;

    /**
     * Row indices of each matrix entry in {@link #cscUnaryProbabilities}. One entry for each unary rule; the same size
     * as {@link #cscUnaryProbabilities}.
     */
    public final short[] cscUnaryRowIndices;

    /** Unary rule probabilities One entry for each unary rule; the same size as {@link #cscUnaryRowIndices}. */
    public final float[] cscUnaryProbabilities;

    /**
     * Indices of the first and last non-terminals which can combine as the right sibling with each non-terminal
     * (indexed by left-child index)
     */
    public final short[] minRightSiblingIndices;
    public final short[] maxRightSiblingIndices;

    public SparseMatrixGrammar(final Reader grammarFile, final Class<? extends PackingFunction> functionClass)
            throws IOException {
        super(grammarFile);

        this.packingFunction = createCartesianProductFunction(functionClass);

        minRightSiblingIndices = new short[numNonTerms()];
        maxRightSiblingIndices = new short[numNonTerms()];
        storeRightSiblingIndices();

        // And all unary productions
        cscUnaryColumnOffsets = new int[numNonTerms() + 1];
        cscUnaryRowIndices = new short[numUnaryProds()];
        cscUnaryProbabilities = new float[numUnaryProds()];

        storeUnaryRulesAsCscMatrix(unaryProductions, cscUnaryColumnOffsets, cscUnaryRowIndices, cscUnaryProbabilities);
    }

    public SparseMatrixGrammar(final Reader grammarFile) throws IOException {
        this(grammarFile, PerfectIntPairHashPackingFunction.class);
    }

    public SparseMatrixGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    public SparseMatrixGrammar(final Grammar g, final Class<? extends PackingFunction> functionClass) {
        super(g);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.packingFunction = createCartesianProductFunction(functionClass);

        // Store all unary productions
        cscUnaryColumnOffsets = new int[numNonTerms() + 1];
        cscUnaryRowIndices = new short[numUnaryProds()];
        cscUnaryProbabilities = new float[numUnaryProds()];

        storeUnaryRulesAsCscMatrix(unaryProductions, cscUnaryColumnOffsets, cscUnaryRowIndices, cscUnaryProbabilities);

        minRightSiblingIndices = new short[numNonTerms()];
        maxRightSiblingIndices = new short[numNonTerms()];
        storeRightSiblingIndices();
    }

    protected SparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final Class<? extends PackingFunction> functionClass) {
        this(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                functionClass, true);
    }

    protected SparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final Class<? extends PackingFunction> functionClass, final boolean initCscMatrices) {
        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat);

        // Initialization code duplicated from constructor above to allow these fields to be final
        this.packingFunction = createCartesianProductFunction(functionClass);

        // And all unary productions
        cscUnaryColumnOffsets = new int[numNonTerms() + 1];
        cscUnaryRowIndices = new short[numUnaryProds()];
        cscUnaryProbabilities = new float[numUnaryProds()];

        if (initCscMatrices) {
            storeUnaryRulesAsCscMatrix(unaryProductions, cscUnaryColumnOffsets, cscUnaryRowIndices,
                    cscUnaryProbabilities);
        }
        minRightSiblingIndices = new short[numNonTerms()];
        maxRightSiblingIndices = new short[numNonTerms()];
        storeRightSiblingIndices();

    }

    /**
     * Stores unary rules in Compressed-Sparse-Column (CSC) matrix format.
     * 
     * @param productions
     * @param validChildPairs Sorted array of valid child pairs
     * @param cscPopulatedColumns
     * @param cscColumnIndices
     * @param cscRowIndices
     * @param cscProbabilities
     */
    private void storeUnaryRulesAsCscMatrix(final Collection<Production> productions, final int[] cscColumnOffsets,
            final short[] cscRowIndices, final float[] cscProbabilities) {

        // Bin all rules by child, mapping parent -> probability
        final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> maps = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>(
                1000);

        for (final Production p : productions) {
            final short child = (short) p.leftChild;

            Short2FloatOpenHashMap map = maps.get(child);
            if (map == null) {
                map = new Short2FloatOpenHashMap(20);
                maps.put(child, map);
            }
            map.put((short) p.parent, p.prob);
        }

        // Store rules in CSC matrix
        int j = 0;
        final short[] keys = maps.keySet().toShortArray();
        Arrays.sort(keys);
        for (short child = 0; child < numNonTerms(); child++) {

            cscColumnOffsets[child] = j;

            if (!maps.containsKey(child)) {
                continue;
            }

            final Short2FloatOpenHashMap map = maps.get(child);
            final short[] parents = map.keySet().toShortArray();
            Arrays.sort(parents);

            for (int k = 0; k < parents.length; k++) {
                cscRowIndices[j] = parents[k];
                cscProbabilities[j++] = map.get(parents[k]);
            }
        }
        cscColumnOffsets[cscColumnOffsets.length - 1] = j;
    }

    @SuppressWarnings("unchecked")
    private PackingFunction createCartesianProductFunction(Class<? extends PackingFunction> functionClass) {
        try {
            if (functionClass == null) {
                functionClass = PerfectIntPairHashPackingFunction.class;
            }

            Constructor<PackingFunction> c;
            try {
                c = (Constructor<PackingFunction>) functionClass.getConstructor(SparseMatrixGrammar.class);
            } catch (final NoSuchMethodException e) {
                c = (Constructor<PackingFunction>) functionClass.getConstructor(getClass());
            }
            return c.newInstance(this);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

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
        return binaryLogProbability(parent, packingFunction.pack((short) leftChild, (short) rightChild));
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
        for (int i = cscUnaryColumnOffsets[child]; i <= cscUnaryColumnOffsets[child + 1]; i++) {
            final int row = cscUnaryRowIndices[i];
            if (row == parent) {
                return cscUnaryProbabilities[i];
            }
            if (row > parent) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return Float.NEGATIVE_INFINITY;
        // for (int i = csrUnaryRowStartIndices[parent]; i <= csrUnaryRowStartIndices[parent + 1]; i++) {
        // final int column = csrUnaryColumnIndices[i];
        // if (column == child) {
        // return csrUnaryProbabilities[i];
        // }
        // if (column > child) {
        // return Float.NEGATIVE_INFINITY;
        // }
        // }
        // return Float.NEGATIVE_INFINITY;
    }

    private void storeRightSiblingIndices() {
        Arrays.fill(minRightSiblingIndices, Short.MAX_VALUE);
        Arrays.fill(maxRightSiblingIndices, Short.MIN_VALUE);

        for (final Production p : binaryProductions) {
            if (p.rightChild < minRightSiblingIndices[p.leftChild]) {
                minRightSiblingIndices[p.leftChild] = (short) p.rightChild;
            }
            if (p.rightChild > maxRightSiblingIndices[p.leftChild]) {
                maxRightSiblingIndices[p.leftChild] = (short) p.rightChild;
            }
        }
    }

    /**
     * Returns the cartesian-product function in use
     * 
     * @return the cartesian-product function in use
     */
    public final PackingFunction cartesianProductFunction() {
        return packingFunction;
    }

    @Override
    public final boolean isValidRightChild(final int nonTerminal) {
        return nonTerminal >= rightChildrenStart && nonTerminal <= rightChildrenEnd && nonTerminal != nullSymbol;
    }

    @Override
    public final boolean isValidLeftChild(final int nonTerminal) {
        return nonTerminal >= leftChildrenStart && nonTerminal <= leftChildrenEnd && nonTerminal != nullSymbol;
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
            validChildPairs.add(packingFunction.pack((short) p.leftChild, (short) p.rightChild));
        }

        final StringBuilder sb = new StringBuilder(10 * 1024);
        for (final int childPair : validChildPairs) {
            final int leftChild = packingFunction.unpackLeftChild(childPair);
            final int rightChild = packingFunction.unpackRightChild(childPair);
            sb.append(leftChild + "," + rightChild + ',' + childPair + '\n');
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public String getStats() {
        final StringBuilder sb = new StringBuilder(1024);
        sb.append(super.getStats());
        sb.append("Cartesian Product Function: " + packingFunction.getClass().getName() + '\n');
        sb.append("Packed Array Size: " + packingFunction.packedArraySize() + '\n');
        return sb.toString();
    }

    public abstract class PackingFunction implements Serializable {

        // Shift lengths and masks for packing and unpacking non-terminals into an int
        public final int shift;
        protected final int lowOrderMask;
        private final int packedArraySize;
        public final int maxPackedLexicalProduction = -numNonTerms() - 1;

        protected PackingFunction(final int maxUnshiftedNonTerminal) {
            shift = Math.logBase2(Math.nextPowerOf2(maxUnshiftedNonTerminal + 1));
            int m = 0;
            for (int i = 0; i < shift; i++) {
                m = m << 1 | 1;
            }
            lowOrderMask = m;

            packedArraySize = numNonTerms() << shift | lowOrderMask;
        }

        /**
         * Returns the array size required to store all possible child combinations.
         * 
         * @return the array size required to store all possible child combinations.
         */
        public int packedArraySize() {
            return packedArraySize;
        }

        /**
         * Returns a single int representing a child pair.
         * 
         * TODO Refactor to take short parameters instead of ints
         * 
         * @param leftChild
         * @param rightChild
         * @return packed representation of the specified child pair or {@link Integer#MIN_VALUE} if the pair is not
         *         permitted by the grammar.
         */
        public int pack(final short leftChild, final short rightChild) {
            return leftChild << shift | (rightChild & lowOrderMask);
        }

        /**
         * Returns a single int representing a unary production.
         * 
         * @param child
         * @return packed representation of the specified production.
         */
        public final int packUnary(final short child) {
            return -child - 1;
        }

        /**
         * Returns a single int representing a lexical production.
         * 
         * @param child
         * @return packed representation of the specified production.
         */
        public final int packLexical(final int child) {
            return maxPackedLexicalProduction - child;
        }

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
        public abstract short unpackRightChild(final int childPair);

        public final String openClPackDefine() {
            return "#define PACK ((leftNonTerminal  << " + shift + ") | (rightNonTerminal & " + lowOrderMask + "))\n"
                    + "#define PACK_UNARY -winningChild - 1\n";
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

    public final class LeftShiftFunction extends PackingFunction {

        public LeftShiftFunction() {
            super(rightChildrenEnd);
        }

        @Override
        public final int pack(final short leftChild, final short rightChild) {
            final int childPair = leftChild << shift | (rightChild & lowOrderMask);
            if (childPair > packedArraySize()) {
                return Integer.MIN_VALUE;
            }
            return childPair;
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
        public final short unpackRightChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxPackedLexicalProduction) {
                    // Lexical production
                    return Production.LEXICAL_PRODUCTION;
                }
                // Unary production
                return Production.UNARY_PRODUCTION;
            }
            return (short) (childPair & lowOrderMask);
        }
    }

    public class RightShiftFunction extends PackingFunction {

        public RightShiftFunction() {
            super(leftChildrenEnd);
        }

        @Override
        public final int pack(final short leftChild, final short rightChild) {
            final int childPair = rightChild << shift | (leftChild & lowOrderMask);
            if (childPair > packedArraySize()) {
                return Integer.MIN_VALUE;
            }
            return childPair;
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
        public final short unpackRightChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxPackedLexicalProduction) {
                    // Lexical production
                    return Production.LEXICAL_PRODUCTION;
                }
                // Unary production
                return Production.UNARY_PRODUCTION;
            }
            return (short) (childPair >> shift);
        }

        // @Override
        // public final String openClPackDefine() {
        // return "#define PACK ((leftNonTerminal  << " + shift + ") | (rightNonTerminal & " + lowOrderMask + "))\n"
        // + "#define PACK_UNARY -winningChild - 1\n";
        // }
        //
        // public final String openClUnpackLeftChild() {
        // final StringBuilder sb = new StringBuilder();
        // sb.append("int unpackLeftChild(const int childPair) {\n");
        // sb.append("    if (childPair < 0) {\n");
        // sb.append("        // Unary or lexical production\n");
        // sb.append("        if (childPair <= MAX_PACKED_LEXICAL_PRODUCTION) {\n");
        // sb.append("            // Lexical production\n");
        // sb.append("            return -childPair + MAX_PACKED_LEXICAL_PRODUCTION;\n");
        // sb.append("        }\n");
        // sb.append("        // Unary production\n");
        // sb.append("        return -childPair - 1;\n");
        // sb.append("    }\n");
        // sb.append("    \n");
        // sb.append("    // Left child of binary production\n");
        // sb.append("    return childPair & " + lowOrderMask + ";\n");
        // sb.append("}\n");
        // return sb.toString();
        // }
    }

    public final class PerfectIntPairHashPackingFunction extends PackingFunction {

        private final int maxLexicalProduction = -numNonTerms() - 1;

        private final int packedArraySize;

        /** Parallel array, indexed by k1 */
        private final int[] maxKey2;
        private final int[] k2Shifts;
        private final int[] k2Masks;

        /** Offsets of each perfect hash `segment' within {@link #hashtable}, indexed by k1. */
        private final int[] hashtableOffsets;
        private final short[] hashtable;

        /** Offsets of each perfect hash `segment' within {@link #displacementTableOffsets}, indexed by k1. */
        private final int[] displacementTable;
        private final int[] displacementTableOffsets;

        private final int size;

        public PerfectIntPairHashPackingFunction() {
            this(binaryProductions, rightChildrenEnd);
        }

        public PerfectIntPairHashPackingFunction(final ArrayList<Production> productions,
                final int maxUnshiftedNonTerminal) {
            super(maxUnshiftedNonTerminal);

            final int[][] childPairs = new int[2][productions.size()];
            int k = 0;
            for (final Production p : productions) {
                childPairs[0][k] = p.leftChild;
                childPairs[1][k++] = p.rightChild;
            }

            // System.out.println("Hashed grammar: " + perfectHash.toString());

            final int parallelArraySize = numNonTerms() + 1; // Math.max(childPairs[0]) + 1;

            // Find unique k2 values for each k1
            final IntOpenHashSet[] k2Sets = new IntOpenHashSet[parallelArraySize];
            for (int i = 0; i < k2Sets.length; i++) {
                k2Sets[i] = new IntOpenHashSet();
            }
            for (int i = 0; i < childPairs[0].length; i++) {
                k2Sets[childPairs[0][i]].add(childPairs[1][i]);
            }

            // Calculate total key pair count
            int tmpSize = 0;
            for (int i = 0; i < k2Sets.length; i++) {
                tmpSize += k2Sets[i].size();
            }
            this.size = tmpSize;

            this.maxKey2 = new int[parallelArraySize];

            // Indexed by k1
            final int[][] k2s = new int[parallelArraySize][];
            for (int i = 0; i < k2s.length; i++) {
                k2s[i] = k2Sets[i].toIntArray();
                maxKey2[i] = Math.max(k2s[i]);
            }

            this.k2Shifts = new int[parallelArraySize];
            this.k2Masks = new int[parallelArraySize];
            this.hashtableOffsets = new int[parallelArraySize + 1];
            this.displacementTableOffsets = new int[parallelArraySize + 1];

            final int tmpArraySize = parallelArraySize * Math.max(childPairs[1]) + 1;
            final short[] tmpHashtable = new short[tmpArraySize];
            final int[] tmpDisplacementTable = new int[tmpArraySize];

            for (int k1 = 0; k1 < k2s.length; k1++) {

                final HashtableSegment hs = createPerfectHash(k2s[k1]);

                this.k2Masks[k1] = hs.hashMask;
                this.k2Shifts[k1] = hs.hashShift;

                // Record the offsets
                hashtableOffsets[k1 + 1] = hashtableOffsets[k1] + hs.hashtableSegment.length;
                displacementTableOffsets[k1 + 1] = displacementTableOffsets[k1] + hs.displacementTableSegment.length;

                // Copy the segment into the temporary hash and displacement arrays
                System.arraycopy(hs.hashtableSegment, 0, tmpHashtable, hashtableOffsets[k1], hs.hashtableSegment.length);

                for (int j = 0; j < hs.displacementTableSegment.length; j++) {
                    tmpDisplacementTable[displacementTableOffsets[k1] + j] = hashtableOffsets[k1]
                            + hs.displacementTableSegment[j];
                }
            }

            this.hashtable = new short[hashtableOffsets[parallelArraySize]];
            System.arraycopy(tmpHashtable, 0, this.hashtable, 0, this.hashtable.length);
            this.displacementTable = new int[displacementTableOffsets[parallelArraySize]];
            System.arraycopy(tmpDisplacementTable, 0, this.displacementTable, 0, this.displacementTable.length);

            this.packedArraySize = hashtableSize();
        }

        private int findDisplacement(final short[] target, final short[] merge) {
            for (int s = 0; s <= target.length - merge.length; s++) {
                if (!shiftCollides(target, merge, s)) {
                    return s;
                }
            }
            throw new RuntimeException("Unable to find a successful shift");
        }

        private HashtableSegment createPerfectHash(final int[] k2s) {

            // If there are no k2 entries for this k1, return a single-entry hash segment, with a shift and mask
            // that will always resolve to the single (empty) entry
            if (k2s.length == 0) {
                return new HashtableSegment(new short[] { Short.MIN_VALUE }, 1, new int[] { 0 }, 1, 32, 0x0,
                        new short[][] { { Short.MIN_VALUE } });
            }

            // Compute the size of the square matrix (m)
            final int m = Math.nextPowerOf2((int) java.lang.Math.sqrt(Math.max(k2s)) + 1);
            final int n = m;

            // Allocate a temporary hashtable of the maximum possible size
            final short[] hashtableSegment = new short[m * n];
            Arrays.fill(hashtableSegment, Short.MIN_VALUE);

            // Allocate the displacement table (r in Getty's notation)
            final int[] displacementTableSegment = new int[m];

            // Compute shift and mask (for hashing k2, prior to displacement)
            final int hashBitShift = Math.logBase2(m);
            int tmp = 0;
            for (int j = 0; j < hashBitShift; j++) {
                tmp = tmp << 1 | 0x01;
            }
            final int hashMask = tmp;

            // Initialize the matrix
            final int[] rowIndices = new int[m];
            final int[] rowCounts = new int[m];
            final short[][] tmpMatrix = new short[m][n];
            for (int i = 0; i < m; i++) {
                rowIndices[i] = i;
                Arrays.fill(tmpMatrix[i], Short.MIN_VALUE);
            }

            // Populate the matrix, and count population of each row.
            for (int i = 0; i < k2s.length; i++) {
                final int k2 = k2s[i];
                final int x = k2 >> hashBitShift;
                final int y = k2 & hashMask;
                tmpMatrix[x][y] = (short) k2;
                rowCounts[x]++;
            }

            // Sort rows in ascending order by population (we'll iterate through the array in reverse order)
            edu.ohsu.cslu.util.Arrays.sort(rowCounts, rowIndices);

            /*
             * Store matrix rows in a single array, using the first-fit descending method. For each non-empty row:
             * 
             * 1. Displace the row right until none of its items collide with any of the items in previous rows.
             * 
             * 2. Record the displacement amount in displacementTableSegment.
             * 
             * 3. Insert this row into hashtableSegment.
             */
            for (int i = m - 1; i >= 0; i--) {
                final int row = rowIndices[i];
                displacementTableSegment[row] = findDisplacement(hashtableSegment, tmpMatrix[row]);
                for (int col = 0; col < m; col++) {
                    if (tmpMatrix[row][col] != Short.MIN_VALUE) {
                        hashtableSegment[displacementTableSegment[row] + col] = tmpMatrix[row][col];
                    }
                }
            }

            // Find the length of the segment (highest populated index in tmpHashtable + n)
            int maxPopulatedIndex = 0;
            for (int i = 0; i < hashtableSegment.length; i++) {
                if (hashtableSegment[i] != Short.MIN_VALUE) {
                    maxPopulatedIndex = i;
                }
            }
            final int segmentLength = maxPopulatedIndex + n;

            return new HashtableSegment(hashtableSegment, segmentLength, displacementTableSegment, m, hashBitShift,
                    hashMask, tmpMatrix);
        }

        /**
         * Returns true if the merged array, when shifted by s, will `collide' with the target array; i.e., if we
         * right-shift merge by s, are any populated elements of merge also populated elements of target.
         * 
         * @param target
         * @param merge
         * @param s
         * @return
         */
        private boolean shiftCollides(final short[] target, final short[] merge, final int s) {
            for (int i = 0; i < merge.length; i++) {
                if (merge[i] != Short.MIN_VALUE && target[s + i] != Short.MIN_VALUE) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public final int pack(final short leftChild, final short rightChild) {
            final int mask = k2Masks[leftChild];
            final int x = rightChild >> k2Shifts[leftChild] & mask;
            final int y = rightChild & mask;
            final int hashcode = displacementTable[displacementTableOffsets[leftChild] + x] + y;
            return hashtable[hashcode] == rightChild ? hashcode : Integer.MIN_VALUE;
        }

        public final int mask(final short leftChild) {
            return k2Masks[leftChild];
        }

        public final int offset(final short leftChild) {
            return displacementTableOffsets[leftChild];
        }

        public final int shift(final short leftChild) {
            return k2Shifts[leftChild];
        }

        public final int pack(final short rightChild, final int rightChildShift, final int mask, final int offset) {
            final int x = rightChild >> rightChildShift & mask;
            final int y = rightChild & mask;
            final int hashcode = displacementTable[offset + x] + y;
            return hashtable[hashcode] == rightChild ? hashcode : Integer.MIN_VALUE;
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

            // Linear search hashtable offsets for index of next-lower offset
            // A binary search might be a bit more efficient, but this shouldn't be a major time consumer
            for (int k1 = 0; k1 < hashtableOffsets.length - 1; k1++) {
                if (childPair >= hashtableOffsets[k1] && childPair < hashtableOffsets[k1 + 1]) {
                    return k1;
                }
            }
            return hashtableOffsets.length - 1;
        }

        @Override
        public final short unpackRightChild(final int childPair) {
            if (childPair < 0) {
                // Unary or lexical production
                if (childPair <= maxLexicalProduction) {
                    // Lexical production
                    return Production.LEXICAL_PRODUCTION;
                }
                // Unary production
                return Production.UNARY_PRODUCTION;
            }

            return hashtable[childPair];
        }

        public final int hashtableSize() {
            return hashtable.length;
        }

        public final int size() {
            return size;
        }

        public final int leftChildStart(final short leftChild) {
            return hashtableOffsets[leftChild];
        }

        @Override
        public String toString() {
            return String.format("keys: %d hashtable size: %d occupancy: %.2f%% shift-table size: %d totalMem: %d",
                    size, hashtableSize(), size * 100f / hashtableSize(), displacementTable.length, hashtable.length
                            * 2 + displacementTable.length * 4);
        }

        private final class HashtableSegment {
            final short[] hashtableSegment;
            final int[] displacementTableSegment;
            final int hashShift;
            final int hashMask;

            final short[][] squareMatrix;

            public HashtableSegment(final short[] hashtableSegment, final int segmentLength,
                    final int[] displacementTableSegment, final int displacementTableSegmentLength,
                    final int hashShift, final int hashMask, final short[][] squareMatrix) {

                this.hashtableSegment = new short[segmentLength];
                System.arraycopy(hashtableSegment, 0, this.hashtableSegment, 0, segmentLength);
                this.displacementTableSegment = new int[displacementTableSegmentLength];
                System.arraycopy(displacementTableSegment, 0, this.displacementTableSegment, 0,
                        displacementTableSegmentLength);
                this.hashShift = hashShift;
                this.hashMask = hashMask;
                this.squareMatrix = squareMatrix;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder();
                sb.append("   |");
                for (int col = 0; col < squareMatrix.length; col++) {
                    sb.append(String.format(" %2d", col));
                }
                sb.append('\n');
                sb.append(Strings.fill('-', squareMatrix.length * 3 + 4));
                sb.append('\n');
                for (int row = 0; row < squareMatrix.length; row++) {
                    sb.append(String.format("%2d |", row));
                    for (int col = 0; col < squareMatrix.length; col++) {
                        sb.append(String.format(
                                " %2s",
                                squareMatrix[row][col] == Short.MIN_VALUE ? "-" : Short
                                        .toString(squareMatrix[row][col])));
                    }
                    sb.append('\n');
                }
                sb.append("index: ");
                for (int i = 0; i < hashtableSegment.length; i++) {
                    sb.append(String.format(" %2d", i));
                }
                sb.append("\nkeys : ");
                for (int i = 0; i < hashtableSegment.length; i++) {
                    sb.append(String.format(" %2s",
                            hashtableSegment[i] == Short.MIN_VALUE ? "-" : Short.toString(hashtableSegment[i])));
                }
                sb.append('\n');
                return sb.toString();
            }
        }

        @Override
        public int packedArraySize() {
            return packedArraySize;
        }
    }
}
