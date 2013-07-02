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
 * Represents a sequence of tokens and the information needed to assign any unknown tokens to unknown-word classes
 * 
 * @author Aaron Dunlop
 */
public class UnkClassSequence extends TagSequence {

    public UnkClassSequence(final String sentence, final SymbolSet<String> lexicon,
            final SymbolSet<String> unkClassSet, final SymbolSet<String> tagSet,
            final SymbolSet<String> unigramSuffixSet, final SymbolSet<String> bigramSuffixSet) {
        super(sentence, lexicon, unkClassSet, tagSet, unigramSuffixSet, bigramSuffixSet);
        // TODO Auto-generated constructor stub
    }

    public UnkClassSequence(final String sentence, final Tagger tagger) {
        super(sentence, tagger);
        // TODO Auto-generated constructor stub
    }

}
