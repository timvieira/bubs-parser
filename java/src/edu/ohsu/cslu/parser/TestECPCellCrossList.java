package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit and performance tests for {@link TestECPCellCrossList}
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestECPCellCrossList extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftListGrammar.class;
    }

    @Override
    protected Parser createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new ECPCellCrossList((LeftListGrammar) grammar, cellSelector);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "10081", "d820", "12376" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();

    }
}
