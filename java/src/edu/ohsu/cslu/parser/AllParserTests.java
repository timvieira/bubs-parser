package edu.ohsu.cslu.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.parser.ml.AllMatrixLoopParserTests;
import edu.ohsu.cslu.parser.spmv.AllSparseMatrixVectorParserTests;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestParser.class, TestECPGramLoop.class, TestECPGramLoopBerkFilter.class,
        TestECPCellCrossHash.class, TestECPCellCrossList.class, TestECPCellCrossMatrix.class,
        AllSparseMatrixVectorParserTests.class, AllMatrixLoopParserTests.class })
public class AllParserTests {

}
