package edu.ohsu.cslu.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestParser.class, TestECPGramLoop.class, TestECPGramLoopBerkFilter.class,
        TestECPCellCrossHash.class, TestECPCellCrossList.class, TestECPCellCrossMatrix.class,
        TestCsrSpmvParser.class, TestCsrSpmvPerMidpointParser.class, TestCscSpmvParser.class,
        TestDenseVectorOpenClSpmvParser.class, TestPackedOpenClSpmvParser.class
// ,TestSortAndScanCsrSpmvParser.class
})
public class AllParserTests {

}
