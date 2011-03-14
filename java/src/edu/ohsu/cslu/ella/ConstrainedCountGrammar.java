package edu.ohsu.cslu.ella;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.util.ArrayList;

import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Grammar computed from (fractional) observation counts, constrained by a base (unsplit) grammar.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ConstrainedCountGrammar extends FractionalCountGrammar {

    /** Parent -> Left child -> Right child -> log(count) */
    private final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> binaryRuleLogCounts = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
    private final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> unaryRuleLogCounts = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
    private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> lexicalRuleLogCounts = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();

    /** Parent -> Packed children -> log(count) */
    private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> packedBinaryRuleLogCounts;
    private final PackingFunction packingFunction;

    // TODO Rename these; they're not really base rules, they're sums over split parents
    /** Parent -> Base grammar left child -> Base grammar right child -> log(count) */
    private final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> baseBinaryRuleLogCounts = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
    /** Parent -> Base grammar child -> log(count) */
    private final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> baseUnaryRuleLogCounts = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
    private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> baseLexicalRuleLogCounts = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();

    /** Parent -> Base grammar packed children -> log(count) */
    private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> basePackedBinaryRuleLogCounts;

    /** Base grammar parent -> Base grammar left child -> Base grammar right child -> log(probability) */
    private final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> baseBinaryRuleLogProbabilities;
    /** Base grammar parent -> Base grammar child -> log(probability) */
    private final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> baseUnaryRuleLogProbabilities;
    private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> baseLexicalRuleLogProbabilities;

    private final Int2IntOpenHashMap basePackedChildren;

    private ConstrainedCountGrammar(final SplitVocabulary vocabulary, final SymbolSet<String> lexicon,
            final PackingFunction packingFunction) {

        super(vocabulary, lexicon);

        this.packingFunction = packingFunction;
        if (packingFunction != null) {
            packedBinaryRuleLogCounts = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();
            basePackedBinaryRuleLogCounts = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();
            basePackedChildren = new Int2IntOpenHashMap();
            basePackedChildren.defaultReturnValue(Integer.MIN_VALUE);
            baseBinaryRuleLogProbabilities = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
            baseUnaryRuleLogProbabilities = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
            baseLexicalRuleLogProbabilities = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();
        } else {
            packedBinaryRuleLogCounts = null;
            basePackedBinaryRuleLogCounts = null;
            basePackedChildren = null;
            baseBinaryRuleLogProbabilities = null;
            baseUnaryRuleLogProbabilities = null;
            baseLexicalRuleLogProbabilities = null;
        }
    }

    public ConstrainedCountGrammar(final ConstrainedCsrSparseMatrixGrammar grammar) {
        this((SplitVocabulary) grammar.nonTermSet, grammar.lexSet, grammar.cartesianProductFunction);

        // Initialize maps of base grammar probabilities
        for (final Production p : grammar.parentGrammar.binaryProductions) {
            Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = baseBinaryRuleLogProbabilities
                    .get((short) p.parent);
            if (leftChildMap == null) {
                leftChildMap = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
                baseBinaryRuleLogProbabilities.put((short) p.parent, leftChildMap);
            }

            Short2FloatOpenHashMap rightChildMap = leftChildMap.get((short) p.leftChild);
            if (rightChildMap == null) {
                rightChildMap = new Short2FloatOpenHashMap();
                rightChildMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                leftChildMap.put((short) p.leftChild, rightChildMap);
            }

            rightChildMap.put((short) p.rightChild, p.prob);
        }

        for (final Production p : grammar.parentGrammar.unaryProductions) {
            Short2FloatOpenHashMap childMap = baseUnaryRuleLogProbabilities.get((short) p.parent);
            if (childMap == null) {
                childMap = new Short2FloatOpenHashMap();
                childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                baseUnaryRuleLogProbabilities.put((short) p.parent, childMap);
            }

            childMap.put((short) p.leftChild, p.prob);
        }

        for (final Production p : grammar.parentGrammar.lexicalProductions) {
            Int2FloatOpenHashMap childMap = baseLexicalRuleLogProbabilities.get((short) p.parent);
            if (childMap == null) {
                childMap = new Int2FloatOpenHashMap();
                childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                baseLexicalRuleLogProbabilities.put((short) p.parent, childMap);
            }

            childMap.put(p.leftChild, p.prob);
        }
    }

    public ConstrainedCountGrammar(final SplitVocabulary vocabulary, final SymbolSet<String> lexicon) {
        this(vocabulary, lexicon, null);
    }

    public void incrementBinaryLogCount(final short parent, final short leftChild, final short rightChild,
            final float logIncrement) {

        if (Float.isNaN(logIncrement)) {
            System.out.println("NaN");
        }
        incrementBinaryLogCount(binaryRuleLogCounts, parent, leftChild, rightChild, logIncrement);

        // Base grammar rule count
        incrementBinaryLogCount(baseBinaryRuleLogCounts, parent, vocabulary.baseCategoryIndices[leftChild],
                vocabulary.baseCategoryIndices[rightChild], logIncrement);
    }

    public void incrementBinaryLogCount(
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

        if (Float.isNaN(logIncrement)) {
            System.out.println("NaN");
        }
        incrementBinaryLogCount(packedBinaryRuleLogCounts, parent, packedChildren, logIncrement);

        int baseChildren = basePackedChildren.get(packedChildren);
        if (baseChildren < 0) {
            final short baseLeftChild = vocabulary.baseCategoryIndices[packingFunction.unpackLeftChild(packedChildren)];
            final short baseRightChild = vocabulary.baseCategoryIndices[packingFunction
                    .unpackRightChild(packedChildren)];
            baseChildren = baseLeftChild << 16 | baseRightChild;
        }

        // Base grammar rule count
        incrementBinaryLogCount(basePackedBinaryRuleLogCounts, parent, baseChildren, logIncrement);
    }

    public void incrementBinaryLogCount(final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> countMap,
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

    @Override
    public void incrementBinaryCount(final short parent, final short leftChild, final short rightChild,
            final float increment) {
        incrementBinaryLogCount(parent, leftChild, rightChild, (float) Math.log(increment));
    }

    public void incrementUnaryLogCount(final short parent, final short child, final float logIncrement) {

        incrementUnaryLogCount(unaryRuleLogCounts, parent, child, logIncrement);

        // Base grammar rule count
        incrementUnaryLogCount(baseUnaryRuleLogCounts, parent, vocabulary.baseCategoryIndices[child], logIncrement);
    }

    public void incrementUnaryLogCount(final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> countMap,
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

    @Override
    public void incrementUnaryCount(final short parent, final short child, final float increment) {
        incrementUnaryLogCount(parent, child, (float) Math.log(increment));
    }

    public void incrementLexicalLogCount(final short parent, final int child, final float logIncrement) {

        // final String sParent = vocabulary.getSymbol(parent);
        // final String sChild = lexicon.getSymbol(child);
        incrementLexicalLogCount(lexicalRuleLogCounts, parent, child, logIncrement);

        // Base grammar rule count
        incrementLexicalLogCount(baseLexicalRuleLogCounts, parent, child, logIncrement);
    }

    public void incrementLexicalLogCount(final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> countMap,
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

    @Override
    public void incrementLexicalCount(final short parent, final int child, final float increment) {
        incrementLexicalLogCount(parent, child, (float) Math.log(increment));
    }

    @Override
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
                    // final double observations = Math.exp(binaryRuleLogObservations(binaryRuleLogCounts, parent,
                    // leftChild, rightChild));
                    // final double baseRuleObservations = Math.exp(binaryRuleLogObservations(baseBinaryRuleLogCounts,
                    // parent, vocabulary.baseCategoryIndices[leftChild],
                    // vocabulary.baseCategoryIndices[rightChild]));

                    // final String observationProbability = Assert
                    // .fraction(Math.log(observations / baseRuleObservations));
                    // final String baseRuleProbability = Assert.fraction(baseBinaryRuleLogProbabilities
                    // .get(vocabulary.baseCategoryIndices[parent]).get(vocabulary.baseCategoryIndices[leftChild])
                    // .get(vocabulary.baseCategoryIndices[rightChild]));

                    // Observations of this rule / Observations of all split rules with the parent X Base rule
                    // probability
                    final float logObservations = binaryRuleLogObservations(binaryRuleLogCounts, parent, leftChild,
                            rightChild);
                    if (logObservations != Float.NEGATIVE_INFINITY) {
                        final float logProbability = logObservations
                                - binaryRuleLogObservations(baseBinaryRuleLogCounts, parent,
                                        vocabulary.baseCategoryIndices[leftChild],
                                        vocabulary.baseCategoryIndices[rightChild])
                                + (baseBinaryRuleLogProbabilities != null ? baseBinaryRuleLogProbabilities
                                        .get(vocabulary.baseCategoryIndices[parent])
                                        .get(vocabulary.baseCategoryIndices[leftChild])
                                        .get(vocabulary.baseCategoryIndices[rightChild]) : 0);
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
                    incrementBinaryLogCount(parent, leftChild, rightChild, childMap.remove(packedChildren));
                }
            }
        }
    }

    @Override
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

                // Observations of this rule / Observations of all split rules with the parent X Base rule probability
                final float logObservations = unaryRuleLogObservations(unaryRuleLogCounts, parent, child);
                if (logObservations != Float.NEGATIVE_INFINITY) {
                    final float logProbability = logObservations
                            - unaryRuleLogObservations(baseUnaryRuleLogCounts, parent,
                                    vocabulary.baseCategoryIndices[child])
                            + (baseUnaryRuleLogProbabilities != null ? baseUnaryRuleLogProbabilities.get(
                                    vocabulary.baseCategoryIndices[parent]).get(vocabulary.baseCategoryIndices[child])
                                    : 0);
                    prods.add(new Production(parent, child, logProbability, false, vocabulary, lexicon));
                }
            }
        }

        return prods;
    }

    @Override
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
                // final double observations = Math.exp(lexicalRuleLogObservations(lexicalRuleLogCounts, parent,
                // child));
                // final double baseRuleObservations = Math.exp(lexicalRuleLogObservations(baseLexicalRuleLogCounts,
                // parent, child));
                //
                // final String observationProbability = Assert.fraction(Math.log(observations / baseRuleObservations));
                // final String baseRuleProbability = Assert.fraction(baseLexicalRuleLogProbabilities.get(
                // vocabulary.baseCategoryIndices[parent]).get(child));

                // Observations of this rule / Observations of all split rules with the parent X Base rule probability
                final float logObservations = lexicalRuleLogObservations(lexicalRuleLogCounts, parent, child);
                if (logObservations != Float.NEGATIVE_INFINITY) {
                    final float logProbability = logObservations
                            - lexicalRuleLogObservations(baseLexicalRuleLogCounts, parent, child)
                            + (baseLexicalRuleLogProbabilities != null ? baseLexicalRuleLogProbabilities.get(
                                    vocabulary.baseCategoryIndices[parent]).get(child) : 0);
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
}
