package edu.ohsu.cslu.ella;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.util.ArrayList;

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

    protected SymbolSet<String> vocabulary;
    protected SymbolSet<String> lexicon;

    /**
     * Contains occurrence counts for each non-terminal which occurs as a binary parent. When representing the grammar
     * for inside-outside re-estimation, we may be able to save space by not creating certain data structures for
     * non-terminals which don't occur as binary parents. And we may be able to save execution time by sorting other
     * data structures according to frequency counts.
     */
    final Short2IntMap binaryParentCounts = new Short2IntOpenHashMap();

    /** Occurrence counts for each non-terminal which occurs as a unary parent. */
    final Short2IntMap unaryParentCounts = new Short2IntOpenHashMap();

    /** Occurrence counts for each non-terminal which occurs as a lexical parent. */
    final Short2IntMap lexicalParentCounts = new Short2IntOpenHashMap();

    private Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2IntOpenHashMap>> binaryRuleCounts = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2IntOpenHashMap>>();
    private Short2ObjectOpenHashMap<Short2IntOpenHashMap> unaryRuleCounts = new Short2ObjectOpenHashMap<Short2IntOpenHashMap>();
    private Short2ObjectOpenHashMap<Int2IntOpenHashMap> lexicalRuleCounts = new Short2ObjectOpenHashMap<Int2IntOpenHashMap>();

    private int binaryRules;
    private int unaryRules;
    private int lexicalRules;

    String startSymbol;

    public MappedCountGrammar(final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon) {
        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
        this.startSymbol = vocabulary.getSymbol(0);
    }

    public void incrementBinaryCount(final short parent, final short leftChild, final short rightChild) {

        binaryParentCounts.put(parent, binaryParentCounts.get(parent) + 1);
        Short2ObjectOpenHashMap<Short2IntOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);
        if (leftChildMap == null) {
            leftChildMap = new Short2ObjectOpenHashMap<Short2IntOpenHashMap>();
            binaryRuleCounts.put(parent, leftChildMap);
        }

        Short2IntOpenHashMap rightChildMap1 = leftChildMap.get(leftChild);
        if (rightChildMap1 == null) {
            rightChildMap1 = new Short2IntOpenHashMap();
            leftChildMap.put(leftChild, rightChildMap1);
        }

        final Short2IntOpenHashMap rightChildMap = rightChildMap1;
        if (rightChildMap.containsKey(rightChild)) {
            rightChildMap.put(rightChild, rightChildMap.get(rightChild) + 1);
        } else {
            rightChildMap.put(rightChild, 1);
            binaryRules++;
        }
    }

    public void incrementBinaryCount(final String parent, final String leftChild, final String rightChild) {
        incrementBinaryCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(leftChild),
                (short) vocabulary.getIndex(rightChild));
    }

    public void incrementUnaryCount(final short parent, final short child) {

        unaryParentCounts.put(parent, unaryParentCounts.get(parent) + 1);
        Short2IntOpenHashMap childMap1 = unaryRuleCounts.get(parent);
        if (childMap1 == null) {
            childMap1 = new Short2IntOpenHashMap();
            unaryRuleCounts.put(parent, childMap1);
        }

        final Short2IntOpenHashMap childMap = childMap1;

        if (childMap.containsKey(child)) {
            childMap.put(child, childMap.get(child) + 1);
        } else {
            childMap.put(child, 1);
            unaryRules++;
        }
    }

    public void incrementUnaryCount(final String parent, final String child) {
        incrementUnaryCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(child));
    }

    public void incrementLexicalCount(final short parent, final int child) {

        lexicalParentCounts.put(parent, lexicalParentCounts.get(parent) + 1);
        Int2IntOpenHashMap childMap = lexicalRuleCounts.get(parent);
        if (childMap == null) {
            childMap = new Int2IntOpenHashMap();
            lexicalRuleCounts.put(parent, childMap);
        }

        if (childMap.containsKey(child)) {
            childMap.put(child, childMap.get(child) + 1);
        } else {
            childMap.put(child, 1);
            lexicalRules++;
        }
    }

    public void incrementLexicalCount(final String parent, final String child) {
        incrementLexicalCount((short) vocabulary.getIndex(parent), lexicon.getIndex(child));
    }

    public ArrayList<Production> binaryProductions() {

        final ArrayList<Production> prods = new ArrayList<Production>();

        for (short parent = 0; parent < vocabulary.size(); parent++) {
            if (!binaryRuleCounts.containsKey(parent)) {
                continue;
            }

            final Short2ObjectOpenHashMap<Short2IntOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);

            for (short leftChild = 0; leftChild < vocabulary.size(); leftChild++) {
                if (!leftChildMap.containsKey(leftChild)) {
                    continue;
                }

                final Short2IntOpenHashMap rightChildMap = leftChildMap.get(leftChild);

                for (short rightChild = 0; rightChild < vocabulary.size(); rightChild++) {
                    if (!rightChildMap.containsKey(rightChild)) {
                        continue;
                    }

                    final float probability = (float) Math.log(binaryRuleObservations(parent, leftChild, rightChild)
                            * 1.0 / observations(parent));
                    prods.add(new Production(parent, leftChild, rightChild, probability));
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

            final Short2IntOpenHashMap childMap = unaryRuleCounts.get(parent);

            for (short child = 0; child < vocabulary.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                final float probability = (float) Math.log(unaryRuleObservations(parent, child) * 1.0
                        / observations(parent));
                prods.add(new Production(parent, child, false, probability));
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

            final Int2IntOpenHashMap childMap = lexicalRuleCounts.get(parent);

            for (int child = 0; child < lexicon.size(); child++) {
                if (!childMap.containsKey(child)) {
                    continue;
                }

                final float probability = (float) Math.log(lexicalRuleObservations(parent, child) * 1.0
                        / observations(parent));
                prods.add(new Production(parent, child, true, probability));
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
    public final int binaryRuleObservations(final String parent, final String leftChild, final String rightChild) {

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
    public final int binaryRuleObservations(final short parent, final short leftChild, final short rightChild) {

        final Short2ObjectOpenHashMap<Short2IntOpenHashMap> leftChildMap = binaryRuleCounts.get(parent);
        if (leftChildMap == null) {
            return 0;
        }

        final Short2IntOpenHashMap rightChildMap = leftChildMap.get(leftChild);
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
    public final int unaryRuleObservations(final String parent, final String child) {
        return unaryRuleObservations((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(child));
    }

    /**
     * Returns the number of observations of a unary rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a unary rule.
     */
    public final int unaryRuleObservations(final short parent, final short child) {

        final Short2IntOpenHashMap childMap = unaryRuleCounts.get(parent);
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
    public final int lexicalRuleObservations(final String parent, final String child) {
        return lexicalRuleObservations((short) vocabulary.getIndex(parent), lexicon.getIndex(child));
    }

    /**
     * Returns the number of observations of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a lexical rule.
     */
    public final int lexicalRuleObservations(final short parent, final int child) {

        final Int2IntOpenHashMap childMap = lexicalRuleCounts.get(parent);
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

    public final int observations(final String parent) {
        return observations((short) vocabulary.getIndex(parent));
    }

    public final int observations(final short parent) {
        int count = 0;

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
