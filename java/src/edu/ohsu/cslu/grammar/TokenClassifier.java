/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.grammar;

import java.io.Serializable;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.util.MutableEnumeration;

public abstract class TokenClassifier implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Returns <code>token</code> if present in the lexicon, or the appropriate unknown-word class if <code>token</code>
     * is unknown.
     * 
     * Deprecated because it cannot incorporate surrounding context into the tagging decision - use
     * {@link #lexiconIndices(String, MutableEnumeration)} instead.
     * 
     * @param token
     * @param sentenceInitial True if the token is the first word in the sentence (some {@link TokenClassifier}
     *            implementations treat the first word differently)
     * @param lexicon
     * @return the lexicon-mapped indices of <code>token</code>
     */
    @Deprecated
    public abstract String lexiconEntry(final String token, final boolean sentenceInitial,
            final MutableEnumeration<String> lexicon);

    /**
     * Returns the lexicon-mapped index of <code>token</code>, or of the appropriate unknown-word class if
     * <code>token</code> is not present in the <code>lexicon</code>.
     * 
     * Deprecated because it cannot incorporate surrounding context into the tagging decision - use
     * {@link #lexiconIndices(String, MutableEnumeration)} instead.
     * 
     * @param token
     * @param sentenceInitial True if the token is the first word in the sentence (some {@link TokenClassifier}
     *            implementations treat the first word differently)
     * @param lexicon
     * @return the lexicon-mapped indices of <code>token</code>
     */
    @Deprecated
    public abstract int lexiconIndex(final String token, final boolean sentenceInitial, final MutableEnumeration<String> lexicon);

    /**
     * Splits the supplied sentence on spaces and returns the lexicon-mapped indices of all words. Convenience method
     * that calls {@link #lexiconIndex(String, boolean, MutableEnumeration)} to obtain mapped indices.
     * 
     * @param sentence
     * @param lexicon
     * @return the lexicon-mapped indices of all words
     */
    public int[] lexiconIndices(final String sentence, final MutableEnumeration<String> lexicon) {
        // TODO This could probably be done faster with something other than a regex
        final String tokens[] = sentence.split("\\s+");
        final int tokenIndices[] = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            tokenIndices[i] = lexiconIndex(tokens[i], i == 0, lexicon);
        }
        return tokenIndices;
    }

    /**
     * Returns the lexicon-mapped indices of all words in the supplied parse tree
     * 
     * @param goldTree
     * @param lexicon
     * @return the lexicon-mapped indices of all words in the supplied parse tree
     */
    public int[] lexiconIndices(final NaryTree<String> goldTree, final MutableEnumeration<String> lexicon) {
        final int tokenIndices[] = new int[goldTree.leaves()];
        int i = 0;
        for (final NaryTree<String> leaf : goldTree.leafTraversal()) {
            tokenIndices[i++] = lexiconIndex(leaf.label(), i == 0, lexicon);
        }
        return tokenIndices;
    }
}
