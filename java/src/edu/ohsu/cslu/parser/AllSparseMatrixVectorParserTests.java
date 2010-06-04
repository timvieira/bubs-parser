package edu.ohsu.cslu.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestCsrSpmvParser.class, TestCsrSpmvPerMidpointParser.class, TestCscSpmvParser.class,
        OpenClSpmvParserTestCase.class, TestSortAndScanCsrSpmvParser.class })
public class AllSparseMatrixVectorParserTests {
}
