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

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Pattern;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Language;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.tests.JUnit;

public class FractionalCountGrammar implements CountGrammar, Cloneable {

    private final static Pattern SUBSTATE_PATTERN = Pattern.compile("^.*_[0-9]+$");

    public final SplitVocabulary vocabulary;
    public final SymbolSet<String> lexicon;
    protected final String startSymbol;

    /** Parent -> Left child -> Right child -> count */
    private final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2DoubleOpenHashMap>> binaryRuleCounts = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2DoubleOpenHashMap>>();

    /** Parent -> child -> count */
    private final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> unaryRuleCounts = new Short2ObjectOpenHashMap<Short2DoubleOpenHashMap>();

    /** Parent -> child -> count */
    private final Short2ObjectOpenHashMap<Int2DoubleOpenHashMap> lexicalRuleCounts = new Short2ObjectOpenHashMap<Int2DoubleOpenHashMap>();

    /** Parent -> count */
    private final Short2DoubleOpenHashMap parentCounts = new Short2DoubleOpenHashMap();

    private final PackingFunction packingFunction;

    public FractionalCountGrammar(final SplitVocabulary vocabulary, final SymbolSet<String> lexicon,
            final PackingFunction packingFunction) {

        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
        this.startSymbol = vocabulary.getSymbol(0);

        this.packingFunction = packingFunction;
        this.parentCounts.defaultReturnValue(0);
    }

    public FractionalCountGrammar(final InsideOutsideCscSparseMatrixGrammar cscGrammar) {
        this((SplitVocabulary) cscGrammar.nonTermSet, cscGrammar.lexSet, cscGrammar.packingFunction);
    }

    protected void incrementBinaryCount(final short parent, final short leftChild, final short rightChild,
            final double increment) {

        Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);
        if (leftChildMap == null) {
            leftChildMap = new Short2ObjectOpenHashMap<Short2DoubleOpenHashMap>();
            binaryRuleCounts.put(parent, leftChildMap);
        }

        Short2DoubleOpenHashMap rightChildMap = leftChildMap.get(leftChild);
        if (rightChildMap == null) {
            rightChildMap = new Short2DoubleOpenHashMap();
            rightChildMap.defaultReturnValue(0);
            leftChildMap.put(leftChild, rightChildMap);
        }

