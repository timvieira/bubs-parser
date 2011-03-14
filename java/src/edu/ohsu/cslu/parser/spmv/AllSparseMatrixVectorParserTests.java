package edu.ohsu.cslu.parser.spmv;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestCscSpmvParser.class, TestBeamCscSpmvParser.class, TestPrunedBeamCscSpmvParser.class,
        TestCsrSpmvParser.class, TestCellParallelCsrSpmvParser.class, TestCsrSpmvPerMidpointParser.class,
        TestDenseVectorOpenClSpmvParser.class
// , TestPackedOpenClSpmvParser
})
public class AllSparseMatrixVectorParserTests {
}
