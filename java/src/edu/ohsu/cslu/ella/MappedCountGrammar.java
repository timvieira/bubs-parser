package edu.ohsu.cslu.ella;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.util.ArrayList;

import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Grammar computed from observation counts in which the non-terminals and terminal vocabularies are mapped to short /
 * int values using {@link SymbolSet}.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class MappedCountGrammar implements CountGrammar {

    protected SplitVocabulary vocabulary;
    protected SymbolSet<String> lexicon;

    /**
     * Contains occurrence counts for each non-terminal which occurs as a binary parent. When representing the grammar
     * for inside-outside re-estimation, we may be able to save space by not creating certain data structures for
     * non-terminals which don't occur as binary parents. And we may be able to save execution time by sorting other
     * data structures according to frequency counts.
     */
    final Short2FloatMap binaryParentCounts = new Short2FloatOpenHashMap();

    /** Occurrence counts for each non-terminal which occurs as a unary parent. */
    final Short2FloatMap unaryParentCounts = new Short2FloatOpenHashMap();

    /** Occurrence counts for each non-terminal which occurs as a lexical parent. */
    final Short2FloatMap lexicalParentCounts = new Short2FloatOpenHashMap();

    private Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> binaryRuleCounts = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
    private Short2ObjectOpenHashMap<Short2FloatOpenHashMap> unaryRuleCounts = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
    private Short2ObjectOpenHashMap<Int2FloatOpenHashMap> lexicalRuleCounts = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();

    private int binaryRules;
    private int unaryRules;
    private int lexicalRules;

    String startSymbol;

    public MappedCountGrammar(final SplitVocabulary vocabulary, final SymbolSet<String> lexicon) {
        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
        this.startSymbol = vocabulary.getSymbol(0);
    }

    public void incrementBinaryCount(final short parent, final short leftChild, final short rightChild) {
        incrementBinaryCount(parent, leftChild, rightChild, 1f);
    }

    public void incrementBinaryCount(final short parent, final short leftChild, final short rightChild,
            final float increment) {

        binaryParentCounts.put(parent, binaryParentCounts.get(parent) + increment);
        Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);
        if (leftChildMap == null) {
            leftChildMap = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
            binaryRuleCounts.put(parent, leftChildMap);
        }

        Short2FloatOpenHashMap rightChildMap1 = leftChildMap.get(leftChild);
        if (rightChildMap1 == null) {
            rightChildMap1 = new Short2FloatOpenHashMap();
            leftChildMap.put(leftChild, rightChildMap1);
        }

        final Short2FloatOpenHashMap rightChildMap = rightChildMap1;
        if (rightChildMap.containsKey(rightChild)) {
            rightChildMap.put(rightChild, rightChildMap.get(rightChild) + increment);
        } else {
            rightChildMap.put(rightChild, increment);
            binaryRules++;
        }
    }

    public void incrementBinaryCount(final String parent, final String leftChild, final String rightChild) {
        incrementBinaryCount(parent, leftChild, rightChild, 1f);
    }

    public void incrementBinaryCount(final String parent, final String leftChild, final String rightChild,
            final float increment) {
        incrementBinaryCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(leftChild),
                (short) vocabulary.getIndex(rightChild), increment);
    }

    public void incrementUnaryCount(final short parent, final short child) {
        incrementUnaryCount(parent, child, 1f);
    }

    public void incrementUnaryCount(final short parent, final short child, final float increment) {

        unaryParentCounts.put(parent, unaryParentCounts.get(parent) + increment);
        Short2FloatOpenHashMap childMap1 = unaryRuleCounts.get(parent);
        if (childMap1 == null) {
            childMap1 = new Short2FloatOpenHashMap();
            unaryRuleCounts.put(parent, childMap1);
        }

        final Short2FloatOpenHashMap childMap = childMap1;

        if (childMap.containsKey(child)) {
            childMap.put(child, childMap.get(child) + increment);
        } else {
            childMap.put(child, increment);
            unaryRules++;
        }
    }

    public void incrementUnaryCount(final String parent, final String child) {
        incrementUnaryCount(parent, child, 1f);
    }

    public void incrementUnaryCount(final String parent, final String child, final float increment) {
        incrementUnaryCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(child), increment);
    }

    public void incrementLexicalCount(final short parent, final int child) {
        incrementLexicalCount(parent, child, 1f);
    }

    public void incrementLexicalCount(final short parent, final int child, final float increment) {

        lexicalParentCounts.put(parent, lexicalParentCounts.get(parent) + increment);
        Int2FloatOpenHashMap childMap = lexicalRuleCounts.get(parent);
        if (childMap == null) {
            childMap = new Int2FloatOpenHashMap();
            lexicalRuleCounts.put(parent, childMap);
        }

        if (childMap.containsKey(child)) {
            childMap.put(child, childMap.get(child) + increment);
        } else {
            childMap.put(child, increment);
            lexicalRules++;
        }
    }

    public void incrementLexicalCount(final String parent, final String child) {
        incrementLexicalCount(parent, child, 1f);
    }

    public void incrementLexicalCount(final String parent, final String child, final float increment) {
        incrementLexicalCount((short) vocabulary.getIndex(parent), lexicon.getIndex(child), increment);
    }

    public ArrayList<Production> binaryProductions() {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!binaryRuleCounts.containsKey(parent)) {
                continue;
            }

            final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);

            for (short leftChild = 0; leftChild < vocabulary.size(); leftChild++) {
                if (!leftChildMap.containsKey(leftChild)) {
                    continue;
                }

                final Short2FloatOpenHashMap rightChildMap = leftChildMap.get(leftChild);

                for (short rightChild = 0; rightChild < vocabulary.size(); rightChild++) {
                    if (!rightChildMap.containsKey(rightChild)) {
                        continue;
                    }

                    final float probability = (float) Math.log(binaryRuleObservations(parent, leftChild, rightChild)
                            / observations(parent));
                    prods.add(new Production(parent, leftChild, rightChild, probability, vocabulary, lexicon));
                }
            }
        }

        return prods;
    }

    public ArrayList<Production> unaryProductions() {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!unaryRuleCounts.containsKey(parent)) {
                continue;
            }

            final Short2FloatOpenHashMap childMap = unaryRuleCounts.get(parent);

            for (short child = 0; child < vocabulary.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                final float probability = (float) Math.log(unaryRuleObservations(parent, child) / observations(parent));
                prods.add(new Production(parent, child, probability, false, vocabulary, lexicon));
            }
        }

        return prods;
    }

    public ArrayList<Production> lexicalProductions() {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!lexicalRuleCounts.containsKey(parent)) {
                continue;
            }

            final Int2FloatOpenHashMap childMap = lexicalRuleCounts.get(parent);

            for (int child = 0; child < lexicon.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                final float probability = (float) Math.log(lexicalRuleObservations(parent, child)
                        / observations(parent));
                prods.add(new Production(parent, child, probability, true, vocabulary, lexicon));
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

        return binaryRuleObservations((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(leftChild),
                (short) vocabulary.getIndex(rightChild));
    }

    /**
     * Returns the number of observations of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return the number of observations of a binary rule.
     */
    public final float binaryRuleObservations(final short parent, final short leftChild, final short rightChild) {

        final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);
        if (leftChildMap == null) {
            return 0;
        }

        final Short2FloatOpenHashMap rightChildMap = leftChildMap.get(leftChild);
        if (rightChildMap == null) {
            return 0;
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
        return unaryRuleObservations((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(child));
    }

    /**
     * Returns the number of observations of a unary rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a unary rule.
     */
    public final float unaryRuleObservations(final short parent, final short child) {

        final Short2FloatOpenHashMap childMap = unaryRuleCounts.get(parent);
        if (childMap == null) {
            return 0;
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
        return lexicalRuleObservations((short) vocabulary.getIndex(parent), lexicon.getIndex(child));
    }

    /**
     * Returns the number of observations of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a lexical rule.
     */
    public final float lexicalRuleObservations(final short parent, final int child) {

        final Int2FloatOpenHashMap childMap = lexicalRuleCounts.get(parent);
        if (childMap == null) {
            return 0;
        }

        return childMap.get(child);
    }

    public final int totalRules() {
        return binaryRules() + unaryRules() + lexicalRules();
    }

    public int binaryRules() {
        return binaryRules;
    }

    public int unaryRules() {
        return unaryRules;
    }

    public int lexicalRules() {
        return lexicalRules;
    }

    public final float observations(final String parent) {
        return observations((short) vocabulary.getIndex(parent));
    }

    public final float observations(final short parent) {
        float count = 0;

        if (binaryRuleCounts.containsKey(parent)) {
            count += binaryParentCounts.get(parent);
        }

        if (unaryRuleCounts.containsKey(parent)) {
            count += unaryParentCounts.get(parent);
        }

        if (lexicalRuleCounts.containsKey(parent)) {
            count += lexicalParentCounts.get(parent);
        }

        return count;
    }
}
