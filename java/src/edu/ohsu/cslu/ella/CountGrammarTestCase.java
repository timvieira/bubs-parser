package edu.ohsu.cslu.ella;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public abstract class CountGrammarTestCase {

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

    protected CountGrammar g;

    @Test
    public void testRuleCounts() throws Exception {

        // Lexical rules
        assertEquals(3, g.lexicalRules());
        assertEquals(2, g.lexicalRuleObservations("a", "c"));
        assertEquals(1, g.lexicalRuleObservations("a", "d"));
        assertEquals(2, g.lexicalRuleObservations("b", "d"));

        assertEquals(0, g.lexicalRuleObservations("b", "c"));
        assertEquals(0, g.lexicalRuleObservations("a", "v"));
        assertEquals(0, g.lexicalRuleObservations("v", "a"));

        // Unary rules
        assertEquals(2, g.unaryRules());
        assertEquals(1, g.unaryRuleObservations("s", "a"));
        assertEquals(1, g.unaryRuleObservations("b", "b"));

        assertEquals(0, g.unaryRuleObservations("b", "a"));
        assertEquals(0, g.unaryRuleObservations("b", "v"));
        assertEquals(0, g.unaryRuleObservations("v", "b"));

        // Binary rules
        assertEquals(3, g.binaryRules());
        assertEquals(2, g.binaryRuleObservations("a", "a", "b"));
        assertEquals(1, g.binaryRuleObservations("a", "a", "a"));
        assertEquals(1, g.binaryRuleObservations("b", "b", "a"));

        assertEquals(0, g.binaryRuleObservations("a", "b", "a"));
        assertEquals(0, g.binaryRuleObservations("a", "v", "b"));
        assertEquals(0, g.binaryRuleObservations("v", "a", "b"));

        // Parent counts
        assertEquals(8, g.totalRules());
        assertEquals(6, g.observations("a"));
        assertEquals(4, g.observations("b"));
        assertEquals(0, g.observations("v"));
        assertEquals(0, g.observations("d"));
    }

}
