package edu.ohsu.cslu.ella;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestStringCountGrammar.class, TestMappedCountGrammar.class, TestProductionListGrammar.class,
        TestConstrainedChart.class, TestConstrainedCsrSpmvParser.class })
public class AllEllaTests {

    /**
     * <pre>
     *             s
     *             |
     *             a 
     *             |
     *       --------------
     *       |           |
     *       a           b
     *       |           |
     *    -------     --------
     *    |     |     |      |
     *    a     b     b      a
     *    |     |     |      |
     *  -----   d     b      d
     *  |   |         |
     *  a   a         d
     *  |   |
     *  c   c
     *  
     *  s -> a 1
     *  a -> a b 2/6
     *  a -> a a 1/6
     *  a -> c 2/6
     *  a -> d 1/6
     *  b -> b a 1/4
     *  b -> b 1/4
     *  b -> d 2/4
     * </pre>
     */
    final static String STRING_SAMPLE_TREE = "(s (a (a (a (a c) (a c)) (b d)) (b (b (b d)) (a d))))";

}
