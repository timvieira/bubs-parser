package edu.ohsu.cslu.parser.spmv;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestCsrSpmvParser.class, TestCsrSpmvPerMidpointParser.class, TestCscSpmvParser.class,
        TestBeamCscSpmvParser.class, TestPrunedBeamCscSpmvParser.class, TestDenseVectorOpenClSpmvParser.class
// , TestPackedOpenClSpmvParser
})
public class AllSparseMatrixVectorParserTests {
}
