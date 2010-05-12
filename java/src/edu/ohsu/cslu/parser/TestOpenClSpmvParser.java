package edu.ohsu.cslu.parser;

import org.junit.Ignore;
import org.junit.Test;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Tests for {@link OpenClSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestOpenClSpmvParser extends SparseMatrixVectorParserTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return CsrSparseMatrixGrammar.class;
    }

    @Override
    protected Parser<?> createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new OpenClSpmvParser((CsrSparseMatrixGrammar) grammar);
    }

    @Override
    @Ignore("OpenCL Parser does not currently implement filtering")
    public void testFilteredCrossProductVectorSimpleGrammar2() throws Exception {
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "2907244" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
