package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit and performance tests for {@link TestECPCellCrossHash}
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestECPCellCrossHash extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftHashGrammar.class;
    }

    @Override
    protected Parser createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new ECPCellCrossHash((LeftHashGrammar) grammar, cellSelector);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "111907", "d820", "169439" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
