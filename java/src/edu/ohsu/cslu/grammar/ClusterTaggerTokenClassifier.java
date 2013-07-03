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

package edu.ohsu.cslu.grammar;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * @author Aaron Dunlop
 * @since Jul 2, 2013
 */
public class ClusterTaggerTokenClassifier extends TokenClassifier {

    /**
     * @param lexicon
     */
    public ClusterTaggerTokenClassifier(final SymbolSet<String> lexicon) {
        super(lexicon);
        // TODO Auto-generated constructor stub
    }

    @Override
    public int[] lexiconIndices(final String sentence) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int[] lexiconIndices(final NaryTree<String> goldTree) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TokenClassifierType type() {
        return TokenClassifierType.ClusterTagger;
    }
}
