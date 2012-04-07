package edu.ohsu.cslu.grammar;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Represents binary grammar rules by left child and right child (in addition to the standard parent-based storage), for
 * efficient access during the outside pass of inside-outside parsing.
 * 
 * @author Aaron Dunlop
 * @since Jul 14, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class InsideOutsideCscSparseMatrixGrammar extends LeftCscSparseMatrixGrammar {

    private static final long serialVersionUID = 1L;

    /**
     * A copy of the CSC grammar, mapped by left child -> parent,right child Note that this confuses the arguments to
     * pack/unpack method of the {@link PackingFunction}, swapping the parent and the left child. So
     * {@link PackingFunction#pack(short, short)} should be called with (parent, right child).
     */
    public final int[] leftChildCscBinaryPopulatedColumns;
    public final int[] leftChildCscBinaryPopulatedColumnOffsets;
    public final int[] leftChildCscBinaryColumnOffsets;
    public final short[] leftChildCscBinaryRowIndices;
    public final float[] leftChildCscBinaryProbabilities;
    public final PerfectIntPairHashPackingFunction leftChildPackingFunction;

    /**
     * A copy of the CSC grammar, mapped by right child -> parent,left child. Note that this confuses the arguments to
     * pack/unpack method of the {@link PackingFunction}, shifting the positions of parent, left child, and right child.
     * So {@link PackingFunction#pack(short, short)} should be called with (parent, left child).
     */
    public final int[] rightChildCscBinaryPopulatedColumns;
    public final int[] rightChildCscBinaryPopulatedColumnOffsets;
    public final int[] rightChildCscBinaryColumnOffsets;
    public final short[] rightChildCscBinaryRowIndices;
    public final float[] rightChildCscBinaryProbabilities;
    public final PerfectIntPairHashPackingFunction rightChildPackingFunction;

    /**
     * Offsets into {@link #csrUnaryColumnIndices} for the start of each row, indexed by row index (non-terminals)
     */
    public final int[] csrUnaryRowStartIndices;

    /**
     * Column indices of each matrix entry in {@link #csrUnaryProbabilities}. One entry for each unary rule; the same
     * size as {@link #csrUnaryProbabilities}.
     */
    public final short[] csrUnaryColumnIndices;

    /** Unary rule probabilities */
    public final float[] csrUnaryProbabilities;

    public InsideOutsideCscSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends PackingFunction> cartesianProductFunctionClass) throws IOException {
        super(grammarFile, cartesianProductFunctionClass);

        // Left child grammar
        final ArrayList<Production> binaryProductions = getBinaryProductions();
        final ArrayList<Production> binaryProductionsByLeftChild = binaryProductionsByLeftChild(binaryProductions);
        this.leftChildPackingFunction = new PerfectIntPairHashPackingFunction(binaryProductionsByLeftChild,
                numNonTerms() - 1);

        final int[] leftChildPopulatedBinaryColumnIndices = populatedBinaryColumnIndices(binaryProductionsByLeftChild,
                leftChildPackingFunction);
        this.leftChildCscBinaryPopulatedColumns = new int[leftChildPopulatedBinaryColumnIndices.length];
        this.leftChildCscBinaryPopulatedColumnOffsets = new int[leftChildCscBinaryPopulatedColumns.length + 1];
        this.leftChildCscBinaryRowIndices = new short[numBinaryProds()];
        this.leftChildCscBinaryProbabilities = new float[numBinaryProds()];
        this.leftChildCscBinaryColumnOffsets = new int[leftChildPackingFunction.packedArraySize() + 1];
        storeRulesAsMatrix(binaryProductionsByLeftChild, leftChildPackingFunction,
                leftChildPopulatedBinaryColumnIndices, leftChildCscBinaryPopulatedColumns,
                leftChildCscBinaryPopulatedColumnOffsets, leftChildCscBinaryColumnOffsets,
                leftChildCscBinaryRowIndices, leftChildCscBinaryProbabilities);

        // Right child grammar
        final ArrayList<Production> binaryProductionsByRightChild = binaryProductionsByRightChild(binaryProductions);
        this.rightChildPackingFunction = new PerfectIntPairHashPackingFunction(binaryProductionsByRightChild,
                numNonTerms() - 1);

        final int[] rightChildPopulatedBinaryColumnIndices = populatedBinaryColumnIndices(
                binaryProductionsByRightChild, rightChildPackingFunction);
        this.rightChildCscBinaryPopulatedColumns = new int[rightChildPopulatedBinaryColumnIndices.length];
        this.rightChildCscBinaryPopulatedColumnOffsets = new int[rightChildCscBinaryPopulatedColumns.length + 1];
        this.rightChildCscBinaryRowIndices = new short[numBinaryProds()];
        this.rightChildCscBinaryProbabilities = new float[numBinaryProds()];
        this.rightChildCscBinaryColumnOffsets = new int[rightChildPackingFunction.packedArraySize() + 1];

        storeRulesAsMatrix(binaryProductionsByRightChild, rightChildPackingFunction,
                rightChildPopulatedBinaryColumnIndices, rightChildCscBinaryPopulatedColumns,
                rightChildCscBinaryPopulatedColumnOffsets, rightChildCscBinaryColumnOffsets,
                rightChildCscBinaryRowIndices, rightChildCscBinaryProbabilities);

        // Store all unary rules
        this.csrUnaryRowStartIndices = new int[numNonTerms() + 1];
        this.csrUnaryColumnIndices = new short[numUnaryProds()];
        this.csrUnaryProbabilities = new float[numUnaryProds()];

        storeUnaryRulesAsCsrMatrix(csrUnaryRowStartIndices, csrUnaryColumnIndices, csrUnaryProbabilities);
    }

    public InsideOutsideCscSparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final Class<? extends PackingFunction> functionClass, final boolean initCscMatrices) {

        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                functionClass, initCscMatrices);

        // Initialization code duplicated from constructor above to allow these fields to be final

        // Left child grammar
        final ArrayList<Production> binaryProductionsByLeftChild = binaryProductionsByLeftChild(binaryProductions);
        this.leftChildPackingFunction = new PerfectIntPairHashPackingFunction(binaryProductionsByLeftChild,
                numNonTerms() - 1);

        final int[] leftChildPopulatedBinaryColumnIndices = populatedBinaryColumnIndices(binaryProductionsByLeftChild,
                leftChildPackingFunction);
        this.leftChildCscBinaryPopulatedColumns = new int[leftChildPopulatedBinaryColumnIndices.length];
        this.leftChildCscBinaryPopulatedColumnOffsets = new int[leftChildCscBinaryPopulatedColumns.length + 1];
        this.leftChildCscBinaryRowIndices = new short[numBinaryProds()];
        this.leftChildCscBinaryProbabilities = new float[numBinaryProds()];
        this.leftChildCscBinaryColumnOffsets = new int[leftChildPackingFunction.packedArraySize() + 1];
        storeRulesAsMatrix(binaryProductionsByLeftChild, leftChildPackingFunction,
                leftChildPopulatedBinaryColumnIndices, leftChildCscBinaryPopulatedColumns,
                leftChildCscBinaryPopulatedColumnOffsets, leftChildCscBinaryColumnOffsets,
                leftChildCscBinaryRowIndices, leftChildCscBinaryProbabilities);

        // Right child grammar
        final ArrayList<Production> binaryProductionsByRightChild = binaryProductionsByRightChild(binaryProductions);
        this.rightChildPackingFunction = new PerfectIntPairHashPackingFunction(binaryProductionsByRightChild,
                numNonTerms() - 1);

        final int[] rightChildPopulatedBinaryColumnIndices = populatedBinaryColumnIndices(
                binaryProductionsByRightChild, rightChildPackingFunction);
        this.rightChildCscBinaryPopulatedColumns = new int[rightChildPopulatedBinaryColumnIndices.length];
        this.rightChildCscBinaryPopulatedColumnOffsets = new int[rightChildCscBinaryPopulatedColumns.length + 1];
        this.rightChildCscBinaryRowIndices = new short[numBinaryProds()];
        this.rightChildCscBinaryProbabilities = new float[numBinaryProds()];
        this.rightChildCscBinaryColumnOffsets = new int[rightChildPackingFunction.packedArraySize() + 1];

        storeRulesAsMatrix(binaryProductionsByRightChild, rightChildPackingFunction,
                rightChildPopulatedBinaryColumnIndices, rightChildCscBinaryPopulatedColumns,
                rightChildCscBinaryPopulatedColumnOffsets, rightChildCscBinaryColumnOffsets,
                rightChildCscBinaryRowIndices, rightChildCscBinaryProbabilities);

        // Store all unary rules
        this.csrUnaryRowStartIndices = new int[numNonTerms() + 1];
        this.csrUnaryColumnIndices = new short[numUnaryProds()];
        this.csrUnaryProbabilities = new float[numUnaryProds()];

        storeUnaryRulesAsCsrMatrix(csrUnaryRowStartIndices, csrUnaryColumnIndices, csrUnaryProbabilities);
    }

    private ArrayList<Production> binaryProductionsByLeftChild(final ArrayList<Production> binaryProductions) {
        final ArrayList<Production> productionsByLeftChild = new ArrayList<Production>(binaryProductions.size());
        for (final Production p : binaryProductions) {
            productionsByLeftChild.add(new Production(p.leftChild, p.parent, p.rightChild, p.prob, nonTermSet, lexSet));
        }
        return productionsByLeftChild;
    }

    private ArrayList<Production> binaryProductionsByRightChild(final ArrayList<Production> binaryProductions) {
        final ArrayList<Production> productionsByRightChild = new ArrayList<Production>(binaryProductions.size());
        for (final Production p : binaryProductions) {
            productionsByRightChild
                    .add(new Production(p.rightChild, p.parent, p.leftChild, p.prob, nonTermSet, lexSet));
        }
        return productionsByRightChild;
    }
}
