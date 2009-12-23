package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
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
        return GrammarByLeftNonTermList.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar,
            final ChartTraversalType chartTraversalType) {
        return new ECPCellCrossList((GrammarByLeftNonTermList) grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "13241" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();

    }
}
