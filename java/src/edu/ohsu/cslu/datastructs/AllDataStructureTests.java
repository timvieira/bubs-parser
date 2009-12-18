package edu.ohsu.cslu.datastructs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.datastructs.matrices.AllMatrixTests;
import edu.ohsu.cslu.datastructs.narytree.AllTreeTests;
import edu.ohsu.cslu.datastructs.vectors.AllVectorTests;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestFloatingPointLongPairwiseDistanceHeap.class,
        TestFixedPointLongPairwiseDistanceHeap.class, AllMatrixTests.class, AllVectorTests.class,
        AllTreeTests.class })
public class AllDataStructureTests {
}
