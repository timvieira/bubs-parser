package edu.ohsu.cslu.parser;

import java.util.Arrays;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseSparseMatrixGrammar;
import edu.ohsu.cslu.parser.SparseMatrixVectorParser.CrossProductVector;
import edu.ohsu.cslu.parser.SparseMatrixVectorParser.DenseVectorChartCell;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelector.CellSelectorType;
import edu.ohsu.cslu.parser.util.ParseTree;
import static org.junit.Assert.assertEquals;

public abstract class SparseMatrixVectorParserTestCase extends ExhaustiveChartParserTestCase {

    /**
     * Tests an imagined example cross-product vector (based very loosely on the computation of the top cell in the 'systems analyst arbitration chef' example)
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testCrossProductVectorExample() throws Exception {

        // Create the parser
        final BaseSparseMatrixGrammar g = (BaseSparseMatrixGrammar) simpleGrammar1;
        final SparseMatrixVectorParser p = (SparseMatrixVectorParser) createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(4);
        final Chart<DenseVectorChartCell> chart = (Chart<DenseVectorChartCell>) p.chart;

        // Cell 0,1 contains 1 (-2)
        // Cell 1,4 contains 1 (-3), 2 (-4)
        // So: 0,1 X 1,4 cross-product = 1/1 (-5,1), 1/2 (-6,1)
        final SparseMatrixVectorParser.DenseVectorChartCell cell_0_1 = chart.getCell(0, 1);
        cell_0_1.addEdge(g.new Production(1, 1, -2, false), chart.getCell(0, 1), null, -2f);
        cell_0_1.finalizeCell();

        final SparseMatrixVectorParser.DenseVectorChartCell cell_1_4 = chart.getCell(1, 4);
        cell_1_4.addEdge(g.new Production(1, 1, -3f, false), chart.getCell(1, 3), null, -3f);
        cell_1_4.addEdge(g.new Production(2, 2, -4f, false), chart.getCell(1, 3), null, -4f);
        cell_1_4.finalizeCell();

        // Cell 0,2 contains 1 (-2), 2 (-3)
        // Cell 2,4 contains 1 (-4), 2 (-4)
        // So: 0,2 X 2,4 cross-product = 1/1 (-6,2), 1/2 (-6,2), 2/1 (-7,2), 2/2 (-7,2)
        final SparseMatrixVectorParser.DenseVectorChartCell cell_0_2 = chart.getCell(0, 2);
        cell_0_2.addEdge(g.new Production(1, 1, -2f, false), chart.getCell(0, 1), null, -2f);
        cell_0_2.addEdge(g.new Production(2, 2, -3f, false), chart.getCell(0, 1), null, -3f);
        cell_0_2.finalizeCell();

        final SparseMatrixVectorParser.DenseVectorChartCell cell_2_4 = chart.getCell(2, 4);
        cell_2_4.addEdge(g.new Production(1, 1, -4f, false), chart.getCell(2, 3), null, -4f);
        cell_2_4.addEdge(g.new Production(2, 2, -4f, false), chart.getCell(2, 3), null, -4f);
        cell_2_4.finalizeCell();

        // Cell 0,3 contains 2 (-2)
        // Cell 3,4 contains 2 (-2)
        // So: 0,3 X 3,4 cross-product = 2/2 (-4,3)
        final SparseMatrixVectorParser.DenseVectorChartCell cell_0_3 = chart.getCell(0, 3);
        cell_0_3.addEdge(g.new Production(2, 2, -2, false), chart.getCell(0, 2), null, -2f);
        cell_0_3.finalizeCell();

        final SparseMatrixVectorParser.DenseVectorChartCell cell_3_4 = chart.getCell(3, 4);
        cell_3_4.addEdge(g.new Production(2, 2, -2f, false), chart.getCell(3, 4), null, -2f);
        cell_3_4.finalizeCell();

        // So: 0,1 X 1,4 cross-product = 1/1 (-5,1), 1/2 (-6,1)
        // So: 0,2 X 2,4 cross-product = 1/1 (-6,2), 1/2 (-6,2), 2/1 (-7,2), 2/2 (-7,2)
        // So: 0,3 X 3,4 cross-product = 2/2 (-4,3)

        // Cross-product union should be 1/1 (-5,1), 1/2 (-6,1), 2/1 (-7,2), 2/2 (-4,3)
        final SparseMatrixVectorParser.CrossProductVector crossProductVector = p.crossProductUnion(0, 4);
        final int[] expectedChildren = new int[] { pack(g, 1, 1), pack(g, 1, 2), pack(g, 2, 1), pack(g, 2, 2) };
        final float[] expectedProbabilities = new float[] { -5f, -6f, -7f, -4f };
        final int[] expectedMidpoints = new int[] { 1, 1, 2, 3 };

        for (int i = 0; i < expectedChildren.length; i++) {
            assertEquals("Wrong probability #" + i, expectedProbabilities[i], crossProductVector.probability(expectedChildren[i]), .01f);
            assertEquals("Wrong midpoint #" + i, expectedMidpoints[i], crossProductVector.midpoint(expectedChildren[i]));
        }
    }

    /**
     * Tests the binary SpMV multiplication of the cross-product computed in {@link #testCrossProductVectorExample()} with simple grammar 1.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testBinarySpMVMultiplyExample() throws Exception {

        // Create the parser
        final BaseSparseMatrixGrammar g = (BaseSparseMatrixGrammar) simpleGrammar1;
        final SparseMatrixVectorParser p = (SparseMatrixVectorParser) createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(4);
        final Chart<DenseVectorChartCell> chart = (Chart<DenseVectorChartCell>) p.chart;
        final DenseVectorChartCell topCell = (DenseVectorChartCell) p.chart.getCell(0, 4);

        final int nn = g.mapNonterminal("NN");
        final int np = g.mapNonterminal("NP");

        final float[] probabilities = new float[g.packedArraySize()];
        Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);
        final short[] midpoints = new short[g.packedArraySize()];

        final int nnNp = g.pack(nn, (short) np);
        probabilities[nnNp] = -1.897f;
        midpoints[nnNp] = 1;

        final int npNp = g.pack(np, (short) np);
        probabilities[npNp] = -1.386f;
        midpoints[npNp] = 2;

        final int npNn = g.pack(np, (short) nn);
        probabilities[npNn] = -1.897f;
        midpoints[npNn] = 3;

        final CrossProductVector cpv = new CrossProductVector(g, probabilities, midpoints, 3);
        p.binarySpmvMultiply(cpv, topCell);
        assertEquals(1, topCell.getNumEdgeEntries());

        final ChartEdge edge = topCell.getBestEdge(np);
        assertEquals(-3.101f, edge.inside, .01f);
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
        final BaseSparseMatrixGrammar g = (BaseSparseMatrixGrammar) simpleGrammar1;
        final SparseMatrixVectorParser p = (SparseMatrixVectorParser) createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(4);

        final DenseVectorChartCell topCell = (DenseVectorChartCell) p.chart.getCell(0, 4);
        topCell.children[2] = 17;
        topCell.midpoints[2] = 3;
        topCell.probabilities[2] = -3.101f;

        p.unarySpmvMultiply(topCell);
        assertEquals(2, topCell.getNumEdgeEntries());

        final ChartEdge topEdge = topCell.getBestEdge(g.mapNonterminal("TOP"));
        assertEquals(-3.101f, topEdge.inside, .01f);
        assertEquals(topCell, topEdge.leftCell);
        assertEquals(null, topEdge.rightCell);
    }

    /**
     * Tests the cross-product vector computed in the top cell of the 'The fish market stands last' example.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testCrossProductVectorSimpleGrammar2() throws Exception {

        // Create the parser
        final BaseSparseMatrixGrammar g = (BaseSparseMatrixGrammar) simpleGrammar2;
        final SparseMatrixVectorParser p = (SparseMatrixVectorParser) createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(5);
        final Chart<DenseVectorChartCell> chart = (Chart<DenseVectorChartCell>) p.chart;

        // Row of span 1
        final SparseMatrixVectorParser.DenseVectorChartCell cell_0_1 = chart.getCell(0, 1);
        cell_0_1.addEdge(g.new Production("DT", "The", 0, true), chart.getCell(0, 1), null, 0f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_1_2 = chart.getCell(1, 2);
        cell_1_2.addEdge(g.new Production("NN", "fish", 0, true), chart.getCell(1, 2), null, 0f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_2_3 = chart.getCell(2, 3);
        cell_2_3.addEdge(g.new Production("VP", "VB", -2.48491f, false), chart.getCell(2, 3), null, -2.48491f);
        cell_2_3.addEdge(g.new Production("NN", "market", -4.0547f, true), chart.getCell(2, 3), null, -.40547f);
        cell_2_3.addEdge(g.new Production("VB", "market", -1.09861f, true), chart.getCell(2, 3), null, -1.09861f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_3_4 = chart.getCell(3, 4);
        cell_3_4.addEdge(g.new Production("VP", "VB", -2.07944f, false), chart.getCell(3, 4), null, -2.07944f);
        cell_3_4.addEdge(g.new Production("NN", "stands", -.69315f, true), chart.getCell(3, 4), null, -.69315f);
        cell_3_4.addEdge(g.new Production("VB", "stands", -.69315f, true), chart.getCell(3, 4), null, -.69315f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_4_5 = chart.getCell(4, 5);
        cell_4_5.addEdge(g.new Production("VP", "VB", -2.48491f, false), chart.getCell(4, 5), null, -2.48491f);
        cell_4_5.addEdge(g.new Production("RB", "last", -.40547f, true), chart.getCell(4, 5), null, -.40547f);
        cell_4_5.addEdge(g.new Production("VB", "last", -1.09861f, true), chart.getCell(4, 5), null, -1.09861f);

        // Row of span 2
        final SparseMatrixVectorParser.DenseVectorChartCell cell_0_2 = chart.getCell(0, 2);
        cell_0_2.addEdge(g.new Production("NP", "DT", "NN", -1.38629f), chart.getCell(0, 1), chart.getCell(1, 2), -1.38629f);
        cell_0_2.addEdge(g.new Production("VP-VB", "NP", -1.38629f, false), chart.getCell(0, 2), null, -1.38629f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_1_3 = chart.getCell(1, 3);
        cell_1_3.addEdge(g.new Production("NP-NN", "NN", "NN", -.40547f), chart.getCell(1, 2), chart.getCell(2, 3), -.40547f);
        cell_1_3.addEdge(g.new Production("NP", "NN", "NN", -2.19722f), chart.getCell(1, 2), chart.getCell(2, 3), -2.19722f);
        cell_1_3.addEdge(g.new Production("VP-VB", "NP", -2.19722f, false), chart.getCell(1, 3), null, -2.19722f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_2_4 = chart.getCell(2, 4);
        cell_2_4.addEdge(g.new Production("NP-NN", "NN", "NN", -1.09861f), chart.getCell(2, 3), chart.getCell(3, 4), -1.09861f);
        cell_2_4.addEdge(g.new Production("NP", "NN", "NN", -2.89037f), chart.getCell(2, 3), chart.getCell(3, 4), -2.89037f);
        cell_2_4.addEdge(g.new Production("VP-VB", "NP", -2.89037f, false), chart.getCell(2, 4), null, -2.89037f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_3_5 = chart.getCell(3, 5);
        cell_3_5.addEdge(g.new Production("VP", "VB", "RB", -1.79176f), chart.getCell(3, 4), chart.getCell(4, 5), -1.79176f);
        cell_3_5.addEdge(g.new Production("NP", "NN", "RB", -2.89037f), chart.getCell(3, 4), chart.getCell(4, 5), -2.89037f);
        cell_3_5.addEdge(g.new Production("VP-VB", "NP", -2.89037f, false), chart.getCell(3, 5), null, -2.89037f);

        // Row of span 3
        final SparseMatrixVectorParser.DenseVectorChartCell cell_0_3 = chart.getCell(0, 3);
        cell_0_3.addEdge(g.new Production("NP", "DT", "NP", -3.58352f), chart.getCell(0, 1), chart.getCell(1, 3), -3.58352f);
        cell_0_3.addEdge(g.new Production("S", "NP", "VP", -3.87120f), chart.getCell(0, 2), chart.getCell(2, 3), -3.87120f);
        cell_0_3.addEdge(g.new Production("VP-VB", "NP", -3.58352f, false), chart.getCell(0, 3), null, -3.58352f);
        cell_0_3.addEdge(g.new Production("TOP", "S", -3.87120f, false), chart.getCell(0, 3), null, -3.87120f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_1_4 = chart.getCell(1, 4);
        cell_1_4.addEdge(g.new Production("NP", "NN", "NP-NN", -2.89037f), chart.getCell(1, 2), chart.getCell(2, 4), -2.89037f);
        cell_1_4.addEdge(g.new Production("S", "NP", "VP", -4.27667f), chart.getCell(1, 3), chart.getCell(3, 4), -4.27667f);
        cell_1_4.addEdge(g.new Production("VP-VB", "NP", -2.89037f, false), chart.getCell(1, 4), null, -2.89037f);
        cell_1_4.addEdge(g.new Production("TOP", "S", -4.27667f, false), chart.getCell(1, 4), null, -4.27667f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_2_5 = chart.getCell(2, 5);
        cell_2_5.addEdge(g.new Production("VP", "VB", "VP-VB", -5.37528f), chart.getCell(2, 3), chart.getCell(3, 5), -5.37528f);
        cell_2_5.addEdge(g.new Production("S", "NP", "VP", -5.37528f), chart.getCell(2, 4), chart.getCell(4, 5), -5.37528f);
        cell_2_5.addEdge(g.new Production("TOP", "S", -5.37528f, false), chart.getCell(2, 5), null, -5.37528f);

        // Row of span 4
        final SparseMatrixVectorParser.DenseVectorChartCell cell_0_4 = chart.getCell(0, 4);
        cell_0_4.addEdge(g.new Production("NP", "DT", "NP", -4.27667f), chart.getCell(0, 1), chart.getCell(1, 4), -4.27667f);
        cell_0_4.addEdge(g.new Production("S", "NP", "VP", -5.66296f), chart.getCell(0, 3), chart.getCell(3, 4), -5.66296f);
        cell_0_4.addEdge(g.new Production("VP-VB", "NP", -4.27667f, false), chart.getCell(0, 4), null, -4.27667f);
        cell_0_4.addEdge(g.new Production("TOP", "S", -5.66296f, false), chart.getCell(0, 4), null, -5.66296f);

        final SparseMatrixVectorParser.DenseVectorChartCell cell_1_5 = chart.getCell(1, 5);
        cell_1_5.addEdge(g.new Production("S", "NP", "VP", -3.98898f), chart.getCell(1, 3), chart.getCell(3, 5), -3.98898f);
        cell_1_5.addEdge(g.new Production("TOP", "S", -3.98898f, false), chart.getCell(1, 5), null, -3.98898f);

        // Row of span 5
        final SparseMatrixVectorParser.DenseVectorChartCell cell_0_5 = chart.getCell(0, 5);
        cell_0_5.addEdge(g.new Production("S", "NP", "VP", -5.37528f), chart.getCell(0, 3), chart.getCell(3, 5), -5.37528f);
        cell_0_5.addEdge(g.new Production("TOP", "S", -5.37528f, false), chart.getCell(0, 5), null, -5.37528f);

        // Finalize all chart cells
        for (int i = 0; i < chart.size(); i++) {
            for (int j = i + 1; j <= chart.size(); j++) {
                (chart.getCell(i, j)).finalizeCell();
            }
        }

        // Cross-product union for cell 0,4
        SparseMatrixVectorParser.CrossProductVector crossProductVector = p.crossProductUnion(0, 4);
        assertEquals(8, crossProductVector.size());

        // Midpoint 1
        assertEquals(-2.890f, crossProductVector.probability(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP-VB"))), .001f);
        assertEquals(1, crossProductVector.midpoint(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP-VB"))));

        assertEquals(-2.890f, crossProductVector.probability(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))), .001f);
        assertEquals(1, crossProductVector.midpoint(pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))));

        // Midpoint 2
        assertEquals(-2.485f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP-NN"))), .001f);
        assertEquals(2, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP-NN"))));

        assertEquals(-4.277f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP-VB"))), .001f);
        assertEquals(2, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP-VB"))));

        assertEquals(-4.277f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))), .001f);
        assertEquals(2, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))));

        // Midpoint 3
        assertEquals(-5.663f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));

        assertEquals(-4.277f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))));

        assertEquals(-4.277f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))));

        // Cross-product union for cell 0,5
        crossProductVector = p.crossProductUnion(0, 5);
        assertEquals(5, crossProductVector.size());

        // Midpoint 3
        assertEquals(-5.37528f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))));

        assertEquals(-6.474f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP-VB"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP-VB"))));

        assertEquals(-6.474f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))), .001f);
        assertEquals(3, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))));

        // Midpoint 4
        assertEquals(-4.682f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))), .001f);
        assertEquals(4, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))));

        assertEquals(-5.375f, crossProductVector.probability(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))), .001f);
        assertEquals(4, crossProductVector.midpoint(pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))));
    }

    /**
     * Tests the binary SpMV multiplication of the cross-products computed in {@link #testCrossProductVectorSimpleGrammar2()} with simple grammar 2.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testBinarySpMVMultiplySimpleGrammar2() throws Exception {

        // Create the parser
        final BaseSparseMatrixGrammar g = (BaseSparseMatrixGrammar) simpleGrammar2;
        final SparseMatrixVectorParser p = (SparseMatrixVectorParser) createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(5);
        final Chart chart = p.chart;

        final float[] probabilities = new float[g.packedArraySize()];
        Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);
        final short[] midpoints = new short[g.packedArraySize()];

        // Construct cross-product union for cell 0,5

        // Midpoint 3
        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))] = -5.37528f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))] = 3;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP-VB"))] = -6.474f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP-VB"))] = 3;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))] = -6.474f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))] = 3;

        // Midpoint 4
        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))] = -4.682f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("RB"))] = 4;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))] = -5.375f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))] = 4;

        CrossProductVector crossProductVector = new CrossProductVector(g, probabilities, midpoints, 8);

        // Check the SpMV multiplication
        DenseVectorChartCell cell = (DenseVectorChartCell) p.chart.getCell(0, 5);
        p.binarySpmvMultiply(crossProductVector, cell);

        assertEquals(1, cell.getNumEdgeEntries());

        ChartEdge s = cell.getBestEdge(g.mapNonterminal("S"));
        assertEquals(-5.37528f, s.inside, .001f);
        assertEquals("Wrong left child cell", chart.getCell(0, 3), s.leftCell);
        assertEquals("Wrong right child cell", chart.getCell(3, 5), s.rightCell);

        // Construct cross-product union for cell 0,4

        // Midpoint 1
        probabilities[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP-VB"))] = -2.890f;
        midpoints[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("VP-VB"))] = 1;

        probabilities[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))] = -2.890f;
        midpoints[pack(g, g.mapNonterminal("DT"), g.mapNonterminal("NP"))] = 1;

        // Midpoint 2
        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP-NN"))] = -2.485f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP-NN"))] = 2;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP-VB"))] = -4.277f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP-VB"))] = 2;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))] = -4.277f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NP"))] = 2;

        // Midpoint 3
        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))] = -5.663f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VP"))] = 3;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))] = -4.277f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("NN"))] = 3;

        probabilities[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))] = -4.277f;
        midpoints[pack(g, g.mapNonterminal("NP"), g.mapNonterminal("VB"))] = 3;

        crossProductVector = new CrossProductVector(g, probabilities, midpoints, 8);

        // Check the SpMV multiplication
        cell = (DenseVectorChartCell) p.chart.getCell(0, 4);
        p.binarySpmvMultiply(crossProductVector, cell);

        assertEquals(2, cell.getNumEdgeEntries());

        final ChartEdge np = cell.getBestEdge(g.mapNonterminal("NP"));
        assertEquals(-4.27667, np.inside, .001f);
        assertEquals("Wrong left child cell", chart.getCell(0, 1), np.leftCell);
        assertEquals("Wrong right child cell", chart.getCell(1, 4), np.rightCell);

        s = cell.getBestEdge(g.mapNonterminal("S"));
        assertEquals(-5.66296f, s.inside, .001f);
        assertEquals("Wrong left child cell", chart.getCell(0, 3), s.leftCell);
        assertEquals("Wrong right child cell", chart.getCell(3, 4), s.rightCell);
    }

    /**
     * Tests the unary SpMV multiplication of the top cell population computed by {@link #testBinarySpMVMultiplySimpleGrammar2()} with simple grammar 2.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testUnarySpMVMultiplySimpleGrammar2() throws Exception {

        // Create the parser
        final BaseSparseMatrixGrammar g = (BaseSparseMatrixGrammar) simpleGrammar2;
        final SparseMatrixVectorParser p = (SparseMatrixVectorParser) createParser(g, CellSelector.create(CellSelectorType.LeftRightBottomTop));
        p.initParser(5);

        // Cell 0,5
        DenseVectorChartCell cell = (DenseVectorChartCell) p.chart.getCell(0, 5);
        cell.addEdge(g.new Production("S", "NP", "VP", -5.37528f), p.chart.getCell(0, 3), p.chart.getCell(3, 5), -5.37528f);

        p.unarySpmvMultiply(cell);

        // We expect a single entry to have been added for 'TOP -> S'
        assertEquals(2, cell.getNumEdgeEntries());

        ChartEdge top = cell.getBestEdge(g.mapNonterminal("TOP"));
        assertEquals(-5.37528f, top.inside, .01f);
        assertEquals(cell, top.leftCell);
        assertEquals(null, top.rightCell);

        // Cell 0,4
        cell = (DenseVectorChartCell) p.chart.getCell(0, 4);
        cell.addEdge(g.new Production("NP", "DT", "NP", -4.27667f), p.chart.getCell(0, 1), p.chart.getCell(1, 4), -4.27667f);
        cell.addEdge(g.new Production("S", "NP", "VP", -5.66296f), p.chart.getCell(0, 3), p.chart.getCell(3, 4), -5.66296f);

        p.unarySpmvMultiply(cell);

        // We expect two entries to have been added for 'TOP -> S' and 'VP-VB -> NP'
        assertEquals(4, cell.getNumEdgeEntries());

        top = cell.getBestEdge(g.mapNonterminal("TOP"));
        assertEquals(-5.66296f, top.inside, .01f);
        assertEquals(cell, top.leftCell);
        assertEquals(null, top.rightCell);

        final ChartEdge vpVb = cell.getBestEdge(g.mapNonterminal("VP-VB"));
        assertEquals(-4.27667f, vpVb.inside, .01f);
        assertEquals(cell, vpVb.leftCell);
        assertEquals(null, vpVb.rightCell);
    }

    private int pack(final BaseSparseMatrixGrammar grammar, final int leftChild, final int rightChild) {
        return grammar.pack(leftChild, (short) rightChild);
    }

    @Override
    @Test
    public void testSimpleGrammar1() throws Exception {
        final long startTime = System.currentTimeMillis();
        super.testSimpleGrammar1();
        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((SparseMatrixVectorParser) parser).totalCrossProductTime,
                ((SparseMatrixVectorParser) parser).totalSpMVTime);
    }

    @Override
    @Test
    public void testSimpleGrammar2() throws Exception {
        final long startTime = System.currentTimeMillis();
        super.testSimpleGrammar2();
        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((SparseMatrixVectorParser) parser).totalCrossProductTime,
                ((SparseMatrixVectorParser) parser).totalSpMVTime);
    }

    @Test
    public void testPartialSentence2() throws Exception {
        final long startTime = System.currentTimeMillis();
        // final String sentence = "The most troublesome report may be due out tomorrow .";
        final String sentence = "The report is due out tomorrow .";
        final ParseTree bestParseTree = parser.findBestParse(sentence);
        assertEquals("(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (DT The) (NN report)) (VP^<S> (AUX is) (ADJP^<VP> (JJ due) (PP^<ADJP> (IN out) (NP^<PP> (NN tomorrow)))))) (. .)))",
                bestParseTree.toString());
        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((SparseMatrixVectorParser) parser).totalCrossProductTime,
                ((SparseMatrixVectorParser) parser).totalSpMVTime);
    }

    @Override
    protected void parseTreebankSentence(final int index) throws Exception {
        final long startTime = System.currentTimeMillis();
        final ParseTree bestParseTree = parser.findBestParse(sentences.get(index)[0]);
        assertEquals(sentences.get(index)[1], bestParseTree.toString());

        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((SparseMatrixVectorParser) parser).totalCrossProductTime,
                ((SparseMatrixVectorParser) parser).totalSpMVTime);
    }
}
