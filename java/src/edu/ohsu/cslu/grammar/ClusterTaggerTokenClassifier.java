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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.perceptron.UnkClassSequence;
import edu.ohsu.cslu.perceptron.UnkClassTagger;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * Classifies tokens using a sequence-tagger model, assigning each unknown word to one of a set of previously-learned
 * clusters.
 * 
 * @author Aaron Dunlop
 */
public class ClusterTaggerTokenClassifier extends TokenClassifier {

    private static final long serialVersionUID = 1L;
    private UnkClassTagger unkClassTagger;

    public ClusterTaggerTokenClassifier(final File classifierModel) throws IOException, ClassNotFoundException {
        super();
        this.unkClassTagger = new UnkClassTagger();
        unkClassTagger.readModel(new FileInputStream(classifierModel));
    }

    /**
     * Splits the supplied sentence on spaces and returns the lexicon-mapped indices of all words. Convenience method
     * that calls {@link #lexiconIndex(String, boolean, MutableEnumeration)} to obtain mapped indices.
     * 
     * @param sentence
     * @param lexicon
     * @return the lexicon-mapped indices of all words
     */
    @Override
    public int[] lexiconIndices(final String sentence, final MutableEnumeration<String> lexicon) {
        // TODO This could probably be done faster with something other than a regex
        final String tokens[] = sentence.split("\\s+");
        final int tokenIndices[] = new int[tokens.length];
        final UnkClassSequence unkClassSequence = new UnkClassSequence(sentence, unkClassTagger);
        final short[] unkClasses = unkClassTagger.classify(unkClassSequence);

        for (int i = 0; i < tokens.length; i++) {
            if (unkClasses[i] < 0) {
                tokenIndices[i] = lexicon.getIndex(tokens[i]);
            } else {
                tokenIndices[i] = lexicon.getIndex(unkClassTagger.tagSet().getSymbol(unkClasses[i]));
            }
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
    @Override
    public int[] lexiconIndices(final NaryTree<String> goldTree, final MutableEnumeration<String> lexicon) {
        final int tokenIndices[] = new int[goldTree.leaves()];
        int i = 0;
        for (final NaryTree<String> leaf : goldTree.leafTraversal()) {
            tokenIndices[i++] = lexiconIndex(leaf.label(), i == 0, lexicon);
        }
        return tokenIndices;
    }

    @Override
    public int lexiconIndex(final String token, final boolean sentenceInitial, final MutableEnumeration<String> lexicon) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String lexiconEntry(final String token, final boolean sentenceInitial, final MutableEnumeration<String> lexicon) {
        throw new UnsupportedOperationException();
    }
}
