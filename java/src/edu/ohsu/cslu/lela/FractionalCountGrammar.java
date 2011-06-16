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
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.TreeSet;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.tests.JUnit;

public class FractionalCountGrammar implements CountGrammar {

    protected final SplitVocabulary vocabulary;
    protected final SymbolSet<String> lexicon;
    protected final String startSymbol;

    // TODO Map to FloatList instead of directly to float, and use Math.logSumExp() to do all the log-summing at once
    /** Parent -> Left child -> Right child -> log(count) */
    private final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> binaryRuleLogCounts = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
    private final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> unaryRuleLogCounts = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
    private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> lexicalRuleLogCounts = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();

    /** Parent -> Packed children -> log(count) */
    private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> packedBinaryRuleLogCounts;
    private final PackingFunction packingFunction;

    /** Parent -> log(count) */
    private final Short2FloatOpenHashMap parentLogCounts = new Short2FloatOpenHashMap();

    // // TODO Rename these; they're not really base rules, they're sums over split parents
    // /** Parent -> Base grammar left child -> Base grammar right child -> log(count) */
    // private final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> baseBinaryRuleLogCounts =
    // new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
    // /** Parent -> Base grammar child -> log(count) */
    // private final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> baseUnaryRuleLogCounts = new
    // Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
    // private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> baseLexicalRuleLogCounts = new
    // Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();

    // /** Parent -> Base grammar packed children -> log(count) */
    // private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> basePackedBinaryRuleLogCounts;

    public FractionalCountGrammar(final SplitVocabulary vocabulary, final SymbolSet<String> lexicon,
            final PackingFunction packingFunction) {

        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
        this.startSymbol = vocabulary.getSymbol(0);

        this.packingFunction = packingFunction;
        if (packingFunction != null) {
            packedBinaryRuleLogCounts = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();
        } else {
            packedBinaryRuleLogCounts = null;
        }
        parentLogCounts.defaultReturnValue(Float.NEGATIVE_INFINITY);
    }

    public void incrementBinaryLogCount(final short parent, final short leftChild, final short rightChild,
            final float logIncrement) {

        incrementBinaryLogCount(binaryRuleLogCounts, parent, leftChild, rightChild, logIncrement);
        incrementParentLogCount(parent, logIncrement);
    }

    protected void incrementBinaryLogCount(
            final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> countMap,
            final short parent, final short leftChild, final short rightChild, final float logIncrement) {

        Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = countMap.get(parent);
        if (leftChildMap == null) {
            leftChildMap = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
            countMap.put(parent, leftChildMap);
        }

        Short2FloatOpenHashMap rightChildMap = leftChildMap.get(leftChild);
        if (rightChildMap == null) {
            rightChildMap = new Short2FloatOpenHashMap();
            rightChildMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
            leftChildMap.put(leftChild, rightChildMap);
        }

        if (rightChildMap.containsKey(rightChild)) {
            rightChildMap.put(rightChild, edu.ohsu.cslu.util.Math.logSum(rightChildMap.get(rightChild), logIncrement));
        } else {
            rightChildMap.put(rightChild, logIncrement);
        }
    }

    public void incrementBinaryLogCount(final short parent, final int packedChildren, final float logIncrement) {

        incrementBinaryLogCount(packedBinaryRuleLogCounts, parent, packedChildren, logIncrement);
        incrementParentLogCount(parent, logIncrement);
    }

    protected void incrementBinaryLogCount(final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> countMap,
            final short parent, final int packedChildren, final float logIncrement) {

        Int2FloatOpenHashMap childMap = countMap.get(parent);
        if (childMap == null) {
            childMap = new Int2FloatOpenHashMap();
            childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
            countMap.put(parent, childMap);
        }

        if (childMap.containsKey(packedChildren)) {
            childMap.put(packedChildren, edu.ohsu.cslu.util.Math.logSum(childMap.get(packedChildren), logIncrement));
        } else {
            childMap.put(packedChildren, logIncrement);
        }
    }

    public void incrementBinaryCount(final short parent, final short leftChild, final short rightChild,
            final float increment) {
        incrementBinaryLogCount(parent, leftChild, rightChild, (float) Math.log(increment));
    }

