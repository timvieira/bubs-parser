package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit and performance tests for {@link TestECPGramLoopBerkFilter}
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestECPGramLoopBerkFilter extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return GrammarByChild.class;
    }

    @Override
    protected Parser<?> createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new ECPGrammarLoopBerkFilter(new ParserOptions(), (GrammarByChild) grammar);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "15594", "d820", "19609" })
    public void profileSentences11Through20() throws Exception {
        super.internalProfileSentences11Through20();
    }
}
