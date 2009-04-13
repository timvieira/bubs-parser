package edu.ohsu.cslu.datastructs.vectors;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {TestIntVector.class, TestPackedIntVector.class, TestFloatVector.class, TestPackedBitVector.class,
                      TestSparseBitVector.class})
public class AllVectorTests
{}
