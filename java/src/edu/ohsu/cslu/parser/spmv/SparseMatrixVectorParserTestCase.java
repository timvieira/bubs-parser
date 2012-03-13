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
package edu.ohsu.cslu.parser.spmv;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartEdge;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.ecp.ExhaustiveChartParserTestCase;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductVector;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Base test class for all sparse-matrix-vector parsers
 * 
 * @author Aaron Dunlop
 * @since Mar 2, 2010
 */
public abstract class SparseMatrixVectorParserTestCase<P extends SparseMatrixVectorParser<? extends SparseMatrixGrammar, ? extends ParallelArrayChart>, C extends PackingFunction>
        extends ExhaustiveChartParserTestCase<P> {

    @Override
    public Grammar createGrammar(final Reader grammarReader) throws Exception {
        return createGrammar(grammarReader, cpfClass());
    }

    protected Grammar createGrammar(final Reader grammarReader, final Class<? extends PackingFunction> cpfClass)
            throws Exception {
        return grammarClass().getConstructor(new Class[] { Reader.class, Class.class }).newInstance(
                new Object[] { grammarReader, cpfClass });
    }

    @SuppressWarnings("unchecked")
    private Class<C> cpfClass() {
        return ((Class<C>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1]);
    }

    /**
     * Constructs the grammar (if necessary) and a new parser instance. Run prior to each test method.
     * 
     * @throws Exception if unable to construct grammar or parser.
     */
    @Override
    @Before
    public void setUp() throws Exception {

        if (f2_21_grammar == null || f2_21_grammar.getClass() != grammarClass()
                || ((SparseMatrixGrammar) f2_21_grammar).packingFunction.getClass() != cpfClass()) {
            f2_21_grammar = createGrammar(JUnit.unitTestDataAsReader(PCFG_FILE));
        }

        if (simpleGrammar1 == null || simpleGrammar1.getClass() != grammarClass()
                || ((SparseMatrixGrammar) simpleGrammar1).packingFunction.getClass() != cpfClass()) {
            simpleGrammar1 = createGrammar(GrammarTestCase.simpleGrammar());
        }

        if (simpleGrammar2 == null || simpleGrammar2.getClass() != grammarClass()
                || ((SparseMatrixGrammar) simpleGrammar2).packingFunction.getClass() != cpfClass()) {
            simpleGrammar2 = createGrammar(simpleGrammar2());
        }

        parser = createParser(f2_21_grammar, parserOptions(), configProperties());
    }

    @After
    public void tearDown() {
        if (parser != null) {
            parser.shutdown();
        }
    }

    /**
     * Tests the binary SpMV multiplication of a cartesian-product against simple grammar 1.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testBinarySpMVMultiplyExample() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar1;
        final P p = createParser(g, parserOptions(), configProperties());
        final ParseTask parseTask = new ParseTask("systems analyst arbitration chef", Parser.InputFormat.Text, g,
                DecodeMethod.ViterbiMax);
        p.initSentence(parseTask);
        // p.parseSentence("systems analyst arbitration chef");
        final Chart chart = p.chart;

        // Cell 0,3 contains NP -> NP NN (3/20)
        final ChartCell cell_0_3 = chart.getCell(0, 3);
        cell_0_3.updateInside(new Production("NP", "NP", "NN", -1.90f, g), chart.getCell(0, 2), chart.getCell(2, 3),
                -1.90f);
        cell_0_3.finalizeCell();

        // Cell 3,4 contains NN -> chef (1)
        final ChartCell cell_3_4 = chart.getCell(3, 4);
        cell_3_4.updateInside(new Production("NN", "stands", 0f, true, g), chart.getCell(3, 4), null, 0f);
        cell_3_4.finalizeCell();

        // Cell 0,4 contains
        // NP -> DT NP (9/200)
        // S -> NP VP (9/200)

        final short nn = (short) g.mapNonterminal("NN");
        final short np = (short) g.mapNonterminal("NP");

        final float[] probabilities = new float[g.cartesianProductFunction().packedArraySize()];
        Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);
        final short[] midpoints = new short[g.cartesianProductFunction().packedArraySize()];

        final int nnNp = g.cartesianProductFunction().pack(nn, np);
        probabilities[nnNp] = -1.897f;
        midpoints[nnNp] = 1;

        final int npNp = g.cartesianProductFunction().pack(np, np);
        probabilities[npNp] = -1.386f;
        midpoints[npNp] = 2;

        final int npNn = g.cartesianProductFunction().pack(np, nn);
        probabilities[npNn] = -1.897f;
        midpoints[npNn] = 3;

        final CartesianProductVector cpv = new CartesianProductVector(g, probabilities, midpoints, 3);
        final ChartCell cell_0_4 = chart.getCell(0, 4);
        p.binarySpmv(cpv, cell_0_4);
        assertEquals(1, cell_0_4.getNumNTs());

        final ChartEdge edge = cell_0_4.getBestEdge(np);
        assertEquals(-3.101f, edge.inside(), .01f);
        assertEquals("Wrong left child cell", chart.getCell(0, 3), edge.leftCell);
        assertEquals("Wrong right child cell", chart.getCell(3, 4), edge.rightCell);
    }

    /**
     * Tests the unary SpMV multiplication of the top cell population computed by
     * {@link #testBinarySpMVMultiplyExample()} with simple grammar 1.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testUnarySpMVMultiplyExample() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar1;
        final SparseMatrixVectorParser<?, ?> p = createParser(g, parserOptions(), configProperties());
        final ParseTask parseTask = new ParseTask("systems analyst arbitration chef", Parser.InputFormat.Text, g,
                DecodeMethod.ViterbiMax);
        p.initSentence(parseTask);
        // p.initSentence(new int[4]);

        final ChartCell topCell = p.chart.getCell(0, 4);
        final int parent = g.mapNonterminal("NP");
        topCell.updateInside(new Production(parent, g.mapNonterminal("NP"), g.mapNonterminal("NN"), -3.101f, g),
                p.chart.getCell(0, 3), p.chart.getCell(3, 4), -3.101f);

        p.unarySpmv(topCell);
        topCell.finalizeCell();
        assertEquals(2, topCell.getNumNTs());

        final ChartEdge topEdge = topCell.getBestEdge(g.mapNonterminal("ROOT"));
        assertEquals(-3.101f, topEdge.inside(), .01f);
        assertEquals(topCell, topEdge.leftCell);
        assertEquals(null, topEdge.rightCell);
    }

    public static void populateSimpleGrammar2Rows1_3(final Chart chart, final Grammar grammar) {

        // Row of span 1
        final ChartCell cell_0_1 = chart.getCell(0, 1);
        cell_0_1.updateInside(new Production("DT", "The", 0, true, grammar.nonTermSet, grammar.lexSet), cell_0_1, null,
                0f);
        cell_0_1.finalizeCell();

        final ChartCell cell_1_2 = chart.getCell(1, 2);
        cell_1_2.updateInside(new Production("NN", "fish", 0, true, grammar.nonTermSet, grammar.lexSet), cell_1_2,
                null, 0f);
        cell_1_2.finalizeCell();

        final ChartCell cell_2_3 = chart.getCell(2, 3);
        cell_2_3.updateInside(new Production("VP", "VB", -2.48491f, false, grammar.nonTermSet, grammar.lexSet),
                cell_2_3, null, -2.48491f);
        cell_2_3.updateInside(new Production("NN", "market", -4.0547f, true, grammar.nonTermSet, grammar.lexSet),
                cell_2_3, null, -.40547f);
        cell_2_3.updateInside(new Production("VB", "market", -1.09861f, true, grammar.nonTermSet, grammar.lexSet),
                cell_2_3, null, -1.09861f);
        cell_2_3.finalizeCell();

        final ChartCell cell_3_4 = chart.getCell(3, 4);
        cell_3_4.updateInside(new Production("VP", "VB", -2.07944f, false, grammar.nonTermSet, grammar.lexSet),
                cell_3_4, null, -2.07944f);
        cell_3_4.updateInside(new Production("NN", "stands", -.69315f, true, grammar.nonTermSet, grammar.lexSet),
                cell_3_4, null, -.69315f);
        cell_3_4.updateInside(new Production("VB", "stands", -.69315f, true, grammar.nonTermSet, grammar.lexSet),
                cell_3_4, null, -.69315f);
        cell_3_4.finalizeCell();

        final ChartCell cell_4_5 = chart.getCell(4, 5);
        cell_4_5.updateInside(new Production("VP", "VB", -2.48491f, false, grammar.nonTermSet, grammar.lexSet),
                cell_4_5, null, -2.48491f);
        cell_4_5.updateInside(new Production("RB", "last", -.40547f, true, grammar.nonTermSet, grammar.lexSet),
                cell_4_5, null, -.40547f);
        cell_4_5.updateInside(new Production("VB", "last", -1.09861f, true, grammar.nonTermSet, grammar.lexSet),
                cell_4_5, null, -1.09861f);
        cell_4_5.finalizeCell();

        // Row of span 2
        final ChartCell cell_0_2 = chart.getCell(0, 2);
        cell_0_2.updateInside(new Production("NP", "DT", "NN", -1.38629f, grammar), cell_0_1, cell_1_2, -1.38629f);
        cell_0_2.updateInside(new Production("VP|VB", "NP", -1.38629f, false, grammar.nonTermSet, grammar.lexSet),
                cell_0_2, null, -1.38629f);
        cell_0_2.finalizeCell();

        final ChartCell cell_1_3 = chart.getCell(1, 3);
        cell_1_3.updateInside(new Production("NP|NN", "NN", "NN", -.40547f, grammar), cell_1_2, cell_2_3, -.40547f);
        cell_1_3.updateInside(new Production("NP", "NN", "NN", -2.19722f, grammar), cell_1_2, cell_2_3, -2.19722f);
        cell_1_3.updateInside(new Production("VP|VB", "NP", -2.19722f, false, grammar.nonTermSet, grammar.lexSet),
                cell_1_3, null, -2.19722f);
        cell_1_3.finalizeCell();

        final ChartCell cell_2_4 = chart.getCell(2, 4);
        cell_2_4.updateInside(new Production("NP|NN", "NN", "NN", -1.09861f, grammar), cell_2_3, cell_3_4, -1.09861f);
        cell_2_4.updateInside(new Production("NP", "NN", "NN", -2.89037f, grammar), cell_2_3, cell_3_4, -2.89037f);
        cell_2_4.updateInside(new Production("VP|VB", "NP", -2.89037f, false, grammar), cell_2_4, null, -2.89037f);
        cell_2_4.finalizeCell();

        final ChartCell cell_3_5 = chart.getCell(3, 5);
        cell_3_5.updateInside(new Production("VP", "VB", "RB", -1.79176f, grammar), cell_3_4, cell_4_5, -1.79176f);
        cell_3_5.updateInside(new Production("NP", "NN", "RB", -2.89037f, grammar), cell_3_4, cell_4_5, -2.89037f);
        cell_3_5.updateInside(new Production("VP|VB", "NP", -2.89037f, false, grammar), cell_3_5, null, -2.89037f);
        cell_3_5.finalizeCell();

        // Row of span 3
        final ChartCell cell_0_3 = chart.getCell(0, 3);
        cell_0_3.updateInside(new Production("NP", "DT", "NP", -3.58352f, grammar), cell_0_1, cell_1_3, -3.58352f);
        cell_0_3.updateInside(new Production("S", "NP", "VP", -3.87120f, grammar), cell_0_2, cell_2_3, -3.87120f);
        cell_0_3.updateInside(new Production("VP|VB", "NP", -3.58352f, false, grammar), cell_0_3, null, -3.58352f);
        cell_0_3.updateInside(new Production("ROOT", "S", -3.87120f, false, grammar), cell_0_3, null, -3.87120f);
        cell_0_3.finalizeCell();

        final ChartCell cell_1_4 = chart.getCell(1, 4);
        cell_1_4.updateInside(new Production("NP", "NN", "NP|NN", -2.89037f, grammar), cell_1_2, cell_2_4, -2.89037f);
        cell_1_4.updateInside(new Production("S", "NP", "VP", -4.27667f, grammar), cell_1_3, cell_3_4, -4.27667f);
        cell_1_4.updateInside(new Production("VP|VB", "NP", -2.89037f, false, grammar), cell_1_4, null, -2.89037f);
        cell_1_4.updateInside(new Production("ROOT", "S", -4.27667f, false, grammar), cell_1_4, null, -4.27667f);
        cell_1_4.finalizeCell();

        final ChartCell cell_2_5 = chart.getCell(2, 5);
        cell_2_5.updateInside(new Production("VP", "VB", "VP|VB", -5.37528f, grammar), cell_2_3, cell_3_5, -5.37528f);
        cell_2_5.updateInside(new Production("S", "NP", "VP", -5.37528f, grammar), cell_2_4, cell_4_5, -5.37528f);
        cell_2_5.updateInside(new Production("ROOT", "S", -5.37528f, false, grammar), cell_2_5, null, -5.37528f);
        cell_2_5.finalizeCell();
    }

    protected void populateSimpleGrammar2Row4(final Chart chart, final Grammar grammar) {
        // Row of span 4
        final ChartCell cell_0_4 = chart.getCell(0, 4);
        cell_0_4.updateInside(new Production("NP", "DT", "NP", -4.27667f, grammar), chart.getCell(0, 1),
                chart.getCell(1, 4), -4.27667f);
        cell_0_4.updateInside(new Production("S", "NP", "VP", -5.66296f, grammar), chart.getCell(0, 3),
                chart.getCell(3, 4), -5.66296f);
        cell_0_4.updateInside(new Production("VP|VB", "NP", -4.27667f, false, grammar), cell_0_4, null, -4.27667f);
        cell_0_4.updateInside(new Production("ROOT", "S", -5.66296f, false, grammar), cell_0_4, null, -5.66296f);

        final ChartCell cell_1_5 = chart.getCell(1, 5);
        cell_1_5.updateInside(new Production("S", "NP", "VP", -3.98898f, grammar), chart.getCell(1, 3),
                chart.getCell(3, 5), -3.98898f);
        cell_1_5.updateInside(new Production("ROOT", "S", -3.98898f, false, grammar), cell_1_5, null, -3.98898f);
    }

    /**
     * Tests the unary SpMV multiplication of the top cell population computed with simple grammar 2.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testUnarySpMVMultiplySimpleGrammar2() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar2;
        final SparseMatrixVectorParser<?, ?> p = createParser(g, parserOptions(), configProperties());
        final ParseTask parseTask = new ParseTask("The fish market stands last", Parser.InputFormat.Text, g,
                DecodeMethod.ViterbiMax);
        p.initSentence(parseTask);
        // p.initSentence(new int[5]);

        final Chart chart = p.chart;
        populateSimpleGrammar2Rows1_3(chart, simpleGrammar2);
        populateSimpleGrammar2Row4(chart, simpleGrammar2);

        // Cell 0,5
        ChartCell cell = chart.getCell(0, 5);
        cell.updateInside(new Production("S", "NP", "VP", -5.37528f, g), chart.getCell(0, 3), chart.getCell(3, 5),
                -5.37528f);
        p.unarySpmv(cell);
        cell.finalizeCell();

        // We expect a single entry to have been added for 'ROOT -> S'
        assertEquals(2, cell.getNumNTs());

        ChartEdge top = cell.getBestEdge(g.mapNonterminal("ROOT"));
        assertEquals(-5.37528f, top.inside(), .01f);
        assertEquals(cell, top.leftCell);
        assertEquals(null, top.rightCell);

        // Cell 0,4
        cell = p.chart.getCell(0, 4);
        cell.updateInside(new Production("NP", "DT", "NP", -4.27667f, g), chart.getCell(0, 1), chart.getCell(1, 4),
                -4.27667f);
        cell.updateInside(new Production("S", "NP", "VP", -5.66296f, g), chart.getCell(0, 3), chart.getCell(3, 4),
                -5.66296f);

        p.unarySpmv(cell);
        cell.finalizeCell();

        // We expect two entries to have been added for 'ROOT -> S' and 'VP|VB -> NP'
        assertEquals(4, cell.getNumNTs());

        top = cell.getBestEdge(g.mapNonterminal("ROOT"));
        assertEquals(-5.66296f, top.inside(), .01f);
        assertEquals(cell, top.leftCell);
        assertEquals(null, top.rightCell);

        final ChartEdge vpVb = cell.getBestEdge(g.mapNonterminal("VP|VB"));
        assertEquals(-4.27667f, vpVb.inside(), .01f);
        assertEquals(cell, vpVb.leftCell);
        assertEquals(null, vpVb.rightCell);
    }

    // @Test
    // public void testPartialSentence2() throws Exception {
    // final String sentence = "The report is due out tomorrow .";
    // final String bestParseTree = parser.parseSentence(sentence).parseBracketString;
    //
    // assertEquals(
    // "(ROOT (S^<ROOT> (S|<NP-VP>^<ROOT> (NP^<S> (DT The) (NN report)) (VP^<S> (AUX is) (ADJP^<VP> (JJ due) (PP^<ADJP> (IN out) (NP^<PP> (NN tomorrow)))))) (. .)))",
    // bestParseTree);
    // System.out.println(parser.getStats());
    // }
}
