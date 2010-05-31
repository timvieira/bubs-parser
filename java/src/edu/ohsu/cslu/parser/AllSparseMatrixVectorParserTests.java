package edu.ohsu.cslu.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestCsrSpmvParser.class, TestCsrSpmvPerMidpointParser.class, TestCscSpmvParser.class,
        TestOpenClSpmvParser.class, TestSortAndScanCsrSpmvParser.class })
public class AllSparseMatrixVectorParserTests {
}
