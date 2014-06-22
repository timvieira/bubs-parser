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

package edu.ohsu.cslu.perceptron;

import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * Represents an ordered sequence of tokens, usually (but not necessarily) a sentence. Intended for use by linear
 * classifiers such as {@link Tagger} and {@link CompleteClosureClassifier}.
 * 
 * @author Aaron Dunlop
 * @since Feb 8, 2013
 */
public class BaseSequence implements Sequence {

    /** Raw tokens from the sequence */
    String[] tokens;

    //
    // Note: these fields should be initialized in the constructor and are semantically final, but we can't mark them as
    // such, since they're initialized in subclass constructors
    //
    /** Integer representation of each token in the sequence (as mapped by {@link #lexicon}). */
    int[] mappedTokens;

    /**
     * Integer representation of the unknown-word class for each token in the sequence (as mapped by
     * {@link #unkClassSet}).
     */
    int[] mappedUnkSymbols;

    /**
     * Integer representation of the part-of-speech for each token in the sequence (as mapped by {@link #posSet}).
     */
    short[] mappedPosSymbols;

    /**
     * Integer representation of the unigram suffix for each token in the sequence (as mapped by
     * {@link #unigramSuffixSet}).
     */
    int[] mappedUnigramSuffix;

    /**
     * Integer representation of the bigram suffix for each token in the sequence (as mapped by {@link #bigramSuffixSet}
     * ).
     */
    int[] mappedBigramSuffix;

    /**
     * The number of individual classifications possible for this sequence. Note that this may be larger than the number
     * of tokens - we may classify chart cells (as in {@link CompleteClosureSequence}) or other entities derived from
     * the actual sequence.
     */
    int length;

    /** Maps words to integer indices and vice-versa */
    protected final MutableEnumeration<String> lexicon;

    /**
     * A separate (smaller) {@link MutableEnumeration} containing only unknown-word classes. The lexicon usually contains these
     * as well, but we can limit the feature-set size by treating them separately for classification purposes.
     */
    protected final MutableEnumeration<String> unkClassSet;

    /**
     * {@link MutableEnumeration} containing part-of-speech classes.
     */
    protected final MutableEnumeration<String> posSet;

    /**
     * {@link MutableEnumeration} containing unigram suffixes.
     */
    protected final MutableEnumeration<String> unigramSuffixSet;

    /**
     * {@link MutableEnumeration} containing bigram suffixes.
     */
    protected final MutableEnumeration<String> bigramSuffixSet;

    /**
     * Constructor for a training or test sequence. Used by subclass constructors.
     * 
     * @param lexicon
     * @param unkClassSet
     * @param posSet
     * @param unigramSuffixSet
     * @param bigramSuffixSet
     */
    protected BaseSequence(final MutableEnumeration<String> lexicon, final MutableEnumeration<String> unkClassSet,
            final MutableEnumeration<String> posSet, final MutableEnumeration<String> unigramSuffixSet,
            final MutableEnumeration<String> bigramSuffixSet) {

        this.lexicon = lexicon;
        this.unkClassSet = unkClassSet;
        this.posSet = posSet;
        this.unigramSuffixSet = unigramSuffixSet;
        this.bigramSuffixSet = bigramSuffixSet;
    }

    /**
     * Constructor for a training or test sequence. Used by subclass constructors.
     * 
     * @param lexicon
     * @param unkClassSet
     */
    protected BaseSequence(final MutableEnumeration<String> lexicon, final MutableEnumeration<String> unkClassSet) {
        this(lexicon, unkClassSet, null, null, null);
    }

    public int length() {
        return length;
    }

    @Override
    public void allocatePredictionStorage() {
    }

    @Override
    public void clearPredictionStorage() {
    }
}