    /**
     * For unit tests
     */
    void incrementBinaryCount(final String parent, final String leftChild, final String rightChild,
            final float increment) {
        incrementBinaryLogCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(leftChild),
                (short) vocabulary.getIndex(rightChild), (float) Math.log(increment));
    }

    public void incrementUnaryLogCount(final short parent, final short child, final float logIncrement) {

        incrementUnaryLogCount(unaryRuleLogCounts, parent, child, logIncrement);
        incrementParentLogCount(parent, logIncrement);
    }

    protected void incrementUnaryLogCount(final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> countMap,
            final short parent, final short child, final float logIncrement) {

        Short2FloatOpenHashMap childMap = countMap.get(parent);
        if (childMap == null) {
            childMap = new Short2FloatOpenHashMap();
            childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
            countMap.put(parent, childMap);
        }

        if (childMap.containsKey(child)) {
            childMap.put(child, edu.ohsu.cslu.util.Math.logSum(childMap.get(child), logIncrement));
        } else {
            childMap.put(child, logIncrement);
        }
    }

    public void incrementUnaryCount(final short parent, final short child, final float increment) {
        incrementUnaryLogCount(parent, child, (float) Math.log(increment));
    }

    /**
     * For unit tests
     */
    void incrementUnaryCount(final String parent, final String child, final float increment) {
        incrementUnaryLogCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(child),
                (float) Math.log(increment));
    }

    public void incrementLexicalLogCount(final short parent, final int child, final float logIncrement) {

        incrementLexicalLogCount(lexicalRuleLogCounts, parent, child, logIncrement);
        incrementParentLogCount(parent, logIncrement);
    }

    protected void incrementLexicalLogCount(final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> countMap,
            final short parent, final int child, final float logIncrement) {

        Int2FloatOpenHashMap childMap = countMap.get(parent);
        if (childMap == null) {
            childMap = new Int2FloatOpenHashMap();
            childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
            countMap.put(parent, childMap);
        }

        if (childMap.containsKey(child)) {
            childMap.put(child, edu.ohsu.cslu.util.Math.logSum(childMap.get(child), logIncrement));
        } else {
            childMap.put(child, logIncrement);
        }
    }

    public void incrementLexicalCount(final short parent, final int child, final float increment) {
        incrementLexicalLogCount(parent, child, (float) Math.log(increment));
    }

    /**
     * For unit tests
     */
    void incrementLexicalCount(final String parent, final String child, final float increment) {
        incrementLexicalLogCount((short) vocabulary.getIndex(parent), lexicon.getIndex(child),
                (float) Math.log(increment));
    }

    public void incrementParentLogCount(final short parent, final float logIncrement) {

        if (parentLogCounts.containsKey(parent)) {
            parentLogCounts.put(parent, edu.ohsu.cslu.util.Math.logSum(parentLogCounts.get(parent), logIncrement));
        } else {
            parentLogCounts.put(parent, logIncrement);
        }
    }

    public ArrayList<Production> binaryProductions() {

        final ArrayList<Production> prods = new ArrayList<Production>();

        movePackedCountsToBinaryMap();
        for (short parent = 0; parent < vocabulary.size(); parent++) {

            if (!binaryRuleLogCounts.containsKey(parent)) {
                continue;
            }

            final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = binaryRuleLogCounts.get(parent);

            for (short leftChild = 0; leftChild < vocabulary.size(); leftChild++) {
                if (!leftChildMap.containsKey(leftChild)) {
                    continue;
                }

                final Short2FloatOpenHashMap rightChildMap = leftChildMap.get(leftChild);

                for (short rightChild = 0; rightChild < vocabulary.size(); rightChild++) {
                    if (!rightChildMap.containsKey(rightChild)) {
                        continue;
                    }

                    // final String sParent = vocabulary.getSymbol(parent);
                    // final String sLeftChild = vocabulary.getSymbol(leftChild);
                    // final String sRightChild = vocabulary.getSymbol(rightChild);
                    //
                    // final double observations = Math.exp(binaryRuleLogObservations(binaryRuleLogCounts,
                    // parent,
                    // leftChild, rightChild));
                    // final double baseRuleObservations =
                    // Math.exp(binaryRuleLogObservations(baseBinaryRuleLogCounts,
                    // parent, vocabulary.baseCategoryIndices[leftChild],
                    // vocabulary.baseCategoryIndices[rightChild]));

                    // final String observationProbability = Assert
                    // .fraction(Math.log(observations / baseRuleObservations));
                    // final String baseRuleProbability = Assert.fraction(baseBinaryRuleLogProbabilities
                    // .get(vocabulary.baseCategoryIndices[parent]).get(vocabulary.baseCategoryIndices[leftChild])
                    // .get(vocabulary.baseCategoryIndices[rightChild]));

                    // Observations of this rule / Observations of all split rules with the parent
                    final float logObservations = binaryRuleLogObservations(binaryRuleLogCounts, parent, leftChild,
                            rightChild);
                    if (logObservations != Float.NEGATIVE_INFINITY) {
                        final float logProbability = logObservations - parentLogCounts.get(parent);
                        prods.add(new Production(parent, leftChild, rightChild, logProbability, vocabulary, lexicon));
                    }
                }
            }
        }

        return prods;
    }

    /**
     * Moves all observations from packed maps to 'normal' binary maps
     */
    private void movePackedCountsToBinaryMap() {

        if (packingFunction == null) {
            return;
        }

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            // Copy all productions from packed map to normal map
            if (packedBinaryRuleLogCounts.containsKey(parent)) {
                final Int2FloatOpenHashMap childMap = packedBinaryRuleLogCounts.get(parent);
                for (final int packedChildren : childMap.keySet()) {
                    final short leftChild = (short) packingFunction.unpackLeftChild(packedChildren);
                    final short rightChild = packingFunction.unpackRightChild(packedChildren);
                    incrementBinaryLogCount(binaryRuleLogCounts, parent, leftChild, rightChild,
                            childMap.remove(packedChildren));
                }
            }
        }
    }

    public ArrayList<Production> unaryProductions() {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!unaryRuleLogCounts.containsKey(parent)) {
                continue;
            }

            final Short2FloatOpenHashMap childMap = unaryRuleLogCounts.get(parent);

            for (short child = 0; child < vocabulary.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                // Observations of this rule / Observations of all rules with the parent
                final float logObservations = unaryRuleLogObservations(unaryRuleLogCounts, parent, child);
                if (logObservations != Float.NEGATIVE_INFINITY) {
                    final float logProbability = logObservations - parentLogCounts.get(parent);
                    prods.add(new Production(parent, child, logProbability, false, vocabulary, lexicon));
                }
            }
        }

        return prods;
    }

    public ArrayList<Production> lexicalProductions() {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!lexicalRuleLogCounts.containsKey(parent)) {
                continue;
            }

            final Int2FloatOpenHashMap childMap = lexicalRuleLogCounts.get(parent);

            for (int child = 0; child < lexicon.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                // final String sParent = vocabulary.getSymbol(parent);
                // final String sChild = lexicon.getSymbol(child);
                //
                // final double observations = Math.exp(lexicalRuleLogObservations(lexicalRuleLogCounts,
                // parent,
                // child));
                // final double baseRuleObservations =
                // Math.exp(lexicalRuleLogObservations(baseLexicalRuleLogCounts,
                // parent, child));
                //
                // final String observationProbability = Assert.fraction(Math.log(observations /
                // baseRuleObservations));
                // final String baseRuleProbability = Assert.fraction(baseLexicalRuleLogProbabilities.get(
                // vocabulary.baseCategoryIndices[parent]).get(child));

                // Observations of this rule / Observations of all rules with the parent
                final float logObservations = lexicalRuleLogObservations(lexicalRuleLogCounts, parent, child);
                if (logObservations != Float.NEGATIVE_INFINITY) {
                    final float logProbability = logObservations - parentLogCounts.get(parent);
                    prods.add(new Production(parent, child, logProbability, true, vocabulary, lexicon));
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
    public final float binaryRuleObservations(final String parent, final String leftChild, final String rightChild) {

        return (float) Math.exp(binaryRuleLogObservations(binaryRuleLogCounts, (short) vocabulary.getIndex(parent),
                (short) vocabulary.getIndex(leftChild), (short) vocabulary.getIndex(rightChild)));
    }

    /**
     * Returns the log of the number of observations of a binary rule.
     * 
     * @param countMap
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return the number of observations of a binary rule.
     */
    private float binaryRuleLogObservations(
            final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> countMap,
            final short parent, final short leftChild, final short rightChild) {

        final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = countMap.get(parent);
        if (leftChildMap == null) {
            return Float.NEGATIVE_INFINITY;
        }

        final Short2FloatOpenHashMap rightChildMap = leftChildMap.get(leftChild);
        if (rightChildMap == null) {
            return Float.NEGATIVE_INFINITY;
        }

        return rightChildMap.get(rightChild);
    }

    /**
     * Returns the number of observations of a unary rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a unary rule.
     */
    public final float unaryRuleObservations(final String parent, final String child) {
        return (float) Math.exp(unaryRuleLogObservations(unaryRuleLogCounts, (short) vocabulary.getIndex(parent),
                (short) vocabulary.getIndex(child)));
    }

    /**
     * Returns the log of the number of observations of a unary rule.
     * 
     * @param countMap
     * @param parent
     * @param child
     * @return the number of observations of a unary rule.
     */
    private float unaryRuleLogObservations(final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> countMap,
            final short parent, final short child) {

        final Short2FloatOpenHashMap childMap = countMap.get(parent);
        if (childMap == null) {
            return Float.NEGATIVE_INFINITY;
        }

        return childMap.get(child);
    }

    /**
     * Returns the number of observations of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a lexical rule.
     */
    public final float lexicalRuleObservations(final String parent, final String child) {
        return (float) Math.exp(lexicalRuleLogObservations(lexicalRuleLogCounts, (short) vocabulary.getIndex(parent),
                lexicon.getIndex(child)));
    }

    /**
     * Returns the natural log of the number of observations of a lexical rule.
     * 
     * @param countMap
     * @param parent
     * @param child
     * @return the number of observations of a lexical rule.
     */
    private float lexicalRuleLogObservations(final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> countMap,
            final short parent, final int child) {

        final Int2FloatOpenHashMap childMap = countMap.get(parent);
        if (childMap == null) {
            return Float.NEGATIVE_INFINITY;
        }

        return childMap.get(child);
    }

    public final int totalRules() {
        return binaryRules() + unaryRules() + lexicalRules();
    }

    public final int binaryRules() {
        return binaryProductions().size();
    }

    public final int unaryRules() {
        return unaryProductions().size();
    }

    public final int lexicalRules() {
        return lexicalProductions().size();
    }

    public final float observations(final String parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        // TODO Switch from fractions to logs
        return toString(true);
    }

    public String toString(final boolean fraction) {
        final TreeSet<String> binaryRules = new TreeSet<String>();
        for (final Production p : binaryProductions()) {
            if (fraction) {
                binaryRules.add(String.format("%s -> %s %s %s", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), JUnit.fraction(p.prob)));
            } else {
                binaryRules.add(String.format("%s -> %s %s %.4f", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), vocabulary.getSymbol(p.rightChild), p.prob));
            }
        }

        final TreeSet<String> unaryRules = new TreeSet<String>();
        for (final Production p : unaryProductions()) {
            if (fraction) {
                unaryRules.add(String.format("%s -> %s %s", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), JUnit.fraction(p.prob)));
            } else {
                unaryRules.add(String.format("%s -> %s %.4f", vocabulary.getSymbol(p.parent),
                        vocabulary.getSymbol(p.leftChild), p.prob));
            }
        }

        final TreeSet<String> lexicalRules = new TreeSet<String>();
        for (final Production p : lexicalProductions()) {
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

        sb.append(Grammar.DELIMITER);
        sb.append('\n');

        for (final String rule : lexicalRules) {
            sb.append(rule);
            sb.append('\n');
        }
        return sb.toString();
    }
}
