package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit tests for {@link PackedOpenClSpmvParser}
 * 
 * @author Aaron Dunlop
 * @since Jun 2, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestPackedOpenClSpmvParser extends OpenClSpmvParserTestCase {
    @Override
    protected Parser<?> createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new PackedOpenClSpmvParser((CsrSparseMatrixGrammar) grammar);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "667853" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
