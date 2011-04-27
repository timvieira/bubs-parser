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
import java.util.TreeSet;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.tests.JUnit;

public class ConstrainedCsrSparseMatrixGrammar extends CsrSparseMatrixGrammar {

    private static final long serialVersionUID = 1L;

    /**
     * Start indices in the main grammar matrix array for split categories of each base non-terminal. Indexed first by
     * split parent and then by unsplit child. Dimensions = |V_split| x (|V_unsplit| + 1)
     */
    final int[][] csrBinaryBaseStartIndices;
    final int[][] csrUnaryBaseStartIndices;

    /**
     * A copy of the CSR grammar, mapped by left child -> parent,right child (note that this confuses the arguments to
     * pack/unpack method of the PackingFunction).
     */
    public final int[] leftChildCsrBinaryRowIndices;
    public final int[] leftChildCsrBinaryColumnIndices;
    public final float[] leftChildCsrBinaryProbabilities;
    final int[][] leftChildCsrBaseStartIndices;
    final PerfectIntPairHashPackingFunction leftChildPackingFunction;

    /**
     * A copy of the CSR grammar, mapped by right child -> parent,left child (note that this confuses the arguments to
     * pack/unpack method of the PackingFunction)
     */
    public final int[] rightChildCsrBinaryRowIndices;
    public final int[] rightChildCsrBinaryColumnIndices;
    public final float[] rightChildCsrBinaryProbabilities;
    final PerfectIntPairHashPackingFunction rightChildPackingFunction;
    /**
     * Start indices in the right-child grammar matrix array for split categories of each base non-terminal. Indexed
     * first by split right child and then by unsplit parent. Dimensions = |V_split| x (|V_unsplit| + 1)
     */
    final int[][] rightChildCsrBaseStartIndices;

    final ProductionListGrammar baseGrammar;

    public ConstrainedCsrSparseMatrixGrammar(final ProductionListGrammar plg, final GrammarFormatType grammarFormat,
            final Class<? extends PackingFunction> functionClass) {
        super(plg.binaryProductions, plg.unaryProductions, plg.lexicalProductions, plg.vocabulary, plg.lexicon,
                grammarFormat, functionClass, false);

        this.csrUnaryBaseStartIndices = new int[numNonTerms()][];
        storeUnaryRulesAsCsrMatrix();
        this.baseGrammar = plg.baseGrammar;

        this.csrBinaryBaseStartIndices = new int[numNonTerms()][];
        storeBinaryRulesAsCsrMatrix(mapBinaryRulesByParent(binaryProductions, packingFunction), csrBinaryRowIndices,
                csrBinaryColumnIndices, csrBinaryProbabilities, csrBinaryBaseStartIndices,
                (PerfectIntPairHashPackingFunction) packingFunction);

        this.leftChildCsrBinaryRowIndices = new int[numNonTerms() + 1];
        this.leftChildCsrBinaryColumnIndices = new int[numBinaryProds()];
        this.leftChildCsrBinaryProbabilities = new float[numBinaryProds()];
        this.leftChildCsrBaseStartIndices = new int[numNonTerms()][];
        final ArrayList<Production> binaryProductionsByLeftChild = binaryProductionsByLeftChild();
        leftChildPackingFunction = this.new PerfectIntPairHashPackingFunction(binaryProductionsByLeftChild,
                numNonTerms() - 1);

        storeBinaryRulesAsCsrMatrix(mapBinaryRulesByParent(binaryProductionsByLeftChild, leftChildPackingFunction),
                leftChildCsrBinaryRowIndices, leftChildCsrBinaryColumnIndices, leftChildCsrBinaryProbabilities,
                leftChildCsrBaseStartIndices, leftChildPackingFunction);

        this.rightChildCsrBinaryRowIndices = new int[numNonTerms() + 1];
        this.rightChildCsrBinaryColumnIndices = new int[numBinaryProds()];
        this.rightChildCsrBinaryProbabilities = new float[numBinaryProds()];
        this.rightChildCsrBaseStartIndices = new int[numNonTerms()][];
        final ArrayList<Production> binaryProductionsByRightChild = binaryProductionsByRightChild();
        rightChildPackingFunction = this.new PerfectIntPairHashPackingFunction(binaryProductionsByRightChild,
                numNonTerms() - 1);
        storeBinaryRulesAsCsrMatrix(mapBinaryRulesByParent(binaryProductionsByRightChild, rightChildPackingFunction),
                rightChildCsrBinaryRowIndices, rightChildCsrBinaryColumnIndices, rightChildCsrBinaryProbabilities,
                rightChildCsrBaseStartIndices, rightChildPackingFunction);

        storeUnaryRulesAsCscMatrix(unaryProductions, cscUnaryColumnOffsets, cscUnaryRowIndices, cscUnaryProbabilities);
    }

