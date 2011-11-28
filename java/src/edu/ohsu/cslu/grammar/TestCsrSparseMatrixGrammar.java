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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.lela.AllLelaTests;
import edu.ohsu.cslu.lela.FractionalCountGrammar;
import edu.ohsu.cslu.lela.StringCountGrammar;

public class TestCsrSparseMatrixGrammar extends SortedGrammarTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return CsrSparseMatrixGrammar.class;
    }

    @Test
    public void testCartesianProductFunction() throws IOException {
        // Induce a grammar from a sample tree
        final StringCountGrammar sg = new StringCountGrammar(new StringReader(AllLelaTests.STRING_SAMPLE_TREE),
                Binarization.RIGHT, GrammarFormatType.Berkeley);
        final FractionalCountGrammar countGrammar0 = sg.toFractionalCountGrammar();

        // Split the grammar
        final FractionalCountGrammar countGrammar1 = countGrammar0.split();
        final SparseMatrixGrammar csrGrammar1 = new CsrSparseMatrixGrammar(
                countGrammar1.binaryProductions(Float.NEGATIVE_INFINITY),
                countGrammar1.unaryProductions(Float.NEGATIVE_INFINITY),
                countGrammar1.lexicalProductions(Float.NEGATIVE_INFINITY), countGrammar1.vocabulary,
                countGrammar1.lexicon, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);

        final PackingFunction f = csrGrammar1.packingFunction;

        assertEquals(1, f.unpackLeftChild(f.pack((short) 1, (short) 4)));
        assertEquals(4, f.unpackRightChild(f.pack((short) 1, (short) 4)));
        assertEquals(2, f.unpackLeftChild(f.pack((short) 2, (short) 4)));
        assertEquals(4, f.unpackRightChild(f.pack((short) 2, (short) 4)));

        assertEquals(4, f.unpackLeftChild(f.packUnary((short) 4)));
        assertEquals(Production.UNARY_PRODUCTION, f.unpackRightChild(f.packUnary((short) 4)));

        assertEquals(9, f.unpackLeftChild(f.packLexical(9)));
        assertEquals(Production.LEXICAL_PRODUCTION, f.unpackRightChild(f.packLexical(9)));

        assertEquals(0, f.unpackLeftChild(f.packLexical(0)));
        assertEquals(Production.LEXICAL_PRODUCTION, f.unpackRightChild(f.packLexical(0)));
    }

}
