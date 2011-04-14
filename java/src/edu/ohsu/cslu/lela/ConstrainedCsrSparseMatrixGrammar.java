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
package edu.ohsu.cslu.lela;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Production;

public class ConstrainedCsrSparseMatrixGrammar extends CsrSparseMatrixGrammar {

    private static final long serialVersionUID = 1L;

    final int[][] csrBinaryBaseStartIndices;
    final int[][] csrUnaryBaseStartIndices;

    /** A copy of the CSR grammar, mapped by left child -> parent,right child */
    public final int[] leftChildCsrBinaryRowIndices;
    public final int[] leftChildCsrBinaryColumnIndices;
    public final float[] leftChildCsrBinaryProbabilities;
    final int[][] leftChildCsrBaseStartIndices;
    final PerfectIntPairHashPackingFunction leftChildPackingFunction;

    /** A copy of the CSR grammar, mapped by right child -> parent,left child */
    public final int[] rightChildCsrBinaryRowIndices;
    public final int[] rightChildCsrBinaryColumnIndices;
    public final float[] rightChildCsrBinaryProbabilities;
    final int[][] rightChildCsrBaseStartIndices;
    final PerfectIntPairHashPackingFunction rightChildPackingFunction;

    final ProductionListGrammar parentGrammar;

    public ConstrainedCsrSparseMatrixGrammar(final ProductionListGrammar plg,
            final GrammarFormatType grammarFormat, final Class<? extends PackingFunction> functionClass) {
        super(plg.binaryProductions, plg.unaryProductions, plg.lexicalProductions, plg.vocabulary,
            plg.lexicon, grammarFormat, functionClass, false);

        this.csrUnaryBaseStartIndices = new int[numNonTerms()][];
        storeUnaryRulesAsCsrMatrix();
        this.parentGrammar = plg.parentGrammar;

        this.csrBinaryBaseStartIndices = new int[numNonTerms()][];
        storeBinaryRulesAsCsrMatrix(mapBinaryRulesByParent(binaryProductions, packingFunction),
            csrBinaryRowIndices, csrBinaryColumnIndices, csrBinaryProbabilities, csrBinaryBaseStartIndices,
            (PerfectIntPairHashPackingFunction) packingFunction);

        this.leftChildCsrBinaryRowIndices = new int[numNonTerms() + 1];
        this.leftChildCsrBinaryColumnIndices = new int[numBinaryProds()];
        this.leftChildCsrBinaryProbabilities = new float[numBinaryProds()];
        this.leftChildCsrBaseStartIndices = new int[numNonTerms()][];
        final ArrayList<Production> binaryProductionsByLeftChild = binaryProductionsByLeftChild();
        leftChildPackingFunction = this.new PerfectIntPairHashPackingFunction(binaryProductionsByLeftChild,
            numNonTerms() - 1);

        storeBinaryRulesAsCsrMatrix(
            mapBinaryRulesByParent(binaryProductionsByLeftChild, leftChildPackingFunction),
            leftChildCsrBinaryRowIndices, leftChildCsrBinaryColumnIndices, leftChildCsrBinaryProbabilities,
            leftChildCsrBaseStartIndices, leftChildPackingFunction);

        this.rightChildCsrBinaryRowIndices = new int[numNonTerms() + 1];
        this.rightChildCsrBinaryColumnIndices = new int[numBinaryProds()];
        this.rightChildCsrBinaryProbabilities = new float[numBinaryProds()];
        this.rightChildCsrBaseStartIndices = new int[numNonTerms()][];
        final ArrayList<Production> binaryProductionsByRightChild = binaryProductionsByRightChild();
        rightChildPackingFunction = this.new PerfectIntPairHashPackingFunction(binaryProductionsByRightChild,
            numNonTerms() - 1);
        storeBinaryRulesAsCsrMatrix(
            mapBinaryRulesByParent(binaryProductionsByRightChild, rightChildPackingFunction),
            rightChildCsrBinaryRowIndices, rightChildCsrBinaryColumnIndices,
            rightChildCsrBinaryProbabilities, rightChildCsrBaseStartIndices, rightChildPackingFunction);
    }

