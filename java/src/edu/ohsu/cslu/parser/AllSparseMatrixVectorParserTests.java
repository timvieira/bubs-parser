package edu.ohsu.cslu.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestOpenClSpmvParser.class, TestJsaSpmvParser.class, TestCsrSpmvParser.class })
public class AllSparseMatrixVectorParserTests {
}
