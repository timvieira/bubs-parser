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
package edu.ohsu.cslu.lela;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;

/**
 * Unit tests for {@link ConstrainedCsrSparseMatrixGrammar}.
 * 
 * @author Aaron Dunlop
 * @since Feb 20, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestConstrainedCsrSparseMatrixGrammar {

    @Test
    public void testBinaryLeftChildStartIndices() {
        final ProductionListGrammar plGrammar0 = new ProductionListGrammar(
            TestMappedCountGrammar.SAMPLE_MAPPED_GRAMMAR());
        final ConstrainedCsrSparseMatrixGrammar csrGrammar0 = new ConstrainedCsrSparseMatrixGrammar(
            plGrammar0, GrammarFormatType.Berkeley,
            SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);

        // 'top' has no binary children
        assertNull(csrGrammar0.csrBinaryBaseStartIndices[0]);

        // Children of a (2), b (1)
        assertArrayEquals(new int[] { 0, 0, 2, 2 }, csrGrammar0.csrBinaryBaseStartIndices[1]);
        assertArrayEquals(new int[] { 2, 2, 2, 3 }, csrGrammar0.csrBinaryBaseStartIndices[2]);

        final ProductionListGrammar plGrammar1 = plGrammar0
            .split(new ProductionListGrammar.RandomNoiseGenerator(0.01f));
        final ConstrainedCsrSparseMatrixGrammar csrGrammar1 = new ConstrainedCsrSparseMatrixGrammar(
            plGrammar1, GrammarFormatType.Berkeley,
            SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);

        // 'top' has no binary children
        assertNull(csrGrammar1.csrBinaryBaseStartIndices[0]);

        // Children of a_0 (4), a_1 (4), b_0 (2), and b_1 (2)
        assertArrayEquals(new int[] { 0, 0, 8, 8 }, csrGrammar1.csrBinaryBaseStartIndices[1]);
        assertArrayEquals(new int[] { 8, 8, 16, 16 }, csrGrammar1.csrBinaryBaseStartIndices[2]);
        assertArrayEquals(new int[] { 16, 16, 16, 20 }, csrGrammar1.csrBinaryBaseStartIndices[3]);
        assertArrayEquals(new int[] { 20, 20, 20, 24 }, csrGrammar1.csrBinaryBaseStartIndices[4]);

        // Split again, and then merge a_2 into a_1 and b_3 into b_2
        final ProductionListGrammar mergedGrammar2 = plGrammar1.split(
            new ProductionListGrammar.RandomNoiseGenerator(0.01f)).merge(new short[] { 3, 7 });
        final ConstrainedCsrSparseMatrixGrammar csrGrammar2 = new ConstrainedCsrSparseMatrixGrammar(
            mergedGrammar2, GrammarFormatType.Berkeley,
            SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);

        // Children of a_0, a_1, a_2, b_0, b_1, b_2
        assertArrayEquals(new int[] { 0, 0, 18, 18 }, csrGrammar2.csrBinaryBaseStartIndices[1]);
        assertArrayEquals(new int[] { 18, 18, 36, 36 }, csrGrammar2.csrBinaryBaseStartIndices[2]);
        assertArrayEquals(new int[] { 36, 36, 54, 54 }, csrGrammar2.csrBinaryBaseStartIndices[3]);
        assertArrayEquals(new int[] { 54, 54, 54, 63 }, csrGrammar2.csrBinaryBaseStartIndices[4]);
        assertArrayEquals(new int[] { 63, 63, 63, 72 }, csrGrammar2.csrBinaryBaseStartIndices[5]);
        assertArrayEquals(new int[] { 72, 72, 72, 81 }, csrGrammar2.csrBinaryBaseStartIndices[6]);
    }
}
