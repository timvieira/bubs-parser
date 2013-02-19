/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.perceptron;

import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Represents an ordered sequence of tokens, usually (but not necessarily) a sentence. Intended for use by linear
 * classifiers such as {@link Tagger} and {@link CompleteClosureClassifier}.
 * 
 * @author Aaron Dunlop
 * @since Feb 8, 2013
 */
public class Sequence {

    //
    // Note: these fields should be initialized in the constructor and are semantically final, but we can't mark them as
    // such, since they're initialized in subclass constructors
    //
    /** Integer representation of each token in the sequence (as mapped by {@link #lexicon}. */
    int[] mappedTokens;

    /**
     * Integer representation of the unknown-word class for each token in the sequence (as mapped by
     * {@link #unkClassSet}.
     */
    int[] mappedUnkSymbols;

    /**
     * The number of individual classifications possible for this sequence. Note that this may be larger than the number
     * of tokens - we may classify chart cells (as in {@link CompleteClosureSequence}) or other entities derived from
     * the actual sequence.
     */
    int length;

    /** Maps words to integer indices and vice-versa */
    protected final SymbolSet<String> lexicon;

    /**
     * A separate (smaller) {@link SymbolSet} containing only unknown-word classes. The lexicon usually contains these
     * as well, but we can limit the feature-set size by treating them separately for classification purposes.
     */
    protected final SymbolSet<String> unkClassSet;

    /**
     * Constructor for a training or test sequence. Used by subclass constructors.
     * 
     * @param lexicon
     */
    protected Sequence(final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet) {
        this.lexicon = lexicon;
        this.unkClassSet = unkClassSet;
    }
}
