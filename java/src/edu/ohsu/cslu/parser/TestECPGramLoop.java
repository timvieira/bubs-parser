package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit and performance tests for {@link TestECPGramLoop}
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestECPGramLoop extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return Grammar.class;
    }

    @Override
    protected Parser createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new ECPGrammarLoop(grammar, cellSelector);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "19884", "d820", "20297" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
