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

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;

/**
 * Represents a sequence of (possibly-tagged) tokens.
 */
public class TagSequence extends Sequence {

    protected final SymbolSet<String> tagSet;
    final short[] tags;
    final short[] predictedTags;

    /**
     * Constructs from a bracketed string (e.g. (DT The) (NN fish) ... or from a space-delimited (untagged) string.
     * 
     * @param sentence
     * @param tagger
     */
    public TagSequence(final String sentence, final Tagger tagger) {
        this(sentence, tagger.lexicon, tagger.unkClassSet, tagger.tagSet);
    }

    /**
     * Constructs from a bracketed string (e.g. (DT The) (NN fish) ... or from a space-delimited (untagged) string.
     * 
     * @param sentence
     * @param lexicon
     * @param unkClassSet
     * @param tagSet
     */
    public TagSequence(final String sentence, final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet,
            final SymbolSet<String> tagSet) {

        super(lexicon, unkClassSet);

        this.tagSet = tagSet;

        // If the sentence starts with '(', first try parsing it as a tree
        if (sentence.charAt(0) == '(') {
            NaryTree<String> tree = null;
            try {
                tree = NaryTree.read(sentence, String.class);
            } catch (final IllegalArgumentException e) {
            }

            // Awkward control flow, to convince the compiler that we're only initializing final fields once
            if (tree != null) {
                this.length = tree.leaves();
                this.mappedTokens = new int[length];
                this.mappedUnkSymbols = new int[length];
                this.tags = new short[length];
                this.predictedTags = new short[length];

                int position = 0;
                for (final NaryTree<String> leaf : tree.leafTraversal()) {
                    mapTagAndToken(position, leaf.parentLabel(), leaf.label());
                    position++;
                }

            } else {
                // Tree parsing failed; treat it as a seies of bracketed tag/word pairs. E.g. (DT The) (NN fish)...
                final String[] split = sentence.replaceAll(" ?\\(", "").split("\\)");
                this.length = split.length;
                this.mappedTokens = new int[length];
                this.mappedUnkSymbols = new int[length];
                this.tags = new short[length];
                this.predictedTags = new short[length];

                for (int i = 0; i < split.length; i++) {
                    final String[] tokenAndPos = split[i].split(" ");
                    mapTagAndToken(i, tokenAndPos[0], tokenAndPos[1]);
                }
            }

        } else {
            // It didn't start with '('; assume it is untagged
            final String[] split = sentence.split(" ");
            final String[] tokens = new String[split.length];
            this.mappedTokens = new int[split.length];
            this.mappedUnkSymbols = new int[split.length];
            this.tags = new short[split.length];
            this.predictedTags = new short[split.length];
            Arrays.fill(tags, (short) -1);
            this.length = split.length;

            for (int i = 0; i < split.length; i++) {
                tokens[i] = split[i];
                if (lexicon.isFinalized()) {
                    mappedTokens[i] = lexicon.getIndex(split[i]);
                    mappedUnkSymbols[i] = unkClassSet.getIndex(Tokenizer.berkeleyGetSignature(tokens[i], i == 0,
                            lexicon));
                } else {
                    mappedTokens[i] = lexicon.addSymbol(split[i]);
                    mappedUnkSymbols[i] = unkClassSet.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i], i == 0,
                            lexicon));
                }
            }
        }
    }

    void mapTagAndToken(final int position, final String tag, final String token) {
        if (tagSet.isFinalized()) {
            tags[position] = (short) tagSet.getIndex(tag);
        } else {
            tags[position] = (short) tagSet.addSymbol(tag);
        }
        predictedTags[position] = tags[position];

        if (lexicon.isFinalized()) {
            mappedTokens[position] = lexicon.getIndex(token);
            mappedUnkSymbols[position] = unkClassSet.getIndex(Tokenizer.berkeleyGetSignature(token, position == 0,
                    lexicon));
        } else {
            mappedTokens[position] = lexicon.addSymbol(token);
            mappedUnkSymbols[position] = unkClassSet.addSymbol(Tokenizer.berkeleyGetSignature(token, position == 0,
                    lexicon));
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        for (int i = 0; i < length; i++) {
            sb.append('(');
            sb.append(tagSet.getSymbol(tags[i]));
            sb.append(' ');
            sb.append(lexicon.getSymbol(mappedTokens[i]));
            sb.append(')');

            if (i < (length - 1)) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}