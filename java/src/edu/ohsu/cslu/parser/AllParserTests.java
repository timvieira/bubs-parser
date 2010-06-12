package edu.ohsu.cslu.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.parser.spmv.TestCscSpmvParser;
import edu.ohsu.cslu.parser.spmv.TestCsrSpmvParser;
import edu.ohsu.cslu.parser.spmv.TestCsrSpmvPerMidpointParser;
import edu.ohsu.cslu.parser.spmv.TestDenseVectorOpenClSpmvParser;
import edu.ohsu.cslu.parser.spmv.TestPackedOpenClSpmvParser;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestParser.class, TestECPGramLoop.class, TestECPGramLoopBerkFilter.class,
        TestECPCellCrossHash.class, TestECPCellCrossList.class, TestECPCellCrossMatrix.class,
        TestCsrSpmvParser.class, TestCsrSpmvPerMidpointParser.class, TestCscSpmvParser.class,
        TestDenseVectorOpenClSpmvParser.class, TestPackedOpenClSpmvParser.class
// ,TestSortAndScanCsrSpmvParser.class
})
public class AllParserTests {

}
