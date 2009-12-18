package edu.ohsu.cslu.datastructs.matrices;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {TestIntMatrix.class, TestShortMatrix.class, TestByteMatrix.class, TestFloatMatrix.class,
    TestFixedPointShortMatrix.class, TestHashSparseFloatMatrix.class})
public class AllMatrixTests
{}
