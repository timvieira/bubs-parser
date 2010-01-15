package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
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
    protected Class<? extends ArrayGrammar> grammarClass() {
        return ArrayGrammar.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar, final ChartTraversalType chartTraversalType) {
        return new ECPGramLoop((ArrayGrammar) grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "17799", "d820", "20297" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
