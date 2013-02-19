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
 * Represents a linguistic sequence and a set of binary tags. The default representation assumes one binary tag per
 * word. However, subclasses may alter that definition (e.g., by applying O(n^2) tags to chart cells for complete
 * closure or beam-width prediction).
 * 
 * @author Aaron Dunlop
 * @since Feb 8, 2013
 */
public class BinarySequence extends Sequence {

    // These fields are populated in the constructor, but possibly in a subclass constructor, so we can't label them
    // final
    // TODO We could replace these with PackedBitVectors to save a little space
    protected boolean[] classes;
    protected boolean[] predictedClasses;

    /**
     * Constructor for tagged training sequence
     * 
     * @param lexicon
     * @param unkClassSet
     */
    protected BinarySequence(final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet) {
        super(lexicon, unkClassSet);
    }

    public final boolean classification(final int i) {
        return classes[i];
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        for (int i = 0; i < length; i++) {
            sb.append(classes[i]);

            if (i < (length - 1)) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
