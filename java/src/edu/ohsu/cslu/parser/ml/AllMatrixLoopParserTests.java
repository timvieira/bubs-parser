package edu.ohsu.cslu.parser.ml;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestLeftChildLoopSpmlParser.class, TestRightChildLoopSpmlParser.class,
        TestCartesianProductBinarySearchSpmlParser.class, TestCartesianProductBinarySearchLeftChildSpmlParser.class,
        TestCartesianProductHashSpmlParser.class, TestGrammarLoopSpmlParser.class })
public class AllMatrixLoopParserTests {
}
