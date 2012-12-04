/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.util;

import static junit.framework.Assert.assertEquals;

import org.cjunit.FilteredRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FilteredRunner.class)
public class TestPorterStemmer {

    @Test
    public void testStem() {
        final PorterStemmer stemmer = new PorterStemmer();

        assertEquals("like", stemmer.stemWord("liking"));

        // Test something the stemmer won't be able to stem
        assertEquals("aboveboard", stemmer.stemWord("aboveboard"));

        // And verify that sentence stemming produces the same thing that word stemming does
        assertEquals("redefin", stemmer.stemWord("redefine"));
        assertEquals("redefin like", stemmer.stemSentence("redefine liking"));
    }
}
