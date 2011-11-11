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

import static org.junit.Assert.assertEquals;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Pattern;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Stores a grammar as lists of rules. This representation is not efficient for parsing, but is quite effective and
 * intuitive for splitting and re-merging of states during latent-variable grammar learning.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 */
public class ProductionListGrammar implements Cloneable {

    public final SplitVocabulary vocabulary;
    public final SymbolSet<String> lexicon;

    public final ArrayList<Production> binaryProductions;
    public final ArrayList<Production> unaryProductions;
    public final ArrayList<Production> lexicalProductions;

    private final String startSymbol;

    final ProductionListGrammar baseGrammar;

    private final static Pattern SUBSTATE_PATTERN = Pattern.compile("^.*_[0-9]+$");
    private final static float LOG_ONE_HALF = (float) Math.log(.5);

    /**
     * Constructs a production-list grammar based on a {@link StringCountGrammar}, inducing a vocabulary sorted by
     * binary parent count.
     * 
     * @param countGrammar
     */
    public ProductionListGrammar(final StringCountGrammar countGrammar) {

        this.vocabulary = countGrammar.induceVocabulary(countGrammar.binaryParentCountComparator());
        this.lexicon = countGrammar.induceLexicon();

        this.binaryProductions = countGrammar.binaryProductions(vocabulary);
        this.unaryProductions = countGrammar.unaryProductions(vocabulary);
        this.lexicalProductions = countGrammar.lexicalProductions(vocabulary, lexicon);

        this.startSymbol = countGrammar.startSymbol;
        this.baseGrammar = this;
    }

    /**
     * Constructs a production-list grammar based on lists of productions.
     */
    ProductionListGrammar(final SplitVocabulary vocabulary, final SymbolSet<String> lexicon,
            final ArrayList<Production> binaryProductions, final ArrayList<Production> unaryProductions,
            final ArrayList<Production> lexicalProductions) {

        this.vocabulary = vocabulary;
        this.lexicon = lexicon;

        this.binaryProductions = binaryProductions;
        this.unaryProductions = unaryProductions;
        this.lexicalProductions = lexicalProductions;

        this.startSymbol = vocabulary.getSymbol(vocabulary.startSymbol());
        this.baseGrammar = this;
    }

    private ProductionListGrammar(final ProductionListGrammar parentGrammar, final SplitVocabulary vocabulary,
            final SymbolSet<String> lexicon) {
        this.vocabulary = vocabulary;
        this.lexicon = lexicon;

        this.binaryProductions = new ArrayList<Production>();
        this.unaryProductions = new ArrayList<Production>();
        this.lexicalProductions = new ArrayList<Production>();

        this.startSymbol = parentGrammar.startSymbol;
        this.baseGrammar = parentGrammar.baseGrammar;
    }

