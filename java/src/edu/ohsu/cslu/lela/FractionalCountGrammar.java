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
import it.unimi.dsi.fastutil.shorts.Short2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.TreeSet;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
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

    public ProductionListGrammar toProductionListGrammar(final float minimumRuleLogProbability) {

        // Construct a pruned (but not normalized) grammar from the accumulated fractional rule counts
        final ProductionListGrammar prunedGrammar = new ProductionListGrammar(vocabulary, lexicon,
                binaryProductions(minimumRuleLogProbability), unaryProductions(minimumRuleLogProbability),
                lexicalProductions(minimumRuleLogProbability));

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
        // TODO Switch from fractions to logs
        return toString(true);
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
