package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByChildMatrix;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit and performance tests for {@link TestECPCellCrossMatrix}
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestECPCellCrossMatrix extends ExhaustiveChartParserTestCase {

    @Override
    protected Class<? extends ArrayGrammar> grammarClass() {
        return GrammarByChildMatrix.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar, final ChartTraversalType chartTraversalType) {
        return new ECPCellCrossMatrix((GrammarByChildMatrix) grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "106400", "d820", "106767" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
