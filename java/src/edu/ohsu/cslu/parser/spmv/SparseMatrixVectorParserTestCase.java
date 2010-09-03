package edu.ohsu.cslu.parser.spmv;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ExhaustiveChartParserTestCase;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelector.CellSelectorType;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartEdge;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductVector;
import edu.ohsu.cslu.parser.util.ParseTree;

/**
 * Base test class for all sparse-matrix-vector parsers
 * 
 * @author Aaron Dunlop
 * @since Mar 2, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class SparseMatrixVectorParserTestCase<P extends SparseMatrixVectorParser<? extends SparseMatrixGrammar, ? extends ParallelArrayChart>>
        extends ExhaustiveChartParserTestCase<P> {

    /**
     * Tests an imagined example cartesian-product vector (based very loosely on the computation of the top cell in the 'systems analyst arbitration chef' example)
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testCartesianProductVectorExample() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar1;
        final P p = createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(4);
        final Chart chart = p.chart;

        final int nn = g.mapNonterminal("NN");
        final int np = g.mapNonterminal("NP");
        // Cell 0,1 contains NN (-2)
        // Cell 1,4 contains NN (-3), NP (-4)
        // So: 0,1 X 1,4 cross-product = NN/NN (-5,1), NN/NP (-6,1)
        final ChartCell cell_0_1 = chart.getCell(0, 1);
        cell_0_1.updateInside(g.new Production("NN", "NN", -2, false), cell_0_1, null, -2f);
        cell_0_1.finalizeCell();

        final ChartCell cell_1_3 = chart.getCell(1, 3);
        final ChartCell cell_1_4 = chart.getCell(1, 4);
        cell_1_4.updateInside(g.new Production("NN", "NN", -3f, false), cell_1_3, null, -3f);
        cell_1_4.updateInside(g.new Production("NP", "NP", -4f, false), cell_1_3, null, -4f);
        cell_1_4.finalizeCell();

        // Cell 0,2 contains NN (-2), NP (-3)
        // Cell 2,4 contains NN (-4), NP (-4)
        // So: 0,2 X 2,4 cross-product = NN/NN (-6,2), NN/NP (-6,2), NP/NN (-7,2), NP/NP (-7,2)
        final ChartCell cell_0_2 = chart.getCell(0, 2);
        cell_0_2.updateInside(g.new Production("NN", "NN", -2f, false), chart.getCell(0, 1), null, -2f);
        cell_0_2.updateInside(g.new Production("NP", "NP", -3f, false), chart.getCell(0, 1), null, -3f);
        cell_0_2.finalizeCell();

        final ChartCell cell_2_4 = chart.getCell(2, 4);
        cell_2_4.updateInside(g.new Production("NN", "NN", -4f, false), chart.getCell(2, 3), null, -4f);
        cell_2_4.updateInside(g.new Production("NP", "NP", -4f, false), chart.getCell(2, 3), null, -4f);
        cell_2_4.finalizeCell();

        // Cell 0,3 contains NP (-2)
        // Cell 3,4 contains NP (-2)
        // So: 0,3 X 3,4 cross-product = NP/NP (-4,3)
        final ChartCell cell_0_3 = chart.getCell(0, 3);
        cell_0_3.updateInside(g.new Production("NP", "NP", -2, false), chart.getCell(0, 2), null, -2f);
        cell_0_3.finalizeCell();

        final ChartCell cell_3_4 = chart.getCell(3, 4);
        cell_3_4.updateInside(g.new Production("NP", "NP", -2f, false), chart.getCell(3, 4), null, -2f);
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
     * Tests the binary SpMV multiplication of a cartesian-product against simple grammar 1.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testBinarySpMVMultiplyExample() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar1;
        final P p = createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(4);
        final Chart chart = p.chart;

        // Cell 0,3 contains NP -> NP NN (3/20)
        final ChartCell cell_0_3 = chart.getCell(0, 3);
        cell_0_3.updateInside(g.new Production("NP", "NP", "NN", -1.90f), chart.getCell(0, 2), chart.getCell(2, 3),
                -1.90f);
        cell_0_3.finalizeCell();

        // Cell 3,4 contains NN -> chef (1)
        final ChartCell cell_3_4 = chart.getCell(3, 4);
        cell_3_4.updateInside(g.new Production("NN", "stands", 0f, true), chart.getCell(3, 4), null, 0f);
        cell_3_4.finalizeCell();

        // Cell 0,4 contains
        // NP -> DT NP (9/200)
        // S -> NP VP (9/200)

        final int nn = g.mapNonterminal("NN");
        final int np = g.mapNonterminal("NP");

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
     * Tests the unary SpMV multiplication of the top cell population computed by {@link #testBinarySpMVMultiplyExample()} with simple grammar 1.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testUnarySpMVMultiplyExample() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar1;
        final SparseMatrixVectorParser<?, ?> p = createParser(g,
                CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(4);

        final ChartCell topCell = p.chart.getCell(0, 4);
        final int parent = g.mapNonterminal("NP");
        topCell.updateInside(g.new Production(parent, g.mapNonterminal("NP"), g.mapNonterminal("NN"), -3.101f),
                p.chart.getCell(0, 3), p.chart.getCell(3, 4), -3.101f);

        p.unarySpmv(topCell);
        assertEquals(2, topCell.getNumNTs());

        final ChartEdge topEdge = topCell.getBestEdge(g.mapNonterminal("TOP"));
        assertEquals(-3.101f, topEdge.inside(), .01f);
        assertEquals(topCell, topEdge.leftCell);
        assertEquals(null, topEdge.rightCell);
    }

    private void populateSimpleGrammar2Rows1_3(final Chart chart) {

        // Row of span 1
        final ChartCell cell_0_1 = chart.getCell(0, 1);
        cell_0_1.updateInside(simpleGrammar2.new Production("DT", "The", 0, true), cell_0_1, null, 0f);

        final ChartCell cell_1_2 = chart.getCell(1, 2);
        cell_1_2.updateInside(simpleGrammar2.new Production("NN", "fish", 0, true), cell_1_2, null, 0f);

        final ChartCell cell_2_3 = chart.getCell(2, 3);
        cell_2_3.updateInside(simpleGrammar2.new Production("VP", "VB", -2.48491f, false), cell_2_3, null, -2.48491f);
        cell_2_3.updateInside(simpleGrammar2.new Production("NN", "market", -4.0547f, true), cell_2_3, null, -.40547f);
        cell_2_3.updateInside(simpleGrammar2.new Production("VB", "market", -1.09861f, true), cell_2_3, null, -1.09861f);

        final ChartCell cell_3_4 = chart.getCell(3, 4);
        cell_3_4.updateInside(simpleGrammar2.new Production("VP", "VB", -2.07944f, false), cell_3_4, null, -2.07944f);
        cell_3_4.updateInside(simpleGrammar2.new Production("NN", "stands", -.69315f, true), cell_3_4, null, -.69315f);
        cell_3_4.updateInside(simpleGrammar2.new Production("VB", "stands", -.69315f, true), cell_3_4, null, -.69315f);

        final ChartCell cell_4_5 = chart.getCell(4, 5);
        cell_4_5.updateInside(simpleGrammar2.new Production("VP", "VB", -2.48491f, false), cell_4_5, null, -2.48491f);
        cell_4_5.updateInside(simpleGrammar2.new Production("RB", "last", -.40547f, true), cell_4_5, null, -.40547f);
        cell_4_5.updateInside(simpleGrammar2.new Production("VB", "last", -1.09861f, true), cell_4_5, null, -1.09861f);

        // Row of span 2
        final ChartCell cell_0_2 = chart.getCell(0, 2);
        cell_0_2.updateInside(simpleGrammar2.new Production("NP", "DT", "NN", -1.38629f), cell_0_1, cell_1_2, -1.38629f);
        cell_0_2.updateInside(simpleGrammar2.new Production("VP|VB", "NP", -1.38629f, false), cell_0_2, null, -1.38629f);

        final ChartCell cell_1_3 = chart.getCell(1, 3);
        cell_1_3.updateInside(simpleGrammar2.new Production("NP|NN", "NN", "NN", -.40547f), cell_1_2, cell_2_3,
                -.40547f);
        cell_1_3.updateInside(simpleGrammar2.new Production("NP", "NN", "NN", -2.19722f), cell_1_2, cell_2_3, -2.19722f);
        cell_1_3.updateInside(simpleGrammar2.new Production("VP|VB", "NP", -2.19722f, false), cell_1_3, null, -2.19722f);

        final ChartCell cell_2_4 = chart.getCell(2, 4);
        cell_2_4.updateInside(simpleGrammar2.new Production("NP|NN", "NN", "NN", -1.09861f), cell_2_3, cell_3_4,
                -1.09861f);
        cell_2_4.updateInside(simpleGrammar2.new Production("NP", "NN", "NN", -2.89037f), cell_2_3, cell_3_4, -2.89037f);
        cell_2_4.updateInside(simpleGrammar2.new Production("VP|VB", "NP", -2.89037f, false), cell_2_4, null, -2.89037f);

        final ChartCell cell_3_5 = chart.getCell(3, 5);
        cell_3_5.updateInside(simpleGrammar2.new Production("VP", "VB", "RB", -1.79176f), cell_3_4, cell_4_5, -1.79176f);
        cell_3_5.updateInside(simpleGrammar2.new Production("NP", "NN", "RB", -2.89037f), cell_3_4, cell_4_5, -2.89037f);
        cell_3_5.updateInside(simpleGrammar2.new Production("VP|VB", "NP", -2.89037f, false), cell_3_5, null, -2.89037f);

        // Row of span 3
        final ChartCell cell_0_3 = chart.getCell(0, 3);
        cell_0_3.updateInside(simpleGrammar2.new Production("NP", "DT", "NP", -3.58352f), cell_0_1, cell_1_3, -3.58352f);
        cell_0_3.updateInside(simpleGrammar2.new Production("S", "NP", "VP", -3.87120f), cell_0_2, cell_2_3, -3.87120f);
        cell_0_3.updateInside(simpleGrammar2.new Production("VP|VB", "NP", -3.58352f, false), cell_0_3, null, -3.58352f);
        cell_0_3.updateInside(simpleGrammar2.new Production("TOP", "S", -3.87120f, false), cell_0_3, null, -3.87120f);
        cell_0_3.finalizeCell();

        final ChartCell cell_1_4 = chart.getCell(1, 4);
        cell_1_4.updateInside(simpleGrammar2.new Production("NP", "NN", "NP|NN", -2.89037f), cell_1_2, cell_2_4,
                -2.89037f);
        cell_1_4.updateInside(simpleGrammar2.new Production("S", "NP", "VP", -4.27667f), cell_1_3, cell_3_4, -4.27667f);
        cell_1_4.updateInside(simpleGrammar2.new Production("VP|VB", "NP", -2.89037f, false), cell_1_4, null, -2.89037f);
        cell_1_4.updateInside(simpleGrammar2.new Production("TOP", "S", -4.27667f, false), cell_1_4, null, -4.27667f);

        final ChartCell cell_2_5 = chart.getCell(2, 5);
        cell_2_5.updateInside(simpleGrammar2.new Production("VP", "VB", "VP|VB", -5.37528f), cell_2_3, cell_3_5,
                -5.37528f);
        cell_2_5.updateInside(simpleGrammar2.new Production("S", "NP", "VP", -5.37528f), cell_2_4, cell_4_5, -5.37528f);
        cell_2_5.updateInside(simpleGrammar2.new Production("TOP", "S", -5.37528f, false), cell_2_5, null, -5.37528f);

    }

    private void populateSimpleGrammar2Row4(final Chart chart) {
        // Row of span 4
        final ChartCell cell_0_4 = chart.getCell(0, 4);
        cell_0_4.updateInside(simpleGrammar2.new Production("NP", "DT", "NP", -4.27667f), chart.getCell(0, 1),
                chart.getCell(1, 4), -4.27667f);
        cell_0_4.updateInside(simpleGrammar2.new Production("S", "NP", "VP", -5.66296f), chart.getCell(0, 3),
                chart.getCell(3, 4), -5.66296f);
        cell_0_4.updateInside(simpleGrammar2.new Production("VP|VB", "NP", -4.27667f, false), cell_0_4, null, -4.27667f);
        cell_0_4.updateInside(simpleGrammar2.new Production("TOP", "S", -5.66296f, false), cell_0_4, null, -5.66296f);

        final ChartCell cell_1_5 = chart.getCell(1, 5);
        cell_1_5.updateInside(simpleGrammar2.new Production("S", "NP", "VP", -3.98898f), chart.getCell(1, 3),
                chart.getCell(3, 5), -3.98898f);
        cell_1_5.updateInside(simpleGrammar2.new Production("TOP", "S", -3.98898f, false), cell_1_5, null, -3.98898f);
    }

    /**
     * Tests the cartesian-product vector computed in the top cells of the 'The fish market stands last' example.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testUnfilteredCartesianProductVectorSimpleGrammar2() throws Exception {

        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar2;

        // Create the parser
        final P p = createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(5);
        final Chart chart = p.chart;

        populateSimpleGrammar2Rows1_3(chart);
        populateSimpleGrammar2Row4(chart);

        // Row of span 5
        final ChartCell cell_0_5 = chart.getCell(0, 5);
        cell_0_5.updateInside(simpleGrammar2.new Production("S", "NP", "VP", -5.37528f), chart.getCell(0, 3),
                chart.getCell(3, 5), -5.37528f);
        cell_0_5.updateInside(simpleGrammar2.new Production("TOP", "S", -5.37528f, false), cell_0_5, null, -5.37528f);

        // Finalize all chart cells
        for (int i = 0; i < chart.size(); i++) {
            for (int j = i + 1; j <= chart.size(); j++) {
                chart.getCell(i, j).finalizeCell();
            }
        }

        // Cross-product union for cell 0,4
        SparseMatrixVectorParser.CartesianProductVector crossProductVector = p.cartesianProductUnion(0, 4);
        assertEquals(21, crossProductVector.size());

        // Midpoint 1
        assertEquals(-2.890f,
                crossProductVector.probability(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP|VB"))), .001f);
        assertEquals(1, crossProductVector.midpoint(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP|VB"))));

        assertEquals(-2.890f, crossProductVector.probability(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))),
                .001f);
        assertEquals(1, crossProductVector.midpoint(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))));

        // Midpoint 2
        assertEquals(-2.485f,
                crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP|NN"))), .001f);
        assertEquals(2, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP|NN"))));

        assertEquals(-4.277f,
                crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))), .001f);
        assertEquals(2, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))));

        assertEquals(-4.277f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))),
                .001f);
        assertEquals(2, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))));

        // Midpoint 3
        assertEquals(-5.663f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))),
                .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));

        assertEquals(-4.277f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))),
                .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))));

        assertEquals(-4.277f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))),
                .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))));

        // Cross-product union for cell 0,5
        crossProductVector = p.cartesianProductUnion(0, 5);
        assertEquals(23, crossProductVector.size());

        // Midpoint 3
        assertEquals(-5.37528f,
                crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));

        assertEquals(-6.474f,
                crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP|VB"))));

        assertEquals(-6.474f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))),
                .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))));

        // Midpoint 4
        assertEquals(-4.682f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))),
                .001f);
        assertEquals(4, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))));

        assertEquals(-5.375f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))),
                .001f);
        assertEquals(4, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))));
    }

    /**
     * Tests the cartesian-product vector computed in the top cells of the 'The fish market stands last' example.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testFilteredCartesianProductVectorSimpleGrammar2() throws Exception {

        final SparseMatrixGrammar g = (SparseMatrixGrammar) createSimpleGrammar2(grammarClass(),
                SparseMatrixGrammar.BitVectorExactFilterFunction.class);

        // Create the parser
        final P p = createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(5);
        final Chart chart = p.chart;

        populateSimpleGrammar2Rows1_3(chart);
        populateSimpleGrammar2Row4(chart);

        // Row of span 5
        final ChartCell cell_0_5 = chart.getCell(0, 5);
        cell_0_5.updateInside(simpleGrammar2.new Production("S", "NP", "VP", -5.37528f), chart.getCell(0, 3),
                chart.getCell(3, 5), -5.37528f);
        cell_0_5.updateInside(simpleGrammar2.new Production("TOP", "S", -5.37528f, false), cell_0_5, null, -5.37528f);

        // Finalize all chart cells
        for (int i = 0; i < chart.size(); i++) {
            for (int j = i + 1; j <= chart.size(); j++) {
                chart.getCell(i, j).finalizeCell();
            }
        }

        // Cross-product union for cell 0,4
        SparseMatrixVectorParser.CartesianProductVector crossProductVector = p.cartesianProductUnion(0, 4);
        assertEquals(2, crossProductVector.size());

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
        assertEquals(1, crossProductVector.size());

        // Midpoint 3
        assertEquals(-5.37528f,
                crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));
    }

    /**
     * Tests the binary SpMV multiplication of the cartesian-product computed in {@link #testUnfilteredCartesianProductVectorSimpleGrammar2()} with simple grammar 2.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testBinarySpMVMultiplySimpleGrammar2() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar2;
        final SparseMatrixVectorParser<?, ?> p = createParser(g,
                CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(5);
        final Chart chart = p.chart;

        final float[] probabilities = new float[g.cartesianProductFunction().packedArraySize()];
        Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);
        final short[] midpoints = new short[g.cartesianProductFunction().packedArraySize()];

        populateSimpleGrammar2Rows1_3(chart);

        //
        // Test SpMV for cell 0,4
        //

        // Midpoint 1
        probabilities[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP|VB"))] = -2.890f;
        midpoints[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP|VB"))] = 1;

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
        populateSimpleGrammar2Rows1_3(chart);
        populateSimpleGrammar2Row4(chart);

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
     * Tests the unary SpMV multiplication of the top cell population computed by {@link #testBinarySpMVMultiplySimpleGrammar2()} with simple grammar 2.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testUnarySpMVMultiplySimpleGrammar2() throws Exception {

        // Create the parser
        final SparseMatrixGrammar g = (SparseMatrixGrammar) simpleGrammar2;
        final SparseMatrixVectorParser<?, ?> p = createParser(g,
                CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(5);

        final Chart chart = p.chart;
        populateSimpleGrammar2Rows1_3(chart);
        populateSimpleGrammar2Row4(chart);

        // Cell 0,5
        ChartCell cell = chart.getCell(0, 5);
        cell.updateInside(g.new Production("S", "NP", "VP", -5.37528f), chart.getCell(0, 3), chart.getCell(3, 5),
                -5.37528f);

        p.unarySpmv(cell);

        // We expect a single entry to have been added for 'TOP -> S'
        assertEquals(2, cell.getNumNTs());

        ChartEdge top = cell.getBestEdge(g.mapNonterminal("TOP"));
        assertEquals(-5.37528f, top.inside(), .01f);
        assertEquals(cell, top.leftCell);
        assertEquals(null, top.rightCell);

        // Cell 0,4
        cell = p.chart.getCell(0, 4);
        cell.updateInside(g.new Production("NP", "DT", "NP", -4.27667f), chart.getCell(0, 1), chart.getCell(1, 4),
                -4.27667f);
        cell.updateInside(g.new Production("S", "NP", "VP", -5.66296f), chart.getCell(0, 3), chart.getCell(3, 4),
                -5.66296f);

        p.unarySpmv(cell);

        // We expect two entries to have been added for 'TOP -> S' and 'VP|VB -> NP'
        assertEquals(4, cell.getNumNTs());

        top = cell.getBestEdge(g.mapNonterminal("TOP"));
        assertEquals(-5.66296f, top.inside(), .01f);
        assertEquals(cell, top.leftCell);
        assertEquals(null, top.rightCell);

        final ChartEdge vpVb = cell.getBestEdge(g.mapNonterminal("VP|VB"));
        assertEquals(-4.27667f, vpVb.inside(), .01f);
        assertEquals(cell, vpVb.leftCell);
        assertEquals(null, vpVb.rightCell);
    }

    private int pack(final SparseMatrixGrammar grammar, final int leftChild, final int rightChild) {
        return grammar.cartesianProductFunction().pack(leftChild, rightChild);
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

    @Test
    public void testPartialSentence2() throws Exception {
        final String sentence = "The report is due out tomorrow .";
        final ParseTree bestParseTree = parser.findBestParse(sentence);

        assertEquals(
                "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (DT The) (NN report)) (VP^<S> (AUX is) (ADJP^<VP> (JJ due) (PP^<ADJP> (IN out) (NP^<PP> (NN tomorrow)))))) (. .)))",
                bestParseTree.toString());
        System.out.println(parser.getStats());
    }

    @Override
    protected void parseTreebankSentence(final int index) throws Exception {
        final ParseTree bestParseTree = parser.findBestParse(sentences.get(index)[0]);
        assertEquals(sentences.get(index)[1], bestParseTree.toString());
        System.out.println(parser.getStats());
    }
}
