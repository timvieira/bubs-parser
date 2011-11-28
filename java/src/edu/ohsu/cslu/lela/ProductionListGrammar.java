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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

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
}
