package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.JsaSparseMatrixGrammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestJsaSparseMatrixVectorParser extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends BaseGrammar> grammarClass() {
        return JsaSparseMatrixGrammar.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar, final ChartTraversalType chartTraversalType) {
        return new JsaSparseMatrixVectorParser((JsaSparseMatrixGrammar) grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "500000", "d820", "500000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    // public void testCrossProductVector() throws Exception {
    //
    // // Create a grammar and a chart
    // final JsaSparseMatrixGrammar simpleGrammar = (JsaSparseMatrixGrammar) GrammarTestCase.createSimpleGrammar(grammarClass());
    // final BaseChartCell[][] chart = new BaseChartCell[4][4];
    // for (int i = 0; i < chart.length; i++) {
    // for (int j = 0; j < chart[i].length; j++) {
    // chart[i][j] = new JsaSparseMatrixVectorParser.SparseVectorChartCell(i, j, simpleGrammar);
    // }
    // }
    // // 1,1 X 2,4 = 1/3 (-4), 1/4 (-5), 2/3 (-5), 2/4 (-6)
    // final JsaSparseMatrixVectorParser.SparseVectorChartCell start1Span1 = ((JsaSparseMatrixVectorParser.SparseVectorChartCell) chart[1][1]);
    // start1Span1.addEdge(simpleGrammar.new Production(1, 1, -1, false), -1, null, null);
    //
    // // ((JsaSparseMatrixVectorParser.SparseVectorChartCell) chart[1][1]).validLeftChildrenProbabilities = new float[] {-1, -2};
    // // ((JsaSparseMatrixVectorParser.SparseVectorChartCell) chart[2][4]).validRightChildren = new short[] {3, 4};
    // // ((JsaSparseMatrixVectorParser.SparseVectorChartCell) chart[1][1]).validRightChildrenProbabilities = new float[] {-3, -4};
    //
    // // 1,1 X 2,4 = 1/3, 1/4, 2/3, 2/4
    // ((JsaSparseMatrixVectorParser.SparseVectorChartCell) chart[1][1]).validLeftChildren = new int[] { 1, 2 };
    // ((JsaSparseMatrixVectorParser.SparseVectorChartCell) chart[2][4]).validLeftChildren = new int[] { 3, 4 };
    //
    // }

    @Override
    @Test
    public void testSimpleGrammar() throws Exception {
        final long startTime = System.currentTimeMillis();
        super.testSimpleGrammar();
        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((JsaSparseMatrixVectorParser) parser).totalCrossProductTime,
                ((JsaSparseMatrixVectorParser) parser).totalSpMVTime);
    }

    @Test
    public void testPartialSentence2() throws Exception {
        final long startTime = System.currentTimeMillis();
        // final String sentence = "The most troublesome report may be due out tomorrow .";
        final String sentence = "The report is due out tomorrow .";
        final ParseTree bestParseTree = parser.findMLParse(sentence);
        assertEquals("(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (DT The) (NN report)) (VP^<S> (AUX is) (ADJP^<VP> (JJ due) (PP^<ADJP> (IN out) (NP^<PP> (NN tomorrow)))))) (. .)))",
                bestParseTree.toString());
        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((JsaSparseMatrixVectorParser) parser).totalCrossProductTime,
                ((JsaSparseMatrixVectorParser) parser).totalSpMVTime);
    }

    // @Override
    // @Test
    // public void testSentence1() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence2() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence3() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence4() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence5() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence6() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence7() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence8() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence9() throws Exception {
    // fail("Not Implemented");
    // }
    //
    // @Override
    // @Test
    // public void testSentence10() throws Exception {
    // super.testSentence10();
    // }

    @Override
    protected void parseTreebankSentence(final int index) throws Exception {
        final long startTime = System.currentTimeMillis();
        final ParseTree bestParseTree = parser.findMLParse(sentences.get(index)[0]);
        assertEquals(sentences.get(index)[1], bestParseTree.toString());

        System.out.format("%6.3f,%5d,%5d\n", (System.currentTimeMillis() - startTime) / 1000f, ((JsaSparseMatrixVectorParser) parser).totalCrossProductTime,
                ((JsaSparseMatrixVectorParser) parser).totalSpMVTime);
    }

}
