package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class SparseMatrixVectorParserTestCase extends ExhaustiveChartParserTestCase {

    @Test
    public void testCrossProductVector() throws Exception {

        // Create a grammar and a parser
        final BaseSparseMatrixGrammar g = (BaseSparseMatrixGrammar) GrammarTestCase.createSimpleGrammar(grammarClass());
        final SparseMatrixVectorParser p = (SparseMatrixVectorParser) createParser(g, ChartTraversalType.LeftRightBottomTopTraversal);
        p.initParser(4);
        final BaseChartCell[][] chart = p.chart;

        // Cell 0,1 contains 0 (-1), 1 (-2)
        // Cell 1,4 contains 1 (-3), 2 (-4)
        // So: 0,1 X 1,4 cross-product = 0/1 (-4,1), 0/2 (-5,1), 1/1 (-5,1), 1/2 (-6,1)
        final SparseMatrixVectorParser.SparseVectorChartCell cell_0_1 = ((SparseMatrixVectorParser.SparseVectorChartCell) chart[0][1]);
        cell_0_1.addEdge(g.new Production(0, 0, -1, false), -1f, chart[0][1], null);
        cell_0_1.addEdge(g.new Production(1, 1, -2, false), -2f, chart[0][1], null);
        cell_0_1.finalizeCell();

        final SparseMatrixVectorParser.SparseVectorChartCell cell_1_4 = ((SparseMatrixVectorParser.SparseVectorChartCell) chart[1][4]);
        cell_1_4.addEdge(g.new Production(1, 1, -3f, false), -3f, chart[1][3], null);
        cell_1_4.addEdge(g.new Production(2, 2, -4f, false), -4f, chart[1][3], null);
        cell_1_4.finalizeCell();

        // Cell 0,2 contains 1 (-2), 2 (-3)
        // Cell 2,4 contains 1 (-4), 2 (-4)
        // So: 0,2 X 2,4 cross-product = 1/1 (-6,2), 1/2 (-6,2), 2/1 (-7,2), 2/2 (-7,2)
        final SparseMatrixVectorParser.SparseVectorChartCell cell_0_2 = ((SparseMatrixVectorParser.SparseVectorChartCell) chart[0][2]);
        cell_0_2.addEdge(g.new Production(1, 1, -2f, false), -2f, chart[0][1], null);
        cell_0_2.addEdge(g.new Production(2, 2, -3f, false), -3f, chart[0][1], null);
        cell_0_2.finalizeCell();

        final SparseMatrixVectorParser.SparseVectorChartCell cell_2_4 = ((SparseMatrixVectorParser.SparseVectorChartCell) chart[2][4]);
        cell_2_4.addEdge(g.new Production(1, 1, -4f, false), -4f, chart[2][3], null);
        cell_2_4.addEdge(g.new Production(2, 2, -4f, false), -4f, chart[2][3], null);
        cell_2_4.finalizeCell();

        // Cell 0,3 contains 0 (-.5), 2 (-2)
        // Cell 3,4 contains 0 (-2), 2 (-2)
        // So: 0,3 X 3,4 cross-product = 0/0 (-2.5,3), 0/2 (-2.5,3), 2/0 (-4,3), 2/2 (-4,3)
        final SparseMatrixVectorParser.SparseVectorChartCell cell_0_3 = ((SparseMatrixVectorParser.SparseVectorChartCell) chart[0][3]);
        cell_0_3.addEdge(g.new Production(0, 0, -.5f, false), -.5f, chart[0][2], null);
        cell_0_3.addEdge(g.new Production(2, 2, -2, false), -2f, chart[0][2], null);
        cell_0_3.finalizeCell();

        final SparseMatrixVectorParser.SparseVectorChartCell cell_3_4 = ((SparseMatrixVectorParser.SparseVectorChartCell) chart[3][4]);
        cell_3_4.addEdge(g.new Production(0, 0, -2f, false), -2f, chart[3][4], null);
        cell_3_4.addEdge(g.new Production(2, 2, -2f, false), -2f, chart[3][4], null);
        cell_3_4.finalizeCell();

        // So: 0,1 X 1,4 cross-product = 0/1 (-4,1), 0/2 (-5,1), 1/1 (-5,1), 1/2 (-6,1)
        // So: 0,2 X 2,4 cross-product = 1/1 (-6,2), 1/2 (-6,2), 2/1 (-7,2), 2/2 (-7,2)
        // So: 0,3 X 3,4 cross-product = 0/0 (-2.5,3), 0/2 (-2.5,3), 2/0 (-4,3), 2/2 (-4,3)

        // Cross-product union should be 0/0 (-2.5,3), 0/1 (-4,1), 0/2 (-2.5,3), 1/1 (-5,1), 1/2 (-6,1), 2/0 (-4,3), 2/1 (-7,2), 2/2 (-4,3)
        final SparseMatrixVectorParser.CrossProductVector crossProductVector = p.crossProductUnion(0, 4);
        final int[] expectedChildren = new int[] { pack(g, 0, 0), pack(g, 0, 1), pack(g, 0, 2), pack(g, 1, 1), pack(g, 1, 2), pack(g, 2, 0), pack(g, 2, 1), pack(g, 2, 2) };
        final float[] expectedProbabilities = new float[] { -2.5f, -4f, -2.5f, -5f, -6f, -4f, -7f, -4f };
        final int[] expectedMidpoints = new int[] { 3, 1, 3, 1, 1, 3, 2, 3 };

        for (int i = 0; i < 8; i++) {
            assertEquals("Wrong probability #" + i, expectedProbabilities[i], crossProductVector.probability(expectedChildren[i]), .01f);
            assertEquals("Wrong midpoint #" + i, expectedMidpoints[i], crossProductVector.midpoint(expectedChildren[i]));
        }
    }

    private int pack(final BaseSparseMatrixGrammar grammar, final int leftChild, final int rightChild) {
        return grammar.pack(leftChild, (short) rightChild);
    }

    @Test
    public void testBinarySpMV() {
        fail("Not Implemented");
    }

    @Test
    public void testUnarySpMV() {
        fail("Not Implemented");
    }

    @Override
    @Test
    public void testSimpleGrammar() throws Exception {
        final long startTime = System.currentTimeMillis();
        super.testSimpleGrammar();
        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((SparseMatrixVectorParser) parser).totalCrossProductTime,
                ((SparseMatrixVectorParser) parser).totalSpMVTime);
    }

    @Test
    public void testPartialSentence2() throws Exception {
        final long startTime = System.currentTimeMillis();
        // final String sentence = "The most troublesome report may be due out tomorrow .";
        final String sentence = "The report is due out tomorrow .";
        final ParseTree bestParseTree = parser.findMLParse(sentence);
        assertEquals("(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (DT The) (NN report)) (VP^<S> (AUX is) (ADJP^<VP> (JJ due) (PP^<ADJP> (IN out) (NP^<PP> (NN tomorrow)))))) (. .)))",
                bestParseTree.toString());
        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((SparseMatrixVectorParser) parser).totalCrossProductTime,
                ((SparseMatrixVectorParser) parser).totalSpMVTime);
    }

    @Override
    protected void parseTreebankSentence(final int index) throws Exception {
        final long startTime = System.currentTimeMillis();
        final ParseTree bestParseTree = parser.findMLParse(sentences.get(index)[0]);
        assertEquals(sentences.get(index)[1], bestParseTree.toString());

        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((SparseMatrixVectorParser) parser).totalCrossProductTime,
                ((SparseMatrixVectorParser) parser).totalSpMVTime);
    }
}
