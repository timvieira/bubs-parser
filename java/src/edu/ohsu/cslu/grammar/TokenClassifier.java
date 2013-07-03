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
package edu.ohsu.cslu.grammar;

import java.io.Serializable;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;

public abstract class TokenClassifier implements Serializable {

    private static final long serialVersionUID = 1L;

    protected SymbolSet<String> lexicon;

    protected TokenClassifier(final SymbolSet<String> lexicon) {
        this.lexicon = lexicon;
    }

    /**
     * Splits the supplied sentence on spaces and returns the lexicon-mapped indices of all words.
     * 
     * @param sentence
     * @return the lexicon-mapped indices of all words
     */
    public abstract int[] lexiconIndices(final String sentence);

    /**
     * Returns the lexicon-mapped indices of all words in the supplied parse tree
     * 
     * @param goldTree
     * @return the lexicon-mapped indices of all words in the supplied parse tree
     */
    public abstract int[] lexiconIndices(final NaryTree<String> goldTree);

    public abstract TokenClassifierType type();

    // public int lexiconIndex(final String word, final boolean sentenceInitial) {
    // return lexicon.getIndex(lexiconEntry(word, sentenceInitial));
    // }
    //
    // public String lexiconEntry(final String word, final boolean sentenceInitial) {
    // if (lexicon.containsKey(word)) {
    // return word;
    // }
    // String unkStr = DecisionTreeTokenClassifier.berkeleyGetSignature(word, sentenceInitial, lexicon);
    //
    // // remove last feature from unk string until we find a matching entry in the lexicon
    // while (!lexicon.containsKey(unkStr) && unkStr.contains("-")) {
    // unkStr = unkStr.substring(0, unkStr.lastIndexOf('-'));
    // }
    //
    // if (lexicon.containsKey(unkStr) == false) {
    // throw new IllegalArgumentException("Word 'UNK' not found in lexicon");
    // }
    //
    // return unkStr;
    // }

    public static enum TokenClassifierType {
        DecisionTree(DecisionTreeTokenClassifier.class), ClusterTagger(ClusterTaggerTokenClassifier.class);

        private Class<? extends TokenClassifier> implementationClass;

        private TokenClassifierType(final Class<? extends TokenClassifier> c) {
            this.implementationClass = c;
        }

        public TokenClassifier create(final SymbolSet<String> lexicon) {
            try {
                return implementationClass.getConstructor(SymbolSet.class).newInstance(lexicon);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
