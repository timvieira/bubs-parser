package edu.ohsu.cslu.math;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.math.linear.TestFixedPointShortMatrix;
import edu.ohsu.cslu.math.linear.TestFloatMatrix;
import edu.ohsu.cslu.math.linear.TestFloatVector;
import edu.ohsu.cslu.math.linear.TestIntMatrix;
import edu.ohsu.cslu.math.linear.TestIntVector;
import edu.ohsu.cslu.math.linear.TestPackedBitVector;
import edu.ohsu.cslu.math.linear.TestPackedIntVector;
import edu.ohsu.cslu.math.linear.TestSparseBitVector;

@RunWith(Suite.class)
@Suite.SuiteClasses( {TestIntMatrix.class, TestFloatMatrix.class, TestFixedPointShortMatrix.class, TestIntVector.class,
                      TestPackedIntVector.class, TestFloatVector.class, TestPackedBitVector.class,
                      TestSparseBitVector.class})
public class AllMathTests
{}
