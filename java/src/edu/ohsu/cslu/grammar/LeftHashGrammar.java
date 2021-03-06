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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class LeftHashGrammar extends ListGrammar {

    private static final long serialVersionUID = 1L;

    private ArrayList<HashMap<Integer, LinkedList<Production>>> binaryProdHash;

    public LeftHashGrammar(final Reader grammarFile, final TokenClassifier tokenClassifier) throws IOException {
        super(grammarFile, tokenClassifier);
        init();
    }

    public LeftHashGrammar(final Grammar g) {
        super(g);
        init();
    }

    private void init() {
        binaryProdHash = new ArrayList<HashMap<Integer, LinkedList<Production>>>(this.numNonTerms());
        for (int i = 0; i < this.numNonTerms(); i++) {
            binaryProdHash.add(i, null);
        }

        // add productions to array (left child) of hash maps (right child) which returns
        // a list of valid productions
        for (final Production p : this.binaryProductions) {
            if (binaryProdHash.get(p.leftChild) == null) {
                binaryProdHash.set(p.leftChild, new HashMap<Integer, LinkedList<Production>>());
            }

            if (binaryProdHash.get(p.leftChild).containsKey(p.rightChild) == false) {
                binaryProdHash.get(p.leftChild).put(p.rightChild, new LinkedList<Production>());
            }

            binaryProdHash.get(p.leftChild).get(p.rightChild).add(p);
        }
    }

    public Collection<Production> getBinaryProductionsWithChildren(final int leftChild, final int rightChild) {
        final HashMap<Integer, LinkedList<Production>> leftChildHash = binaryProdHash.get(leftChild);
        if (leftChildHash == null) {
            return new LinkedList<Production>();
        }
        final LinkedList<Production> productions = leftChildHash.get(rightChild);
        if (productions == null) {
            return new LinkedList<Production>();
        }
        return productions;
    }

    @Override
    public Production getBinaryProduction(final String A, final String B, final String C) {
        if (nonTermSet.containsKey(A) && nonTermSet.containsKey(B) && nonTermSet.containsKey(C)) {
            final int parent = nonTermSet.getIndex(A);
            final int leftChild = nonTermSet.getIndex(B);
            final int rightChild = nonTermSet.getIndex(C);
            // System.out.println("A=" + parent + "(" + A + ") B=" + leftChild + "(" + B + ") C=" + rightChild
            // + "(" + C + ")");
            for (final Production p : getBinaryProductionsWithChildren(leftChild, rightChild)) {
                if (p.parent == parent) {
                    return p;
                }
            }
        }
        return null;
    }
}
