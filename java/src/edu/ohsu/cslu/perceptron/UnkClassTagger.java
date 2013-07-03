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

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Classifies unknown-words into clusters, using lexical features and the surrounding syntax. Trains on transformed
 * corpus including POS tags, tokens, and unknown-word classes for all rare words. Class/token format is
 * UNK-&lt;clusterID&gt;|&lt;token&gt;. Supports bracketed input (POS token) (POS UNK*) (POS token)... or trees.
 * 
 * @author Aaron Dunlop
 */
public class UnkClassTagger extends Tagger {

    private static final long serialVersionUID = 1L;

    SymbolSet<String> unigramSuffixSet;
    SymbolSet<String> bigramSuffixSet;

    /**
     * Default Feature Templates:
     * 
     * <pre>
     * # Contains-numeral and numeral percentage
     * num
     * num20
     * num40
     * num60
     * num80
     * num100
     * 
     * # Contains-punctuation and punctuation percentage
     * punct
     * punct20
     * punct40
     * punct60
     * punct80
     * punct100
     * 
     * # POS features
     * posm1
     * pos
     * posp1
     * 
     * # Unigram and bigram suffixes
     * us
     * bs
     * </pre>
     */
    @Override
    protected final String DEFAULT_FEATURE_TEMPLATES() {
        return "num,num20,num40,num60,num80,num100,punct,punct20,punct40,punct60,punct80,punct100,posm1,pos,posp1,us,bs";
    }

    public UnkClassTagger() {
        this.posSet = new SymbolSet<String>();
        this.posSet.defaultReturnValue(Grammar.nullSymbolStr);

        this.unigramSuffixSet = new SymbolSet<String>();
        this.unigramSuffixSet.defaultReturnValue(Grammar.nullSymbolStr);

        this.bigramSuffixSet = new SymbolSet<String>();
        this.bigramSuffixSet.defaultReturnValue(Grammar.nullSymbolStr);
    }

    @Override
    protected TagSequence createSequence(final String line) {
        return new UnkClassSequence(line, this);
    }

    @Override
    protected void finalizeMaps() {
        super.finalizeMaps();

        unigramSuffixSet.finalize();
        bigramSuffixSet.finalize();
    }

    public static void main(final String[] args) {
        run(args);
    }
}
