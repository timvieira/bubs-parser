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
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.Tree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.tests.JUnit;

public class FractionalCountGrammar implements CountGrammar {

    protected final SplitVocabulary vocabulary;
    protected final SymbolSet<String> lexicon;
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

    /**
     * Returns a {@link ProductionListGrammar} populated using the counts from this grammar and UNK-class lexical rules.
     * 
     * @param minimumRuleLogProbability Minimum threshold rule probability. Rules with lower probability will be pruned.
     * @param unkClassMap Maps word entries from the lexicon to their UNK-class entries.
     * @param openClassPreterminalThreshold Minimum number of terminal children a preterminal must have to be considered
     *            open-class. UNK-class rules will be created for open-class preterminals.
     * @param corpusWordCounts Word-counts from the training corpus
     * @param rareWordThreshold Rules for rare words (those occurring less often than this threshold) will be smoothed
     *            with their UNK-class probabilities.
     * @return {@link ProductionListGrammar} populated using the counts from this grammar and UNK-class lexical rules
     */
    public ProductionListGrammar toProductionListGrammar(final float minimumRuleLogProbability,
            final Int2IntOpenHashMap unkClassMap, final int openClassPreterminalThreshold,
            final Int2IntOpenHashMap corpusWordCounts, final int rareWordThreshold) {

        // Construct a pruned (but not normalized) grammar from the accumulated fractional rule counts
        final ProductionListGrammar prunedGrammar = new ProductionListGrammar(vocabulary, lexicon,
                binaryProductions(minimumRuleLogProbability), unaryProductions(minimumRuleLogProbability),
                lexicalProductions(minimumRuleLogProbability, unkClassMap, openClassPreterminalThreshold,
                        corpusWordCounts, rareWordThreshold));

        return prunedGrammar.normalizeProbabilities();
    }

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
     * Returns lexical and UNK-class rule productions
     * 
     * @param minimumRuleLogProbability Minimum threshold rule probability. Rules with lower probability will be omitted
     *            from the returned ruleset.
     * @param unkClassMap Maps word entries from the lexicon to their UNK-class entries.
     * @param openClassPreterminalThreshold Minimum number of terminal children a preterminal must have to be considered
     *            open-class. UNK-class rules will be created for open-class preterminals.
     * @param corpusWordCounts Word-counts from the training corpus
     * @param rareWordThreshold Rules for rare words (those occurring less often than this threshold) will be smoothed
     *            with their UNK-class probabilities.
     * @return Lexical rules including UNK-class rules
     */
    public ArrayList<Production> lexicalProductions(final float minimumRuleLogProbability,
            final Int2IntOpenHashMap unkClassMap, final int openClassPreterminalThreshold,
            final Int2IntOpenHashMap corpusWordCounts, final int rareWordThreshold) {

        final ArrayList<Production> prods = new ArrayList<Production>();
        final Short2ObjectOpenHashMap<Int2DoubleOpenHashMap> unkClassCounts = unkClassCounts(unkClassMap,
                openClassPreterminalThreshold, corpusWordCounts, rareWordThreshold);

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!lexicalRuleCounts.containsKey(parent)) {
                continue;
            }
            final double tagCount = parentCounts.get(parent);

            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
            // An open-class preterminal is one with more than 'openClassPreterminalThreshold' observed children
            final boolean openClassPreterminal = (childMap.size() >= openClassPreterminalThreshold);

            for (int child = 0; child < lexicon.size(); child++) {

                if (!childMap.containsKey(child)) {
                    continue;
                }
                final double wordGivenTag = childMap.get(child);

                if (wordGivenTag < rareWordThreshold && openClassPreterminal) {

                    // c(w) / r
                    final double lambda = ((double) corpusWordCounts.get(child)) / rareWordThreshold;

                    //
                    // (c(w) / r * c(w|t) / c(t)) + (1- c(w) / r) * (c(UNK-class|t) / c(t))
                    //
                    final double unkClassCount = corpusWordCounts.get(unkClassMap.get(child));
                    final float logProbability = (float) Math.log(lambda * (wordGivenTag / tagCount) + (1 - lambda)
                            * (unkClassCount / tagCount));

                    if (logProbability > minimumRuleLogProbability) {
                        prods.add(new Production(parent, child, logProbability, true, vocabulary, lexicon));
                    }

                } else {
                    // c(w|t) / c(t) - observations of this word / observations of the parent
                    final float logProbability = (float) Math.log(wordGivenTag / tagCount);
                    if (logProbability > minimumRuleLogProbability) {
                        prods.add(new Production(parent, child, logProbability, true, vocabulary, lexicon));
                    }
                }
            }

