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
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.chart.Chart.RecoveryStrategy;
import edu.ohsu.cslu.parser.ecp.ExhaustiveChartParserTestCase;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParserTestCase;

/**
 * Unit tests for {@link Chart}
 * 
 * @author Aaron Dunlop
 */
@RunWith(FilteredRunner.class)
public class TestChart {

    /**
     * Tests extracting a 'recovery' parse - used to produce a tree when the parse fails.
     * 
     * @throws Exception
     */
    @Test
    public void testExtractRecoveryParse() throws Exception {
        final SparseMatrixGrammar simpleGrammar2 = GrammarTestCase.createGrammar(LeftCscSparseMatrixGrammar.class,
                ExhaustiveChartParserTestCase.simpleGrammar2());
        final Chart chart = new PackedArrayChart(new ParseTask("The fish market stands last", Parser.InputFormat.Text,
                simpleGrammar2, InsideProb.INSTANCE, RecoveryStrategy.RightBiased, DecodeMethod.ViterbiMax),
                simpleGrammar2);
        SparseMatrixVectorParserTestCase.populateSimpleGrammar2Rows1_3(chart, simpleGrammar2);

        final NaryTree<String> tree = chart.extractRecoveryParse(RecoveryStrategy.RightBiased);
        assertEquals("(ROOT (S (NP (DT The) (NN fish)) (S (NP (NN market) (NN stands)) (VP (VB last)))))",
                tree.toString());
    }
}
