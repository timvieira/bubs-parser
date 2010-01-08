package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
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
    protected Class<? extends ArrayGrammar> grammarClass() {
        return ArrayGrammar.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final ArrayGrammar grammar,
            final ChartTraversalType chartTraversalType) {
        return new ECPGramLoopBerkFilter(grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "8067", "d820", "10000" })
    public void profileSentences11Through20() throws Exception {
        super.internalProfileSentences11Through20();
    }
}