    private void storeBinaryRulesAsCsrMatrix(final Int2FloatOpenHashMap[] maps, final int[] csrRowIndices,
            final int[] csrColumnIndices, final float[] csrProbabilities, final int[][] baseStartIndices,
            final PerfectIntPairHashPackingFunction packingFunction) {

        final SplitVocabulary splitVocabulary = (SplitVocabulary) nonTermSet;

        // Store rules in CSR matrix
        int i = 0;
        for (int parent = 0; parent < numNonTerms(); parent++) {

            csrRowIndices[parent] = i;

            final int[] children = maps[parent].keySet().toIntArray();
            if (children.length > 0) {
                baseStartIndices[parent] = new int[splitVocabulary.baseVocabulary.size() + 1];
                baseStartIndices[parent][0] = i;
            }
            Arrays.sort(children);

            short baseLeftChild = 0;
            for (int j = 0; j < children.length; j++) {
                final int childPair = children[j];
                csrColumnIndices[i] = childPair;
                csrProbabilities[i] = maps[parent].get(childPair);

                final short leftChild = (short) packingFunction.unpackLeftChild(childPair);
                if (splitVocabulary.baseCategoryIndices[leftChild] != baseLeftChild) {
                    Arrays.fill(baseStartIndices[parent], baseLeftChild + 1,
                        splitVocabulary.baseCategoryIndices[leftChild] + 1, i);
                    baseLeftChild = splitVocabulary.baseCategoryIndices[leftChild];
                }
                i++;
            }
            if (children.length > 0) {
                Arrays.fill(baseStartIndices[parent], baseLeftChild + 1, baseStartIndices[parent].length, i);
            }
        }
        csrRowIndices[csrRowIndices.length - 1] = i;
    }

    private ArrayList<Production> binaryProductionsByLeftChild() {
        final ArrayList<Production> productionsByLeftChild = new ArrayList<Production>(
            binaryProductions.size());
        for (final Production p : binaryProductions) {
            productionsByLeftChild.add(new Production(p.leftChild, p.parent, p.rightChild, p.prob,
                nonTermSet, lexSet));
        }
        return productionsByLeftChild;
    }

    private ArrayList<Production> binaryProductionsByRightChild() {
        final ArrayList<Production> productionsByRightChild = new ArrayList<Production>(
            binaryProductions.size());
        for (final Production p : binaryProductions) {
            productionsByRightChild.add(new Production(p.rightChild, p.parent, p.leftChild, p.prob,
                nonTermSet, lexSet));
        }
        return productionsByRightChild;
    }

    @Override
    protected void storeUnaryRulesAsCsrMatrix() {

        // Bin all rules by parent, mapping child -> probability
        final Short2FloatOpenHashMap[] maps = new Short2FloatOpenHashMap[numNonTerms()];
        for (int i = 0; i < numNonTerms(); i++) {
            maps[i] = new Short2FloatOpenHashMap(1000);
        }

        for (final Production p : unaryProductions) {
            maps[p.parent].put((short) p.leftChild, p.prob);
        }

        final SplitVocabulary splitVocabulary = (SplitVocabulary) nonTermSet;

        // Store rules in CSR matrix
        int i = 0;
        for (int parent = 0; parent < numNonTerms(); parent++) {
            csrUnaryRowStartIndices[parent] = i;

            final short[] children = maps[parent].keySet().toShortArray();

            if (children.length > 0) {
                csrUnaryBaseStartIndices[parent] = new int[splitVocabulary.baseVocabulary.size() + 1];
                csrUnaryBaseStartIndices[parent][0] = i;
            }
            Arrays.sort(children);

            short baseChild = 0;
            for (int j = 0; j < children.length; j++) {
                final short child = children[j];
                csrUnaryColumnIndices[i] = child;
                csrUnaryProbabilities[i] = maps[parent].get(children[j]);

                if (splitVocabulary.baseCategoryIndices[child] != baseChild) {
                    Arrays.fill(csrUnaryBaseStartIndices[parent], baseChild + 1,
                        splitVocabulary.baseCategoryIndices[child] + 1, i);
                    baseChild = splitVocabulary.baseCategoryIndices[child];
                }
                i++;
            }
            if (children.length > 0) {
                Arrays.fill(csrUnaryBaseStartIndices[parent], baseChild + 1,
                    csrUnaryBaseStartIndices[parent].length, i);
            }
        }
        csrUnaryRowStartIndices[csrUnaryRowStartIndices.length - 1] = i;
    }
}
