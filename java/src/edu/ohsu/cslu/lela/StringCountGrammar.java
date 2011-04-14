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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree.Factorization;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;

/**
 * Grammar computed from observation counts in a training corpus. Generally used for initial induction of a
 * Markov-0 grammar from a treebank.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class StringCountGrammar implements CountGrammar {

    /**
     * Contains occurrence counts for each non-terminal which occurs as a binary parent. When representing the
     * grammar for inside-outside re-estimation, we may be able to save space by not creating certain data
     * structures for non-terminals which don't occur as binary parents. And we may be able to save execution
     * time by sorting other data structures according to frequency counts.
     */
    final Object2IntMap<String> binaryParentCounts = new Object2IntOpenHashMap<String>();

    /** Occurrence counts for each non-terminal which occurs as a unary parent. */
    final Object2IntMap<String> unaryParentCounts = new Object2IntOpenHashMap<String>();

    /** Occurrence counts for each non-terminal which occurs as a lexical parent. */
    final Object2IntMap<String> lexicalParentCounts = new Object2IntOpenHashMap<String>();

    private final HashMap<String, HashMap<String, Object2IntMap<String>>> binaryRuleCounts = new HashMap<String, HashMap<String, Object2IntMap<String>>>();
    private final HashMap<String, Object2IntMap<String>> unaryRuleCounts = new HashMap<String, Object2IntMap<String>>();
    private final HashMap<String, Object2IntMap<String>> lexicalRuleCounts = new HashMap<String, Object2IntMap<String>>();

    private int binaryRules;
    private int unaryRules;
    private int lexicalRules;

    String startSymbol;

    private final LinkedHashSet<String> observedNonTerminals = new LinkedHashSet<String>();
    private final Object2IntOpenHashMap<String> lexicalEntryOccurrences = new Object2IntOpenHashMap<String>();

    /**
     * Induces a grammar from a treebank, formatted in standard Penn-Treebank format, one bracketed sentence
     * per line.
     * 
     * The non-terminal and terminal vocabularies induced (V and T) will be mapped in the order of observation
     * in the original treebank.
     * 
     * @param reader
     * @param factorization
     *            Factorization direction. If null, the tree is assumed to be already binarized.
     * @param grammarFormatType
     *            Grammar format used in factorization. If null, the tree is assumed to be already binarized.
     * @param lexicalUnkThreshold
     *            The number of occurrences of a word which must be observed in order to add it to the
     *            lexicon. Words observed less than this threshold are instead mapped to UNK- tokens.
     * @throws IOException
     */
    public StringCountGrammar(final Reader reader, final Factorization factorization,
            final GrammarFormatType grammarFormatType, final int lexicalUnkThreshold) throws IOException {

        // Temporary string-based maps recording counts of binary, unary, and lexical rules. We will transfer
        // these
        // counts to more compact index-mapped maps after collapsing unknown words in the lexicon

        final BufferedReader br = new BufferedReader(reader);

        // Add counts for each rule to temporary String maps
        for (String line = br.readLine(); line != null; line = br.readLine()) {

            final BinaryTree<String> tree = factorization != null ? NaryTree.read(line, String.class).factor(
                grammarFormatType, factorization) : BinaryTree.read(line, String.class);

            if (startSymbol == null) {
                startSymbol = tree.label();
                observedNonTerminals.add(startSymbol);
            }

            for (final Iterator<BinaryTree<String>> i = tree.inOrderIterator(); i.hasNext();) {
                final BinaryTree<String> node = i.next();

                // Skip leaf nodes - only internal nodes are parents
                if (node.isLeaf()) {
                    continue;
                }

                final String parent = node.label().intern();
                observedNonTerminals.add(parent);
                final String leftChild = node.leftChild().label().intern();

                if (node.rightChild() != null) {
                    // Binary rule
                    final String rightChild = node.rightChild().label().intern();
                    observedNonTerminals.add(leftChild);
                    observedNonTerminals.add(rightChild);
                    incrementBinaryCount(parent, leftChild, rightChild);

                } else {
                    if (node.leftChild().isLeaf()) {
                        // Lexical rule
                        incrementLexicalCount(parent, leftChild);
                        lexicalEntryOccurrences.put(leftChild, lexicalEntryOccurrences.getInt(leftChild) + 1);
                    } else {
                        // Unary rule
                        incrementUnaryCount(parent, leftChild);
                        observedNonTerminals.add(leftChild);
                    }
                }
            }
        }

        // Iterate through lexical counts and collapse any terminals which occur < threshold into UNK
        // categories
        for (final String word : lexicalEntryOccurrences.keySet()) {
            final int occurrences = lexicalEntryOccurrences.getInt(word);
            if (occurrences < lexicalUnkThreshold) {
                // TODO For the moment, we don't treat sentence-initial words any differently; we should
                // probably handle
                // those separately while reading the corpus
                final String unk = Tokenizer.berkeleyGetSignature(word, 1, null);
                lexicalEntryOccurrences.put(unk, lexicalEntryOccurrences.getInt(unk) + occurrences);
                lexicalEntryOccurrences.remove(word);
            }
        }
    }

    private void incrementBinaryCount(final String parent, final String leftChild, final String rightChild) {

        binaryParentCounts.put(parent, binaryParentCounts.getInt(parent) + 1);

        HashMap<String, Object2IntMap<String>> leftChildMap = binaryRuleCounts.get(parent);
        if (leftChildMap == null) {
            leftChildMap = new HashMap<String, Object2IntMap<String>>();
            binaryRuleCounts.put(parent, leftChildMap);
        }

        Object2IntMap<String> rightChildMap = leftChildMap.get(leftChild);
        if (rightChildMap == null) {
            rightChildMap = new Object2IntOpenHashMap<String>();
            leftChildMap.put(leftChild, rightChildMap);
        }

        if (!rightChildMap.containsKey(rightChild)) {
            binaryRules++;
        }

        rightChildMap.put(rightChild, rightChildMap.getInt(rightChild) + 1);
    }

    private void incrementUnaryCount(final String parent, final String child) {

        unaryParentCounts.put(parent, unaryParentCounts.getInt(parent) + 1);

        Object2IntMap<String> childMap = unaryRuleCounts.get(parent);
        if (childMap == null) {
            childMap = new Object2IntOpenHashMap<String>();
            unaryRuleCounts.put(parent, childMap);
        }

        if (!childMap.containsKey(child)) {
            unaryRules++;
        }

        childMap.put(child, childMap.getInt(child) + 1);
    }

    private void incrementLexicalCount(final String parent, final String child) {

        lexicalParentCounts.put(parent, lexicalParentCounts.getInt(parent) + 1);

        Object2IntMap<String> childMap = lexicalRuleCounts.get(parent);
        if (childMap == null) {
            childMap = new Object2IntOpenHashMap<String>();
            lexicalRuleCounts.put(parent, childMap);
        }

        if (!childMap.containsKey(child)) {
            lexicalRules++;
        }

        childMap.put(child, childMap.getInt(child) + 1);
    }

    @Override
    public final float binaryRuleObservations(final String parent, final String leftChild,
            final String rightChild) {

        final HashMap<String, Object2IntMap<String>> leftChildMap = binaryRuleCounts.get(parent);
        if (leftChildMap == null) {
            return 0;
        }

        final Object2IntMap<String> rightChildMap = leftChildMap.get(leftChild);
        if (rightChildMap == null) {
            return 0;
        }

        return rightChildMap.getInt(rightChild);
    }

    @Override
    public final float unaryRuleObservations(final String parent, final String child) {

        final Object2IntMap<String> childMap = unaryRuleCounts.get(parent);
        if (childMap == null) {
            return 0;
        }

        return childMap.getInt(child);
    }

    @Override
    public final float lexicalRuleObservations(final String parent, final String child) {

        final Object2IntMap<String> childMap = lexicalRuleCounts.get(parent);
        if (childMap == null) {
            return 0;
        }

        return childMap.getInt(child);
    }

    /**
     * @param comparator
     *            Sort order for the induced vocabulary. If null, non-terminals will be ordered in the order
     *            of their observation, starting with the start symbol.
     * 
     * @return A {@link SymbolSet} induced from the observed non-terminals, sorted according to the supplied
     *         comparator.
     */
    public final SplitVocabulary induceVocabulary(final Comparator<String> comparator) {
        final ArrayList<String> nts = new ArrayList<String>(observedNonTerminals);
        if (comparator != null) {
            Collections.sort(nts, comparator);
        }

        return new SplitVocabulary(nts);
    }

    public final SymbolSet<String> induceLexicon() {
        return new SymbolSet<String>(lexicalEntryOccurrences.keySet());
    }

    public ArrayList<Production> binaryProductions(final SymbolSet<String> vocabulary) {

        final ArrayList<Production> prods = new ArrayList<Production>();

        // Iterate over mapped parents and children so the production list is in the same order as the
        // vocabulary
        for (short parent = 0; parent < vocabulary.size(); parent++) {
            final String sParent = vocabulary.getSymbol(parent);
            if (!binaryRuleCounts.containsKey(sParent)) {
                continue;
            }

            final HashMap<String, Object2IntMap<String>> leftChildMap = binaryRuleCounts.get(sParent);

            for (short leftChild = 0; leftChild < vocabulary.size(); leftChild++) {
                final String sLeftChild = vocabulary.getSymbol(leftChild);
                if (!leftChildMap.containsKey(sLeftChild)) {
                    continue;
                }

                final Object2IntMap<String> rightChildMap = leftChildMap.get(sLeftChild);

                for (short rightChild = 0; rightChild < vocabulary.size(); rightChild++) {
                    final String sRightChild = vocabulary.getSymbol(rightChild);
                    if (!rightChildMap.containsKey(sRightChild)) {
                        continue;
                    }

                    final float probability = (float) Math.log(binaryRuleObservations(sParent, sLeftChild,
                        sRightChild) * 1.0 / observations(sParent));
                    prods.add(new Production(parent, leftChild, rightChild, probability, vocabulary, null));
                }
            }
        }

        return prods;
    }

    public ArrayList<Production> unaryProductions(final SymbolSet<String> vocabulary) {

        final ArrayList<Production> prods = new ArrayList<Production>();

        // Iterate over mapped parents and children so the production list is in the same order as the
        // vocabulary
        for (short parent = 0; parent < vocabulary.size(); parent++) {
            final String sParent = vocabulary.getSymbol(parent);
            if (!unaryRuleCounts.containsKey(sParent)) {
                continue;
            }

            final Object2IntMap<String> childMap = unaryRuleCounts.get(sParent);

            for (short child = 0; child < vocabulary.size(); child++) {
                final String sChild = vocabulary.getSymbol(child);
                if (!childMap.containsKey(sChild)) {
                    continue;
                }

                final float probability = (float) Math.log(unaryRuleObservations(sParent, sChild) * 1.0
                        / observations(sParent));
                prods.add(new Production(parent, child, probability, true, vocabulary, null));
            }
        }

        return prods;
    }

    public ArrayList<Production> lexicalProductions(final SymbolSet<String> vocabulary) {
        return lexicalProductions(vocabulary, induceLexicon());
    }

    public ArrayList<Production> lexicalProductions(final SymbolSet<String> vocabulary,
            final SymbolSet<String> lexicon) {

        final ArrayList<Production> prods = new ArrayList<Production>();

        // Iterate over mapped parents and children so the production list is in the same order as the
        // vocabulary
        for (short parent = 0; parent < vocabulary.size(); parent++) {
            final String sParent = vocabulary.getSymbol(parent);
            if (!lexicalRuleCounts.containsKey(sParent)) {
                continue;
            }

            final Object2IntMap<String> childMap = lexicalRuleCounts.get(sParent);

            for (int child = 0; child < lexicon.size(); child++) {
                final String sChild = lexicon.getSymbol(child);
                if (!childMap.containsKey(sChild)) {
                    continue;
                }

                final float probability = (float) Math.log(lexicalRuleObservations(sParent, sChild) * 1.0
                        / observations(sParent));
                prods.add(new Production(parent, child, probability, true, vocabulary, lexicon));
            }
        }

        return prods;
    }

    /**
     * @return a comparator which orders Strings based on the number of times each was observed as a binary
     *         parent. The most frequent parents are ordered earlier, with the exception of the start symbol,
     *         which is always first.
     */
    public Comparator<String> binaryParentCountComparator() {

        return new Comparator<String>() {

            @Override
            public int compare(final String o1, final String o2) {
                if (o1.equals(startSymbol)) {
                    return o2.equals(startSymbol) ? 0 : -1;
                } else if (o2.equals(startSymbol)) {
                    return 1;
                }

                final int count1 = binaryParentCounts.getInt(o1);
                final int count2 = binaryParentCounts.getInt(o2);
                if (count1 > count2) {
                    return -1;
                } else if (count1 < count2) {
                    return 1;
                }
                return 0;
            }
        };
    }

    @Override
    public final int totalRules() {
        return binaryRules() + unaryRules() + lexicalRules();
    }

    @Override
    public int binaryRules() {
        return binaryRules;
    }

    @Override
    public int unaryRules() {
        return unaryRules;
    }

    @Override
    public int lexicalRules() {
        return lexicalRules;
    }

    @Override
    public final float observations(final String parent) {
        int count = 0;

        if (binaryRuleCounts.containsKey(parent)) {
            count += binaryParentCounts.getInt(parent);
        }

        if (unaryRuleCounts.containsKey(parent)) {
            count += unaryParentCounts.getInt(parent);
        }

        if (lexicalRuleCounts.containsKey(parent)) {
            count += lexicalParentCounts.getInt(parent);
        }

        return count;
    }
}
