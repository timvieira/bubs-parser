package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.PackedSparseMatrixGrammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestSparseMatrixVectorParser extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends BaseGrammar> grammarClass() {
        return PackedSparseMatrixGrammar.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar, final ChartTraversalType chartTraversalType) {
        return new SparseMatrixVectorParser((PackedSparseMatrixGrammar) grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "50000", "d820", "50000" })
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

    @Override
    public void testSentence1() throws Exception {
        fail("Not implemented");
    }

    @Override
    public void testSentence2() throws Exception {
        fail("Not implemented");
        super.testSentence2();
    }

    @Override
    public void testSentence3() throws Exception {
        fail("Not implemented");
    }

    @Override
    public void testSentence4() throws Exception {
        fail("Not implemented");
    }

    @Override
    public void testSentence5() throws Exception {
        fail("Not implemented");
    }

    @Override
    public void testSentence6() throws Exception {
        fail("Not implemented");
    }

    @Override
    public void testSentence7() throws Exception {
        fail("Not implemented");
        super.testSentence7();
    }

    @Override
    public void testSentence8() throws Exception {
        fail("Not implemented");
    }

    @Override
    public void testSentence9() throws Exception {
        fail("Not implemented");
    }

    @Override
    public void testSentence10() throws Exception {
        fail("Not implemented");
    }
}
