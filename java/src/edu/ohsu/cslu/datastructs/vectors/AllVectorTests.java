package edu.ohsu.cslu.datastructs.vectors;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {TestIntVector.class, TestPackedIntVector.class, TestFloatVector.class,
    TestHashSparseFloatVector.class, TestPackedBitVector.class, TestSparseBitVector.class, TestMutableSparseBitVector.class})
public class AllVectorTests
{}