    /**
     * 
     * @param maps
     * @param csrRowIndices
     * @param csrColumnIndices
     * @param csrProbabilities
     * @param baseStartIndices Start indices in the main grammar array of rules for each parent / unsplit child
     *            combination. 2-dimensional array indexed by split parent and unsplit child.
     * @param packingFunction
     */
    private void storeBinaryRulesAsCsrMatrix(final Int2FloatOpenHashMap[] maps, final int[] csrRowIndices,
            final int[] csrColumnIndices, final float[] csrProbabilities, final int[][] baseStartIndices,
            final PerfectIntPairHashPackingFunction packingFunction) {

        final SplitVocabulary splitVocabulary = (SplitVocabulary) nonTermSet;

        // Store rules in CSR matrix
        int i = 0; // Current position in main grammar matrix array
        for (int parent = 0; parent < numNonTerms(); parent++) {
            csrRowIndices[parent] = i;

            final int[] children = maps[parent].keySet().toIntArray();
            if (children.length > 0) {
                baseStartIndices[parent] = new int[splitVocabulary.baseVocabulary.size() + 1];
                baseStartIndices[parent][0] = i;
            }
            Arrays.sort(children);

            short baseLeftChild = -1;
            for (int j = 0; j < children.length; j++) {
                final int childPair = children[j];
                csrColumnIndices[i] = childPair;
                csrProbabilities[i] = maps[parent].get(childPair);

                final short leftChild = (short) packingFunction.unpackLeftChild(childPair);

                // When we find a left child split from a new base non-terminal, fill the array mapping split NTs ->
                // base NTs
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
        final ArrayList<Production> productionsByLeftChild = new ArrayList<Production>(binaryProductions.size());
        for (final Production p : binaryProductions) {
            productionsByLeftChild.add(new Production(p.leftChild, p.parent, p.rightChild, p.prob, nonTermSet, lexSet));
        }
        return productionsByLeftChild;
    }

    private ArrayList<Production> binaryProductionsByRightChild() {
        final ArrayList<Production> productionsByRightChild = new ArrayList<Production>(binaryProductions.size());
        for (final Production p : binaryProductions) {
            productionsByRightChild
                    .add(new Production(p.rightChild, p.parent, p.leftChild, p.prob, nonTermSet, lexSet));
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
                Arrays.fill(csrUnaryBaseStartIndices[parent], baseChild + 1, csrUnaryBaseStartIndices[parent].length, i);
            }
        }
        csrUnaryRowStartIndices[csrUnaryRowStartIndices.length - 1] = i;
    }

    private String unaryRulesAsString(final boolean fraction) {

        final TreeSet<String> unaryRules = new TreeSet<String>();
        for (int parent = 0; parent < numNonTerms(); parent++) {
            for (int j = csrUnaryRowStartIndices[parent]; j < csrUnaryRowStartIndices[parent + 1]; j++) {
                final short child = csrUnaryColumnIndices[j];
                final float probability = csrUnaryProbabilities[j];

                if (fraction) {
                    unaryRules.add(String.format("%s -> %s %s", nonTermSet.getSymbol(parent),
                            nonTermSet.getSymbol(child), JUnit.fraction(probability)));
                } else {
                    unaryRules.add(String.format("%s -> %s %.4f", nonTermSet.getSymbol(parent),
                            nonTermSet.getSymbol(child), probability));
                }
            }
        }

        final StringBuilder sb = new StringBuilder(1024);
        for (final String rule : unaryRules) {
            sb.append(rule);
            sb.append('\n');
        }
        return sb.toString();
    }

    private String lexicalRulesAsString(final boolean fraction) {
        final TreeSet<String> lexicalRules = new TreeSet<String>();
        for (int child = 0; child < lexSet.size(); child++) {
            for (final Production p : lexicalProdsByChild[child]) {
                if (fraction) {
                    lexicalRules.add(String.format("%s -> %s %s", nonTermSet.getSymbol(p.parent),
                            lexSet.getSymbol(child), JUnit.fraction(p.prob)));
                } else {
                    lexicalRules.add(String.format("%s -> %s %.4f", nonTermSet.getSymbol(p.parent),
                            lexSet.getSymbol(child), p.prob));
                }
            }
        }

        final StringBuilder sb = new StringBuilder(1024);
        for (final String rule : lexicalRules) {
            sb.append(rule);
            sb.append('\n');
        }
        return sb.toString();
    }

    public String mainGrammarString(final boolean fraction) {
        final TreeSet<String> binaryRules = new TreeSet<String>();
        for (int parent = 0; parent < numNonTerms(); parent++) {
            for (int j = csrBinaryRowIndices[parent]; j < csrBinaryRowIndices[parent + 1]; j++) {
                final int childPair = csrBinaryColumnIndices[j];
                final float probability = csrBinaryProbabilities[j];
                if (fraction) {
                    binaryRules.add(String.format("%s -> %s %s %s", nonTermSet.getSymbol(parent),
                            nonTermSet.getSymbol(packingFunction.unpackLeftChild(childPair)),
                            nonTermSet.getSymbol(packingFunction.unpackRightChild(childPair)),
                            JUnit.fraction(probability)));
                } else {
                    binaryRules.add(String.format("%s -> %s %s %.4f", nonTermSet.getSymbol(parent),
                            nonTermSet.getSymbol(packingFunction.unpackLeftChild(childPair)),
                            nonTermSet.getSymbol(packingFunction.unpackRightChild(childPair)), probability));
                }
            }
        }

        final StringBuilder sb = new StringBuilder(1024);
        for (final String rule : binaryRules) {
            sb.append(rule);
            sb.append('\n');
        }

        sb.append(unaryRulesAsString(fraction));
        sb.append(Grammar.DELIMITER);
        sb.append('\n');
        sb.append(lexicalRulesAsString(fraction));

        return sb.toString();
    }

    public String leftGrammarString(final boolean fraction) {
        final TreeSet<String> binaryRules = new TreeSet<String>();
        for (int leftChild = 0; leftChild < numNonTerms(); leftChild++) {
            for (int j = leftChildCsrBinaryRowIndices[leftChild]; j < leftChildCsrBinaryRowIndices[leftChild + 1]; j++) {
                final int childPair = csrBinaryColumnIndices[j];
                final float probability = csrBinaryProbabilities[j];
                if (fraction) {
                    binaryRules.add(String.format("%s -> %s %s %s",
                            nonTermSet.getSymbol(packingFunction.unpackLeftChild(childPair)),
                            nonTermSet.getSymbol(leftChild),
                            nonTermSet.getSymbol(packingFunction.unpackRightChild(childPair)),
                            JUnit.fraction(probability)));
                } else {
                    binaryRules.add(String.format("%s -> %s %s %.4f",
                            nonTermSet.getSymbol(packingFunction.unpackLeftChild(childPair)),
                            nonTermSet.getSymbol(leftChild),
                            nonTermSet.getSymbol(packingFunction.unpackRightChild(childPair)), probability));
                }
            }
        }

        final StringBuilder sb = new StringBuilder(1024);
        for (final String rule : binaryRules) {
            sb.append(rule);
            sb.append('\n');
        }

        sb.append(unaryRulesAsString(fraction));
        sb.append(Grammar.DELIMITER);
        sb.append('\n');
        sb.append(lexicalRulesAsString(fraction));

        return sb.toString();
    }

    public String rightGrammarString(final boolean fraction) {
        final TreeSet<String> binaryRules = new TreeSet<String>();
        for (int rightChild = 0; rightChild < numNonTerms(); rightChild++) {
            for (int j = rightChildCsrBinaryRowIndices[rightChild]; j < rightChildCsrBinaryRowIndices[rightChild + 1]; j++) {
                final int childPair = csrBinaryColumnIndices[j];
                final float probability = csrBinaryProbabilities[j];
                if (fraction) {
                    binaryRules.add(String.format("%s -> %s %s %s",
                            nonTermSet.getSymbol(packingFunction.unpackLeftChild(childPair)),
                            nonTermSet.getSymbol(packingFunction.unpackRightChild(childPair)),
                            nonTermSet.getSymbol(rightChild), JUnit.fraction(probability)));
                } else {
                    binaryRules.add(String.format("%s -> %s %s %.4f",
                            nonTermSet.getSymbol(packingFunction.unpackLeftChild(childPair)),
                            nonTermSet.getSymbol(packingFunction.unpackRightChild(childPair)),
                            nonTermSet.getSymbol(rightChild), probability));
                }
            }
        }

        final StringBuilder sb = new StringBuilder(1024);
        for (final String rule : binaryRules) {
            sb.append(rule);
            sb.append('\n');
        }

        sb.append(unaryRulesAsString(fraction));
        sb.append(Grammar.DELIMITER);
        sb.append('\n');
        sb.append(lexicalRulesAsString(fraction));

        return sb.toString();
    }

    @Override
    public String toString() {
        // TODO Switch from fractions to logs
        return mainGrammarString(true);
    }
}
