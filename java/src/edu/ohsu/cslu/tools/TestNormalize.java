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

import org.junit.Test;

import cltool4j.ToolTestCase;

public class TestNormalize extends ToolTestCase {

    @Test
    public void testNNP() throws Exception {

        final StringBuilder input = new StringBuilder();
        input.append("(ROOT (S (NP (NNP AAPL)) (VP (VBD rose) (PP (IN on) (DT the) (NN news))) (. .)))\n");
        input.append("(ROOT (NP (: --) (NNP Rollin) (NNP S.) (NNP Trexler) (. .)))\n");

        final StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("(ROOT (S (NP (NNP UNK-CAPS)) (VP (VBD rose) (PP (IN on) (DT the) (NN news))) (. .)))\n");
        expectedOutput.append("(ROOT (NP (: --) (NNP UNK-CAPS) (NNP UNK-CAPS) (NNP UNK-CAPS-er) (. .)))\n");

        final String output = executeTool(new DecisionTreeNormalize(), "-t NNP -th 1", input.toString());
        assertEquals(expectedOutput.toString(), output);
    }
}
