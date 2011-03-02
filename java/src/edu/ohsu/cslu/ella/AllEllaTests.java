package edu.ohsu.cslu.ella;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestStringCountGrammar.class, TestMappedCountGrammar.class, TestConstrainedCountGrammar.class,
        TestProductionListGrammar.class, TestConstrainedCsrSparseMatrixGrammar.class, TestConstrainedChart.class,
        TestConstrainedCsrSpmvParser.class })
public class AllEllaTests {

    /**
     * <pre>
     *            top
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
     *  top -> a 1
     *  a -> a b 2/6
     *  a -> a a 1/6
     *  a -> c 2/6
     *  a -> d 1/6
     *  b -> b a 1/4
     *  b -> b 1/4
     *  b -> d 2/4
     * </pre>
     */
    public final static String STRING_SAMPLE_TREE = "(top (a (a (a (a c) (a c)) (b d)) (b (b (b d)) (a d))))";

    public final static String TREE_WITH_LONG_UNARY_CHAIN = "(TOP (S (NP (NP (RB Not) (PDT all) (DT those)) (SBAR (WHNP (WP who)) (S (VP (VBD wrote))))) (VP (VBP oppose) (NP (DT the) (NNS changes))) (. .)))";

}
