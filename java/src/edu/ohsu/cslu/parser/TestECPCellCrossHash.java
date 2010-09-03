package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit and performance tests for {@link TestECPCellCrossHash}
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestECPCellCrossHash extends ExhaustiveChartParserTestCase<ECPCellCrossHash> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "277203", "d820", "337254" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
