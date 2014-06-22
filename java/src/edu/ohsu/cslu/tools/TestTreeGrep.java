/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */

package edu.ohsu.cslu.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import cltool4j.ToolTestCase;

/**
 * Unit tests for {@link TreeGrep}
 * 
 * @author Aaron Dunlop
 * @since Oct 16, 2012
 */
public class TestTreeGrep extends ToolTestCase {

    private String input;

    @Before
    public void setUp() {
        final StringBuilder sb = new StringBuilder(1024);

        // Long unary chains (length 4 and 3)
        sb.append("(top (a (b (c (d (e f)))) (g h)))\n");
        sb.append("(top (a (b (c (d f))) (g h)))\n");

        // One with multiple children of the root node
        sb.append("(top (a b) (c d))\n");
        input = sb.toString();
    }

    @Test
    public void testUnaryChainLength() throws Exception {
        assertEquals("(top (a (b (c (d (e f)))) (g h)))\n", executeTool(new TreeGrep(), "-ucl 4", input));
        final String expectedOutput = "(top (a (b (c (d (e f)))) (g h)))\n" + "(top (a (b (c (d f))) (g h)))\n";
        assertEquals(expectedOutput, executeTool(new TreeGrep(), "-ucl 3", input));
    }

    @Test
    public void testMultiChildRoot() throws Exception {
        assertEquals("(top (a b) (c d))\n", executeTool(new TreeGrep(), "-mcr ", input));
    }
}
