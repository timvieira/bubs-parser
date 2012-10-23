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

import static com.nativelibs4java.opencl.JavaCL.createBestContext;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartEdge;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductVector;

/**
 * Tests for {@link OpenClSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 */
public abstract class OpenClSpmvParserTestCase<P extends OpenClSpmvParser<? extends ParallelArrayChart>, C extends PackingFunction>
        extends SparseMatrixVectorParserTestCase<P, C> {

    @BeforeClass
    public static void checkOpenCL() throws Exception {

        // Verify that we can load the JavaCL library; ignore tests of OpenCL parsers on platforms that do not
        // support OpenCL
        try {
            createBestContext();
        } catch (final Throwable t) {
            org.junit.Assume.assumeNoException(t);
        }
        f2_21_grammar = null;
        simpleGrammar1 = null;
        simpleGrammar2 = null;
    }

    /**
     * OpenCL parsers must use a simple shift function, since we don't (yet) implement the more complex hashing in
     * OpenCL code. So we override setUp() and tearDown() to null out the grammar and force re-creation, and override
     * createGrammar() to create a grammar implementation using a simpler CPF.
     */
    @AfterClass
    public static void suiteTearDown() throws Exception {
        f2_21_grammar = null;
        simpleGrammar1 = null;
        simpleGrammar2 = null;
    }

    /**
     * Tests the binary SpMV multiplication of the cartesian-product computed in
     * {@link #testUnfilteredCartesianProductVectorSimpleGrammar2()} with simple grammar 2.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testBinarySpMVMultiplySimpleGrammar2() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar2;
        final SparseMatrixVectorParser<?, ?> p = createParser(g, parserOptions(), configProperties());
        final ParseTask parseTask = new ParseTask("The fish market stands last", Parser.InputFormat.Text, g,
                DecodeMethod.ViterbiMax);
        p.initSentence(parseTask);
        final Chart chart = p.chart;

        final float[] probabilities = new float[g.packingFunction().packedArraySize()];
        Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);
        final short[] midpoints = new short[g.packingFunction().packedArraySize()];

        populateSimpleGrammar2Rows1_3(chart, g);

        //
        // Test SpMV for cell 0,4
        //

        // Midpoint 1
        probabilities[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("S"))] = -2.890f;
        midpoints[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("S"))] = 1;

        probabilities[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))] = -2.890f;
        midpoints[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))] = 1;

        // Midpoint 2
        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP|NN"))] = -2.485f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP|NN"))] = 2;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))] = -4.277f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))] = 2;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))] = -4.277f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))] = 2;

        // Midpoint 3
        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))] = -5.663f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))] = 3;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))] = -4.277f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))] = 3;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))] = -4.277f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))] = 3;

        CartesianProductVector crossProductVector = new CartesianProductVector(g, probabilities, midpoints, 8);

        // Check the SpMV multiplication
        final ChartCell cell_0_4 = p.chart.getCell(0, 4);
        p.binarySpmv(crossProductVector, cell_0_4);

        assertEquals(2, cell_0_4.getNumNTs());

        final ChartEdge np = cell_0_4.getBestEdge(g.mapNonterminal("NP"));
        assertEquals(-4.27667, np.inside(), .001f);
        assertEquals("Wrong left child cell", chart.getCell(0, 1), np.leftCell);
        assertEquals("Wrong right child cell", chart.getCell(1, 4), np.rightCell);

        ChartEdge s = cell_0_4.getBestEdge(g.mapNonterminal("S"));
        assertEquals(-5.66296f, s.inside(), .001f);
        assertEquals("Wrong left child cell", chart.getCell(0, 3), s.leftCell);
        assertEquals("Wrong right child cell", chart.getCell(3, 4), s.rightCell);

        //
        // Test SpMV for cell 0,5
        //
        populateSimpleGrammar2Rows1_3(chart, g);
        populateSimpleGrammar2Row4(chart, g);

        Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);

        // Midpoint 3
        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))] = -5.37528f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))] = 3;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))] = -6.474f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))] = 3;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))] = -6.474f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))] = 3;

        // Midpoint 4
        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))] = -4.682f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))] = 4;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))] = -5.375f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))] = 4;

        crossProductVector = new CartesianProductVector(g, probabilities, midpoints, 8);

        // Check the SpMV multiplication
        final ChartCell cell_0_5 = p.chart.getCell(0, 5);
        p.binarySpmv(crossProductVector, cell_0_5);

        assertEquals(1, cell_0_5.getNumNTs());

        s = cell_0_5.getBestEdge(g.mapNonterminal("S"));
        assertEquals(-5.37528f, s.inside(), .001f);
        assertEquals("Wrong left child cell", chart.getCell(0, 3), s.leftCell);
        assertEquals("Wrong right child cell", chart.getCell(3, 5), s.rightCell);
    }

    /**
     * Tests an imagined example cartesian-product vector (based very loosely on the computation of the top cell in the
     * 'systems analyst arbitration chef' example)
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testCartesianProductVectorExample() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar1;
        final P p = createParser(g, parserOptions(), configProperties());
        final ParseTask parseTask = new ParseTask("systems analyst arbitration chef", Parser.InputFormat.Text, g,
                DecodeMethod.ViterbiMax);
        p.initSentence(parseTask);
        final Chart chart = p.chart;

        final int nn = g.mapNonterminal("NN");
        final int np = g.mapNonterminal("NP");
        // Cell 0,1 contains NN (-2)
        // Cell 1,4 contains NN (-3), NP (-4)
        // So: 0,1 X 1,4 cross-product = NN/NN (-5,1), NN/NP (-6,1)
        final ChartCell cell_0_1 = chart.getCell(0, 1);
        cell_0_1.updateInside(new Production("NN", "NN", -2, false, g), cell_0_1, null, -2f);
        cell_0_1.finalizeCell();

        final ChartCell cell_1_3 = chart.getCell(1, 3);
        final ChartCell cell_1_4 = chart.getCell(1, 4);
        cell_1_4.updateInside(new Production("NN", "NN", -3f, false, g), cell_1_3, null, -3f);
        cell_1_4.updateInside(new Production("NP", "NP", -4f, false, g), cell_1_3, null, -4f);
        cell_1_4.finalizeCell();

        // Cell 0,2 contains NN (-2), NP (-3)
        // Cell 2,4 contains NN (-4), NP (-4)
        // So: 0,2 X 2,4 cross-product = NN/NN (-6,2), NN/NP (-6,2), NP/NN (-7,2), NP/NP (-7,2)
        final ChartCell cell_0_2 = chart.getCell(0, 2);
        cell_0_2.updateInside(new Production("NN", "NN", -2f, false, g), chart.getCell(0, 1), null, -2f);
        cell_0_2.updateInside(new Production("NP", "NP", -3f, false, g), chart.getCell(0, 1), null, -3f);
        cell_0_2.finalizeCell();

        final ChartCell cell_2_4 = chart.getCell(2, 4);
        cell_2_4.updateInside(new Production("NN", "NN", -4f, false, g), chart.getCell(2, 3), null, -4f);
        cell_2_4.updateInside(new Production("NP", "NP", -4f, false, g), chart.getCell(2, 3), null, -4f);
        cell_2_4.finalizeCell();

        // Cell 0,3 contains NP (-2)
        // Cell 3,4 contains NP (-2)
        // So: 0,3 X 3,4 cross-product = NP/NP (-4,3)
        final ChartCell cell_0_3 = chart.getCell(0, 3);
        cell_0_3.updateInside(new Production("NP", "NP", -2, false, g), chart.getCell(0, 2), null, -2f);
        cell_0_3.finalizeCell();

        final ChartCell cell_3_4 = chart.getCell(3, 4);
        cell_3_4.updateInside(new Production("NP", "NP", -2f, false, g), chart.getCell(3, 4), null, -2f);
        cell_3_4.finalizeCell();

        // So: 0,1 X 1,4 cross-product = NN/NN (-5,1), NN/NP (-6,1)
        // So: 0,2 X 2,4 cross-product = NN/NN (-6,2), NN/NP (-6,2), NP/NN (-7,2), NP/NP (-7,2)
        // So: 0,3 X 3,4 cross-product = NP/NP (-4,3)

        // Cross-product union should be NN/NN (-5,1), NN/NP (-6,1), NP/NN (-7,2), NP/NP (-4,3)
        final SparseMatrixVectorParser.CartesianProductVector crossProductVector = p.cartesianProductUnion(0, 4);
        final int[] expectedChildren = new int[] { pack(g, nn, nn), pack(g, nn, np), pack(g, np, nn), pack(g, np, np) };
        final float[] expectedProbabilities = new float[] { -5f, -6f, -7f, -4f };
        final int[] expectedMidpoints = new int[] { 1, 1, 2, 3 };

        for (int i = 0; i < expectedChildren.length; i++) {
            assertEquals("Wrong probability #" + i, expectedProbabilities[i],
                    crossProductVector.probability(expectedChildren[i]), .01f);
            assertEquals("Wrong midpoint #" + i, expectedMidpoints[i], crossProductVector.midpoint(expectedChildren[i]));
        }
    }

    /**
     * Tests the cartesian-product vector computed in the top cells of the 'The fish market stands last' example.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testFilteredCartesianProductVectorSimpleGrammar2() throws Exception {

        final SparseMatrixGrammar g = (SparseMatrixGrammar) createGrammar(simpleGrammar2(), LeftShiftFunction.class);

        // Create the parser
        final P p = createParser(g, parserOptions(), configProperties());
        final ParseTask parseTask = new ParseTask("The fish market stands last", Parser.InputFormat.Text, g,
                DecodeMethod.ViterbiMax);
        p.initSentence(parseTask);
        final Chart chart = p.chart;

        populateSimpleGrammar2Rows1_3(chart, g);
        populateSimpleGrammar2Row4(chart, g);

        // Row of span 5
        final ChartCell cell_0_5 = chart.getCell(0, 5);
        cell_0_5.updateInside(new Production("S", "NP", "VP", -5.37528f, simpleGrammar2), chart.getCell(0, 3),
                chart.getCell(3, 5), -5.37528f);
        cell_0_5.updateInside(new Production("TOP", "S", -5.37528f, false, simpleGrammar2), cell_0_5, null, -5.37528f);

        // Finalize all chart cells
        for (int i = 0; i < chart.size(); i++) {
            for (int j = i + 1; j <= chart.size(); j++) {
                chart.getCell(i, j).finalizeCell();
            }
        }

        // Cross-product union for cell 0,4
        SparseMatrixVectorParser.CartesianProductVector crossProductVector = p.cartesianProductUnion(0, 4);
        assertEquals(14, crossProductVector.size());

        // Midpoint 1
        assertEquals(-2.890f, crossProductVector.probability(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))),
                .001f);
        assertEquals(1, crossProductVector.midpoint(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))));

        // Midpoint 3
        assertEquals(-5.663f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))),
                .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));

        // Cross-product union for cell 0,5
        crossProductVector = p.cartesianProductUnion(0, 5);
        assertEquals(12, crossProductVector.size());

        // Midpoint 3
        assertEquals(-5.37528f,
                crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));
    }

    @Override
    @Test
    public void testSimpleGrammar1() throws Exception {
        super.testSimpleGrammar1();
        System.out.println(parser.getStats());
    }

    @Override
    @Test
    public void testSimpleGrammar2() throws Exception {
        super.testSimpleGrammar2();
        System.out.println(parser.getStats());
    }

    /**
     * Tests the cartesian-product vector computed in the top cells of the 'The fish market stands last' example.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testUnfilteredCartesianProductVectorSimpleGrammar2() throws Exception {

        final SparseMatrixGrammar g = (SparseMatrixGrammar) createGrammar(simpleGrammar2(), LeftShiftFunction.class);

        // Create the parser
        final P p = createParser(g, parserOptions(), configProperties());
        final ParseTask parseTask = new ParseTask("The fish market stands last", Parser.InputFormat.Text, g,
                DecodeMethod.ViterbiMax);
        p.initSentence(parseTask);
        final Chart chart = p.chart;

        populateSimpleGrammar2Rows1_3(chart, g);
        populateSimpleGrammar2Row4(chart, g);

        // Row of span 5
        final ChartCell cell_0_5 = chart.getCell(0, 5);
        cell_0_5.updateInside(new Production("S", "NP", "VP", -5.37528f, simpleGrammar2), chart.getCell(0, 3),
                chart.getCell(3, 5), -5.37528f);
        cell_0_5.updateInside(new Production("TOP", "S", -5.37528f, false, simpleGrammar2), cell_0_5, null, -5.37528f);

        // Finalize all chart cells
        for (int i = 0; i < chart.size(); i++) {
            for (int j = i + 1; j <= chart.size(); j++) {
                chart.getCell(i, j).finalizeCell();
            }
        }

        // Cross-product union for cell 0,4
        SparseMatrixVectorParser.CartesianProductVector cartesianProductVector = p.cartesianProductUnion(0, 4);
        assertEquals(14, cartesianProductVector.size());

        // Midpoint 1
        assertEquals(-2.890f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP|VB"))), .001f);
        assertEquals(1, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP|VB"))));

        assertEquals(-2.890f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))), .001f);
        assertEquals(1, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))));

        // Midpoint 2
        assertEquals(-2.485f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP|NN"))), .001f);
        assertEquals(2, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP|NN"))));

        assertEquals(-4.277f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))), .001f);
        assertEquals(2, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))));

        assertEquals(-4.277f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))), .001f);
        assertEquals(2, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))));

        // Midpoint 3
        assertEquals(-5.663f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))), .001f);
        assertEquals(3, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));

        assertEquals(-4.277f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))), .001f);
        assertEquals(3, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))));

        // assertEquals(-4.277f,
        // cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))), .001f);
        // assertEquals(3, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))));

        // Cross-product union for cell 0,5
        cartesianProductVector = p.cartesianProductUnion(0, 5);
        assertEquals(12, cartesianProductVector.size());

        // Midpoint 3
        assertEquals(-5.37528f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))), .001f);
        assertEquals(3, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));

        assertEquals(-6.474f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))), .001f);
        assertEquals(3, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))));

        assertEquals(-6.474f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))), .001f);
        assertEquals(3, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))));

        // Midpoint 4
        assertEquals(-4.682f,
                cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))), .001f);
        assertEquals(4, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))));

        // assertEquals(-5.375f,
        // cartesianProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))), .001f);
        // assertEquals(4, cartesianProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))));
    }

    private int pack(final SparseMatrixGrammar grammar, final int leftChild, final int rightChild) {
        return grammar.packingFunction().pack((short) leftChild, (short) rightChild);
    }
}
