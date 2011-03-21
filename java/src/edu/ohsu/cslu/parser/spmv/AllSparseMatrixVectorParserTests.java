package edu.ohsu.cslu.parser.spmv;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestCscSpmvParser.class, TestRowParallelCscSpmvParser.class, TestPrunedBeamCscSpmvParser.class,
        TestCsrSpmvParser.class, TestRowParallelCsrSpmvParser.class, TestCellParallelCsrSpmvParser.class,
        TestPrunedBeamCsrSpmvParser.class, TestDenseVectorOpenClSpmvParser.class
// , TestPackedOpenClSpmvParser
})
public class AllSparseMatrixVectorParserTests {
}