        rightChildMap.put(rightChild, rightChildMap.get(rightChild) + increment);
        parentCounts.put(parent, parentCounts.get(parent) + increment);
    }

    public void incrementBinaryCount(final short parent, final int childPair, final double increment) {

        final short leftChild = (short) packingFunction.unpackLeftChild(childPair);
        final short rightChild = packingFunction.unpackRightChild(childPair);
        incrementBinaryCount(parent, leftChild, rightChild, increment);
    }

    public void incrementBinaryLogCount(final short parent, final int childPair, final float logIncrement) {
        incrementBinaryCount(parent, childPair, Math.exp(logIncrement));
    }

    /**
     * For unit tests
     */
    void incrementBinaryCount(final String parent, final String leftChild, final String rightChild,
            final float increment) {
        incrementBinaryCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(leftChild),
                (short) vocabulary.getIndex(rightChild), increment);
    }

    public void incrementUnaryCount(final short parent, final short child, final double increment) {

        Short2DoubleOpenHashMap childMap = unaryRuleCounts.get(parent);
        if (childMap == null) {
            childMap = new Short2DoubleOpenHashMap();
            childMap.defaultReturnValue(0);
            unaryRuleCounts.put(parent, childMap);
        }

        childMap.put(child, childMap.get(child) + increment);
        parentCounts.put(parent, parentCounts.get(parent) + increment);
    }

    public void incrementUnaryLogCount(final short parent, final short child, final float logIncrement) {
        incrementUnaryCount(parent, child, Math.exp(logIncrement));
    }

    /**
     * For unit tests
     */
    void incrementUnaryCount(final String parent, final String child, final double increment) {
        incrementUnaryCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(child), increment);
    }

    protected void incrementLexicalCount(final short parent, final int child, final double increment) {

        Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
        if (childMap == null) {
            childMap = new Int2DoubleOpenHashMap();
            childMap.defaultReturnValue(0);
            lexicalRuleCounts.put(parent, childMap);
        }

        childMap.put(child, childMap.get(child) + increment);
        parentCounts.put(parent, parentCounts.get(parent) + increment);
    }

    protected void incrementLexicalLogCount(final short parent, final int child, final float logIncrement) {
        incrementLexicalCount(parent, child, Math.exp(logIncrement));
    }

    /**
     * For unit tests
     */
    void incrementLexicalCount(final String parent, final String child, final double increment) {
        incrementLexicalCount((short) vocabulary.getIndex(parent), lexicon.getIndex(child), increment);
    }

    /**
     * @param minimumRuleLogProbability Minimum threshold rule probability. Rules with lower probability will be pruned.
     * @return {@link ProductionListGrammar} populated using the counts from this grammar
     */
    public ProductionListGrammar toProductionListGrammar(final float minimumRuleLogProbability) {

        // Construct a pruned (but not normalized) grammar from the accumulated fractional rule counts
        final ProductionListGrammar prunedGrammar = new ProductionListGrammar(vocabulary, lexicon,
                binaryProductions(minimumRuleLogProbability), unaryProductions(minimumRuleLogProbability),
                lexicalProductions(minimumRuleLogProbability));

        return prunedGrammar.normalizeProbabilities();
    }

    // /**
    // * Returns a {@link ProductionListGrammar} populated using the counts from this grammar and UNK-class lexical
    // rules.
    // *
    // * @param minimumRuleLogProbability Minimum threshold rule probability. Rules with lower probability will be
    // pruned.
    // * @param unkClassMap Maps word entries from the lexicon to their UNK-class entries.
    // * @param openClassPreterminalThreshold Minimum number of terminal children a preterminal must have to be
    // considered
    // * open-class. UNK-class rules will be created for open-class preterminals.
    // * @param corpusWordCounts Word-counts from the training corpus
    // * @param rareWordThreshold Rules for rare words (those occurring less often than this threshold) will be smoothed
    // * with their UNK-class probabilities.
    // * @return {@link ProductionListGrammar} populated using the counts from this grammar and UNK-class lexical rules
    // */
    // public ProductionListGrammar toProductionListGrammar(final float minimumRuleLogProbability,
    // final Int2IntOpenHashMap unkClassMap, final int openClassPreterminalThreshold,
    // final Int2IntOpenHashMap corpusWordCounts, final int rareWordThreshold) {
    //
    // // Construct a pruned (but not normalized) grammar from the accumulated fractional rule counts
    // final ProductionListGrammar prunedGrammar = new ProductionListGrammar(vocabulary, lexicon,
    // binaryProductions(minimumRuleLogProbability), unaryProductions(minimumRuleLogProbability),
    // lexicalProductions(minimumRuleLogProbability, unkClassMap, openClassPreterminalThreshold,
    // corpusWordCounts, rareWordThreshold));
    //
    // return prunedGrammar.normalizeProbabilities();
    // }

    public ArrayList<Production> binaryProductions(final float minimumRuleLogProbability) {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {

            if (!binaryRuleCounts.containsKey(parent)) {
                continue;
            }

            final double parentCount = parentCounts.get(parent);
            final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);

            for (short leftChild = 0; leftChild < vocabulary.size(); leftChild++) {
                if (!leftChildMap.containsKey(leftChild)) {
                    continue;
                }

                final Short2DoubleOpenHashMap rightChildMap = leftChildMap.get(leftChild);

                for (short rightChild = 0; rightChild < vocabulary.size(); rightChild++) {
                    if (!rightChildMap.containsKey(rightChild)) {
                        continue;
                    }

                    // Observations of this rule / Observations of all split rules with the parent
                    final double observations = rightChildMap.get(rightChild);
                    if (observations != 0) {
                        final float logProbability = (float) Math.log(observations / parentCount);
                        if (logProbability > minimumRuleLogProbability) {
                            prods.add(new Production(parent, leftChild, rightChild, logProbability, vocabulary, lexicon));
                        }
                    }
                }
            }
        }

        return prods;
    }

    public ArrayList<Production> unaryProductions(final float minimumRuleLogProbability) {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!unaryRuleCounts.containsKey(parent)) {
                continue;
            }

            final double parentCount = parentCounts.get(parent);
            final Short2DoubleOpenHashMap childMap = unaryRuleCounts.get(parent);

            for (short child = 0; child < vocabulary.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                // Observations of this rule / Observations of all rules with the parent
                final double count = childMap.get(child);
                // TODO Remove these redundant checks
                if (count != 0) {
                    final float logProbability = (float) Math.log(count / parentCount);
                    if (logProbability > minimumRuleLogProbability) {
                        prods.add(new Production(parent, child, logProbability, false, vocabulary, lexicon));
                    }
                }
            }
        }

        return prods;
    }

    public ArrayList<Production> lexicalProductions(final float minimumRuleLogProbability) {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!lexicalRuleCounts.containsKey(parent)) {
                continue;
            }
            final double parentCount = parentCounts.get(parent);

            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);

            for (int child = 0; child < lexicon.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                // Observations of this rule / Observations of all rules with the parent
                final double count = childMap.get(child);
                if (count != 0) {
                    final float logProbability = (float) Math.log(count / parentCount);
                    if (logProbability > minimumRuleLogProbability) {
                        prods.add(new Production(parent, child, logProbability, true, vocabulary, lexicon));
                    }
                }
            }
        }

        return prods;
    }

    /**
     * Returns a copy of this grammar including UNK-class pseudo-counts based on observed counts of rare words. Counts
     * for rare words are smoothed using their UNK-class counts.
     * 
     * @param unkClassMap Maps word entries from the lexicon to their UNK-class entries.
     * @param openClassPreterminalThreshold Minimum number of terminal children a preterminal must have to be considered
     *            open-class. UNK-class rules will be created for open-class preterminals.
     * @param corpusWordCounts Word-counts from the training corpus
     * @param rareWordThreshold Rules for rare words (those occurring less often than this threshold) will be smoothed
     *            with their UNK-class probabilities.
     * @return A copy of this grammar including UNK-class pseudo-counts based on observed counts of rare words.
     */
    public FractionalCountGrammar addUnkCounts(final Int2IntOpenHashMap unkClassMap,
            final int openClassPreterminalThreshold, final Int2IntOpenHashMap corpusWordCounts,
            final int rareWordThreshold) {

        final FractionalCountGrammar grammarWithUnks = clone();

        for (short parent = 0; parent < vocabulary.size(); parent++) {

            // Skip parents without any lexical children and closed-class preterminals. An open-class preterminal is one
            // with more than 'openClassPreterminalThreshold' observed children.
            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
            if (childMap == null || childMap.size() < openClassPreterminalThreshold) {
                continue;
            }

            final double tagCount = parentCounts.get(parent);

            for (int word = 0; word < lexicon.size(); word++) {

                if (corpusWordCounts.get(word) < rareWordThreshold) {

                    // Note rare-word-threshold is abbreviated 'r' in the following formulae
                    // c(w) / r
                    final double lambda = ((double) corpusWordCounts.get(word)) / rareWordThreshold;

                    // Add UNK pseudo-counts
                    // TODO Should we weight this somehow? Probably not needed, but might help
                    grammarWithUnks.incrementLexicalCount(parent, unkClassMap.get(word), childMap.get(word));

                    // And rare-word pseudo-counts
                    //
                    // Total probability is (lambda * c(w|t) / c(t)) + (1- lambda) * (c(UNK-class|t) / c(t))
                    // We're adding the second term here
                    final double unkClassCount = corpusWordCounts.get(unkClassMap.get(word));
                    grammarWithUnks.incrementLexicalCount(parent, word, (1 - lambda) * unkClassCount / tagCount);
                    // } else {
                    // grammarWithUnks.incrementLexicalCount(parent, word, childMap.get(word));
                }
            }
        }

        return grammarWithUnks;
    }

    // /**
    // *
    // * @param unkClassMap
    // * @param openClassPreterminalThreshold
    // * @param corpusWordCounts
    // * @param rareWordThreshold
    // * @return
    // */
    // private Short2ObjectOpenHashMap<Int2DoubleOpenHashMap> unkClassCounts(final Int2IntOpenHashMap unkClassMap,
    // final int openClassPreterminalThreshold, final Int2IntOpenHashMap corpusWordCounts,
    // final int rareWordThreshold) {
    // final Short2ObjectOpenHashMap<Int2DoubleOpenHashMap> unkClassCounts = new
    // Short2ObjectOpenHashMap<Int2DoubleOpenHashMap>();
    //
    // for (short parent = 0; parent < vocabulary.size(); parent++) {
    // if (!lexicalRuleCounts.containsKey(parent)) {
    // continue;
    // }
    //
    // final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
    //
    // if (childMap.size() < openClassPreterminalThreshold) {
    // continue;
    // }
    //
    // final Int2DoubleOpenHashMap unkChildMap = new Int2DoubleOpenHashMap();
    // unkChildMap.defaultReturnValue(0);
    // unkClassCounts.put(parent, unkChildMap);
    //
    // for (int child = 0; child < lexicon.size(); child++) {
    //
    // final double wordGivenTag = childMap.get(child);
    // if (wordGivenTag > 0 && corpusWordCounts.get(child) < rareWordThreshold) {
    // final int unkClass = unkClassMap.get(child);
    // unkChildMap.put(unkClass, unkChildMap.get(unkClass) + wordGivenTag);
    // }
    // }
    // }
    // return unkClassCounts;
    // }

    /**
     * Returns the number of observations of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return the number of observations of a binary rule.
     */
    public final double binaryRuleObservations(final String parent, final String leftChild, final String rightChild) {

        final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> leftChildMap = binaryRuleCounts.get((short) vocabulary
                .getIndex(parent));
        if (leftChildMap == null) {
            return 0f;
        }

        final Short2DoubleOpenHashMap rightChildMap = leftChildMap.get((short) vocabulary.getIndex(leftChild));
        if (rightChildMap == null) {
            return 0f;
        }

        return rightChildMap.get((short) vocabulary.getIndex(rightChild));
    }

    /**
     * Returns the (log) probability of a binary rule
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return the (log) probability of a binary rule
     */
    public final float binaryLogProbability(final String parent, final String leftChild, final String rightChild) {
        return (float) Math.log(binaryRuleObservations(parent, leftChild, rightChild)
                / parentCounts.get((short) vocabulary.getIndex(parent)));
    }

    /**
     * Returns the number of observations of a unary rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a unary rule.
     */
    public final double unaryRuleObservations(final String parent, final String child) {

        final Short2DoubleOpenHashMap childMap = unaryRuleCounts.get((short) vocabulary.getIndex(parent));
        if (childMap == null) {
            return 0f;
        }

        return childMap.get((short) vocabulary.getIndex(child));
    }

    /**
     * Returns the (log) probability of a unary rule
     * 
     * @param parent
     * @param child
     * @return the (log) probability of a unary rule
     */
    public final float unaryLogProbability(final String parent, final String child) {
        return (float) Math.log(unaryRuleObservations(parent, child)
                / parentCounts.get((short) vocabulary.getIndex(parent)));
    }

    /**
     * Returns the number of observations of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a lexical rule.
     */
    public final double lexicalRuleObservations(final String parent, final String child) {
        final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get((short) vocabulary.getIndex(parent));
        if (childMap == null) {
            return 0f;
        }

        return childMap.get(lexicon.getIndex(child));
    }

    /**
     * Returns the (log) probability of a lexical rule
     * 
     * @param parent
     * @param child
     * @return the (log) probability of a lexical rule
     */
    public final float lexicalLogProbability(final String parent, final String child) {
        return (float) Math.log(lexicalRuleObservations(parent, child)
                / parentCounts.get((short) vocabulary.getIndex(parent)));
    }

    /**
     * Splits each non-terminal in the grammar into 2 sub-states, constructing a new grammar, vocabulary, and lexicon.
     * 
     * @return Newly-constructed grammar
     */
    public FractionalCountGrammar split() {
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

        final FractionalCountGrammar splitGrammar = new FractionalCountGrammar(splitVocabulary, lexicon, null);

        //
        // Iterate through each rule, creating split rules in the new grammar
        //

        // Split each binary production into 8
        for (final short parent : binaryRuleCounts.keySet()) {
            final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);
            final short splitParent0 = (short) (parent << 1);
            final short splitParent1 = (short) (splitParent0 + 1);
            assert splitParent1 < splitVocabulary.size();

            for (final short leftChild : leftChildMap.keySet()) {
                final Short2DoubleOpenHashMap rightChildMap = leftChildMap.get(leftChild);
                final short splitLeftChild0 = (short) (leftChild << 1);
                final short splitLeftChild1 = (short) (splitLeftChild0 + 1);
                assert splitLeftChild1 < splitVocabulary.size();

                for (final short rightChild : rightChildMap.keySet()) {
                    final short splitRightChild0 = (short) (rightChild << 1);
                    final short splitRightChild1 = (short) (splitRightChild0 + 1);
                    assert splitRightChild1 < splitVocabulary.size();

                    final double count = rightChildMap.get(rightChild) / 4;

                    splitGrammar.incrementBinaryCount(splitParent0, splitLeftChild0, splitRightChild0, count);
                    splitGrammar.incrementBinaryCount(splitParent0, splitLeftChild0, splitRightChild1, count);
                    splitGrammar.incrementBinaryCount(splitParent0, splitLeftChild1, splitRightChild0, count);
                    splitGrammar.incrementBinaryCount(splitParent0, splitLeftChild1, splitRightChild1, count);
                    splitGrammar.incrementBinaryCount(splitParent1, splitLeftChild0, splitRightChild0, count);
                    splitGrammar.incrementBinaryCount(splitParent1, splitLeftChild0, splitRightChild1, count);
                    splitGrammar.incrementBinaryCount(splitParent1, splitLeftChild1, splitRightChild0, count);
                    splitGrammar.incrementBinaryCount(splitParent1, splitLeftChild1, splitRightChild1, count);
                }
            }
        }

        // Split each unary production into fourths
        for (final short parent : unaryRuleCounts.keySet()) {
            final Short2DoubleOpenHashMap childMap = unaryRuleCounts.get(parent);
            final short splitParent0 = (short) (parent << 1);
            final short splitParent1 = (short) (splitParent0 + 1);
            assert splitParent1 < splitVocabulary.size();

            // Special-case for the start symbol. Since we do not split it, we only split unaries of which it is the
            // parent in two
            if (parent == 0) {
                for (final short child : childMap.keySet()) {
                    final short splitChild0 = (short) (child << 1);
                    final short splitChild1 = (short) (splitChild0 + 1);
                    assert splitChild1 < splitVocabulary.size();

                    final double count = childMap.get(child);

                    splitGrammar.incrementUnaryCount(parent, splitChild0, count);
                    splitGrammar.incrementUnaryCount(parent, splitChild1, count);
                }

            } else {
                for (final short child : childMap.keySet()) {
                    final short splitChild0 = (short) (child << 1);
                    final short splitChild1 = (short) (splitChild0 + 1);
                    assert splitChild1 < splitVocabulary.size();

                    final double count = childMap.get(child) / 2;

                    splitGrammar.incrementUnaryCount(splitParent0, splitChild0, count);
                    splitGrammar.incrementUnaryCount(splitParent0, splitChild1, count);
                    splitGrammar.incrementUnaryCount(splitParent1, splitChild0, count);
                    splitGrammar.incrementUnaryCount(splitParent1, splitChild1, count);
                }
            }
        }

        // Split lexical productions in half. Each split production has the same probability as the original production
        // TODO Is this correct? Or should we add noise here as well?
        for (final short parent : lexicalRuleCounts.keySet()) {
            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
            final short splitParent0 = (short) (parent << 1);
            final short splitParent1 = (short) (splitParent0 + 1);
            assert splitParent1 < splitVocabulary.size();

            for (final int child : childMap.keySet()) {

                final double count = childMap.get(child);

                splitGrammar.incrementLexicalCount(splitParent0, child, count);
                splitGrammar.incrementLexicalCount(splitParent0, child, count);
                splitGrammar.incrementLexicalCount(splitParent1, child, count);
                splitGrammar.incrementLexicalCount(splitParent1, child, count);
            }
        }

        return splitGrammar;
    }

    /**
     * Adds random pseudo-count noise to each rule. The number of pseudo-counts is proportional to the observed parent
     * count.
     */
    public void randomize(final Random random, final float amount) {

        // Short-circuit if we won't add any pseudo-counts
        if (amount == 0) {
            return;
        }

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> binaryLeftChildMap = binaryRuleCounts.get(parent);
            final Short2DoubleOpenHashMap unaryChildMap = unaryRuleCounts.get(parent);
            final Int2DoubleOpenHashMap lexicalChildMap = lexicalRuleCounts.get(parent);

            final double parentCount = parentCounts.get(parent);

            // Binary rules
            if (binaryLeftChildMap != null) {
                for (final short leftChild : binaryLeftChildMap.keySet()) {
                    final Short2DoubleOpenHashMap binaryRightChildMap = binaryLeftChildMap.get(leftChild);

                    for (final short rightChild : binaryRightChildMap.keySet()) {
                        incrementBinaryCount(parent, leftChild, rightChild, parentCount * 2 * random.nextDouble()
                                * amount);
                    }
                }
            }

            // Unary rules
            if (unaryChildMap != null) {
                for (final short child : unaryChildMap.keySet()) {
                    incrementUnaryCount(parent, child, parentCount * 2 * random.nextDouble() * amount);
                }
            }

            // Lexical rules
            if (lexicalChildMap != null) {
                for (final int child : lexicalChildMap.keySet()) {
                    incrementLexicalCount(parent, child, parentCount * 2 * random.nextDouble() * amount);
                }
            }
        }
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
     * Re-merges splits specified by non-terminal indices, producing a new {@link ProductionListGrammar} with its own
     * vocabulary and lexicon.
     * 
     * @param indices Non-terminal indices to merge. Each index is assumed to be the <i>second</i> of a split pair.
     *            i.e., if A and B were split, merging A into B is equivalent to merging B into A. The merge operation
     *            assumes that the indices will be of each non-terminal B.
     * @return Merged grammar
     */
    public FractionalCountGrammar merge(final short[] indices) {

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

        //
        // Create a new grammar, populated with all binary, unary, and lexical rule counts from this grammar
        //
        final FractionalCountGrammar mergedGrammar = new FractionalCountGrammar(mergedVocabulary, lexicon, null);

        for (final short parent : binaryRuleCounts.keySet()) {
            final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);
            for (final short leftChild : leftChildMap.keySet()) {
                final Short2DoubleOpenHashMap rightChildMap = leftChildMap.get(leftChild);
                for (final short rightChild : rightChildMap.keySet()) {
                    mergedGrammar.incrementBinaryCount(parentToMergedIndexMap.get(parent),
                            parentToMergedIndexMap.get(leftChild), parentToMergedIndexMap.get(rightChild),
                            rightChildMap.get(rightChild));
                }
            }
        }

        for (final short parent : unaryRuleCounts.keySet()) {
            final Short2DoubleOpenHashMap childMap = unaryRuleCounts.get(parent);
            for (final short child : childMap.keySet()) {
                mergedGrammar.incrementUnaryCount(parentToMergedIndexMap.get(parent),
                        parentToMergedIndexMap.get(child), childMap.get(child));
            }
        }

        for (final short parent : lexicalRuleCounts.keySet()) {
            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
            for (final int child : childMap.keySet()) {
                mergedGrammar.incrementLexicalCount(parentToMergedIndexMap.get(parent), child, childMap.get(child));
            }
        }

        return mergedGrammar;
    }

    /**
     * Performs a 'partial' merge, merging counts but <em>not</em> non-terminals. This creates a 'merged' grammar with
     * which we can still perform constrained parses using the current {@link ConstrainingChart}, allowing post-merge
     * EM. To complete the partial merge, call {@link #merge(short[])}.
     * 
     * @param indices Non-terminal indices to merge. Each index is assumed to be the <i>second</i> of a split pair.
     *            i.e., if A and B were split, merging A into B is equivalent to merging B into A. The merge operation
     *            assumes that the indices will be of each non-terminal B.
     * @return Merged grammar
     */
    public FractionalCountGrammar mergeCounts(final short[] indices) {

        // Post-merge indices, indexed by pre-merge values
        final short[] mergedIndices = new short[vocabulary.size()];
        for (short i = 0, j = 0; i < mergedIndices.length; i++) {
            if (j < indices.length && indices[j] == i) {
                mergedIndices[i] = (short) (i - 1);
            } else {
                mergedIndices[i] = i;
            }
        }

        //
        // Create a new grammar, populated with all binary, unary, and lexical rule counts from this grammar
        //
        final FractionalCountGrammar mergedGrammar = new FractionalCountGrammar(vocabulary, lexicon, null);

        for (final short parent : binaryRuleCounts.keySet()) {
            final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);
            for (final short leftChild : leftChildMap.keySet()) {
                final Short2DoubleOpenHashMap rightChildMap = leftChildMap.get(leftChild);
                for (final short rightChild : rightChildMap.keySet()) {
                    mergedGrammar.incrementBinaryCount(mergedIndices[parent], mergedIndices[leftChild],
                            mergedIndices[rightChild], rightChildMap.get(rightChild));
                }
            }
        }

        for (final short parent : unaryRuleCounts.keySet()) {
            final Short2DoubleOpenHashMap childMap = unaryRuleCounts.get(parent);
            for (final short child : childMap.keySet()) {
                mergedGrammar.incrementUnaryCount(mergedIndices[parent], mergedIndices[child], childMap.get(child));
            }
        }

        for (final short parent : lexicalRuleCounts.keySet()) {
            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
            for (final int child : childMap.keySet()) {
                mergedGrammar.incrementLexicalCount(mergedIndices[parent], child, childMap.get(child));
            }
        }

        return mergedGrammar;
    }

    /**
     * Computes relative counts of each split pair. E.g., if NP_0 has been split into NP_0 and NP_1, and NP_0 occurs 12
     * times and NP_1 8, the array entries for NP_0 and NP_1 will contain (log) 12/20 and 8/20 respectively.
     * 
     * @return relative counts of each split pair.
     */
    public float[] logSplitFraction() {
        final float[] logSplitCounts = new float[vocabulary.size()];
        for (short i = 0; i < logSplitCounts.length; i += 2) {
            final double split0Count = parentCounts.get(i);
            final double split1Count = parentCounts.get((short) (i + 1));
            final double totalCount = split0Count + split1Count;
            logSplitCounts[i] = (float) Math.log(split0Count / totalCount);
            logSplitCounts[i + 1] = (float) Math.log(split1Count / totalCount);
        }
        return logSplitCounts;
    }

    // /**
    // * Add UNK rules to the lexicon, stealing from other lexical probabilities to allot some probability mass for UNK
    // * rules.
    // *
    // * Note that even rare words (those that occur <= lexicalUnkThreshold times) will still be included in their
    // * lexicalized form.
    // *
    // * @param countGrammar {@link FractionalCountGrammar} with posterior counts from training corpus. Note :
    // fractional
    // * counts will be added for UNK rules, modifying this grammar.
    // */
    // public void addUnkProbabilities(final Collection<NaryTree<String>> goldTrees, final int lexicalUnkThreshold) {
    //
    // //
    // // Count words - overall and separately when they occur as the first word in a sentence
    // //
    // final Object2IntOpenHashMap<String> lexicalEntryCounts = new Object2IntOpenHashMap<String>();
    // lexicalEntryCounts.defaultReturnValue(0);
    //
    // for (final NaryTree<String> tree : goldTrees) {
    //
    // for (final NaryTree<String> leaf : tree.leafTraversal()) {
    // final String word = leaf.label();
    // lexicalEntryCounts.put(word, lexicalEntryCounts.getInt(word) + 1);
    // }
    // }
    //
    // // Create a map from lexical entries to parents and probabilities (word -> parent -> probability)
    // final Int2ObjectOpenHashMap<Short2DoubleOpenHashMap> lexicalParentProbabilities = new
    // Int2ObjectOpenHashMap<Short2DoubleOpenHashMap>();
    // for (final short parent : lexicalRuleCounts.keySet()) {
    // final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
    // for (final int child : childMap.keySet()) {
    // Short2DoubleOpenHashMap parentMap = lexicalParentProbabilities.get(child);
    // if (parentMap == null) {
    // parentMap = new Short2DoubleOpenHashMap();
    // parentMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
    // lexicalParentProbabilities.put(child, parentMap);
    // }
    // parentMap.put(parent, childMap.get(child) / parentCounts.get(parent));
    // }
    // }
    //
    // //
    // // Iterate through the training corpus again, adding UNK counts for rare words
    // //
    // for (final NaryTree<String> tree : goldTrees) {
    //
    // // Handle first word separately
    // final Tree<String> leftmostLeaf = tree.leftmostLeaf();
    // final String firstWord = leftmostLeaf.label();
    // if (lexicalEntryCounts.getInt(firstWord) <= lexicalUnkThreshold) {
    // final int initialUnkIndex = lexicon.addSymbol(Tokenizer.berkeleyGetSignature(firstWord, true, lexicon));
    // final Short2DoubleOpenHashMap parentMap = lexicalParentProbabilities.get(lexicon.getIndex(firstWord));
    // for (final short parent : parentMap.keySet()) {
    // incrementLexicalCount(parent, initialUnkIndex, parentMap.get(parent));
    // }
    // }
    //
    // for (final NaryTree<String> leaf : tree.leafTraversal()) {
    //
    // final String word = leaf.label();
    // final int count = lexicalEntryCounts.getInt(word);
    //
    // if (count <= lexicalUnkThreshold) {
    // final int unkIndex = lexicon.addSymbol(Tokenizer.berkeleyGetSignature(word, false, lexicon));
    // final Short2DoubleOpenHashMap parentMap = lexicalParentProbabilities.get(lexicon.getIndex(word));
    // for (final short parent : parentMap.keySet()) {
    // incrementLexicalCount(parent, unkIndex, parentMap.get(parent));
    // }
    // }
    // }
    // }
    // }

    /**
     * Merges the grammar back into a Markov-0 (unsplit) grammar, and ensures that the merged probabilities are the same
     * as the supplied unsplit grammar (usually the original Markov-0 grammar induced from the training corpus). Used
     * for unit testing.
     * 
     * @param unsplitPlg
     */
    void verifyVsUnsplitGrammar(final ProductionListGrammar unsplitPlg) {

        final FractionalCountGrammar unsplitGrammar = new FractionalCountGrammar(unsplitPlg.vocabulary,
                unsplitPlg.lexicon, null);

        for (final short parent : binaryRuleCounts.keySet()) {
            final short unsplitParent = vocabulary.getBaseIndex(parent);
            final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);

            for (final short leftChild : leftChildMap.keySet()) {
                final short unsplitLeftChild = vocabulary.getBaseIndex(leftChild);
                final Short2DoubleOpenHashMap rightChildMap = leftChildMap.get(leftChild);

                for (final short rightChild : rightChildMap.keySet()) {
                    final short unsplitRightChild = vocabulary.getBaseIndex(rightChild);
                    unsplitGrammar.incrementBinaryCount(unsplitParent, unsplitLeftChild, unsplitRightChild,
                            rightChildMap.get(rightChild));
                }
            }
        }

        for (final short parent : unaryRuleCounts.keySet()) {
            final short unsplitParent = vocabulary.getBaseIndex(parent);
            final Short2DoubleOpenHashMap childMap = unaryRuleCounts.get(parent);

            for (final short child : childMap.keySet()) {
                final short unsplitChild = vocabulary.getBaseIndex(child);
                unsplitGrammar.incrementUnaryCount(unsplitParent, unsplitChild, childMap.get(child));
            }
        }

        for (final short parent : lexicalRuleCounts.keySet()) {
            final short unsplitParent = vocabulary.getBaseIndex(parent);
            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);

            for (final int child : childMap.keySet()) {
                unsplitGrammar.incrementLexicalCount(unsplitParent, child, childMap.get(child));
            }
        }

        final ProductionListGrammar newUnsplitPlg = unsplitGrammar.toProductionListGrammar(Float.NEGATIVE_INFINITY);
        newUnsplitPlg.verifyVsExpectedGrammar(unsplitPlg);
    }

    public final int totalRules() {
        return binaryRules() + unaryRules() + lexicalRules();
    }

    public final int binaryRules() {
        int count = 0;
        for (final short parent : binaryRuleCounts.keySet()) {
            final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);

            for (final short leftChild : leftChildMap.keySet()) {
                final Short2DoubleOpenHashMap rightChildMap = leftChildMap.get(leftChild);

                count += rightChildMap.size();
            }
        }
        return count;
    }

    public final int unaryRules() {
        int count = 0;
        for (final short parent : unaryRuleCounts.keySet()) {
            count += unaryRuleCounts.get(parent).size();
        }
        return count;
    }

    public final int lexicalRules() {
        int count = 0;
        for (final short parent : lexicalRuleCounts.keySet()) {
            count += lexicalRuleCounts.get(parent).size();
        }
        return count;
    }

    public final double observations(final String parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FractionalCountGrammar clone() {
        return clone(Float.NEGATIVE_INFINITY);
    }

    public FractionalCountGrammar clone(final float minimumRuleLogProbability) {
        final FractionalCountGrammar clone = new FractionalCountGrammar(vocabulary, lexicon, packingFunction);

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            final double threshold = minimumRuleLogProbability > Float.NEGATIVE_INFINITY ? parentCounts.get(parent)
                    * Math.exp(minimumRuleLogProbability) : 0;

            final Short2ObjectOpenHashMap<Short2DoubleOpenHashMap> binaryLeftChildMap = binaryRuleCounts.get(parent);
            final Short2DoubleOpenHashMap unaryChildMap = unaryRuleCounts.get(parent);
            final Int2DoubleOpenHashMap lexicalChildMap = lexicalRuleCounts.get(parent);

            // Binary rules
            if (binaryLeftChildMap != null) {
                for (final short leftChild : binaryLeftChildMap.keySet()) {
                    final Short2DoubleOpenHashMap binaryRightChildMap = binaryLeftChildMap.get(leftChild);

                    for (final short rightChild : binaryRightChildMap.keySet()) {
                        final double count = binaryRightChildMap.get(rightChild);
                        if (count > threshold) {
                            clone.incrementBinaryCount(parent, leftChild, rightChild, count);
                        }
                    }
                }
            }

            // Unary rules
            if (unaryChildMap != null) {
                for (final short child : unaryChildMap.keySet()) {
                    final double count = unaryChildMap.get(child);
                    if (count > threshold) {
                        clone.incrementUnaryCount(parent, child, count);
                    }
                }
            }

            // Lexical rules
            if (lexicalChildMap != null) {
                for (final int child : lexicalChildMap.keySet()) {
                    final double count = lexicalChildMap.get(child);
                    if (count > threshold) {
                        clone.incrementLexicalCount(parent, child, count);
                    }
                }
            }
        }
        return clone;
    }

    // @Override
    // public String toString() {
    // return toString(true);
    // }
    //
    // public String toString(final boolean fraction) {
    // final TreeSet<String> binaryRules = new TreeSet<String>();
    // for (final Production p : binaryProductions(Float.NEGATIVE_INFINITY)) {
    // if (fraction) {
    // binaryRules.add(String.format("%s -> %s %s %s", vocabulary.getSymbol(p.parent),
    // vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), JUnit.fraction(p.prob)));
    // } else {
    // binaryRules.add(String.format("%s -> %s %s %.4f", vocabulary.getSymbol(p.parent),
    // vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), p.prob));
    // }
    // }
    //
    // final TreeSet<String> unaryRules = new TreeSet<String>();
    // for (final Production p : unaryProductions(Float.NEGATIVE_INFINITY)) {
    // if (fraction) {
    // unaryRules.add(String.format("%s -> %s %s", vocabulary.getSymbol(p.parent),
    // vocabulary.getSymbol(p.leftChild), JUnit.fraction(p.prob)));
    // } else {
    // unaryRules.add(String.format("%s -> %s %.4f", vocabulary.getSymbol(p.parent),
    // vocabulary.getSymbol(p.leftChild), p.prob));
    // }
    // }
    //
    // final TreeSet<String> lexicalRules = new TreeSet<String>();
    // for (final Production p : lexicalProductions(Float.NEGATIVE_INFINITY)) {
    // if (fraction) {
    // lexicalRules.add(String.format("%s -> %s %s", vocabulary.getSymbol(p.parent),
    // lexicon.getSymbol(p.leftChild), JUnit.fraction(p.prob)));
    // } else {
    // lexicalRules.add(String.format("%s -> %s %.4f", vocabulary.getSymbol(p.parent),
    // lexicon.getSymbol(p.leftChild), p.prob));
    // }
    // }
    //
    // final StringBuilder sb = new StringBuilder(1024);
    // for (final String rule : binaryRules) {
    // sb.append(rule);
    // sb.append('\n');
    // }
    // for (final String rule : unaryRules) {
    // sb.append(rule);
    // sb.append('\n');
    // }
    //
    // sb.append(Grammar.LEXICON_DELIMITER);
    // sb.append('\n');
    //
    // for (final String rule : lexicalRules) {
    // sb.append(rule);
    // sb.append('\n');
    // }
    // return sb.toString();
    // }

    @Override
    public String toString() {
        return toString(false, null, null, -1);
    }

    public String toString(final boolean fraction, final Language language, final GrammarFormatType grammarFormatType,
            final int lexicalUnkThreshold) {
        final TreeSet<String> binaryRules = new TreeSet<String>();
        for (final Production p : binaryProductions(Float.NEGATIVE_INFINITY)) {
            if (fraction) {
                binaryRules.add(String.format("%s -> %s %s %s", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), JUnit.fraction(p.prob)));
            } else {
                binaryRules.add(String.format("%s -> %s %s %.4f", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), p.prob));
            }
        }

        final TreeSet<String> unaryRules = new TreeSet<String>();
        for (final Production p : unaryProductions(Float.NEGATIVE_INFINITY)) {
            if (fraction) {
                unaryRules.add(String.format("%s -> %s %s", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), JUnit.fraction(p.prob)));
            } else {
                unaryRules.add(String.format("%s -> %s %.4f", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), p.prob));
            }
        }

        final TreeSet<String> lexicalRules = new TreeSet<String>();
        for (final Production p : lexicalProductions(Float.NEGATIVE_INFINITY)) {
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
        sb.append(" vocabSize=" + vocabulary.size());
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
}
