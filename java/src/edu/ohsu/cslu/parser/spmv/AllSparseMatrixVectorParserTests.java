package edu.ohsu.cslu.parser.spmv;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestCsrSpmvParser.class, TestCsrSpmvPerMidpointParser.class, TestCscSpmvParser.class,
        TestDenseVectorOpenClSpmvParser.class
// , TestPackedOpenClSpmvParser
// , TestSortAndScanCsrSpmvParser.class
})
public class AllSparseMatrixVectorParserTests {
}
