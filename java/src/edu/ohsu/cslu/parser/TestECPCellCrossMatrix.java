package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit and performance tests for {@link TestECPCellCrossMatrix}
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestECPCellCrossMatrix extends ExhaustiveChartParserTestCase<ECPCellCrossMatrix> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "89443", "d820", "120416" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
