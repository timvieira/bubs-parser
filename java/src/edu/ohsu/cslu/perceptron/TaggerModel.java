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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * A container object for a tagging model, including an averaged perceptron and the various symbol-mappings required to
 * map features.
 * 
 * @author Aaron Dunlop
 */
class TaggerModel implements Serializable {

    private static final long serialVersionUID = 1L;

    final String featureTemplates;
    SymbolSet<String> lexicon;
    SymbolSet<String> unkClassSet;
    SymbolSet<String> tagSet;
    AveragedPerceptron perceptronModel;

    public TaggerModel(final String featureTemplates) {
        this.lexicon = new SymbolSet<String>();
        this.lexicon.defaultReturnValue(Tagger.NULL_SYMBOL);

        this.unkClassSet = new SymbolSet<String>();
        this.unkClassSet.defaultReturnValue(Tagger.NULL_SYMBOL);

        this.tagSet = new SymbolSet<String>();
        this.tagSet.defaultReturnValue(Tagger.NULL_SYMBOL);

        this.featureTemplates = featureTemplates;
    }

    public void finalizeMaps() {
        lexicon.finalize();
        unkClassSet.finalize();
        tagSet.finalize();
    }

    /**
     * Reads in a serialized tagging model from disk, including the template string, lexicon, trained perceptron, etc.
     * Note that the {@link InputStream} is <em>not</em> closed, since the same model file may contain other serialized
     * models as well. The client should ensure that the stream is closed appropriately.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static TaggerModel read(final InputStream is) throws ClassNotFoundException, IOException {
        final TaggerModel m = (TaggerModel) new ObjectInputStream(is).readObject();
        m.perceptronModel.trim();
        return m;
    }

    public int classify(final Vector featureVector) {
        return perceptronModel.classify(featureVector);
    }
}