package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.PerformanceTest;
import static org.junit.Assert.assertEquals;

public class TestCsrSparseMatrixVectorParser extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends BaseGrammar> grammarClass() {
        return CsrSparseMatrixGrammar.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar, final ChartTraversalType chartTraversalType) {
        return new CsrSparseMatrixVectorParser((CsrSparseMatrixGrammar) grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "500000", "d820", "500000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    @Test
    public void testSimpleGrammar() throws Exception {
        super.testSimpleGrammar();
    }

    @Test
    public void testPartialSentence2() throws Exception {
        // final String sentence = "The most troublesome report may be due out tomorrow .";
        final String sentence = "The report is due out tomorrow .";
        final ParseTree bestParseTree = parser.findMLParse(sentence);
        assertEquals("(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (DT The) (NN report)) (VP^<S> (AUX is) (ADJP^<VP> (JJ due) (PP^<ADJP> (IN out) (NP^<PP> (NN tomorrow)))))) (. .)))",
                bestParseTree.toString());
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
    // fail("Not Implemented");
    // }

}
