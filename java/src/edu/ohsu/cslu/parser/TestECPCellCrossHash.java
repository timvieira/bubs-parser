package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermHash;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
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
    protected MaximumLikelihoodParser createParser(final Grammar grammar,
            final ChartTraversalType chartTraversalType) {
        return new ECPCellCrossHash((GrammarByLeftNonTermHash) grammar, chartTraversalType);
    }

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return GrammarByLeftNonTermHash.class;
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "118002" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();

    }
}
