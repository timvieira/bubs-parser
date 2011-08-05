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
package edu.ohsu.cslu.parser.chart;

import static org.junit.Assert.assertEquals;

import org.cjunit.FilteredRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.ecp.ExhaustiveChartParserTestCase;

/**
 * Unit tests for {@link PackedArrayChart}
 * 
 * @author Aaron Dunlop
 * @since Sep 9, 2010
 */
@RunWith(FilteredRunner.class)
public class TestPackedArrayChart {

    private PackedArrayChart chart;
    private SparseMatrixGrammar simpleGrammar2;

    /**
     * Constructs the grammar (if necessary) and a new parser instance. Run prior to each test method.
     * 
     * @throws Exception if unable to construct grammar or parser.
     */
    @Before
    public void setUp() throws Exception {
        simpleGrammar2 = GrammarTestCase.createGrammar(LeftCscSparseMatrixGrammar.class,
                ExhaustiveChartParserTestCase.simpleGrammar2());
    }

    @Test
    public void testUnprunedFinalizeCell() throws Exception {
        chart = new PackedArrayChart(new int[] { 1, 2, 3, 4, 5 }, simpleGrammar2);
        final ChartCell cell_2_3 = chart.getCell(2, 3);

        // Three binary productions
        cell_2_3.updateInside(new Production(1, 2, 3, -3f, simpleGrammar2), cell_2_3, null, -3f);
        cell_2_3.updateInside(new Production(2, 3, 4, -2f, simpleGrammar2), cell_2_3, null, -2f);
        cell_2_3.updateInside(new Production(3, 2, 2, -4f, simpleGrammar2), cell_2_3, null, -4f);

        // Two unary productions, one of which will override a binary production
        cell_2_3.updateInside(new Production(2, 3, -1.5f, false, simpleGrammar2), cell_2_3, null, -1.5f);
        cell_2_3.updateInside(new Production(4, 3, -1.5f, false, simpleGrammar2), cell_2_3, null, -1.5f);
        cell_2_3.finalizeCell();

        assertEquals(4, cell_2_3.getNumNTs());
        assertEquals(-3f, cell_2_3.getInside(1), 0.01f);
        assertEquals(-1.5f, cell_2_3.getInside(2), 0.01f);
        assertEquals(-4f, cell_2_3.getInside(3), 0.01f);
        assertEquals(-1.5f, cell_2_3.getInside(4), 0.01f);
    }

    @Test
    public void testPrunedFinalizeCell() throws Exception {
        chart = new PackedArrayChart(new int[] { 1, 2, 3, 4, 5 }, simpleGrammar2, 2, 2);
        final ChartCell cell_2_3 = chart.getCell(2, 3);

        // Three binary productions
        cell_2_3.updateInside(new Production(1, 2, 3, -3f, simpleGrammar2), cell_2_3, null, -3f);
        cell_2_3.updateInside(new Production(2, 3, 4, -2f, simpleGrammar2), cell_2_3, null, -2f);
        cell_2_3.updateInside(new Production(3, 2, 2, -4f, simpleGrammar2), cell_2_3, null, -4f);

        // Two unary productions, one of which will override a binary production
        cell_2_3.updateInside(new Production(2, 3, -1.5f, false, simpleGrammar2), cell_2_3, null, -1.5f);
        cell_2_3.updateInside(new Production(4, 3, -1.5f, false, simpleGrammar2), cell_2_3, null, -1.5f);
        cell_2_3.finalizeCell();

        assertEquals(2, cell_2_3.getNumNTs());
        assertEquals(-1.5f, cell_2_3.getInside(2), 0.01f);
        assertEquals(-1.5f, cell_2_3.getInside(4), 0.01f);
    }
}
