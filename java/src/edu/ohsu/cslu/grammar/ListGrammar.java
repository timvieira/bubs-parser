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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import edu.ohsu.cslu.parser.Util;

/**
 */
public class ListGrammar extends Grammar {

    private static final long serialVersionUID = 1L;

    protected final Collection<Production>[] unaryProductionsByChild;
    protected final Collection<Production>[] lexicalProdsByChild;

    /**
     * @param grammarFile
     * @throws IOException
     */
    public ListGrammar(final Reader grammarFile) throws IOException {
        super(grammarFile);
        unaryProductionsByChild = storeProductionByChild(unaryProductions, nonTermSet.size() - 1);
        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);
    }

    /**
     * @param grammarFile
     * @throws IOException
     */
    public ListGrammar(final String grammarFile) throws IOException {
        this(new InputStreamReader(Util.file2inputStream(grammarFile)));
    }

    /**
     * @param binaryProductions
     * @param unaryProductions
     * @param lexicalProductions
     * @param vocabulary
     * @param lexicon
     * @param grammarFormat
     */
    public ListGrammar(final ArrayList<Production> binaryProductions, final ArrayList<Production> unaryProductions,
            final ArrayList<Production> lexicalProductions, final SymbolSet<String> vocabulary,
            final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat) {
        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat);

        this.unaryProductionsByChild = null;
        this.lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);
    }

    /**
     * @param g
     */
    public ListGrammar(final Grammar g) {
        this(g.binaryProductions, g.unaryProductions, g.lexicalProductions, g.nonTermSet, g.lexSet, g.grammarFormat);
    }

    @Override
    public Collection<Production> getUnaryProductionsWithChild(final int child) {
        if (child > unaryProductionsByChild.length - 1 || unaryProductionsByChild[child] == null) {
            return new LinkedList<Production>();
        }
        return unaryProductionsByChild[child];
    }

    @Override
    public final Collection<Production> getLexicalProductionsWithChild(final int child) {
        if (child > lexicalProdsByChild.length - 1 || lexicalProdsByChild[child] == null) {
            return new LinkedList<Production>();
        }
        return lexicalProdsByChild[child];
    }

    @SuppressWarnings({ "cast", "unchecked" })
    private static Collection<Production>[] storeProductionByChild(final Collection<Production> prods,
            final int maxIndex) {
        final Collection<Production>[] prodsByChild = (LinkedList<Production>[]) new LinkedList[maxIndex + 1];

        for (int i = 0; i < prodsByChild.length; i++) {
            prodsByChild[i] = new LinkedList<Production>();
        }

        for (final Production p : prods) {
            prodsByChild[p.child()].add(p);
        }

        return prodsByChild;
    }
}