    /**
     * Returns the log probability of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return Log probability of the specified rule.
     */
    public final float binaryLogProbability(final String parent, final String leftChild, final String rightChild) {

        final int intParent = vocabulary.getIndex(parent);
        final int intLeftChild = vocabulary.getIndex(leftChild);
        final int intRightChild = vocabulary.getIndex(rightChild);

        if (intParent < 0 || intLeftChild < 0 || intRightChild < 0) {
            return Float.NEGATIVE_INFINITY;
        }

        for (final Production p : binaryProductions) {
            if (p.parent == intParent && p.leftChild == intLeftChild && p.rightChild == intRightChild) {
                return p.prob;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Returns the log probability of a unary rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public final float unaryLogProbability(final String parent, final String child) {

        final int intParent = vocabulary.getIndex(parent);
        final int intChild = vocabulary.getIndex(child);

        if (intParent < 0 || intChild < 0) {
            return Float.NEGATIVE_INFINITY;
        }

        for (final Production p : unaryProductions) {
            if (p.parent == intParent && p.leftChild == intChild) {
                return p.prob;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Returns the log probability of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public final float lexicalLogProbability(final String parent, final String child) {

        final int intParent = vocabulary.getIndex(parent);
        final int intChild = lexicon.getIndex(child);

        if (intParent < 0 || intChild < 0) {
            return Float.NEGATIVE_INFINITY;
        }

        for (final Production p : lexicalProductions) {
            if (p.parent == intParent && p.leftChild == intChild) {
                return p.prob;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * @return A string representation of the binary and unary rules, in the same format used by {@link Grammar}
     */
    public String pcfgString() {
        final StringBuilder sb = new StringBuilder();

        // Handle start symbol separately
        sb.append(startSymbol);
        sb.append('\n');

        for (final Production p : binaryProductions) {
            sb.append(String.format("%s -> %s %s %.6f\n", vocabulary.getSymbol(p.parent),
                    vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), p.prob));
        }

        for (final Production p : unaryProductions) {
            sb.append(String.format("%s -> %s %.6f\n", vocabulary.getSymbol(p.parent),
                    vocabulary.getSymbol(p.leftChild), p.prob));
        }

        return sb.toString();
    }

    /**
     * @return A string representation of the lexicon, in the same format used by {@link Grammar}
     */
    public String lexiconString() {
        final StringBuilder sb = new StringBuilder();

        for (final Production p : lexicalProductions) {
            sb.append(String.format("%s -> %s %.6f\n", vocabulary.getSymbol(p.parent), lexicon.getSymbol(p.leftChild),
                    p.prob));
        }

        return sb.toString();
    }

    /**
     * Splits each non-terminal in the grammar into 2 sub-states, constructing a new grammar, vocabulary, and lexicon.
     * 
     * @param noiseGenerator Source of random noise
     * @return Newly-constructed grammar
     */
    public ProductionListGrammar split(final NoiseGenerator noiseGenerator) {
        // Produce a new vocabulary, splitting each non-terminal into two substates

        final SplitVocabulary splitVocabulary = new SplitVocabulary(vocabulary);

        // Add a dummy non-terminal for the start symbol. The start symbol is always index 0, and using index 1 makes
        // computing other splits simpler. We'll always re-merge the dummy symbol.
        splitVocabulary.addSymbol(startSymbol);
        splitVocabulary.addSymbol(startSymbol + "_1");
        for (int i = 1; i < vocabulary.size(); i++) {
            final String[] substates = substates(vocabulary.getSymbol(i));
            splitVocabulary.addSymbol(substates[0]);
            splitVocabulary.addSymbol(substates[1]);
        }

        final ProductionListGrammar tmpGrammar = new ProductionListGrammar(this, splitVocabulary, lexicon);

        // Iterate through each rule, creating split rules in the new grammar

        final float logOneHalf = (float) Math.log(.5);
        final float logOneFourth = (float) Math.log(.25);

        // Split each binary production into 8. Each split production has 1/4 the probability of the original production
        // + noise
        for (final Production p : binaryProductions) {

            final int splitParent0 = p.parent << 1;
            final int splitParent1 = splitParent0 + 1;
            assert splitParent1 < splitVocabulary.size();

            final int splitLeftChild0 = p.leftChild << 1;
            final int splitLeftChild1 = splitLeftChild0 + 1;
            assert splitLeftChild1 < splitVocabulary.size();

            final int splitRightChild0 = p.rightChild << 1;
            final int splitRightChild1 = splitRightChild0 + 1;
            assert splitRightChild1 < splitVocabulary.size();

            final float[] noise = noiseGenerator.noise(8);

            tmpGrammar.binaryProductions.add(new Production(splitParent0, splitLeftChild0, splitRightChild0, p.prob
                    + logOneFourth + noise[0], splitVocabulary, lexicon));

            tmpGrammar.binaryProductions.add(new Production(splitParent0, splitLeftChild0, splitRightChild1, p.prob
                    + logOneFourth + noise[1], splitVocabulary, lexicon));

            tmpGrammar.binaryProductions.add(new Production(splitParent0, splitLeftChild1, splitRightChild0, p.prob
                    + logOneFourth + noise[2], splitVocabulary, lexicon));

            tmpGrammar.binaryProductions.add(new Production(splitParent0, splitLeftChild1, splitRightChild1, p.prob
                    + logOneFourth + noise[3], splitVocabulary, lexicon));

            tmpGrammar.binaryProductions.add(new Production(splitParent1, splitLeftChild0, splitRightChild0, p.prob
                    + logOneFourth + noise[4], splitVocabulary, lexicon));

            tmpGrammar.binaryProductions.add(new Production(splitParent1, splitLeftChild0, splitRightChild1, p.prob
                    + logOneFourth + noise[5], splitVocabulary, lexicon));

            tmpGrammar.binaryProductions.add(new Production(splitParent1, splitLeftChild1, splitRightChild0, p.prob
                    + logOneFourth + noise[6], splitVocabulary, lexicon));

            tmpGrammar.binaryProductions.add(new Production(splitParent1, splitLeftChild1, splitRightChild1, p.prob
                    + logOneFourth + noise[7], splitVocabulary, lexicon));
        }

        // Split unary productions in 4ths. Each split production has 1/2 the probability of the original
        // production + noise
        for (final Production p : unaryProductions) {

            final int splitChild0 = p.leftChild << 1;
            final int splitChild1 = splitChild0 + 1;
            assert splitChild1 < splitVocabulary.size();

            // Special-case for the start symbol. Since we do not split it, we only split unaries of which it is the
            // parent in two
            if (p.parent == 0) {
                final float[] noise = noiseGenerator.noise(2);
                tmpGrammar.unaryProductions.add(new Production(0, splitChild0, p.prob + logOneHalf + noise[0], false,
                        splitVocabulary, lexicon));

                tmpGrammar.unaryProductions.add(new Production(0, splitChild1, p.prob + logOneHalf + noise[1], false,
                        splitVocabulary, lexicon));

            } else {
                final float[] noise = noiseGenerator.noise(4);

                final int splitParent0 = p.parent << 1;
                final int splitParent1 = splitParent0 + 1;
                assert splitParent1 < splitVocabulary.size();

                tmpGrammar.unaryProductions.add(new Production(splitParent0, splitChild0, p.prob + logOneHalf
                        + noise[0], false, splitVocabulary, lexicon));

                tmpGrammar.unaryProductions.add(new Production(splitParent0, splitChild1, p.prob + logOneHalf
                        + noise[1], false, splitVocabulary, lexicon));

                tmpGrammar.unaryProductions.add(new Production(splitParent1, splitChild0, p.prob + logOneHalf
                        + noise[2], false, splitVocabulary, lexicon));

                tmpGrammar.unaryProductions.add(new Production(splitParent1, splitChild1, p.prob + logOneHalf
                        + noise[3], false, splitVocabulary, lexicon));
            }
        }

        // Split lexical productions in half. Each split production has the same probability as the original production
        // TODO Is this correct? Or should we add noise here as well?
        for (final Production p : lexicalProductions) {
            final int splitParent0 = p.parent << 1;
            final int splitParent1 = splitParent0 + 1;
            assert splitParent1 < splitVocabulary.size();

            // final float[] noise = noiseGenerator.noise(2);
            tmpGrammar.lexicalProductions.add(new Production(splitParent0, p.child(), p.prob, true, splitVocabulary,
                    lexicon));

            tmpGrammar.lexicalProductions.add(new Production(splitParent1, p.child(), p.prob, true, splitVocabulary,
                    lexicon));
        }

        return tmpGrammar.normalizeProbabilities();
    }

    private String[] substates(final String state) {
        if (SUBSTATE_PATTERN.matcher(state).matches()) {
            final String[] rootAndIndex = state.split("_");
            final int substateIndex = Integer.parseInt(rootAndIndex[1]);
            return new String[] { rootAndIndex[0] + '_' + (substateIndex * 2),
                    rootAndIndex[0] + '_' + (substateIndex * 2 + 1) };
        }
        return new String[] { state + "_0", state + "_1" };
    }

    /**
     * @return a fully normalized version of the grammar
     */
    final ProductionListGrammar normalizeProbabilities() {

        // System.out.println("Normalizing");
        //
        // Sum up the total probability of all rules headed by each parent.
        //
        final FloatArrayList[] probabilitiesByParent = new FloatArrayList[vocabulary.size()];

        for (int i = 0; i < probabilitiesByParent.length; i++) {
            probabilitiesByParent[i] = new FloatArrayList();
        }

        for (final Production p : binaryProductions) {
            probabilitiesByParent[p.parent].add(p.prob);
        }

        for (final Production p : unaryProductions) {
            probabilitiesByParent[p.parent].add(p.prob);
        }

        for (final Production p : lexicalProductions) {
            probabilitiesByParent[p.parent].add(p.prob);
        }

        //
        // Compute the required normalization for each parent (the difference from a normalized probability
        // distribution)
        //
        final float[] normalization = new float[probabilitiesByParent.length];
        for (int i = 0; i < normalization.length; i++) {
            if (probabilitiesByParent[i].size() > 0) {
                final double totalProbability = edu.ohsu.cslu.util.Math.sumExp(probabilitiesByParent[i].toFloatArray());
                normalization[i] = (float) Math.log(1 - (totalProbability - 1) / totalProbability);
                assert !Float.isInfinite(normalization[i]);
                assert !Float.isNaN(normalization[i]);
            }
        }

        //
        // Re-normalize rule probabilities
        //
        final ProductionListGrammar normalizedGrammar = new ProductionListGrammar(this, vocabulary, lexicon);
        for (final Production p : binaryProductions) {
            normalizedGrammar.binaryProductions.add(new Production(p.parent, p.leftChild, p.rightChild, Math.min(0f,
                    p.prob + normalization[p.parent]), vocabulary, lexicon));
        }
        for (final Production p : unaryProductions) {
            normalizedGrammar.unaryProductions.add(new Production(p.parent, p.leftChild, Math.min(0f, p.prob
                    + normalization[p.parent]), false, vocabulary, lexicon));
        }
        for (final Production p : lexicalProductions) {
            normalizedGrammar.lexicalProductions.add(new Production(p.parent, p.leftChild, Math.min(0f, p.prob
                    + normalization[p.parent]), true, vocabulary, lexicon));
        }
        return normalizedGrammar;
    }

    /**
     * Re-merges splits specified by non-terminal indices, producing a new {@link ProductionListGrammar} with its own
     * vocabulary and lexicon.
     * 
     * @param indices Non-terminal indices to merge. Each index is assumed to be the <i>second</i> of a split pair.
     *            i.e., if A and B were split, merging A into B is equivalent to merging B into A. The merge operation
     *            assumes that the indices will be of each non-terminal B.
     * @return Merged grammar
     */
    public ProductionListGrammar merge(final short[] indices) {

        // Create merged vocabulary and map from old vocabulary indices to new
        final Short2ShortOpenHashMap parentToMergedIndexMap = new Short2ShortOpenHashMap();

        // Set of merged indices which were merged 'into'
        final ShortOpenHashSet mergedIndices = new ShortOpenHashSet();

        short j = 0;
        Arrays.sort(indices);
        final ArrayList<String> mergedSymbols = new ArrayList<String>();
        mergedSymbols.add(startSymbol);

        String previousRoot = "";
        int nextSubstate = 0;

        for (short i = 1; i < vocabulary.size(); i++) {
            if (j < indices.length && indices[j] == i) {
                j++;
                mergedIndices.add((short) (mergedSymbols.size() - 1));
            } else {
                // This would be much shorter and clearer if Java had tuples...
                final String mergedRoot = vocabulary.getSymbol(i).split("_")[0];

                if (mergedRoot.equals(previousRoot)) {
                    // Add the next split index in order, which may not match that of the split grammar symbol
                    mergedSymbols.add(previousRoot + '_' + nextSubstate);
                    nextSubstate++;
                } else {
                    mergedSymbols.add(mergedRoot + "_0");
                    nextSubstate = 1;
                }
                previousRoot = mergedRoot;
            }
            parentToMergedIndexMap.put(i, (short) (mergedSymbols.size() - 1));
        }

        final SplitVocabulary mergedVocabulary = new SplitVocabulary(mergedSymbols, vocabulary, parentToMergedIndexMap,
                mergedIndices);

        // Create maps to store new rules in
        final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> binaryRuleMap = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
        final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> unaryRuleMap = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
        final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> lexicalRuleMap = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();

        // For each existing rule, compute the new (merged) non-terminals and add the rule probability to the
        // rule map.
        // If multiple split rules merge into a single merged rule, sum the probabilities.

        for (final Production p : binaryProductions) {
            final short mergedParent = parentToMergedIndexMap.get((short) p.parent);
            addBinaryRuleProbability(binaryRuleMap, mergedParent, parentToMergedIndexMap.get((short) p.leftChild),
                    parentToMergedIndexMap.get((short) p.rightChild), p.prob
                            + (mergedIndices.contains(mergedParent) && mergedParent != 0 ? LOG_ONE_HALF : 0));
        }

        for (final Production p : unaryProductions) {
            final short mergedParent = parentToMergedIndexMap.get((short) p.parent);
            addUnaryRuleProbability(unaryRuleMap, mergedParent, parentToMergedIndexMap.get((short) p.leftChild), p.prob
                    + (mergedIndices.contains(mergedParent) && mergedParent != 0 ? LOG_ONE_HALF : 0));
        }

        for (final Production p : lexicalProductions) {
            final short mergedParent = parentToMergedIndexMap.get((short) p.parent);
            addLexicalRuleProbability(lexicalRuleMap, mergedParent, p.leftChild,
                    p.prob + (mergedIndices.contains(mergedParent) && mergedParent != 0 ? LOG_ONE_HALF : 0));
        }

        final ProductionListGrammar mergedGrammar = new ProductionListGrammar(baseGrammar, mergedVocabulary, lexicon);
        mergedGrammar.addToBinaryProductionsList(binaryRuleMap);
        mergedGrammar.addToUnaryProductionsList(unaryRuleMap);
        mergedGrammar.addToLexicalProductionList(lexicalRuleMap);
        return mergedGrammar;
    }

    private void addBinaryRuleProbability(
            final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> binaryRuleMap,
            final short parent, final short leftChild, final short rightChild, final float probability) {

        Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = binaryRuleMap.get(parent);
        if (leftChildMap == null) {
            leftChildMap = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
            binaryRuleMap.put(parent, leftChildMap);
        }

        Short2FloatOpenHashMap rightChildMap = leftChildMap.get(leftChild);
        if (rightChildMap == null) {
            rightChildMap = new Short2FloatOpenHashMap();
            rightChildMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
            leftChildMap.put(leftChild, rightChildMap);
        }

        final float p = edu.ohsu.cslu.util.Math.logSum(rightChildMap.get(rightChild), probability);
        assert p <= 1e-5f;
        rightChildMap.put(rightChild, Math.min(0f, p));
    }

    private void addToBinaryProductionsList(
            final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> binaryRuleMap) {

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!binaryRuleMap.containsKey(parent)) {
                continue;
            }

            final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = binaryRuleMap.get(parent);

            for (short leftChild = 0; leftChild < vocabulary.size(); leftChild++) {
                if (!leftChildMap.containsKey(leftChild)) {
                    continue;
                }

                final Short2FloatOpenHashMap rightChildMap = leftChildMap.get(leftChild);

                for (short rightChild = 0; rightChild < vocabulary.size(); rightChild++) {
                    if (!rightChildMap.containsKey(rightChild)) {
                        continue;
                    }

                    binaryProductions.add(new Production(parent, leftChild, rightChild, rightChildMap.get(rightChild),
                            vocabulary, lexicon));
                }
            }
        }
    }

    private void addUnaryRuleProbability(final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> unaryRuleMap,
            final short parent, final short child, final float probability) {

        Short2FloatOpenHashMap childMap = unaryRuleMap.get(parent);
        if (childMap == null) {
            childMap = new Short2FloatOpenHashMap();
            childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
            unaryRuleMap.put(parent, childMap);
        }

        final float p = edu.ohsu.cslu.util.Math.logSum(childMap.get(child), probability);
        assert p <= 1e-5f;
        childMap.put(child, Math.min(0f, p));
    }

    private void addToUnaryProductionsList(final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> unaryRuleMap) {

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!unaryRuleMap.containsKey(parent)) {
                continue;
            }

            final Short2FloatOpenHashMap childMap = unaryRuleMap.get(parent);

            for (short child = 0; child < vocabulary.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                unaryProductions.add(new Production(parent, child, childMap.get(child), false, vocabulary, lexicon));
            }
        }
    }

    private void addLexicalRuleProbability(final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> lexicalRuleMap,
            final short parent, final int child, final float probability) {

        Int2FloatOpenHashMap childMap = lexicalRuleMap.get(parent);
        if (childMap == null) {
            childMap = new Int2FloatOpenHashMap();
            childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
            lexicalRuleMap.put(parent, childMap);
        }

        final float p = edu.ohsu.cslu.util.Math.logSum(childMap.get(child), probability);
        assert p <= 1e-5f;
        childMap.put(child, Math.min(0f, p));
    }

    private void addToLexicalProductionList(final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> lexicalRuleMap) {

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!lexicalRuleMap.containsKey(parent)) {
                continue;
            }

            final Int2FloatOpenHashMap childMap = lexicalRuleMap.get(parent);

            for (int child = 0; child < lexicon.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                lexicalProductions.add(new Production(parent, child, childMap.get(child), true, vocabulary, lexicon));
            }
        }
    }

    /**
     * Verifies that the grammar rules for each parent non-terminal sum to 1. Used for unit testing
     */
    void verifyProbabilityDistribution() {

        @SuppressWarnings("unchecked")
        final List<Production>[] prodsByParent = new List[vocabulary.size()];
        for (int i = 0; i < vocabulary.size(); i++) {
            prodsByParent[i] = new ArrayList<Production>();
        }

        for (final Production p : binaryProductions) {
            prodsByParent[p.parent].add(p);
        }

        for (final Production p : unaryProductions) {
            prodsByParent[p.parent].add(p);
        }

        for (final Production p : lexicalProductions) {
            prodsByParent[p.parent].add(p);
        }

        for (int parent = 0, j = 0; parent < prodsByParent.length; parent++, j = 0) {
            final float[] probabilities = new float[prodsByParent[parent].size()];
            for (final Production p : prodsByParent[parent]) {
                probabilities[j++] = p.prob;
            }
            final float logSum = probabilities.length == 0 ? 0 : edu.ohsu.cslu.util.Math.logSumExp(probabilities);
            assertEquals("Invalid probability distribution for parent " + vocabulary.getSymbol(parent), 0, logSum, .001);
        }
    }

    public void verifyVsExpectedGrammar(final ProductionListGrammar unsplitPlg) {
        final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> unsplitBinaryProbabilities = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
        final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> unaryProbabilities = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
        final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> lexicalProbabilities = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();

        for (final Production p : binaryProductions) {

            Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = unsplitBinaryProbabilities
                    .get((short) p.parent);
            if (leftChildMap == null) {
                leftChildMap = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
                unsplitBinaryProbabilities.put((short) p.parent, leftChildMap);
            }

            Short2FloatOpenHashMap rightChildMap = leftChildMap.get((short) p.leftChild);
            if (rightChildMap == null) {
                rightChildMap = new Short2FloatOpenHashMap();
                rightChildMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                leftChildMap.put((short) p.leftChild, rightChildMap);
            }

            rightChildMap.put((short) p.rightChild,
                    edu.ohsu.cslu.util.Math.logSum(rightChildMap.get((short) p.rightChild), p.prob));
        }

        for (final Production p : unaryProductions) {

            Short2FloatOpenHashMap childMap = unaryProbabilities.get((short) p.parent);
            if (childMap == null) {
                childMap = new Short2FloatOpenHashMap();
                childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                unaryProbabilities.put((short) p.parent, childMap);
            }

            childMap.put((short) p.leftChild, edu.ohsu.cslu.util.Math.logSum(childMap.get((short) p.leftChild), p.prob));
        }

        for (final Production p : lexicalProductions) {

            Int2FloatOpenHashMap childMap = lexicalProbabilities.get((short) p.parent);
            if (childMap == null) {
                childMap = new Int2FloatOpenHashMap();
                childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                lexicalProbabilities.put((short) p.parent, childMap);
            }

            childMap.put((short) p.leftChild, edu.ohsu.cslu.util.Math.logSum(childMap.get((short) p.leftChild), p.prob));
        }

        //
        // Verify that the summed probabilities from the current grammar match those from the unsplit grammar
        //
        for (final Production p : unsplitPlg.binaryProductions) {
            assertEquals(
                    "Verification failed on " + p.toString(),
                    p.prob,
                    unsplitBinaryProbabilities.get((short) p.parent).get((short) p.leftChild).get((short) p.rightChild),
                    0.001f);
        }

        for (final Production p : unsplitPlg.unaryProductions) {
            assertEquals("Verification failed on " + p.toString(), p.prob, unaryProbabilities.get((short) p.parent)
                    .get((short) p.leftChild), 0.001f);
        }

        for (final Production p : unsplitPlg.lexicalProductions) {
            assertEquals("Verification failed on " + p.toString(), p.prob, lexicalProbabilities.get((short) p.parent)
                    .get(p.leftChild), 0.001f);
        }
    }

    @Override
    protected ProductionListGrammar clone() {
        final ProductionListGrammar clone = new ProductionListGrammar(baseGrammar, vocabulary, lexicon);
        clone.binaryProductions.addAll(binaryProductions);
        clone.unaryProductions.addAll(unaryProductions);
        clone.lexicalProductions.addAll(lexicalProductions);
        return clone;
    }

    @Override
    public String toString() {
        return toString(false, null, null, -1);
    }

    public String toString(final boolean fraction, final String language, final GrammarFormatType grammarFormatType,
            final int lexicalUnkThreshold) {
        final TreeSet<String> binaryRules = new TreeSet<String>();
        for (final Production p : binaryProductions) {
            if (fraction) {
                binaryRules.add(String.format("%s -> %s %s %s", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), JUnit.fraction(p.prob)));
            } else {
                binaryRules.add(String.format("%s -> %s %s %.4f", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), p.prob));
            }
        }

        final TreeSet<String> unaryRules = new TreeSet<String>();
        for (final Production p : unaryProductions) {
            if (fraction) {
                unaryRules.add(String.format("%s -> %s %s", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), JUnit.fraction(p.prob)));
            } else {
                unaryRules.add(String.format("%s -> %s %.4f", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), p.prob));
            }
        }

        final TreeSet<String> lexicalRules = new TreeSet<String>();
        for (final Production p : lexicalProductions) {
            if (fraction) {
                lexicalRules.add(String.format("%s -> %s %s", vocabulary.getSymbol(p.parent),
                        lexicon.getSymbol(p.leftChild), JUnit.fraction(p.prob)));
            } else {
                lexicalRules.add(String.format("%s -> %s %.4f", vocabulary.getSymbol(p.parent),
                        lexicon.getSymbol(p.leftChild), p.prob));
            }
        }

        // TODO Consolidate into base Grammar class
        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        final String dateNowStr = dateFormat.format(new Date());

        final StringBuilder sb = new StringBuilder(1024);
        sb.append("lang=" + (language != null ? language : "UNK"));
        sb.append(" format=" + grammarFormatType);
        sb.append(" unkThresh=" + lexicalUnkThreshold);
        sb.append(" start=" + startSymbol);
        sb.append(" hMarkov=UNK");
        sb.append(" vMarkov=UNK");
        sb.append(" date=" + dateNowStr);
        sb.append(" nBinary=" + binaryRules.size());
        sb.append(" nUnary=" + unaryRules.size());
        sb.append(" nLex=" + lexicalRules.size());
        sb.append("\n");

        for (final String rule : binaryRules) {
            sb.append(rule + '\n');
        }
        for (final String rule : unaryRules) {
            sb.append(rule + '\n');
        }

        sb.append(Grammar.LEXICON_DELIMITER + '\n');
        for (final String rule : lexicalRules) {
            sb.append(rule + '\n');
        }
        return sb.toString();
    }

    public static interface NoiseGenerator {

        /**
         * Returns an array of size <code>count</code> containing the natural log of 1 + generated 'noise' (generally
         * random or biased, depending on the implementation). Each adjacent pair in the returned array (0,1; 2,3; etc.)
         * differs by a fixed amount of noise.
         * 
         * @param count
         * @return An array of size <code>count</code> containing the natural log of generated noise
         */
        public float[] noise(int count);
    }

    public static class BiasedNoiseGenerator implements NoiseGenerator {

        private final float bias0;
        private final float bias1;

        /**
         * @param amount Amount of bias (0-1) to add to the rule probabilities in the new grammar (e.g., if
         *            <code>amount</code> is 0.01, the first rule in each pair will be preferred by 1%). With 0 noise,
         *            the probabilities of each rule will be split equally.
         */
        public BiasedNoiseGenerator(final float amount) {
            this.bias0 = (float) Math.log1p(amount);
            this.bias1 = (float) Math.log1p(-amount);
        }

        @Override
        public float[] noise(final int count) {
            final float[] noise = new float[count];
            for (int i = 0; i < count; i += 2) {
                noise[i] = bias0;
                noise[i + 1] = bias1;
            }
            return noise;
        }
    }

    public static class RandomNoiseGenerator implements NoiseGenerator {

        final Random random;
        private final float bias0;
        private final float bias1;

        /**
         * @param amount Amount of randomness (0-1) to add to the rule probabilities in the new grammar (e.g., if
         *            <code>amount</code> is 0.01, each pair differs by 1%). With 0 noise, the probabilities of each
         *            rule will be split equally. Some noise is generally required to break ties in the new grammar.
         * @param seed The random seed to initialize with
         */
        public RandomNoiseGenerator(final float amount, final long seed) {
            random = new Random(seed);
            this.bias0 = (float) Math.log1p(amount);
            this.bias1 = (float) Math.log1p(-amount);
        }

        /**
         * @param amount Amount of randomness (0-1) to add to the rule probabilities in the new grammar (e.g., if
         *            <code>amount</code> is 0.01, each pair differs by 1%). With 0 noise, the probabilities of each
         *            rule will be split equally. Some noise is generally required to break ties in the new grammar.
         */
        public RandomNoiseGenerator(final float amount) {
            this(amount, System.currentTimeMillis());
        }

        @Override
        public float[] noise(final int count) {
            final float[] noise = new float[count];

            for (int i = 0; i < count; i += 2) {
                if (random.nextBoolean()) {
                    noise[i] = bias0;
                    noise[i + 1] = bias1;
                } else {
                    noise[i] = bias1;
                    noise[i + 1] = bias0;
                }
            }
            return noise;
        }
    }
}
