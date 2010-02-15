package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Tests for {@link OpenClSparseMatrixVectorParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestOpenClSparseMatrixVectorParser extends SparseMatrixVectorParserTestCase {

    @Override
    protected Class<? extends BaseGrammar> grammarClass() {
        return CsrSparseMatrixGrammar.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar, final ChartTraversalType chartTraversalType) {
        return new OpenClSparseMatrixVectorParser((CsrSparseMatrixGrammar) grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "88146", "d820", "200000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
