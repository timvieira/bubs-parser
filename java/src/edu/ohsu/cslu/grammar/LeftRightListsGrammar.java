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

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

public class LeftRightListsGrammar extends LeftListGrammar {

    private static final long serialVersionUID = 1L;

    private LinkedList<Production>[] binaryProdsByRightNonTerm;

    public LeftRightListsGrammar(final Reader grammarFile, final TokenClassifier tokenClassifier) throws IOException {
        super(grammarFile, tokenClassifier);
        init();
    }

    public LeftRightListsGrammar(final Grammar g) {
        super(g);
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        binaryProdsByLeftNonTerm = new LinkedList[this.numNonTerms()];
        binaryProdsByRightNonTerm = new LinkedList[this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdsByLeftNonTerm[p.leftChild] == null) {
                binaryProdsByLeftNonTerm[p.leftChild] = new LinkedList<Production>();
            }
            binaryProdsByLeftNonTerm[p.leftChild].add(p);

            if (binaryProdsByRightNonTerm[p.rightChild] == null) {
                binaryProdsByRightNonTerm[p.rightChild] = new LinkedList<Production>();
            }
            binaryProdsByRightNonTerm[p.rightChild].add(p);
        }
    }

    public LinkedList<Production> getBinaryProductionsWithRightChild(final int rightChild) {
        final LinkedList<Production> prodList = binaryProdsByRightNonTerm[rightChild];
        if (prodList != null) {
            return prodList;
        }
        return new LinkedList<Production>();
    }
}
