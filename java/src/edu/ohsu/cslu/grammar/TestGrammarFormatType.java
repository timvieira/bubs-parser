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
package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link GrammarFormatType}
 * 
 * @author Aaron Dunlop
 */
public class TestGrammarFormatType {
    @Test
    public void testGetBaseNT() {
        assertEquals("@NP", GrammarFormatType.Berkeley.getBaseNT("@NP_2", false));
        assertEquals("NP", GrammarFormatType.Berkeley.getBaseNT("@NP_2", true));

        // Factored but not parent-annotated
        assertEquals("NP|", GrammarFormatType.CSLU.getBaseNT("NP|<S>", false));
        assertEquals("NP", GrammarFormatType.CSLU.getBaseNT("NP|<S>", true));
        assertEquals("NP|", GrammarFormatType.CSLU.getBaseNT("NP|<S-NN>", false));
        assertEquals("NP", GrammarFormatType.CSLU.getBaseNT("NP|<S-NN>", true));

        // Factored and parent-annotated
        assertEquals("NP|", GrammarFormatType.CSLU.getBaseNT("NP^ADJP|<S-NN>", false));
        assertEquals("NP", GrammarFormatType.CSLU.getBaseNT("NP^ADJP|<S-NN>", true));
    }
}
