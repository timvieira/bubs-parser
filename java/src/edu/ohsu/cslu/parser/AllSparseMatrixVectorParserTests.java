package edu.ohsu.cslu.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestOpenClSparseMatrixVectorParser.class, TestJsaSparseMatrixVectorParser.class, TestCsrSparseMatrixVectorParser.class })
public class AllSparseMatrixVectorParserTests {
}