            // Add UNK-class rules for open-class parents
            if (openClassPreterminal) {

                for (final int unkClass : unkClassCounts.get(parent).keySet()) {
                    // c(UNK-class|t) / c(t)
                    final double unkClassCount = unkClassCounts.get(parent).get(unkClass);
                    final float logProbability = (float) Math.log(unkClassCount / tagCount);

                    if (logProbability > minimumRuleLogProbability) {
                        prods.add(new Production(parent, unkClass, logProbability, true, vocabulary, lexicon));
                    }
                }
            }
        }

        return prods;
    }

    /**
     * Adds pseudo-counts for UNK classes, only for open-class tags (those with more than
     * <code>openClassPreterminalThreshold</code> observed children)
     * 
     * @param openClassPreterminalThreshold Minimum child word observations required to consider a preterminal as
     *            open-class
     */
    private Short2ObjectOpenHashMap<Int2DoubleOpenHashMap> unkClassCounts(final Int2IntOpenHashMap unkClassMap,
            final int openClassPreterminalThreshold, final Int2IntOpenHashMap corpusWordCounts,
            final int rareWordThreshold) {
        final Short2ObjectOpenHashMap<Int2DoubleOpenHashMap> unkClassCounts = new Short2ObjectOpenHashMap<Int2DoubleOpenHashMap>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!lexicalRuleCounts.containsKey(parent)) {
                continue;
            }

            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);

            if (childMap.size() < openClassPreterminalThreshold) {
                continue;
            }

            final Int2DoubleOpenHashMap unkChildMap = new Int2DoubleOpenHashMap();
            unkChildMap.defaultReturnValue(0);
            unkClassCounts.put(parent, unkChildMap);

            for (int child = 0; child < lexicon.size(); child++) {

                final double wordGivenTag = childMap.get(child);
                if (wordGivenTag > 0 && corpusWordCounts.get(child) < rareWordThreshold) {
                    final int unkClass = unkClassMap.get(child);
                    unkChildMap.put(unkClass, unkChildMap.get(unkClass) + wordGivenTag);
                }
            }
        }
        return unkClassCounts;
    }

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

    /**
     * Add UNK rules to the lexicon, stealing from other lexical probabilities to allot some probability mass for UNK
     * rules.
     * 
     * Note that even rare words (those that occur <= lexicalUnkThreshold times) will still be included in their
     * lexicalized form.
     * 
     * @param countGrammar {@link FractionalCountGrammar} with posterior counts from training corpus. Note : fractional
     *            counts will be added for UNK rules, modifying this grammar.
     */
    public void addUnkProbabilities(final Collection<NaryTree<String>> goldTrees, final int lexicalUnkThreshold) {

        //
        // Count words - overall and separately when they occur as the first word in a sentence
        //
        final Object2IntOpenHashMap<String> lexicalEntryCounts = new Object2IntOpenHashMap<String>();
        lexicalEntryCounts.defaultReturnValue(0);

        for (final NaryTree<String> tree : goldTrees) {

            for (final NaryTree<String> leaf : tree.leafTraversal()) {
                final String word = leaf.label();
                lexicalEntryCounts.put(word, lexicalEntryCounts.getInt(word) + 1);
            }
        }

        // Create a map from lexical entries to parents and probabilities (word -> parent -> probability)
        final Int2ObjectOpenHashMap<Short2DoubleOpenHashMap> lexicalParentProbabilities = new Int2ObjectOpenHashMap<Short2DoubleOpenHashMap>();
        for (final short parent : lexicalRuleCounts.keySet()) {
            final Int2DoubleOpenHashMap childMap = lexicalRuleCounts.get(parent);
            for (final int child : childMap.keySet()) {
                Short2DoubleOpenHashMap parentMap = lexicalParentProbabilities.get(child);
                if (parentMap == null) {
                    parentMap = new Short2DoubleOpenHashMap();
                    parentMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                    lexicalParentProbabilities.put(child, parentMap);
                }
                parentMap.put(parent, childMap.get(child) / parentCounts.get(parent));
            }
        }

        //
        // Iterate through the training corpus again, adding UNK counts for rare words
        //
        for (final NaryTree<String> tree : goldTrees) {

            // Handle first word separately
            final Tree<String> leftmostLeaf = tree.leftmostLeaf();
            final String firstWord = leftmostLeaf.label();
            if (lexicalEntryCounts.getInt(firstWord) <= lexicalUnkThreshold) {
                final int initialUnkIndex = lexicon.addSymbol(Tokenizer.berkeleyGetSignature(firstWord, true, lexicon));
                final Short2DoubleOpenHashMap parentMap = lexicalParentProbabilities.get(lexicon.getIndex(firstWord));
                for (final short parent : parentMap.keySet()) {
                    incrementLexicalCount(parent, initialUnkIndex, parentMap.get(parent));
                }
            }

            for (final NaryTree<String> leaf : tree.leafTraversal()) {

                final String word = leaf.label();
                final int count = lexicalEntryCounts.getInt(word);

                if (count <= lexicalUnkThreshold) {
                    final int unkIndex = lexicon.addSymbol(Tokenizer.berkeleyGetSignature(word, false, lexicon));
                    final Short2DoubleOpenHashMap parentMap = lexicalParentProbabilities.get(lexicon.getIndex(word));
                    for (final short parent : parentMap.keySet()) {
                        incrementLexicalCount(parent, unkIndex, parentMap.get(parent));
                    }
                }
            }
        }
    }

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
        return binaryProductions(Float.NEGATIVE_INFINITY).size();
    }

    public final int unaryRules() {
        return unaryProductions(Float.NEGATIVE_INFINITY).size();
    }

    public final int lexicalRules() {
        return lexicalProductions(Float.NEGATIVE_INFINITY).size();
    }

    public final double observations(final String parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(final boolean fraction) {
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

        final StringBuilder sb = new StringBuilder(1024);
        for (final String rule : binaryRules) {
            sb.append(rule);
            sb.append('\n');
        }
        for (final String rule : unaryRules) {
            sb.append(rule);
            sb.append('\n');
        }

        sb.append(Grammar.LEXICON_DELIMITER);
        sb.append('\n');

        for (final String rule : lexicalRules) {
            sb.append(rule);
            sb.append('\n');
        }
        return sb.toString();
    }
}
