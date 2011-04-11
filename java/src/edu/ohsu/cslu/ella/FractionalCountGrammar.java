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
package edu.ohsu.cslu.ella;

import java.util.ArrayList;

import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;

public abstract class FractionalCountGrammar implements CountGrammar {

    protected final SplitVocabulary vocabulary;
    protected final SymbolSet<String> lexicon;
    protected final String startSymbol;

    public FractionalCountGrammar(final SplitVocabulary vocabulary, final SymbolSet<String> lexicon) {
        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
        this.startSymbol = vocabulary.getSymbol(0);
    }

    public abstract void incrementBinaryCount(final short parent, final short leftChild,
            final short rightChild, final float increment);

    public abstract void incrementUnaryCount(final short parent, final short child, final float increment);

    public abstract void incrementLexicalCount(final short parent, final int child, final float increment);

    public final void incrementBinaryCount(final short parent, final short leftChild, final short rightChild) {
        incrementBinaryCount(parent, leftChild, rightChild, 1f);
    }

    public abstract ArrayList<Production> binaryProductions();

    public abstract ArrayList<Production> unaryProductions();

    public abstract ArrayList<Production> lexicalProductions();

    public final void incrementBinaryCount(final String parent, final String leftChild,
            final String rightChild) {
        incrementBinaryCount(parent, leftChild, rightChild, 1f);
    }

    public final void incrementBinaryCount(final String parent, final String leftChild,
            final String rightChild, final float increment) {
        incrementBinaryCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(leftChild),
            (short) vocabulary.getIndex(rightChild), increment);
    }

    public final void incrementUnaryCount(final short parent, final short child) {
        incrementUnaryCount(parent, child, 1f);
    }

    public final void incrementUnaryCount(final String parent, final String child) {
        incrementUnaryCount(parent, child, 1f);
    }

    public final void incrementUnaryCount(final String parent, final String child, final float increment) {
        incrementUnaryCount((short) vocabulary.getIndex(parent), (short) vocabulary.getIndex(child),
            increment);
    }

    public final void incrementLexicalCount(final short parent, final int child) {
        incrementLexicalCount(parent, child, 1f);
    }

    public final void incrementLexicalCount(final String parent, final String child) {
        incrementLexicalCount(parent, child, 1f);
    }

    public final void incrementLexicalCount(final String parent, final String child, final float increment) {
        incrementLexicalCount((short) vocabulary.getIndex(parent), lexicon.getIndex(child), increment);
    }
}
