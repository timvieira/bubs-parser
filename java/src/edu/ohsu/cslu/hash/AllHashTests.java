package edu.ohsu.cslu.hash;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestBasicInt2IntHash.class, TestPerfectInt2IntHash.class,
        TestBasicIntPair2IntHash.class, TestPerfectIntPair2IntHash.class,
        TestSegmentedPerfectIntPair2IntHash.class })
public class AllHashTests {

}
