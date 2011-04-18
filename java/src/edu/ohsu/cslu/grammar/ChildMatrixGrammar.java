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

public class ChildMatrixGrammar extends Grammar {

    private static final long serialVersionUID = 1L;

    public LinkedList<Production>[][] binaryProdMatrix;

    public ChildMatrixGrammar(final Reader grammarFile) throws IOException {
        super(grammarFile);
        init();
    }

    public ChildMatrixGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    public ChildMatrixGrammar(final Grammar g) {
        super(g);
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        binaryProdMatrix = new LinkedList[this.numNonTerms()][this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdMatrix[p.leftChild][p.rightChild] == null) {
                binaryProdMatrix[p.leftChild][p.rightChild] = new LinkedList<Production>();
            }
            binaryProdMatrix[p.leftChild][p.rightChild].add(p);
        }
    }

}
