/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.ella;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public abstract class CountGrammarTestCase {

    protected CountGrammar g;

    @Test
    public void testRuleCounts() throws Exception {

        // Lexical rules
        assertEquals(3, g.lexicalRules());
        assertEquals(2, g.lexicalRuleObservations("a", "c"), 0.01f);
        assertEquals(1, g.lexicalRuleObservations("a", "d"), 0.01f);
        assertEquals(2, g.lexicalRuleObservations("b", "d"), 0.01f);

        assertEquals(0, g.lexicalRuleObservations("b", "c"), 0.01f);
        assertEquals(0, g.lexicalRuleObservations("a", "v"), 0.01f);
        assertEquals(0, g.lexicalRuleObservations("v", "a"), 0.01f);

        // Unary rules
        assertEquals(2, g.unaryRules());
        assertEquals(1, g.unaryRuleObservations("top", "a"), 0.01f);
        assertEquals(1, g.unaryRuleObservations("b", "b"), 0.01f);

        assertEquals(0, g.unaryRuleObservations("b", "a"), 0.01f);
        assertEquals(0, g.unaryRuleObservations("b", "v"), 0.01f);
        assertEquals(0, g.unaryRuleObservations("v", "b"), 0.01f);

        // Binary rules
        assertEquals(3, g.binaryRules());
        assertEquals(2, g.binaryRuleObservations("a", "a", "b"), 0.01f);
        assertEquals(1, g.binaryRuleObservations("a", "a", "a"), 0.01f);
        assertEquals(1, g.binaryRuleObservations("b", "b", "a"), 0.01f);

        assertEquals(0, g.binaryRuleObservations("a", "b", "a"), 0.01f);
        assertEquals(0, g.binaryRuleObservations("a", "v", "b"), 0.01f);
        assertEquals(0, g.binaryRuleObservations("v", "a", "b"), 0.01f);

        // Parent counts
        assertEquals(8, g.totalRules());
        try {
            assertEquals(6, g.observations("a"), 0.01f);
            assertEquals(4, g.observations("b"), 0.01f);
            assertEquals(0, g.observations("v"), 0.01f);
            assertEquals(0, g.observations("d"), 0.01f);
        } catch (final UnsupportedOperationException ignore) {
        }
    }

}
