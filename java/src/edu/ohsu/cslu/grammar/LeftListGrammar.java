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
package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

public class LeftListGrammar extends Grammar {

    private static final long serialVersionUID = 1L;

    protected LinkedList<Production>[] binaryProdsByLeftNonTerm;

    public LeftListGrammar(final Reader grammarFile) throws IOException {
        super(grammarFile);
        init();
    }

    public LeftListGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    public LeftListGrammar(final Grammar g) {
        super(g);
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        binaryProdsByLeftNonTerm = new LinkedList[this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdsByLeftNonTerm[p.leftChild] == null) {
                binaryProdsByLeftNonTerm[p.leftChild] = new LinkedList<Production>();
            }
            binaryProdsByLeftNonTerm[p.leftChild].add(p);
        }
    }

    public LinkedList<Production> getBinaryProductionsWithLeftChild(final int leftChild) {
        final LinkedList<Production> prodList = binaryProdsByLeftNonTerm[leftChild];
        if (prodList != null) {
            return prodList;
        }
        // return an empty list instead of 'null' so we can still iterate over it
        return new LinkedList<Production>();
    }

    @Override
    public Production getBinaryProduction(final int parent, final int leftChild, final int rightChild) {
        for (final Production p : getBinaryProductionsWithLeftChild(leftChild)) {
            if (p.parent == parent && p.rightChild == rightChild) {
                return p;
            }
        }
        return null;
    }
}
