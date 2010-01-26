package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.PackedSparseMatrixGrammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestPackedSparseMatrixVectorParser extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends BaseGrammar> grammarClass() {
        return PackedSparseMatrixGrammar.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar, final ChartTraversalType chartTraversalType) {
        return new PackedSparseMatrixVectorParser((PackedSparseMatrixGrammar) grammar, chartTraversalType);
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
}
